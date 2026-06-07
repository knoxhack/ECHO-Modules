package com.knoxhack.echoritualcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoritualcore.EchoRitualCore;
import com.knoxhack.echoritualcore.block.entity.BasicAltarBlockEntity;
import com.knoxhack.echoritualcore.block.entity.OfferingPedestalBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    private static final Object BLOCK_ENTITIES =
            EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoRitualCore.MODID);

    public static final EchoBackendRegistryEntry<BlockEntityType<BasicAltarBlockEntity>> BASIC_ALTAR =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "basic_altar", () -> new BlockEntityType<>(
                    BasicAltarBlockEntity::new,
                    Set.of((Block) ModBlocks.BASIC_ALTAR.get())));

    public static final EchoBackendRegistryEntry<BlockEntityType<OfferingPedestalBlockEntity>> OFFERING_PEDESTAL =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "offering_pedestal", () -> new BlockEntityType<>(
                    OfferingPedestalBlockEntity::new,
                    Set.of((Block) ModBlocks.OFFERING_PEDESTAL.get())));

    private ModBlockEntities() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
    }
}
