package com.knoxhack.echomultiblockcore.api;

import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echocore.api.EchoCoreServices;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class AutomationRecipeRegistry {
    private static volatile Map<Identifier, MultiblockAutomationRecipe> recipes = Map.of();

    private AutomationRecipeRegistry() {
    }

    public static void replaceRecipes(Map<Identifier, MultiblockAutomationRecipe> loaded) {
        recipes = Map.copyOf(loaded == null ? Map.of() : loaded);
        EchoMultiblockCore.LOGGER.info("ECHO MultiblockCore loaded {} automation recipe(s).", recipes.size());
        EchoCoreServices.invalidateIndexRecipes("multiblock automation recipes changed");
    }

    public static Optional<MultiblockAutomationRecipe> byId(Identifier id) {
        return Optional.ofNullable(id == null ? null : recipes.get(id));
    }

    public static Optional<MultiblockAutomationRecipe> byCommandName(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String clean = raw.strip();
        Identifier id = clean.contains(":") ? Identifier.tryParse(clean) : EchoMultiblockCore.id(clean);
        return byId(id);
    }

    public static List<MultiblockAutomationRecipe> all() {
        return recipes.values().stream()
                .sorted(Comparator.comparing(recipe -> recipe.id().toString()))
                .toList();
    }

    public static Map<Identifier, MultiblockAutomationRecipe> snapshot() {
        return new LinkedHashMap<>(recipes);
    }
}
