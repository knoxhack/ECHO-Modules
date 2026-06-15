package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockEntitySnapshot;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockState;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeCapabilityRequest;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeItemStack;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationProofKind;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationReceipt;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationTarget;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePosition;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeSaveData;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeStructurePlacement;

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

    public NativeResult dispatchInventoryGrant(String runtimeHostId, String actionId, NativePlayerRef player,
                                               NativeItemStack stack, NativeMutationContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operation", "inventory_grant");
        payload.put("stack", stackSnapshot(stack));
        return dispatchGameplayAction(runtimeHostId, actionId, payload, player, "", null, null, context);
    }

    public NativeResult dispatchInventoryRemove(String runtimeHostId, String actionId, NativePlayerRef player,
                                                String itemId, int count, NativeMutationContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operation", "inventory_remove");
        payload.put("itemId", AdapterContractGuards.requireText(itemId, "inventory remove item id"));
        payload.put("count", count);
        return dispatchGameplayAction(runtimeHostId, actionId, payload, player, "", null, null, context);
    }

    public NativeResult dispatchPlayerState(String runtimeHostId, String actionId, NativePlayerRef player,
                                            NativePosition position, Map<String, Object> payload,
                                            NativeMutationContext context) {
        Map<String, Object> actionPayload = copyPayload(payload);
        actionPayload.putIfAbsent("operation", "player_state");
        if (position != null) {
            actionPayload.put("position", positionSnapshot(position));
        }
        return dispatchGameplayAction(runtimeHostId, actionId, actionPayload, player, "", position, null, context);
    }

    public NativeResult dispatchBlockPlacement(String runtimeHostId, String actionId, NativeBlockRef block,
                                               NativeBlockState state, Map<String, Object> payload,
                                               NativeMutationContext context) {
        Map<String, Object> actionPayload = copyPayload(payload);
        actionPayload.putIfAbsent("operation", "block_placement");
        actionPayload.put("blockState", blockStateSnapshot(state));
        return dispatchGameplayAction(runtimeHostId, actionId, actionPayload, null,
                block == null ? "" : block.dimensionId(), null, block, context);
    }

    public NativeResult dispatchStructurePlacement(String runtimeHostId, String actionId,
                                                   NativeStructurePlacement placement,
                                                   NativeMutationContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operation", "structure_placement");
        payload.put("structure", structureSnapshot(placement));
        NativePosition position = placement == null ? null : new NativePosition(
                placement.dimensionId(),
                placement.originX(),
                placement.originY(),
                placement.originZ(),
                0.0F,
                0.0F);
        return dispatchGameplayAction(runtimeHostId, actionId, payload, null,
                placement == null ? "" : placement.dimensionId(), position, null, context);
    }

    public NativeResult dispatchBlockEntityTick(String runtimeHostId, String actionId, NativeBlockRef block,
                                                Map<String, Object> payload, NativeMutationContext context) {
        Map<String, Object> actionPayload = copyPayload(payload);
        actionPayload.putIfAbsent("operation", "block_entity_tick");
        return dispatchGameplayAction(runtimeHostId, actionId, actionPayload, null,
                block == null ? "" : block.dimensionId(), null, block, context);
    }

    public NativeResult dispatchBlockEntitySnapshotApply(String runtimeHostId, String actionId,
                                                         NativeBlockEntitySnapshot snapshot,
                                                         NativeMutationContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operation", "block_entity_apply_snapshot");
        payload.put("snapshot", blockEntitySnapshot(snapshot));
        NativeBlockRef block = snapshot == null ? null : snapshot.block();
        return dispatchGameplayAction(runtimeHostId, actionId, payload, null,
                block == null ? "" : block.dimensionId(), null, block, context);
    }

    public NativeResult dispatchCapabilityMutation(String runtimeHostId, String actionId,
                                                   NativeCapabilityRequest request, Map<String, Object> payload,
                                                   NativeMutationContext context) {
        Map<String, Object> actionPayload = copyPayload(payload);
        actionPayload.putIfAbsent("operation", "capability_mutation");
        actionPayload.put("capability", capabilitySnapshot(request));
        NativeBlockRef block = request == null ? null : request.block();
        return dispatchGameplayAction(runtimeHostId, actionId, actionPayload, null,
                block == null ? "" : block.dimensionId(), null, block, context);
    }

    public NativeResult dispatchSaveWrite(String runtimeHostId, String actionId, NativeSaveData data,
                                          NativeMutationContext context) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("operation", "save_write");
        payload.put("saveData", saveDataSnapshot(data));
        return dispatchGameplayAction(runtimeHostId, actionId, payload, null, "", null, null, context);
    }

    public NativeResult dispatchHudOrEvent(String runtimeHostId, String actionId, NativePlayerRef player,
                                           Map<String, Object> payload, NativeMutationContext context) {
        Map<String, Object> actionPayload = copyPayload(payload);
        actionPayload.putIfAbsent("operation", "hud_or_event");
        return dispatchGameplayAction(runtimeHostId, actionId, actionPayload, player, "", null, null, context);
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
            NativeResult strictResult = enforceReleaseProof(canonicalAction, outcome);
            append(
                    canonicalAction,
                    outcome.beforeSummary(),
                    outcome.afterSummary(),
                    strictResult,
                    outcome.saveTouched(),
                    outcome.hudOrEventEmitted());
            return strictResult;
        } catch (RuntimeException exception) {
            NativeResult result = NativeResult.failed("Runtime action handler failed.", Map.of(
                    "failureReason", failureReason(exception),
                    "runtimeHostId", canonicalAction.runtimeHostId(),
                    "actionId", canonicalAction.actionId()));
            append(canonicalAction, Map.of(), Map.of(), result, false, false);
            return result;
        }
    }

    private NativeResult dispatchGameplayAction(
            String runtimeHostId,
            String actionId,
            Map<String, Object> inputPayload,
            NativePlayerRef targetPlayer,
            String targetWorldId,
            NativePosition targetPosition,
            NativeBlockRef targetBlock,
            NativeMutationContext context) {
        return dispatch(new EchoRuntimeAction(
                actionId,
                runtimeHostId,
                inputPayload,
                targetPlayer,
                targetWorldId,
                targetPosition,
                targetBlock,
                context));
    }

    /**
     * Converts factual host mutation claims into release-grade evidence, or
     * fails the action when the result is metadata-only, queued-only, or
     * diagnostic-only.
     */
    private NativeResult enforceReleaseProof(EchoRuntimeAction action, EchoRuntimeActionOutcome outcome) {
        NativeResult result = outcome.result();
        if (!result.completedWithMutation()) {
            return result;
        }
        if (result.hasReleaseProof()) {
            return result;
        }
        if (result.receipt() != null) {
            return failedMissingReleaseProof(action, result,
                    "mutation receipt is diagnostic-only, queued-only, or lacks host evidence");
        }
        if (!outcome.hasReleaseProofEvidence()) {
            return failedMissingReleaseProof(action, result,
                    "missing before/after, save, HUD, packet, or host-returned mutation evidence");
        }
        return result.withReceipt(receiptFor(action, outcome, result));
    }

    private NativeResult failedMissingReleaseProof(EchoRuntimeAction action, NativeResult result, String reason) {
        Map<String, Object> snapshot = new LinkedHashMap<>(result.snapshot());
        snapshot.put("failureReason", "mutated result missing release proof: " + reason);
        snapshot.put("originalResultStatus", result.resultStatus().name());
        snapshot.put("runtimeHostId", action.runtimeHostId());
        snapshot.put("actionId", action.actionId());
        snapshot.put("releaseProof", false);
        return NativeResult.failed("Runtime action returned MUTATED without release-grade mutation receipt.", snapshot);
    }

    private NativeMutationReceipt receiptFor(EchoRuntimeAction action, EchoRuntimeActionOutcome outcome,
                                             NativeResult result) {
        NativeMutationContext context = action.context();
        String moduleId = context == null ? "adaptercore:unknown_module" : context.moduleId();
        String idempotencyKey = context == null ? action.actionId() : context.idempotencyKey();
        NativeMutationProofKind proofKind = proofKindFor(action.actionId(), outcome);
        return new NativeMutationReceipt(
                action.actionId() + ":" + idempotencyKey,
                action.runtimeHostId(),
                moduleId,
                EchoNativeRuntimeHost.interfaceForHostApi(action.actionId()),
                action.actionId(),
                result.status(),
                proofKind,
                outcome.beforeSummary(),
                outcome.afterSummary(),
                outcome.saveTouched(),
                outcome.hudOrEventEmitted(),
                idempotencyKey);
    }

    private static NativeMutationProofKind proofKindFor(String actionId, EchoRuntimeActionOutcome outcome) {
        String normalizedAction = actionId == null ? "" : actionId.toLowerCase(java.util.Locale.ROOT);
        if (outcome.saveTouched()) {
            return NativeMutationProofKind.SAVE_WRITE;
        }
        if (outcome.hudOrEventEmitted()) {
            if (normalizedAction.contains("packet") || normalizedAction.contains("network")
                    || normalizedAction.contains("sync")) {
                return NativeMutationProofKind.PACKET_EVENT;
            }
            return NativeMutationProofKind.HUD_EVENT;
        }
        return NativeMutationProofKind.HOST_STATE;
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

    private static Map<String, Object> copyPayload(Map<String, Object> payload) {
        return payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
    }

    private static Map<String, Object> stackSnapshot(NativeItemStack stack) {
        if (stack == null) {
            return Map.of();
        }
        return Map.of(
                "itemId", stack.itemId(),
                "count", stack.count(),
                "components", stack.components());
    }

    private static Map<String, Object> positionSnapshot(NativePosition position) {
        return Map.of(
                "dimensionId", position.dimensionId(),
                "x", position.x(),
                "y", position.y(),
                "z", position.z(),
                "yaw", position.yaw(),
                "pitch", position.pitch());
    }

    private static Map<String, Object> blockStateSnapshot(NativeBlockState state) {
        if (state == null) {
            return Map.of();
        }
        return Map.of(
                "blockId", state.blockId(),
                "properties", state.properties());
    }

    private static Map<String, Object> structureSnapshot(NativeStructurePlacement placement) {
        if (placement == null) {
            return Map.of();
        }
        return Map.of(
                "structureId", placement.structureId(),
                "dimensionId", placement.dimensionId(),
                "originX", placement.originX(),
                "originY", placement.originY(),
                "originZ", placement.originZ(),
                "anchor", placement.anchor(),
                "constraints", placement.constraints());
    }

    private static Map<String, Object> blockEntitySnapshot(NativeBlockEntitySnapshot snapshot) {
        if (snapshot == null) {
            return Map.of();
        }
        return Map.of(
                "blockEntityId", snapshot.blockEntityId(),
                "block", blockRefSnapshot(snapshot.block()),
                "state", snapshot.state());
    }

    private static Map<String, Object> capabilitySnapshot(NativeCapabilityRequest request) {
        if (request == null) {
            return Map.of();
        }
        return Map.of(
                "capabilityId", request.capabilityId(),
                "block", blockRefSnapshot(request.block()),
                "side", request.side(),
                "query", request.query());
    }

    private static Map<String, Object> blockRefSnapshot(NativeBlockRef block) {
        if (block == null) {
            return Map.of();
        }
        return Map.of(
                "dimensionId", block.dimensionId(),
                "x", block.x(),
                "y", block.y(),
                "z", block.z());
    }

    private static Map<String, Object> saveDataSnapshot(NativeSaveData data) {
        if (data == null) {
            return Map.of();
        }
        return Map.of(
                "scope", data.scope(),
                "key", data.key(),
                "payload", data.payload());
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

        boolean hasReleaseProofEvidence() {
            boolean hasStateDelta = (!beforeSummary.isEmpty() || !afterSummary.isEmpty())
                    && !beforeSummary.equals(afterSummary);
            return hasStateDelta || saveTouched || hudOrEventEmitted;
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
