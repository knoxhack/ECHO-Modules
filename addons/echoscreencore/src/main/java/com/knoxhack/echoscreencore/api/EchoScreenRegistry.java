package com.knoxhack.echoscreencore.api;

import com.knoxhack.echoscreencore.api.action.EchoAction;
import com.knoxhack.echoscreencore.api.action.EchoActionRegistry;
import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoScreenRegistry {
    private static final Map<String, EchoComponentFactory> COMPONENTS = new LinkedHashMap<>();
    private static final Map<String, EchoDataProvider> DATA_PROVIDERS = new LinkedHashMap<>();
    private static final List<Identifier> STYLE_SHEETS = new ArrayList<>();

    private EchoScreenRegistry() {
    }

    public static void registerComponent(String tagName, EchoComponentFactory factory) {
        if (tagName == null || tagName.isBlank() || factory == null) {
            return;
        }
        COMPONENTS.put(normalizeTag(tagName), factory);
    }

    public static Optional<EchoComponentFactory> componentFactory(String tagName) {
        if (tagName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(COMPONENTS.get(normalizeTag(tagName)));
    }

    public static Map<String, EchoComponentFactory> componentFactories() {
        return Collections.unmodifiableMap(COMPONENTS);
    }

    public static void registerDataProvider(Identifier id, EchoDataProvider provider) {
        if (id == null || provider == null) {
            return;
        }
        DATA_PROVIDERS.put(id.toString(), provider);
        DATA_PROVIDERS.put(id.getPath(), provider);
    }

    public static void registerDataProvider(String key, EchoDataProvider provider) {
        if (key == null || key.isBlank() || provider == null) {
            return;
        }
        DATA_PROVIDERS.put(key.trim(), provider);
    }

    public static Optional<EchoDataProvider> dataProvider(String key) {
        return Optional.ofNullable(key == null ? null : DATA_PROVIDERS.get(key));
    }

    public static void registerAction(Identifier id, EchoAction action) {
        EchoActionRegistry.register(id, action);
    }

    public static void registerAction(String id, EchoAction action) {
        EchoActionRegistry.register(id, action);
    }

    public static void registerStyleSheet(Identifier styleSheetId) {
        if (styleSheetId != null && !STYLE_SHEETS.contains(styleSheetId)) {
            STYLE_SHEETS.add(styleSheetId);
        }
    }

    public static List<Identifier> styleSheets() {
        return List.copyOf(STYLE_SHEETS);
    }

    public static void clearForTests() {
        COMPONENTS.clear();
        DATA_PROVIDERS.clear();
        STYLE_SHEETS.clear();
        EchoActionRegistry.clearForTests();
    }

    private static String normalizeTag(String tagName) {
        return tagName.trim().toLowerCase(Locale.ROOT);
    }
}
