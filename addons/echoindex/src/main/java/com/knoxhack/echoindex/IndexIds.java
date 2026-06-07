package com.knoxhack.echoindex;

import net.minecraft.resources.Identifier;

public final class IndexIds {
    public static final Identifier PROVIDER_BUILTIN = id("provider/builtin");
    public static final Identifier PROVIDER_DATAPACK = id("provider/datapack");
    public static final Identifier PROVIDER_VANILLA_RECIPES = id("provider/vanilla_recipes");
    public static final Identifier PROVIDER_TERMINAL = id("provider/terminal_bridge");
    public static final Identifier PROVIDER_TERMINAL_IMPORT = id("provider/terminal_import");
    public static final Identifier CATEGORY_ITEMS = id("items");
    public static final Identifier CATEGORY_BLOCKS = id("blocks");
    public static final Identifier CATEGORY_MACHINES = id("machines");
    public static final Identifier CATEGORY_TOOLS = id("tools");
    public static final Identifier CATEGORY_COMBAT = id("combat");
    public static final Identifier CATEGORY_RECIPES = id("recipes");
    public static final Identifier CATEGORY_SOURCES = id("sources");
    public static final Identifier CATEGORY_TUTORIALS = id("tutorials");
    public static final Identifier CATEGORY_LORE = id("lore");
    public static final Identifier CATEGORY_RESEARCH = id("research");
    public static final Identifier CATEGORY_ROUTES = id("routes");
    public static final Identifier CATEGORY_HAZARDS = id("hazards");
    public static final Identifier CATEGORY_FACTIONS = id("factions");
    public static final Identifier CATEGORY_DIAGNOSTICS = id("diagnostics");
    public static final Identifier ENTRY_OVERVIEW = id("index_overview");

    private IndexIds() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoIndex.MODID, sanitize(path));
    }

    private static String sanitize(String path) {
        String clean = path == null ? "unknown" : path.trim().toLowerCase(java.util.Locale.ROOT);
        clean = clean.replace('\\', '/').replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
        while (clean.contains("//")) {
            clean = clean.replace("//", "/");
        }
        return clean.isBlank() ? "unknown" : clean;
    }
}
