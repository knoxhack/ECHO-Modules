package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherExposureMitigationRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherExposureMitigationResult;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWeatherExposureModifier;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoNativeWeatherExposureMitigationBridge {
    private final String moduleId;

    public EchoNativeWeatherExposureMitigationBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "weather exposure module id");
    }

    public EchoWeatherExposureMitigationResult mitigate(EchoWeatherExposureMitigationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("weather exposure mitigation request must not be null");
        }
        EchoWeatherExposureModifier weather = request.weatherModifier();
        EchoWeatherExposureModifier countermeasure = request.countermeasureModifier();
        double filterDrain = weather.filterDrainMultiplier();
        double radiationExposure = weather.radiationExposureMultiplier();
        double toxicExposure = weather.toxicExposureMultiplier();
        double coldExposure = weather.coldExposureMultiplier();
        double heatExposure = weather.heatExposureMultiplier();
        double routeRisk = weather.routeRiskModifier();
        if (request.sheltered()) {
            filterDrain *= countermeasure.filterDrainMultiplier();
            radiationExposure *= countermeasure.radiationExposureMultiplier();
            toxicExposure *= countermeasure.toxicExposureMultiplier();
            coldExposure *= countermeasure.coldExposureMultiplier();
            heatExposure *= countermeasure.heatExposureMultiplier();
            routeRisk *= countermeasure.routeRiskModifier();
        }
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("moduleId", moduleId);
        state.put("filterDrainMultiplier", filterDrain);
        state.put("radiationExposureMultiplier", radiationExposure);
        state.put("toxicExposureMultiplier", toxicExposure);
        state.put("coldExposureMultiplier", coldExposure);
        state.put("heatExposureMultiplier", heatExposure);
        state.put("routeRiskModifier", routeRisk);
        return new EchoWeatherExposureMitigationResult(
                request.playerId(),
                request.weatherId(),
                request.weatherType(),
                request.sheltered(),
                state,
                request.gameTick(),
                request.sourceReason(),
                request.sheltered());
    }

    public Map<String, Object> report(EchoWeatherExposureMitigationRequest request) {
        EchoWeatherExposureMitigationResult result = mitigate(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("playerId", result.playerId());
        report.put("weatherId", result.weatherId());
        report.put("weatherType", result.weatherType());
        report.put("sheltered", result.sheltered());
        report.put("modifierState", result.modifierState());
        report.put("gameTick", result.gameTick());
        report.put("sourceReason", result.sourceReason());
        report.put("mitigated", result.mitigated());
        return Map.copyOf(report);
    }
}
