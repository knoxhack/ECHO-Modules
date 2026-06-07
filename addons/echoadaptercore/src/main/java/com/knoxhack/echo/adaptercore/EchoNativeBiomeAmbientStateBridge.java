package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoBiomeAmbientStateRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoBiomeAmbientStateResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeBiomeAmbientStateBridge {
    private final String moduleId;

    public EchoNativeBiomeAmbientStateBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native biome ambient module id");
    }

    public EchoBiomeAmbientStateResult apply(EchoBiomeAmbientStateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("biome ambient state request must not be null");
        }
        Map<String, Object> hudState = new LinkedHashMap<>();
        hudState.put("biomeProfile", request.biomeProfileId());
        hudState.put("biomeTag", request.biomeTag());
        hudState.put("ambience", request.ambienceId());

        Map<String, Object> audioState = new LinkedHashMap<>();
        audioState.put("cue", request.soundProfileId());
        audioState.put("loop", true);
        audioState.put("biomeProfile", request.biomeProfileId());

        Map<String, Object> renderState = new LinkedHashMap<>();
        renderState.put("atmosphereProfile", request.atmosphereProfileId());
        renderState.put("particleProfile", request.particleProfileId());
        renderState.put("visibilityModifier", request.visibilityModifier());
        renderState.put("ambientAssets", request.ambientAssetIds());

        List<Map<String, String>> runtimeBindings = List.of(
                binding("hud.biome", "biomeProfile", "echohudcore:biome/status_line"),
                binding("sound.ambience", "soundProfile", request.soundProfileId()),
                binding("render.particles", "particleProfile", request.particleProfileId()),
                binding("render.atmosphere", "atmosphereProfile", request.atmosphereProfileId()));

        Map<String, Object> ambientState = new LinkedHashMap<>();
        ambientState.put("adapterCoreContract", "echobiomecore:biome/ambient_state");
        ambientState.put("adapterCoreBridge", true);
        ambientState.put("nativeLoaderBackend", true);
        ambientState.put("moduleId", moduleId);
        ambientState.put("playerId", request.playerId());
        ambientState.put("biomeProfileId", request.biomeProfileId());
        ambientState.put("biomeTag", request.biomeTag());
        ambientState.put("ambienceId", request.ambienceId());
        ambientState.put("soundProfileId", request.soundProfileId());
        ambientState.put("particleProfileId", request.particleProfileId());
        ambientState.put("ambientAssetIds", request.ambientAssetIds());
        ambientState.put("atmosphereProfileId", request.atmosphereProfileId());
        ambientState.put("visibilityModifier", request.visibilityModifier());
        ambientState.put("hudState", hudState);
        ambientState.put("audioState", audioState);
        ambientState.put("renderState", renderState);
        ambientState.put("runtimeBindings", runtimeBindings);
        ambientState.put("gameTick", request.gameTick());
        ambientState.put("sourceReason", request.sourceReason());

        return new EchoBiomeAmbientStateResult(
                request.playerId(),
                request.biomeProfileId(),
                request.biomeTag(),
                request.ambienceId(),
                hudState,
                audioState,
                renderState,
                ambientState,
                runtimeBindings,
                request.gameTick(),
                request.sourceReason(),
                true
        );
    }

    public Map<String, Object> report(EchoBiomeAmbientStateRequest request) {
        EchoBiomeAmbientStateResult result = apply(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_biome_ambient_state");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("biomeAmbientStateResult", result);
        report.put("status", result.applied() ? "PASS" : "SKIPPED");
        report.put("summary", "Native Loader backend applied a BiomeCore ambient profile into retained HUD/audio/render state.");
        return Map.copyOf(report);
    }

    private static Map<String, String> binding(String target, String source, String adapterHook) {
        Map<String, String> binding = new LinkedHashMap<>();
        binding.put("target", target);
        binding.put("source", source);
        binding.put("adapterHook", adapterHook);
        return Map.copyOf(binding);
    }
}
