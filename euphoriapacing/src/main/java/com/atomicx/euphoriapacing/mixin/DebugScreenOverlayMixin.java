package com.atomicx.euphoriapacing.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Injects vibrant green branding into the F3 debug screen.
 */
@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
    @Inject(method = "renderLines", at = @At("HEAD"), require = 0)
    private void euphoriapacing$addDebugLine(GuiGraphics guiGraphics, List<String> lines, boolean rightAligned, CallbackInfo ci) {
        if (!rightAligned && lines != null) {
            String text = ChatFormatting.GREEN + "[Euphoria] Pixel AI: Active" + ChatFormatting.RESET;
            if (!lines.contains(text)) {
                int insertIndex = Math.min(1, lines.size());
                lines.add(insertIndex, text);
            }
        }
    }
}
