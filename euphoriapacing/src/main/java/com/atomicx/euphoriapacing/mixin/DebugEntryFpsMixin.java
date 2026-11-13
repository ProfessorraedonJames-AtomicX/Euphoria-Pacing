package com.atomicx.euphoriapacing.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.debug.DebugEntryFps;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects vibrant green branding into Minecraft 1.21.9+ / 1.21.11 / 26.x modular F3 debug screen.
 */
@Mixin(DebugEntryFps.class)
public abstract class DebugEntryFpsMixin {
    @Inject(method = "display", at = @At("TAIL"), require = 0)
    private void euphoriapacing$addDebugFpsLine(DebugScreenDisplayer displayer, Level level, LevelChunk clientChunk, LevelChunk serverChunk, CallbackInfo ci) {
        if (displayer != null) {
            displayer.addPriorityLine(ChatFormatting.GREEN + "[Euphoria] Pixel AI: Active" + ChatFormatting.RESET);
        }
    }
}
