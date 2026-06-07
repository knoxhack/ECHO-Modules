package com.knoxhack.echo.npcore.client.screencore;

import com.knoxhack.echo.npcore.network.CloseNpcInteractionPacket;
import com.knoxhack.echo.npcore.network.EchoNpcScreenState;
import com.knoxhack.echo.npcore.network.RequestNpcServicePacket;
import com.knoxhack.echo.npcore.network.RequestNpcScreenRefreshPacket;
import com.knoxhack.echo.npcore.network.RequestNpcTradePacket;
import com.knoxhack.echo.npcore.network.SelectDialogueOptionPacket;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.action.EchoActionContext;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ScreenCoreNpcActions {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private ScreenCoreNpcActions() {
    }

    static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        EchoScreenRegistry.registerAction("npcore.tab", ScreenCoreNpcActions::tab);
        EchoScreenRegistry.registerAction("npcore.dialogue.select", ScreenCoreNpcActions::dialogue);
        EchoScreenRegistry.registerAction("npcore.trade.request", ScreenCoreNpcActions::trade);
        EchoScreenRegistry.registerAction("npcore.service.request", ScreenCoreNpcActions::service);
        EchoScreenRegistry.registerAction("npcore.close", ScreenCoreNpcActions::close);
        EchoScreenRegistry.registerAction("npcore.refresh", ScreenCoreNpcActions::refresh);
    }

    private static boolean tab(EchoActionContext context) {
        String tab = value(context).toLowerCase(Locale.ROOT);
        if ("exit".equals(tab) || "close".equals(tab)) {
            return close(context);
        }
        if (!validTab(tab)) {
            return false;
        }
        ScreenCoreNpcUiState.selectedTab(tab);
        ScreenCoreNpcScreenBridge.invalidate();
        return true;
    }

    private static boolean dialogue(EchoActionContext context) {
        EchoNpcScreenState state = ScreenCoreNpcUiState.state();
        if (state == null) {
            return false;
        }
        EchoNetClientActions.sendServerboundAction(new SelectDialogueOptionPacket(state.entityId(), value(context)));
        return true;
    }

    private static boolean trade(EchoActionContext context) {
        EchoNpcScreenState state = ScreenCoreNpcUiState.state();
        if (state == null) {
            return false;
        }
        EchoNetClientActions.sendServerboundAction(new RequestNpcTradePacket(state.entityId(), value(context)));
        return true;
    }

    private static boolean service(EchoActionContext context) {
        EchoNpcScreenState state = ScreenCoreNpcUiState.state();
        if (state == null) {
            return false;
        }
        EchoNetClientActions.sendServerboundAction(new RequestNpcServicePacket(state.entityId(), value(context)));
        return true;
    }

    private static boolean close(EchoActionContext context) {
        EchoNpcScreenState state = ScreenCoreNpcUiState.state();
        if (state != null) {
            EchoNetClientActions.sendServerboundAction(new CloseNpcInteractionPacket(state.entityId()));
        }
        ScreenCoreNpcUiState.clear();
        return context.close();
    }

    private static boolean refresh(EchoActionContext context) {
        EchoNpcScreenState state = ScreenCoreNpcUiState.state();
        if (state == null) {
            return false;
        }
        EchoNetClientActions.sendServerboundAction(
                new RequestNpcScreenRefreshPacket(state.entityId(), ScreenCoreNpcUiState.selectedTab()));
        ScreenCoreNpcScreenBridge.invalidate();
        return true;
    }

    private static boolean validTab(String tab) {
        return switch (tab) {
            case "talk", "trade", "services", "intel" -> true;
            default -> false;
        };
    }

    private static String value(EchoActionContext context) {
        return context == null || context.actionValue() == null ? "" : context.actionValue().trim();
    }
}
