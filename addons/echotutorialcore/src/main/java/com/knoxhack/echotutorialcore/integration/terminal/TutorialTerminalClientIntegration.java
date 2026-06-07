package com.knoxhack.echotutorialcore.integration.terminal;

import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.TutorialCategory;
import com.knoxhack.echotutorialcore.api.card.TutorialCard;
import com.knoxhack.echotutorialcore.data.TutorialCoreRegistries;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class TutorialTerminalClientIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    public static final Identifier TAB_ID = Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "guide");
    private static final int ACCENT = 0xFF92F7A6;

    private TutorialTerminalClientIntegration() {}

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        TerminalTabRegistry.register(new TutorialGuideTab());
        TerminalNavigationProfiles.register(TAB_ID, TerminalNavigationProfile.intel(45));
        EchoTutorialCore.LOGGER.info("ECHO: TutorialCore Terminal Guide tab registered.");
    }
}
