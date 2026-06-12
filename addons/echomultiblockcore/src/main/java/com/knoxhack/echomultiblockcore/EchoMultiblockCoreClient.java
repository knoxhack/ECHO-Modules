package com.knoxhack.echomultiblockcore;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echomultiblockcore.client.MultiblockControllerScreen;
import com.knoxhack.echomultiblockcore.client.MultiblockCrateScreen;
import com.knoxhack.echomultiblockcore.client.MultiblockPreviewRenderer;
import com.knoxhack.echomultiblockcore.client.RobotAnimationClientState;
import com.knoxhack.echomultiblockcore.registry.ModMenus;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class EchoMultiblockCoreClient {
    private static final KeyMapping.Category KEY_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(EchoMultiblockCore.MODID, "build_assist"));
    public static final KeyMapping PREVIEW_ROTATE_KEY = new KeyMapping(
            "key.echomultiblockcore.preview_rotate",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY);
    public static final KeyMapping PREVIEW_MIRROR_KEY = new KeyMapping(
            "key.echomultiblockcore.preview_toggle_mirror",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            KEY_CATEGORY);
    public static final KeyMapping PREVIEW_LAYER_UP_KEY = new KeyMapping(
            "key.echomultiblockcore.preview_layer_up",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_PAGE_UP,
            KEY_CATEGORY);
    public static final KeyMapping PREVIEW_LAYER_DOWN_KEY = new KeyMapping(
            "key.echomultiblockcore.preview_layer_down",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_PAGE_DOWN,
            KEY_CATEGORY);

    public EchoMultiblockCoreClient() {
        this(null);
    }

    public EchoMultiblockCoreClient(Object modEventBus) {
        EchoBackendLifecycleBridge.registerGameEventHandler(MultiblockPreviewRenderer::render);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoMultiblockCoreClient::onKeyInput);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoMultiblockCoreClient::onRenderGui);
        EchoBackendLifecycleBridge.registerGameEventHandler(EchoMultiblockCoreClient::onClientTick);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoMultiblockCoreClient::onRegisterKeyMappings);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoMultiblockCoreClient::onRegisterMenuScreens);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, EchoMultiblockCoreClient::onRegisterRenderers);
    }

    private static void onKeyInput(Object event) {
        if (!EchoBackendClientBridge.keyActionEquals(event, GLFW.GLFW_PRESS)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || !MultiblockPreviewRenderer.holdingBlueprint()) {
            return;
        }
        if (EchoBackendClientBridge.keyMappingMatches(PREVIEW_ROTATE_KEY, event)) {
            MultiblockPreviewRenderer.rotatePreview();
        } else if (EchoBackendClientBridge.keyMappingMatches(PREVIEW_MIRROR_KEY, event)) {
            MultiblockPreviewRenderer.toggleMirror();
        } else if (EchoBackendClientBridge.keyMappingMatches(PREVIEW_LAYER_UP_KEY, event)) {
            MultiblockPreviewRenderer.layerUp();
        } else if (EchoBackendClientBridge.keyMappingMatches(PREVIEW_LAYER_DOWN_KEY, event)) {
            MultiblockPreviewRenderer.layerDown();
        }
    }

    private static void onRenderGui(Object event) {
        var graphics = EchoBackendClientBridge.guiGraphics(event);
        if (graphics != null) {
            MultiblockPreviewRenderer.renderHud(graphics, EchoBackendClientBridge.guiPartialTick(event));
        }
    }

    private static void onClientTick(Object event) {
        RobotAnimationClientState.tick();
    }

    static void onRegisterKeyMappings(Object event) {
        EchoBackendClientBridge.registerKeyCategory(event, KEY_CATEGORY);
        EchoBackendClientBridge.registerKeyMapping(event, PREVIEW_ROTATE_KEY);
        EchoBackendClientBridge.registerKeyMapping(event, PREVIEW_MIRROR_KEY);
        EchoBackendClientBridge.registerKeyMapping(event, PREVIEW_LAYER_UP_KEY);
        EchoBackendClientBridge.registerKeyMapping(event, PREVIEW_LAYER_DOWN_KEY);
    }

    static void onRegisterMenuScreens(Object event) {
        EchoBackendClientBridge.registerMenuScreen(event, ModMenus.CONTROLLER.get(), MultiblockControllerScreen.class);
        EchoBackendClientBridge.registerMenuScreen(event, ModMenus.CRATE.get(), MultiblockCrateScreen.class);
    }

    static void onRegisterRenderers(Object event) {
        if (!EchoRuntimeModules.isLoaded("echorendercore")) {
            return;
        }
        try {
            Class.forName("com.knoxhack.echomultiblockcore.integration.MultiblockRenderCoreClientIntegration")
                    .getMethod("registerBlockRenderers", Object.class)
                    .invoke(null, event);
        } catch (ReflectiveOperationException | LinkageError exception) {
            EchoMultiblockCore.LOGGER.warn("ECHO MultiblockCore RenderCore block integration could not be registered.", exception);
        }
    }
}
