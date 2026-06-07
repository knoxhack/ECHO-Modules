package com.knoxhack.echogrimoire.integration;

import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echoarcanacore.api.ArcanaProviderInterfaces;
import com.knoxhack.echoarcanacore.api.VeilboundRuntimeSnapshot;
import com.knoxhack.echoterminal.api.TerminalAddonGuide;
import com.knoxhack.echoterminal.api.TerminalAddonInfo;
import com.knoxhack.echoterminal.api.TerminalAddonInfoProvider;
import com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonMetric;
import com.knoxhack.echoterminal.api.TerminalAddonSection;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import com.knoxhack.echogrimoire.EchoGrimoire;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;

public final class GrimoireTerminalIntegration {
    private static boolean registered;

    private GrimoireTerminalIntegration() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        for (GrimoireEntries.Entry entry : GrimoireEntries.allEntries()) {
            TerminalArchiveRegistry.register(EchoGrimoire.MODID, new TerminalArchiveEntry(
                    entry.id(),
                    "Arcana Grimoire / " + entry.group(),
                    entry.title(),
                    entry.status(),
                    entry.lines(),
                    entry.locked()));
        }
        TerminalAddonInfoRegistry.register(new Provider());
        EchoGrimoire.LOGGER.info("ECHO: Grimoire Terminal archive entries registered.");
    }

    private static final class Provider implements TerminalAddonInfoProvider {
        @Override
        public String chapterId() {
            return "grimoire";
        }

        @Override
        public TerminalAddonInfo info(Player player) {
            int total = GrimoireEntries.allEntries().size();
            long locked = GrimoireEntries.allEntries().stream().filter(GrimoireEntries.Entry::locked).count();
            VeilboundRuntimeSnapshot snapshot = ArcanaCoreServices.providers(ArcanaProviderInterfaces.VeilboundRuntimeProvider.class)
                    .stream()
                    .findFirst()
                    .map(provider -> provider.snapshot(player))
                    .orElseGet(() -> VeilboundRuntimeSnapshot.unavailable(false));
            List<TerminalAddonMetric> metrics = new ArrayList<>();
            metrics.add(new TerminalAddonMetric("Entries", Integer.toString(total), "Starter Grimoire archive records", 0x7DE6D1));
            metrics.add(new TerminalAddonMetric("Locked", Long.toString(locked), "Discovery-gated or warning-gated records", 0xB78DFF));
            metrics.add(new TerminalAddonMetric("JEI", "Optional", "Never the source of truth", 0xFFD166));
            if (snapshot.available()) {
                metrics.add(new TerminalAddonMetric("ARCANA Scans", Integer.toString(snapshot.scanCount()),
                        "Live Field Journal scan count from ARCANA saved data", 0x7DE6D1));
                metrics.add(new TerminalAddonMetric("Pressure", snapshot.pressureSummary(),
                        "Live or last saved Veil/fracture field diagnostic", 0xC94CFF));
            }
            List<TerminalAddonSection> sections = new ArrayList<>();
            sections.add(new TerminalAddonSection("Role", List.of(
                    "Cross-addon lore and progression archive inside Terminal.",
                    "Complements Arcane Index and ARCANA Field Journal.")));
            sections.add(new TerminalAddonSection("Unlock Sources", List.of(
                    "Lens scan, Veil Lens scan, Field Journal research, MissionCore objectives.",
                    "Relic decode, ritual completion, curse events, familiar bonds, rift entry, boss gates.")));
            if (snapshot.available()) {
                sections.add(new TerminalAddonSection("Live Veilbound Bridge", List.of(
                        "Active research: " + (snapshot.activeResearch().isBlank() ? "none" : snapshot.activeResearch()) + ".",
                        "Unlocked research: " + snapshot.unlockedResearch().size() + "; observations: " + snapshot.observations().size() + ".",
                        "Endgame path: " + (snapshot.endgamePath().isBlank() ? "none" : snapshot.endgamePath()) + ".")));
            }
            return new TerminalAddonInfo(
                    "Terminal archive shell for Arcana Division lore, progression, and forbidden pages.",
                    List.copyOf(metrics),
                    List.copyOf(sections),
                    List.of(),
                    TerminalAddonGuide.optional(92, "Archive Shell",
                            "Open Terminal Archives and filter Arcana Grimoire records.",
                            List.of("Read Aether Signal", "Read Ritual Stability", "Review forbidden warnings")));
        }
    }
}
