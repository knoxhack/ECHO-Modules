package com.knoxhack.echospellcore.integration.terminal;

import com.knoxhack.echospellcore.registry.ModItems;
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

public final class SpellCoreTerminalIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int ACCENT = 0xFF46E7FF;

    private SpellCoreTerminalIntegration() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            TerminalAddonInfoRegistry.register(new SpellCoreInfoProvider());
        }
    }

    private static final class SpellCoreInfoProvider implements TerminalAddonInfoProvider {
        @Override
        public String chapterId() {
            return "spellcore";
        }

        @Override
        public TerminalAddonInfo info(Player player) {
            return new TerminalAddonInfo(
                    "ECHO: SpellCore - Signal Focus casting, socketed Spell Deck loadouts, school projectiles, Aether costs, cooldowns, and curse feedback.",
                    List.of(
                            new TerminalAddonMetric("Starter Spells", "24", "All Arcana schools", ACCENT),
                            new TerminalAddonMetric("Loadout Slots", "6", "Spell Deck", ACCENT),
                            new TerminalAddonMetric("Sockets", "3/slot", "Range/Efficiency/Overcharge", ACCENT),
                            new TerminalAddonMetric("Unlock", "awakened_spell_core", "RitualCore", ACCENT)),
                    List.of(
                            new TerminalAddonSection("Playable Now", List.of(
                                    "Signal Pulse, Echo Mark, and Static Burst run first Signal-school field operations.",
                                    "Aether Bolt, Dust Lance, Null Bolt, and Storm Lance spawn synchronized spell projectiles.",
                                    "Aether Shield, Arcane Lift, Ash Veil, and Cinder Skin cover utility/defense casting.",
                                    "Void Step, Hollow Cage, Static Dash, Thunder Cage, Crystal Wall, Shard Burst, and Resonant Armor broaden the school matrix.",
                                    "Blood Surge, Rift Blink, Soul Thread, Decay Touch, Veil Trace, and Fracture Shear cover the riskier school endpoints.",
                                    "Spell Deck stores active slots and socketed modifier state for the Signal Focus.")),
                            new TerminalAddonSection("Casting Rules", List.of(
                                    "Sneak-use Signal Focus to cycle Spell Deck slots when a deck is carried.",
                                    "Use Spell Deck to select slot spells and install modifiers into three sockets.",
                                    "Use Signal Focus to cast the selected spell.",
                                    "Aether cost and cooldown checks run on the server.",
                                    "Overcharge consumes two sockets and can increase curse/corruption backlash.",
                                    "When RitualCore is present, carry Awakened Spell Core to authorize casting."))),
                    List.of(new TerminalAddonLink(
                            Identifier.fromNamespaceAndPath("echospellcore", "terminal/spellcore"),
                            "SpellCore",
                            "Focus casting records",
                            ACCENT)),
                    TerminalAddonGuide.optional(80, "Arcana route",
                            "Awaken a spell core through RitualCore, carry a Spell Deck, configure a loadout, and cast starter spells.",
                            List.of(
                                    "Run RitualCore's Spell Core Awakening to get awakened_spell_core.",
                                    "Craft or obtain Signal Focus and Spell Deck.",
                                    "Open Spell Deck to assign all Arcana school starter spells to six slots.",
                                    "Install Range, Efficiency, or Overcharge modifiers into socket capacity.",
                                    "Watch the HUD for active slot, sockets, Aether, cooldown, and feedback risk.")));
        }
    }
}
