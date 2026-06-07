package com.knoxhack.echotutorialcore.integration.terminal;

import com.knoxhack.echoterminal.client.hud.TerminalHudNotice;
import com.knoxhack.echoterminal.client.hud.TerminalHudNoticeSurface;
import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.client.TutorialToastOverlay;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TutorialTerminalNoticeIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final String OWNER = EchoTutorialCore.MODID;

    private TutorialTerminalNoticeIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        TerminalHudNoticeSurface.registerExternalNoticeSupplier(OWNER, TutorialTerminalNoticeIntegration::activeNotice);
        TutorialToastOverlay.setInternalToastRendererAllowedSupplier(TerminalHudNoticeSurface::shouldRenderInternalCards);
        EchoTutorialCore.LOGGER.info("ECHO: TutorialCore HUD notices routed through Terminal surface.");
    }

    public static Optional<TerminalHudNotice> activeNotice() {
        return TutorialToastOverlay.activeNoticeSnapshotForHud().map(TutorialTerminalNoticeIntegration::toTerminalNotice);
    }

    public static TerminalHudNotice noticeForHudForTests(String title, String message, String details, boolean danger) {
        return toTerminalNotice(TutorialToastOverlay.noticeSnapshotForTests(title, message, details, danger));
    }

    private static TerminalHudNotice toTerminalNotice(TutorialToastOverlay.NoticeSnapshot snapshot) {
        return new TerminalHudNotice(
                snapshot.sourceLabel(),
                snapshot.statusLabel(),
                snapshot.title(),
                snapshot.detail(),
                snapshot.footer(),
                snapshot.accentColor(),
                0.0F,
                1);
    }
}
