package com.knoxhack.echomultiblockcore.api;

import java.util.List;

public record MultiblockCapabilityRuntime(
        List<CapabilityNode> nodes,
        List<CapabilityThroughput> throughput,
        List<CapabilityDiagnostic> diagnostics) {
    public static final MultiblockCapabilityRuntime EMPTY = new MultiblockCapabilityRuntime(List.of(), List.of(), List.of());

    public MultiblockCapabilityRuntime {
        nodes = List.copyOf(nodes == null ? List.of() : nodes);
        throughput = List.copyOf(throughput == null ? List.of() : throughput);
        diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
    }

    public boolean satisfied() {
        return diagnostics.stream().noneMatch(CapabilityDiagnostic::blocking)
                && throughput.stream().allMatch(CapabilityThroughput::satisfied);
    }

    public String summary() {
        if (throughput.isEmpty()) {
            return "No runtime capability demand";
        }
        return throughput.stream()
                .map(line -> line.capabilityId().getPath() + " " + line.available() + "/" + line.required() + " " + line.unit())
                .reduce((left, right) -> left + ", " + right)
                .orElse("No runtime capability demand");
    }
}
