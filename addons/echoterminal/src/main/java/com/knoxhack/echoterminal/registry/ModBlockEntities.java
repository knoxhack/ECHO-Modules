package com.knoxhack.echoterminal.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.block.entity.EchoTerminalBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final Object BLOCK_ENTITIES =
            EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoTerminal.MODID);

    public static final EchoBackendRegistryEntry<BlockEntityType<EchoTerminalBlockEntity>> ECHO_TERMINAL =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "echo_terminal",
                    () -> new BlockEntityType<>(EchoTerminalBlockEntity::new, Set.of(ModBlocks.ECHO_TERMINAL_BLOCK.get())));

    private ModBlockEntities() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
    }
}
