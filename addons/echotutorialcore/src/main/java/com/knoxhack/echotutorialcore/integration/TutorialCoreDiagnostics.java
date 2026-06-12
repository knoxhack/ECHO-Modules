package com.knoxhack.echotutorialcore.integration;

import com.echoplatform.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class TutorialCoreDiagnostics {
    private TutorialCoreDiagnostics() {}

    public static List<EchoDiagnosticBlocker> diagnostics(Player player) {
        TutorialPlayerData data = TutorialPlayerData.get(player);
        List<EchoDiagnosticBlocker> blockers = new ArrayList<>();
        if (data.unreadCardCount() > 0) {
            blockers.add(blocker("unread_guidance", EchoDiagnosticBlocker.Severity.INFO,
                    "Unread ECHO-7 guidance",
                    data.unreadCardCount() + " guide card(s) are unread.",
                    "Open Terminal > Guide."));
        }
        if (data.lastProgressGameTime() > 0 && player != null
                && player.level().getGameTime() - data.lastProgressGameTime() >= 45L * 1200L) {
            blockers.add(blocker("stuck_state", EchoDiagnosticBlocker.Severity.WARNING,
                    "Progress stalled",
                    "No major tutorial progress has been recorded recently.",
                    "Open Terminal > Guide > What Now."));
        }
        if (!data.hasProgress(Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "used_scanner"))) {
            blockers.add(blocker("missing_route_basics", EchoDiagnosticBlocker.Severity.INFO,
                    "Route basics missing",
                    "Scanner route discovery has not been recorded.",
                    "Use the scanner, then check HoloMap."));
        }
        return blockers;
    }

    private static EchoDiagnosticBlocker blocker(String path, EchoDiagnosticBlocker.Severity severity,
            String title, String detail, String nextAction) {
        return new EchoDiagnosticBlocker(Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, path),
                EchoTutorialCore.MODID, severity, title, detail, nextAction);
    }
}
