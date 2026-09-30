package com.replaymod.porting;

import com.google.common.util.concurrent.ListenableFuture;
import com.replaymod.core.ReplayMod;
import com.replaymod.pathing.player.RealtimeTimelinePlayer;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.simplepathing.SPTimeline;
import com.replaymod.simplepathing.InterpolatorType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Opt-in, development-only integration test; never included in the mod artifact. */
@EventBusSubscriber(modid = "replaymod", value = Dist.CLIENT)
public final class PortIntegration {
    private static final Logger LOG = LogManager.getLogger("ReplayModPortIntegration");
    private static final Path REPORT = Path.of("E:/NeoForgeReplay-porting/integration");
    private static final long RUN_START = System.currentTimeMillis();
    private static int phase;
    private static long since = RUN_START;
    private static long worldStart;
    private static int worldTicks;
    private static Vec3d initialPosition;
    private static BlockPos placed;
    private static boolean placedBlock, brokeBlock, interacted;
    private static ReplayHandler replay;
    private static int sampleTimestamp;
    private static int speedIndex;
    private static final double[] SPEEDS = {0.5, 1, 2, 4};
    private static Vec3d cameraStart;
    private static int seekIndex;
    private static final int[] SEEKS = {20000, 5000, 25000, 2000};
    private static SPTimeline path;
    private static ListenableFuture<Void> pathFuture;
    private static boolean pathSampled;
    private static com.replaymod.simplepathing.gui.GuiEditKeyframe.Position editor;
    private static com.replaymod.render.gui.GuiRenderSettings renderGui;
    private static boolean finished;
    private static boolean busy;
    private static int movementIndex;
    private static int scrubExpected;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (phase < 4) runChecks();
    }

    private static void runChecks() {
        if (!Boolean.getBoolean("replaymod.porting.test") || finished || busy) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!mc.isFinishedLoading()) return;
        busy = true;
        try {
            long elapsed = System.currentTimeMillis() - since;
            if (elapsed > 180000) throw new IllegalStateException("Phase " + phase + " timed out: screen=" + mc.currentScreen);
            switch (phase) {
                case 0 -> {
                    Files.createDirectories(REPORT);
                    // Playback pause suppresses world ticks, but rendered frames continue.
                    new de.johni0702.minecraft.gui.utils.EventRegistrations() {
                        { on(com.replaymod.core.events.PostRenderCallback.EVENT, () -> {
                            if (phase >= 4) runChecks();
                        }); }
                    }.register();
                    mc.options.pauseOnLostFocus = false;
                    mc.options.getViewDistance().setValue(4);
                    ReplayMod.instance.getSettingsRegistry().set(com.replaymod.core.Setting.RECORDING_PATH,
                            REPORT.resolve("recordings-" + RUN_START).toString());
                    ReplayMod.instance.getSettingsRegistry().set(com.replaymod.core.Setting.CACHE_PATH,
                            REPORT.resolve("cache-" + RUN_START).toString());
                    // Enable the test in memory, respecting the user's persisted preferences.
                    ReplayMod.instance.getSettingsRegistry().set(com.replaymod.recording.Setting.RECORD_SINGLEPLAYER, true);
                    ReplayMod.instance.getSettingsRegistry().set(com.replaymod.recording.Setting.AUTO_START_RECORDING, true);
                    ReplayMod.instance.getSettingsRegistry().set(com.replaymod.recording.Setting.RENAME_DIALOG, false);
                    screenshot(mc, "title");
                    LOG.info("PORT_TEST startup: Minecraft={}, NeoForge mod ReplayMod loaded", mc.getGameVersion());
                    String existingReplay = System.getProperty("replaymod.porting.replayFile");
                    if (existingReplay != null) {
                        replay = ReplayModReplay.instance.startReplay(ReplayMod.instance.files.open(Path.of(existingReplay)), false, true);
                        next(4);
                        break;
                    }
                    String world = "ReplayMod-1.21.1-Test-" + RUN_START;
                    mc.createIntegratedServerLoader().createAndStart(world,
                            new LevelInfo(world, GameMode.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), DataConfiguration.SAFE_MODE),
                            new GeneratorOptions(12101, false, false),
                            registries -> registries.get(RegistryKeys.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createDimensionsRegistryHolder(),
                            new TitleScreen());
                    next(1);
                }
                case 1 -> {
                    if (mc.world != null && mc.player != null && mc.currentScreen == null) {
                        initialPosition = mc.player.getPos();
                        worldStart = System.currentTimeMillis();
                        mc.player.setYaw(0);
                        mc.player.setPitch(15);
                        mc.getNetworkHandler().sendChatCommand("give @s minecraft:stone 64");
                        mc.getNetworkHandler().sendChatCommand("give @s minecraft:stick 1");
                        mc.getNetworkHandler().sendChatCommand("summon minecraft:cow ~ ~ ~3");
                        LOG.info("PORT_TEST entered real singleplayer world at {}", initialPosition);
                        next(2);
                    }
                }
                case 2 -> record(mc);
                case 3 -> {
                    Path folder = ReplayMod.instance.folders.getReplayFolder();
                    Path recording;
                    try (var files = Files.list(folder)) {
                        recording = files.filter(p -> p.toString().endsWith(".mcpr"))
                                .filter(p -> p.toFile().lastModified() >= RUN_START)
                                .max(Comparator.comparingLong(p -> p.toFile().lastModified())).orElse(null);
                    }
                    if (recording != null && elapsed > 3000) {
                        LOG.info("PORT_TEST saved replay {} ({} bytes)", recording, Files.size(recording));
                        Files.writeString(REPORT.resolve("replay-path.txt"), recording.toAbsolutePath().toString());
                        replay = ReplayModReplay.instance.startReplay(ReplayMod.instance.files.open(recording), false, true);
                        next(4);
                    }
                }
                case 4 -> {
                    if (mc.world != null && replay.getCameraEntity() != null && elapsed > 5000) {
                        check(replay.getReplayDuration() >= 30000, "Replay duration >= 30 seconds");
                        check(mc.world.getChunkManager().getLoadedChunkCount() > 0, "Replay chunks loaded");
                        check(mc.world.getEntities().iterator().hasNext(), "Replay entities loaded");
                        check(mc.world.getPlayers().stream().anyMatch(p -> !(p instanceof CameraEntity)), "Recorded player model exists");
                        screenshot(mc, "replay-loaded");
                        replay.getReplaySender().setReplaySpeed(0);
                        sampleTimestamp = replay.getReplaySender().currentTimeStamp();
                        next(5);
                    }
                }
                case 5 -> {
                    if (elapsed > 1000) {
                        check(Math.abs(replay.getReplaySender().currentTimeStamp() - sampleTimestamp) < 150, "Replay pause holds timestamp");
                        cameraStart = replay.getCameraEntity().getPos();
                        mc.options.forwardKey.setPressed(true);
                        mc.options.jumpKey.setPressed(true);
                        next(6);
                    }
                }
                case 6 -> {
                    if (elapsed > 1200) {
                        resetKeys(mc);
                        check(replay.getCameraEntity().getPos().squaredDistanceTo(cameraStart) > 0.05, "Free camera moves while replay paused");
                        replay.getCameraEntity().setCameraRotation(40, 15, 10);
                        replay.getCameraEntity().setCameraFov(90);
                        screenshot(mc, "free-camera-paused");
                        movementIndex = 0;
                        next(17);
                    }
                }
                case 7 -> {
                    if (elapsed > 1100) {
                        int delta = replay.getReplaySender().currentTimeStamp() - sampleTimestamp;
                        LOG.info("PORT_TEST speed {}x advanced {}ms over {}ms", SPEEDS[speedIndex], delta, elapsed);
                        check(Math.abs(delta - elapsed * SPEEDS[speedIndex]) < 250,
                                "Replay timestamp advances at " + SPEEDS[speedIndex] + "x");
                        if (++speedIndex < SPEEDS.length) { startSpeed(); next(7); }
                        else { replay.getReplaySender().setReplaySpeed(0); seekIndex = 0; replay.doJump(SEEKS[0], true); next(8); }
                    }
                }
                case 8 -> {
                    if (elapsed > 3000 && mc.world != null && replay.getCameraEntity() != null) {
                        int now = replay.getReplaySender().currentTimeStamp();
                        check(Math.abs(now - SEEKS[seekIndex]) < 1500, "Seek to " + SEEKS[seekIndex] + ", actual=" + now);
                        check(mc.world.getChunkManager().getLoadedChunkCount() > 0, "Chunks survive seek");
                        screenshot(mc, "seek-" + SEEKS[seekIndex]);
                        if (++seekIndex < SEEKS.length) { replay.doJump(SEEKS[seekIndex], true); next(8); }
                        else {
                            replay.getOverlay().setMouseVisible(true);
                            var button = replay.getOverlay().playPauseButton;
                            var origin = new de.johni0702.minecraft.gui.utils.lwjgl.Point(0, 0);
                            button.getContainer().convertFor(button, origin);
                            mc.currentScreen.mouseClicked(-origin.getX() + 10, -origin.getY() + 10, 0);
                            check(!replay.getReplaySender().paused(), "Overlay play button resumes replay");
                            mc.currentScreen.mouseClicked(-origin.getX() + 10, -origin.getY() + 10, 0);
                            check(replay.getReplaySender().paused(), "Overlay pause button pauses replay");
                            timelineInput(mc, 0.7, false);
                            next(19);
                        }
                    }
                }
                case 9 -> {
                    if (!pathSampled && elapsed > 1700 && elapsed < 2600) {
                        check(replay.getCameraEntity().getCameraFov() > 75 && replay.getCameraEntity().getCameraFov() <= 90,
                                "FOV interpolates during camera path");
                        check(replay.getCameraEntity().roll > 5, "Roll interpolates during camera path");
                        screenshot(mc, "camera-path-midpoint");
                        pathSampled = true;
                    }
                    if (pathFuture.isDone()) {
                        pathFuture.get();
                        next(10);
                    }
                }
                case 10 -> {
                    if (elapsed > 200) {
                        check(replay.getCameraEntity().getCameraFov() == 90,
                                "Camera path restores free-camera FOV");
                        check(pathSampled, "Camera path interpolation sampled in game");
                        LOG.info("PORT_TEST camera path completed with 3 position/FOV/roll and 2 time keyframes");
                        screenshot(mc, "camera-path-complete");
                        com.replaymod.simplepathing.ReplayModSimplePathing.instance.setCurrentTimeline(path);
                        editor = new com.replaymod.simplepathing.gui.GuiEditKeyframe.Position(
                                com.replaymod.simplepathing.ReplayModSimplePathing.instance.getGuiPathing(), SPTimeline.SPPath.POSITION, 2000);
                        editor.open();
                        next(11);
                    }
                }
                case 11 -> {
                    if (elapsed > 1500) {
                        screenshot(mc, "keyframe-editor");
                        editor.fovField.setValue(85);
                        editor.saveButton.getOnClick().consume(new de.johni0702.minecraft.gui.function.Click(0, 0, 0, 0));
                        check(path.getPositionPath().getKeyframe(2000).getValue(com.replaymod.pathing.properties.FovProperty.PROPERTY).orElseThrow() == 85,
                                "Keyframe editor saves FOV");
                        var renderScreen = new de.johni0702.minecraft.gui.container.GuiScreen();
                        renderScreen.display();
                        renderGui = new com.replaymod.render.gui.GuiRenderSettings(renderScreen, replay, path.getTimeline());
                        renderGui.open();
                        next(12);
                    }
                }
                case 12 -> {
                    if (elapsed > 1500) {
                        screenshot(mc, "render-settings");
                        renderGui.close();
                        mc.setScreen(null);
                        renderFrames();
                        next(13);
                    }
                }
                case 13 -> {
                    if (elapsed > 1000) {
                        screenshot(mc, "render-complete");
                        new com.replaymod.core.gui.GuiReplaySettings(null, ReplayMod.instance.getSettingsRegistry()).display();
                        next(14);
                    }
                }
                case 14 -> {
                    if (elapsed > 1500) {
                        screenshot(mc, "replay-settings");
                        replay.endReplay();
                        new com.replaymod.replay.gui.screen.GuiReplayViewer(ReplayModReplay.instance).display();
                        next(15);
                    }
                }
                case 15 -> {
                    if (elapsed > 1500) {
                        screenshot(mc, "replay-viewer");
                        mc.setScreen(new TitleScreen());
                        next(16);
                    }
                }
                case 16 -> {
                    if (elapsed > 1500) {
                        screenshot(mc, "main-menu");
                        LOG.info("PORT_TEST auditing remaining Mixin targets");
                        org.spongepowered.asm.mixin.MixinEnvironment.getCurrentEnvironment().audit();
                        LOG.info("PORT_TEST Mixin target audit returned");
                        Files.writeString(REPORT.resolve("SUCCESS.txt"), "Integration completed at " + System.currentTimeMillis());
                        finished = true;
                        LOG.info("PORT_TEST ALL INTEGRATION CHECKS PASSED");
                        mc.scheduleStop();
                    }
                }
                case 17 -> {
                    cameraStart = replay.getCameraEntity().getPos();
                    cameraKeys(mc)[movementIndex].setPressed(true);
                    next(18);
                }
                case 18 -> {
                    if (elapsed > 350) {
                        resetKeys(mc);
                        check(replay.getCameraEntity().getPos().squaredDistanceTo(cameraStart) > 0.0025,
                                "Paused camera input " + new String[]{"W", "S", "A", "D", "Space", "Shift"}[movementIndex]);
                        if (++movementIndex < 6) next(17);
                        else {
                            CameraEntity camera = replay.getCameraEntity();
                            float yaw = camera.getYaw(), pitch = camera.getPitch();
                            camera.changeLookDirection(12, -5);
                            check(camera.getYaw() != yaw && camera.getPitch() != pitch, "Mouse look changes camera rotation");
                            com.replaymod.replay.InputReplayTimer.handleScroll(5);
                            com.replaymod.replay.InputReplayTimer.handleScroll(-5);
                            check(mc.getCameraEntity() == camera, "Camera stays independent from recorded player");
                            speedIndex = 0;
                            startSpeed();
                            next(7);
                        }
                    }
                }
                case 19, 20, 21 -> {
                    if (elapsed > 1500 && mc.world != null && replay.getCameraEntity() != null) {
                        int now = replay.getReplaySender().currentTimeStamp();
                        check(Math.abs(now - scrubExpected) < 150,
                                "Timeline GUI " + new String[]{"click", "backward drag", "forward drag"}[phase - 19]
                                        + " seeks to " + scrubExpected + ", actual=" + now);
                        check(mc.world.getChunkManager().getLoadedChunkCount() > 0, "Timeline GUI seek preserves chunks");
                        if (phase == 19) { timelineInput(mc, 0.15, true); next(20); }
                        else if (phase == 20) {
                            check(mc.currentScreen != null && replay.getOverlay().isMouseVisible(),
                                    "Timeline drag retains input screen after rewind");
                            timelineInput(mc, 0.6, true);
                            next(21);
                        }
                        else {
                            mc.currentScreen.mouseReleased(0, 0, 0);
                            replay.getOverlay().setMouseVisible(false);
                            createPath();
                            next(9);
                        }
                    }
                }
                default -> throw new IllegalStateException("Unknown test phase " + phase);
            }
        } catch (Exception e) {
            finished = true;
            LOG.error("PORT_TEST FAILED in phase {}", phase, e);
            try { Files.createDirectories(REPORT); Files.writeString(REPORT.resolve("FAILURE.txt"), "Phase " + phase + ": " + e); }
            catch (java.io.IOException reportError) { e.addSuppressed(reportError); }
            throw new IllegalStateException("ReplayMod integration test failed", e);
        } finally {
            busy = false;
        }
    }

    private static void record(MinecraftClient mc) throws Exception {
        worldTicks++;
        long elapsed = System.currentTimeMillis() - worldStart;
        mc.options.forwardKey.setPressed(elapsed < 1800);
        mc.options.leftKey.setPressed(elapsed >= 1800 && elapsed < 3000);
        mc.options.jumpKey.setPressed(elapsed >= 700 && elapsed < 950);
        mc.options.backKey.setPressed(elapsed >= 3000 && elapsed < 4000);
        mc.options.rightKey.setPressed(elapsed >= 4000 && elapsed < 5000);
        if (worldTicks == 60) {
            // Regression: late startup scanning must not move an active recording.
            ReplayMod.instance.files.initialScan(ReplayMod.instance);
            LOG.info("PORT_TEST rescanned during active recording");
        }
        if (!placedBlock && elapsed > 6000) {
            // Keep the placement clear of the moving player and spawned cow.
            placed = mc.player.getBlockPos().add(2, -1, 0);
            mc.player.getInventory().selectedSlot = 0;
            check(mc.player.getMainHandStack().isOf(Items.STONE), "Given block arrives through network");
            var result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND,
                    new BlockHitResult(Vec3d.ofCenter(placed).add(0, 0.5, 0), Direction.UP, placed, false));
            LOG.info("PORT_TEST place: player={}, support={} {}, result={}", mc.player.getPos(), placed, mc.world.getBlockState(placed), result);
            placed = placed.up();
            placedBlock = true;
        }
        if (!brokeBlock && elapsed > 9000) {
            check(mc.world.getBlockState(placed).isOf(net.minecraft.block.Blocks.STONE), "Block placement synchronized");
            mc.interactionManager.attackBlock(placed, Direction.UP);
            brokeBlock = true;
        }
        if (!interacted && elapsed > 12000) {
            mc.player.getInventory().selectedSlot = 1;
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof CowEntity) {
                    mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND);
                    interacted = true;
                    break;
                }
            }
            check(interacted, "Entity spawned and interaction sent");
        }
        if (elapsed > 15000 && worldTicks % 100 == 0) screenshot(mc, "recording-world");
        if (elapsed >= 35000) {
            resetKeys(mc);
            check(mc.player.getPos().squaredDistanceTo(initialPosition) > 0.25, "WASD moved recorded player");
            check(mc.world.getBlockState(placed).isAir(), "Block breaking synchronized");
            LOG.info("PORT_TEST recorded {}ms, {} client ticks, placement, break, item switch, entity interaction", elapsed, worldTicks);
            screenshot(mc, "recording-complete");
            mc.world.disconnect();
            mc.disconnect(new TitleScreen());
            next(3);
        }
    }

    private static void createPath() {
        path = new SPTimeline();
        path.setDefaultInterpolatorType(InterpolatorType.LINEAR);
        CameraEntity camera = replay.getCameraEntity();
        camera.setCameraFov(90);
        double x = camera.getX(), y = camera.getY() + 3, z = camera.getZ();
        path.addPositionKeyframe(0, x, y, z, 0, 15, 0, -1, 70);
        path.addPositionKeyframe(2000, x + 6, y + 2, z + 3, 45, 20, 15, -1, 90);
        path.addPositionKeyframe(4000, x + 12, y, z + 6, 90, 15, 0, -1, 70);
        path.addTimeKeyframe(0, 2000);
        path.addTimeKeyframe(4000, 6000);
        path.addPositionKeyframe(3000, x, y, z, 0, 0, 0, -1, 70);
        path.removePositionKeyframe(3000);
        path.moveKeyframe(SPTimeline.SPPath.POSITION, 2000, 2100);
        path.moveKeyframe(SPTimeline.SPPath.POSITION, 2100, 2000);
        check(path.getPositionPath().getKeyframes().size() == 3, "Keyframe create/delete/move preserve 3 position keyframes");
        path.getTimeline().getPaths().forEach(p -> p.updateAll());
        pathFuture = new RealtimeTimelinePlayer(replay).start(path.getTimeline());
    }

    private static void renderFrames() throws Exception {
        com.replaymod.render.RenderSettings settings = new com.replaymod.render.RenderSettings(
                com.replaymod.render.RenderSettings.RenderMethod.DEFAULT,
                com.replaymod.render.RenderSettings.EncodingPreset.PNG, 320, 180, 5, 1 << 20,
                REPORT.resolve("render-frames-" + RUN_START).toFile(), true, false, false, false, false,
                null, 360, 180, false, false, false, com.replaymod.render.RenderSettings.AntiAliasing.NONE, "", "", false);
        try {
            check(new com.replaymod.render.rendering.VideoRenderer(settings, replay, path.getTimeline()).renderVideo(),
                    "Video renderer completes PNG pipeline");
        } catch (Error error) {
            throw error;
        } catch (Throwable error) {
            throw new Exception("Video rendering failed", error);
        }
        try (var frames = Files.list(settings.getOutputFile().toPath())) {
            check(frames.filter(p -> p.toString().endsWith(".png")).count() == 20, "Video render writes 20 PNG frames");
        }
        String ffmpeg = "E:/NeoForgeReplay-porting/ffmpeg.exe";
        if (Files.exists(Path.of(ffmpeg))) {
            mcSetScreenNull();
            settings = new com.replaymod.render.RenderSettings(
                    com.replaymod.render.RenderSettings.RenderMethod.DEFAULT,
                    com.replaymod.render.RenderSettings.EncodingPreset.MP4_CUSTOM, 320, 180, 5, 1 << 20,
                    REPORT.resolve("render-video-" + RUN_START + ".mp4").toFile(), true, false, false, false, false,
                    null, 360, 180, false, false, false, com.replaymod.render.RenderSettings.AntiAliasing.NONE,
                    ffmpeg, com.replaymod.render.RenderSettings.EncodingPreset.MP4_CUSTOM.getValue(), false);
            try {
                check(new com.replaymod.render.rendering.VideoRenderer(settings, replay, path.getTimeline()).renderVideo(),
                        "FFmpeg MP4 renderer completes");
            } catch (Error error) { throw error; }
            catch (Throwable error) { throw new Exception("MP4 rendering failed", error); }
            check(Files.size(settings.getOutputFile().toPath()) > 1000, "FFmpeg writes MP4 file");
        }
    }

    private static void mcSetScreenNull() { MinecraftClient.getInstance().setScreen(null); }
    private static void timelineInput(MinecraftClient mc, double fraction, boolean drag) {
        var timeline = replay.getOverlay().timeline;
        var origin = new de.johni0702.minecraft.gui.utils.lwjgl.Point(0, 0);
        timeline.getContainer().convertFor(timeline, origin);
        int body = timeline.getLastSize().getWidth() - 8;
        int localX = 4 + (int) Math.round(body * fraction);
        int x = localX - origin.getX(), y = 10 - origin.getY();
        scrubExpected = (int) Math.round(timeline.getOffset()
                + timeline.getLength() * timeline.getZoom() * (localX - 4) / body);
        check(drag ? mc.currentScreen.mouseDragged(x, y, 0, 1, 0)
                : mc.currentScreen.mouseClicked(x, y, 0), "Timeline screen input handled");
    }
    private static net.minecraft.client.option.KeyBinding[] cameraKeys(MinecraftClient mc) {
        return new net.minecraft.client.option.KeyBinding[]{mc.options.forwardKey, mc.options.backKey,
                mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey, mc.options.sneakKey};
    }

    private static void startSpeed() {
        sampleTimestamp = replay.getReplaySender().currentTimeStamp();
        replay.getReplaySender().setReplaySpeed(SPEEDS[speedIndex]);
    }

    private static void resetKeys(MinecraftClient mc) {
        mc.options.forwardKey.setPressed(false); mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false); mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false); mc.options.sneakKey.setPressed(false);
    }

    private static void next(int target) { phase = target; since = System.currentTimeMillis(); LOG.info("PORT_TEST phase {}", target); }
    private static void check(boolean condition, String name) {
        if (!condition) throw new IllegalStateException(name);
        LOG.info("PORT_TEST PASS {}", name);
    }
    private static void screenshot(MinecraftClient mc, String name) throws java.io.IOException {
        try (NativeImage image = ScreenshotRecorder.takeScreenshot(mc.getFramebuffer())) { image.writeTo(REPORT.resolve(name + ".png")); }
    }
}
