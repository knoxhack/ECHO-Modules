package com.knoxhack.echoskyrelayprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoskyrelayprotocol.EchoSkyRelayProtocol;
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

public final class SkyRelayItems {
    public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoSkyRelayProtocol.MODID);

    public static final List<EchoBackendRegistryEntry<BlockItem>> BLOCK_ITEMS =
            SkyRelayBlocks.ALL_BLOCKS.stream().map(SkyRelayItems::blockItem).toList();

    public static final EchoBackendRegistryEntry<Item> OPERATOR_BADGE = simple(
            "operator_badge", properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> RELAY_ANCHOR_KEY = simple(
            "relay_anchor_key", properties -> properties.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> SKY_FRAGMENT_CHART = simple(
            "sky_fragment_chart", properties -> properties.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> CHARGED_RELAY_COIL = simple(
            "charged_relay_coil", properties -> properties.stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> RELAY_ALLOY_PLATE = simple(
            "relay_alloy_plate", properties -> properties.stacksTo(64));
    public static final EchoBackendRegistryEntry<Item> SIGNAL_CALIBRATION_CHIP = simple(
            "signal_calibration_chip", properties -> properties.stacksTo(32).rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> ATMOSPHERIC_FILTER = simple(
            "atmospheric_filter", properties -> properties.stacksTo(16));
    public static final EchoBackendRegistryEntry<Item> STORMPROOF_WRAP = simple(
            "stormproof_wrap", properties -> properties.stacksTo(16));
    public static final EchoBackendRegistryEntry<Item> RELAY_FIRMWARE_SHARD = simple(
            "relay_firmware_shard", properties -> properties.stacksTo(32).rarity(Rarity.UNCOMMON));
    public static final EchoBackendRegistryEntry<Item> STABILIZED_PLATFORM_CORE = simple(
            "stabilized_platform_core", properties -> properties.stacksTo(4).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> FRAGMENT_ACCESS_CIPHER = simple(
            "fragment_access_cipher", properties -> properties.stacksTo(16).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> STATIC_FILAMENT = simple(
            "static_filament", properties -> properties.stacksTo(32).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> ORBITAL_ALLOY_SCRAP = simple(
            "orbital_alloy_scrap", properties -> properties.stacksTo(32).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> SATELLITE_LENS = simple(
            "satellite_lens", properties -> properties.stacksTo(8).rarity(Rarity.RARE));
    public static final EchoBackendRegistryEntry<Item> ECHO_CRYSTAL_CHARGE = simple(
            "echo_crystal_charge", properties -> properties.stacksTo(8).rarity(Rarity.EPIC).fireResistant());
    public static final EchoBackendRegistryEntry<Item> SKY_RELAY_BADGE = simple(
            "sky_relay_badge", properties -> properties.stacksTo(1).rarity(Rarity.EPIC).fireResistant());

    public static final List<EchoBackendRegistryEntry<Item>> CORE_ITEMS = List.of(
            OPERATOR_BADGE,
            RELAY_ANCHOR_KEY,
            SKY_FRAGMENT_CHART,
            CHARGED_RELAY_COIL,
            RELAY_ALLOY_PLATE,
            SIGNAL_CALIBRATION_CHIP,
            ATMOSPHERIC_FILTER,
            STORMPROOF_WRAP,
            RELAY_FIRMWARE_SHARD,
            STABILIZED_PLATFORM_CORE,
            FRAGMENT_ACCESS_CIPHER,
            STATIC_FILAMENT,
            ORBITAL_ALLOY_SCRAP,
            SATELLITE_LENS,
            ECHO_CRYSTAL_CHARGE,
            SKY_RELAY_BADGE
    );

    private SkyRelayItems() {
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
