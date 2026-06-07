package com.knoxhack.echopresencelink.presence;

import com.knoxhack.echopresencelink.EchoPresenceLink;
import com.knoxhack.echopresencelink.api.EchoPresenceContext;
import com.knoxhack.echopresencelink.api.EchoPresenceProvider;
import com.knoxhack.echopresencelink.api.EchoPresenceSnapshot;
import java.util.Locale;
import net.minecraft.resources.Identifier;

public final class TerminalPresenceProvider implements EchoPresenceProvider {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(EchoPresenceLink.MODID, "terminal");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public EchoPresenceSnapshot snapshot(EchoPresenceContext context) {
        if (context == null) {
            return null;
        }
        String className = context.screenClassName().toLowerCase(Locale.ROOT);
        String title = context.screenTitle();
        String titleLower = title.toLowerCase(Locale.ROOT);
        boolean terminal = className.contains("echoterminal")
                || className.contains("signalos")
                || titleLower.contains("echo terminal")
                || titleLower.contains("terminal");
        if (!terminal) {
            return null;
        }
        String surface = title.isBlank() ? "Protocol Roadmap" : title;
        String state = titleLower.contains("archive") ? "Reviewing field archives"
                : titleLower.contains("mission") ? "Reviewing mission routes"
                : "Reviewing the Protocol Roadmap";
        return new EchoPresenceSnapshot(ID, 80, "ECHO Terminal", state, "echo_terminal",
                surface, "echo_ashfall", "ECHO", context.sessionStartEpochSeconds(),
                java.util.List.of(), false);
    }

    @Override
    public int order() {
        return 20;
    }
}
