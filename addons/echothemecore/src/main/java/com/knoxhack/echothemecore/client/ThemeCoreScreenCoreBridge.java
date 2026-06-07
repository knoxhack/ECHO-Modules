package com.knoxhack.echothemecore.client;

import com.knoxhack.echothemecore.EchoThemeCore;
import com.knoxhack.echothemecore.api.EchoTheme;
import com.knoxhack.echothemecore.api.EchoThemeTextureKey;
import com.knoxhack.echothemecore.content.ThemeRegistry;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class ThemeCoreScreenCoreBridge {
    private static final Identifier PICKER_PAGE =
            Identifier.fromNamespaceAndPath(EchoThemeCore.MODID, "client_theme_picker");

    private ThemeCoreScreenCoreBridge() {
    }

    public static boolean openThemePicker() {
        if (!screenCoreLoaded()) {
            return false;
        }
        try {
            Class<?> echoScreens = Class.forName("com.knoxhack.echoscreencore.api.EchoScreens");
            Class<?> dataContext = Class.forName("com.knoxhack.echoscreencore.api.EchoDataContext");
            Object context = dataContext.getMethod("empty").invoke(null);
            Object opened = echoScreens.getMethod("open", Identifier.class, dataContext)
                    .invoke(null, PICKER_PAGE, context);
            return opened instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoThemeCore.LOGGER.warn("ThemeCore ScreenCore theme picker could not open.", exception);
            return false;
        }
    }

    public static void registerIfAvailable() {
        if (!screenCoreLoaded()) {
            return;
        }
        try {
            Class<?> registry = Class.forName("com.knoxhack.echoscreencore.api.EchoScreenRegistry");
            Class<?> actionClass = Class.forName("com.knoxhack.echoscreencore.api.action.EchoAction");
            Class<?> dataProviderClass = Class.forName("com.knoxhack.echoscreencore.api.EchoDataProvider");
            registry.getMethod("registerDataProvider", String.class, dataProviderClass)
                    .invoke(null, "themeCore", dataProvider(dataProviderClass));
            registry.getMethod("registerAction", String.class, actionClass)
                    .invoke(null, "echothemecore.set_client_theme", action(actionClass, ThemeCoreScreenCoreBridge::setTheme));
            registry.getMethod("registerAction", String.class, actionClass)
                    .invoke(null, "echothemecore.cycle_client_theme", action(actionClass, ThemeCoreScreenCoreBridge::cycleTheme));
            registry.getMethod("registerAction", String.class, actionClass)
                    .invoke(null, "echothemecore.reset_client_theme", action(actionClass, ThemeCoreScreenCoreBridge::resetTheme));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoThemeCore.LOGGER.warn("ThemeCore ScreenCore theme picker bridge could not register.", exception);
        }
    }

    public static List<Map<String, Object>> publicThemeRowsForTests() {
        return publicThemeRows();
    }

    private static boolean screenCoreLoaded() {
        return EchoRuntimeModules.isLoaded("echoscreencore");
    }

    private static Object dataProvider(Class<?> dataProviderClass) {
        return Proxy.newProxyInstance(
                dataProviderClass.getClassLoader(),
                new Class<?>[] { dataProviderClass },
                (proxy, method, args) -> {
                    if (!"resolve".equals(method.getName())) {
                        return null;
                    }
                    List<?> path = args != null && args.length > 1 && args[1] instanceof List<?> list ? list : List.of();
                    return resolveThemeData(path);
                });
    }

    private static Object resolveThemeData(List<?> path) {
        String key = path.isEmpty() ? "" : String.valueOf(path.get(0));
        EchoTheme active = ClientThemeState.currentTheme();
        return switch (key) {
            case "", "current" -> currentThemeData(active);
            case "activeId" -> active.id().toString();
            case "activeName" -> active.displayName();
            case "publicThemes" -> publicThemeRows();
            case "count" -> ClientThemeState.listPublicThemes().size();
            default -> null;
        };
    }

    private static Map<String, Object> currentThemeData(EchoTheme active) {
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("activeId", active.id().toString());
        data.put("activeName", active.displayName());
        data.put("count", ClientThemeState.listPublicThemes().size());
        data.put("publicThemes", publicThemeRows());
        return data;
    }

    private static List<Map<String, Object>> publicThemeRows() {
        Identifier activeId = ClientThemeState.currentThemeId();
        return ClientThemeState.listPublicThemes().stream()
                .map(theme -> themeRow(theme, activeId))
                .toList();
    }

    private static Map<String, Object> themeRow(EchoTheme theme, Identifier activeId) {
        boolean selected = theme.id().equals(activeId);
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("id", theme.id().toString());
        row.put("displayName", theme.displayName());
        row.put("description", theme.description());
        row.put("family", metadata(theme, "family", theme.id().getPath()));
        row.put("cycleOrder", ThemeRegistry.cycleOrder(theme));
        row.put("replacementLevel", metadata(theme, "replacement_level", "full").toUpperCase(Locale.ROOT));
        row.put("moduleTags", metadata(theme, "module_tags", "hud,loading,menu,item_icon,screencore"));
        row.put("icon", metadata(theme, "icon", texture(theme, EchoThemeTextureKey.ICON_PACK)));
        row.put("selected", selected);
        row.put("selectedStatus", selected ? "success" : "info");
        row.put("buttonLabel", selected ? "Active" : "Apply");
        row.put("previewProgress", selected ? 100 : Math.max(44, Math.min(96, 68 + ThemeRegistry.cycleOrder(theme) / 4)));
        row.put("primary", color(theme.colors().primary()));
        row.put("secondary", color(theme.colors().secondary()));
        row.put("accent", color(theme.colors().accent()));
        row.put("loadingBackground", texture(theme, EchoThemeTextureKey.LOADING_BACKGROUND));
        row.put("menuBackplate", texture(theme, EchoThemeTextureKey.MENU_MAIN_BACKPLATE));
        row.put("hudAccent", texture(theme, EchoThemeTextureKey.HUD_HOTBAR_FRAME));
        row.put("itemFrame", texture(theme, EchoThemeTextureKey.ITEM_ICON_FRAME));
        row.put("buttonTexture", texture(theme, EchoThemeTextureKey.SCREENCORE_BUTTON));
        return row;
    }

    private static String metadata(EchoTheme theme, String key, String fallback) {
        String value = theme.metadata().get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String texture(EchoTheme theme, EchoThemeTextureKey key) {
        return theme.moduleTexture(key)
                .or(() -> theme.vanillaUiProfile().texture(key))
                .or(() -> theme.uiAssets().texture(key))
                .map(Identifier::toString)
                .orElse("");
    }

    private static String color(int argb) {
        return String.format(Locale.ROOT, "#%06X", argb & 0x00FFFFFF);
    }

    private static Object action(Class<?> actionClass, ThemeAction action) {
        return Proxy.newProxyInstance(
                actionClass.getClassLoader(),
                new Class<?>[] { actionClass },
                (proxy, method, args) -> {
                    if (!"run".equals(method.getName()) || args == null || args.length == 0) {
                        return false;
                    }
                    return action.run(actionValue(args[0]));
                });
    }

    private static boolean setTheme(String rawValue) {
        Identifier id = parseThemeId(rawValue);
        if (id == null) {
            return false;
        }
        ClientThemeState.setTheme(id);
        afterThemeChanged("ThemeCore selected theme");
        return true;
    }

    private static boolean cycleTheme(String rawValue) {
        int direction = "previous".equalsIgnoreCase(rawValue == null ? "" : rawValue.strip()) ? -1 : 1;
        ClientThemeState.cycleTheme(direction);
        afterThemeChanged("ThemeCore cycled theme");
        return true;
    }

    private static boolean resetTheme(String rawValue) {
        ClientThemeState.reset();
        afterThemeChanged("ThemeCore reset theme");
        return true;
    }

    private static Identifier parseThemeId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        String trimmed = rawId.trim();
        return trimmed.contains(":")
            ? Identifier.tryParse(trimmed)
            : Identifier.fromNamespaceAndPath(EchoThemeCore.MODID, trimmed);
    }

    private static String actionValue(Object context) {
        try {
            Object rawValue = context.getClass().getMethod("actionValue").invoke(context);
            return rawValue == null ? "" : rawValue.toString();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return "";
        }
    }

    private static void afterThemeChanged(String prefix) {
        invalidatePickerPage();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal(
                    prefix + ": " + ClientThemeState.currentTheme().displayName()
                            + " (" + ClientThemeState.currentThemeId() + ")"));
        }
    }

    private static void invalidatePickerPage() {
        try {
            Class<?> echoScreens = Class.forName("com.knoxhack.echoscreencore.api.EchoScreens");
            echoScreens.getMethod("invalidatePage", Identifier.class).invoke(null, PICKER_PAGE);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            // ScreenCore is optional; stale picker data is harmless outside an open ScreenCore page.
        }
    }

    @FunctionalInterface
    private interface ThemeAction {
        boolean run(String actionValue);
    }
}
