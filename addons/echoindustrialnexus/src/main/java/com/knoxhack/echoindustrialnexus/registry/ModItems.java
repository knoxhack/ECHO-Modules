package com.knoxhack.echoindustrialnexus.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echomultiblockcore.api.RobotToolType;
import com.knoxhack.echomultiblockcore.item.BlueprintItem;
import com.knoxhack.echomultiblockcore.item.ToolHeadItem;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.item.EmergencyCoolantPackItem;
import com.knoxhack.echoindustrialnexus.item.FluxMultimeterItem;
import com.knoxhack.echoindustrialnexus.item.FactoryDiagnosticToolItem;
import com.knoxhack.echoindustrialnexus.item.FurnaceWardenSummonerItem;
import com.knoxhack.echoindustrialnexus.item.SalvageMagnetItem;
import com.knoxhack.echoindustrialnexus.item.ThermalWrenchItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Block;

public final class ModItems {
   public static final Object ITEMS = EchoBackendRegistryBridge.create(BuiltInRegistries.ITEM, EchoIndustrialNexus.MODID);
   private static final List<EchoBackendRegistryEntry<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();
   public static final EchoBackendRegistryEntry<Item> SCRAP_METAL = simple("scrap_metal");
   public static final EchoBackendRegistryEntry<Item> SCRAP_FUEL = simple("scrap_fuel");
   public static final EchoBackendRegistryEntry<Item> COMPACTED_ASH_FUEL = simple("compacted_ash_fuel");
   public static final EchoBackendRegistryEntry<Item> THERMAL_DUST = simple("thermal_dust");
   public static final EchoBackendRegistryEntry<Item> RUST_DUST = simple("rust_dust");
   public static final EchoBackendRegistryEntry<Item> CIRCUIT_DUST = simple("circuit_dust");
   public static final EchoBackendRegistryEntry<Item> IRON_DUST = simple("iron_dust");
   public static final EchoBackendRegistryEntry<Item> COPPER_DUST = simple("copper_dust");
   public static final EchoBackendRegistryEntry<Item> GOLD_DUST = simple("gold_dust");
   public static final EchoBackendRegistryEntry<Item> URANIUM_DUST = simple("uranium_dust");
   public static final EchoBackendRegistryEntry<Item> NEXUS_DUST = simple("nexus_dust", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> RAD_SLAG = simple("rad_slag", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> BROKEN_CIRCUIT = simple("broken_circuit");
   public static final EchoBackendRegistryEntry<Item> OLD_CIRCUIT = simple("old_circuit");
   public static final EchoBackendRegistryEntry<Item> ECHO_CIRCUIT = simple("echo_circuit", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> COPPER_WIRE = simple("copper_wire");
   public static final EchoBackendRegistryEntry<Item> SIGNAL_WIRE = simple("signal_wire", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> SERVO_MOTOR = simple("servo_motor");
   public static final EchoBackendRegistryEntry<Item> PRESSURE_VALVE = simple("pressure_valve");
   public static final EchoBackendRegistryEntry<Item> FIELD_RELAY = simple("field_relay", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> THERMAL_REGULATOR = simple("thermal_regulator", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> SCRAP_PLATE = simple("scrap_plate");
   public static final EchoBackendRegistryEntry<Item> REFINED_PLATE = simple("refined_plate");
   public static final EchoBackendRegistryEntry<Item> REINFORCED_PLATE = simple("reinforced_plate", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> HEAT_COIL = simple("heat_coil");
   public static final EchoBackendRegistryEntry<Item> FLUX_CRYSTAL = simple("flux_crystal", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> STABILIZED_SLAG = simple("stabilized_slag");
   public static final EchoBackendRegistryEntry<Item> INDUSTRIAL_MEMBRANE = simple("industrial_membrane");
   public static final EchoBackendRegistryEntry<Item> FIELD_MEMBRANE = simple("field_membrane", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> INDUSTRIAL_FILTER_CORE = simple("industrial_filter_core", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> GAS_MASK_FILTER = simple("gas_mask_filter");
   public static final EchoBackendRegistryEntry<Item> COOLANT_CELL = simple("coolant_cell");
   public static final EchoBackendRegistryEntry<Item> DENSE_ALLOY_FRAGMENT = simple("dense_alloy_fragment");
   public static final EchoBackendRegistryEntry<Item> DENSE_ALLOY_INGOT = simple("dense_alloy_ingot");
   public static final EchoBackendRegistryEntry<Item> DENSE_ALLOY_PLATE = simple("dense_alloy_plate");
   public static final EchoBackendRegistryEntry<Item> STABILIZED_ALLOY_INGOT = simple("stabilized_alloy_ingot", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> STABILIZED_ALLOY_PLATE = simple("stabilized_alloy_plate", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> STABLE_NEXUS_CORE = simple("stable_nexus_core", p -> p.rarity(Rarity.RARE).fireResistant());
   public static final EchoBackendRegistryEntry<Item> HYBRID_THERMAL_CORE = simple("hybrid_thermal_core", p -> p.rarity(Rarity.RARE).fireResistant());
   public static final EchoBackendRegistryEntry<Item> MACHINE_FRAME = simple("machine_frame");
   public static final EchoBackendRegistryEntry<Item> REINFORCED_MACHINE_FRAME = simple("reinforced_machine_frame", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> PRECISION_CIRCUIT = simple("precision_circuit", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> INDUSTRIAL_CIRCUIT = simple("industrial_circuit", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> ROBOTIC_JOINT = simple("robotic_joint");
   public static final EchoBackendRegistryEntry<Item> HYDRAULIC_ACTUATOR = simple("hydraulic_actuator");
   public static final EchoBackendRegistryEntry<Item> CONVEYOR_GEAR = simple("conveyor_gear");
   public static final EchoBackendRegistryEntry<Item> COOLING_COIL = simple("cooling_coil");
   public static final EchoBackendRegistryEntry<Item> DATA_PROCESSOR = simple("data_processor", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> RECIPE_MATRIX_SHARD = simple("recipe_matrix_shard", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> INDUSTRIAL_UPGRADE_CHIP = simple("industrial_upgrade_chip", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> SPEED_UPGRADE_CHIP = simple("speed_upgrade_chip", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> COOLING_UPGRADE_CHIP = simple("cooling_upgrade_chip", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> PRECISION_UPGRADE_CHIP = simple("precision_upgrade_chip", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> STABILIZED_MACHINE_FRAME = simple("stabilized_machine_frame", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> HYBRID_NEXUS_FRAME = simple("hybrid_nexus_frame", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> SPEED_SERVO = simple("speed_servo", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> EFFICIENCY_COIL = simple("efficiency_coil", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> HEAT_SINK_UPGRADE = simple("heat_sink_upgrade");
   public static final EchoBackendRegistryEntry<Item> FILTER_MODULE = simple("filter_module");
   public static final EchoBackendRegistryEntry<Item> SECONDARY_OUTPUT_MODULE = simple("secondary_output_module", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> RADIATION_SHIELDING_UPGRADE = simple("radiation_shielding_upgrade", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> NEXUS_STABILIZER_UPGRADE = simple("nexus_stabilizer_upgrade", p -> p.rarity(Rarity.RARE).fireResistant());
   public static final EchoBackendRegistryEntry<Item> FACTORY_LINK_CHIP = simple("factory_link_chip", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> OVERCLOCK_CORE = simple("overclock_core", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> EMERGENCY_SHUTDOWN_MODULE = simple("emergency_shutdown_module", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> DIRTY_WATER_CELL = simple("dirty_water_cell");
   public static final EchoBackendRegistryEntry<Item> CLEAN_WATER_CELL = simple("clean_water_cell");
   public static final EchoBackendRegistryEntry<Item> TOXIC_SLUDGE_CELL = simple("toxic_sludge_cell");
   public static final EchoBackendRegistryEntry<Item> CHEMICAL_SOLVENT = simple("chemical_solvent");
   public static final EchoBackendRegistryEntry<Item> WASTE_CANISTER = simple("waste_canister");
   public static final EchoBackendRegistryEntry<Item> STATIC_FLUID_CELL = simple("static_fluid_cell", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> NEXUS_GEL = simple("nexus_gel", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> CRYO_GEL = simple("cryo_gel");
   public static final EchoBackendRegistryEntry<Item> CRYO_DUST = simple("cryo_dust");
   public static final EchoBackendRegistryEntry<Item> FROZEN_CORE = simple("frozen_core", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> FUEL_CELL = simple("fuel_cell");
   public static final EchoBackendRegistryEntry<Item> TAR = simple("tar");
   public static final EchoBackendRegistryEntry<Item> PRESSURE_COMPONENT = simple("pressure_component", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> OXYGEN_COMPRESSOR_PART = simple("oxygen_compressor_part", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> SOLAR_GLASS = simple("solar_glass", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> LAUNCH_PLATFORM_FRAME = simple("launch_platform_frame", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> STATION_BATTERY = simple("station_battery", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> PRESSURE_SEAL_KIT = simple("pressure_seal_kit");
   public static final EchoBackendRegistryEntry<Item> EMERGENCY_OXYGEN_FILTER = simple("emergency_oxygen_filter");
   public static final EchoBackendRegistryEntry<Item> HULL_REPAIR_FOAM = simple("hull_repair_foam");
   public static final EchoBackendRegistryEntry<Item> AI_OVERRIDE_CHIP_CASING = simple("ai_override_chip_casing", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> SIGNAL_PANIC_DAMPENER = simple("signal_panic_dampener", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> CORE_KEY_ASSEMBLY = simple("core_key_assembly", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> TRUTH_ENGINE_PART = simple("truth_engine_part", p -> p.rarity(Rarity.RARE).fireResistant());
   public static final EchoBackendRegistryEntry<Item> MEMORY_STABILIZER_CASING = simple("memory_stabilizer_casing", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> BLACKBOX_DECODER_COOLING_SYSTEM = simple("blackbox_decoder_cooling_system", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> PROTOCOL_EXTRACTOR_COIL = simple("protocol_extractor_coil", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> WARDEN_THERMAL_CORE = simple("warden_thermal_core", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> FURNACE_WARDEN_TROPHY = simple("furnace_warden_trophy", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> FURNACE_WARDEN_WAKE_CORE = tracked(
      item("furnace_warden_wake_core", FurnaceWardenSummonerItem::new, p -> p.stacksTo(1).rarity(Rarity.EPIC).fireResistant())
   );
   public static final EchoBackendRegistryEntry<Item> THERMAL_WRENCH = tracked(
      item("thermal_wrench", ThermalWrenchItem::new, p -> p.stacksTo(1).durability(256))
   );
   public static final EchoBackendRegistryEntry<Item> FLUX_MULTIMETER = tracked(
      item("flux_multimeter", FluxMultimeterItem::new, p -> p.stacksTo(1).durability(256))
   );
   public static final EchoBackendRegistryEntry<Item> EMERGENCY_COOLANT_PACK = tracked(
      item("emergency_coolant_pack", EmergencyCoolantPackItem::new, p -> p.stacksTo(16))
   );
   public static final EchoBackendRegistryEntry<Item> SALVAGE_MAGNET = tracked(
      item("salvage_magnet", SalvageMagnetItem::new, p -> p.stacksTo(1).durability(192).rarity(Rarity.UNCOMMON))
   );
   public static final EchoBackendRegistryEntry<Item> FACTORY_DIAGNOSTIC_TOOL = tracked(
      item("factory_diagnostic_tool", FactoryDiagnosticToolItem::new, p -> p.stacksTo(1).durability(256))
   );
   public static final EchoBackendRegistryEntry<Item> ASSEMBLY_LINE_BLUEPRINT = blueprint("assembly_line_blueprint", "industrial_assembly_line");
   public static final EchoBackendRegistryEntry<Item> SCRAP_PROCESSOR_BLUEPRINT = blueprint("scrap_processor_blueprint", "scrap_processor");
   public static final EchoBackendRegistryEntry<Item> PLATE_PRESS_BLUEPRINT = blueprint("plate_press_blueprint", "plate_press");
   public static final EchoBackendRegistryEntry<Item> CIRCUIT_FABRICATOR_BLUEPRINT = blueprint("circuit_fabricator_blueprint", "circuit_fabricator");
   public static final EchoBackendRegistryEntry<Item> RECIPE_MATRIX_BLUEPRINT = blueprint("recipe_matrix_blueprint", "recipe_matrix_core");
   public static final EchoBackendRegistryEntry<Item> NEXUS_FURNACE_ARRAY_BLUEPRINT = blueprint("nexus_furnace_array_blueprint", "nexus_furnace_array");
   public static final EchoBackendRegistryEntry<Item> INDUSTRIAL_WELDER_HEAD = toolHead("industrial_welder_head", RobotToolType.WELDER);
   public static final EchoBackendRegistryEntry<Item> PRECISION_ASSEMBLER_HEAD = toolHead("precision_assembler_head", RobotToolType.ASSEMBLER);
   public static final EchoBackendRegistryEntry<Item> PLASMA_CUTTER_HEAD = toolHead("plasma_cutter_head", RobotToolType.CUTTER);
   public static final EchoBackendRegistryEntry<Item> INSPECTION_SCANNER_HEAD = toolHead("inspection_scanner_head", RobotToolType.SCANNER);
   public static final EchoBackendRegistryEntry<Item> HEAVY_GRIPPER_HEAD = toolHead("heavy_gripper_head", RobotToolType.GRIPPER);
   public static final EchoBackendRegistryEntry<Item> COOLANT_INJECTOR_HEAD = toolHead("coolant_injector_head", RobotToolType.INJECTOR);
   public static final EchoBackendRegistryEntry<Item> INDUSTRIAL_EXO_HELMET = armor("industrial_exo_helmet", ArmorType.HELMET);
   public static final EchoBackendRegistryEntry<Item> INDUSTRIAL_EXO_CHESTPLATE = armor("industrial_exo_chestplate", ArmorType.CHESTPLATE);
   public static final EchoBackendRegistryEntry<Item> INDUSTRIAL_EXO_LEGGINGS = armor("industrial_exo_leggings", ArmorType.LEGGINGS);
   public static final EchoBackendRegistryEntry<Item> INDUSTRIAL_EXO_BOOTS = armor("industrial_exo_boots", ArmorType.BOOTS);
   public static final EchoBackendRegistryEntry<Item> NANO_CARBON_ALLOY_INGOT = simple("nano_carbon_alloy_ingot", p -> p);
   public static final EchoBackendRegistryEntry<Item> NANO_CARBON_ALLOY_PLATE = simple("nano_carbon_alloy_plate", p -> p);
   public static final EchoBackendRegistryEntry<Item> NANO_CARBON_WEAVE = simple("nano_carbon_weave", p -> p);
   public static final EchoBackendRegistryEntry<Item> REACTOR_STEEL_INGOT = simple("reactor_steel_ingot", p -> p);
   public static final EchoBackendRegistryEntry<Item> REACTOR_STEEL_PLATE = simple("reactor_steel_plate", p -> p);
   public static final EchoBackendRegistryEntry<Item> REACTOR_STEEL_FRAME = simple("reactor_steel_frame", p -> p);
   public static final EchoBackendRegistryEntry<Item> IONIZED_TITANIUM_INGOT = simple("ionized_titanium_ingot", p -> p);
   public static final EchoBackendRegistryEntry<Item> IONIZED_TITANIUM_PLATE = simple("ionized_titanium_plate", p -> p);
   public static final EchoBackendRegistryEntry<Item> PLASMA_INFUSED_ALLOY_INGOT = simple("plasma_infused_alloy_ingot", p -> p);
   public static final EchoBackendRegistryEntry<Item> PLASMA_INFUSED_ALLOY_PLATE = simple("plasma_infused_alloy_plate", p -> p);
   public static final EchoBackendRegistryEntry<Item> FLUX_CONDUCTOR_WIRE = simple("flux_conductor_wire", p -> p);
   public static final EchoBackendRegistryEntry<Item> FLUX_CONDUCTOR_MESH = simple("flux_conductor_mesh", p -> p);
   public static final EchoBackendRegistryEntry<Item> LUMINOUS_POLYMER_SHEET = simple("luminous_polymer_sheet", p -> p);
   public static final EchoBackendRegistryEntry<Item> LUMINOUS_POLYMER_LENS = simple("luminous_polymer_lens", p -> p);
   public static final EchoBackendRegistryEntry<Item> SYNTHETIC_CRYSTAL_SHARD = simple("synthetic_crystal_shard", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> SYNTHETIC_CRYSTAL_CORE = simple("synthetic_crystal_core", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> DIMENSIONAL_GLASS_SHARD = simple("dimensional_glass_shard", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> DIMENSIONAL_GLASS_PANEL = simple("dimensional_glass_panel", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> QUANTUM_CIRCUITRY_WAFER = simple("quantum_circuitry_wafer", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> QUANTUM_CIRCUITRY_BOARD = simple("quantum_circuitry_board", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> AI_NEURAL_SUBSTRATE = simple("ai_neural_substrate", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> NEURAL_MEMORY_LATTICE = simple("neural_memory_lattice", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> CYAN_PHOTON_EMITTER = simple("cyan_photon_emitter", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> ELECTRIC_BLUE_RESONATOR = simple("electric_blue_resonator", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> WHITE_LIGHT_MATRIX = simple("white_light_matrix", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> ORANGE_WARNING_DIODE = simple("orange_warning_diode", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> PURPLE_DIMENSIONAL_RESONATOR = simple("purple_dimensional_resonator", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> GREEN_REACTOR_ISOTOPE = simple("green_reactor_isotope", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> POLISHED_STEEL_MICROGEAR = simple("polished_steel_microgear", p -> p);
   public static final EchoBackendRegistryEntry<Item> GRAPHITE_MACHINE_PIN = simple("graphite_machine_pin", p -> p);
   public static final EchoBackendRegistryEntry<Item> CARBON_SEALED_BEARING = simple("carbon_sealed_bearing", p -> p);
   public static final EchoBackendRegistryEntry<Item> MODULAR_PANEL_FASTENER = simple("modular_panel_fastener", p -> p);
   public static final EchoBackendRegistryEntry<Item> MICRO_FLUX_CELL = simple("micro_flux_cell", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> STANDARD_FLUX_CELL = simple("standard_flux_cell", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> DENSE_FLUX_CELL = simple("dense_flux_cell", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> REACTOR_POWER_CELL = simple("reactor_power_cell", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> QUANTUM_POWER_CELL = simple("quantum_power_cell", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> AI_CONTROL_CHIP = simple("ai_control_chip", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> NEURAL_LOGIC_CORE = simple("neural_logic_core", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> AUTONOMOUS_PATHING_MODULE = simple("autonomous_pathing_module", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> MACHINE_VISION_ARRAY = simple("machine_vision_array", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> HOLOGRAPHIC_PROJECTOR_CHIP = simple("holographic_projector_chip", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> INDUSTRIAL_SENSOR_CLUSTER = simple("industrial_sensor_cluster", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> CYBERNETIC_SERVO_BUNDLE = simple("cybernetic_servo_bundle", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> PRECISION_ROBOTIC_HAND = simple("precision_robotic_hand", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> DRONE_ROTOR_CORE = simple("drone_rotor_core", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> DRONE_OPTIC_EYE = simple("drone_optic_eye", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> MAINTENANCE_DRONE_SHELL = simple("maintenance_drone_shell", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> ASSEMBLY_DRONE_CORE = simple("assembly_drone_core", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> LOGISTICS_DRONE_CORE = simple("logistics_drone_core", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> AUTOMATION_BUS_MODULE = simple("automation_bus_module", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> INPUT_FILTER_MODULE = simple("input_filter_module", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> OUTPUT_SORTER_MODULE = simple("output_sorter_module", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> OVERCLOCK_HEAT_SPREADER = simple("overclock_heat_spreader", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> COOLANT_DISTRIBUTION_MANIFOLD = simple("coolant_distribution_manifold", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> SHIELDED_DATA_CABLE = simple("shielded_data_cable", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> ENCRYPTED_CONTROL_BUS = simple("encrypted_control_bus", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> MODULAR_STORAGE_CORE = simple("modular_storage_core", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> DIMENSIONAL_STORAGE_LATTICE = simple("dimensional_storage_lattice", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> ITEM_COMPRESSION_MATRIX = simple("item_compression_matrix", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> FLUID_REGULATION_CORE = simple("fluid_regulation_core", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> REACTOR_SAFETY_INTERLOCK = simple("reactor_safety_interlock", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> RADIATION_CONTROL_MATRIX = simple("radiation_control_matrix", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> FACTORY_SYNC_TRANSPONDER = simple("factory_sync_transponder", p -> p.rarity(Rarity.UNCOMMON));
   public static final EchoBackendRegistryEntry<Item> NANO_FABRICATOR_TOOL = simple("nano_fabricator_tool", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> ION_TORCH = simple("ion_torch", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> QUANTUM_MULTITOOL = simple("quantum_multitool", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> HOLOGRAPHIC_SCHEMATIC_TABLET = simple("holographic_schematic_tablet", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> CYBERNETIC_INTERFACE_SPINE = simple("cybernetic_interface_spine", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> NEURAL_CALIBRATION_HARNESS = simple("neural_calibration_harness", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> REACTOR_GAUNTLET_CORE = simple("reactor_gauntlet_core", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> PLASMA_CUTTER_BLADE = simple("plasma_cutter_blade", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> FLUX_RIFLE_RECEIVER = simple("flux_rifle_receiver", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> MODULAR_WEAPON_CHASSIS = simple("modular_weapon_chassis", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> IONIZED_TITANIUM_BARREL = simple("ionized_titanium_barrel", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> QUANTUM_FOCUS_LENS = simple("quantum_focus_lens", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> AI_TARGETING_PROCESSOR = simple("ai_targeting_processor", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> DIMENSIONAL_ANCHOR = simple("dimensional_anchor", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> PHASE_STABILIZER = simple("phase_stabilizer", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> RIFT_CONTAINMENT_ORB = simple("rift_containment_orb", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> REALITY_ALIGNMENT_CORE = simple("reality_alignment_core", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> SYNTHETIC_ECOLOGY_SEED = simple("synthetic_ecology_seed", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> BIO_SYNTHETIC_FILTER = simple("bio_synthetic_filter", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> CLEANROOM_GROWTH_CAPSULE = simple("cleanroom_growth_capsule", p -> p.rarity(Rarity.RARE));
   public static final EchoBackendRegistryEntry<Item> ANCIENT_FACTORY_KEY = simple("ancient_factory_key", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> MEGAFACTORY_ACCESS_CIPHER = simple("megafactory_access_cipher", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> REACTOR_AI_BLACKBOX = simple("reactor_ai_blackbox", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> DORMANT_OVERSEER_CORE = simple("dormant_overseer_core", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> PRECURSOR_ALIGNMENT_CORE = simple("precursor_alignment_core", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> QUANTUM_ARCHIVE_DRIVE = simple("quantum_archive_drive", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> NULLSPACE_STORAGE_KEY = simple("nullspace_storage_key", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> LUMINOUS_CONTROL_KEY = simple("luminous_control_key", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> PLASMA_REACTOR_CROWN = simple("plasma_reactor_crown", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> NEXUS_INDUSTRIAL_CONDUIT = simple("nexus_industrial_conduit", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> DIMENSIONAL_RESEARCH_ARTIFACT = simple("dimensional_research_artifact", p -> p.rarity(Rarity.EPIC).fireResistant());
   public static final EchoBackendRegistryEntry<Item> INDUSTRIAL_NEXUS_HEART = simple("industrial_nexus_heart", p -> p.rarity(Rarity.EPIC).fireResistant());

   private ModItems() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(ITEMS, eventBus);
   }

   public static List<EchoBackendRegistryEntry<? extends Item>> creativeItems() {
      return List.copyOf(CREATIVE_ITEMS);
   }

   private static EchoBackendRegistryEntry<Item> simple(String name) {
      return simple(name, p -> p);
   }

   private static EchoBackendRegistryEntry<Item> armor(String name, ArmorType type) {
      return simple(name, p -> p.humanoidArmor(ArmorMaterials.IRON, type).rarity(Rarity.UNCOMMON));
   }

   private static EchoBackendRegistryEntry<Item> simple(String name, UnaryOperator<Properties> properties) {
      return tracked(item(name, Item::new, properties));
   }

   private static EchoBackendRegistryEntry<Item> toolHead(String name, RobotToolType toolType) {
      return tracked(item(name, properties -> new ToolHeadItem(toolType, properties), p -> p.stacksTo(1).rarity(Rarity.UNCOMMON)));
   }

   private static EchoBackendRegistryEntry<Item> blueprint(String name, String definitionPath) {
      return tracked(item(
         name,
         properties -> new BlueprintItem(EchoIndustrialNexus.id(definitionPath), properties),
         p -> p.stacksTo(1).rarity(Rarity.UNCOMMON)
      ));
   }

   private static <T extends Item> EchoBackendRegistryEntry<T> tracked(EchoBackendRegistryEntry<T> item) {
      CREATIVE_ITEMS.add(item);
      return item;
   }

   private static <T extends Item> EchoBackendRegistryEntry<T> item(
      String name,
      Function<Properties, ? extends T> factory,
      UnaryOperator<Properties> properties
   ) {
      return EchoBackendRegistryBridge.registerWithId(
         ITEMS,
         name,
         id -> factory.apply(withId(properties.apply(new Properties()), id))
      );
   }

   private static EchoBackendRegistryEntry<BlockItem> blockItem(EchoBackendRegistryEntry<? extends Block> block) {
      Identifier id = BuiltInRegistries.BLOCK.getKey(block.get());
      return EchoBackendRegistryBridge.registerWithId(
         ITEMS,
         id.getPath(),
         itemId -> new BlockItem(block.get(), withId(new Properties(), itemId))
      );
   }

   private static Properties withId(Properties properties, Identifier id) {
      return properties.setId(ResourceKey.create(Registries.ITEM, id));
   }

   static {
      ModBlocks.ALL_BLOCKS.forEach(block -> tracked(blockItem(block)));
   }
}
