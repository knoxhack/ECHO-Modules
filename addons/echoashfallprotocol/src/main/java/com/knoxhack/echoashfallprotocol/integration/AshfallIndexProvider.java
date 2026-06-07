package com.knoxhack.echoashfallprotocol.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.index.IIndexContentProvider;
import com.knoxhack.echocore.api.index.IIndexRegistry;
import com.knoxhack.echocore.api.index.IndexBuildContext;
import com.knoxhack.echocore.api.index.IndexCategory;
import com.knoxhack.echocore.api.index.IndexContentBuilder;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.index.IndexEntry;
import com.knoxhack.echocore.api.index.IndexEntryState;
import com.knoxhack.echocore.api.index.IndexMachineLayoutTemplates;
import com.knoxhack.echocore.api.index.IndexRecipeCategory;
import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexSlotRole;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.block.entity.CrystallineSynthesizerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.HandRecyclerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.IsotopeRefinerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.OreGrinderBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.RadiationCleanserBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.WaterPurifierBlockEntity;
import com.knoxhack.echoashfallprotocol.recipe.ScrapPressRecipe;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum AshfallIndexProvider implements IIndexContentProvider {
    INSTANCE;

    private static final Identifier CATEGORY_MACHINES = id("machines");
    private static final Identifier SCRAP_PRESS = id("recipe/scrap_press");
    private static final Identifier SUBSTRATE_GRINDER = id("recipe/substrate_grinder");
    private static final Identifier HAND_RECYCLER = id("recipe/hand_recycler");
    private static final Identifier ISOTOPE_REFINER = id("recipe/isotope_refiner");
    private static final Identifier WATER_PURIFIER = id("recipe/water_purifier");
    private static final Identifier FILTER_WORKBENCH = id("recipe/filter_workbench");
    private static final Identifier RADIATION_CLEANSER = id("recipe/radiation_cleanser");
    private static final Identifier CRYSTALLINE_SYNTHESIZER = id("recipe/crystalline_synthesizer");

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
        builder.addMachineLayouts(IndexMachineLayoutTemplates.representativeAll(recipes, "ashfall_station"));
        return builder.snapshot();
    }

    public void register(IIndexRegistry registry) {
        registry.registerCategory(new IndexCategory(
                CATEGORY_MACHINES,
                "Ashfall Machines",
                "Core survival processing machines from Ashfall Protocol.",
                new ItemStack(ModBlocks.SCRAP_PRESS_ITEM.get()),
                120,
                EchoAshfallProtocol.MODID));
        registry.registerEntry(entry(
                "scrap_press",
                "Scrap Press",
                "Compresses raw scrap into stable crafting components.",
                "The Scrap Press turns salvage piles into usable machine parts without depending on vanilla crafting grids.",
                new ItemStack(ModBlocks.SCRAP_PRESS_ITEM.get()),
                List.of(itemId(ModBlocks.SCRAP_PRESS_ITEM.get().asItem())),
                10));
        registry.registerEntry(entry(
                "substrate_grinder",
                "Substrate Grinder",
                "Refines stone, rubble, and trace fragments into Ashfall resource streams.",
                "The Substrate Grinder exposes biome substrate routes, byproducts, power costs, and handling hints to ECHO: Index.",
                new ItemStack(ModBlocks.ORE_GRINDER_ITEM.get()),
                List.of(itemId(ModBlocks.ORE_GRINDER_ITEM.get().asItem())),
                20));
        registry.registerEntry(entry(
                "hand_recycler",
                "Hand Recycler",
                "Converts scrap into useful materials via manual processing.",
                "The Hand Recycler breaks down salvaged scrap into machine casings, circuit boards, energy cells, and filtration membranes.",
                new ItemStack(ModBlocks.HAND_RECYCLER_ITEM.get()),
                List.of(itemId(ModBlocks.HAND_RECYCLER_ITEM.get().asItem())),
                30));
        registry.registerEntry(entry(
                "isotope_refiner",
                "Isotope Refiner",
                "Tier 2 extraction that converts ingots into rare resources with risk of contamination.",
                "The Isotope Refiner uses crystal dust catalysts to transmute iron, copper, and coal into gold, redstone, and lapis with a chance of contamination.",
                new ItemStack(ModBlocks.ISOTOPE_REFINER_ITEM.get()),
                List.of(itemId(ModBlocks.ISOTOPE_REFINER_ITEM.get().asItem())),
                40));
        registry.registerEntry(entry(
                "water_purifier",
                "Water Purifier",
                "Converts dirty water and contaminated resources into clean variants.",
                "The Water Purifier uses filter cartridges to cleanse dirty water bottles and decontaminate irradiated iron, gold, redstone, and lapis.",
                new ItemStack(ModBlocks.WATER_PURIFIER_ITEM.get()),
                List.of(itemId(ModBlocks.WATER_PURIFIER_ITEM.get().asItem())),
                50));
        registry.registerEntry(entry(
                "filter_workbench",
                "Filter Workbench",
                "Crafts and upgrades filter cartridges for purification systems.",
                "The Filter Workbench assembles basic, advanced, and elite filter cartridges from scrap plastic, membranes, circuit boards, and machine casings.",
                new ItemStack(ModBlocks.FILTER_WORKBENCH_ITEM.get()),
                List.of(itemId(ModBlocks.FILTER_WORKBENCH_ITEM.get().asItem())),
                60));
        registry.registerEntry(entry(
                "radiation_cleanser",
                "Radiation Cleanser",
                "Removes contamination from items using advanced filters.",
                "The Radiation Cleanser stabilizes contaminated iron, gold, redstone, and lapis back into their clean forms. Requires an advanced filter cartridge.",
                new ItemStack(ModBlocks.RADIATION_CLEANSER_ITEM.get()),
                List.of(itemId(ModBlocks.RADIATION_CLEANSER_ITEM.get().asItem())),
                70));
        registry.registerEntry(entry(
                "crystalline_synthesizer",
                "Crystalline Synthesizer",
                "Tier 3 extraction that synthesizes gems and netherite scrap.",
                "The Crystalline Synthesizer fuses gem fragments, dense alloy, and energy cells into diamonds, emeralds, or netherite scrap.",
                new ItemStack(ModBlocks.CRYSTALLINE_SYNTHESIZER_ITEM.get()),
                List.of(itemId(ModBlocks.CRYSTALLINE_SYNTHESIZER_ITEM.get().asItem())),
                80));
    }

    public List<IndexRecipeCategory> recipeCategories(Player player) {
        return List.of(
                new IndexRecipeCategory(SCRAP_PRESS, "Ashfall Scrap Press",
                        new ItemStack(ModBlocks.SCRAP_PRESS_ITEM.get()), 0xFF66E8FF, 220),
                new IndexRecipeCategory(SUBSTRATE_GRINDER, "Ashfall Substrate Grinder",
                        new ItemStack(ModBlocks.ORE_GRINDER_ITEM.get()), 0xFFFFD166, 230),
                new IndexRecipeCategory(HAND_RECYCLER, "Ashfall Hand Recycler",
                        new ItemStack(ModBlocks.HAND_RECYCLER_ITEM.get()), 0xFF7DDF8A, 240),
                new IndexRecipeCategory(ISOTOPE_REFINER, "Ashfall Isotope Refiner",
                        new ItemStack(ModBlocks.ISOTOPE_REFINER_ITEM.get()), 0xFFE09CFF, 250),
                new IndexRecipeCategory(WATER_PURIFIER, "Ashfall Water Purifier",
                        new ItemStack(ModBlocks.WATER_PURIFIER_ITEM.get()), 0xFF5BC0EB, 260),
                new IndexRecipeCategory(FILTER_WORKBENCH, "Ashfall Filter Workbench",
                        new ItemStack(ModBlocks.FILTER_WORKBENCH_ITEM.get()), 0xFFFFB86B, 270),
                new IndexRecipeCategory(RADIATION_CLEANSER, "Ashfall Radiation Cleanser",
                        new ItemStack(ModBlocks.RADIATION_CLEANSER_ITEM.get()), 0xFFA8F7C5, 280),
                new IndexRecipeCategory(CRYSTALLINE_SYNTHESIZER, "Ashfall Crystalline Synthesizer",
                        new ItemStack(ModBlocks.CRYSTALLINE_SYNTHESIZER_ITEM.get()), 0xFFD6A6FF, 290));
    }

    public List<IndexRecipeView> recipes(Player player) {
        List<IndexRecipeView> views = new ArrayList<>();
        addScrapPressRecipes(views);
        addSubstrateGrinderRecipes(views);
        addHandRecyclerRecipes(views);
        addIsotopeRefinerRecipes(views);
        addWaterPurifierRecipes(views);
        addFilterWorkbenchRecipes(views);
        addRadiationCleanserRecipes(views);
        addCrystallineSynthesizerRecipes(views);
        return views;
    }

    private static void addScrapPressRecipes(List<IndexRecipeView> views) {
        for (ScrapPressRecipe recipe : ScrapPressRecipe.getAllRecipes()) {
            ItemStack input = recipe.createInputStack();
            ItemStack output = recipe.createOutputStack();
            views.add(new IndexRecipeView(
                    id("scrap_press/" + path(input.getItem()) + "_to_" + path(output.getItem())),
                    SCRAP_PRESS,
                    output.getHoverName().getString(),
                    new ItemStack(ModBlocks.SCRAP_PRESS_ITEM.get()),
                    List.of(
                            IndexRecipeSlot.input(input),
                            IndexRecipeSlot.machine(new ItemStack(ModBlocks.SCRAP_PRESS_ITEM.get())),
                            IndexRecipeSlot.output(output)),
                    List.of("Pressure-forming salvage route.", "Time: " + recipe.processingTime() + " ticks"),
                    recipe.processingTime(),
                    false,
                    EchoAshfallProtocol.MODID));
        }
    }

    private static void addSubstrateGrinderRecipes(List<IndexRecipeView> views) {
        for (OreGrinderBlockEntity.GrinderRecipe recipe : OreGrinderBlockEntity.getSubstrateRecipes().values()) {
            List<IndexRecipeSlot> slots = new ArrayList<>();
            slots.add(IndexRecipeSlot.input(new ItemStack(recipe.input(), recipe.inputCount())));
            slots.add(IndexRecipeSlot.machine(new ItemStack(ModBlocks.ORE_GRINDER_ITEM.get())));
            slots.add(IndexRecipeSlot.output(new ItemStack(recipe.output(), recipe.outputCount())));
            if (recipe.byproduct() != null) {
                slots.add(new IndexRecipeSlot(IndexSlotRole.OUTPUT,
                        List.of(new ItemStack(recipe.byproduct(), recipe.byproductCount())),
                        "Byproduct " + Math.round(recipe.byproductChance() * 100.0F) + "%"));
            }
            views.add(new IndexRecipeView(
                    id("substrate_grinder/" + path(recipe.input()) + "_to_" + path(recipe.output())),
                    SUBSTRATE_GRINDER,
                    new ItemStack(recipe.output()).getHoverName().getString(),
                    new ItemStack(ModBlocks.ORE_GRINDER_ITEM.get()),
                    slots,
                    List.of(recipe.categoryLabel(), recipe.handlingHint(),
                            "Power: " + recipe.powerPerOperation() + " FE/op"),
                    recipe.processTime(),
                    false,
                    EchoAshfallProtocol.MODID));
        }
    }

    private static void addHandRecyclerRecipes(List<IndexRecipeView> views) {
        ItemStack machine = new ItemStack(ModBlocks.HAND_RECYCLER_ITEM.get());
        for (java.util.Map.Entry<Item, Item> entry : HandRecyclerBlockEntity.getRecipes().entrySet()) {
            ItemStack input = new ItemStack(entry.getKey());
            ItemStack output = new ItemStack(entry.getValue());
            views.add(new IndexRecipeView(
                    id("hand_recycler/" + path(entry.getKey()) + "_to_" + path(entry.getValue())),
                    HAND_RECYCLER,
                    output.getHoverName().getString(),
                    machine,
                    List.of(
                            IndexRecipeSlot.input(input),
                            IndexRecipeSlot.machine(machine),
                            IndexRecipeSlot.output(output)),
                    List.of("Manual scrap breakdown route.", "Time: 100 ticks", "Power: 1 FE/tick"),
                    100,
                    false,
                    EchoAshfallProtocol.MODID));
        }
    }

    private static void addIsotopeRefinerRecipes(List<IndexRecipeView> views) {
        ItemStack machine = new ItemStack(ModBlocks.ISOTOPE_REFINER_ITEM.get());
        ItemStack catalyst = new ItemStack(ModItems.CRYSTAL_DUST.get());
        for (java.util.Map.Entry<Item, Item[]> entry : IsotopeRefinerBlockEntity.getRefinerRecipes().entrySet()) {
            ItemStack input = new ItemStack(entry.getKey(), 2);
            Item cleanOutput = entry.getValue()[0];
            Item contaminatedOutput = entry.getValue()[1];
            List<IndexRecipeSlot> slots = new ArrayList<>();
            slots.add(IndexRecipeSlot.input(input));
            slots.add(new IndexRecipeSlot(IndexSlotRole.CATALYST, List.of(catalyst), "Catalyst"));
            slots.add(IndexRecipeSlot.machine(machine));
            slots.add(IndexRecipeSlot.output(new ItemStack(cleanOutput)));
            slots.add(new IndexRecipeSlot(IndexSlotRole.OUTPUT,
                    List.of(new ItemStack(contaminatedOutput)),
                    "Contaminated 20%"));
            views.add(new IndexRecipeView(
                    id("isotope_refiner/" + path(entry.getKey()) + "_to_" + path(cleanOutput)),
                    ISOTOPE_REFINER,
                    new ItemStack(cleanOutput).getHoverName().getString(),
                    machine,
                    slots,
                    List.of("Tier 2 isotope transmutation.", "Time: 160 ticks", "Power: 500 FE/op", "Contamination chance: 20%"),
                    160,
                    false,
                    EchoAshfallProtocol.MODID));
        }
    }

    private static void addWaterPurifierRecipes(List<IndexRecipeView> views) {
        ItemStack machine = new ItemStack(ModBlocks.WATER_PURIFIER_ITEM.get());
        ItemStack filterBasic = new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get());
        ItemStack filterAdvanced = new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get());
        ItemStack filterElite = new ItemStack(ModItems.FILTER_CARTRIDGE_ELITE.get());

        // Dirty water purification
        views.add(new IndexRecipeView(
                id("water_purifier/dirty_water_to_clean"),
                WATER_PURIFIER,
                new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get()).getHoverName().getString(),
                machine,
                List.of(
                        IndexRecipeSlot.input(new ItemStack(ModItems.DIRTY_WATER_BOTTLE.get())),
                        new IndexRecipeSlot(IndexSlotRole.CATALYST, List.of(filterBasic, filterAdvanced, filterElite), "Filter Cartridge"),
                        IndexRecipeSlot.machine(machine),
                        IndexRecipeSlot.output(new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get()))),
                List.of("Batch purifies up to 3 bottles.", "Time: 60 ticks", "Power: 20 FE/purify", "15% filter wear chance"),
                60,
                false,
                EchoAshfallProtocol.MODID));

        // Contaminated resource purification
        for (java.util.Map.Entry<Item, Item> entry : WaterPurifierBlockEntity.getContaminatedPurify().entrySet()) {
            ItemStack input = new ItemStack(entry.getKey());
            ItemStack output = new ItemStack(entry.getValue());
            views.add(new IndexRecipeView(
                    id("water_purifier/" + path(entry.getKey()) + "_to_" + path(entry.getValue())),
                    WATER_PURIFIER,
                    output.getHoverName().getString(),
                    machine,
                    List.of(
                            IndexRecipeSlot.input(input),
                            new IndexRecipeSlot(IndexSlotRole.CATALYST, List.of(filterBasic, filterAdvanced, filterElite), "Filter Cartridge"),
                            IndexRecipeSlot.machine(machine),
                            IndexRecipeSlot.output(output)),
                    List.of("Contaminated resource decontamination.", "Time: 60 ticks", "Power: 20 FE/purify"),
                    60,
                    false,
                    EchoAshfallProtocol.MODID));
        }
    }

    private static void addFilterWorkbenchRecipes(List<IndexRecipeView> views) {
        ItemStack machine = new ItemStack(ModBlocks.FILTER_WORKBENCH_ITEM.get());

        // Basic Filter
        views.add(new IndexRecipeView(
                id("filter_workbench/basic_filter"),
                FILTER_WORKBENCH,
                new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get()).getHoverName().getString(),
                machine,
                List.of(
                        new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(new ItemStack(ModItems.SCRAP_PLASTIC.get(), 2)), "Primary"),
                        new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(new ItemStack(ModItems.FILTRATION_MEMBRANE.get())), "Secondary"),
                        IndexRecipeSlot.machine(machine),
                        IndexRecipeSlot.output(new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get()))),
                List.of("Basic filtration cartridge.", "Time: 20 ticks", "Power: 30 FE/craft"),
                20,
                false,
                EchoAshfallProtocol.MODID));

        // Advanced Filter
        views.add(new IndexRecipeView(
                id("filter_workbench/advanced_filter"),
                FILTER_WORKBENCH,
                new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get()).getHoverName().getString(),
                machine,
                List.of(
                        new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get())), "Primary"),
                        new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(new ItemStack(ModItems.CIRCUIT_BOARD.get())), "Secondary"),
                        new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(new ItemStack(ModItems.ENERGY_CELL.get())), "Tertiary"),
                        IndexRecipeSlot.machine(machine),
                        IndexRecipeSlot.output(new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get()))),
                List.of("Upgraded filtration with circuitry.", "Time: 20 ticks", "Power: 30 FE/craft"),
                20,
                false,
                EchoAshfallProtocol.MODID));

        // Elite Filter
        views.add(new IndexRecipeView(
                id("filter_workbench/elite_filter"),
                FILTER_WORKBENCH,
                new ItemStack(ModItems.FILTER_CARTRIDGE_ELITE.get()).getHoverName().getString(),
                machine,
                List.of(
                        new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get())), "Primary"),
                        new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(new ItemStack(ModItems.MACHINE_CASING.get())), "Secondary"),
                        new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(new ItemStack(ModItems.ENERGY_CELL.get())), "Tertiary"),
                        IndexRecipeSlot.machine(machine),
                        IndexRecipeSlot.output(new ItemStack(ModItems.FILTER_CARTRIDGE_ELITE.get()))),
                List.of("Maximum-grade filtration system.", "Time: 20 ticks", "Power: 30 FE/craft"),
                20,
                false,
                EchoAshfallProtocol.MODID));
    }

    private static void addRadiationCleanserRecipes(List<IndexRecipeView> views) {
        ItemStack machine = new ItemStack(ModBlocks.RADIATION_CLEANSER_ITEM.get());
        ItemStack filter = new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get());
        for (java.util.Map.Entry<Item, Item> entry : RadiationCleanserBlockEntity.getDecontaminationMap().entrySet()) {
            ItemStack input = new ItemStack(entry.getKey());
            ItemStack output = new ItemStack(entry.getValue());
            views.add(new IndexRecipeView(
                    id("radiation_cleanser/" + path(entry.getKey()) + "_to_" + path(entry.getValue())),
                    RADIATION_CLEANSER,
                    output.getHoverName().getString(),
                    machine,
                    List.of(
                            IndexRecipeSlot.input(input),
                            new IndexRecipeSlot(IndexSlotRole.CATALYST, List.of(filter), "Advanced Filter"),
                            IndexRecipeSlot.machine(machine),
                            IndexRecipeSlot.output(output)),
                    List.of("Full radiation decontamination.", "Time: 400 ticks", "Power: 8 FE/tick", "20% filter wear chance"),
                    400,
                    false,
                    EchoAshfallProtocol.MODID));
        }
    }

    private static void addCrystallineSynthesizerRecipes(List<IndexRecipeView> views) {
        ItemStack machine = new ItemStack(ModBlocks.CRYSTALLINE_SYNTHESIZER_ITEM.get());
        List<ItemStack> possibleOutputs = new ArrayList<>();
        for (Item item : CrystallineSynthesizerBlockEntity.getPossibleOutputs()) {
            possibleOutputs.add(new ItemStack(item));
        }
        views.add(new IndexRecipeView(
                id("crystalline_synthesizer/gem_synthesis"),
                CRYSTALLINE_SYNTHESIZER,
                "Gem / Netherite Scrap",
                machine,
                List.of(
                        new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(new ItemStack(ModItems.GEM_FRAGMENT.get(), 4)), "Gem Fragments"),
                        new IndexRecipeSlot(IndexSlotRole.INPUT, List.of(new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get())), "Dense Alloy"),
                        new IndexRecipeSlot(IndexSlotRole.CATALYST, List.of(new ItemStack(ModItems.ENERGY_CELL.get(), 2)), "Energy Cells"),
                        IndexRecipeSlot.machine(machine),
                        new IndexRecipeSlot(IndexSlotRole.OUTPUT, possibleOutputs, "Possible Output")),
                List.of("Tier 3 crystalline synthesis.", "Time: 400 ticks", "Power: 1-3 FE/tick by phase", "Output varies by RNG; power failure biases toward netherite scrap"),
                400,
                false,
                EchoAshfallProtocol.MODID));
    }

    private static IndexEntry entry(String path, String title, String summary, String body, ItemStack icon,
            List<Identifier> linkedItems, int sortOrder) {
        return new IndexEntry(
                id("machine/" + path),
                CATEGORY_MACHINES,
                title,
                "",
                summary,
                body,
                icon,
                EchoAshfallProtocol.MODID,
                List.of("machine", "ashfall", "recipe", path),
                IndexEntryState.VISIBLE,
                List.of(),
                linkedItems,
                List.of(),
                sortOrder);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, sanitize(path));
    }

    private static Identifier itemId(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? Identifier.withDefaultNamespace("air") : id;
    }

    private static String path(Item item) {
        return sanitize(itemId(item).getPath());
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
