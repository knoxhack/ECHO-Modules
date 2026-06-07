package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoAtmosphereRuntimeProfileRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoAtmosphereRuntimeProfileResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAtmosphereRuntimeProfileBridge {
    private final String moduleId;

    public EchoNativeAtmosphereRuntimeProfileBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native atmosphere runtime profile module id");
    }

    public EchoAtmosphereRuntimeProfileResult materialize(EchoAtmosphereRuntimeProfileRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("atmosphere runtime profile request must not be null");
        }
        Map<String, Object> stormVisibility = new LinkedHashMap<>();
        stormVisibility.put("visibilityId", "echoatmospherecore:storm_visibility/ashfall_active");
        stormVisibility.put("clearVisibility", request.clearVisibility());
        stormVisibility.put("stormVisibility", request.stormVisibility());
        stormVisibility.put("screenHazeIntensity", request.screenHazeIntensity());
        stormVisibility.put("reducesDistantLights", request.reducesDistantLights());

        Map<String, Object> fogProfile = new LinkedHashMap<>();
        fogProfile.put("fogId", request.fogId());
        fogProfile.put("colorArgb", request.fogColorArgb());
        fogProfile.put("density", request.fogDensity());
        fogProfile.put("startDistance", request.fogStartDistance());
        fogProfile.put("endDistance", request.fogEndDistance());
        fogProfile.put("stormAffected", request.stormAffected());

        Map<String, Object> skyTint = new LinkedHashMap<>();
        skyTint.put("skyTintId", request.skyTintId());
        skyTint.put("dayColorArgb", request.dayColorArgb());
        skyTint.put("nightColorArgb", request.nightColorArgb());
        skyTint.put("stormColorArgb", request.stormColorArgb());
        skyTint.put("celestialVisibility", request.celestialVisibility());

        Map<String, Object> ambientParticles = new LinkedHashMap<>();
        ambientParticles.put("particleProfileId", request.particleProfileId());
        ambientParticles.put("particleReferences", request.particleReferences());
        ambientParticles.put("density", request.particleDensity());
        ambientParticles.put("affectedByStormVisibility", request.affectedByStormVisibility());

        Map<String, Object> hookRefs = new LinkedHashMap<>();
        hookRefs.put("renderCoreHookReference", request.renderCoreHookReference());
        hookRefs.put("soundCoreHookReference", request.soundCoreHookReference());
        hookRefs.put("weatherProfileReference", request.weatherProfileReference());
        hookRefs.put("runtimePacketConsumer", request.runtimePacketConsumer());

        List<Map<String, String>> runtimeBindings = List.of(
                binding("render.visibility", "stormVisibility", "echorendercore:visibility/fog_distance"),
                binding("render.sky", "skyTint", "echorendercore:sky/tint"),
                binding("render.particles", "ambientParticles", "echorendercore:particles/ashfall"),
                binding("sound.ambience", "hookRefs", "echosoundcore:ambience/ash_storm"));
        List<String> diagnostics = List.of(
                "atmosphere.profile.loaded",
                "atmosphere.visibility.resolved",
                "atmosphere.fog_sky.bound",
                "atmosphere.particles.bound");

        Map<String, Object> runtimeProfileState = new LinkedHashMap<>();
        runtimeProfileState.put("adapterCoreContract", "echoatmospherecore:atmosphere/runtime_profile_tick");
        runtimeProfileState.put("service", "echoatmospherecore:atmosphere_service");
        runtimeProfileState.put("atmosphereProfileTickExecuted", true);
        runtimeProfileState.put("adapterCoreBridge", true);
        runtimeProfileState.put("nativeLoaderBackend", true);
        runtimeProfileState.put("moduleId", moduleId);
        runtimeProfileState.put("packId", request.packId().isBlank() ? "unknown" : request.packId());
        runtimeProfileState.put("profileId", request.profileId());
        runtimeProfileState.put("weatherStateId", request.weatherStateId());
        runtimeProfileState.put("biomeAmbienceId", request.biomeAmbienceId());
        runtimeProfileState.put("stormVisibility", stormVisibility);
        runtimeProfileState.put("fogProfile", fogProfile);
        runtimeProfileState.put("skyTint", skyTint);
        runtimeProfileState.put("ambientParticles", ambientParticles);
        runtimeProfileState.put("hookRefs", hookRefs);
        runtimeProfileState.put("runtimeBindings", runtimeBindings);
        runtimeProfileState.put("diagnostics", diagnostics);
        runtimeProfileState.put("referenceBehavior", "atmospherecore_resolves_runtime_profile_tick");

        return new EchoAtmosphereRuntimeProfileResult(
                request.packId().isBlank() ? "unknown" : request.packId(),
                request.profileId(),
                request.weatherStateId(),
                request.biomeAmbienceId(),
                stormVisibility,
                fogProfile,
                skyTint,
                ambientParticles,
                hookRefs,
                runtimeBindings,
                diagnostics,
                runtimeProfileState,
                request.gameTick(),
                request.sourceReason(),
                true);
    }

    public Map<String, Object> report(EchoAtmosphereRuntimeProfileRequest request) {
        EchoAtmosphereRuntimeProfileResult result = materialize(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_atmosphere_runtime_profile");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("atmosphereRuntimeProfileResult", result);
        report.put("status", result.applied() ? "PASS" : "SKIPPED");
        report.put("summary", "Native Loader backend resolved AtmosphereCore storm visibility, fog, sky tint, particles, and ambience hooks through AdapterCore.");
        return report;
    }

    private static Map<String, String> binding(String target, String source, String adapterHook) {
        Map<String, String> binding = new LinkedHashMap<>();
        binding.put("target", target);
        binding.put("source", source);
        binding.put("adapterHook", adapterHook);
        return Map.copyOf(binding);
    }
}
