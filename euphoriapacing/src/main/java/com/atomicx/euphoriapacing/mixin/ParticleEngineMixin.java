package com.atomicx.euphoriapacing.mixin;

import com.atomicx.euphoriapacing.AdaptiveQuality;
import com.atomicx.euphoriapacing.EuphoriaPacingConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

/**
 * Prevents explosion, firework, and mob effect lag spikes by enforcing
 * an intake burst cap and distance-culling distant particles that waste
 * CPU physics and GPU fill rate.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Shadow @Final private Queue<Particle> particlesToAdd;

    @Inject(method = "add(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void euphoriapacing$filterParticle(Particle particle, CallbackInfo ci) {
        if (particle == null) return;

        int cap = EuphoriaPacingConfig.particleBurstCap;
        if (cap > 0 && this.particlesToAdd.size() >= cap) {
            ci.cancel();
            return;
        }

        if (EuphoriaPacingConfig.smartParticleCulling) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameRenderer != null) {
                Camera cam = mc.gameRenderer.getMainCamera();
                if (cam != null && cam.isInitialized()) {
                    Vec3 camPos = cam.position();
                    AABB bb = particle.getBoundingBox();
                    if (bb != null) {
                        double dx = bb.getCenter().x - camPos.x;
                        double dy = bb.getCenter().y - camPos.y;
                        double dz = bb.getCenter().z - camPos.z;
                        double distSq = dx * dx + dy * dy + dz * dz;

                        double maxDist = AdaptiveQuality.getParticleDistance();
                        if (distSq > maxDist * maxDist) {
                            ci.cancel();
                        }
                    }
                }
            }
        }
    }
}
