package com.knoxhack.echoindex.service;

import com.knoxhack.echocore.api.index.IIndexRecipeProvider;
import com.knoxhack.echocore.api.index.IndexRecipeCategory;
import com.knoxhack.echocore.api.index.IndexRecipeSlot;
import com.knoxhack.echocore.api.index.IndexRecipeView;
import com.knoxhack.echocore.api.index.IndexSlotRole;
import com.knoxhack.echoindex.EchoIndex;
import com.knoxhack.echoindex.IndexIds;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;

public enum VanillaIndexRecipeProvider implements IIndexRecipeProvider {
    INSTANCE;

    private static final Identifier CRAFTING = EchoIndex.id("recipe/crafting");
    private static final Identifier SMELTING = EchoIndex.id("recipe/smelting");
    private static final Identifier BLASTING = EchoIndex.id("recipe/blasting");
    private static final Identifier SMOKING = EchoIndex.id("recipe/smoking");
    private static final Identifier CAMPFIRE = EchoIndex.id("recipe/campfire_cooking");
    private static final Identifier STONECUTTING = EchoIndex.id("recipe/stonecutting");
    private static final Identifier SMITHING = EchoIndex.id("recipe/smithing");
    private static final Set<String> DEDICATED_CUSTOM_TYPES = Set.of(
            "echoindustrialnexus:industrial_processing",
            "echonexusprotocol:nexus_processing",
            "echoblackboxprotocol:blackbox_processing",
            "echoconvoyprotocol:convoy_station_processing",
            "echoorbitalremnants:orbital_processing");
    private volatile int skippedRecipeCount;
    private final Map<Identifier, IndexRecipeDisplayMetadata> displayMetadata = new ConcurrentHashMap<>();

    @Override
    public Identifier id() {
        return IndexIds.PROVIDER_VANILLA_RECIPES;
    }

    @Override
    public List<IndexRecipeCategory> recipeCategories(Player player) {
        List<IndexRecipeCategory> categories = new ArrayList<>(List.of(
                category(CRAFTING, "Crafting", Items.CRAFTING_TABLE, 0xFF66E8FF, 10),
                category(SMELTING, "Smelting", Items.FURNACE, 0xFFFFD166, 20),
                category(BLASTING, "Blasting", Items.BLAST_FURNACE, 0xFFFF8FA3, 30),
                category(SMOKING, "Smoking", Items.SMOKER, 0xFFFFA05B, 40),
                category(CAMPFIRE, "Campfire Cooking", Items.CAMPFIRE, 0xFFFFD166, 50),
                category(STONECUTTING, "Stonecutting", Items.STONECUTTER, 0xFFE9FBFF, 60),
                category(SMITHING, "Smithing", Items.SMITHING_TABLE, 0xFF92F7A6, 70)));
        categories.addAll(customCategories(player));
        return List.copyOf(categories);
    }

    @Override
    public List<IndexRecipeView> recipes(Player player) {
        if (player == null || player.level() == null) {
            return List.of();
        }
        displayMetadata.clear();
        List<IndexRecipeView> views = new ArrayList<>();
        int skipped = 0;
        List<RecipeHolder<?>> holders = completeRecipeHolders(player);
        if (!holders.isEmpty()) {
            for (RecipeHolder<?> holder : holders) {
                IndexRecipeView view = adapt(holder);
                if (view != null) {
                    views.add(view);
                } else {
                    skipped++;
                }
            }
        } else {
            for (RecipeDisplayEntry entry : clientDisplayEntries(player)) {
                IndexRecipeView view = adaptDisplayEntry(entry);
                if (view != null) {
                    views.add(view);
                } else {
                    skipped++;
                }
            }
        }
        skippedRecipeCount = skipped;
        return views;
    }

    public static List<RecipeHolder<?>> completeRecipeHolders(Player player) {
        List<RecipeHolder<?>> holders = recipeHolders(player);
        if (!holders.isEmpty()) {
            return holders;
        }
        return recipeAccessHolders(player);
    }

    private static List<RecipeHolder<?>> recipeHolders(Player player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return List.of();
        }
        try {
            return List.copyOf(server.getRecipeManager().getRecipes());
        } catch (RuntimeException exception) {
            EchoIndex.LOGGER.debug("Index could not enumerate runtime recipes for {}.", player.getName().getString(), exception);
        }
        return List.of();
    }

    private static List<RecipeHolder<?>> recipeAccessHolders(Player player) {
        if (player == null || player.level() == null) {
            return List.of();
        }
        try {
            Object access = player.level().recipeAccess();
            Object recipes = access.getClass().getMethod("getRecipes").invoke(access);
            if (recipes instanceof Iterable<?> iterable) {
                List<RecipeHolder<?>> holders = new ArrayList<>();
                for (Object candidate : iterable) {
                    if (candidate instanceof RecipeHolder<?> holder) {
                        holders.add(holder);
                    }
                }
                return holders;
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoIndex.LOGGER.debug("Index recipeAccess did not expose a complete recipe list for {}.",
                    player.getName().getString(), exception);
        }
        return List.of();
    }

    public static int rawRecipeCount(Player player) {
        if (player == null || player.level() == null) {
            return 0;
        }
        List<RecipeHolder<?>> holders = completeRecipeHolders(player);
        return holders.isEmpty() ? clientDisplayEntries(player).size() : holders.size();
    }

    public static int adaptedRecipeCount(Player player) {
        return INSTANCE.recipes(player).size();
    }

    public int skippedRecipeCount() {
        return skippedRecipeCount;
    }

    public Optional<IndexRecipeDisplayMetadata> metadataFor(Identifier recipeId) {
        return recipeId == null ? Optional.empty() : Optional.ofNullable(displayMetadata.get(recipeId));
    }

    private static List<RecipeDisplayEntry> clientDisplayEntries(Player player) {
        try {
            Object result = Class.forName("com.knoxhack.echoindex.client.ClientRecipeDisplayAccess")
                    .getMethod("recipeDisplays", Player.class)
                    .invoke(null, player);
            if (result instanceof List<?> list) {
                List<RecipeDisplayEntry> entries = new ArrayList<>();
                Set<Integer> seen = new LinkedHashSet<>();
                for (Object candidate : list) {
                    if (candidate instanceof RecipeDisplayEntry entry && seen.add(entry.id().index())) {
                        entries.add(entry);
                    }
                }
                if (!entries.isEmpty()) {
                    return entries;
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoIndex.LOGGER.debug("Index could not enumerate direct client recipe displays for {}.",
                    player.getName().getString(), exception);
        }
        try {
            Object recipeBook = player.getClass().getMethod("getRecipeBook").invoke(player);
            Object collections = recipeBook.getClass().getMethod("getCollections").invoke(recipeBook);
            if (!(collections instanceof Iterable<?> iterable)) {
                return List.of();
            }
            List<RecipeDisplayEntry> entries = new ArrayList<>();
            Set<Integer> seen = new LinkedHashSet<>();
            for (Object collection : iterable) {
                Object recipes = collection.getClass().getMethod("getRecipes").invoke(collection);
                if (!(recipes instanceof Iterable<?> recipeIterable)) {
                    continue;
                }
                for (Object candidate : recipeIterable) {
                    if (candidate instanceof RecipeDisplayEntry entry && seen.add(entry.id().index())) {
                        entries.add(entry);
                    }
                }
            }
            return entries;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            EchoIndex.LOGGER.debug("Index could not enumerate client recipe displays for {}.",
                    player.getName().getString(), exception);
            return List.of();
        }
    }

    private static IndexRecipeView adapt(RecipeHolder<?> holder) {
        Recipe<?> recipe = holder.value();
        Identifier categoryId = categoryId(recipe);
        if (categoryId == null) {
            return null;
        }
        ItemStack output = output(recipe);
        if (output.isEmpty()) {
            return null;
        }
        ItemStack machine = machine(recipe, categoryId);
        List<IndexRecipeSlot> slots = new ArrayList<>();
        for (Ingredient ingredient : recipe.placementInfo().ingredients()) {
            List<ItemStack> stacks = ingredient.items()
                    .map(Holder::value)
                    .map(ItemStack::new)
                    .filter(stack -> !stack.isEmpty())
                    .limit(24)
                    .toList();
            if (!stacks.isEmpty()) {
                slots.add(IndexRecipeSlot.inputs(stacks));
            }
        }
        addDisplayCatalysts(recipe, slots);
        if (!machine.isEmpty()) {
            slots.add(IndexRecipeSlot.machine(machine));
        }
        slots.add(IndexRecipeSlot.output(output));
        String title = output.getHoverName().getString();
        int ticks = processTicks(recipe);
        IndexRecipeView view = new IndexRecipeView(
                holder.id().identifier(),
                categoryId,
                title,
                machine,
                slots,
                List.of("Source: " + holder.id().identifier().getNamespace()),
                ticks,
                false,
                holder.id().identifier().getNamespace());
        INSTANCE.displayMetadata.put(view.id(), displayMetadata(view.id(), recipe, categoryId, output, machine, slots));
        return view;
    }

    private static IndexRecipeView adaptDisplayEntry(RecipeDisplayEntry entry) {
        RecipeDisplay display = entry.display();
        ItemStack output = display.result().resolveForFirstStack(ContextMap.EMPTY);
        if (output.isEmpty()) {
            return null;
        }
        ItemStack machine = display.craftingStation().resolveForFirstStack(ContextMap.EMPTY);
        Identifier categoryId = categoryId(display, machine);
        List<IndexRecipeSlot> slots = new ArrayList<>();
        Optional<List<Ingredient>> requirements = entry.craftingRequirements();
        if (requirements.isPresent()) {
            for (Ingredient ingredient : requirements.get()) {
                addIngredientSlot(slots, ingredient);
            }
        }
        if (slots.isEmpty()) {
            for (SlotDisplay input : inputDisplays(display)) {
                List<ItemStack> stacks = input.resolveForStacks(ContextMap.EMPTY).stream()
                        .filter(stack -> stack != null && !stack.isEmpty())
                        .map(ItemStack::copy)
                        .limit(24)
                        .toList();
                if (!stacks.isEmpty()) {
                    slots.add(IndexRecipeSlot.inputs(stacks));
                }
            }
        }
        if (display instanceof FurnaceRecipeDisplay furnace) {
            List<ItemStack> fuel = furnace.fuel().resolveForStacks(ContextMap.EMPTY).stream()
                    .filter(stack -> stack != null && !stack.isEmpty())
                    .map(ItemStack::copy)
                    .limit(24)
                    .toList();
            if (!fuel.isEmpty()) {
                slots.add(new IndexRecipeSlot(IndexSlotRole.CATALYST, fuel, "Fuel"));
            }
        }
        if (!machine.isEmpty()) {
            slots.add(IndexRecipeSlot.machine(machine));
        }
        slots.add(IndexRecipeSlot.output(output.copy()));
        int ticks = display instanceof FurnaceRecipeDisplay furnace ? furnace.duration() : 0;
        Identifier outputId = IndexService.itemId(output.getItem());
        Identifier recipeId = transferAlias(categoryId, slots, output);
        IndexRecipeView view = new IndexRecipeView(
                recipeId,
                categoryId,
                output.getHoverName().getString(),
                machine,
                slots,
                List.of("Source: " + outputId.getNamespace()),
                ticks,
                false,
                outputId.getNamespace());
        INSTANCE.displayMetadata.put(view.id(), displayMetadata(view.id(), display, categoryId, output, machine, slots));
        return view;
    }

    public static Identifier transferAlias(RecipeHolder<?> holder) {
        IndexRecipeView view = adapt(holder);
        return view == null ? holder.id().identifier() : transferAlias(view);
    }

    public static Identifier transferAlias(IndexRecipeView recipe) {
        return recipe == null ? EchoIndex.id("recipe_alias/empty")
                : transferAlias(recipe.categoryId(), recipe.slots(), IndexRecipeUiLike.output(recipe));
    }

    private static Identifier transferAlias(Identifier categoryId, List<IndexRecipeSlot> slots, ItemStack output) {
        StringBuilder builder = new StringBuilder();
        builder.append(categoryId == null ? "" : categoryId.toString()).append('|');
        builder.append(output.isEmpty() ? "" : IndexService.itemId(output.getItem())).append('|');
        for (IndexRecipeSlot slot : slots) {
            if (slot.role() != IndexSlotRole.INPUT && slot.role() != IndexSlotRole.CATALYST) {
                continue;
            }
            builder.append(slot.role()).append(':');
            for (ItemStack stack : slot.stacks()) {
                if (!stack.isEmpty()) {
                    builder.append(IndexService.itemId(stack.getItem())).append('@').append(Math.max(1, stack.getCount())).append(',');
                }
            }
            builder.append(';');
        }
        return EchoIndex.id("recipe_alias/" + Integer.toHexString(builder.toString().hashCode()));
    }

    private static IndexRecipeCategory category(Identifier id, String title, Item icon, int color, int order) {
        return new IndexRecipeCategory(id, title, new ItemStack(icon), color, order);
    }

    private static List<IndexRecipeCategory> customCategories(Player player) {
        if (player == null || player.level() == null) {
            return List.of();
        }
        List<IndexRecipeCategory> categories = new ArrayList<>();
        Set<Identifier> seen = new LinkedHashSet<>();
        for (RecipeHolder<?> holder : completeRecipeHolders(player)) {
            Recipe<?> recipe = holder.value();
            if (knownVanillaType(recipe) || dedicatedCustomType(recipe)) {
                continue;
            }
            Identifier typeId = recipeTypeId(recipe.getType());
            Identifier categoryId = customCategoryId(typeId);
            if (!seen.add(categoryId) || output(recipe).isEmpty()) {
                continue;
            }
            ItemStack icon = machine(recipe, categoryId);
            if (icon.isEmpty()) {
                icon = output(recipe);
            }
            categories.add(new IndexRecipeCategory(
                    categoryId,
                    customCategoryTitle(typeId),
                    icon,
                    0xFFC77DFF,
                    1000 + Math.floorMod(typeId.toString().hashCode(), 500)));
        }
        return categories;
    }

    private static Identifier categoryId(Recipe<?> recipe) {
        if (dedicatedCustomType(recipe)) {
            return null;
        }
        RecipeType<?> type = recipe.getType();
        if (type == RecipeType.CRAFTING) {
            return CRAFTING;
        }
        if (type == RecipeType.SMELTING) {
            return SMELTING;
        }
        if (type == RecipeType.BLASTING) {
            return BLASTING;
        }
        if (type == RecipeType.SMOKING) {
            return SMOKING;
        }
        if (type == RecipeType.CAMPFIRE_COOKING) {
            return CAMPFIRE;
        }
        if (type == RecipeType.STONECUTTING) {
            return STONECUTTING;
        }
        if (type == RecipeType.SMITHING) {
            return SMITHING;
        }
        return customCategoryId(recipeTypeId(type));
    }

    private static boolean knownVanillaType(Recipe<?> recipe) {
        RecipeType<?> type = recipe.getType();
        return type == RecipeType.CRAFTING
                || type == RecipeType.SMELTING
                || type == RecipeType.BLASTING
                || type == RecipeType.SMOKING
                || type == RecipeType.CAMPFIRE_COOKING
                || type == RecipeType.STONECUTTING
                || type == RecipeType.SMITHING;
    }

    private static boolean dedicatedCustomType(Recipe<?> recipe) {
        return DEDICATED_CUSTOM_TYPES.contains(recipeTypeId(recipe.getType()).toString());
    }

    private static Identifier recipeTypeId(RecipeType<?> type) {
        Identifier id = BuiltInRegistries.RECIPE_TYPE.getKey(type);
        return id == null ? EchoIndex.id("unknown") : id;
    }

    private static Identifier customCategoryId(Identifier typeId) {
        return EchoIndex.id("recipe/custom/" + typeId.getNamespace() + "/" + typeId.getPath());
    }

    private static String customCategoryTitle(Identifier typeId) {
        String path = typeId.getPath().replace('/', ' ').replace('_', ' ').replace('-', ' ');
        StringBuilder builder = new StringBuilder();
        for (String part : path.split(" ")) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? "Custom Recipes" : builder.toString();
    }

    private static Identifier categoryId(RecipeDisplay display, ItemStack machine) {
        if (display instanceof StonecutterRecipeDisplay || machine.is(Items.STONECUTTER)) {
            return STONECUTTING;
        }
        if (display instanceof SmithingRecipeDisplay || machine.is(Items.SMITHING_TABLE)) {
            return SMITHING;
        }
        if (display instanceof FurnaceRecipeDisplay) {
            if (machine.is(Items.BLAST_FURNACE)) {
                return BLASTING;
            }
            if (machine.is(Items.SMOKER)) {
                return SMOKING;
            }
            if (machine.is(Items.CAMPFIRE) || machine.is(Items.SOUL_CAMPFIRE)) {
                return CAMPFIRE;
            }
            return SMELTING;
        }
        return CRAFTING;
    }

    private static ItemStack output(Recipe<?> recipe) {
        for (RecipeDisplay display : recipe.display()) {
            ItemStack stack = display.result().resolveForFirstStack(ContextMap.EMPTY);
            if (!stack.isEmpty()) {
                return stack.copy();
            }
        }
        try {
            Object value = recipe.getClass().getMethod("result").invoke(recipe);
            if (value instanceof ItemStack stack && !stack.isEmpty()) {
                return stack.copy();
            }
        } catch (ReflectiveOperationException exception) {
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack machine(Recipe<?> recipe, Identifier categoryId) {
        for (RecipeDisplay display : recipe.display()) {
            ItemStack stack = display.craftingStation().resolveForFirstStack(ContextMap.EMPTY);
            if (!stack.isEmpty()) {
                return stack.copy();
            }
        }
        String path = categoryId.getPath().toLowerCase(Locale.ROOT);
        if (path.contains("smelting")) {
            return new ItemStack(Items.FURNACE);
        }
        if (path.contains("blasting")) {
            return new ItemStack(Items.BLAST_FURNACE);
        }
        if (path.contains("smoking")) {
            return new ItemStack(Items.SMOKER);
        }
        if (path.contains("campfire")) {
            return new ItemStack(Items.CAMPFIRE);
        }
        if (path.contains("stonecutting")) {
            return new ItemStack(Items.STONECUTTER);
        }
        if (path.contains("smithing")) {
            return new ItemStack(Items.SMITHING_TABLE);
        }
        return new ItemStack(Items.CRAFTING_TABLE);
    }

    private static int processTicks(Recipe<?> recipe) {
        try {
            Object value = recipe.getClass().getMethod("cookingTime").invoke(recipe);
            return value instanceof Integer ticks ? ticks : 0;
        } catch (ReflectiveOperationException exception) {
            return 0;
        }
    }

    private static void addDisplayCatalysts(Recipe<?> recipe, List<IndexRecipeSlot> slots) {
        for (RecipeDisplay display : recipe.display()) {
            if (display instanceof FurnaceRecipeDisplay furnace) {
                List<ItemStack> fuel = choices(furnace.fuel(), 24);
                if (!fuel.isEmpty()) {
                    slots.add(new IndexRecipeSlot(IndexSlotRole.CATALYST, fuel, "Fuel"));
                }
                return;
            }
        }
    }

    private static IndexRecipeDisplayMetadata displayMetadata(Identifier recipeId, Recipe<?> recipe,
            Identifier categoryId, ItemStack output, ItemStack machine, List<IndexRecipeSlot> slots) {
        IndexRecipeDisplayMetadata concrete = concreteMetadata(recipeId, recipe, categoryId, output, machine);
        if (concrete.hasRenderableInputCells()) {
            return concrete;
        }
        for (RecipeDisplay display : recipe.display()) {
            IndexRecipeDisplayMetadata metadata = displayMetadata(recipeId, display, categoryId, output, machine);
            if (metadata.hasRenderableInputCells()) {
                return metadata;
            }
            IndexRecipeDisplayMetadata slotFallback = metadata.withFallbackInputCellsFromSlots(slots);
            if (slotFallback.hasRenderableInputCells()) {
                return slotFallback;
            }
        }
        return fallbackMetadata(recipeId, categoryId, output, machine, slots);
    }

    private static IndexRecipeDisplayMetadata displayMetadata(Identifier recipeId, RecipeDisplay display,
            Identifier categoryId, ItemStack output, ItemStack machine) {
        ItemStack resolvedOutput = output == null || output.isEmpty()
                ? display.result().resolveForFirstStack(ContextMap.EMPTY)
                : output.copy();
        ItemStack resolvedMachine = machine == null || machine.isEmpty()
                ? display.craftingStation().resolveForFirstStack(ContextMap.EMPTY)
                : machine.copy();
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            return new IndexRecipeDisplayMetadata(recipeId, IndexRecipeLayoutType.CRAFTING_SHAPED,
                    shaped.width(), shaped.height(), cells(shaped.ingredients()), resolvedMachine, resolvedOutput);
        }
        if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            List<List<ItemStack>> cells = cells(shapeless.ingredients());
            return new IndexRecipeDisplayMetadata(recipeId, IndexRecipeLayoutType.CRAFTING_SHAPELESS,
                    3, Math.max(1, Math.min(3, (cells.size() + 2) / 3)), cells, resolvedMachine, resolvedOutput);
        }
        if (display instanceof FurnaceRecipeDisplay furnace) {
            return new IndexRecipeDisplayMetadata(recipeId, IndexRecipeLayoutType.COOKING, 1, 1,
                    List.of(choices(furnace.ingredient(), 24)), resolvedMachine, resolvedOutput);
        }
        if (display instanceof StonecutterRecipeDisplay stonecutter) {
            return new IndexRecipeDisplayMetadata(recipeId, IndexRecipeLayoutType.STONECUTTING, 1, 1,
                    List.of(choices(stonecutter.input(), 24)), resolvedMachine, resolvedOutput);
        }
        if (display instanceof SmithingRecipeDisplay smithing) {
            return new IndexRecipeDisplayMetadata(recipeId, IndexRecipeLayoutType.SMITHING, 3, 1,
                    List.of(choices(smithing.template(), 24), choices(smithing.base(), 24),
                            choices(smithing.addition(), 24)),
                    resolvedMachine, resolvedOutput);
        }
        return IndexRecipeDisplayMetadata.generic(recipeId);
    }

    private static IndexRecipeDisplayMetadata displayMetadata(Identifier recipeId, RecipeDisplay display,
            Identifier categoryId, ItemStack output, ItemStack machine, List<IndexRecipeSlot> slots) {
        IndexRecipeDisplayMetadata metadata = displayMetadata(recipeId, display, categoryId, output, machine);
        return metadata.hasRenderableInputCells() ? metadata : metadata.withFallbackInputCellsFromSlots(slots);
    }

    private static IndexRecipeDisplayMetadata concreteMetadata(Identifier recipeId, Recipe<?> recipe,
            Identifier categoryId, ItemStack output, ItemStack machine) {
        if (recipe instanceof ShapedRecipe shaped) {
            return new IndexRecipeDisplayMetadata(recipeId, IndexRecipeLayoutType.CRAFTING_SHAPED,
                    shaped.getWidth(), shaped.getHeight(), cellsFromOptionalIngredients(shaped.getIngredients()),
                    machine, output);
        }
        if (recipe instanceof ShapelessRecipe shapeless) {
            List<List<ItemStack>> cells = cellsFromIngredients(shapeless.placementInfo().ingredients());
            return new IndexRecipeDisplayMetadata(recipeId, IndexRecipeLayoutType.CRAFTING_SHAPELESS,
                    3, Math.max(1, Math.min(3, (cells.size() + 2) / 3)), cells, machine, output);
        }
        if (recipe instanceof AbstractCookingRecipe cooking) {
            return new IndexRecipeDisplayMetadata(recipeId, IndexRecipeLayoutType.COOKING, 1, 1,
                    List.of(choices(cooking.input(), 24)), machine, output);
        }
        if (recipe instanceof StonecutterRecipe stonecutter) {
            return new IndexRecipeDisplayMetadata(recipeId, IndexRecipeLayoutType.STONECUTTING, 1, 1,
                    List.of(choices(stonecutter.input(), 24)), machine, output);
        }
        if (recipe instanceof SmithingRecipe smithing) {
            List<List<ItemStack>> cells = new ArrayList<>();
            cells.add(smithing.templateIngredient().map(ingredient -> choices(ingredient, 24)).orElse(List.of()));
            cells.add(choices(smithing.baseIngredient(), 24));
            cells.add(smithing.additionIngredient().map(ingredient -> choices(ingredient, 24)).orElse(List.of()));
            return new IndexRecipeDisplayMetadata(recipeId, IndexRecipeLayoutType.SMITHING, 3, 1, cells, machine, output);
        }
        return IndexRecipeDisplayMetadata.generic(recipeId);
    }

    private static IndexRecipeDisplayMetadata fallbackMetadata(Identifier recipeId, Identifier categoryId,
            ItemStack output, ItemStack machine, List<IndexRecipeSlot> slots) {
        if (CRAFTING.equals(categoryId)) {
            List<List<ItemStack>> cells = slots.stream()
                    .filter(slot -> slot.role() == IndexSlotRole.INPUT)
                    .map(IndexRecipeSlot::stacks)
                    .toList();
            return new IndexRecipeDisplayMetadata(recipeId, IndexRecipeLayoutType.CRAFTING_SHAPELESS,
                    3, Math.max(1, Math.min(3, (cells.size() + 2) / 3)), cells, machine, output);
        }
        if (SMELTING.equals(categoryId) || BLASTING.equals(categoryId) || SMOKING.equals(categoryId) || CAMPFIRE.equals(categoryId)) {
            return new IndexRecipeDisplayMetadata(recipeId, IndexRecipeLayoutType.COOKING, 1, 1,
                    firstRoleCells(slots, IndexSlotRole.INPUT), machine, output);
        }
        if (STONECUTTING.equals(categoryId)) {
            return new IndexRecipeDisplayMetadata(recipeId, IndexRecipeLayoutType.STONECUTTING, 1, 1,
                    firstRoleCells(slots, IndexSlotRole.INPUT), machine, output);
        }
        if (SMITHING.equals(categoryId)) {
            return new IndexRecipeDisplayMetadata(recipeId, IndexRecipeLayoutType.SMITHING, 3, 1,
                    firstRoleCells(slots, IndexSlotRole.INPUT), machine, output);
        }
        return IndexRecipeDisplayMetadata.generic(recipeId);
    }

    private static List<List<ItemStack>> cells(List<SlotDisplay> displays) {
        if (displays == null || displays.isEmpty()) {
            return List.of();
        }
        List<List<ItemStack>> cells = new ArrayList<>();
        for (SlotDisplay display : displays) {
            cells.add(choices(display, 24));
        }
        return List.copyOf(cells);
    }

    private static List<List<ItemStack>> cellsFromOptionalIngredients(List<Optional<Ingredient>> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return List.of();
        }
        List<List<ItemStack>> cells = new ArrayList<>();
        for (Optional<Ingredient> ingredient : ingredients) {
            cells.add(ingredient == null || ingredient.isEmpty() ? List.of() : choices(ingredient.get(), 24));
        }
        return List.copyOf(cells);
    }

    private static List<List<ItemStack>> cellsFromIngredients(List<Ingredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return List.of();
        }
        List<List<ItemStack>> cells = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            cells.add(choices(ingredient, 24));
        }
        return List.copyOf(cells);
    }

    private static List<ItemStack> choices(SlotDisplay display, int limit) {
        if (display == null) {
            return List.of();
        }
        return display.resolveForStacks(ContextMap.EMPTY).stream()
                .filter(stack -> stack != null && !stack.isEmpty())
                .map(ItemStack::copy)
                .limit(limit)
                .toList();
    }

    private static List<ItemStack> choices(Ingredient ingredient, int limit) {
        if (ingredient == null) {
            return List.of();
        }
        return ingredient.items()
                .map(Holder::value)
                .map(ItemStack::new)
                .filter(stack -> !stack.isEmpty())
                .limit(limit)
                .toList();
    }

    private static List<List<ItemStack>> firstRoleCells(List<IndexRecipeSlot> slots, IndexSlotRole role) {
        if (slots == null) {
            return List.of(List.of());
        }
        return slots.stream()
                .filter(slot -> slot.role() == role)
                .map(IndexRecipeSlot::stacks)
                .findFirst()
                .map(List::of)
                .orElse(List.of(List.of()));
    }

    private static void addIngredientSlot(List<IndexRecipeSlot> slots, Ingredient ingredient) {
        List<ItemStack> stacks = choices(ingredient, 24);
        if (!stacks.isEmpty()) {
            slots.add(IndexRecipeSlot.inputs(stacks));
        }
    }

    private static List<SlotDisplay> inputDisplays(RecipeDisplay display) {
        if (display instanceof ShapedCraftingRecipeDisplay shaped) {
            return shaped.ingredients();
        }
        if (display instanceof ShapelessCraftingRecipeDisplay shapeless) {
            return shapeless.ingredients();
        }
        if (display instanceof FurnaceRecipeDisplay furnace) {
            return List.of(furnace.ingredient());
        }
        if (display instanceof StonecutterRecipeDisplay stonecutter) {
            return List.of(stonecutter.input());
        }
        if (display instanceof SmithingRecipeDisplay smithing) {
            return List.of(smithing.template(), smithing.base(), smithing.addition());
        }
        return List.of();
    }

    private static final class IndexRecipeUiLike {
        private static ItemStack output(IndexRecipeView recipe) {
            for (IndexRecipeSlot slot : recipe.slots()) {
                if (slot.role() == IndexSlotRole.OUTPUT && !slot.stacks().isEmpty() && !slot.stacks().getFirst().isEmpty()) {
                    return slot.stacks().getFirst();
                }
            }
            return ItemStack.EMPTY;
        }
    }
}
