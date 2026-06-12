package com.knoxhack.echoorbitalremnants.integration;

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
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.block.OrbitalMachineBlock.MachineKind;
import com.knoxhack.echoorbitalremnants.recipe.OrbitalProcessingRecipe;
import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModRecipes;
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

public enum OrbitalIndexProvider implements IIndexContentProvider {
    INSTANCE;

    private static final Identifier CATEGORY_MACHINES = id("machines");

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
        builder.addMachineLayouts(IndexMachineLayoutTemplates.representativeAll(recipes, "orbital_station"));
        return builder.snapshot();
    }

    public void register(IIndexRegistry registry) {
        registry.registerCategory(new IndexCategory(
                CATEGORY_MACHINES,
                "Orbital Machines",
                "Orbital route fabrication, life-support, and survey machines.",
                machineStack(MachineKind.ORBITAL_FABRICATOR),
                130,
                EchoOrbitalRemnants.MODID));
        for (MachineKind kind : MachineKind.values()) {
            registry.registerEntry(new IndexEntry(
                    id("machine/" + kind.getSerializedName()),
                    CATEGORY_MACHINES,
                    kind.displayName(),
                    "",
                    kind.processingRecipeDriven()
                            ? "Processes orbital materials through ECHO: Index recipe data."
                            : "Supports orbital route operations and terminal workflows.",
                    kind.displayName() + " is part of the Orbital Remnants machine network.",
                    machineStack(kind),
                    EchoOrbitalRemnants.MODID,
                    List.of("machine", "orbital", "space", kind.getSerializedName()),
                    IndexEntryState.VISIBLE,
                    List.of(),
                    List.of(itemId(machineStack(kind))),
                    List.of(),
                    10 + kind.ordinal()));
        }
    }

    public List<IndexRecipeCategory> recipeCategories(Player player) {
        List<IndexRecipeCategory> categories = new ArrayList<>();
        for (MachineKind kind : MachineKind.values()) {
            if (kind.processingRecipeDriven()) {
                categories.add(new IndexRecipeCategory(categoryId(kind), kind.displayName(),
                        machineStack(kind), 0xFF66E8FF, 300 + kind.ordinal()));
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
            if (!(holder.value() instanceof OrbitalProcessingRecipe recipe)) {
                continue;
            }
            ItemStack machine = machineStack(recipe.machine());
            ItemStack output = recipe.result();
            views.add(new IndexRecipeView(
                    holder.id().identifier(),
                    categoryId(recipe.machine()),
                    output.getHoverName().getString(),
                    machine,
                    List.of(
                            IndexRecipeSlot.inputs(stacks(recipe.ingredient(), 1)),
                            IndexRecipeSlot.machine(machine),
                            IndexRecipeSlot.output(output)),
                    List.of("Machine: " + recipe.machine().displayName(),
                            "Charge: " + recipe.chargeCost(),
                            "Duration: " + recipe.duration() + " ticks"),
                    recipe.duration(),
                    false,
                    EchoOrbitalRemnants.MODID));
        }
        return views;
    }

    private static List<RecipeHolder<?>> recipeHolders(Player player) {
        try {
            List<RecipeHolder<?>> holders = new ArrayList<>();
            for (RecipeHolder<?> holder : allRecipeHolders(player)) {
                if (holder.value().getType() == ModRecipes.ORBITAL_PROCESSING_TYPE.get()) {
                    holders.add(holder);
                }
            }
            return holders;
        } catch (RuntimeException ignored) {
            EchoOrbitalRemnants.LOGGER.debug("ECHO: Index could not enumerate Orbital recipes.");
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
            EchoOrbitalRemnants.LOGGER.debug("ECHO: Index could not enumerate client Orbital recipes.");
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

    private static Identifier categoryId(MachineKind kind) {
        return id("recipe/" + kind.getSerializedName());
    }

    private static ItemStack machineStack(MachineKind kind) {
        return new ItemStack(machineBlock(kind).get());
    }

    private static EchoBackendRegistryEntry<Block> machineBlock(MachineKind kind) {
        return switch (kind) {
            case ROCKET_ASSEMBLY_FRAME -> ModBlocks.ROCKET_ASSEMBLY_FRAME;
            case OXYGEN_COMPRESSOR -> ModBlocks.OXYGEN_COMPRESSOR;
            case FUEL_REFINERY -> ModBlocks.FUEL_REFINERY;
            case HEAT_SHIELD_FABRICATOR -> ModBlocks.HEAT_SHIELD_FABRICATOR;
            case ORBITAL_FABRICATOR -> ModBlocks.ORBITAL_FABRICATOR;
            case VACUUM_SMELTER -> ModBlocks.VACUUM_SMELTER;
            case SOLAR_RECLAIMER -> ModBlocks.SOLAR_RECLAIMER;
            case SUIT_CHARGING_STATION -> ModBlocks.SUIT_CHARGING_STATION;
            case SIGNAL_ANALYZER -> ModBlocks.SIGNAL_ANALYZER;
            case NAVIGATION_CONSOLE -> ModBlocks.NAVIGATION_CONSOLE;
            case STATION_LIFE_SUPPORT_CORE -> ModBlocks.STATION_LIFE_SUPPORT_CORE;
        };
    }

    private static Identifier itemId(ItemStack stack) {
        Identifier id = stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? Identifier.withDefaultNamespace("air") : id;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, sanitize(path));
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
