package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWorldRegionTransitionRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWorldRegionTransitionResult;
import java.util.List;
import java.util.Map;

public final class EchoNativeWorldRegionTransitionBridge {
    private final String moduleId;

    public EchoNativeWorldRegionTransitionBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native world region transition module id");
    }

    public EchoWorldRegionTransitionResult transition(EchoWorldRegionTransitionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("world region transition request must not be null");
        }
        boolean hadPrevious = !request.previousRegionId().isBlank();
        boolean hasCurrent = !request.currentRegionId().isBlank();
        boolean sameRegion = hadPrevious && hasCurrent && request.previousRegionId().equals(request.currentRegionId());
        boolean entered = hasCurrent && !sameRegion;
        boolean exited = hadPrevious && !sameRegion;
        String eventType;
        if (entered && exited) {
            eventType = "SWITCH";
        } else if (entered) {
            eventType = "ENTER";
        } else if (exited) {
            eventType = "EXIT";
        } else {
            eventType = "STAY";
        }
        List<String> missionEvents = entered && !request.currentMissionId().isBlank()
                ? List.of(request.currentMissionId())
                : List.of();
        return new EchoWorldRegionTransitionResult(
                request.playerId(),
                request.previousRegionId(),
                request.currentRegionId(),
                eventType,
                entered,
                exited,
                missionEvents,
                request.gameTick(),
                request.sourceReason()
        );
    }

    public Map<String, Object> report(EchoWorldRegionTransitionRequest request) {
        EchoWorldRegionTransitionResult result = transition(request);
        return Map.of(
                "adapterCoreContract", "EchoWorldRegionTransition",
                "nativeLoaderBackend", moduleId,
                "playerId", result.playerId(),
                "previousRegionId", result.previousRegionId(),
                "currentRegionId", result.currentRegionId(),
                "eventType", result.eventType(),
                "regionEntered", result.regionEntered(),
                "regionExited", result.regionExited(),
                "missionEvents", result.missionEvents(),
                "gameTick", result.gameTick()
        );
    }
}
