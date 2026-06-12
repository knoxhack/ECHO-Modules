package com.knoxhack.echoindex.service;

import com.echoplatform.echocore.api.index.IndexRecipeView;

public enum IndexRecipeSourceKind {
    VANILLA,
    ADDON,
    BLOCK_DROP,
    LOOT_TABLE,
    WORLDGEN,
    STRUCTURE,
    TRADER,
    CACHE,
    MISSION_REWARD,
    ROUTE_UNLOCK,
    RESEARCH,
    MACHINE,
    SOURCE_CARD,
    UNKNOWN;

    public static IndexRecipeSourceKind of(IndexRecipeView view) {
        if (view == null) {
            return UNKNOWN;
        }
        if (isSourceCard(view)) {
            String title = view.title().toLowerCase(java.util.Locale.ROOT);
            String notes = String.join(" ", view.notes()).toLowerCase(java.util.Locale.ROOT);
            String combined = title + " " + notes;
            if (combined.contains("block drop") || combined.contains("block loot")) {
                return BLOCK_DROP;
            }
            if (combined.contains("world generation") || combined.contains("worldgen")) {
                return WORLDGEN;
            }
            if (combined.contains("structure") || combined.contains("poi")) {
                return STRUCTURE;
            }
            if (combined.contains("trader") || combined.contains("trade")) {
                return TRADER;
            }
            if (combined.contains("cache")) {
                return CACHE;
            }
            if (combined.contains("mission reward") || combined.contains("mission")) {
                return MISSION_REWARD;
            }
            if (combined.contains("route unlock") || combined.contains("route")) {
                return ROUTE_UNLOCK;
            }
            if (combined.contains("research") || combined.contains("schematic")) {
                return RESEARCH;
            }
            if (combined.contains("machine") || combined.contains("process")) {
                return MACHINE;
            }
            if (combined.contains("loot")) {
                return LOOT_TABLE;
            }
            return SOURCE_CARD;
        }
        if ("minecraft".equals(view.sourceModId())) {
            return VANILLA;
        }
        return ADDON;
    }

    public static boolean isSourceCard(IndexRecipeView view) {
        return view != null && IndexSourceRecipeProvider.CATEGORY.equals(view.categoryId());
    }

    public String label() {
        return switch (this) {
            case BLOCK_DROP -> "Block Drop";
            case LOOT_TABLE -> "Loot Source";
            case WORLDGEN -> "World Generation";
            case STRUCTURE -> "Structure";
            case TRADER -> "Trader";
            case CACHE -> "Cache";
            case MISSION_REWARD -> "Mission Reward";
            case ROUTE_UNLOCK -> "Route Unlock";
            case RESEARCH -> "Research";
            case MACHINE -> "Machine";
            case SOURCE_CARD -> "Source";
            case VANILLA -> "Vanilla";
            case ADDON -> "Addon";
            case UNKNOWN -> "Unknown";
        };
    }
}
