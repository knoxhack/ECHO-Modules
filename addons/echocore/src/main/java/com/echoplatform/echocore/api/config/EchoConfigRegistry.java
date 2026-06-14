package com.echoplatform.echocore.api.config;

import java.util.Collection;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class EchoConfigRegistry {
    private static final EchoConfigRegistry GLOBAL = new EchoConfigRegistry();
    private final Map<String, EchoConfigProvider> providers = new LinkedHashMap<>();

    public void register(String moduleId, EchoConfigProvider provider) {
        providers.put(moduleId, provider);
    }

    public static void register(EchoConfigProvider provider) {
        EchoConfigModule module = provider == null ? null : provider.describeConfig();
        if (module != null) {
            GLOBAL.register(module.moduleId(), provider);
        }
    }

    public static void withClearedForTests(Runnable body) {
        Map<String, EchoConfigProvider> snapshot = new LinkedHashMap<>(GLOBAL.providers);
        GLOBAL.providers.clear();
        try {
            body.run();
        } finally {
            GLOBAL.providers.clear();
            GLOBAL.providers.putAll(snapshot);
        }
    }

    public Optional<EchoConfigProvider> find(String moduleId) {
        return Optional.ofNullable(providers.get(moduleId));
    }

    public Collection<EchoConfigModule> snapshots() {
        return providers.values().stream().map(EchoConfigProvider::describeConfig).toList();
    }

    public static List<EchoConfigModuleSnapshot> snapshots(EchoConfigSide side) {
        return GLOBAL.providers.values().stream()
                .map(EchoConfigProvider::describeConfig)
                .map(module -> snapshot(module, side))
                .filter(EchoConfigModuleSnapshot::hasEntries)
                .toList();
    }

    public static Optional<EchoConfigModuleSnapshot> snapshot(String moduleId, EchoConfigSide side) {
        EchoConfigProvider provider = GLOBAL.providers.get(moduleId);
        if (provider == null) {
            return Optional.empty();
        }
        EchoConfigModule module = provider.describeConfig();
        if (module == null) {
            return Optional.empty();
        }
        EchoConfigModuleSnapshot snapshot = snapshot(module, side);
        if (side != null && !snapshot.hasEntries()) {
            return Optional.empty();
        }
        return Optional.of(snapshot);
    }

    public static EchoConfigApplyResult apply(EchoConfigSide side, String moduleId, String entryId, String value) {
        EchoConfigProvider provider = GLOBAL.providers.get(moduleId);
        if (provider == null) {
            return EchoConfigApplyResult.rejected("Config provider is unavailable.");
        }
        EchoConfigModule module = provider.describeConfig();
        if (module == null) {
            return EchoConfigApplyResult.rejected("Config side is unavailable.");
        }
        Optional<EchoConfigEntry> entry = module.categories().stream()
                .flatMap(category -> category.entries().stream())
                .filter(candidate -> candidate.key().equals(entryId))
                .findFirst();
        if (entry.isEmpty()) {
            return EchoConfigApplyResult.rejected("Config entry is unavailable.");
        }
        EchoConfigEntry configEntry = entry.get();
        if (side != null && configEntry.side() != side) {
            return EchoConfigApplyResult.rejected("Config side is unavailable.");
        }
        EchoConfigApplyResult entryResult = configEntry.apply(value);
        if (!entryResult.success()) {
            return entryResult;
        }
        EchoConfigApplyResult providerResult = provider.apply(module);
        return providerResult == null ? EchoConfigApplyResult.acceptedResult() : providerResult;
    }

    public static EchoConfigApplyResult reset(EchoConfigSide side, String moduleId, String entryId) {
        return apply(side, moduleId, entryId, "");
    }

    private static EchoConfigModuleSnapshot snapshot(EchoConfigModule module) {
        return snapshot(module, null);
    }

    private static EchoConfigModuleSnapshot snapshot(EchoConfigModule module, EchoConfigSide side) {
        List<EchoConfigCategorySnapshot> categories = module.categories().stream()
                .map(category -> {
                    List<EchoConfigEntrySnapshot> entries = category.entries().stream()
                                .filter(entry -> side == null || entry.side() == side)
                                .map(entry -> new EchoConfigEntrySnapshot(
                                        module.moduleId(),
                                        category.id(),
                                        entry.key(),
                                        entry.key(),
                                        entry.description(),
                                        entry.side(),
                                        entry.kind(),
                                        entry.defaultValue(),
                                        entry.defaultValue(),
                                        "",
                                        "",
                                        List.of(),
                                        true,
                                        false,
                                        false,
                                        ""))
                                .toList();
                    return new EchoConfigCategorySnapshot(category.id(), category.displayName(), entries);
                })
                .filter(category -> !category.entries().isEmpty())
                .toList();
        return new EchoConfigModuleSnapshot(module.moduleId(), module.moduleId(), categories);
    }
}
