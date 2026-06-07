package com.knoxhack.echowiki.client;

import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.action.EchoActionContext;
import com.knoxhack.echowiki.EchoWiki;
import com.knoxhack.echowiki.platform.WikiModuleAccess;
import java.lang.reflect.Method;
import net.minecraft.resources.Identifier;

public final class WikiActions {
    private WikiActions() {
    }

    public static void register() {
        register("wiki.open_page", WikiActions::openPage);
        register("wiki.open_article", WikiActions::openArticle);
        register("wiki.open_guide_book", WikiActions::openGuideBook);
        register("wiki.set_search", WikiActions::setSearch);
        register("wiki.set_category", WikiActions::setCategory);
        register("wiki.next_article_page", WikiActions::nextArticlePage);
        register("wiki.previous_article_page", WikiActions::previousArticlePage);
        register("wiki.next_guide_book_page", WikiActions::nextGuideBookPage);
        register("wiki.previous_guide_book_page", WikiActions::previousGuideBookPage);
        register("wiki.open_related", WikiActions::openRelated);
        register("wiki.clear_filters", WikiActions::clearFilters);
        register("wiki.select_region", WikiActions::selectRegion);
        register("wiki.select_hazard", WikiActions::selectHazard);
        register("wiki.select_mission", WikiActions::selectMission);
        register("wiki.select_faction", WikiActions::selectFaction);
        register("wiki.select_discovery", WikiActions::selectDiscovery);
    }

    private static void register(String id, com.knoxhack.echoscreencore.api.action.EchoAction action) {
        EchoScreenRegistry.registerAction(id, action);
    }

    private static boolean openPage(EchoActionContext context) {
        Identifier page = pageFrom(context.param("page").isBlank() ? context.actionValue() : context.param("page"));
        WikiUiState.INSTANCE.currentPage(page);
        WikiScreenCoreBridge.invalidate();
        return context.open(page);
    }

    private static boolean openArticle(EchoActionContext context) {
        Identifier articleId = idFrom(context.actionValue());
        if (articleId == null) {
            return false;
        }
        WikiUiState.INSTANCE.selectedArticle(articleId);
        WikiUiState.INSTANCE.currentPage(WikiScreenCorePages.ARTICLE_DETAIL);
        WikiScreenCoreBridge.invalidate();
        return context.open(WikiScreenCorePages.ARTICLE_DETAIL);
    }

    private static boolean openGuideBook(EchoActionContext context) {
        Identifier guideId = idFrom(context.actionValue());
        if (!WikiUiState.INSTANCE.selectVisibleGuideBook(guideId)) {
            WikiUiState.INSTANCE.selectFirstVisibleGuideBook();
            WikiUiState.INSTANCE.currentPage(WikiScreenCorePages.GUIDE_BOOKS);
            WikiScreenCoreBridge.invalidate();
            return context.open(WikiScreenCorePages.GUIDE_BOOKS);
        }
        WikiUiState.INSTANCE.currentPage(WikiScreenCorePages.GUIDE_BOOK_READER);
        WikiScreenCoreBridge.invalidate();
        return context.open(WikiScreenCorePages.GUIDE_BOOK_READER);
    }

    private static boolean setSearch(EchoActionContext context) {
        WikiUiState.INSTANCE.searchQuery(context.actionValue());
        WikiScreenCoreBridge.invalidate();
        return true;
    }

    private static boolean nextArticlePage(EchoActionContext context) {
        WikiUiState.INSTANCE.nextArticlePage();
        WikiScreenCoreBridge.invalidate();
        return true;
    }

    private static boolean previousArticlePage(EchoActionContext context) {
        WikiUiState.INSTANCE.previousArticlePage();
        WikiScreenCoreBridge.invalidate();
        return true;
    }

    private static boolean nextGuideBookPage(EchoActionContext context) {
        WikiUiState.INSTANCE.nextGuideBookPage();
        WikiScreenCoreBridge.invalidate();
        return true;
    }

    private static boolean previousGuideBookPage(EchoActionContext context) {
        WikiUiState.INSTANCE.previousGuideBookPage();
        WikiScreenCoreBridge.invalidate();
        return true;
    }

    private static boolean setCategory(EchoActionContext context) {
        WikiUiState.INSTANCE.category(context.actionValue());
        WikiScreenCoreBridge.invalidate();
        return true;
    }

    private static boolean clearFilters(EchoActionContext context) {
        WikiUiState.INSTANCE.clearFilters();
        WikiScreenCoreBridge.invalidate();
        return true;
    }

    private static boolean openRelated(EchoActionContext context) {
        String kind = context.param("kind");
        Identifier id = idFrom(context.actionValue());
        if (id == null) {
            return false;
        }
        return switch (kind == null ? "" : kind.strip().toLowerCase(java.util.Locale.ROOT)) {
            case "article" -> openArticle(context);
            case "guide_book", "guidebook", "guide" -> openGuideBook(context);
            case "region" -> selectAndOpen(context, id, WikiScreenCorePages.REGIONS, () -> WikiUiState.INSTANCE.selectedRegion(id));
            case "hazard" -> selectAndOpen(context, id, WikiScreenCorePages.HAZARDS, () -> WikiUiState.INSTANCE.selectedHazard(id));
            case "mission" -> selectAndOpen(context, id, WikiScreenCorePages.MISSIONS, () -> WikiUiState.INSTANCE.selectedMission(id));
            case "faction" -> selectAndOpen(context, id, WikiScreenCorePages.FACTIONS, () -> WikiUiState.INSTANCE.selectedFaction(id));
            case "item" -> openIndexBridge("openItem", id);
            case "recipe" -> openIndexBridge("openRecipe", id);
            default -> true;
        };
    }

    private static boolean selectRegion(EchoActionContext context) {
        Identifier id = idFrom(context.actionValue());
        if (id == null) {
            return false;
        }
        WikiUiState.INSTANCE.selectedRegion(id);
        WikiScreenCoreBridge.invalidate();
        return true;
    }

    private static boolean selectHazard(EchoActionContext context) {
        Identifier id = idFrom(context.actionValue());
        if (id == null) {
            return false;
        }
        WikiUiState.INSTANCE.selectedHazard(id);
        WikiScreenCoreBridge.invalidate();
        return true;
    }

    private static boolean selectMission(EchoActionContext context) {
        Identifier id = idFrom(context.actionValue());
        if (id == null) {
            return false;
        }
        WikiUiState.INSTANCE.selectedMission(id);
        WikiScreenCoreBridge.invalidate();
        return true;
    }

    private static boolean selectFaction(EchoActionContext context) {
        Identifier id = idFrom(context.actionValue());
        if (id == null) {
            return false;
        }
        WikiUiState.INSTANCE.selectedFaction(id);
        WikiScreenCoreBridge.invalidate();
        return true;
    }

    private static boolean selectDiscovery(EchoActionContext context) {
        Identifier id = idFrom(context.actionValue());
        if (id == null) {
            return false;
        }
        WikiUiState.INSTANCE.selectedDiscovery(id);
        WikiScreenCoreBridge.invalidate();
        return true;
    }

    private static boolean selectAndOpen(EchoActionContext context, Identifier id, Identifier page, Runnable selector) {
        selector.run();
        WikiUiState.INSTANCE.currentPage(page);
        WikiScreenCoreBridge.invalidate();
        return context.open(page);
    }

    private static boolean openIndexBridge(String methodName, Identifier id) {
        if (!WikiModuleAccess.isLoaded("echoindex")) {
            return true;
        }
        try {
            Class<?> bridge = Class.forName("com.knoxhack.echoindex.client.IndexScreenCoreBridge");
            Method method = bridge.getMethod(methodName, Identifier.class);
            Object opened = method.invoke(null, id);
            return opened instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return false;
        }
    }

    private static Identifier pageFrom(String raw) {
        Identifier id = idFrom(raw);
        return id == null ? WikiScreenCorePages.DASHBOARD : id;
    }

    private static Identifier idFrom(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            String clean = raw.strip();
            return clean.contains(":")
                    ? Identifier.parse(clean)
                    : Identifier.fromNamespaceAndPath(EchoWiki.MODID, clean);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
