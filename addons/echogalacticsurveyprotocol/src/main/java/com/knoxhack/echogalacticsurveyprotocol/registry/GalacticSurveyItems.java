package com.knoxhack.echogalacticsurveyprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echogalacticsurveyprotocol.EchoGalacticSurveyProtocol;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class GalacticSurveyItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoGalacticSurveyProtocol.MODID);

    public static final List<EchoBackendRegistryEntry<BlockItem>> BLOCK_ITEMS =
            GalacticSurveyBlocks.ALL_BLOCKS.stream().map(GalacticSurveyItems::blockItem).toList();

    public static final EchoBackendRegistryEntry<Item> STARTER_PROBE = simple(
            "starter_probe", properties -> properties.stacksTo(4).rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> SURVEY_BEACON = simple(
            "survey_beacon", properties -> properties.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> FUEL_CANISTER = simple(
            "fuel_canister", properties -> properties.stacksTo(8));
    public static final EchoBackendRegistryEntry<Item> NAVIGATION_CORE = simple(
            "navigation_core", properties -> properties.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> BURNED_NAVIGATION_CORE = simple(
            "burned_navigation_core", properties -> properties.stacksTo(16));
    public static final EchoBackendRegistryEntry<Item> ORBITAL_SCRAP = simple(
            "orbital_scrap", properties -> properties.stacksTo(64));
    public static final EchoBackendRegistryEntry<Item> CATALOG_BADGE = simple(
            "catalog_badge", properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> LONG_RANGE_PROBE = simple(
            "long_range_probe", properties -> properties.stacksTo(4).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> RADIATION_SHIELDING = simple(
            "radiation_shielding", properties -> properties.stacksTo(8).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> ROUTE_STABILIZER = simple(
            "route_stabilizer", properties -> properties.stacksTo(8).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> DEEP_SPACE_LENS = simple(
            "deep_space_lens", properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> STELLAR_CHART_FRAGMENT = simple(
            "stellar_chart_fragment", properties -> properties.stacksTo(16).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> SURVEY_ARRAY_KEY = simple(
            "survey_array_key", properties -> properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final EchoBackendRegistryEntry<Item> DEPOT_MANIFEST = simple(
            "depot_manifest", properties -> properties.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> FUEL_QUALITY_SAMPLE = simple(
            "fuel_quality_sample", properties -> properties.stacksTo(32));
    public static final EchoBackendRegistryEntry<Item> GALACTIC_SURVEY_BADGE = simple(
            "galactic_survey_badge", properties -> properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    public static final List<EchoBackendRegistryEntry<Item>> CORE_ITEMS = List.of(
            STARTER_PROBE,
            SURVEY_BEACON,
            FUEL_CANISTER,
            NAVIGATION_CORE,
            BURNED_NAVIGATION_CORE,
            ORBITAL_SCRAP,
            CATALOG_BADGE,
            LONG_RANGE_PROBE,
            RADIATION_SHIELDING,
            ROUTE_STABILIZER,
            DEEP_SPACE_LENS,
            STELLAR_CHART_FRAGMENT,
            SURVEY_ARRAY_KEY,
            DEPOT_MANIFEST,
            FUEL_QUALITY_SAMPLE,
            GALACTIC_SURVEY_BADGE
    );

    private GalacticSurveyItems() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
    }

    public static List<String> coreItemIds() {
        return CORE_ITEMS.stream().map(entry -> entry.id().getPath()).toList();
    }

    public static List<String> blockItemIds() {
        return BLOCK_ITEMS.stream().map(entry -> entry.id().getPath()).toList();
    }

    private static EchoBackendRegistryEntry<Item> simple(String name, UnaryOperator<Item.Properties> properties) {
        return item(name, Item::new, properties);
    }

    private static EchoBackendRegistryEntry<BlockItem> blockItem(EchoBackendRegistryEntry<? extends Block> block) {
        return EchoBackendRegistryBridge.registerWithId(ITEMS, block.id().getPath(), id ->
                new BlockItem(block.get(), new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, id))
                        .useBlockDescriptionPrefix()));
    }

    private static EchoBackendRegistryEntry<Item> item(String name,
            Function<Item.Properties, Item> factory,
            UnaryOperator<Item.Properties> properties) {
        return EchoBackendRegistryBridge.registerWithId(ITEMS, name, id ->
                factory.apply(properties.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)))));
    }
}
