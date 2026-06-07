package com.knoxhack.echologisticsnetwork.content;

import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import com.knoxhack.echocore.api.EchoCoreServices;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class LogisticsContent {
   private static final Map<Identifier, SupplyCategory> DEFAULT_CATEGORIES = new LinkedHashMap<>();
   private static final Map<Identifier, LoadoutPreset> DEFAULT_LOADOUTS = new LinkedHashMap<>();
   private static final Map<Identifier, FactionDepotOffer> DEFAULT_OFFERS = new LinkedHashMap<>();
   private static final List<Identifier> DEFAULT_LOADOUT_TARGETS = List.of(
      Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, "loadout_locker"),
      Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, "supply_crate"),
      Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, "route_requester"),
      Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, "auto_restock_station"),
      Identifier.fromNamespaceAndPath("echoindustrialnexus", "input_depot_crate")
   );
   private static volatile LoadedContent jsonContent = LoadedContent.empty();

   static {
      defaults();
   }

   private LogisticsContent() {
   }

   public static void replaceJsonContent(LoadedContent loaded) {
      jsonContent = loaded == null ? LoadedContent.empty() : loaded;
      EchoLogisticsNetwork.LOGGER.info("ECHO Logistics loaded {} JSON categories, {} JSON loadouts, and {} JSON depot offers.",
         jsonContent.categories().size(), jsonContent.loadouts().size(), jsonContent.offers().size());
      EchoCoreServices.invalidateIndexRecipes("logistics content changed");
   }

   public static List<SupplyCategory> categories() {
      Map<Identifier, SupplyCategory> merged = new LinkedHashMap<>(DEFAULT_CATEGORIES);
      merged.putAll(jsonContent.categories());
      return merged.values().stream()
         .sorted(Comparator.comparingInt(SupplyCategory::order).thenComparing(category -> category.id().toString()))
         .toList();
   }

   public static List<LoadoutPreset> loadouts() {
      Map<Identifier, LoadoutPreset> merged = new LinkedHashMap<>(DEFAULT_LOADOUTS);
      merged.putAll(jsonContent.loadouts());
      return merged.values().stream()
         .sorted(Comparator.comparingInt(LoadoutPreset::order).thenComparing(loadout -> loadout.id().toString()))
         .toList();
   }

   public static List<FactionDepotOffer> offers() {
      Map<Identifier, FactionDepotOffer> merged = new LinkedHashMap<>(DEFAULT_OFFERS);
      merged.putAll(jsonContent.offers());
      return merged.values().stream()
         .sorted(Comparator.comparing(offer -> offer.id().toString()))
         .toList();
   }

   public static Optional<SupplyCategory> category(String id) {
      Identifier identifier = safeId(id);
      return identifier == null ? Optional.empty() : categories().stream().filter(category -> category.id().equals(identifier)).findFirst();
   }

   public static Optional<LoadoutPreset> loadout(String id) {
      Identifier identifier = safeId(id);
      if (identifier == null) {
         return loadouts().stream().findFirst();
      }
      return loadouts().stream().filter(loadout -> loadout.id().equals(identifier)).findFirst();
   }

   public static String firstLoadoutId() {
      return loadouts().stream().findFirst().map(loadout -> loadout.id().toString()).orElse("");
   }

   public static void clearJsonForTests() {
      jsonContent = LoadedContent.empty();
   }

   private static Identifier safeId(String id) {
      if (id == null || id.isBlank()) {
         return null;
      }
      try {
         return Identifier.parse(id);
      } catch (RuntimeException exception) {
         return null;
      }
   }

   private static void defaults() {
      category("water", "Water", 0, 0xFF4FB8FF, 16);
      category("food", "Food", 10, 0xFF92F7A6, 16);
      category("medicine", "Medicine", 20, 0xFFFF8FA3, 8);
      category("filters", "Filters", 30, 0xFF66E8FF, 8);
      category("ammo", "Ammo", 40, 0xFFFFD166, 64);
      category("machine_parts", "Machine Parts", 50, 0xFFFF9F3D, 16);
      category("rocket_parts", "Rocket Parts", 60, 0xFFBFD0FF, 8);
      category("station_parts", "Station Parts", 70, 0xFF8AF6B6, 8);
      category("blackbox_parts", "Blackbox Parts", 80, 0xFFD48BFF, 4);
      category("faction_goods", "Faction Goods", 90, 0xFFFFD166, 12);

      loadout("toxic_expedition_kit", "Toxic Expedition Kit", 0, "minecraft:glass_bottle", 160,
         reqCategory("water", 3), reqCategory("food", 3), reqCategory("filters", 2), reqCategory("medicine", 1));
      loadout("radiation_expedition_kit", "Radiation Expedition Kit", 10, "minecraft:redstone", 180,
         reqCategory("water", 3), reqCategory("food", 2), reqCategory("medicine", 2), reqCategory("filters", 1));
      loadout("cryogenic_expedition_kit", "Cryogenic Expedition Kit", 20, "minecraft:packed_ice", 180,
         reqCategory("food", 2), reqCategory("medicine", 1), reqCategory("machine_parts", 2));
      loadout("orbital_launch_kit", "Orbital Launch Kit", 30, "minecraft:firework_rocket", 220,
         reqCategory("rocket_parts", 4), reqCategory("machine_parts", 4), reqCategory("water", 4), reqCategory("food", 4));
      loadout("stationfall_boarding_kit", "Stationfall Boarding Kit", 40, "minecraft:iron_door", 220,
         reqCategory("station_parts", 4), reqCategory("filters", 2), reqCategory("medicine", 2), reqCategory("ammo", 16));
      loadout("nexus_core_kit", "Nexus Core Kit", 50, "minecraft:ender_eye", 240,
         reqCategory("machine_parts", 4), reqCategory("blackbox_parts", 1), reqCategory("medicine", 2));
      loadout("blackbox_finale_kit", "Blackbox Finale Kit", 60, "minecraft:echo_shard", 260,
         reqCategory("blackbox_parts", 3), reqCategory("station_parts", 2), reqCategory("ammo", 32), reqCategory("medicine", 3));

      // Faction offers are shipped as datapack JSON so ItemStacks are not created before item components bind.
   }

   private static void category(String id, String title, int order, int color, int lowStockTarget) {
      Identifier identifier = Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, id);
      DEFAULT_CATEGORIES.put(identifier, new SupplyCategory(identifier, title, order, color, lowStockTarget,
         Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, "echo_logistics/" + id)));
   }

   private static void loadout(String id, String title, int order, String icon, int ticks, LoadoutRequirement... requirements) {
      Identifier identifier = Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, id);
      DEFAULT_LOADOUTS.put(identifier, new LoadoutPreset(identifier, title, order, Identifier.parse(icon), List.of(requirements), DEFAULT_LOADOUT_TARGETS, ticks));
   }

   private static LoadoutRequirement reqCategory(String category, int count) {
      return new LoadoutRequirement(LoadoutRequirement.Kind.CATEGORY, Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, category), count, false);
   }

   public record LoadedContent(
      Map<Identifier, SupplyCategory> categories,
      Map<Identifier, LoadoutPreset> loadouts,
      Map<Identifier, FactionDepotOffer> offers
   ) {
      public LoadedContent {
         categories = Map.copyOf(categories == null ? Map.of() : categories);
         loadouts = Map.copyOf(loadouts == null ? Map.of() : loadouts);
         offers = Map.copyOf(offers == null ? Map.of() : offers);
      }

      public static LoadedContent empty() {
         return new LoadedContent(Map.of(), Map.of(), Map.of());
      }
   }
}
