package com.knoxhack.echoindex.service;

import com.knoxhack.echocore.api.index.IndexRecipeView;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public final class IndexRecipeQueryClientState {
    private static final long REQUEST_COOLDOWN_MS = 750L;
    private static final Map<Identifier, QueryResult> RESULTS = new ConcurrentHashMap<>();
    private static final Map<Identifier, IndexRecipeDisplayMetadata> METADATA = new ConcurrentHashMap<>();
    private static final Map<Identifier, Long> LAST_REQUESTS = new ConcurrentHashMap<>();
    private static final AtomicLong REVISION = new AtomicLong();
    private static volatile Health health = Health.empty();
    private static volatile Identifier lastQueriedItem;
    private static volatile String lastQueryWarning = "";

    private IndexRecipeQueryClientState() {
    }

    public static void apply(Identifier itemId, CompoundTag tag) {
        if (itemId == null || tag == null) {
            return;
        }
        Health decodedHealth = Health.from(tag);
        health = decodedHealth;
        lastQueriedItem = itemId;
        lastQueryWarning = tag.getStringOr("query_warning", "");
        applyMetadata(tag.getListOrEmpty("recipes"));
        applyMetadata(tag.getListOrEmpty("uses"));
        applyMetadata(tag.getListOrEmpty("sources"));
        RESULTS.put(itemId, new QueryResult(
                itemId,
                decodedHealth.generation(),
                IndexRecipeSnapshotCodec.decodeRecipeViews(tag.getListOrEmpty("recipes")),
                IndexRecipeSnapshotCodec.decodeRecipeViews(tag.getListOrEmpty("uses")),
                IndexRecipeSnapshotCodec.decodeRecipeViews(tag.getListOrEmpty("sources")),
                lastQueryWarning,
                System.currentTimeMillis()));
        REVISION.incrementAndGet();
    }

    public static void applyHealth(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return;
        }
        Health decoded = Health.from(tag);
        if (decoded.generation() > health.generation()) {
            RESULTS.clear();
            METADATA.clear();
            REVISION.incrementAndGet();
        }
        health = decoded;
    }

    public static boolean shouldRequest(Identifier itemId) {
        if (itemId == null) {
            return false;
        }
        QueryResult result = RESULTS.get(itemId);
        if (result != null && result.generation() >= health.generation()) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long last = LAST_REQUESTS.get(itemId);
        if (last != null && now - last < REQUEST_COOLDOWN_MS) {
            return false;
        }
        LAST_REQUESTS.put(itemId, now);
        return true;
    }

    public static List<IndexRecipeView> recipesFor(Item item) {
        return result(item).map(QueryResult::recipes).orElse(List.of());
    }

    public static List<IndexRecipeView> usesFor(Item item) {
        return result(item).map(QueryResult::uses).orElse(List.of());
    }

    public static List<IndexRecipeView> sourcesFor(Item item) {
        return result(item).map(QueryResult::sources).orElse(List.of());
    }

    public static Optional<QueryResult> result(Item item) {
        if (item == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(RESULTS.get(IndexService.itemId(item)));
    }

    public static Optional<IndexRecipeView> recipe(Identifier recipeId) {
        if (recipeId == null) {
            return Optional.empty();
        }
        return RESULTS.values().stream()
                .flatMap(result -> result.allViews().stream())
                .filter(view -> recipeId.equals(view.id()))
                .findFirst();
    }

    public static Optional<IndexRecipeDisplayMetadata> metadata(Identifier recipeId) {
        if (recipeId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(METADATA.get(recipeId));
    }

    public static long revision() {
        return REVISION.get();
    }

    public static Health health() {
        return health;
    }

    public static Identifier lastQueriedItem() {
        return lastQueriedItem;
    }

    public static String lastQueryWarning() {
        return lastQueryWarning;
    }

    public static boolean hasResult(Item item) {
        return result(item).isPresent();
    }

    public static boolean loading(Item item) {
        return item != null && !hasResult(item);
    }

    private static void applyMetadata(net.minecraft.nbt.ListTag recipes) {
        IndexRecipeSnapshotCodec.decodeDisplayMetadata(recipes).forEach(METADATA::put);
    }

    public record QueryResult(
            Identifier itemId,
            long generation,
            List<IndexRecipeView> recipes,
            List<IndexRecipeView> uses,
            List<IndexRecipeView> sources,
            String warning,
            long receivedAtMillis) {
        public QueryResult {
            recipes = recipes == null ? List.of() : List.copyOf(recipes);
            uses = uses == null ? List.of() : List.copyOf(uses);
            sources = sources == null ? List.of() : List.copyOf(sources);
            warning = warning == null ? "" : warning;
        }

        public List<IndexRecipeView> allViews() {
            Map<Identifier, IndexRecipeView> views = new LinkedHashMap<>();
            recipes.forEach(view -> views.putIfAbsent(view.id(), view));
            uses.forEach(view -> views.putIfAbsent(view.id(), view));
            sources.forEach(view -> views.putIfAbsent(view.id(), view));
            return List.copyOf(views.values());
        }
    }

    public record Health(
            long generation,
            String reason,
            int rawRecipeCount,
            int adaptedRecipeCount,
            int sourceCardCount,
            int sourceFactCount,
            int usageItemCount,
            int providerCount,
            int skippedRecipeCount,
            int warningCount,
            String lastProviderError) {
        public static Health empty() {
            return new Health(0L, "empty", 0, 0, 0, 0, 0, 0, 0, 0, "");
        }

        static Health from(CompoundTag tag) {
            return new Health(
                    tag.getLongOr("generation", 0L),
                    tag.getStringOr("reason", "server query"),
                    tag.getIntOr("raw_recipe_count", 0),
                    tag.getIntOr("adapted_recipe_count", 0),
                    tag.getIntOr("source_card_count", 0),
                    tag.getIntOr("source_fact_count", 0),
                    tag.getIntOr("usage_item_count", 0),
                    tag.getIntOr("provider_count", 0),
                    tag.getIntOr("skipped_recipe_count", 0),
                    tag.getIntOr("warning_count", 0),
                    tag.getStringOr("last_provider_error", ""));
        }

        public boolean available() {
            return rawRecipeCount > 0 || adaptedRecipeCount > 0 || sourceFactCount > 0 || providerCount > 0;
        }
    }
}
