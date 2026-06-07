package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AutomationTaskHandlers {
    private static final List<AutomationTaskHandler> HANDLERS = new CopyOnWriteArrayList<>();

    private AutomationTaskHandlers() {
    }

    public static void register(AutomationTaskHandler handler) {
        if (handler != null && !HANDLERS.contains(handler)) {
            HANDLERS.add(handler);
        }
    }

    public static List<AutomationTaskHandler> handlersFor(MultiblockAutomationRecipe recipe) {
        if (recipe == null || HANDLERS.isEmpty()) {
            return List.of();
        }
        return HANDLERS.stream()
            .filter(handler -> {
                try {
                    return handler.canHandle(recipe);
                } catch (RuntimeException exception) {
                    return false;
                }
            })
            .toList();
    }
}
