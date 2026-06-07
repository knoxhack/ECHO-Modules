package com.knoxhack.echoconvoyprotocol.client;

import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class ConvoyVehicleModel extends EntityModel<ConvoyVehicleRenderState> {
   private static final int TEXTURE_WIDTH = 256;
   private static final int TEXTURE_HEIGHT = 256;
   private static final ModelLayerLocation[] LAYER_LOCATIONS = createLayerLocations();

   private final ModelPart root;
   private final ModelPart[] wheels;
   private final ModelPart[] scannerAssemblies;
   private final ModelPart[] antennae;
   private final ModelPart[] dockClamps;
   private final ModelPart[] shieldPlates;
   private final ModelPart[] cargoMarkers;
   private final ModelPart[] damagePlates;
   private final ModelPart[] poweredLights;
   private final ModelPart[] lowPowerLights;

   public ConvoyVehicleModel(ModelPart root) {
      super(root);
      this.root = root;
      this.wheels = children(root,
         "front_tire", "rear_tire",
         "left_front_wheel", "right_front_wheel", "left_rear_wheel", "right_rear_wheel",
         "left_road_wheel_0", "right_road_wheel_0", "left_road_wheel_1", "right_road_wheel_1",
         "left_road_wheel_2", "right_road_wheel_2", "left_road_wheel_3", "right_road_wheel_3",
         "left_road_wheel_4", "right_road_wheel_4",
         "left_idler_front", "right_idler_front", "left_idler_rear", "right_idler_rear",
         "left_wheel_0", "right_wheel_0", "left_wheel_1", "right_wheel_1", "left_wheel_2", "right_wheel_2"
      );
      this.scannerAssemblies = children(root, "scanner_pod", "relay_dish_back", "relay_scanner_bar", "route_scanner");
      this.antennae = children(root,
         "antenna_left", "antenna_right", "rear_antenna", "scanner_whip",
         "roof_antenna_primary", "roof_antenna_secondary", "crawler_rear_antenna", "relay_comms_mast"
      );
      this.dockClamps = children(root, "dock_clamp_left", "dock_clamp_right", "dock_outrigger_left", "dock_outrigger_right");
      this.shieldPlates = children(root,
         "shield_plate_left", "shield_plate_right",
         "relay_shield_front_left", "relay_shield_front_right", "relay_shield_rear_left", "relay_shield_rear_right"
      );
      this.cargoMarkers = children(root,
         "cargo_marker_left", "cargo_marker_right", "cargo_marker_rear",
         "cargo_load_front", "cargo_load_mid", "cargo_load_rear"
      );
      this.damagePlates = children(root,
         "damage_plate_front", "damage_plate_left", "damage_plate_right", "damage_plate_rear",
         "damage_plate_tank", "damage_plate_engine"
      );
      this.poweredLights = children(root,
         "status_light", "route_light", "relay_core_light", "headlamp_glow", "crawler_status_light",
         "scrap_front_vent_left", "scrap_front_vent_right", "scrap_rear_vent_left", "scrap_rear_vent_right",
         "side_cyan_panel_left", "side_cyan_panel_right", "cyan_dashboard_panel", "crawler_front_marker",
         "crawler_rear_marker", "relay_dashboard_panel", "relay_side_screen_left", "relay_side_screen_right"
      );
      this.lowPowerLights = children(root, "low_power_warning", "rear_warning_light");
   }

   public static ModelLayerLocation layerLocation(ConvoyVehicleKind kind) {
      return LAYER_LOCATIONS[kind.ordinal()];
   }

   public Map<String, ModelPart> namedPartsForRenderCore() {
      Map<String, ModelPart> parts = new LinkedHashMap<>();
      putAlias(parts, "body", root);
      putAlias(parts, "wheel_front_left", firstChild(root, "left_front_wheel", "front_tire", "left_road_wheel_0", "left_wheel_0"));
      putAlias(parts, "wheel_front_right", firstChild(root, "right_front_wheel", "front_tire", "right_road_wheel_0", "right_wheel_0"));
      putAlias(parts, "wheel_back_left", firstChild(root, "left_rear_wheel", "rear_tire", "left_road_wheel_4", "left_wheel_2"));
      putAlias(parts, "wheel_back_right", firstChild(root, "right_rear_wheel", "rear_tire", "right_road_wheel_4", "right_wheel_2"));
      putAlias(parts, "suspension_left", firstChild(root, "front_left_wheel_well", "left_track_outer_belt", "left_suspension_block_0"));
      putAlias(parts, "suspension_right", firstChild(root, "front_right_wheel_well", "right_track_outer_belt", "right_suspension_block_0"));
      putAlias(parts, "scanner", firstChild(root, "scanner_pod", "relay_scanner_bar", "relay_dish_back", "front_sensor_nose", "headlamp"));
      putAlias(parts, "antenna", firstChild(root, "scanner_whip", "roof_antenna_primary", "crawler_rear_antenna", "relay_comms_mast", "antenna_left"));
      putAlias(parts, "core", firstChild(root, "cyan_dashboard_panel", "status_light", "crawler_status_light", "relay_core_light", "headlamp_glow", "route_light"));
      putAlias(parts, "terminal_panel", firstChild(root, "cyan_dashboard_panel", "relay_dashboard_panel", "windshield", "windshield_slit", "front_windows"));
      putAlias(parts, "screen", firstChild(root, "cyan_dashboard_panel", "relay_dashboard_panel", "windshield", "windshield_slit", "front_windows"));
      putAlias(parts, "headlight_left", firstChild(root, "left_headlight", "headlamp", "front_light_bar", "scrap_front_vent_left"));
      putAlias(parts, "headlight_right", firstChild(root, "right_headlight", "headlamp", "front_light_bar", "scrap_front_vent_right"));
      putAlias(parts, "scanner_lens", firstChild(root, "scanner_pod", "relay_dish_back", "front_sensor_nose", "headlamp"));
      putAlias(parts, "exhaust_left", firstChild(root, "exhaust_pipe", "muffler", "left_toolbox", "left_fuel_tank", "scrap_rear_vent_left"));
      putAlias(parts, "exhaust_right", firstChild(root, "muffler", "right_toolbox", "right_fuel_tank", "scrap_rear_vent_right"));
      putAlias(parts, "cabin", firstChild(root, "cab_lower", "heavy_cab_lower", "armored_cab_lower", "fuel_tank", "seat"));
      putAlias(parts, "engine", firstChild(root, "engine_block", "crawler_chassis", "heavy_chassis", "teal_power_core"));
      putAlias(parts, "cargo", firstChild(root, "rear_cargo_roll", "left_saddlebag", "cargo_bed_floor", "crate_large", "tech_hull_lower"));
      return parts;
   }

   public static LayerDefinition createBodyLayer(ConvoyVehicleKind kind) {
      return switch (kind) {
         case SCRAP_BIKE -> createScrapBikeLayer();
         case WASTELAND_ROVER -> createWastelandRoverLayer();
         case CARGO_CRAWLER -> createCargoCrawlerLayer();
         case ARMORED_RELAY_TRUCK -> createArmoredRelayTruckLayer();
      };
   }

   @Override
   public void setupAnim(ConvoyVehicleRenderState state) {
      super.setupAnim(state);
      root.xScale = 1.0F;
      root.yScale = 1.0F;
      root.zScale = 1.0F;
      root.xRot = 0.0F;
      root.yRot = 0.0F;
      root.zRot = 0.0F;

      float movement = Mth.clamp(state.speed * 12.0F, 0.0F, 1.0F);
      float damaged = Mth.clamp((state.damageRatio - 0.55F) * 2.2F, 0.0F, 1.0F);
      root.y = Mth.abs(Mth.sin(state.ageInTicks * 0.55F)) * movement * 0.18F
         + Mth.sin(state.ageInTicks * 0.31F) * state.damageRatio * 0.16F;
      root.xRot = Mth.sin(state.ageInTicks * 0.19F) * movement * 0.006F;
      root.zRot = Mth.sin(state.ageInTicks * 0.73F) * damaged * 0.028F;

      float spin = state.driven ? state.ageInTicks * Mth.clamp(state.speed * 30.0F, 0.25F, 3.2F) : 0.0F;
      for (ModelPart wheel : wheels) {
         wheel.xRot = spin;
      }

      float powerRatio = Math.max(state.fuelRatio, state.batteryRatio);
      boolean scannerActive = state.hasTravelPower && powerRatio > 0.04F;
      for (ModelPart scanner : scannerAssemblies) {
         scanner.yRot = scannerActive ? state.ageInTicks * 0.045F : 0.0F;
         scanner.xRot = scannerActive ? Mth.sin(state.ageInTicks * 0.065F) * 0.04F : 0.0F;
      }
      for (int i = 0; i < antennae.length; i++) {
         ModelPart antenna = antennae[i];
         antenna.xRot = Mth.sin(state.ageInTicks * 0.11F + i * 0.7F) * (scannerActive ? 0.05F : 0.018F);
         antenna.zRot = Mth.sin(state.ageInTicks * 0.07F + i) * 0.025F;
      }

      setVisible(dockClamps, state.docked);
      setVisibleCount(shieldPlates, state.shieldingRatio);
      setVisibleCount(cargoMarkers, state.cargoRatio);
      setVisibleCount(damagePlates, state.damageRatio);
      setVisible(poweredLights, state.hasTravelPower);
      setVisible(lowPowerLights, state.hasTravelPower && powerRatio < 0.23F && ((int)(state.ageInTicks / 6.0F) & 1) == 0);
   }

   private static ModelLayerLocation[] createLayerLocations() {
      ConvoyVehicleKind[] kinds = ConvoyVehicleKind.values();
      ModelLayerLocation[] locations = new ModelLayerLocation[kinds.length];
      for (int i = 0; i < kinds.length; i++) {
         locations[i] = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, "convoy_vehicle_" + kinds[i].getSerializedName()),
            "main"
         );
      }
      return locations;
   }

   private static LayerDefinition createScrapBikeLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      part(root, "lower_frame", 48, 0, -1.4F, -10.0F, -11.5F, 2.8F, 1.5F, 23.0F);
      part(root, "frame_backbone", 48, 24, -0.75F, -15.6F, -9.4F, 1.5F, 1.6F, 17.4F);
      part(root, "frame_mid_rail_left", 56, 24, -3.3F, -13.5F, -8.4F, 0.9F, 1.0F, 15.0F);
      part(root, "frame_mid_rail_right", 56, 24, 2.4F, -13.5F, -8.4F, 0.9F, 1.0F, 15.0F);
      part(root, "left_engine_cradle", 64, 0, -3.2F, -11.4F, -4.2F, 1.0F, 5.2F, 9.5F);
      part(root, "right_engine_cradle", 64, 0, 2.2F, -11.4F, -4.2F, 1.0F, 5.2F, 9.5F);
      part(root, "engine_block", 80, 0, -2.6F, -13.3F, -4.0F, 5.2F, 5.8F, 8.2F);
      part(root, "engine_grille", 112, 0, -3.2F, -12.4F, -1.9F, 0.9F, 4.3F, 4.4F);
      part(root, "engine_belt", 128, 0, 2.3F, -12.4F, -1.9F, 0.9F, 4.3F, 4.4F);
      part(root, "fuel_tank", 0, 32, -2.6F, -18.0F, -9.6F, 5.2F, 4.4F, 9.8F);
      part(root, "tank_top_cap", 32, 32, -0.9F, -19.5F, -6.8F, 1.8F, 1.3F, 3.0F);
      part(root, "patched_tank_plate", 40, 32, -3.1F, -17.4F, -5.5F, 0.9F, 3.0F, 5.0F, 0.08F);
      part(root, "seat", 72, 32, -2.4F, -17.2F, 0.2F, 4.8F, 2.1F, 7.8F);
      part(root, "seat_back_block", 72, 40, -2.3F, -18.9F, 7.1F, 4.6F, 3.2F, 2.2F);
      part(root, "front_sensor_nose", 104, 32, -2.8F, -14.3F, -18.8F, 5.6F, 3.0F, 5.2F);
      part(root, "rear_power_block", 104, 40, -3.0F, -14.6F, 8.8F, 6.0F, 3.8F, 6.8F);
      wheel(root, "front_tire", 0, 0, -1.45F, -12.4F, -20.6F, 2.9F, 9.2F, 8.8F);
      wheel(root, "rear_tire", 0, 0, -1.45F, -12.2F, 7.0F, 2.9F, 9.0F, 9.2F);
      part(root, "front_fender", 24, 56, -3.2F, -16.3F, -20.0F, 6.4F, 1.4F, 7.6F);
      part(root, "rear_fender", 24, 56, -3.3F, -16.1F, 7.6F, 6.6F, 1.4F, 8.0F);
      part(root, "left_outrigger", 120, 40, -4.3F, -12.4F, -8.5F, 1.3F, 1.8F, 15.8F);
      part(root, "right_outrigger", 120, 40, 3.0F, -12.4F, -8.5F, 1.3F, 1.8F, 15.8F);
      part(root, "front_thruster_left", 0, 56, -4.9F, -9.9F, -17.0F, 1.8F, 3.5F, 6.6F);
      part(root, "front_thruster_right", 0, 56, 3.1F, -9.9F, -17.0F, 1.8F, 3.5F, 6.6F);
      part(root, "rear_thruster_left", 0, 56, -5.0F, -9.8F, 5.9F, 1.8F, 3.5F, 7.0F);
      part(root, "rear_thruster_right", 0, 56, 3.2F, -9.8F, 5.9F, 1.8F, 3.5F, 7.0F);
      part(root, "scrap_front_vent_left", 176, 120, -6.2F, -8.6F, -18.0F, 2.8F, 2.0F, 0.9F, 0.08F);
      part(root, "scrap_front_vent_right", 176, 120, 3.4F, -8.6F, -18.0F, 2.8F, 2.0F, 0.9F, 0.08F);
      part(root, "scrap_rear_vent_left", 176, 120, -6.2F, -8.5F, 13.5F, 2.8F, 2.0F, 0.9F, 0.08F);
      part(root, "scrap_rear_vent_right", 176, 120, 3.4F, -8.5F, 13.5F, 2.8F, 2.0F, 0.9F, 0.08F);
      part(root, "left_fork_upper", 16, 56, -3.4F, -19.0F, -14.4F, 0.8F, 8.8F, 1.1F);
      part(root, "right_fork_upper", 16, 56, 2.6F, -19.0F, -14.4F, 0.8F, 8.8F, 1.1F);
      part(root, "handlebar_stem", 16, 56, -0.55F, -21.8F, -13.1F, 1.1F, 5.0F, 1.2F);
      part(root, "handlebar_crossbar", 32, 56, -5.7F, -22.3F, -13.5F, 11.4F, 0.8F, 1.2F);
      part(root, "left_grip", 48, 56, -7.2F, -22.4F, -13.6F, 2.0F, 1.0F, 1.4F);
      part(root, "right_grip", 48, 56, 5.2F, -22.4F, -13.6F, 2.0F, 1.0F, 1.4F);
      part(root, "headlamp", 64, 56, -1.8F, -17.8F, -18.9F, 3.6F, 3.0F, 1.4F);
      part(root, "headlamp_glow", 176, 120, -1.45F, -16.5F, -18.85F, 2.9F, 1.9F, 0.8F, 0.08F);
      part(root, "left_saddlebag", 88, 56, -5.8F, -14.0F, 4.6F, 2.4F, 4.5F, 6.8F);
      part(root, "right_saddlebag", 88, 56, 3.4F, -14.0F, 4.6F, 2.4F, 4.5F, 6.8F);
      part(root, "exhaust_pipe", 120, 56, -4.3F, -8.4F, -3.8F, 1.1F, 1.1F, 16.0F);
      part(root, "muffler", 152, 56, -5.2F, -9.6F, 8.6F, 2.8F, 2.8F, 6.8F);
      part(root, "rear_cargo_roll", 184, 56, -2.2F, -19.0F, 9.2F, 4.4F, 2.5F, 5.5F);
      part(root, "cargo_marker_rear", 192, 120, -2.0F, -20.1F, 9.6F, 4.0F, 1.0F, 3.8F);
      part(root, "dock_clamp_left", 208, 120, -7.0F, -8.8F, -0.5F, 2.0F, 4.4F, 9.5F);
      part(root, "dock_clamp_right", 208, 120, 5.0F, -8.8F, -0.5F, 2.0F, 4.4F, 9.5F);
      part(root, "damage_plate_tank", 224, 120, -3.6F, -17.8F, -8.5F, 1.0F, 3.2F, 6.0F, 0.08F);
      part(root, "damage_plate_engine", 224, 128, 2.8F, -13.0F, -1.0F, 1.0F, 4.2F, 4.0F, 0.08F);
      part(root, "status_light", 240, 120, -1.1F, -15.6F, -9.4F, 2.2F, 1.0F, 1.0F);
      part(root, "low_power_warning", 240, 128, -1.0F, -14.1F, -9.4F, 2.0F, 1.0F, 1.0F);
      return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
   }

   private static LayerDefinition createWastelandRoverLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      part(root, "lower_chassis", 0, 0, -13.0F, -7.8F, -19.0F, 26.0F, 4.2F, 38.0F);
      part(root, "center_frame_rail", 48, 0, -8.8F, -10.1F, -17.0F, 17.6F, 2.6F, 34.0F);
      part(root, "front_skid_plate", 96, 0, -10.2F, -8.5F, -22.8F, 20.4F, 1.8F, 4.8F);
      part(root, "rear_skid_plate", 96, 0, -9.2F, -8.5F, 18.2F, 18.4F, 1.8F, 4.8F);
      part(root, "armored_hood_lower", 0, 52, -10.6F, -13.6F, -18.8F, 21.2F, 4.3F, 13.8F);
      part(root, "armored_hood_center", 48, 52, -7.4F, -16.0F, -17.2F, 14.8F, 2.4F, 10.8F);
      part(root, "hood_side_louver_left", 96, 52, -12.0F, -14.4F, -15.5F, 1.2F, 4.0F, 7.0F);
      part(root, "hood_side_louver_right", 96, 52, 10.8F, -14.4F, -15.5F, 1.2F, 4.0F, 7.0F);
      part(root, "cab_lower", 72, 52, -9.8F, -21.0F, -6.0F, 19.6F, 11.4F, 11.5F);
      part(root, "cab_roof", 120, 52, -8.9F, -23.7F, -6.6F, 17.8F, 2.8F, 13.4F);
      part(root, "cab_front_pillar_left", 184, 52, -10.9F, -22.2F, -7.4F, 2.2F, 12.4F, 2.0F);
      part(root, "cab_front_pillar_right", 184, 52, 8.7F, -22.2F, -7.4F, 2.2F, 12.4F, 2.0F);
      part(root, "cab_rear_pillar_left", 184, 52, -10.6F, -22.0F, 4.4F, 2.0F, 10.8F, 2.0F);
      part(root, "cab_rear_pillar_right", 184, 52, 8.6F, -22.0F, 4.4F, 2.0F, 10.8F, 2.0F);
      part(root, "cab_b_pillar", 184, 52, -1.0F, -21.3F, -6.9F, 2.0F, 8.6F, 1.2F);
      part(root, "rear_utility_box", 136, 52, -9.2F, -17.5F, 7.2F, 18.4F, 8.4F, 12.0F);
      part(root, "rear_side_panel_left", 176, 52, -10.5F, -17.9F, 8.2F, 1.7F, 7.3F, 10.3F);
      part(root, "rear_side_panel_right", 176, 52, 8.8F, -17.9F, 8.2F, 1.7F, 7.3F, 10.3F);
      part(root, "windshield", 0, 80, -8.0F, -20.1F, -7.1F, 16.0F, 5.4F, 1.0F);
      part(root, "cyan_dashboard_panel", 176, 120, -3.6F, -14.2F, -7.7F, 7.2F, 1.6F, 0.8F, 0.08F);
      part(root, "side_window_left", 48, 80, -11.2F, -19.2F, -2.8F, 1.0F, 5.2F, 6.2F);
      part(root, "side_window_right", 48, 80, 10.2F, -19.2F, -2.8F, 1.0F, 5.2F, 6.2F);
      part(root, "front_bumper", 72, 80, -13.4F, -8.5F, -23.0F, 26.8F, 2.8F, 3.8F);
      part(root, "rear_bumper", 72, 80, -12.0F, -8.5F, 20.0F, 24.0F, 2.8F, 3.8F);
      part(root, "bullbar_top", 128, 80, -11.4F, -13.6F, -24.0F, 22.8F, 1.5F, 1.5F);
      part(root, "bullbar_left", 160, 80, -12.4F, -12.8F, -23.4F, 1.5F, 6.4F, 1.5F);
      part(root, "bullbar_right", 160, 80, 10.9F, -12.8F, -23.4F, 1.5F, 6.4F, 1.5F);
      part(root, "left_headlight", 176, 80, -10.5F, -13.2F, -23.8F, 4.0F, 3.0F, 1.0F);
      part(root, "right_headlight", 176, 80, 6.5F, -13.2F, -23.8F, 4.0F, 3.0F, 1.0F);
      part(root, "roof_rack_floor", 0, 100, -9.4F, -24.9F, -7.4F, 18.8F, 1.2F, 15.8F);
      part(root, "roof_rack_front_rail", 48, 100, -9.8F, -26.3F, -8.2F, 19.6F, 1.5F, 1.3F);
      part(root, "roof_rack_rear_rail", 48, 100, -9.8F, -26.3F, 8.3F, 19.6F, 1.5F, 1.3F);
      part(root, "roof_rack_left_rail", 64, 100, -10.6F, -26.3F, -7.4F, 1.4F, 2.2F, 15.8F);
      part(root, "roof_rack_right_rail", 64, 100, 9.2F, -26.3F, -7.4F, 1.4F, 2.2F, 15.8F);
      part(root, "roof_cargo_plate", 88, 100, -6.8F, -27.4F, -5.6F, 13.6F, 1.0F, 10.8F);
      part(root, "roof_antenna_box", 104, 100, 5.8F, -29.0F, 4.8F, 4.6F, 2.4F, 4.2F);
      PartDefinition scanner = pivot(root, "scanner_pod", 0.0F, -27.5F, -0.6F);
      cube(scanner, "scanner_body", 80, 100, -2.8F, -1.9F, -2.2F, 5.6F, 3.8F, 4.4F);
      cube(scanner, "scanner_lens", 104, 100, -2.0F, -1.0F, -2.9F, 4.0F, 2.0F, 0.9F);
      cube(scanner, "scanner_side_cap_left", 112, 100, -3.5F, -1.3F, -1.4F, 0.8F, 2.8F, 2.8F);
      cube(scanner, "scanner_side_cap_right", 112, 100, 2.7F, -1.3F, -1.4F, 0.8F, 2.8F, 2.8F);
      part(root, "scanner_support_left", 112, 100, -4.6F, -26.5F, -1.2F, 1.1F, 3.4F, 2.4F);
      part(root, "scanner_support_right", 112, 100, 3.5F, -26.5F, -1.2F, 1.1F, 3.4F, 2.4F);
      PartDefinition whip = pivot(root, "scanner_whip", 8.2F, -27.5F, 7.2F);
      cube(whip, "scanner_whip_base", 120, 100, -0.6F, -3.2F, -0.6F, 1.2F, 3.2F, 1.2F);
      cube(whip, "scanner_whip_tip", 128, 100, -0.3F, -12.0F, -0.3F, 0.6F, 8.8F, 0.6F);
      antenna(root, "roof_antenna_primary", 136, 100, -7.5F, -30.8F, 5.4F, 12.5F);
      antenna(root, "roof_antenna_secondary", 136, 100, 7.2F, -30.2F, -5.8F, 8.4F);
      wheel(root, "spare_tire", 120, 100, -5.0F, -17.0F, 20.8F, 10.0F, 10.0F, 3.2F);
      wheel(root, "left_front_wheel", 0, 124, -18.3F, -12.2F, -17.2F, 5.6F, 13.0F, 11.8F);
      wheel(root, "right_front_wheel", 0, 124, 12.7F, -12.2F, -17.2F, 5.6F, 13.0F, 11.8F);
      wheel(root, "left_rear_wheel", 0, 124, -18.3F, -12.2F, 8.0F, 5.6F, 13.0F, 11.8F);
      wheel(root, "right_rear_wheel", 0, 124, 12.7F, -12.2F, 8.0F, 5.6F, 13.0F, 11.8F);
      part(root, "front_left_wheel_well", 48, 124, -18.5F, -15.2F, -18.2F, 6.2F, 3.0F, 14.0F);
      part(root, "front_right_wheel_well", 48, 124, 12.3F, -15.2F, -18.2F, 6.2F, 3.0F, 14.0F);
      part(root, "rear_left_wheel_well", 48, 124, -18.5F, -15.2F, 7.0F, 6.2F, 3.0F, 14.0F);
      part(root, "rear_right_wheel_well", 48, 124, 12.3F, -15.2F, 7.0F, 6.2F, 3.0F, 14.0F);
      part(root, "front_left_suspension", 72, 124, -15.2F, -13.2F, -14.5F, 2.0F, 4.0F, 6.0F);
      part(root, "front_right_suspension", 72, 124, 13.2F, -13.2F, -14.5F, 2.0F, 4.0F, 6.0F);
      part(root, "rear_left_suspension", 72, 124, -15.2F, -13.2F, 10.5F, 2.0F, 4.0F, 6.0F);
      part(root, "rear_right_suspension", 72, 124, 13.2F, -13.2F, 10.5F, 2.0F, 4.0F, 6.0F);
      part(root, "front_left_fender_thick", 48, 124, -19.6F, -16.3F, -18.5F, 7.2F, 4.0F, 14.5F);
      part(root, "front_right_fender_thick", 48, 124, 12.4F, -16.3F, -18.5F, 7.2F, 4.0F, 14.5F);
      part(root, "rear_left_fender_thick", 48, 124, -19.6F, -16.2F, 7.0F, 7.2F, 3.8F, 14.5F);
      part(root, "rear_right_fender_thick", 48, 124, 12.4F, -16.2F, 7.0F, 7.2F, 3.8F, 14.5F);
      part(root, "front_left_fender_lip", 112, 124, -20.4F, -13.4F, -18.0F, 2.0F, 2.0F, 13.2F);
      part(root, "front_right_fender_lip", 112, 124, 18.4F, -13.4F, -18.0F, 2.0F, 2.0F, 13.2F);
      part(root, "rear_left_fender_lip", 112, 124, -20.4F, -13.4F, 7.4F, 2.0F, 2.0F, 13.2F);
      part(root, "rear_right_fender_lip", 112, 124, 18.4F, -13.4F, 7.4F, 2.0F, 2.0F, 13.2F);
      part(root, "left_toolbox", 88, 124, -16.0F, -12.0F, -2.0F, 4.0F, 5.0F, 8.0F);
      part(root, "right_toolbox", 88, 124, 12.0F, -12.0F, -2.0F, 4.0F, 5.0F, 8.0F);
      part(root, "side_cyan_panel_left", 176, 120, -16.3F, -16.4F, 3.5F, 1.0F, 3.0F, 4.8F, 0.08F);
      part(root, "side_cyan_panel_right", 176, 120, 15.3F, -16.4F, 3.5F, 1.0F, 3.0F, 4.8F, 0.08F);
      part(root, "hood_plate_left", 112, 124, -12.2F, -15.8F, -16.5F, 1.8F, 5.0F, 8.5F, 0.1F);
      part(root, "hood_plate_right", 112, 124, 10.4F, -15.8F, -16.5F, 1.8F, 5.0F, 8.5F, 0.1F);
      part(root, "jerry_can_left", 136, 124, -15.5F, -17.5F, 9.0F, 4.0F, 6.0F, 4.0F);
      part(root, "jerry_can_right", 136, 124, 11.5F, -17.5F, 9.0F, 4.0F, 6.0F, 4.0F);
      part(root, "shield_plate_left", 176, 136, -15.2F, -20.5F, -1.0F, 2.0F, 9.0F, 10.0F, 0.1F);
      part(root, "shield_plate_right", 176, 136, 13.2F, -20.5F, -1.0F, 2.0F, 9.0F, 10.0F, 0.1F);
      part(root, "cargo_marker_left", 200, 136, -15.0F, -18.4F, 10.0F, 5.0F, 2.0F, 8.0F);
      part(root, "cargo_marker_right", 200, 136, 10.0F, -18.4F, 10.0F, 5.0F, 2.0F, 8.0F);
      part(root, "dock_clamp_left", 208, 120, -17.4F, -8.0F, -5.0F, 3.0F, 4.0F, 14.0F);
      part(root, "dock_clamp_right", 208, 120, 14.4F, -8.0F, -5.0F, 3.0F, 4.0F, 14.0F);
      part(root, "damage_plate_front", 184, 120, -10.0F, -14.8F, -20.3F, 20.0F, 2.0F, 1.0F, 0.08F);
      part(root, "damage_plate_left", 224, 128, -14.5F, -16.5F, 4.0F, 1.0F, 8.0F, 10.0F, 0.08F);
      part(root, "status_light", 240, 120, -2.0F, -23.8F, -8.4F, 4.0F, 1.0F, 1.0F);
      part(root, "low_power_warning", 240, 128, 9.0F, -16.5F, -19.5F, 2.0F, 1.0F, 1.0F);
      return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
   }

   private static LayerDefinition createCargoCrawlerLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      part(root, "left_track_outer_belt", 0, 0, -18.4F, -6.9F, -25.0F, 7.6F, 5.5F, 50.0F);
      part(root, "right_track_outer_belt", 0, 0, 10.8F, -6.9F, -25.0F, 7.6F, 5.5F, 50.0F);
      part(root, "left_track_inner_shadow", 40, 0, -15.9F, -5.9F, -22.5F, 2.6F, 2.9F, 45.0F);
      part(root, "right_track_inner_shadow", 40, 0, 13.3F, -5.9F, -22.5F, 2.6F, 2.9F, 45.0F);
      part(root, "left_track_top_rail", 72, 0, -18.6F, -8.4F, -23.8F, 8.0F, 1.4F, 47.6F);
      part(root, "right_track_top_rail", 72, 0, 10.6F, -8.4F, -23.8F, 8.0F, 1.4F, 47.6F);
      part(root, "left_track_bottom_rail", 72, 0, -18.6F, -1.7F, -23.8F, 8.0F, 1.5F, 47.6F);
      part(root, "right_track_bottom_rail", 72, 0, 10.6F, -1.7F, -23.8F, 8.0F, 1.5F, 47.6F);
      for (int i = 0; i < 9; i++) {
         float z = -23.5F + i * 5.9F;
         part(root, "left_track_pad_" + i, 120, 0, -18.9F, -8.3F, z, 8.4F, 1.4F, 3.0F);
         part(root, "right_track_pad_" + i, 120, 0, 10.5F, -8.3F, z, 8.4F, 1.4F, 3.0F);
         part(root, "left_track_ground_pad_" + i, 120, 8, -18.7F, -1.9F, z, 8.0F, 1.7F, 3.0F);
         part(root, "right_track_ground_pad_" + i, 120, 8, 10.7F, -1.9F, z, 8.0F, 1.7F, 3.0F);
      }
      wheel(root, "left_idler_front", 0, 62, -18.2F, -6.1F, -24.6F, 3.4F, 6.0F, 6.0F);
      wheel(root, "right_idler_front", 0, 62, 14.8F, -6.1F, -24.6F, 3.4F, 6.0F, 6.0F);
      wheel(root, "left_idler_rear", 0, 62, -18.2F, -6.1F, 18.6F, 3.4F, 6.0F, 6.0F);
      wheel(root, "right_idler_rear", 0, 62, 14.8F, -6.1F, 18.6F, 3.4F, 6.0F, 6.0F);
      for (int i = 0; i < 5; i++) {
         float z = -18.0F + i * 9.0F;
         wheel(root, "left_road_wheel_" + i, 0, 62, -18.3F, -6.2F, z, 3.4F, 6.2F, 6.2F);
         wheel(root, "right_road_wheel_" + i, 0, 62, 14.9F, -6.2F, z, 3.4F, 6.2F, 6.2F);
      }
      part(root, "crawler_chassis", 32, 62, -14.0F, -12.3F, -23.0F, 28.0F, 5.2F, 46.0F);
      part(root, "crawler_side_sill_left", 88, 62, -15.6F, -13.0F, -20.0F, 2.6F, 2.4F, 40.0F);
      part(root, "crawler_side_sill_right", 88, 62, 13.0F, -13.0F, -20.0F, 2.6F, 2.4F, 40.0F);
      part(root, "heavy_cab_lower", 0, 116, -12.4F, -21.8F, -25.0F, 24.8F, 9.7F, 14.5F);
      part(root, "heavy_cab_roof", 56, 116, -10.3F, -24.6F, -23.8F, 20.6F, 2.8F, 12.6F);
      part(root, "heavy_cab_front_frame_left", 88, 116, -14.0F, -23.2F, -26.4F, 3.0F, 12.0F, 2.4F);
      part(root, "heavy_cab_front_frame_right", 88, 116, 11.0F, -23.2F, -26.4F, 3.0F, 12.0F, 2.4F);
      part(root, "heavy_cab_side_layer_left", 96, 116, -15.0F, -21.4F, -22.0F, 2.4F, 8.6F, 10.5F);
      part(root, "heavy_cab_side_layer_right", 96, 116, 12.6F, -21.4F, -22.0F, 2.4F, 8.6F, 10.5F);
      part(root, "hood_nose", 72, 116, -11.0F, -16.9F, -30.6F, 22.0F, 6.0F, 6.6F);
      part(root, "cab_grille", 112, 116, -7.5F, -18.0F, -32.0F, 15.0F, 6.5F, 2.0F);
      part(root, "front_windows", 144, 116, -10.0F, -22.5F, -26.0F, 20.0F, 5.5F, 1.0F);
      part(root, "crawler_front_marker", 176, 120, -2.2F, -18.4F, -32.3F, 4.4F, 2.0F, 0.8F, 0.08F);
      part(root, "cab_side_window_left", 176, 116, -13.5F, -22.0F, -21.0F, 1.0F, 5.0F, 7.0F);
      part(root, "cab_side_window_right", 176, 116, 12.5F, -22.0F, -21.0F, 1.0F, 5.0F, 7.0F);
      part(root, "cargo_bed_floor", 0, 146, -13.5F, -14.5F, -8.0F, 27.0F, 2.2F, 34.0F);
      part(root, "left_bed_wall_lower", 72, 146, -15.3F, -18.8F, -8.0F, 2.2F, 5.2F, 35.0F);
      part(root, "right_bed_wall_lower", 72, 146, 13.1F, -18.8F, -8.0F, 2.2F, 5.2F, 35.0F);
      part(root, "tailgate", 112, 146, -13.5F, -18.8F, 25.0F, 27.0F, 5.2F, 2.4F);
      for (int i = 0; i < 6; i++) {
         float z = -6.5F + i * 6.0F;
         part(root, "cargo_slat_left_" + i, 152, 146, -16.0F, -20.0F, z, 3.0F, 6.4F, 1.4F);
         part(root, "cargo_slat_right_" + i, 152, 146, 13.0F, -20.0F, z, 3.0F, 6.4F, 1.4F);
      }
      part(root, "cargo_cage_top_left", 152, 146, -15.8F, -22.2F, -7.5F, 3.0F, 1.6F, 34.0F);
      part(root, "cargo_cage_top_right", 152, 146, 12.8F, -22.2F, -7.5F, 3.0F, 1.6F, 34.0F);
      part(root, "cargo_cage_front", 144, 184, -14.5F, -21.9F, -8.2F, 29.0F, 2.0F, 1.4F);
      part(root, "cargo_cage_rear", 144, 184, -14.5F, -21.9F, 25.3F, 29.0F, 2.0F, 1.4F);
      part(root, "crate_large", 0, 184, -6.8F, -20.7F, -3.5F, 10.4F, 5.8F, 8.5F);
      part(root, "crate_small_left", 56, 184, -12.6F, -19.4F, 7.5F, 6.4F, 4.7F, 6.5F);
      part(root, "crate_small_right", 56, 184, 6.2F, -19.4F, 8.8F, 6.4F, 4.7F, 6.5F);
      part(root, "crate_high_front", 56, 184, -4.0F, -24.8F, 2.5F, 8.0F, 3.8F, 7.0F);
      part(root, "tarp_high_rear", 96, 184, -10.5F, -23.8F, 12.5F, 21.0F, 2.8F, 8.5F);
      part(root, "rolled_tarp", 96, 184, -8.6F, -21.0F, 15.0F, 17.2F, 2.4F, 6.2F);
      part(root, "tie_down_front", 144, 184, -14.0F, -20.7F, -7.0F, 28.0F, 1.0F, 1.3F);
      part(root, "tie_down_rear", 144, 184, -14.0F, -20.7F, 22.0F, 28.0F, 1.0F, 1.3F);
      wheel(root, "roof_spare", 0, 212, -4.5F, -27.0F, -17.0F, 9.0F, 3.6F, 9.0F);
      antenna(root, "crawler_rear_antenna", 136, 138, 9.0F, -25.6F, 22.0F, 10.0F);
      part(root, "left_fuel_tank", 32, 212, -18.6F, -14.8F, -1.0F, 3.6F, 6.6F, 12.0F);
      part(root, "right_fuel_tank", 32, 212, 15.0F, -14.8F, -1.0F, 3.6F, 6.6F, 12.0F);
      part(root, "rear_utility_block", 184, 120, -5.0F, -16.2F, 27.0F, 10.0F, 6.2F, 5.0F);
      part(root, "crawler_rear_marker", 176, 120, -2.2F, -13.8F, 31.6F, 4.4F, 1.8F, 0.8F, 0.08F);
      part(root, "cargo_load_front", 160, 184, -10.5F, -21.8F, -5.8F, 9.0F, 1.8F, 6.0F);
      part(root, "cargo_load_mid", 160, 196, -1.0F, -22.2F, 4.8F, 10.8F, 2.0F, 7.0F);
      part(root, "cargo_load_rear", 160, 208, -11.5F, -21.6F, 16.0F, 10.0F, 1.8F, 6.0F);
      part(root, "dock_outrigger_left", 208, 120, -20.0F, -8.6F, -3.0F, 2.8F, 4.8F, 14.0F);
      part(root, "dock_outrigger_right", 208, 120, 17.2F, -8.6F, -3.0F, 2.8F, 4.8F, 14.0F);
      part(root, "damage_plate_front", 184, 120, -12.0F, -18.5F, -31.5F, 24.0F, 3.0F, 1.0F, 0.08F);
      part(root, "damage_plate_right", 184, 128, 14.2F, -19.4F, 0.0F, 1.0F, 8.4F, 18.0F, 0.08F);
      part(root, "crawler_status_light", 240, 120, -2.0F, -20.0F, -32.5F, 4.0F, 2.0F, 1.0F);
      part(root, "low_power_warning", 240, 128, 8.0F, -19.6F, -32.5F, 2.0F, 1.0F, 1.0F);
      return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
   }

   private static LayerDefinition createArmoredRelayTruckLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      part(root, "heavy_chassis", 0, 0, -17.2F, -8.1F, -25.0F, 34.4F, 5.0F, 50.0F);
      part(root, "lower_keel", 48, 0, -11.8F, -10.1F, -22.0F, 23.6F, 2.7F, 44.0F);
      for (int i = 0; i < 3; i++) {
         float z = -18.0F + i * 18.0F;
         wheel(root, "left_wheel_" + i, 96, 0, -22.0F, -12.2F, z - 0.3F, 6.0F, 13.2F, 12.0F);
         wheel(root, "right_wheel_" + i, 96, 0, 16.0F, -12.2F, z - 0.3F, 6.0F, 13.2F, 12.0F);
         part(root, "left_wheel_armor_" + i, 132, 0, -22.8F, -15.0F, z - 1.4F, 7.6F, 3.8F, 13.6F);
         part(root, "right_wheel_armor_" + i, 132, 0, 15.2F, -15.0F, z - 1.4F, 7.6F, 3.8F, 13.6F);
         part(root, "left_suspension_block_" + i, 148, 0, -17.2F, -13.0F, z + 1.0F, 3.0F, 3.4F, 7.0F);
         part(root, "right_suspension_block_" + i, 148, 0, 14.2F, -13.0F, z + 1.0F, 3.0F, 3.4F, 7.0F);
      }
      part(root, "armored_cab_lower", 0, 62, -15.3F, -23.2F, -25.0F, 30.6F, 14.2F, 16.5F);
      part(root, "armored_cab_depth_left", 112, 62, -18.2F, -22.4F, -24.4F, 4.0F, 12.4F, 15.0F);
      part(root, "armored_cab_depth_right", 112, 62, 14.2F, -22.4F, -24.4F, 4.0F, 12.4F, 15.0F);
      part(root, "cab_roof_brow", 80, 62, -16.0F, -27.1F, -23.8F, 32.0F, 4.0F, 9.4F, 0.1F);
      part(root, "cab_cheek_left", 112, 62, -17.0F, -20.5F, -23.6F, 4.4F, 8.8F, 8.4F);
      part(root, "cab_cheek_right", 112, 62, 12.6F, -20.5F, -23.6F, 4.4F, 8.8F, 8.4F);
      part(root, "cab_center_armor", 144, 62, -6.0F, -17.4F, -28.8F, 12.0F, 3.0F, 1.6F);
      part(root, "windshield_slit", 128, 62, -9.5F, -20.6F, -25.8F, 19.0F, 3.0F, 1.0F);
      part(root, "front_crash_plate", 160, 62, -16.6F, -14.8F, -29.0F, 33.2F, 6.6F, 3.8F);
      part(root, "front_bumper_lower", 208, 62, -13.0F, -8.6F, -31.0F, 26.0F, 3.0F, 2.8F);
      part(root, "front_bumper_left_layer", 208, 62, -18.2F, -11.4F, -31.8F, 8.0F, 5.4F, 4.0F);
      part(root, "front_bumper_right_layer", 208, 62, 10.2F, -11.4F, -31.8F, 8.0F, 5.4F, 4.0F);
      part(root, "front_bumper_center_layer", 208, 72, -7.2F, -10.4F, -32.6F, 14.4F, 4.4F, 3.4F);
      part(root, "front_plow_lower", 208, 72, -14.0F, -8.4F, -34.4F, 28.0F, 3.4F, 3.8F);
      part(root, "front_plow_left_cheek", 208, 62, -19.0F, -13.4F, -33.2F, 6.8F, 4.8F, 4.4F);
      part(root, "front_plow_right_cheek", 208, 62, 12.2F, -13.4F, -33.2F, 6.8F, 4.8F, 4.4F);
      part(root, "front_grille_left", 224, 62, -10.0F, -14.4F, -29.8F, 6.5F, 4.6F, 1.0F);
      part(root, "front_grille_right", 224, 62, 3.5F, -14.4F, -29.8F, 6.5F, 4.6F, 1.0F);
      part(root, "relay_dashboard_panel", 176, 120, -5.6F, -18.2F, -26.2F, 11.2F, 1.8F, 0.8F, 0.08F);
      part(root, "tech_hull_lower", 0, 96, -14.4F, -21.8F, -8.0F, 28.8F, 11.8F, 30.0F);
      part(root, "tech_hull_roof", 56, 96, -13.0F, -25.2F, -6.0F, 26.0F, 3.6F, 25.0F);
      part(root, "rear_relay_module", 80, 96, -13.0F, -24.4F, 17.0F, 26.0F, 14.8F, 11.6F);
      part(root, "left_side_module_depth", 120, 166, -21.5F, -20.0F, -2.0F, 5.0F, 8.8F, 22.0F);
      part(root, "right_side_module_depth", 120, 166, 16.5F, -20.0F, -2.0F, 5.0F, 8.8F, 22.0F);
      part(root, "relay_side_screen_left", 176, 120, -22.0F, -17.5F, 1.0F, 1.0F, 3.2F, 6.0F, 0.08F);
      part(root, "relay_side_screen_right", 176, 120, 21.0F, -17.5F, 1.0F, 1.0F, 3.2F, 6.0F, 0.08F);
      part(root, "left_side_armor_front", 136, 96, -18.8F, -21.0F, -5.5F, 3.8F, 10.5F, 12.0F, 0.12F);
      part(root, "left_side_armor_rear", 136, 96, -18.8F, -21.0F, 8.0F, 3.8F, 10.5F, 12.5F, 0.12F);
      part(root, "right_side_armor_front", 136, 96, 15.0F, -21.0F, -5.5F, 3.8F, 10.5F, 12.0F, 0.12F);
      part(root, "right_side_armor_rear", 136, 96, 15.0F, -21.0F, 8.0F, 3.8F, 10.5F, 12.5F, 0.12F);
      part(root, "roof_radar_base", 0, 138, -9.5F, -28.0F, -2.0F, 19.0F, 2.6F, 17.2F);
      part(root, "relay_scanner_bar", 40, 138, -8.0F, -29.7F, 4.0F, 16.0F, 1.8F, 3.6F);
      part(root, "roof_comms_stack_lower", 40, 138, -4.8F, -31.6F, -5.0F, 9.6F, 3.6F, 8.2F);
      part(root, "roof_comms_stack_upper", 56, 138, -3.0F, -34.8F, -3.8F, 6.0F, 3.2F, 5.8F);
      PartDefinition dish = pivot(root, "relay_dish_back", 0.0F, -30.7F, 10.6F);
      cube(dish, "relay_dish_panel", 56, 138, -7.2F, -2.1F, -5.0F, 14.4F, 4.2F, 10.0F);
      cube(dish, "relay_dish_top_lip", 80, 138, -6.3F, -3.5F, -4.2F, 12.6F, 1.4F, 8.4F);
      cube(dish, "relay_dish_left_lip", 80, 138, -8.0F, -2.3F, -4.8F, 1.4F, 4.8F, 9.6F);
      cube(dish, "relay_dish_right_lip", 80, 138, 6.6F, -2.3F, -4.8F, 1.4F, 4.8F, 9.6F);
      cube(dish, "relay_lens", 96, 138, -4.5F, -2.1F, 5.0F, 9.0F, 3.5F, 1.8F);
      antenna(root, "antenna_left", 120, 138, -9.0F, -34.0F, -4.0F, 9.4F);
      antenna(root, "antenna_right", 120, 138, 9.0F, -34.0F, -4.0F, 9.4F);
      antenna(root, "relay_comms_mast", 136, 138, 0.0F, -39.5F, -1.4F, 18.0F);
      antenna(root, "rear_antenna", 136, 138, 0.5F, -38.6F, 23.3F, 13.0F);
      part(root, "teal_power_core", 152, 138, -5.0F, -21.2F, 25.4F, 10.0F, 7.2F, 1.8F);
      part(root, "relay_core_light", 184, 138, -3.6F, -19.8F, 26.8F, 7.2F, 4.0F, 0.8F, 0.08F);
      part(root, "left_hazard_plate", 0, 166, -18.2F, -17.0F, -20.0F, 2.8F, 6.0F, 12.0F);
      part(root, "right_hazard_plate", 0, 166, 15.4F, -17.0F, -20.0F, 2.8F, 6.0F, 12.0F);
      part(root, "front_light_bar", 24, 166, -12.4F, -14.2F, -30.0F, 24.8F, 2.4F, 2.0F);
      part(root, "rear_light_bar", 72, 166, -11.5F, -14.0F, 29.0F, 23.0F, 2.4F, 2.0F);
      part(root, "left_storage_pods", 120, 166, -19.8F, -16.8F, 0.0F, 3.8F, 7.8F, 17.0F);
      part(root, "right_storage_pods", 120, 166, 16.0F, -16.8F, 0.0F, 3.8F, 7.8F, 17.0F);
      part(root, "relay_shield_front_left", 176, 136, -19.8F, -23.0F, -5.0F, 2.0F, 11.0F, 12.0F, 0.1F);
      part(root, "relay_shield_front_right", 176, 136, 17.8F, -23.0F, -5.0F, 2.0F, 11.0F, 12.0F, 0.1F);
      part(root, "relay_shield_rear_left", 176, 152, -19.8F, -23.0F, 8.0F, 2.0F, 11.0F, 13.0F, 0.1F);
      part(root, "relay_shield_rear_right", 176, 152, 17.8F, -23.0F, 8.0F, 2.0F, 11.0F, 13.0F, 0.1F);
      part(root, "dock_outrigger_left", 208, 120, -21.8F, -8.8F, -2.0F, 2.8F, 4.8F, 17.0F);
      part(root, "dock_outrigger_right", 208, 120, 19.0F, -8.8F, -2.0F, 2.8F, 4.8F, 17.0F);
      part(root, "damage_plate_front", 184, 120, -14.0F, -20.0F, -29.8F, 28.0F, 3.6F, 1.0F, 0.08F);
      part(root, "damage_plate_rear", 184, 128, -10.5F, -23.0F, 29.2F, 21.0F, 7.0F, 1.0F, 0.08F);
      part(root, "route_light", 240, 120, -2.0F, -26.5F, -24.5F, 4.0F, 2.0F, 1.0F);
      part(root, "rear_warning_light", 240, 128, -2.0F, -14.0F, 30.0F, 4.0F, 2.0F, 1.0F);
      return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
   }

   private static ModelPart[] children(ModelPart root, String... names) {
      List<ModelPart> parts = new ArrayList<>();
      for (String name : names) {
         ModelPart part = childOrNull(root, name);
         if (part != null) {
            parts.add(part);
         }
      }
      return parts.toArray(ModelPart[]::new);
   }

   private static ModelPart childOrNull(ModelPart root, String name) {
      if (root == null) {
         return null;
      }
      try {
         return root.getChild(name);
      } catch (NoSuchElementException exception) {
         return null;
      }
   }

   private static ModelPart firstChild(ModelPart root, String... names) {
      for (String name : names) {
         ModelPart part = childOrNull(root, name);
         if (part != null) {
            return part;
         }
      }
      return null;
   }

   private static void putAlias(Map<String, ModelPart> parts, String alias, ModelPart part) {
      if (part != null) {
         parts.put(alias, part);
      }
   }

   private static void setVisible(ModelPart[] parts, boolean visible) {
      for (ModelPart part : parts) {
         part.visible = visible;
      }
   }

   private static void setVisibleCount(ModelPart[] parts, float ratio) {
      int visible = (int)Math.ceil(Mth.clamp(ratio, 0.0F, 1.0F) * parts.length);
      for (int i = 0; i < parts.length; i++) {
         parts[i].visible = i < visible;
      }
   }

   private static void part(PartDefinition root, String name, int u, int v, float x, float y, float z, float width, float height, float depth) {
      root.addOrReplaceChild(name,
         CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, width, height, depth),
         PartPose.offset(0.0F, 24.0F, 0.0F));
   }

   private static void part(PartDefinition root, String name, int u, int v, float x, float y, float z, float width, float height, float depth, float deformation) {
      root.addOrReplaceChild(name,
         CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, width, height, depth, new CubeDeformation(deformation)),
         PartPose.offset(0.0F, 24.0F, 0.0F));
   }

   private static PartDefinition pivot(PartDefinition root, String name, float x, float y, float z) {
      return root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(x, 24.0F + y, z));
   }

   private static void cube(PartDefinition parent, String name, int u, int v, float x, float y, float z, float width, float height, float depth) {
      parent.addOrReplaceChild(name,
         CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, width, height, depth),
         PartPose.offset(0.0F, 0.0F, 0.0F));
   }

   private static void wheel(PartDefinition root, String name, int u, int v, float x, float y, float z, float width, float height, float depth) {
      float pivotX = x + width * 0.5F;
      float pivotY = y + height * 0.5F;
      float pivotZ = z + depth * 0.5F;
      PartDefinition wheel = pivot(root, name, pivotX, pivotY, pivotZ);
      float side = Math.max(0.85F, Math.min(height, depth) * 0.18F);
      float faceInset = Math.max(0.35F, width * 0.12F);
      float hubHeight = height * 0.42F;
      float hubDepth = depth * 0.42F;
      cube(wheel, "tire_top", u, v, -width * 0.5F, -height * 0.5F, -depth * 0.34F, width, side, depth * 0.68F);
      cube(wheel, "tire_bottom", u, v, -width * 0.5F, height * 0.5F - side, -depth * 0.34F, width, side, depth * 0.68F);
      cube(wheel, "tire_front", u + 16, v, -width * 0.5F, -height * 0.28F, -depth * 0.5F, width, height * 0.56F, side);
      cube(wheel, "tire_back", u + 16, v, -width * 0.5F, -height * 0.28F, depth * 0.5F - side, width, height * 0.56F, side);
      cube(wheel, "tread_upper_front", u + 32, v, -width * 0.52F, -height * 0.5F + side * 0.2F, -depth * 0.5F + side * 0.2F, width * 1.04F, side, side);
      cube(wheel, "tread_upper_back", u + 32, v, -width * 0.52F, -height * 0.5F + side * 0.2F, depth * 0.5F - side * 1.2F, width * 1.04F, side, side);
      cube(wheel, "tread_lower_front", u + 32, v, -width * 0.52F, height * 0.5F - side * 1.2F, -depth * 0.5F + side * 0.2F, width * 1.04F, side, side);
      cube(wheel, "tread_lower_back", u + 32, v, -width * 0.52F, height * 0.5F - side * 1.2F, depth * 0.5F - side * 1.2F, width * 1.04F, side, side);
      cube(wheel, "hub_left", u + 48, v, -width * 0.5F - faceInset * 0.35F, -hubHeight * 0.5F, -hubDepth * 0.5F, faceInset, hubHeight, hubDepth);
      cube(wheel, "hub_right", u + 48, v, width * 0.5F - faceInset * 0.65F, -hubHeight * 0.5F, -hubDepth * 0.5F, faceInset, hubHeight, hubDepth);
      cube(wheel, "axle_bar", u + 64, v, -width * 0.55F, -height * 0.09F, -depth * 0.09F, width * 1.1F, height * 0.18F, depth * 0.18F);
   }

   private static void antenna(PartDefinition root, String name, int u, int v, float x, float y, float z, float height) {
      PartDefinition antenna = pivot(root, name, x, y, z);
      cube(antenna, "base", u, v, -1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F);
      cube(antenna, "tip", u + 8, v, -0.5F, -height, -0.5F, 1.0F, height, 1.0F);
   }
}
