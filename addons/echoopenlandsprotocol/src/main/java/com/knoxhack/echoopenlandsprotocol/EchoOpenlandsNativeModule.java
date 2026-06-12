package com.knoxhack.echoopenlandsprotocol;

import com.knoxhack.echoopenlandsprotocol.contract.OpenlandsRuntimeContracts;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoOpenlandsNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = OpenlandsRuntimeContracts.MODULE_ID;

    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> result = new LinkedHashMap<>(OpenlandsRuntimeContracts.adapterManifest());
        result.put("activated", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context == null ? OpenlandsRuntimeContracts.PACK_ID : context.getOrDefault("packId", OpenlandsRuntimeContracts.PACK_ID));
        result.put("registeredFeatureContracts", OpenlandsRuntimeContracts.contractIds());
        result.put("contractResourcePaths", OpenlandsRuntimeContracts.contractResourcePaths());
        result.put("adapterBootstrapStepIds", OpenlandsRuntimeContracts.adapterLoadStepIds());
        result.put("requiredRuntimeEvidence", OpenlandsRuntimeContracts.runtimeEvidenceIds());
        result.put("requiredPublicAlphaEvidence", OpenlandsRuntimeContracts.requiredPublicAlphaEvidenceIds());
        result.put("standardMode", OpenlandsRuntimeContracts.STANDARD_MODE);
        result.put("hardcoreMetersDefault", false);
        result.put("adapterDomains", OpenlandsRuntimeContracts.REQUIRED_CONTENT_ROOTS);
        result.put("summary", "Openlands native contract exposes source-backed registry resources, typed adapter load phases, relaxed Standard rules, first-hour route, waystone parity, and alpha distribution gates.");
        return Map.copyOf(result);
    }
}
