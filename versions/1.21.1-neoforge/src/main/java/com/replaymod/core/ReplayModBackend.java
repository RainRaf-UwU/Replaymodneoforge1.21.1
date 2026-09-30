package com.replaymod.core;

import com.replaymod.core.utils.Restrictions;
import com.replaymod.core.versions.LangResourcePack;
import net.minecraft.SharedConstants;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackPosition;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.core.gui.GuiReplaySettings;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Optional;
import java.util.Set;

import static com.replaymod.core.ReplayMod.MOD_ID;

@Mod(value = MOD_ID, dist = Dist.CLIENT)
public class ReplayModBackend {
    private static final Set<Identifier> CAMERA_HIDDEN_LAYERS = Set.of(
            VanillaGuiLayers.HOTBAR, VanillaGuiLayers.JUMP_METER, VanillaGuiLayers.EXPERIENCE_BAR,
            VanillaGuiLayers.PLAYER_HEALTH, VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.VEHICLE_HEALTH, VanillaGuiLayers.AIR_LEVEL, VanillaGuiLayers.EFFECTS,
            VanillaGuiLayers.EXPERIENCE_LEVEL, VanillaGuiLayers.SELECTED_ITEM_NAME, VanillaGuiLayers.SPECTATOR_TOOLTIP);
    private ReplayMod mod;

    public ReplayModBackend(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::registerKeyMappings);
        modEventBus.addListener(this::addPackFinders);
        modEventBus.addListener(this::registerPayloadHandlers);
        NeoForge.EVENT_BUS.addListener(this::renderReplayHud);
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new GuiReplaySettings(parent, mod.getSettingsRegistry()).toMinecraft());
    }

    private void renderReplayHud(RenderGuiLayerEvent.Pre event) {
        if (MinecraftClient.getInstance().player instanceof CameraEntity
                && CAMERA_HIDDEN_LAYERS.contains(event.getName())) {
            event.setCanceled(true);
        }
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToClient(Restrictions.ID, Restrictions.CODEC, (payload, context) -> {});
        registrar.configurationToClient(Restrictions.ID, Restrictions.CODEC, (payload, context) -> {});
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // NeoForge fires this on the render thread after constructing Minecraft's client services,
        // before the initial resource reload (and before FMLClientSetupEvent).
        mod = new ReplayMod(this);
        mod.initModules();
        mod.getKeyBindingRegistry().getBindings().values()
                .forEach(binding -> event.register(binding.keyBinding));
    }

    private void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == ResourceType.CLIENT_RESOURCES) {
            ResourcePackProfile langPack = ResourcePackProfile.create(
                    new ResourcePackInfo(LangResourcePack.NAME, Text.literal("ReplayMod Translations"), ResourcePackSource.BUILTIN, Optional.empty()),
                    new ResourcePackProfile.PackFactory() {
                        public ResourcePack open(ResourcePackInfo i) { return new LangResourcePack(); }
                        public ResourcePack openWithOverlays(ResourcePackInfo i, ResourcePackProfile.Metadata m) { return open(i); }
                    },
                    ResourceType.CLIENT_RESOURCES,
                    new ResourcePackPosition(true, ResourcePackProfile.InsertionPosition.TOP, true)
            );
            if (langPack != null) {
                event.addRepositorySource(consumer -> consumer.accept(langPack));
            }
            if (ReplayMod.jGuiResourcePack != null) {
                final ResourcePack jguiPack0 = ReplayMod.jGuiResourcePack;
                ResourcePackProfile jguiPack = ResourcePackProfile.create(
                        new ResourcePackInfo(ReplayMod.JGUI_RESOURCE_PACK_NAME, Text.literal("jGui Resources (dev)"), ResourcePackSource.BUILTIN, Optional.empty()),
                        new ResourcePackProfile.PackFactory() {
                            public ResourcePack open(ResourcePackInfo i) { return jguiPack0; }
                            public ResourcePack openWithOverlays(ResourcePackInfo i, ResourcePackProfile.Metadata m) { return open(i); }
                        },
                        ResourceType.CLIENT_RESOURCES,
                        new ResourcePackPosition(true, ResourcePackProfile.InsertionPosition.TOP, true)
                );
                if (jguiPack != null) {
                    event.addRepositorySource(consumer -> consumer.accept(jguiPack));
                }
            }
        }
    }

    public String getVersion() {
        return ModList.get().getModContainerById(MOD_ID)
                .orElseThrow(IllegalStateException::new)
                .getModInfo().getVersion().toString();
    }

    public String getMinecraftVersion() {
        return SharedConstants.getGameVersion().getName();
    }

    public boolean isModLoaded(String id) {
        return ModList.get().isLoaded(id);
    }
}
