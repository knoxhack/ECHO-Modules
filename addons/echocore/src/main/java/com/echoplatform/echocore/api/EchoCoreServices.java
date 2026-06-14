package com.echoplatform.echocore.api;

import com.knoxhack.echocore.EchoCore;
import com.echoplatform.echocore.api.config.EchoConfigRegistry;
import com.echoplatform.echocore.api.index.IIndexRegistry;
import com.echoplatform.echocore.api.index.IndexBuildContext;
import com.echoplatform.echocore.api.index.IndexContentSnapshot;
import com.echoplatform.echocore.api.index.IndexEntry;
import com.echoplatform.echocore.api.index.IIndexContentProvider;
import com.echoplatform.echocore.api.index.IIndexRecipeProvider;
import com.echoplatform.echocore.api.index.IIndexService;
import com.echoplatform.echocore.api.mission.IMissionRegistry;
import com.echoplatform.echocore.api.mission.IMissionService;
import com.echoplatform.echocore.api.mission.InMemoryMissionRegistry;
import com.echoplatform.echocore.api.mission.InMemoryMissionService;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.mission.NoOpMissionService;
import com.echoplatform.echocore.api.network.INetworkService;
import com.echoplatform.echocore.api.network.NoOpNetworkService;
import com.echoplatform.echocore.api.spine.EchoSpineBus;
import com.echoplatform.echocore.api.spine.InMemoryEchoSpineBus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class EchoCoreServices {
    public static final Identifier ACCEPT_FACTION_CONTRACT_ACTION =
            Identifier.fromNamespaceAndPath(EchoCore.MODID, "accept_faction_contract");
    public static final Identifier COMPLETE_FACTION_CONTRACT_ACTION =
            Identifier.fromNamespaceAndPath(EchoCore.MODID, "complete_faction_contract");

    private static final EchoServiceRegistry REGISTRY = new EchoServiceRegistry();
    private static final EchoRuntimeModules RUNTIME_MODULES = new EchoRuntimeModules();
    private static final List<IIndexContentProvider> INDEX_CONTENT_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<IIndexRecipeProvider> INDEX_RECIPE_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<EchoDiscoveryProvider> DISCOVERY_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<IMapDataProvider> MAP_DATA_PROVIDERS = new CopyOnWriteArrayList<>();
    private static final List<EchoFactionActionHandlerService> FACTION_ACTION_HANDLERS = new CopyOnWriteArrayList<>();
    private static final List<EchoRecoveryService> RECOVERY_SERVICES = new CopyOnWriteArrayList<>();
    private static final Map<String, Consumer<IMissionRegistry>> MISSION_CONTENT_REGISTRARS = new ConcurrentHashMap<>();
    private static final Map<Identifier, EchoFactionDefinition> FACTIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<Identifier, EchoFactionProfile>> FACTION_PROFILES = new ConcurrentHashMap<>();
    private static final Map<UUID, EchoProfile> PROFILES = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<Identifier>> DISCOVERED_FEATURES = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> UNLOCKED_ARCHIVES = new ConcurrentHashMap<>();
    private static volatile Function<Player, EchoHazardTelemetry> hazardTelemetryProvider = player -> EchoHazardTelemetry.nominal();
    private static volatile Function<Player, List<EchoDiagnosticBlocker>> diagnosticProvider = player -> List.of();
    private static volatile Function<Player, List<EchoRouteRecord>> routeRecordProvider = player -> List.of();
    private static volatile Predicate<Player> nexusPathProvider = player -> false;
    private static volatile NexusCampaignService nexusCampaignService = new NexusCampaignService() {
    };
    private static volatile IntelMirrorService intelMirrorService = (player, sourceModId, id, title, content) -> {
    };
    private static volatile TerminalPlacementService terminalPlacementService = new TerminalPlacementService() {
    };
    private static volatile TerminalRewardService terminalRewardService = new TerminalRewardService() {
    };
    private static volatile TerminalService terminalService = new TerminalService() {
    };
    private static volatile StructureDiscoveryService structureDiscoveryService = new StructureDiscoveryService() {
    };
    private static volatile IRuntimeBudgetService runtimeBudgetService = new IRuntimeBudgetService() {
    };
    private static volatile ILensService lensService = new ILensService() {
    };
    private static volatile ISoundService soundService = new ISoundService() {
    };
    private static final IMapMarkerService DEFAULT_MAP_MARKER_SERVICE = new IMapMarkerService() {
        @Override
        public boolean registerProvider(IMapDataProvider provider) {
            return registerMapDataProviderInternal(provider);
        }

        @Override
        public List<IMapLayer> layers(Player player) {
            return registeredMapLayers(player);
        }

        @Override
        public List<IMapMarker> markers(Player player) {
            return registeredMapMarkers(player);
        }

        @Override
        public boolean refresh(ServerPlayer player, String reason) {
            return refreshRegisteredMapProviders(player, reason);
        }

        @Override
        public int providerCount() {
            return MAP_DATA_PROVIDERS.size();
        }
    };
    private static volatile IMapMarkerService mapMarkerService = DEFAULT_MAP_MARKER_SERVICE;
    private static volatile IWorldRegionService worldRegionService = NoOpWorldService.INSTANCE;
    private static volatile Object indexService;

    static {
        bootstrapDefaults();
    }

    private EchoCoreServices() {
    }

    public static EchoServiceRegistry registry() {
        return REGISTRY;
    }

    public static EchoRuntimeModules runtimeModules() {
        return RUNTIME_MODULES;
    }

    public static EchoSpineBus spineBus() {
        return REGISTRY.require(EchoSpineBus.class);
    }

    public static EchoConfigRegistry configRegistry() {
        return REGISTRY.require(EchoConfigRegistry.class);
    }

    public static IMissionRegistry missionRegistry() {
        return REGISTRY.find(IMissionRegistry.class).orElse(NoOpMissionService.INSTANCE);
    }

    public static IMissionService missionService() {
        return REGISTRY.find(IMissionService.class).orElse(NoOpMissionService.INSTANCE);
    }

    public static void registerMissionService(IMissionService service) {
        if (service != null) {
            REGISTRY.register(IMissionService.class, service);
            REGISTRY.register(IMissionRegistry.class, service);
        }
    }

    public static INetworkService networkService() {
        return REGISTRY.find(INetworkService.class).orElse(NoOpNetworkService.INSTANCE);
    }

    public static IThemeService themeService() {
        return REGISTRY.require(IThemeService.class);
    }

    public static void registerThemeService(IThemeService service) {
        if (service != null) {
            REGISTRY.register(IThemeService.class, service);
        }
    }

    public static boolean themeCoreAvailable() {
        return themeService().available();
    }

    public static IDataService dataService() {
        return REGISTRY.find(IDataService.class).orElse(NoOpDataService.INSTANCE);
    }

    public static void registerDataService(IDataService service) {
        if (service != null) {
            REGISTRY.register(IDataService.class, service);
        }
    }

    public static IRegionService regionService() {
        return REGISTRY.find(IRegionService.class).orElse(NoOpWorldService.INSTANCE);
    }

    public static IPlayerDataView playerData(Player player) {
        return dataService().player(player);
    }

    public static IWorldDataView worldData(Level level) {
        return dataService().world(level);
    }

    public static ITeamDataView teamData(Level level, Identifier teamId) {
        return dataService().team(level, teamId);
    }

    public static IDataSyncBridge dataSyncBridge() {
        return dataService().syncBridge();
    }

    public static <T> IDataKey<T> registerDataKey(IDataKey<T> key) {
        return dataService().registerKey(key);
    }

    public static void registerNetworkService(INetworkService service) {
        if (service != null) {
            REGISTRY.register(INetworkService.class, service);
        }
    }

    public static com.echoplatform.echocore.api.diagnostic.EchoDiagnosticService diagnostics() {
        return REGISTRY.require(com.echoplatform.echocore.api.diagnostic.EchoDiagnosticService.class);
    }

    public static EchoHazardTelemetry hazardTelemetry(Player player) {
        try {
            EchoHazardTelemetry telemetry = hazardTelemetryProvider.apply(player);
            return telemetry == null ? EchoHazardTelemetry.nominal() : telemetry;
        } catch (RuntimeException exception) {
            return EchoHazardTelemetry.nominal();
        }
    }

    public static void registerHazardTelemetryService(Function<Player, EchoHazardTelemetry> provider) {
        hazardTelemetryProvider = provider == null ? player -> EchoHazardTelemetry.nominal() : provider;
    }

    public static List<EchoDiagnosticBlocker> diagnostics(Player player) {
        try {
            List<EchoDiagnosticBlocker> blockers = diagnosticProvider.apply(player);
            return blockers == null ? List.of() : List.copyOf(blockers);
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    public static void registerDiagnosticService(Function<Player, List<EchoDiagnosticBlocker>> provider) {
        diagnosticProvider = provider == null ? player -> List.of() : provider;
    }

    public static List<EchoRouteRecord> routeRecords(Player player) {
        try {
            List<EchoRouteRecord> records = routeRecordProvider.apply(player);
            return records == null ? List.of() : List.copyOf(records);
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    public static boolean discoverVisibleRouteRecords(ServerPlayer player) {
        boolean changed = false;
        for (EchoRouteRecord record : routeRecords(player)) {
            if (record != null && record.id() != null) {
                changed |= discoverFeature(player, routeDiscoveryId(record.id()));
            }
        }
        return changed;
    }

    public static void registerRouteRecordService(Function<Player, List<EchoRouteRecord>> provider) {
        routeRecordProvider = provider == null ? player -> List.of() : provider;
    }

    public static void registerRecoveryService(EchoRecoveryService service) {
        if (service != null && !RECOVERY_SERVICES.contains(service)) {
            RECOVERY_SERVICES.add(service);
        }
    }

    public static boolean recover(ServerPlayer player, String recoveryId) {
        if (player == null || recoveryId == null || recoveryId.isBlank()) {
            return false;
        }
        for (EchoRecoveryService service : RECOVERY_SERVICES) {
            try {
                if (service.recover(player, recoveryId)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return false;
    }

    public static Identifier routeDiscoveryId(Identifier recordId) {
        if (recordId == null) {
            return Identifier.fromNamespaceAndPath(EchoCore.MOD_ID, "route/unknown");
        }
        return Identifier.fromNamespaceAndPath(recordId.getNamespace(), "route/" + recordId.getPath());
    }

    public static boolean discoverFeature(Player player, Identifier featureId) {
        if (player == null || featureId == null) {
            return false;
        }
        return DISCOVERED_FEATURES
                .computeIfAbsent(player.getUUID(), ignored -> ConcurrentHashMap.newKeySet())
                .add(featureId);
    }

    public static boolean hasDiscoveredFeature(Player player, Identifier featureId) {
        return player != null && featureId != null
                && DISCOVERED_FEATURES.getOrDefault(player.getUUID(), Set.of()).contains(featureId);
    }

    public static void registerDiscoveryProvider(EchoDiscoveryProvider provider) {
        if (provider != null && DISCOVERY_PROVIDERS.stream().noneMatch(existing -> existing.getClass().equals(provider.getClass()))) {
            DISCOVERY_PROVIDERS.add(provider);
        }
    }

    public static List<EchoResolvedDiscoveryEntry> resolvedDiscoveryEntries(Player player) {
        List<EchoResolvedDiscoveryEntry> entries = new ArrayList<>();
        for (EchoDiscoveryProvider provider : DISCOVERY_PROVIDERS) {
            List<EchoDiscoveryEntry> provided;
            try {
                provided = provider.entries(player);
            } catch (RuntimeException exception) {
                provided = List.of();
            }
            for (EchoDiscoveryEntry entry : provided == null ? List.<EchoDiscoveryEntry>of() : provided) {
                EchoDiscoveryState state;
                try {
                    state = provider.state(player, entry);
                } catch (RuntimeException exception) {
                    state = EchoDiscoveryState.LOCKED;
                }
                if (state == EchoDiscoveryState.LOCKED && hasDiscoveredFeature(player, entry.id())) {
                    state = EchoDiscoveryState.DISCOVERED;
                }
                entries.add(new EchoResolvedDiscoveryEntry(entry, state));
            }
        }
        entries.sort(Comparator.comparingInt(value -> value.entry().sortOrder()));
        return List.copyOf(entries);
    }

    public static List<EchoDiscoveryEntry> discoveryEntries(Player player) {
        return resolvedDiscoveryEntries(player).stream()
                .map(EchoResolvedDiscoveryEntry::entry)
                .toList();
    }

    public static EchoDiscoveryState discoveryState(Player player, EchoDiscoveryEntry entry) {
        if (entry == null) {
            return EchoDiscoveryState.LOCKED;
        }
        for (EchoResolvedDiscoveryEntry resolved : resolvedDiscoveryEntries(player)) {
            if (resolved.entry() != null && entry.id() != null && entry.id().equals(resolved.entry().id())) {
                return resolved.state();
            }
        }
        return hasDiscoveredFeature(player, entry.id()) ? EchoDiscoveryState.DISCOVERED : EchoDiscoveryState.LOCKED;
    }

    public static void unlockArchive(Player player, String archiveId) {
        if (player != null && archiveId != null && !archiveId.isBlank()) {
            UNLOCKED_ARCHIVES.computeIfAbsent(player.getUUID(), ignored -> ConcurrentHashMap.newKeySet()).add(archiveId);
        }
    }

    public static boolean isArchiveUnlocked(Player player, String archiveId) {
        return player != null && archiveId != null
                && UNLOCKED_ARCHIVES.getOrDefault(player.getUUID(), Set.of()).contains(archiveId);
    }

    public static StructureDiscoveryService structureDiscoveryService() {
        return EchoServiceRegistry.find(IStructureDiscoveryService.class)
                .<StructureDiscoveryService>map(service -> service)
                .or(() -> EchoServiceRegistry.find(StructureDiscoveryService.class))
                .orElse(structureDiscoveryService);
    }

    public static void registerStructureDiscoveryService(StructureDiscoveryService service) {
        structureDiscoveryService = service == null ? new StructureDiscoveryService() {
        } : service;
        EchoServiceRegistry.register(StructureDiscoveryService.class, structureDiscoveryService);
        if (service instanceof IStructureDiscoveryService legacyService) {
            EchoServiceRegistry.register(IStructureDiscoveryService.class, legacyService);
        }
    }

    public static void registerMapDataProvider(IMapDataProvider provider) {
        registerMapDataProviderInternal(provider);
        if (provider != null && mapMarkerService != DEFAULT_MAP_MARKER_SERVICE) {
            mapMarkerService.registerProvider(provider);
        }
    }

    private static boolean registerMapDataProviderInternal(IMapDataProvider provider) {
        if (provider != null && provider.providerId() != null
                && MAP_DATA_PROVIDERS.stream().noneMatch(existing -> provider.providerId().equals(existing.providerId()))) {
            MAP_DATA_PROVIDERS.add(provider);
            return true;
        }
        return false;
    }

    public static void registerMapMarkerService(IMapMarkerService service) {
        mapMarkerService = service == null ? DEFAULT_MAP_MARKER_SERVICE : service;
        if (mapMarkerService != DEFAULT_MAP_MARKER_SERVICE) {
            for (IMapDataProvider provider : MAP_DATA_PROVIDERS) {
                mapMarkerService.registerProvider(provider);
            }
        }
    }

    public static IMapMarkerService mapMarkerService() {
        return mapMarkerService == null ? DEFAULT_MAP_MARKER_SERVICE : mapMarkerService;
    }

    public static List<IMapLayer> mapLayers(Player player) {
        try {
            List<IMapLayer> layers = mapMarkerService().layers(player);
            return layers == null ? List.of() : List.copyOf(layers);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    public static List<IMapMarker> mapMarkers(Player player) {
        try {
            List<IMapMarker> markers = mapMarkerService().markers(player);
            return markers == null ? List.of() : List.copyOf(markers);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    public static boolean refreshMapMarkers(ServerPlayer player, String reason) {
        try {
            return mapMarkerService().refresh(player, reason);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static List<IMapLayer> registeredMapLayers(Player player) {
        List<IMapLayer> layers = new ArrayList<>();
        for (IMapDataProvider provider : MAP_DATA_PROVIDERS) {
            try {
                List<IMapLayer> provided = provider.layers(player);
                if (provided != null) {
                    layers.addAll(provided);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return List.copyOf(layers);
    }

    private static List<IMapMarker> registeredMapMarkers(Player player) {
        List<IMapMarker> markers = new ArrayList<>();
        for (IMapDataProvider provider : MAP_DATA_PROVIDERS) {
            try {
                List<IMapMarker> provided = provider.markers(player);
                if (provided != null) {
                    markers.addAll(provided);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return List.copyOf(markers);
    }

    private static boolean refreshRegisteredMapProviders(ServerPlayer player, String reason) {
        boolean refreshed = false;
        for (IMapDataProvider provider : MAP_DATA_PROVIDERS) {
            try {
                refreshed |= provider.refresh(player, reason);
            } catch (RuntimeException ignored) {
            }
        }
        return refreshed;
    }

    public static void registerIndexContentProvider(IIndexContentProvider provider) {
        if (provider == null || provider.id() == null) {
            return;
        }
        if (INDEX_CONTENT_PROVIDERS.stream().noneMatch(value -> provider.id().equals(value.id()))) {
            INDEX_CONTENT_PROVIDERS.add(provider);
        }
        registerContentProviderWithIndexService(indexService, provider);
    }

    public static List<IIndexContentProvider> indexContentProviders() {
        return List.copyOf(INDEX_CONTENT_PROVIDERS);
    }

    public static void registerIndexRecipeProvider(IIndexRecipeProvider provider) {
        if (provider == null || provider.id() == null) {
            return;
        }
        if (INDEX_RECIPE_PROVIDERS.stream().noneMatch(value -> provider.id().equals(value.id()))) {
            INDEX_RECIPE_PROVIDERS.add(provider);
        }
        registerRecipeProviderWithIndexService(indexService, provider);
    }

    public static List<IIndexRecipeProvider> indexRecipeProviders() {
        return List.copyOf(INDEX_RECIPE_PROVIDERS);
    }

    public static void registerIndexService(Object service) {
        indexService = service;
        if (service != null) {
            for (IIndexContentProvider provider : INDEX_CONTENT_PROVIDERS) {
                registerContentProviderWithIndexService(service, provider);
            }
            for (IIndexRecipeProvider provider : INDEX_RECIPE_PROVIDERS) {
                registerRecipeProviderWithIndexService(service, provider);
            }
        }
    }

    public static Optional<IIndexService> indexService() {
        return indexService instanceof IIndexService service ? Optional.of(service) : Optional.empty();
    }

    public static List<IndexContentSnapshot> indexContentSnapshots(Player player) {
        Optional<IIndexService> service = indexService();
        if (service.isPresent()) {
            try {
                List<IndexContentSnapshot> snapshots = service.get().registry().contentSnapshots(player);
                return snapshots == null ? List.of() : List.copyOf(snapshots);
            } catch (RuntimeException ignored) {
            }
        }
        List<IndexContentSnapshot> snapshots = new ArrayList<>();
        IndexBuildContext context = IndexBuildContext.of(player, player != null && player.level().isClientSide(), "echocore_facade");
        for (IIndexContentProvider provider : INDEX_CONTENT_PROVIDERS) {
            try {
                IndexContentSnapshot snapshot = provider.snapshot(context);
                if (snapshot != null) {
                    snapshots.add(snapshot);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return List.copyOf(snapshots);
    }

    public static List<IndexEntry> indexEntries(Player player) {
        Optional<IIndexService> service = indexService();
        if (service.isPresent()) {
            try {
                List<IndexEntry> entries = service.get().registry().entries(player);
                return entries == null ? List.of() : List.copyOf(entries);
            } catch (RuntimeException ignored) {
            }
        }
        Map<Identifier, IndexEntry> entries = new LinkedHashMap<>();
        for (IndexContentSnapshot snapshot : indexContentSnapshots(player)) {
            for (IndexEntry entry : snapshot.entries()) {
                if (entry != null && entry.id() != null) {
                    entries.putIfAbsent(entry.id(), entry);
                }
            }
        }
        return entries.values().stream()
                .sorted(Comparator.comparingInt(IndexEntry::sortOrder)
                        .thenComparing(entry -> entry.id().toString()))
                .toList();
    }

    public static void invalidateIndexRecipes(String reason) {
        Object service = indexService;
        if (service == null) {
            return;
        }
        try {
            service.getClass().getMethod("invalidateRecipes", String.class).invoke(service, reason);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    public static boolean itemStackComponentsBound() {
        try {
            return net.minecraft.core.registries.BuiltInRegistries.ITEM != null;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static void registerContentProviderWithIndexService(Object service, IIndexContentProvider provider) {
        if (service == null || provider == null) {
            return;
        }
        try {
            if (service instanceof IIndexService index) {
                index.registry().registerContentProvider(provider);
                return;
            }
            if (service instanceof IIndexRegistry registry) {
                registry.registerContentProvider(provider);
                return;
            }
            service.getClass().getMethod("registerContentProvider", IIndexContentProvider.class).invoke(service, provider);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void registerRecipeProviderWithIndexService(Object service, IIndexRecipeProvider provider) {
        if (service == null || provider == null) {
            return;
        }
        try {
            service.getClass().getMethod("registerProvider", IIndexRecipeProvider.class).invoke(service, provider);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    public static void registerWorldRegionService(IWorldRegionService service) {
        worldRegionService = service == null ? NoOpWorldService.INSTANCE : service;
        REGISTRY.register(IRegionService.class, worldRegionService);
        REGISTRY.register(IWorldRegionService.class, worldRegionService);
    }

    public static IWorldRegionService worldRegions() {
        return REGISTRY.find(IWorldRegionService.class).orElse(NoOpWorldService.INSTANCE);
    }

    public static IWorldRegionService worldMarkerService() {
        return worldRegions();
    }

    public static IWorldRegionService hazardService() {
        return worldRegions();
    }

    public static void registerRuntimeBudgetService(IRuntimeBudgetService service) {
        runtimeBudgetService = service == null ? new IRuntimeBudgetService() {
        } : service;
    }

    public static IRuntimeBudgetService runtimeBudgetService() {
        return runtimeBudgetService;
    }

    public static void registerLensService(ILensService service) {
        lensService = service == null ? new ILensService() {
        } : service;
    }

    public static ILensService lensService() {
        return lensService;
    }

    public static void registerSoundService(ISoundService service) {
        soundService = service == null ? new ISoundService() {
        } : service;
        REGISTRY.register(ISoundService.class, soundService);
    }

    public static ISoundService soundService() {
        return REGISTRY.find(ISoundService.class).orElse(soundService);
    }

    public static boolean soundCoreAvailable() {
        return soundService().available();
    }

    public static List<EchoChapterCapability> chapterCapabilities(Player player) {
        Map<String, EchoChapterCapability> capabilities = new LinkedHashMap<>();
        for (EchoRuntimeModules.EchoRuntimeModule module : RUNTIME_MODULES.all()) {
            capabilities.put(module.id(), new EchoChapterCapability(
                    Identifier.fromNamespaceAndPath(EchoCore.MOD_ID, module.id()),
                    module.displayName(),
                    true,
                    true,
                    module.side()));
        }
        for (EchoAddonChapter chapter : EchoAddonRegistry.chapters()) {
            capabilities.put(chapter.id(), new EchoChapterCapability(
                    Identifier.fromNamespaceAndPath(EchoCore.MOD_ID, chapter.id()),
                    chapter.displayName(),
                    true,
                    chapter.isAvailable(player),
                    chapter.statusLine(player)));
        }
        return List.copyOf(capabilities.values());
    }

    public static WorldContextSnapshot worldContext(Player player) {
        WorldContextSnapshot context = worldRegions().worldContext(player);
        if (context != null && (context.currentRegionOptional().isPresent()
                || !context.activeRegions().isEmpty()
                || !context.nearbyMarkers().isEmpty()
                || !context.discoveredRegions().isEmpty())) {
            return context;
        }
        return new WorldContextSnapshot(Optional.empty(), WorldHazardSnapshot.fromTelemetry(hazardTelemetry(player)),
                mapMarkers(player), List.of());
    }

    public static EchoPackMode packMode(Player player) {
        if (EchoRuntimeModules.isLoaded("echoorbitalremnants") && !EchoRuntimeModules.isLoaded("echoashfallprotocol")) {
            return EchoPackMode.ORBITAL_STANDALONE;
        }
        return EchoRuntimeModules.isLoaded("echoashfallprotocol") ? EchoPackMode.ASHFALL : EchoPackMode.BASELINE;
    }

    public static String nexusCampaignStatusLine(Player player) {
        return nexusPathProvider.test(player) ? "Nexus path active." : "";
    }

    public static int nexusInstability(Player player) {
        return 0;
    }

    public static String nexusCampaignPathId(Player player) {
        return nexusPathProvider.test(player) ? "nexus" : "";
    }

    public static void registerMissionContent(String moduleId, Consumer<IMissionRegistry> registrar) {
        if (registrar != null) {
            String key = moduleId == null || moduleId.isBlank() ? registrar.getClass().getName() : moduleId;
            MISSION_CONTENT_REGISTRARS.put(key, registrar);
            registrar.accept(missionRegistry());
        }
    }

    public static void replayMissionContent(IMissionRegistry registry) {
        replayMissionContent(registry, Set.of());
    }

    public static void replayMissionContent(IMissionRegistry registry, Set<String> excludedModuleIds) {
        if (registry == null) {
            return;
        }
        Set<String> excluded = excludedModuleIds == null ? Set.of() : Set.copyOf(excludedModuleIds);
        for (Map.Entry<String, Consumer<IMissionRegistry>> entry : MISSION_CONTENT_REGISTRARS.entrySet()) {
            if (excluded.contains(entry.getKey())) {
                continue;
            }
            try {
                entry.getValue().accept(registry);
            } catch (RuntimeException ignored) {
            }
        }
    }

    public static void registerMissionHookCoverage(String moduleId, Identifier missionId, Identifier objectiveTarget) {
        missionService().registerHookCoverage(moduleId, missionId, objectiveTarget);
    }

    public static Map<String, String> missionHookCoverageSummary() {
        return missionService().missionHookCoverageBySource();
    }

    public static boolean recordMissionObjective(ServerPlayer player, MissionObjectiveType type, Identifier objectiveTarget,
            int amount) {
        return recordMissionObjective(player, type, objectiveTarget, amount, Map.of());
    }

    public static boolean recordMissionObjective(ServerPlayer player, MissionObjectiveType type, Identifier objectiveTarget,
            int amount, Map<String, String> context) {
        return missionService().recordObjective(player, type, objectiveTarget, amount, context);
    }

    public static boolean missionCoreAvailable() {
        return REGISTRY.find(IMissionService.class).map(IMissionService::available).orElse(false);
    }

    public static boolean startMission(ServerPlayer player, Identifier missionId) {
        return missionService().startMission(player, missionId);
    }

    public static boolean completeMission(ServerPlayer player, Identifier missionId) {
        return missionService().completeMission(player, missionId);
    }

    public static boolean claimMissionReward(ServerPlayer player, Identifier missionId) {
        return missionService().claimReward(player, missionId);
    }

    public static boolean handleMissionAction(ServerPlayer player, Identifier missionId, String actionId) {
        return missionService().handleAction(player, missionId, actionId);
    }

    public static boolean storeTerminalRewards(ServerPlayer player, String missionId, List<ItemStack> rewards) {
        return activeTerminalRewardService().storeRewards(player, missionId, rewards);
    }

    public static boolean claimTerminalRewards(ServerPlayer player) {
        return activeTerminalRewardService().claimRewards(player);
    }

    public static int pendingTerminalRewardCount(Player player) {
        return activeTerminalRewardService().pendingRewardCount(player);
    }

    public static void registerTerminalRewardService(TerminalRewardService service) {
        terminalRewardService = service == null ? new TerminalRewardService() {
        } : service;
        REGISTRY.register(TerminalRewardService.class, terminalRewardService);
    }

    public static boolean placeTerminal(Level level, BlockPos pos, Player owner) {
        return activeTerminalPlacementService().placeTerminal(level, pos, owner);
    }

    public static BlockState terminalStructureBlockState() {
        return activeTerminalPlacementService().structureBlockState();
    }

    public static boolean isTerminalBlock(BlockState state) {
        return state != null && activeTerminalPlacementService().isTerminalBlock(state);
    }

    public static void registerTerminalPlacementService(TerminalPlacementService service) {
        terminalPlacementService = service == null ? new TerminalPlacementService() {
        } : service;
        REGISTRY.register(TerminalPlacementService.class, terminalPlacementService);
    }

    public static void registerTerminalService(TerminalService service) {
        terminalService = service == null ? new TerminalService() {
        } : service;
        REGISTRY.register(TerminalService.class, terminalService);
    }

    public static TerminalService terminalService() {
        return REGISTRY.find(TerminalService.class).orElse(terminalService);
    }

    private static TerminalPlacementService activeTerminalPlacementService() {
        return REGISTRY.find(TerminalPlacementService.class).orElse(terminalPlacementService);
    }

    private static TerminalRewardService activeTerminalRewardService() {
        return REGISTRY.find(TerminalRewardService.class).orElse(terminalRewardService);
    }

    public static EchoProfile profile(Player player) {
        if (player == null) {
            return EchoProfile.empty();
        }
        return PROFILES.computeIfAbsent(player.getUUID(), ignored -> EchoProfile.empty());
    }

    public static EchoProgressLedger progressLedger(Player player) {
        return new EchoProgressLedger(profile(player).milestones());
    }

    public static void saveProfile(ServerPlayer player, EchoProfile profile) {
        if (player != null && profile != null) {
            PROFILES.put(player.getUUID(), profile);
        }
    }

    public static void recordMilestone(ServerPlayer player, String milestone) {
        if (player != null) {
            PROFILES.compute(player.getUUID(), (ignored, profile) ->
                    (profile == null ? EchoProfile.empty() : profile).recordMilestone(milestone));
        }
    }

    public static void recordMilestone(Player player, String milestone) {
        if (player != null) {
            PROFILES.compute(player.getUUID(), (ignored, profile) ->
                    (profile == null ? EchoProfile.empty() : profile).recordMilestone(milestone));
        }
    }

    public static void registerNexusPathService(Predicate<Player> provider) {
        nexusPathProvider = provider == null ? player -> false : provider;
    }

    public static void registerNexusCampaignService(NexusCampaignService service) {
        nexusCampaignService = service == null ? new NexusCampaignService() {
        } : service;
    }

    public static NexusCampaignService nexusCampaignService() {
        return nexusCampaignService;
    }

    public static void registerIntelMirrorService(IntelMirrorService service) {
        intelMirrorService = service == null ? (player, sourceModId, id, title, content) -> {
        } : service;
    }

    public static void mirrorIntel(ServerPlayer player, String sourceModId, String id, String title, String content) {
        intelMirrorService.mirror(player, sourceModId, id, title, content);
    }

    public static void registerFaction(EchoFactionDefinition definition) {
        if (definition != null && definition.id() != null) {
            FACTIONS.put(definition.id(), definition);
        }
    }

    public static List<EchoFactionDefinition> factionDefinitions() {
        return FACTIONS.values().stream()
                .sorted(Comparator.comparing(EchoFactionDefinition::displayName))
                .toList();
    }

    public static Optional<EchoFactionDefinition> factionDefinition(Identifier factionId) {
        return Optional.ofNullable(FACTIONS.get(factionId));
    }

    public static List<EchoFactionProfile> factionProfiles(Player player) {
        if (player == null) {
            return List.of();
        }
        Map<Identifier, EchoFactionProfile> profiles = profilesFor(player);
        for (EchoFactionDefinition definition : FACTIONS.values()) {
            profiles.computeIfAbsent(definition.id(), ignored -> new EchoFactionProfile(
                    definition, 0, false, 0, 0L, "", "Neutral", "", null, Set.of()));
        }
        return profiles.values().stream()
                .sorted(Comparator.comparing(profile -> profile.definition().displayName()))
                .toList();
    }

    public static Optional<EchoFactionProfile> factionProfile(Player player, Identifier factionId) {
        if (player == null || factionId == null) {
            return Optional.empty();
        }
        EchoFactionDefinition definition = FACTIONS.get(factionId);
        if (definition == null) {
            return Optional.empty();
        }
        return Optional.of(profilesFor(player).computeIfAbsent(factionId, ignored -> new EchoFactionProfile(
                definition, 0, false, 0, 0L, "", "Neutral", "", null, Set.of())));
    }

    public static void addFactionReputation(ServerPlayer player, Identifier factionId, int delta) {
        factionProfile(player, factionId).ifPresent(profile ->
                profilesFor(player).put(factionId, profile.withReputation(profile.reputation() + delta)));
    }

    public static void addFactionReputation(Player player, Identifier factionId, int delta) {
        factionProfile(player, factionId).ifPresent(profile ->
                profilesFor(player).put(factionId, profile.withReputation(profile.reputation() + delta)));
    }

    public static void setFactionReputation(Player player, Identifier factionId, int value) {
        if (player == null || factionId == null) {
            return;
        }
        EchoFactionProfile profile = factionProfileOrCreate(player, factionId);
        profilesFor(player).put(factionId, profile.withReputation(value));
    }

    public static void markFactionContacted(ServerPlayer player, Identifier factionId) {
        recordFactionInteraction(player, factionId, "", player == null ? 0L : player.level().getGameTime());
    }

    public static void markFactionContacted(Player player, Identifier factionId) {
        recordFactionInteraction(player, factionId, "", player == null ? 0L : player.level().getGameTime());
    }

    public static void recordFactionInteraction(ServerPlayer player, Identifier factionId, String roleId, long tick) {
        factionProfile(player, factionId).ifPresent(profile ->
                profilesFor(player).put(factionId, profile.contacted(tick, roleId, profile.npcMemory())));
    }

    public static void recordFactionInteraction(Player player, Identifier factionId, String roleId, long tick) {
        if (player == null || factionId == null) {
            return;
        }
        EchoFactionProfile profile = factionProfileOrCreate(player, factionId);
        profilesFor(player).put(factionId, profile.contacted(tick, roleId, profile.npcMemory()));
    }

    public static void rememberFactionNpc(ServerPlayer player, Identifier factionId, String memory) {
        factionProfile(player, factionId).ifPresent(profile ->
                profilesFor(player).put(factionId, new EchoFactionProfile(
                        profile.definition(), profile.reputation(), true, Math.max(1, profile.contactCount()),
                        player == null ? profile.lastInteractionTick() : player.level().getGameTime(),
                        profile.lastRoleId(), profile.standing(), memory, profile.activeContractId(),
                        profile.completedContractIds())));
    }

    public static void setFactionActiveContract(ServerPlayer player, Identifier factionId, Identifier contractId) {
        if (player == null || factionId == null || contractId == null) {
            return;
        }
        EchoFactionProfile profile = factionProfileOrCreate(player, factionId);
        profilesFor(player).put(factionId, profile.withActiveContract(contractId));
    }

    public static void clearFactionActiveContract(ServerPlayer player, Identifier factionId) {
        if (player == null || factionId == null) {
            return;
        }
        EchoFactionProfile profile = factionProfileOrCreate(player, factionId);
        profilesFor(player).put(factionId, profile.withoutActiveContract());
    }

    public static void markFactionContractCompleted(ServerPlayer player, Identifier factionId, Identifier contractId) {
        if (player == null || factionId == null || contractId == null) {
            return;
        }
        EchoFactionProfile profile = factionProfileOrCreate(player, factionId);
        profilesFor(player).put(factionId, profile.withoutActiveContract().withCompletedContract(contractId));
    }

    public static Optional<EchoFactionInteractionSnapshot> factionInteractionSnapshot(
            Player player, Identifier factionId, String roleId) {
        return factionProfile(player, factionId).map(profile -> {
            List<EchoFactionAction> actions = new ArrayList<>(profile.definition().actions());
            String localContext = "";
            for (EchoFactionActionHandlerService handler : FACTION_ACTION_HANDLERS) {
                if (handler.supports(factionId)) {
                    actions.addAll(handler.actions(player, profile, roleId));
                    localContext = handler.localContext(player, profile, roleId);
                }
            }
            return new EchoFactionInteractionSnapshot(profile, roleId, actions, profile.definition().contracts(), localContext);
        });
    }

    public static EchoFactionContractState factionContractState(
            Player player, Identifier factionId, Identifier contractId, String roleId) {
        EchoFactionProfile profile = factionProfile(player, factionId).orElse(null);
        if (profile == null || contractId == null) {
            return new EchoFactionContractState(contractId, false, false, false, false, "", "Faction unavailable.");
        }
        EchoFactionContract contract = profile.definition().contracts().stream()
                .filter(value -> contractId.equals(value.id()))
                .findFirst()
                .orElse(null);
        if (contract == null) {
            return new EchoFactionContractState(contractId, false, false, false, false, "", "Contract unavailable.");
        }
        for (EchoFactionActionHandlerService handler : FACTION_ACTION_HANDLERS) {
            if (handler.supports(factionId)) {
                return handler.contractState(player, profile, contract, roleId);
            }
        }
        boolean unlocked = profile.reputation() >= contract.requiredReputation();
        return new EchoFactionContractState(contractId, unlocked, false, false, false,
                unlocked ? "Ready to accept." : "Requires standing " + contract.requiredReputation() + ".",
                unlocked ? "" : "Raise faction standing.");
    }

    public static EchoFactionActionResult performFactionAction(
            ServerPlayer player, Identifier factionId, Identifier actionId, String roleId, Identifier targetId) {
        for (EchoFactionActionHandlerService handler : FACTION_ACTION_HANDLERS) {
            if (handler.supports(factionId)) {
                return handler.handle(player, factionId, actionId, roleId, targetId);
            }
        }
        return EchoFactionActionResult.failure("Unavailable", "No faction handler is registered.");
    }

    public static void registerFactionActionHandler(EchoFactionActionHandlerService handler) {
        if (handler != null && FACTION_ACTION_HANDLERS.stream().noneMatch(existing -> existing.getClass().equals(handler.getClass()))) {
            FACTION_ACTION_HANDLERS.add(handler);
        }
    }

    public static void syncFactionDataToClient(ServerPlayer player) {
    }

    public static String platformProviderSummary() {
        return "discoveries=" + DISCOVERY_PROVIDERS.size()
                + ", factions=" + FACTIONS.size()
                + ", maps=" + MAP_DATA_PROVIDERS.size()
                + ", indexContent=" + INDEX_CONTENT_PROVIDERS.size()
                + ", dataKeys=" + dataService().registeredKeys().size();
    }

    public static List<EchoModuleInfo> moduleReport() {
        return RUNTIME_MODULES.all().stream()
                .map(module -> new EchoModuleInfo(module.id(), module.version(), module.side(), module.official()))
                .toList();
    }

    public static void replayDeferredContent(String reason) {
    }

    public static synchronized void clearPlatformServicesForTests() {
        EchoServiceRegistry.clear();
        INDEX_CONTENT_PROVIDERS.clear();
        INDEX_RECIPE_PROVIDERS.clear();
        DISCOVERY_PROVIDERS.clear();
        MAP_DATA_PROVIDERS.clear();
        FACTION_ACTION_HANDLERS.clear();
        RECOVERY_SERVICES.clear();
        MISSION_CONTENT_REGISTRARS.clear();
        FACTIONS.clear();
        FACTION_PROFILES.clear();
        PROFILES.clear();
        DISCOVERED_FEATURES.clear();
        UNLOCKED_ARCHIVES.clear();
        hazardTelemetryProvider = player -> EchoHazardTelemetry.nominal();
        diagnosticProvider = player -> List.of();
        routeRecordProvider = player -> List.of();
        nexusPathProvider = player -> false;
        nexusCampaignService = new NexusCampaignService() {
        };
        intelMirrorService = (player, sourceModId, id, title, content) -> {
        };
        terminalPlacementService = new TerminalPlacementService() {
        };
        terminalRewardService = new TerminalRewardService() {
        };
        terminalService = new TerminalService() {
        };
        structureDiscoveryService = new StructureDiscoveryService() {
        };
        runtimeBudgetService = new IRuntimeBudgetService() {
        };
        lensService = new ILensService() {
        };
        soundService = new ISoundService() {
        };
        mapMarkerService = DEFAULT_MAP_MARKER_SERVICE;
        worldRegionService = NoOpWorldService.INSTANCE;
        indexService = null;
        bootstrapDefaults();
    }

    private static Map<Identifier, EchoFactionProfile> profilesFor(Player player) {
        return FACTION_PROFILES.computeIfAbsent(player.getUUID(), ignored -> new ConcurrentHashMap<>());
    }

    private static EchoFactionProfile factionProfileOrCreate(Player player, Identifier factionId) {
        EchoFactionDefinition definition = FACTIONS.computeIfAbsent(factionId, EchoCoreServices::minimalFactionDefinition);
        return profilesFor(player).computeIfAbsent(factionId, ignored -> new EchoFactionProfile(
                definition, 0, false, 0, 0L, "", "Neutral", "", null, Set.of()));
    }

    private static EchoFactionDefinition minimalFactionDefinition(Identifier factionId) {
        String displayName = factionId == null ? "Unknown Faction" : factionId.getPath().replace('_', ' ');
        if (!displayName.isBlank()) {
            displayName = Character.toUpperCase(displayName.charAt(0)) + displayName.substring(1);
        }
        return new EchoFactionDefinition(
                factionId,
                displayName,
                displayName,
                "",
                "Runtime mirrored faction.",
                "",
                "",
                "",
                0xFF8CA7B5,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new EchoDialogueTree("", List.of(), ""));
    }

    public static synchronized void bootstrapDefaults() {
        if (!RUNTIME_MODULES.isLoaded(EchoCore.MOD_ID)) {
            RUNTIME_MODULES.register(new EchoRuntimeModules.EchoRuntimeModule(EchoCore.MOD_ID, EchoCore.VERSION, "common", true));
        }
        REGISTRY.register(EchoRuntimeModules.class, RUNTIME_MODULES);
        REGISTRY.register(EchoSpineBus.class, new InMemoryEchoSpineBus());
        REGISTRY.register(EchoConfigRegistry.class, new EchoConfigRegistry());
        REGISTRY.register(IDataService.class, new InMemoryDataService());
        REGISTRY.register(IRegionService.class, new InMemoryRegionService());
        REGISTRY.register(IWorldRegionService.class, worldRegionService);
        InMemoryMissionRegistry missions = new InMemoryMissionRegistry();
        REGISTRY.register(IMissionRegistry.class, missions);
        REGISTRY.register(IMissionService.class, new InMemoryMissionService(missions));
        REGISTRY.register(INetworkService.class, NoOpNetworkService.INSTANCE);
        REGISTRY.register(IThemeService.class, new NoOpThemeService());
        REGISTRY.register(com.echoplatform.echocore.api.diagnostic.EchoDiagnosticService.class,
                new com.echoplatform.echocore.api.diagnostic.EchoDiagnosticService());
    }

    @FunctionalInterface
    public interface IntelMirrorService {
        void mirror(ServerPlayer player, String sourceModId, String id, String title, String content);
    }
}
