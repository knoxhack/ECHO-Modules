package com.knoxhack.echoashfallprotocol.survival;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.block.entity.ScrubberSafeZoneManager;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventData;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventType;
import com.knoxhack.echoashfallprotocol.item.GasMaskItem;
import com.knoxhack.echoashfallprotocol.item.HazmatArmorItem;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Locale;

/**
 * Central expedition hazard classifier.
 * Keeps biome, event, source-block, protection, and safe-zone decisions in one place.
 */
public final class HazardZoneManager {

    private static final int SOURCE_SCAN_RADIUS = 5;
    private static final int SOURCE_SCAN_VERTICAL_RADIUS = 2;
    public static final TagKey<Block> TOXIC_AIR_SOURCES = blockTag("toxic_air_sources");
    public static final TagKey<Block> RADIATION_SOURCES = blockTag("radiation_sources");
    public static final TagKey<Block> ACID_SOURCES = blockTag("acid_sources");
    public static final TagKey<Block> CRYO_SOURCES = blockTag("cryo_sources");
    public static final TagKey<Block> NEXUS_ANOMALY_SOURCES = blockTag("nexus_anomaly_sources");

    private static final TagKey<Biome> TOXIC_AIR_BIOMES = biomeTag("toxic_air_biomes");
    private static final TagKey<Biome> RADIATION_BIOMES = biomeTag("radiation_biomes");
    private static final TagKey<Biome> CRYO_BIOMES = biomeTag("cryogenic_biomes");
    private static final TagKey<Biome> NEXUS_ANOMALY_BIOMES = biomeTag("nexus_anomaly_biomes");

    private HazardZoneManager() {
    }

    public static HazardSnapshot scan(ServerPlayer player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        boolean biomeHazards = Config.ENABLE_BIOME_HAZARDS.get();
        float nexusMultiplier = Math.max(0.0f, Config.NEXUS_HAZARD_MULTIPLIER.get().floatValue());
        boolean safeZone = level instanceof ServerLevel serverLevel && ScrubberSafeZoneManager.isInSafeZone(serverLevel, pos);

        boolean acidContact = isContacting(level, pos, ACID_SOURCES);
        boolean nexus = biomeHazards && isBiome(level, pos, NEXUS_ANOMALY_BIOMES)
                || hasNearby(level, pos, NEXUS_ANOMALY_SOURCES, SOURCE_SCAN_RADIUS, SOURCE_SCAN_VERTICAL_RADIUS);

        boolean toxicBiome = biomeHazards && isBiome(level, pos, TOXIC_AIR_BIOMES);
        boolean globalToxicAir = Config.GLOBAL_TOXIC_AIR.get();
        boolean toxicSource = shouldScanToxicSources(toxicBiome)
                && (globalToxicAir
                || hasNearby(level, pos, TOXIC_AIR_SOURCES, SOURCE_SCAN_RADIUS, SOURCE_SCAN_VERTICAL_RADIUS));
        boolean toxicAir = isToxicAirActive(toxicBiome, toxicSource, nexus, safeZone);

        boolean radiationBiome = biomeHazards && isBiome(level, pos, RADIATION_BIOMES);
        boolean radiationSource = hasNearby(level, pos, RADIATION_SOURCES, SOURCE_SCAN_RADIUS + 1, SOURCE_SCAN_VERTICAL_RADIUS);
        boolean radiationStorm = isRadiationStorm(level);
        boolean stormSheltered = radiationStorm && isSheltered(level, pos);
        boolean stormRadiationExposure = radiationStorm && !stormSheltered;
        boolean radiationZone = (radiationBiome || radiationSource || nexus || stormRadiationExposure) && !safeZone;

        boolean cryoBiome = biomeHazards && isBiome(level, pos, CRYO_BIOMES);
        boolean cryoSource = hasNearby(level, pos, CRYO_SOURCES, SOURCE_SCAN_RADIUS, SOURCE_SCAN_VERTICAL_RADIUS);
        boolean cryoCold = cryoBiome || cryoSource;

        float toxicIntensity = 0.0f;
        if (toxicAir) {
            toxicIntensity = globalToxicAir ? 0.75f : 0.0f;
            if (toxicBiome) toxicIntensity = Math.max(toxicIntensity, 1.0f);
            if (toxicSource) toxicIntensity = Math.max(toxicIntensity, 1.15f);
            if (nexus) toxicIntensity = Math.max(toxicIntensity, nexusMultiplier);
            toxicIntensity *= Math.max(0.0f, Config.TOXIC_HAZARD_FILTER_MULTIPLIER.get().floatValue());
        }

        float radiationIntensity = 0.0f;
        if (radiationZone) {
            if (radiationBiome) radiationIntensity += 1.0f;
            if (radiationSource) radiationIntensity += 1.15f;
            if (stormRadiationExposure) radiationIntensity += 1.35f;
            if (nexus) radiationIntensity += 0.85f * nexusMultiplier;
            radiationIntensity *= Math.max(0.0f, Config.RADIATION_HAZARD_MULTIPLIER.get().floatValue());
        }

        float coldIntensity = 0.0f;
        if (cryoCold) {
            coldIntensity = (cryoBiome ? 1.0f : 0.65f)
                    * Math.max(0.0f, Config.CRYO_COLD_LOSS_MULTIPLIER.get().floatValue());
            if (safeZone) {
                coldIntensity *= 0.35f;
            }
        }

        float acidIntensity = acidContact ? 1.0f : 0.0f;
        HazardType primary = primaryType(safeZone, nexus, radiationStorm, radiationZone, toxicAir, cryoCold, acidContact);
        float maxIntensity = Math.max(Math.max(toxicIntensity, radiationIntensity), Math.max(coldIntensity, acidIntensity));
        HazardSeverity severity = severity(primary, maxIntensity, stormSheltered);
        String reason = reason(primary, safeZone, nexus, radiationStorm, stormSheltered, radiationSource, radiationBiome,
                toxicSource, toxicBiome, cryoCold, acidContact);

        return new HazardSnapshot(
                primary,
                severity,
                safeZone,
                toxicAir,
                radiationZone,
                cryoCold,
                acidContact,
                nexus,
                radiationStorm,
                stormSheltered,
                clampIntensity(toxicIntensity),
                clampIntensity(radiationIntensity),
                clampIntensity(coldIntensity),
                clampIntensity(acidIntensity),
                reason
        );
    }

    public static boolean hasRespiratoryProtection(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GasMaskItem || hasFullHazmat(player);
    }

    public static boolean hasFullHazmat(Player player) {
        return HazmatArmorItem.hasFullSet(getArmorItems(player));
    }

    public static boolean hasAcidProtection(Player player) {
        return hasFullHazmat(player);
    }

    public static boolean hasThermalProtection(Player player) {
        return player.getInventory().contains(new ItemStack(ModItems.THERMAL_LINER.get())) || hasFullHazmat(player);
    }

    public static float radiationResistance(Player player) {
        return HazmatArmorItem.getTotalRadiationResistance(getArmorItems(player));
    }

    public static boolean isSheltered(Level level, BlockPos pos) {
        if (pos.getY() < 50) {
            return true;
        }
        if (level instanceof ServerLevel serverLevel && !serverLevel.canSeeSky(pos.above())) {
            return true;
        }
        BlockPos headPos = pos.above(2);
        int coverCount = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (!level.isEmptyBlock(headPos.offset(dx, 0, dz))) {
                    coverCount++;
                }
            }
        }
        return coverCount >= 5;
    }

    public static boolean isRadiationStorm(Level level) {
        if (!(level instanceof ServerLevel serverLevel) || !Config.ENABLE_RADIATION_STORMS.get()) {
            return false;
        }
        return EnvironmentalEventData.get(serverLevel).getCurrentEvent() == EnvironmentalEventType.RADIATION_STORM;
    }

    private static HazardType primaryType(boolean safeZone, boolean nexus, boolean radiationStorm, boolean radiationZone,
                                          boolean toxicAir, boolean cryoCold, boolean acidContact) {
        if (safeZone) return HazardType.SAFE_ZONE;
        if (nexus) return HazardType.NEXUS_ANOMALY;
        if (radiationStorm) return HazardType.RADIATION_STORM;
        if (radiationZone) return HazardType.RADIATION;
        if (toxicAir) return HazardType.TOXIC_AIR;
        if (cryoCold) return HazardType.CRYO_COLD;
        if (acidContact) return HazardType.ACID;
        return HazardType.NONE;
    }

    private static HazardSeverity severity(HazardType primary, float intensity, boolean stormSheltered) {
        if (primary == HazardType.NONE) return HazardSeverity.NONE;
        if (primary == HazardType.SAFE_ZONE) return HazardSeverity.LOW;
        if (primary == HazardType.NEXUS_ANOMALY) return HazardSeverity.EXTREME;
        if (primary == HazardType.RADIATION_STORM && !stormSheltered) return HazardSeverity.HIGH;
        if (intensity >= 2.25f) return HazardSeverity.EXTREME;
        if (intensity >= 1.35f) return HazardSeverity.HIGH;
        if (intensity >= 0.75f) return HazardSeverity.MEDIUM;
        return HazardSeverity.LOW;
    }

    private static String reason(HazardType primary, boolean safeZone, boolean nexus, boolean radiationStorm, boolean stormSheltered,
                                 boolean radiationSource, boolean radiationBiome, boolean toxicSource, boolean toxicBiome,
                                 boolean cryoCold, boolean acidContact) {
        if (acidContact) return "acid_contact";
        if (nexus) return "nexus_anomaly";
        if (radiationStorm) return stormSheltered ? "radiation_storm_sheltered" : "radiation_storm_exposed";
        if (safeZone) return "scrubber_safe_zone";
        if (radiationSource) return "radiation_source";
        if (radiationBiome) return "radiation_biome";
        if (toxicSource) return Config.GLOBAL_TOXIC_AIR.get() ? "global_toxic_air" : "toxic_source";
        if (toxicBiome) return "toxic_biome";
        if (cryoCold) return "cryogenic_exposure";
        return primary.name().toLowerCase(Locale.ROOT);
    }

    private static boolean isBiome(Level level, BlockPos pos, TagKey<Biome> tag) {
        return level.getBiome(pos).is(tag);
    }

    private static boolean shouldScanToxicSources(boolean toxicBiome) {
        return !toxicBiome;
    }

    private static boolean isToxicAirActive(boolean toxicBiome, boolean toxicSource, boolean nexus, boolean safeZone) {
        return (toxicBiome || toxicSource || nexus) && !safeZone;
    }

    private static boolean isContacting(Level level, BlockPos center, TagKey<Block> tag) {
        return level.getBlockState(center).is(tag) || level.getBlockState(center.below()).is(tag);
    }

    private static boolean hasNearby(Level level, BlockPos center, TagKey<Block> tag, int radius, int verticalRadius) {
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -verticalRadius, -radius),
                center.offset(radius, verticalRadius, radius))) {
            if (level.getBlockState(pos).is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static float clampIntensity(float value) {
        return Math.max(0.0f, Math.min(3.0f, value));
    }

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, path));
    }

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, path));
    }

    private static Iterable<ItemStack> getArmorItems(Player player) {
        return List.of(
                player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.FEET)
        );
    }

    public enum HazardType {
        NONE,
        TOXIC_AIR,
        RADIATION,
        CRYO_COLD,
        ACID,
        NEXUS_ANOMALY,
        RADIATION_STORM,
        SAFE_ZONE
    }

    public enum HazardSeverity {
        NONE,
        LOW,
        MEDIUM,
        HIGH,
        EXTREME
    }

    public record HazardSnapshot(
            HazardType primaryType,
            HazardSeverity severity,
            boolean safeZone,
            boolean toxicAir,
            boolean radiationZone,
            boolean cryoCold,
            boolean acidContact,
            boolean nexusAnomaly,
            boolean radiationStorm,
            boolean stormSheltered,
            float toxicIntensity,
            float radiationIntensity,
            float coldIntensity,
            float acidIntensity,
            String reason
    ) {
        public float primaryIntensity() {
            return switch (primaryType) {
                case TOXIC_AIR -> toxicIntensity;
                case RADIATION, RADIATION_STORM, NEXUS_ANOMALY -> radiationIntensity;
                case CRYO_COLD -> coldIntensity;
                case ACID -> acidIntensity;
                case SAFE_ZONE -> 0.25f;
                case NONE -> 0.0f;
            };
        }
    }
}
