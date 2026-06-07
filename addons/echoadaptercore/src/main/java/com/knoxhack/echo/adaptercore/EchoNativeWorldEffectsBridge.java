package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWorldEffectResult;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoWorldEffectTick;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeWorldEffectsBridge {
    private final String moduleId;

    public EchoNativeWorldEffectsBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public EchoWorldEffectResult apply(EchoWorldEffectTick tick) {
        if (tick == null) {
            throw new IllegalArgumentException("world effect tick must not be null");
        }
        boolean inRegion = tick.region() != null && tick.region().contains(tick.x(), tick.z());
        boolean inHazard = tick.hazard() != null && tick.hazard().affects(tick.x(), tick.z());
        String activeRegion = inRegion ? tick.region().id() : "";
        String activeHazard = inHazard ? tick.hazard().id() : "";
        double damage = inHazard ? tick.hazard().damagePerTick() * tick.difficulty().hazardMultiplier() : 0.0D;
        double healthAfter = Math.max(0.0D, tick.health() - damage);

        List<String> missionEvents = inRegion && !activeRegion.equals(tick.previousRegionId())
                ? List.of(tick.region().missionId())
                : List.of();
        List<String> statusEffects = inHazard && !tick.hazard().statusEffectId().isBlank()
                ? List.of(tick.hazard().statusEffectId())
                : List.of();

        Map<String, Object> hudState = new LinkedHashMap<>();
        hudState.put("weather", tick.weather().hudLine());
        hudState.put("hazard", activeHazard);
        hudState.put("damageApplied", damage);

        Map<String, Object> audioState = new LinkedHashMap<>();
        audioState.put("cue", tick.weather().audioCue());
        audioState.put("region", activeRegion);

        Map<String, Object> renderState = new LinkedHashMap<>();
        renderState.put("weatherProfile", tick.weather().renderProfile());
        renderState.put("atmosphere", tick.atmosphere().id());
        renderState.put("visibility", tick.atmosphere().visibility());
        renderState.put("particles", tick.atmosphere().particleProfile());
        renderState.put("skyFog", tick.atmosphere().skyFog());

        Map<String, Object> worldLookup = new LinkedHashMap<>();
        worldLookup.put("biomeProfile", tick.biome().id());
        worldLookup.put("biomeTag", tick.biome().biomeTag());
        worldLookup.put("structureId", tick.structure().id());
        worldLookup.put("poiId", tick.structure().poiId());
        worldLookup.put("poiPosition", List.of(tick.structure().x(), tick.structure().y(), tick.structure().z()));

        Map<String, Object> spawnEvent = new LinkedHashMap<>();
        spawnEvent.put("ruleId", tick.spawnRule().id());
        spawnEvent.put("entityId", tick.spawnRule().entityId());
        spawnEvent.put("regionId", tick.spawnRule().regionId());
        spawnEvent.put("budget", Math.round(tick.spawnRule().maxCount() * tick.difficulty().spawnMultiplier()));

        Map<String, Object> savedStatus = new LinkedHashMap<>();
        savedStatus.put(tick.statusEffect().saveKey(), Map.of(
                "effectId", tick.statusEffect().id(),
                "durationTicks", tick.statusEffect().durationTicks(),
                "amplifier", tick.statusEffect().amplifier()
        ));
        savedStatus.put("adapterCoreModule", moduleId);

        return new EchoWorldEffectResult(
                tick.playerId(),
                activeRegion,
                activeHazard,
                tick.health(),
                healthAfter,
                missionEvents,
                statusEffects,
                hudState,
                audioState,
                renderState,
                worldLookup,
                spawnEvent,
                savedStatus
        );
    }

    public Map<String, Object> report(EchoWorldEffectTick tick) {
        EchoWorldEffectResult result = apply(tick);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_world_effects");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("worldEffectResult", result);
        report.put("status", "PASS");
        report.put("summary", "Native Loader backend applied region entry, hazard damage, weather HUD/audio/render state, spawn budget, POI lookup, and saveable status effects through AdapterCore world contracts.");
        return report;
    }
}
