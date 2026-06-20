package com.knoxhack.echoindex.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoindex.IndexIds;
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
        if (TerminalRecipeRegistry.providers().stream()
                .noneMatch(provider -> IndexIds.PROVIDER_TERMINAL.equals(provider.id()))) {
            TerminalRecipeRegistry.register(IndexTerminalRecipeProvider.INSTANCE);
        }
    }

    private static void invalidateRecipes() {
        IndexService.INSTANCE.invalidateRecipes("terminal recipe registry changed");
    }
}
