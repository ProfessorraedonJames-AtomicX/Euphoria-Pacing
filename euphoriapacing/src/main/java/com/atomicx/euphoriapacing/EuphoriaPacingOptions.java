package com.atomicx.euphoriapacing;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;

/** Options shown in Video Settings. They read and write Euphoria Pacing's config file. */
public final class EuphoriaPacingOptions {
    private EuphoriaPacingOptions() {}

    /** Slider from 15 to 240 FPS in steps of 5. */
    public static OptionInstance<Integer> targetFps() {
        int initial = Math.min(240, Math.max(15, Math.round(EuphoriaPacingConfig.targetFps / 5.0f) * 5));
        return new OptionInstance<Integer>(
            "options.euphoriapacing.targetFps",
            OptionInstance.<Integer>cachedConstantTooltip(Component.translatable("options.euphoriapacing.targetFps.tooltip")),
            (caption, value) -> Options.genericValueLabel(caption, value),
            new OptionInstance.IntRange(3, 48).xmap(i -> i * 5, v -> v / 5, false),
            initial,
            EuphoriaPacingConfig::setTargetFps
        );
    }

    public static OptionInstance<Boolean> autoQuality() {
        return OptionInstance.createBoolean(
            "options.euphoriapacing.autoQuality",
            OptionInstance.<Boolean>cachedConstantTooltip(Component.translatable("options.euphoriapacing.autoQuality.tooltip")),
            EuphoriaPacingConfig.adaptive,
            EuphoriaPacingConfig::setAdaptive
        );
    }

    public static OptionInstance<Boolean> blockEntityCulling() {
        return OptionInstance.createBoolean(
            "options.euphoriapacing.blockEntityCulling",
            OptionInstance.<Boolean>cachedConstantTooltip(Component.translatable("options.euphoriapacing.blockEntityCulling.tooltip")),
            EuphoriaPacingConfig.blockEntityCulling,
            EuphoriaPacingConfig::setBlockEntityCulling
        );
    }

    public static OptionInstance<Boolean> budgetedUploads() {
        return OptionInstance.createBoolean(
            "options.euphoriapacing.budgetedUploads",
            OptionInstance.<Boolean>cachedConstantTooltip(Component.translatable("options.euphoriapacing.budgetedUploads.tooltip")),
            EuphoriaPacingConfig.budgetedUploads,
            EuphoriaPacingConfig::setBudgetedUploads
        );
    }
}
