package com.knoxhack.echoscreencore.client;

import com.knoxhack.echo.adaptercore.EchoBackendCommandEventBridge;
import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echoscreencore.EchoScreenCoreMod;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.EchoScreens;
import com.knoxhack.echoscreencore.api.theme.EchoAccessibilitySettings;
import com.knoxhack.echoscreencore.client.component.EchoComponentRegistry;
import com.knoxhack.echoscreencore.client.engine.EchoScreenEngine;
import com.knoxhack.echoscreencore.client.reference.ScreenCoreReferenceData;
import com.knoxhack.echoscreencore.client.screen.EchoScreen;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class EchoScreenCoreClient {
    private static final String REGISTER_CLIENT_COMMANDS_EVENT =
            "net.neoforged.neoforge.client.event.RegisterClientCommandsEvent";
    private static final String ADD_CLIENT_RELOAD_LISTENERS_EVENT =
            "net.neoforged.neoforge.client.event.AddClientReloadListenersEvent";
    private static final boolean DEBUG_SHADER_RESOURCES =
            Boolean.getBoolean("echoscreencore.debugShaderResources");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static EchoAccessibilitySettings accessibility = EchoAccessibilitySettings.DEFAULT;
    private static boolean debugEnabled;

    public EchoScreenCoreClient() {
        this(null);
    }

    public EchoScreenCoreClient(Object modEventBus) {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        EchoComponentRegistry.registerDefaults();
        registerBuiltinDataProviders();
        ScreenCoreReferenceData.register();
        registerDemoActions();
        EchoBackendLifecycleBridge.registerGameEventHandler(REGISTER_CLIENT_COMMANDS_EVENT,
                EchoScreenCoreClient::onClientCommands);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ADD_CLIENT_RELOAD_LISTENERS_EVENT,
                EchoScreenCoreClient::onAddClientReloadListeners);
        EchoScreens.registerClientOpener((pageId, context) -> {
            Minecraft.getInstance().setScreen(new EchoScreen(pageId, context, accessibility, debugEnabled));
            return true;
        });
        EchoScreens.registerInvalidationHandler(EchoScreenCoreClient::invalidateOpenScreen);
        EchoScreenCoreMod.LOGGER.info("ECHO: ScreenCore client ready.");
    }

    public static boolean ensureRegisteredForNativeLoader() {
        if (REGISTERED.get()) {
            return false;
        }
        new EchoScreenCoreClient((Object) null);
        return REGISTERED.get();
    }

    public void registerClientCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        EchoScreenCoreClientCommands.register(dispatcher);
    }

    private static void onClientCommands(Object event) {
        CommandDispatcher<CommandSourceStack> dispatcher = EchoBackendCommandEventBridge.clientDispatcher(event);
        if (dispatcher != null) {
            EchoScreenCoreClientCommands.register(dispatcher);
        }
    }

    private static void onAddClientReloadListeners(Object event) {
        EchoBackendClientBridge.addClientReloadListener(event, EchoScreenCoreMod.id("eui"), new ScreenCoreReloadListener());
    }

    public static EchoAccessibilitySettings accessibility() {
        return accessibility;
    }

    public static void setAccessibility(EchoAccessibilitySettings next) {
        accessibility = next == null ? EchoAccessibilitySettings.DEFAULT : next;
    }

    public static boolean debugEnabled() {
        return debugEnabled;
    }

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    public static EchoDataContext testDashboardContext() {
        return EchoDataContext.empty()
            .put("screen.title", "ScreenCore Test Dashboard")
            .put("system.status", "ready")
            .put("metrics.layout", 82)
            .put("metrics.readability", 95)
            .put("player.water", 74)
            .put("selectedMission.id", "restore_water")
            .put("selectedMission.title", "Restore Water Supply")
            .put("filters.search", "")
            .put("filters.status", "active")
            .put("filters.tags", "water,signal")
            .put("filters.availableStatuses", demoStatuses())
            .put("tags.visible", demoTags())
            .put("toggles.highContrastDemo", false)
            .put("missions.demo", demoMissions())
            .put("missions.large", largeMissions())
            .put("missions.empty", List.of())
            .put("recipes.visible", demoRecipes())
            .put("guide.searchResults", demoArticles())
            .put("selectedMission.rewards", List.of("Readable screens", "Less coordinate math"));
    }

    private static void registerBuiltinDataProviders() {
        EchoScreenRegistry.registerDataProvider("system", (context, path) -> {
            if (path.isEmpty() || "status".equals(path.get(0))) {
                return "ready";
            }
            return null;
        });
        EchoScreenRegistry.registerDataProvider("metrics", (context, path) -> {
            if (!path.isEmpty() && "layout".equals(path.get(0))) {
                return 82;
            }
            return null;
        });
    }

    private static void registerDemoActions() {
        EchoScreenRegistry.registerAction("terminal.select_mission", context -> {
            if (context.actionValue() != null && !context.actionValue().isBlank()) {
                context.dataContext().put("selectedMission.id", context.actionValue());
                EchoScreens.invalidateData();
            }
            return true;
        });
        EchoScreenRegistry.registerAction("terminal.set_search", context -> {
            context.dataContext().put("filters.search", context.actionValue());
            EchoScreens.invalidateData();
            return true;
        });
        EchoScreenRegistry.registerAction("terminal.apply_search", context -> {
            EchoScreens.invalidateData();
            return true;
        });
        EchoScreenRegistry.registerAction("demo.toggle", context -> {
            context.dataContext().put("toggles.highContrastDemo", Boolean.parseBoolean(context.actionValue()));
            EchoScreens.invalidateData();
            return true;
        });
        EchoScreenRegistry.registerAction("demo.set_filter", context -> {
            context.dataContext().put("filters.status", context.actionValue());
            EchoScreens.invalidateData();
            return true;
        });
        EchoScreenRegistry.registerAction("settings.reset_accessibility", context -> {
            setAccessibility(EchoAccessibilitySettings.DEFAULT);
            EchoScreens.invalidateData();
            return true;
        });
        EchoScreenRegistry.registerAction("demo.echo", context -> true);
        EchoScreenRegistry.registerAction("demo.menu_action", context -> true);
    }

    private static List<Map<String, Object>> demoMissions() {
        return List.of(
            Map.of("id", "restore_water", "title", "Restore Water Supply", "summary", "Find and filter a clean water source.", "status", "active", "statusLabel", "ACTIVE"),
            Map.of("id", "raise_antenna", "title", "Raise Signal Antenna", "summary", "Recover parts for a higher-gain relay.", "status", "ready", "statusLabel", "READY"),
            Map.of("id", "secure_shelter", "title", "Secure Shelter", "summary", "Stabilize a safe room before the next weather event.", "status", "warning", "statusLabel", "WARN")
        );
    }

    private static List<Map<String, Object>> demoRecipes() {
        return List.of(
            Map.of("id", "circuit", "title", "Signal Circuit", "summary", "Comparator-grade signal component.", "item", "minecraft:comparator"),
            Map.of("id", "relay", "title", "Relay Plate", "summary", "Iron and redstone routing layer.", "item", "minecraft:iron_ingot")
        );
    }

    private static List<Map<String, Object>> demoStatuses() {
        return List.of(
            Map.of("id", "active", "label", "Active"),
            Map.of("id", "ready", "label", "Ready"),
            Map.of("id", "locked", "label", "Locked"),
            Map.of("id", "done", "label", "Done"),
            Map.of("id", "all", "label", "All")
        );
    }

    private static List<Map<String, Object>> demoTags() {
        return List.of(
            Map.of("id", "water", "label", "Water", "description", "Hydration and filtration objectives"),
            Map.of("id", "signal", "label", "Signal", "description", "Relay and antenna work"),
            Map.of("id", "shelter", "label", "Shelter", "description", "Base safety and weather prep"),
            Map.of("id", "crafting", "label", "Crafting", "description", "Recipes and machines"),
            Map.of("id", "lore", "label", "Lore", "description", "Recovered records")
        );
    }

    private static List<Map<String, Object>> largeMissions() {
        java.util.ArrayList<Map<String, Object>> rows = new java.util.ArrayList<>();
        String[] statuses = {"active", "ready", "warning", "done", "locked"};
        for (int i = 1; i <= 250; i++) {
            String id = "mission_" + i;
            String status = statuses[i % statuses.length];
            rows.add(Map.of(
                "id", id,
                "title", "Migration Row " + i,
                "summary", "Synthetic large-list row for ScreenCore layout and repeater validation.",
                "status", status,
                "statusLabel", status.toUpperCase(java.util.Locale.ROOT)
            ));
        }
        return List.copyOf(rows);
    }

    private static List<Map<String, Object>> demoArticles() {
        return List.of(
            Map.of("id", "first_steps", "title", "First Terminal Steps", "summary", "Open pages and read current status."),
            Map.of("id", "filters", "title", "Using Filters", "summary", "Search boxes update provider-backed result sets.")
        );
    }

    public static void onClientResourcesReloaded() {
        EchoScreenEngine.clearCaches();
    }

    private static void logShaderResourceProbe(ResourceManager manager) {
        if (!DEBUG_SHADER_RESOURCES) {
            return;
        }
        Identifier textVertex = Identifier.fromNamespaceAndPath("minecraft", "shaders/core/rendertype_text.vsh");
        Identifier textFragment = Identifier.fromNamespaceAndPath("minecraft", "shaders/core/rendertype_text.fsh");
        Map<Identifier, ?> shaderResources = manager.listResources("shaders", id ->
                id.getPath().endsWith(".vsh") || id.getPath().endsWith(".fsh") || id.getPath().endsWith(".glsl"));
        boolean listedVertex = shaderResources.containsKey(textVertex);
        boolean listedFragment = shaderResources.containsKey(textFragment);
        EchoScreenCoreMod.LOGGER.info(
                "ECHO: ScreenCore shader resource probe vertexDirect={} fragmentDirect={} vertexListed={} fragmentListed={} shaderResourceCount={}",
                manager.getResource(textVertex).isPresent(),
                manager.getResource(textFragment).isPresent(),
                listedVertex,
                listedFragment,
                shaderResources.size());
    }

    private static void invalidateOpenScreen(net.minecraft.resources.Identifier pageId) {
        runOnClientThread(() -> {
            if (Minecraft.getInstance().screen instanceof EchoScreen screen
                && (pageId == null || pageId.equals(screen.pageId()))) {
                screen.markDataDirty();
            }
        });
    }

    private static void runOnClientThread(Runnable task) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                minecraft.execute(task);
            }
        } catch (RuntimeException | LinkageError exception) {
            EchoScreenCoreMod.LOGGER.debug("Unable to schedule ScreenCore data invalidation on the client thread.",
                    exception);
        }
    }

    private static final class ScreenCoreReloadListener extends SimplePreparableReloadListener<Boolean> {
        @Override
        protected Boolean prepare(ResourceManager manager, ProfilerFiller profiler) {
            logShaderResourceProbe(manager);
            return Boolean.TRUE;
        }

        @Override
        protected void apply(Boolean prepared, ResourceManager manager, ProfilerFiller profiler) {
            onClientResourcesReloaded();
            if (Minecraft.getInstance().screen instanceof EchoScreen screen) {
                screen.reloadPage();
            }
        }
    }
}
