package com.atomicx.euphoriapacing;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/** Euphoria Pacing by AtomicX - client-side dynamic performance governor and frame-pacing mod. */
@Mod(EuphoriaPacing.MODID)
public final class EuphoriaPacing {
    public static final String MODID = "euphoriapacing";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EuphoriaPacing(FMLJavaModLoadingContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            var modBusGroup = context.getModBusGroup();
            context.registerConfig(ModConfig.Type.CLIENT, EuphoriaPacingConfig.SPEC);
            FMLClientSetupEvent.getBus(modBusGroup).addListener(this::clientSetup);
            LOGGER.info("Euphoria Pacing by AtomicX loaded.");
        }
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        AdaptiveQuality.start();
    }
}
