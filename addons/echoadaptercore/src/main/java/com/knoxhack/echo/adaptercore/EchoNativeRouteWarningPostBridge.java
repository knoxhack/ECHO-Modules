package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoRouteWarningPostUseRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoRouteWarningPostUseResult;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeRouteWarningPostBridge {
    private final String moduleId;

    public EchoNativeRouteWarningPostBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native route warning post module id");
    }

    public EchoRouteWarningPostUseResult use(EchoRouteWarningPostUseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("route warning post request must not be null");
        }
        String message = "Route Warning Post: Risk is " + request.risk();
        Map<String, Object> hudState = new LinkedHashMap<>();
        hudState.put("routeWarning", message);
        hudState.put("risk", request.risk());
        hudState.put("severity", request.severity());
        hudState.put("weatherId", request.weatherId());
        Map<String, Object> audioState = new LinkedHashMap<>();
        audioState.put("cue", "echoweathercore:route_warning/" + request.risk().toLowerCase(java.util.Locale.ROOT));
        audioState.put("risk", request.risk());
        Map<String, Object> renderState = new LinkedHashMap<>();
        renderState.put("warningPostOverlay", "echoweathercore:route_warning_post/"
                + request.risk().toLowerCase(java.util.Locale.ROOT));
        renderState.put("severity", request.severity());
        renderState.put("risk", request.risk());
        renderState.put("routeRiskModifier", request.routeRiskModifier());
        return new EchoRouteWarningPostUseResult(
                request.playerId(),
                request.weatherId(),
                request.severity().toUpperCase(java.util.Locale.ROOT),
                request.risk().toUpperCase(java.util.Locale.ROOT),
                request.routeRiskModifier(),
                request.x(),
                request.y(),
                request.z(),
                message,
                hudState,
                audioState,
                renderState,
                request.gameTick(),
                request.sourceReason(),
                !request.playerId().isBlank()
        );
    }

    public Map<String, Object> report(EchoRouteWarningPostUseRequest request) {
        EchoRouteWarningPostUseResult result = use(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_route_warning_post");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("routeWarningPostResult", result);
        report.put("status", result.delivered() ? "DELIVERED" : "NO_PLAYER");
        report.put("summary", "Native Loader backend materialized a Route Warning Post interaction into player-facing warning state.");
        return Map.copyOf(report);
    }
}
