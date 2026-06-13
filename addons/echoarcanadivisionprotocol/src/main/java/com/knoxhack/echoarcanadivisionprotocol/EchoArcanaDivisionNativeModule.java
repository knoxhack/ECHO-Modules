package com.knoxhack.echoarcanadivisionprotocol;

import dev.echo.nativeplatform.contracts.EchoNativeSurfaceModuleEntrypoint;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EchoArcanaDivisionNativeModule implements EchoNativeSurfaceModuleEntrypoint {
    public static final String MODULE_ID = EchoArcanaDivisionProtocol.MODID;

    @Override
    public Map<String, Object> describeNativeSurfaces(Map<String, String> context) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activated", true);
        result.put("moduleId", MODULE_ID);
        result.put("packId", context == null ? MODULE_ID : context.getOrDefault("packId", MODULE_ID));
        result.put("foundationModules", EchoArcanaDivisionProtocol.FOUNDATION_MODULES);
        result.put("arcanaModules", EchoArcanaDivisionProtocol.ARCANA_MODULES);
        result.put("launcherSupportModules", EchoArcanaDivisionProtocol.LAUNCHER_SUPPORT_MODULES);
        result.put("runtimeModules", EchoArcanaDivisionProtocol.BETA_RUNTIME_MODULES);
        result.put("registeredFeatureContracts", java.util.List.of(
                "arcana_division.protocol",
                "arcana_division.research",
                "arcana_division.rituals",
                "arcana_division.spells",
                "arcana_division.familiars",
                "arcana_division.curses",
                "arcana_division.rifts",
                "arcana_division.anomaly_containment",
                "arcana_division.release_readiness"
        ));
        result.put("contractResourcePaths", java.util.List.of(
                "data/echoarcanadivisionprotocol/arcana_division/contracts/bootstrap_profile_routes.json",
                "data/echoarcanadivisionprotocol/arcana_division/contracts/module_ownership.json",
                "data/echoarcanadivisionprotocol/arcana_division/contracts/release_gates.json"
        ));
        result.put("summary", "Arcana Division native contract exposes magical research, rituals, spells, familiars, curses, rifts, anomaly containment, and release-readiness surfaces without using the NeoForge @Mod root as the Native entrypoint.");
        return Map.copyOf(result);
    }
}
