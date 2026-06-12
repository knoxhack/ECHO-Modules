package com.echoplatform.echocore.api;

import java.util.LinkedHashSet;
import java.util.Set;

public record EchoProfile(String nexusPath, Set<String> completedArcs, Set<String> milestones) {
    public EchoProfile {
        nexusPath = nexusPath == null ? "" : nexusPath;
        completedArcs = completedArcs == null ? Set.of() : Set.copyOf(completedArcs);
        milestones = milestones == null ? Set.of() : Set.copyOf(milestones);
    }

    public static EchoProfile empty() {
        return new EchoProfile("", Set.of(), Set.of());
    }

    public String callsign() {
        return milestones.stream()
                .filter(value -> value != null && value.startsWith("callsign:"))
                .map(value -> value.substring("callsign:".length()))
                .findFirst()
                .orElse("Echo Operator");
    }

    public String difficulty() {
        return milestones.stream()
                .filter(value -> value != null && value.startsWith("difficulty:"))
                .map(value -> value.substring("difficulty:".length()))
                .findFirst()
                .orElse("standard");
    }

    public Set<String> discoveredRecords() {
        return milestones.stream()
                .filter(value -> value != null && value.startsWith("record:"))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public EchoProfile withNexusPath(String path) {
        return new EchoProfile(path, completedArcs, milestones);
    }

    public EchoProfile completeArc(String arc) {
        Set<String> next = new LinkedHashSet<>(completedArcs);
        if (arc != null && !arc.isBlank()) {
            next.add(arc);
        }
        return new EchoProfile(nexusPath, next, milestones);
    }

    public EchoProfile recordMilestone(String milestone) {
        Set<String> next = new LinkedHashSet<>(milestones);
        if (milestone != null && !milestone.isBlank()) {
            next.add(milestone);
        }
        return new EchoProfile(nexusPath, completedArcs, next);
    }
}
