package com.knoxhack.echoindex.client;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echoindex.EchoIndexClient;
import com.knoxhack.echoindex.service.IndexService;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class IndexHotkeys {
    private static Screen lastRenderedScreen;
    private static int lastMouseX;
    private static int lastMouseY;

    private IndexHotkeys() {
    }

    public static void onScreenRendered(Object event) {
        recordNativeScreenRender(
                EchoBackendClientBridge.screen(event),
                EchoBackendClientBridge.screenMouseX(event),
                EchoBackendClientBridge.screenMouseY(event));
    }

    public static void onKeyPressed(Object event) {
        if (openNativeHoveredStack(EchoBackendClientBridge.screen(event), modeFor(EchoBackendClientBridge.keyEvent(event)))) {
            EchoBackendClientBridge.cancel(event);
        }
    }

    public static boolean recordNativeScreenRender(Screen screen, int mouseX, int mouseY) {
        if (screen == null) {
            return false;
        }
        lastRenderedScreen = screen;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return true;
    }

    public static boolean handleNativeKey(int key) {
        return openNativeHoveredStack(Minecraft.getInstance().screen, modeFor(key));
    }

    private static boolean openNativeHoveredStack(Screen screen, IndexRecipeScreen.Mode mode) {
        Minecraft minecraft = Minecraft.getInstance();
        if (mode == null || minecraft.player == null || screen == null || echoIndexScreen(screen)
                || screenHasFocusedInput(screen)) {
            return false;
        }
        ItemStack hoveredStack = hoveredContainerStack(screen);
        if (hoveredStack.isEmpty()) {
            return false;
        }
        EchoNativeLoadStatus lifecycleStatus = EchoIndexClient.publishNativeScreenLifecycle(
                "open",
                "index.hotkey_key_pressed",
                IndexRecipeScreen.class.getName(),
                Map.of(
                        "targetScreenClass", IndexRecipeScreen.class.getName(),
                        "transitionSource", "index_hotkey_hovered_stack",
                        "recipeMode", mode.name(),
                        "itemId", IndexService.itemId(hoveredStack.getItem()).toString()
                ));
        if (EchoIndexClient.nativeLoaderClientActiveForScreens()
                && lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
            return false;
        }
        minecraft.setScreen(new IndexRecipeScreen(hoveredStack, mode));
        return true;
    }

    private static IndexRecipeScreen.Mode modeFor(KeyEvent keyEvent) {
        if (EchoIndexClient.SHOW_RECIPE_KEY.matches(keyEvent)) {
            return IndexRecipeScreen.Mode.RECIPES;
        }
        if (EchoIndexClient.SHOW_USAGE_KEY.matches(keyEvent)) {
            return IndexRecipeScreen.Mode.USES;
        }
        return null;
    }

    private static IndexRecipeScreen.Mode modeFor(int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_R -> IndexRecipeScreen.Mode.RECIPES;
            case GLFW.GLFW_KEY_U -> IndexRecipeScreen.Mode.USES;
            default -> null;
        };
    }

    private static boolean echoIndexScreen(Screen screen) {
        return screen instanceof IndexCatalogScreen
                || screen instanceof IndexRecipeScreen
                || screen instanceof IndexDiagnosticsScreen;
    }

    private static boolean screenHasFocusedInput(Screen screen) {
        GuiEventListener focused = screen.getFocused();
        return focused != null && focused.isFocused();
    }

    private static ItemStack hoveredContainerStack(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> container) || screen != lastRenderedScreen) {
            return ItemStack.EMPTY;
        }
        int left = container.getLeftPos();
        int top = container.getTopPos();
        for (Slot slot : container.getMenu().slots) {
            if (slot == null || !slot.isActive() || !slot.hasItem()) {
                continue;
            }
            if (inside(lastMouseX, lastMouseY, left + slot.x, top + slot.y, 16, 16)) {
                return slot.getItem().copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}
