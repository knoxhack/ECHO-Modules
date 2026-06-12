package com.knoxhack.echoindex.command;

import com.knoxhack.echo.adaptercore.EchoBackendCommandEventBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.index.IndexCategory;
import com.echoplatform.echocore.api.index.IndexEntry;
import com.echoplatform.echocore.api.index.IndexRecipeView;
import com.knoxhack.echoindex.Config;
import com.knoxhack.echoindex.EchoIndex;
import com.knoxhack.echoindex.service.IndexDiscoveryStore;
import com.knoxhack.echoindex.service.IndexIngredientNeed;
import com.knoxhack.echoindex.service.IndexRecipePlan;
import com.knoxhack.echoindex.service.IndexRecipePlanner;
import com.knoxhack.echoindex.service.IndexRecipeProviderStats;
import com.knoxhack.echoindex.service.IndexRecipeSnapshot;
import com.knoxhack.echoindex.service.IndexRecipeSourceKind;
import com.knoxhack.echoindex.service.IndexService;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.Item;

public final class IndexCommands {
    private IndexCommands() {
    }

    public static void register() {
        EchoBackendLifecycleBridge.registerGameEventHandler(IndexCommands::onRegisterCommands);
    }

    public static void onRegisterCommands(Object event) {
        var dispatcher = EchoBackendCommandEventBridge.dispatcher(event);
        if (dispatcher == null) {
            return;
        }
        dispatcher.register(Commands.literal("echoindex")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("categories").executes(ctx -> categories(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("recipes")
                        .executes(ctx -> recipes(ctx.getSource().getPlayerOrException()))
                        .then(Commands.literal("item")
                                .then(Commands.argument("item", StringArgumentType.string())
                                        .executes(ctx -> recipeItem(ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "item"))))))
                .then(Commands.literal("plan")
                        .then(Commands.argument("recipe", StringArgumentType.string())
                                .executes(ctx -> plan(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "recipe")))))
                .then(Commands.literal("trace")
                        .then(Commands.argument("item", StringArgumentType.string())
                                .executes(ctx -> trace(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "item")))))
                .then(Commands.literal("doctor").executes(ctx -> doctor(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("unlock")
                        .then(Commands.argument("entry", StringArgumentType.string())
                                .executes(ctx -> unlock(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "entry")))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("entry", StringArgumentType.string())
                                .executes(ctx -> reset(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "entry")))))
                .then(Commands.literal("validate").executes(ctx -> validate(ctx.getSource().getPlayerOrException()))));
    }

    private static int list(ServerPlayer player) {
        int recipeCategoryCount = IndexService.INSTANCE.recipeCategories(player).size();
        int recipeCount = IndexService.INSTANCE.recipes(player).size();
        tell(player, "ECHO Index // Categories " + IndexService.INSTANCE.categories(player).size()
                + ", entries " + IndexService.INSTANCE.entries(player).size()
                + ", recipe categories " + recipeCategoryCount
                + ", recipes " + recipeCount + ".", ChatFormatting.AQUA);
        return Command.SINGLE_SUCCESS;
    }

    private static int categories(ServerPlayer player) {
        for (IndexCategory category : IndexService.INSTANCE.categories(player)) {
            tell(player, " - " + category.id() + " | " + category.titleKey(), ChatFormatting.GRAY);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int recipes(ServerPlayer player) {
        IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshot(player);
        int catalogCount = IndexService.INSTANCE.catalogCount(player);
        tell(player, "ECHO Index // Recipe providers " + IndexService.INSTANCE.providerCount()
                + ", categories " + snapshot.categories().size()
                + ", recipes " + snapshot.recipes().size()
                + ", source cards " + snapshot.sourceCardCount()
                + ", source facts " + snapshot.sourceFactCount()
                + ", usage items " + snapshot.usageItemCount()
                + ", catalog " + catalogCount
                + ", warnings " + snapshot.warnings().size()
                + ", generation " + snapshot.generation() + ".", ChatFormatting.AQUA);
        tell(player, " - " + snapshot.healthLine(), ChatFormatting.DARK_AQUA);
        for (IndexRecipeProviderStats stats : snapshot.providerStats()) {
            tell(player, " - " + stats.summaryLine(), stats.hasWarning() ? ChatFormatting.YELLOW : ChatFormatting.GRAY);
        }
        if (!snapshot.warnings().isEmpty()) {
            int limit = Math.min(6, snapshot.warnings().size());
            for (int i = 0; i < limit; i++) {
                tell(player, " ! " + snapshot.warnings().get(i), ChatFormatting.YELLOW);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int doctor(ServerPlayer player) {
        IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshot(player);
        int itemCount = IndexService.INSTANCE.itemCatalog(player).size();
        tell(player, "ECHO Index Doctor // " + snapshot.healthLine() + ".", ChatFormatting.AQUA);
        tell(player, " - item cache " + itemCount
                + ", unfiltered matches " + IndexService.INSTANCE.filteredItemsUnbounded(player, "").size()
                + ", categories " + IndexService.INSTANCE.categories(player).size()
                + ", entries " + IndexService.INSTANCE.entries(player).size()
                + ", source providers " + IndexService.INSTANCE.sourceProviderCount()
                + ", source facts " + IndexService.INSTANCE.sourceFacts(player).size()
                + ", snapshot age " + snapshot.ageSeconds() + "s.", ChatFormatting.GRAY);
        if (snapshot.recipesStillLoading()) {
            tell(player, " - recipe snapshot appears to still be loading; reopen Index or use /reload if this persists.",
                    ChatFormatting.YELLOW);
        }
        if (snapshot.rawRecipeCount() > 0 && snapshot.adaptedRecipeCount() == 0) {
            tell(player, " - raw recipes are present but no recipe cards were adapted.", ChatFormatting.RED);
        }
        if (snapshot.providerErrored()) {
            tell(player, " - last provider error: " + snapshot.lastProviderError(), ChatFormatting.RED);
        }
        if (snapshot.warnings().isEmpty()) {
            tell(player, " - no recipe provider warnings.", ChatFormatting.GREEN);
        } else {
            for (String warning : snapshot.warnings().stream().limit(5).toList()) {
                tell(player, " ! " + warning, ChatFormatting.YELLOW);
            }
        }
        return snapshot.providerErrored() ? 0 : Command.SINGLE_SUCCESS;
    }

    private static int recipeItem(ServerPlayer player, String rawItem) {
        Identifier id = Identifier.tryParse(rawItem);
        if (id == null) {
            tell(player, "ECHO Index // Invalid item id: " + rawItem, ChatFormatting.RED);
            return 0;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            tell(player, "ECHO Index // Unknown item id: " + id, ChatFormatting.RED);
            return 0;
        }
        IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshot(player);
        List<IndexRecipeView> allRecipes = snapshot.recipesFor(item);
        List<IndexRecipeView> recipes = allRecipes.stream()
                .filter(recipe -> !IndexRecipeSourceKind.isSourceCard(recipe))
                .toList();
        List<IndexRecipeView> sources = allRecipes.stream()
                .filter(IndexRecipeSourceKind::isSourceCard)
                .toList();
        List<IndexRecipeView> uses = snapshot.usesFor(item);
        tell(player, "ECHO Index // " + id + " recipes " + recipes.size()
                + ", sources " + sources.size() + ", uses " + uses.size() + ".", ChatFormatting.AQUA);
        emitRecipeList(player, "Recipes", recipes);
        emitRecipeList(player, "Uses", uses);
        emitRecipeList(player, "Sources", sources);
        return Command.SINGLE_SUCCESS;
    }

    private static void emitRecipeList(ServerPlayer player, String label, List<IndexRecipeView> recipes) {
        if (recipes.isEmpty()) {
            tell(player, " - no " + label.toLowerCase() + ".", ChatFormatting.DARK_GRAY);
            return;
        }
        tell(player, " - " + label + ":", ChatFormatting.GRAY);
        int limit = Math.min(8, recipes.size());
        for (int i = 0; i < limit; i++) {
            IndexRecipeView recipe = recipes.get(i);
            String sourceKind = IndexRecipeSourceKind.isSourceCard(recipe)
                    ? " [" + sourceKindLabel(recipe) + "]"
                    : "";
            IndexRecipePlan plan = IndexRecipePlanner.plan(player, recipe);
            String readiness = plan.ready() ? "Ready"
                    : plan.missingCount() > 0 ? "Missing " + plan.missingCount()
                    : "Plan Only";
            tell(player, "   " + recipe.categoryId() + " | " + recipe.id()
                    + sourceKind + " | " + readiness + " | " + recipe.title(), ChatFormatting.GRAY);
        }
        if (recipes.size() > limit) {
            tell(player, "   ... " + (recipes.size() - limit) + " more " + label.toLowerCase() + ".", ChatFormatting.DARK_GRAY);
        }
    }

    private static int plan(ServerPlayer player, String rawRecipe) {
        Identifier id = Identifier.tryParse(rawRecipe);
        if (id == null) {
            tell(player, "ECHO Index // Invalid recipe id: " + rawRecipe, ChatFormatting.RED);
            return 0;
        }
        IndexRecipeView recipe = IndexService.INSTANCE.recipeSnapshot(player).byId().get(id);
        if (recipe == null) {
            tell(player, "ECHO Index // Unknown indexed recipe: " + id, ChatFormatting.RED);
            return 0;
        }
        IndexRecipePlan plan = IndexRecipePlanner.plan(player, recipe);
        tell(player, "ECHO Index // Plan " + id + " | " + recipe.title(), ChatFormatting.AQUA);
        tell(player, " - category " + recipe.categoryId()
                + ", state " + plan.state().label()
                + ", actionable " + plan.canTransfer()
                + ", pinned " + plan.pinned() + ".", ChatFormatting.GRAY);
        if (!plan.transferBlocker().isBlank()) {
            tell(player, " - blocker: " + plan.transferBlocker(), ChatFormatting.YELLOW);
        }
        if (plan.needs().isEmpty()) {
            tell(player, " - no ingredient needs indexed.", ChatFormatting.DARK_GRAY);
            return Command.SINGLE_SUCCESS;
        }
        for (IndexIngredientNeed need : plan.needs().stream().limit(10).toList()) {
            String choice = need.selected().isEmpty()
                    ? need.label()
                    : BuiltInRegistries.ITEM.getKey(need.selected().getItem()).toString();
            ChatFormatting color = need.satisfied() ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
            tell(player, " - " + choice + ": required " + need.required()
                    + ", available " + need.available()
                    + ", missing " + need.missing() + ".", color);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int trace(ServerPlayer player, String rawItem) {
        Identifier id = Identifier.tryParse(rawItem);
        if (id == null) {
            tell(player, "ECHO Index // Invalid item id: " + rawItem, ChatFormatting.RED);
            return 0;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            tell(player, "ECHO Index // Unknown item id: " + id, ChatFormatting.RED);
            return 0;
        }
        IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshot(player);
        List<IndexRecipeView> outputViews = snapshot.recipesFor(item);
        List<IndexRecipeView> recipes = outputViews.stream()
                .filter(recipe -> !IndexRecipeSourceKind.isSourceCard(recipe))
                .toList();
        List<IndexRecipeView> sources = outputViews.stream()
                .filter(IndexRecipeSourceKind::isSourceCard)
                .toList();
        List<IndexRecipeView> uses = snapshot.usesFor(item);
        tell(player, "ECHO Index Trace // " + id + " recipes " + recipes.size()
                + ", uses " + uses.size() + ", sources " + sources.size()
                + ", generation " + snapshot.generation() + ".", ChatFormatting.AQUA);
        if (recipes.isEmpty()) {
            tell(player, " - no craft/machine recipe outputs for this item.", ChatFormatting.DARK_GRAY);
        }
        int limit = Math.min(4, recipes.size());
        for (int i = 0; i < limit; i++) {
            IndexRecipeView recipe = recipes.get(i);
            IndexRecipePlan plan = IndexRecipePlanner.plan(player, recipe);
            tell(player, " - " + recipe.id() + " | " + recipe.title()
                    + " | " + IndexRecipeSourceKind.of(recipe).label()
                    + " | " + (plan.missingCount() > 0 ? "missing " + plan.missingCount() : plan.state().label()),
                    plan.missingCount() > 0 ? ChatFormatting.YELLOW : ChatFormatting.GRAY);
            emitTraceNeeds(player, snapshot, plan);
        }
        if (!sources.isEmpty()) {
            tell(player, " - source cards available: " + sources.size() + ".", ChatFormatting.GRAY);
            for (IndexRecipeView source : sources.stream().limit(4).toList()) {
                tell(player, "   source " + source.id() + " [" + sourceKindLabel(source) + "] " + source.title(),
                        ChatFormatting.DARK_AQUA);
            }
        } else {
            tell(player, " - no source cards for this item.", ChatFormatting.DARK_GRAY);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void emitTraceNeeds(ServerPlayer player, IndexRecipeSnapshot snapshot, IndexRecipePlan plan) {
        List<IndexIngredientNeed> missing = plan.needs().stream()
                .filter(need -> need.missing() > 0 && !need.selected().isEmpty())
                .limit(6)
                .toList();
        if (missing.isEmpty()) {
            tell(player, "   dependencies: none missing.", ChatFormatting.GREEN);
            return;
        }
        for (IndexIngredientNeed need : missing) {
            Item item = need.selected().getItem();
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            List<IndexRecipeView> outputs = snapshot.recipesFor(item);
            long sourceCount = outputs.stream().filter(IndexRecipeSourceKind::isSourceCard).count();
            long recipeCount = outputs.size() - sourceCount;
            tell(player, "   need " + id + ": required " + need.required()
                    + ", available " + need.available()
                    + ", missing " + need.missing()
                    + ", recipes " + recipeCount
                    + ", sources " + sourceCount
                    + ", uses " + snapshot.usesFor(item).size() + ".",
                    ChatFormatting.YELLOW);
        }
    }

    private static int unlock(ServerPlayer player, String rawEntry) {
        if (!Config.DEBUG_COMMANDS.get()) {
            tell(player, "ECHO Index // Debug commands are disabled in config.", ChatFormatting.RED);
            return 0;
        }
        Identifier id = Identifier.tryParse(rawEntry);
        if (id == null) {
            tell(player, "ECHO Index // Invalid entry id: " + rawEntry, ChatFormatting.RED);
            return 0;
        }
        IndexDiscoveryStore.INSTANCE.discover(player, id);
        tell(player, "ECHO Index // Entry unlocked: " + id + ".", ChatFormatting.GREEN);
        return Command.SINGLE_SUCCESS;
    }

    private static int reset(ServerPlayer player, String rawEntry) {
        if (!Config.DEBUG_COMMANDS.get()) {
            tell(player, "ECHO Index // Debug commands are disabled in config.", ChatFormatting.RED);
            return 0;
        }
        Identifier id = Identifier.tryParse(rawEntry);
        if (id == null) {
            tell(player, "ECHO Index // Invalid entry id: " + rawEntry, ChatFormatting.RED);
            return 0;
        }
        IndexDiscoveryStore.INSTANCE.reset(player, id);
        tell(player, "ECHO Index // Entry state reset: " + id + ".", ChatFormatting.YELLOW);
        return Command.SINGLE_SUCCESS;
    }

    private static int validate(ServerPlayer player) {
        List<String> warnings = new ArrayList<>(IndexService.INSTANCE.validateContent(player));
        IndexRecipeSnapshot snapshot = IndexService.INSTANCE.recipeSnapshot(player);
        Identifier hazmatHelmetId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "hazmat_helmet");
        BuiltInRegistries.ITEM.getOptional(hazmatHelmetId).ifPresent(item -> {
            if (snapshot.rawRecipeCount() > 0
                    && snapshot.recipesFor(item).stream().noneMatch(recipe -> !IndexRecipeSourceKind.isSourceCard(recipe))) {
                warnings.add("Smoke-test item " + hazmatHelmetId + " has no recipe cards despite raw recipes being loaded.");
            }
        });
        if (warnings.isEmpty()) {
            tell(player, "ECHO Index // Validation passed.", ChatFormatting.GREEN);
        } else {
            tell(player, "ECHO Index // Validation found " + warnings.size() + " warning(s): "
                    + String.join(", ", warnings), ChatFormatting.YELLOW);
        }
        return warnings.isEmpty() ? Command.SINGLE_SUCCESS : 0;
    }

    private static void tell(ServerPlayer player, String message, ChatFormatting color) {
        player.sendSystemMessage(Component.literal(message).withStyle(color));
    }

    private static String sourceKindLabel(IndexRecipeView recipe) {
        return IndexRecipeSourceKind.of(recipe).label();
    }
}
