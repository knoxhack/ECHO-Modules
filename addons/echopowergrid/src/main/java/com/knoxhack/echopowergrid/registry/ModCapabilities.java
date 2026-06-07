package com.knoxhack.echopowergrid.registry;

import com.knoxhack.echopowergrid.block.entity.BatteryBlockEntity;
import com.knoxhack.echopowergrid.block.entity.GeneratorBlockEntity;
import com.knoxhack.echopowergrid.capability.EpEnergyHandler;
import java.util.List;

public final class ModCapabilities {
    private ModCapabilities() {}

    public static List<String> energyCapabilityTargets() {
        return List.of(ModBlockEntities.GENERATOR.id(), ModBlockEntities.BATTERY.id());
    }

    public static EpEnergyHandler generatorEnergyHandler(GeneratorBlockEntity blockEntity) {
        return new EpEnergyHandler(blockEntity, () -> {});
    }

    public static EpEnergyHandler batteryEnergyHandler(BatteryBlockEntity blockEntity) {
        return new EpEnergyHandler(blockEntity, () -> {});
    }
}
