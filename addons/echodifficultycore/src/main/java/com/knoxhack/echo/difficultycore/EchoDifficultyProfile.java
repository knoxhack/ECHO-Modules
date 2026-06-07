package com.knoxhack.echo.difficultycore;

import com.knoxhack.echo.contentcore.EchoContentGate;
import com.knoxhack.echo.contentcore.EchoContentReference;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EchoDifficultyProfile(
        EchoDifficultyProfileId id,
        String displayName,
        EchoDifficultyMode mode,
        EchoModuleId owningModule,
        EchoContentReference hazardReference,
        EchoContentReference lootReference,
        EchoContentReference combatReference,
        EchoContentReference survivalDrainReference,
        List<EchoDifficultyTuning> tunings,
        EchoPackVariantDifficultyPolicy packVariantPolicy,
        EchoServerDifficultyPolicy serverPolicy,
        EchoContentGate gate,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoDifficultyProfile {
        Objects.requireNonNull(id, "id");
        displayName = DifficultyContractGuards.requireText(displayName, "difficulty profile display name");
        mode = mode == null ? EchoDifficultyMode.UNKNOWN : mode;
        tunings = DifficultyContractGuards.immutableList(tunings);
        gate = gate == null ? EchoContentGate.open() : gate;
        diagnostics = DifficultyContractGuards.immutableList(diagnostics);
        attributes = DifficultyContractGuards.immutableMap(attributes);
    }

    public boolean blocking() {
        return gate.blocksWhenMissing() || diagnostics.stream().anyMatch(EchoDiagnostic::blocking);
    }
}
