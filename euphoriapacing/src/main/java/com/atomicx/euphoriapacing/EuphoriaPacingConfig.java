package com.atomicx.euphoriapacing;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = EuphoriaPacing.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EuphoriaPacingConfig {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue ADAPTIVE =
        B.comment("Automatically lower entity distance, particles, and graphics when FPS drops")
         .define("adaptiveQuality", true);
    private static final ForgeConfigSpec.IntValue TARGET_FPS =
        B.comment("FPS Euphoria Pacing tries to maintain")
         .defineInRange("targetFps", 60, 1, 5000);
    private static final ForgeConfigSpec.DoubleValue MIN_ENTITY_SCALE =
        B.comment("Lowest entity distance scaling the governor may use (0.5 is game minimum)")
         .defineInRange("minEntityDistanceScale", 0.5, 0.5, 5.0);

    private static final ForgeConfigSpec.BooleanValue BUDGET_UPLOADS =
        B.comment("Spread chunk mesh uploads over several frames with dynamic elasticity (eliminates stutter)")
         .define("budgetedUploads", true);
    private static final ForgeConfigSpec.DoubleValue UPLOAD_BUDGET_MS =
        B.comment("Base milliseconds per frame spent uploading chunk meshes (0 = no limit / vanilla)")
         .defineInRange("uploadBudgetMs", 2.0, 0.0, 1000.0);
    private static final ForgeConfigSpec.DoubleValue SYNC_REBUILD_BUDGET_MS =
        B.comment("Max milliseconds per frame spent rebuilding chunk sections on the main thread; remainder go to worker threads. (0 = no limit)")
         .defineInRange("syncRebuildBudgetMs", 3.0, 0.0, 1000.0);

    private static final ForgeConfigSpec.BooleanValue ADAPTIVE_VISUALS =
        B.comment("Governor may adjust clouds, entity shadows, and weather radius when FPS is low")
         .define("adaptiveVisuals", true);
    private static final ForgeConfigSpec.BooleanValue ADAPTIVE_SIM_DISTANCE =
        B.comment("Governor may lower simulation distance in singleplayer (reduces internal server CPU load)")
         .define("adaptiveSimulationDistance", true);
    private static final ForgeConfigSpec.BooleanValue ADAPTIVE_RENDER_DISTANCE =
        B.comment("Governor may lower render distance as a last resort under sustained heavy load")
         .define("adaptiveRenderDistance", true);
    private static final ForgeConfigSpec.IntValue MIN_RENDER_DISTANCE =
        B.comment("Render distance (chunks) the governor will never go below")
         .defineInRange("minRenderDistance", 8, 2, 32);

    private static final ForgeConfigSpec.IntValue PARTICLE_BURST_CAP =
        B.comment("Max particles accepted per tick (stops explosion and firework lag spikes). 0 = no cap")
         .defineInRange("particleBurstCap", 1500, 0, Integer.MAX_VALUE);
    private static final ForgeConfigSpec.BooleanValue SMART_PARTICLE_CULLING =
        B.comment("Culls distant and non-visible particles to save CPU tick time and GPU fillrate")
         .define("smartParticleCulling", true);

    private static final ForgeConfigSpec.BooleanValue BLOCK_ENTITY_CULLING =
        B.comment("Adaptively culls distant tile entities (chests, signs, banners) in large storage rooms and mob farms")
         .define("blockEntityCulling", true);

    private static final ForgeConfigSpec.IntValue JOIN_GRACE_SECONDS =
        B.comment("Seconds to wait after joining a world before the governor starts evaluating FPS")
         .defineInRange("joinGraceSeconds", 10, 0, 3600);

    private static final ForgeConfigSpec.BooleanValue UNLOCK_FRAMERATE =
        B.comment("Optional benchmark helper: turns VSync off and sets framerate limit to Max")
         .define("unlockFramerate", false);

    static final ForgeConfigSpec SPEC = B.build();

    public static volatile boolean adaptive = true;
    public static volatile int targetFps = 60;
    public static volatile double minEntityScale = 0.5;
    public static volatile boolean unlockFramerate = false;
    public static volatile int joinGraceSeconds = 10;
    public static volatile boolean adaptiveVisuals = true;
    public static volatile boolean adaptiveSimulationDistance = true;
    public static volatile boolean adaptiveRenderDistance = true;
    public static volatile int minRenderDistance = 8;
    public static volatile int particleBurstCap = 1500;
    public static volatile boolean smartParticleCulling = true;
    public static volatile boolean blockEntityCulling = true;
    public static volatile boolean budgetedUploads = true;
    public static volatile long uploadBudgetNanos = 2_000_000L;
    public static volatile long syncRebuildBudgetNanos = 3_000_000L;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        try {
            adaptive = ADAPTIVE.get();
            targetFps = TARGET_FPS.get();
            minEntityScale = MIN_ENTITY_SCALE.get();
            unlockFramerate = UNLOCK_FRAMERATE.get();
            joinGraceSeconds = JOIN_GRACE_SECONDS.get();
            adaptiveVisuals = ADAPTIVE_VISUALS.get();
            adaptiveSimulationDistance = ADAPTIVE_SIM_DISTANCE.get();
            adaptiveRenderDistance = ADAPTIVE_RENDER_DISTANCE.get();
            minRenderDistance = MIN_RENDER_DISTANCE.get();
            particleBurstCap = PARTICLE_BURST_CAP.get();
            smartParticleCulling = SMART_PARTICLE_CULLING.get();
            blockEntityCulling = BLOCK_ENTITY_CULLING.get();
            budgetedUploads = BUDGET_UPLOADS.get();
            uploadBudgetNanos = (long) (UPLOAD_BUDGET_MS.get() * 1_000_000.0);
            syncRebuildBudgetNanos = (long) (SYNC_REBUILD_BUDGET_MS.get() * 1_000_000.0);
        } catch (IllegalStateException ignored) {
            // not loaded yet
        }
    }

    public static void setTargetFps(int value) {
        targetFps = value;
        try {
            TARGET_FPS.set(value);
            TARGET_FPS.save();
        } catch (RuntimeException e) {
            EuphoriaPacing.LOGGER.warn("Euphoria Pacing: could not save targetFps to config", e);
        }
    }

    public static void setAdaptive(boolean value) {
        adaptive = value;
        try {
            ADAPTIVE.set(value);
            ADAPTIVE.save();
        } catch (RuntimeException e) {
            EuphoriaPacing.LOGGER.warn("Euphoria Pacing: could not save adaptiveQuality to config", e);
        }
    }

    public static void setBlockEntityCulling(boolean value) {
        blockEntityCulling = value;
        try {
            BLOCK_ENTITY_CULLING.set(value);
            BLOCK_ENTITY_CULLING.save();
        } catch (RuntimeException e) {
            EuphoriaPacing.LOGGER.warn("Euphoria Pacing: could not save blockEntityCulling to config", e);
        }
    }

    public static void setBudgetedUploads(boolean value) {
        budgetedUploads = value;
        try {
            BUDGET_UPLOADS.set(value);
            BUDGET_UPLOADS.save();
        } catch (RuntimeException e) {
            EuphoriaPacing.LOGGER.warn("Euphoria Pacing: could not save budgetedUploads to config", e);
        }
    }
}
