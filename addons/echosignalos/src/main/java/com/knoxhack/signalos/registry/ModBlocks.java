package com.knoxhack.signalos.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.block.SignalOsNetworkRelayBlock;
import com.knoxhack.signalos.block.SignalOsServerRackBlock;
import com.knoxhack.signalos.block.SignalOsTerminalBlock;
import com.knoxhack.signalos.item.SignalOsDataDriveItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final Object BLOCKS = EchoBackendRegistryBridge.create(Registries.BLOCK, SignalOS.MODID);
    public static final Object BLOCK_ITEMS = EchoBackendRegistryBridge.create(Registries.ITEM, SignalOS.MODID);

    public static final EchoBackendRegistryEntry<Block> TERMINAL = EchoBackendRegistryBridge.register(BLOCKS,
            "terminal",
            () -> new SignalOsTerminalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 7)));

    public static final EchoBackendRegistryEntry<Block> WORKSTATION = EchoBackendRegistryBridge.register(BLOCKS,
            "workstation",
            () -> new SignalOsTerminalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 9)));

    public static final EchoBackendRegistryEntry<Block> SERVER_RACK = EchoBackendRegistryBridge.register(BLOCKS,
            "server_rack",
            () -> new SignalOsServerRackBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(4.0F, 8.0F)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 4)));

    public static final EchoBackendRegistryEntry<Block> NETWORK_RELAY = EchoBackendRegistryBridge.register(BLOCKS,
            "network_relay",
            () -> new SignalOsNetworkRelayBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(2.5F, 5.0F)
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 10)));

    public static final EchoBackendRegistryEntry<BlockItem> TERMINAL_ITEM =
            EchoBackendRegistryBridge.register(BLOCK_ITEMS, "terminal",
                    () -> new BlockItem(TERMINAL.get(), new Item.Properties()));
    public static final EchoBackendRegistryEntry<BlockItem> WORKSTATION_ITEM =
            EchoBackendRegistryBridge.register(BLOCK_ITEMS, "workstation",
                    () -> new BlockItem(WORKSTATION.get(), new Item.Properties()));
    public static final EchoBackendRegistryEntry<BlockItem> SERVER_RACK_ITEM =
            EchoBackendRegistryBridge.register(BLOCK_ITEMS, "server_rack",
                    () -> new BlockItem(SERVER_RACK.get(), new Item.Properties()));
    public static final EchoBackendRegistryEntry<BlockItem> NETWORK_RELAY_ITEM =
            EchoBackendRegistryBridge.register(BLOCK_ITEMS, "network_relay",
                    () -> new BlockItem(NETWORK_RELAY.get(), new Item.Properties()));
    public static final EchoBackendRegistryEntry<SignalOsDataDriveItem> DATA_DRIVE =
            EchoBackendRegistryBridge.register(BLOCK_ITEMS, "data_drive",
                    () -> new SignalOsDataDriveItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    private ModBlocks() {
    }

    public static boolean isComputerAccessBlock(Block block) {
        return block == TERMINAL.get() || block == WORKSTATION.get();
    }

    public static boolean isComputerNetworkBlock(Block block) {
        return isComputerAccessBlock(block) || block == SERVER_RACK.get() || block == NETWORK_RELAY.get();
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
        EchoBackendRegistryBridge.registerEventBus(BLOCK_ITEMS, eventBus);
    }
}
