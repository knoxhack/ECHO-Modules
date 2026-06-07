package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWorldHazardTransitionRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWorldHazardTransitionResult;
import java.util.List;
import java.util.Map;

public final class EchoNativeWorldHazardTransitionBridge {
    private final String moduleId;

    public EchoNativeWorldHazardTransitionBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native world hazard transition module id");
    }

    public EchoWorldHazardTransitionResult transition(EchoWorldHazardTransitionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("world hazard transition request must not be null");
        }
        boolean hadPrevious = !request.previousHazardId().isBlank();
        boolean hasCurrent = !request.currentHazardId().isBlank();
        boolean sameHazard = hadPrevious && hasCurrent && request.previousHazardId().equals(request.currentHazardId());
        boolean entered = hasCurrent && !sameHazard;
        boolean exited = hadPrevious && !sameHazard;
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
        List<String> statusEffects = entered && !request.statusEffectId().isBlank()
                ? List.of(request.statusEffectId())
                : List.of();
        return new EchoWorldHazardTransitionResult(
                request.playerId(),
                request.previousHazardId(),
                request.currentHazardId(),
                eventType,
                entered,
                exited,
                statusEffects,
                request.gameTick(),
                request.sourceReason()
        );
    }

    public Map<String, Object> report(EchoWorldHazardTransitionRequest request) {
        EchoWorldHazardTransitionResult result = transition(request);
        return Map.of(
                "adapterCoreContract", "EchoWorldHazardTransition",
                "nativeLoaderBackend", moduleId,
                "playerId", result.playerId(),
                "previousHazardId", result.previousHazardId(),
                "currentHazardId", result.currentHazardId(),
                "eventType", result.eventType(),
                "hazardEntered", result.hazardEntered(),
                "hazardExited", result.hazardExited(),
                "statusEffects", result.statusEffects(),
                "gameTick", result.gameTick()
        );
    }
}
