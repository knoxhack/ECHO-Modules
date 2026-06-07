package com.knoxhack.echoritualcore.integration.terminal;

import com.knoxhack.echoritualcore.registry.ModBlocks;
import com.knoxhack.echoterminal.api.TerminalAddonGuide;
import com.knoxhack.echoterminal.api.TerminalAddonInfo;
import com.knoxhack.echoterminal.api.TerminalAddonInfoProvider;
import com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonLink;
import com.knoxhack.echoterminal.api.TerminalAddonMetric;
import com.knoxhack.echoterminal.api.TerminalAddonSection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.world.entity.player.Player;

public final class RitualCoreTerminalIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int ACCENT = 0xFFB072FF;

    private RitualCoreTerminalIntegration() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            TerminalAddonInfoRegistry.register(new RitualCoreInfoProvider());
        }
    }

    private static final class RitualCoreInfoProvider implements TerminalAddonInfoProvider {
        @Override
        public String chapterId() {
            return "ritualcore";
        }

        @Override
        public TerminalAddonInfo info(Player player) {
            return new TerminalAddonInfo(
                    "ECHO: RitualCore - shared ritual circuits for relic stabilization, cleansing, and future spell/rift/familiar systems.",
                    List.of(
                            new TerminalAddonMetric("Registered Rituals", "5", "Arcana Core", ACCENT),
                            new TerminalAddonMetric("Playable Now", "5", "Basic Altar", ACCENT),
                            new TerminalAddonMetric("Array", "4 runes + pedestal", "Field circuit", ACCENT)),
                    List.of(
                            new TerminalAddonSection("Active Rituals", List.of(
                                    "Aether Calibration - complete a basic array and condense Refined Aether Sample",
                                    "Relic Stabilization - hold an identified damaged relic; consumes Stability Seal",
                                    "Curse Cleansing I - sneak-use with corrupted relic; consumes Purity Catalyst",
                                    "Spell Core Awakening - consumes Ritual Focus plus Refined Aether Sample",
                                    "Rift Crack Reveal - creates an imprecise HoloMap rift trace")),
                            new TerminalAddonSection("Circuit Blocks", List.of(
                                    "Basic Altar - early ritual center",
                                    "Rune Circle - array guide block",
                                    "Offering Pedestal - one-slot ritual input node",
                                    "Stability Pylon - stability support",
                                    "Corrupted Altar - forbidden ritual placeholder"))),
                    List.of(new TerminalAddonLink(
                            net.minecraft.resources.Identifier.fromNamespaceAndPath("echoritualcore", "terminal/ritualcore"),
                            "RitualCore",
                            "Ritual engineering records",
                            ACCENT)),
                    TerminalAddonGuide.optional(70, "Arcana route",
                            "Build the Basic Altar after RelicTech discovery, then stabilize risky devices before broader ritual engineering.",
                            List.of(
                                    "Build a Basic Altar with four Rune Circles and at least one Offering Pedestal nearby.",
                                    "Use Aether Chalk to run Aether Calibration before advanced work.",
                                    "Hold a damaged RelicTech relic and use the altar with a Stability Seal available.",
                                    "Sneak-use the altar with a corrupted relic and Purity Catalyst to cleanse active corruption.")));
        }
    }
}
