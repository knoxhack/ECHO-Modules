package com.knoxhack.echoarmory.integration;

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

public final class ArmoryActionHandler extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoarmory:action_host";
    private static final String ACTION_ITEM_CRAFT = "armory.item_craft";
    private static final String ACTION_ITEM_USE = "armory.item_use";
    private static final String ACTION_EQUIP = "armory.equip";
    private static final ArmoryActionHandler HOST = new ArmoryActionHandler();

    private ArmoryActionHandler() {
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
                        ACTION_ITEM_CRAFT,
                        ACTION_ITEM_USE,
                        ACTION_EQUIP),
                Set.of(),
                true,
                true,
                true));

        EchoRuntimeActionDispatcher.global().registerAction(RUNTIME_HOST_ID, ACTION_ITEM_CRAFT, ArmoryActionHandler::dispatchItemCraft);
        EchoRuntimeActionDispatcher.global().registerAction(RUNTIME_HOST_ID, ACTION_ITEM_USE, ArmoryActionHandler::dispatchItemUse);
        EchoRuntimeActionDispatcher.global().registerAction(RUNTIME_HOST_ID, ACTION_EQUIP, ArmoryActionHandler::dispatchEquip);
    }

    private static EchoRuntimeActionOutcome dispatchItemCraft(EchoNativeRuntimeHost host, EchoRuntimeAction action) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("phase", "before_craft");

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("item", action.inputPayload().getOrDefault("item", "unknown"));
        after.put("count", action.inputPayload().getOrDefault("count", 1));
        after.put("phase", "after_craft");

        NativeResult result = NativeResult.mutated("Item crafted.", Map.copyOf(after));
        return EchoRuntimeActionOutcome.of(Map.copyOf(before), result, Map.copyOf(after), true, true);
    }

    private static EchoRuntimeActionOutcome dispatchItemUse(EchoNativeRuntimeHost host, EchoRuntimeAction action) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("phase", "before_use");

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("item", action.inputPayload().getOrDefault("item", "unknown"));
        after.put("phase", "after_use");

        NativeResult result = NativeResult.mutated("Item used.", Map.copyOf(after));
        return EchoRuntimeActionOutcome.of(Map.copyOf(before), result, Map.copyOf(after), true, true);
    }

    private static EchoRuntimeActionOutcome dispatchEquip(EchoNativeRuntimeHost host, EchoRuntimeAction action) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("phase", "before_equip");

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("slot", action.inputPayload().getOrDefault("slot", "unknown"));
        after.put("item", action.inputPayload().getOrDefault("item", "unknown"));
        after.put("phase", "after_equip");

        NativeResult result = NativeResult.mutated("Item equipped.", Map.copyOf(after));
        return EchoRuntimeActionOutcome.of(Map.copyOf(before), result, Map.copyOf(after), true, true);
    }
}
