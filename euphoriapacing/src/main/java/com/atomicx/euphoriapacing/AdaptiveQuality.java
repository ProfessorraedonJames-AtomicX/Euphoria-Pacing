package com.atomicx.euphoriapacing;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Intelligent Adaptive Quality Governor for Euphoria Pacing.
 * Samples FPS and frame pacing once per second. If sustained framerates drop
 * below the target, it progressively reduces non-critical visuals (entity distance,
 * block entities, particles, clouds, simulation distance, and render distance).
 * Once performance recovers, settings are smoothly restored to the player's baseline.
 *
 * Designed with strict hysteresis and VSync/refresh-rate awareness to prevent
 * degradation loops and frame freezes. Automatically coordinates with Euphoria Pacing Bridge.
 */
public final class AdaptiveQuality {
    private static final int STEP_DOWN_CHECKS = 3;
    private static final int STEP_DOWN_CHECKS_RD = 8;
    private static final int STEP_UP_CHECKS = 6;
    private static final int STEP_UP_CHECKS_RD = 18;

    private static boolean started;
    private static int level = 0;
    private static int lowStreak = 0;
    private static int highStreak = 0;
    private static int grace = 0;
    private static boolean inWorld = false;
    private static boolean framerateUnlocked = false;
    private static float smoothedFps = 60.0f;
    private static long lastRenderDistanceChangeTime = 0L;

    // Snapshot of the player's baseline settings
    private static boolean captured = false;
    private static double baseEntityScale = 1.0;
    private static ParticleStatus baseParticles = ParticleStatus.ALL;
    private static CloudStatus baseClouds = CloudStatus.FANCY;
    private static boolean baseShadows = true;
    private static int baseWeather = 7;
    private static int baseRenderDistance = 12;
    private static int baseSimDistance = 10;
    private static int baseCloudRange = 64;

    private AdaptiveQuality() {}

    public static synchronized void start() {
        if (started) return;
        started = true;
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "EuphoriaPacing-Adaptive-Timer");
            t.setDaemon(true);
            return t;
        });
        timer.scheduleAtFixedRate(
            () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null) {
                    mc.execute(AdaptiveQuality::check);
                }
            },
            4, 1, TimeUnit.SECONDS
        );
    }

    public static int getLevel() {
        return level;
    }

    public static double getBlockEntityScale() {
        if (!EuphoriaPacingConfig.blockEntityCulling || level == 0) return 1.0;
        return switch (level) {
            case 1 -> 0.80;
            case 2 -> 0.65;
            default -> 0.50;
        };
    }

    public static double getParticleDistance() {
        if (level == 0) return 64.0;
        return switch (level) {
            case 1 -> 48.0;
            case 2 -> 36.0;
            default -> 28.0;
        };
    }

    private static int maxLevel() {
        return EuphoriaPacingConfig.adaptiveRenderDistance ? 6 : 3;
    }

    // Runs on the main Minecraft client thread
    private static void check() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.options == null) return;

            if (!framerateUnlocked && EuphoriaPacingConfig.unlockFramerate) {
                unlockFramerate(mc.options);
                framerateUnlocked = true;
            }

            if (mc.level == null) {
                if (level > 0 && captured) {
                    apply(mc.options, 0);
                }
                lowStreak = 0;
                highStreak = 0;
                inWorld = false;
                return;
            }

            if (!inWorld) {
                inWorld = true;
                grace = EuphoriaPacingConfig.joinGraceSeconds;
                smoothedFps = Math.max(30.0f, mc.getFps());
            }

            if (grace > 0) {
                grace--;
                return;
            }

            if (!EuphoriaPacingConfig.adaptive || mc.isPaused()) return;

            int currentFps = mc.getFps();

            // Check optional bridge telemetry (if EPE reported micro-stutter / 1% low)
            try {
                Class<?> bridge = Class.forName("com.atomicx.euphoriapacingbridge.api.BridgeState");
                int bridgeLowFps = (int) bridge.getMethod("getOnePercentLowFps").invoke(null);
                if (bridgeLowFps > 0 && bridgeLowFps < currentFps) {
                    // Blend 1% low FPS into smoothed FPS to react to micro-stutters
                    currentFps = (int) (currentFps * 0.7 + bridgeLowFps * 0.3);
                }
            } catch (Throwable ignored) {}

            // Exponential moving average to filter out momentary 1-frame glitches
            smoothedFps = (smoothedFps * 0.65f) + (currentFps * 0.35f);

            int target = EuphoriaPacingConfig.targetFps;
            int fpsLimit = mc.options.framerateLimit().get();
            boolean vsync = mc.options.enableVsync().get();

            // Clamp target if player explicitly configured a lower framerate limit
            int effectiveTarget = target;
            if (fpsLimit < target && fpsLimit > 0) {
                effectiveTarget = fpsLimit;
            }

            // Hysteresis deadband: prevents oscillating around target
            int lowThreshold = Math.max(10, effectiveTarget - 4);
            int highThreshold = effectiveTarget;

            boolean isLow = smoothedFps < lowThreshold;
            // High is detected either when above target OR when pinned to VSync / FPS limit
            boolean isHigh = smoothedFps >= highThreshold
                || (vsync && smoothedFps >= 58.0f)
                || (fpsLimit <= target && smoothedFps >= fpsLimit - 1.5f);

            if (isLow) {
                lowStreak++;
                highStreak = Math.max(0, highStreak - 2);
            } else if (isHigh) {
                highStreak++;
                lowStreak = Math.max(0, lowStreak - 2);
            } else {
                // In neutral deadband: gently decay streaks
                lowStreak = Math.max(0, lowStreak - 1);
                highStreak = Math.max(0, highStreak - 1);
            }

            int currentMax = maxLevel();
            if (level > currentMax) {
                apply(mc.options, currentMax);
                return;
            }

            int downNeeded = level >= 3 ? STEP_DOWN_CHECKS_RD : STEP_DOWN_CHECKS;
            int upNeeded = level > 3 ? STEP_UP_CHECKS_RD : STEP_UP_CHECKS;

            // Anti-freeze check: enforce cooldown for render distance modifications (levels 4-6)
            long now = System.currentTimeMillis();
            boolean rdCooldownReady = (now - lastRenderDistanceChangeTime) > 15_000L;

            if (lowStreak >= downNeeded && level < currentMax) {
                if (level >= 3 && !rdCooldownReady) {
                    return;
                }
                apply(mc.options, level + 1);
                lowStreak = 0;
                if (level >= 4) {
                    lastRenderDistanceChangeTime = now;
                }
            } else if (highStreak >= upNeeded && level > 0) {
                if (level > 3 && !rdCooldownReady) {
                    return;
                }
                apply(mc.options, level - 1);
                highStreak = 0;
                if (level >= 3) {
                    lastRenderDistanceChangeTime = now;
                }
            }
        } catch (Throwable t) {
            EuphoriaPacing.LOGGER.error("Euphoria Pacing adaptive check failed", t);
        }
    }

    private static void unlockFramerate(Options o) {
        o.enableVsync().set(false);
        o.framerateLimit().set(Options.UNLIMITED_FRAMERATE_CUTOFF);
        EuphoriaPacing.LOGGER.info("Euphoria Pacing: VSync off, framerate limit set to Max (unlimited)");
    }

    private static void capture(Options o) {
        baseEntityScale = o.entityDistanceScaling().get();
        baseParticles = o.particles().get();
        baseClouds = o.cloudStatus().get();
        baseShadows = o.entityShadows().get();
        baseWeather = o.weatherRadius().get();
        baseRenderDistance = o.renderDistance().get();
        baseSimDistance = o.simulationDistance().get();
        baseCloudRange = o.cloudRange().get();
        captured = true;
    }

    private static void apply(Options o, int newLevel) {
        if (!captured) capture(o);
        level = newLevel;

        // Notify bridge of updated governor level
        boolean hasBridgeEngine = false;
        try {
            Class<?> bridge = Class.forName("com.atomicx.euphoriapacingbridge.api.BridgeState");
            bridge.getMethod("setGovernorLevel", int.class).invoke(null, newLevel);
            hasBridgeEngine = (boolean) bridge.getMethod("isExtendedLoaded").invoke(null);
        } catch (Throwable ignored) {}

        boolean visuals = EuphoriaPacingConfig.adaptiveVisuals;
        double minScale = Math.min(baseEntityScale, EuphoriaPacingConfig.minEntityScale);

        double entityScale = baseEntityScale;
        ParticleStatus particles = baseParticles;
        CloudStatus clouds = baseClouds;
        boolean shadows = baseShadows;
        int weather = baseWeather;
        int renderDistance = baseRenderDistance;
        int simDistance = baseSimDistance;
        int cloudRange = baseCloudRange;
        boolean sim = EuphoriaPacingConfig.adaptiveSimulationDistance;

        if (level >= 1) {
            if (visuals) cloudRange = Math.min(cloudRange, 48);
            entityScale = Math.max(minScale, baseEntityScale * 0.85);
            if (visuals) {
                shadows = false;
                if (clouds == CloudStatus.FANCY) clouds = CloudStatus.FAST;
            }
        }
        if (level >= 2) {
            entityScale = Math.max(minScale, baseEntityScale * 0.70);
            particles = worse(particles, ParticleStatus.DECREASED);
            if (visuals) {
                weather = Math.min(weather, 5);
                cloudRange = Math.min(cloudRange, 24);
            }
            if (sim) simDistance = Math.min(simDistance, 8);
        }
        if (level >= 3) {
            entityScale = minScale;
            particles = ParticleStatus.MINIMAL;
            if (visuals) clouds = CloudStatus.OFF;
            if (visuals) weather = Math.min(weather, 3);
            if (sim) simDistance = Math.min(simDistance, 6);
        }
        if (level >= 4) {
            int cut = (level - 3) * 2; // 2, 4, 6 chunks
            int floor = Math.min(baseRenderDistance, EuphoriaPacingConfig.minRenderDistance);
            renderDistance = Math.max(floor, baseRenderDistance - cut);
        }

        // Quantize entity scale to 0.05 steps
        entityScale = Math.min(5.0, Math.max(0.5, Math.round(entityScale * 20.0) / 20.0));

        o.entityDistanceScaling().set(entityScale);
        o.particles().set(particles);
        o.cloudStatus().set(clouds);
        o.entityShadows().set(shadows);
        o.weatherRadius().set(Math.max(3, weather));

        // If the Bridge Engine is active, it handles distance culling at the draw-list level without allChanged() reload spikes
        if (!hasBridgeEngine && o.renderDistance().get() != renderDistance) {
            o.renderDistance().set(renderDistance);
        }

        if (o.simulationDistance().get() != simDistance) {
            o.simulationDistance().set(Math.max(5, simDistance));
        }
        o.cloudRange().set(Math.max(2, cloudRange));

        if (level == 0) captured = false; // Player settings restored
        EuphoriaPacing.LOGGER.info("Euphoria Pacing adaptive quality level -> {} (smoothed FPS: {})", level, (int) smoothedFps);
    }

    private static ParticleStatus worse(ParticleStatus a, ParticleStatus b) {
        return a.ordinal() > b.ordinal() ? a : b;
    }
}
