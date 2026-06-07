package com.knoxhack.echomultiblockcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockCrateBlockEntity;
import com.knoxhack.echomultiblockcore.block.entity.RoboticArmBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    private static final Object BLOCK_ENTITIES =
            EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoMultiblockCore.MODID);

    public static final EchoBackendRegistryEntry<BlockEntityType<MultiblockControllerBlockEntity>> CONTROLLER =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "controller", () -> new BlockEntityType<>(
                    MultiblockControllerBlockEntity::new,
                    Set.of(ModBlocks.MULTIBLOCK_CONTROLLER.get(), ModBlocks.SIGNAL_TOWER_CORE.get())));
    public static final EchoBackendRegistryEntry<BlockEntityType<MultiblockCrateBlockEntity>> CRATE =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "crate", () -> new BlockEntityType<>(
                    MultiblockCrateBlockEntity::new,
                    Set.of((Block) ModBlocks.INPUT_CRATE.get(), (Block) ModBlocks.OUTPUT_CRATE.get())));
    public static final EchoBackendRegistryEntry<BlockEntityType<RoboticArmBlockEntity>> ROBOTIC_ARM =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "robotic_arm", () -> new BlockEntityType<>(
                    RoboticArmBlockEntity::new,
                    Set.of((Block) ModBlocks.ROBOTIC_ARM.get())));

    private ModBlockEntities() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
    }
}
