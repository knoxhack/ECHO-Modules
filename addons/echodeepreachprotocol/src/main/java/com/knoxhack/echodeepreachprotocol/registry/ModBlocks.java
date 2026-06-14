package com.knoxhack.echodeepreachprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echodeepreachprotocol.EchoDeepReachProtocol;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Block registry for ECHO: Deep Reach Protocol.
 * Registers depth-zone terrain and decorative blocks for flooded caverns and abyssal ruins.
 */
public final class ModBlocks {
    public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoDeepReachProtocol.MODID);
    public static final Object BLOCK_ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoDeepReachProtocol.MODID);

    public static final EchoBackendRegistryEntry<Block> ABYSSAL_STONE = registerSimpleBlock("abyssal_stone",
            p -> p.mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops());
    public static final EchoBackendRegistryEntry<BlockItem> ABYSSAL_STONE_ITEM = registerSimpleBlockItem("abyssal_stone", ABYSSAL_STONE);

    public static final EchoBackendRegistryEntry<Block> LATTICE_CRYSTAL = registerSimpleBlock("lattice_crystal",
            p -> p.mapColor(MapColor.COLOR_CYAN)
                    .strength(1.0f)
                    .sound(SoundType.AMETHYST)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 12)
                    .noOcclusion());
    public static final EchoBackendRegistryEntry<BlockItem> LATTICE_CRYSTAL_ITEM = registerSimpleBlockItem("lattice_crystal", LATTICE_CRYSTAL);

    public static final EchoBackendRegistryEntry<Block> THERMAL_VENT = registerSimpleBlock("thermal_vent",
            p -> p.mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.2f, 6.0f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .lightLevel(s -> 10));
    public static final EchoBackendRegistryEntry<BlockItem> THERMAL_VENT_ITEM = registerSimpleBlockItem("thermal_vent", THERMAL_VENT);

    public static final EchoBackendRegistryEntry<Block> SUNKEN_SAND = registerSimpleBlock("sunken_sand",
            p -> p.mapColor(MapColor.SAND)
                    .strength(0.5f)
                    .sound(SoundType.SAND));
    public static final EchoBackendRegistryEntry<BlockItem> SUNKEN_SAND_ITEM = registerSimpleBlockItem("sunken_sand", SUNKEN_SAND);

    private ModBlocks() {
    }

    private static EchoBackendRegistryEntry<Block> registerSimpleBlock(String name,
            Function<BlockBehaviour.Properties, BlockBehaviour.Properties> propertiesFactory) {
        return EchoBackendRegistryBridge.registerWithId(BLOCKS, name,
                id -> new Block(withId(propertiesFactory.apply(BlockBehaviour.Properties.of()), id)));
    }

    private static EchoBackendRegistryEntry<BlockItem> registerSimpleBlockItem(String name,
            Supplier<? extends Block> block) {
        return EchoBackendRegistryBridge.registerWithId(BLOCK_ITEMS, name,
                id -> new BlockItem(block.get(), new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM, id))));
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
        EchoBackendRegistryBridge.registerEventBus(BLOCK_ITEMS, eventBus);
    }

    private static BlockBehaviour.Properties withId(BlockBehaviour.Properties properties, Identifier id) {
        return properties.setId(ResourceKey.create(Registries.BLOCK, id));
    }
}
