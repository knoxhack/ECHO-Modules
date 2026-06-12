package com.knoxhack.echoindex.service;

import com.echoplatform.echocore.api.index.IIndexContentProvider;
import com.echoplatform.echocore.api.index.IIndexDiscoveryService;
import com.echoplatform.echocore.api.index.IIndexOverlayService;
import com.echoplatform.echocore.api.index.IIndexRecipeProvider;
import com.echoplatform.echocore.api.index.IIndexRecipeService;
import com.echoplatform.echocore.api.index.IIndexRegistry;
import com.echoplatform.echocore.api.index.IIndexSearchService;
import com.echoplatform.echocore.api.index.IIndexService;
import com.echoplatform.echocore.api.index.IIndexSourceProvider;
import com.echoplatform.echocore.api.index.IndexBuildContext;
import com.echoplatform.echocore.api.index.IndexCategory;
import com.echoplatform.echocore.api.index.IndexContentSnapshot;
import com.echoplatform.echocore.api.index.IndexEntry;
import com.echoplatform.echocore.api.index.IndexEntryState;
import com.echoplatform.echocore.api.index.IndexMachineLayout;
import com.echoplatform.echocore.api.index.IndexMachineLayoutGauge;
import com.echoplatform.echocore.api.index.IndexMachineLayoutSlot;
import com.echoplatform.echocore.api.index.IndexProviderDiagnostic;
import com.echoplatform.echocore.api.index.IndexRecipeCategory;
import com.echoplatform.echocore.api.index.IndexRecipeSlot;
import com.echoplatform.echocore.api.index.IndexRecipeView;
import com.echoplatform.echocore.api.index.IndexRelation;
import com.echoplatform.echocore.api.index.IndexSearchResult;
import com.echoplatform.echocore.api.index.IndexSlotRole;
import com.echoplatform.echocore.api.index.IndexSourceFact;
import com.knoxhack.echoindex.Config;
import com.knoxhack.echoindex.EchoIndex;
import com.knoxhack.echoindex.IndexIds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class IndexService implements IIndexService, IIndexRegistry, IIndexRecipeService,
        IIndexSearchService, IIndexDiscoveryService, IIndexOverlayService {
    public static final IndexService INSTANCE = new IndexService();

    private final Map<Identifier, IndexCategory> categories = new LinkedHashMap<>();
    private final Map<Identifier, IndexEntry> entries = new LinkedHashMap<>();
    private final Map<Identifier, IndexCategory> dataCategories = new LinkedHashMap<>();
    private final Map<Identifier, IndexEntry> dataEntries = new LinkedHashMap<>();
    private final List<IIndexContentProvider> contentProviders = new CopyOnWriteArrayList<>();
    private final List<IIndexRecipeProvider> recipeProviders = new CopyOnWriteArrayList<>();
    private final List<IIndexSourceProvider> sourceProviders = new CopyOnWriteArrayList<>();
    private final Object recipeSnapshotLock = new Object();
    private final ThreadLocal<Boolean> contentSnapshotBuilding = ThreadLocal.withInitial(() -> false);
    private final AtomicLong recipeSnapshotGeneration = new AtomicLong();
    private final AtomicLong itemCatalogRevision = new AtomicLong();
    private volatile List<ItemStack> cachedItems = List.of();
    private volatile boolean cachedItemsClient;
    private volatile IndexRecipeSnapshot clientRecipeSnapshot;
    private volatile IndexRecipeSnapshot serverRecipeSnapshot;
    private volatile IndexContentSnapshot dataContentSnapshot = IndexContentSnapshot.empty(IndexIds.PROVIDER_DATAPACK);
    private volatile List<IndexContentSnapshot> latestContentSnapshots = List.of();
    private volatile Map<Identifier, IndexCategory> contentCategories = Map.of();
    private volatile Map<Identifier, IndexEntry> contentEntries = Map.of();
    private volatile List<IndexRelation> contentRelations = List.of();
    private volatile List<IndexProviderDiagnostic> contentDiagnostics = List.of();
    private volatile String clientSnapshotReason = "initial client build";
    private volatile String serverSnapshotReason = "initial server build";

    private IndexService() {
    }

    @Override
    public IIndexRegistry registry() {
        return this;
    }

    @Override
    public IIndexRecipeService recipes() {
        return this;
    }

    @Override
    public IIndexSearchService search() {
        return this;
    }

    @Override
    public IIndexDiscoveryService discovery() {
        return this;
    }

    @Override
    public IIndexOverlayService overlay() {
        return this;
    }

    @Override
    public boolean registerCategory(IndexCategory category) {
        if (category == null || category.id() == null) {
            return false;
        }
        synchronized (categories) {
            return categories.putIfAbsent(category.id(), category) == null;
        }
    }

    @Override
    public boolean registerEntry(IndexEntry entry) {
        if (entry == null || entry.id() == null) {
            return false;
        }
        synchronized (entries) {
            return entries.putIfAbsent(entry.id(), entry) == null;
        }
    }

    @Override
    public boolean registerContentProvider(IIndexContentProvider provider) {
        if (provider == null || provider.id() == null) {
            return false;
        }
        for (IIndexContentProvider existing : contentProviders) {
            if (provider.id().equals(existing.id())) {
                return existing == provider;
            }
        }
        contentProviders.add(provider);
        invalidateRecipes("content provider registered: " + provider.id());
        return true;
    }

    @Override
    public List<IndexContentSnapshot> contentSnapshots(Player player) {
        List<String> warnings = new ArrayList<>();
        List<IndexContentSnapshot> snapshots = collectContentProviderSnapshots(player, warnings);
        applyContentSnapshots(snapshots);
        return snapshots;
    }

    public void replaceDataDriven(Map<Identifier, IndexCategory> newCategories, Map<Identifier, IndexEntry> newEntries) {
        replaceDataDriven(newCategories, newEntries, IndexContentSnapshot.empty(IndexIds.PROVIDER_DATAPACK));
    }

    public void replaceDataDriven(Map<Identifier, IndexCategory> newCategories, Map<Identifier, IndexEntry> newEntries,
            IndexContentSnapshot contentSnapshot) {
        synchronized (categories) {
            for (Identifier id : dataCategories.keySet()) {
                categories.remove(id);
            }
            dataCategories.clear();
            if (newCategories != null) {
                dataCategories.putAll(newCategories);
                newCategories.values().forEach(category -> categories.put(category.id(), category));
            }
        }
        synchronized (entries) {
            for (Identifier id : dataEntries.keySet()) {
                entries.remove(id);
            }
            dataEntries.clear();
            if (newEntries != null) {
                dataEntries.putAll(newEntries);
                newEntries.values().forEach(entry -> entries.put(entry.id(), entry));
            }
        }
        dataContentSnapshot = contentSnapshot == null
                ? IndexContentSnapshot.empty(IndexIds.PROVIDER_DATAPACK)
                : contentSnapshot;
        refresh();
    }

    @Override
    public List<IndexCategory> categories(Player player) {
        refreshContentCaches(player);
        Map<Identifier, IndexCategory> merged = new LinkedHashMap<>();
        synchronized (categories) {
            merged.putAll(categories);
        }
        merged.putAll(contentCategories);
        return merged.values().stream()
                .sorted(Comparator.comparingInt(IndexCategory::sortOrder)
                        .thenComparing(category -> category.id().toString()))
                .toList();
    }

    @Override
    public List<IndexEntry> entries(Player player) {
        refreshContentCaches(player);
        Map<Identifier, IndexEntry> merged = new LinkedHashMap<>();
        synchronized (entries) {
            merged.putAll(entries);
        }
        merged.putAll(contentEntries);
        return merged.values().stream()
                .filter(entry -> visible(player, entry))
                .sorted(Comparator.comparingInt(IndexEntry::sortOrder)
                        .thenComparing(entry -> entry.id().toString()))
                .toList();
    }

    @Override
    public Optional<IndexEntry> entry(Player player, Identifier id) {
        if (id == null) {
            return Optional.empty();
        }
        refreshContentCaches(player);
        IndexEntry contentEntry = contentEntries.get(id);
        if (contentEntry != null) {
            return Optional.of(contentEntry);
        }
        synchronized (entries) {
            return Optional.ofNullable(entries.get(id));
        }
    }

    @Override
    public boolean registerProvider(IIndexRecipeProvider provider) {
        if (provider == null || provider.id() == null) {
            return false;
        }
        for (IIndexRecipeProvider existing : recipeProviders) {
            if (provider.id().equals(existing.id())) {
                return existing == provider;
            }
        }
        recipeProviders.add(provider);
        invalidateRecipes("recipe provider registered: " + provider.id());
        return true;
    }

    @Override
    public boolean registerSourceProvider(IIndexSourceProvider provider) {
        if (provider == null || provider.id() == null) {
            return false;
        }
        for (IIndexSourceProvider existing : sourceProviders) {
            if (provider.id().equals(existing.id())) {
                return existing == provider;
            }
        }
        sourceProviders.add(provider);
        invalidateRecipes("source provider registered: " + provider.id());
        return true;
    }

    @Override
    public List<IndexRecipeCategory> recipeCategories(Player player) {
        return recipeSnapshot(player).categories();
    }

    @Override
    public List<IndexRecipeView> recipes(Player player) {
        return recipeSnapshot(player).recipes();
    }

    @Override
    public List<IndexRecipeView> recipesFor(Player player, Item item) {
        return recipeSnapshot(player).recipesFor(item);
    }

    @Override
    public List<IndexRecipeView> usesFor(Player player, Item item) {
        return recipeSnapshot(player).usesFor(item);
    }

    @Override
    public Optional<IndexRecipeView> recipe(Player player, Identifier id) {
        return recipeSnapshot(player).recipe(id);
    }

    @Override
    public int providerCount() {
        return recipeProviders.size();
    }

    public int contentProviderCount() {
        return contentProviders.size();
    }

    public int sourceProviderCount() {
        return sourceProviders.size();
    }

    @Override
    public List<IndexSourceFact> sourceFacts(Player player) {
        recipeSnapshot(player);
        return IndexSourceRecipeProvider.INSTANCE.sourceFacts().stream()
                .map(source -> new IndexSourceFact(source.itemId(), source.sourceId(), source.kind(),
                        source.title(), source.notes(), source.icon(), source.sourceModId()))
                .toList();
    }

    public IndexRecipeSnapshot recipeSnapshot(Player player) {
        boolean client = recipeClientContext(player);
        IndexRecipeSnapshot snapshot = client ? clientRecipeSnapshot : serverRecipeSnapshot;
        if (snapshot != null && !staleClientSnapshot(player, snapshot)) {
            return snapshot;
        }
        synchronized (recipeSnapshotLock) {
            snapshot = client ? clientRecipeSnapshot : serverRecipeSnapshot;
            if (snapshot == null || staleClientSnapshot(player, snapshot)) {
                snapshot = buildRecipeSnapshot(player);
                if (client) {
                    clientRecipeSnapshot = snapshot;
                } else {
                    serverRecipeSnapshot = snapshot;
                }
            }
            return snapshot;
        }
    }

    public List<IndexRecipeProviderStats> providerStats(Player player) {
        return recipeSnapshot(player).providerStats();
    }

    public List<String> recipeWarnings(Player player) {
        return recipeSnapshot(player).warnings();
    }

    public void invalidateRecipes() {
        invalidateRecipes("manual invalidation");
    }

    public void invalidateRecipes(String reason) {
        String safeReason = reason == null || reason.isBlank() ? "manual invalidation" : reason.strip();
        clientSnapshotReason = safeReason;
        serverSnapshotReason = safeReason;
        clientRecipeSnapshot = null;
        serverRecipeSnapshot = null;
        cachedItems = List.of();
        cachedItemsClient = false;
        itemCatalogRevision.incrementAndGet();
    }

    public IndexRecipeSnapshot rebuildRecipes(Player player, String reason) {
        boolean client = recipeClientContext(player);
        setSnapshotReason(client, reason);
        synchronized (recipeSnapshotLock) {
            IndexRecipeSnapshot snapshot = buildRecipeSnapshot(player);
            if (client) {
                clientRecipeSnapshot = snapshot;
            } else {
                serverRecipeSnapshot = snapshot;
            }
            return snapshot;
        }
    }

    public IndexRecipeSnapshot recipeSnapshotForTests(Player player, List<IIndexRecipeProvider> providers) {
        return recipeSnapshotForTests(player, providers, List.of());
    }

    public IndexRecipeSnapshot recipeSnapshotForTests(Player player, List<IIndexRecipeProvider> providers,
            List<IIndexSourceProvider> sourceProvidersForTest) {
        return recipeSnapshotForTests(player, providers, sourceProvidersForTest, List.of());
    }

    public IndexRecipeSnapshot recipeSnapshotForTests(Player player, List<IIndexRecipeProvider> providers,
            List<IIndexSourceProvider> sourceProvidersForTest, List<IIndexContentProvider> contentProvidersForTest) {
        synchronized (recipeSnapshotLock) {
            List<IIndexContentProvider> previousContentProviders = List.copyOf(contentProviders);
            List<IIndexRecipeProvider> previousProviders = List.copyOf(recipeProviders);
            List<IIndexSourceProvider> previousSourceProviders = List.copyOf(sourceProviders);
            IndexRecipeSnapshot previousClientSnapshot = clientRecipeSnapshot;
            IndexRecipeSnapshot previousServerSnapshot = serverRecipeSnapshot;
            List<IndexContentSnapshot> previousContentSnapshots = latestContentSnapshots;
            Map<Identifier, IndexCategory> previousContentCategories = contentCategories;
            Map<Identifier, IndexEntry> previousContentEntries = contentEntries;
            List<IndexRelation> previousContentRelations = contentRelations;
            List<IndexProviderDiagnostic> previousContentDiagnostics = contentDiagnostics;
            String previousClientReason = clientSnapshotReason;
            String previousServerReason = serverSnapshotReason;
            try {
                contentProviders.clear();
                Set<Identifier> seenContentProviderIds = new LinkedHashSet<>();
                if (contentProvidersForTest != null) {
                    for (IIndexContentProvider provider : contentProvidersForTest) {
                        if (provider == null || provider.id() == null || !seenContentProviderIds.add(provider.id())) {
                            continue;
                        }
                        contentProviders.add(provider);
                    }
                }
                recipeProviders.clear();
                Set<Identifier> seenProviderIds = new LinkedHashSet<>();
                if (providers != null) {
                    for (IIndexRecipeProvider provider : providers) {
                        if (provider == null || provider.id() == null || !seenProviderIds.add(provider.id())) {
                            continue;
                        }
                        recipeProviders.add(provider);
                    }
                }
                sourceProviders.clear();
                Set<Identifier> seenSourceProviderIds = new LinkedHashSet<>();
                if (sourceProvidersForTest != null) {
                    for (IIndexSourceProvider provider : sourceProvidersForTest) {
                        if (provider == null || provider.id() == null || !seenSourceProviderIds.add(provider.id())) {
                            continue;
                        }
                        sourceProviders.add(provider);
                    }
                }
                setSnapshotReason(recipeClientContext(player), "test recipe snapshot");
                return buildRecipeSnapshot(player);
            } finally {
                contentProviders.clear();
                contentProviders.addAll(previousContentProviders);
                recipeProviders.clear();
                recipeProviders.addAll(previousProviders);
                sourceProviders.clear();
                sourceProviders.addAll(previousSourceProviders);
                clientRecipeSnapshot = previousClientSnapshot;
                serverRecipeSnapshot = previousServerSnapshot;
                latestContentSnapshots = previousContentSnapshots;
                contentCategories = previousContentCategories;
                contentEntries = previousContentEntries;
                contentRelations = previousContentRelations;
                contentDiagnostics = previousContentDiagnostics;
                clientSnapshotReason = previousClientReason;
                serverSnapshotReason = previousServerReason;
            }
        }
    }

    public void applySyncedRecipeSnapshot(CompoundTag tag) {
        IndexRecipeSnapshot snapshot = IndexRecipeSnapshotCodec.decode(tag);
        if (snapshot == null || snapshot.recipes().isEmpty()) {
            return;
        }
        clientSnapshotReason = snapshot.buildReason();
        clientRecipeSnapshot = snapshot;
    }

    private void setSnapshotReason(boolean client, String reason) {
        String safeReason = reason == null || reason.isBlank() ? "manual rebuild" : reason.strip();
        if (client) {
            clientSnapshotReason = safeReason;
        } else {
            serverSnapshotReason = safeReason;
        }
    }

    private boolean recipeClientContext(Player player) {
        return player != null && player.level() != null && player.level().getServer() == null;
    }

    private boolean staleClientSnapshot(Player player, IndexRecipeSnapshot snapshot) {
        if (!recipeClientContext(player) || snapshot == null) {
            return false;
        }
        Optional<IndexRecipeProviderStats> vanilla = snapshot.providerStats().stream()
                .filter(stats -> IndexIds.PROVIDER_VANILLA_RECIPES.equals(stats.providerId()))
                .findFirst();
        if (vanilla.isEmpty()) {
            return false;
        }
        return vanilla.get().rawRecipeCount() == 0 && VanillaIndexRecipeProvider.rawRecipeCount(player) > 0;
    }

    private IndexRecipeSnapshot buildRecipeSnapshot(Player player) {
        Map<Identifier, IndexRecipeCategory> mergedCategories = new LinkedHashMap<>();
        List<IndexRecipeView> mergedRecipes = new ArrayList<>();
        List<IndexRecipeProviderStats> stats = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<Identifier> seenRecipes = new LinkedHashSet<>();
        Map<String, IndexRecipeView> seenSemanticRecipes = new LinkedHashMap<>();
        Map<String, Identifier> seenSemanticProviders = new LinkedHashMap<>();
        Map<Identifier, List<IndexRecipeView>> byProviderMutable = new LinkedHashMap<>();
        List<IndexContentSnapshot> contentSnapshots = collectContentProviderSnapshots(player, warnings);
        applyContentSnapshots(contentSnapshots);
        IndexSourceRecipeProvider.INSTANCE.replaceProviderSources(collectSourceFacts(player, contentSnapshots, warnings));

        for (IndexContentSnapshot snapshot : contentSnapshots) {
            mergeRecipeViews(
                    snapshot.providerId(),
                    snapshot.recipeCategories(),
                    snapshot.recipes(),
                    mergedCategories,
                    mergedRecipes,
                    byProviderMutable,
                    seenRecipes,
                    seenSemanticRecipes,
                    seenSemanticProviders,
                    warnings);
            stats.add(statsForContentSnapshot(snapshot));
        }

        for (IIndexRecipeProvider provider : recipeProviders) {
            if (provider == null || provider.id() == null) {
                continue;
            }
            Identifier providerId = provider.id();
            List<IndexRecipeCategory> providerCategories = List.of();
            List<IndexRecipeView> providerRecipes = List.of();
            String error = "";
            try {
                List<IndexRecipeCategory> categories = provider.recipeCategories(player);
                if (categories != null) {
                    providerCategories = categories.stream()
                            .filter(category -> category != null && category.id() != null)
                            .toList();
                }
            } catch (RuntimeException exception) {
                error = compactError(exception);
                warnings.add("Provider " + provider.id() + " failed to list categories: " + error);
                EchoIndex.LOGGER.warn("Index recipe provider {} failed to list categories", provider.id(), exception);
            }
            try {
                List<IndexRecipeView> recipes = provider.recipes(player);
                if (recipes != null) {
                    providerRecipes = recipes.stream()
                    .filter(recipe -> recipe != null && recipe.id() != null && recipe.categoryId() != null)
                            .toList();
                }
            } catch (RuntimeException exception) {
                String recipeError = compactError(exception);
                error = error.isBlank() ? recipeError : error + "; " + recipeError;
                warnings.add("Provider " + provider.id() + " failed to list recipes: " + recipeError);
                EchoIndex.LOGGER.warn("Index recipe provider {} failed to list recipes", provider.id(), exception);
            }

            mergeRecipeViews(
                    providerId,
                    providerCategories,
                    providerRecipes,
                    mergedCategories,
                    mergedRecipes,
                    byProviderMutable,
                    seenRecipes,
                    seenSemanticRecipes,
                    seenSemanticProviders,
                    warnings);
            IndexRecipeProviderStats providerStats = statsForProvider(player, provider, providerCategories, providerRecipes, error);
            stats.add(providerStats);
            if (provider == VanillaIndexRecipeProvider.INSTANCE
                    && providerStats.rawRecipeCount() > 0
                    && providerStats.adaptedRecipeCount() == 0) {
                warnings.add("Vanilla recipes were received (" + providerStats.rawRecipeCount()
                        + " raw) but no recipe views were adapted.");
            }
        }

        List<IndexRecipeCategory> sortedCategories = mergedCategories.values().stream()
                .sorted(Comparator.comparingInt(IndexRecipeCategory::order)
                        .thenComparing(category -> category.id().toString()))
                .toList();
        Map<Identifier, Integer> categoryOrder = new LinkedHashMap<>();
        for (int i = 0; i < sortedCategories.size(); i++) {
            categoryOrder.put(sortedCategories.get(i).id(), i);
        }

        List<IndexRecipeView> sortedRecipes = mergedRecipes.stream()
                .sorted(recipeComparator(categoryOrder))
                .toList();
        Map<Item, List<IndexRecipeView>> byOutputMutable = new LinkedHashMap<>();
        Map<Item, List<IndexRecipeView>> byUsageMutable = new LinkedHashMap<>();
        Map<Identifier, List<IndexRecipeView>> byCategoryMutable = new LinkedHashMap<>();
        Map<Identifier, IndexRecipeView> byId = new LinkedHashMap<>();
        Map<Identifier, IndexRecipeDisplayMetadata> displayMetadata = new LinkedHashMap<>();
        Map<Identifier, Integer> categoryCounts = new LinkedHashMap<>();

        for (IndexRecipeView recipe : sortedRecipes) {
            byId.put(recipe.id(), recipe);
            VanillaIndexRecipeProvider.INSTANCE.metadataFor(recipe.id())
                    .ifPresent(metadata -> displayMetadata.put(recipe.id(), metadata));
            categoryCounts.merge(recipe.categoryId(), 1, Integer::sum);
            byCategoryMutable.computeIfAbsent(recipe.categoryId(), ignored -> new ArrayList<>()).add(recipe);
            addItems(byOutputMutable, recipe.itemsForRole(IndexSlotRole.OUTPUT), recipe);
            if (!sourceCard(recipe)) {
                addItems(byUsageMutable, recipe.itemsForRole(IndexSlotRole.INPUT), recipe);
                addItems(byUsageMutable, recipe.itemsForRole(IndexSlotRole.CATALYST), recipe);
                addItems(byUsageMutable, recipe.itemsForRole(IndexSlotRole.MACHINE), recipe);
            }
            if (!IndexRecipeSnapshot.hasRole(recipe, IndexSlotRole.OUTPUT)) {
                warnings.add("Recipe " + recipe.id() + " has no output slot.");
            }
            if (!sourceCard(recipe)
                    && !IndexRecipeSnapshot.hasRole(recipe, IndexSlotRole.INPUT)
                    && !IndexRecipeSnapshot.hasRole(recipe, IndexSlotRole.CATALYST)
                    && !IndexRecipeSnapshot.hasRole(recipe, IndexSlotRole.MACHINE)) {
                warnings.add("Recipe " + recipe.id() + " has no input, catalyst, or machine slot.");
            }
        }

        Map<Identifier, IndexMachineLayout> machineLayouts = collectMachineLayouts(contentSnapshots, byId, warnings);
        for (IndexMachineLayout layout : machineLayouts.values()) {
            IndexRecipeDisplayMetadata existing = displayMetadata.get(layout.recipeId());
            displayMetadata.put(layout.recipeId(), (existing == null
                    ? IndexRecipeDisplayMetadata.generic(layout.recipeId())
                    : existing).withMachineLayout(layout));
        }

        for (IndexRecipeCategory category : sortedCategories) {
            if (categoryCounts.getOrDefault(category.id(), 0) == 0) {
                warnings.add("Recipe category " + category.id() + " has zero recipe views.");
            }
        }

        boolean client = recipeClientContext(player);
        String buildReason = client ? clientSnapshotReason : serverSnapshotReason;
        return new IndexRecipeSnapshot(
                sortedCategories,
                sortedRecipes,
                freezeIdentifierIndex(byProviderMutable),
                freezeIdentifierIndex(byCategoryMutable),
                freezeIndex(byOutputMutable),
                freezeIndex(byUsageMutable),
                byId,
                displayMetadata,
                stats,
                warnings,
                System.currentTimeMillis(),
                recipeSnapshotGeneration.incrementAndGet(),
                buildReason);
    }

    private void refreshContentCaches(Player player) {
        if (contentProviders.isEmpty() && !hasContent(dataContentSnapshot)) {
            if (!latestContentSnapshots.isEmpty()) {
                applyContentSnapshots(List.of());
            }
            return;
        }
        List<String> warnings = new ArrayList<>();
        applyContentSnapshots(collectContentProviderSnapshots(player, warnings));
    }

    private List<IndexContentSnapshot> collectContentProviderSnapshots(Player player, List<String> warnings) {
        if (contentProviders.isEmpty() && !hasContent(dataContentSnapshot)) {
            return List.of();
        }
        if (contentSnapshotBuilding.get()) {
            return latestContentSnapshots;
        }
        contentSnapshotBuilding.set(true);
        try {
            List<IndexContentSnapshot> snapshots = new ArrayList<>();
            if (hasContent(dataContentSnapshot)) {
                snapshots.add(dataContentSnapshot);
            }
            IndexBuildContext context = IndexBuildContext.of(player, recipeClientContext(player), snapshotReason(player));
            for (IIndexContentProvider provider : contentProviders) {
                if (provider == null || provider.id() == null) {
                    continue;
                }
                try {
                    IndexContentSnapshot snapshot = provider.snapshot(context);
                    if (snapshot == null) {
                        snapshot = IndexContentSnapshot.empty(provider.id());
                    }
                    if (!provider.id().equals(snapshot.providerId())) {
                        warnings.add("Content provider " + provider.id()
                                + " emitted snapshot for " + snapshot.providerId() + ".");
                    }
                    snapshots.add(snapshot);
                    for (IndexProviderDiagnostic diagnostic : snapshot.diagnostics()) {
                        if (diagnostic.severity() != IndexProviderDiagnostic.Severity.INFO) {
                            warnings.add(diagnosticLine(diagnostic));
                        }
                    }
                } catch (RuntimeException exception) {
                    String error = compactError(exception);
                    warnings.add("Content provider " + provider.id() + " failed to build snapshot: " + error);
                    EchoIndex.LOGGER.warn("Index content provider {} failed to build snapshot", provider.id(), exception);
                }
            }
            return snapshots;
        } finally {
            contentSnapshotBuilding.set(false);
        }
    }

    private static boolean hasContent(IndexContentSnapshot snapshot) {
        return snapshot != null
                && (!snapshot.categories().isEmpty()
                || !snapshot.entries().isEmpty()
                || !snapshot.recipeCategories().isEmpty()
                || !snapshot.recipes().isEmpty()
                || !snapshot.machineLayouts().isEmpty()
                || !snapshot.sourceFacts().isEmpty()
                || !snapshot.relations().isEmpty()
                || !snapshot.diagnostics().isEmpty());
    }

    private static Map<Identifier, IndexMachineLayout> collectMachineLayouts(List<IndexContentSnapshot> snapshots,
            Map<Identifier, IndexRecipeView> byId, List<String> warnings) {
        if (snapshots == null || snapshots.isEmpty()) {
            return Map.of();
        }
        Map<Identifier, IndexMachineLayout> layouts = new LinkedHashMap<>();
        for (IndexContentSnapshot snapshot : snapshots) {
            if (snapshot == null || snapshot.machineLayouts().isEmpty()) {
                continue;
            }
            for (IndexMachineLayout layout : snapshot.machineLayouts()) {
                if (layout == null || layout.recipeId() == null) {
                    continue;
                }
                if (layouts.containsKey(layout.recipeId())) {
                    warnings.add("Content provider " + snapshot.providerId()
                            + " emitted duplicate machine layout for " + layout.recipeId() + ".");
                    continue;
                }
                IndexRecipeView recipe = byId.get(layout.recipeId());
                if (recipe == null) {
                    warnings.add("Content provider " + snapshot.providerId()
                            + " emitted machine layout for missing recipe " + layout.recipeId() + ".");
                    continue;
                }
                if (!validMachineLayout(snapshot.providerId(), recipe, layout, warnings)) {
                    continue;
                }
                layouts.put(layout.recipeId(), layout);
            }
        }
        return Map.copyOf(layouts);
    }

    private static boolean validMachineLayout(Identifier providerId, IndexRecipeView recipe, IndexMachineLayout layout,
            List<String> warnings) {
        if (layout.empty()) {
            warnings.add("Content provider " + providerId + " emitted empty machine layout for "
                    + layout.recipeId() + ".");
            return false;
        }
        for (IndexMachineLayoutSlot slot : layout.slots()) {
            if (slot.recipeSlotIndex() < -1 || slot.recipeSlotIndex() >= recipe.slots().size()) {
                warnings.add("Content provider " + providerId + " emitted machine layout " + layout.recipeId()
                        + " with invalid slot index " + slot.recipeSlotIndex() + ".");
                return false;
            }
            if (slot.size() <= 0 || slot.x() < 0 || slot.y() < 0
                    || slot.x() + slot.size() > layout.width()
                    || slot.y() + slot.size() > layout.height()) {
                warnings.add("Content provider " + providerId + " emitted out-of-bounds machine slot for "
                        + layout.recipeId() + ".");
                return false;
            }
        }
        for (IndexMachineLayoutGauge gauge : layout.gauges()) {
            if (gauge.width() <= 0 || gauge.height() <= 0 || gauge.x() < 0 || gauge.y() < 0
                    || gauge.x() + gauge.width() > layout.width()
                    || gauge.y() + gauge.height() > layout.height()) {
                warnings.add("Content provider " + providerId + " emitted out-of-bounds machine gauge for "
                        + layout.recipeId() + ".");
                return false;
            }
        }
        return true;
    }

    private void applyContentSnapshots(List<IndexContentSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            latestContentSnapshots = List.of();
            contentCategories = Map.of();
            contentEntries = Map.of();
            contentRelations = List.of();
            contentDiagnostics = List.of();
            return;
        }
        Map<Identifier, IndexCategory> snapshotCategories = new LinkedHashMap<>();
        Map<Identifier, IndexEntry> snapshotEntries = new LinkedHashMap<>();
        List<IndexRelation> snapshotRelations = new ArrayList<>();
        List<IndexProviderDiagnostic> snapshotDiagnostics = new ArrayList<>();
        for (IndexContentSnapshot snapshot : snapshots) {
            for (IndexCategory category : snapshot.categories()) {
                if (category != null && category.id() != null) {
                    snapshotCategories.putIfAbsent(category.id(), category);
                }
            }
            for (IndexEntry entry : snapshot.entries()) {
                if (entry != null && entry.id() != null && entry.categoryId() != null) {
                    snapshotEntries.putIfAbsent(entry.id(), entry);
                }
            }
            for (IndexRelation relation : snapshot.relations()) {
                if (relation != null && relation.id() != null) {
                    snapshotRelations.add(relation);
                }
            }
            snapshotDiagnostics.addAll(snapshot.diagnostics());
        }
        latestContentSnapshots = List.copyOf(snapshots);
        contentCategories = Map.copyOf(snapshotCategories);
        contentEntries = Map.copyOf(snapshotEntries);
        contentRelations = List.copyOf(snapshotRelations);
        contentDiagnostics = List.copyOf(snapshotDiagnostics);
    }

    private List<IndexSourceFact> collectSourceFacts(Player player,
            List<IndexContentSnapshot> snapshots, List<String> warnings) {
        List<IndexSourceFact> facts = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (IIndexSourceProvider provider : sourceProviders) {
            if (provider == null || provider.id() == null) {
                continue;
            }
            try {
                List<IndexSourceFact> providerFacts = provider.sourceFacts(player);
                if (providerFacts == null) {
                    continue;
                }
                for (IndexSourceFact fact : providerFacts) {
                    addSourceFact(facts, seen, fact, warnings, "Source provider " + provider.id());
                }
            } catch (RuntimeException exception) {
                String error = compactError(exception);
                warnings.add("Source provider " + provider.id() + " failed to list source facts: " + error);
                EchoIndex.LOGGER.warn("Index source provider {} failed to list source facts", provider.id(), exception);
            }
        }
        if (snapshots != null) {
            for (IndexContentSnapshot snapshot : snapshots) {
                for (IndexSourceFact fact : snapshot.sourceFacts()) {
                    addSourceFact(facts, seen, fact, warnings, "Content provider " + snapshot.providerId());
                }
            }
        }
        return facts;
    }

    private static void addSourceFact(List<IndexSourceFact> facts, Set<String> seen, IndexSourceFact fact,
            List<String> warnings, String source) {
        if (fact == null || fact.itemId() == null) {
            warnings.add(source + " emitted a source fact with no item id.");
            return;
        }
        String key = fact.itemId() + "|" + fact.kind() + "|" + fact.sourceId() + "|" + fact.title();
        if (!seen.add(key)) {
            warnings.add("Duplicate source fact skipped from " + source + ": " + key);
            return;
        }
        facts.add(fact);
    }

    private static void mergeRecipeViews(Identifier providerId,
            List<IndexRecipeCategory> providerCategories,
            List<IndexRecipeView> providerRecipes,
            Map<Identifier, IndexRecipeCategory> mergedCategories,
            List<IndexRecipeView> mergedRecipes,
            Map<Identifier, List<IndexRecipeView>> byProviderMutable,
            Set<Identifier> seenRecipes,
            Map<String, IndexRecipeView> seenSemanticRecipes,
            Map<String, Identifier> seenSemanticProviders,
            List<String> warnings) {
        if (providerId == null) {
            return;
        }
        if (providerCategories != null) {
            for (IndexRecipeCategory category : providerCategories) {
                if (category != null && category.id() != null) {
                    mergedCategories.putIfAbsent(category.id(), category);
                }
            }
        }
        if (providerRecipes == null) {
            return;
        }
        for (IndexRecipeView recipe : providerRecipes) {
            if (recipe == null || recipe.id() == null || recipe.categoryId() == null) {
                warnings.add("Provider " + providerId + " emitted a malformed recipe view.");
                continue;
            }
            String semanticKey = semanticRecipeKey(recipe);
            Identifier existingProvider = semanticKey.isBlank() ? null : seenSemanticProviders.get(semanticKey);
            if (existingProvider != null) {
                boolean existingImport = terminalImportProvider(existingProvider);
                boolean incomingImport = terminalImportProvider(providerId);
                if (existingImport && !incomingImport) {
                    IndexRecipeView existingRecipe = seenSemanticRecipes.get(semanticKey);
                    removeMergedRecipe(mergedRecipes, byProviderMutable, existingProvider, existingRecipe);
                    if (existingRecipe != null) {
                        seenRecipes.remove(existingRecipe.id());
                    }
                    warnings.add("Terminal-import duplicate replaced by direct recipe view: " + recipe.id());
                } else {
                    warnings.add("Semantic duplicate recipe view skipped: " + recipe.id());
                    continue;
                }
            }
            if (!seenRecipes.add(recipe.id())) {
                warnings.add("Duplicate recipe view skipped: " + recipe.id());
                continue;
            }
            mergedRecipes.add(recipe);
            byProviderMutable.computeIfAbsent(providerId, ignored -> new ArrayList<>()).add(recipe);
            if (!semanticKey.isBlank()) {
                seenSemanticRecipes.put(semanticKey, recipe);
                seenSemanticProviders.put(semanticKey, providerId);
            }
        }
    }

    private static IndexRecipeProviderStats statsForContentSnapshot(IndexContentSnapshot snapshot) {
        int sourceCards = (int) snapshot.recipes().stream().filter(IndexService::sourceCard).count();
        int sourceFacts = snapshot.sourceFacts().size();
        String error = "";
        for (IndexProviderDiagnostic diagnostic : snapshot.diagnostics()) {
            if (diagnostic.severity() == IndexProviderDiagnostic.Severity.ERROR) {
                error = error.isBlank() ? diagnosticLine(diagnostic) : error + "; " + diagnosticLine(diagnostic);
            }
        }
        return new IndexRecipeProviderStats(snapshot.providerId(), snapshot.recipeCategories().size(),
                snapshot.recipes().size() + sourceFacts, snapshot.recipes().size(), sourceFacts,
                sourceCards, 0, error);
    }

    private String snapshotReason(Player player) {
        return recipeClientContext(player) ? clientSnapshotReason : serverSnapshotReason;
    }

    private static String diagnosticLine(IndexProviderDiagnostic diagnostic) {
        String line = "Content provider " + diagnostic.providerId() + " " + diagnostic.severity().name().toLowerCase(Locale.ROOT)
                + ": " + diagnostic.message();
        if (!diagnostic.detail().isBlank()) {
            line += " (" + diagnostic.detail() + ")";
        }
        return line;
    }

    private static IndexRecipeProviderStats statsForProvider(Player player, IIndexRecipeProvider provider,
            List<IndexRecipeCategory> categories, List<IndexRecipeView> recipes, String error) {
        int raw = recipes.size();
        int adapted = recipes.size();
        int sourceCards = 0;
        int skipped = 0;
        int sourceFacts = 0;
        if (provider == VanillaIndexRecipeProvider.INSTANCE) {
            raw = VanillaIndexRecipeProvider.rawRecipeCount(player);
            skipped = VanillaIndexRecipeProvider.INSTANCE.skippedRecipeCount();
        } else if (provider == IndexSourceRecipeProvider.INSTANCE) {
            sourceFacts = IndexSourceRecipeProvider.INSTANCE.sourceFactCount();
            sourceCards = recipes.size();
            raw = sourceFacts;
            adapted = sourceCards;
        } else {
            sourceCards = (int) recipes.stream().filter(IndexService::sourceCard).count();
        }
        return new IndexRecipeProviderStats(provider.id(), categories.size(), raw, adapted, sourceFacts,
                sourceCards, skipped, error);
    }

    public List<String> validateContent(Player player) {
        List<String> warnings = new ArrayList<>();
        IndexRecipeSnapshot snapshot = recipeSnapshot(player);
        Map<Identifier, IndexCategory> allCategories;
        Map<Identifier, IndexEntry> allEntries;
        synchronized (categories) {
            allCategories = new LinkedHashMap<>(categories);
        }
        synchronized (entries) {
            allEntries = new LinkedHashMap<>(entries);
        }
        allCategories.putAll(contentCategories);
        allEntries.putAll(contentEntries);
        for (IndexEntry entry : allEntries.values()) {
            if (!allCategories.containsKey(entry.categoryId())) {
                warnings.add("Entry " + entry.id() + " references missing category " + entry.categoryId() + ".");
            }
            for (Identifier related : entry.relatedEntries()) {
                if (!allEntries.containsKey(related)) {
                    warnings.add("Entry " + entry.id() + " references missing related entry " + related + ".");
                }
            }
            for (Identifier itemId : entry.linkedItems()) {
                if (BuiltInRegistries.ITEM.getOptional(itemId).isEmpty()) {
                    warnings.add("Entry " + entry.id() + " references missing item " + itemId + ".");
                }
            }
            for (Identifier recipeId : entry.linkedRecipes()) {
                if (!snapshot.byId().containsKey(recipeId)) {
                    warnings.add("Entry " + entry.id() + " references missing indexed recipe " + recipeId + ".");
                }
            }
        }
        for (Identifier categoryId : allCategories.keySet()) {
            boolean hasEntry = allEntries.values().stream().anyMatch(entry -> categoryId.equals(entry.categoryId()));
            if (!hasEntry) {
                warnings.add("Category " + categoryId + " has no Index entries.");
            }
        }
        for (IndexSourceRecipeProvider.SourceFact source : IndexSourceRecipeProvider.INSTANCE.sourceFacts()) {
            if (source.itemId() == null || BuiltInRegistries.ITEM.getOptional(source.itemId()).isEmpty()) {
                warnings.add("Source fact " + source.sourceId() + " references missing item " + source.itemId() + ".");
            }
            if (source.sourceId() == null) {
                warnings.add("Source fact for " + source.itemId() + " has no source id.");
            }
        }
        Set<Identifier> knownRelationTargets = new LinkedHashSet<>(allEntries.keySet());
        knownRelationTargets.addAll(snapshot.byId().keySet());
        for (IndexSourceRecipeProvider.SourceFact source : IndexSourceRecipeProvider.INSTANCE.sourceFacts()) {
            if (source.sourceId() != null) {
                knownRelationTargets.add(source.sourceId());
            }
            if (source.itemId() != null) {
                knownRelationTargets.add(source.itemId());
            }
        }
        for (IndexRelation relation : contentRelations) {
            if (relation.fromId() == null || !knownRelationTargets.contains(relation.fromId())) {
                warnings.add("Relation " + relation.id() + " references missing source " + relation.fromId() + ".");
            }
            if (relation.toId() == null || !knownRelationTargets.contains(relation.toId())) {
                warnings.add("Relation " + relation.id() + " references missing target " + relation.toId() + ".");
            }
        }
        for (IndexProviderDiagnostic diagnostic : contentDiagnostics) {
            if (diagnostic.severity() != IndexProviderDiagnostic.Severity.INFO) {
                warnings.add(diagnosticLine(diagnostic));
            }
        }
        warnings.addAll(snapshot.warnings());
        return warnings.stream().distinct().toList();
    }

    private static Comparator<IndexRecipeView> recipeComparator(Map<Identifier, Integer> categoryOrder) {
        return Comparator.comparing(IndexService::sourceCard)
                .thenComparingInt(recipe -> categoryOrder.getOrDefault(recipe.categoryId(), Integer.MAX_VALUE))
                .thenComparing(recipe -> recipe.title().toLowerCase(Locale.ROOT))
                .thenComparing(recipe -> recipe.id().toString());
    }

    private static void removeMergedRecipe(List<IndexRecipeView> recipes,
            Map<Identifier, List<IndexRecipeView>> byProvider, Identifier providerId, IndexRecipeView recipe) {
        if (recipe == null) {
            return;
        }
        recipes.remove(recipe);
        List<IndexRecipeView> providerRecipes = byProvider.get(providerId);
        if (providerRecipes != null) {
            providerRecipes.remove(recipe);
            if (providerRecipes.isEmpty()) {
                byProvider.remove(providerId);
            }
        }
    }

    private static boolean terminalImportProvider(Identifier providerId) {
        return IndexIds.PROVIDER_TERMINAL_IMPORT.equals(providerId);
    }

    private static String semanticRecipeKey(IndexRecipeView recipe) {
        if (recipe == null || sourceCard(recipe)) {
            return "";
        }
        List<String> outputs = semanticRoleParts(recipe, IndexSlotRole.OUTPUT);
        if (outputs.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(recipe.sourceModId().toLowerCase(Locale.ROOT)).append('|');
        appendStackPart(builder, "machine", recipe.machine());
        appendRoleParts(builder, "input", semanticRoleParts(recipe, IndexSlotRole.INPUT));
        appendRoleParts(builder, "catalyst", semanticRoleParts(recipe, IndexSlotRole.CATALYST));
        appendRoleParts(builder, "machine", semanticRoleParts(recipe, IndexSlotRole.MACHINE));
        appendRoleParts(builder, "output", outputs);
        return builder.toString();
    }

    private static List<String> semanticRoleParts(IndexRecipeView recipe, IndexSlotRole role) {
        Set<String> uniqueParts = new LinkedHashSet<>();
        if (role == IndexSlotRole.MACHINE && !recipe.machine().isEmpty()) {
            uniqueParts.add(stackPart(recipe.machine()));
        }
        for (IndexRecipeSlot slot : recipe.slots()) {
            if (slot.role() != role) {
                continue;
            }
            if (slot.stacks().isEmpty()) {
                if (!slot.label().isBlank()) {
                    uniqueParts.add("label=" + normalize(slot.label()));
                }
                continue;
            }
            for (ItemStack stack : slot.stacks()) {
                if (!stack.isEmpty()) {
                    uniqueParts.add(stackPart(stack));
                }
            }
        }
        List<String> parts = new ArrayList<>(uniqueParts);
        parts.sort(String::compareTo);
        return parts;
    }

    private static void appendRoleParts(StringBuilder builder, String role, List<String> parts) {
        builder.append(role).append('=');
        for (String part : parts) {
            builder.append(part).append(',');
        }
        builder.append('|');
    }

    private static void appendStackPart(StringBuilder builder, String role, ItemStack stack) {
        builder.append(role).append('=');
        if (stack != null && !stack.isEmpty()) {
            builder.append(stackPart(stack));
        }
        builder.append('|');
    }

    private static String stackPart(ItemStack stack) {
        return itemId(stack.getItem()) + "@" + Math.max(1, stack.getCount());
    }

    private static Map<Item, List<IndexRecipeView>> freezeIndex(Map<Item, List<IndexRecipeView>> mutable) {
        Map<Item, List<IndexRecipeView>> frozen = new LinkedHashMap<>();
        mutable.forEach((item, recipes) -> frozen.put(item, List.copyOf(recipes)));
        return frozen;
    }

    private static Map<Identifier, List<IndexRecipeView>> freezeIdentifierIndex(
            Map<Identifier, List<IndexRecipeView>> mutable) {
        Map<Identifier, List<IndexRecipeView>> frozen = new LinkedHashMap<>();
        mutable.forEach((id, recipes) -> frozen.put(id, List.copyOf(recipes)));
        return frozen;
    }

    private static void addItems(Map<Item, List<IndexRecipeView>> index, Set<Item> items, IndexRecipeView recipe) {
        for (Item item : items) {
            if (item == null || item == Items.AIR) {
                continue;
            }
            index.computeIfAbsent(item, ignored -> new ArrayList<>()).add(recipe);
        }
    }

    private static boolean sourceCard(IndexRecipeView recipe) {
        return IndexRecipeSourceKind.isSourceCard(recipe);
    }

    private static String compactError(RuntimeException exception) {
        String message = exception.getMessage();
        String type = exception.getClass().getSimpleName();
        if (message == null || message.isBlank()) {
            return type;
        }
        return type + ": " + message.strip();
    }

    private static List<ItemStack> clientCatalogStacks(Player player) {
        if (!recipeClientContextStatic(player)) {
            return List.of();
        }
        try {
            Object result = Class.forName("com.knoxhack.echoindex.client.ClientRecipeDisplayAccess")
                    .getMethod("creativeCatalogStacks", Player.class)
                    .invoke(null, player);
            if (result instanceof List<?> list) {
                List<ItemStack> stacks = new ArrayList<>();
                for (Object candidate : list) {
                    if (candidate instanceof ItemStack stack && !stack.isEmpty()) {
                        stacks.add(stack.copy());
                    }
                }
                return stacks;
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoIndex.LOGGER.debug("Index could not merge creative-tab display stacks.", exception);
        }
        return List.of();
    }

    private static boolean recipeClientContextStatic(Player player) {
        return player != null && player.level() != null && player.level().getServer() == null;
    }

    private static String stackKey(ItemStack stack) {
        return itemId(stack.getItem()) + "#" + ItemStack.hashItemAndComponents(stack);
    }

    public List<ItemStack> itemCatalog(Player player) {
        boolean client = recipeClientContext(player);
        List<ItemStack> cached = cachedItems;
        if (Config.SEARCH_CACHE_ENABLED.get() && !cached.isEmpty() && cachedItemsClient == client) {
            return cached;
        }
        Map<String, ItemStack> stacks = new LinkedHashMap<>();
        BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .map(item -> new ItemStack(item))
                .forEach(stack -> stacks.putIfAbsent(stackKey(stack), stack));
        for (ItemStack stack : clientCatalogStacks(player)) {
            if (!stack.isEmpty()) {
                stacks.putIfAbsent(stackKey(stack), stack.copy());
            }
        }
        List<ItemStack> built = stacks.values().stream()
                .sorted(itemCatalogComparator())
                .toList();
        cachedItems = built;
        cachedItemsClient = client;
        return built;
    }

    public long itemCatalogRevision() {
        return itemCatalogRevision.get();
    }

    private static Comparator<ItemStack> itemCatalogComparator() {
        return Comparator
                .comparingInt((ItemStack stack) -> "minecraft".equals(itemId(stack.getItem()).getNamespace()) ? 0 : 1)
                .thenComparing(stack -> itemId(stack.getItem()).getNamespace().toLowerCase(Locale.ROOT))
                .thenComparing(stack -> stack.getHoverName().getString().toLowerCase(Locale.ROOT))
                .thenComparing(stack -> itemId(stack.getItem()).getPath())
                .thenComparing(IndexService::stackKey);
    }

    public List<ItemStack> filteredItems(Player player, String query, int maxResults) {
        String normalized = normalize(query);
        int limit = Math.max(1, Math.min(maxResults, Config.SEARCH_MAX_RESULTS.get()));
        return itemCatalog(player).stream()
                .filter(stack -> itemMatches(player, stack, normalized))
                .limit(limit)
                .toList();
    }

    public List<ItemStack> filteredItemsUnbounded(Player player, String query) {
        String normalized = normalize(query);
        return itemCatalog(player).stream()
                .filter(stack -> itemMatches(player, stack, normalized))
                .toList();
    }

    public int catalogCount(Player player) {
        return itemCatalog(player).size();
    }

    @Override
    public List<IndexSearchResult> search(Player player, String query, int maxResults) {
        String normalized = normalize(query);
        int limit = Math.max(1, Math.min(maxResults, Config.SEARCH_MAX_RESULTS.get()));
        List<IndexSearchResult> results = new ArrayList<>();
        for (ItemStack stack : itemCatalog(player)) {
            if (itemMatches(player, stack, normalized)) {
                Identifier id = itemId(stack.getItem());
                results.add(new IndexSearchResult(id, "item", stack.getHoverName().getString(), stack, score(id, normalized)));
            }
            if (results.size() >= limit) {
                return results;
            }
        }
        for (IndexEntry entry : entries(player)) {
            if (entryMatches(player, entry, normalized)) {
                results.add(new IndexSearchResult(entry.id(), "entry", entry.titleKey(), entry.icon(), score(entry.id(), normalized)));
            }
            if (results.size() >= limit) {
                break;
            }
        }
        results.sort(Comparator.comparingInt(IndexSearchResult::score).reversed()
                .thenComparing(result -> result.title().toLowerCase(Locale.ROOT)));
        return results;
    }

    @Override
    public void invalidate() {
        cachedItems = List.of();
        cachedItemsClient = false;
        invalidateRecipes("index cache invalidated");
    }

    public void refresh() {
        invalidate();
    }

    @Override
    public IndexEntryState state(Player player, Identifier entryId) {
        return IndexDiscoveryStore.INSTANCE.state(player, entryId);
    }

    @Override
    public boolean discover(ServerPlayer player, Identifier entryId) {
        return IndexDiscoveryStore.INSTANCE.discover(player, entryId);
    }

    @Override
    public boolean markRead(ServerPlayer player, Identifier entryId) {
        return IndexDiscoveryStore.INSTANCE.markRead(player, entryId);
    }

    @Override
    public boolean setBookmarked(ServerPlayer player, Identifier entryId, boolean bookmarked) {
        return IndexDiscoveryStore.INSTANCE.setBookmarked(player, entryId, bookmarked);
    }

    @Override
    public boolean isBookmarked(Player player, Identifier entryId) {
        if (player == null) {
            return ClientIndexState.isBookmarked(entryId);
        }
        return IndexDiscoveryStore.INSTANCE.isBookmarked(player, entryId);
    }

    @Override
    public Set<Identifier> bookmarks(Player player) {
        return player == null ? ClientIndexState.bookmarks() : IndexDiscoveryStore.INSTANCE.bookmarks(player);
    }

    public boolean setRecipePinned(ServerPlayer player, Identifier recipeId, boolean pinned) {
        return IndexDiscoveryStore.INSTANCE.setRecipePinned(player, recipeId, pinned);
    }

    public boolean isRecipePinned(Player player, Identifier recipeId) {
        if (player == null) {
            return ClientIndexState.isRecipePinned(recipeId);
        }
        return IndexDiscoveryStore.INSTANCE.isRecipePinned(player, recipeId);
    }

    public Set<Identifier> pinnedRecipes(Player player) {
        return player == null ? ClientIndexState.pinnedRecipes() : IndexDiscoveryStore.INSTANCE.pinnedRecipes(player);
    }

    @Override
    public boolean overlayEnabled(Player player) {
        return Config.OVERLAY_ENABLED.get();
    }

    @Override
    public boolean excludedScreen(String screenClassName) {
        if (screenClassName == null) {
            return false;
        }
        return screenClassName.contains("EchoTerminalScreen") || screenClassName.contains("IndexRecipeScreen");
    }

    private boolean visible(Player player, IndexEntry entry) {
        if (!Config.DISCOVERY_ENABLED.get()) {
            return true;
        }
        IndexEntryState state = IndexDiscoveryStore.INSTANCE.state(player, entry.id());
        return state != IndexEntryState.HIDDEN
                && (!Config.DISCOVERY_HIDE_LOCKED.get() || state != IndexEntryState.LOCKED);
    }

    private boolean itemMatches(Player player, ItemStack stack, String query) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (query.isBlank()) {
            return true;
        }
        Identifier id = itemId(stack.getItem());
        for (String token : query.split("\\s+")) {
            if (!itemMatchesToken(player, stack, id, token)) {
                return false;
            }
        }
        return true;
    }

    private boolean itemMatchesToken(Player player, ItemStack stack, Identifier id, String token) {
        if (token.isBlank()) {
            return true;
        }
        if (token.startsWith("@")) {
            return id.getNamespace().contains(token.substring(1));
        }
        if (token.startsWith("#")) {
            if (!Config.SEARCH_TAG_SEARCH.get()) {
                return false;
            }
            String needle = token.substring(1);
            return stack.getItem().builtInRegistryHolder().tags()
                    .map(TagKey::location)
                    .map(Identifier::toString)
                    .anyMatch(tag -> tag.contains(needle));
        }
        if (token.startsWith("$")) {
            return categoryToken(stack, id, token.substring(1));
        }
        if ("bookmarked".equals(token) || "favorite".equals(token)) {
            return bookmarks(player).contains(id);
        }
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        return name.contains(token)
                || (Config.SEARCH_REGISTRY_SEARCH.get() && id.toString().contains(token))
                || (Config.SEARCH_TOOLTIP_SEARCH.get()
                        && stack.getItem().getDescriptionId().toLowerCase(Locale.ROOT).contains(token));
    }

    private static boolean categoryToken(ItemStack stack, Identifier id, String category) {
        String token = category == null ? "" : category.toLowerCase(Locale.ROOT);
        Item item = stack.getItem();
        String path = id == null ? "" : id.getPath().toLowerCase(Locale.ROOT);
        String namespace = id == null ? "" : id.getNamespace().toLowerCase(Locale.ROOT);
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        return switch (token) {
            case "block", "blocks" -> item instanceof BlockItem;
            case "machine", "machines" -> item instanceof BlockItem && hasAny(path + " " + name,
                    "machine", "station", "bench", "workbench", "forge", "fabricator", "generator",
                    "press", "grinder", "compressor", "refinery", "smelter", "reclaimer", "scanner",
                    "terminal", "console", "dock", "beacon", "scrubber", "purifier", "hopper",
                    "condenser", "charger", "array", "core");
            case "tool", "tools" -> stack.isDamageableItem();
            case "combat", "weapon", "weapons" -> hasAny(path + " " + name,
                    "sword", "bow", "armor", "shield", "rifle", "gun", "blade", "hammer", "staff",
                    "dagger", "chakram", "gauntlet", "launcher", "lance", "knife");
            case "echo" -> namespace.startsWith("echo") || name.contains("echo") || path.contains("echo");
            default -> false;
        };
    }

    private static boolean hasAny(String value, String... needles) {
        String haystack = value == null ? "" : value;
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean entryMatches(Player player, IndexEntry entry, String query) {
        if (query.isBlank()) {
            return true;
        }
        String haystack = (entry.id() + " " + entry.categoryId() + " " + entry.titleKey() + " "
                + entry.subtitleKey() + " " + entry.summaryKey() + " " + String.join(" ", entry.tags()))
                .toLowerCase(Locale.ROOT);
        for (String token : query.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            if (token.startsWith("@")) {
                String mod = token.substring(1);
                if (!entry.id().getNamespace().contains(mod) && !entry.sourceModId().toLowerCase(Locale.ROOT).contains(mod)) {
                    return false;
                }
                continue;
            }
            if (token.startsWith("$")) {
                String category = token.substring(1);
                String categoryId = entry.categoryId().toString().toLowerCase(Locale.ROOT);
                boolean matched = categoryId.contains(category)
                        || ("machines".equals(category) && entry.tags().stream().anyMatch(value -> value.equalsIgnoreCase("machine")))
                        || ("machine".equals(category) && entry.tags().stream().anyMatch(value -> value.equalsIgnoreCase("machine")))
                        || ("echo".equals(category) && entry.sourceModId().toLowerCase(Locale.ROOT).startsWith("echo"));
                if (!matched) {
                    return false;
                }
                continue;
            }
            if (token.startsWith("#")) {
                String tag = token.substring(1);
                if (entry.tags().stream().noneMatch(value -> value.toLowerCase(Locale.ROOT).contains(tag))) {
                    return false;
                }
                continue;
            }
            if ("bookmarked".equals(token) || "favorite".equals(token)) {
                if (!bookmarks(player).contains(entry.id())) {
                    return false;
                }
                continue;
            }
            if ("discovered".equals(token)) {
                IndexEntryState state = state(player, entry.id());
                if (state != IndexEntryState.DISCOVERED
                        && state != IndexEntryState.COMPLETED
                        && state != IndexEntryState.ARCHIVED) {
                    return false;
                }
                continue;
            }
            if ("locked".equals(token)) {
                if (state(player, entry.id()) != IndexEntryState.LOCKED) {
                    return false;
                }
                continue;
            }
            if (!haystack.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private static int score(Identifier id, String query) {
        if (query.isBlank()) {
            return 1;
        }
        String value = id.toString();
        if (value.equals(query)) {
            return 1000;
        }
        if (value.endsWith(query)) {
            return 500;
        }
        return value.contains(query) ? 100 : 10;
    }

    private static String normalize(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    public static Identifier itemId(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? Identifier.fromNamespaceAndPath("minecraft", "air") : id;
    }
}
