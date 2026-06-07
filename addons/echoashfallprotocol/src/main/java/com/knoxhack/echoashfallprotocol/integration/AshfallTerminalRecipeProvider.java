package com.knoxhack.echoashfallprotocol.integration;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.block.entity.OreGrinderBlockEntity;
import com.knoxhack.echoashfallprotocol.recipe.ScrapPressRecipe;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.schematic.SchematicTier;
import com.knoxhack.echoashfallprotocol.schematic.SchematicUnlockTable;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeCategory;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeEntry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeNote;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeProvider;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeSlot;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class AshfallTerminalRecipeProvider implements TerminalRecipeProvider {
    public static final AshfallTerminalRecipeProvider INSTANCE = new AshfallTerminalRecipeProvider();
    private static final int ACCENT = 0xFF66D9FF;

    private static final Identifier HAND_RECYCLER = id("hand_recycler");
    private static final Identifier INFO = id("recipe_info");
    private static final Identifier WATER_PURIFIER = id("water_purifier");
    private static final Identifier WATER_COLLECTION = id("water_collection");
    private static final Identifier THERMAL_BURNER = id("thermal_burner");
    private static final Identifier MICRO_GENERATOR = id("micro_generator");
    private static final Identifier FILTER_WORKBENCH = id("filter_workbench");
    private static final Identifier SCRAP_PRESS = id("scrap_press");
    private static final Identifier ORE_GRINDER = id("ore_grinder");
    private static final Identifier ISOTOPE_REFINER = id("isotope_refiner");
    private static final Identifier RADIATION_CLEANSER = id("radiation_cleanser");
    private static final Identifier CRYSTALLINE_SYNTHESIZER = id("crystalline_synthesizer");
    private static final Identifier DEEP_CORE_MINER = id("deep_core_miner");

    private AshfallTerminalRecipeProvider() {
    }

    @Override
    public Identifier id() {
        return id("ashfall_recipes");
    }

    @Override
    public List<TerminalRecipeCategory> categories(Player player) {
        return List.of(
                category(INFO, "Item Info", ModBlocks.RAIN_COLLECTOR_ITEM.get(), 5),
                category(HAND_RECYCLER, "Hand Recycler", ModBlocks.HAND_RECYCLER_ITEM.get(), 10),
                category(WATER_PURIFIER, "Water Purifier", ModBlocks.WATER_PURIFIER_ITEM.get(), 20),
                category(WATER_COLLECTION, "Water Collection", ModItems.DIRTY_WATER_BOTTLE.get(), 25),
                category(THERMAL_BURNER, "Thermal Burner", ModBlocks.THERMAL_BURNER_ITEM.get(), 30),
                category(MICRO_GENERATOR, "Micro Generator", ModBlocks.MICRO_GENERATOR_ITEM.get(), 40),
                category(FILTER_WORKBENCH, "Filter Workbench", ModBlocks.FILTER_WORKBENCH_ITEM.get(), 50),
                category(SCRAP_PRESS, "Scrap Press", ModBlocks.SCRAP_PRESS_ITEM.get(), 60),
                category(ORE_GRINDER, "Substrate Grinder", ModBlocks.ORE_GRINDER_ITEM.get(), 70),
                category(ISOTOPE_REFINER, "Isotope Refiner", ModBlocks.ISOTOPE_REFINER_ITEM.get(), 80),
                category(RADIATION_CLEANSER, "Radiation Cleanser", ModBlocks.RADIATION_CLEANSER_ITEM.get(), 90),
                category(CRYSTALLINE_SYNTHESIZER, "Crystalline Synthesizer", ModBlocks.CRYSTALLINE_SYNTHESIZER_ITEM.get(), 100),
                category(DEEP_CORE_MINER, "Deep Core Miner", ModBlocks.DEEP_CORE_MINER_ITEM.get(), 110));
    }

    @Override
    public List<TerminalRecipeEntry> recipes(Player player) {
        List<TerminalRecipeEntry> recipes = new ArrayList<>();
        recipes.addAll(infoEntries());
        recipes.addAll(handRecycler());
        recipes.addAll(waterPurifier());
        recipes.addAll(waterCollection());
        recipes.addAll(thermalBurner());
        recipes.addAll(microGenerator());
        recipes.addAll(filterWorkbench());
        recipes.addAll(scrapPress());
        recipes.addAll(oreGrinder());
        recipes.addAll(isotopeRefiner());
        recipes.addAll(radiationCleanser());
        recipes.addAll(crystallineSynthesizer());
        recipes.addAll(deepCoreMiner());
        return List.copyOf(recipes);
    }

    private static List<TerminalRecipeEntry> infoEntries() {
        List<TerminalRecipeEntry> entries = new ArrayList<>();
        for (AshfallRecipeInfoCatalog.InfoEntry info : AshfallRecipeInfoCatalog.entries()) {
            if (info.stack().isEmpty() || info.text().getString().isEmpty()) {
                continue;
            }
            Identifier itemId = BuiltInRegistries.ITEM.getKey(info.stack().getItem());
            if (itemId == null) {
                continue;
            }
            entries.add(new TerminalRecipeEntry(
                    Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID,
                            "recipe_info/" + itemId.getNamespace() + "/" + itemId.getPath()),
                    INFO,
                    info.stack().getHoverName().getString(),
                    ItemStack.EMPTY,
                    List.of(TerminalRecipeSlot.info(info.stack(), "Info")),
                    List.of(TerminalRecipeNote.of(info.text())),
                    0,
                    false));
        }
        return entries;
    }

    private static List<TerminalRecipeEntry> handRecycler() {
        return List.of(
                recipe(HAND_RECYCLER, "scrap_metal", "Recycle Scrap Metal", ModBlocks.HAND_RECYCLER_ITEM.get(),
                        slots(in(ModItems.SCRAP_METAL), out(ModItems.MACHINE_CASING)), 100,
                        note("Consumes 1 scrap and 1 power per tick.")),
                recipe(HAND_RECYCLER, "scrap_wire", "Recycle Scrap Wire", ModBlocks.HAND_RECYCLER_ITEM.get(),
                        slots(in(ModItems.SCRAP_WIRE), out(ModItems.CIRCUIT_BOARD)), 100,
                        note("Consumes 1 scrap and 1 power per tick.")),
                recipe(HAND_RECYCLER, "scrap_circuit", "Recycle Scrap Circuit", ModBlocks.HAND_RECYCLER_ITEM.get(),
                        slots(in(ModItems.SCRAP_CIRCUIT), out(ModItems.ENERGY_CELL)), 100,
                        note("Consumes 1 scrap and 1 power per tick.")),
                recipe(HAND_RECYCLER, "scrap_plastic", "Recycle Scrap Plastic", ModBlocks.HAND_RECYCLER_ITEM.get(),
                        slots(in(ModItems.SCRAP_PLASTIC), out(ModItems.FILTRATION_MEMBRANE)), 100,
                        note("Consumes 1 scrap and 1 power per tick.")));
    }

    private static List<TerminalRecipeEntry> waterPurifier() {
        List<ItemStack> filters = stacks(ModItems.FILTER_CARTRIDGE_BASIC, ModItems.FILTER_CARTRIDGE_ADVANCED, ModItems.FILTER_CARTRIDGE_ELITE);
        return List.of(
                recipe(WATER_PURIFIER, "dirty_water", "Purify Dirty Water", ModBlocks.WATER_PURIFIER_ITEM.get(),
                        slots(in(ModItems.DIRTY_WATER_BOTTLE), catalyst(filters), out(ModItems.CLEAN_WATER_BOTTLE)), 60,
                        note("Filter has a 15% chance to be consumed.")),
                cleanse("contaminated_iron", ModItems.CONTAMINATED_IRON.get(), Items.IRON_INGOT, filters),
                cleanse("contaminated_gold", ModItems.CONTAMINATED_GOLD.get(), Items.GOLD_INGOT, filters),
                cleanse("contaminated_redstone", ModItems.CONTAMINATED_REDSTONE.get(), Items.REDSTONE, filters),
                cleanse("contaminated_lapis", ModItems.CONTAMINATED_LAPIS.get(), Items.LAPIS_LAZULI, filters));
    }

    private static TerminalRecipeEntry cleanse(String path, Item input, Item output, List<ItemStack> filters) {
        return recipe(WATER_PURIFIER, path, "Cleanse " + title(output), ModBlocks.WATER_PURIFIER_ITEM.get(),
                slots(in(input), catalyst(filters), out(output)), 60,
                note("Filter has a 15% chance to be consumed."));
    }

    private static List<TerminalRecipeEntry> waterCollection() {
        return List.of(recipe(WATER_COLLECTION, "dirty_water_bottle", "Collect Dirty Water", ModItems.DIRTY_WATER_BOTTLE.get(),
                slots(in(Items.GLASS_BOTTLE),
                        TerminalRecipeSlot.info(new ItemStack(Items.WATER_BUCKET), "Source"),
                        TerminalRecipeSlot.info(new ItemStack(ModBlocks.TOXIC_PUDDLE_ITEM.get()), "Source"),
                        TerminalRecipeSlot.info(new ItemStack(ModBlocks.ACIDIC_SLUDGE_ITEM.get()), "Source"),
                        out(ModItems.DIRTY_WATER_BOTTLE)),
                0,
                note("Right-click water, toxic puddle, or acidic sludge."),
                note("Consumes 1 glass bottle.")));
    }

    private static List<TerminalRecipeEntry> thermalBurner() {
        return List.of(recipe(THERMAL_BURNER, "any_item", "Burn Item To Ash", ModBlocks.THERMAL_BURNER_ITEM.get(),
                slots(TerminalRecipeSlot.inputs(stacks(Items.STICK, Items.COAL, Items.ROTTEN_FLESH, ModItems.SCRAP_PLASTIC, ModItems.ASH)),
                        out(ModItems.ASH)),
                40,
                note("Burns almost any item for 50 energy."),
                note("Produces 1 ash after every 4 burned items.")));
    }

    private static List<TerminalRecipeEntry> microGenerator() {
        return List.of(recipe(MICRO_GENERATOR, "fuel", "Generate Starter Power", ModBlocks.MICRO_GENERATOR_ITEM.get(),
                slots(TerminalRecipeSlot.inputs(stacks(Items.COAL, Items.CHARCOAL, Items.OAK_PLANKS, Items.SPRUCE_PLANKS,
                        Items.BIRCH_PLANKS, Items.DARK_OAK_PLANKS, Items.STICK)),
                        out(ModItems.POWER_CELL)),
                160,
                note("Generates internal power, then feeds adjacent machines."),
                note("Can fail as machine wear rises.")));
    }

    private static List<TerminalRecipeEntry> filterWorkbench() {
        return List.of(
                recipe(FILTER_WORKBENCH, "basic", "Assemble Basic Filter", ModBlocks.FILTER_WORKBENCH_ITEM.get(),
                        slots(in(stack(ModItems.SCRAP_PLASTIC, 2)), in(ModItems.FILTRATION_MEMBRANE), out(ModItems.FILTER_CARTRIDGE_BASIC)), 20,
                        note("Powered filter assembly.")),
                recipe(FILTER_WORKBENCH, "advanced", "Upgrade Advanced Filter", ModBlocks.FILTER_WORKBENCH_ITEM.get(),
                        slots(in(ModItems.FILTER_CARTRIDGE_BASIC), in(ModItems.CIRCUIT_BOARD), in(ModItems.ENERGY_CELL),
                                out(ModItems.FILTER_CARTRIDGE_ADVANCED)), 20,
                        note("Powered filter upgrade.")),
                recipe(FILTER_WORKBENCH, "elite", "Upgrade Elite Filter", ModBlocks.FILTER_WORKBENCH_ITEM.get(),
                        slots(in(ModItems.FILTER_CARTRIDGE_ADVANCED), in(ModItems.MACHINE_CASING), in(ModItems.ENERGY_CELL),
                                out(ModItems.FILTER_CARTRIDGE_ELITE)), 20,
                        note("Powered filter upgrade.")));
    }

    private static List<TerminalRecipeEntry> scrapPress() {
        List<ScrapPressRecipe> registered = ScrapPressRecipe.getAllRecipes();
        if (registered.isEmpty()) {
            return List.of(
                    press("machine_casing", ModItems.SCRAP_METAL.get(), 9, ModItems.MACHINE_CASING.get(), 1, 40),
                    press("circuit_board", ModItems.SCRAP_CIRCUIT.get(), 4, ModItems.CIRCUIT_BOARD.get(), 1, 60),
                    press("filtration_membrane", ModItems.SCRAP_PLASTIC.get(), 4, ModItems.FILTRATION_MEMBRANE.get(), 1, 50));
        }
        List<TerminalRecipeEntry> entries = new ArrayList<>();
        int index = 0;
        for (ScrapPressRecipe recipe : registered) {
            entries.add(recipe(SCRAP_PRESS, "registered_" + index++, "Press " + recipe.createOutputStack().getHoverName().getString(),
                    ModBlocks.SCRAP_PRESS_ITEM.get(),
                    slots(in(recipe.createInputStack()), out(recipe.createOutputStack())), recipe.processingTime(),
                    note("Powered compression recipe.")));
        }
        return entries;
    }

    private static TerminalRecipeEntry press(String path, Item input, int inputCount, Item output, int outputCount, int ticks) {
        return recipe(SCRAP_PRESS, path, "Press " + title(output), ModBlocks.SCRAP_PRESS_ITEM.get(),
                slots(in(new ItemStack(input, inputCount)), out(new ItemStack(output, outputCount))), ticks,
                note("Powered compression recipe."));
    }

    private static List<TerminalRecipeEntry> oreGrinder() {
        List<TerminalRecipeEntry> entries = new ArrayList<>();
        for (OreGrinderBlockEntity.GrinderRecipe grinder : OreGrinderBlockEntity.getSubstrateRecipes().values()) {
            Identifier inputId = BuiltInRegistries.ITEM.getKey(grinder.input());
            String path = inputId.getNamespace().equals(EchoAshfallProtocol.MODID)
                    ? inputId.getPath()
                    : inputId.getNamespace() + "_" + inputId.getPath();
            List<TerminalRecipeSlot> slots = new ArrayList<>();
            slots.add(in(new ItemStack(grinder.input(), grinder.inputCount())));
            slots.add(out(new ItemStack(grinder.output(), grinder.outputCount())));
            if (grinder.byproduct() != null) {
                slots.add(out(new ItemStack(grinder.byproduct(), grinder.byproductCount())));
            }
            List<TerminalRecipeNote> notes = new ArrayList<>();
            notes.add(note(grinder.categoryLabel() + ": " + grinder.handlingHint()));
            notes.add(note(grinder.partialBatch()
                    ? "Consumes up to " + grinder.inputCount() + " inputs per operation."
                    : "Consumes " + grinder.inputCount() + " inputs per operation."));
            if (grinder.byproduct() != null) {
                notes.add(note(Math.round(grinder.byproductChance() * 100.0F) + "% chance for byproduct output."));
            }
            notes.add(note("Uses " + grinder.powerPerOperation() + " FE over " + grinder.processTime() + " ticks."));
            entries.add(recipe(ORE_GRINDER, path, "Grind " + title(grinder.input()), ModBlocks.ORE_GRINDER_ITEM.get(),
                    slots, grinder.processTime(), notes));
        }
        return entries;
    }

    private static List<TerminalRecipeEntry> isotopeRefiner() {
        return List.of(
                isotope("iron", Items.IRON_INGOT, Items.GOLD_INGOT, ModItems.CONTAMINATED_GOLD.get()),
                isotope("copper", Items.COPPER_INGOT, Items.REDSTONE, ModItems.CONTAMINATED_REDSTONE.get()),
                isotope("coal", Items.COAL, Items.LAPIS_LAZULI, ModItems.CONTAMINATED_LAPIS.get()));
    }

    private static TerminalRecipeEntry isotope(String path, Item input, Item cleanOutput, Item contaminatedOutput) {
        return recipe(ISOTOPE_REFINER, path, "Refine " + title(input), ModBlocks.ISOTOPE_REFINER_ITEM.get(),
                slots(in(new ItemStack(input, 2)), catalyst(new ItemStack(ModItems.CRYSTAL_DUST.get())),
                        out(cleanOutput), out(contaminatedOutput)),
                160,
                note("Consumes 2 input and 1 crystal dust."),
                note("20% chance to produce contaminated output."));
    }

    private static List<TerminalRecipeEntry> radiationCleanser() {
        return List.of(
                radiation("iron", ModItems.CONTAMINATED_IRON.get(), Items.IRON_INGOT),
                radiation("gold", ModItems.CONTAMINATED_GOLD.get(), Items.GOLD_INGOT),
                radiation("redstone", ModItems.CONTAMINATED_REDSTONE.get(), Items.REDSTONE),
                radiation("lapis", ModItems.CONTAMINATED_LAPIS.get(), Items.LAPIS_LAZULI));
    }

    private static TerminalRecipeEntry radiation(String path, Item input, Item output) {
        return recipe(RADIATION_CLEANSER, path, "Cleanse " + title(output), ModBlocks.RADIATION_CLEANSER_ITEM.get(),
                slots(in(input), catalyst(ModItems.FILTER_CARTRIDGE_ADVANCED), out(output)), 400,
                note("Advanced filter has a 20% chance to be consumed."));
    }

    private static List<TerminalRecipeEntry> crystallineSynthesizer() {
        return List.of(recipe(CRYSTALLINE_SYNTHESIZER, "high_value", "Synthesize High-Value Crystal Output",
                ModBlocks.CRYSTALLINE_SYNTHESIZER_ITEM.get(),
                slots(in(stack(ModItems.GEM_FRAGMENT, 4)), in(ModItems.DENSE_ALLOY_CHUNK), in(stack(ModItems.ENERGY_CELL, 2)),
                        TerminalRecipeSlot.outputs(stacks(Items.DIAMOND, Items.EMERALD, Items.NETHERITE_SCRAP))),
                400,
                withGate("echoashfallprotocol:crystalline_synthesizer",
                        note("Random output. Power failure can collapse to netherite scrap."))));
    }

    private static List<TerminalRecipeEntry> deepCoreMiner() {
        return List.of(recipe(DEEP_CORE_MINER, "core_sample", "Mine Deep Core Sample", ModBlocks.DEEP_CORE_MINER_ITEM.get(),
                slots(TerminalRecipeSlot.outputs(stacks(ModItems.DENSE_ALLOY_CHUNK, ModItems.GEM_FRAGMENT, ModItems.CRYSTAL_DUST,
                        Items.REDSTONE, Items.LAPIS_LAZULI))),
                800,
                note("Requires power and placement at Y <= -32."),
                note("Generates a random core sample.")));
    }

    private static TerminalRecipeCategory category(Identifier id, String title, ItemLike icon, int order) {
        return new TerminalRecipeCategory(id, title, new ItemStack(icon), ACCENT, order);
    }

    private static TerminalRecipeEntry recipe(Identifier category, String path, String title, ItemLike machine,
            List<TerminalRecipeSlot> slots, int ticks, TerminalRecipeNote... notes) {
        return recipe(category, path, title, machine, slots, ticks, List.of(notes));
    }

    private static TerminalRecipeEntry recipe(Identifier category, String path, String title, ItemLike machine,
            List<TerminalRecipeSlot> slots, int ticks, List<TerminalRecipeNote> notes) {
        return new TerminalRecipeEntry(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, category.getPath() + "/" + path),
                category, title, new ItemStack(machine), slots, notes, ticks, locked(notes));
    }

    private static boolean locked(List<TerminalRecipeNote> notes) {
        return notes.stream().anyMatch(TerminalRecipeNote::warning);
    }

    private static TerminalRecipeSlot in(ItemLike item) {
        return TerminalRecipeSlot.input(new ItemStack(item));
    }

    private static TerminalRecipeSlot in(ItemStack stack) {
        return TerminalRecipeSlot.input(stack);
    }

    private static TerminalRecipeSlot out(ItemLike item) {
        return TerminalRecipeSlot.output(new ItemStack(item));
    }

    private static TerminalRecipeSlot out(ItemStack stack) {
        return TerminalRecipeSlot.output(stack);
    }

    private static TerminalRecipeSlot catalyst(ItemLike item) {
        return TerminalRecipeSlot.catalyst(new ItemStack(item));
    }

    private static TerminalRecipeSlot catalyst(ItemStack stack) {
        return TerminalRecipeSlot.catalyst(stack);
    }

    private static TerminalRecipeSlot catalyst(List<ItemStack> stacks) {
        return new TerminalRecipeSlot(TerminalRecipeSlot.Role.CATALYST, stacks, "Catalyst");
    }

    private static List<TerminalRecipeSlot> slots(TerminalRecipeSlot... slots) {
        return List.of(slots);
    }

    private static List<ItemStack> stacks(ItemLike... items) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemLike item : items) {
            stacks.add(new ItemStack(item));
        }
        return stacks;
    }

    private static ItemStack stack(ItemLike item, int count) {
        return new ItemStack(item, count);
    }

    private static TerminalRecipeNote note(String text) {
        return TerminalRecipeNote.info(text);
    }

    private static List<TerminalRecipeNote> withGate(String recipeId, TerminalRecipeNote... notes) {
        List<TerminalRecipeNote> list = new ArrayList<>();
        TerminalRecipeNote gate = gatedNote(recipeId);
        if (!gate.text().getString().isEmpty()) {
            list.add(gate);
        }
        list.addAll(List.of(notes));
        return list;
    }

    private static TerminalRecipeNote gatedNote(String recipeId) {
        for (Map.Entry<SchematicTier, List<String>> entry : gatedByTier().entrySet()) {
            if (entry.getValue().contains(recipeId)) {
                return TerminalRecipeNote.warning("Requires " + entry.getKey().name() + " schematic unlock.");
            }
        }
        return TerminalRecipeNote.info("");
    }

    private static Map<SchematicTier, List<String>> gatedByTier() {
        Map<SchematicTier, List<String>> map = new EnumMap<>(SchematicTier.class);
        for (SchematicTier tier : SchematicTier.values()) {
            map.put(tier, SchematicUnlockTable.recipesFor(tier));
        }
        return map;
    }

    private static String title(ItemLike item) {
        return new ItemStack(item).getHoverName().getString();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, path);
    }
}
