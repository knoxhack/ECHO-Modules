package com.knoxhack.echoashfallprotocol.test;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echo.machinecore.EchoMachineProfile;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeRegistry;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeSnapshot;
import com.knoxhack.echo.machinecore.EchoMachineState;
import com.knoxhack.echo.machinecore.EchoMachineUiBridge;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.block.entity.AtmosphericScrubberBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.BatteryBankBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ContaminantCondenserBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.CrystallineSynthesizerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.FactoryControllerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.FilterWorkbenchBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.HandRecyclerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.IsotopeRefinerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.MicroGeneratorBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.OreGrinderBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.RadiationCleanserBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ScrapPressBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ThermalArrayBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ThermalBurnerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.WaterPurifierBlockEntity;
import com.knoxhack.echoashfallprotocol.integration.AshfallMachineCoreAdapter;
import com.knoxhack.echoashfallprotocol.integration.AshfallMachineCoreRuntimeProvider;
import com.knoxhack.echoashfallprotocol.item.BatteryItem;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echolens.api.LensAccessPolicy;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensScanMode;
import com.knoxhack.echolens.api.ServerLensProvider;
import com.knoxhack.echolens.integration.MachineCoreLensIntegration;
import com.knoxhack.echolens.registry.LensProviderRegistry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeEntry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeRegistry;
import com.knoxhack.echoterminal.integration.MachineCoreTerminalIntegration;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class AshfallMachineCoreGameTests {
    private AshfallMachineCoreGameTests() {
    }
    public static void ashfallMachineCoreRuntimeSnapshotContract(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos relativePos = new BlockPos(2, 2, 2);
        BlockPos purifierPos = helper.absolutePos(relativePos);
        helper.setBlock(relativePos, ModBlocks.WATER_PURIFIER.get().defaultBlockState());

        helper.assertTrue(level.getBlockEntity(purifierPos) instanceof WaterPurifierBlockEntity,
                "MachineCore Ashfall proof should create a live Water Purifier block entity");
        if (!(level.getBlockEntity(purifierPos) instanceof WaterPurifierBlockEntity purifier)) {
            helper.succeed();
            return;
        }

        MachineWearData wearData = new MachineWearData(level);
        wearData.repair(purifierPos, MachineWearData.MAX_WEAR);
        purifier.setEnergyStored(640);
        purifier.getInventory().setStackInSlot(0, new ItemStack(ModItems.DIRTY_WATER_BOTTLE.get(), 2));
        purifier.getInventory().setStackInSlot(1, new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get()));
        purifier.getInventory().setStackInSlot(
                WaterPurifierBlockEntity.BATTERY_SLOT,
                BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 300));
        purifier.data.set(0, 12);
        purifier.data.set(1, 60);
        purifier.data.set(2, 1);
        purifier.setChanged();

        EchoMachineRuntimeSnapshot snapshot = AshfallMachineCoreAdapter.runtimeSnapshot(purifier);
        helper.assertTrue(snapshot.id().value().equals("echoashfallprotocol:water_purifier")
                        && snapshot.ownerModule().value().equals(EchoAshfallProtocol.MODID)
                        && snapshot.machineBlockId().equals("echoashfallprotocol:water_purifier"),
                "MachineCore snapshot should preserve the Ashfall machine identity");
        helper.assertTrue(snapshot.kind().serializedName().equals("refinery")
                        && snapshot.state() == EchoMachineState.IDLE
                        && !snapshot.degraded(),
                "MachineCore snapshot should translate the powered idle Water Purifier into a neutral refinery state");
        helper.assertTrue("Water Purifier".equals(snapshot.displayName()),
                "MachineCore snapshot should expose the readable Ashfall machine display name");
        helper.assertTrue(snapshot.inventory().totalSlots() == 4
                        && snapshot.inventory().occupiedSlots() == 3
                        && snapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == 0
                                && slot.role().equals("input")
                                && slot.itemId().equals("echoashfallprotocol:dirty_water_bottle")
                                && slot.count() == 2)
                        && snapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == 1
                                && slot.role().equals("filter")
                                && slot.itemId().equals("echoashfallprotocol:filter_cartridge_basic"))
                        && snapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == WaterPurifierBlockEntity.BATTERY_SLOT
                                && slot.role().equals("battery")
                                && slot.itemId().equals("echoashfallprotocol:basic_battery")),
                "MachineCore inventory contract should preserve Ashfall water, filter, and battery slots");
        helper.assertTrue(snapshot.energy().resourceId().equals("echoashfallprotocol:fe_power")
                        && snapshot.energy().unit().equals("FE")
                        && snapshot.energy().stored() == 640
                        && snapshot.energy().capacity() == 1000
                        && snapshot.energy().canReceive()
                        && snapshot.energy().canExtract(),
                "MachineCore energy contract should preserve Ashfall FE buffer state");
        helper.assertTrue(!snapshot.fluids().supported()
                        && "2".equals(snapshot.fluids().attributes().get("dirtyBottleCount")),
                "MachineCore fluid contract should truthfully mark Ashfall water as item-bottle based");
        helper.assertTrue(snapshot.process().progressTicks() == 12
                        && snapshot.process().maxProgressTicks() == 60
                        && snapshot.process().progressPercent() == 20
                        && snapshot.process().recipeContract().equals("echoashfallprotocol:water_purification"),
                "MachineCore process contract should preserve Ashfall purification progress and recipe family");
        helper.assertTrue(snapshot.side().label().equals("Ashfall hopper routing")
                        && snapshot.side().upSlots().stream().anyMatch(slot -> slot.contains("water_input"))
                        && snapshot.side().downSlots().stream().anyMatch(slot -> slot.contains("output:2:output")),
                "MachineCore side contract should expose Ashfall hopper input and output routing");
        helper.assertTrue(snapshot.upgrades().capacity() == 0
                        && snapshot.savedState().persistedKeys().contains("inventory")
                        && snapshot.savedState().persistedKeys().contains("progress")
                        && snapshot.savedState().persistedKeys().contains("energy")
                        && snapshot.savedState().energyStored() == 640,
                "MachineCore saved-state contract should preserve Ashfall persisted machine keys and energy");
        helper.assertTrue(snapshot.integrationRefs().optionalFeatures().stream()
                        .anyMatch(feature -> feature.value().equals("echoterminal.machine_status"))
                        && snapshot.integrationRefs().optionalFeatures().stream()
                        .anyMatch(feature -> feature.value().equals("echolens.machine_scan")),
                "MachineCore snapshot should carry Ashfall UI/provider integration references");

        EchoMachineProfile profile = AshfallMachineCoreAdapter.profile(purifier);
        helper.assertTrue(profile.id().equals(snapshot.id()) && profile.ownerModule().equals(snapshot.ownerModule()),
                "MachineCore profile should align with the Ashfall runtime snapshot identity");
        helper.assertTrue(profile.recipeBindings().stream().anyMatch(binding -> binding.recipeId() != null
                                && binding.recipeId().value().equals("echoashfallprotocol:water_purification"))
                        && profile.automationHooks().stream().anyMatch(hook -> hook.kind().serializedName().equals("power_input"))
                        && profile.automationHooks().stream().anyMatch(hook -> hook.kind().serializedName().equals("maintenance"))
                        && profile.maintenanceProfile().supportsFieldRepair(),
                "MachineCore profile should expose Ashfall recipe, power, maintenance, and automation contracts");
        assertAshfallMachineCoreAdditionalFamilies(helper, level);
        assertAshfallMachineCoreSpecialtyFamilies(helper, level);
        assertAshfallMachineCoreSpecialtyConsumerProjections(helper, level);
        helper.succeed();
    }

    private static void assertAshfallMachineCoreAdditionalFamilies(GameTestHelper helper, ServerLevel level) {
        BlockPos scrapRelative = new BlockPos(5, 2, 2);
        BlockPos oreRelative = new BlockPos(7, 2, 2);
        BlockPos generatorRelative = new BlockPos(9, 2, 2);
        BlockPos batteryRelative = new BlockPos(11, 2, 2);
        BlockPos factoryRelative = new BlockPos(13, 2, 2);
        helper.setBlock(scrapRelative, ModBlocks.SCRAP_PRESS.get().defaultBlockState());
        helper.setBlock(oreRelative, ModBlocks.ORE_GRINDER.get().defaultBlockState());
        helper.setBlock(generatorRelative, ModBlocks.MICRO_GENERATOR.get().defaultBlockState());
        helper.setBlock(batteryRelative, ModBlocks.BATTERY_BANK.get().defaultBlockState());
        helper.setBlock(factoryRelative, ModBlocks.FACTORY_CONTROLLER.get().defaultBlockState());

        BlockPos scrapPos = helper.absolutePos(scrapRelative);
        BlockPos orePos = helper.absolutePos(oreRelative);
        BlockPos generatorPos = helper.absolutePos(generatorRelative);
        BlockPos batteryPos = helper.absolutePos(batteryRelative);
        BlockPos factoryPos = helper.absolutePos(factoryRelative);
        MachineWearData wearData = new MachineWearData(level);

        helper.assertTrue(level.getBlockEntity(scrapPos) instanceof ScrapPressBlockEntity,
                "MachineCore Ashfall proof should create a live Scrap Press block entity");
        helper.assertTrue(level.getBlockEntity(orePos) instanceof OreGrinderBlockEntity,
                "MachineCore Ashfall proof should create a live Ore Grinder block entity");
        helper.assertTrue(level.getBlockEntity(generatorPos) instanceof MicroGeneratorBlockEntity,
                "MachineCore Ashfall proof should create a live Micro Generator block entity");
        helper.assertTrue(level.getBlockEntity(batteryPos) instanceof BatteryBankBlockEntity,
                "MachineCore Ashfall proof should create a live Battery Bank block entity");
        helper.assertTrue(level.getBlockEntity(factoryPos) instanceof FactoryControllerBlockEntity,
                "MachineCore Ashfall proof should create a live Factory Controller block entity");

        if (!(level.getBlockEntity(scrapPos) instanceof ScrapPressBlockEntity scrapPress)
                || !(level.getBlockEntity(orePos) instanceof OreGrinderBlockEntity oreGrinder)
                || !(level.getBlockEntity(generatorPos) instanceof MicroGeneratorBlockEntity generator)
                || !(level.getBlockEntity(batteryPos) instanceof BatteryBankBlockEntity battery)
                || !(level.getBlockEntity(factoryPos) instanceof FactoryControllerBlockEntity factory)) {
            return;
        }

        wearData.repair(scrapPos, MachineWearData.MAX_WEAR);
        scrapPress.getInventory().setStackInSlot(0, new ItemStack(ModItems.SCRAP_METAL.get(), 9));
        scrapPress.getInventory().setStackInSlot(
                ScrapPressBlockEntity.BATTERY_SLOT,
                BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 200));
        scrapPress.data.set(0, 8);
        scrapPress.data.set(1, 40);
        scrapPress.data.set(3, 1);
        scrapPress.data.set(6, 512);
        scrapPress.setChanged();

        EchoMachineRuntimeSnapshot scrapSnapshot = AshfallMachineCoreAdapter.runtimeSnapshot(level, scrapPos);
        helper.assertTrue(scrapSnapshot.machineBlockId().equals("echoashfallprotocol:scrap_press")
                        && scrapSnapshot.kind().serializedName().equals("fabricator")
                        && scrapSnapshot.inventory().totalSlots() == 3
                        && scrapSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == 0
                                && slot.role().equals("input")
                                && slot.itemId().equals("echoashfallprotocol:scrap_metal"))
                        && scrapSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == ScrapPressBlockEntity.BATTERY_SLOT
                                && slot.role().equals("battery"))
                        && scrapSnapshot.process().recipeContract().equals("echoashfallprotocol:scrap_pressing")
                        && scrapSnapshot.energy().stored() == 512,
                "MachineCore should map a real Ashfall Scrap Press as a powered fabricator with input/output/battery contracts.");
        assertMachineCoreProfileBinding(helper, scrapPress, "echoashfallprotocol:scrap_pressing");

        wearData.repair(orePos, MachineWearData.MAX_WEAR);
        oreGrinder.getInventory().setStackInSlot(OreGrinderBlockEntity.INPUT_SLOT_1, new ItemStack(Items.STONE, 4));
        oreGrinder.getInventory().setStackInSlot(
                OreGrinderBlockEntity.BATTERY_SLOT,
                BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 200));
        oreGrinder.data.set(0, 16);
        oreGrinder.data.set(1, 80);
        oreGrinder.data.set(2, 1);
        oreGrinder.data.set(5, 768);
        oreGrinder.setChanged();

        EchoMachineRuntimeSnapshot oreSnapshot = AshfallMachineCoreAdapter.runtimeSnapshot(oreGrinder);
        helper.assertTrue(oreSnapshot.machineBlockId().equals("echoashfallprotocol:ore_grinder")
                        && oreSnapshot.kind().serializedName().equals("fabricator")
                        && oreSnapshot.inventory().totalSlots() == 5
                        && oreSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == OreGrinderBlockEntity.OUTPUT_SLOT
                                && slot.role().equals("output"))
                        && oreSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == OreGrinderBlockEntity.BYPRODUCT_SLOT
                                && slot.role().equals("output"))
                        && oreSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == OreGrinderBlockEntity.BATTERY_SLOT
                                && slot.role().equals("battery"))
                        && oreSnapshot.process().progressTicks() == 16
                        && oreSnapshot.process().recipeContract().equals("echoashfallprotocol:substrate_grinding")
                        && oreSnapshot.energy().stored() == 768,
                "MachineCore should map a real Ashfall Ore Grinder as a substrate fabricator with input, output, byproduct, and battery contracts.");
        assertMachineCoreProfileBinding(helper, oreGrinder, "echoashfallprotocol:substrate_grinding");

        wearData.repair(generatorPos, MachineWearData.MAX_WEAR);
        generator.getInventory().setStackInSlot(0, new ItemStack(Items.COAL, 2));
        generator.getInventory().setStackInSlot(
                MicroGeneratorBlockEntity.BATTERY_SLOT,
                BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 100));
        generator.data.set(0, 1024);
        generator.data.set(2, 80);
        generator.data.set(3, 160);
        generator.setChanged();

        EchoMachineRuntimeSnapshot generatorSnapshot = AshfallMachineCoreAdapter.runtimeSnapshot(generator);
        helper.assertTrue(generatorSnapshot.machineBlockId().equals("echoashfallprotocol:micro_generator")
                        && generatorSnapshot.kind().serializedName().equals("powered_station")
                        && generatorSnapshot.energy().stored() == 1024
                        && !generatorSnapshot.energy().canReceive()
                        && generatorSnapshot.energy().canExtract()
                        && generatorSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == 0
                                && slot.role().equals("input")
                                && slot.attributes().get("ashfallRole").equals("fuel_input"))
                        && generatorSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == MicroGeneratorBlockEntity.BATTERY_SLOT
                                && slot.role().equals("battery"))
                        && generatorSnapshot.process().progressTicks() == 80
                        && generatorSnapshot.process().maxProgressTicks() == 160
                        && generatorSnapshot.process().recipeContract().equals("echoashfallprotocol:micro_power_generation"),
                "MachineCore should map a real Ashfall Micro Generator as a power station with fuel, battery, burn-time, and output-power contracts.");
        assertMachineCoreProfileBinding(helper, generator, "echoashfallprotocol:micro_power_generation");

        battery.setEnergyStored(2400);
        battery.getInventory().setStackInSlot(
                BatteryBankBlockEntity.BATTERY_SLOT,
                BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 100));
        battery.setChanged();

        EchoMachineRuntimeSnapshot batterySnapshot = AshfallMachineCoreAdapter.runtimeSnapshot(battery);
        helper.assertTrue(batterySnapshot.machineBlockId().equals("echoashfallprotocol:battery_bank")
                        && batterySnapshot.kind().serializedName().equals("powered_station")
                        && batterySnapshot.energy().stored() == 2400
                        && batterySnapshot.energy().capacity() == BatteryBankBlockEntity.CAPACITY
                        && batterySnapshot.energy().canReceive()
                        && batterySnapshot.energy().canExtract()
                        && batterySnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == BatteryBankBlockEntity.BATTERY_SLOT
                                && slot.role().equals("battery"))
                        && batterySnapshot.process().recipeContract().equals("echoashfallprotocol:battery_storage"),
                "MachineCore should map a real Ashfall Battery Bank as a power station with stored-energy and battery-slot contracts.");
        assertMachineCoreProfileBinding(helper, battery, "echoashfallprotocol:battery_storage");

        factory.setNetworkEnabled(true);
        EchoMachineRuntimeSnapshot factorySnapshot = AshfallMachineCoreAdapter.runtimeSnapshot(factory);
        helper.assertTrue(factorySnapshot.machineBlockId().equals("echoashfallprotocol:factory_controller")
                        && factorySnapshot.kind().serializedName().equals("automation_node")
                        && factorySnapshot.inventory().totalSlots() == 0
                        && factorySnapshot.energy().capacity() == 0
                        && factorySnapshot.process().recipeContract().equals("echoashfallprotocol:factory_control")
                        && factorySnapshot.integrationRefs().optionalFeatures().stream()
                                .anyMatch(feature -> feature.value().equals("echologisticsnetwork.item_routing")),
                "MachineCore should map a real Ashfall Factory Controller as an automation node instead of a generic decorative machine.");
        assertMachineCoreProfileBinding(helper, factory, "echoashfallprotocol:factory_control");
    }

    private static void assertAshfallMachineCoreSpecialtyFamilies(GameTestHelper helper, ServerLevel level) {
        BlockPos recyclerRelative = new BlockPos(2, 4, 2);
        BlockPos burnerRelative = new BlockPos(4, 4, 2);
        BlockPos filterRelative = new BlockPos(6, 4, 2);
        BlockPos cleanserRelative = new BlockPos(8, 4, 2);
        BlockPos isotopeRelative = new BlockPos(10, 4, 2);
        BlockPos synthRelative = new BlockPos(12, 4, 2);
        BlockPos arrayRelative = new BlockPos(14, 4, 2);
        BlockPos scrubberRelative = new BlockPos(16, 4, 2);
        BlockPos condenserRelative = new BlockPos(18, 4, 2);

        helper.setBlock(recyclerRelative, ModBlocks.HAND_RECYCLER.get().defaultBlockState());
        helper.setBlock(burnerRelative, ModBlocks.THERMAL_BURNER.get().defaultBlockState());
        helper.setBlock(filterRelative, ModBlocks.FILTER_WORKBENCH.get().defaultBlockState());
        helper.setBlock(cleanserRelative, ModBlocks.RADIATION_CLEANSER.get().defaultBlockState());
        helper.setBlock(isotopeRelative, ModBlocks.ISOTOPE_REFINER.get().defaultBlockState());
        helper.setBlock(synthRelative, ModBlocks.CRYSTALLINE_SYNTHESIZER.get().defaultBlockState());
        helper.setBlock(arrayRelative, ModBlocks.THERMAL_ARRAY.get().defaultBlockState());
        helper.setBlock(scrubberRelative, ModBlocks.ATMOSPHERIC_SCRUBBER.get().defaultBlockState());
        helper.setBlock(condenserRelative, ModBlocks.CONTAMINANT_CONDENSER.get().defaultBlockState());

        BlockPos recyclerPos = helper.absolutePos(recyclerRelative);
        BlockPos burnerPos = helper.absolutePos(burnerRelative);
        BlockPos filterPos = helper.absolutePos(filterRelative);
        BlockPos cleanserPos = helper.absolutePos(cleanserRelative);
        BlockPos isotopePos = helper.absolutePos(isotopeRelative);
        BlockPos synthPos = helper.absolutePos(synthRelative);
        BlockPos arrayPos = helper.absolutePos(arrayRelative);
        BlockPos scrubberPos = helper.absolutePos(scrubberRelative);
        BlockPos condenserPos = helper.absolutePos(condenserRelative);

        helper.assertTrue(level.getBlockEntity(recyclerPos) instanceof HandRecyclerBlockEntity,
                "MachineCore Ashfall proof should create a live Hand Recycler block entity");
        helper.assertTrue(level.getBlockEntity(burnerPos) instanceof ThermalBurnerBlockEntity,
                "MachineCore Ashfall proof should create a live Thermal Burner block entity");
        helper.assertTrue(level.getBlockEntity(filterPos) instanceof FilterWorkbenchBlockEntity,
                "MachineCore Ashfall proof should create a live Filter Workbench block entity");
        helper.assertTrue(level.getBlockEntity(cleanserPos) instanceof RadiationCleanserBlockEntity,
                "MachineCore Ashfall proof should create a live Radiation Cleanser block entity");
        helper.assertTrue(level.getBlockEntity(isotopePos) instanceof IsotopeRefinerBlockEntity,
                "MachineCore Ashfall proof should create a live Isotope Refiner block entity");
        helper.assertTrue(level.getBlockEntity(synthPos) instanceof CrystallineSynthesizerBlockEntity,
                "MachineCore Ashfall proof should create a live Crystalline Synthesizer block entity");
        helper.assertTrue(level.getBlockEntity(arrayPos) instanceof ThermalArrayBlockEntity,
                "MachineCore Ashfall proof should create a live Thermal Array block entity");
        helper.assertTrue(level.getBlockEntity(scrubberPos) instanceof AtmosphericScrubberBlockEntity,
                "MachineCore Ashfall proof should create a live Atmospheric Scrubber block entity");
        helper.assertTrue(level.getBlockEntity(condenserPos) instanceof ContaminantCondenserBlockEntity,
                "MachineCore Ashfall proof should create a live Contaminant Condenser block entity");

        if (!(level.getBlockEntity(recyclerPos) instanceof HandRecyclerBlockEntity recycler)
                || !(level.getBlockEntity(burnerPos) instanceof ThermalBurnerBlockEntity burner)
                || !(level.getBlockEntity(filterPos) instanceof FilterWorkbenchBlockEntity filter)
                || !(level.getBlockEntity(cleanserPos) instanceof RadiationCleanserBlockEntity cleanser)
                || !(level.getBlockEntity(isotopePos) instanceof IsotopeRefinerBlockEntity isotope)
                || !(level.getBlockEntity(synthPos) instanceof CrystallineSynthesizerBlockEntity synthesizer)
                || !(level.getBlockEntity(arrayPos) instanceof ThermalArrayBlockEntity thermalArray)
                || !(level.getBlockEntity(scrubberPos) instanceof AtmosphericScrubberBlockEntity scrubber)
                || !(level.getBlockEntity(condenserPos) instanceof ContaminantCondenserBlockEntity condenser)) {
            return;
        }

        MachineWearData wearData = new MachineWearData(level);
        wearData.repair(recyclerPos, MachineWearData.MAX_WEAR);
        recycler.setEnergyStored(700);
        recycler.getInventory().setStackInSlot(0, new ItemStack(ModItems.SCRAP_METAL.get(), 2));
        recycler.getInventory().setStackInSlot(2, new ItemStack(ModItems.MACHINE_UPGRADE_SPEED.get()));
        recycler.getInventory().setStackInSlot(
                HandRecyclerBlockEntity.BATTERY_SLOT,
                BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 150));
        recycler.data.set(0, 20);
        recycler.data.set(1, 100);
        recycler.data.set(2, 1);
        recycler.setChanged();

        EchoMachineRuntimeSnapshot recyclerSnapshot = AshfallMachineCoreAdapter.runtimeSnapshot(recycler);
        helper.assertTrue(recyclerSnapshot.machineBlockId().equals("echoashfallprotocol:hand_recycler")
                        && recyclerSnapshot.kind().serializedName().equals("repair_bench")
                        && recyclerSnapshot.inventory().totalSlots() == 4
                        && recyclerSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == 0
                                && slot.role().equals("input")
                                && slot.itemId().equals("echoashfallprotocol:scrap_metal"))
                        && recyclerSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == 2
                                && slot.role().equals("upgrade")
                                && slot.itemId().equals("echoashfallprotocol:machine_upgrade_speed"))
                        && recyclerSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == HandRecyclerBlockEntity.BATTERY_SLOT
                                && slot.role().equals("battery"))
                        && recyclerSnapshot.process().recipeContract().equals("echoashfallprotocol:hand_recycling")
                        && recyclerSnapshot.energy().stored() == 700,
                "MachineCore should map a real Ashfall Hand Recycler as a repair bench with input, upgrade, battery, and recycling contracts.");
        assertMachineCoreProfileBinding(helper, recycler, "echoashfallprotocol:hand_recycling");

        wearData.repair(burnerPos, MachineWearData.MAX_WEAR);
        burner.setEnergyStored(350);
        burner.getInventory().setStackInSlot(0, new ItemStack(Items.COAL, 2));
        burner.getInventory().setStackInSlot(1, new ItemStack(ModItems.ASH.get()));
        burner.getInventory().setStackInSlot(
                ThermalBurnerBlockEntity.BATTERY_SLOT,
                BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 120));
        burner.data.set(0, 10);
        burner.data.set(1, 40);
        burner.setChanged();

        EchoMachineRuntimeSnapshot burnerSnapshot = AshfallMachineCoreAdapter.runtimeSnapshot(level, burnerPos);
        helper.assertTrue(burnerSnapshot.machineBlockId().equals("echoashfallprotocol:thermal_burner")
                        && burnerSnapshot.kind().serializedName().equals("fabricator")
                        && burnerSnapshot.inventory().totalSlots() == 3
                        && burnerSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == 0
                                && slot.role().equals("input")
                                && slot.attributes().get("ashfallRole").equals("fuel_input"))
                        && burnerSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == 1
                                && slot.role().equals("output")
                                && slot.attributes().get("ashfallRole").equals("byproduct"))
                        && burnerSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == ThermalBurnerBlockEntity.BATTERY_SLOT
                                && slot.role().equals("battery"))
                        && burnerSnapshot.process().progressTicks() == 10
                        && burnerSnapshot.process().maxProgressTicks() == 40
                        && burnerSnapshot.process().recipeContract().equals("echoashfallprotocol:thermal_burning")
                        && burnerSnapshot.energy().stored() == 350
                        && !burnerSnapshot.energy().canReceive()
                        && burnerSnapshot.energy().canExtract(),
                "MachineCore should map a real Ashfall Thermal Burner as a fuel-to-power fabricator with ash byproduct routing.");
        assertMachineCoreProfileBinding(helper, burner, "echoashfallprotocol:thermal_burning");

        wearData.repair(filterPos, MachineWearData.MAX_WEAR);
        filter.setEnergyStored(900);
        filter.getInventory().setStackInSlot(0, new ItemStack(ModItems.SCRAP_PLASTIC.get(), 2));
        filter.getInventory().setStackInSlot(1, new ItemStack(ModItems.FILTRATION_MEMBRANE.get()));
        filter.getInventory().setStackInSlot(3, new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get()));
        filter.getInventory().setStackInSlot(
                FilterWorkbenchBlockEntity.BATTERY_SLOT,
                BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 100));
        filter.data.set(0, 8);
        filter.setChanged();

        EchoMachineRuntimeSnapshot filterSnapshot = AshfallMachineCoreAdapter.runtimeSnapshot(filter);
        helper.assertTrue(filterSnapshot.machineBlockId().equals("echoashfallprotocol:filter_workbench")
                        && filterSnapshot.kind().serializedName().equals("refinery")
                        && filterSnapshot.inventory().totalSlots() == 5
                        && filterSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == 0
                                && slot.role().equals("input")
                                && slot.attributes().get("ashfallRole").equals("primary_input"))
                        && filterSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == 3
                                && slot.role().equals("output")
                                && slot.itemId().equals("echoashfallprotocol:filter_cartridge_basic"))
                        && filterSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == FilterWorkbenchBlockEntity.BATTERY_SLOT
                                && slot.role().equals("battery"))
                        && filterSnapshot.process().progressTicks() == 8
                        && filterSnapshot.process().maxProgressTicks() == 20
                        && filterSnapshot.process().recipeContract().equals("echoashfallprotocol:filter_crafting")
                        && filterSnapshot.energy().stored() == 900,
                "MachineCore should map a real Ashfall Filter Workbench as a powered filter crafting station with material, output, and battery contracts.");
        assertMachineCoreProfileBinding(helper, filter, "echoashfallprotocol:filter_crafting");

        wearData.repair(cleanserPos, MachineWearData.MAX_WEAR);
        cleanser.setEnergyStored(1200);
        cleanser.getInventory().setStackInSlot(RadiationCleanserBlockEntity.INPUT_SLOT,
                new ItemStack(ModItems.CONTAMINATED_IRON.get()));
        cleanser.getInventory().setStackInSlot(RadiationCleanserBlockEntity.FILTER_SLOT,
                new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get()));
        cleanser.getInventory().setStackInSlot(RadiationCleanserBlockEntity.OUTPUT_SLOT,
                new ItemStack(Items.IRON_INGOT));
        cleanser.getInventory().setStackInSlot(
                RadiationCleanserBlockEntity.BATTERY_SLOT,
                BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 100));
        cleanser.data.set(0, 40);
        cleanser.data.set(2, 7);
        cleanser.setChanged();

        EchoMachineRuntimeSnapshot cleanserSnapshot = AshfallMachineCoreAdapter.runtimeSnapshot(cleanser);
        helper.assertTrue(cleanserSnapshot.machineBlockId().equals("echoashfallprotocol:radiation_cleanser")
                        && cleanserSnapshot.kind().serializedName().equals("refinery")
                        && cleanserSnapshot.inventory().totalSlots() == 4
                        && cleanserSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == RadiationCleanserBlockEntity.INPUT_SLOT
                                && slot.role().equals("input")
                                && slot.itemId().equals("echoashfallprotocol:contaminated_iron"))
                        && cleanserSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == RadiationCleanserBlockEntity.FILTER_SLOT
                                && slot.role().equals("filter")
                                && slot.itemId().equals("echoashfallprotocol:filter_cartridge_advanced"))
                        && cleanserSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == RadiationCleanserBlockEntity.OUTPUT_SLOT
                                && slot.role().equals("output"))
                        && cleanserSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == RadiationCleanserBlockEntity.BATTERY_SLOT
                                && slot.role().equals("battery"))
                        && cleanserSnapshot.process().progressTicks() == 40
                        && cleanserSnapshot.process().maxProgressTicks() == RadiationCleanserBlockEntity.TOTAL_TICKS
                        && cleanserSnapshot.process().recipeContract().equals("echoashfallprotocol:radiation_cleansing")
                        && cleanserSnapshot.energy().stored() == 1200,
                "MachineCore should map a real Ashfall Radiation Cleanser as a powered refinery with contaminated input, advanced filter, output, and battery contracts.");
        assertMachineCoreProfileBinding(helper, cleanser, "echoashfallprotocol:radiation_cleansing");

        wearData.repair(isotopePos, MachineWearData.MAX_WEAR);
        isotope.setEnergyStored(2000);
        isotope.getInventory().setStackInSlot(IsotopeRefinerBlockEntity.INPUT_SLOT, new ItemStack(Items.IRON_INGOT, 2));
        isotope.getInventory().setStackInSlot(IsotopeRefinerBlockEntity.CATALYST_SLOT,
                new ItemStack(ModItems.CRYSTAL_DUST.get()));
        isotope.getInventory().setStackInSlot(IsotopeRefinerBlockEntity.OUTPUT_SLOT_1, new ItemStack(Items.GOLD_INGOT));
        isotope.getInventory().setStackInSlot(
                IsotopeRefinerBlockEntity.BATTERY_SLOT,
                BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 100));
        isotope.data.set(0, 32);
        isotope.data.set(1, 160);
        isotope.data.set(2, 20);
        isotope.data.set(5, 2000);
        isotope.setChanged();

        EchoMachineRuntimeSnapshot isotopeSnapshot = AshfallMachineCoreAdapter.runtimeSnapshot(isotope);
        helper.assertTrue(isotopeSnapshot.machineBlockId().equals("echoashfallprotocol:isotope_refiner")
                        && isotopeSnapshot.kind().serializedName().equals("refinery")
                        && isotopeSnapshot.inventory().totalSlots() == 5
                        && isotopeSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == IsotopeRefinerBlockEntity.INPUT_SLOT
                                && slot.role().equals("input"))
                        && isotopeSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == IsotopeRefinerBlockEntity.CATALYST_SLOT
                                && slot.role().equals("filter")
                                && slot.attributes().get("ashfallRole").equals("catalyst"))
                        && isotopeSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == IsotopeRefinerBlockEntity.OUTPUT_SLOT_1
                                && slot.role().equals("output"))
                        && isotopeSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == IsotopeRefinerBlockEntity.OUTPUT_SLOT_2
                                && slot.role().equals("output"))
                        && isotopeSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == IsotopeRefinerBlockEntity.BATTERY_SLOT
                                && slot.role().equals("battery"))
                        && isotopeSnapshot.process().progressTicks() == 32
                        && isotopeSnapshot.process().maxProgressTicks() == 160
                        && isotopeSnapshot.process().recipeContract().equals("echoashfallprotocol:isotope_refining")
                        && isotopeSnapshot.energy().stored() == 2000,
                "MachineCore should map a real Ashfall Isotope Refiner as a late-game refinery with catalyst, dual outputs, battery, and isotope recipe contracts.");
        assertMachineCoreProfileBinding(helper, isotope, "echoashfallprotocol:isotope_refining");

        wearData.repair(synthPos, MachineWearData.MAX_WEAR);
        synthesizer.setEnergyStored(3000);
        synthesizer.getInventory().setStackInSlot(CrystallineSynthesizerBlockEntity.INPUT_SLOT_1,
                new ItemStack(ModItems.GEM_FRAGMENT.get(), 4));
        synthesizer.getInventory().setStackInSlot(CrystallineSynthesizerBlockEntity.INPUT_SLOT_2,
                new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get()));
        synthesizer.getInventory().setStackInSlot(CrystallineSynthesizerBlockEntity.CATALYST_SLOT,
                new ItemStack(ModItems.ENERGY_CELL.get(), 2));
        synthesizer.getInventory().setStackInSlot(CrystallineSynthesizerBlockEntity.OUTPUT_SLOT, new ItemStack(Items.DIAMOND));
        synthesizer.getInventory().setStackInSlot(
                CrystallineSynthesizerBlockEntity.BATTERY_SLOT,
                BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 100));
        synthesizer.data.set(0, 120);
        synthesizer.data.set(2, 2);
        synthesizer.setChanged();

        EchoMachineRuntimeSnapshot synthSnapshot = AshfallMachineCoreAdapter.runtimeSnapshot(synthesizer);
        helper.assertTrue(synthSnapshot.machineBlockId().equals("echoashfallprotocol:crystalline_synthesizer")
                        && synthSnapshot.kind().serializedName().equals("fabricator")
                        && synthSnapshot.inventory().totalSlots() == 5
                        && synthSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == CrystallineSynthesizerBlockEntity.INPUT_SLOT_1
                                && slot.role().equals("input")
                                && slot.itemId().equals("echoashfallprotocol:gem_fragment"))
                        && synthSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == CrystallineSynthesizerBlockEntity.CATALYST_SLOT
                                && slot.role().equals("filter")
                                && slot.attributes().get("ashfallRole").equals("catalyst"))
                        && synthSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == CrystallineSynthesizerBlockEntity.OUTPUT_SLOT
                                && slot.role().equals("output"))
                        && synthSnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == CrystallineSynthesizerBlockEntity.BATTERY_SLOT
                                && slot.role().equals("battery"))
                        && synthSnapshot.process().progressTicks() == 120
                        && synthSnapshot.process().maxProgressTicks() == CrystallineSynthesizerBlockEntity.TOTAL_TICKS
                        && synthSnapshot.process().recipeContract().equals("echoashfallprotocol:crystalline_synthesis")
                        && synthSnapshot.energy().stored() == 3000,
                "MachineCore should map a real Ashfall Crystalline Synthesizer as a late-game fabricator with multi-input, catalyst, output, and battery contracts.");
        assertMachineCoreProfileBinding(helper, synthesizer, "echoashfallprotocol:crystalline_synthesis");

        wearData.repair(arrayPos, MachineWearData.MAX_WEAR);
        thermalArray.setEnergyStored(1500);
        thermalArray.getInventory().setStackInSlot(0, new ItemStack(Items.COAL, 2));
        thermalArray.getInventory().setStackInSlot(1, new ItemStack(Items.CHARCOAL));
        thermalArray.getInventory().setStackInSlot(
                ThermalArrayBlockEntity.BATTERY_SLOT,
                BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 100));
        thermalArray.data.set(2, 90);
        thermalArray.data.set(3, 240);
        thermalArray.setChanged();

        EchoMachineRuntimeSnapshot arraySnapshot = AshfallMachineCoreAdapter.runtimeSnapshot(thermalArray);
        helper.assertTrue(arraySnapshot.machineBlockId().equals("echoashfallprotocol:thermal_array")
                        && arraySnapshot.kind().serializedName().equals("powered_station")
                        && arraySnapshot.inventory().totalSlots() == 4
                        && arraySnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == 0
                                && slot.role().equals("input")
                                && slot.attributes().get("ashfallRole").equals("fuel_input"))
                        && arraySnapshot.inventory().slots().stream().anyMatch(slot -> slot.index() == ThermalArrayBlockEntity.BATTERY_SLOT
                                && slot.role().equals("battery"))
                        && arraySnapshot.process().progressTicks() == 90
                        && arraySnapshot.process().maxProgressTicks() == 240
                        && arraySnapshot.process().recipeContract().equals("echoashfallprotocol:thermal_array_generation")
                        && arraySnapshot.energy().stored() == 1500
                        && !arraySnapshot.energy().canReceive()
                        && arraySnapshot.energy().canExtract(),
                "MachineCore should map a real Ashfall Thermal Array as a multi-fuel power station with battery output and burn-time contracts.");
        assertMachineCoreProfileBinding(helper, thermalArray, "echoashfallprotocol:thermal_array_generation");

        wearData.repair(scrubberPos, MachineWearData.MAX_WEAR);
        scrubber.setEnergyStored(1000);
        EchoMachineRuntimeSnapshot scrubberSnapshot = AshfallMachineCoreAdapter.runtimeSnapshot(scrubber);
        helper.assertTrue(scrubberSnapshot.machineBlockId().equals("echoashfallprotocol:atmospheric_scrubber")
                        && scrubberSnapshot.kind().serializedName().equals("refinery")
                        && scrubberSnapshot.inventory().totalSlots() == 0
                        && scrubberSnapshot.energy().stored() == 1000
                        && scrubberSnapshot.energy().canReceive()
                        && scrubberSnapshot.energy().canExtract()
                        && scrubberSnapshot.process().recipeContract().equals("echoashfallprotocol:atmospheric_scrubbing"),
                "MachineCore should map a real Ashfall Atmospheric Scrubber as a powered area refinery with no invented inventory slots.");
        assertMachineCoreProfileBinding(helper, scrubber, "echoashfallprotocol:atmospheric_scrubbing");

        wearData.repair(condenserPos, MachineWearData.MAX_WEAR);
        condenser.setEnergyStored(900);
        EchoMachineRuntimeSnapshot condenserSnapshot = AshfallMachineCoreAdapter.runtimeSnapshot(condenser);
        helper.assertTrue(condenserSnapshot.machineBlockId().equals("echoashfallprotocol:contaminant_condenser")
                        && condenserSnapshot.kind().serializedName().equals("refinery")
                        && condenserSnapshot.inventory().totalSlots() == 0
                        && condenserSnapshot.energy().stored() == 900
                        && condenserSnapshot.energy().canReceive()
                        && condenserSnapshot.energy().canExtract()
                        && condenserSnapshot.process().recipeContract().equals("echoashfallprotocol:contaminant_condensing"),
                "MachineCore should map a real Ashfall Contaminant Condenser as a powered area refinery with no invented inventory slots.");
        assertMachineCoreProfileBinding(helper, condenser, "echoashfallprotocol:contaminant_condensing");
    }

    private static void assertAshfallMachineCoreSpecialtyConsumerProjections(GameTestHelper helper, ServerLevel level) {
        AshfallMachineCoreRuntimeProvider.register();
        MachineCoreTerminalIntegration.register();
        MachineCoreLensIntegration.register();
        registerMachineCoreIndexForTest(helper);
        registerMachineCoreHoloMapForTest(helper);
        registerMachineCoreLogisticsForTest(helper);

        List<AshfallMachineCoreConsumerCase> cases = List.of(
                new AshfallMachineCoreConsumerCase(
                        new BlockPos(2, 6, 2),
                        ModBlocks.HAND_RECYCLER.get(),
                        "hand_recycler",
                        "Hand Recycler",
                        "echoashfallprotocol:hand_recycling",
                        701,
                        true,
                        (caseHelper, machine) -> {
                            if (!(machine instanceof HandRecyclerBlockEntity recycler)) {
                                caseHelper.assertTrue(false, "Ashfall MachineCore consumer proof should create a live Hand Recycler block entity");
                                return;
                            }
                            recycler.setEnergyStored(701);
                            recycler.getInventory().setStackInSlot(0, new ItemStack(ModItems.SCRAP_METAL.get(), 2));
                            recycler.getInventory().setStackInSlot(2, new ItemStack(ModItems.MACHINE_UPGRADE_SPEED.get()));
                            recycler.getInventory().setStackInSlot(
                                    HandRecyclerBlockEntity.BATTERY_SLOT,
                                    BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 80));
                            recycler.data.set(0, 9);
                            recycler.data.set(1, 100);
                            recycler.data.set(2, 1);
                        }),
                new AshfallMachineCoreConsumerCase(
                        new BlockPos(4, 6, 2),
                        ModBlocks.THERMAL_BURNER.get(),
                        "thermal_burner",
                        "Thermal Burner",
                        "echoashfallprotocol:thermal_burning",
                        702,
                        true,
                        (caseHelper, machine) -> {
                            if (!(machine instanceof ThermalBurnerBlockEntity burner)) {
                                caseHelper.assertTrue(false, "Ashfall MachineCore consumer proof should create a live Thermal Burner block entity");
                                return;
                            }
                            burner.setEnergyStored(702);
                            burner.getInventory().setStackInSlot(0, new ItemStack(Items.COAL, 2));
                            burner.getInventory().setStackInSlot(1, new ItemStack(ModItems.ASH.get()));
                            burner.getInventory().setStackInSlot(
                                    ThermalBurnerBlockEntity.BATTERY_SLOT,
                                    BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 80));
                            burner.data.set(0, 12);
                            burner.data.set(1, 40);
                        }),
                new AshfallMachineCoreConsumerCase(
                        new BlockPos(6, 6, 2),
                        ModBlocks.FILTER_WORKBENCH.get(),
                        "filter_workbench",
                        "Filter Workbench",
                        "echoashfallprotocol:filter_crafting",
                        777,
                        true,
                        (caseHelper, machine) -> {
                            if (!(machine instanceof FilterWorkbenchBlockEntity filterWorkbench)) {
                                caseHelper.assertTrue(false, "Ashfall MachineCore consumer proof should create a live Filter Workbench block entity");
                                return;
                            }
                            filterWorkbench.setEnergyStored(777);
                            filterWorkbench.getInventory().setStackInSlot(0, new ItemStack(Items.PAPER, 2));
                            filterWorkbench.getInventory().setStackInSlot(1, new ItemStack(Items.CHARCOAL));
                            filterWorkbench.getInventory().setStackInSlot(3, new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get()));
                            filterWorkbench.getInventory().setStackInSlot(
                                    FilterWorkbenchBlockEntity.BATTERY_SLOT,
                                    BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 80));
                            filterWorkbench.data.set(0, 6);
                        }),
                new AshfallMachineCoreConsumerCase(
                        new BlockPos(8, 6, 2),
                        ModBlocks.RADIATION_CLEANSER.get(),
                        "radiation_cleanser",
                        "Radiation Cleanser",
                        "echoashfallprotocol:radiation_cleansing",
                        778,
                        true,
                        (caseHelper, machine) -> {
                            if (!(machine instanceof RadiationCleanserBlockEntity cleanser)) {
                                caseHelper.assertTrue(false, "Ashfall MachineCore consumer proof should create a live Radiation Cleanser block entity");
                                return;
                            }
                            cleanser.setEnergyStored(778);
                            cleanser.getInventory().setStackInSlot(RadiationCleanserBlockEntity.INPUT_SLOT,
                                    new ItemStack(ModItems.CONTAMINATED_IRON.get()));
                            cleanser.getInventory().setStackInSlot(RadiationCleanserBlockEntity.FILTER_SLOT,
                                    new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get()));
                            cleanser.getInventory().setStackInSlot(RadiationCleanserBlockEntity.OUTPUT_SLOT,
                                    new ItemStack(Items.IRON_INGOT));
                            cleanser.getInventory().setStackInSlot(
                                    RadiationCleanserBlockEntity.BATTERY_SLOT,
                                    BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 80));
                            cleanser.data.set(0, 18);
                            cleanser.data.set(2, 3);
                        }),
                new AshfallMachineCoreConsumerCase(
                        new BlockPos(10, 6, 2),
                        ModBlocks.ISOTOPE_REFINER.get(),
                        "isotope_refiner",
                        "Isotope Refiner",
                        "echoashfallprotocol:isotope_refining",
                        779,
                        true,
                        (caseHelper, machine) -> {
                            if (!(machine instanceof IsotopeRefinerBlockEntity isotope)) {
                                caseHelper.assertTrue(false, "Ashfall MachineCore consumer proof should create a live Isotope Refiner block entity");
                                return;
                            }
                            isotope.setEnergyStored(779);
                            isotope.getInventory().setStackInSlot(IsotopeRefinerBlockEntity.INPUT_SLOT, new ItemStack(Items.IRON_INGOT, 2));
                            isotope.getInventory().setStackInSlot(IsotopeRefinerBlockEntity.CATALYST_SLOT,
                                    new ItemStack(ModItems.CRYSTAL_DUST.get()));
                            isotope.getInventory().setStackInSlot(IsotopeRefinerBlockEntity.OUTPUT_SLOT_1, new ItemStack(Items.GOLD_INGOT));
                            isotope.getInventory().setStackInSlot(
                                    IsotopeRefinerBlockEntity.BATTERY_SLOT,
                                    BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 80));
                            isotope.data.set(0, 22);
                            isotope.data.set(1, 160);
                            isotope.data.set(2, 8);
                            isotope.data.set(5, 779);
                        }),
                new AshfallMachineCoreConsumerCase(
                        new BlockPos(12, 6, 2),
                        ModBlocks.CRYSTALLINE_SYNTHESIZER.get(),
                        "crystalline_synthesizer",
                        "Crystalline Synthesizer",
                        "echoashfallprotocol:crystalline_synthesis",
                        780,
                        true,
                        (caseHelper, machine) -> {
                            if (!(machine instanceof CrystallineSynthesizerBlockEntity synthesizer)) {
                                caseHelper.assertTrue(false, "Ashfall MachineCore consumer proof should create a live Crystalline Synthesizer block entity");
                                return;
                            }
                            synthesizer.setEnergyStored(780);
                            synthesizer.getInventory().setStackInSlot(CrystallineSynthesizerBlockEntity.INPUT_SLOT_1,
                                    new ItemStack(ModItems.GEM_FRAGMENT.get(), 4));
                            synthesizer.getInventory().setStackInSlot(CrystallineSynthesizerBlockEntity.INPUT_SLOT_2,
                                    new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get()));
                            synthesizer.getInventory().setStackInSlot(CrystallineSynthesizerBlockEntity.CATALYST_SLOT,
                                    new ItemStack(ModItems.ENERGY_CELL.get(), 2));
                            synthesizer.getInventory().setStackInSlot(CrystallineSynthesizerBlockEntity.OUTPUT_SLOT, new ItemStack(Items.DIAMOND));
                            synthesizer.getInventory().setStackInSlot(
                                    CrystallineSynthesizerBlockEntity.BATTERY_SLOT,
                                    BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 80));
                            synthesizer.data.set(0, 33);
                            synthesizer.data.set(2, 1);
                        }),
                new AshfallMachineCoreConsumerCase(
                        new BlockPos(14, 6, 2),
                        ModBlocks.THERMAL_ARRAY.get(),
                        "thermal_array",
                        "Thermal Array",
                        "echoashfallprotocol:thermal_array_generation",
                        781,
                        false,
                        (caseHelper, machine) -> {
                            if (!(machine instanceof ThermalArrayBlockEntity thermalArray)) {
                                caseHelper.assertTrue(false, "Ashfall MachineCore consumer proof should create a live Thermal Array block entity");
                                return;
                            }
                            thermalArray.setEnergyStored(781);
                            thermalArray.getInventory().setStackInSlot(0, new ItemStack(Items.COAL, 2));
                            thermalArray.getInventory().setStackInSlot(1, new ItemStack(Items.CHARCOAL));
                            thermalArray.getInventory().setStackInSlot(
                                    ThermalArrayBlockEntity.BATTERY_SLOT,
                                    BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 80));
                            thermalArray.data.set(2, 44);
                            thermalArray.data.set(3, 240);
                        }),
                new AshfallMachineCoreConsumerCase(
                        new BlockPos(16, 6, 2),
                        ModBlocks.ATMOSPHERIC_SCRUBBER.get(),
                        "atmospheric_scrubber",
                        "Atmospheric Scrubber",
                        "echoashfallprotocol:atmospheric_scrubbing",
                        782,
                        false,
                        (caseHelper, machine) -> {
                            if (!(machine instanceof AtmosphericScrubberBlockEntity scrubber)) {
                                caseHelper.assertTrue(false, "Ashfall MachineCore consumer proof should create a live Atmospheric Scrubber block entity");
                                return;
                            }
                            scrubber.setEnergyStored(782);
                        }),
                new AshfallMachineCoreConsumerCase(
                        new BlockPos(18, 6, 2),
                        ModBlocks.CONTAMINANT_CONDENSER.get(),
                        "contaminant_condenser",
                        "Contaminant Condenser",
                        "echoashfallprotocol:contaminant_condensing",
                        783,
                        false,
                        (caseHelper, machine) -> {
                            if (!(machine instanceof ContaminantCondenserBlockEntity condenser)) {
                                caseHelper.assertTrue(false, "Ashfall MachineCore consumer proof should create a live Contaminant Condenser block entity");
                                return;
                            }
                            condenser.setEnergyStored(783);
                        })
        );

        MachineWearData wearData = new MachineWearData(level);
        for (AshfallMachineCoreConsumerCase consumerCase : cases) {
            helper.setBlock(consumerCase.local(), consumerCase.block().defaultBlockState());
            BlockPos pos = helper.absolutePos(consumerCase.local());
            BlockEntity machine = level.getBlockEntity(pos);
            helper.assertTrue(machine != null,
                    "Ashfall MachineCore consumer proof should create a live " + consumerCase.title() + " block entity");
            if (machine == null) {
                continue;
            }
            wearData.repair(pos, MachineWearData.MAX_WEAR);
            consumerCase.seeder().seed(helper, machine);
            machine.setChanged();
        }

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos anchor = helper.absolutePos(cases.get(0).local());
        player.setPos(anchor.above().getCenter());

        List<TerminalRecipeEntry> terminalRecipes = TerminalRecipeRegistry.recipes(player);
        Optional<ServerLensProvider> lensProvider = LensProviderRegistry.serverProviders().stream()
                .filter(provider -> provider.id().equals(Identifier.fromNamespaceAndPath("echolens", "machinecore_runtime")))
                .findFirst();
        helper.assertTrue(lensProvider.isPresent(),
                "Lens should register a server-safe MachineCore Deep Scan provider for Ashfall specialty machines.");

        Object indexService = indexServiceForTest(helper);
        rebuildIndexRecipes(helper, indexService, player);
        List<?> indexEntries = indexEntries(helper, indexService, player);
        List<?> indexRecipes = indexRecipes(helper, indexService, player);
        List<?> indexSourceFacts = indexSourceFacts(helper, indexService, player);
        List<?> markers = holoMapMarkers(helper, player);

        for (AshfallMachineCoreConsumerCase consumerCase : cases) {
            BlockPos pos = helper.absolutePos(consumerCase.local());
            BlockEntity machine = level.getBlockEntity(pos);
            EchoMachineRuntimeSnapshot runtime = EchoMachineRuntimeRegistry.snapshot(level, pos).orElse(null);
            helper.assertTrue(runtime != null
                            && runtime.id().value().equals(consumerCase.machineId().toString())
                            && runtime.process().recipeContract().equals(consumerCase.recipeContract())
                            && runtime.energy().stored() == consumerCase.energy()
                            && EchoMachineUiBridge.hasAutomationSurface(runtime) == consumerCase.expectLogisticsEndpoint(),
                    "Ashfall runtime provider should publish the placed " + consumerCase.title()
                            + " specialty machine through MachineCore with truthful automation-surface state.");

            helper.assertTrue(terminalRecipes.stream().anyMatch(entry -> entry.id().getPath().contains(consumerCase.machinePath())
                            && entry.machine().is(consumerCase.block().asItem())),
                    "Terminal recipe registry should project the Ashfall MachineCore " + consumerCase.title() + " profile.");

            LensContext lensContext = LensContext.block(
                    player,
                    level,
                    pos,
                    machine.getBlockState(),
                    machine.getBlockState().getFluidState(),
                    LensScanMode.DEEP,
                    LensAccessPolicy.ALLOW_DETAILED);
            helper.assertTrue(lensProvider.isPresent() && lensProvider.get().supports(lensContext),
                    "Lens should support the Ashfall MachineCore " + consumerCase.title() + " specialty machine.");
            List<LensInfoSection> lensSections = lensProvider.get().inspect(lensContext);
            helper.assertTrue(lensSections.stream().anyMatch(section -> section.id().toString().equals("echolens:section/machinecore_runtime")
                            && lensSectionContains(section, consumerCase.title())
                            && lensSectionContains(section, Integer.toString(consumerCase.energy()))),
                    "Lens MachineCore provider should expose live " + consumerCase.title()
                            + " identity and energy rows.");

            helper.assertTrue(indexEntries.stream()
                            .anyMatch(entry -> reflectedIdentifier(helper, entry, "id").equals(Identifier.fromNamespaceAndPath("echoindex", consumerCase.machinePath()))),
                    "Index should project the Ashfall MachineCore " + consumerCase.title() + " profile as a machine entry.");
            helper.assertTrue(indexRecipes.stream()
                            .anyMatch(recipe -> reflectedIdentifier(helper, recipe, "id").getPath().contains(consumerCase.machinePath())),
                    "Index should project the Ashfall MachineCore " + consumerCase.title() + " profile as a recipe/process view.");
            helper.assertTrue(indexSourceFacts.stream()
                            .anyMatch(fact -> reflectedIdentifier(helper, fact, "itemId").equals(consumerCase.machineId())
                                    && reflectedIdentifier(helper, fact, "sourceId").getPath().contains(consumerCase.machinePath())),
                    "Index should project the Ashfall MachineCore " + consumerCase.title() + " profile as a source fact.");

            helper.assertTrue(markers.stream().anyMatch(marker -> reflectedIdentifier(helper, marker, "sourceId")
                            .equals(Identifier.fromNamespaceAndPath("echoholomap", "machinecore_runtime"))
                            && consumerCase.title().equals(reflectedString(helper, marker, "title"))
                            && (int) Math.floor(reflectedDouble(helper, marker, "x")) == pos.getX()
                            && (int) Math.floor(reflectedDouble(helper, marker, "y")) == pos.getY()
                            && (int) Math.floor(reflectedDouble(helper, marker, "z")) == pos.getZ()),
                    "HoloMap should project the Ashfall MachineCore " + consumerCase.title()
                            + " snapshot as a precise machine marker.");

            List<?> endpoints = machineCoreLogisticsEndpoints(helper, pos, "ashfall-machinecore-specialty-" + consumerCase.blockPath());
            boolean hasEndpoint = endpoints.stream().anyMatch(endpoint -> pos.equals(reflectedEndpointPos(helper, endpoint)));
            helper.assertTrue(hasEndpoint == consumerCase.expectLogisticsEndpoint(),
                    "Logistics MachineCore endpoint provider should expose routed Ashfall " + consumerCase.title()
                            + " machines only when the MachineCore snapshot has an automation surface.");
            Object logisticsSnapshot = logisticsSnapshot(helper, pos, "ashfall-machinecore-specialty-" + consumerCase.blockPath(), player);
            helper.assertTrue(reflectedInt(helper, logisticsSnapshot, "endpointCount") >= (consumerCase.expectLogisticsEndpoint() ? 1 : 0),
                    "Logistics shared snapshot should keep truthful endpoint counts for Ashfall " + consumerCase.title() + ".");
        }
    }

    private record AshfallMachineCoreConsumerCase(
            BlockPos local,
            net.minecraft.world.level.block.Block block,
            String blockPath,
            String title,
            String recipeContract,
            int energy,
            boolean expectLogisticsEndpoint,
            AshfallMachineCoreConsumerSeeder seeder
    ) {
        Identifier machineId() {
            return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, blockPath);
        }

        String machinePath() {
            return "machinecore/" + EchoAshfallProtocol.MODID + "/" + blockPath;
        }
    }

    @FunctionalInterface
    private interface AshfallMachineCoreConsumerSeeder {
        void seed(GameTestHelper helper, BlockEntity machine);
    }

    private static void assertMachineCoreProfileBinding(GameTestHelper helper, BlockEntity machine, String recipeId) {
        EchoMachineProfile profile = AshfallMachineCoreAdapter.profile(machine);
        helper.assertTrue(profile.recipeBindings().stream().anyMatch(binding -> binding.recipeId() != null
                        && binding.recipeId().value().equals(recipeId)),
                "MachineCore profile should expose Ashfall recipe binding " + recipeId);
        helper.assertTrue(profile.automationHooks().stream().anyMatch(hook -> hook.kind().serializedName().equals("remote_status"))
                        && profile.maintenanceProfile().supportsFieldRepair(),
                "MachineCore profile should expose remote status and field-maintenance hooks for " + recipeId);
    }

    private static boolean lensSectionContains(LensInfoSection section, String text) {
        if (section == null || text == null || text.isBlank()) {
            return false;
        }
        if (section.title().getString().contains(text)) {
            return true;
        }
        return section.rows().stream()
                .anyMatch(row -> row.label().getString().contains(text) || row.value().getString().contains(text));
    }

    private static void registerMachineCoreIndexForTest(GameTestHelper helper) {
        Object indexService = indexServiceForTest(helper);
        EchoCoreServices.registerIndexService(indexService);
        try {
            Class.forName("com.knoxhack.echoindex.integration.MachineCoreIndexIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Index MachineCore integration should be loadable for Ashfall consumer proof: "
                    + exception.getMessage());
        }
    }

    private static Object indexServiceForTest(GameTestHelper helper) {
        try {
            return Class.forName("com.knoxhack.echoindex.service.IndexService")
                    .getField("INSTANCE")
                    .get(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "IndexService should be loadable for Ashfall consumer proof: "
                    + exception.getMessage());
            return null;
        }
    }

    private static void rebuildIndexRecipes(GameTestHelper helper, Object indexService, Player player) {
        try {
            indexService.getClass()
                    .getMethod("rebuildRecipes", Player.class, String.class)
                    .invoke(indexService, player, "ashfall machinecore specialty consumer gametest");
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "IndexService.rebuildRecipes should be callable for Ashfall consumer proof: "
                    + exception.getMessage());
        }
    }

    private static List<?> indexEntries(GameTestHelper helper, Object indexService, Player player) {
        return reflectedList(helper, indexService, "entries", player);
    }

    private static List<?> indexRecipes(GameTestHelper helper, Object indexService, Player player) {
        return reflectedList(helper, indexService, "recipes", player);
    }

    private static List<?> indexSourceFacts(GameTestHelper helper, Object indexService, Player player) {
        return reflectedList(helper, indexService, "sourceFacts", player);
    }

    private static void registerMachineCoreHoloMapForTest(GameTestHelper helper) {
        try {
            Class.forName("com.knoxhack.echoholomap.integration.MachineCoreHoloMapIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "HoloMap MachineCore integration should be loadable for Ashfall consumer proof: "
                    + exception.getMessage());
        }
    }

    private static List<?> holoMapMarkers(GameTestHelper helper, Player player) {
        try {
            Object service = Class.forName("com.knoxhack.echoholomap.map.HoloMapService")
                    .getField("INSTANCE")
                    .get(null);
            Object value = service.getClass()
                    .getMethod("richMarkers", Player.class)
                    .invoke(service, player);
            if (value instanceof List<?> list) {
                return list;
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "HoloMap rich marker service should be callable for Ashfall consumer proof: "
                    + exception.getMessage());
        }
        return List.of();
    }

    private static void registerMachineCoreLogisticsForTest(GameTestHelper helper) {
        try {
            Class.forName("com.knoxhack.echologisticsnetwork.integration.MachineCoreLogisticsEndpointProvider")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Logistics MachineCore endpoint provider should be loadable for Ashfall consumer proof: "
                    + exception.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static List<?> machineCoreLogisticsEndpoints(GameTestHelper helper, BlockPos origin, String networkId) {
        try {
            Object value = Class.forName("com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService")
                    .getMethod("externalEndpointsFromProvider", net.minecraft.world.level.Level.class, BlockPos.class, String.class, Identifier.class)
                    .invoke(null, helper.getLevel(), origin, networkId,
                            Identifier.fromNamespaceAndPath("echologisticsnetwork", "machinecore_machine_endpoints"));
            if (value instanceof List<?> endpoints) {
                return endpoints;
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Logistics MachineCore endpoint diagnostics should be callable for Ashfall consumer proof: "
                    + exception.getMessage());
        }
        return List.of();
    }

    private static Object logisticsSnapshot(GameTestHelper helper, BlockPos origin, String networkId, Player player) {
        try {
            Class<?> service = Class.forName("com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService");
            service.getMethod("invalidateSnapshots").invoke(null);
            return service
                    .getMethod("snapshot", net.minecraft.world.level.Level.class, BlockPos.class, String.class, Player.class, boolean.class)
                    .invoke(null, helper.getLevel(), origin, networkId, player, true);
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Logistics shared snapshot should be callable for Ashfall consumer proof: "
                    + exception.getMessage());
            return null;
        }
    }

    private static BlockPos reflectedEndpointPos(GameTestHelper helper, Object endpoint) {
        try {
            Object value = endpoint == null ? null : endpoint.getClass().getMethod("pos").invoke(endpoint);
            if (value instanceof BlockPos pos) {
                return pos;
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Logistics external endpoint position should be readable for Ashfall consumer proof: "
                    + exception.getMessage());
        }
        return BlockPos.ZERO;
    }

    private static List<?> reflectedList(GameTestHelper helper, Object target, String method, Player player) {
        try {
            Object value = target == null ? null : target.getClass().getMethod(method, Player.class).invoke(target, player);
            if (value instanceof List<?> list) {
                return list;
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "List accessor should be readable for Ashfall consumer proof: "
                    + method + ": " + exception.getMessage());
        }
        return List.of();
    }

    private static Identifier reflectedIdentifier(GameTestHelper helper, Object target, String method) {
        try {
            Object value = target == null ? null : target.getClass().getMethod(method).invoke(target);
            if (value instanceof Identifier id) {
                return id;
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Identifier accessor should be readable for Ashfall consumer proof: "
                    + method + ": " + exception.getMessage());
        }
        return Identifier.withDefaultNamespace("air");
    }

    private static String reflectedString(GameTestHelper helper, Object target, String method) {
        try {
            Object value = target == null ? null : target.getClass().getMethod(method).invoke(target);
            if (value instanceof String string) {
                return string;
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "String accessor should be readable for Ashfall consumer proof: "
                    + method + ": " + exception.getMessage());
        }
        return "";
    }

    private static double reflectedDouble(GameTestHelper helper, Object target, String method) {
        try {
            Object value = target == null ? null : target.getClass().getMethod(method).invoke(target);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Numeric accessor should be readable for Ashfall consumer proof: "
                    + method + ": " + exception.getMessage());
        }
        return 0.0D;
    }

    private static int reflectedInt(GameTestHelper helper, Object target, String method) {
        try {
            Object value = target == null ? null : target.getClass().getMethod(method).invoke(target);
            if (value instanceof Number number) {
                return number.intValue();
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Logistics snapshot integer accessor should be readable for Ashfall consumer proof: "
                    + method + ": " + exception.getMessage());
        }
        return 0;
    }
}
