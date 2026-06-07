package com.knoxhack.echoritualcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoritualcore.EchoRitualCore;
import com.knoxhack.echoritualcore.block.BasicAltarBlock;
import com.knoxhack.echoritualcore.block.OfferingPedestalBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoRitualCore.MODID);
    private static final List<EchoBackendRegistryEntry<Block>> BLOCK_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Block> BASIC_ALTAR = tracked(EchoBackendRegistryBridge.registerBlock(BLOCKS, "basic_altar", BasicAltarBlock::new, altarProps()));
    public static final EchoBackendRegistryEntry<Block> RITUAL_BASIN = tracked(EchoBackendRegistryBridge.registerSimpleBlock(BLOCKS, "ritual_basin", altarProps()));
    public static final EchoBackendRegistryEntry<Block> OFFERING_PEDESTAL = tracked(EchoBackendRegistryBridge.registerBlock(BLOCKS, "offering_pedestal", OfferingPedestalBlock::new, altarProps()));
    public static final EchoBackendRegistryEntry<Block> RUNE_CIRCLE = tracked(EchoBackendRegistryBridge.registerSimpleBlock(BLOCKS, "rune_circle", circuitProps()));
    public static final EchoBackendRegistryEntry<Block> STABILITY_PYLON = tracked(EchoBackendRegistryBridge.registerSimpleBlock(BLOCKS, "stability_pylon", altarProps()));
    public static final EchoBackendRegistryEntry<Block> MOON_DIAL = tracked(EchoBackendRegistryBridge.registerSimpleBlock(BLOCKS, "moon_dial", altarProps()));
    public static final EchoBackendRegistryEntry<Block> WEATHER_ANCHOR = tracked(EchoBackendRegistryBridge.registerSimpleBlock(BLOCKS, "weather_anchor", altarProps()));
    public static final EchoBackendRegistryEntry<Block> CORRUPTED_ALTAR = tracked(EchoBackendRegistryBridge.registerSimpleBlock(BLOCKS, "corrupted_altar", corruptedProps()));

    private ModBlocks() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
    }

    public static List<EchoBackendRegistryEntry<Block>> blockItems() {
        return List.copyOf(BLOCK_ITEMS);
    }

    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> altarProps() {
        return p -> p.mapColor(MapColor.COLOR_PURPLE).strength(3.0F, 8.0F).sound(SoundType.AMETHYST).requiresCorrectToolForDrops();
    }

    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> circuitProps() {
        return p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.2F, 3.0F).sound(SoundType.AMETHYST);
    }

    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> corruptedProps() {
        return p -> p.mapColor(MapColor.COLOR_BLACK).strength(4.0F, 12.0F).sound(SoundType.SCULK).requiresCorrectToolForDrops();
    }

    private static EchoBackendRegistryEntry<Block> tracked(EchoBackendRegistryEntry<Block> block) {
        BLOCK_ITEMS.add(block);
        return block;
    }
}
