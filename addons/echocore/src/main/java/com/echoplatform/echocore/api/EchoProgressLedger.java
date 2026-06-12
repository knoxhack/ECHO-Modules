package com.echoplatform.echocore.api;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public record EchoProgressLedger(Set<String> milestones) {
    public EchoProgressLedger {
        milestones = milestones == null ? Set.of() : Set.copyOf(milestones);
    }

    public static EchoProgressLedger empty() {
        return new EchoProgressLedger(Set.of());
    }

    public EchoProgressLedger withMilestone(String milestone) {
        if (milestone == null || milestone.isBlank()) {
            return this;
        }
        Set<String> next = new LinkedHashSet<>(milestones);
        next.add(milestone);
        return new EchoProgressLedger(next);
    }

    public boolean hasMilestone(String milestone) {
        if (milestone == null || milestone.isBlank()) {
            return false;
        }
        String requested = normalize(milestone);
        for (String present : milestones) {
            if (matches(requested, normalize(present))) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String requested, String present) {
        if (requested.equals(present)) {
            return true;
        }
        if (EchoHandoffs.STATIONFALL_BLACKBOX_RECOVERED.equals(requested)) {
            return Set.of(
                    "stationfall:blackbox_recovered",
                    "stationfall:blackbox_retrieved",
                    "stationfall.blackbox_recovered",
                    "stationfall.blackbox_retrieved",
                    "echostationfall:blackbox_recovered",
                    "echostationfall:blackbox_retrieved").contains(present);
        }
        if (EchoHandoffs.NEXUS_PROTOCOL_COMPLETE.equals(requested)) {
            return Set.of(
                    "nexus:protocol_complete",
                    "nexus_protocol_complete",
                    "echonexusprotocol:protocol_complete",
                    "echonexusprotocol:nexus_protocol_complete",
                    "nexus:path:restore",
                    "nexus:path:control",
                    "nexus:path:destroy",
                    "nexus:path:merge",
                    "ashfall:nexus:restore",
                    "ashfall:nexus:control",
                    "ashfall:nexus:destroy",
                    "ashfall:nexus:merge").contains(present);
        }
        return false;
    }

    private static String normalize(String milestone) {
        return milestone == null ? "" : milestone.trim().toLowerCase(Locale.ROOT);
    }
}
