package de.johni0702.minecraft.gui.versions.mixin;

import de.johni0702.minecraft.gui.function.CharInput;
import de.johni0702.minecraft.gui.function.KeyInput;
import de.johni0702.minecraft.gui.versions.callbacks.KeyboardCallback;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboardListener {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void registerKeyboardCallbacks(MinecraftClient minecraft, CallbackInfo ci) {
        // Screen events cover NeoForge's changed character lambdas, including
        // the separate UTF-16 characters emitted for supplementary code points.
        NeoForge.EVENT_BUS.addListener((ScreenEvent.KeyPressed.Pre event) -> {
            if (KeyboardCallback.EVENT.invoker().keyPressed(
                    new KeyInput(event.getKeyCode(), event.getScanCode(), event.getModifiers()))) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.KeyReleased.Pre event) -> {
            if (KeyboardCallback.EVENT.invoker().keyReleased(
                    new KeyInput(event.getKeyCode(), event.getScanCode(), event.getModifiers()))) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.CharacterTyped.Pre event) -> {
            if (KeyboardCallback.EVENT.invoker().charTyped(
                    new CharInput(event.getCodePoint(), event.getModifiers()))) {
                event.setCanceled(true);
            }
        });
    }
}
