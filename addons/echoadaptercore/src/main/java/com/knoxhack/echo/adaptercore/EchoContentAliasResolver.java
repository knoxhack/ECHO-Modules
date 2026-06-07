package com.knoxhack.echo.adaptercore;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class EchoContentAliasResolver {
    private static final EchoContentAliasResolver STANDARD = builder()
            .alias("clean_water", EchoCanonicalContentIds.ITEM_CLEAN_WATER_BOTTLE)
            .alias("ashfall:clean_water", EchoCanonicalContentIds.ITEM_CLEAN_WATER_BOTTLE)
            .alias("dirty_water", EchoCanonicalContentIds.ITEM_DIRTY_WATER_BOTTLE)
            .alias("ashfall:dirty_water", EchoCanonicalContentIds.ITEM_DIRTY_WATER_BOTTLE)
            .alias("crude_filter", EchoCanonicalContentIds.ITEM_CRUDE_FILTER)
            .alias("ashfall:crude_filter", EchoCanonicalContentIds.ITEM_CRUDE_FILTER)
            .alias("portable_signal_scanner", EchoCanonicalContentIds.ITEM_PORTABLE_SIGNAL_SCANNER)
            .alias("signal_scanner_item", EchoCanonicalContentIds.ITEM_PORTABLE_SIGNAL_SCANNER)
            .alias("signal_scanner", EchoCanonicalContentIds.BLOCK_SIGNAL_SCANNER)
            .alias("ashfall:signal_scanner", EchoCanonicalContentIds.BLOCK_SIGNAL_SCANNER)
            .alias("relay_station", EchoCanonicalContentIds.BLOCK_RELAY_STATION)
            .alias("ashfall:relay_station", EchoCanonicalContentIds.BLOCK_RELAY_STATION)
            .alias("power_node", EchoCanonicalContentIds.BLOCK_POWER_NODE)
            .alias("ashfall:power_node", EchoCanonicalContentIds.BLOCK_POWER_NODE)
            .alias("rare_schematic", EchoCanonicalContentIds.ITEM_RARE_TECH_SCHEMATIC)
            .alias("rare_tech_schematic", EchoCanonicalContentIds.ITEM_RARE_TECH_SCHEMATIC)
            .alias("schematic_unlocked", EchoCanonicalContentIds.EVENT_ASHFALL_SCHEMATIC_UNLOCKED)
            .alias("research_updated", EchoCanonicalContentIds.EVENT_ASHFALL_RESEARCH_UPDATED)
            .alias("radaway", EchoCanonicalContentIds.ITEM_RAD_AWAY)
            .alias("rad_away", EchoCanonicalContentIds.ITEM_RAD_AWAY)
            .alias("bandage", EchoCanonicalContentIds.ITEM_BANDAGE)
            .alias("ashfall:bandage", EchoCanonicalContentIds.ITEM_BANDAGE)
            .alias("stimpack", EchoCanonicalContentIds.ITEM_STIM_PACK)
            .alias("stim_pack", EchoCanonicalContentIds.ITEM_STIM_PACK)
            .alias("ashfall:stim_pack", EchoCanonicalContentIds.ITEM_STIM_PACK)
            .alias("mutagen", EchoCanonicalContentIds.ITEM_MUTAGEN_VIAL)
            .alias("mutagen_vial", EchoCanonicalContentIds.ITEM_MUTAGEN_VIAL)
            .alias("ashfall:mutagen", EchoCanonicalContentIds.ITEM_MUTAGEN_VIAL)
            .alias("ashfall:mutagen_vial", EchoCanonicalContentIds.ITEM_MUTAGEN_VIAL)
            .alias("instability_dampener", EchoCanonicalContentIds.ITEM_INSTABILITY_DAMPENER)
            .alias("dampener", EchoCanonicalContentIds.ITEM_INSTABILITY_DAMPENER)
            .alias("nexus_dampener", EchoCanonicalContentIds.ITEM_INSTABILITY_DAMPENER)
            .alias("ashfall:instability_dampener", EchoCanonicalContentIds.ITEM_INSTABILITY_DAMPENER)
            .alias("return_beacon", EchoCanonicalContentIds.ITEM_RETURN_BEACON)
            .alias("ashfall:return_beacon", EchoCanonicalContentIds.ITEM_RETURN_BEACON)
            .alias("nexus_return_beacon", EchoCanonicalContentIds.ITEM_RETURN_BEACON)
            .alias("field_med_bay_used", EchoCanonicalContentIds.EVENT_ASHFALL_MED_BAY_USED)
            .alias("radiation_cleanser_used", EchoCanonicalContentIds.EVENT_ASHFALL_CLEANSER_USED)
            .alias("atmospheric_scrubber_used", EchoCanonicalContentIds.EVENT_ASHFALL_SCRUBBER_USED)
            .alias("item_obtained", EchoCanonicalContentIds.EVENT_PLAYER_ITEM_OBTAINED)
            .alias("item_consumed", EchoCanonicalContentIds.EVENT_PLAYER_ITEM_CONSUMED)
            .alias("recipe_crafted", EchoCanonicalContentIds.EVENT_PLAYER_RECIPE_CRAFTED)
            .alias("block_placed", EchoCanonicalContentIds.EVENT_PLAYER_BLOCK_PLACED)
            .build();

    private final Map<String, String> aliases;

    private EchoContentAliasResolver(Map<String, String> aliases) {
        this.aliases = Map.copyOf(aliases);
    }

    public static EchoContentAliasResolver standard() {
        return STANDARD;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String resolveContentId(String contentId) {
        String normalized = normalize(contentId);
        String resolved = aliases.getOrDefault(normalized, normalized);
        if (resolved.contains(".") && !resolved.contains(":")) {
            return EchoCanonicalContentIds.normalizeEventName(resolved);
        }
        return EchoCanonicalContentIds.normalizeContentId(resolved);
    }

    public String resolveActionId(String actionId) {
        String normalized = normalize(actionId);
        return aliases.getOrDefault(normalized, normalized);
    }

    public Map<String, String> aliases() {
        return aliases;
    }

    private static String normalize(String value) {
        return AdapterContractGuards.requireText(value, "content alias").toLowerCase(Locale.ROOT);
    }

    public static final class Builder {
        private final Map<String, String> aliases = new LinkedHashMap<>();

        public Builder alias(String alias, String canonicalId) {
            aliases.put(normalize(alias), AdapterContractGuards.requireText(canonicalId, "canonical id")
                    .toLowerCase(Locale.ROOT));
            return this;
        }

        public EchoContentAliasResolver build() {
            return new EchoContentAliasResolver(aliases);
        }
    }
}
