package com.knoxhack.echothemecore.api;

import com.knoxhack.echothemecore.config.ThemeCoreConfig;
import com.knoxhack.echothemecore.content.ThemeRegistry;

public record ThemeVisualSettings(
    EchoThemeColors colors,
    int hologramColor,
    int edgeGlowColor,
    int particleColor,
    float glowIntensity,
    float emissiveIntensity,
    float hologramOpacity,
    float glassOpacity,
    float noiseStrength,
    float distortionStrength,
    float edgeGlowStrength,
    float particleIntensity,
    float animationIntensity,
    float pulseSpeed,
    boolean glowEnabled,
    boolean noiseEnabled,
    boolean distortionEnabled,
    boolean particlesEnabled,
    boolean edgeGlowEnabled
) {
    public static ThemeVisualSettings resolve(EchoTheme theme) {
        EchoThemeRenderProfile render = theme.renderProfile();
        float debugScale = ThemeRegistry.debugVisualIntensity();
        boolean reduceGlow = ThemeCoreConfig.reduceGlow();
        boolean disableDistortion = ThemeCoreConfig.disableDistortion();
        boolean disableNoise = ThemeCoreConfig.disableNoise();
        boolean particlesEnabled = ThemeCoreConfig.enableParticleGlints();
        boolean edgeGlowEnabled = ThemeCoreConfig.enableEdgeGlow();

        float glow = scale(render.glowIntensity(), debugScale);
        float emissive = scale(render.emissiveIntensity(), debugScale);
        float particle = scale(render.particleIntensity(), debugScale);
        float edge = scale(render.edgeGlowStrength(), debugScale);
        float animation = scale(render.animationIntensity(), debugScale);

        if (reduceGlow) {
            glow *= 0.45F;
            emissive *= 0.55F;
            particle *= 0.55F;
            edge *= 0.5F;
        }

        float noise = disableNoise ? 0.0F : render.noiseStrength();
        float distortion = disableDistortion ? 0.0F : render.distortionStrength();

        if (ThemeCoreConfig.forceHighContrast()) {
            return new ThemeVisualSettings(
                highContrast(theme.colors()),
                render.hologramColor(),
                render.edgeGlowColor(),
                render.particlePrimary(),
                clamp(glow),
                clamp(emissive),
                clamp(Math.max(0.75F, render.hologramOpacity())),
                clamp(Math.max(0.72F, render.glassOpacity())),
                noise,
                distortion,
                clamp(edge),
                clamp(particle),
                clamp(animation),
                render.pulseSpeed(),
                glow > 0.01F,
                noise > 0.0F,
                distortion > 0.0F,
                particlesEnabled && particle > 0.01F,
                edgeGlowEnabled && edge > 0.01F
            );
        }

        return new ThemeVisualSettings(
            theme.colors(),
            render.hologramColor(),
            render.edgeGlowColor(),
            render.particlePrimary(),
            clamp(glow),
            clamp(emissive),
            clamp(render.hologramOpacity()),
            clamp(render.glassOpacity()),
            noise,
            distortion,
            clamp(edge),
            clamp(particle),
            clamp(animation),
            render.pulseSpeed(),
            glow > 0.01F,
            noise > 0.0F,
            distortion > 0.0F,
            particlesEnabled && particle > 0.01F,
            edgeGlowEnabled && edge > 0.01F
        );
    }

    private static float scale(float value, float debugScale) {
        return value * Math.max(0.0F, debugScale);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(2.0F, value));
    }

    private static EchoThemeColors highContrast(EchoThemeColors colors) {
        return new EchoThemeColors(
            colors.primary(),
            colors.secondary(),
            colors.accent(),
            0xFF000000,
            0xE5000000,
            0xE50A0E18,
            0xD5001118,
            colors.border(),
            colors.borderSoft(),
            0xFFFFFFFF,
            0xFFC9E8F2,
            colors.success(),
            colors.warning(),
            colors.error(),
            colors.locked(),
            colors.glow(),
            colors.selection()
        );
    }
}
