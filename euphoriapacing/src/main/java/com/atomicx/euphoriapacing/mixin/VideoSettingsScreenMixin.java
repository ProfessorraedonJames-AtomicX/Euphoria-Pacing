package com.atomicx.euphoriapacing.mixin;

import com.atomicx.euphoriapacing.EuphoriaPacingOptions;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the Euphoria Pacing Performance section to Video Settings. */
@Mixin(VideoSettingsScreen.class)
public abstract class VideoSettingsScreenMixin extends OptionsSubScreen {
    protected VideoSettingsScreenMixin(Screen lastScreen, Options options, Component title) {
        super(lastScreen, options, title);
    }

    @Inject(method = "addOptions", at = @At("TAIL"), require = 0)
    private void euphoriapacing$addEuphoriaOptions(CallbackInfo ci) {
        if (this.list == null) return;
        this.list.addHeader(Component.translatable("options.euphoriapacing.header"));
        this.list.addSmall(new OptionInstance<?>[] {
            EuphoriaPacingOptions.targetFps(),
            EuphoriaPacingOptions.autoQuality()
        });
        this.list.addSmall(new OptionInstance<?>[] {
            EuphoriaPacingOptions.blockEntityCulling(),
            EuphoriaPacingOptions.budgetedUploads()
        });
    }
}
