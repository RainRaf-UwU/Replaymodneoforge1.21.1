package de.johni0702.minecraft.gui.versions.mixin;

import de.johni0702.minecraft.gui.versions.callbacks.RenderHudCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class Mixin_RenderHudCallback {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void registerHudCallback(MinecraftClient minecraft, CallbackInfo ci) {
        // Draw after NeoForge's HUD layers so modal jGui popups cover chat and
        // overlays, whose layer depth would otherwise put them above the popup.
        NeoForge.EVENT_BUS.addListener((RenderGuiEvent.Post event) -> {
            if (!minecraft.options.hudHidden) {
                var matrices = event.getGuiGraphics().getMatrices();
                matrices.push();
                // VanillaGuiLayers occupy successive 200-unit depth ranges.
                // Keep jGui in front of those ranges, inside the GUI projection.
                matrices.translate(0, 0, 8000);
                try {
                    RenderHudCallback.EVENT.invoker().renderHud(event.getGuiGraphics(),
                            event.getPartialTick().getTickDelta(true));
                } finally { matrices.pop(); }
            }
        });
    }
}
