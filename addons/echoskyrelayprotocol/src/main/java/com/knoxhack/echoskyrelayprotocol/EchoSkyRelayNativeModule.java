package com.knoxhack.echoskyrelayprotocol;

import com.knoxhack.echoskyrelayprotocol.contract.SkyRelayRuntimeContracts;
import com.knoxhack.echoskyrelayprotocol.integration.SkyRelaySystemIntegrationContracts;
import com.knoxhack.echoskyrelayprotocol.runtime.SkyRelayFragmentRuntime;
import com.knoxhack.echoskyrelayprotocol.runtime.SkyRelayProgressionRuntime;
import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoSkyRelayNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = SkyRelayRuntimeContracts.MODULE_ID;

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> result = new LinkedHashMap<>(SkyRelayRuntimeContracts.adapterManifest());
        result.put("activated", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context == null ? SkyRelayRuntimeContracts.PACK_ID : context.getOrDefault("packId", SkyRelayRuntimeContracts.PACK_ID));
        result.put("registeredFeatureContracts", SkyRelayRuntimeContracts.contractIds());
        result.put("contractResourcePaths", SkyRelayRuntimeContracts.contractResourcePaths());
        result.put("runtimeTargets", SkyRelayRuntimeContracts.RUNTIME_TARGETS);
        result.put("editionIds", SkyRelayRuntimeContracts.EDITION_IDS);
        result.put("fragmentRuntime", SkyRelayFragmentRuntime.adapterManifest());
        result.put("progressionRuntime", SkyRelayProgressionRuntime.adapterManifest());
        result.put("systemIntegrations", SkyRelaySystemIntegrationContracts.adapterManifest());
        result.put("summary", "Sky Relay native contract exposes stable fragment, anchor, storm, power, Terminal, Lens, HoloMap, recovery, and release-readiness surfaces.");
        return Map.copyOf(result);
    }
}
