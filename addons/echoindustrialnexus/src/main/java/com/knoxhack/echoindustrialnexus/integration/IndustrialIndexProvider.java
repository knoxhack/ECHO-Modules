package com.knoxhack.echoindustrialnexus.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.index.IIndexContentProvider;
import com.knoxhack.echocore.api.index.IndexBuildContext;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.index.IndexMachineLayout;
import com.knoxhack.echocore.api.index.IndexMachineLayoutGauge;
import com.knoxhack.echocore.api.index.IndexMachineLayoutSlot;
import com.knoxhack.echocore.api.index.IndexRecipeCategory;
import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexSlotRole;
import com.knoxhack.echocore.api.index.IndexSourceFact;
import com.knoxhack.echocore.api.index.IndexSourceKind;
import com.knoxhack.echomultiblockcore.api.AutomationOutput;
import com.knoxhack.echomultiblockcore.api.AutomationRecipeRegistry;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.api.IndustrialProcessSource;
import com.knoxhack.echoindustrialnexus.block.IndustrialMachineBlock;
import com.knoxhack.echoindustrialnexus.menu.IndustrialMachineMenu;
import com.knoxhack.echoindustrialnexus.recipe.IndustrialProcessingRecipe;
import com.knoxhack.echoindustrialnexus.registry.ModBlocks;
import com.knoxhack.echoindustrialnexus.registry.ModFluids;
import com.knoxhack.echoindustrialnexus.registry.ModRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;

public enum IndustrialIndexProvider implements IIndexContentProvider {
   INSTANCE;

   private static final int ACCENT = 0xFFFF9F3D;
   private static final List<StaticSource> STATIC_SOURCES = List.of(
      source("gas_mask_filter", "source/mission/filters_survival", IndexSourceKind.MISSION_REWARD,
         "Industrial support cache: Filters for Survival", "Mission cache reward and Filter Press automation support."),
      source("industrial_membrane", "source/mission/filters_survival", IndexSourceKind.MISSION_REWARD,
         "Industrial support cache: Filters for Survival", "Mission cache reward and Filter Press component."),
      source("warden_thermal_core", "source/furnace_warden", IndexSourceKind.STRUCTURE,
         "Furnace Warden reward", "Dropped after defeating the Industrial Nexus Furnace Warden encounter."),
      source("furnace_warden_trophy", "source/furnace_warden", IndexSourceKind.STRUCTURE,
         "Furnace Warden trophy", "Boss trophy source from the Furnace Warden route."),
      source("furnace_warden_wake_core", "source/poi/abandoned_thermal_plant", IndexSourceKind.STRUCTURE,
         "Abandoned Thermal Plant cache", "Used to wake the Furnace Warden in Industrial thermal plant content."),
      source("assembly_line_blueprint", "source/poi/rusted_factory_complex", IndexSourceKind.STRUCTURE,
         "Rusted Factory Complex schematic", "Industrial factory schematic found through POI and mission progression."),
      source("scrap_processor_blueprint", "source/poi/rusted_factory_complex", IndexSourceKind.STRUCTURE,
         "Rusted Factory Complex schematic", "Industrial factory schematic found through POI and mission progression."),
      source("plate_press_blueprint", "source/poi/rusted_factory_complex", IndexSourceKind.STRUCTURE,
         "Rusted Factory Complex schematic", "Industrial factory schematic found through POI and mission progression."),
      source("circuit_fabricator_blueprint", "source/poi/rusted_factory_complex", IndexSourceKind.STRUCTURE,
         "Rusted Factory Complex schematic", "Industrial factory schematic found through POI and mission progression."),
      source("recipe_matrix_blueprint", "source/poi/nexus_heat_exchanger_ruins", IndexSourceKind.STRUCTURE,
         "Nexus Heat Exchanger schematic", "Late factory schematic for Recipe Matrix Core progression."),
      source("nexus_furnace_array_blueprint", "source/poi/nexus_heat_exchanger_ruins", IndexSourceKind.STRUCTURE,
         "Nexus Heat Exchanger schematic", "Late factory schematic for Nexus Furnace Array progression.")
   );

   public static void register() {
      EchoCoreServices.registerIndexContentProvider(INSTANCE);
   }

   @Override
   public Identifier id() {
      return EchoIndustrialNexus.id("provider/index_recipes");
   }

   @Override
   public IndexContentSnapshot snapshot(IndexBuildContext context) {
      if (!EchoCoreServices.itemStackComponentsBound()) {
         return IndexContentSnapshot.empty(id());
      }
      Player player = context == null ? null : context.player();
      List<IndexRecipeView> recipes = recipes(player);
      return new IndexContentSnapshot(id(), List.of(), List.of(), recipeCategories(player), recipes,
         machineLayouts(recipes), sourceFacts(player), List.of(), List.of());
   }

   public List<IndexRecipeCategory> recipeCategories(Player player) {
      List<IndexRecipeCategory> categories = new ArrayList<>();
      for (IndustrialMachineBlock.MachineKind kind : IndustrialMachineBlock.MachineKind.values()) {
         if (kind.recipeDriven()) {
            categories.add(new IndexRecipeCategory(categoryId(kind), kind.displayName(), machineStack(kind), ACCENT, 400 + kind.ordinal()));
         }
      }
      return categories;
   }

   public List<IndexRecipeView> recipes(Player player) {
      if (player == null || player.level() == null) {
         return List.of();
      }
      List<IndexRecipeView> views = new ArrayList<>();
      for (RecipeHolder<?> holder : recipeHolders(player)) {
         if (holder.value() instanceof IndustrialProcessingRecipe recipe) {
            views.add(view(holder, recipe));
         }
      }
      return List.copyOf(views);
   }

   public List<IndexSourceFact> sourceFacts(Player player) {
      List<IndexSourceFact> facts = new ArrayList<>();
      for (RecipeHolder<?> holder : recipeHolders(player)) {
         if (holder.value() instanceof IndustrialProcessingRecipe recipe) {
            addRecipeSource(facts, holder, recipe);
         }
      }
      for (MultiblockAutomationRecipe recipe : AutomationRecipeRegistry.all()) {
         if (recipe.id() != null && EchoIndustrialNexus.MODID.equals(recipe.id().getNamespace())) {
            addAutomationSources(facts, recipe);
         }
      }
      STATIC_SOURCES.stream().map(IndustrialIndexProvider::staticFact).forEach(facts::add);
      return List.copyOf(facts);
   }

   private static IndexRecipeView view(RecipeHolder<?> holder, IndustrialProcessingRecipe recipe) {
      IndustrialMachineBlock.MachineKind kind = recipe.machine();
      ItemStack machine = machineStack(kind);
      List<IndexRecipeSlot> slots = new ArrayList<>();
      List<ItemStack> inputs = stacks(recipe.ingredient(), 1);
      if (!inputs.isEmpty()) {
         slots.add(IndexRecipeSlot.inputs(inputs));
      }
      if (recipe.inputFluidAmount() > 0) {
         slots.add(new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(),
            "Input fluid: " + fluidLabel(recipe.inputFluidId()) + " x" + recipe.inputFluidAmount() + " mB"));
      }
      ItemStack catalyst = recipe.catalyst();
      if (!catalyst.isEmpty()) {
         slots.add(IndexRecipeSlot.catalyst(catalyst, "Catalyst"));
      }
      if (!machine.isEmpty()) {
         slots.add(IndexRecipeSlot.machine(machine));
      }
      ItemStack output = recipe.result();
      if (!output.isEmpty()) {
         slots.add(IndexRecipeSlot.output(output));
      }
      ItemStack byproduct = recipe.byproduct();
      if (!byproduct.isEmpty()) {
         slots.add(new IndexRecipeSlot(IndexSlotRole.OUTPUT, List.of(byproduct), "Byproduct"));
      }
      if (recipe.outputFluidAmount() > 0) {
         slots.add(new IndexRecipeSlot(IndexSlotRole.OUTPUT, List.of(),
            "Output fluid: " + fluidLabel(recipe.outputFluidId()) + " x" + recipe.outputFluidAmount() + " mB"));
      }

      List<String> notes = new ArrayList<>();
      if (!byproduct.isEmpty()) {
         notes.add(recipe.byproductChance() + "% chance for byproduct output.");
      }
      if (recipe.fluxCost() > 0) {
         notes.add("Consumes " + recipe.fluxCost() + " Thermal Flux.");
      }
      if (recipe.fluxGeneration() > 0) {
         notes.add("Generates " + recipe.fluxGeneration() + " Thermal Flux.");
      }
      if (recipe.heat() > 0) {
         notes.add("Adds " + recipe.heat() + " heat.");
      }
      if (recipe.inputFluidAmount() > 0) {
         notes.add("Requires " + fluidLabel(recipe.inputFluidId()) + " x" + recipe.inputFluidAmount() + " mB.");
      }
      if (recipe.outputFluidAmount() > 0) {
         notes.add("Produces " + fluidLabel(recipe.outputFluidId()) + " x" + recipe.outputFluidAmount() + " mB.");
      }

      String title = kind.displayName() + ": " + (output.isEmpty() ? "Fluid Process" : output.getHoverName().getString());
      return new IndexRecipeView(
         holder.id().identifier(),
         categoryId(kind),
         title,
         machine,
         slots,
         notes,
         recipe.duration(),
         false,
         EchoIndustrialNexus.MODID);
   }

   private static List<IndexMachineLayout> machineLayouts(List<IndexRecipeView> recipes) {
      List<IndexMachineLayout> layouts = new ArrayList<>();
      for (IndexRecipeView recipe : recipes) {
         List<IndexMachineLayoutSlot> slots = new ArrayList<>();
         addLayoutSlot(slots, recipe, IndexSlotRole.INPUT, "", IndustrialMachineMenu.INPUT_X, 92);
         addLayoutSlot(slots, recipe, IndexSlotRole.CATALYST, "", IndustrialMachineMenu.CATALYST_X, 92);
         addLayoutSlot(slots, recipe, IndexSlotRole.MACHINE, "", 166, 150);
         addLayoutSlot(slots, recipe, IndexSlotRole.OUTPUT, "", IndustrialMachineMenu.OUTPUT_X, 92);
         addLayoutSlot(slots, recipe, IndexSlotRole.OUTPUT, "byproduct", IndustrialMachineMenu.BYPRODUCT_X, 92);
         layouts.add(new IndexMachineLayout(
            recipe.id(),
            "industrial_machine",
            recipe.machine().isEmpty() ? recipe.title() : recipe.machine().getHoverName().getString(),
            IndustrialMachineMenu.GUI_WIDTH,
            180,
            true,
            slots,
            List.of(
               new IndexMachineLayoutGauge("progress", "Progress", 118, 88, 112, 8, 0xFF64D97B),
               new IndexMachineLayoutGauge("energy", "Thermal Flux", 118, 112, 112, 8, 0xFF66E8FF),
               new IndexMachineLayoutGauge("heat", "Heat", 118, 136, 112, 8, 0xFFFF9F3D),
               new IndexMachineLayoutGauge("fluid", "Input Tank", 244, 150, 72, 7, 0xFF4FB8FF),
               new IndexMachineLayoutGauge("fluid", "Output Tank", 244, 165, 72, 7, 0xFF8AF6B6))));
      }
      return List.copyOf(layouts);
   }

   private static void addLayoutSlot(List<IndexMachineLayoutSlot> slots, IndexRecipeView recipe, IndexSlotRole role,
         String labelContains, int x, int y) {
      int index = slotIndex(recipe, role, labelContains);
      if (index >= 0) {
         IndexRecipeSlot slot = recipe.slots().get(index);
         slots.add(new IndexMachineLayoutSlot(index, role, slot.label(), x, y, 18, false));
      }
   }

   private static int slotIndex(IndexRecipeView recipe, IndexSlotRole role, String labelContains) {
      String needle = labelContains == null ? "" : labelContains.toLowerCase(Locale.ROOT);
      for (int i = 0; i < recipe.slots().size(); i++) {
         IndexRecipeSlot slot = recipe.slots().get(i);
         if (slot.role() != role) {
            continue;
         }
         if (needle.isBlank() || slot.label().toLowerCase(Locale.ROOT).contains(needle)) {
            return i;
         }
      }
      return -1;
   }

   private static List<RecipeHolder<?>> recipeHolders(Player player) {
      if (player == null || player.level() == null) {
         return List.of();
      }
      MinecraftServer server = player.level().getServer();
      if (server == null) {
         return List.of();
      }
      try {
         List<RecipeHolder<?>> holders = new ArrayList<>();
         for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            if (holder.value().getType() == ModRecipes.INDUSTRIAL_PROCESSING_TYPE.get()) {
               holders.add(holder);
            }
         }
         return holders;
      } catch (RuntimeException exception) {
         EchoIndustrialNexus.LOGGER.debug("ECHO: Index could not enumerate Industrial recipes.", exception);
      }
      return List.of();
   }

   private static void addRecipeSource(List<IndexSourceFact> facts, RecipeHolder<?> holder, IndustrialProcessingRecipe recipe) {
      ItemStack result = recipe.result();
      if (!result.isEmpty()) {
         facts.add(fact(new IndustrialProcessSource(
            itemId(result),
            holder.id().identifier(),
            IndexSourceKind.MACHINE.label(),
            "Made in " + recipe.machine().displayName(),
            recipeNotes(recipe),
            machineStack(recipe.machine())
         )));
      }
      ItemStack byproduct = recipe.byproduct();
      if (!byproduct.isEmpty()) {
         facts.add(fact(new IndustrialProcessSource(
            itemId(byproduct),
            holder.id().identifier(),
            IndexSourceKind.MACHINE.label(),
            "Byproduct from " + recipe.machine().displayName(),
            List.of(recipe.byproductChance() + "% chance while running " + recipe.machine().displayName() + "."),
            byproduct
         )));
      }
   }

   private static void addAutomationSources(List<IndexSourceFact> facts, MultiblockAutomationRecipe recipe) {
      for (AutomationOutput output : recipe.outputs()) {
         ItemStack stack = output.stack();
         if (!stack.isEmpty()) {
            List<String> notes = new ArrayList<>();
            notes.add("Factory task: " + recipe.displayName() + ".");
            notes.add("Inputs: " + recipe.inputSummary() + ".");
            notes.add("Outputs: " + recipe.outputSummary() + ".");
            notes.add("Duration: " + recipe.durationTicks() + " ticks.");
            facts.add(fact(new IndustrialProcessSource(
               output.itemId(),
               recipe.id(),
               IndexSourceKind.MACHINE.label(),
               "Factory task output: " + recipe.displayName(),
               notes,
               stack
            )));
         }
      }
   }

   private static List<String> recipeNotes(IndustrialProcessingRecipe recipe) {
      List<String> notes = new ArrayList<>();
      notes.add("Industrial processing recipe for " + recipe.machine().displayName() + ".");
      if (recipe.fluxCost() > 0) {
         notes.add("Consumes " + recipe.fluxCost() + " Thermal Flux.");
      }
      if (recipe.fluxGeneration() > 0) {
         notes.add("Generates " + recipe.fluxGeneration() + " Thermal Flux.");
      }
      if (recipe.heat() > 0) {
         notes.add("Adds " + recipe.heat() + " heat.");
      }
      if (recipe.inputFluidAmount() > 0) {
         notes.add("Requires " + fluidLabel(recipe.inputFluidId()) + " x" + recipe.inputFluidAmount() + " mB.");
      }
      if (recipe.outputFluidAmount() > 0) {
         notes.add("Produces " + fluidLabel(recipe.outputFluidId()) + " x" + recipe.outputFluidAmount() + " mB.");
      }
      return List.copyOf(notes);
   }

   private static IndexSourceFact fact(IndustrialProcessSource source) {
      return new IndexSourceFact(
         source.itemId(),
         source.sourceId(),
         sourceKind(source.sourceKind()),
         source.title(),
         source.notes(),
         source.icon(),
         EchoIndustrialNexus.MODID
      );
   }

   private static IndexSourceKind sourceKind(String label) {
      for (IndexSourceKind kind : IndexSourceKind.values()) {
         if (kind.label().equals(label) || kind.name().equalsIgnoreCase(label)) {
            return kind;
         }
      }
      return IndexSourceKind.SOURCE_CARD;
   }

   private static IndexSourceFact staticFact(StaticSource source) {
      return new IndexSourceFact(
         source.itemId(),
         source.sourceId(),
         source.kind(),
         source.title(),
         source.notes(),
         stack(source.itemId()),
         EchoIndustrialNexus.MODID
      );
   }

   private static StaticSource source(String itemPath, String sourcePath, IndexSourceKind kind, String title, String note) {
      return new StaticSource(EchoIndustrialNexus.id(itemPath), EchoIndustrialNexus.id(sourcePath), kind, title, List.of(note));
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

   private static Identifier categoryId(IndustrialMachineBlock.MachineKind kind) {
      return EchoIndustrialNexus.id("recipe/" + kind.getSerializedName());
   }

   private static ItemStack machineStack(IndustrialMachineBlock.MachineKind kind) {
      if (!EchoCoreServices.itemStackComponentsBound()) {
         return ItemStack.EMPTY;
      }
      Optional<Block> block = ModBlocks.ALL_BLOCKS.stream()
         .map(holder -> holder.get())
         .filter(candidate -> candidate instanceof IndustrialMachineBlock machine && machine.kind() == kind)
         .findFirst();
      return block.map(value -> new ItemStack(value.asItem())).orElse(ItemStack.EMPTY);
   }

   private static ItemStack stack(Identifier itemId) {
      if (itemId == null || !EchoCoreServices.itemStackComponentsBound()) {
         return ItemStack.EMPTY;
      }
      return BuiltInRegistries.ITEM.getOptional(itemId)
         .map(ItemStack::new)
         .orElse(ItemStack.EMPTY);
   }

   private static String fluidLabel(int id) {
      return id <= 0 ? "Unknown fluid" : ModFluids.displayNameFor(id);
   }

   @SuppressWarnings("unused")
   private static Identifier itemId(ItemStack stack) {
      Identifier id = stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
      return id == null ? Identifier.withDefaultNamespace("air") : id;
   }

   private record StaticSource(
      Identifier itemId,
      Identifier sourceId,
      IndexSourceKind kind,
      String title,
      List<String> notes
   ) {
      private StaticSource {
         if (itemId == null) {
            throw new IllegalArgumentException("Industrial static source item id is required.");
         }
         sourceId = sourceId == null ? itemId : sourceId;
         kind = kind == null ? IndexSourceKind.SOURCE_CARD : kind;
         title = title == null || title.isBlank() ? kind.label() : title.strip();
         notes = notes == null ? List.of() : List.copyOf(notes.stream()
            .filter(note -> note != null && !note.isBlank())
            .map(String::strip)
            .toList());
      }
   }
}
