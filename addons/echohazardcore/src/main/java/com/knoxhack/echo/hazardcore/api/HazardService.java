package com.knoxhack.echo.hazardcore.api;

import com.echoplatform.echocore.api.EchoServiceRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Central hazard runtime. Packs register hazard sources and resistance providers,
 * then tick exposure through {@link #tickPlayer(ServerPlayer)}.
 */
public final class HazardService {
    private static HazardService instance;

    private final Map<HazardType, HazardType> registeredHazards = new ConcurrentHashMap<>();
    private final List<IHazardSource> sources = Collections.synchronizedList(new ArrayList<>());
    private final List<IHazardResistanceProvider> resistanceProviders = Collections.synchronizedList(new ArrayList<>());

    private HazardService() {
        // Register built-in hazards.
        registerHazard(HazardType.PRESSURE);
        registerHazard(HazardType.OXYGEN_DEPRIVATION);
        registerHazard(HazardType.COLD);
        registerHazard(HazardType.HEAT);
        registerHazard(HazardType.CORRUPTION);
        registerHazard(HazardType.DECOMPRESSION_SICKNESS);
    }

    public static synchronized HazardService getInstance() {
        if (instance == null) {
            instance = new HazardService();
            EchoServiceRegistry.register(HazardService.class, instance);
        }
        return instance;
    }

    public static HazardService find() {
        return EchoServiceRegistry.find(HazardService.class).orElseGet(HazardService::getInstance);
    }

    public void registerHazard(HazardType hazard) {
        registeredHazards.put(hazard, hazard);
    }

    public List<HazardType> getRegisteredHazards() {
        return List.copyOf(registeredHazards.values());
    }

    public void registerSource(IHazardSource source) {
        sources.add(source);
    }

    public void registerResistanceProvider(IHazardResistanceProvider provider) {
        resistanceProviders.add(provider);
    }

    public float getTotalResistance(ServerPlayer player, HazardType hazard) {
        float resistance = 0.0f;
        synchronized (resistanceProviders) {
            for (IHazardResistanceProvider provider : resistanceProviders) {
                resistance += provider.getResistance(player, hazard);
            }
        }
        return Math.min(resistance, 1.0f);
    }

    public HazardExposure computeExposure(ServerPlayer player, HazardType hazard) {
        float maxIntensity = 0.0f;
        String sourceKey = "none";
        float threshold = 0.0f;
        synchronized (sources) {
            for (IHazardSource source : sources) {
                if (!source.produces(hazard)) continue;
                HazardExposure exposure = source.computeExposure(player, hazard);
                if (exposure.intensity() > maxIntensity) {
                    maxIntensity = exposure.intensity();
                    sourceKey = exposure.sourceKey();
                    threshold = exposure.threshold();
                }
            }
        }
        return new HazardExposure(hazard, maxIntensity, threshold, sourceKey);
    }

    /**
     * Compute final exposure after resistance. Values are clamped to non-negative.
     */
    public HazardExposure computeFinalExposure(ServerPlayer player, HazardType hazard) {
        HazardExposure base = computeExposure(player, hazard);
        float resistance = getTotalResistance(player, hazard);
        float finalIntensity = base.intensity() * (1.0f - resistance);
        return new HazardExposure(hazard, finalIntensity, base.threshold(), base.sourceKey());
    }

    /**
     * Evaluate every registered hazard for the player. Callers apply damage/status effects.
     */
    public List<HazardExposure> tickPlayer(ServerPlayer player) {
        List<HazardExposure> result = new ArrayList<>();
        for (HazardType hazard : getRegisteredHazards()) {
            HazardExposure finalExposure = computeFinalExposure(player, hazard);
            result.add(finalExposure);
        }
        return result;
    }
}
