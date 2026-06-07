package com.knoxhack.echoarmory.content;

import com.knoxhack.echoarmory.EchoArmory;
import com.knoxhack.echoarmory.block.ArmoryStationBlock.StationKind;
import com.knoxhack.echoarmory.item.ArmoryGearItem;
import com.knoxhack.echoarmory.item.ArmoryData;
import java.util.Comparator;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ArmoryContent {
   private static final Map<Identifier, GearDefinition> DEFAULT_GEAR = new LinkedHashMap<>();
   private static final Map<Identifier, ModuleDefinition> DEFAULT_MODULES = new LinkedHashMap<>();
   private static final Map<Identifier, SynergyDefinition> DEFAULT_SYNERGIES = new LinkedHashMap<>();
   private static final Map<Identifier, ArmoryLoadoutDefinition> DEFAULT_LOADOUTS = new LinkedHashMap<>();
   private static final Map<Identifier, FactionUnlockDefinition> DEFAULT_FACTION_UNLOCKS = new LinkedHashMap<>();
   private static final Map<Identifier, BossRecommendationDefinition> DEFAULT_BOSS_RECOMMENDATIONS = new LinkedHashMap<>();
   private static final Map<Identifier, StationRecipeDefinition> DEFAULT_STATION_RECIPES = new LinkedHashMap<>();
   private static final Map<Identifier, FiringModeDefinition> DEFAULT_FIRING_MODES = new LinkedHashMap<>();
   private static final Map<Identifier, RouteProfileDefinition> DEFAULT_ROUTE_PROFILES = new LinkedHashMap<>();
   private static volatile LoadedContent jsonContent = LoadedContent.empty();

   static {
      defaults();
   }

   private ArmoryContent() {
   }

   public static void replaceJsonContent(LoadedContent loaded) {
      jsonContent = loaded == null ? LoadedContent.empty() : loaded;
      EchoArmory.LOGGER.info("ECHO Armory loaded {} gear, {} modules, {} synergies, {} loadouts, {} faction unlocks, {} boss recommendations, {} station recipes, {} firing modes, and {} route profiles from JSON.",
         jsonContent.gear().size(), jsonContent.modules().size(), jsonContent.synergies().size(),
         jsonContent.loadouts().size(), jsonContent.factionUnlocks().size(), jsonContent.bossRecommendations().size(),
         jsonContent.stationRecipes().size(), jsonContent.firingModes().size(), jsonContent.routeProfiles().size());
   }

   public static List<GearDefinition> gear() {
      Map<Identifier, GearDefinition> merged = new LinkedHashMap<>(DEFAULT_GEAR);
      merged.putAll(jsonContent.gear());
      return merged.values().stream().sorted(Comparator.comparingInt(GearDefinition::tier).thenComparing(gear -> gear.id().toString())).toList();
   }

   public static List<ModuleDefinition> modules() {
      Map<Identifier, ModuleDefinition> merged = new LinkedHashMap<>(DEFAULT_MODULES);
      merged.putAll(jsonContent.modules());
      return merged.values().stream().sorted(Comparator.comparing(module -> module.id().toString())).toList();
   }

   public static List<SynergyDefinition> synergies() {
      Map<Identifier, SynergyDefinition> merged = new LinkedHashMap<>(DEFAULT_SYNERGIES);
      merged.putAll(jsonContent.synergies());
      return merged.values().stream().sorted(Comparator.comparing(synergy -> synergy.id().toString())).toList();
   }

   public static List<ArmoryLoadoutDefinition> loadouts() {
      Map<Identifier, ArmoryLoadoutDefinition> merged = new LinkedHashMap<>(DEFAULT_LOADOUTS);
      merged.putAll(jsonContent.loadouts());
      return merged.values().stream().sorted(Comparator.comparingInt(ArmoryLoadoutDefinition::order).thenComparing(loadout -> loadout.id().toString())).toList();
   }

   public static List<FactionUnlockDefinition> factionUnlocks() {
      Map<Identifier, FactionUnlockDefinition> merged = new LinkedHashMap<>(DEFAULT_FACTION_UNLOCKS);
      merged.putAll(jsonContent.factionUnlocks());
      return merged.values().stream().sorted(Comparator.comparing(unlock -> unlock.id().toString())).toList();
   }

   public static List<BossRecommendationDefinition> bossRecommendations() {
      Map<Identifier, BossRecommendationDefinition> merged = new LinkedHashMap<>(DEFAULT_BOSS_RECOMMENDATIONS);
      merged.putAll(jsonContent.bossRecommendations());
      return merged.values().stream().sorted(Comparator.comparing(recommendation -> recommendation.id().toString())).toList();
   }

   public static List<StationRecipeDefinition> stationRecipes() {
      Map<Identifier, StationRecipeDefinition> merged = new LinkedHashMap<>(DEFAULT_STATION_RECIPES);
      merged.putAll(jsonContent.stationRecipes());
      return merged.values().stream()
         .sorted(Comparator.comparingInt(StationRecipeDefinition::order).thenComparing(recipe -> recipe.id().toString()))
         .toList();
   }

   public static List<FiringModeDefinition> firingModes() {
      Map<Identifier, FiringModeDefinition> merged = new LinkedHashMap<>(DEFAULT_FIRING_MODES);
      merged.putAll(jsonContent.firingModes());
      return merged.values().stream().sorted(Comparator.comparing(mode -> mode.id().toString())).toList();
   }

   public static List<RouteProfileDefinition> routeProfiles() {
      Map<Identifier, RouteProfileDefinition> merged = new LinkedHashMap<>(DEFAULT_ROUTE_PROFILES);
      merged.putAll(jsonContent.routeProfiles());
      return merged.values().stream()
         .sorted(Comparator.comparingInt(RouteProfileDefinition::order).thenComparing(profile -> profile.id().toString()))
         .toList();
   }

   public static Optional<FiringModeDefinition> firingModeFor(GearDefinition gear) {
      if (gear == null) {
         return Optional.empty();
      }
      return firingModes().stream()
         .filter(mode -> mode.matches(gear))
         .max(Comparator.comparingInt(mode -> firingModeScore(mode, gear)));
   }

   public static Optional<RouteProfileDefinition> routeProfileFor(ArmoryLoadoutDefinition loadout) {
      if (loadout == null) {
         return Optional.empty();
      }
      String loadoutId = loadout.id().toString();
      String loadoutPath = loadout.id().getPath();
      return routeProfiles().stream()
         .filter(profile -> profile.loadoutId().equals(loadoutId) || profile.loadoutId().equals(loadoutPath))
         .findFirst();
   }

   public static Optional<GearDefinition> gear(String id) {
      Identifier identifier = safeId(id);
      return identifier == null ? Optional.empty() : gear().stream().filter(definition -> definition.id().equals(identifier)).findFirst();
   }

   public static Optional<ModuleDefinition> module(String id) {
      Identifier identifier = safeId(id);
      return identifier == null ? Optional.empty() : modules().stream().filter(definition -> definition.id().equals(identifier)).findFirst();
   }

   public static void clearJsonForTests() {
      jsonContent = LoadedContent.empty();
   }

   public static List<String> validationErrors() {
      ArrayList<String> errors = new ArrayList<>();
      Set<String> knownTags = new HashSet<>();
      for (GearDefinition definition : gear()) {
         if (!registeredKind(definition.id(), ArmoryGearItem.ArmoryGearKind.WEAPON)
            && !registeredKind(definition.id(), ArmoryGearItem.ArmoryGearKind.ARMOR)) {
            errors.add("Gear id is not a registered Armory gear item: " + definition.id());
         }
         knownTags.addAll(definition.allowedSlots());
         knownTags.addAll(definition.tags());
      }
      for (ModuleDefinition definition : modules()) {
         if (!registeredKind(definition.id(), ArmoryGearItem.ArmoryGearKind.MODULE)) {
            errors.add("Module id is not a registered Armory module item: " + definition.id());
         }
         knownTags.add(definition.slotType());
         knownTags.add(definition.effectType());
         knownTags.addAll(definition.synergyTags());
      }
      for (SynergyDefinition definition : synergies()) {
         knownTags.addAll(definition.requiredTags());
      }
      for (ArmoryLoadoutDefinition definition : loadouts()) {
         validateLoadoutItem(definition.weapon(), "weapon", definition, errors);
         for (String armorId : definition.armor()) {
            validateLoadoutItem(armorId, "armor", definition, errors);
         }
         for (String moduleId : definition.modules()) {
            if (module(moduleId).isEmpty()) {
               errors.add("Loadout " + definition.id() + " references missing module " + moduleId);
            }
         }
      }
      for (FactionUnlockDefinition definition : factionUnlocks()) {
         boolean found = gear().stream().anyMatch(gear -> gear.id().getPath().equals(definition.unlockId())
            || gear.id().toString().equals(definition.unlockId()));
         if (!found) {
            errors.add("Faction unlock " + definition.id() + " references missing gear unlock " + definition.unlockId());
         }
      }
      for (BossRecommendationDefinition definition : bossRecommendations()) {
         for (String tag : definition.recommendedTags()) {
            if (!knownTags.contains(tag)) {
               errors.add("Boss recommendation " + definition.id() + " references unknown tag " + tag);
            }
         }
      }
      for (StationRecipeDefinition definition : stationRecipes()) {
         for (String tag : definition.gearTags()) {
            if (!knownTags.contains(tag) && gear(tag).isEmpty()) {
               errors.add("Station recipe " + definition.id() + " references unknown gear tag " + tag);
            }
         }
         for (String tag : definition.moduleTags()) {
            if (!knownTags.contains(tag) && module(tag).isEmpty()) {
               errors.add("Station recipe " + definition.id() + " references unknown module tag " + tag);
            }
         }
      }
      for (FiringModeDefinition definition : firingModes()) {
         if (definition.gearTags().stream().noneMatch(knownTags::contains)) {
            errors.add("Firing mode " + definition.id() + " has no known gear tag selector.");
         }
      }
      for (RouteProfileDefinition definition : routeProfiles()) {
         if (!definition.loadoutId().isBlank() && ArmoryContent.loadouts().stream().noneMatch(loadout ->
            loadout.id().toString().equals(definition.loadoutId()) || loadout.id().getPath().equals(definition.loadoutId()))) {
            errors.add("Route profile " + definition.id() + " references missing loadout " + definition.loadoutId());
         }
         for (String tag : definition.recommendedTags()) {
            if (!knownTags.contains(tag)) {
               errors.add("Route profile " + definition.id() + " references unknown tag " + tag);
            }
         }
      }
      return List.copyOf(errors);
   }

   static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoArmory.MODID, path);
   }

   private static Identifier safeId(String id) {
      if (id == null || id.isBlank()) {
         return null;
      }
      try {
         Identifier parsed = Identifier.parse(id);
         return parsed.getNamespace().equals("minecraft") && !id.contains(":") ? Identifier.fromNamespaceAndPath(EchoArmory.MODID, id) : parsed;
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private static void validateLoadoutItem(String itemId, String label, ArmoryLoadoutDefinition loadout, List<String> errors) {
      if (itemId == null || itemId.isBlank()) {
         return;
      }
      if (gear(itemId).isEmpty()) {
         errors.add("Loadout " + loadout.id() + " references missing " + label + " gear " + itemId);
      }
   }

   private static boolean registeredKind(Identifier id, ArmoryGearItem.ArmoryGearKind kind) {
      return BuiltInRegistries.ITEM.getOptional(id)
         .filter(item -> item instanceof ArmoryGearItem gearItem && gearItem.gearKind() == kind)
         .isPresent();
   }

   private static int firingModeScore(FiringModeDefinition mode, GearDefinition gear) {
      int score = 0;
      if (gear.baseType().equals(mode.family())) {
         score += 6;
      }
      if (gear.tags().contains(mode.family())) {
         score += 4;
      }
      for (String tag : mode.gearTags()) {
         if (gear.baseType().equals(tag)) {
            score += 3;
         }
         if (gear.tags().contains(tag)) {
            score += 2;
         }
         if (gear.id().getPath().contains(tag)) {
            score += 2;
         }
      }
      return score;
   }

   private static void defaults() {
      gear("alloy_sword", "Alloy Sword", "melee", 1, 3, 7.0F, 0, 120, "Early survival", "", List.of("damage", "elemental", "stability", "utility"), List.of("alloy", "blade"));
      gear("frost_blade", "Frost Blade", "melee", 2, 3, 8.5F, 0, 180, "Mid-tech", "", List.of("damage", "elemental", "stability", "utility", "power"), List.of("frost", "blade"));
      gear("veil_sabre", "Veil Sabre", "melee", 3, 4, 10.0F, 0, 260, "Industrial", "echoashfallprotocol:radwarden_compact", List.of("damage", "elemental", "stability", "utility", "power"), List.of("veil", "blade"));
      gear("harmonic_staff", "Harmonic Staff", "staff", 2, 4, 6.0F, 0, 240, "Mid-tech", "", List.of("elemental", "utility", "magic", "power"), List.of("resonance", "staff"));
      gear("arcane_dagger", "Arcane Dagger", "melee", 2, 2, 5.5F, 0, 120, "Mid-tech", "", List.of("damage", "utility", "stability"), List.of("arcane", "quick"));
      gear("energy_rifle", "Energy Rifle", "ranged", 3, 3, 9.5F, 0, 320, "Industrial", "echoashfallprotocol:crashbreak_salvage", List.of("damage", "elemental", "stability", "power"), List.of("energy", "ranged"));
      gear("veil_bow", "Veil Bow", "ranged", 2, 3, 8.0F, 0, 220, "Mid-tech", "", List.of("damage", "elemental", "utility", "power"), List.of("veil", "ranged"));
      gear("convergence_gun", "Convergence Gun", "ranged", 4, 4, 13.0F, 0, 520, "Endgame", "echoashfallprotocol:radwarden_compact", List.of("damage", "elemental", "stability", "utility", "power"), List.of("convergence", "energy", "ranged"));
      gear("resonance_hammer", "Resonance Hammer", "heavy", 3, 3, 12.0F, 0, 260, "Industrial", "", List.of("damage", "elemental", "stability", "power"), List.of("resonance", "heavy"));
      gear("sigil_chakram", "Sigil Chakram", "ranged", 4, 2, 8.5F, 0, 260, "Endgame", "", List.of("elemental", "utility", "magic"), List.of("sigil", "ranged"));
      gear("construct_gauntlet", "Construct Gauntlet", "support", 3, 3, 7.0F, 2, 300, "Industrial", "echoashfallprotocol:crashbreak_salvage", List.of("utility", "support", "power"), List.of("construct", "drone"));
      gear("arcane_shield", "Arcane Shield", "shield", 2, 2, 1.0F, 5, 180, "Mid-tech", "", List.of("defense", "utility", "power"), List.of("shield", "arcane"));
      gear("veil_resistant_helm", "Veil-resistant Helm", "armor_head", 2, 3, 0.0F, 3, 140, "Mid-tech", "echoashfallprotocol:radwarden_compact", List.of("defense", "survival", "utility", "power"), List.of("veil", "armor", "head"));
      gear("thermal_chestplate", "Thermal Chestplate", "armor_chest", 2, 3, 0.0F, 8, 180, "Mid-tech", "", List.of("defense", "survival", "utility", "power"), List.of("thermal", "armor", "chest"));
      gear("drone_leggings", "Drone-enhanced Leggings", "armor_legs", 3, 2, 0.0F, 6, 220, "Industrial", "echoashfallprotocol:crashbreak_salvage", List.of("defense", "mobility", "support", "utility"), List.of("drone", "armor", "legs"));
      gear("orbital_boots", "Orbital Boots", "armor_feet", 4, 2, 0.0F, 4, 220, "Endgame", "", List.of("defense", "mobility", "survival"), List.of("orbital", "armor", "feet"));
      gear("construct_harness", "Construct Harness", "armor_chest", 4, 4, 0.0F, 9, 360, "Endgame", "echoashfallprotocol:crashbreak_salvage", List.of("defense", "support", "utility", "power"), List.of("construct", "drone", "armor", "chest"));
      gear("sigil_augmented_suit", "Sigil Augmented Suit", "armor_chest", 4, 5, 0.0F, 10, 420, "Endgame", "", List.of("defense", "elemental", "survival", "mobility", "utility", "power"), List.of("sigil", "armor", "suit"));

      module("fire_core", "Fire Core", "elemental", "fire", 2.0F, 0, 20, 8, 0, 0, 0, 20, 0, List.of("melee", "ranged", "staff"), List.of("fire"));
      module("frost_core", "Frost Core", "elemental", "frost", 1.5F, 0, 18, 6, 0, 0, 35, 0, 0, List.of("melee", "ranged", "staff", "armor_chest"), List.of("frost"));
      module("lightning_core", "Lightning Core", "elemental", "lightning", 2.5F, 0, 26, 12, 0, 0, 0, 0, 0, List.of("melee", "ranged", "staff"), List.of("lightning"));
      module("void_core", "Void Core", "elemental", "void", 3.0F, 0, 32, 18, 0, 0, 0, 0, 30, List.of("melee", "ranged", "staff"), List.of("void", "veil"));
      module("stability_rune", "Stability Rune", "stability", "handling", 0.5F, 0, 0, 0, 0, 0, 0, 0, 5, List.of("melee", "ranged", "heavy", "staff"), List.of("stable"));
      module("life_leech_sigil", "Life Leech Sigil", "utility", "life_leech", 1.0F, 0, 18, 14, 0, 0, 0, 0, 0, List.of("melee", "heavy"), List.of("leech"));
      module("veil_shield", "Veil Shield", "defense", "fracture", 0.0F, 2, 12, 4, 0, 0, 0, 0, 45, List.of("armor_head", "armor_chest", "armor_legs", "armor_feet", "shield"), List.of("veil", "shield"));
      module("thermal_regulator", "Thermal Regulator", "survival", "thermal", 0.0F, 1, 8, 0, 0, 0, 40, 40, 0, List.of("armor_chest", "armor_legs", "armor_feet"), List.of("thermal"));
      module("gas_mask_filter", "Gas Mask Module", "survival", "toxic", 0.0F, 1, 5, 0, 55, 0, 0, 0, 0, List.of("armor_head", "armor_chest"), List.of("toxic"));
      module("radiation_shield", "Radiation Shield", "survival", "radiation", 0.0F, 2, 10, 2, 0, 55, 0, 0, 0, List.of("armor_head", "armor_chest", "armor_legs"), List.of("radiation"));
      module("mobility_servo", "Mobility Servo", "mobility", "movement", 0.0F, 0, 6, 0, 0, 0, 0, 0, 0, List.of("armor_legs", "armor_feet"), List.of("mobility"));
      module("drone_dock", "Drone Dock", "support", "repair_drone", 0.0F, 1, 20, 8, 0, 0, 0, 0, 10, List.of("armor_chest", "armor_legs", "support"), List.of("drone", "construct"));

      synergy("frost_aegis", "Frost Aegis", List.of("frost", "armor"), "ice_aura", 2, "Full frost protection and a frost weapon create a slowing aura.");
      synergy("veilbreaker", "Veilbreaker", List.of("veil", "blade", "shield"), "fracture_immunity", 3, "Veil armor and a Veil blade temporarily suppress fracture exposure.");
      synergy("construct_command", "Construct Command", List.of("construct", "drone"), "drone_scaling", 2, "Construct gauntlets and drone armor increase repair and shield output.");

      loadout("toxic_breach_kit", "Toxic Breach Kit", 0, "echoarmory:gas_mask_filter", "echoarmory:alloy_sword", List.of("echoarmory:thermal_chestplate"), List.of("echoarmory:gas_mask_filter"), 1, 0, Map.of(ArmoryData.ProtectionType.TOXIC, 55), "echoarmory:toxic_breach_kit");
      loadout("fracture_guardian_kit", "Fracture Guardian Kit", 20, "echoarmory:veil_sabre", "echoarmory:veil_sabre", List.of("echoarmory:veil_resistant_helm", "echoarmory:construct_harness"), List.of("echoarmory:veil_shield", "echoarmory:void_core"), 3, 0, Map.of(ArmoryData.ProtectionType.FRACTURE, 60), "echoarmory:fracture_guardian_kit");
      loadout("orbital_assault_kit", "Orbital Assault Kit", 40, "echoarmory:energy_rifle", "echoarmory:energy_rifle", List.of("echoarmory:thermal_chestplate", "echoarmory:orbital_boots"), List.of("echoarmory:stability_rune", "echoarmory:mobility_servo", "echoarmory:thermal_regulator"), 3, 0, Map.of(ArmoryData.ProtectionType.COLD, 40, ArmoryData.ProtectionType.HEAT, 40), "echoarmory:orbital_assault_kit");

      factionUnlock("salvager_energy_weapons", "echoashfallprotocol:crashbreak_salvage", 35, "energy_rifle", "Energy Rifle fabrication");
      factionUnlock("remnant_veil_armor", "echoashfallprotocol:radwarden_compact", 35, "veil_resistant_helm", "Veil-resistant armor");
      factionUnlock("construct_harness", "echoashfallprotocol:crashbreak_salvage", 55, "construct_harness", "Construct drone harness");

      bossRecommendation("fracture_heart", "Fracture Heart", 3, 60, List.of("veil", "shield", "void"), "Bring a Veil blade, Veil Shield modules, and stabilized energy reserves.");
      bossRecommendation("veilbound_guardian", "Veilbound Guardian", 2, 45, List.of("frost", "veil", "stable"), "Frost control and stability runes reduce Guardian counter-bursts.");

      firingMode("energy_bolt", "Energy Bolt", "energy", FiringModeDefinition.ProjectileKind.ENERGY_BOLT, 1, 18, 32, 18.0D, 0.95D, 1.0F, 4, List.of("energy", "ranged", "staff"));
      firingMode("veil_arrow", "Veil Arrow", "veil", FiringModeDefinition.ProjectileKind.VEIL_ARROW, 1, 14, 28, 20.0D, 1.15D, 0.95F, 3, List.of("veil", "bow", "ranged"));
      firingMode("sigil_chakram", "Sigil Chakram", "sigil", FiringModeDefinition.ProjectileKind.SIGIL_CHAKRAM, 1, 22, 36, 14.0D, 0.75D, 1.15F, 8, List.of("sigil", "chakram", "magic", "ranged"));

      stationRecipe("bench_repair_tune", "Bench Repair/Tune", StationKind.ARMORY_BENCH, "repair_or_tune", List.of("alloy", "blade", "armor"), List.of(), List.of("echoarmory:armory_alloy_plate"), "repair or initialize gear", 0, 0);
      stationRecipe("weapon_forge_upgrade", "Weapon Forge Upgrade", StationKind.WEAPON_FORGE, "upgrade_weapon", List.of("blade", "heavy", "ranged", "staff", "energy"), List.of(), List.of("echoarmory:resonance_shard", "echoarmory:veil_crystal", "echoarmory:blackbox_fragment"), "increase weapon tier", 0, 10);
      stationRecipe("armor_forge_upgrade", "Armor Forge Upgrade", StationKind.ARMOR_FORGE, "upgrade_armor", List.of("armor", "survival", "mobility"), List.of(), List.of("echoarmory:armory_alloy_plate", "echoarmory:veil_crystal", "echoarmory:blackbox_fragment"), "increase armor tier", 0, 20);
      stationRecipe("module_table_install", "Module Table Install", StationKind.MODULE_UPGRADE_TABLE, "install_module", List.of("alloy", "blade", "armor", "ranged", "staff"), List.of("elemental", "survival", "stability", "utility"), List.of(), "install module", 0, 30);
      stationRecipe("energy_station_recharge", "Energy Station Recharge", StationKind.ENERGY_CORE_CHARGING_STATION, "recharge", List.of("power", "energy", "ranged", "staff"), List.of(), List.of("echoarmory:veil_crystal", "echoarmory:resonance_shard"), "restore energy core", 0, 40);
      stationRecipe("sigil_engraver_trim", "Sigil Engraver Trim", StationKind.SIGIL_ENGRAVER, "engrave", List.of("sigil", "armor", "blade"), List.of(), List.of("echoarmory:resonance_shard", "echoarmory:veil_crystal"), "apply cosmetic sigil", 0, 50);
      stationRecipe("veil_infuser_upgrade", "Veil Infuser Upgrade", StationKind.VEIL_INFUSER, "install_veil_module", List.of("veil", "power", "armor", "blade"), List.of("veil", "void", "fracture"), List.of("echoarmory:veil_crystal"), "install fracture/void module", 0, 60);
      stationRecipe("construct_dock_tune", "Construct Dock Tune", StationKind.CONSTRUCT_DOCK, "install_construct_module", List.of("construct", "drone", "support"), List.of("drone", "construct", "support"), List.of("echoarmory:blackbox_fragment"), "tune construct support", 0, 70);
      stationRecipe("loadout_terminal_bind", "Loadout Terminal Bind", StationKind.LOADOUT_TERMINAL, "bind_route_kit", List.of("alloy", "armor", "energy", "ranged", "veil"), List.of(), List.of(), "bind best route kit", 0, 80);
      stationRecipe("rack_stage_readiness", "Rack/Stand Staging", StationKind.WEAPON_RACK, "stage_readiness", List.of("blade", "heavy", "ranged", "staff"), List.of(), List.of(), "stage weapon readiness", 0, 90);
      stationRecipe("stand_stage_readiness", "Armor Stand Staging", StationKind.ARMOR_STAND, "stage_readiness", List.of("armor", "survival", "mobility"), List.of(), List.of(), "stage armor readiness", 0, 100);

      routeProfile("toxic_breach", "Toxic Breach", "ashfall_toxic", "toxic", "", "echoarmory:toxic_breach_kit", 1, Map.of(ArmoryData.ProtectionType.TOXIC, 55), List.of("toxic", "survival", "alloy"), 0);
      routeProfile("fracture_guardian", "Fracture Guardian", "ashfall_fracture", "fracture", "echoarmory:fracture_heart", "echoarmory:fracture_guardian_kit", 3, Map.of(ArmoryData.ProtectionType.FRACTURE, 60), List.of("veil", "shield", "void"), 20);
      routeProfile("orbital_assault", "Orbital Assault", "orbital_assault", "thermal", "", "echoarmory:orbital_assault_kit", 3, Map.of(ArmoryData.ProtectionType.COLD, 40, ArmoryData.ProtectionType.HEAT, 40), List.of("mobility", "thermal", "energy"), 40);
   }

   private static void gear(String path, String title, String baseType, int tier, int slots, float damage, int defense, int energy, String stage, String factionGate, List<String> allowedSlots, List<String> tags) {
      Identifier id = id(path);
      DEFAULT_GEAR.put(id, new GearDefinition(id, title, baseType, tier, slots, damage, defense, energy, stage, factionGate, allowedSlots, tags));
   }

   private static void module(String path, String title, String slotType, String effectType, float damageBonus, int defenseBonus, int energyCost, int instability, int toxic, int radiation, int cold, int heat, int fracture, List<String> compatibleTypes, List<String> synergyTags) {
      Identifier id = id(path);
      DEFAULT_MODULES.put(id, new ModuleDefinition(id, title, slotType, effectType, damageBonus, defenseBonus, energyCost, instability, toxic, radiation, cold, heat, fracture, compatibleTypes, synergyTags));
   }

   private static void synergy(String path, String title, List<String> tags, String effect, int potency, String hint) {
      Identifier id = id(path);
      DEFAULT_SYNERGIES.put(id, new SynergyDefinition(id, title, tags, effect, potency, hint));
   }

   private static void loadout(
      String path,
      String title,
      int order,
      String icon,
      String weapon,
      List<String> armor,
      List<String> modules,
      int minTier,
      int minProtection,
      Map<ArmoryData.ProtectionType, Integer> requiredProtections,
      String logisticsPreset
   ) {
      Identifier id = id(path);
      DEFAULT_LOADOUTS.put(id, new ArmoryLoadoutDefinition(id, title, order, Identifier.parse(icon), weapon, armor, modules, minTier, minProtection, requiredProtections, logisticsPreset));
   }

   private static void factionUnlock(String path, String factionId, int minReputation, String unlockId, String title) {
      Identifier id = id(path);
      DEFAULT_FACTION_UNLOCKS.put(id, new FactionUnlockDefinition(id, Identifier.parse(factionId), minReputation, unlockId, title));
   }

   private static void bossRecommendation(String path, String boss, int tier, int fracture, List<String> tags, String hint) {
      Identifier id = id(path);
      DEFAULT_BOSS_RECOMMENDATIONS.put(id, new BossRecommendationDefinition(id, boss, tier, fracture, tags, hint));
   }

   private static void firingMode(String path, String title, String family, FiringModeDefinition.ProjectileKind kind, int ammoCost, int energyCost, int cooldown, double range, double velocity, float damageScale, int instability, List<String> gearTags) {
      Identifier id = id(path);
      DEFAULT_FIRING_MODES.put(id, new FiringModeDefinition(id, title, family, kind, ammoCost, energyCost, cooldown, range, velocity, damageScale, instability, gearTags));
   }

   private static void stationRecipe(String path, String title, StationKind kind, String operation, List<String> gearTags, List<String> moduleTags, List<String> auxItems, String result, int energyCost, int order) {
      Identifier id = id(path);
      DEFAULT_STATION_RECIPES.put(id, new StationRecipeDefinition(id, title, kind, operation, gearTags, moduleTags, auxItems, result, energyCost, order));
   }

   private static void routeProfile(String path, String title, String family, String hazard, String boss, String loadout, int minTier, Map<ArmoryData.ProtectionType, Integer> protections, List<String> tags, int order) {
      Identifier id = id(path);
      DEFAULT_ROUTE_PROFILES.put(id, new RouteProfileDefinition(id, title, family, hazard, boss, loadout, minTier, protections, tags, order));
   }

   public record LoadedContent(
      Map<Identifier, GearDefinition> gear,
      Map<Identifier, ModuleDefinition> modules,
      Map<Identifier, SynergyDefinition> synergies,
      Map<Identifier, ArmoryLoadoutDefinition> loadouts,
      Map<Identifier, FactionUnlockDefinition> factionUnlocks,
      Map<Identifier, BossRecommendationDefinition> bossRecommendations,
      Map<Identifier, StationRecipeDefinition> stationRecipes,
      Map<Identifier, FiringModeDefinition> firingModes,
      Map<Identifier, RouteProfileDefinition> routeProfiles
   ) {
      public LoadedContent {
         gear = Map.copyOf(gear == null ? Map.of() : gear);
         modules = Map.copyOf(modules == null ? Map.of() : modules);
         synergies = Map.copyOf(synergies == null ? Map.of() : synergies);
         loadouts = Map.copyOf(loadouts == null ? Map.of() : loadouts);
         factionUnlocks = Map.copyOf(factionUnlocks == null ? Map.of() : factionUnlocks);
         bossRecommendations = Map.copyOf(bossRecommendations == null ? Map.of() : bossRecommendations);
         stationRecipes = Map.copyOf(stationRecipes == null ? Map.of() : stationRecipes);
         firingModes = Map.copyOf(firingModes == null ? Map.of() : firingModes);
         routeProfiles = Map.copyOf(routeProfiles == null ? Map.of() : routeProfiles);
      }

      public static LoadedContent empty() {
         return new LoadedContent(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
      }
   }
}
