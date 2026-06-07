package com.knoxhack.echoashfallprotocol.guardian;

import com.knoxhack.echoashfallprotocol.entity.boss.BiomeBossEntity;
import com.knoxhack.echoashfallprotocol.worldgen.StructureType;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public record BiomeGuardianProfile(
        String biomePath,
        String missionId,
        String bossPath,
        String title,
        Identifier ownerFaction,
        StructureType mainStructure,
        Set<StructureType> supportStructures,
        String surfaceEntrance,
        String undergroundSite,
        String lore,
        Supplier<EntityType<? extends BiomeBossEntity>> bossType,
        Supplier<EntityType<? extends Entity>> defenderType,
        int color,
        float maxHealth,
        float attackDamage,
        float armor,
        float movementSpeed,
        float pulseDamage,
        float pulseRadius,
        int pulseCooldownBase,
        int summonCooldownBase,
        int maxDefenders,
        GuardianAbility ability,
        ArenaHazard arenaHazard,
        ParticleCue particleCue,
        PulseSound pulseSound,
        boolean pulseKnockback,
        List<StatusEffect> strikeEffects,
        List<StatusEffect> pulseEffects,
        RewardBundle rewardBundle,
        VisualProfile visual,
        String mechanicHint,
        String prepHint,
        String phaseTwoLine,
        String phaseThreeLine,
        String defeatLine,
        PolishData polish
) {
    public String entityId() {
        return "echoashfallprotocol:" + bossPath;
    }

    public boolean supportsStructure(StructureType type) {
        return mainStructure == type || supportStructures.contains(type);
    }

    public CinematicCue cinematicCue() {
        return new CinematicCue(
                ability.phaseWarningLabel(),
                ability.dangerVerb(),
                arenaHazard.counterplayLabel(),
                polish == null || polish.hudObjectiveLabel().isBlank() ? title : polish.hudObjectiveLabel()
        );
    }

    public int signatureSummonBonus(int phase) {
        return switch (ability) {
            case SHIELD_PULSE, COMMAND_RALLY -> phase >= 2 ? 1 : 0;
            case TOXIC_BROOD, NEXUS_RECURSION -> 2;
            default -> 0;
        };
    }

    public float signatureDamageScale(int phase) {
        return switch (ability) {
            case SHIELD_PULSE, COMMAND_RALLY -> 0.0F;
            case SHADOW_AMBUSH -> 0.45F;
            case OVERHEAT_SLAM -> phase >= 3 ? 0.85F : 0.65F;
            case TOXIC_BROOD -> phase >= 3 ? 0.45F : 0.32F;
            case DEBRIS_ARTILLERY -> phase >= 3 ? 0.90F : 0.72F;
            case RADIATION_MELTDOWN -> phase >= 3 ? 0.75F : 0.58F;
            case CRYO_LOCK -> phase >= 3 ? 0.42F : 0.28F;
            case NEXUS_RECURSION -> phase >= 3 ? 0.72F : 0.52F;
        };
    }

    public double signatureTeleportRange() {
        return switch (ability) {
            case SHADOW_AMBUSH -> 4.5D;
            case NEXUS_RECURSION -> 5.0D;
            default -> 0.0D;
        };
    }

    public boolean signatureRetargetsDefenders() {
        return ability == GuardianAbility.COMMAND_RALLY;
    }

    public boolean signatureGrantsResistance() {
        return ability == GuardianAbility.SHIELD_PULSE || ability == GuardianAbility.COMMAND_RALLY;
    }

    public boolean signatureHeals() {
        return ability == GuardianAbility.SHIELD_PULSE;
    }

    public enum GuardianAbility {
        SHIELD_PULSE("Shield pulse", "RESCUE SHIELD SURGE", "Shield pulse"),
        COMMAND_RALLY("Command rally", "COMMAND RALLY", "Reserve pressure"),
        SHADOW_AMBUSH("Shadow ambush", "BLACKOUT AMBUSH", "Sightline break"),
        OVERHEAT_SLAM("Overheat slam", "OVERHEAT SLAM", "Armor slam"),
        TOXIC_BROOD("Toxic brood", "HIVE BLOOM", "Brood pressure"),
        DEBRIS_ARTILLERY("Debris artillery", "DEBRIS IMPACT", "Artillery lanes"),
        RADIATION_MELTDOWN("Radiation meltdown", "MELTDOWN PULSE", "Exposure spike"),
        CRYO_LOCK("Cryo lock", "CRYO LOCKDOWN", "Freeze control"),
        NEXUS_RECURSION("Nexus recursion", "NEXUS RECURSION", "Anomaly loop");

        private final String displayName;
        private final String phaseWarningLabel;
        private final String dangerVerb;

        GuardianAbility(String displayName, String phaseWarningLabel, String dangerVerb) {
            this.displayName = displayName;
            this.phaseWarningLabel = phaseWarningLabel;
            this.dangerVerb = dangerVerb;
        }

        public String displayName() {
            return displayName;
        }

        public String phaseWarningLabel() {
            return phaseWarningLabel;
        }

        public String dangerVerb() {
            return dangerVerb;
        }
    }

    public enum ArenaHazard {
        RECOVERY_BEACON("Recovery beacon", "Use the lit recovery pockets between shield pulses.", "Recover at beacons"),
        COMMAND_BUNKER("Command bunker", "Clear reserve lanes before the command rally lands.", "Clear reserve lanes"),
        SHADOW_CORRIDORS("Shadow corridors", "Light and sightlines matter more than raw armor.", "Hold lit sightlines"),
        HEAT_VENTS("Heat vents", "Fight around vent windows, not inside machinery lanes.", "Bait vent windows"),
        HIVE_PODS("Hive pods", "Poison pods make staying still expensive.", "Clear hive pods"),
        DEBRIS_FIELD("Debris field", "Watch marked wreckage lanes before shockwaves land.", "Watch shock lanes"),
        REACTOR_HOT_ZONE("Reactor hot zone", "Treat exposure after each pulse and use scrubber pockets.", "Use scrubber pockets"),
        CRYO_VENTS("Cryo vents", "Keep warm supplies ready and move before freeze stacks.", "Use warm pockets"),
        ANOMALY_RIFTS("Anomaly rifts", "Expect teleports, clones, and weakness loops.", "Break anomaly loops");

        private final String displayName;
        private final String hint;
        private final String counterplayLabel;

        ArenaHazard(String displayName, String hint, String counterplayLabel) {
            this.displayName = displayName;
            this.hint = hint;
            this.counterplayLabel = counterplayLabel;
        }

        public String displayName() {
            return displayName;
        }

        public String hint() {
            return hint;
        }

        public String counterplayLabel() {
            return counterplayLabel;
        }
    }

    public enum ParticleCue {
        ELECTRIC_SPARK,
        ASH,
        SMOKE,
        SPORE,
        GLOW,
        SNOW,
        PORTAL;

        public ParticleOptions particle() {
            return switch (this) {
                case ASH -> ParticleTypes.ASH;
                case SMOKE -> ParticleTypes.SMOKE;
                case SPORE -> ParticleTypes.SPORE_BLOSSOM_AIR;
                case GLOW -> ParticleTypes.GLOW;
                case SNOW -> ParticleTypes.SNOWFLAKE;
                case PORTAL -> ParticleTypes.PORTAL;
                case ELECTRIC_SPARK -> ParticleTypes.ELECTRIC_SPARK;
            };
        }
    }

    public enum PulseSound {
        EXPLOSION,
        GLASS,
        SLIME,
        SHRIEK,
        ANCHOR,
        BEACON,
        ANVIL,
        AMETHYST;

        public SoundEvent sound() {
            return switch (this) {
                case GLASS -> SoundEvents.GLASS_BREAK;
                case SLIME -> SoundEvents.SLIME_SQUISH;
                case SHRIEK -> SoundEvents.SCULK_SHRIEKER_SHRIEK;
                case ANCHOR -> SoundEvents.RESPAWN_ANCHOR_DEPLETE.value();
                case BEACON -> SoundEvents.BEACON_POWER_SELECT;
                case ANVIL -> SoundEvents.ANVIL_LAND;
                case AMETHYST -> SoundEvents.AMETHYST_BLOCK_CHIME;
                case EXPLOSION -> SoundEvents.GENERIC_EXPLODE.value();
            };
        }
    }

    public enum VisualVariant {
        NONE,
        SENTINEL,
        WARLORD,
        STALKER,
        JUGGERNAUT,
        MATRIARCH,
        COLOSSUS,
        BEHEMOTH,
        OVERSEER,
        NEXUS
    }

    public record StatusEffect(Holder<MobEffect> effect, int duration, int amplifier) {
        public MobEffectInstance instance(int phase) {
            return new MobEffectInstance(effect, duration + phase * 15, amplifier);
        }
    }

    public record LootEntry(Supplier<? extends ItemLike> item, int minCount, int randomBonus) {
        public ItemStack stack(RandomSource random) {
            int count = minCount + (randomBonus <= 0 ? 0 : random.nextInt(randomBonus + 1));
            return new ItemStack(item.get(), Math.max(1, count));
        }
    }

    public record RewardBundle(List<LootEntry> entries) {
        public boolean isEmpty() {
            return entries.isEmpty();
        }
    }

    public record VisualProfile(
            String texturePath,
            String glowTexturePath,
            int tint,
            int glowColor,
            float scale,
            float shadow,
            BossEvent.BossBarColor bossBarColor,
            boolean darkenScreen,
            VisualVariant variant
    ) {
    }

    public record CinematicCue(
            String phaseWarningLabel,
            String dangerVerb,
            String counterplayLabel,
            String objectiveLabel
    ) {
    }

    public record PolishData(
            String arenaSetPiece,
            String phaseCue,
            String counterplayObject,
            String addPressurePattern,
            String rewardCategory,
            String codexSummary,
            String hudObjectiveLabel
    ) {
    }
}
