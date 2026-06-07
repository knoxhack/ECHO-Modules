package com.knoxhack.echoruntimeguard.api;

import java.util.Map;
import net.minecraft.resources.Identifier;

public record NetworkSnapshot(
        int packetsThisSecond,
        int bytesThisSecond,
        int warnings,
        int duplicateDrops,
        Map<Identifier, Integer> packetsByChannel,
        Map<Identifier, Integer> bytesByChannel) {
    public NetworkSnapshot {
        packetsByChannel = packetsByChannel == null || packetsByChannel.isEmpty() ? Map.of() : Map.copyOf(packetsByChannel);
        bytesByChannel = bytesByChannel == null || bytesByChannel.isEmpty() ? Map.of() : Map.copyOf(bytesByChannel);
    }
}
