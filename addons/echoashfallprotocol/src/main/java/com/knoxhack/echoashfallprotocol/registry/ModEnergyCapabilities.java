package com.knoxhack.echoashfallprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendEnergyBridge;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.item.BatteryItem;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ModEnergyCapabilities {
    private ModEnergyCapabilities() {
    }

    public static void register(Object event) {
        EchoBackendEnergyBridge.registerItemEnergy(event, ModEnergyCapabilities::batteryHandler,
                ModItems.BASIC_BATTERY.get(), ModItems.ADVANCED_BATTERY.get(), ModItems.ELITE_BATTERY.get());

        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.BATTERY_BANK.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.SCRAP_DYNAMO.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.NEXUS_CAPACITOR.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.LOAD_DISTRIBUTOR.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.MICRO_GENERATOR.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.THERMAL_ARRAY.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.THERMAL_BURNER.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.POWER_CABLE.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.POWER_NODE.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.HAND_RECYCLER.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.WATER_PURIFIER.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.FILTER_WORKBENCH.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.SCRAP_PRESS.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.ORE_GRINDER.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.ISOTOPE_REFINER.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.CRYSTALLINE_SYNTHESIZER.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.DEEP_CORE_MINER.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.RADIATION_CLEANSER.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.ATMOSPHERIC_SCRUBBER.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.FIELD_MED_BAY.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.CONTAMINANT_CONDENSER.get(),
                (be, side) -> blockEnergyHandler(be));
        EchoBackendEnergyBridge.registerBlockEntityEnergy(event, ModBlockEntities.AUTOFEED_HOPPER.get(),
                (be, side) -> blockEnergyHandler(be));
    }

    private static Object blockEnergyHandler(BlockEntity be) {
        if (be instanceof IEnergyStorage storage) {
            return EnergyAccess.backendEnergyHandler(storage, be::setChanged);
        }
        return null;
    }

    private static Object batteryHandler(ItemStack stack, Object access) {
        if (stack.getItem() instanceof BatteryItem battery) {
            return EchoBackendEnergyBridge.backendItemEnergyHandler(access, ModDataComponents.STORED_ENERGY.get(),
                    battery.getCapacity(), battery.getMaxReceive(), battery.getMaxExtract());
        }
        return null;
    }
}
