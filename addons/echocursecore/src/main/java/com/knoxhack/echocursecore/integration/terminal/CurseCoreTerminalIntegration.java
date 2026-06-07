package com.knoxhack.echocursecore.integration.terminal;

import com.knoxhack.echocursecore.api.CurseCoreApi;
import com.knoxhack.echocursecore.registry.ModItems;
import com.knoxhack.echoterminal.api.TerminalAddonGuide;
import com.knoxhack.echoterminal.api.TerminalAddonInfo;
import com.knoxhack.echoterminal.api.TerminalAddonInfoProvider;
import com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonLink;
import com.knoxhack.echoterminal.api.TerminalAddonMetric;
import com.knoxhack.echoterminal.api.TerminalAddonSection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class CurseCoreTerminalIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int ACCENT = 0xFFE05A7A;

    private CurseCoreTerminalIntegration() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            TerminalAddonInfoRegistry.register(new CurseCoreInfoProvider());
        }
    }

    private static final class CurseCoreInfoProvider implements TerminalAddonInfoProvider {
        @Override
        public String chapterId() {
            return "cursecore";
        }

        @Override
        public TerminalAddonInfo info(Player player) {
            int active = CurseCoreApi.activeCurses(player).size();
            return new TerminalAddonInfo(
                    "ECHO: CurseCore - persistent curse signatures, symptoms, and cleansing hooks.",
                    List.of(
                            new TerminalAddonMetric("Active Curses", Integer.toString(active), CurseCoreApi.summary(player), ACCENT),
                            new TerminalAddonMetric("Live Targets", "2", "Echo Rot / Glass Veins", ACCENT),
                            new TerminalAddonMetric("Cleansing", "RitualCore", "Curse Cleansing I", ACCENT)),
                    List.of(
                            new TerminalAddonSection("Starter Curses", List.of(
                                    "Echo Rot - signal/mind curse from sample or spell backlash",
                                    "Glass Veins - body/crystal curse record for future spell tradeoffs")),
                            new TerminalAddonSection("Cleansing Bridge", List.of(
                                    "Use Echo Rot Sample to create a live curse target.",
                                    "Sneak-use RitualCore Basic Altar with Purity Catalyst to reduce a curse stage.",
                                    "MissionCore records both curse gained and curse cleansed objectives."))),
                    List.of(new TerminalAddonLink(
                            Identifier.fromNamespaceAndPath("echocursecore", "terminal/cursecore"),
                            "CurseCore",
                            "Curse diagnostics",
                            ACCENT)),
                    TerminalAddonGuide.optional(90, "Arcana route",
                            "Apply a controlled Echo Rot sample, then reduce it through RitualCore Curse Cleansing I.",
                            List.of(
                                    "Carry or use Echo Rot Sample to gain a visible stage-I curse.",
                                    "Build a complete RitualCore Basic Altar array.",
                                    "Provide Purity Catalyst and sneak-use the altar to cleanse.",
                                    "Confirm Terminal or Lens reports no active curse signatures.")));
        }
    }
}
