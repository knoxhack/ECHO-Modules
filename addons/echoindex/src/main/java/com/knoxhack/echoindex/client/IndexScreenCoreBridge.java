package com.knoxhack.echoindex.client;

import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.EchoScreens;
import com.knoxhack.echoindex.Config;
import com.knoxhack.echoindex.EchoIndex;
import com.knoxhack.echoindex.EchoIndexClient;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class IndexScreenCoreBridge {
    private static boolean registered;

    private IndexScreenCoreBridge() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        IndexDataProviders.register();
        IndexActions.register();
        registerStyles();
        EchoIndex.LOGGER.info("ECHO: Index registered ScreenCore pages, providers, actions, and styles.");
    }

    public static boolean open() {
        register();
        if (!Config.UI_USE_SCREENCORE.get()) {
            return false;
        }
        if (Minecraft.getInstance().player == null) {
            return false;
        }
        Identifier page = IndexUiState.INSTANCE.startPage();
        IndexUiState.INSTANCE.setCurrentPage(page);
        try {
            EchoNativeLoadStatus lifecycleStatus = publishOpenLifecycle(
                    "index.screencore.open", page, "index_screencore_open", Map.of());
            if (EchoIndexClient.nativeLoaderClientActiveForScreens()
                    && lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
                return false;
            }
            boolean opened = EchoScreens.open(page, IndexDataProviders.context());
            if (!opened) {
                EchoIndex.LOGGER.warn("ECHO: ScreenCore did not open page {}; legacy Index fallback will be used.", page);
            }
            return opened;
        } catch (RuntimeException exception) {
            EchoIndex.LOGGER.warn("ECHO: ScreenCore Index failed; legacy Index fallback will be used.", exception);
            return false;
        }
    }

    public static boolean openMode(String mode) {
        register();
        if (!Config.UI_USE_SCREENCORE.get()) {
            return false;
        }
        if (Minecraft.getInstance().player == null) {
            return false;
        }
        Identifier page = IndexScreenCorePages.fromMode(mode);
        IndexUiState.INSTANCE.setCurrentPage(page);
        try {
            EchoNativeLoadStatus lifecycleStatus = publishOpenLifecycle("index.screencore.open_mode", page, "index_screencore_open_mode", Map.of(
                    "mode", mode == null ? "" : mode
            ));
            if (EchoIndexClient.nativeLoaderClientActiveForScreens()
                    && lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
                return false;
            }
            boolean opened = EchoScreens.open(page, IndexDataProviders.context());
            if (!opened) {
                EchoIndex.LOGGER.warn("ECHO: ScreenCore did not open page {}; legacy Index fallback will be used.", page);
            }
            return opened;
        } catch (RuntimeException exception) {
            EchoIndex.LOGGER.warn("ECHO: ScreenCore Index page {} failed; legacy Index fallback will be used.", page, exception);
            return false;
        }
    }

    public static boolean openItem(Identifier itemId) {
        register();
        if (itemId != null) {
            IndexUiState.INSTANCE.selection().selectItem(itemId);
        }
        IndexUiState.INSTANCE.setCurrentPage(IndexScreenCorePages.ITEM_DETAIL);
        EchoNativeLoadStatus lifecycleStatus = publishOpenLifecycle("index.screencore.open_item", IndexScreenCorePages.ITEM_DETAIL,
                "index_screencore_open_item", Map.of("itemId", itemId == null ? "" : itemId.toString()));
        if (EchoIndexClient.nativeLoaderClientActiveForScreens()
                && lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
            return false;
        }
        return EchoScreens.open(IndexScreenCorePages.ITEM_DETAIL, IndexDataProviders.context());
    }

    public static boolean openRecipe(Identifier recipeId) {
        register();
        if (recipeId != null) {
            IndexUiState.INSTANCE.selection().selectRecipe(recipeId);
        }
        IndexUiState.INSTANCE.setCurrentPage(IndexScreenCorePages.RECIPE_DETAIL);
        EchoNativeLoadStatus lifecycleStatus = publishOpenLifecycle("index.screencore.open_recipe", IndexScreenCorePages.RECIPE_DETAIL,
                "index_screencore_open_recipe", Map.of("recipeId", recipeId == null ? "" : recipeId.toString()));
        if (EchoIndexClient.nativeLoaderClientActiveForScreens()
                && lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
            return false;
        }
        return EchoScreens.open(IndexScreenCorePages.RECIPE_DETAIL, IndexDataProviders.context());
    }

    public static void invalidate() {
        IndexRecipeCache.invalidate();
        EchoScreens.invalidateData();
    }

    private static void registerStyles() {
        for (Identifier style : List.of(
                EchoIndex.id("index_cyberglass"),
                EchoIndex.id("index_components"),
                EchoIndex.id("index_items"),
                EchoIndex.id("index_recipes"),
                EchoIndex.id("index_machines"))) {
            EchoScreenRegistry.registerStyleSheet(style);
        }
    }

    private static EchoNativeLoadStatus publishOpenLifecycle(
            String actionId,
            Identifier page,
            String transitionSource,
            Map<String, Object> metadata
    ) {
        Map<String, Object> event = new java.util.LinkedHashMap<>();
        event.put("targetScreenClass", IndexScreenCoreBridge.class.getName());
        event.put("transitionSource", transitionSource == null ? "" : transitionSource);
        event.put("screenBridge", "echoscreencore");
        event.put("pageId", page == null ? "" : page.toString());
        if (metadata != null) {
            event.putAll(metadata);
        }
        return EchoIndexClient.publishNativeScreenLifecycle(
                "open",
                actionId,
                IndexScreenCoreBridge.class.getName(),
                Map.copyOf(event));
    }
}
