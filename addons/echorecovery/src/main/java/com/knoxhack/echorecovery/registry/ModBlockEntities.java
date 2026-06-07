package com.knoxhack.echorecovery.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.block.entity.GraveBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final Object BLOCK_ENTITIES = EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoRecovery.MODID);

    @SuppressWarnings("unchecked")
    public static final EchoBackendRegistryEntry<BlockEntityType<GraveBlockEntity>> GRAVE = EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "grave",
        () -> new BlockEntityType<>(GraveBlockEntity::new, Set.copyOf(ModBlocks.blockItems().stream().map(b -> (Block) b.get()).toList())));

    private ModBlockEntities() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
    }
}
