package com.knoxhack.echomissioncore.integration;

import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoDiagnosticService;
import com.knoxhack.echomissioncore.EchoMissionCore;
import com.knoxhack.echomissioncore.service.MissionCoreService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public enum MissionCoreDiagnostics implements EchoDiagnosticService {
    INSTANCE;

    @Override
    public List<EchoDiagnosticBlocker> diagnostics(Player player) {
        List<String> warnings = MissionCoreService.INSTANCE.validateContent();
        if (warnings.isEmpty()) {
            return List.of();
        }
        List<EchoDiagnosticBlocker> blockers = new ArrayList<>();
        int index = 0;
        for (String warning : warnings.stream().limit(12).toList()) {
            blockers.add(new EchoDiagnosticBlocker(
                    Identifier.fromNamespaceAndPath(EchoMissionCore.MODID, "validation/" + index++),
                    EchoMissionCore.MODID,
                    EchoDiagnosticBlocker.Severity.WARNING,
                    "MissionCore Validation",
                    warning,
                    "Run /echomission validate and fix the owning addon or datapack entry."));
        }
        return List.copyOf(blockers);
    }
}
