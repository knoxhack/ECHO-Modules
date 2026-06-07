package com.knoxhack.echoconvoyprotocol;

import com.knoxhack.echocore.api.config.EchoConfigCategory;
import com.knoxhack.echocore.api.config.EchoConfigEntry;
import com.knoxhack.echocore.api.config.EchoConfigModule;
import com.knoxhack.echocore.api.config.EchoConfigProvider;
import com.knoxhack.echocore.api.config.EchoConfigRegistry;
import com.knoxhack.echocore.api.config.EchoConfigSide;
import com.knoxhack.echocore.api.config.EchoNativeConfigSpec;
import java.util.List;

public final class Config {
   private static final EchoNativeConfigSpec.Builder BUILDER = new EchoNativeConfigSpec.Builder();

   public static final EchoNativeConfigSpec.DoubleValue ROUTE_DIFFICULTY_MULTIPLIER = BUILDER
      .comment("Multiplier applied to Convoy route fuel gates and route threat pressure. 1.0 keeps bundled route balance.")
      .defineInRange("routes.difficultyMultiplier", 1.0D, 0.25D, 4.0D);

   public static final EchoNativeConfigSpec.DoubleValue FIELD_OPS_DURATION_SCALE = BUILDER
      .comment("Multiplier applied to depot Field Ops duration. 1.0 keeps datapack-authored duration.")
      .defineInRange("fieldOps.durationScale", 1.0D, 0.25D, 8.0D);

   public static final EchoNativeConfigSpec.EnumValue<VehicleJoinPolicy> VEHICLE_JOIN_POLICY = BUILDER
      .comment("How physical vehicles participate in depot Field Ops.")
      .defineEnum("fieldOps.vehicleJoinPolicy", VehicleJoinPolicy.HYBRID);

   public static final EchoNativeConfigSpec.BooleanValue PRECISE_HOLOMAP_MARKERS = BUILDER
      .comment("Allow precise Convoy HoloMap markers when roadside signals or generated structures are known.")
      .define("holomap.preciseMarkers", true);

   public static final EchoNativeConfigSpec.IntValue INCIDENT_FREQUENCY_PERCENT = BUILDER
      .comment("Percent of eligible data-driven Field Ops incidents that may trigger. 100 keeps authored defaults.")
      .defineInRange("fieldOps.incidentFrequencyPercent", 100, 0, 100);

   public static final EchoNativeConfigSpec.BooleanValue DEBUG_DIAGNOSTICS = BUILDER
      .comment("Write extra Convoy integration, datapack migration, and fallback diagnostics to the log.")
      .define("diagnostics.debug", false);

   public static final EchoNativeConfigSpec SPEC = BUILDER.build();

   private Config() {
   }

   public static double routeDifficultyMultiplier() {
      return ROUTE_DIFFICULTY_MULTIPLIER.get();
   }

   public static double fieldOpsDurationScale() {
      return FIELD_OPS_DURATION_SCALE.get();
   }

   public static VehicleJoinPolicy vehicleJoinPolicy() {
      return VEHICLE_JOIN_POLICY.get();
   }

   public static boolean preciseHoloMapMarkers() {
      return PRECISE_HOLOMAP_MARKERS.get();
   }

   public static int incidentFrequencyPercent() {
      return INCIDENT_FREQUENCY_PERCENT.get();
   }

   public static boolean debugDiagnostics() {
      return DEBUG_DIAGNOSTICS.get();
   }

   public static void registerEchoConfig() {
      EchoConfigRegistry.register(EchoConfigProvider.of(EchoConvoyProtocol.MODID, () -> new EchoConfigModule(
         EchoConvoyProtocol.MODID,
         "Convoy Protocol",
         List.of(
            new EchoConfigCategory("routes", "Routes", List.of(
               EchoConfigEntry.doubleSpec("difficulty_multiplier", "Difficulty Multiplier",
                  "Multiplier applied to Convoy route fuel gates and route threat pressure.",
                  EchoConfigSide.COMMON, ROUTE_DIFFICULTY_MULTIPLIER, 0.25D, 4.0D,
                  true, false, false)
            )),
            new EchoConfigCategory("field_ops", "Field Ops", List.of(
               EchoConfigEntry.doubleSpec("duration_scale", "Duration Scale",
                  "Multiplier applied to depot Field Ops duration.",
                  EchoConfigSide.COMMON, FIELD_OPS_DURATION_SCALE, 0.25D, 8.0D,
                  true, false, false),
               EchoConfigEntry.enumSpec("vehicle_join_policy", "Vehicle Join Policy",
                  "Controls whether depot Field Ops require, ignore, or are enhanced by physical vehicles.",
                  EchoConfigSide.COMMON, VEHICLE_JOIN_POLICY, VehicleJoinPolicy.class,
                  true, false, false),
               EchoConfigEntry.intSpec("incident_frequency_percent", "Incident Frequency",
                  "Percent of eligible data-driven Field Ops incidents that may trigger.",
                  EchoConfigSide.COMMON, INCIDENT_FREQUENCY_PERCENT, 0, 100,
                  true, false, false)
            )),
            new EchoConfigCategory("holomap", "HoloMap", List.of(
               EchoConfigEntry.booleanSpec("precise_markers", "Precise Markers",
                  "Allow precise Convoy HoloMap markers when roadside signals or generated structures are known.",
                  EchoConfigSide.COMMON, PRECISE_HOLOMAP_MARKERS,
                  true, false, false)
            )),
            new EchoConfigCategory("diagnostics", "Diagnostics", List.of(
               EchoConfigEntry.booleanSpec("debug", "Debug Diagnostics",
                  "Write extra Convoy integration, datapack migration, and fallback diagnostics to the log.",
                  EchoConfigSide.COMMON, DEBUG_DIAGNOSTICS,
                  true, false, false)
            ))
         )
      )));
   }

   public enum VehicleJoinPolicy {
      HYBRID,
      VEHICLE_REQUIRED,
      DEPOT_ONLY
   }
}
