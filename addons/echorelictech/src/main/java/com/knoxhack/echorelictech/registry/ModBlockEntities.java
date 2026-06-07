package com.knoxhack.echorelictech.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echorelictech.EchoRelicTech;
import com.knoxhack.echorelictech.block.entity.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

public final class ModBlockEntities {
    private static final Object BLOCK_ENTITIES =
        EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoRelicTech.MODID);

    public static final EchoBackendRegistryEntry<BlockEntityType<RelicAnalyzerBlockEntity>> RELIC_ANALYZER =
        EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "relic_analyzer", () -> new BlockEntityType<>(RelicAnalyzerBlockEntity::new,
            Set.of((Block) ModBlocks.RELIC_ANALYZER.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<PrototypeWorkbenchBlockEntity>> PROTOTYPE_WORKBENCH =
        EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "prototype_workbench", () -> new BlockEntityType<>(PrototypeWorkbenchBlockEntity::new,
            Set.of((Block) ModBlocks.PROTOTYPE_WORKBENCH.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<ContainmentLockerBlockEntity>> CONTAINMENT_LOCKER =
        EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "containment_locker", () -> new BlockEntityType<>(ContainmentLockerBlockEntity::new,
            Set.of((Block) ModBlocks.CONTAINMENT_LOCKER.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<NullBatteryDockBlockEntity>> NULL_BATTERY_DOCK =
        EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "null_battery_dock", () -> new BlockEntityType<>(NullBatteryDockBlockEntity::new,
            Set.of((Block) ModBlocks.NULL_BATTERY_DOCK.get())));

    private ModBlockEntities() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
    }
}
