package com.knoxhack.echoprimecore.service;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoprimecore.EchoPrimeCore;
import com.knoxhack.echoprimecore.progression.PrimePlayerData;
import com.knoxhack.echoprimecore.progression.PrimeProgressionService;
import net.minecraft.server.level.ServerPlayer;

public final class PrimeWorldSignalService {
    private PrimeWorldSignalService() {
    }

    public static PrimeWorldMetadata metadata(ServerPlayer player) {
        if (player == null) {
            return new PrimeWorldMetadata(0, "unknown", "unknown", 0, 0, 0);
        }
        PrimePlayerData data = PrimePlayerData.get(player);
        int signalLevel = PrimeProgressionService.worldSignalLevel(player);
        int discoveredStructures = EchoCoreServices.structureDiscoveryService().discoveredRegions(player).size();
        int anomalyRecords = 0;
        if (data.hasFlag(EchoPrimeCore.id("first_aether_trace"))) {
            anomalyRecords++;
        }
        if (data.hasFlag(EchoPrimeCore.id("orbital_signal_found"))) {
            anomalyRecords++;
        }
        if (data.hasFlag(EchoPrimeCore.id("stationfall_trace_found"))) {
            anomalyRecords++;
        }
        if (data.hasFlag(EchoPrimeCore.id("nexus_trace_found"))) {
            anomalyRecords++;
        }
        int resourceRichness = 1;
        if (data.hasFlag(EchoPrimeCore.id("first_signal"))) {
            resourceRichness++;
        }
        if (data.hasFlag(EchoPrimeCore.id("first_ruin"))) {
            resourceRichness++;
        }
        if (data.hasFlag(EchoPrimeCore.id("first_machine"))) {
            resourceRichness++;
        }
        return new PrimeWorldMetadata(
                signalLevel,
                signalLevel >= 3 ? "active" : "stable",
                anomalyRecords > 1 ? "elevated" : "low",
                discoveredStructures,
                anomalyRecords,
                resourceRichness);
    }

    public record PrimeWorldMetadata(
            int worldSignalLevel,
            String regionStability,
            String regionDanger,
            int discoveredStructures,
            int anomalyRecords,
            int resourceRichness) {
    }
}
