package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoDifficultyProfile;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoDifficultyProfileSelectionRequest;
import com.knoxhack.echo.adaptercore.EchoWorldContracts.EchoDifficultyProfileSelectionResult;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class EchoNativeDifficultyProfileSelectionBridge {
    private final String moduleId;

    public EchoNativeDifficultyProfileSelectionBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "native difficulty profile selection module id");
    }

    public EchoDifficultyProfileSelectionResult select(EchoDifficultyProfileSelectionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("difficulty profile selection request must not be null");
        }
        String selectedDifficulty = normalizeDifficulty(request.requestedDifficulty());
        EchoDifficultyProfile profile = profileFor(selectedDifficulty);
        return new EchoDifficultyProfileSelectionResult(
                request.playerId(),
                request.regionId(),
                request.missionId(),
                request.requestedDifficulty(),
                selectedDifficulty,
                profile.id(),
                profile.hazardMultiplier(),
                profile.spawnMultiplier(),
                request.gameTick(),
                request.sourceReason(),
                true
        );
    }

    public EchoDifficultyProfile profile(EchoDifficultyProfileSelectionRequest request) {
        EchoDifficultyProfileSelectionResult result = select(request);
        return new EchoDifficultyProfile(result.difficultyId(), result.hazardMultiplier(), result.spawnMultiplier());
    }

    public Map<String, Object> report(EchoDifficultyProfileSelectionRequest request) {
        EchoDifficultyProfileSelectionResult result = select(request);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_difficulty_profile_selection");
        report.put("adapterCoreBridge", true);
        report.put("nativeLoaderBackend", true);
        report.put("difficultyProfileSelectionResult", result);
        report.put("status", result.selected() ? "PASS" : "SKIPPED");
        report.put("summary", "Native Loader backend selected a DifficultyCore hazard/spawn multiplier profile through AdapterCore world contracts.");
        return report;
    }

    private static EchoDifficultyProfile profileFor(String difficulty) {
        return switch (difficulty) {
            case "easy" -> new EchoDifficultyProfile("echodifficultycore:easy", 1.0D, 0.85D);
            case "hard" -> new EchoDifficultyProfile("echodifficultycore:hard", 1.5D, 1.25D);
            case "extreme" -> new EchoDifficultyProfile("echodifficultycore:extreme", 2.0D, 1.5D);
            default -> new EchoDifficultyProfile("echodifficultycore:normal", 1.25D, 1.0D);
        };
    }

    private static String normalizeDifficulty(String difficulty) {
        String normalized = AdapterContractGuards.requireText(difficulty, "difficulty profile selection difficulty")
                .toLowerCase(Locale.ROOT)
                .strip();
        if (normalized.startsWith("echodifficultycore:")) {
            normalized = normalized.substring("echodifficultycore:".length());
        }
        return switch (normalized) {
            case "easy", "normal", "hard", "extreme" -> normalized;
            default -> "normal";
        };
    }
}
