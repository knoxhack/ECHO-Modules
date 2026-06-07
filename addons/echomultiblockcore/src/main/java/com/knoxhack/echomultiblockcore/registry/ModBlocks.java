package com.knoxhack.echomultiblockcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.block.MultiblockControllerBlock;
import com.knoxhack.echomultiblockcore.block.MultiblockCrateBlock;
import com.knoxhack.echomultiblockcore.block.RoboticArmBlock;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoMultiblockCore.MODID);

    public static final EchoBackendRegistryEntry<Block> MULTIBLOCK_CONTROLLER = custom(
            "multiblock_controller",
            properties -> new MultiblockControllerBlock(EchoMultiblockCore.id("industrial_assembly_line"), properties),
            p -> p.mapColor(MapColor.COLOR_CYAN).strength(4.0F, 8.0F).sound(SoundType.METAL).noOcclusion());
    public static final EchoBackendRegistryEntry<Block> SIGNAL_TOWER_CORE = custom(
            "signal_tower_core",
            properties -> new MultiblockControllerBlock(EchoMultiblockCore.id("signal_tower_tier_1"), properties),
            p -> p.mapColor(MapColor.COLOR_CYAN).strength(4.0F, 8.0F).sound(SoundType.METAL).noOcclusion());
    public static final EchoBackendRegistryEntry<Block> REINFORCED_FRAME = metal("reinforced_frame", MapColor.COLOR_GRAY);
    public static final EchoBackendRegistryEntry<Block> SIGNAL_CONDUIT = metal("signal_conduit", MapColor.COLOR_CYAN);
    public static final EchoBackendRegistryEntry<Block> POWER_BUS = metal("power_bus", MapColor.COLOR_ORANGE);
    public static final EchoBackendRegistryEntry<Block> DATA_BUS = metal("data_bus", MapColor.COLOR_LIGHT_BLUE);
    public static final EchoBackendRegistryEntry<Block> INPUT_CRATE = custom(
            "input_crate",
            properties -> new MultiblockCrateBlock(MultiblockCrateBlock.CrateKind.INPUT, properties),
            p -> p.mapColor(MapColor.WOOD).strength(2.5F, 4.0F).sound(SoundType.WOOD));
    public static final EchoBackendRegistryEntry<Block> OUTPUT_CRATE = custom(
            "output_crate",
            properties -> new MultiblockCrateBlock(MultiblockCrateBlock.CrateKind.OUTPUT, properties),
            p -> p.mapColor(MapColor.WOOD).strength(2.5F, 4.0F).sound(SoundType.WOOD));
    public static final EchoBackendRegistryEntry<Block> ROBOTIC_ARM = custom(
            "robotic_arm",
            RoboticArmBlock::new,
            p -> p.mapColor(MapColor.COLOR_GRAY).strength(3.5F, 8.0F).sound(SoundType.METAL).noOcclusion());
    public static final EchoBackendRegistryEntry<Block> AUTO_BUILDER = metal("auto_builder", MapColor.COLOR_GREEN);
    public static final EchoBackendRegistryEntry<Block> REINFORCED_MACHINE_FRAME = metal("reinforced_machine_frame", MapColor.METAL);

    public static final List<EchoBackendRegistryEntry<Block>> ALL_BLOCKS = List.of(
            MULTIBLOCK_CONTROLLER,
            SIGNAL_TOWER_CORE,
            REINFORCED_FRAME,
            SIGNAL_CONDUIT,
            POWER_BUS,
            DATA_BUS,
            INPUT_CRATE,
            OUTPUT_CRATE,
            ROBOTIC_ARM,
            AUTO_BUILDER,
            REINFORCED_MACHINE_FRAME);

    private ModBlocks() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
    }

    public static boolean isController(Block block) {
        return block == MULTIBLOCK_CONTROLLER.get() || block == SIGNAL_TOWER_CORE.get();
    }

    public static Identifier definitionFor(Block block) {
        if (block instanceof MultiblockControllerBlock controller) {
            return controller.defaultDefinitionId();
        }
        return EchoMultiblockCore.id("industrial_assembly_line");
    }

    private static EchoBackendRegistryEntry<Block> metal(String name, MapColor color) {
        return custom(name, Block::new, p -> p.mapColor(color).strength(4.0F, 8.0F).sound(SoundType.METAL));
    }

    private static EchoBackendRegistryEntry<Block> custom(
            String name,
            Function<BlockBehaviour.Properties, ? extends Block> factory,
            Function<BlockBehaviour.Properties, BlockBehaviour.Properties> propertiesFactory
    ) {
        return EchoBackendRegistryBridge.registerWithId(
                BLOCKS,
                name,
                id -> factory.apply(withId(propertiesFactory.apply(BlockBehaviour.Properties.of()), id)));
    }

    private static BlockBehaviour.Properties withId(BlockBehaviour.Properties properties, Identifier id) {
        return properties.setId(ResourceKey.create(Registries.BLOCK, id));
    }
}
