package com.knoxhack.echoindustrialnexus.integration;

import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.block.IndustrialMachineBlock;
import com.knoxhack.echoindustrialnexus.registry.ModBlocks;
import com.knoxhack.echoindustrialnexus.registry.ModFluids;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeCategory;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeEntry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeNote;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeProvider;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeSlot;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public final class IndustrialTerminalRecipeProvider implements TerminalRecipeProvider {
   public static final IndustrialTerminalRecipeProvider INSTANCE = new IndustrialTerminalRecipeProvider();
   private static final int ACCENT = 0xFFFF9F3D;
   private static volatile List<TerminalRecipeEntry> cachedRecipes;
   private static volatile ResourceManager cachedResourceManager;

   private IndustrialTerminalRecipeProvider() {
   }

   @Override
   public Identifier id() {
      return id("industrial_recipes");
   }

   @Override
   public List<TerminalRecipeCategory> categories(Player player) {
      List<TerminalRecipeCategory> categories = new ArrayList<>();
      for (IndustrialMachineBlock.MachineKind kind : IndustrialMachineBlock.MachineKind.values()) {
         if (!kind.recipeDriven() && !kind.generator()) {
            continue;
         }
         categories.add(new TerminalRecipeCategory(categoryId(kind), kind.displayName(), machineStack(kind), ACCENT, 400 + kind.ordinal()));
      }
      return categories;
   }

   @Override
   public List<TerminalRecipeEntry> recipes(Player player) {
      ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
      List<TerminalRecipeEntry> recipes = cachedRecipes;
      if (recipes == null || cachedResourceManager != resourceManager) {
         recipes = loadRecipes(resourceManager);
         cachedRecipes = recipes;
         cachedResourceManager = resourceManager;
      }
      return recipes;
   }

   public static void invalidateForTests() {
      cachedRecipes = null;
      cachedResourceManager = null;
   }

   public static Optional<TerminalRecipeEntry> parseForTests(String path, JsonObject json) {
      return parse(Identifier.fromNamespaceAndPath(EchoIndustrialNexus.MODID, "recipe/" + path + ".json"), json);
   }

   private static List<TerminalRecipeEntry> loadRecipes(ResourceManager resourceManager) {
      if (resourceManager == null) {
         return List.of();
      }
      List<TerminalRecipeEntry> entries = new ArrayList<>();
      Map<Identifier, Resource> resources = resourceManager
         .listResources("recipe", id -> EchoIndustrialNexus.MODID.equals(id.getNamespace()) && id.getPath().endsWith(".json"));
      for (Map.Entry<Identifier, Resource> resourceEntry : resources.entrySet()) {
         try (BufferedReader reader = resourceEntry.getValue().openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (!"echoindustrialnexus:industrial_processing".equals(string(json, "type", ""))) {
               continue;
            }
            parse(resourceEntry.getKey(), json).ifPresent(entries::add);
         } catch (IOException | RuntimeException exception) {
            EchoIndustrialNexus.LOGGER.warn("Could not load industrial terminal recipe {}", resourceEntry.getKey(), exception);
         }
      }
      entries.sort(Comparator.comparing(entry -> entry.id().toString()));
      return List.copyOf(entries);
   }

   private static Optional<TerminalRecipeEntry> parse(Identifier resourceId, JsonObject json) {
      IndustrialMachineBlock.MachineKind kind = machineKind(string(json, "machine", "ore_grinder"));
      if (kind == null) {
         return Optional.empty();
      }
      List<ItemStack> inputs = ingredientStacks(json.get("ingredient"));
      String inputLabel = ingredientLabel(json.get("ingredient"));
      Item result = item(json.get("result"));
      int count = integer(json, "count", 1);
      Item catalyst = item(json.get("catalyst"));
      int catalystCount = integer(json, "catalystCount", catalyst == Items.AIR ? 0 : 1);
      Item byproduct = item(json.get("byproduct"));
      int byproductCount = integer(json, "byproductCount", byproduct == Items.AIR ? 0 : 1);
      int byproductChance = integer(json, "byproductChance", 100);
      int duration = integer(json, "duration", 160);
      int fluxCost = integer(json, "fluxCost", 80);
      int fluxGeneration = integer(json, "fluxGeneration", 0);
      int heat = integer(json, "heat", 1);
      int inputFluidId = fluidId(json, "inputFluid", "inputFluidId");
      int inputFluidAmount = integer(json, "inputFluidAmount", 0);
      int outputFluidId = fluidId(json, "outputFluid", "outputFluidId");
      int outputFluidAmount = integer(json, "outputFluidAmount", 0);

      List<TerminalRecipeSlot> slots = new ArrayList<>();
      if (!inputs.isEmpty()) {
         slots.add(new TerminalRecipeSlot(TerminalRecipeSlot.Role.INPUT, inputs, inputLabel));
      }
      if (catalyst != Items.AIR && catalystCount > 0) {
         slots.add(TerminalRecipeSlot.catalyst(new ItemStack(catalyst, catalystCount)));
      }
      if (inputFluidAmount > 0) {
         slots.add(TerminalRecipeSlot.text(TerminalRecipeSlot.Role.INPUT,
            "Input fluid: " + fluidLabel(inputFluidId) + " x" + inputFluidAmount + " mB"));
      }
      if (result != Items.AIR && count > 0) {
         slots.add(TerminalRecipeSlot.output(new ItemStack(result, count)));
      }
      if (byproduct != Items.AIR && byproductCount > 0) {
         slots.add(TerminalRecipeSlot.output(new ItemStack(byproduct, byproductCount)));
      }
      if (outputFluidAmount > 0) {
         slots.add(TerminalRecipeSlot.text(TerminalRecipeSlot.Role.OUTPUT,
            "Output fluid: " + fluidLabel(outputFluidId) + " x" + outputFluidAmount + " mB"));
      }

      List<TerminalRecipeNote> notes = new ArrayList<>();
      if (byproduct != Items.AIR && byproductCount > 0) {
         notes.add(TerminalRecipeNote.info(byproductChance + "% chance for byproduct output."));
      }
      if (fluxCost > 0) {
         notes.add(TerminalRecipeNote.info("Consumes " + fluxCost + " Thermal Flux."));
      }
      if (fluxGeneration > 0) {
         notes.add(TerminalRecipeNote.info("Generates " + fluxGeneration + " Thermal Flux."));
      }
      if (heat > 0) {
         notes.add(TerminalRecipeNote.info("Adds " + heat + " heat."));
      }
      if (inputFluidAmount > 0 && inputs.isEmpty()) {
         notes.add(TerminalRecipeNote.info("Fluid-only input process."));
      }
      if (outputFluidAmount > 0) {
         notes.add(TerminalRecipeNote.info("Produces " + fluidLabel(outputFluidId) + " x" + outputFluidAmount + " mB."));
      }

      ItemStack output = result == Items.AIR || count <= 0 ? ItemStack.EMPTY : new ItemStack(result, count);
      String title = kind.displayName() + ": " + (output.isEmpty() ? "Fluid Process" : output.getHoverName().getString());
      String recipePath = resourceId.getPath().replace("recipe/", "").replace(".json", "");
      return Optional.of(new TerminalRecipeEntry(
         Identifier.fromNamespaceAndPath(EchoIndustrialNexus.MODID, "industrial_processing/" + recipePath),
         categoryId(kind),
         title,
         machineStack(kind),
         slots,
         notes,
         duration,
         false));
   }

   private static Identifier categoryId(IndustrialMachineBlock.MachineKind kind) {
      return Identifier.fromNamespaceAndPath(EchoIndustrialNexus.MODID, kind.getSerializedName());
   }

   private static ItemStack machineStack(IndustrialMachineBlock.MachineKind kind) {
      Optional<Block> block = ModBlocks.ALL_BLOCKS.stream()
         .map(holder -> holder.get())
         .filter(candidate -> candidate instanceof IndustrialMachineBlock machine && machine.kind() == kind)
         .findFirst();
      return block.map(value -> new ItemStack(value.asItem())).orElse(ItemStack.EMPTY);
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoIndustrialNexus.MODID, path);
   }

   private static IndustrialMachineBlock.MachineKind machineKind(String name) {
      for (IndustrialMachineBlock.MachineKind kind : IndustrialMachineBlock.MachineKind.values()) {
         if (kind.getSerializedName().equals(name)) {
            return kind;
         }
      }
      return null;
   }

   private static Item item(JsonElement element) {
      if (element == null || element.isJsonNull()) {
         return Items.AIR;
      }
      String id;
      if (element.isJsonPrimitive()) {
         id = element.getAsString();
      } else if (element.isJsonObject()) {
         JsonObject object = element.getAsJsonObject();
         id = string(object, "item", string(object, "id", ""));
      } else {
         id = "";
      }
      Identifier identifier = Identifier.tryParse(id);
      return identifier == null ? Items.AIR : BuiltInRegistries.ITEM.getOptional(identifier).orElse(Items.AIR);
   }

   private static List<ItemStack> ingredientStacks(JsonElement element) {
      if (element == null || element.isJsonNull()) {
         return List.of();
      }
      List<ItemStack> stacks = new ArrayList<>();
      if (element.isJsonArray()) {
         JsonArray array = element.getAsJsonArray();
         for (JsonElement child : array) {
            stacks.addAll(ingredientStacks(child));
         }
         return List.copyOf(stacks);
      }
      if (element.isJsonObject()) {
         JsonObject object = element.getAsJsonObject();
         Identifier tagId = Identifier.tryParse(string(object, "tag", ""));
         if (tagId != null) {
            BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, tagId))
               .forEach(holder -> stacks.add(new ItemStack(holder.value())));
            return stacks.isEmpty() ? List.of() : List.copyOf(stacks);
         }
      }
      Item parsed = item(element);
      return parsed == Items.AIR ? List.of() : List.of(new ItemStack(parsed));
   }

   private static String ingredientLabel(JsonElement element) {
      if (element != null && element.isJsonObject()) {
         String tag = string(element.getAsJsonObject(), "tag", "");
         if (!tag.isBlank()) {
            return "#" + tag;
         }
      }
      return "";
   }

   private static int integer(JsonObject object, String key, int fallback) {
      return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsInt() : fallback;
   }

   private static int fluidId(JsonObject object, String aliasKey, String numericKey) {
      int numeric = integer(object, numericKey, 0);
      if (numeric > 0) {
         return numeric;
      }
      return ModFluids.idForName(string(object, aliasKey, ""));
   }

   private static String fluidLabel(int id) {
      return id <= 0 ? "Unknown fluid" : ModFluids.displayNameFor(id);
   }

   private static String string(JsonObject object, String key, String fallback) {
      return object.has(key) && object.get(key).isJsonPrimitive() ? object.get(key).getAsString() : fallback;
   }
}
