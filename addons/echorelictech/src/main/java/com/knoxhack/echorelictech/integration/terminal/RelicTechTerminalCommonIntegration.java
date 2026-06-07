package com.knoxhack.echorelictech.integration.terminal;

import com.knoxhack.echorelictech.api.RelicTechApi;
import com.knoxhack.echoterminal.api.TerminalActionRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonGuide;
import com.knoxhack.echoterminal.api.TerminalAddonInfo;
import com.knoxhack.echoterminal.api.TerminalAddonInfoProvider;
import com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonLink;
import com.knoxhack.echoterminal.api.TerminalAddonMetric;
import com.knoxhack.echoterminal.api.TerminalAddonSection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class RelicTechTerminalCommonIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int ACCENT = 0xFF55FFDD;

    private RelicTechTerminalCommonIntegration() {}

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        registerActions();
        TerminalAddonInfoRegistry.register(new RelicTechAddonInfoProvider());
    }

    private static void registerActions() {
        TerminalActionRegistry.register(RelicTechTerminalIds.RELICTECH_TAB, RelicTechTerminalIds.SCAN_RELICS,
            (player, payload) -> {
                if (player == null) {
                    return;
                }
                player.sendSystemMessage(Component.literal("ECHO RELICTECH // Scanning for relic signatures..."));
                if (player instanceof ServerPlayer serverPlayer) {
                    player.sendSystemMessage(Component.literal(RelicTechApi.getTerminalRelicSummary(serverPlayer)));
                }
            });

        TerminalActionRegistry.register(RelicTechTerminalIds.RELICTECH_TAB, RelicTechTerminalIds.OPEN_ANALYZER,
            (player, payload) -> {
                if (player != null) {
                    player.sendSystemMessage(Component.literal("ECHO RELICTECH // Place a Relic Analyzer to begin identification."));
                }
            });
    }

    private static final class RelicTechAddonInfoProvider implements TerminalAddonInfoProvider {
        @Override
        public String chapterId() {
            return "relictech";
        }

        @Override
        public TerminalAddonInfo info(Player player) {
            String relicCount = "0";
            String analyzed = "0";
            String contained = "0";
            String instability = "0";
            if (player instanceof ServerPlayer serverPlayer) {
                relicCount = Integer.toString(RelicTechApi.getPlayerRelicSummary(serverPlayer).values().stream().mapToInt(Integer::intValue).sum());
                analyzed = Integer.toString(RelicTechApi.getAnalyzedRelicCount(serverPlayer));
                contained = Integer.toString(RelicTechApi.getContainedRelicCount(serverPlayer));
                instability = Integer.toString(RelicTechApi.getInstability(serverPlayer));
            }

            String summary = "ECHO: RelicTech - Recover, analyze, stabilize, and risk using pre-Gridfall relics.";
            List<TerminalAddonMetric> metrics = List.of(
                new TerminalAddonMetric("Relics Held", relicCount, "Live profile", ACCENT),
                new TerminalAddonMetric("Analyzed", analyzed, "Relic Analyzer", ACCENT),
                new TerminalAddonMetric("Contained", contained, "Locker-safe", ACCENT),
                new TerminalAddonMetric("Instability", instability, "Player risk", ACCENT)
            );
            List<TerminalAddonSection> sections = List.of(
                new TerminalAddonSection("Core Machines", List.of(
                    "Relic Analyzer - identifies unknown relics",
                    "Prototype Workbench - stabilizes, overclocks, contains, purges",
                    "Containment Locker - stores dangerous relics",
                    "Null Battery Dock - charges Null Batteries"
                )),
                new TerminalAddonSection("Starter Relics", List.of(
                    "Phase Anchor - bind/recall teleport",
                    "Echo Mirror - defensive echo projection",
                    "Gravity Clamp - push/pull entities",
                    "Rift Lantern - reveal hostile rift traces",
                    "Blood Circuit - health-for-power overcharge",
                    "Broken Climate Key - storm control pulse",
                    "Soul Capacitor - soul-aether ward",
                    "Void Compass - vault locator"
                )),
                new TerminalAddonSection("Support Devices", List.of(
                    "Guardian Lens - scan for relic traces",
                    "Matter Stitcher - heal/armor repair",
                    "Null Battery - stores Null Charge"
                ))
            );
            List<TerminalAddonLink> links = List.of(
                new TerminalAddonLink(RelicTechTerminalIds.RELICTECH_TAB, "RelicTech", "Relic vault and machine records", ACCENT)
            );
            TerminalAddonGuide guide = TerminalAddonGuide.optional(50, "Side route",
                "RelicTech is optional relic progression; start it when you find pre-Gridfall vaults.",
                List.of(
                    "Open RelicTech and scan for relic vaults.",
                    "Use Relic Analyzer to identify unknown relics.",
                    "Stabilize relics in Prototype Workbench for safe use."
                ));
            return new TerminalAddonInfo(summary, metrics, sections, links, guide);
        }
    }
}
