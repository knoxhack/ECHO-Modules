package com.knoxhack.echoindex.client;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class IndexAddonPresentation {
    private static final int DEFAULT_ACCENT = 0xFF66E8FF;
    private static final Map<String, Descriptor> DESCRIPTORS = descriptors();

    private IndexAddonPresentation() {
    }

    static Style style(String modId) {
        String clean = cleanModId(modId);
        Descriptor descriptor = DESCRIPTORS.get(clean);
        String displayName = loadedDisplayName(clean, descriptor);
        String shortLabel = descriptor == null ? compactLabel(displayName, clean) : descriptor.shortLabel();
        int accent = descriptor == null ? DEFAULT_ACCENT : descriptor.accent();
        ItemStack icon = descriptor == null ? ItemStack.EMPTY : firstIcon(descriptor.iconIds());
        String version = loadedVersion(clean);
        return new Style(clean, displayName, shortLabel, accent, icon, version);
    }

    static ItemStack icon(String modId, ItemStack fallback) {
        ItemStack icon = style(modId).icon();
        return icon.isEmpty() ? (fallback == null ? ItemStack.EMPTY : fallback.copy()) : icon;
    }

    static String displayName(String modId) {
        return style(modId).displayName();
    }

    static int accent(String modId) {
        return style(modId).accent();
    }

    static String categoryLabel(Identifier categoryId) {
        if (categoryId == null) {
            return "Recipe";
        }
        String path = categoryId.getPath();
        int slash = path.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < path.length()) {
            path = path.substring(slash + 1);
        }
        if (path.startsWith("crafting_")) {
            path = path.substring("crafting_".length()) + "_crafting";
        }
        return titleCase(path);
    }

    static String compactCategoryLabel(Identifier categoryId) {
        String label = categoryLabel(categoryId);
        return label.length() <= 18 ? label : label.substring(0, 16) + "..";
    }

    static String humanize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return titleCase(value);
    }

    private static Map<String, Descriptor> descriptors() {
        Map<String, Descriptor> map = new LinkedHashMap<>();
        put(map, "minecraft", "Minecraft", "MC", 0xFF7EDB8B, "minecraft:grass_block", "minecraft:crafting_table");
        put(map, "echoindex", "ECHO Index", "IDX", DEFAULT_ACCENT, "minecraft:compass", "minecraft:book");
        put(map, "echocore", "ECHO Core", "CORE", 0xFF66E8FF, "minecraft:nether_star", "minecraft:amethyst_shard");
        put(map, "echonetcore", "ECHO NetCore", "NET", 0xFF9BE7FF, "minecraft:redstone", "minecraft:repeater");
        put(map, "echoashfallprotocol", "Ashfall Protocol", "ASH", 0xFFFF6B6B,
                "echoashfallprotocol:clean_water_cell", "echoashfallprotocol:plant_fiber", "minecraft:magma_block");
        put(map, "echoindustrialnexus", "Industrial Nexus", "IND", 0xFFFF9F3D,
                "echoindustrialnexus:clean_water_cell", "echoindustrialnexus:fluid_refiner", "minecraft:blast_furnace");
        put(map, "echonexusprotocol", "Nexus Protocol", "NEX", 0xFFC77DFF,
                "echonexusprotocol:nexus_shard", "echonexusprotocol:nexus_recycler", "minecraft:ender_eye");
        put(map, "echoblackboxprotocol", "Blackbox Protocol", "BBX", 0xFFE0E6F0,
                "echoblackboxprotocol:blackbox_decoder", "echoblackboxprotocol:memory_projector", "minecraft:observer");
        put(map, "echoorbitalremnants", "Orbital Remnants", "ORB", 0xFF78C6FF,
                "echoorbitalremnants:oxygen_canister", "echoorbitalremnants:orbital_scrap", "minecraft:end_rod");
        put(map, "echoagriculturereclamation", "Agriculture Reclamation", "AGR", 0xFFA8F7C5,
                "echoagriculturereclamation:soil_purifier", "echoagriculturereclamation:purification_enzyme", "minecraft:wheat");
        put(map, "echoconvoyprotocol", "Convoy Protocol", "CNV", 0xFFFFD166,
                "echoconvoyprotocol:convoy_station", "echoconvoyprotocol:route_marker", "minecraft:minecart");
        put(map, "echologisticsnetwork", "Logistics Network", "LOG", 0xFF9BE7FF,
                "echologisticsnetwork:loadout_card", "echologisticsnetwork:logistics_terminal", "minecraft:chest");
        put(map, "echoarmory", "Armory", "ARM", 0xFFE0E6F0,
                "echoarmory:armory_bench", "echoarmory:sidearm_frame", "minecraft:iron_sword");
        put(map, "echomultiblockcore", "Multiblock Core", "MLT", 0xFF75D7FF,
                "echomultiblockcore:multiblock_controller", "minecraft:structure_block");
        put(map, "echoworldcore", "WorldCore", "WRD", 0xFF66E8FF,
                "echoworldcore:hazard_scanner", "minecraft:map");
        put(map, "echomissioncore", "MissionCore", "MIS", 0xFFFFD166,
                "echomissioncore:mission_terminal", "minecraft:writable_book");
        put(map, "echodatacore", "DataCore", "DAT", 0xFF8FD6FF,
                "echodatacore:data_drive", "minecraft:paper");
        put(map, "echoblockworks", "Blockworks", "BLK", 0xFFD0E0EA,
                "echoblockworks:blockworks_table", "minecraft:stonecutter");
        put(map, "echoholomap", "HoloMap", "MAP", 0xFF66E8FF,
                "echoholomap:holo_projector", "minecraft:filled_map");
        put(map, "echorecovery", "Recovery", "REC", 0xFFA8F7C5,
                "echorecovery:recovery_beacon", "minecraft:respawn_anchor");
        put(map, "echotutorialcore", "TutorialCore", "TUT", 0xFFE8D4A2,
                "echotutorialcore:tutorial_tablet", "minecraft:book");
        put(map, "echosoundcore", "SoundCore", "SND", 0xFFB6E3FF, "minecraft:note_block");
        put(map, "echoweathercore", "WeatherCore", "WTH", 0xFF9BE7FF, "minecraft:lightning_rod", "minecraft:snowball");
        put(map, "echopowergrid", "PowerGrid", "PWR", 0xFFFFD166, "minecraft:redstone_block", "minecraft:lightning_rod");
        put(map, "echorelictech", "RelicTech", "REL", 0xFFE09CFF, "minecraft:echo_shard", "minecraft:amethyst_shard");
        put(map, "echolens", "ECHO Lens", "LENS", 0xFFB6E3FF, "minecraft:spyglass");
        put(map, "echoterminal", "ECHO Terminal", "TERM", 0xFF66E8FF, "minecraft:compass");
        return map;
    }

    private static void put(Map<String, Descriptor> map, String modId, String displayName,
            String shortLabel, int accent, String... iconIds) {
        map.put(modId, new Descriptor(displayName, shortLabel, accent, iconIds));
    }

    private static ItemStack firstIcon(String[] iconIds) {
        if (iconIds == null) {
            return ItemStack.EMPTY;
        }
        for (String raw : iconIds) {
            Identifier id = Identifier.tryParse(raw);
            if (id == null) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }

    private static String loadedDisplayName(String modId, Descriptor descriptor) {
        String fallback = descriptor == null ? fallbackName(modId) : descriptor.displayName();
        String display = EchoRuntimeModules.metadata(modId, fallback).displayName();
        if (display == null || display.isBlank()) {
            return fallback;
        }
        String clean = display.strip();
        return isIdLikeDisplay(clean, modId) ? fallback : clean;
    }

    private static String loadedVersion(String modId) {
        String version = EchoRuntimeModules.metadata(modId, modId).version();
        return version == null ? "" : version;
    }

    private static String fallbackName(String modId) {
        return titleCase(cleanModId(modId)
                .replaceFirst("^echo", "echo ")
                .replaceFirst("^neo", "neo "));
    }

    private static boolean isIdLikeDisplay(String displayName, String modId) {
        String display = normalizeIdLike(displayName);
        String id = normalizeIdLike(cleanModId(modId));
        return display.equals(id) || display.equals(normalizeIdLike(fallbackName(modId)));
    }

    private static String normalizeIdLike(String value) {
        return value == null
                ? ""
                : value.replace("_", "")
                        .replace("-", "")
                        .replace(" ", "")
                        .toLowerCase(Locale.ROOT);
    }

    private static String compactLabel(String displayName, String modId) {
        String[] words = displayName.split("\\s+");
        StringBuilder label = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            label.append(Character.toUpperCase(word.charAt(0)));
            if (label.length() >= 4) {
                break;
            }
        }
        if (!label.isEmpty()) {
            return label.toString();
        }
        String clean = cleanModId(modId);
        return clean.length() <= 4 ? clean.toUpperCase(Locale.ROOT) : clean.substring(0, 4).toUpperCase(Locale.ROOT);
    }

    private static String titleCase(String value) {
        String clean = value.replace('_', ' ').replace('-', ' ').replace('/', ' ').strip();
        if (clean.isBlank()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String part : clean.split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            String lower = part.toLowerCase(Locale.ROOT);
            builder.append(lower.substring(0, 1).toUpperCase(Locale.ROOT));
            if (lower.length() > 1) {
                builder.append(lower.substring(1));
            }
        }
        return builder.toString();
    }

    private static String cleanModId(String modId) {
        return modId == null || modId.isBlank() ? "minecraft" : modId.strip().toLowerCase(Locale.ROOT);
    }

    record Style(String modId, String displayName, String shortLabel, int accent, ItemStack icon, String version) {
    }

    private record Descriptor(String displayName, String shortLabel, int accent, String[] iconIds) {
    }
}
