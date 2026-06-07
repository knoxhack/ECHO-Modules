package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationTarget;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePosition;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class EchoRuntimeActionDispatcher {
    private static final EchoRuntimeActionDispatcher GLOBAL = new EchoRuntimeActionDispatcher(
            EchoRuntimeHostRegistry.global(),
            EchoRuntimeMutationLedger.global(),
            EchoContentAliasResolver.standard());

    private final EchoRuntimeHostRegistry hostRegistry;
    private final EchoRuntimeMutationLedger ledger;
    private final EchoContentAliasResolver aliasResolver;
    private final ConcurrentMap<ActionKey, EchoRuntimeActionHandler> handlers = new ConcurrentHashMap<>();

    public EchoRuntimeActionDispatcher(
            EchoRuntimeHostRegistry hostRegistry,
            EchoRuntimeMutationLedger ledger,
            EchoContentAliasResolver aliasResolver) {
        this.hostRegistry = hostRegistry == null ? EchoRuntimeHostRegistry.global() : hostRegistry;
        this.ledger = ledger == null ? EchoRuntimeMutationLedger.global() : ledger;
        this.aliasResolver = aliasResolver == null ? EchoContentAliasResolver.standard() : aliasResolver;
    }

    public static EchoRuntimeActionDispatcher global() {
        return GLOBAL;
    }

    public void registerAction(String runtimeHostId, String actionId, EchoRuntimeActionHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("runtime action handler must not be null");
        }
        handlers.put(new ActionKey(runtimeHostId, aliasResolver.resolveActionId(actionId)), handler);
    }

    public NativeResult dispatch(EchoRuntimeAction action) {
        return dispatch(action, null);
    }

    public NativeResult dispatch(EchoRuntimeAction action, EchoRuntimeActionHandler directHandler) {
        if (action == null) {
            NativeResult result = NativeResult.failed("AdapterCore action dispatch failed for missing action.", Map.of(
                    "failureReason", "missing action"));
            ledger.append(
                    "adaptercore:missing_action",
                    "adaptercore:missing_host",
                    Map.of(),
                    NativeMutationTarget.none(),
                    Map.of(),
                    Map.of(),
                    result,
                    false,
                    false);
            return result;
        }

        EchoRuntimeAction canonicalAction = action.withActionId(aliasResolver.resolveActionId(action.actionId()));
        EchoRuntimeHostRegistry.RegisteredRuntimeHost registered = hostRegistry.resolve(canonicalAction.runtimeHostId())
                .orElse(null);
        if (registered == null) {
            NativeResult result = NativeResult.unsupported("No runtime host is registered for AdapterCore action.", Map.of(
                    "failureReason", "missing runtime host",
                    "runtimeHostId", canonicalAction.runtimeHostId(),
                    "actionId", canonicalAction.actionId()));
            append(canonicalAction, Map.of(), Map.of(), result, false, false);
            return result;
        }
        if (!registered.capabilities().supportsAction(canonicalAction.actionId())) {
            NativeResult result = NativeResult.unsupported("Runtime host does not declare AdapterCore action support.", Map.of(
                    "failureReason", "runtime action not declared by host capabilities",
                    "runtimeHostId", canonicalAction.runtimeHostId(),
                    "actionId", canonicalAction.actionId()));
            append(canonicalAction, Map.of(), registered.capabilities().snapshot(), result, false, false);
            return result;
        }

        ActionKey key = new ActionKey(canonicalAction.runtimeHostId(), canonicalAction.actionId());
        EchoRuntimeActionHandler handler = directHandler == null ? handlers.get(key) : directHandler;
        if (handler == null) {
            NativeResult result = NativeResult.unsupported("Runtime host does not implement AdapterCore action.", Map.of(
                    "failureReason", "missing runtime action handler",
                    "runtimeHostId", canonicalAction.runtimeHostId(),
                    "actionId", canonicalAction.actionId()));
            append(canonicalAction, Map.of(), registered.capabilities().snapshot(), result, false, false);
            return result;
        }

        try {
            EchoRuntimeActionOutcome outcome = handler.dispatch(registered.host(), canonicalAction);
            if (outcome == null || outcome.result() == null) {
                NativeResult result = NativeResult.failed("Runtime action handler returned no result.", Map.of(
                        "failureReason", "missing runtime result",
                        "runtimeHostId", canonicalAction.runtimeHostId(),
                        "actionId", canonicalAction.actionId()));
                append(canonicalAction, Map.of(), Map.of(), result, false, false);
                return result;
            }
            append(
                    canonicalAction,
                    outcome.beforeSummary(),
                    outcome.afterSummary(),
                    outcome.result(),
                    outcome.saveTouched(),
                    outcome.hudOrEventEmitted());
            return outcome.result();
        } catch (RuntimeException exception) {
            NativeResult result = NativeResult.failed("Runtime action handler failed.", Map.of(
                    "failureReason", failureReason(exception),
                    "runtimeHostId", canonicalAction.runtimeHostId(),
                    "actionId", canonicalAction.actionId()));
            append(canonicalAction, Map.of(), Map.of(), result, false, false);
            return result;
        }
    }

    private static String failureReason(RuntimeException exception) {
        String reason = exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getName()
                : exception.getClass().getName() + ": " + exception.getMessage();
        StackTraceElement[] stack = exception.getStackTrace();
        if (stack.length == 0) {
            return reason;
        }
        StackTraceElement frame = stack[0];
        return reason + " at " + frame.getClassName() + "." + frame.getMethodName()
                + "(" + frame.getFileName() + ":" + frame.getLineNumber() + ")";
    }

    private void append(
            EchoRuntimeAction action,
            Map<String, Object> beforeSummary,
            Map<String, Object> afterSummary,
            NativeResult result,
            boolean saveTouched,
            boolean hudOrEventEmitted) {
        ledger.append(
                action.actionId(),
                action.runtimeHostId(),
                action.inputPayload(),
                action.target(),
                beforeSummary,
                afterSummary,
                result,
                saveTouched,
                hudOrEventEmitted);
    }

    public record EchoRuntimeAction(
            String actionId,
            String runtimeHostId,
            Map<String, Object> inputPayload,
            NativePlayerRef targetPlayer,
            String targetWorldId,
            NativePosition targetPosition,
            NativeBlockRef targetBlock,
            NativeMutationContext context) {
        public EchoRuntimeAction {
            actionId = AdapterContractGuards.requireText(actionId, "runtime action id");
            runtimeHostId = AdapterContractGuards.requireText(runtimeHostId, "runtime host id");
            inputPayload = inputPayload == null ? Map.of() : Map.copyOf(inputPayload);
            targetWorldId = AdapterContractGuards.optionalText(targetWorldId);
        }

        EchoRuntimeAction withActionId(String canonicalActionId) {
            return new EchoRuntimeAction(
                    canonicalActionId,
                    runtimeHostId,
                    inputPayload,
                    targetPlayer,
                    targetWorldId,
                    targetPosition,
                    targetBlock,
                    context);
        }

        NativeMutationTarget target() {
            return new NativeMutationTarget(targetPlayer, targetWorldId, targetPosition, targetBlock);
        }

        public Map<String, Object> targetSnapshot() {
            return target().snapshot();
        }
    }

    public record EchoRuntimeActionOutcome(
            Map<String, Object> beforeSummary,
            NativeResult result,
            Map<String, Object> afterSummary,
            boolean saveTouched,
            boolean hudOrEventEmitted) {
        public EchoRuntimeActionOutcome {
            beforeSummary = beforeSummary == null ? Map.of() : Map.copyOf(beforeSummary);
            afterSummary = afterSummary == null ? Map.of() : Map.copyOf(afterSummary);
        }

        public static EchoRuntimeActionOutcome of(
                Map<String, Object> beforeSummary,
                NativeResult result,
                Map<String, Object> afterSummary,
                boolean saveTouched,
                boolean hudOrEventEmitted) {
            return new EchoRuntimeActionOutcome(
                    beforeSummary,
                    result,
                    afterSummary,
                    saveTouched,
                    hudOrEventEmitted);
        }
    }

    @FunctionalInterface
    public interface EchoRuntimeActionHandler {
        EchoRuntimeActionOutcome dispatch(EchoNativeRuntimeHost host, EchoRuntimeAction action);
    }

    private record ActionKey(String runtimeHostId, String actionId) {
        private ActionKey {
            runtimeHostId = AdapterContractGuards.requireText(runtimeHostId, "runtime host id");
            actionId = AdapterContractGuards.requireText(actionId, "runtime action id");
        }
    }
}
