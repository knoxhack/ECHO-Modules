package com.knoxhack.echopowergrid.registry;

import com.knoxhack.echopowergrid.EchoPowerGrid;
import com.knoxhack.echopowergrid.api.GeneratorType;
import com.knoxhack.echopowergrid.block.BatteryBlock;
import com.knoxhack.echopowergrid.block.BreakerBlock;
import com.knoxhack.echopowergrid.block.CableBlock;
import com.knoxhack.echopowergrid.block.GeneratorBlock;
import com.knoxhack.echopowergrid.block.MeterBlock;
import com.knoxhack.echopowergrid.block.SubstationBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    private static final List<NativeRegistryHolder<Block>> BLOCK_ITEMS = new ArrayList<>();

    // Generators
    public static final NativeRegistryHolder<Block> HAND_CRANK_GENERATOR = registerGenerator("hand_crank_generator", 5, 100, GeneratorType.HAND_CRANK);
    public static final NativeRegistryHolder<Block> SCRAP_BURNER_GENERATOR = registerGenerator("scrap_burner_generator", 40, 2000, GeneratorType.FUEL_BURNER);
    public static final NativeRegistryHolder<Block> SOLAR_PANEL = registerGenerator("solar_panel", 10, 200, GeneratorType.SOLAR);
    public static final NativeRegistryHolder<Block> REINFORCED_SOLAR_PANEL = registerGenerator("reinforced_solar_panel", 30, 900, GeneratorType.SOLAR);
    public static final NativeRegistryHolder<Block> BIOFUEL_GENERATOR = registerGenerator("biofuel_generator", 80, 4000, GeneratorType.FUEL_BURNER);
    public static final NativeRegistryHolder<Block> CREATIVE_POWER_SOURCE = registerGenerator("creative_power_source", Long.MAX_VALUE / 4, Long.MAX_VALUE / 4, GeneratorType.CREATIVE);

    // Storage
    public static final NativeRegistryHolder<Block> SMALL_BATTERY_BANK = registerStorage("small_battery_bank", 20000, 100, 100);
    public static final NativeRegistryHolder<Block> MEDIUM_BATTERY_BANK = registerStorage("medium_battery_bank", 80000, 400, 400);
    public static final NativeRegistryHolder<Block> FIELD_BATTERY_BANK = registerStorage("field_battery_bank", 160000, 800, 800);
    public static final NativeRegistryHolder<Block> INDUSTRIAL_BATTERY_BANK = registerStorage("industrial_battery_bank", 320000, 1200, 1200);

    // Cables
    public static final NativeRegistryHolder<Block> LOW_VOLTAGE_CABLE = registerCable("low_voltage_cable", 100);
    public static final NativeRegistryHolder<Block> INDUSTRIAL_CABLE = registerCable("industrial_cable", 500);
    public static final NativeRegistryHolder<Block> HIGH_VOLTAGE_CABLE = registerCable("high_voltage_cable", 1200);

    // Control
    public static final NativeRegistryHolder<Block> OUTPOST_SUBSTATION = registerSubstation("outpost_substation");
    public static final NativeRegistryHolder<Block> RELAY_SUBSTATION = registerSubstation("relay_substation");
    public static final NativeRegistryHolder<Block> FACTORY_SUBSTATION = registerSubstation("factory_substation");
    public static final NativeRegistryHolder<Block> NEXUS_STABILIZER_COUPLER = registerSubstation("nexus_stabilizer_coupler");
    public static final NativeRegistryHolder<Block> EMERGENCY_BREAKER = registerBreaker("emergency_breaker");
    public static final NativeRegistryHolder<Block> POWER_METER = registerMeter("power_meter");

    // Creative/Test
    public static final NativeRegistryHolder<Block> CREATIVE_POWER_SINK = registerConsumer("creative_power_sink", Long.MAX_VALUE / 4);
    public static final NativeRegistryHolder<Block> TEST_POWER_CONSUMER = registerConsumer("test_power_consumer", 20);

    private ModBlocks() {}

    public static void register() {
    }

    public static List<NativeRegistryHolder<Block>> blockItems() {
        return List.copyOf(BLOCK_ITEMS);
    }

    public static boolean isPowerNode(BlockState state) {
        return state.getBlock() instanceof GeneratorBlock
            || state.getBlock() instanceof BatteryBlock
            || state.getBlock() instanceof CableBlock
            || state.getBlock() instanceof SubstationBlock
            || state.getBlock() instanceof BreakerBlock
            || state.getBlock() instanceof MeterBlock
            || state.getBlock() instanceof com.knoxhack.echopowergrid.block.ConsumerBlock;
    }

    public static long getTransferLimit(BlockState state) {
        if (state.getBlock() instanceof CableBlock cable) {
            return cable.getTransferLimit();
        }
        if (state.is(FACTORY_SUBSTATION.get())) return 3000;
        if (state.is(RELAY_SUBSTATION.get())) return 2000;
        if (state.is(NEXUS_STABILIZER_COUPLER.get())) return 1600;
        if (state.getBlock() instanceof SubstationBlock) return 500;
        if (state.getBlock() instanceof BreakerBlock breaker && !breaker.isTripped(state)) return 1000;
        return Long.MAX_VALUE; // Generators, batteries, meters have no cable-like transfer limit
    }

    private static NativeRegistryHolder<Block> registerGenerator(String name, long genRate, long buffer, GeneratorType type) {
        return tracked(name, new GeneratorBlock(genRate, buffer, type, defaultProps().apply(BlockBehaviour.Properties.of())));
    }

    private static NativeRegistryHolder<Block> registerStorage(String name, long capacity, long maxIn, long maxOut) {
        return tracked(name, new BatteryBlock(capacity, maxIn, maxOut, defaultProps().apply(BlockBehaviour.Properties.of())));
    }

    private static NativeRegistryHolder<Block> registerCable(String name, long transferLimit) {
        return tracked(name, new CableBlock(transferLimit, cableProps().apply(BlockBehaviour.Properties.of())));
    }

    private static NativeRegistryHolder<Block> registerSubstation(String name) {
        return tracked(name, new SubstationBlock(defaultProps().apply(BlockBehaviour.Properties.of())));
    }

    private static NativeRegistryHolder<Block> registerBreaker(String name) {
        return tracked(name, new BreakerBlock(defaultProps().apply(BlockBehaviour.Properties.of())));
    }

    private static NativeRegistryHolder<Block> registerMeter(String name) {
        return tracked(name, new MeterBlock(defaultProps().apply(BlockBehaviour.Properties.of())));
    }

    private static NativeRegistryHolder<Block> registerConsumer(String name, long demand) {
        return tracked(name, new com.knoxhack.echopowergrid.block.ConsumerBlock(demand, defaultProps().apply(BlockBehaviour.Properties.of())));
    }

    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> defaultProps() {
        return p -> p.mapColor(MapColor.COLOR_GRAY).strength(2.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private static java.util.function.UnaryOperator<BlockBehaviour.Properties> cableProps() {
        return p -> p.mapColor(MapColor.COLOR_GRAY).strength(0.8F, 2.0F).sound(SoundType.COPPER).noOcclusion().dynamicShape();
    }

    private static NativeRegistryHolder<Block> tracked(String name, Block block) {
        NativeRegistryHolder<Block> holder = NativeRegistryHolder.of(name, block);
        BLOCK_ITEMS.add(holder);
        return holder;
    }
}
