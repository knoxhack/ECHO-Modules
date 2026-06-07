package com.knoxhack.echoconvoyprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoconvoyprotocol.block.ConvoyMultiblockControllerBlock;
import com.knoxhack.echoconvoyprotocol.block.ConvoyMultiblockCrateBlock;
import com.knoxhack.echoconvoyprotocol.block.ConvoyBlock;
import com.knoxhack.echoconvoyprotocol.block.ConvoyBlock.ConvoyBlockKind;
import com.knoxhack.echomultiblockcore.block.MultiblockCrateBlock.CrateKind;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
   public static final Object BLOCKS = EchoBackendRegistryBridge.create(BuiltInRegistries.BLOCK, EchoConvoyProtocol.MODID);

   public static final EchoBackendRegistryEntry<Block> VEHICLE_WORKBENCH = convoy(ConvoyBlockKind.VEHICLE_WORKBENCH, MapColor.METAL, 3.5F);
   public static final EchoBackendRegistryEntry<Block> FUEL_STILL = convoy(ConvoyBlockKind.FUEL_STILL, MapColor.COLOR_ORANGE, 3.0F);
   public static final EchoBackendRegistryEntry<Block> BATTERY_CHARGING_PAD = convoy(ConvoyBlockKind.BATTERY_CHARGING_PAD, MapColor.COLOR_LIGHT_BLUE, 2.5F);
   public static final EchoBackendRegistryEntry<Block> VEHICLE_DOCK = convoy(ConvoyBlockKind.VEHICLE_DOCK, MapColor.COLOR_GRAY, 4.0F);
   public static final EchoBackendRegistryEntry<Block> VEHICLE_UPGRADE_BAY = convoy(ConvoyBlockKind.VEHICLE_UPGRADE_BAY, MapColor.METAL, 4.0F);
   public static final EchoBackendRegistryEntry<Block> CONVOY_BEACON = convoy(ConvoyBlockKind.CONVOY_BEACON, MapColor.COLOR_CYAN, 3.0F);
   public static final EchoBackendRegistryEntry<Block> ROADSIDE_SIGNAL_MARKER = convoy(ConvoyBlockKind.ROADSIDE_SIGNAL_MARKER, MapColor.COLOR_YELLOW, 1.5F);
   public static final EchoBackendRegistryEntry<Block> CARGO_ANCHOR = convoy(ConvoyBlockKind.CARGO_ANCHOR, MapColor.WOOD, 2.5F);
   public static final EchoBackendRegistryEntry<Block> FIELD_REPAIR_STATION = convoy(ConvoyBlockKind.FIELD_REPAIR_STATION, MapColor.METAL, 3.0F);

   public static final EchoBackendRegistryEntry<Block> CONVOY_DEPOT_CONTROLLER =
      controller("convoy_depot_controller", "convoy_depot", MapColor.COLOR_CYAN);
   public static final EchoBackendRegistryEntry<Block> VEHICLE_REPAIR_GANTRY_CONTROLLER =
      controller("vehicle_repair_gantry_controller", "vehicle_repair_gantry", MapColor.METAL);
   public static final EchoBackendRegistryEntry<Block> CARGO_LOADING_BAY_CONTROLLER =
      controller("cargo_loading_bay_controller", "cargo_loading_bay", MapColor.COLOR_YELLOW);
   public static final EchoBackendRegistryEntry<Block> FUEL_REFINERY_PAD_CONTROLLER =
      controller("fuel_refinery_pad_controller", "fuel_refinery_pad", MapColor.COLOR_ORANGE);
   public static final EchoBackendRegistryEntry<Block> ROUTE_DISPATCH_TOWER_CONTROLLER =
      controller("route_dispatch_tower_controller", "route_dispatch_tower", MapColor.COLOR_LIGHT_BLUE);
   public static final EchoBackendRegistryEntry<Block> MOBILE_COMMAND_GARAGE_CONTROLLER =
      controller("mobile_command_garage_controller", "mobile_command_garage", MapColor.COLOR_GRAY);
   public static final EchoBackendRegistryEntry<Block> CONVOY_RECOVERY_BEACON_CONTROLLER =
      controller("convoy_recovery_beacon_controller", "convoy_recovery_beacon", MapColor.COLOR_RED);

   public static final EchoBackendRegistryEntry<Block> CONVOY_ARMOR_PANEL = metal("convoy_armor_panel", MapColor.COLOR_GREEN, 4.0F);
   public static final EchoBackendRegistryEntry<Block> DEPOT_FLOOR_PLATE = metal("depot_floor_plate", MapColor.COLOR_GRAY, 3.0F);
   public static final EchoBackendRegistryEntry<Block> HEAVY_GARAGE_FRAME = metal("heavy_garage_frame", MapColor.METAL, 5.0F);
   public static final EchoBackendRegistryEntry<Block> ROUTE_MARKER_PANEL = metal("route_marker_panel", MapColor.COLOR_CYAN, 2.5F);
   public static final EchoBackendRegistryEntry<Block> FIELD_COMMAND_WALL = metal("field_command_wall", MapColor.COLOR_LIGHT_BLUE, 3.5F);
   public static final EchoBackendRegistryEntry<Block> REINFORCED_RAMP_BLOCK = metal("reinforced_ramp_block", MapColor.COLOR_GRAY, 3.5F);
   public static final EchoBackendRegistryEntry<Block> VEHICLE_CLAMP_BLOCK = metal("vehicle_clamp_block", MapColor.METAL, 4.0F);
   public static final EchoBackendRegistryEntry<Block> CARGO_LOCK_BLOCK = metal("cargo_lock_block", MapColor.COLOR_YELLOW, 3.0F);
   public static final EchoBackendRegistryEntry<Block> CONVOY_WARNING_LIGHT = metal("convoy_warning_light", MapColor.COLOR_ORANGE, 1.5F);
   public static final EchoBackendRegistryEntry<Block> DEPOT_POWER_BUS = metal("depot_power_bus", MapColor.COLOR_ORANGE, 3.0F);
   public static final EchoBackendRegistryEntry<Block> DEPOT_DATA_BUS = metal("depot_data_bus", MapColor.COLOR_LIGHT_BLUE, 3.0F);
   public static final EchoBackendRegistryEntry<Block> DEPOT_ITEM_BUS = metal("depot_item_bus", MapColor.COLOR_YELLOW, 3.0F);
   public static final EchoBackendRegistryEntry<Block> FUEL_PIPE_BLOCK = metal("fuel_pipe_block", MapColor.COLOR_ORANGE, 2.0F);
   public static final EchoBackendRegistryEntry<Block> FUEL_TANK_BLOCK = metal("fuel_tank_block", MapColor.COLOR_ORANGE, 3.5F);
   public static final EchoBackendRegistryEntry<Block> REPAIR_BAY_WALL = metal("repair_bay_wall", MapColor.METAL, 4.0F);
   public static final EchoBackendRegistryEntry<Block> MAINTENANCE_CATWALK = metal("maintenance_catwalk", MapColor.COLOR_GRAY, 2.5F);
   public static final EchoBackendRegistryEntry<Block> GARAGE_DOOR_FRAME = metal("garage_door_frame", MapColor.METAL, 4.0F);
   public static final EchoBackendRegistryEntry<Block> MOBILE_OPERATIONS_CONSOLE = metal("mobile_operations_console", MapColor.COLOR_CYAN, 2.5F);

   public static final EchoBackendRegistryEntry<Block> CARGO_INPUT_CRATE = crate("cargo_input_crate", CrateKind.INPUT, MapColor.WOOD);
   public static final EchoBackendRegistryEntry<Block> CARGO_OUTPUT_CRATE = crate("cargo_output_crate", CrateKind.OUTPUT, MapColor.WOOD);
   public static final EchoBackendRegistryEntry<Block> FUEL_INPUT_TANK = crate("fuel_input_tank", CrateKind.INPUT, MapColor.COLOR_ORANGE);
   public static final EchoBackendRegistryEntry<Block> FUEL_OUTPUT_TANK = crate("fuel_output_tank", CrateKind.OUTPUT, MapColor.COLOR_ORANGE);
   public static final EchoBackendRegistryEntry<Block> VEHICLE_SERVICE_PAD = metal("vehicle_service_pad", MapColor.COLOR_GRAY, 4.0F);
   public static final EchoBackendRegistryEntry<Block> REPAIR_ARM_MOUNT = metal("repair_arm_mount", MapColor.METAL, 3.5F);
   public static final EchoBackendRegistryEntry<Block> CARGO_LOADER_ARM_MOUNT = metal("cargo_loader_arm_mount", MapColor.COLOR_YELLOW, 3.5F);
   public static final EchoBackendRegistryEntry<Block> FUEL_INJECTOR_ARM_MOUNT = metal("fuel_injector_arm_mount", MapColor.COLOR_ORANGE, 3.5F);
   public static final EchoBackendRegistryEntry<Block> DISPATCH_TERMINAL_BLOCK = metal("dispatch_terminal_block", MapColor.COLOR_CYAN, 3.0F);
   public static final EchoBackendRegistryEntry<Block> ROUTE_UPLINK_ANTENNA = metal("route_uplink_antenna", MapColor.COLOR_LIGHT_BLUE, 2.5F);
   public static final EchoBackendRegistryEntry<Block> CONVOY_PARTS_STORAGE = metal("convoy_parts_storage", MapColor.WOOD, 2.5F);
   public static final EchoBackendRegistryEntry<Block> EMERGENCY_RECOVERY_STATION = metal("emergency_recovery_station", MapColor.COLOR_RED, 3.5F);

   public static final List<EchoBackendRegistryEntry<Block>> ALL_BLOCKS = List.of(
      VEHICLE_WORKBENCH,
      FUEL_STILL,
      BATTERY_CHARGING_PAD,
      VEHICLE_DOCK,
      VEHICLE_UPGRADE_BAY,
      CONVOY_BEACON,
      ROADSIDE_SIGNAL_MARKER,
      CARGO_ANCHOR,
      FIELD_REPAIR_STATION,
      CONVOY_DEPOT_CONTROLLER,
      VEHICLE_REPAIR_GANTRY_CONTROLLER,
      CARGO_LOADING_BAY_CONTROLLER,
      FUEL_REFINERY_PAD_CONTROLLER,
      ROUTE_DISPATCH_TOWER_CONTROLLER,
      MOBILE_COMMAND_GARAGE_CONTROLLER,
      CONVOY_RECOVERY_BEACON_CONTROLLER,
      CONVOY_ARMOR_PANEL,
      DEPOT_FLOOR_PLATE,
      HEAVY_GARAGE_FRAME,
      ROUTE_MARKER_PANEL,
      FIELD_COMMAND_WALL,
      REINFORCED_RAMP_BLOCK,
      VEHICLE_CLAMP_BLOCK,
      CARGO_LOCK_BLOCK,
      CONVOY_WARNING_LIGHT,
      DEPOT_POWER_BUS,
      DEPOT_DATA_BUS,
      DEPOT_ITEM_BUS,
      FUEL_PIPE_BLOCK,
      FUEL_TANK_BLOCK,
      REPAIR_BAY_WALL,
      MAINTENANCE_CATWALK,
      GARAGE_DOOR_FRAME,
      MOBILE_OPERATIONS_CONSOLE,
      CARGO_INPUT_CRATE,
      CARGO_OUTPUT_CRATE,
      FUEL_INPUT_TANK,
      FUEL_OUTPUT_TANK,
      VEHICLE_SERVICE_PAD,
      REPAIR_ARM_MOUNT,
      CARGO_LOADER_ARM_MOUNT,
      FUEL_INJECTOR_ARM_MOUNT,
      DISPATCH_TERMINAL_BLOCK,
      ROUTE_UPLINK_ANTENNA,
      CONVOY_PARTS_STORAGE,
      EMERGENCY_RECOVERY_STATION
   );

   private ModBlocks() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(BLOCKS, eventBus);
   }

   private static EchoBackendRegistryEntry<Block> convoy(ConvoyBlockKind kind, MapColor color, float strength) {
      return customBlock(
         kind.getSerializedName(),
         properties -> new ConvoyBlock(kind, properties),
         properties -> properties.mapColor(color).strength(strength, strength * 2.0F).sound(SoundType.METAL).noOcclusion()
      );
   }

   private static EchoBackendRegistryEntry<Block> controller(String name, String definitionPath, MapColor color) {
      return customBlock(
         name,
         properties -> new ConvoyMultiblockControllerBlock(id(definitionPath), properties),
         properties -> properties.mapColor(color).strength(4.5F, 9.0F).sound(SoundType.METAL).noOcclusion()
      );
   }

   private static EchoBackendRegistryEntry<Block> crate(String name, CrateKind kind, MapColor color) {
      return customBlock(
         name,
         properties -> new ConvoyMultiblockCrateBlock(kind, properties),
         properties -> properties.mapColor(color).strength(2.5F, 4.0F).sound(SoundType.WOOD)
      );
   }

   private static EchoBackendRegistryEntry<Block> metal(String name, MapColor color, float strength) {
      return customBlock(
         name,
         Block::new,
         properties -> properties.mapColor(color).strength(strength, strength * 2.0F).sound(SoundType.METAL).noOcclusion()
      );
   }

   private static <T extends Block> EchoBackendRegistryEntry<T> customBlock(String name,
         Function<BlockBehaviour.Properties, T> factory, UnaryOperator<BlockBehaviour.Properties> propertyMutator) {
      return EchoBackendRegistryBridge.registerWithId(BLOCKS, name, id -> factory.apply(propertyMutator.apply(properties(id))));
   }

   private static BlockBehaviour.Properties properties(Identifier id) {
      return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id));
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, path);
   }
}
