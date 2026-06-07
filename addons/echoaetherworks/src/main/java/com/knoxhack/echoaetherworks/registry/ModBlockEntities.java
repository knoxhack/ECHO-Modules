package com.knoxhack.echoaetherworks.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoaetherworks.EchoAetherWorks;
import com.knoxhack.echoaetherworks.block.entity.AetherCellBlockEntity;
import com.knoxhack.echoaetherworks.block.entity.AetherCondenserBlockEntity;
import com.knoxhack.echoaetherworks.block.entity.AetherConduitBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    private static final Object BLOCK_ENTITIES =
            EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoAetherWorks.MODID);

    public static final EchoBackendRegistryEntry<BlockEntityType<AetherCondenserBlockEntity>> AETHER_CONDENSER =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "aether_condenser", () -> new BlockEntityType<>(
                    AetherCondenserBlockEntity::new,
                    Set.of((Block) ModBlocks.AETHER_CONDENSER.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<AetherCellBlockEntity>> AETHER_CELL =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "aether_cell", () -> new BlockEntityType<>(
                    AetherCellBlockEntity::new,
                    Set.of((Block) ModBlocks.AETHER_CELL.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<AetherConduitBlockEntity>> AETHER_CONDUIT =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "aether_conduit", () -> new BlockEntityType<>(
                    AetherConduitBlockEntity::new,
                    Set.of((Block) ModBlocks.AETHER_CONDUIT.get())));

    private ModBlockEntities() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
    }
}
