package com.knoxhack.echo.machinecore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class EchoMachineRuntimeRegistry {
    private static final List<EchoMachineRuntimeProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private EchoMachineRuntimeRegistry() {
    }

    public static void register(EchoMachineRuntimeProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("MachineCore runtime provider is required.");
        }
        Identifier id = provider.providerId();
        if (id == null) {
            throw new IllegalArgumentException("MachineCore runtime provider id is required.");
        }
        for (EchoMachineRuntimeProvider existing : PROVIDERS) {
            if (id.equals(existing.providerId())) {
                if (existing != provider) {
                    throw new IllegalArgumentException("Duplicate MachineCore runtime provider id: " + id);
                }
                return;
            }
        }
        PROVIDERS.add(provider);
        sort();
    }

    public static List<EchoMachineRuntimeProvider> providers() {
        return List.copyOf(PROVIDERS);
    }

    public static int count() {
        return PROVIDERS.size();
    }

    public static boolean hasProvider(Identifier providerId) {
        if (providerId == null) {
            return false;
        }
        return PROVIDERS.stream().anyMatch(provider -> providerId.equals(provider.providerId()));
    }

    public static Optional<EchoMachineRuntimeSnapshot> snapshot(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return Optional.empty();
        }
        for (EchoMachineRuntimeProvider provider : PROVIDERS) {
            try {
                Optional<EchoMachineRuntimeSnapshot> snapshot = provider.snapshot(level, pos);
                if (snapshot != null && snapshot.isPresent()) {
                    return snapshot;
                }
            } catch (RuntimeException ignored) {
                // Provider isolation is part of the neutral runtime contract.
            }
        }
        return Optional.empty();
    }

    public static List<EchoMachineRuntimeSnapshot> snapshots(Player player) {
        Map<String, EchoMachineRuntimeSnapshot> snapshots = new LinkedHashMap<>();
        for (EchoMachineRuntimeProvider provider : PROVIDERS) {
            try {
                List<EchoMachineRuntimeSnapshot> provided = provider.snapshots(player);
                if (provided == null) {
                    continue;
                }
                for (EchoMachineRuntimeSnapshot snapshot : provided) {
                    if (snapshot != null) {
                        snapshots.putIfAbsent(snapshotKey(snapshot), snapshot);
                    }
                }
            } catch (RuntimeException ignored) {
                // Keep sibling UI/runtime consumers available when one addon misbehaves.
            }
        }
        return List.copyOf(snapshots.values());
    }

    private static String snapshotKey(EchoMachineRuntimeSnapshot snapshot) {
        Map<String, String> attributes = snapshot.attributes();
        String position = attributes.getOrDefault("position", attributes.getOrDefault("worldPos", ""));
        String dimension = attributes.getOrDefault("dimension", "");
        return snapshot.id().value() + "@" + dimension + "@" + position;
    }

    public static List<EchoMachineProfile> profiles(Player player) {
        Map<EchoMachineId, EchoMachineProfile> profiles = new LinkedHashMap<>();
        for (EchoMachineRuntimeProvider provider : PROVIDERS) {
            try {
                List<EchoMachineProfile> provided = provider.profiles(player);
                if (provided == null) {
                    continue;
                }
                for (EchoMachineProfile profile : provided) {
                    if (profile != null) {
                        profiles.putIfAbsent(profile.id(), profile);
                    }
                }
            } catch (RuntimeException ignored) {
                // Provider isolation is part of the neutral runtime contract.
            }
        }
        return List.copyOf(profiles.values());
    }

    public static void clearForTests() {
        PROVIDERS.clear();
    }

    public static void withClearedForTests(Runnable body) {
        List<EchoMachineRuntimeProvider> snapshot = new ArrayList<>(PROVIDERS);
        PROVIDERS.clear();
        try {
            body.run();
        } finally {
            PROVIDERS.clear();
            PROVIDERS.addAll(snapshot);
            sort();
        }
    }

    private static void sort() {
        List<EchoMachineRuntimeProvider> sorted = new ArrayList<>(PROVIDERS);
        sorted.sort(Comparator.comparing(provider -> provider.providerId().toString()));
        PROVIDERS.clear();
        PROVIDERS.addAll(sorted);
    }
}
