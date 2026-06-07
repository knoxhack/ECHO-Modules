package com.knoxhack.echoindex.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echoindex.service.IndexService;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeRegistry;
import java.util.concurrent.atomic.AtomicBoolean;

public final class IndexTerminalCommonIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private IndexTerminalCommonIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        TerminalRecipeRegistry.addChangeListener(IndexTerminalCommonIntegration::invalidateRecipes);
        EchoCoreServices.registerIndexContentProvider(IndexTerminalImportRecipeProvider.INSTANCE);
        TerminalRecipeRegistry.register(IndexTerminalRecipeProvider.INSTANCE);
    }

    private static void invalidateRecipes() {
        IndexService.INSTANCE.invalidateRecipes("terminal recipe registry changed");
    }
}
