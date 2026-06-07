package com.knoxhack.echoprimecore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoprimecore.EchoPrimeCore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoPrimeCore.MODID);
    private static final List<EchoBackendRegistryEntry<Block>> BLOCK_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Block> SIGNAL_ORE = ore("signal_ore", MapColor.STONE, 3.0F);
    public static final EchoBackendRegistryEntry<Block> DEEPSLATE_SIGNAL_ORE = ore("deepslate_signal_ore", MapColor.DEEPSLATE, 4.5F);
    public static final EchoBackendRegistryEntry<Block> RESONANT_CRYSTAL_ORE = ore("resonant_crystal_ore", MapColor.COLOR_CYAN, 3.5F);
    public static final EchoBackendRegistryEntry<Block> SCRAP_DEPOSIT = block("scrap_deposit", MapColor.METAL, 2.0F, SoundType.COPPER);
    public static final EchoBackendRegistryEntry<Block> RELAY_CASING = block("relay_casing", MapColor.METAL, 2.5F, SoundType.COPPER);
    public static final EchoBackendRegistryEntry<Block> CRACKED_RELAY_CASING = block("cracked_relay_casing", MapColor.METAL, 1.8F, SoundType.COPPER);
    public static final EchoBackendRegistryEntry<Block> MACHINE_CASING = block("machine_casing", MapColor.METAL, 3.0F, SoundType.METAL);
    public static final EchoBackendRegistryEntry<Block> DATA_CONDUIT = block("data_conduit", MapColor.COLOR_LIGHT_BLUE, 1.5F, SoundType.COPPER);
    public static final EchoBackendRegistryEntry<Block> SIGNAL_LAMP = block("signal_lamp", MapColor.COLOR_CYAN, 1.0F, SoundType.GLASS);
    public static final EchoBackendRegistryEntry<Block> FIELD_WORKBENCH = block("field_workbench", MapColor.WOOD, 2.5F, SoundType.WOOD);
    public static final EchoBackendRegistryEntry<Block> STORAGE_CRATE = block("storage_crate", MapColor.WOOD, 2.0F, SoundType.WOOD);
    public static final EchoBackendRegistryEntry<Block> REINFORCED_STORAGE_CRATE = block("reinforced_storage_crate", MapColor.METAL, 3.5F, SoundType.METAL);
    public static final EchoBackendRegistryEntry<Block> RELAY_BEACON = block("relay_beacon", MapColor.COLOR_CYAN, 2.0F, SoundType.AMETHYST);
    public static final EchoBackendRegistryEntry<Block> DORMANT_RELAY_CORE = block("dormant_relay_core", MapColor.COLOR_BLUE, 4.0F, SoundType.AMETHYST);
    public static final EchoBackendRegistryEntry<Block> PRIME_DATA_CONSOLE = block("prime_data_console", MapColor.METAL, 3.0F, SoundType.METAL);

    private ModBlocks() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
    }

    public static List<EchoBackendRegistryEntry<Block>> blockItems() {
        return List.copyOf(BLOCK_ITEMS);
    }

    private static EchoBackendRegistryEntry<Block> ore(String name, MapColor color, float strength) {
        return block(name, color, strength, SoundType.STONE);
    }

    private static EchoBackendRegistryEntry<Block> block(String name, MapColor color, float strength, SoundType sound) {
        EchoBackendRegistryEntry<Block> block = EchoBackendRegistryBridge.registerBlock(BLOCKS, name, Block::new,
                properties -> properties.mapColor(color).strength(strength, strength * 2.0F).sound(sound));
        BLOCK_ITEMS.add(block);
        return block;
    }
}
