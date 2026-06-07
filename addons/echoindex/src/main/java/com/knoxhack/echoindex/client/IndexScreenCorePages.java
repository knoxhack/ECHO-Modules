package com.knoxhack.echoindex.client;

import com.knoxhack.echoindex.EchoIndex;
import net.minecraft.resources.Identifier;

public final class IndexScreenCorePages {
    public static final Identifier DASHBOARD = EchoIndex.id("index_dashboard");
    public static final Identifier ITEMS = EchoIndex.id("index_items");
    public static final Identifier RECIPES = EchoIndex.id("index_recipes");
    public static final Identifier USAGES = EchoIndex.id("index_usages");
    public static final Identifier MACHINES = EchoIndex.id("index_machines");
    public static final Identifier MODS = EchoIndex.id("index_mods");
    public static final Identifier FAVORITES = EchoIndex.id("index_favorites");
    public static final Identifier HISTORY = EchoIndex.id("index_history");
    public static final Identifier ITEM_DETAIL = EchoIndex.id("index_item_detail");
    public static final Identifier RECIPE_DETAIL = EchoIndex.id("index_recipe_detail");
    public static final Identifier MACHINE_DETAIL = EchoIndex.id("index_machine_detail");
    public static final Identifier SETTINGS = EchoIndex.id("index_settings");

    private IndexScreenCorePages() {
    }

    public static Identifier fromMode(String mode) {
        String clean = mode == null ? "" : mode.strip().toLowerCase(java.util.Locale.ROOT);
        return switch (clean) {
            case "items", "item" -> ITEMS;
            case "recipes", "recipe" -> RECIPES;
            case "usages", "usage", "uses" -> USAGES;
            case "machines", "machine" -> MACHINES;
            case "mods", "mod" -> MODS;
            case "favorites", "favorite", "bookmarks", "bookmark" -> FAVORITES;
            case "history", "recent" -> HISTORY;
            case "settings", "filters" -> SETTINGS;
            case "item_detail" -> ITEM_DETAIL;
            case "recipe_detail" -> RECIPE_DETAIL;
            case "machine_detail" -> MACHINE_DETAIL;
            default -> DASHBOARD;
        };
    }

    public static String modeFor(Identifier pageId) {
        if (ITEMS.equals(pageId)) {
            return "Items";
        }
        if (RECIPES.equals(pageId)) {
            return "Recipes";
        }
        if (USAGES.equals(pageId)) {
            return "Usages";
        }
        if (MACHINES.equals(pageId)) {
            return "Machines";
        }
        if (MODS.equals(pageId)) {
            return "Mods";
        }
        if (FAVORITES.equals(pageId)) {
            return "Favorites";
        }
        if (HISTORY.equals(pageId)) {
            return "History";
        }
        if (SETTINGS.equals(pageId)) {
            return "Settings";
        }
        if (ITEM_DETAIL.equals(pageId)) {
            return "Item Detail";
        }
        if (RECIPE_DETAIL.equals(pageId)) {
            return "Recipe Detail";
        }
        if (MACHINE_DETAIL.equals(pageId)) {
            return "Machine Detail";
        }
        return "Overview";
    }
}
