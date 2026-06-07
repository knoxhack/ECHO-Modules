package com.knoxhack.signalos.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.block.entity.SignalOsServerRackBlockEntity;
import com.knoxhack.signalos.block.entity.SignalOsTerminalBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final Object BLOCK_ENTITIES =
            EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, SignalOS.MODID);

    public static final EchoBackendRegistryEntry<BlockEntityType<SignalOsTerminalBlockEntity>> TERMINAL =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "terminal",
                    () -> new BlockEntityType<>(SignalOsTerminalBlockEntity::new,
                            Set.of(ModBlocks.TERMINAL.get(), ModBlocks.WORKSTATION.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<SignalOsServerRackBlockEntity>> SERVER_RACK =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "server_rack",
                    () -> new BlockEntityType<>(SignalOsServerRackBlockEntity::new,
                            Set.of(ModBlocks.SERVER_RACK.get())));

    private ModBlockEntities() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
    }
}
