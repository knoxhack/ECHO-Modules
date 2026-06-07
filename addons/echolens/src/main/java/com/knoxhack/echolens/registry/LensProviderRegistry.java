package com.knoxhack.echolens.registry;

import com.knoxhack.echolens.EchoLens;
import com.knoxhack.echolens.api.LensInfoProvider;
import com.knoxhack.echolens.api.LensProviderDiagnostic;
import com.knoxhack.echolens.api.ServerLensProvider;
import com.knoxhack.echolens.config.LensConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.resources.Identifier;

public final class LensProviderRegistry {
    private static final Map<Identifier, LensInfoProvider> PROVIDERS_BY_ID = new ConcurrentHashMap<>();
    private static final Map<Identifier, ProviderState> PROVIDER_STATES = new ConcurrentHashMap<>();
    private static final List<LensInfoProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private LensProviderRegistry() {
    }

    public static void register(LensInfoProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Lens provider is required.");
        }
        Identifier id = provider.id();
        if (id == null) {
            throw new IllegalArgumentException("Lens provider id is required: " + provider.getClass().getName());
        }
        LensInfoProvider existing = PROVIDERS_BY_ID.putIfAbsent(id, provider);
        if (existing != null && existing != provider) {
            throw new IllegalArgumentException("Duplicate Lens provider id: " + id);
        }
        if (!PROVIDERS.contains(provider)) {
            PROVIDERS.add(provider);
            PROVIDER_STATES.putIfAbsent(id, new ProviderState(provider));
            sort();
            EchoLens.LOGGER.debug("Registered Lens provider {} ({})", id, provider.getClass().getName());
        }
    }

    public static void registerAll(Iterable<? extends LensInfoProvider> providers) {
        if (providers == null) {
            throw new IllegalArgumentException("Lens providers are required.");
        }
        for (LensInfoProvider provider : providers) {
            register(provider);
        }
    }

    public static List<LensInfoProvider> providers() {
        return List.copyOf(PROVIDERS);
    }

    public static List<ServerLensProvider> serverProviders() {
        return PROVIDERS.stream()
                .filter(ServerLensProvider.class::isInstance)
                .map(ServerLensProvider.class::cast)
                .toList();
    }

    public static List<LensProviderDiagnostic> diagnostics() {
        return PROVIDERS.stream()
                .map(provider -> new LensProviderDiagnostic(
                        provider.id(),
                        provider.getClass().getName(),
                        provider.priority(),
                        provider.category(),
                        true,
                        categoryEnabled(provider.category())))
                .toList();
    }

    public static List<LensProviderHealth> health() {
        return PROVIDERS.stream()
                .map(LensProviderRegistry::health)
                .toList();
    }

    public static LensProviderHealth health(LensInfoProvider provider) {
        Identifier id = provider == null ? null : provider.id();
        if (id == null) {
            return new LensProviderHealth(null, false, false, false, "unknown", 0, "");
        }
        ProviderState state = PROVIDER_STATES.computeIfAbsent(id, ignored -> new ProviderState(provider));
        return state.snapshot(provider);
    }

    public static void recordProviderSuccess(LensInfoProvider provider) {
        Identifier id = provider == null ? null : provider.id();
        if (id != null) {
            PROVIDER_STATES.computeIfAbsent(id, ignored -> new ProviderState(provider)).recordSuccess();
        }
    }

    public static void recordProviderFailure(LensInfoProvider provider, RuntimeException exception) {
        Identifier id = provider == null ? null : provider.id();
        if (id != null) {
            PROVIDER_STATES.computeIfAbsent(id, ignored -> new ProviderState(provider)).recordFailure(exception);
        }
    }

    public static int count() {
        return PROVIDERS.size();
    }

    public static boolean hasProvider(Identifier id) {
        return PROVIDERS_BY_ID.containsKey(id);
    }

    public static void clearForTests() {
        PROVIDERS_BY_ID.clear();
        PROVIDER_STATES.clear();
        PROVIDERS.clear();
    }

    public static void withClearedForTests(Runnable body) {
        Map<Identifier, LensInfoProvider> ids = Map.copyOf(PROVIDERS_BY_ID);
        Map<Identifier, ProviderState> states = Map.copyOf(PROVIDER_STATES);
        List<LensInfoProvider> providers = List.copyOf(PROVIDERS);
        PROVIDERS_BY_ID.clear();
        PROVIDER_STATES.clear();
        PROVIDERS.clear();
        try {
            body.run();
        } finally {
            PROVIDERS_BY_ID.clear();
            PROVIDERS_BY_ID.putAll(ids);
            PROVIDER_STATES.clear();
            PROVIDER_STATES.putAll(states);
            PROVIDERS.clear();
            PROVIDERS.addAll(providers);
        }
    }

    private static void sort() {
        List<LensInfoProvider> sorted = new ArrayList<>(PROVIDERS);
        sorted.sort(Comparator.comparingInt(LensInfoProvider::priority)
                .thenComparing(provider -> provider.id().toString()));
        PROVIDERS.clear();
        PROVIDERS.addAll(sorted);
    }

    private static boolean categoryEnabled(com.knoxhack.echolens.api.LensDataCategory category) {
        return switch (category == null ? com.knoxhack.echolens.api.LensDataCategory.IDENTITY : category) {
            case IDENTITY -> LensConfig.bool(LensConfig.SHOW_IDENTITY, true);
            case BLOCK -> LensConfig.bool(LensConfig.SHOW_BLOCK, true);
            case ENTITY -> LensConfig.bool(LensConfig.SHOW_ENTITY, true);
            case FLUID -> LensConfig.bool(LensConfig.SHOW_FLUID, true);
            case MACHINE -> LensConfig.bool(LensConfig.SHOW_MACHINE, true);
            case INVENTORY -> LensConfig.bool(LensConfig.SHOW_INVENTORY, true);
            case INTEGRATION -> LensConfig.bool(LensConfig.SHOW_INTEGRATION, true);
            case HINTS -> LensConfig.bool(LensConfig.BEGINNER_HINTS, true);
            case ACTIONS -> LensConfig.bool(LensConfig.SHOW_ACTIONS, true);
        };
    }

    private static final class ProviderState {
        private final String registrationSource;
        private final boolean serverSafe;
        private final AtomicInteger failureCount = new AtomicInteger();
        private volatile String lastFailure = "";

        private ProviderState(LensInfoProvider provider) {
            registrationSource = provider == null ? "unknown" : provider.getClass().getName();
            serverSafe = provider instanceof ServerLensProvider;
        }

        private void recordSuccess() {
        }

        private void recordFailure(RuntimeException exception) {
            failureCount.incrementAndGet();
            lastFailure = exception == null
                    ? "unknown failure"
                    : exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }

        private LensProviderHealth snapshot(LensInfoProvider provider) {
            return new LensProviderHealth(
                    provider.id(),
                    true,
                    categoryEnabled(provider.category()),
                    serverSafe,
                    registrationSource,
                    failureCount.get(),
                    lastFailure);
        }
    }
}
