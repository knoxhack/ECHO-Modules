package com.knoxhack.echoconvoyprotocol.integration;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoconvoyprotocol.block.ConvoyBlock.ConvoyBlockKind;
import com.knoxhack.echoconvoyprotocol.content.ConvoyContent;
import com.knoxhack.echoconvoyprotocol.content.ConvoyRouteDefinition;
import com.knoxhack.echoconvoyprotocol.recipe.ConvoyStationRecipe;
import com.knoxhack.echoconvoyprotocol.registry.ModBlocks;
import com.knoxhack.echoconvoyprotocol.registry.ModItems;
import com.knoxhack.echoconvoyprotocol.registry.ModRecipes;
import com.knoxhack.echoconvoyprotocol.service.ConvoyRouteService;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.index.IIndexContentProvider;
import com.echoplatform.echocore.api.index.IIndexRegistry;
import com.echoplatform.echocore.api.index.IndexBuildContext;
import com.echoplatform.echocore.api.index.IndexCategory;
import com.echoplatform.echocore.api.index.IndexContentBuilder;
import com.echoplatform.echocore.api.index.IndexContentSnapshot;
import com.echoplatform.echocore.api.index.IndexEntry;
import com.echoplatform.echocore.api.index.IndexEntryState;
import com.echoplatform.echocore.api.index.IndexMachineLayoutTemplates;
import com.echoplatform.echocore.api.index.IndexRecipeCategory;
import com.echoplatform.echocore.api.index.IndexRecipeSlot;
import com.echoplatform.echocore.api.index.IndexRecipeView;
import com.echoplatform.echocore.api.index.IndexSlotRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;

public enum ConvoyIndexProvider implements IIndexContentProvider {
   INSTANCE;

   private static final Identifier CATEGORY_MACHINES = id("machines");
   private static final Identifier CATEGORY_ROUTES = id("recipe/convoy_routes");

   public static void register() {
      EchoCoreServices.registerIndexContentProvider(INSTANCE);
   }

   @Override
   public Identifier id() {
      return id("provider/index");
   }

   @Override
   public IndexContentSnapshot snapshot(IndexBuildContext context) {
      Player player = context == null ? null : context.player();
      IndexContentBuilder builder = IndexContentBuilder.create(id());
      register(builder);
      builder.addRecipeCategories(recipeCategories(player));
      List<IndexRecipeView> recipes = recipes(player);
      builder.addRecipes(recipes);
      builder.addMachineLayouts(IndexMachineLayoutTemplates.representativeAll(recipes, "convoy_field_station"));
      return builder.snapshot();
   }

   public void register(IIndexRegistry registry) {
      registry.registerCategory(new IndexCategory(
         CATEGORY_MACHINES,
         "Convoy Stations",
         "Vehicle fabrication, fuel, battery, and field support stations.",
         machineStack(ConvoyBlockKind.VEHICLE_WORKBENCH),
         140,
         EchoConvoyProtocol.MODID));
      for (ConvoyBlockKind kind : ConvoyBlockKind.values()) {
         registry.registerEntry(new IndexEntry(
            id("station/" + kind.getSerializedName()),
            CATEGORY_MACHINES,
            kind.displayName(),
            "",
            kind.recipeDriven() ? "Processes route and vehicle supplies." : "Supports route, vehicle, or field logistics.",
            kind.displayName() + " is part of the Convoy Protocol field station network.",
            machineStack(kind),
            EchoConvoyProtocol.MODID,
            List.of("machine", "convoy", "station", kind.getSerializedName()),
            IndexEntryState.VISIBLE,
            List.of(),
            List.of(itemId(machineStack(kind))),
            List.of(),
            10 + kind.ordinal()));
      }
   }

   public List<IndexRecipeCategory> recipeCategories(Player player) {
      List<IndexRecipeCategory> categories = new ArrayList<>();
      for (ConvoyBlockKind kind : ConvoyBlockKind.values()) {
         if (kind.recipeDriven()) {
            categories.add(new IndexRecipeCategory(categoryId(kind), kind.displayName(),
               machineStack(kind), 0xFFFFD166, 360 + kind.ordinal()));
         }
      }
      categories.add(new IndexRecipeCategory(CATEGORY_ROUTES, "Convoy Routes",
         new ItemStack(ModItems.ROUTE_BEACON.get()), 0xFF00D8FF, 390));
      return categories;
   }

   public List<IndexRecipeView> recipes(Player player) {
      if (player == null || player.level() == null) {
         return List.of();
      }
      List<IndexRecipeView> views = new ArrayList<>();
      for (RecipeHolder<?> holder : recipeHolders(player)) {
         if (!(holder.value() instanceof ConvoyStationRecipe recipe)) {
            continue;
         }
         ItemStack machine = machineStack(recipe.station());
         List<IndexRecipeSlot> slots = new ArrayList<>();
         for (var ingredient : recipe.ingredients()) {
            slots.add(IndexRecipeSlot.inputs(stacks(ingredient.ingredient(), ingredient.count())));
         }
         slots.add(IndexRecipeSlot.machine(machine));
         slots.add(IndexRecipeSlot.output(recipe.result()));
         views.add(new IndexRecipeView(
            holder.id().identifier(),
            categoryId(recipe.station()),
            recipe.result().getHoverName().getString(),
            machine,
            slots,
            List.of("Station: " + recipe.station().displayName(),
               "Energy: " + recipe.energyCost(),
               "Duration: " + recipe.duration() + " ticks"),
            recipe.duration(),
            false,
            EchoConvoyProtocol.MODID));
      }
      for (ConvoyRouteDefinition route : ConvoyContent.routes()) {
         views.add(routeView(route));
      }
      return views;
   }

   private static IndexRecipeView routeView(ConvoyRouteDefinition route) {
      ItemStack machine = new ItemStack(ModBlocks.ROUTE_DISPATCH_TOWER_CONTROLLER.asItem());
      List<IndexRecipeSlot> slots = new ArrayList<>();
      for (ConvoyRouteDefinition.StackSpec cargo : route.requiredCargo()) {
         ItemStack stack = cargo.stack();
         if (stack.isEmpty()) {
            slots.add(new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(), cargo.count() + "x " + cargo.itemId()));
         } else {
            slots.add(IndexRecipeSlot.input(stack));
         }
      }
      if (route.minFuel() > 0 || route.fuelCost() > 0) {
         int fuel = Math.max(ConvoyRouteService.requiredFuel(route), route.fuelCost());
         slots.add(new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(), "Fuel readiness: " + fuel));
      }
      ItemStack vehicle = vehicleKit(route.requiredVehicle());
      if (vehicle.isEmpty()) {
         slots.add(new IndexRecipeSlot(IndexSlotRole.CATALYST, List.of(), "Vehicle: " + route.requiredVehicle()));
      } else {
         slots.add(IndexRecipeSlot.catalyst(vehicle, "Vehicle"));
      }
      slots.add(IndexRecipeSlot.machine(machine));
      slots.add(IndexRecipeSlot.of(IndexSlotRole.MACHINE, new ItemStack(ModBlocks.ROADSIDE_SIGNAL_MARKER.asItem()), "Signal marker"));
      for (ConvoyRouteDefinition.StackSpec reward : route.rewards()) {
         ItemStack stack = reward.stack();
         if (stack.isEmpty()) {
            slots.add(new IndexRecipeSlot(IndexSlotRole.OUTPUT, List.of(), reward.count() + "x " + reward.itemId()));
         } else {
            slots.add(IndexRecipeSlot.output(stack));
         }
      }
      if (route.rewards().isEmpty()) {
         slots.add(new IndexRecipeSlot(IndexSlotRole.OUTPUT, List.of(), "Route completion: " + route.title()));
      }

      List<String> notes = new ArrayList<>();
      if (!route.summary().isBlank()) {
         notes.add(route.summary());
      }
      notes.add("Destination: " + route.destinationHint());
      notes.add("Threat: " + route.threat().label() + " (level " + route.threatLevel() + ")");
      notes.add("Distance: " + route.distance() + " blocks");
      notes.add("Checkpoint: " + route.checkpoint().label() + ", faction " + route.checkpointFactionId()
         + ", reputation " + route.minReputation());
      notes.add("Markers: " + route.requiredSignalMarkers() + ", readiness " + route.requiredReadiness() + "%");
      if (route.logisticsLoadoutId() != null) {
         notes.add("Logistics loadout: " + route.logisticsLoadoutId());
      }
      if (!route.possibleHazards().isEmpty()) {
         notes.add("Possible hazards: " + String.join(", ", route.possibleHazards()));
      }
      notes.add("Field ops: " + route.fieldOps().stageCount(route) + " stage(s), "
         + route.fieldOps().durationTicks(route) + " ticks, profile " + route.fieldOps().incidentProfile());

      return new IndexRecipeView(
         id("recipe/route/" + route.id().getNamespace() + "/" + route.id().getPath()),
         CATEGORY_ROUTES,
         route.title(),
         machine,
         slots,
         notes,
         route.fieldOps().durationTicks(route),
         false,
         EchoConvoyProtocol.MODID);
   }

   private static List<RecipeHolder<?>> recipeHolders(Player player) {
      try {
         List<RecipeHolder<?>> holders = new ArrayList<>();
         for (RecipeHolder<?> holder : allRecipeHolders(player)) {
            if (holder.value().getType() == ModRecipes.CONVOY_STATION_PROCESSING_TYPE.get()) {
               holders.add(holder);
            }
         }
         return holders;
      } catch (RuntimeException ignored) {
         EchoConvoyProtocol.LOGGER.debug("ECHO: Index could not enumerate Convoy station recipes.");
      }
      return List.of();
   }

   private static List<RecipeHolder<?>> allRecipeHolders(Player player) {
      MinecraftServer server = player.level().getServer();
      if (server != null) {
         return List.copyOf(server.getRecipeManager().getRecipes());
      }
      try {
         Object recipes = player.level().recipeAccess().getClass().getMethod("getRecipes")
            .invoke(player.level().recipeAccess());
         if (recipes instanceof Iterable<?> iterable) {
            List<RecipeHolder<?>> holders = new ArrayList<>();
            for (Object candidate : iterable) {
               if (candidate instanceof RecipeHolder<?> holder) {
                  holders.add(holder);
               }
            }
            return holders;
         }
      } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
         EchoConvoyProtocol.LOGGER.debug("ECHO: Index could not enumerate client Convoy station recipes.");
      }
      return List.of();
   }

   private static List<ItemStack> stacks(Ingredient ingredient, int count) {
      if (ingredient == null || ingredient.isEmpty()) {
         return List.of();
      }
      return ingredient.items()
         .map(Holder::value)
         .map(item -> new ItemStack(item, Math.max(1, count)))
         .filter(stack -> !stack.isEmpty())
         .limit(24)
         .toList();
   }

   private static ItemStack vehicleKit(String requiredVehicle) {
      return switch ((requiredVehicle == null ? "" : requiredVehicle).toLowerCase(Locale.ROOT)) {
         case "scrap_bike" -> new ItemStack(ModItems.SCRAP_BIKE_KIT.get());
         case "wasteland_rover" -> new ItemStack(ModItems.WASTELAND_ROVER_KIT.get());
         case "cargo_crawler" -> new ItemStack(ModItems.CARGO_CRAWLER_KIT.get());
         case "armored_relay_truck" -> new ItemStack(ModItems.ARMORED_RELAY_TRUCK_KIT.get());
         default -> ItemStack.EMPTY;
      };
   }

   private static Identifier categoryId(ConvoyBlockKind kind) {
      return id("recipe/" + kind.getSerializedName());
   }

   private static ItemStack machineStack(ConvoyBlockKind kind) {
      return new ItemStack(machineBlock(kind).get());
   }

   private static EchoBackendRegistryEntry<Block> machineBlock(ConvoyBlockKind kind) {
      return switch (kind) {
         case VEHICLE_WORKBENCH -> ModBlocks.VEHICLE_WORKBENCH;
         case FUEL_STILL -> ModBlocks.FUEL_STILL;
         case BATTERY_CHARGING_PAD -> ModBlocks.BATTERY_CHARGING_PAD;
         case VEHICLE_DOCK -> ModBlocks.VEHICLE_DOCK;
         case VEHICLE_UPGRADE_BAY -> ModBlocks.VEHICLE_UPGRADE_BAY;
         case CONVOY_BEACON -> ModBlocks.CONVOY_BEACON;
         case ROADSIDE_SIGNAL_MARKER -> ModBlocks.ROADSIDE_SIGNAL_MARKER;
         case CARGO_ANCHOR -> ModBlocks.CARGO_ANCHOR;
         case FIELD_REPAIR_STATION -> ModBlocks.FIELD_REPAIR_STATION;
      };
   }

   private static Identifier itemId(ItemStack stack) {
      Identifier id = stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
      return id == null ? Identifier.withDefaultNamespace("air") : id;
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, sanitize(path));
   }

   private static String sanitize(String path) {
      String clean = path == null ? "unknown" : path.trim().toLowerCase(Locale.ROOT);
      clean = clean.replace('\\', '/').replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
      while (clean.contains("//")) {
         clean = clean.replace("//", "/");
      }
      return clean.isBlank() ? "unknown" : clean;
   }
}
