package com.knoxhack.echoorbitalremnants.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.block.entity.OrbitalMachineBlockEntity;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    private static final Object BLOCK_ENTITIES =
            EchoBackendRegistryBridge.create(Registries.BLOCK_ENTITY_TYPE, EchoOrbitalRemnants.MODID);

    public static final EchoBackendRegistryEntry<BlockEntityType<OrbitalMachineBlockEntity>> ORBITAL_MACHINE =
            EchoBackendRegistryBridge.register(BLOCK_ENTITIES, "orbital_machine",
                    () -> new BlockEntityType<>(OrbitalMachineBlockEntity::new, machineBlocks()));

    private ModBlockEntities() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCK_ENTITIES, eventBus);
    }

    private static Set<Block> machineBlocks() {
        return Set.of(
                ModBlocks.ROCKET_ASSEMBLY_FRAME.get(),
                ModBlocks.FUEL_REFINERY.get(),
                ModBlocks.OXYGEN_COMPRESSOR.get(),
                ModBlocks.HEAT_SHIELD_FABRICATOR.get(),
                ModBlocks.ORBITAL_FABRICATOR.get(),
                ModBlocks.VACUUM_SMELTER.get(),
                ModBlocks.SOLAR_RECLAIMER.get(),
                ModBlocks.SUIT_CHARGING_STATION.get(),
                ModBlocks.SIGNAL_ANALYZER.get(),
                ModBlocks.NAVIGATION_CONSOLE.get(),
                ModBlocks.STATION_LIFE_SUPPORT_CORE.get());
    }
}
