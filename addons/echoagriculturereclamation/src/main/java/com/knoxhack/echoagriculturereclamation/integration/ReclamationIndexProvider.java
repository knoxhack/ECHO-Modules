package com.knoxhack.echoagriculturereclamation.integration;

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
import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoagriculturereclamation.content.CropCategory;
import com.knoxhack.echoagriculturereclamation.content.CropSpec;
import com.knoxhack.echoagriculturereclamation.content.ReclamationContent;
import com.knoxhack.echoagriculturereclamation.content.ReclamationCropRule;
import com.knoxhack.echoagriculturereclamation.content.ReclamationMachineRules;
import com.knoxhack.echoagriculturereclamation.content.ReclamationProcessDefinition;
import com.knoxhack.echoagriculturereclamation.content.ReclamationSoilRule;
import com.knoxhack.echoagriculturereclamation.content.SoilState;
import com.knoxhack.echoagriculturereclamation.registry.ModBlocks;
import com.knoxhack.echoagriculturereclamation.registry.ModItems;
import com.knoxhack.echoagriculturereclamation.menu.ReclamationMachineMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public enum ReclamationIndexProvider implements IIndexContentProvider {
   INSTANCE;

   private static final Identifier CATEGORY_CROPS = id("recipe/agriculture_reclamation");
   private static final Identifier CATEGORY_SOIL = id("recipe/soil_restoration");
   private static final Identifier CATEGORY_MACHINES = id("recipe/reclamation_machines");

   @Override
   public Identifier id() {
      return id("provider/index_recipes");
   }

   @Override
   public IndexContentSnapshot snapshot(IndexBuildContext context) {
      Player player = context == null ? null : context.player();
      List<IndexRecipeView> recipes = recipes(player);
      return new IndexContentSnapshot(id(), List.of(), List.of(), recipeCategories(player), recipes,
         machineLayouts(recipes), List.of(), List.of(), List.of());
   }

   public List<IndexRecipeCategory> recipeCategories(Player player) {
      return List.of(
         new IndexRecipeCategory(CATEGORY_CROPS, "Agriculture Reclamation",
            new ItemStack(ModItems.RECOVERED_SEED_CAPSULE.get()), 0xFF92F7A6, 540),
         new IndexRecipeCategory(CATEGORY_SOIL, "Soil Restoration",
            new ItemStack(ModBlocks.SOIL_PURIFIER.asItem()), 0xFF8AF6B6, 545),
         new IndexRecipeCategory(CATEGORY_MACHINES, "Reclamation Machines",
            new ItemStack(ModBlocks.GREENHOUSE_CONTROLLER.asItem()), 0xFF66E8FF, 550)
      );
   }

   public List<IndexRecipeView> recipes(Player player) {
      List<IndexRecipeView> views = new ArrayList<>();
      for (CropSpec spec : CropSpec.sorted()) {
         views.add(cropView(spec));
      }
      for (SoilState state : SoilState.values()) {
         views.add(soilView(state));
      }
      views.addAll(machineViews());
      ReclamationContent.processes().values().forEach(process -> views.add(processView(process)));
      return List.copyOf(views);
   }

   private static IndexRecipeView cropView(CropSpec spec) {
      ReclamationCropRule rule = ReclamationContent.crop(spec);
      ItemStack machine = new ItemStack(ModBlocks.HYDROPONIC_TRAY.asItem());
      List<IndexRecipeSlot> slots = new ArrayList<>();
      slots.add(new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(new ItemStack(ModItems.STABILIZED_SEED.get())),
         "Seed profile: " + spec.displayName()));
      slots.add(new IndexRecipeSlot(IndexSlotRole.CATALYST, List.of(), "Crop category: " + spec.category().displayName()));
      slots.add(IndexRecipeSlot.machine(machine));
      slots.add(IndexRecipeSlot.output(new ItemStack(ModItems.produceFor(spec).get(), Math.max(1, rule.baseYield()))));
      slots.add(new IndexRecipeSlot(IndexSlotRole.OUTPUT, List.of(), "Restoration weight: " + rule.restorationWeight()));
      return new IndexRecipeView(
         id("recipe/crop/" + spec.path()),
         CATEGORY_CROPS,
         spec.displayName() + " Cultivation",
         machine,
         slots,
         List.of(
            "Base growth chance: " + rule.baseGrowthChance() + "%",
            "Base yield: " + rule.baseYield(),
            "Supported by soils matching " + spec.category().displayName() + "; greenhouse bypass at "
               + rule.greenhouseBypassThreshold() + "% safety.",
            "Hydroponic yield bonus: +" + rule.hydroponicYieldBonus(),
            "Stable crop bonus: growth +" + rule.stableGrowthBonus() + ", yield +" + rule.stableYieldBonus()),
         ReclamationContent.machines().hydroponicGrowthTicks(),
         false,
         EchoAgricultureReclamation.MODID);
   }

   private static IndexRecipeView soilView(SoilState state) {
      ReclamationSoilRule rule = ReclamationContent.soil(state);
      SoilState next = state.purifiedStep();
      ItemStack machine = new ItemStack(ModBlocks.SOIL_PURIFIER.asItem());
      List<IndexRecipeSlot> slots = new ArrayList<>();
      slots.add(IndexRecipeSlot.input(new ItemStack(ModBlocks.blockFor(state))));
      slots.add(IndexRecipeSlot.catalyst(new ItemStack(ModItems.PURIFICATION_ENZYME.get()), "Purification enzyme"));
      slots.add(IndexRecipeSlot.catalyst(new ItemStack(ModItems.SOIL_NUTRIENT_MIX.get()), "Nutrient mix"));
      slots.add(IndexRecipeSlot.machine(machine));
      slots.add(IndexRecipeSlot.output(new ItemStack(ModBlocks.blockFor(next))));
      return new IndexRecipeView(
         id("recipe/soil/" + state.name().toLowerCase(Locale.ROOT)),
         CATEGORY_SOIL,
         state.displayName() + " Purification",
         machine,
         slots,
         List.of(
            "Safe soil: " + yesNo(rule.safe()),
            "Growth chance: " + rule.growthChance() + "%",
            "Restoration gain: " + rule.restorationGain(),
            "Native support: " + categoryList(rule.supportedCategories()),
            "Stable support: " + categoryList(rule.stabilizedSupportedCategories())
               + " at " + rule.stabilizedSupportMinStability() + "% stability"),
         80,
         false,
         EchoAgricultureReclamation.MODID);
   }

   private static List<IndexRecipeView> machineViews() {
      ReclamationMachineRules rules = ReclamationContent.machines();
      return List.of(
         machineView("hydroponic_tray", "Hydroponic Growth",
            new ItemStack(ModBlocks.HYDROPONIC_TRAY.asItem()),
            List.of(
               IndexRecipeSlot.input(new ItemStack(ModItems.STABILIZED_SEED.get())),
               IndexRecipeSlot.catalyst(new ItemStack(ModItems.SOIL_NUTRIENT_MIX.get()), "Nutrient charge"),
               new IndexRecipeSlot(IndexSlotRole.OUTPUT, List.of(), "Accelerated crop growth")),
            List.of("Growth cycle: " + rules.hydroponicGrowthTicks() + " ticks",
               "Nutrient capacity: " + rules.hydroponicNutrientCap(),
               "Nutrients per mix: " + rules.hydroponicNutrientPerMix())),
         machineView("gene_stabilizer", "Seed Stabilization",
            new ItemStack(ModBlocks.GENE_STABILIZER.asItem()),
            List.of(
               IndexRecipeSlot.input(new ItemStack(ModItems.CONTAMINATED_SEED.get())),
               IndexRecipeSlot.catalyst(new ItemStack(ModItems.BIO_GEL.get()), "Bio gel"),
               IndexRecipeSlot.catalyst(new ItemStack(ModItems.GENE_SAMPLE.get()), "Gene sample"),
               IndexRecipeSlot.output(new ItemStack(ModItems.STABILIZED_SEED.get()))),
            List.of("Converts contaminated crop routes into stable seed profiles.")),
         machineView("bio_reactor", "Bioreactor Synthesis",
            new ItemStack(ModBlocks.BIO_REACTOR.asItem()),
            List.of(
               new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(), "Crop matter or recovered seed biomass"),
               IndexRecipeSlot.output(new ItemStack(ModItems.BIO_GEL.get(), rules.bioReactorOrganicOutput())),
               IndexRecipeSlot.output(new ItemStack(ModItems.GENE_SAMPLE.get(), rules.bioReactorGeneSampleOutput()))),
            List.of("Processes agriculture biomass into gel and gene samples.")),
         machineView("compost_recycler", "Compost Recycling",
            new ItemStack(ModBlocks.COMPOST_RECYCLER.asItem()),
            List.of(
               new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(), "Crop matter or failed growth waste"),
               IndexRecipeSlot.output(new ItemStack(ModItems.SOIL_NUTRIENT_MIX.get(), rules.compostRecyclerOutput()))),
            List.of("Reclaims plant mass into nutrient mix.")),
         machineView("greenhouse_support", "Greenhouse Support",
            new ItemStack(ModBlocks.GREENHOUSE_CONTROLLER.asItem()),
            List.of(
               IndexRecipeSlot.catalyst(new ItemStack(ModBlocks.GREENHOUSE_GLASS.asItem()), "Greenhouse glass"),
               IndexRecipeSlot.catalyst(new ItemStack(ModBlocks.SPORE_FILTER.asItem()), "Spore filter"),
               IndexRecipeSlot.catalyst(new ItemStack(ModBlocks.POLLINATOR_DRONE_DOCK.asItem()), "Pollinator dock"),
               new IndexRecipeSlot(IndexSlotRole.OUTPUT, List.of(), "Safe greenhouse envelope")),
            List.of("Safe threshold: " + ReclamationContent.progression().greenhouseSafeThreshold() + "%",
               "Scan range: " + rules.greenhouseHorizontalRange() + " horizontal, "
                  + rules.greenhouseDownRange() + " down, " + rules.greenhouseUpRange() + " up",
               "Pollinator service radius: " + rules.pollinatorDroneServiceRadius()
                  + ", growth bonus +" + rules.pollinatorDroneGrowthBonus()))
      );
   }

   private static IndexRecipeView machineView(String path, String title, ItemStack machine,
         List<IndexRecipeSlot> slots, List<String> notes) {
      List<IndexRecipeSlot> allSlots = new ArrayList<>(slots);
      allSlots.add(IndexRecipeSlot.machine(machine));
      return new IndexRecipeView(
         id("recipe/machine/" + path),
         CATEGORY_MACHINES,
         title,
         machine,
         allSlots,
         notes,
         0,
         false,
         EchoAgricultureReclamation.MODID);
   }

   private static IndexRecipeView processView(ReclamationProcessDefinition process) {
      List<IndexRecipeSlot> slots = new ArrayList<>();
      process.inputs().forEach(input -> slots.add(new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(), input)));
      process.catalysts().forEach(catalyst -> slots.add(new IndexRecipeSlot(IndexSlotRole.CATALYST, List.of(), catalyst)));
      process.outputs().forEach(output -> slots.add(new IndexRecipeSlot(IndexSlotRole.OUTPUT, List.of(), output)));
      ItemStack machine = machineStack(process.machine());
      slots.add(IndexRecipeSlot.machine(machine));
      List<String> notes = new ArrayList<>(process.notes());
      if (process.powerCost() > 0) {
         notes.add("Optional powered throughput cost: " + process.powerCost() + " EP.");
      }
      return new IndexRecipeView(
         id("recipe/process/" + process.id()),
         CATEGORY_MACHINES,
         process.title(),
         machine,
         slots,
         notes,
         process.ticks(),
         false,
         EchoAgricultureReclamation.MODID);
   }

   private static List<IndexMachineLayout> machineLayouts(List<IndexRecipeView> recipes) {
      List<IndexMachineLayout> layouts = new ArrayList<>();
      for (IndexRecipeView recipe : recipes) {
         List<IndexMachineLayoutSlot> slots = new ArrayList<>();
         addLayoutSlot(slots, recipe, IndexSlotRole.INPUT, 0, 42, 74, false);
         addLayoutSlot(slots, recipe, IndexSlotRole.CATALYST, 0, 78, 74, false);
         addLayoutSlot(slots, recipe, IndexSlotRole.MACHINE, 0, 146, 74, true);
         addLayoutSlot(slots, recipe, IndexSlotRole.OUTPUT, 0, 206, 74, false);
         addAuxLayoutSlot(slots, recipe);
         layouts.add(new IndexMachineLayout(
            recipe.id(),
            "reclamation_machine",
            recipe.machine().isEmpty() ? recipe.title() : recipe.machine().getHoverName().getString(),
            ReclamationMachineMenu.GUI_WIDTH,
            112,
            true,
            slots,
            List.of(new IndexMachineLayoutGauge("progress", "Progress", 112, 77, 76, 8, 0xFF92F7A6))));
      }
      return List.copyOf(layouts);
   }

   private static void addAuxLayoutSlot(List<IndexMachineLayoutSlot> slots, IndexRecipeView recipe) {
      int catalyst = slotIndex(recipe, IndexSlotRole.CATALYST, 1);
      if (catalyst >= 0) {
         IndexRecipeSlot slot = recipe.slots().get(catalyst);
         slots.add(new IndexMachineLayoutSlot(catalyst, IndexSlotRole.CATALYST, slot.label(), 242, 74, 18, true));
         return;
      }
      int output = slotIndex(recipe, IndexSlotRole.OUTPUT, 1);
      if (output >= 0) {
         IndexRecipeSlot slot = recipe.slots().get(output);
         slots.add(new IndexMachineLayoutSlot(output, IndexSlotRole.OUTPUT, slot.label(), 242, 74, 18, true));
      }
   }

   private static void addLayoutSlot(List<IndexMachineLayoutSlot> slots, IndexRecipeView recipe, IndexSlotRole role,
         int occurrence, int x, int y, boolean optional) {
      int index = slotIndex(recipe, role, occurrence);
      if (index >= 0) {
         IndexRecipeSlot slot = recipe.slots().get(index);
         slots.add(new IndexMachineLayoutSlot(index, role, slot.label(), x, y, 18, optional));
      }
   }

   private static int slotIndex(IndexRecipeView recipe, IndexSlotRole role, int occurrence) {
      int seen = 0;
      for (int i = 0; i < recipe.slots().size(); i++) {
         if (recipe.slots().get(i).role() != role) {
            continue;
         }
         if (seen == occurrence) {
            return i;
         }
         seen++;
      }
      return -1;
   }

   private static ItemStack machineStack(String machine) {
      return switch (machine) {
         case "seed_vault_terminal" -> new ItemStack(ModBlocks.SEED_VAULT_TERMINAL.asItem());
         case "soil_purifier" -> new ItemStack(ModBlocks.SOIL_PURIFIER.asItem());
         case "gene_stabilizer" -> new ItemStack(ModBlocks.GENE_STABILIZER.asItem());
         case "bio_reactor" -> new ItemStack(ModBlocks.BIO_REACTOR.asItem());
         case "compost_recycler" -> new ItemStack(ModBlocks.COMPOST_RECYCLER.asItem());
         case "greenhouse_controller" -> new ItemStack(ModBlocks.GREENHOUSE_CONTROLLER.asItem());
         case "pollinator_drone_dock" -> new ItemStack(ModBlocks.POLLINATOR_DRONE_DOCK.asItem());
         case "ecology_scanner" -> new ItemStack(ModBlocks.ECOLOGY_SCANNER.asItem());
         default -> new ItemStack(ModBlocks.SEED_VAULT_TERMINAL.asItem());
      };
   }

   private static String categoryList(Set<CropCategory> categories) {
      if (categories == null || categories.isEmpty()) {
         return "none";
      }
      return categories.stream()
         .map(CropCategory::displayName)
         .sorted(String::compareTo)
         .reduce((left, right) -> left + ", " + right)
         .orElse("none");
   }

   private static String yesNo(boolean value) {
      return value ? "yes" : "no";
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoAgricultureReclamation.MODID, sanitize(path));
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
