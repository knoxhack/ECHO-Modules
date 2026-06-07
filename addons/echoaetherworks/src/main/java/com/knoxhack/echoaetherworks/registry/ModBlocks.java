package com.knoxhack.echoaetherworks.registry;

import net.minecraft.core.registries.Registries;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoaetherworks.EchoAetherWorks;
import com.knoxhack.echoaetherworks.block.AetherCellBlock;
import com.knoxhack.echoaetherworks.block.AetherCondenserBlock;
import com.knoxhack.echoaetherworks.block.AetherConduitBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final Object BLOCKS = EchoBackendRegistryBridge.create(Registries.BLOCK, EchoAetherWorks.MODID);
    private static final List<EchoBackendRegistryEntry<Block>> BLOCK_ITEMS = new ArrayList<>();

    public static final EchoBackendRegistryEntry<Block> AETHER_CONDENSER =
            tracked(EchoBackendRegistryBridge.registerBlock(BLOCKS, "aether_condenser", AetherCondenserBlock::new, machineProps()));
    public static final EchoBackendRegistryEntry<Block> AETHER_CELL =
            tracked(EchoBackendRegistryBridge.registerBlock(BLOCKS, "aether_cell", AetherCellBlock::new, cellProps()));
    public static final EchoBackendRegistryEntry<Block> AETHER_CONDUIT =
            tracked(EchoBackendRegistryBridge.registerBlock(BLOCKS, "aether_conduit", AetherConduitBlock::new, conduitProps()));

    private ModBlocks() {
    }

    public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
   }

    public static List<EchoBackendRegistryEntry<Block>> blockItems() {
        return List.copyOf(BLOCK_ITEMS);
    }

    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> machineProps() {
        return p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(3.0F, 8.0F)
                .sound(SoundType.AMETHYST).requiresCorrectToolForDrops();
    }

    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> cellProps() {
        return p -> p.mapColor(MapColor.COLOR_CYAN).strength(2.4F, 7.0F)
                .sound(SoundType.AMETHYST).requiresCorrectToolForDrops();
    }

    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> conduitProps() {
        return p -> p.mapColor(MapColor.COLOR_PURPLE).strength(1.4F, 4.0F).sound(SoundType.COPPER);
    }

    private static EchoBackendRegistryEntry<Block> tracked(EchoBackendRegistryEntry<Block> block) {
        BLOCK_ITEMS.add(block);
        return block;
    }
}
