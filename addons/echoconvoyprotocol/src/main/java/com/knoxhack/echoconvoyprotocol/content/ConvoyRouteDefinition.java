package com.knoxhack.echoconvoyprotocol.content;

import com.knoxhack.echoconvoyprotocol.Config;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleKind;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record ConvoyRouteDefinition(
   Identifier id,
   String title,
   String summary,
   int order,
   String requiredVehicle,
   int minFuel,
   List<StackSpec> requiredCargo,
   List<StackSpec> rewards,
   int threatLevel,
   ThreatSpec threat,
   Identifier checkpointFactionId,
   int minReputation,
   CheckpointSpec checkpoint,
   String destinationHint,
   int requiredSignalMarkers,
   int minDistanceFromStart,
   List<RouteLeg> legs,
   String missionType,
   int distance,
   String biomeTheme,
   int fuelCost,
   int cargoCapacityRecommendation,
   int requiredReadiness,
   List<String> possibleHazards,
   String holomapIcon,
   int holomapColor,
   String unlockRequirement,
   String logisticsNetworkId,
   Identifier logisticsLoadoutId,
   boolean autoRequestCargo,
   Identifier holomapLayerId,
   FieldOpsSpec fieldOps
) {
   public ConvoyRouteDefinition {
      if (id == null) {
         throw new IllegalArgumentException("Convoy route id is required.");
      }
      title = title == null || title.isBlank() ? id.getPath() : title.strip();
      summary = summary == null ? "" : summary.strip();
      requiredVehicle = requiredVehicle == null || requiredVehicle.isBlank() ? "any" : requiredVehicle.strip();
      minFuel = Math.max(0, minFuel);
      requiredCargo = List.copyOf(requiredCargo == null ? List.of() : requiredCargo);
      rewards = List.copyOf(rewards == null ? List.of() : rewards);
      threatLevel = Math.max(0, Math.min(5, threatLevel));
      threat = threat == null ? ThreatSpec.fromThreatLevel(threatLevel) : threat;
      checkpointFactionId = checkpointFactionId == null ? Identifier.fromNamespaceAndPath("echoashfallprotocol", "crashbreak_salvage") : checkpointFactionId;
      minReputation = Math.max(0, minReputation);
      checkpoint = checkpoint == null ? CheckpointSpec.defaults() : checkpoint;
      destinationHint = destinationHint == null ? "Overworld roadside corridor" : destinationHint.strip();
      requiredSignalMarkers = Math.max(1, requiredSignalMarkers);
      minDistanceFromStart = Math.max(0, minDistanceFromStart);
      legs = List.copyOf(legs == null || legs.isEmpty()
         ? defaultLegs(requiredSignalMarkers, minDistanceFromStart, minReputation > 0)
         : legs);
      requiredSignalMarkers = Math.max(1, legs.size());
      missionType = missionType == null || missionType.isBlank() ? "delivery" : missionType.strip();
      distance = Math.max(minDistanceFromStart, distance);
      biomeTheme = biomeTheme == null || biomeTheme.isBlank() ? "ruined_highway" : biomeTheme.strip();
      fuelCost = Math.max(minFuel, fuelCost);
      cargoCapacityRecommendation = Math.max(0, cargoCapacityRecommendation);
      requiredReadiness = Math.max(0, Math.min(100, requiredReadiness));
      possibleHazards = List.copyOf(possibleHazards == null ? List.of() : possibleHazards);
      holomapIcon = holomapIcon == null || holomapIcon.isBlank() ? "convoy_route" : holomapIcon.strip();
      holomapColor = holomapColor == 0 ? 0xFF00D8FF : holomapColor;
      unlockRequirement = unlockRequirement == null ? "" : unlockRequirement.strip();
      logisticsNetworkId = logisticsNetworkId == null || logisticsNetworkId.isBlank() ? "global" : logisticsNetworkId.strip();
      holomapLayerId = holomapLayerId == null
         ? Identifier.fromNamespaceAndPath("echoconvoyprotocol", "convoy_routes")
         : holomapLayerId;
      fieldOps = fieldOps == null
         ? FieldOpsSpec.defaults(distance, requiredSignalMarkers, biomeTheme)
         : fieldOps;
   }

   public ConvoyRouteDefinition(
      Identifier id,
      String title,
      String summary,
      int order,
      String requiredVehicle,
      int minFuel,
      List<StackSpec> requiredCargo,
      List<StackSpec> rewards,
      int threatLevel,
      Identifier checkpointFactionId,
      int minReputation,
      String destinationHint
   ) {
      this(
         id,
         title,
         summary,
         order,
         requiredVehicle,
         minFuel,
         requiredCargo,
         rewards,
         threatLevel,
         ThreatSpec.fromThreatLevel(threatLevel),
         checkpointFactionId,
         minReputation,
         CheckpointSpec.defaults(),
         destinationHint,
         1,
         24,
         List.of(),
         "delivery",
         24,
         "ruined_highway",
         minFuel,
         1,
         50,
         List.of(),
         "convoy_route",
         0xFF00D8FF,
         "",
         "global",
         null,
         false,
         null,
         null
      );
   }

   public ConvoyRouteDefinition(
      Identifier id,
      String title,
      String summary,
      int order,
      String requiredVehicle,
      int minFuel,
      List<StackSpec> requiredCargo,
      List<StackSpec> rewards,
      int threatLevel,
      Identifier checkpointFactionId,
      int minReputation,
      String destinationHint,
      int requiredSignalMarkers,
      int minDistanceFromStart
   ) {
      this(
         id,
         title,
         summary,
         order,
         requiredVehicle,
         minFuel,
         requiredCargo,
         rewards,
         threatLevel,
         ThreatSpec.fromThreatLevel(threatLevel),
         checkpointFactionId,
         minReputation,
         CheckpointSpec.defaults(),
         destinationHint,
         requiredSignalMarkers,
         minDistanceFromStart,
         List.of(),
         "delivery",
         minDistanceFromStart,
         "ruined_highway",
         minFuel,
         1,
         50,
         List.of(),
         "convoy_route",
         0xFF00D8FF,
         "",
         "global",
         null,
         false,
         null,
         null
      );
   }

   public ConvoyRouteDefinition(
      Identifier id,
      String title,
      String summary,
      int order,
      String requiredVehicle,
      int minFuel,
      List<StackSpec> requiredCargo,
      List<StackSpec> rewards,
      int threatLevel,
      Identifier checkpointFactionId,
      int minReputation,
      String destinationHint,
      int requiredSignalMarkers,
      int minDistanceFromStart,
      List<RouteLeg> legs
   ) {
      this(
         id,
         title,
         summary,
         order,
         requiredVehicle,
         minFuel,
         requiredCargo,
         rewards,
         threatLevel,
         ThreatSpec.fromThreatLevel(threatLevel),
         checkpointFactionId,
         minReputation,
         CheckpointSpec.defaults(),
         destinationHint,
         requiredSignalMarkers,
         minDistanceFromStart,
         legs,
         "delivery",
         minDistanceFromStart,
         "ruined_highway",
         minFuel,
         1,
         50,
         List.of(),
         "convoy_route",
         0xFF00D8FF,
         "",
         "global",
         null,
         false,
         null,
         null
      );
   }

   public boolean acceptsVehicle(ConvoyVehicleKind kind) {
      return "any".equals(requiredVehicle) || kind.getSerializedName().equals(requiredVehicle);
   }

   public RouteLeg leg(int index) {
      if (index < 0) {
         return legs.getFirst();
      }
      if (index >= legs.size()) {
         return legs.getLast();
      }
      return legs.get(index);
   }

   private static List<RouteLeg> defaultLegs(int requiredSignalMarkers, int minDistanceFromStart, boolean checkpointRoute) {
      int count = Math.max(1, requiredSignalMarkers);
      int distance = Math.max(0, minDistanceFromStart);
      List<RouteLeg> defaults = new java.util.ArrayList<>();
      for (int i = 0; i < count; i++) {
         boolean finalLeg = i == count - 1;
         int legDistance = finalLeg ? distance : distance * (i + 1) / count;
         defaults.add(new RouteLeg(
            finalLeg ? "destination" : "leg_" + (i + 1),
            finalLeg ? "Destination Signal" : "Roadside Signal " + (i + 1),
            legDistance,
            checkpointRoute && finalLeg,
            RouteLeg.DEFAULT_ROADSIDE_STRUCTURE
         ));
      }
      return defaults;
   }

   public record RouteLeg(String id, String title, int minDistanceFromStart, boolean requiresCheckpoint, Identifier roadsideStructure) {
      public static final Identifier DEFAULT_ROADSIDE_STRUCTURE =
         Identifier.fromNamespaceAndPath("echoconvoyprotocol", "roadside/signal_marker");

      public RouteLeg {
         id = id == null || id.isBlank() ? "destination" : id.strip();
         title = title == null || title.isBlank() ? id : title.strip();
         minDistanceFromStart = Math.max(0, minDistanceFromStart);
         roadsideStructure = roadsideStructure == null ? DEFAULT_ROADSIDE_STRUCTURE : roadsideStructure;
      }
   }

   public record ThreatSpec(
      String label,
      Identifier entityType,
      int minCount,
      int maxCount,
      int chanceOneIn,
      int cooldownTicks,
      int vehicleDamage,
      double spawnRadius,
      String warning
   ) {
      public ThreatSpec {
         label = label == null || label.isBlank() ? "Road Ambush" : label.strip();
         entityType = entityType == null ? Identifier.withDefaultNamespace("zombie") : entityType;
         minCount = Math.max(0, minCount);
         maxCount = Math.max(minCount, maxCount);
         chanceOneIn = Math.max(0, chanceOneIn);
         cooldownTicks = Math.max(20, cooldownTicks);
         vehicleDamage = Math.max(0, vehicleDamage);
         spawnRadius = Math.max(4.0D, spawnRadius);
         warning = warning == null || warning.isBlank() ? "Road ambush on %route%. Keep moving." : warning.strip();
      }

      public static ThreatSpec fromThreatLevel(int threatLevel) {
         int level = Math.max(0, Math.min(5, threatLevel));
         if (level == 0) {
            return new ThreatSpec("No Road Threat", Identifier.withDefaultNamespace("zombie"), 0, 0, 0, 1200, 0, 8.0D, "");
         }
         return new ThreatSpec(
            "Road Ambush",
            Identifier.withDefaultNamespace("zombie"),
            Math.max(1, level),
            Math.max(1, level),
            Math.max(2, 18 - level * 3),
            1200,
            level,
            8.0D,
            "Road ambush on %route%. Keep moving."
         );
      }

      public boolean enabled() {
         return maxCount > 0 && chanceOneIn > 0;
      }

      public int rollCount(RandomSource random) {
         if (maxCount <= minCount) {
            return minCount;
         }
         return minCount + random.nextInt(maxCount - minCount + 1);
      }

      public String warningFor(ConvoyRouteDefinition route) {
         return warning
            .replace("%route%", route.title())
            .replace("%threat%", label)
            .replace("%entity%", entityType.toString());
      }
   }

   public record CheckpointSpec(String label, String warning, String clearedMessage) {
      public CheckpointSpec {
         label = label == null || label.isBlank() ? "Faction Checkpoint" : label.strip();
         warning = warning == null || warning.isBlank()
            ? "%checkpoint% blocked: %faction% reputation %reputation%/%required%."
            : warning.strip();
         clearedMessage = clearedMessage == null || clearedMessage.isBlank()
            ? "%checkpoint% cleared for %route%."
            : clearedMessage.strip();
      }

      public static CheckpointSpec defaults() {
         return new CheckpointSpec("Faction Checkpoint", "", "");
      }

      public String blockedMessage(ConvoyRouteDefinition route, int reputation) {
         return tokens(warning, route, reputation);
      }

      public String clearedMessage(ConvoyRouteDefinition route) {
         return tokens(clearedMessage, route, route.minReputation());
      }

      private String tokens(String message, ConvoyRouteDefinition route, int reputation) {
         return message
            .replace("%checkpoint%", label)
            .replace("%route%", route.title())
            .replace("%faction%", route.checkpointFactionId().toString())
            .replace("%reputation%", Integer.toString(reputation))
            .replace("%required%", Integer.toString(route.minReputation()));
      }
   }

   public record FieldOpsSpec(
      int durationTicks,
      int stageCount,
      Identifier incidentProfile,
      String vehicleJoinPolicy,
      String completionMode
   ) {
      public FieldOpsSpec {
         durationTicks = Math.max(200, durationTicks);
         stageCount = Math.max(1, stageCount);
         incidentProfile = incidentProfile == null
            ? Identifier.fromNamespaceAndPath("echoconvoyprotocol", "standard")
            : incidentProfile;
         vehicleJoinPolicy = normalizeVehicleJoinPolicy(vehicleJoinPolicy);
         completionMode = normalizeCompletionMode(completionMode);
      }

      public static FieldOpsSpec defaults(int distance, int stageCount, String biomeTheme) {
         String profile = biomeTheme == null || biomeTheme.isBlank() ? "standard" : biomeTheme.strip().toLowerCase(java.util.Locale.ROOT);
         int duration = Math.max(300, Math.max(1, distance) * 4);
         return new FieldOpsSpec(duration, Math.max(1, stageCount),
            Identifier.fromNamespaceAndPath("echoconvoyprotocol", profile),
            "optional",
            "depot_return");
      }

      public int durationTicks(ConvoyRouteDefinition route) {
         int base = Math.max(200, durationTicks <= 0 ? Math.max(300, route.distance() * 4) : durationTicks);
         return Math.max(200, (int)Math.round(base * Config.fieldOpsDurationScale()));
      }

      public int stageCount(ConvoyRouteDefinition route) {
         return Math.max(1, stageCount <= 0 ? route.legs().size() : stageCount);
      }

      public Config.VehicleJoinPolicy effectiveVehicleJoinPolicy() {
         Config.VehicleJoinPolicy global = Config.vehicleJoinPolicy();
         if (global != Config.VehicleJoinPolicy.HYBRID) {
            return global;
         }
         return switch (vehicleJoinPolicy) {
            case "required", "vehicle_required" -> Config.VehicleJoinPolicy.VEHICLE_REQUIRED;
            case "depot_only" -> Config.VehicleJoinPolicy.DEPOT_ONLY;
            default -> Config.VehicleJoinPolicy.HYBRID;
         };
      }

      private static String normalizeVehicleJoinPolicy(String raw) {
         String value = raw == null || raw.isBlank() ? "optional" : raw.strip().toLowerCase(java.util.Locale.ROOT);
         return switch (value) {
            case "optional", "hybrid" -> "optional";
            case "required", "vehicle_required" -> "required";
            case "depot", "depot-only", "depot_only" -> "depot_only";
            default -> value;
         };
      }

      private static String normalizeCompletionMode(String raw) {
         String value = raw == null || raw.isBlank() ? "depot_return" : raw.strip().toLowerCase(java.util.Locale.ROOT);
         return switch (value) {
            case "depot-return" -> "depot_return";
            case "signal-return" -> "signal_return";
            case "manual-closeout" -> "manual_closeout";
            default -> value;
         };
      }
   }

   public record StackSpec(Identifier itemId, int count) {
      public StackSpec {
         itemId = itemId == null ? Identifier.withDefaultNamespace("air") : itemId;
         count = Math.max(1, count);
      }

      public Item item() {
         Item item = BuiltInRegistries.ITEM.getValue(itemId);
         return item == null ? Items.AIR : item;
      }

      public ItemStack stack() {
         return new ItemStack(item(), count);
      }
   }
}
