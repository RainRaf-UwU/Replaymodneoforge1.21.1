package com.replaymod.render.mixin;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

@Mixin(ParticleManager.class)
public abstract class MixinParticleManager {
    // NeoForge moves the loop into its added render overload, whose name is
    // already Mojang-named in both development and production. Select its full
    // signature: the vanilla three-argument wrapper is also named render in
    // production. Class literals in @Desc are remapped with the class file,
    // while the vanilla particle call uses the Minecraft refmap.
    @Redirect(target = @Desc(value = "render", args = {LightmapTextureManager.class,
            Camera.class, float.class, Frustum.class, Predicate.class}), remap = false, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/Particle;buildGeometry(Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/Camera;F)V", remap = true))
    private void buildOrientedGeometry(Particle particle, VertexConsumer vertices, Camera camera, float partialTicks) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
        if (handler == null || !handler.omnidirectional) {
            particle.buildGeometry(vertices, camera, partialTicks);
            return;
        }
        Quaternionf rotation = camera.getRotation();
        Quaternionf original = new Quaternionf(rotation);
        try {
            Vec3d from = new Vec3d(0, 0, 1);
            Vec3d to = MCVer.getPosition(particle, partialTicks).subtract(camera.getPos()).normalize();
            Vec3d axis = from.crossProduct(to);
            rotation.set((float) axis.x, (float) axis.y, (float) axis.z, (float) (1 + from.dotProduct(to))).normalize();
            particle.buildGeometry(vertices, camera, partialTicks);
        } finally {
            rotation.set(original);
        }
    }
}
