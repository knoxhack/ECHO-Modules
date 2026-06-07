package com.knoxhack.echothemecore.api;

public record EchoThemeRenderProfile(
    int hologramColor,
    int hologramSecondary,
    int particlePrimary,
    int particleSecondary,
    int emissivePrimary,
    int emissiveSecondary,
    int edgeGlowColor,
    int warningGlowColor,
    int successGlowColor,
    int errorGlowColor,
    int distortionColor,
    float glowIntensity,
    float emissiveIntensity,
    float hologramOpacity,
    float glassOpacity,
    float noiseStrength,
    float distortionStrength,
    float edgeGlowStrength,
    float hologramPulseStrength,
    float energyPatternStrength,
    float particleIntensity,
    float animationIntensity,
    float pulseSpeed,
    HologramStyle hologramStyle,
    ParticleStyle particleStyle,
    DistortionStyle distortionStyle,
    String overlayStyle,
    TransitionStyle transitionStyle
) {
    public int color(EchoThemeRenderColorKey key) {
        return switch (key) {
            case HOLOGRAM -> hologramColor;
            case HOLOGRAM_SECONDARY -> hologramSecondary;
            case PARTICLE_PRIMARY -> particlePrimary;
            case PARTICLE_SECONDARY -> particleSecondary;
            case EMISSIVE_PRIMARY -> emissivePrimary;
            case EMISSIVE_SECONDARY -> emissiveSecondary;
            case EDGE_GLOW -> edgeGlowColor;
            case WARNING_GLOW -> warningGlowColor;
            case SUCCESS_GLOW -> successGlowColor;
            case ERROR_GLOW -> errorGlowColor;
            case DISTORTION -> distortionColor;
        };
    }

    public float intensity(EchoThemeRenderIntensityKey key) {
        return switch (key) {
            case GLOW -> glowIntensity;
            case EMISSIVE -> emissiveIntensity;
            case HOLOGRAM_OPACITY -> hologramOpacity;
            case GLASS_OPACITY -> glassOpacity;
            case NOISE -> noiseStrength;
            case DISTORTION -> distortionStrength;
            case EDGE_GLOW -> edgeGlowStrength;
            case HOLOGRAM_PULSE -> hologramPulseStrength;
            case ENERGY_PATTERN -> energyPatternStrength;
            case PARTICLE -> particleIntensity;
            case ANIMATION -> animationIntensity;
            case PULSE_SPEED -> pulseSpeed;
        };
    }
}
