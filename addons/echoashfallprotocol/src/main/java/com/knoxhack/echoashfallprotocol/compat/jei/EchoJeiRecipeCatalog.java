package com.knoxhack.echoashfallprotocol.compat.jei;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.block.entity.OreGrinderBlockEntity;
import com.knoxhack.echoashfallprotocol.recipe.ScrapPressRecipe;
import com.knoxhack.echoashfallprotocol.integration.AshfallRecipeInfoCatalog;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.schematic.SchematicTier;
import com.knoxhack.echoashfallprotocol.schematic.SchematicUnlockTable;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EchoJeiRecipeCatalog {
    private static final int IN_X = 18;
    private static final int MID_X = 42;
    private static final int OUT_X = 112;
    private static final int TOP_Y = 27;
    private static final int LOWER_Y = 47;

    private EchoJeiRecipeCatalog() {
    }

    public static Map<IRecipeType<EchoJeiRecipe>, List<EchoJeiRecipe>> allRecipes() {
        Map<IRecipeType<EchoJeiRecipe>, List<EchoJeiRecipe>> map = new HashMap<>();
        put(map, EchoJeiRecipeTypes.HAND_RECYCLER, handRecycler());
        put(map, EchoJeiRecipeTypes.WATER_PURIFIER, waterPurifier());
        put(map, EchoJeiRecipeTypes.WATER_COLLECTION, waterCollection());
        put(map, EchoJeiRecipeTypes.THERMAL_BURNER, thermalBurner());
        put(map, EchoJeiRecipeTypes.MICRO_GENERATOR, microGenerator());
        put(map, EchoJeiRecipeTypes.FILTER_WORKBENCH, filterWorkbench());
        put(map, EchoJeiRecipeTypes.SCRAP_PRESS, scrapPress());
        put(map, EchoJeiRecipeTypes.ORE_GRINDER, oreGrinder());
        put(map, EchoJeiRecipeTypes.ISOTOPE_REFINER, isotopeRefiner());
        put(map, EchoJeiRecipeTypes.RADIATION_CLEANSER, radiationCleanser());
        put(map, EchoJeiRecipeTypes.CRYSTALLINE_SYNTHESIZER, crystallineSynthesizer());
        put(map, EchoJeiRecipeTypes.DEEP_CORE_MINER, deepCoreMiner());
        return map;
    }

    public static Map<Item, Component> gatedItemInfo() {
        return AshfallRecipeInfoCatalog.gatedItemInfo();
    }

    public static Component gatedNote(String recipeId) {
        for (Map.Entry<SchematicTier, List<String>> entry : gatedByTier().entrySet()) {
            if (entry.getValue().contains(recipeId)) {
                return Component.literal("Requires " + entry.getKey().name() + " schematic unlock.");
            }
        }
        return Component.empty();
    }

    private static void put(Map<IRecipeType<EchoJeiRecipe>, List<EchoJeiRecipe>> map,
                            IRecipeType<EchoJeiRecipe> type,
                            List<EchoJeiRecipe> recipes) {
        map.put(type, recipes);
    }

    private static List<EchoJeiRecipe> handRecycler() {
        return List.of(
                recipe(EchoJeiRecipeTypes.HAND_RECYCLER, "hand_recycler/scrap_metal",
                        inputs(slot(IN_X, TOP_Y, ModItems.SCRAP_METAL)),
                        outputs(slot(OUT_X, TOP_Y, ModItems.MACHINE_CASING)), 100,
                        note("Consumes 1 scrap and 1 power per tick.")),
                recipe(EchoJeiRecipeTypes.HAND_RECYCLER, "hand_recycler/scrap_wire",
                        inputs(slot(IN_X, TOP_Y, ModItems.SCRAP_WIRE)),
                        outputs(slot(OUT_X, TOP_Y, ModItems.CIRCUIT_BOARD)), 100,
                        note("Consumes 1 scrap and 1 power per tick.")),
                recipe(EchoJeiRecipeTypes.HAND_RECYCLER, "hand_recycler/scrap_circuit",
                        inputs(slot(IN_X, TOP_Y, ModItems.SCRAP_CIRCUIT)),
                        outputs(slot(OUT_X, TOP_Y, ModItems.ENERGY_CELL)), 100,
                        note("Consumes 1 scrap and 1 power per tick.")),
                recipe(EchoJeiRecipeTypes.HAND_RECYCLER, "hand_recycler/scrap_plastic",
                        inputs(slot(IN_X, TOP_Y, ModItems.SCRAP_PLASTIC)),
                        outputs(slot(OUT_X, TOP_Y, ModItems.FILTRATION_MEMBRANE)), 100,
                        note("Consumes 1 scrap and 1 power per tick."))
        );
    }

    private static List<EchoJeiRecipe> waterPurifier() {
        List<ItemStack> filters = List.of(
                stack(ModItems.FILTER_CARTRIDGE_BASIC),
                stack(ModItems.FILTER_CARTRIDGE_ADVANCED),
                stack(ModItems.FILTER_CARTRIDGE_ELITE)
        );
        List<EchoJeiRecipe> recipes = new ArrayList<>();
        recipes.add(recipe(EchoJeiRecipeTypes.WATER_PURIFIER, "water_purifier/dirty_water",
                inputs(slot(IN_X, TOP_Y, ModItems.DIRTY_WATER_BOTTLE), slot(MID_X, LOWER_Y, filters)),
                outputs(slot(OUT_X, TOP_Y, ModItems.CLEAN_WATER_BOTTLE)), 60,
                note("Filter has a 15% chance to be consumed.")));
        recipes.add(cleanse(EchoJeiRecipeTypes.WATER_PURIFIER, "water_purifier/contaminated_iron", ModItems.CONTAMINATED_IRON.get(), Items.IRON_INGOT, filters, 60));
        recipes.add(cleanse(EchoJeiRecipeTypes.WATER_PURIFIER, "water_purifier/contaminated_gold", ModItems.CONTAMINATED_GOLD.get(), Items.GOLD_INGOT, filters, 60));
        recipes.add(cleanse(EchoJeiRecipeTypes.WATER_PURIFIER, "water_purifier/contaminated_redstone", ModItems.CONTAMINATED_REDSTONE.get(), Items.REDSTONE, filters, 60));
        recipes.add(cleanse(EchoJeiRecipeTypes.WATER_PURIFIER, "water_purifier/contaminated_lapis", ModItems.CONTAMINATED_LAPIS.get(), Items.LAPIS_LAZULI, filters, 60));
        return recipes;
    }

    private static List<EchoJeiRecipe> waterCollection() {
        List<ItemStack> sources = List.of(
                new ItemStack(Items.WATER_BUCKET),
                stack(ModBlocks.TOXIC_PUDDLE_ITEM),
                stack(ModBlocks.ACIDIC_SLUDGE_ITEM)
        );
        return List.of(recipe(EchoJeiRecipeTypes.WATER_COLLECTION, "water_collection/dirty_water_bottle",
                inputs(slot(IN_X, TOP_Y, Items.GLASS_BOTTLE), slot(MID_X, LOWER_Y, sources)),
                outputs(slot(OUT_X, TOP_Y, ModItems.DIRTY_WATER_BOTTLE)), 0,
                note("Right-click water, toxic puddle, or acidic sludge."),
                note("Consumes 1 glass bottle.")));
    }

    private static List<EchoJeiRecipe> thermalBurner() {
        return List.of(recipe(EchoJeiRecipeTypes.THERMAL_BURNER, "thermal_burner/any_item",
                inputs(slot(IN_X, TOP_Y, List.of(
                        new ItemStack(Items.STICK),
                        new ItemStack(Items.COAL),
                        new ItemStack(Items.ROTTEN_FLESH),
                        stack(ModItems.SCRAP_PLASTIC),
                        stack(ModItems.ASH)
                ))),
                outputs(slot(OUT_X, TOP_Y, ModItems.ASH)), 40,
                note("Burns almost any item for 50 energy."),
                note("Produces 1 ash after every 4 burned items.")));
    }

    private static List<EchoJeiRecipe> microGenerator() {
        return List.of(recipe(EchoJeiRecipeTypes.MICRO_GENERATOR, "micro_generator/fuel",
                inputs(slot(IN_X, TOP_Y, List.of(
                        new ItemStack(Items.COAL),
                        new ItemStack(Items.CHARCOAL),
                        new ItemStack(Items.OAK_PLANKS),
                        new ItemStack(Items.SPRUCE_PLANKS),
                        new ItemStack(Items.BIRCH_PLANKS),
                        new ItemStack(Items.DARK_OAK_PLANKS),
                        new ItemStack(Items.STICK)
                ))),
                outputs(slot(OUT_X, TOP_Y, ModItems.POWER_CELL)), 160,
                note("Generates internal power, then feeds adjacent machines."),
                note("Can fail as machine wear rises.")));
    }

    private static List<EchoJeiRecipe> filterWorkbench() {
        return List.of(
                recipe(EchoJeiRecipeTypes.FILTER_WORKBENCH, "filter_workbench/basic",
                        inputs(slot(IN_X, TOP_Y, stack(ModItems.SCRAP_PLASTIC, 2)), slot(MID_X, TOP_Y, ModItems.FILTRATION_MEMBRANE)),
                        outputs(slot(OUT_X, TOP_Y, ModItems.FILTER_CARTRIDGE_BASIC)), 20,
                        note("Powered filter assembly.")),
                recipe(EchoJeiRecipeTypes.FILTER_WORKBENCH, "filter_workbench/advanced",
                        inputs(slot(IN_X, TOP_Y, ModItems.FILTER_CARTRIDGE_BASIC), slot(MID_X, TOP_Y, ModItems.CIRCUIT_BOARD), slot(MID_X, LOWER_Y, ModItems.ENERGY_CELL)),
                        outputs(slot(OUT_X, TOP_Y, ModItems.FILTER_CARTRIDGE_ADVANCED)), 20,
                        note("Powered filter upgrade.")),
                recipe(EchoJeiRecipeTypes.FILTER_WORKBENCH, "filter_workbench/elite",
                        inputs(slot(IN_X, TOP_Y, ModItems.FILTER_CARTRIDGE_ADVANCED), slot(MID_X, TOP_Y, ModItems.MACHINE_CASING), slot(MID_X, LOWER_Y, ModItems.ENERGY_CELL)),
                        outputs(slot(OUT_X, TOP_Y, ModItems.FILTER_CARTRIDGE_ELITE)), 20,
                        note("Powered filter upgrade."))
        );
    }

    private static List<EchoJeiRecipe> scrapPress() {
        List<ScrapPressRecipe> registered = ScrapPressRecipe.getAllRecipes();
        if (registered.isEmpty()) {
            return List.of(
                    scrapPressFallback("scrap_press/machine_casing", ModItems.SCRAP_METAL.get(), 9, ModItems.MACHINE_CASING.get(), 1, 40),
                    scrapPressFallback("scrap_press/circuit_board", ModItems.SCRAP_CIRCUIT.get(), 4, ModItems.CIRCUIT_BOARD.get(), 1, 60),
                    scrapPressFallback("scrap_press/filtration_membrane", ModItems.SCRAP_PLASTIC.get(), 4, ModItems.FILTRATION_MEMBRANE.get(), 1, 50)
            );
        }
        List<EchoJeiRecipe> recipes = new ArrayList<>();
        int index = 0;
        for (ScrapPressRecipe recipe : registered) {
            recipes.add(recipe(EchoJeiRecipeTypes.SCRAP_PRESS, "scrap_press/" + index++,
                    inputs(slot(IN_X, TOP_Y, recipe.createInputStack())),
                    outputs(slot(OUT_X, TOP_Y, recipe.createOutputStack())), recipe.processingTime(),
                    note("Powered compression recipe.")));
        }
        return recipes;
    }

    private static List<EchoJeiRecipe> oreGrinder() {
        List<EchoJeiRecipe> recipes = new ArrayList<>();
        for (OreGrinderBlockEntity.GrinderRecipe recipe : OreGrinderBlockEntity.getSubstrateRecipes().values()) {
            Identifier inputId = BuiltInRegistries.ITEM.getKey(recipe.input());
            String path = inputId.getNamespace().equals(EchoAshfallProtocol.MODID)
                    ? inputId.getPath()
                    : inputId.getNamespace() + "_" + inputId.getPath();
            recipes.add(ore(path, recipe));
        }
        return recipes;
    }

    private static List<EchoJeiRecipe> isotopeRefiner() {
        return List.of(
                isotope("iron", Items.IRON_INGOT, Items.GOLD_INGOT, ModItems.CONTAMINATED_GOLD.get()),
                isotope("copper", Items.COPPER_INGOT, Items.REDSTONE, ModItems.CONTAMINATED_REDSTONE.get()),
                isotope("coal", Items.COAL, Items.LAPIS_LAZULI, ModItems.CONTAMINATED_LAPIS.get())
        );
    }

    private static List<EchoJeiRecipe> radiationCleanser() {
        return List.of(
                radiation("iron", ModItems.CONTAMINATED_IRON.get(), Items.IRON_INGOT),
                radiation("gold", ModItems.CONTAMINATED_GOLD.get(), Items.GOLD_INGOT),
                radiation("redstone", ModItems.CONTAMINATED_REDSTONE.get(), Items.REDSTONE),
                radiation("lapis", ModItems.CONTAMINATED_LAPIS.get(), Items.LAPIS_LAZULI)
        );
    }

    private static List<EchoJeiRecipe> crystallineSynthesizer() {
        return List.of(recipe(EchoJeiRecipeTypes.CRYSTALLINE_SYNTHESIZER, "crystalline_synthesizer/high_value",
                inputs(slot(IN_X, TOP_Y, stack(ModItems.GEM_FRAGMENT, 4)), slot(IN_X, LOWER_Y, ModItems.DENSE_ALLOY_CHUNK), slot(MID_X, TOP_Y, stack(ModItems.ENERGY_CELL, 2))),
                outputs(slot(OUT_X, TOP_Y, List.of(new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD), new ItemStack(Items.NETHERITE_SCRAP)))),
                400,
                withGate("echoashfallprotocol:crystalline_synthesizer",
                        note("Random output. Power failure can collapse to netherite scrap."))));
    }

    private static List<EchoJeiRecipe> deepCoreMiner() {
        return List.of(recipe(EchoJeiRecipeTypes.DEEP_CORE_MINER, "deep_core_miner/core_sample",
                List.of(),
                outputs(slot(OUT_X, TOP_Y, List.of(
                        stack(ModItems.DENSE_ALLOY_CHUNK),
                        stack(ModItems.GEM_FRAGMENT),
                        stack(ModItems.CRYSTAL_DUST),
                        new ItemStack(Items.REDSTONE),
                        new ItemStack(Items.LAPIS_LAZULI)
                ))),
                800,
                note("Requires power and placement at Y <= -32."),
                note("Generates a random core sample.")));
    }

    private static EchoJeiRecipe cleanse(IRecipeType<EchoJeiRecipe> type, String path, Item input, Item output, List<ItemStack> filters, int ticks) {
        return recipe(type, path,
                inputs(slot(IN_X, TOP_Y, input), slot(MID_X, LOWER_Y, filters)),
                outputs(slot(OUT_X, TOP_Y, output)), ticks,
                note("Filter has a 15% chance to be consumed."));
    }

    private static EchoJeiRecipe scrapPressFallback(String path, Item input, int inputCount, Item output, int outputCount, int ticks) {
        return recipe(EchoJeiRecipeTypes.SCRAP_PRESS, path,
                inputs(slot(IN_X, TOP_Y, new ItemStack(input, inputCount))),
                outputs(slot(OUT_X, TOP_Y, new ItemStack(output, outputCount))), ticks,
                note("Powered compression recipe."));
    }

    private static EchoJeiRecipe ore(String id, OreGrinderBlockEntity.GrinderRecipe grinderRecipe) {
        List<EchoJeiRecipe.EchoJeiSlot> outputSlots = new ArrayList<>();
        outputSlots.add(slot(OUT_X, TOP_Y, new ItemStack(grinderRecipe.output(), grinderRecipe.outputCount())));
        if (grinderRecipe.byproduct() != null) {
            outputSlots.add(slot(OUT_X, LOWER_Y,
                    new ItemStack(grinderRecipe.byproduct(), grinderRecipe.byproductCount())));
        }

        List<Component> notes = new ArrayList<>();
        notes.add(note(grinderRecipe.categoryLabel() + ": " + grinderRecipe.handlingHint()));
        notes.add(note(grinderRecipe.partialBatch()
                ? "Consumes up to " + grinderRecipe.inputCount() + " inputs per operation."
                : "Consumes " + grinderRecipe.inputCount() + " inputs per operation."));
        if (grinderRecipe.byproduct() != null) {
            notes.add(note(Math.round(grinderRecipe.byproductChance() * 100.0F)
                    + "% chance for byproduct output."));
        }
        notes.add(note("Uses " + grinderRecipe.powerPerOperation() + " FE over "
                + grinderRecipe.processTime() + " ticks."));

        return recipe(EchoJeiRecipeTypes.ORE_GRINDER, "ore_grinder/" + id,
                inputs(slot(IN_X, TOP_Y, new ItemStack(grinderRecipe.input(), grinderRecipe.inputCount()))),
                outputSlots,
                grinderRecipe.processTime(),
                notes);
    }

    private static EchoJeiRecipe isotope(String id, Item input, Item cleanOutput, Item contaminatedOutput) {
        return recipe(EchoJeiRecipeTypes.ISOTOPE_REFINER, "isotope_refiner/" + id,
                inputs(slot(IN_X, TOP_Y, new ItemStack(input, 2)), slot(MID_X, LOWER_Y, ModItems.CRYSTAL_DUST)),
                outputs(slot(OUT_X, TOP_Y, cleanOutput), slot(OUT_X, LOWER_Y, contaminatedOutput)),
                160,
                note("Consumes 2 input and 1 crystal dust."),
                note("20% chance to produce contaminated output."));
    }

    private static EchoJeiRecipe radiation(String id, Item input, Item output) {
        return recipe(EchoJeiRecipeTypes.RADIATION_CLEANSER, "radiation_cleanser/" + id,
                inputs(slot(IN_X, TOP_Y, input), slot(MID_X, TOP_Y, ModItems.FILTER_CARTRIDGE_ADVANCED)),
                outputs(slot(OUT_X, TOP_Y, output)),
                400,
                note("Advanced filter has a 20% chance to be consumed."));
    }

    private static EchoJeiRecipe recipe(IRecipeType<EchoJeiRecipe> type, String path,
                                        List<EchoJeiRecipe.EchoJeiSlot> inputs,
                                        List<EchoJeiRecipe.EchoJeiSlot> outputs,
                                        int ticks,
                                        List<Component> notes) {
        return new EchoJeiRecipe(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, path), type, inputs, outputs, notes, ticks);
    }

    private static EchoJeiRecipe recipe(IRecipeType<EchoJeiRecipe> type, String path,
                                        List<EchoJeiRecipe.EchoJeiSlot> inputs,
                                        List<EchoJeiRecipe.EchoJeiSlot> outputs,
                                        int ticks,
                                        Component... notes) {
        return recipe(type, path, inputs, outputs, ticks, List.of(notes));
    }

    private static List<Component> withGate(String recipeId, Component... notes) {
        List<Component> list = new ArrayList<>();
        Component gate = gatedNote(recipeId);
        if (!gate.getString().isEmpty()) {
            list.add(gate);
        }
        list.addAll(List.of(notes));
        return list;
    }

    private static Component note(String text) {
        return Component.literal(text);
    }

    private static List<EchoJeiRecipe.EchoJeiSlot> inputs(EchoJeiRecipe.EchoJeiSlot... slots) {
        return List.of(slots);
    }

    private static List<EchoJeiRecipe.EchoJeiSlot> outputs(EchoJeiRecipe.EchoJeiSlot... slots) {
        return List.of(slots);
    }

    private static EchoJeiRecipe.EchoJeiSlot slot(int x, int y, ItemLike item) {
        return EchoJeiRecipe.EchoJeiSlot.of(x, y, stack(item));
    }

    private static EchoJeiRecipe.EchoJeiSlot slot(int x, int y, ItemStack stack) {
        return EchoJeiRecipe.EchoJeiSlot.of(x, y, stack);
    }

    private static EchoJeiRecipe.EchoJeiSlot slot(int x, int y, List<ItemStack> stacks) {
        return EchoJeiRecipe.EchoJeiSlot.of(x, y, stacks);
    }

    private static ItemStack stack(ItemLike item) {
        return new ItemStack(item);
    }

    private static ItemStack stack(ItemLike item, int count) {
        return new ItemStack(item, count);
    }

    private static Map<SchematicTier, List<String>> gatedByTier() {
        Map<SchematicTier, List<String>> map = new EnumMap<>(SchematicTier.class);
        for (SchematicTier tier : SchematicTier.values()) {
            map.put(tier, SchematicUnlockTable.recipesFor(tier));
        }
        return map;
    }
}
