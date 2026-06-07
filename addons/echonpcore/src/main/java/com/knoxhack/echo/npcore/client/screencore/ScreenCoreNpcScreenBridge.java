package com.knoxhack.echo.npcore.client.screencore;

import com.knoxhack.echo.npcore.EchoNpcCore;
import com.knoxhack.echo.npcore.network.EchoNpcScreenState;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.EchoScreens;
import com.knoxhack.echoscreencore.client.screen.EchoScreen;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class ScreenCoreNpcScreenBridge {
    static final Identifier PAGE_ID = Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "npc_interaction");
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private ScreenCoreNpcScreenBridge() {
    }

    public static boolean open(EchoNpcScreenState state) {
        if (state == null || "close".equals(state.currentTab()) || Minecraft.getInstance().player == null) {
            return false;
        }
        register();
        ScreenCoreNpcUiState.open(state);
        boolean opened = EchoScreens.open(PAGE_ID, context());
        if (!opened) {
            ScreenCoreNpcUiState.clear();
            EchoNpcCore.LOGGER.warn("ScreenCore declined NPCore page {}; classic NPC screen will be used.", PAGE_ID);
        }
        return opened;
    }

    public static boolean sync(EchoNpcScreenState state) {
        if (!ScreenCoreNpcUiState.activeFor(state)) {
            return false;
        }
        if ("close".equals(state.currentTab())) {
            ScreenCoreNpcUiState.clear();
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        if (!(Minecraft.getInstance().screen instanceof EchoScreen screen) || !PAGE_ID.equals(screen.pageId())) {
            ScreenCoreNpcUiState.clear();
            return false;
        }
        ScreenCoreNpcUiState.update(state);
        screen.markDataDirty();
        EchoScreens.invalidatePage(PAGE_ID);
        return true;
    }

    static EchoDataContext context() {
        return EchoDataContext.empty()
                .missingPlaceholder("")
                .provider("npcore", ScreenCoreNpcDataProviders.PROVIDER)
                .put("screen.title", "ECHO NPC");
    }

    static void invalidate() {
        EchoScreens.invalidatePage(PAGE_ID);
    }

    private static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        ScreenCoreNpcDataProviders.register();
        ScreenCoreNpcActions.register();
        EchoScreenRegistry.registerStyleSheet(Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "npc_interaction"));
        EchoNpcCore.LOGGER.info("ECHO: NPCore registered ScreenCore NPC page, provider, actions, and styles.");
    }

    static List<String> smokeHints() {
        return List.of(
                "Right-click an ECHO NPC with useScreenCoreNpcScreens=true.",
                "Use Talk, Trade, Services, and Intel tabs on the ScreenCore page.",
                "Disable ScreenCore NPC screens to verify the classic fallback.");
    }
}
