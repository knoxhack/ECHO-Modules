package com.knoxhack.echogalacticsurveyprotocol;

import com.knoxhack.echogalacticsurveyprotocol.contract.GalacticSurveyRuntimeContracts;
import com.knoxhack.echogalacticsurveyprotocol.integration.GalacticSurveySystemIntegrationContracts;
import com.knoxhack.echogalacticsurveyprotocol.runtime.GalacticSurveyProbeRuntime;
import com.knoxhack.echogalacticsurveyprotocol.runtime.GalacticSurveyProgressionRuntime;
import com.knoxhack.echogalacticsurveyprotocol.runtime.GalacticSurveyRuntimeService;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoGalacticSurveyNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = GalacticSurveyRuntimeContracts.MODULE_ID;

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> result = new LinkedHashMap<>(GalacticSurveyRuntimeContracts.adapterManifest());
        result.put("activated", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context == null
                ? GalacticSurveyRuntimeContracts.PACK_ID
                : context.getOrDefault("packId", GalacticSurveyRuntimeContracts.PACK_ID));
        result.put("registeredFeatureContracts", GalacticSurveyRuntimeContracts.contractIds());
        result.put("contractResourcePaths", GalacticSurveyRuntimeContracts.contractResourcePaths());
        result.put("runtimeTargets", GalacticSurveyRuntimeContracts.RUNTIME_TARGETS);
        result.put("editionIds", GalacticSurveyRuntimeContracts.EDITION_IDS);
        result.put("probeRuntime", GalacticSurveyProbeRuntime.adapterManifest());
        result.put("progressionRuntime", GalacticSurveyProgressionRuntime.adapterManifest());
        result.put("surveyRuntime", GalacticSurveyRuntimeService.adapterManifest());
        result.put("systemIntegrations", GalacticSurveySystemIntegrationContracts.adapterManifest());
        result.put("summary", "Galactic Survey native contract exposes stable sector, probe, fuel route, depot, salvage, Terminal, Lens, HoloMap, Index, MissionCore, and release-readiness surfaces.");
        return Map.copyOf(result);
    }
}
