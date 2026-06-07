package com.knoxhack.echogrimoire.integration;

import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echoarcanacore.api.ArcaneRelicDefinition;
import com.knoxhack.echoarcanacore.api.RitualDefinition;
import com.knoxhack.echoarcanacore.integration.veilbound.VeilboundBridgeCatalog;
import com.knoxhack.echogrimoire.EchoGrimoire;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class GrimoireEntries {
    private GrimoireEntries() {
    }

    public record Entry(Identifier id, String group, String title, String status, List<String> lines, boolean locked) {
    }

    public static List<Entry> allEntries() {
        List<Entry> entries = new ArrayList<>();
        entries.addAll(starterEntries());
        entries.addAll(relicEntries());
        entries.addAll(ritualEntries());
        entries.addAll(spellEntries());
        entries.addAll(curseEntries());
        entries.addAll(veilboundBridgeEntries());
        return List.copyOf(entries);
    }

    public static List<Entry> starterEntries() {
        return List.of(
                entry("what_is_aether_signal", "Overview", "What Is Aether Signal?", "UNLOCKED", false,
                        "Aether is not a blue bar. It is a signal layer hidden under matter.",
                        "Arcana Core tracks type, storage, transfer, contamination, corruption, and operator limits."),
                entry("what_is_resonance", "Overview", "What Is Resonance?", "UNLOCKED", false,
                        "Resonance is the readable pressure pattern left by reality systems under load.",
                        "Veilbound resonance remains ARCANA-owned and bridges into ECHO through stable IDs."),
                entry("the_veil", "Veilbound Studies", "The Veil", "LOCKED", true,
                        "Field Journal access required.",
                        "The Veil is not a dimension in the ordinary sense. It is a permissions layer."),
                entry("fracture_pressure", "Veilbound Studies", "Fracture Pressure", "LOCKED", true,
                        "High pressure corrupts local rules before it breaks blocks.",
                        "Arcana Division treats pressure as map, mission, ritual, and archive data."),
                entry("unknown_relics", "Relics", "Unknown Relics", "UNLOCKED", false,
                        "Unknown relics are recovered first, interpreted second, stabilized third.",
                        "Do not make JEI the explanation for relic lifecycle or risk."),
                entry("ritual_stability", "Rituals", "Ritual Stability", "UNLOCKED", false,
                        "Rituals are engineered circuits with a center, pattern, inputs, conditions, and failure tables.",
                        "A clean Index page should explain why a ritual failed without eating the player's items."),
                entry("spell_schools", "Spells", "Spell Schools", "UNLOCKED", false,
                        "Signal, Void, Ash, Blood, Aether, Storm, Rift, Soul, Crystal, Decay, Veil, and Fracture are operator lenses.",
                        "The spell system should feel like hacking reality, not generic wand spam."),
                entry("curse_stages", "Curses", "Curse Stages", "UNLOCKED", false,
                        "Curses begin as symptoms, become discoveries, and then become choices.",
                        "Cleansing, contracts, and controlled consequences all need official knowledge pages."),
                entry("familiar_bonds", "Familiars", "Familiar Bonds", "PLANNED", false,
                        "Familiars are companions, tools, and sometimes warnings.",
                        "Bond state will bridge ritual, spell, curse, and Terminal status surfaces."),
                entry("rift_cracks", "Rifts", "Rift Cracks", "PLANNED", false,
                        "Small rifts are map markers, scan targets, hazards, and story hooks.",
                        "RiftWorlds should begin with contained encounters before full dimensional escalation."),
                entry("field_journal", "Veilbound Studies", "The Field Journal", "LOCKED", true,
                        "The Field Journal is Veilbound-specific and remains separate from the Grimoire.",
                        "Grimoire summarizes cross-addon context; it does not replace ARCANA's research UI."),
                entry("veil_lens", "Veilbound Studies", "The Veil Lens", "LOCKED", true,
                        "The Veil Lens owns Veilbound observation flow.",
                        "ECHO Lens can mirror discoveries without duplicate scan spam."),
                entry("deep_veil_gate", "Boss Gates", "The Deep Veil Gate", "FORBIDDEN", true,
                        "Gate records require Field Journal progression.",
                        "No player should reach this route through hidden impossible progression."),
                entry("fracture_heart", "Endgame Paths", "The Fracture Heart", "FORBIDDEN", true,
                        "The Fracture Heart path is Veilbound-owned endgame content.",
                        "Arcana Division may reference the path, but must not swallow the addon identity."),
                entry("forbidden_blood_circuit", "Forbidden Pages", "Forbidden Page: Blood Circuit", "WARNING", true,
                        "Full read may increase corruption or invoke CurseCore when present.",
                        "The safe read path should always exist."),
                entry("forbidden_void_mark", "Forbidden Pages", "Forbidden Page: Void Mark", "WARNING", true,
                        "Void Mark is a consequence, not a flavor label.",
                        "Index and Grimoire must warn before irreversible or semi-permanent states."),
                entry("forbidden_unwritten_one", "Forbidden Pages", "Forbidden Page: The Unwritten One", "WARNING", true,
                        "Some archive pages are not meant to be trusted.",
                        "The warning screen is gameplay, not decoration."));
    }

    public static List<Entry> veilboundBridgeEntries() {
        List<Entry> entries = new ArrayList<>();
        entries.add(entry("veilbound/field_journal_summary", "Veilbound Studies", "Field Journal Summary", "BRIDGE", true,
                "The Grimoire summarizes Field Journal state for the Arcana Division.",
                "ARCANA still owns detailed observations, theories, tracked research, and campaign UI."));
        entries.add(entry("veilbound/active_research", "Veilbound Studies", "Active Research", "BRIDGE", true,
                "Active Field Journal research is mirrored into Terminal through the optional runtime bridge.",
                "ARCANA still owns claiming, tracking, and research UI behavior."));
        entries.add(entry("veilbound/tracked_research", "Veilbound Studies", "Tracked Research", "BRIDGE", true,
                "Tracked research is a player-facing priority list, not a duplicate research graph.",
                "MissionCore and Arcane Index can point to the same IDs so progression stays explainable."));
        entries.add(entry("veilbound/recent_observations", "Field Notes", "Recent Observations", "BRIDGE", true,
                "Veil Lens and ECHO Lens scans should converge on one discovery trail.",
                "Duplicate scan spam is avoided by using shared bridge IDs and discovery hooks."));
        entries.add(entry("veilbound/pressure_diagnostics", "Field Notes", "Pressure Diagnostics", "LOCKED", true,
                "Veil pressure describes readable system load; fracture pressure describes unsafe rewrite pressure.",
                "The runtime bridge reads last player diagnostics and live server chunk pressure when ARCANA is present."));
        entries.add(entry("veilbound/fracture_diagnostics", "Field Notes", "Fracture Diagnostics", "LOCKED", true,
                "Fracture rifts, seals, detectors, and transformed terrain are surfaced as connected archive records.",
                "HoloMap markers use exact ECHO Lens scan positions, exact persisted ARCANA Veil Lens scan positions, and ARCANA discovery state otherwise."));
        entries.add(entry("veilbound/endgame_path_status", "Endgame Paths", "Endgame Path Status", "FORBIDDEN", true,
                "Seal, harmonize, exploit, or architect choices belong to ARCANA's Field Journal progression.",
                "The Grimoire can warn and summarize, but it must not silently select or bypass a path."));
        entries.add(entry("veilbound/boss_gate_status", "Boss Gates", "Boss Gate Status", "FORBIDDEN", true,
                "Deep Veil Gate, Guardian, Unwritten One, and Fracture Heart gates are warning-gated records.",
                "MissionCore route V8 and V9 reference them without hiding requirements."));
        entries.add(entry("veilbound/known_landmarks", "Landmarks", "Known Landmarks", "BRIDGE", true,
                "Nine Veilbound landmarks are mirrored into HoloMap and Arcane Index.",
                "Known ARCANA discoveries unlock markers; ECHO Lens and persisted Veil Lens scans add exact marker coordinates."));
        entries.add(entry("veilbound/known_entities", "Arcane Mobs", "Known Entities", "BRIDGE", true,
                "Nine Veilbound entities are mirrored into Lens, Index, MissionCore, and Grimoire records.",
                "Sigil Construct keeps its own Veilbound identity rather than becoming a generic familiar."));
        entries.add(entry("veilbound/forbidden_studies", "Forbidden Pages", "Forbidden Studies", "WARNING", true,
                "Forbidden pages must warn before corruption, curses, boss gates, or irreversible choices.",
                "Safe read paths remain mandatory even when CurseCore is absent."));

        for (VeilboundBridgeCatalog.Entry entry : VeilboundBridgeCatalog.allEntries()) {
            if (entry.kind() == VeilboundBridgeCatalog.Kind.RESEARCH
                    || entry.kind() == VeilboundBridgeCatalog.Kind.LANDMARK
                    || entry.kind() == VeilboundBridgeCatalog.Kind.ENTITY
                    || entry.kind() == VeilboundBridgeCatalog.Kind.BOSS_GATE) {
                entries.add(catalogEntry(entry));
            }
        }
        return List.copyOf(entries);
    }

    public static List<Entry> relicEntries() {
        List<Entry> entries = new ArrayList<>();
        List<ArcaneRelicDefinition> relics = ArcanaCoreServices.relics();
        if (relics.isEmpty()) {
            return relicStarterFallbackEntries();
        }
        for (ArcaneRelicDefinition relic : relics) {
            String path = "relics/" + relic.id().getPath().replace('/', '_');
            String status = switch (relic.lifecycle()) {
                case UNKNOWN -> "UNKNOWN";
                case FORBIDDEN -> "WARNING";
                case CORRUPTED -> "CORRUPTED";
                default -> "DECODED";
            };
            boolean locked = relic.lifecycle() == com.knoxhack.echoarcanacore.api.RelicLifecycle.UNKNOWN
                    || relic.lifecycle() == com.knoxhack.echoarcanacore.api.RelicLifecycle.FORBIDDEN;
            entries.add(entry(path, "Relics", title(relic.id().getPath()), status, locked,
                    "Category: " + relic.category() + "; lifecycle: " + relic.lifecycle().name().toLowerCase() + ".",
                    "Instability " + relic.instability() + ", curse risk " + relic.curseRisk()
                            + ", upgrade slots " + relic.upgradeSlots() + ".",
                    "Arcane Index page: " + relic.indexPageId() + "."));
        }
        return List.copyOf(entries);
    }

    public static List<Entry> ritualEntries() {
        List<Entry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (RitualDefinition ritual : ArcanaCoreServices.rituals()) {
            if (!"echoritualcore".equals(ritual.id().getNamespace())) {
                continue;
            }
            String path = ritual.id().getPath().replace('/', '_');
            seen.add(path);
            entries.add(entry("rituals/" + path, "Rituals", title(path), ritualStatus(path), ritualLocked(path),
                    "Family: " + ritual.family().name().toLowerCase() + "; required Aether: " + ritual.requiredAether() + ".",
                    "Center: " + ritual.centerBlock() + "; required items: " + ritual.requiredItems() + ".",
                    ritualLine(path)));
        }
        for (String path : List.of("aether_calibration", "relic_stabilization", "curse_cleansing_i",
                "spell_core_awakening", "rift_crack_reveal")) {
            if (seen.contains(path)) {
                continue;
            }
            entries.add(entry("rituals/" + path, "Rituals", title(path), ritualStatus(path), ritualLocked(path),
                    fallbackRitualLine(path),
                    "Arcane Index page: echoarcaneindex:ritualcore/" + path + ".",
                    "RitualCore owns shared ritual diagnostics; JEI is optional compatibility only."));
        }
        entries.add(entry("rituals/basic_altar_field_note", "Field Notes", "Basic Altar Field Note", "ACTIVE", false,
                "Basic Altar is the first playable RitualCore center.",
                "Normal use attempts Relic Stabilization. Sneak-use attempts Curse Cleansing I.",
                "Invalid setup checks explain missing inputs before consuming catalysts."));
        return List.copyOf(entries);
    }

    public static List<Entry> spellEntries() {
        return List.of(
                entry("spells/signal_focus", "Spells", "Signal Focus", "ACTIVE", false,
                        "The Signal Focus is the first SpellCore casting surface.",
                        "Normal use casts the active function; sneak-use cycles Spell Deck slots when a deck is carried.",
                        "When RitualCore is present, awakened_spell_core is the first authorization token."),
                entry("spells/spell_deck", "Spells", "Spell Deck", "ACTIVE", false,
                        "The Spell Deck is a loadout shell for the Signal Focus.",
                        "Six slots can hold starter Signal, Aether, Ash, Void, Storm, Crystal, Blood, Rift, Soul, and Decay spells. Each slot has three item-backed modifier sockets."),
                entry("spells/spell_modifiers", "Spells", "Spell Modifiers", "ACTIVE", false,
                        "Range, Efficiency, and Overcharge form the first socketed modifier bus.",
                        "Overcharge consumes two sockets and is powerful, but feeds curse and Aether contamination feedback."),
                entry("spells/signal_pulse", "Spells", "Signal Pulse", "ACTIVE", false,
                        "Signal Pulse is a cone scan-disrupt spell.",
                        "It costs Signal Aether, marks living signatures, and teaches that spells are signal tools before they are weapons."),
                entry("spells/echo_mark", "Spells", "Echo Mark", "ACTIVE", false,
                        "Echo Mark is a single-target signal tag.",
                        "It marks and weakens the looked-at entity so operators can turn scan discipline into tactical focus."),
                entry("spells/static_burst", "Spells", "Static Burst", "ACTIVE", false,
                        "Static Burst is a close signal disruption pulse.",
                        "It slows and weakens nearby hostile signatures without becoming generic elemental spectacle."),
                entry("spells/aether_bolt", "Spells", "Aether Bolt", "ACTIVE", false,
                        "Aether Bolt is the first synchronized SpellCore projectile.",
                        "It spends raw Aether, spawns a server-owned projectile, and starts a server-side cooldown."),
                entry("spells/aether_shield", "Spells", "Aether Shield", "ACTIVE", false,
                        "Aether Shield turns raw Aether into a short ward.",
                        "It grants absorption and resistance, making early casting useful when a fight goes sideways."),
                entry("spells/arcane_lift", "Spells", "Arcane Lift", "ACTIVE", false,
                        "Arcane Lift is a field-operator movement tool.",
                        "It levitates a looked-at target or stabilizes the operator's descent when no target resolves."),
                entry("spells/ash_veil", "Spells", "Ash Veil", "ACTIVE", false,
                        "Ash Veil hides the operator inside ash signal noise.",
                        "It grants brief invisibility and resistance while the HUD reports cooldown and Aether state."),
                entry("spells/dust_lance", "Spells", "Dust Lance", "ACTIVE", false,
                        "Dust Lance is an Ash-school projectile with harsh signal grit.",
                        "It weakens and slows targets on hit, pairing projectile sync with debuff utility."),
                entry("spells/cinder_skin", "Spells", "Cinder Skin", "ACTIVE", false,
                        "Cinder Skin is the first Ash defensive skin.",
                        "It grants fire resistance and resistance, with Overcharge increasing output and risk."),
                entry("spells/void_step", "Spells", "Void Step", "ACTIVE", false,
                        "Void Step is the first Void-school movement spell.",
                        "It spends Rift Aether, searches for a safe exit vector, and leaves portal feedback at both ends."),
                entry("spells/null_bolt", "Spells", "Null Bolt", "ACTIVE", false,
                        "Null Bolt is a Void projectile.",
                        "It weakens and fatigues targets, adding hostile-system shutdown to the synchronized projectile layer."),
                entry("spells/hollow_cage", "Spells", "Hollow Cage", "ACTIVE", false,
                        "Hollow Cage is Void containment.",
                        "It constrains signatures near a target or forward anchor with slowness, weakness, and mining fatigue."),
                entry("spells/storm_lance", "Spells", "Storm Lance", "ACTIVE", false,
                        "Storm Lance is a faster projectile channel.",
                        "It uses Signal Aether to carry electric disruption and visible client/server spark trails."),
                entry("spells/static_dash", "Spells", "Static Dash", "ACTIVE", false,
                        "Static Dash is operator repositioning.",
                        "It pushes the caster forward and adds speed, turning Storm into field mobility instead of simple lightning damage."),
                entry("spells/thunder_cage", "Spells", "Thunder Cage", "ACTIVE", false,
                        "Thunder Cage is a Storm control field.",
                        "It stuns and lightly damages nearby hostile signatures around a target point."),
                entry("spells/crystal_wall", "Spells", "Crystal Wall", "ACTIVE", false,
                        "Crystal Wall is a defensive projection.",
                        "It spends Refined Aether for absorption, resistance, and crystal-style feedback particles."),
                entry("spells/shard_burst", "Spells", "Shard Burst", "ACTIVE", false,
                        "Shard Burst is a Crystal cone attack.",
                        "It damages and reveals targets in front of the operator without creating permanent blocks yet."),
                entry("spells/resonant_armor", "Spells", "Resonant Armor", "ACTIVE", false,
                        "Resonant Armor is a Crystal ward weave.",
                        "It layers resistance and absorption while surfacing feedback risk through the SpellCore HUD."),
                entry("spells/blood_surge", "Spells", "Blood Surge", "ACTIVE", false,
                        "Blood Surge is a forbidden body overclock.",
                        "It trades health and Cursed Aether for strength and speed, making the curse cost visible instead of hiding it in flavor text."),
                entry("spells/rift_blink", "Spells", "Rift Blink", "ACTIVE", false,
                        "Rift Blink is the fracture-aware movement variant.",
                        "It reuses safe blink discipline but emits stronger rift feedback and spends Rift Aether."),
                entry("spells/soul_thread", "Spells", "Soul Thread", "ACTIVE", false,
                        "Soul Thread is support magic shaped as a diagnostic tether.",
                        "It heals allies and burdens hostile targets, establishing Soul as field repair and containment."),
                entry("spells/decay_touch", "Spells", "Decay Touch", "ACTIVE", false,
                        "Decay Touch is close-range cursed degradation.",
                        "It uses Cursed Aether to apply damage, poison, and weakness as the first Decay school combat slice."),
                entry("spells/veil_trace", "Spells", "Veil Trace", "ACTIVE", false,
                        "Veil Trace is diagnostic casting for hidden signal work.",
                        "It spends Veil Resonance to expose a target or place a short-lived scan mark, tying SpellCore back into Veilbound field methods."),
                entry("spells/fracture_shear", "Spells", "Fracture Shear", "ACTIVE", false,
                        "Fracture Shear is a dangerous projectile pattern.",
                        "It uses Fracture Energy, the synchronized projectile entity, and client prediction to make fracture magic feel unstable but readable."),
                entry("spells/aether_cost_cooldown", "Field Notes", "Spell Aether And Cooldown", "ACTIVE", false,
                        "SpellCore mirrors active spell, deck slot, socketed modifiers, Aether, cooldown, contamination, curse risk, and status into the focus stack for HUD and tooltip display.",
                        "The server owns casting, cost, cooldown, projectile spawn, and backlash decisions."));
    }

    public static List<Entry> curseEntries() {
        return List.of(
                entry("curses/echo_rot", "Curses", "Echo Rot", "ACTIVE", false,
                        "Echo Rot is a live signal/mind curse target.",
                        "It can come from Echo Rot Sample or SpellCore signal backlash, persists on the player, and appears in Lens/Terminal diagnostics."),
                entry("curses/glass_veins", "Curses", "Glass Veins", "ACTIVE", false,
                        "Glass Veins is registered now as the first body/crystal curse record.",
                        "Future Crystal school work can build on the same stable CurseCore ID."),
                entry("curses/echo_rot_sample", "Curses", "Echo Rot Sample", "ACTIVE", false,
                        "Echo Rot Sample creates a controlled stage-I curse for testing and early play.",
                        "It exists so Curse Cleansing I has a real live target before full cursed mobs and rift events land."),
                entry("curses/curse_cleansing_bridge", "Rituals", "Curse Cleansing Bridge", "ACTIVE", false,
                        "RitualCore calls CurseCore through an optional reflective bridge.",
                        "A complete Basic Altar with Purity Catalyst reduces one active player curse stage before consuming the catalyst."));
    }

    private static List<Entry> relicStarterFallbackEntries() {
        return List.of(
                relicFallback("phase_anchor", "DECODED", false, "Short blink recall prototype with destination drift risk."),
                relicFallback("echo_mirror", "WARNING", true, "Forbidden decoy mirror that can reflect hostile attention."),
                relicFallback("gravity_clamp", "DECODED", false, "Prototype motion clamp for push, pull, and gravity well handling."),
                relicFallback("rift_lantern", "DECODED", false, "Rift-light device for rune, trace, and hostile signature reveal."),
                relicFallback("blood_circuit", "WARNING", true, "Forbidden health-to-power circuit with Blood Debt risk."),
                relicFallback("broken_climate_key", "SCANNED", false, "Broken storm-control key with weather backlash risk."),
                relicFallback("soul_capacitor", "DECODED", false, "Soul-aether battery and warding relic."),
                relicFallback("void_compass", "DECODED", false, "Dimensional compass that can reveal relic vault coordinates."));
    }

    private static Entry relicFallback(String path, String status, boolean locked, String summary) {
        return entry("relics/" + path, "Relics", title(path), status, locked,
                summary,
                "Arcane Index page: echoarcaneindex:relic/" + path + ".",
                "RelicTech owns gameplay behavior and Arcana Core owns the shared registry bridge.");
    }

    private static String ritualStatus(String path) {
        return switch (path) {
            case "aether_calibration", "relic_stabilization", "curse_cleansing_i",
                    "spell_core_awakening", "rift_crack_reveal" -> "ACTIVE";
            default -> "PLANNED";
        };
    }

    private static boolean ritualLocked(String path) {
        return false;
    }

    private static String ritualLine(String path) {
        return switch (path) {
            case "relic_stabilization" -> "Consumes RelicTech lifecycle/risk data and Stability Seal to stabilize damaged or corrupted relics.";
            case "curse_cleansing_i" -> "Consumes Purity Catalyst and RelicTech corruption flags to cleanse relic-bound curse signatures.";
            case "aether_calibration" -> "Consumes Aether Chalk in a complete basic array and returns Refined Aether Sample.";
            case "spell_core_awakening" -> "Consumes Ritual Focus and Refined Aether Sample to create an Awakened Spell Core shell.";
            case "rift_crack_reveal" -> "Consumes Refined Aether Sample and Aether Chalk to generate an imprecise HoloMap rift trace.";
            default -> "RitualCore registry-backed archive entry.";
        };
    }

    private static String fallbackRitualLine(String path) {
        return switch (path) {
            case "relic_stabilization" -> "Relic Stabilization is active when RitualCore and RelicTech are both loaded.";
            case "curse_cleansing_i" -> "Curse Cleansing I is active for relic-bound corruption before full CurseCore lands.";
            case "aether_calibration" -> "Aether Calibration is the first active non-RelicTech ritual.";
            case "spell_core_awakening" -> "Spell Core Awakening creates a RitualCore-owned spell core shell until SpellCore lands.";
            case "rift_crack_reveal" -> "Rift Crack Reveal writes imprecise HoloMap marker data for future RiftWorlds content.";
            default -> "RitualCore planned bridge entry.";
        };
    }

    private static Entry entry(String path, String group, String title, String status, boolean locked, String... lines) {
        return new Entry(EchoGrimoire.id("archive/" + path), group, title, status, List.of(lines), locked);
    }

    private static Entry catalogEntry(VeilboundBridgeCatalog.Entry catalog) {
        String path = "veilbound/" + VeilboundBridgeCatalog.kindPath(catalog.kind()) + "/" + VeilboundBridgeCatalog.entryPath(catalog);
        return entry(path, group(catalog.kind()), catalog.title(), status(catalog.kind()), locked(catalog.kind()),
                catalog.summary(),
                "Arcane Index page: echoarcaneindex:" + VeilboundBridgeCatalog.indexPagePath(catalog) + ".",
                "ARCANA registry id: " + catalog.id() + ".");
    }

    private static String group(VeilboundBridgeCatalog.Kind kind) {
        return switch (kind) {
            case RESEARCH -> "Veilbound Research";
            case LANDMARK -> "Landmarks";
            case ENTITY -> "Arcane Mobs";
            case BOSS_GATE -> "Boss Gates";
            default -> "Veilbound Studies";
        };
    }

    private static String status(VeilboundBridgeCatalog.Kind kind) {
        return switch (kind) {
            case BOSS_GATE -> "FORBIDDEN";
            case LANDMARK, ENTITY -> "SCANNED";
            case RESEARCH -> "LOCKED";
            default -> "BRIDGE";
        };
    }

    private static boolean locked(VeilboundBridgeCatalog.Kind kind) {
        return kind == VeilboundBridgeCatalog.Kind.RESEARCH || kind == VeilboundBridgeCatalog.Kind.BOSS_GATE;
    }

    private static String title(String raw) {
        String text = raw == null ? "" : raw.replace('/', ' ').replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
        StringBuilder builder = new StringBuilder(text.length());
        boolean nextUpper = true;
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                builder.append(c);
                nextUpper = true;
            } else if (nextUpper) {
                builder.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
