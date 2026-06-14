package com.knoxhack.echo.settlementcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echo.settlementcore.EchoSettlementCore;
import com.knoxhack.echo.settlementcore.block.AirlockBlock;
import com.knoxhack.echo.settlementcore.block.CargoLockerBlock;
import com.knoxhack.echo.settlementcore.block.DiversQuartersBlock;
import com.knoxhack.echo.settlementcore.block.HabitatBlock;
import com.knoxhack.echo.settlementcore.block.MedBayBlock;
import com.knoxhack.echo.settlementcore.block.OxygenRecyclerBlock;
import com.knoxhack.echo.settlementcore.block.PressurePumpBlock;
import com.knoxhack.echo.settlementcore.block.SubmersibleDockBlock;
import com.knoxhack.echo.settlementcore.block.WorkshopBlock;
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
    public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoSettlementCore.MODID);

    public static final EchoBackendRegistryEntry<Block> AIRLOCK = registerWithId("airlock", id -> new AirlockBlock(habitatProperties(id, MapColor.COLOR_GRAY)));
    public static final EchoBackendRegistryEntry<Block> OXYGEN_RECYCLER = registerWithId("oxygen_recycler", id -> new OxygenRecyclerBlock(habitatProperties(id, MapColor.COLOR_LIGHT_BLUE)));
    public static final EchoBackendRegistryEntry<Block> PRESSURE_PUMP = registerWithId("pressure_pump", id -> new PressurePumpBlock(habitatProperties(id, MapColor.COLOR_BLUE)));
    public static final EchoBackendRegistryEntry<Block> WORKSHOP = registerWithId("workshop", id -> new WorkshopBlock(habitatProperties(id, MapColor.TERRACOTTA_YELLOW)));
    public static final EchoBackendRegistryEntry<Block> MED_BAY = registerWithId("med_bay", id -> new MedBayBlock(habitatProperties(id, MapColor.COLOR_GREEN)));
    public static final EchoBackendRegistryEntry<Block> DIVERS_QUARTERS = registerWithId("divers_quarters", id -> new DiversQuartersBlock(habitatProperties(id, MapColor.WATER)));
    public static final EchoBackendRegistryEntry<Block> CARGO_LOCKER = registerWithId("cargo_locker", id -> new CargoLockerBlock(habitatProperties(id, MapColor.WOOD)));
    public static final EchoBackendRegistryEntry<Block> SUBMERSIBLE_DOCK = registerWithId("submersible_dock", id -> new SubmersibleDockBlock(habitatProperties(id, MapColor.COLOR_BLACK)));

    public static final EchoBackendRegistryEntry<Block> DEEP_MINER_STATION = registerWithId("deep_miner_station", id -> new HabitatBlock(habitatProperties(id, MapColor.COLOR_BLACK)));
    public static final EchoBackendRegistryEntry<Block> PRESSURE_MECHANIC_STATION = registerWithId("pressure_mechanic_station", id -> new HabitatBlock(habitatProperties(id, MapColor.COLOR_GRAY)));
    public static final EchoBackendRegistryEntry<Block> XENO_BIOLOGIST_LAB = registerWithId("xenobiologist_lab", id -> new HabitatBlock(habitatProperties(id, MapColor.COLOR_PURPLE)));

    public static final List<EchoBackendRegistryEntry<Block>> ALL_BLOCKS = List.of(
        AIRLOCK,
        OXYGEN_RECYCLER,
        PRESSURE_PUMP,
        WORKSHOP,
        MED_BAY,
        DIVERS_QUARTERS,
        CARGO_LOCKER,
        SUBMERSIBLE_DOCK,
        DEEP_MINER_STATION,
        PRESSURE_MECHANIC_STATION,
        XENO_BIOLOGIST_LAB
    );

    private ModBlocks() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
    }

    private static EchoBackendRegistryEntry<Block> registerWithId(String name, Function<Identifier, Block> factory) {
        return EchoBackendRegistryBridge.registerWithId(BLOCKS, name, factory);
    }

    private static BlockBehaviour.Properties habitatProperties(Identifier id, MapColor color) {
        return BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, id))
            .mapColor(color)
            .strength(3.0F, 6.0F)
            .sound(SoundType.METAL)
            .noOcclusion();
    }
}
