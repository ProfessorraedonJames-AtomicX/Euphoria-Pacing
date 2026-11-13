package com.atomicx.euphoriapacing.mixin;

import com.atomicx.euphoriapacing.AdaptiveQuality;
import com.atomicx.euphoriapacing.EuphoriaPacingConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adaptively culls distant block entities (chests, signs, banners, skulls)
 * to prevent massive FPS drops in large bases and storage facilities.
 *
 * Camera is queried safely via Minecraft.getInstance().gameRenderer.getMainCamera()
 * eliminating the broken @Shadow field camera removed in Minecraft 1.21.9+.
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {

    @Inject(
        method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private <E extends BlockEntity> void euphoriapacing$cullBlockEntity(E blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (!EuphoriaPacingConfig.blockEntityCulling) return;

        double scale = AdaptiveQuality.getBlockEntityScale();
        if (scale >= 1.0) return; // Full quality, let vanilla handle default view distance

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null) return;
        Camera camera = mc.gameRenderer.getMainCamera();
        if (camera == null || !camera.isInitialized()) return;

        BlockPos pos = blockEntity.getBlockPos();
        Vec3 camPos = camera.position();
        double dx = (pos.getX() + 0.5) - camPos.x;
        double dy = (pos.getY() + 0.5) - camPos.y;
        double dz = (pos.getZ() + 0.5) - camPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;

        double maxDist = 64.0 * scale;
        if (distSq > maxDist * maxDist) {
            ci.cancel();
        }
    }
}
