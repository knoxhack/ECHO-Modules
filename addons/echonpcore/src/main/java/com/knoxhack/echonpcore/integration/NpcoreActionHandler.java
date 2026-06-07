package com.knoxhack.echonpcore.integration;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeAction;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeActionOutcome;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class NpcoreActionHandler extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echonpcore:action_host";
    private static final String ACTION_NPC_INTERACT = "npcore.npc_interact";
    private static final String ACTION_TRADE = "npcore.trade";
    private static final String ACTION_QUEST_GIVE = "npcore.quest_give";
    private static final NpcoreActionHandler HOST = new NpcoreActionHandler();

    private NpcoreActionHandler() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                        "EchoNativeRuntimeHost.WorldState",
                        "EchoNativeRuntimeHost.Events",
                        "EchoNativeRuntimeHost.Capabilities",
                        "EchoNativeRuntimeHost.SaveData"),
                Set.of(
                        ACTION_NPC_INTERACT,
                        ACTION_TRADE,
                        ACTION_QUEST_GIVE),
                Set.of(),
                true,
                true,
                true));

        EchoRuntimeActionDispatcher.global().registerAction(RUNTIME_HOST_ID, ACTION_NPC_INTERACT, NpcoreActionHandler::dispatchNpcInteract);
        EchoRuntimeActionDispatcher.global().registerAction(RUNTIME_HOST_ID, ACTION_TRADE, NpcoreActionHandler::dispatchTrade);
        EchoRuntimeActionDispatcher.global().registerAction(RUNTIME_HOST_ID, ACTION_QUEST_GIVE, NpcoreActionHandler::dispatchQuestGive);
    }

    private static EchoRuntimeActionOutcome dispatchNpcInteract(EchoNativeRuntimeHost host, EchoRuntimeAction action) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("phase", "before_interact");
        before.put("npc", action.inputPayload().getOrDefault("npc", "unknown"));

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("npc", action.inputPayload().getOrDefault("npc", "unknown"));
        after.put("phase", "after_interact");

        NativeResult result = NativeResult.mutated("NPC interacted.", Map.copyOf(after));
        return EchoRuntimeActionOutcome.of(Map.copyOf(before), result, Map.copyOf(after), false, true);
    }

    private static EchoRuntimeActionOutcome dispatchTrade(EchoNativeRuntimeHost host, EchoRuntimeAction action) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("phase", "before_trade");
        before.put("npc", action.inputPayload().getOrDefault("npc", "unknown"));

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("npc", action.inputPayload().getOrDefault("npc", "unknown"));
        after.put("item", action.inputPayload().getOrDefault("item", "unknown"));
        after.put("phase", "after_trade");

        NativeResult result = NativeResult.mutated("Trade completed.", Map.copyOf(after));
        return EchoRuntimeActionOutcome.of(Map.copyOf(before), result, Map.copyOf(after), true, true);
    }

    private static EchoRuntimeActionOutcome dispatchQuestGive(EchoNativeRuntimeHost host, EchoRuntimeAction action) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("phase", "before_quest_give");
        before.put("quest", action.inputPayload().getOrDefault("quest", "unknown"));

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("quest", action.inputPayload().getOrDefault("quest", "unknown"));
        after.put("npc", action.inputPayload().getOrDefault("npc", "unknown"));
        after.put("phase", "after_quest_give");

        NativeResult result = NativeResult.mutated("Quest given.", Map.copyOf(after));
        return EchoRuntimeActionOutcome.of(Map.copyOf(before), result, Map.copyOf(after), true, true);
    }
}
