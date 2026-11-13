package com.atomicx.euphoriapacing.mixin;

import com.atomicx.euphoriapacing.EuphoriaPacingConfig;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

/**
 * Spreads chunk mesh GPU uploads over frames with an adaptive time budget
 * to eliminate frame-time spikes without starving chunk loading.
 */
@Mixin(SectionRenderDispatcher.class)
public abstract class SectionRenderDispatcherMixin {
    @Shadow @Final private Queue<Runnable> toUpload;
    @Shadow @Final Queue<SectionMesh> toClose;
    @Shadow volatile boolean closed;

    @Inject(method = "uploadAllPendingUploads", at = @At("HEAD"), cancellable = true, require = 0)
    private void euphoriapacing$budgetedUploads(CallbackInfo ci) {
        if (this.closed || !EuphoriaPacingConfig.budgetedUploads || EuphoriaPacingConfig.uploadBudgetNanos <= 0L) return;

        int backlog = this.toUpload.size();
        long budget = EuphoriaPacingConfig.uploadBudgetNanos;
        int minUploads = 1;

        // Dynamic budget elasticity: prevent starvation when player moves fast or teleports
        if (backlog > 40) {
            budget = Math.min(6_000_000L, budget * 3L); // up to 6ms
            minUploads = Math.min(backlog, 6);
        } else if (backlog > 15) {
            budget = Math.min(3_500_000L, (long) (budget * 1.75)); // up to 3.5ms
            minUploads = Math.min(backlog, 3);
        }

        long deadline = System.nanoTime() + budget;
        int executed = 0;
        Runnable task;
        while ((task = this.toUpload.poll()) != null) {
            task.run();
            executed++;
            if (executed >= minUploads && System.nanoTime() >= deadline) {
                break;
            }
        }

        SectionMesh mesh;
        while ((mesh = this.toClose.poll()) != null) {
            mesh.close();
        }
        ci.cancel();
    }
}
