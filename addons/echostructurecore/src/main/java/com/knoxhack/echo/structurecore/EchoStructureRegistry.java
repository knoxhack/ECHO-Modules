package com.knoxhack.echo.structurecore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoStructureRegistry(
        Map<EchoStructureId, EchoStructureProfile> structures,
        Map<EchoPoiId, EchoPoiMetadata> pointsOfInterest,
        List<EchoStructureBinding> bindings,
        List<EchoDiagnostic> diagnostics
) {
    public EchoStructureRegistry {
        structures = StructureContractGuards.immutableMap(structures);
        pointsOfInterest = StructureContractGuards.immutableMap(pointsOfInterest);
        bindings = StructureContractGuards.immutableList(bindings);
        diagnostics = StructureContractGuards.immutableList(diagnostics);
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || structures.values().stream().anyMatch(EchoStructureProfile::blocking)
                || pointsOfInterest.values().stream().anyMatch(EchoPoiMetadata::blocking);
    }
}
