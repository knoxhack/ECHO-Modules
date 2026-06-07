package com.knoxhack.echo.creatorcore;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendCommandEventBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.creatorcore.config.CreatorCoreConfig;
import com.knoxhack.echo.creatorcore.client.CodexVisionCaptureService;
import com.knoxhack.echo.creatorcore.ui.CreatorDashboardScreen;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import org.lwjgl.glfw.GLFW;

public final class EchoCreatorCoreClient {
    private static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(EchoCreatorCore.MODID, "creator"));
    public static final KeyMapping OPEN_DASHBOARD_KEY = new KeyMapping(
            "key.echocreatorcore.open_dashboard",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            KEY_CATEGORY);
    public static final KeyMapping CAPTURE_CODEX_VISION_KEY = new KeyMapping(
            "key.echocreatorcore.capture_codex_vision",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            KEY_CATEGORY);

    public EchoCreatorCoreClient() {
        this(null);
    }

    public EchoCreatorCoreClient(Object modEventBus) {
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ClientModEvents::onRegisterKeyMappings);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoCreatorCoreClient::onKeyInput);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoCreatorCoreClient::onClientCommands);
        registerOptionalClientIntegration("echoscreencore",
                "com.knoxhack.echo.creatorcore.client.CreatorCoreScreenCoreClientIntegration");
        registerOptionalClientIntegration("echoterminal",
                "com.knoxhack.echo.creatorcore.client.CreatorCoreTerminalClientIntegration");
    }

    public static void openDashboard() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (!CreatorCoreConfig.bool(CreatorCoreConfig.ALLOW_CLIENT_DASHBOARD, true)) {
                if (minecraft.player != null) {
                    minecraft.player.sendSystemMessage(Component.literal("CreatorCore dashboard is disabled by config."));
                }
                return;
            }
            if (CreatorCoreConfig.bool(CreatorCoreConfig.LOG_UI_OPEN, true)) {
                EchoCreatorCore.LOGGER.info("Opening CreatorCore dashboard.");
            }
            if (CreatorCoreConfig.bool(CreatorCoreConfig.PREFER_SCREENCORE_UI, true)
                    && openScreenCoreDashboard()) {
                return;
            }
            minecraft.setScreen(new CreatorDashboardScreen());
        });
    }

    private static void onKeyInput(Object event) {
        if (!EchoBackendClientBridge.keyActionEquals(event, GLFW.GLFW_PRESS)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (EchoBackendClientBridge.keyMappingMatches(OPEN_DASHBOARD_KEY, event) && minecraft.player != null && minecraft.screen == null) {
            openDashboard();
        } else if (EchoBackendClientBridge.keyMappingMatches(CAPTURE_CODEX_VISION_KEY, event) && minecraft.player != null) {
            CodexVisionCaptureService.capture("hotkey");
        }
    }

    private static void onClientCommands(Object event) {
        var dispatcher = EchoBackendCommandEventBridge.clientDispatcher(event);
        if (dispatcher == null) {
            return;
        }
        dispatcher.register(Commands.literal("echocreatorcore")
                .then(Commands.literal("open").executes(ctx -> openFromClientCommand()))
                .then(clientCodexVisionRoot()));
        dispatcher.register(Commands.literal("echo")
                .then(Commands.literal("creatorcore")
                        .then(Commands.literal("open").executes(ctx -> openFromClientCommand()))
                        .then(clientCodexVisionRoot()))
                .then(Commands.literal("creator")
                        .then(Commands.literal("open").executes(ctx -> openFromClientCommand()))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> clientCodexVisionRoot() {
        return Commands.literal("codex")
                .then(Commands.literal("vision")
                        .then(Commands.literal("capture")
                                .executes(ctx -> CodexVisionCaptureService.capture("manual"))
                                .then(Commands.argument("label", StringArgumentType.greedyString())
                                        .executes(ctx -> CodexVisionCaptureService.capture(
                                                StringArgumentType.getString(ctx, "label"))))));
    }

    private static int openFromClientCommand() {
        openDashboard();
        return 1;
    }

    private static boolean openScreenCoreDashboard() {
        if (!EchoRuntimeModules.isLoaded("echoscreencore")) {
            return false;
        }
        try {
            Class<?> integration = Class.forName(
                    "com.knoxhack.echo.creatorcore.client.CreatorCoreScreenCoreClientIntegration",
                    true,
                    Thread.currentThread().getContextClassLoader());
            Object opened = integration.getMethod("openDashboard").invoke(null);
            return opened instanceof Boolean value && value;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoCreatorCore.LOGGER.warn("CreatorCore ScreenCore dashboard opener failed; using vanilla dashboard.", exception);
            return false;
        }
    }

    private static void registerOptionalClientIntegration(String modId, String className) {
        if (!EchoRuntimeModules.isLoaded(modId)) {
            return;
        }
        try {
            Class<?> integration = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            integration.getMethod("register").invoke(null);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            EchoCreatorCore.LOGGER.warn("CreatorCore optional client integration {} could not register.", className, exception);
        }
    }

        public static final class ClientModEvents {
        private ClientModEvents() {
        }

            static void onRegisterKeyMappings(Object event) {
            EchoBackendClientBridge.registerKeyCategory(event, KEY_CATEGORY);
            EchoBackendClientBridge.registerKeyMapping(event, OPEN_DASHBOARD_KEY);
            EchoBackendClientBridge.registerKeyMapping(event, CAPTURE_CODEX_VISION_KEY);
        }
    }
}
