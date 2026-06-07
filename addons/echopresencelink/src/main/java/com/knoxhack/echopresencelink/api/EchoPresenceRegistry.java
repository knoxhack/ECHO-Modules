package com.knoxhack.echopresencelink.api;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.Identifier;

public final class EchoPresenceRegistry {
    private static final List<EchoPresenceProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private EchoPresenceRegistry() {
    }

    public static void register(EchoPresenceProvider provider) {
        if (provider == null || provider.id() == null) {
            return;
        }
        Identifier id = provider.id();
        for (EchoPresenceProvider existing : PROVIDERS) {
            if (id.equals(existing.id())) {
                return;
            }
        }
        PROVIDERS.add(provider);
    }

    public static List<EchoPresenceProvider> providers() {
        return List.copyOf(PROVIDERS);
    }

    public static Optional<EchoPresenceSnapshot> select(EchoPresenceContext context) {
        return select(PROVIDERS, context);
    }

    public static Optional<EchoPresenceSnapshot> select(Collection<EchoPresenceProvider> providers,
            EchoPresenceContext context) {
        if (providers == null || providers.isEmpty()) {
            return Optional.empty();
        }
        return providers.stream()
                .filter(provider -> provider != null && provider.id() != null)
                .map(provider -> candidate(provider, context))
                .flatMap(Optional::stream)
                .max(Comparator
                        .comparingInt(PresenceCandidate::priority)
                        .thenComparingInt(PresenceCandidate::order)
                        .thenComparing(candidate -> candidate.providerId().toString()))
                .map(PresenceCandidate::snapshot);
    }

    private static Optional<PresenceCandidate> candidate(EchoPresenceProvider provider, EchoPresenceContext context) {
        try {
            EchoPresenceSnapshot snapshot = provider.snapshot(context);
            if (snapshot == null || snapshot.clear()) {
                return Optional.empty();
            }
            return Optional.of(new PresenceCandidate(provider.id(), provider.order(), snapshot));
        } catch (RuntimeException | LinkageError ignored) {
            return Optional.empty();
        }
    }

    private record PresenceCandidate(Identifier providerId, int order, EchoPresenceSnapshot snapshot)
            implements java.util.function.Supplier<EchoPresenceSnapshot> {
        int priority() {
            return snapshot.priority();
        }

        @Override
        public EchoPresenceSnapshot get() {
            return snapshot;
        }
    }
}
