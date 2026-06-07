package com.knoxhack.echoblockworks.integration;

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

public final class BlockworksActionHandler extends EchoUnsupportedRuntimeHost {
    public static final String RUNTIME_HOST_ID = "echoblockworks:action_host";
    private static final String ACTION_BLOCK_PLACE = "blockworks.block_place";
    private static final String ACTION_BLOCK_BREAK = "blockworks.block_break";
    private static final BlockworksActionHandler HOST = new BlockworksActionHandler();

    private BlockworksActionHandler() {
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
                        ACTION_BLOCK_PLACE,
                        ACTION_BLOCK_BREAK),
                Set.of(),
                true,
                false,
                true));

        EchoRuntimeActionDispatcher.global().registerAction(RUNTIME_HOST_ID, ACTION_BLOCK_PLACE, BlockworksActionHandler::dispatchBlockPlace);
        EchoRuntimeActionDispatcher.global().registerAction(RUNTIME_HOST_ID, ACTION_BLOCK_BREAK, BlockworksActionHandler::dispatchBlockBreak);
    }

    private static EchoRuntimeActionOutcome dispatchBlockPlace(EchoNativeRuntimeHost host, EchoRuntimeAction action) {
        if (action.targetPosition() == null) {
            Map<String, Object> snapshot = Map.of("error", "missing target position");
            NativeResult result = NativeResult.failed("Block place failed: no target position.", snapshot);
            return EchoRuntimeActionOutcome.of(Map.of(), result, snapshot, false, false);
        }

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("position", positionSummary(action.targetPosition()));
        before.put("phase", "before_place");

        String blockId = String.valueOf(action.inputPayload().getOrDefault("block", "unknown"));

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("position", positionSummary(action.targetPosition()));
        after.put("block", blockId);
        after.put("phase", "after_place");

        NativeResult result = NativeResult.mutated("Block placed.", Map.copyOf(after));
        return EchoRuntimeActionOutcome.of(Map.copyOf(before), result, Map.copyOf(after), true, true);
    }

    private static EchoRuntimeActionOutcome dispatchBlockBreak(EchoNativeRuntimeHost host, EchoRuntimeAction action) {
        if (action.targetPosition() == null) {
            Map<String, Object> snapshot = Map.of("error", "missing target position");
            NativeResult result = NativeResult.failed("Block break failed: no target position.", snapshot);
            return EchoRuntimeActionOutcome.of(Map.of(), result, snapshot, false, false);
        }

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("position", positionSummary(action.targetPosition()));
        before.put("phase", "before_break");

        String blockId = String.valueOf(action.inputPayload().getOrDefault("block", "unknown"));

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("position", positionSummary(action.targetPosition()));
        after.put("block", blockId);
        after.put("phase", "after_break");

        NativeResult result = NativeResult.mutated("Block broken.", Map.copyOf(after));
        return EchoRuntimeActionOutcome.of(Map.copyOf(before), result, Map.copyOf(after), true, true);
    }

    private static Map<String, Object> positionSummary(EchoNativeRuntimeHost.NativePosition position) {
        if (position == null) {
            return Map.of();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("dimensionId", position.dimensionId());
        map.put("x", position.x());
        map.put("y", position.y());
        map.put("z", position.z());
        return Map.copyOf(map);
    }
}
