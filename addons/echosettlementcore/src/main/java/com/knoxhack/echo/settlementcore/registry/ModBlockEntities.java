package com.knoxhack.echo.settlementcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echo.settlementcore.EchoSettlementCore;
import com.knoxhack.echo.settlementcore.block.entity.MedBayBlockEntity;
import com.knoxhack.echo.settlementcore.block.entity.OxygenRecyclerBlockEntity;
import com.knoxhack.echo.settlementcore.block.entity.PressurePumpBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    private static final Object BLOCK_ENTITIES =
        EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoSettlementCore.MODID);

    public static final EchoBackendRegistryEntry<BlockEntityType<OxygenRecyclerBlockEntity>> OXYGEN_RECYCLER =
        EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "oxygen_recycler",
            () -> new BlockEntityType<>(OxygenRecyclerBlockEntity::new, Set.of(ModBlocks.OXYGEN_RECYCLER.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<PressurePumpBlockEntity>> PRESSURE_PUMP =
        EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "pressure_pump",
            () -> new BlockEntityType<>(PressurePumpBlockEntity::new, Set.of(ModBlocks.PRESSURE_PUMP.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<MedBayBlockEntity>> MED_BAY =
        EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "med_bay",
            () -> new BlockEntityType<>(MedBayBlockEntity::new, Set.of(ModBlocks.MED_BAY.get())));

    private ModBlockEntities() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
    }
}
