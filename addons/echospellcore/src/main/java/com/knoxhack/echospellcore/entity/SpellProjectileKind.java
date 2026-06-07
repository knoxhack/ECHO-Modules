package com.knoxhack.echospellcore.entity;

import com.knoxhack.echospellcore.EchoSpellCore;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public enum SpellProjectileKind {
    AETHER_BOLT(5.0F, 1.05D, 1.35F, ParticleTypes.END_ROD, ParticleTypes.ENCHANT,
            0xFFB68CFF, 0xAA46E7FF, 0xFFFFFFFF, 0.22F, 1.42F, 4, 9.0F,
            "aether_bolt", 1.95F, 0.62F, ProjectileModelProfile.ORB, 2),
    DUST_LANCE(4.0F, 1.18D, 0.85F, ParticleTypes.ASH, ParticleTypes.CAMPFIRE_COSY_SMOKE,
            0xFFFFA66A, 0xAA6E4F42, 0xFFFFD1A8, 0.18F, 1.12F, 2, 13.0F,
            "dust_lance", 2.25F, 0.48F, ProjectileModelProfile.LANCE, 4),
    NULL_BOLT(4.5F, 1.0D, 0.65F, ParticleTypes.REVERSE_PORTAL, ParticleTypes.WITCH,
            0xFF8B6DFF, 0xAA2A163F, 0xFFC8B6FF, 0.23F, 1.66F, 5, -7.5F,
            "null_bolt", 2.05F, 0.58F, ProjectileModelProfile.VOID_CORE, 3),
    STORM_LANCE(5.5F, 1.28D, 1.7F, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.END_ROD,
            0xFF6AE6FF, 0xAAD5FBFF, 0xFFFFFFFF, 0.20F, 1.9F, 6, 18.0F,
            "storm_lance", 2.42F, 0.52F, ProjectileModelProfile.STORM_RAIL, 5),
    FRACTURE_SHEAR(6.0F, 1.12D, 0.55F, ParticleTypes.REVERSE_PORTAL, ParticleTypes.ENCHANT,
            0xFFFF6AF3, 0xAAFFB6F6, 0xFFFFE4FD, 0.24F, 2.05F, 7, -15.0F,
            "fracture_shear", 2.55F, 0.64F, ProjectileModelProfile.SHEAR, 6);

    public enum ProjectileModelProfile {
        ORB,
        LANCE,
        VOID_CORE,
        STORM_RAIL,
        SHEAR
    }

    private final float baseDamage;
    private final double velocity;
    private final float pitch;
    private final ParticleOptions trailParticle;
    private final ParticleOptions accentParticle;
    private final int coreColor;
    private final int glowColor;
    private final int flareColor;
    private final float renderScale;
    private final float haloScale;
    private final int shardCount;
    private final float twistDegreesPerTick;
    private final Identifier spriteTexture;
    private final Identifier modelAsset;
    private final float spriteScale;
    private final float geometryBlend;
    private final ProjectileModelProfile modelProfile;
    private final int trailSegments;

    SpellProjectileKind(float baseDamage, double velocity, float pitch, ParticleOptions trailParticle,
            ParticleOptions accentParticle, int coreColor, int glowColor, int flareColor, float renderScale,
            float haloScale, int shardCount, float twistDegreesPerTick, String spriteName, float spriteScale,
            float geometryBlend, ProjectileModelProfile modelProfile, int trailSegments) {
        this.baseDamage = baseDamage;
        this.velocity = velocity;
        this.pitch = pitch;
        this.trailParticle = trailParticle;
        this.accentParticle = accentParticle;
        this.coreColor = coreColor;
        this.glowColor = glowColor;
        this.flareColor = flareColor;
        this.renderScale = renderScale;
        this.haloScale = haloScale;
        this.shardCount = shardCount;
        this.twistDegreesPerTick = twistDegreesPerTick;
        this.spriteTexture = EchoSpellCore.id("textures/entity/projectile/" + spriteName + ".png");
        this.modelAsset = EchoSpellCore.id("models/entity/projectile/" + spriteName + ".json");
        this.spriteScale = spriteScale;
        this.geometryBlend = geometryBlend;
        this.modelProfile = modelProfile;
        this.trailSegments = trailSegments;
    }

    public float baseDamage() {
        return baseDamage;
    }

    public double velocity() {
        return velocity;
    }

    public float pitch() {
        return pitch;
    }

    public ParticleOptions trailParticle() {
        return trailParticle;
    }

    public ParticleOptions accentParticle() {
        return accentParticle;
    }

    public int coreColor() {
        return coreColor;
    }

    public int glowColor() {
        return glowColor;
    }

    public int flareColor() {
        return flareColor;
    }

    public float renderScale() {
        return renderScale;
    }

    public float haloScale() {
        return haloScale;
    }

    public int shardCount() {
        return shardCount;
    }

    public float twistDegreesPerTick() {
        return twistDegreesPerTick;
    }

    public Identifier spriteTexture() {
        return spriteTexture;
    }

    public Identifier modelAsset() {
        return modelAsset;
    }

    public float spriteScale() {
        return spriteScale;
    }

    public float geometryBlend() {
        return geometryBlend;
    }

    public ProjectileModelProfile modelProfile() {
        return modelProfile;
    }

    public int trailSegments() {
        return trailSegments;
    }

    public void applyHitEffects(LivingEntity target) {
        switch (this) {
            case AETHER_BOLT -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, true));
            case DUST_LANCE -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0, false, true));
            }
            case NULL_BOLT -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 80, 0, false, true));
            }
            case STORM_LANCE -> {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 90, 0, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, 1, false, true));
            }
            case FRACTURE_SHEAR -> {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 110, 1, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 70, 0, false, true));
            }
        }
    }
}
