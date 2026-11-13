package com.atomicx.euphoriapacing.mixin;

import com.atomicx.euphoriapacing.EuphoriaPacingConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Nearby sections directly around the player are rebuilt synchronously for
 * instant block break/place responsiveness. Background sections exceeding the
 * frame budget are smoothly deferred to worker threads.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    private long euphoriapacing$syncStart;
    private Vec3 euphoriapacing$cameraPos;

    @Inject(method = "compileSections", at = @At("HEAD"), require = 0)
    private void euphoriapacing$beginCompile(Camera camera, CallbackInfo ci) {
        this.euphoriapacing$syncStart = System.nanoTime();
        this.euphoriapacing$cameraPos = camera != null ? camera.position() : null;
    }

    @Redirect(
        method = "compileSections",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;rebuildSectionSync(Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;Lnet/minecraft/client/renderer/chunk/RenderRegionCache;)V"
        ),
        require = 0
    )
    private void euphoriapacing$budgetedSyncRebuild(SectionRenderDispatcher dispatcher,
                                                    SectionRenderDispatcher.RenderSection section,
                                                    RenderRegionCache cache) {
        long used = System.nanoTime() - this.euphoriapacing$syncStart;
        long budget = EuphoriaPacingConfig.syncRebuildBudgetNanos;

        // Check if section is immediately adjacent to player camera (within 24 blocks)
        boolean nearPlayer = false;
        if (this.euphoriapacing$cameraPos != null) {
            BlockPos origin = section.getRenderOrigin();
            if (origin != null) {
                double dx = (origin.getX() + 8.0) - this.euphoriapacing$cameraPos.x;
                double dy = (origin.getY() + 8.0) - this.euphoriapacing$cameraPos.y;
                double dz = (origin.getZ() + 8.0) - this.euphoriapacing$cameraPos.z;
                nearPlayer = (dx * dx + dy * dy + dz * dz) < 576.0; // 24^2
            }
        }

        if (budget <= 0L || nearPlayer || used < budget) {
            dispatcher.rebuildSectionSync(section, cache);
        } else {
            section.rebuildSectionAsync(cache);
        }
    }
}
