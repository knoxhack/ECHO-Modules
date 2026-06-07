package com.knoxhack.echoagriculturereclamation.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoFactionProfile;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoagriculturereclamation.config.ReclamationConfig;
import com.knoxhack.echoagriculturereclamation.content.CropCategory;
import com.knoxhack.echoagriculturereclamation.content.CropSpec;
import com.knoxhack.echoagriculturereclamation.content.ReclamationContent;
import com.knoxhack.echoagriculturereclamation.content.SeedProfile;
import com.knoxhack.echoagriculturereclamation.content.SoilState;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class ReclamationCrossAddonIntegration {
   private static final String ASHFALL_MODID = "echoashfallprotocol";
   private static final List<String> RESTORATION_MODIDS = List.of(
      "echorestorationproject",
      "echorestoration",
      "restorationproject",
      "restoration_project"
   );
   private static final Map<String, SoilState> ASHFALL_SOIL_STATES = Map.ofEntries(
      Map.entry("wasteland_dirt", SoilState.DEAD),
      Map.entry("wasteland_grass_block", SoilState.DEAD),
      Map.entry("ashen_wasteland_dirt", SoilState.DEAD),
      Map.entry("burnt_wasteland_soil", SoilState.DEAD),
      Map.entry("contaminated_soil", SoilState.CONTAMINATED),
      Map.entry("toxic_wasteland_grass_block", SoilState.CONTAMINATED),
      Map.entry("toxic_puddle", SoilState.TOXIC_MUD),
      Map.entry("acidic_sludge", SoilState.TOXIC_MUD),
      Map.entry("irradiated_crust", SoilState.IRRADIATED),
      Map.entry("irradiated_shale", SoilState.IRRADIATED),
      Map.entry("toxic_slagstone", SoilState.TOXIC_MUD),
      Map.entry("nexus_cracked_soil", SoilState.IRRADIATED)
   );
   private static final List<Identifier> REMNANT_FACTIONS = List.of(
      id(ASHFALL_MODID, "radwarden_compact")
   );
   private static final List<Identifier> SALVAGER_FACTIONS = List.of(
      id(ASHFALL_MODID, "crashbreak_salvage")
   );
   private static final List<Identifier> MUTANT_FACTIONS = List.of(
      id(ASHFALL_MODID, "sporebound_sanctum")
   );

   private ReclamationCrossAddonIntegration() {
   }

   public static SoilState externalSoilState(BlockState state) {
      Identifier key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
      if (key == null) {
         return null;
      }
      return externalSoilState(key);
   }

   public static SoilState externalSoilState(Identifier key) {
      if (key == null) {
         return null;
      }
      if (ASHFALL_MODID.equals(key.getNamespace())) {
         return ASHFALL_SOIL_STATES.get(key.getPath());
      }
      if (RESTORATION_MODIDS.contains(key.getNamespace())) {
         return restorationProjectSoilState(key.getPath());
      }
      return null;
   }

   public static boolean isExternalRestorableSoil(BlockState state) {
      return externalSoilState(state) != null;
   }

   public static CropSpec recoveredCrop(Player player, RandomSource random) {
      List<CropSpec> preferred = preferredCrops(factionPreference(player));
      if (preferred.isEmpty() || random.nextInt(100) >= 70) {
         return CropSpec.byIndex(random.nextInt(CropSpec.ALL.size()));
      }
      return preferred.get(random.nextInt(preferred.size()));
   }

   public static SeedProfile recoveredProfile(Player player, CropSpec spec, RandomSource random) {
      SeedProfile base = ReclamationContent.progression().recoveredProfile(spec, random);
      int contamination = base.contaminationTier();
      int stability = base.stability();
      FactionPreference preference = factionPreference(player);
      if (preference == FactionPreference.REMNANT) {
         contamination -= 1;
         stability += 12;
      } else if (preference == FactionPreference.SALVAGER) {
         stability += 6;
      } else if (preference == FactionPreference.MUTANT_FRONT && spec.category() == CropCategory.MUTATED) {
         contamination = Math.max(1, contamination);
         stability += 10;
      }
      if (hasRestoreAlignment(player) && spec.restorationWeight() > 1) {
         contamination -= 1;
         stability += 6;
      }
      return new SeedProfile(spec.path(), contamination, stability);
   }

   public static int restorationGain(Player player, int baseGain) {
      if (baseGain <= 0 || !hasRestoreAlignment(player)) {
         return Math.max(0, baseGain);
      }
      return Math.min(12, baseGain + Math.max(1, baseGain / 4));
   }

   public static int weatherGrowthPenalty(Level level, BlockPos pos, boolean safeGreenhouse) {
      if (level == null || pos == null || safeGreenhouse || !ReclamationConfig.weatherCropPenaltiesEnabled() || !EchoRuntimeModules.isLoaded("echoweathercore")) {
         return 0;
      }
      try {
         Class<?> api = Class.forName("com.knoxhack.echoweathercore.api.WeatherCoreApi");
         Object severity = api.getMethod("getWeatherSeverity", Level.class, BlockPos.class).invoke(null, level, pos);
         String name = severity instanceof Enum<?> value ? value.name() : String.valueOf(severity);
         int penalty = switch (name) {
            case "EXTREME" -> 14;
            case "SEVERE" -> 9;
            case "MODERATE" -> 5;
            default -> 0;
         };
         return ReclamationConfig.scaleWeatherPenalty(penalty);
      } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
         return 0;
      }
   }

   public static int poweredThroughputDivisor(Level level, BlockPos pos) {
      if (level == null || pos == null || !ReclamationConfig.powerAccelerationEnabled() || !EchoRuntimeModules.isLoaded("echopowergrid")) {
         return 1;
      }
      try {
         Class<?> api = Class.forName("com.knoxhack.echopowergrid.api.EchoPowerGridApi");
         long available = (Long)api.getMethod("getAvailablePower", Level.class, BlockPos.class).invoke(null, level, pos);
         return available > 0L ? ReclamationConfig.poweredThroughputDivisor() : 1;
      } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
         return 1;
      }
   }

   public static void playFieldCue(Player player, String cue) {
      if (player == null || !EchoRuntimeModules.isLoaded("echosoundcore")) {
         return;
      }
      try {
         Class<?> api = Class.forName("com.knoxhack.echosoundcore.api.SoundCoreApi");
         String method = switch (cue == null ? "" : cue) {
            case "complete", "restore" -> "playObjectiveComplete";
            case "reward" -> "playMissionComplete";
            default -> "playMissionUpdated";
         };
         api.getMethod(method, Player.class).invoke(null, player);
      } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
      }
   }

   public static void markTutorialProgress(Player player, String key) {
      if (!(player instanceof ServerPlayer serverPlayer) || key == null || key.isBlank() || !EchoRuntimeModules.isLoaded("echotutorialcore")) {
         return;
      }
      try {
         Class<?> api = Class.forName("com.knoxhack.echotutorialcore.api.TutorialCoreApi");
         api.getMethod("markProgress", ServerPlayer.class, Identifier.class)
            .invoke(null, serverPlayer, Identifier.fromNamespaceAndPath("echoagriculturereclamation", key));
      } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
      }
   }

   public static void recordMilestone(Player player, String key) {
      if (player instanceof ServerPlayer serverPlayer && key != null && !key.isBlank()) {
         EchoCoreServices.recordMilestone(serverPlayer, "echoagriculturereclamation:" + key);
      }
   }

   public static FactionPreference factionPreference(Player player) {
      int remnant = bestReputation(player, REMNANT_FACTIONS);
      int salvager = bestReputation(player, SALVAGER_FACTIONS);
      int mutant = bestReputation(player, MUTANT_FACTIONS);
      int best = Math.max(remnant, Math.max(salvager, mutant));
      if (best < 25) {
         return FactionPreference.NONE;
      }
      if (remnant == best) {
         return FactionPreference.REMNANT;
      }
      if (salvager == best) {
         return FactionPreference.SALVAGER;
      }
      return FactionPreference.MUTANT_FRONT;
   }

   private static int bestReputation(Player player, List<Identifier> factionIds) {
      if (player == null) {
         return Integer.MIN_VALUE;
      }
      int best = Integer.MIN_VALUE;
      for (Identifier factionId : factionIds) {
         Optional<EchoFactionProfile> profile = EchoCoreServices.factionProfile(player, factionId);
         if (profile.isPresent()) {
            EchoFactionProfile value = profile.get();
            if (value.contacted() || value.reputation() > 0) {
               best = Math.max(best, value.reputation());
            }
         }
      }
      return best;
   }

   private static boolean hasRestoreAlignment(Player player) {
      if (player == null) {
         return false;
      }
      try {
         String path = EchoCoreServices.nexusCampaignPathId(player);
         if ("restore".equalsIgnoreCase(path) || "restored".equalsIgnoreCase(path)) {
            return true;
         }
      } catch (RuntimeException ignored) {
      }
      try {
         return EchoRuntimeModules.isLoaded("echonexusprotocol") && EchoCoreServices.progressLedger(player).hasMilestone("nexus:path:restore")
            || RESTORATION_MODIDS.stream().anyMatch(EchoRuntimeModules::isLoaded);
      } catch (RuntimeException ignored) {
         return false;
      }
   }

   private static SoilState restorationProjectSoilState(String path) {
      return switch (path) {
         case "dead_soil", "dead_earth", "ash_soil", "barren_soil" -> SoilState.DEAD;
         case "contaminated_soil", "tainted_soil", "polluted_soil" -> SoilState.CONTAMINATED;
         case "irradiated_soil", "radioactive_soil", "hot_soil" -> SoilState.IRRADIATED;
         case "toxic_mud", "toxic_soil", "acidic_sludge" -> SoilState.TOXIC_MUD;
         case "purified_soil", "clean_soil" -> SoilState.PURIFIED;
         case "stabilized_soil", "stable_soil" -> SoilState.STABILIZED;
         case "restored_soil", "living_soil" -> SoilState.RESTORED;
         default -> null;
      };
   }

   private static List<CropSpec> preferredCrops(FactionPreference preference) {
      return switch (preference) {
         case REMNANT -> specs("clean_corn", "medicinal_aloe", "filter_reed", "cryo_moss", "ash_wheat");
         case SALVAGER -> specs("nexus_orchid", "signal_fungus", "filter_reed", "cryo_moss");
         case MUTANT_FRONT -> specs("glow_beans", "radleaf", "mutant_berries", "signal_fungus");
         case NONE -> List.of();
      };
   }

   private static List<CropSpec> specs(String... paths) {
      return java.util.Arrays.stream(paths).map(CropSpec::byPath).toList();
   }

   private static Identifier id(String namespace, String path) {
      return Identifier.fromNamespaceAndPath(namespace, path);
   }

   public enum FactionPreference {
      NONE,
      REMNANT,
      SALVAGER,
      MUTANT_FRONT
   }
}
