package de.johni0702.minecraft.gui.versions.mixin;

import de.johni0702.minecraft.gui.versions.callbacks.PostRenderScreenCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    // NeoForge draws the active screen and its background layers through this
    // hook. Capture its context and dispatch after the complete draw, as before.
    private static final String DRAW_SCREEN = "Lnet/neoforged/neoforge/client/ClientHooks;drawScreen(Lnet/minecraft/client/gui/screen/Screen;Lnet/minecraft/client/gui/DrawContext;IIF)V";

    @Unique
    private DrawContext context;

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = DRAW_SCREEN), index = 1)
    private DrawContext captureContext(DrawContext context) {
        this.context = context;
        return context;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = DRAW_SCREEN, shift = At.Shift.AFTER))
    private void postRenderScreen(RenderTickCounter tickCounter, boolean renderWorld, CallbackInfo ci) {
        PostRenderScreenCallback.EVENT.invoker().postRenderScreen(context, tickCounter.getTickDelta(true));
    }
}
