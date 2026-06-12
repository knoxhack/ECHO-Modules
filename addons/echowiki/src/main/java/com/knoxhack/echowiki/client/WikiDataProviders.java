package com.knoxhack.echowiki.client;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoDiscoveryState;
import com.echoplatform.echocore.api.EchoFactionDefinition;
import com.echoplatform.echocore.api.EchoResolvedDiscoveryEntry;
import com.echoplatform.echocore.api.WorldContextSnapshot;
import com.echoplatform.echocore.api.WorldHazardDefinition;
import com.echoplatform.echocore.api.WorldHazardSnapshot;
import com.echoplatform.echocore.api.WorldRegionDefinition;
import com.echoplatform.echocore.api.index.IndexContentSnapshot;
import com.echoplatform.echocore.api.index.IndexEntry;
import com.echoplatform.echocore.api.index.IndexMachineLayout;
import com.echoplatform.echocore.api.index.IndexRecipeView;
import com.echoplatform.echocore.api.mission.IMissionProgressView;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.EchoDataProvider;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echowiki.EchoWiki;
import com.knoxhack.echowiki.content.GuideBookDefinition;
import com.knoxhack.echowiki.content.GuideBookLabels;
import com.knoxhack.echowiki.content.GuideBookRegistry;
import com.knoxhack.echowiki.content.WikiArticleDefinition;
import com.knoxhack.echowiki.content.WikiArticleSection;
import com.knoxhack.echowiki.content.WikiCollectionDefinition;
import com.knoxhack.echowiki.content.WikiContentRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.knoxhack.echowiki.platform.WikiModuleAccess;

public final class WikiDataProviders {
    public static final EchoDataProvider PROVIDER = WikiDataProviders::resolve;
    private static final int DASHBOARD_PREVIEW_LIMIT = 4;
    private static volatile GuideBookDataCache guideBookDataCache;
    private static volatile DashboardDataCache dashboardDataCache;

    private WikiDataProviders() {
    }

    private static GuideBookDataCache guideBookDataCache() {
        GuideBookCacheKey key = GuideBookCacheKey.current();
        GuideBookDataCache cache = guideBookDataCache;
        if (cache != null && cache.key().equals(key)) {
            return cache;
        }
        synchronized (WikiDataProviders.class) {
            cache = guideBookDataCache;
            if (cache != null && cache.key().equals(key)) {
                return cache;
            }
            cache = buildGuideBookDataCache(key);
            guideBookDataCache = cache;
            return cache;
        }
    }

    private static GuideBookDataCache buildGuideBookDataCache(GuideBookCacheKey key) {
        List<Map<String, Object>> allRows = GuideBookRegistry.guideBooks().stream()
                .map(WikiDataProviders::guideBookRow)
                .toList();
        List<Map<String, Object>> visibleRows = filteredVisibleGuideBooks().stream()
                .map(WikiDataProviders::guideBookRow)
                .toList();
        return new GuideBookDataCache(
                key,
                allRows,
                visibleRows,
                page(visibleRows, state().guideBookPage(), WikiUiState.GUIDE_BOOK_PAGE_SIZE),
                pageInfo(visibleRows.size(), state().guideBookPage(), WikiUiState.GUIDE_BOOK_PAGE_SIZE),
                buildSelectedGuideBook(),
                buildSelectedGuideBookChapters(),
                buildSelectedGuideBookRelated());
    }

    private static List<GuideBookDefinition> filteredVisibleGuideBooks() {
        String query = normalized(state().searchQuery());
        String category = state().category();
        return GuideBookRegistry.visibleGuideBooks().stream()
                .filter(guide -> "All".equalsIgnoreCase(category) || guide.moduleId().equalsIgnoreCase(category)
                        || guide.requiredModId().equalsIgnoreCase(category))
                .filter(guide -> query.isBlank() || searchable(guide).contains(query))
                .toList();
    }

    public static void clearCachesForTests() {
        guideBookDataCache = null;
        dashboardDataCache = null;
    }

    private record GuideBookCacheKey(
            long contentRevision,
            long guideRevision,
            Identifier selectedGuideBook,
            String searchQuery,
            String category,
            int guideBookPage,
            boolean indexLoaded) {
        private static GuideBookCacheKey current() {
            return new GuideBookCacheKey(
                    WikiContentRegistry.revision(),
                    GuideBookRegistry.revision(),
                    state().selectedGuideBook(),
                    normalized(state().searchQuery()),
                    state().category(),
                    state().guideBookPage(),
                    WikiModuleAccess.isLoaded("echoindex"));
        }
    }

    private record GuideBookDataCache(
            GuideBookCacheKey key,
            List<Map<String, Object>> allRows,
            List<Map<String, Object>> visibleRows,
            List<Map<String, Object>> pagedVisibleRows,
            PageInfo pageInfo,
            Map<String, Object> selectedGuideBook,
            List<Map<String, Object>> chapters,
            List<Map<String, Object>> related) {
    }

    private static DashboardDataCache dashboardDataCache() {
        DashboardCacheKey key = DashboardCacheKey.current();
        DashboardDataCache cache = dashboardDataCache;
        if (cache != null && cache.key().equals(key)) {
            return cache;
        }
        synchronized (WikiDataProviders.class) {
            cache = dashboardDataCache;
            if (cache != null && cache.key().equals(key)) {
                return cache;
            }
            cache = new DashboardDataCache(key, buildDashboard());
            dashboardDataCache = cache;
            return cache;
        }
    }

    private record DashboardCacheKey(
            long contentRevision,
            long guideRevision,
            Identifier currentPage,
            String searchQuery,
            String category,
            boolean indexLoaded,
            boolean terminalLoaded) {
        private static DashboardCacheKey current() {
            return new DashboardCacheKey(
                    WikiContentRegistry.revision(),
                    GuideBookRegistry.revision(),
                    state().currentPage(),
                    normalized(state().searchQuery()),
                    state().category(),
                    WikiModuleAccess.isLoaded("echoindex"),
                    WikiModuleAccess.isLoaded("echoterminal"));
        }
    }

    private record DashboardDataCache(
            DashboardCacheKey key,
            Map<String, Object> dashboard) {
    }

    public static void register() {
        EchoScreenRegistry.registerDataProvider("wiki", PROVIDER);
        EchoScreenRegistry.registerDataProvider(EchoWiki.id("wiki"), PROVIDER);
    }

    private static Object resolve(EchoDataContext context, List<String> path) {
        if (path == null || path.isEmpty()) {
            return dashboard();
        }
        String key = String.join(".", path);
        return switch (key) {
            case "dashboard" -> dashboard();
            case "dashboard.worldContext" -> worldContextRows();
            case "dashboard.optionalAddons" -> optionalAddonRows();
            case "dashboard.collections" -> dashboardCollectionRows();
            case "dashboard.guideBooks" -> dashboardGuideBookRows();
            case "dashboard.articles" -> dashboardArticleRows();
            case "nav.sections" -> navSections();
            case "articles.all" -> articleRows(false);
            case "articles.visible" -> articleRows(true);
            case "articles.pageLabel" -> articlePageInfo().label();
            case "articles.hasPreviousPage" -> articlePageInfo().hasPrevious();
            case "articles.hasNextPage" -> articlePageInfo().hasNext();
            case "articles.previousPageDisabled" -> !articlePageInfo().hasPrevious();
            case "articles.nextPageDisabled" -> !articlePageInfo().hasNext();
            case "articles.selected", "article.selected" -> selectedArticle();
            case "article.sections", "article.blocks" -> selectedArticleBlocks();
            case "article.toc" -> selectedArticleToc();
            case "article.related" -> selectedArticleRelated();
            case "guideBooks.all" -> guideBookRows(false);
            case "guideBooks.visible" -> guideBookRows(true);
            case "guideBooks.pageLabel" -> guideBookPageInfo().label();
            case "guideBooks.hasPreviousPage" -> guideBookPageInfo().hasPrevious();
            case "guideBooks.hasNextPage" -> guideBookPageInfo().hasNext();
            case "guideBooks.previousPageDisabled" -> !guideBookPageInfo().hasPrevious();
            case "guideBooks.nextPageDisabled" -> !guideBookPageInfo().hasNext();
            case "guideBooks.emptyTitle" -> guideBookEmptyTitle();
            case "guideBooks.emptyBody" -> guideBookEmptyBody();
            case "guideBooks.selected", "guideBook.selected" -> selectedGuideBook();
            case "guideBook.chapters" -> selectedGuideBookChapters();
            case "guideBook.related" -> selectedGuideBookRelated();
            case "collections.visible" -> collectionRows();
            case "filters.categories" -> categoryRows();
            case "filters.category" -> state().category();
            case "search.query" -> state().searchQuery();
            case "regions.visible" -> regionRows();
            case "regions.selected", "region.selected" -> selectedRegion();
            case "hazards.visible" -> hazardRows();
            case "hazards.active" -> activeHazards();
            case "hazards.selected", "hazard.selected" -> selectedHazard();
            case "missions.chapters" -> missionChapterRows();
            case "missions.visible" -> missionRows();
            case "missions.selected", "mission.selected" -> selectedMission();
            case "machines.visible" -> machineRows();
            case "factions.visible" -> factionRows();
            case "factions.selected", "faction.selected" -> selectedFaction();
            case "discoveries.visible" -> discoveryRows();
            case "discoveries.selected", "discovery.selected" -> selectedDiscovery();
            case "settings", "debug.stats" -> settings();
            case "debug.warnings" -> WikiContentRegistry.warnings().stream()
                    .map(warning -> row("label", warning, "status", "warning"))
                    .toList();
            default -> resolveNested(key);
        };
    }

    private static Object resolveNested(String key) {
        return nested(key, "dashboard.", dashboard())
                .orElseGet(() -> nested(key, "article.selected.", selectedArticle())
                        .orElseGet(() -> nested(key, "guideBook.selected.", selectedGuideBook())
                                .orElseGet(() -> nested(key, "regions.selected.", selectedRegion())
                                        .orElseGet(() -> nested(key, "hazards.selected.", selectedHazard())
                                                .orElseGet(() -> nested(key, "missions.selected.", selectedMission())
                                                        .orElseGet(() -> nested(key, "factions.selected.", selectedFaction())
                                                                .orElseGet(() -> nested(key, "discoveries.selected.", selectedDiscovery())
                                                                        .orElseGet(() -> nested(key, "settings.", settings()).orElse("")))))))));
    }

    private static java.util.Optional<Object> nested(String key, String prefix, Map<String, Object> map) {
        if (!key.startsWith(prefix)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(map.getOrDefault(key.substring(prefix.length()), ""));
    }

    private static WikiUiState state() {
        return WikiUiState.INSTANCE;
    }

    private static Player player() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft == null ? null : minecraft.player;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static Map<String, Object> dashboard() {
        return dashboardDataCache().dashboard();
    }

    private static Map<String, Object> buildDashboard() {
        int articleCount = WikiContentRegistry.articles().size();
        int visibleArticleCount = visibleArticleCount();
        int guideBookCount = GuideBookRegistry.guideBooks().size();
        int visibleGuideBookCount = visibleGuideBookCount();
        return row(
                "title", "ECHO Survival Codex",
                "subtitle", "Articles, discoveries, regions, hazards, missions, machines, factions, and addon field notes.",
                "mode", WikiScreenCorePages.titleFor(state().currentPage()),
                "query", state().searchQuery().isBlank() ? "none" : state().searchQuery(),
                "articleCount", articleCount,
                "visibleArticleCount", visibleArticleCount,
                "guideBookCount", guideBookCount,
                "visibleGuideBookCount", visibleGuideBookCount,
                "unavailableGuideBookCount", Math.max(0, guideBookCount - visibleGuideBookCount),
                "regionCount", regionCount(),
                "hazardCount", hazardCount(),
                "missionCount", missionCount(),
                "factionCount", factionCount(),
                "dataArticleCount", WikiContentRegistry.dataArticleCount(),
                "warningCount", WikiContentRegistry.warnings().size(),
                "indexAvailable", WikiModuleAccess.isLoaded("echoindex"),
                "terminalAvailable", WikiModuleAccess.isLoaded("echoterminal"));
    }

    private static int visibleArticleCount() {
        String query = normalized(state().searchQuery());
        String category = state().category();
        return (int) WikiContentRegistry.articles().stream()
                .filter(article -> "All".equalsIgnoreCase(category) || article.category().equalsIgnoreCase(category))
                .filter(article -> query.isBlank() || searchable(article).contains(query))
                .count();
    }

    private static int visibleGuideBookCount() {
        String query = normalized(state().searchQuery());
        String category = state().category();
        return (int) GuideBookRegistry.visibleGuideBooks().stream()
                .filter(guide -> "All".equalsIgnoreCase(category) || guide.moduleId().equalsIgnoreCase(category)
                        || guide.requiredModId().equalsIgnoreCase(category))
                .filter(guide -> query.isBlank() || searchable(guide).contains(query))
                .count();
    }

    private static int regionCount() {
        try {
            return EchoCoreServices.regionService().regionDefinitions().size();
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private static int hazardCount() {
        try {
            return EchoCoreServices.hazardService().hazardDefinitions().size();
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private static int missionCount() {
        try {
            if (EchoCoreServices.missionCoreAvailable()) {
                return EchoCoreServices.missionService().missions(player()).size();
            }
            return EchoCoreServices.missionService().missionDefinitions().size();
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private static int factionCount() {
        try {
            return EchoCoreServices.factionDefinitions().size();
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private static List<Map<String, Object>> navSections() {
        return List.of(
                nav("Overview", "Codex status", WikiScreenCorePages.DASHBOARD),
                nav("Articles", "Guides and field notes", WikiScreenCorePages.ARTICLES),
                nav("Guide Books", "Physical manuals", WikiScreenCorePages.GUIDE_BOOKS),
                nav("Regions", "WorldCore regions", WikiScreenCorePages.REGIONS),
                nav("Hazards", "Warnings and counters", WikiScreenCorePages.HAZARDS),
                nav("Missions", "Route objectives", WikiScreenCorePages.MISSIONS),
                nav("Machines", "Index-backed systems", WikiScreenCorePages.MACHINES),
                nav("Factions", "Standing and contacts", WikiScreenCorePages.FACTIONS),
                nav("Discoveries", "Spoiler-safe intel", WikiScreenCorePages.DISCOVERIES),
                nav("Debug", "Provider state", WikiScreenCorePages.SETTINGS));
    }

    private static Map<String, Object> nav(String title, String subtitle, Identifier page) {
        boolean selected = page.equals(state().currentPage());
        return row("title", title, "subtitle", subtitle, "page", page.toString(), "selected", selected, "selectedTone", selected ? "ready" : "info");
    }

    private static List<Map<String, Object>> articleRows(boolean filtered) {
        Player player = player();
        List<WikiArticleDefinition> articles = filtered ? pagedArticles() : WikiContentRegistry.articles();
        return articles.stream()
                .map(article -> articleRow(article, player))
                .toList();
    }

    private static List<WikiArticleDefinition> filteredArticles() {
        String query = normalized(state().searchQuery());
        String category = state().category();
        return WikiContentRegistry.articles().stream()
                .filter(article -> "All".equalsIgnoreCase(category) || article.category().equalsIgnoreCase(category))
                .filter(article -> query.isBlank() || searchable(article).contains(query))
                .toList();
    }

    private static List<WikiArticleDefinition> pagedArticles() {
        return page(filteredArticles(), state().articlePage(), WikiUiState.ARTICLE_PAGE_SIZE);
    }

    private static PageInfo articlePageInfo() {
        return pageInfo(filteredArticles().size(), state().articlePage(), WikiUiState.ARTICLE_PAGE_SIZE);
    }

    private static String searchable(WikiArticleDefinition article) {
        return normalized(article.title() + " " + article.category() + " " + article.summary() + " " + String.join(" ", article.tags()));
    }

    private static Map<String, Object> articleRow(WikiArticleDefinition article, Player player) {
        boolean unlocked = unlocked(article, player);
        return row(
                "id", article.id().toString(),
                "title", unlocked ? article.title() : "Locked Intel",
                "category", article.category(),
                "summary", unlocked ? article.summary() : "Discover the linked signal to reveal this Codex entry.",
                "icon", article.icon().toString(),
                "heroArt", article.heroArt() == null ? "" : article.heroArt().toString(),
                "state", unlocked ? "known" : "locked",
                "statusLabel", unlocked ? "known" : "locked",
                "chip", unlocked ? "known" : "locked",
                "locked", !unlocked,
                "tags", String.join(", ", article.tags()),
                "tagsLabel", article.tags().isEmpty() ? "" : "tags: " + String.join(", ", article.tags()),
                "summaryPreview", preview(unlocked ? article.summary() : "Discover the linked signal to reveal this Codex entry.", 150),
                "heroArtFallback", article.heroArt() == null ? "echowiki:textures/gui/guide_books/wiki_banner.png" : article.heroArt().toString(),
                "countLabel", GuideBookLabels.sectionCountLabel(article.sections().size()),
                "selectedTone", Objects.equals(article.id(), state().selectedArticle()) ? "selected" : "idle",
                "emptyHint", "No Codex articles match the current filters.",
                "openDisabled", false,
                "selected", Objects.equals(article.id(), state().selectedArticle()));
    }

    private static List<Map<String, Object>> guideBookRows(boolean filtered) {
        GuideBookDataCache cache = guideBookDataCache();
        return filtered ? cache.pagedVisibleRows() : cache.allRows();
    }

    private static PageInfo guideBookPageInfo() {
        return guideBookDataCache().pageInfo();
    }

    private static String searchable(GuideBookDefinition guide) {
        return normalized(guide.title() + " " + guide.subtitle() + " " + guide.summary() + " "
                + guide.moduleId() + " " + guide.requiredModId() + " " + String.join(" ", guide.tags()));
    }

    private static Map<String, Object> guideBookRow(GuideBookDefinition guide) {
        boolean visible = GuideBookRegistry.isVisible(guide);
        int chapterCount = guide.allArticleIds().size();
        String moduleLabel = GuideBookLabels.moduleLabel(guide.moduleId());
        String availabilityLabel = GuideBookLabels.availabilityLabel(guide);
        String chapterLabel = GuideBookLabels.chapterCountLabel(chapterCount);
        String tags = String.join(", ", guide.tags());
        return row(
                "id", guide.id().toString(),
                "title", guide.title(),
                "subtitle", guide.subtitle(),
                "summary", guide.summary(),
                "subtitlePreview", preview(guide.subtitle(), 104),
                "summaryPreview", preview(guide.summary(), 150),
                "moduleId", guide.moduleId(),
                "moduleLabel", moduleLabel,
                "moduleBadge", moduleLabel,
                "requiredModId", guide.requiredModId(),
                "collectionId", guide.collectionId() == null ? "" : guide.collectionId().toString(),
                "homeArticleId", guide.homeArticleId() == null ? "" : guide.homeArticleId().toString(),
                "icon", GuideBookLabels.safeItemIcon(guide.icon()).toString(),
                "heroArt", guideBookHeroArt(guide),
                "accent", guide.accent(),
                "state", visible ? "ready" : "optional",
                "status", visible ? "ready" : "optional",
                "statusLabel", availabilityLabel,
                "availabilityLabel", availabilityLabel,
                "chip", moduleLabel,
                "meta", moduleLabel,
                "chapterCount", chapterCount,
                "chapterBadge", chapterLabel,
                "chapterCountLabel", chapterLabel,
                "countLabel", chapterLabel,
                "tags", tags,
                "tagsLabel", tags.isBlank() ? "" : "tags: " + tags,
                "hasSelection", visible,
                "openDisabled", !visible,
                "openLabel", Component.translatable("screen.echowiki.guide_book.open").getString(),
                "selected", Objects.equals(guide.id(), state().selectedGuideBook()));
    }

    private static String guideBookEmptyTitle() {
        if (GuideBookRegistry.guideBooks().isEmpty()) {
            return Component.translatable("screen.echowiki.guide_book.empty.none_loaded.title").getString();
        }
        if (GuideBookRegistry.visibleGuideBooks().isEmpty()) {
            return Component.translatable("screen.echowiki.guide_book.empty.none_available.title").getString();
        }
        return Component.translatable("screen.echowiki.guide_book.empty.no_matches.title").getString();
    }

    private static String guideBookEmptyBody() {
        if (GuideBookRegistry.guideBooks().isEmpty()) {
            return Component.translatable("screen.echowiki.guide_book.empty.none_loaded.body").getString();
        }
        if (GuideBookRegistry.visibleGuideBooks().isEmpty()) {
            return Component.translatable("screen.echowiki.guide_book.empty.none_available.body").getString();
        }
        return Component.translatable("screen.echowiki.guide_book.empty.no_matches.body").getString();
    }

    private static boolean unlocked(WikiArticleDefinition article, Player player) {
        return article.unlockDiscovery() == null || (player != null && EchoCoreServices.hasDiscoveredFeature(player, article.unlockDiscovery()));
    }

    private static Map<String, Object> selectedArticle() {
        Identifier id = state().selectedArticle();
        WikiArticleDefinition article = WikiContentRegistry.article(id).orElse(null);
        if (article == null) {
            return row(
                    "id", "",
                    "title", "Select an Article",
                    "summary", "Choose an article row to inspect its summary, collections, and links.",
                    "category", "Codex",
                    "state", "info",
                    "status", "info",
                    "statusLabel", "select row",
                    "icon", "minecraft:book",
                    "heroArt", "",
                    "heroArtFallback", "echowiki:textures/gui/guide_books/wiki_banner.png",
                    "summaryPreview", "Choose an article row to inspect its summary, collections, and links.",
                    "countLabel", "",
                    "tagsLabel", "",
                    "openDisabled", true);
        }
        return articleRow(article, player());
    }

    private static List<Map<String, Object>> selectedArticleBlocks() {
        Identifier id = state().selectedArticle();
        WikiArticleDefinition article = WikiContentRegistry.article(id).orElse(null);
        if (article == null) {
            return List.of();
        }
        if (!unlocked(article, player())) {
            return List.of(sectionRow(0, new WikiArticleSection(
                    "callout",
                    "Locked Intel",
                    "This entry is still hidden behind discovery progress.",
                    "locked",
                    "",
                    "cover",
                    "minecraft:barrier",
                    "",
                    1,
                    "",
                    "",
                    "",
                    "")));
        }
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 0; index < article.sections().size(); index++) {
            rows.add(sectionRow(index, article.sections().get(index)));
        }
        return List.copyOf(rows);
    }

    private static List<Map<String, Object>> selectedArticleToc() {
        Identifier id = state().selectedArticle();
        WikiArticleDefinition article = WikiContentRegistry.article(id).orElse(null);
        if (article == null || !unlocked(article, player())) {
            return List.of();
        }
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 0; index < article.sections().size(); index++) {
            WikiArticleSection section = article.sections().get(index);
            String title = section.title().isBlank() ? labelForBlock(section, index) : section.title();
            rows.add(row(
                    "id", "block-" + index,
                    "title", title,
                    "ordinal", String.format(Locale.ROOT, "%02d", index + 1),
                    "type", blockType(section),
                    "tone", section.tone(),
                    "chip", blockChip(section)));
        }
        return List.copyOf(rows);
    }

    private static Map<String, Object> sectionRow(int index, WikiArticleSection section) {
        String type = blockType(section);
        String targetKind = blockTargetKind(section);
        String target = blockTarget(section);
        String label = section.label().isBlank() ? blockLabel(section, target) : section.label();
        String body = section.body();
        String subtitle = section.subtitle().isBlank() ? blockSubtitle(section, targetKind) : section.subtitle();
        String action = target.isBlank() ? "noop" : "wiki.open_related";
        String image = section.image();
        return row(
                "id", "block-" + index,
                "index", index,
                "ordinal", String.format(Locale.ROOT, "%02d", index + 1),
                "type", type,
                "typeLabel", labelForBlock(section, index),
                "chip", blockChip(section),
                "title", section.title(),
                "titleOrLabel", section.title().isBlank() ? label : section.title(),
                "body", body,
                "bodyPreview", preview(body, 160),
                "tone", section.tone(),
                "status", blockStatus(section),
                "height", blockHeight(section, body),
                "image", image,
                "imageFit", section.imageFit(),
                "hasImage", !image.isBlank(),
                "icon", blockIcon(section, targetKind),
                "item", blockItem(section, targetKind, target),
                "count", Math.max(1, section.count()),
                "targetKind", targetKind,
                "target", target,
                "label", label,
                "subtitle", subtitle,
                "subtitlePreview", preview(subtitle, 120),
                "action", action);
    }

    private static List<Map<String, Object>> selectedArticleRelated() {
        Identifier id = state().selectedArticle();
        WikiArticleDefinition article = WikiContentRegistry.article(id).orElse(null);
        if (article == null || !unlocked(article, player())) {
            return List.of();
        }
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        article.relatedArticles().forEach(value -> rows.add(related("article", value, label(value), "Open article", true)));
        article.relatedItems().forEach(value -> rows.add(related("item", value, itemLabel(value), "Open in Index when available", WikiModuleAccess.isLoaded("echoindex"))));
        article.relatedRecipes().forEach(value -> rows.add(related("recipe", value, label(value), "Open recipe in Index when available", WikiModuleAccess.isLoaded("echoindex"))));
        article.relatedMissions().forEach(value -> rows.add(related("mission", value, label(value), "Mission reference", true)));
        article.relatedRegions().forEach(value -> rows.add(related("region", value, label(value), "Region reference", true)));
        article.relatedHazards().forEach(value -> rows.add(related("hazard", value, label(value), "Hazard reference", true)));
        article.relatedFactions().forEach(value -> rows.add(related("faction", value, label(value), "Faction reference", true)));
        Set<String> seen = rows.stream()
                .map(row -> row.get("kind") + ":" + row.get("id"))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        addBlockRelatedRows(rows, seen, article);
        return List.copyOf(rows);
    }

    private static Map<String, Object> selectedGuideBook() {
        return guideBookDataCache().selectedGuideBook();
    }

    private static Map<String, Object> buildSelectedGuideBook() {
        Identifier id = state().selectedGuideBook();
        GuideBookDefinition guide = GuideBookRegistry.visibleGuideBook(id).orElse(null);
        if (guide == null) {
            return row(
                    "id", "",
                    "title", Component.translatable("screen.echowiki.guide_book.no_selection.title").getString(),
                    "summary", Component.translatable("screen.echowiki.guide_book.no_selection.body").getString(),
                    "state", "missing",
                    "status", "missing",
                    "statusLabel", Component.translatable("screen.echowiki.guide_book.no_selection.status").getString(),
                    "subtitle", "",
                    "moduleId", "",
                    "moduleLabel", "",
                    "moduleBadge", "",
                    "availabilityLabel", "",
                    "icon", GuideBookLabels.DEFAULT_ICON.toString(),
                    "heroArt", "",
                    "subtitlePreview", "",
                    "summaryPreview", Component.translatable("screen.echowiki.guide_book.no_selection.body").getString(),
                    "countLabel", GuideBookLabels.chapterCountLabel(0),
                    "chapterBadge", GuideBookLabels.chapterCountLabel(0),
                    "chapterCountLabel", GuideBookLabels.chapterCountLabel(0),
                    "hasSelection", false,
                    "openDisabled", true,
                    "openLabel", Component.translatable("screen.echowiki.guide_book.open_disabled").getString());
        }
        return guideBookRow(guide);
    }

    private static String guideBookHeroArt(GuideBookDefinition guide) {
        if (guide == null || guide.homeArticleId() == null) {
            return "";
        }
        return WikiContentRegistry.article(guide.homeArticleId())
                .map(WikiArticleDefinition::heroArt)
                .map(Identifier::toString)
                .orElse("");
    }

    private static List<Map<String, Object>> selectedGuideBookChapters() {
        return guideBookDataCache().chapters();
    }

    private static List<Map<String, Object>> buildSelectedGuideBookChapters() {
        Identifier id = state().selectedGuideBook();
        GuideBookDefinition guide = GuideBookRegistry.visibleGuideBook(id).orElse(null);
        if (guide == null) {
            return List.of();
        }
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        List<Identifier> articleIds = guide.allArticleIds();
        for (int index = 0; index < articleIds.size(); index++) {
            Identifier articleId = articleIds.get(index);
            String ordinalLabel = String.format(Locale.ROOT, "%02d", index + 1);
            String roleLabel = GuideBookLabels.chapterRoleLabel(articleId, guide.homeArticleId(), index);
            rows.add(WikiContentRegistry.article(articleId)
                    .map(article -> row(
                                "id", article.id().toString(),
                                "title", article.title(),
                                "summary", article.summary(),
                                "summaryPreview", preview(article.summary(), 142),
                                "category", article.category(),
                                "icon", article.icon().toString(),
                                "status", "ready",
                                "statusLabel", "ready",
                                "chip", ordinalLabel,
                                "ordinalLabel", ordinalLabel,
                                "ordinalBadge", ordinalLabel,
                                "roleLabel", roleLabel,
                                "roleBadge", roleLabel,
                                "meta", roleLabel,
                                "countLabel", GuideBookLabels.sectionCountLabel(article.sections().size()),
                                "action", "wiki.open_article"))
                    .orElse(row(
                                "id", articleId.toString(),
                                "title", label(articleId),
                                "summary", Component.translatable("text.echowiki.guide_book.chapter_missing.body").getString(),
                                "summaryPreview", Component.translatable("text.echowiki.guide_book.chapter_missing.body").getString(),
                                "category", "missing",
                                "icon", "minecraft:barrier",
                                "status", "missing",
                                "statusLabel", "missing",
                                "chip", ordinalLabel,
                                "ordinalLabel", ordinalLabel,
                                "ordinalBadge", ordinalLabel,
                                "roleLabel", roleLabel,
                                "roleBadge", roleLabel,
                                "meta", roleLabel,
                                "countLabel", Component.translatable("text.echowiki.guide_book.chapter_missing.count").getString(),
                                "action", "noop")));
        }
        return List.copyOf(rows);
    }

    private static List<Map<String, Object>> selectedGuideBookRelated() {
        return guideBookDataCache().related();
    }

    private static List<Map<String, Object>> buildSelectedGuideBookRelated() {
        Identifier id = state().selectedGuideBook();
        GuideBookDefinition guide = GuideBookRegistry.visibleGuideBook(id).orElse(null);
        if (guide == null) {
            return List.of();
        }
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        if (guide.homeArticleId() != null) {
            rows.add(related("article", guide.homeArticleId(), "Open Overview", "Manual overview", true));
        }
        Set<Identifier> manualArticleIds = new LinkedHashSet<>(guide.allArticleIds());
        Set<String> seen = new LinkedHashSet<>();
        if (guide.homeArticleId() != null) {
            seen.add("article:" + guide.homeArticleId());
        }
        for (Identifier articleId : guide.allArticleIds()) {
            WikiContentRegistry.article(articleId).ifPresent(article ->
                    addExternalRelatedRows(rows, seen, manualArticleIds, article));
        }
        return List.copyOf(rows);
    }

    private static void addExternalRelatedRows(
            List<Map<String, Object>> rows,
            Set<String> seen,
            Set<Identifier> manualArticleIds,
            WikiArticleDefinition article) {
        article.relatedArticles().stream()
                .filter(value -> !manualArticleIds.contains(value))
                .forEach(value -> addRelated(rows, seen, "article", value, label(value), "Related article", true));
        article.relatedItems().forEach(value ->
                addRelated(rows, seen, "item", value, itemLabel(value), "Open in Index when available", WikiModuleAccess.isLoaded("echoindex")));
        article.relatedRecipes().forEach(value ->
                addRelated(rows, seen, "recipe", value, label(value), "Open recipe in Index when available", WikiModuleAccess.isLoaded("echoindex")));
        article.relatedMissions().forEach(value ->
                addRelated(rows, seen, "mission", value, label(value), "Mission reference", true));
        article.relatedRegions().forEach(value ->
                addRelated(rows, seen, "region", value, label(value), "Region reference", true));
        article.relatedHazards().forEach(value ->
                addRelated(rows, seen, "hazard", value, label(value), "Hazard reference", true));
        article.relatedFactions().forEach(value ->
                addRelated(rows, seen, "faction", value, label(value), "Faction reference", true));
        addBlockRelatedRows(rows, seen, article);
    }

    private static void addBlockRelatedRows(List<Map<String, Object>> rows, Set<String> seen, WikiArticleDefinition article) {
        for (WikiArticleSection section : article.sections()) {
            String kind = blockTargetKind(section);
            String target = blockTarget(section);
            Identifier id = Identifier.tryParse(target);
            if (kind.isBlank() || id == null) {
                continue;
            }
            boolean actionable = !"item".equals(kind) && !"recipe".equals(kind) || WikiModuleAccess.isLoaded("echoindex");
            String label = section.label().isBlank() ? blockLabel(section, target) : section.label();
            String subtitle = section.subtitle().isBlank() ? blockSubtitle(section, kind) : section.subtitle();
            addRelated(rows, seen, kind, id, label, subtitle, actionable);
        }
    }

    private static void addRelated(
            List<Map<String, Object>> rows,
            Set<String> seen,
            String kind,
            Identifier id,
            String label,
            String subtitle,
            boolean actionable) {
        String key = kind + ":" + id;
        if (id != null && seen.add(key)) {
            rows.add(related(kind, id, label, subtitle, actionable));
        }
    }

    private static Map<String, Object> related(String kind, Identifier id, String label, String subtitle, boolean actionable) {
        return row(
                "kind", kind,
                "id", id.toString(),
                "label", label,
                "subtitle", subtitle,
                "kindLabel", relatedKindLabel(kind, actionable),
                "subtitlePreview", preview(actionable ? subtitle : disabledRelatedSubtitle(kind), 118),
                "actionable", actionable,
                "disabled", !actionable,
                "disabledReason", actionable ? subtitle : disabledRelatedSubtitle(kind),
                "status", actionable ? "ready" : "optional");
    }

    private static String relatedKindLabel(String kind, boolean actionable) {
        String label = switch (kind == null ? "" : kind.toLowerCase(Locale.ROOT)) {
            case "article" -> "article";
            case "item" -> "item";
            case "recipe" -> "recipe";
            case "mission" -> "mission";
            case "region" -> "region";
            case "hazard" -> "hazard";
            case "faction" -> "faction";
            default -> "link";
        };
        return actionable ? label : label + " *";
    }

    private static String disabledRelatedSubtitle(String kind) {
        return switch (kind == null ? "" : kind.toLowerCase(Locale.ROOT)) {
            case "item", "recipe" -> "Index addon unavailable";
            default -> "Optional addon unavailable";
        };
    }

    private static String blockType(WikiArticleSection section) {
        String type = section == null ? "" : section.type();
        if (type == null || type.isBlank()) {
            return "body".equalsIgnoreCase(section == null ? "" : section.tone()) ? "paragraph" : "callout";
        }
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "paragraph", "callout", "image", "item", "link", "divider" -> type.toLowerCase(Locale.ROOT);
            default -> "paragraph";
        };
    }

    private static String blockStatus(WikiArticleSection section) {
        String tone = section == null ? "" : section.tone().toLowerCase(Locale.ROOT);
        return switch (tone) {
            case "tip", "success" -> "ready";
            case "warning" -> "warning";
            case "danger" -> "danger";
            case "spoiler", "locked" -> "locked";
            default -> "info";
        };
    }

    private static String blockChip(WikiArticleSection section) {
        String type = blockType(section);
        String tone = section == null ? "body" : section.tone();
        if (!"body".equalsIgnoreCase(tone) && !"paragraph".equals(type)) {
            return tone;
        }
        return switch (type) {
            case "image" -> "image";
            case "item" -> "item";
            case "link" -> "link";
            case "divider" -> "break";
            case "callout" -> "note";
            default -> "text";
        };
    }

    private static String labelForBlock(WikiArticleSection section, int index) {
        if (section != null && !section.title().isBlank()) {
            return section.title();
        }
        return switch (blockType(section)) {
            case "image" -> "Image";
            case "item" -> "Item Reference";
            case "link" -> "Related Link";
            case "divider" -> "Divider";
            case "callout" -> "Field Note";
            default -> "Section " + (index + 1);
        };
    }

    private static String blockTargetKind(WikiArticleSection section) {
        if (section == null) {
            return "";
        }
        if (!section.targetKind().isBlank()) {
            return section.targetKind().toLowerCase(Locale.ROOT);
        }
        if ("item".equals(blockType(section)) || !section.item().isBlank()) {
            return "item";
        }
        return "";
    }

    private static String blockTarget(WikiArticleSection section) {
        if (section == null) {
            return "";
        }
        if (!section.target().isBlank()) {
            return section.target();
        }
        if ("item".equals(blockTargetKind(section))) {
            return section.item();
        }
        return "";
    }

    private static String blockLabel(WikiArticleSection section, String target) {
        if (section != null && !section.title().isBlank()) {
            return section.title();
        }
        Identifier id = Identifier.tryParse(target);
        if (id == null) {
            return target == null ? "" : target;
        }
        return "item".equals(blockTargetKind(section)) ? itemLabel(id) : label(id);
    }

    private static String blockSubtitle(WikiArticleSection section, String targetKind) {
        String body = section == null ? "" : section.body();
        if (!body.isBlank()) {
            return body;
        }
        return switch (targetKind) {
            case "item" -> "Item reference";
            case "recipe" -> "Recipe reference";
            case "mission" -> "Mission reference";
            case "region" -> "Region reference";
            case "hazard" -> "Hazard reference";
            case "faction" -> "Faction reference";
            case "article" -> "Related article";
            default -> "";
        };
    }

    private static String blockIcon(WikiArticleSection section, String targetKind) {
        if (section != null && !section.icon().isBlank()) {
            return section.icon();
        }
        if (section != null && "item".equals(targetKind) && !section.item().isBlank()) {
            return section.item();
        }
        return switch (targetKind) {
            case "recipe" -> "minecraft:crafting_table";
            case "mission" -> "minecraft:filled_map";
            case "region" -> "minecraft:map";
            case "hazard" -> "minecraft:fire_charge";
            case "faction" -> "minecraft:banner";
            case "article" -> "minecraft:book";
            default -> "minecraft:paper";
        };
    }

    private static String blockItem(WikiArticleSection section, String targetKind, String target) {
        if (section != null && !section.item().isBlank()) {
            return section.item();
        }
        return "item".equals(targetKind) ? target : blockIcon(section, targetKind);
    }

    private static int blockHeight(WikiArticleSection section, String body) {
        String type = blockType(section);
        int bodyLines = Math.max(1, (int) Math.ceil((body == null ? 0 : body.length()) / 74.0D));
        int base = switch (type) {
            case "image" -> 176;
            case "item", "link" -> 84;
            case "divider" -> 38;
            case "callout" -> 92;
            default -> 78;
        };
        return base + bodyLines * 12;
    }

    private static List<Map<String, Object>> collectionRows() {
        return WikiContentRegistry.collections().stream().map(WikiDataProviders::collectionRow).toList();
    }

    private static Map<String, Object> collectionRow(WikiCollectionDefinition collection) {
        return row(
                "id", collection.id().toString(),
                "title", collection.title(),
                "summary", collection.summary(),
                "category", collection.category(),
                "articleCount", collection.articles().size(),
                "countLabel", collection.articles().size() + " article(s)",
                "chip", collection.category());
    }

    private static List<Map<String, Object>> categoryRows() {
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row("label", "All", "value", "All", "selected", "All".equalsIgnoreCase(state().category())));
        for (String category : WikiContentRegistry.categories()) {
            rows.add(row("label", category, "value", category, "selected", category.equalsIgnoreCase(state().category())));
        }
        for (GuideBookDefinition guide : GuideBookRegistry.visibleGuideBooks()) {
            if (rows.stream().noneMatch(row -> guide.moduleId().equalsIgnoreCase(String.valueOf(row.get("value"))))) {
                rows.add(row("label", GuideBookLabels.moduleLabel(guide.moduleId()), "value", guide.moduleId(), "selected", guide.moduleId().equalsIgnoreCase(state().category())));
            }
        }
        return List.copyOf(rows);
    }

    private static List<Map<String, Object>> regionRows() {
        try {
            return EchoCoreServices.regionService().regionDefinitions().stream()
                    .sorted(Comparator.comparingInt(WorldRegionDefinition::sortOrder).thenComparing(region -> region.id().toString()))
                    .map(WikiDataProviders::regionRow)
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static Map<String, Object> regionRow(WorldRegionDefinition region) {
        WorldContextSnapshot context = EchoCoreServices.worldContext(player());
        boolean active = context.activeRegions().stream().anyMatch(instance -> instance.definitionId().equals(region.id()));
        boolean current = context.currentRegionOptional().map(instance -> instance.definitionId().equals(region.id())).orElse(false);
        return row(
                "id", region.id().toString(),
                "title", region.displayName(),
                "summary", region.summary(),
                "type", region.type().name(),
                "typeLabel", region.type().name().toLowerCase(Locale.ROOT).replace('_', ' '),
                "hazardCount", region.hazardIds().size(),
                "hazardCountLabel", region.hazardIds().size() + " hazard(s)",
                "chip", current ? "current" : active ? "active" : "region",
                "statusLabel", current ? "current" : active ? "active" : "known",
                "meta", region.type().name().toLowerCase(Locale.ROOT).replace('_', ' '),
                "countLabel", region.hazardIds().size() + " hazard(s)",
                "status", current ? "current" : active ? "active" : "known",
                "selected", Objects.equals(region.id(), state().selectedRegion()));
    }

    private static Map<String, Object> selectedRegion() {
        Identifier id = state().selectedRegion();
        if (id == null) {
            return row(
                    "id", "",
                    "title", "Select a Region",
                    "summary", "Choose a region row to inspect its active status and hazard links.",
                    "status", "info",
                    "statusLabel", "select row",
                    "typeLabel", "none",
                    "hazardCount", 0,
                    "hazardCountLabel", "0 hazard(s)");
        }
        return EchoCoreServices.regionService().regionDefinition(id).map(WikiDataProviders::regionRow)
                .orElse(row(
                        "id", id.toString(),
                        "title", label(id),
                        "summary", "Region data is not available.",
                        "status", "missing",
                        "statusLabel", "unavailable",
                        "typeLabel", "unknown",
                        "hazardCount", 0,
                        "hazardCountLabel", "0 hazard(s)"));
    }

    private static List<Map<String, Object>> hazardRows() {
        try {
            WorldHazardSnapshot active = EchoCoreServices.hazardService().hazardSnapshot(player());
            return EchoCoreServices.hazardService().hazardDefinitions().stream()
                    .sorted(Comparator.comparingInt(WorldHazardDefinition::defaultSeverity).reversed().thenComparing(hazard -> hazard.id().toString()))
                    .map(hazard -> hazardRow(hazard, active.hazardIds().contains(hazard.id())))
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static List<Map<String, Object>> activeHazards() {
        try {
            WorldHazardSnapshot active = EchoCoreServices.hazardService().hazardSnapshot(player());
            return active.hazardIds().stream()
                    .map(id -> EchoCoreServices.hazardService().hazardDefinition(id)
                            .map(hazard -> hazardRow(hazard, true))
                            .orElse(row("id", id.toString(), "title", label(id), "summary", active.summary(), "severity", active.severity(), "status", "active")))
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static Map<String, Object> hazardRow(WorldHazardDefinition hazard, boolean active) {
        return row(
                "id", hazard.id().toString(),
                "title", hazard.displayName(),
                "summary", hazard.summary(),
                "severity", hazard.defaultSeverity(),
                "severityLabel", String.valueOf(hazard.defaultSeverity()),
                "ticking", hazard.ticking(),
                "chip", active ? "active" : String.valueOf(hazard.defaultSeverity()),
                "statusLabel", active ? "active" : "severity " + hazard.defaultSeverity(),
                "meta", "severity " + hazard.defaultSeverity(),
                "countLabel", hazard.ticking() ? "ticking" : "static",
                "status", active ? "danger" : hazard.defaultSeverity() > 50 ? "warning" : "known",
                "selected", Objects.equals(hazard.id(), state().selectedHazard()));
    }

    private static Map<String, Object> selectedHazard() {
        Identifier id = state().selectedHazard();
        if (id == null) {
            return row(
                    "id", "",
                    "title", "Select a Hazard",
                    "summary", "Choose a hazard row to inspect severity, ticking behavior, and current status.",
                    "status", "info",
                    "statusLabel", "select row",
                    "severity", "",
                    "severityLabel", "none",
                    "ticking", "none");
        }
        return EchoCoreServices.hazardService().hazardDefinition(id).map(hazard -> hazardRow(hazard, false))
                .orElse(row(
                        "id", id.toString(),
                        "title", label(id),
                        "summary", "Hazard data is not available.",
                        "status", "missing",
                        "statusLabel", "unavailable",
                        "severity", "",
                        "severityLabel", "unknown",
                        "ticking", "unknown"));
    }

    private static List<Map<String, Object>> missionChapterRows() {
        try {
            return EchoCoreServices.missionService().chapters().stream()
                    .sorted(Comparator.comparingInt(MissionChapterDefinition::order).thenComparing(chapter -> chapter.id().toString()))
                    .map(chapter -> row("id", chapter.id().toString(), "title", chapter.title(), "summary", chapter.summary(), "accentColor", chapter.accentColor()))
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static List<Map<String, Object>> missionRows() {
        Player player = player();
        try {
            if (EchoCoreServices.missionCoreAvailable()) {
                return EchoCoreServices.missionService().missions(player).stream()
                        .map(WikiDataProviders::missionProgressRow)
                        .toList();
            }
            return EchoCoreServices.missionService().missionDefinitions().stream()
                    .map(WikiDataProviders::missionDefinitionRow)
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static Map<String, Object> missionProgressRow(IMissionProgressView view) {
        MissionDefinition definition = view.definition();
        return row(
                "id", view.id().toString(),
                "title", definition.title(),
                "summary", definition.briefing(),
                "chapter", view.chapterId().toString(),
                "status", view.status().name().toLowerCase(Locale.ROOT),
                "statusLabel", view.statusLabel(),
                "chip", view.statusLabel(),
                "meta", view.chapterId().toString(),
                "countLabel", Math.round(view.progress() * 100.0F) + "%",
                "progress", Math.round(view.progress() * 100.0F),
                "progressLabel", Math.round(view.progress() * 100.0F) + "%",
                "selected", Objects.equals(view.id(), state().selectedMission()));
    }

    private static Map<String, Object> missionDefinitionRow(MissionDefinition mission) {
        return row(
                "id", mission.id().toString(),
                "title", mission.title(),
                "summary", mission.briefing(),
                "chapter", mission.chapterId().toString(),
                "status", "known",
                "statusLabel", mission.kind().name(),
                "chip", mission.kind().name(),
                "meta", mission.chapterId().toString(),
                "countLabel", "definition",
                "progress", 0,
                "progressLabel", "definition",
                "selected", Objects.equals(mission.id(), state().selectedMission()));
    }

    private static Map<String, Object> selectedMission() {
        Identifier id = state().selectedMission();
        if (id == null) {
            return row(
                    "id", "",
                    "title", "Select a Mission",
                    "summary", "Choose a mission row to inspect progress and chapter routing.",
                    "status", "info",
                    "statusLabel", "select row",
                    "progress", 0,
                    "progressLabel", "select row",
                    "chapter", "none");
        }
        try {
            return EchoCoreServices.missionService().mission(player(), id)
                    .map(WikiDataProviders::missionProgressRow)
                    .or(() -> EchoCoreServices.missionService().missionDefinition(id).map(WikiDataProviders::missionDefinitionRow))
                    .orElse(row(
                            "id", id.toString(),
                            "title", label(id),
                            "summary", "Mission data is not available.",
                            "status", "missing",
                            "statusLabel", "unavailable",
                            "progress", 0,
                            "progressLabel", "unavailable",
                            "chapter", "unknown"));
        } catch (RuntimeException exception) {
            return row(
                    "id", id.toString(),
                    "title", label(id),
                    "summary", "Mission data is not available.",
                    "status", "missing",
                    "statusLabel", "unavailable",
                    "progress", 0,
                    "progressLabel", "unavailable",
                    "chapter", "unknown");
        }
    }

    private static List<Map<String, Object>> machineRows() {
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        try {
            for (IndexContentSnapshot snapshot : EchoCoreServices.indexContentSnapshots(player())) {
                for (IndexMachineLayout layout : snapshot.machineLayouts()) {
                    rows.add(row("id", layout.recipeId().toString(), "title", label(layout.recipeId()), "summary", "Machine layout from " + snapshot.providerId(), "status", "index", "chip", "layout", "meta", snapshot.providerId().toString(), "countLabel", "Index"));
                }
                for (IndexRecipeView recipe : snapshot.recipes()) {
                    rows.add(row("id", recipe.id().toString(), "title", recipe.title(), "summary", "Recipe reference", "status", "recipe", "chip", "recipe", "meta", recipe.categoryId().toString(), "countLabel", "Index"));
                }
            }
            if (rows.isEmpty()) {
                for (IndexEntry entry : EchoCoreServices.indexEntries(player())) {
                    rows.add(row("id", entry.id().toString(), "title", entry.titleKey(), "summary", entry.summaryKey(), "status", "entry", "chip", "entry", "meta", entry.categoryId().toString(), "countLabel", "Index"));
                }
            }
        } catch (RuntimeException ignored) {
        }
        return rows.stream().limit(80).toList();
    }

    private static List<Map<String, Object>> factionRows() {
        try {
            return EchoCoreServices.factionDefinitions().stream()
                    .sorted(Comparator.comparing(faction -> faction.displayName().toLowerCase(Locale.ROOT)))
                    .map(WikiDataProviders::factionRow)
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static Map<String, Object> factionRow(EchoFactionDefinition faction) {
        return row(
                "id", faction.id().toString(),
                "title", faction.displayName(),
                "summary", faction.summary(),
                "route", faction.route(),
                "hazard", faction.hazard(),
                "prepHint", faction.prepHint(),
                "serviceSummary", faction.serviceSummary(),
                "chip", faction.landmarkFaction() ? "landmark" : "faction",
                "statusLabel", faction.landmarkFaction() ? "landmark" : "known",
                "meta", faction.route(),
                "countLabel", faction.hazard(),
                "status", faction.landmarkFaction() ? "landmark" : "known",
                "selected", Objects.equals(faction.id(), state().selectedFaction()));
    }

    private static Map<String, Object> selectedFaction() {
        Identifier id = state().selectedFaction();
        if (id == null) {
            return row(
                    "id", "",
                    "title", "Select a Faction",
                    "summary", "Choose a faction row to inspect route role, hazards, and preparation hints.",
                    "status", "info",
                    "statusLabel", "select row",
                    "route", "none",
                    "hazard", "none",
                    "prepHint", "none",
                    "serviceSummary", "");
        }
        return EchoCoreServices.factionDefinitions().stream()
                .filter(faction -> faction.id().equals(id))
                .findFirst()
                .map(WikiDataProviders::factionRow)
                .orElse(row(
                        "id", id.toString(),
                        "title", label(id),
                        "summary", "Faction data is not available.",
                        "status", "missing",
                        "statusLabel", "unavailable",
                        "route", "unknown",
                        "hazard", "unknown",
                        "prepHint", "unknown",
                        "serviceSummary", ""));
    }

    private static List<Map<String, Object>> discoveryRows() {
        Player player = player();
        try {
            return EchoCoreServices.resolvedDiscoveryEntries(player).stream()
                    .sorted(Comparator.comparing(resolved -> resolved.entry().id().toString()))
                    .map(WikiDataProviders::discoveryRow)
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static Map<String, Object> discoveryRow(EchoResolvedDiscoveryEntry resolved) {
        boolean locked = resolved.state() == EchoDiscoveryState.LOCKED;
        return row(
                "id", resolved.entry().id().toString(),
                "title", locked ? resolved.entry().lockedHintTitle() : resolved.entry().revealedTitle(),
                "summary", locked ? resolved.entry().hintText() : resolved.entry().revealedSummary(),
                "category", resolved.entry().category().name(),
                "state", resolved.state().name().toLowerCase(Locale.ROOT),
                "status", resolved.state().name().toLowerCase(Locale.ROOT),
                "chip", locked ? "locked" : "intel",
                "statusLabel", locked ? "locked" : "revealed",
                "meta", resolved.entry().category().name(),
                "countLabel", locked ? "hint" : "known",
                "icon", resolved.entry().iconArt() == null ? "minecraft:filled_map" : resolved.entry().iconArt().toString(),
                "selected", Objects.equals(resolved.entry().id(), state().selectedDiscovery()));
    }

    private static Map<String, Object> selectedDiscovery() {
        Identifier selected = state().selectedDiscovery();
        try {
            EchoResolvedDiscoveryEntry first = null;
            for (EchoResolvedDiscoveryEntry resolved : EchoCoreServices.resolvedDiscoveryEntries(player())) {
                if (first == null) {
                    first = resolved;
                }
                if (Objects.equals(resolved.entry().id(), selected)) {
                    return discoveryRow(resolved);
                }
            }
            if (first != null) {
                return discoveryRow(first);
            }
        } catch (RuntimeException ignored) {
        }
        return row(
                "id", "",
                "title", "Select Intel",
                "summary", "Choose a discovery row to inspect spoiler-safe state and reveal status.",
                "category", "Discovery",
                "state", "info",
                "status", "info",
                "chip", "intel",
                "statusLabel", "select row",
                "meta", "none",
                "countLabel", "0 entries",
                "icon", "minecraft:filled_map",
                "selected", false);
    }

    private static List<Map<String, Object>> worldContextRows() {
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        try {
            WorldContextSnapshot context = EchoCoreServices.worldContext(player());
            context.currentRegionOptional().ifPresent(region -> rows.add(row(
                    "id", region.definitionId().toString(),
                    "title", region.displayName(),
                    "summary", "Current region telemetry",
                    "status", "current",
                    "chip", "current",
                    "meta", region.type().name().toLowerCase(Locale.ROOT).replace('_', ' '),
                    "countLabel", "WorldCore")));
            WorldHazardSnapshot hazards = context.hazard();
            if (!hazards.safeZone()) {
                rows.add(row(
                        "id", "hazards",
                        "title", "Active Hazard Snapshot",
                        "summary", hazards.summary(),
                        "status", hazards.severity() > 50 ? "warning" : "active",
                        "chip", String.valueOf(hazards.severity()),
                        "meta", hazards.hazardIds().size() + " hazard(s)",
                        "countLabel", "snapshot"));
            }
        } catch (RuntimeException ignored) {
        }
        if (rows.isEmpty()) {
            rows.add(row("id", "nominal", "title", "World Context Nominal", "summary", "No active region or hazard telemetry is available.", "status", "known", "chip", "safe", "meta", "WorldCore", "countLabel", "idle"));
        }
        return List.copyOf(rows);
    }

    private static List<Map<String, Object>> optionalAddonRows() {
        return List.of(
                addon("ScreenCore", "echoscreencore"),
                addon("Terminal", "echoterminal"),
                addon("Index", "echoindex"),
                addon("WorldCore", "echoworldcore"),
                addon("MissionCore", "echomissioncore"),
                addon("TutorialCore", "echotutorialcore"));
    }

    private static Map<String, Object> addon(String label, String modId) {
        boolean loaded = WikiModuleAccess.isLoaded(modId);
        return row("id", modId, "title", label, "summary", loaded ? "integration online" : "optional integration absent", "status", loaded ? "ready" : "optional", "chip", loaded ? "online" : "off", "meta", modId, "countLabel", loaded ? "loaded" : "optional");
    }

    private static List<Map<String, Object>> dashboardCollectionRows() {
        return collectionRows().stream().limit(DASHBOARD_PREVIEW_LIMIT).toList();
    }

    private static List<Map<String, Object>> dashboardGuideBookRows() {
        return guideBookDataCache().visibleRows().stream().limit(DASHBOARD_PREVIEW_LIMIT).toList();
    }

    private static List<Map<String, Object>> dashboardArticleRows() {
        Player player = player();
        return filteredArticles().stream()
                .limit(DASHBOARD_PREVIEW_LIMIT)
                .map(article -> articleRow(article, player))
                .toList();
    }

    private static Map<String, Object> settings() {
        long guideBookWarnings = WikiContentRegistry.warnings().stream()
                .filter(warning -> normalized(warning).contains("guide book"))
                .count();
        return row(
                "screenCore", true,
                "query", state().searchQuery(),
                "category", state().category(),
                "articleCount", WikiContentRegistry.articles().size(),
                "collectionCount", WikiContentRegistry.collections().size(),
                "guideBookCount", GuideBookRegistry.guideBooks().size(),
                "visibleGuideBookCount", GuideBookRegistry.visibleGuideBooks().size(),
                "unavailableGuideBookCount", Math.max(0, GuideBookRegistry.guideBooks().size() - GuideBookRegistry.visibleGuideBooks().size()),
                "guideBookWarningCount", guideBookWarnings,
                "dataArticleCount", WikiContentRegistry.dataArticleCount(),
                "dataCollectionCount", WikiContentRegistry.dataCollectionCount(),
                "warnings", WikiContentRegistry.warnings().size(),
                "indexLoaded", WikiModuleAccess.isLoaded("echoindex"),
                "terminalLoaded", WikiModuleAccess.isLoaded("echoterminal"),
                "worldCoreLoaded", WikiModuleAccess.isLoaded("echoworldcore"),
                "missionCoreLoaded", WikiModuleAccess.isLoaded("echomissioncore"));
    }

    private static String itemLabel(Identifier id) {
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            return label(id);
        }
        return new ItemStack(item).getHoverName().getString();
    }

    private static String label(Identifier id) {
        if (id == null) {
            return "";
        }
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        if (slash >= 0 && slash < path.length() - 1) {
            path = path.substring(slash + 1);
        }
        String[] parts = path.split("_+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? id.toString() : builder.toString();
    }

    private static String normalized(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).strip();
    }

    private static String preview(String value, int maxLength) {
        String text = value == null ? "" : value.strip();
        if (maxLength <= 4 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3).stripTrailing() + "...";
    }

    private static <T> List<T> page(List<T> rows, int page, int pageSize) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        PageInfo info = pageInfo(rows.size(), page, pageSize);
        int start = info.page() * pageSize;
        int end = Math.min(rows.size(), start + pageSize);
        return List.copyOf(rows.subList(start, end));
    }

    private static PageInfo pageInfo(int totalRows, int requestedPage, int pageSize) {
        int safeSize = Math.max(1, pageSize);
        int totalPages = Math.max(1, (int) Math.ceil(Math.max(0, totalRows) / (double) safeSize));
        int page = Math.max(0, Math.min(Math.max(0, requestedPage), totalPages - 1));
        return new PageInfo(
                page,
                totalPages,
                page > 0,
                page < totalPages - 1,
                "Page " + (page + 1) + " / " + totalPages + " - " + totalRows + " result(s)");
    }

    private static Map<String, Object> row(Object... pairs) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }

    private record PageInfo(int page, int totalPages, boolean hasPrevious, boolean hasNext, String label) {
    }
}
