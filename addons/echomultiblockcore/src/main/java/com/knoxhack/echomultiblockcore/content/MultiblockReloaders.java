package com.knoxhack.echomultiblockcore.content;

import com.knoxhack.echomultiblockcore.EchoMultiblockCore;

public final class MultiblockReloaders {
    private MultiblockReloaders() {
    }

    public static void addServerReloadListeners(Object event) {
        addListener(event, "definitions", new MultiblockJsonReloadListener());
        addListener(event, "automation_recipes", new AutomationRecipeJsonReloadListener());
        addListener(event, "upgrades", new MultiblockUpgradeJsonReloadListener());
        addListener(event, "progression", new MultiblockProgressionJsonReloadListener());
    }

    private static void addListener(Object event, String id, Object listener) {
        if (event == null || listener == null) {
            return;
        }
        try {
            for (var method : event.getClass().getMethods()) {
                if ("addListener".equals(method.getName()) && method.getParameterCount() == 2) {
                    method.invoke(event, EchoMultiblockCore.id(id), listener);
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            EchoMultiblockCore.LOGGER.warn("ECHO MultiblockCore reload listener {} could not be registered.", id, exception);
        }
    }
}
