package com.knoxhack.echoconvoyprotocol.event;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class ModTooltipEvents {
   private static final Map<String, String> TOOLTIP_KEYS = Map.ofEntries(
      Map.entry("vehicle_workbench", "tooltip.echoconvoyprotocol.vehicle_workbench"),
      Map.entry("fuel_still", "tooltip.echoconvoyprotocol.fuel_still"),
      Map.entry("battery_charging_pad", "tooltip.echoconvoyprotocol.battery_charging_pad"),
      Map.entry("vehicle_dock", "tooltip.echoconvoyprotocol.vehicle_dock"),
      Map.entry("vehicle_upgrade_bay", "tooltip.echoconvoyprotocol.vehicle_upgrade_bay"),
      Map.entry("convoy_beacon", "tooltip.echoconvoyprotocol.convoy_beacon"),
      Map.entry("roadside_signal_marker", "tooltip.echoconvoyprotocol.roadside_signal_marker"),
      Map.entry("cargo_anchor", "tooltip.echoconvoyprotocol.cargo_anchor"),
      Map.entry("field_repair_station", "tooltip.echoconvoyprotocol.field_repair_station"),
      Map.entry("scrap_tire", "tooltip.echoconvoyprotocol.scrap_tire"),
      Map.entry("armored_tire", "tooltip.echoconvoyprotocol.armored_tire"),
      Map.entry("vehicle_frame", "tooltip.echoconvoyprotocol.vehicle_frame"),
      Map.entry("fuel_canister", "tooltip.echoconvoyprotocol.fuel_canister"),
      Map.entry("battery_cell", "tooltip.echoconvoyprotocol.battery_cell"),
      Map.entry("engine_core", "tooltip.echoconvoyprotocol.engine_core"),
      Map.entry("radiation_shielding_plate", "tooltip.echoconvoyprotocol.radiation_shielding_plate"),
      Map.entry("convoy_repair_kit", "tooltip.echoconvoyprotocol.convoy_repair_kit"),
      Map.entry("cargo_net", "tooltip.echoconvoyprotocol.cargo_net"),
      Map.entry("route_beacon", "tooltip.echoconvoyprotocol.route_beacon"),
      Map.entry("scrap_bike_kit", "tooltip.echoconvoyprotocol.scrap_bike_kit"),
      Map.entry("wasteland_rover_kit", "tooltip.echoconvoyprotocol.wasteland_rover_kit"),
      Map.entry("cargo_crawler_kit", "tooltip.echoconvoyprotocol.cargo_crawler_kit"),
      Map.entry("armored_relay_truck_kit", "tooltip.echoconvoyprotocol.armored_relay_truck_kit"),
      Map.entry("scrap_bike_tuned_chain_kit", "tooltip.echoconvoyprotocol.scrap_bike_tuned_chain_kit"),
      Map.entry("scrap_bike_saddlebag_frame_kit", "tooltip.echoconvoyprotocol.scrap_bike_saddlebag_frame_kit"),
      Map.entry("scrap_bike_crash_cage_kit", "tooltip.echoconvoyprotocol.scrap_bike_crash_cage_kit"),
      Map.entry("wasteland_rover_suspension_kit", "tooltip.echoconvoyprotocol.wasteland_rover_suspension_kit"),
      Map.entry("wasteland_rover_scanner_array_kit", "tooltip.echoconvoyprotocol.wasteland_rover_scanner_array_kit"),
      Map.entry("wasteland_rover_reinforced_panel_kit", "tooltip.echoconvoyprotocol.wasteland_rover_reinforced_panel_kit"),
      Map.entry("cargo_crawler_low_gear_drive_kit", "tooltip.echoconvoyprotocol.cargo_crawler_low_gear_drive_kit"),
      Map.entry("cargo_crawler_expanded_bay_kit", "tooltip.echoconvoyprotocol.cargo_crawler_expanded_bay_kit"),
      Map.entry("cargo_crawler_track_skirt_kit", "tooltip.echoconvoyprotocol.cargo_crawler_track_skirt_kit"),
      Map.entry("armored_relay_truck_torque_axle_kit", "tooltip.echoconvoyprotocol.armored_relay_truck_torque_axle_kit"),
      Map.entry("armored_relay_truck_relay_array_kit", "tooltip.echoconvoyprotocol.armored_relay_truck_relay_array_kit"),
      Map.entry("armored_relay_truck_reactive_armor_kit", "tooltip.echoconvoyprotocol.armored_relay_truck_reactive_armor_kit")
   );

   private ModTooltipEvents() {
   }

   public static void onItemTooltip(Object event) {
      ItemStack stack = EchoBackendClientBridge.tooltipItemStack(event);
      Identifier id = stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
      if (id == null || !EchoConvoyProtocol.MODID.equals(id.getNamespace())) {
         return;
      }
      String key = TOOLTIP_KEYS.get(id.getPath());
      if (key != null) {
         EchoBackendClientBridge.addTooltipLine(event, Component.translatable(key).withStyle(ChatFormatting.GRAY));
         EchoBackendClientBridge.addTooltipLine(event, Component.translatable(key + ".hint").withStyle(ChatFormatting.DARK_GRAY));
      }
   }
}
