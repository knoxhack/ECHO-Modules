package com.knoxhack.echowiki.client;

import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.EchoScreens;
import com.knoxhack.echowiki.EchoWiki;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class WikiScreenCoreBridge {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private WikiScreenCoreBridge() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        WikiDataProviders.register();
        WikiActions.register();
        registerStyles();
        EchoWiki.LOGGER.info("ECHO: Wiki registered ScreenCore pages, providers, actions, and styles.");
    }

    public static boolean open() {
        register();
        if (Minecraft.getInstance().player == null) {
            return false;
        }
        WikiUiState.INSTANCE.currentPage(WikiScreenCorePages.DASHBOARD);
        return EchoScreens.open(WikiScreenCorePages.DASHBOARD, context());
    }

    public static boolean openArticle(Identifier articleId) {
        register();
        if (Minecraft.getInstance().player == null) {
            return false;
        }
        if (articleId != null) {
            WikiUiState.INSTANCE.selectedArticle(articleId);
        }
        WikiUiState.INSTANCE.currentPage(WikiScreenCorePages.ARTICLE_DETAIL);
        return EchoScreens.open(WikiScreenCorePages.ARTICLE_DETAIL, context());
    }

    public static boolean openGuideBook(Identifier guideBookId) {
        register();
        if (Minecraft.getInstance().player == null) {
            return false;
        }
        if (!WikiUiState.INSTANCE.selectVisibleGuideBook(guideBookId)) {
            WikiUiState.INSTANCE.selectFirstVisibleGuideBook();
            WikiUiState.INSTANCE.currentPage(WikiScreenCorePages.GUIDE_BOOKS);
            return EchoScreens.open(WikiScreenCorePages.GUIDE_BOOKS, context());
        }
        WikiUiState.INSTANCE.currentPage(WikiScreenCorePages.GUIDE_BOOK_READER);
        return EchoScreens.open(WikiScreenCorePages.GUIDE_BOOK_READER, context());
    }

    public static boolean openGuideBookLibrary() {
        register();
        if (Minecraft.getInstance().player == null) {
            return false;
        }
        WikiUiState.INSTANCE.selectFirstVisibleGuideBook();
        WikiUiState.INSTANCE.currentPage(WikiScreenCorePages.GUIDE_BOOKS);
        return EchoScreens.open(WikiScreenCorePages.GUIDE_BOOKS, context());
    }

    public static EchoDataContext context() {
        return EchoDataContext.empty()
                .missingPlaceholder("-")
                .provider("wiki", WikiDataProviders.PROVIDER)
                .put("wiki.activePageId", WikiUiState.INSTANCE.currentPage().toString());
    }

    public static void invalidate() {
        EchoScreens.invalidateData();
    }

    private static void registerStyles() {
        for (Identifier style : List.of(
                EchoWiki.id("wiki_cyberglass"),
                EchoWiki.id("wiki_components"),
                EchoWiki.id("wiki_lists"))) {
            EchoScreenRegistry.registerStyleSheet(style);
        }
    }
}
