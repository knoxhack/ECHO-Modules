package com.knoxhack.echoashfallprotocol.client;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.client.hud.HudState;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventProfile;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventProfiles;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;

/**
 * Client-only environmental presentation driven by the server environmental sync packet.
 */
public final class EnvironmentalVisualController {
    private static float easedIntensity = 0.0F;
    private static long localTicks = 0L;
    private static int orbitalTicksRemaining = 0;
    private static int orbitalOverlayColor = 0x553066FF;
    private static int orbitalParticleColor = 0xFFE09CFF;
    private static float orbitalIntensity = 0.0F;
    private static long orbitalSeed = 0L;

    private EnvironmentalVisualController() {
    }

    public static void tick() {
        localTicks++;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            easedIntensity = approach(easedIntensity, 0.0F, 0.05F);
            return;
        }

        EnvironmentalEventType type = currentEventType();
        EnvironmentalEventProfile profile = EnvironmentalEventProfiles.get(type);
        if (orbitalTicksRemaining > 0) {
            orbitalTicksRemaining--;
        }
        float exposureScale = weatherExposureScale(minecraft, minecraft.player, type);
        float target = profile == null
                ? (orbitalTicksRemaining > 0 ? orbitalIntensity : 0.0F)
                : Math.max(0.0F, HudState.getEnvEventIntensity()) * exposureScale;
        easedIntensity = approach(easedIntensity, target, 0.035F);
        if (profile != null && exposureScale > 0.0F && easedIntensity > 0.05F) {
            spawnWeatherParticles(minecraft, minecraft.player, profile);
        } else if (orbitalTicksRemaining > 0 && easedIntensity > 0.05F) {
            spawnOrbitalParticles(minecraft, minecraft.player);
        }
    }

    public static void renderOverlay(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        EnvironmentalEventType type = currentEventType();
        EnvironmentalEventProfile profile = EnvironmentalEventProfiles.get(type);
        if (profile == null && orbitalTicksRemaining <= 0) {
            return;
        }
        if (easedIntensity <= 0.01F) {
            return;
        }
        float visualIntensity = easedIntensity * weatherExposureScale(minecraft, minecraft.player, type);
        if (visualIntensity <= 0.01F) {
            return;
        }

        float visualScale = Math.max(0.0F, Config.WEATHER_VISUAL_INTENSITY.get().floatValue());
        if (visualScale <= 0.0F) {
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int overlayColor = profile == null ? orbitalOverlayColor : profile.overlayColor();
        int alpha = Math.min(overlayAlphaCap(type), Math.round(((overlayColor >>> 24) & 0xFF) * visualIntensity * visualScale));
        if (type == EnvironmentalEventType.BLACKOUT) {
            alpha = Math.min(180, alpha + ((localTicks / 7 + HudState.getEnvEventSeed()) % 3 == 0 ? 24 : 0));
        }
        graphics.fill(0, 0, width, height, (alpha << 24) | (overlayColor & 0x00FFFFFF));
    }

    public static void triggerOrbitalPulse(int overlayColor, int particleColor, float intensity, long seed) {
        orbitalTicksRemaining = 120;
        orbitalOverlayColor = overlayColor;
        orbitalParticleColor = particleColor;
        orbitalIntensity = Math.max(0.35F, intensity);
        orbitalSeed = seed;
        easedIntensity = Math.max(easedIntensity, orbitalIntensity);
    }

    private static EnvironmentalEventType currentEventType() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return EnvironmentalEventType.NONE;
        }
        return HudState.getEnvironmentalEventStatus(minecraft.level.getGameTime()).type();
    }

    private static void spawnWeatherParticles(Minecraft minecraft, Player player, EnvironmentalEventProfile profile) {
        float density = Math.max(0.0F, Config.WEATHER_PARTICLE_DENSITY.get().floatValue());
        if (density <= 0.0F || minecraft.level == null || (localTicks & 1L) != 0L) {
            return;
        }
        int count = Math.min(particleCap(profile.type()), Math.round(profile.particleBudget() * easedIntensity * density));
        ParticleOptions particle = particleFor(profile.type());
        var random = minecraft.level.getRandom();
        for (int i = 0; i < count; i++) {
            double x = player.getX() + (random.nextDouble() - 0.5D) * 18.0D;
            double y = player.getY() + 2.5D + random.nextDouble() * 7.0D;
            double z = player.getZ() + (random.nextDouble() - 0.5D) * 18.0D;
            double vx = driftX(profile.type()) + (random.nextDouble() - 0.5D) * 0.015D;
            double vy = verticalSpeed(profile.type());
            double vz = (random.nextDouble() - 0.5D) * 0.025D;
            minecraft.level.addParticle(particle, x, y, z, vx, vy, vz);
        }
    }

    private static void spawnOrbitalParticles(Minecraft minecraft, Player player) {
        float density = Math.max(0.0F, Config.WEATHER_PARTICLE_DENSITY.get().floatValue());
        if (density <= 0.0F || minecraft.level == null || (localTicks & 1L) != 0L) {
            return;
        }
        int count = Math.min(16, Math.round(10 * easedIntensity * density));
        ParticleOptions particle = (orbitalParticleColor & 0x00FF00FF) != 0 ? ParticleTypes.GLOW : ParticleTypes.ELECTRIC_SPARK;
        var random = minecraft.level.getRandom();
        for (int i = 0; i < count; i++) {
            double phase = (orbitalSeed + localTicks + i * 17L) * 0.11D;
            double radius = 2.5D + random.nextDouble() * 8.0D;
            double x = player.getX() + Math.cos(phase) * radius;
            double y = player.getY() + 1.0D + random.nextDouble() * 4.5D;
            double z = player.getZ() + Math.sin(phase) * radius;
            minecraft.level.addParticle(particle, x, y, z, 0.0D, 0.015D, 0.0D);
        }
    }

    private static float weatherExposureScale(Minecraft minecraft, Player player, EnvironmentalEventType type) {
        if (!requiresOutdoorExposure(type)) {
            return 1.0F;
        }
        return isShelteredFromWeather(minecraft, player) ? 0.0F : 1.0F;
    }

    private static boolean requiresOutdoorExposure(EnvironmentalEventType type) {
        return switch (type) {
            case RADIATION_STORM, TOXIC_STORM, ASH_STORM, CRYO_FRONT -> true;
            default -> false;
        };
    }

    private static boolean isShelteredFromWeather(Minecraft minecraft, Player player) {
        if (minecraft.level == null) {
            return true;
        }
        BlockPos pos = player.blockPosition();
        if (pos.getY() < 50) {
            return true;
        }
        BlockPos headPos = pos.above(2);
        if (!minecraft.level.canSeeSky(headPos)) {
            return true;
        }

        int coverCount = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (!minecraft.level.isEmptyBlock(headPos.offset(dx, 0, dz))) {
                    coverCount++;
                }
            }
        }
        return coverCount >= 5;
    }

    private static ParticleOptions particleFor(EnvironmentalEventType type) {
        return switch (type) {
            case RADIATION_STORM -> ParticleTypes.GLOW;
            case TOXIC_STORM -> ParticleTypes.FALLING_WATER;
            case BLACKOUT, NEXUS_SURGE -> ParticleTypes.ELECTRIC_SPARK;
            case ASH_STORM -> ParticleTypes.ASH;
            case CRYO_FRONT -> ParticleTypes.SNOWFLAKE;
            default -> ParticleTypes.SMOKE;
        };
    }

    private static double driftX(EnvironmentalEventType type) {
        return switch (type) {
            case ASH_STORM -> 0.09D;
            case CRYO_FRONT -> 0.035D;
            default -> 0.015D;
        };
    }

    private static double verticalSpeed(EnvironmentalEventType type) {
        return switch (type) {
            case RADIATION_STORM, BLACKOUT, NEXUS_SURGE -> 0.01D;
            case ASH_STORM -> -0.045D;
            case CRYO_FRONT -> -0.035D;
            default -> -0.08D;
        };
    }

    private static int overlayAlphaCap(EnvironmentalEventType type) {
        return switch (type) {
            case BLACKOUT -> 180;
            case ASH_STORM -> 150;
            case NEXUS_SURGE -> 145;
            case CRYO_FRONT -> 125;
            case TOXIC_STORM, RADIATION_STORM -> 135;
            default -> 130;
        };
    }

    private static int particleCap(EnvironmentalEventType type) {
        return switch (type) {
            case ASH_STORM -> 32;
            case RADIATION_STORM, TOXIC_STORM -> 24;
            case CRYO_FRONT -> 20;
            case NEXUS_SURGE -> 18;
            case BLACKOUT -> 12;
            default -> 16;
        };
    }

    private static float approach(float current, float target, float step) {
        if (current < target) {
            return Math.min(target, current + step);
        }
        return Math.max(target, current - step);
    }
}
