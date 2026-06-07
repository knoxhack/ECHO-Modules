package com.knoxhack.echopowergrid.registry;

import com.knoxhack.echopowergrid.block.entity.GeneratorBlockEntity;
import com.knoxhack.echopowergrid.block.entity.BatteryBlockEntity;
import com.knoxhack.echopowergrid.block.entity.PowerConsumerBlockEntity;
import com.knoxhack.echopowergrid.block.entity.SubstationBlockEntity;
import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final NativeRegistryHolder<BlockEntityType<GeneratorBlockEntity>> GENERATOR =
        NativeRegistryHolder.of("generator", new BlockEntityType<>(GeneratorBlockEntity::new,
            Set.of((Block) ModBlocks.HAND_CRANK_GENERATOR.get(), (Block) ModBlocks.SCRAP_BURNER_GENERATOR.get(),
                   (Block) ModBlocks.SOLAR_PANEL.get(), (Block) ModBlocks.REINFORCED_SOLAR_PANEL.get(),
                   (Block) ModBlocks.BIOFUEL_GENERATOR.get(), (Block) ModBlocks.CREATIVE_POWER_SOURCE.get())));

    public static final NativeRegistryHolder<BlockEntityType<BatteryBlockEntity>> BATTERY =
        NativeRegistryHolder.of("battery", new BlockEntityType<>(BatteryBlockEntity::new,
            Set.of((Block) ModBlocks.SMALL_BATTERY_BANK.get(), (Block) ModBlocks.MEDIUM_BATTERY_BANK.get(),
                   (Block) ModBlocks.FIELD_BATTERY_BANK.get(), (Block) ModBlocks.INDUSTRIAL_BATTERY_BANK.get())));

    public static final NativeRegistryHolder<BlockEntityType<PowerConsumerBlockEntity>> CONSUMER =
        NativeRegistryHolder.of("consumer", new BlockEntityType<>(PowerConsumerBlockEntity::new,
            Set.of((Block) ModBlocks.CREATIVE_POWER_SINK.get(), (Block) ModBlocks.TEST_POWER_CONSUMER.get())));

    public static final NativeRegistryHolder<BlockEntityType<SubstationBlockEntity>> SUBSTATION =
        NativeRegistryHolder.of("substation", new BlockEntityType<>(SubstationBlockEntity::new,
            Set.of((Block) ModBlocks.OUTPOST_SUBSTATION.get(), (Block) ModBlocks.RELAY_SUBSTATION.get(),
                   (Block) ModBlocks.FACTORY_SUBSTATION.get(), (Block) ModBlocks.NEXUS_STABILIZER_COUPLER.get())));

    private ModBlockEntities() {}

    public static void register() {
    }
}
