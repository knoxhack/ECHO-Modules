package com.knoxhack.echoindex.client;

import com.knoxhack.echoindex.Config;
import java.util.Locale;

public final class IndexFilterState {
    private String mod = "";
    private String category = "all";
    private String recipeType = "all";
    private String machine = "";
    private String status = "all";
    private String sort = "name";
    private String grouping = Config.UI_GROUP_BY_MOD.get() ? "mod" : "flat";
    private boolean favoritesOnly;
    private boolean showLocked = Config.UI_SHOW_LOCKED_ITEMS.get();
    private long revision;

    public String mod() {
        return mod;
    }

    public String category() {
        return category;
    }

    public String recipeType() {
        return recipeType;
    }

    public String machine() {
        return machine;
    }

    public String status() {
        return status;
    }

    public String sort() {
        return sort;
    }

    public String grouping() {
        return grouping;
    }

    public boolean favoritesOnly() {
        return favoritesOnly;
    }

    public boolean showLocked() {
        return showLocked;
    }

    public long revision() {
        return revision;
    }

    public void set(String key, String value) {
        String cleanKey = clean(key);
        String cleanValue = value == null ? "" : value.strip();
        switch (cleanKey) {
            case "mod" -> setMod(cleanValue);
            case "category" -> category = cleanValue.isBlank() ? "all" : cleanValue.toLowerCase(Locale.ROOT);
            case "recipe_type", "recipetype", "type" -> recipeType = cleanValue.isBlank() ? "all" : cleanValue;
            case "machine" -> machine = cleanValue;
            case "status" -> status = cleanValue.isBlank() ? "all" : cleanValue.toLowerCase(Locale.ROOT);
            case "sort" -> sort = cleanValue.isBlank() ? "name" : cleanValue.toLowerCase(Locale.ROOT);
            case "grouping", "group" -> grouping = cleanValue.isBlank() ? "flat" : cleanValue.toLowerCase(Locale.ROOT);
            case "favorites", "favorite" -> favoritesOnly = Boolean.parseBoolean(cleanValue)
                    || "favorites".equalsIgnoreCase(cleanValue)
                    || "only".equalsIgnoreCase(cleanValue);
            case "show_locked", "locked" -> showLocked = cleanValue.isBlank() || Boolean.parseBoolean(cleanValue);
            default -> {
                return;
            }
        }
        revision++;
    }

    public void setMod(String value) {
        mod = value == null || "all".equalsIgnoreCase(value) ? "" : value.strip().toLowerCase(Locale.ROOT);
        revision++;
    }

    public void setSort(String value) {
        sort = value == null || value.isBlank() ? "name" : value.strip().toLowerCase(Locale.ROOT);
        revision++;
    }

    public void reset() {
        mod = "";
        category = "all";
        recipeType = "all";
        machine = "";
        status = "all";
        sort = "name";
        grouping = Config.UI_GROUP_BY_MOD.get() ? "mod" : "flat";
        favoritesOnly = false;
        showLocked = Config.UI_SHOW_LOCKED_ITEMS.get();
        revision++;
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
