package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Standalone-safe creative inventory contract shared by Native Loader and
 * Standalone entrypoints. NeoForge-only registry expansion stays in ModCreativeTabs.
 */
public final class AshfallNativeCreativeTabCatalog {
    private static final List<String> FEATURED_ITEMS = List.of(
            "echoashfallprotocol:field_manual",
            "echoashfallprotocol:portable_signal_scanner",
            "echoashfallprotocol:gas_mask",
            "echoashfallprotocol:filter_cartridge_basic",
            "echoashfallprotocol:basic_battery",
            "echoashfallprotocol:energy_cell",
            "echoashfallprotocol:hand_recycler",
            "echoashfallprotocol:water_purifier",
            "echoashfallprotocol:micro_generator",
            "echoashfallprotocol:signal_scanner",
            "echoashfallprotocol:scrap_press",
            "echoashfallprotocol:factory_controller",
            "echoashfallprotocol:relay_scanner_lens",
            "echoashfallprotocol:survey_table",
            "echoashfallprotocol:nexus_crystal"
    );

    private static final List<String> BASELINE_ITEMS = List.of(
            "echoashfallprotocol:field_manual",
            "echoashfallprotocol:portable_signal_scanner",
            "echoashfallprotocol:gas_mask",
            "echoashfallprotocol:filter_cartridge_basic",
            "echoashfallprotocol:basic_battery",
            "echoashfallprotocol:energy_cell",
            "echoashfallprotocol:hand_recycler",
            "echoashfallprotocol:water_purifier",
            "echoashfallprotocol:micro_generator",
            "echoashfallprotocol:signal_scanner",
            "echoashfallprotocol:scrap_press",
            "echoashfallprotocol:factory_controller",
            "echoashfallprotocol:relay_scanner_lens",
            "echoashfallprotocol:survey_table",
            "echoashfallprotocol:nexus_crystal",
            "echoterminal:echo_terminal",
            "echoterminal:echo_terminal_remote"
    );

    private AshfallNativeCreativeTabCatalog() {
    }

    public static List<String> featuredItemIds() {
        return FEATURED_ITEMS;
    }

    public static List<String> baselineItemIds() {
        return BASELINE_ITEMS;
    }

    public static List<String> itemIds() {
        LinkedHashSet<String> itemIds = new LinkedHashSet<>();
        itemIds.addAll(FEATURED_ITEMS);
        itemIds.addAll(BASELINE_ITEMS);
        return List.copyOf(itemIds);
    }

    public static List<String> namespaces() {
        return itemIds().stream()
                .map(AshfallNativeCreativeTabCatalog::namespace)
                .filter(namespace -> !namespace.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private static String namespace(String itemId) {
        int separator = itemId == null ? -1 : itemId.indexOf(':');
        return separator <= 0 ? "" : itemId.substring(0, separator);
    }
}
