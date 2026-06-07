package com.knoxhack.echowiki.content;

import com.knoxhack.echowiki.EchoWiki;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class WikiContentRegistry {
    private static final Map<Identifier, WikiArticleDefinition> BUILTIN_ARTICLES = new LinkedHashMap<>();
    private static final Map<Identifier, WikiCollectionDefinition> BUILTIN_COLLECTIONS = new LinkedHashMap<>();
    private static volatile Map<Identifier, WikiArticleDefinition> dataArticles = Map.of();
    private static volatile Map<Identifier, WikiCollectionDefinition> dataCollections = Map.of();
    private static volatile List<String> warnings = List.of();
    private static volatile long revision;
    private static volatile ContentSnapshot cachedSnapshot;

    private WikiContentRegistry() {
    }

    public static synchronized void ensureDefaults() {
        if (!BUILTIN_ARTICLES.isEmpty()) {
            return;
        }
        WikiArticleDefinition firstHour = new WikiArticleDefinition(
                EchoWiki.id("survival/first_hour"),
                "First Hour Survival",
                "Survival",
                "Stabilize water, shelter, filtration, and route awareness before deeper salvage.",
                List.of(
                        new WikiArticleSection("Immediate Priorities",
                                "Secure clean water or a filtration route, avoid unscanned hazard zones, and keep a shelter marker in mind before committing to long searches.",
                                "body"),
                        new WikiArticleSection("Recommended Loop",
                                "Scan, salvage, return, refine, and only then push into higher-risk regions. ECHO systems reward short, prepared routes over blind wandering.",
                                "tip")),
                List.of("survival", "starter", "ashfall"),
                Identifier.fromNamespaceAndPath("minecraft", "compass"),
                null,
                List.of(EchoWiki.id("systems/survival_codex")),
                List.of(Identifier.fromNamespaceAndPath("minecraft", "water_bucket")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                0,
                10);
        WikiArticleDefinition codex = new WikiArticleDefinition(
                EchoWiki.id("systems/survival_codex"),
                "Using the Survival Codex",
                "Systems",
                "The Codex gathers ECHO articles, discoveries, regions, hazards, missions, factions, and related Index records.",
                List.of(
                        new WikiArticleSection("What It Shows",
                                "Articles are datapack-driven. Dynamic pages are assembled from EchoCore services, so optional addons can appear without the Wiki hardcoding their content.",
                                "body"),
                        new WikiArticleSection("Locked Intel",
                                "Discovery-linked entries show safe hints until the owning feature is discovered.",
                                "warning")),
                List.of("wiki", "screencore", "guide"),
                Identifier.fromNamespaceAndPath("minecraft", "written_book"),
                null,
                List.of(firstHour.id()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                0,
                20);
        BUILTIN_ARTICLES.put(firstHour.id(), firstHour);
        BUILTIN_ARTICLES.put(codex.id(), codex);
        BUILTIN_COLLECTIONS.put(EchoWiki.id("survival_basics"), new WikiCollectionDefinition(
                EchoWiki.id("survival_basics"),
                "Survival Basics",
                "Starter articles for stabilizing the early Ashfall route.",
                "Survival",
                List.of(firstHour.id(), codex.id()),
                10));
    }

    public static synchronized void replaceData(
            Map<Identifier, WikiArticleDefinition> articles,
            Map<Identifier, WikiCollectionDefinition> collections,
            List<String> loadWarnings) {
        ensureDefaults();
        dataArticles = Map.copyOf(articles == null ? Map.of() : articles);
        dataCollections = Map.copyOf(collections == null ? Map.of() : collections);
        warnings = List.copyOf(loadWarnings == null ? List.of() : loadWarnings);
        revision++;
        cachedSnapshot = null;
    }

    public static List<WikiArticleDefinition> articles() {
        return snapshot().articles();
    }

    public static Optional<WikiArticleDefinition> article(Identifier id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(snapshot().articleById().get(id));
    }

    public static List<WikiCollectionDefinition> collections() {
        return snapshot().collections();
    }

    public static List<String> warnings() {
        return warnings;
    }

    public static int dataArticleCount() {
        return dataArticles.size();
    }

    public static int dataCollectionCount() {
        return dataCollections.size();
    }

    public static synchronized void clearDataForTests() {
        dataArticles = Map.of();
        dataCollections = Map.of();
        warnings = List.of();
        revision++;
        cachedSnapshot = null;
    }

    public static long revision() {
        return revision;
    }

    private static ContentSnapshot snapshot() {
        ensureDefaults();
        ContentSnapshot snapshot = cachedSnapshot;
        if (snapshot != null && snapshot.revision() == revision) {
            return snapshot;
        }
        synchronized (WikiContentRegistry.class) {
            snapshot = cachedSnapshot;
            if (snapshot != null && snapshot.revision() == revision) {
                return snapshot;
            }
            snapshot = buildSnapshot();
            cachedSnapshot = snapshot;
            return snapshot;
        }
    }

    private static ContentSnapshot buildSnapshot() {
        Map<Identifier, WikiArticleDefinition> mergedArticles = new LinkedHashMap<>(BUILTIN_ARTICLES);
        mergedArticles.putAll(dataArticles);
        List<WikiArticleDefinition> articles = mergedArticles.values().stream()
                .sorted(Comparator.comparingInt(WikiArticleDefinition::sortOrder)
                        .thenComparing(article -> article.title().toLowerCase(java.util.Locale.ROOT))
                        .thenComparing(article -> article.id().toString()))
                .toList();

        Map<Identifier, WikiCollectionDefinition> mergedCollections = new LinkedHashMap<>(BUILTIN_COLLECTIONS);
        mergedCollections.putAll(dataCollections);
        List<WikiCollectionDefinition> collections = mergedCollections.values().stream()
                .sorted(Comparator.comparingInt(WikiCollectionDefinition::sortOrder)
                        .thenComparing(collection -> collection.title().toLowerCase(java.util.Locale.ROOT))
                        .thenComparing(collection -> collection.id().toString()))
                .toList();

        List<String> categories = articles.stream()
                .map(WikiArticleDefinition::category)
                .filter(category -> category != null && !category.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        return new ContentSnapshot(revision, Map.copyOf(mergedArticles), articles, collections, categories);
    }

    public static List<String> categories() {
        return snapshot().categories();
    }

    private record ContentSnapshot(
            long revision,
            Map<Identifier, WikiArticleDefinition> articleById,
            List<WikiArticleDefinition> articles,
            List<WikiCollectionDefinition> collections,
            List<String> categories) {
    }
}
