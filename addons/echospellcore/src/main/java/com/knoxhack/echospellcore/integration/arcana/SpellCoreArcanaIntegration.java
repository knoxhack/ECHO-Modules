package com.knoxhack.echospellcore.integration.arcana;

import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echoarcanacore.api.ArcanaProviderInterfaces;
import com.knoxhack.echoarcanacore.api.CastType;
import com.knoxhack.echoarcanacore.api.SpellDefinition;
import com.knoxhack.echospellcore.EchoSpellCore;
import com.knoxhack.echospellcore.spell.StarterSpell;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public enum SpellCoreArcanaIntegration implements ArcanaProviderInterfaces.SpellProvider,
        ArcanaProviderInterfaces.ArcaneIndexProvider,
        ArcanaProviderInterfaces.GrimoireEntryProvider,
        ArcanaProviderInterfaces.ArcaneLensProvider,
        ArcanaProviderInterfaces.ArcaneMissionProvider,
        ArcanaProviderInterfaces.TerminalArcanaProvider {
    INSTANCE;

    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final List<SpellDefinition> SPELLS = StarterSpell.ordered().stream()
            .map(SpellCoreArcanaIntegration::spell)
            .toList();

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        ArcanaCoreServices.registerProvider(INSTANCE);
        EchoSpellCore.LOGGER.info("ECHO: SpellCore registered Arcana Core spell provider.");
    }

    @Override
    public Identifier id() {
        return EchoSpellCore.id("arcana_provider/spellcore");
    }

    @Override
    public List<SpellDefinition> spells() {
        return SPELLS;
    }

    @Override
    public List<Identifier> pageIds(Player player) {
        List<Identifier> pages = new ArrayList<>();
        pages.add(Identifier.fromNamespaceAndPath("echoarcaneindex", "spellcore/signal_focus"));
        pages.add(Identifier.fromNamespaceAndPath("echoarcaneindex", "spellcore/spell_deck"));
        pages.add(Identifier.fromNamespaceAndPath("echoarcaneindex", "spellcore/spell_modifiers"));
        StarterSpell.ordered().forEach(spell ->
                pages.add(Identifier.fromNamespaceAndPath("echoarcaneindex", "spellcore/" + spell.path())));
        return List.copyOf(pages);
    }

    @Override
    public List<Identifier> grimoireEntryIds(Player player) {
        List<Identifier> entries = new ArrayList<>();
        entries.add(Identifier.fromNamespaceAndPath("echogrimoire", "archive/spells/signal_focus"));
        entries.add(Identifier.fromNamespaceAndPath("echogrimoire", "archive/spells/spell_deck"));
        entries.add(Identifier.fromNamespaceAndPath("echogrimoire", "archive/spells/spell_modifiers"));
        StarterSpell.ordered().forEach(spell ->
                entries.add(Identifier.fromNamespaceAndPath("echogrimoire", "archive/spells/" + spell.path())));
        return List.copyOf(entries);
    }

    @Override
    public List<String> scanHints(Player player, Identifier targetId) {
        if (targetId == null || !EchoSpellCore.MODID.equals(targetId.getNamespace())) {
            return List.of();
        }
        if ("signal_focus".equals(targetId.getPath())) {
            return List.of("SpellCore focus", "Sneak-use to cycle the active Spell Deck slot.",
                    "Carry RitualCore's awakened_spell_core when RitualCore is loaded.");
        }
        if ("spell_deck".equals(targetId.getPath())) {
            return List.of("SpellCore loadout deck", "Stores six slots with three physical modifier sockets each.",
                    "Focus casts the active deck slot when a deck is carried.");
        }
        return List.of("Starter SpellCore spell", "Aether cost, cooldown, modifiers, and HUD state are server authoritative.");
    }

    @Override
    public List<Identifier> missionIds(Player player) {
        return List.of(
                EchoSpellCore.id("arcana_spellcore/craft_signal_focus"),
                EchoSpellCore.id("arcana_spellcore/carry_awakened_spell_core"),
                EchoSpellCore.id("arcana_spellcore/cast_signal_pulse"),
                EchoSpellCore.id("arcana_spellcore/cast_aether_bolt"),
                EchoSpellCore.id("arcana_spellcore/cast_ash_veil"),
                EchoSpellCore.id("arcana_spellcore/cast_void_step"),
                EchoSpellCore.id("arcana_spellcore/cast_storm_lance"),
                EchoSpellCore.id("arcana_spellcore/cast_crystal_wall"),
                EchoSpellCore.id("arcana_spellcore/cast_blood_surge"),
                EchoSpellCore.id("arcana_spellcore/cast_rift_blink"),
                EchoSpellCore.id("arcana_spellcore/cast_soul_thread"),
                EchoSpellCore.id("arcana_spellcore/cast_decay_touch"),
                EchoSpellCore.id("arcana_spellcore/cast_veil_trace"),
                EchoSpellCore.id("arcana_spellcore/cast_fracture_shear"),
                EchoSpellCore.id("arcana_spellcore/configure_spell_deck"),
                EchoSpellCore.id("arcana_spellcore/fire_spell_projectile"),
                EchoSpellCore.id("arcana_spellcore/install_spell_modifier"),
                EchoSpellCore.id("arcana_spellcore/manage_cooldown"));
    }

    @Override
    public Map<String, String> terminalSummary(Player player) {
        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("module", "ECHO: SpellCore");
        summary.put("starter_spells", "24 Signal/Aether/Ash/Void/Storm/Crystal/Blood/Rift/Soul/Decay/Veil/Fracture spells");
        summary.put("focus", "signal_focus + socketed spell_deck");
        summary.put("unlock", "echoritualcore:awakened_spell_core");
        summary.put("aether", "costs_cooldowns_sockets_feedback_active");
        return Map.copyOf(summary);
    }

    private static SpellDefinition spell(StarterSpell spell) {
        return new SpellDefinition(
                spell.id(),
                spell.translationKey(),
                spell.school(),
                EchoSpellCore.id("textures/gui/icons/" + spell.path() + ".png"),
                Identifier.fromNamespaceAndPath("echoritualcore", "spell_core_awakening"),
                spell.cost(),
                spell.cooldownTicks(),
                0,
                0,
                spell.range(),
                spell.targetingMode(),
                CastType.INSTANT,
                EchoSpellCore.id("effect/" + spell.path()),
                3,
                spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.ASH
                        || spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.CRYSTAL
                        || spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.SOUL
                        || spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.VEIL ? 0.02D : 0.0D,
                spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.VOID
                        || spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.RIFT
                        || spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.BLOOD
                        || spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.DECAY
                        || spell.school() == com.knoxhack.echoarcanacore.api.SpellSchool.FRACTURE ? 0.05D
                        : spell == StarterSpell.SIGNAL_PULSE ? 0.02D : 0.0D,
                EchoSpellCore.id("visual/" + spell.path()),
                EchoSpellCore.id("sound/" + spell.path()),
                Identifier.fromNamespaceAndPath("echoarcaneindex", "spellcore/" + spell.path()),
                Identifier.fromNamespaceAndPath("echogrimoire", "archive/spells/" + spell.path()),
                List.of("starter", spell.school().name().toLowerCase(java.util.Locale.ROOT), "signal_focus"));
    }
}
