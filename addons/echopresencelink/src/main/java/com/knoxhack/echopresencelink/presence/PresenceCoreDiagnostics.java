package com.knoxhack.echopresencelink.presence;

import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoDiagnosticService;
import com.knoxhack.echopresencelink.EchoPresenceLink;
import com.knoxhack.echopresencelink.config.PresenceLinkConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class PresenceCoreDiagnostics implements EchoDiagnosticService {
    @Override
    public List<EchoDiagnosticBlocker> diagnostics(Player player) {
        List<EchoDiagnosticBlocker> diagnostics = new ArrayList<>();
        PresenceLinkDiagnostics.Snapshot snapshot = PresenceLinkDiagnostics.snapshot();
        diagnostics.add(new EchoDiagnosticBlocker(
                id("config"),
                EchoPresenceLink.MODID,
                PresenceLinkConfig.enabled() ? EchoDiagnosticBlocker.Severity.INFO : EchoDiagnosticBlocker.Severity.WARNING,
                "Presence Link Config",
                PresenceLinkConfig.enabled() ? "Presence Link is enabled on this client." : "Presence Link is disabled.",
                PresenceLinkConfig.enabled() ? "" : "Enable general.enabled in the Presence Link client config."));
        EchoDiagnosticBlocker.Severity severity = snapshot.lastFailure().isBlank()
                ? EchoDiagnosticBlocker.Severity.INFO
                : EchoDiagnosticBlocker.Severity.WARNING;
        diagnostics.add(new EchoDiagnosticBlocker(
                id("discord_ipc"),
                EchoPresenceLink.MODID,
                severity,
                "Discord IPC",
                snapshot.statusLine(),
                snapshot.lastFailure().isBlank() ? "" : "Start Discord desktop or verify the application id."));
        return diagnostics;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoPresenceLink.MODID, path);
    }
}
