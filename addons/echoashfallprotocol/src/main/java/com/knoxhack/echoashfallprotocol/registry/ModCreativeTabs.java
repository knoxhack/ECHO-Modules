package com.knoxhack.echoashfallprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final String NATIVE_LOADER_REGISTRY_BACKED_TAB_ID =
            EchoAshfallProtocol.MODID + ":native_modules_tab";
    public static final Object CREATIVE_MODE_TABS =
            EchoBackendRegistryBridge.create(Registries.CREATIVE_MODE_TAB, EchoAshfallProtocol.MODID);
    private static final Set<String> EXTRA_NATIVE_MODULE_NAMESPACES = Set.of("signalos", "signalosexample");

    public static final EchoBackendRegistryEntry<CreativeModeTab> ASHES_TAB = EchoBackendRegistryBridge.register(
            CREATIVE_MODE_TABS,
            "ashes_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.EchoAshfallProtocol"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.SCRAP_KNIFE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Scrap Materials
                        output.accept(ModItems.SCRAP_METAL.get());
                        output.accept(ModItems.SCRAP_WIRE.get());
                        output.accept(ModItems.SCRAP_CIRCUIT.get());
                        output.accept(ModItems.SCRAP_PLASTIC.get());

                        // Crafting Components
                        output.accept(ModItems.ASH.get());
                        output.accept(ModItems.CIRCUIT_BOARD.get());
                        output.accept(ModItems.ENERGY_CELL.get());
                        output.accept(ModItems.BASIC_BATTERY.get());
                        output.accept(ModItems.ADVANCED_BATTERY.get());
                        output.accept(ModItems.ELITE_BATTERY.get());
                        output.accept(ModItems.POWER_CELL.get());
                        output.accept(ModItems.FILTRATION_MEMBRANE.get());
                        output.accept(ModItems.THERMAL_LINER.get());
                        output.accept(ModItems.MACHINE_CASING.get());
                        output.accept(ModItems.MUTATED_TISSUE.get());

                        // Survival
                        output.accept(ModItems.DIRTY_WATER_BOTTLE.get());
                        output.accept(ModItems.FILTERED_WATER_BOTTLE.get());
                        output.accept(ModItems.BOILED_WATER_BOTTLE.get());
                        output.accept(ModItems.CLEAN_WATER_BOTTLE.get());
                        output.accept(ModItems.MUTAGEN_VIAL.get());
                        output.accept(ModItems.RAD_AWAY.get());
                        output.accept(ModItems.FILTER_CARTRIDGE_BASIC.get());
                        output.accept(ModItems.FILTER_CARTRIDGE_ADVANCED.get());
                        output.accept(ModItems.FILTER_CARTRIDGE_ELITE.get());
                        output.accept(ModItems.CRUDE_FILTER.get());

                        // Tools & Weapons
                        output.accept(ModItems.PORTABLE_SIGNAL_SCANNER.get());
                        output.accept(ModItems.SCRAP_KNIFE.get());
                        output.accept(ModItems.ASHBONE_SHIV.get());
                        output.accept(ModItems.SCAVENGER_SPEAR.get());
                        output.accept(ModItems.ALLOY_BLADE.get());
                        output.accept(ModItems.ALLOY_HAMMER.get());
                        output.accept(ModItems.NEXUS_BLADE.get());
                        output.accept(ModItems.NEXUS_ANNIHILATOR.get());

                        // Armor
                        output.accept(ModItems.GAS_MASK.get());
                        output.accept(ModItems.HAZMAT_HELMET.get());
                        output.accept(ModItems.HAZMAT_CHESTPLATE.get());
                        output.accept(ModItems.HAZMAT_LEGGINGS.get());
                        output.accept(ModItems.HAZMAT_BOOTS.get());
                        output.accept(ModItems.HIDE_WRAP.get());
                        output.accept(ModItems.ALLOY_HELMET.get());
                        output.accept(ModItems.ALLOY_CHESTPLATE.get());
                        output.accept(ModItems.ALLOY_LEGGINGS.get());
                        output.accept(ModItems.ALLOY_BOOTS.get());
                        output.accept(ModItems.NEXUS_HELMET.get());
                        output.accept(ModItems.NEXUS_CHESTPLATE.get());
                        output.accept(ModItems.NEXUS_LEGGINGS.get());
                        output.accept(ModItems.NEXUS_BOOTS.get());

                        output.accept(ModItems.BANDAGE.get());
                        output.accept(ModItems.STIM_PACK.get());
                        output.accept(ModItems.EMERGENCY_RATION.get());
                        output.accept(ModItems.HAND_WARMER.get());
                        output.accept(ModItems.SCHEMATIC_FRAGMENT.get());
                        output.accept(ModItems.RARE_TECH_SCHEMATIC.get());
                        output.accept(ModItems.SCHEMATIC_FRAGMENT_WEAPONS.get());
                        output.accept(ModItems.SCHEMATIC_FRAGMENT_ARMOR.get());
                        output.accept(ModItems.SCHEMATIC_FRAGMENT_MACHINES.get());
                        output.accept(ModItems.SCHEMATIC_FRAGMENT_MEDICAL.get());
                        output.accept(ModItems.SCHEMATIC_FRAGMENT_ENERGY.get());
                        output.accept(ModItems.WILD_BERRY.get());
                        output.accept(ModItems.SOOTCORD_BINDING.get());
                        output.accept(ModItems.ASHGRASS_FIBER.get());
                        output.accept(ModItems.ASHBONE_SHARD.get());
                        output.accept(ModItems.SCORCHED_HIDE_STRIP.get());

                        // Mob Spawn Eggs
                        output.accept(ModItems.RAD_ZOMBIE_SPAWN_EGG.get());
                        output.accept(ModItems.SCAVENGER_BANDIT_SPAWN_EGG.get());
                        output.accept(ModItems.IRRADIATED_WOLF_SPAWN_EGG.get());
                        output.accept(ModItems.ECHO_DRONE_SPAWN_EGG.get());
                        output.accept(ModItems.SCOUT_DRONE_SPAWN_EGG.get());
                        output.accept(ModItems.GLOWING_GHOUL_SPAWN_EGG.get());
                        output.accept(ModItems.ASH_WRAITH_SPAWN_EGG.get());
                        output.accept(ModItems.TOXIC_SLIME_SPAWN_EGG.get());
                        output.accept(ModItems.CITY_STALKER_SPAWN_EGG.get());
                        output.accept(ModItems.RUST_WALKER_SPAWN_EGG.get());
                        output.accept(ModItems.STEAM_WRAITH_SPAWN_EGG.get());
                        output.accept(ModItems.MUTATED_CRAWLER_SPAWN_EGG.get());
                        output.accept(ModItems.ECHO_COMPANION_DRONE_SPAWN_EGG.get());
                        output.accept(ModItems.GRIDBOUND_HUSK_SPAWN_EGG.get());
                        output.accept(ModItems.RELAY_WARDEN_SPAWN_EGG.get());
                        output.accept(ModItems.SIGNAL_LEECH_SPAWN_EGG.get());
                        output.accept(ModItems.NEXUS_NULLIFIER_SPAWN_EGG.get());
                        output.accept(ModItems.WARDEN_BOSS_SPAWN_EGG.get());
                        output.accept(ModItems.WASTELAND_SENTINEL_SPAWN_EGG.get());
                        output.accept(ModItems.CRASH_ZONE_COLOSSUS_SPAWN_EGG.get());
                        output.accept(ModItems.CRYOGENIC_OVERSEER_SPAWN_EGG.get());
                        output.accept(ModItems.INDUSTRIAL_JUGGERNAUT_SPAWN_EGG.get());
                        output.accept(ModItems.NEXUS_SCAR_AVATAR_SPAWN_EGG.get());
                        output.accept(ModItems.RADIATION_BEHEMOTH_SPAWN_EGG.get());
                        output.accept(ModItems.CITY_RUIN_STALKER_SPAWN_EGG.get());
                        output.accept(ModItems.PLAINS_WARLORD_SPAWN_EGG.get());
                        output.accept(ModItems.TOXIC_HIVE_MATRIARCH_SPAWN_EGG.get());
                        output.accept(ModItems.CORRUPTION_BLOOM_SPAWN_EGG.get());
                        output.accept(ModItems.SEVERANCE_ENGINE_SPAWN_EGG.get());
                        output.accept(ModItems.MIRROR_COMMAND_SPAWN_EGG.get());
                        output.accept(ModItems.WILD_DOG_SPAWN_EGG.get());
                        output.accept(ModItems.FERAL_HUMAN_SPAWN_EGG.get());
                        output.accept(ModItems.CRASH_SURVIVOR_SPAWN_EGG.get());

                        // Upgrades
                        output.accept(ModItems.MACHINE_UPGRADE_SPEED.get());
                        output.accept(ModItems.MACHINE_UPGRADE_EFFICIENCY.get());
                        output.accept(ModItems.MACHINE_UPGRADE_OVERCLOCK.get());

                        // Drones
                        output.accept(ModItems.SCOUT_DRONE_ITEM.get());

                        // Structure Blocks
                        output.accept(ModBlocks.DEBRIS_BLOCK_ITEM.get());
                        output.accept(ModBlocks.TOXIC_PUDDLE_ITEM.get());
                        output.accept(ModBlocks.RADIATION_BLOCK_ITEM.get());
                        output.accept(ModBlocks.DROP_POD_HULL_ITEM.get());
                        output.accept(ModBlocks.DROP_POD_GLASS_ITEM.get());
                        output.accept(ModBlocks.EMERGENCY_BUNK_ITEM.get());
                        output.accept(ModBlocks.ECHO_CACHE_ITEM.get());
                        output.accept(ModBlocks.ECHO_CRATE_ITEM.get());
                        output.accept(ModBlocks.STRUCTURE_CACHE_ITEM.get());

                        // Machines
                        output.accept(ModBlocks.HAND_RECYCLER_ITEM.get());
                        output.accept(ModBlocks.THERMAL_BURNER_ITEM.get());
                        output.accept(ModBlocks.WATER_PURIFIER_ITEM.get());
                        output.accept(ModBlocks.MICRO_GENERATOR_ITEM.get());
                        output.accept(ModBlocks.THERMAL_ARRAY_ITEM.get()); // Tier 2.5 power upgrade
                        output.accept(ModBlocks.SCRAP_DYNAMO_ITEM.get());
                        output.accept(ModBlocks.RESEARCH_LAB_ITEM.get());
                        output.accept(ModBlocks.WORKSHOP_BLOCK_ITEM.get());
                        output.accept(ModBlocks.FILTER_WORKBENCH_ITEM.get());
                        output.accept(ModBlocks.BATTERY_BANK_ITEM.get());
                        output.accept(ModBlocks.SCRAP_PRESS_ITEM.get());
                        output.accept(ModBlocks.SIGNAL_SCANNER_ITEM.get());
                        output.accept(ModBlocks.FIELD_MED_BAY_ITEM.get());
                        output.accept(ModBlocks.ATMOSPHERIC_SCRUBBER_ITEM.get());
                        output.accept(ModBlocks.AUTOFEED_HOPPER_ITEM.get());
                        output.accept(ModBlocks.RELAY_STATION_ITEM.get());
                        output.accept(ModBlocks.ITEM_PIPE_ITEM.get());
                        output.accept(ModBlocks.POWER_CABLE_ITEM.get());
                        output.accept(ModBlocks.REINFORCED_POWER_CABLE_ITEM.get());
                        output.accept(ModBlocks.HIGH_VOLTAGE_POWER_CABLE_ITEM.get());
                        output.accept(ModBlocks.ENERGY_METER_ITEM.get());
                        output.accept(ModBlocks.LOAD_DISTRIBUTOR_ITEM.get());
                        output.accept(ModBlocks.FACTORY_CONTROLLER_ITEM.get());
                        output.accept(ModBlocks.CONTAMINANT_CONDENSER_ITEM.get());

                        // Geo-Extractor Machines
                        output.accept(ModBlocks.ORE_GRINDER_ITEM.get());
                        output.accept(ModBlocks.ISOTOPE_REFINER_ITEM.get());
                        output.accept(ModBlocks.CRYSTALLINE_SYNTHESIZER_ITEM.get());
                        output.accept(ModBlocks.DEEP_CORE_MINER_ITEM.get());
                        output.accept(ModBlocks.RADIATION_CLEANSER_ITEM.get());

                        // Wasteland Hazard Blocks
                        output.accept(ModBlocks.WASTELAND_DIRT_ITEM.get());
                        output.accept(ModBlocks.WASTELAND_GRASS_BLOCK_ITEM.get());
                        output.accept(ModBlocks.ASHEN_WASTELAND_DIRT_ITEM.get());
                        output.accept(ModBlocks.BURNT_WASTELAND_SOIL_ITEM.get());
                        output.accept(ModBlocks.TOXIC_WASTELAND_GRASS_BLOCK_ITEM.get());
                        output.accept(ModBlocks.MUTATED_WASTELAND_GRASS_BLOCK_ITEM.get());
                        output.accept(ModBlocks.IRRADIATED_CRUST_ITEM.get());
                        output.accept(ModBlocks.NEXUS_CRACKED_SOIL_ITEM.get());
                        output.accept(ModBlocks.WASTELAND_STONE_ITEM.get());
                        output.accept(ModBlocks.ACIDIC_SLUDGE_ITEM.get());
                        output.accept(ModBlocks.FALLOUT_DUST_ITEM.get());
                        output.accept(ModBlocks.CONTAMINATED_SOIL_ITEM.get());
                        output.accept(ModBlocks.OIL_STAINED_CONCRETE_ITEM.get());
                        output.accept(ModBlocks.CRACKED_ASPHALT_ITEM.get());
                        output.accept(ModBlocks.CONCRETE_RUBBLE_ITEM.get());
                        output.accept(ModBlocks.RUSTED_METAL_SHEET_ITEM.get());
                        output.accept(ModBlocks.TOXIC_WASTE_BARREL_ITEM.get());
                        output.accept(ModBlocks.MUTATED_BUSH_ITEM.get());
                        output.accept(ModBlocks.WILD_BERRY_BUSH_ITEM.get());
                        output.accept(ModBlocks.RAIN_COLLECTOR_ITEM.get());
                        output.accept(ModBlocks.ASH_CAMPFIRE_ITEM.get());
                        output.accept(ModBlocks.DEAD_WOOD_LOG_ITEM.get());
                        output.accept(ModBlocks.CHARRED_WOOD_LOG_ITEM.get());

                        // Wasteland Vegetation
                        output.accept(ModBlocks.DRY_GRASS_ITEM.get());
                        output.accept(ModBlocks.DRY_TALL_GRASS_ITEM.get());
                        output.accept(ModBlocks.WASTELAND_GRASS_ITEM.get());
                        output.accept(ModBlocks.WASTELAND_TALL_GRASS_ITEM.get());
                        output.accept(ModBlocks.TOXIC_GRASS_ITEM.get());
                        output.accept(ModBlocks.TOXIC_TALL_GRASS_ITEM.get());
                        output.accept(ModBlocks.NUCLEAR_GRASS_ITEM.get());
                        output.accept(ModBlocks.NUCLEAR_TALL_GRASS_ITEM.get());
                        output.accept(ModBlocks.BURNT_GRASS_ITEM.get());
                        output.accept(ModBlocks.BURNT_TALL_GRASS_ITEM.get());
                        output.accept(ModBlocks.MUTATED_LEAVES_PURPLE_ITEM.get());
                        output.accept(ModBlocks.MUTATED_LEAVES_GRAY_ITEM.get());
                        output.accept(ModBlocks.ASH_LAYER_ITEM.get());
                        output.accept(ModBlocks.IRRADIATED_CACTUS_ITEM.get());
                        output.accept(ModBlocks.WASTELAND_REED_ITEM.get());
                        output.accept(ModBlocks.ASH_BUSH_ITEM.get());
                        output.accept(ModBlocks.NUCLEAR_FUNGUS_ITEM.get());
                        output.accept(ModBlocks.RUSTY_WHEAT_ITEM.get());
                        output.accept(ModBlocks.TOXIC_MOSS_ITEM.get());
                        output.accept(ModBlocks.BURNT_FERN_ITEM.get());
                        output.accept(ModBlocks.MUTATED_SAPLING_ITEM.get());

                        // Ground Debris
                        output.accept(ModBlocks.RUBBLE_ITEM.get());
                        output.accept(ModBlocks.CONCRETE_CHUNK_ITEM.get());
                        output.accept(ModBlocks.RUSTED_METAL_DEBRIS_ITEM.get());
                        output.accept(ModBlocks.SCATTERED_BONES_ITEM.get());
                        output.accept(ModBlocks.DEEP_ASH_ITEM.get());
                        output.accept(ModBlocks.WASTELAND_TRACE_RUBBLE_ITEM.get());
                        output.accept(ModBlocks.INDUSTRIAL_AGGREGATE_ITEM.get());
                        output.accept(ModBlocks.TOXIC_SLAGSTONE_ITEM.get());
                        output.accept(ModBlocks.IRRADIATED_SHALE_ITEM.get());
                        output.accept(ModBlocks.CRYOGENIC_FRACTURED_STONE_ITEM.get());
                        output.accept(ModBlocks.CRASH_SLAG_ITEM.get());

                        // Trace Fragments
                        output.accept(ModItems.IRON_SHARD.get());
                        output.accept(ModItems.COPPER_SHARD.get());
                        output.accept(ModItems.COAL_DUST.get());
                        output.accept(ModItems.GOLD_TRACE.get());
                        output.accept(ModItems.CRYSTAL_DUST.get());
                        output.accept(ModItems.GEM_FRAGMENT.get());
                        output.accept(ModItems.DENSE_ALLOY_CHUNK.get());
                        output.accept(ModItems.URANIUM_SHARD.get());
                        output.accept(ModItems.GOLD_CLUSTER.get());
                        output.accept(ModItems.SCRAP_IRON_BUNDLE.get());

                        // Contaminated Resources
                        output.accept(ModItems.CONTAMINATED_IRON.get());
                        output.accept(ModItems.CONTAMINATED_GOLD.get());
                        output.accept(ModItems.CHARGED_ASH_CIRCUIT.get());
                        output.accept(ModItems.BLUE_ASH_SALT.get());

                        // Endgame
                        output.accept(ModItems.NEXUS_CRYSTAL.get());
                        output.accept(ModItems.GUARDIAN_DATACORE.get());
                        output.accept(ModItems.WARDEN_ARCHIVE_CIPHER.get());
                        output.accept(ModItems.PREFALL_ARCHIVES_KEY.get());
                        output.accept(ModItems.RETURN_KEYSTONE.get());
                        output.accept(ModItems.INSTABILITY_DAMPENER.get());
                        output.accept(ModItems.RELAY_SCANNER_LENS.get());
                        output.accept(ModItems.RETURN_BEACON.get());
                        output.accept(ModBlocks.POWER_NODE_ITEM.get());
                        output.accept(ModBlocks.NEXUS_CAPACITOR_ITEM.get());
                        output.accept(ModBlocks.NEXUS_CORE_ITEM.get());

                        // Faction Profession Blocks
                        output.accept(ModBlocks.WEAPON_RACK_ITEM.get());
                        output.accept(ModBlocks.SUPPLY_CRATE_ITEM.get());
                        output.accept(ModBlocks.TRADE_COUNTER_ITEM.get());
                        output.accept(ModBlocks.SURVEY_TABLE_ITEM.get());
                        output.accept(ModBlocks.BIO_PROCESSING_STATION_ITEM.get());
                        output.accept(ModBlocks.SPORE_GARDEN_ITEM.get());

                        // Lore Data Logs
                        output.accept(ModItems.DATA_LOG_NEXUS_ORIGIN.get());
                        output.accept(ModItems.DATA_LOG_GRIDFALL_DAY.get());
                        output.accept(ModItems.DATA_LOG_ECHO_CREATION.get());
                        output.accept(ModItems.DATA_LOG_SURVIVOR_ALPHA.get());
                        output.accept(ModItems.DATA_LOG_CLIMATE_WEAPONS.get());
                        output.accept(ModItems.DATA_LOG_RADWARDEN_CHARTER.get());
                        output.accept(ModItems.DATA_LOG_CRASHBREAK_CODE.get());
                        output.accept(ModItems.DATA_LOG_SPOREBOUND_ADAPTATION.get());
                        output.accept(ModItems.DATA_LOG_FIRST_LIGHT.get());
                        output.accept(ModItems.DATA_LOG_RESEARCH_PROTOCOL.get());
                        output.accept(ModItems.DATA_LOG_SUBSTRATE_EXTRACTION.get());
                        output.accept(ModItems.DATA_LOG_RELAY_NETWORK.get());
                        output.accept(ModItems.DATA_LOG_NEXUS_SCAR.get());
                        output.accept(ModItems.DATA_LOG_BIOME_BOSSES.get());
                        output.accept(ModItems.DATA_LOG_PREFALL_WARDEN.get());
                    }).build()
    );

    public static final EchoBackendRegistryEntry<CreativeModeTab> NATIVE_MODULES_TAB = EchoBackendRegistryBridge.register(
            CREATIVE_MODE_TABS,
            "native_modules_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.EchoAshfallNativeModules"))
                    .withTabsBefore(CreativeModeTabs.BUILDING_BLOCKS)
                    .icon(() -> ModItems.PORTABLE_SIGNAL_SCANNER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> acceptNativeModuleItems(output))
                    .build()
    );

    private static void acceptNativeModuleItems(CreativeModeTab.Output output) {
        Set<String> accepted = new LinkedHashSet<>();
        for (Supplier<? extends ItemLike> item : featuredNativeModuleItems()) {
            accept(output, accepted, item.get());
        }
        nativeLoaderRegistryBackedItems().stream()
                .sorted(Comparator.comparing(ModCreativeTabs::itemSortKey))
                .forEach(item -> accept(output, accepted, item));
    }

    public static List<String> nativeModuleCreativeItemIds() {
        Set<String> itemIds = new LinkedHashSet<>();
        for (Supplier<? extends ItemLike> item : featuredNativeModuleItems()) {
            String itemId = itemSortKey(item.get());
            if (!itemId.isBlank()) {
                itemIds.add(itemId);
            }
        }
        nativeLoaderRegistryBackedItems().stream()
                .sorted(Comparator.comparing(ModCreativeTabs::itemSortKey))
                .map(ModCreativeTabs::itemSortKey)
                .filter(itemId -> !itemId.isBlank())
                .forEach(itemIds::add);
        return List.copyOf(itemIds);
    }

    public static List<String> nativeLoaderRegistryBackedCreativeItemIds() {
        return nativeLoaderRegistryBackedItems().stream()
                .sorted(Comparator.comparing(ModCreativeTabs::itemSortKey))
                .map(ModCreativeTabs::itemSortKey)
                .filter(itemId -> !itemId.isBlank())
                .toList();
    }

    public static List<String> nativeModuleCreativeFeaturedItemIds() {
        return featuredNativeModuleItems().stream()
                .map(Supplier::get)
                .map(ModCreativeTabs::itemSortKey)
                .filter(itemId -> !itemId.isBlank())
                .toList();
    }

    public static List<String> nativeModuleCreativeNamespaces() {
        return nativeModuleCreativeItemIds().stream()
                .map(ModCreativeTabs::namespace)
                .filter(namespace -> !namespace.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private static List<ItemLike> nativeLoaderRegistryBackedItems() {
        List<ItemLike> remaining = new ArrayList<>();
        EchoBackendRegistryBridge.entries(ModItems.ITEMS).forEach(holder -> remaining.add((ItemLike) holder.get()));
        EchoBackendRegistryBridge.entries(ModBlocks.BLOCK_ITEMS).forEach(holder -> remaining.add((ItemLike) holder.get()));
        BuiltInRegistries.ITEM.stream()
                .filter(ModCreativeTabs::isNativeModuleRegistryItem)
                .forEach(remaining::add);
        return remaining;
    }

    private static boolean isNativeModuleRegistryItem(Item item) {
        if (item == null) {
            return false;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return false;
        }
        String namespace = id.getNamespace();
        return EchoAshfallProtocol.MODID.equals(namespace)
                || namespace.startsWith("echo")
                || EXTRA_NATIVE_MODULE_NAMESPACES.contains(namespace);
    }

    private static List<Supplier<? extends ItemLike>> featuredNativeModuleItems() {
        return List.of(
                ModItems.FIELD_MANUAL::get,
                ModItems.PORTABLE_SIGNAL_SCANNER::get,
                ModItems.GAS_MASK::get,
                ModItems.FILTER_CARTRIDGE_BASIC::get,
                ModItems.BASIC_BATTERY::get,
                ModItems.ENERGY_CELL::get,
                ModBlocks.HAND_RECYCLER_ITEM::get,
                ModBlocks.WATER_PURIFIER_ITEM::get,
                ModBlocks.MICRO_GENERATOR_ITEM::get,
                ModBlocks.SIGNAL_SCANNER_ITEM::get,
                ModBlocks.SCRAP_PRESS_ITEM::get,
                ModBlocks.FACTORY_CONTROLLER_ITEM::get,
                ModItems.RELAY_SCANNER_LENS::get,
                ModBlocks.SURVEY_TABLE_ITEM::get,
                ModItems.NEXUS_CRYSTAL::get
        );
    }

    private static void accept(CreativeModeTab.Output output, Set<String> accepted, ItemLike item) {
        if (item == null) {
            return;
        }
        String key = itemSortKey(item);
        if (!key.isBlank() && accepted.add(key)) {
            output.accept(item);
        }
    }

    private static String itemSortKey(ItemLike item) {
        if (item == null || item.asItem() == null) {
            return "";
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(item.asItem());
        return id == null ? "" : id.toString();
    }

    private static String namespace(String itemId) {
        int separator = itemId == null ? -1 : itemId.indexOf(':');
        return separator <= 0 ? "" : itemId.substring(0, separator);
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(CREATIVE_MODE_TABS, eventBus);
    }
}
