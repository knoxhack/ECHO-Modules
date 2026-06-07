package com.knoxhack.echocombatcore.integration;

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

public final class CombatcoreActionHandler extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echocombatcore:action_host";
    private static final String ACTION_DAMAGE_DEALT = "combatcore.damage_dealt";
    private static final String ACTION_DAMAGE_TAKEN = "combatcore.damage_taken";
    private static final String ACTION_KILL = "combatcore.kill";
    private static final CombatcoreActionHandler HOST = new CombatcoreActionHandler();

    private CombatcoreActionHandler() {
        super(RUNTIME_HOST_ID);
    }

    public static void register() {
        EchoRuntimeHostRegistry.global().register(HOST, new EchoRuntimeHostCapabilities(
                RUNTIME_HOST_ID,
                Set.of(
                        "EchoNativeRuntimeHost.WorldState",
                        "EchoNativeRuntimeHost.Events",
                        "EchoNativeRuntimeHost.Capabilities"),
                Set.of(
                        ACTION_DAMAGE_DEALT,
                        ACTION_DAMAGE_TAKEN,
                        ACTION_KILL),
                Set.of(),
                true,
                false,
                true));

        EchoRuntimeActionDispatcher.global().registerAction(RUNTIME_HOST_ID, ACTION_DAMAGE_DEALT, CombatcoreActionHandler::dispatchDamageDealt);
        EchoRuntimeActionDispatcher.global().registerAction(RUNTIME_HOST_ID, ACTION_DAMAGE_TAKEN, CombatcoreActionHandler::dispatchDamageTaken);
        EchoRuntimeActionDispatcher.global().registerAction(RUNTIME_HOST_ID, ACTION_KILL, CombatcoreActionHandler::dispatchKill);
    }

    private static EchoRuntimeActionOutcome dispatchDamageDealt(EchoNativeRuntimeHost host, EchoRuntimeAction action) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("phase", "before_damage_dealt");
        before.put("target", action.inputPayload().getOrDefault("target", "unknown"));

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("damage", action.inputPayload().getOrDefault("damage", 0));
        after.put("target", action.inputPayload().getOrDefault("target", "unknown"));
        after.put("phase", "after_damage_dealt");

        NativeResult result = NativeResult.mutated("Damage dealt.", Map.copyOf(after));
        return EchoRuntimeActionOutcome.of(Map.copyOf(before), result, Map.copyOf(after), false, true);
    }

    private static EchoRuntimeActionOutcome dispatchDamageTaken(EchoNativeRuntimeHost host, EchoRuntimeAction action) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("phase", "before_damage_taken");
        before.put("source", action.inputPayload().getOrDefault("source", "unknown"));

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("damage", action.inputPayload().getOrDefault("damage", 0));
        after.put("source", action.inputPayload().getOrDefault("source", "unknown"));
        after.put("phase", "after_damage_taken");

        NativeResult result = NativeResult.mutated("Damage taken.", Map.copyOf(after));
        return EchoRuntimeActionOutcome.of(Map.copyOf(before), result, Map.copyOf(after), false, true);
    }

    private static EchoRuntimeActionOutcome dispatchKill(EchoNativeRuntimeHost host, EchoRuntimeAction action) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("phase", "before_kill");
        before.put("target", action.inputPayload().getOrDefault("target", "unknown"));

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("target", action.inputPayload().getOrDefault("target", "unknown"));
        after.put("phase", "after_kill");

        NativeResult result = NativeResult.mutated("Kill recorded.", Map.copyOf(after));
        return EchoRuntimeActionOutcome.of(Map.copyOf(before), result, Map.copyOf(after), false, true);
    }
}
