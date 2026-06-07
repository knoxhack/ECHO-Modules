package com.knoxhack.echo.adaptercore.smoke;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePosition;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher;
import com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Smoke test for all gameplay action dispatcher handlers.
 */
public final class EchoGameplayActionHandlerSmoke {
    private EchoGameplayActionHandlerSmoke() {
    }

    public static void main(String[] args) {
        Map<String, Object> report = capture();
        if (!Boolean.TRUE.equals(report.get("passed"))) {
            throw new AssertionError("EchoGameplayActionHandlerSmoke failed: " + report);
        }
        System.out.println("echo gameplay action handler smoke PASS handlers="
                + report.get("handlersRegistered") + " dispatchPass=" + report.get("dispatchPass"));
    }

    public static Map<String, Object> capture() {
        EchoRuntimeMutationLedger ledger = EchoRuntimeMutationLedger.global();
        ledger.clear();

        List<String> missingHandlers = registerHandlers();

        int beforeLedger = ledger.entries().size();

        // Dispatch block place action
        NativeResult blockPlace = EchoRuntimeActionDispatcher.global().dispatch(
                new EchoRuntimeActionDispatcher.EchoRuntimeAction(
                        "blockworks.block_place",
                        "echoblockworks:action_host",
                        Map.of("block", "minecraft:stone"),
                        null,
                        null,
                        new NativePosition("minecraft:overworld", 0.0D, 64.0D, 0.0D, 0.0F, 0.0F),
                        null,
                        context("gameplay-smoke-block-place")));

        // Dispatch combat damage action
        NativeResult combat = EchoRuntimeActionDispatcher.global().dispatch(
                new EchoRuntimeActionDispatcher.EchoRuntimeAction(
                        "combatcore.damage_dealt",
                        "echocombatcore:action_host",
                        Map.of("damage", 10, "target", "zombie"),
                        null,
                        null,
                        null,
                        null,
                        context("gameplay-smoke-combat")));

        int afterLedger = ledger.entries().size();
        boolean dispatchPass = blockPlace != null && blockPlace.resultStatus() == EchoNativeRuntimeHost.NativeResultStatus.MUTATED
                && combat != null && combat.resultStatus() == EchoNativeRuntimeHost.NativeResultStatus.MUTATED;

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", "echo.gameplay_action_handler_smoke.v1");
        report.put("passed", missingHandlers.isEmpty() && afterLedger > beforeLedger && dispatchPass);
        report.put("handlersRegistered", 5 - missingHandlers.size());
        report.put("missingHandlers", missingHandlers);
        report.put("dispatchPass", dispatchPass);
        report.put("ledgerEntries", afterLedger - beforeLedger);
        return Map.copyOf(report);
    }

    private static List<String> registerHandlers() {
        return List.of(
                        "com.knoxhack.echoblockworks.integration.BlockworksActionHandler",
                        "com.knoxhack.echoarmory.integration.ArmoryActionHandler",
                        "com.knoxhack.echorecipecore.integration.RecipecoreActionHandler",
                        "com.knoxhack.echocombatcore.integration.CombatcoreActionHandler",
                        "com.knoxhack.echonpcore.integration.NpcoreActionHandler")
                .stream()
                .filter(EchoGameplayActionHandlerSmoke::registerHandlerMissing)
                .toList();
    }

    private static boolean registerHandlerMissing(String className) {
        try {
            Class<?> type = Class.forName(className);
            Method register = type.getMethod("register");
            register.invoke(null);
            return false;
        } catch (ReflectiveOperationException exception) {
            return true;
        }
    }

    private static NativeMutationContext context(String idempotencyKey) {
        return new NativeMutationContext(
                "echoadaptercore",
                "minecraft:overworld",
                idempotencyKey,
                "SERVER",
                0L,
                Map.of("source", "gameplay_action_handler_smoke"));
    }
}
