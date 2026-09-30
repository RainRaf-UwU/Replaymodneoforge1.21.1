package com.replaymod.replay.mixin;

import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class MixinCameraFov {
    @Inject(method = "getFov(Lnet/minecraft/client/render/Camera;FZ)D", at = @At("RETURN"), cancellable = true)
    private void replaymod$cameraPathFov(Camera camera, float tickDelta, boolean changingFov,
                                       CallbackInfoReturnable<Double> callback) {
        if (camera.getFocusedEntity() instanceof CameraEntity replayCamera) {
            double fov = replayCamera.getCameraFovOverride();
            if (Double.isFinite(fov)) callback.setReturnValue(fov);
        }
    }
}
