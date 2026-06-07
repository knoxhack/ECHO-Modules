package com.knoxhack.echotutorialcore.integration.terminal;

import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.data.TutorialCoreRegistries;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import com.knoxhack.echotutorialcore.server.TutorialCardManager;
import com.knoxhack.echotutorialcore.server.TutorialProgressManager;
import com.knoxhack.echoterminal.api.TerminalAddonGuide;
import com.knoxhack.echoterminal.api.TerminalAddonInfo;
import com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonLink;
import com.knoxhack.echoterminal.api.TerminalAddonMetric;
import com.knoxhack.echoterminal.api.TerminalAddonSection;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class TutorialTerminalIntegration {
    private static boolean registered;

    private TutorialTerminalIntegration() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        TerminalAddonInfoRegistry.register(new com.knoxhack.echoterminal.api.TerminalAddonInfoProvider() {
            @Override
            public String chapterId() {
                return "echotutorialcore";
            }

            @Override
            public TerminalAddonInfo info(net.minecraft.world.entity.player.Player player) {
                TutorialPlayerData data = TutorialPlayerData.get(player);
                return new TerminalAddonInfo(
                        "ECHO-7 guidance overlay, first-hour flows, and contextual help.",
                        List.of(
                                new TerminalAddonMetric("Mode", TutorialProgressManager.getGuideMode(player).name(), "Current tutorial voice", 0xFF92F7A6),
                                new TerminalAddonMetric("Unread", String.valueOf(TutorialCardManager.unreadCount(player)), "Guide cards needing review", 0xFFFFD166),
                                new TerminalAddonMetric("Flows", data.completedFlowIds().size() + "/" + TutorialCoreRegistries.flowCount(), "Completed onboarding flows", 0xFF8CA7B5)),
                        List.of(new TerminalAddonSection("What Now", com.knoxhack.echotutorialcore.api.TutorialCoreApi.getRecommendedNextSteps(player))),
                        List.of(new TerminalAddonLink(Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "guide"), "Open Guide", "Cards, search, and guide mode controls", 0xFF92F7A6)),
                        TerminalAddonGuide.optional(45, "Onboarding", "Open the Guide tab when Ashfall stops making sense.",
                                List.of("Check unread guide cards", "Follow What Now recommendations", "Use Lens/Scanner/HoloMap when stuck")));
            }
        });

        // Client-side tab registration is loaded reflectively to avoid classloading
        // Terminal client classes on dedicated servers.
        try {
            Class<?> clientIntegration = Class.forName(
                    "com.knoxhack.echotutorialcore.integration.terminal.TutorialTerminalClientIntegration");
            clientIntegration.getMethod("register").invoke(null);
        } catch (ClassNotFoundException e) {
            EchoTutorialCore.LOGGER.debug("TutorialTerminalClientIntegration not present (client-only).");
        } catch (ReflectiveOperationException | LinkageError e) {
            EchoTutorialCore.LOGGER.warn("TutorialTerminalClientIntegration could not be registered.", e);
        }

        EchoTutorialCore.LOGGER.info("ECHO: TutorialCore integrated with Terminal.");
    }
}
