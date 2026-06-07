package com.knoxhack.echoarcaneindex.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.index.IIndexContentProvider;
import com.knoxhack.echocore.api.index.IIndexRegistry;
import com.knoxhack.echocore.api.index.IndexBuildContext;
import com.knoxhack.echocore.api.index.IndexCategory;
import com.knoxhack.echocore.api.index.IndexContentBuilder;
import com.knoxhack.echocore.api.index.IndexContentSnapshot;
import com.knoxhack.echocore.api.index.IndexEntry;
import com.knoxhack.echocore.api.index.IndexEntryState;
import com.knoxhack.echocore.api.index.IndexRelation;
import com.knoxhack.echocore.api.index.IndexVisibility;
import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echoarcanacore.api.ArcaneRelicDefinition;
import com.knoxhack.echoarcanacore.api.CurseDefinition;
import com.knoxhack.echoarcanacore.api.RitualDefinition;
import com.knoxhack.echoarcanacore.api.SpellDefinition;
import com.knoxhack.echoarcanacore.integration.veilbound.VeilboundBridgeCatalog;
import com.knoxhack.echoarcaneindex.EchoArcaneIndex;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import com.knoxhack.echocore.api.EchoRuntimeModules;

public enum ArcaneIndexProvider implements IIndexContentProvider {
    INSTANCE;

    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final Identifier CATEGORY_OVERVIEW = id("category/overview");
    private static final Identifier CATEGORY_AETHER = id("category/aether_signal");
    private static final Identifier CATEGORY_RESONANCE = id("category/resonance");
    private static final Identifier CATEGORY_RELICS = id("category/relics");
    private static final Identifier CATEGORY_SPELLS = id("category/spells");
    private static final Identifier CATEGORY_RITUALS = id("category/rituals");
    private static final Identifier CATEGORY_CURSES = id("category/curses");
    private static final Identifier CATEGORY_AETHER_MACHINES = id("category/aether_machines");
    private static final Identifier CATEGORY_RIFT_DATA = id("category/rift_data");
    private static final Identifier CATEGORY_FAMILIARS = id("category/familiars");
    private static final Identifier CATEGORY_VEILBOUND = id("category/veilbound_studies");
    private static final Identifier CATEGORY_FRACTURE = id("category/fracture");
    private static final Identifier CATEGORY_CONVERGENCE = id("category/convergence");
    private static final Identifier CATEGORY_ARCANE_MOBS = id("category/arcane_mobs");
    private static final Identifier CATEGORY_LANDMARKS = id("category/landmarks");
    private static final Identifier CATEGORY_BOSS_GATES = id("category/boss_gates");
    private static final Identifier CATEGORY_FORBIDDEN = id("category/forbidden_knowledge");

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            EchoCoreServices.registerIndexContentProvider(INSTANCE);
        }
    }

    @Override
    public Identifier id() {
        return id("provider/arcane_index");
    }

    @Override
    public IndexContentSnapshot snapshot(IndexBuildContext context) {
        ArcanaCoreServices.registerBuiltIns();
        IndexContentBuilder builder = IndexContentBuilder.create(id());
        register(builder);
        builder.addRelations(relations());
        return builder.snapshot();
    }

    public void register(IIndexRegistry registry) {
        categories().forEach(registry::registerCategory);
        baseEntries().forEach(registry::registerEntry);
        for (SpellDefinition spell : ArcanaCoreServices.spells()) {
            registry.registerEntry(new IndexEntry(
                    id("spell/" + spell.id().getPath().replace('/', '_')),
                    CATEGORY_SPELLS,
                    title(spell.id().getPath()),
                    title(spell.school().name()),
                    "Aether cost " + spell.aetherCost() + ", cooldown " + spell.cooldownTicks() + " ticks.",
                    "Spell record: " + spell.id() + ". Targeting " + title(spell.targetingMode().name())
                            + ", cast type " + title(spell.castType().name()) + ".",
                    new ItemStack(Items.AMETHYST_SHARD),
                    EchoArcaneIndex.MODID,
                    List.of("arcana", "spell", spell.school().name().toLowerCase(Locale.ROOT)),
                    IndexEntryState.VISIBLE,
                    List.of(),
                    List.of(),
                    List.of(),
                    300 + spell.school().ordinal()));
        }
        for (RitualDefinition ritual : ArcanaCoreServices.rituals()) {
            registry.registerEntry(new IndexEntry(
                    id("ritual/" + ritual.id().getPath().replace('/', '_')),
                    CATEGORY_RITUALS,
                    title(ritual.id().getPath()),
                    title(ritual.family().name()),
                    "Required Aether " + ritual.requiredAether() + ", instability " + ritual.instability() + ".",
                    "Ritual record: " + ritual.id() + ". Structure pattern " + safe(ritual.structurePattern()) + ".",
                    new ItemStack(Items.CRYING_OBSIDIAN),
                    EchoArcaneIndex.MODID,
                    List.of("arcana", "ritual", ritual.family().name().toLowerCase(Locale.ROOT)),
                    IndexEntryState.VISIBLE,
                    List.of(),
                    List.of(),
                    List.of(),
                    400 + ritual.family().ordinal()));
        }
        for (CurseDefinition curse : ArcanaCoreServices.curses()) {
            registry.registerEntry(new IndexEntry(
                    id("curse/" + curse.id().getPath().replace('/', '_')),
                    CATEGORY_CURSES,
                    title(curse.id().getPath()),
                    title(curse.category().name()),
                    "Stages " + curse.stageCount() + ", max severity " + curse.maxSeverity() + ".",
                    "Curse record: " + curse.id() + ". Symptoms: " + String.join(", ", curse.symptoms()) + ".",
                    new ItemStack(Items.SCULK),
                    EchoArcaneIndex.MODID,
                    List.of("arcana", "curse", curse.category().name().toLowerCase(Locale.ROOT)),
                    curse.visible() ? IndexEntryState.VISIBLE : IndexEntryState.LOCKED,
                    List.of(),
                    List.of(),
                    List.of(),
                    500 + curse.category().ordinal()));
        }
        for (ArcaneRelicDefinition relic : ArcanaCoreServices.relics()) {
            registry.registerEntry(new IndexEntry(
                    id("relic/" + relic.id().getPath().replace('/', '_')),
                    CATEGORY_RELICS,
                    title(relic.id().getPath()),
                    title(relic.category()),
                    "Lifecycle " + title(relic.lifecycle().name()) + ", instability " + relic.instability()
                            + ", curse risk " + relic.curseRisk() + ".",
                    relicBody(relic),
                    iconStack(relic.id(), Items.RECOVERY_COMPASS),
                    EchoArcaneIndex.MODID,
                    List.of("arcana", "relic", relic.category(), relic.lifecycle().name().toLowerCase(Locale.ROOT)),
                    relicState(relic),
                    List.of(),
                    linkedItem(relic.id()),
                    List.of(),
                    600 + Math.abs(relic.id().toString().hashCode() % 300)));
        }
    }

    private static List<IndexCategory> categories() {
        return List.of(
                category(CATEGORY_OVERVIEW, "Arcana Overview", "Branch overview and Index-first rules.", Items.KNOWLEDGE_BOOK, 10),
                category(CATEGORY_AETHER, "Aether Signal", "Raw, refined, cursed, rift, soul, signal, veil, and fracture energy.", Items.ECHO_SHARD, 20),
                category(CATEGORY_RESONANCE, "Resonance", "Veilbound resonance categories, assignments, and scan semantics.", Items.AMETHYST_SHARD, 30),
                category(CATEGORY_RELICS, "Relics", "Relic lifecycle, stabilization, binding, corruption, and upgrades.", Items.RECOVERY_COMPASS, 40),
                category(CATEGORY_SPELLS, "Spells", "Spell schools, focus casting, modifiers, and corruption risk.", Items.AMETHYST_SHARD, 50),
                category(CATEGORY_RITUALS, "Rituals", "Ritual layouts, conditions, instability, failures, and outputs.", Items.CRYING_OBSIDIAN, 60),
                category(CATEGORY_CURSES, "Curses", "Curse stages, symptoms, contracts, and cleansing.", Items.SCULK, 70),
                category(CATEGORY_AETHER_MACHINES, "Aether Machines", "Arcane machines, storage, transfer, contamination, and overload.", Items.REDSTONE, 80),
                category(CATEGORY_RIFT_DATA, "Rift Data", "RiftWorlds route data, portals, ruins, hazards, and bosses.", Items.RESPAWN_ANCHOR, 90),
                category(CATEGORY_FAMILIARS, "Familiars", "Familiar bonds, commands, evolution, and curse contamination.", Items.NAME_TAG, 100),
                category(CATEGORY_VEILBOUND, "Veilbound Studies", "ARCANA: Veilbound Studies bridge pages.", Items.WRITABLE_BOOK, 110),
                category(CATEGORY_FRACTURE, "Fracture", "Fracture pressure, rifts, seals, transforms, gates, and boss states.", Items.RESPAWN_ANCHOR, 120),
                category(CATEGORY_CONVERGENCE, "Convergence", "Veilbound convergence recipes and stabilizer budgets.", Items.AMETHYST_CLUSTER, 130),
                category(CATEGORY_ARCANE_MOBS, "Arcane Mobs", "Arcana entities, drops, bosses, and scan context.", Items.ARMOR_STAND, 140),
                category(CATEGORY_LANDMARKS, "Landmarks", "Ritual circles, observatories, vaults, archives, springs, gates, and monoliths.", Items.FILLED_MAP, 150),
                category(CATEGORY_BOSS_GATES, "Boss Gates", "Deep Veil, guardian, Unwritten One, and Fracture Heart gates.", Items.NETHER_STAR, 160),
                category(CATEGORY_FORBIDDEN, "Forbidden Knowledge", "Warning-gated pages and consequences.", Items.ENCHANTED_BOOK, 170));
    }

    private static List<IndexEntry> baseEntries() {
        List<IndexEntry> entries = new ArrayList<>();
        entries.add(entry("overview/arcana_division", CATEGORY_OVERVIEW, "ECHO: Arcana Division",
                "Magic is an ancient operating system hidden behind reality.",
                "The player is a field operator, relic engineer, ritual hacker, curse survivor, resonance analyst, and forbidden archive researcher.",
                Items.KNOWLEDGE_BOOK, IndexEntryState.VISIBLE, 10, "overview", "index-first"));
        entries.add(entry("overview/index_first_rule", CATEGORY_OVERVIEW, "Index First Rule",
                "ECHO: Index and ECHO: Arcane Index are official knowledge systems.",
                "JEI may mirror fallback recipe views, but Index, Arcane Index, Terminal, Grimoire, Lens, Field Journal, and HoloMap must explain progression without JEI.",
                Items.COMPASS, IndexEntryState.VISIBLE, 20, "jei-optional", "knowledge"));
        entries.add(entry("aether/aether_signal", CATEGORY_AETHER, "Aether Signal",
                "Arcane energy as decoded signal science.",
                "Aether types: raw, refined, cursed, rift, soul, signal, veil resonance, and fracture energy.",
                Items.ECHO_SHARD, IndexEntryState.VISIBLE, 30, "aether", "energy"));
        entries.add(entry("aether/contamination", CATEGORY_AETHER, "Contamination",
                "Aether can become unstable, cursed, or cross-contaminated.",
                "Purification, storage limits, transfer rate, accepted types, and output type are shared Arcana Core concepts.",
                Items.GLASS_BOTTLE, IndexEntryState.VISIBLE, 40, "aether", "contamination"));
        entries.add(entry("relics/lifecycle", CATEGORY_RELICS, "Relic Lifecycle",
                "Unknown, scanned, decoded, stabilized, awakened, bound, corrupted, forbidden, legendary.",
                "RelicTech and Veilbound relics can report lifecycle state into Arcana Core and Arcane Index.",
                Items.RECOVERY_COMPASS, IndexEntryState.VISIBLE, 50, "relic", "lifecycle"));
        entries.add(entry("rituals/stability", CATEGORY_RITUALS, "Ritual Stability",
                "Rituals are engineered circuits, not arbitrary recipes.",
                "Altar type, structure pattern, pedestal inputs, aether, resonance categories, conditions, and failure tables define each ritual.",
                Items.CRYING_OBSIDIAN, IndexEntryState.VISIBLE, 60, "ritual", "stability"));
        entries.add(entry("curses/stages", CATEGORY_CURSES, "Curse Stages",
                "Curses progress through discoverable symptoms and staged consequences.",
                "Arcana Core tracks category, stage count, severity, discovery condition, symptoms, cleansing methods, and contract options.",
                Items.SCULK_CATALYST, IndexEntryState.VISIBLE, 70, "curse", "stages"));
        addRitualCoreEntries(entries);
        addSpellCoreEntries(entries);
        addCurseCoreEntries(entries);
        addVeilboundEntries(entries);
        return entries;
    }

    private static void addRitualCoreEntries(List<IndexEntry> entries) {
        IndexEntryState state = EchoRuntimeModules.isLoaded("echoritualcore") ? IndexEntryState.VISIBLE : IndexEntryState.LOCKED;
        entries.add(entry("ritualcore/basic_altar", CATEGORY_RITUALS, "Basic Altar",
                "Early shared ritual center for multi-block diagnostics and focused ritual execution.",
                "A valid basic array needs four Rune Circles and at least one Offering Pedestal nearby. The altar diagnostics screen, Lens rows, and HoloMap marker bridge all read this structure state.",
                Items.CRYING_OBSIDIAN, state, 80, "ritualcore", "basic_altar", "altar"));
        entries.add(entry("ritualcore/offering_pedestal", CATEGORY_RITUALS, "Offering Pedestal",
                "One-slot pedestal input node for shared array rituals.",
                "Basic Altars search nearby pedestals before player inventory, so Aether Chalk, Stability Seal, Purity Catalyst, Ritual Focus, and Refined Aether Sample can be staged safely.",
                Items.AMETHYST_BLOCK, state, 81, "ritualcore", "offering_pedestal", "layout"));
        entries.add(entry("ritualcore/rune_circle", CATEGORY_RITUALS, "Rune Circle",
                "Circuit block used by RitualCore layouts and Index previews.",
                "Rune Circles describe ritual structure, scan hints, and placement guidance without making JEI the source of truth.",
                Items.CALCITE, state, 82, "ritualcore", "rune_circle", "layout"));
        entries.add(entry("ritualcore/stability_pylon", CATEGORY_RITUALS, "Stability Pylon",
                "Support block for reducing instability in advanced arrays.",
                "The pylon is surfaced now so Relic Stabilization, backlash prevention, and future grand arrays share a visible vocabulary.",
                Items.POLISHED_BLACKSTONE_BRICKS, state, 83, "ritualcore", "stability_pylon", "stability"));
        entries.add(entry("ritualcore/ritual_backlash", CATEGORY_RITUALS, "Ritual Backlash",
                "Failed ritual checks should explain missing inputs before consuming catalysts.",
                "RitualCore records safe failure diagnostics for MissionCore and Terminal while preserving the held relic stack.",
                Items.REDSTONE_TORCH, state, 84, "ritualcore", "backlash", "diagnostic"));
        entries.add(entry("ritualcore/relic_stabilization", CATEGORY_RITUALS, "Relic Stabilization",
                "Consumes RelicTech lifecycle and risk data to turn damaged or corrupted relics into stabilized relics.",
                "Requires an identified RelicTech relic plus Stability Seal. Corrupted relics require extra seals, clear overclock/corruption flags, reduce instability, and keep the same item stack.",
                Items.ECHO_SHARD, state, 85, "ritualcore", "relic_stabilization", "relictech"));
        entries.add(entry("ritualcore/curse_cleansing_i", CATEGORY_RITUALS, "Curse Cleansing I",
                "First playable curse-cleansing ritual for relic-bound corruption.",
                "Requires a complete basic array, sneak-use on a Basic Altar with an identified corrupted RelicTech relic, and Purity Catalyst from pedestal or inventory. Clears corruption/overclock flags and steps corrupted relics back to damaged.",
                Items.GLOWSTONE_DUST, state, 86, "ritualcore", "curse_cleansing_i", "curse"));
        entries.add(entry("ritualcore/forbidden_rituals", CATEGORY_FORBIDDEN, "Forbidden Rituals",
                "Corrupted arrays and future Blood Contract work must stay warning-gated.",
                "Forbidden rituals may add corruption, curses, or mission warnings when CurseCore exists. Safe read paths and Index explanations remain mandatory.",
                Items.ENCHANTED_BOOK, state, 87, "ritualcore", "forbidden", "warning"));
        entries.add(entry("ritualcore/aether_calibration", CATEGORY_RITUALS, "Aether Calibration",
                "First active non-RelicTech ritual in the shared engine.",
                "Requires a complete basic array and Aether Chalk. The output is Refined Aether Sample, used by Spell Core Awakening and Rift Crack Reveal.",
                Items.GLOWSTONE, state, 88, "ritualcore", "aether_calibration", "aether"));
        entries.add(entry("ritualcore/spell_core_awakening", CATEGORY_RITUALS, "Spell Core Awakening",
                "Awakens the first shared spell-core shell before full SpellCore lands.",
                "Requires a complete basic array, Ritual Focus, and Refined Aether Sample. Produces RitualCore's Awakened Spell Core item for later SpellCore migration.",
                Items.ENDER_EYE, state, 89, "ritualcore", "spell_core_awakening", "spellcore"));
        entries.add(entry("ritualcore/rift_crack_reveal", CATEGORY_RITUALS, "Rift Crack Reveal",
                "Uses ritual triangulation to create HoloMap and Lens follow-up data for RiftWorlds.",
                "Requires a complete basic array, Refined Aether Sample, and Aether Chalk. The generated rift marker is imprecise by design.",
                Items.RECOVERY_COMPASS, state, 90, "ritualcore", "rift_crack_reveal", "rift"));
    }

    private static void addSpellCoreEntries(List<IndexEntry> entries) {
        IndexEntryState state = EchoRuntimeModules.isLoaded("echospellcore") ? IndexEntryState.VISIBLE : IndexEntryState.LOCKED;
        entries.add(entry("spellcore/signal_focus", CATEGORY_SPELLS, "Signal Focus",
                "Starter focus for SpellCore's first playable casting loop.",
                "Normal use casts the active spell. Sneak-use cycles Spell Deck slots when a deck is carried, or cycles the focus fallback selection. When RitualCore is present, carrying awakened_spell_core authorizes casting.",
                Items.BLAZE_ROD, state, 210, "spellcore", "signal_focus", "focus"));
        entries.add(entry("spellcore/awakened_spell_core", CATEGORY_SPELLS, "Awakened Spell Core",
                "RitualCore's spell-core awakening output becomes the first SpellCore authorization token.",
                "SpellCore does not duplicate the ritual. It consumes the unlock state at runtime and keeps the Index path connected to RitualCore.",
                Items.ENDER_EYE, state, 211, "spellcore", "awakened_spell_core", "ritualcore"));
        entries.add(entry("spellcore/spell_deck", CATEGORY_SPELLS, "Spell Deck",
                "Loadout surface for the real SpellCore casting layer.",
                "Stores six spell slots with three physical modifier sockets per slot. Carry it with Signal Focus to cast the active slot, or open it to assign starter spells across every Arcana school.",
                Items.BOOK, state, 212, "spellcore", "spell_deck", "loadout"));
        entries.add(entry("spellcore/spell_modifiers", CATEGORY_SPELLS, "Spell Modifiers",
                "Socketed modifier bus for range, efficiency, and overcharge tradeoffs.",
                "Range stretches targeting, Efficiency reduces cost/cooldown pressure, and Overcharge consumes two sockets while raising curse and contamination backlash risk.",
                Items.REDSTONE_TORCH, state, 213, "spellcore", "modifiers", "overcharge"));
        entries.add(entry("spellcore/signal_pulse", CATEGORY_SPELLS, "Signal Pulse",
                "Cone pulse that marks nearby signatures and supports Lens-style discovery.",
                "Costs Signal Aether, applies glow and slow to living targets in front of the operator, then starts a server-side cooldown.",
                Items.AMETHYST_SHARD, state, 214, "spellcore", "signal_pulse", "signal"));
        entries.add(entry("spellcore/echo_mark", CATEGORY_SPELLS, "Echo Mark",
                "Signal raycast tag for a single target.",
                "Marks and weakens the looked-at entity, turning scan discipline into combat-readable target focus.",
                Items.GLOW_INK_SAC, state, 215, "spellcore", "echo_mark", "signal"));
        entries.add(entry("spellcore/static_burst", CATEGORY_SPELLS, "Static Burst",
                "Close signal disruption burst.",
                "Slows, weakens, and nudges nearby hostiles while teaching area casting without generic fireball language.",
                Items.REDSTONE, state, 216, "spellcore", "static_burst", "signal"));
        entries.add(entry("spellcore/aether_bolt", CATEGORY_SPELLS, "Aether Bolt",
                "Starter synchronized Aether projectile.",
                "Costs raw Aether and spawns SpellCore's server-owned projectile entity with synced kind, damage, travel life, trail, and hit effects.",
                Items.ECHO_SHARD, state, 217, "spellcore", "aether_bolt", "aether", "projectile"));
        entries.add(entry("spellcore/aether_shield", CATEGORY_SPELLS, "Aether Shield",
                "Self-targeted ward for early survival casting.",
                "Converts raw Aether into absorption and resistance, with Overcharge improving output but adding feedback risk.",
                Items.SHIELD, state, 218, "spellcore", "aether_shield", "aether"));
        entries.add(entry("spellcore/arcane_lift", CATEGORY_SPELLS, "Arcane Lift",
                "Utility lift and descent stabilization spell.",
                "Targets a looked-at entity with brief levitation, or stabilizes the operator with slow falling when no target resolves.",
                Items.FEATHER, state, 219, "spellcore", "arcane_lift", "aether"));
        entries.add(entry("spellcore/ash_veil", CATEGORY_SPELLS, "Ash Veil",
                "Self-targeted survival utility that hides the operator in ash signal noise.",
                "Costs raw Aether and grants brief invisibility plus resistance while the HUD reports cooldown and remaining aether.",
                Items.GUNPOWDER, state, 220, "spellcore", "ash_veil", "ash"));
        entries.add(entry("spellcore/dust_lance", CATEGORY_SPELLS, "Dust Lance",
                "Ash-school synchronized projectile.",
                "Fires a lower, harsher projectile that weakens and slows targets on hit, making Ash feel like signal grit rather than pure flame.",
                Items.BLAZE_POWDER, state, 221, "spellcore", "dust_lance", "ash", "projectile"));
        entries.add(entry("spellcore/cinder_skin", CATEGORY_SPELLS, "Cinder Skin",
                "Ash survival skin for hostile heat and contact risk.",
                "Grants fire resistance plus resistance and accepts the same modifier/cooldown rules as the rest of the starter deck.",
                Items.MAGMA_CREAM, state, 222, "spellcore", "cinder_skin", "ash"));
        entries.add(entry("spellcore/void_step", CATEGORY_SPELLS, "Void Step",
                "Short self blink for the first Void-school mobility slice.",
                "Uses Rift Aether, finds a safe line-of-sight exit vector, and creates visible portal feedback at both ends.",
                Items.ENDER_PEARL, state, 223, "spellcore", "void_step", "void"));
        entries.add(entry("spellcore/null_bolt", CATEGORY_SPELLS, "Null Bolt",
                "Void-school synchronized projectile.",
                "Fires a reversed-portal projectile that weakens and fatigues targets, extending the projectile layer beyond raw Aether and Ash.",
                Items.OBSIDIAN, state, 224, "spellcore", "null_bolt", "void", "projectile"));
        entries.add(entry("spellcore/hollow_cage", CATEGORY_SPELLS, "Hollow Cage",
                "Void containment pulse for raycast control.",
                "Constrains hostiles near a looked-at target or forward position with slowness, weakness, and mining fatigue.",
                Items.IRON_INGOT, state, 225, "spellcore", "hollow_cage", "void"));
        entries.add(entry("spellcore/storm_lance", CATEGORY_SPELLS, "Storm Lance",
                "Storm-school synchronized projectile.",
                "Travels faster than Aether Bolt, sparks on the client and server, and leaves glow/slow effects on hit.",
                Items.LIGHTNING_ROD, state, 226, "spellcore", "storm_lance", "storm", "projectile"));
        entries.add(entry("spellcore/static_dash", CATEGORY_SPELLS, "Static Dash",
                "Storm movement burst.",
                "Converts Signal Aether into a forward dash and speed effect, with Overcharge pushing harder at higher feedback risk.",
                Items.FEATHER, state, 227, "spellcore", "static_dash", "storm"));
        entries.add(entry("spellcore/thunder_cage", CATEGORY_SPELLS, "Thunder Cage",
                "Storm area-control spell.",
                "Stuns and lightly damages hostiles around a looked-at target or the operator while reporting exact target count.",
                Items.COPPER_INGOT, state, 228, "spellcore", "thunder_cage", "storm"));
        entries.add(entry("spellcore/crystal_wall", CATEGORY_SPELLS, "Crystal Wall",
                "Crystal defensive projection.",
                "Uses Refined Aether to raise absorption and resistance with visible crystal-style particles.",
                Items.GLASS, state, 229, "spellcore", "crystal_wall", "crystal"));
        entries.add(entry("spellcore/shard_burst", CATEGORY_SPELLS, "Shard Burst",
                "Crystal cone burst for close engagements.",
                "Damages and reveals targets inside a forward cone, giving Crystal a clear tactical identity without new blocks yet.",
                Items.AMETHYST_SHARD, state, 230, "spellcore", "shard_burst", "crystal"));
        entries.add(entry("spellcore/resonant_armor", CATEGORY_SPELLS, "Resonant Armor",
                "Crystal armor weave.",
                "Turns Refined Aether into resistance and absorption, with resonance feedback visible on the HUD.",
                Items.IRON_CHESTPLATE, state, 231, "spellcore", "resonant_armor", "crystal"));
        entries.add(entry("spellcore/blood_surge", CATEGORY_SPELLS, "Blood Surge",
                "Blood-school self overclock.",
                "Spends health and Cursed Aether to grant a short power burst, explicitly surfacing the price of forbidden casting.",
                Items.RED_DYE, state, 232, "spellcore", "blood_surge", "blood", "curse"));
        entries.add(entry("spellcore/rift_blink", CATEGORY_SPELLS, "Rift Blink",
                "Rift-school blink variant.",
                "Builds on Void Step's safe teleport search while using Rift Aether and stronger fracture-style feedback.",
                Items.ENDER_EYE, state, 233, "spellcore", "rift_blink", "rift"));
        entries.add(entry("spellcore/soul_thread", CATEGORY_SPELLS, "Soul Thread",
                "Soul-school raycast utility.",
                "Heals allies and weakens hostile targets, giving Soul magic a support/control role before familiars arrive.",
                Items.SOUL_LANTERN, state, 234, "spellcore", "soul_thread", "soul"));
        entries.add(entry("spellcore/decay_touch", CATEGORY_SPELLS, "Decay Touch",
                "Decay-school close raycast.",
                "Applies damage, poison, and weakness through Cursed Aether so corruption pressure has an early combat expression.",
                Items.ROTTEN_FLESH, state, 235, "spellcore", "decay_touch", "decay", "curse"));
        entries.add(entry("spellcore/veil_trace", CATEGORY_SPELLS, "Veil Trace",
                "Veil-school diagnostic raycast.",
                "Spends Veil Resonance to expose a hidden signal or mark a looked-at entity, giving the shared casting layer a bridge into Veilbound field research language.",
                Items.SPYGLASS, state, 236, "spellcore", "veil_trace", "veil", "scan"));
        entries.add(entry("spellcore/fracture_shear", CATEGORY_SPELLS, "Fracture Shear",
                "Fracture-school synchronized projectile.",
                "Uses Fracture Energy to fire a hard-edged projectile with client prediction, stronger rendered cross-light, and weakening hit effects.",
                Items.ECHO_SHARD, state, 237, "spellcore", "fracture_shear", "fracture", "projectile", "curse"));
        entries.add(entry("spellcore/aether_cost_cooldown", CATEGORY_AETHER, "Spell Aether And Cooldown",
                "SpellCore uses Arcana Core Aether Signal and its own server-side cooldown ledger.",
                "Focus custom data mirrors active spell, deck slot, socketed modifiers, aether, cooldown, contamination, curse risk, and last status for HUD and tooltip display without trusting the client.",
                Items.REPEATER, state, 238, "spellcore", "cooldown", "aether"));
    }

    private static void addCurseCoreEntries(List<IndexEntry> entries) {
        IndexEntryState state = EchoRuntimeModules.isLoaded("echocursecore") ? IndexEntryState.VISIBLE : IndexEntryState.LOCKED;
        entries.add(entry("cursecore/echo_rot", CATEGORY_CURSES, "Echo Rot",
                "First live CurseCore target for SpellCore backlash and RitualCore cleansing.",
                "Echo Rot is a signal/mind curse with persistent player stages, periodic symptoms, Lens rows, Terminal reporting, and MissionCore hooks.",
                Items.SCULK, state, 230, "cursecore", "echo_rot", "live"));
        entries.add(entry("cursecore/glass_veins", CATEGORY_CURSES, "Glass Veins",
                "Starter body/crystal curse record for future spell tradeoffs.",
                "The first slice registers Glass Veins, persists stages, and exposes symptoms so later Crystal school work has a stable target.",
                Items.AMETHYST_CLUSTER, state, 231, "cursecore", "glass_veins"));
        entries.add(entry("cursecore/echo_rot_sample", CATEGORY_CURSES, "Echo Rot Sample",
                "Controlled item that applies Echo Rot I to create a live cleansing target.",
                "This makes Curse Cleansing I testable without waiting for future CurseCore mobs, rifts, or corrupted spellcasting loops.",
                Items.SCULK_VEIN, state, 232, "cursecore", "echo_rot_sample", "sample"));
        entries.add(entry("cursecore/curse_cleansing_bridge", CATEGORY_RITUALS, "Curse Cleansing Bridge",
                "RitualCore can reduce live CurseCore player curses through a reflective optional bridge.",
                "Sneak-use a complete Basic Altar array with Purity Catalyst. If CurseCore is loaded and the player has a curse, a stage is reduced before the catalyst is consumed.",
                Items.GLOWSTONE_DUST, state, 233, "cursecore", "curse_cleansing_bridge", "ritualcore"));
    }

    private static void addVeilboundEntries(List<IndexEntry> entries) {
        for (VeilboundBridgeCatalog.Entry entry : VeilboundBridgeCatalog.allEntries()) {
            entries.add(catalogEntry(entry));
        }
    }

    private static IndexEntry catalogEntry(VeilboundBridgeCatalog.Entry entry) {
        String path = VeilboundBridgeCatalog.indexPagePath(entry);
        Identifier linkedItem = linkableItem(entry);
        List<Identifier> linkedItems = linkedItem == null ? List.of() : List.of(linkedItem);
        return new IndexEntry(
                id(path),
                categoryFor(entry.kind()),
                entry.title(),
                "ARCANA: Veilbound Studies / " + VeilboundBridgeCatalog.kindTitle(entry.kind()),
                entry.summary(),
                catalogBody(entry),
                iconStack(entry.icon(), fallbackIcon(entry.kind())),
                EchoArcaneIndex.MODID,
                catalogTags(entry),
                catalogState(entry),
                List.of(),
                linkedItems,
                List.of(),
                1000 + entry.sortOrder());
    }

    private static List<IndexRelation> relations() {
        return List.of(
                relation("overview_to_aether", id("overview/arcana_division"), id("aether/aether_signal"), "teaches"),
                relation("index_first_to_jei", id("overview/index_first_rule"), id("overview/arcana_division"), "governs"),
                relation("ritual_stability_to_basic_altar", id("rituals/stability"), id("ritualcore/basic_altar"), "teaches"),
                relation("basic_altar_to_relic_stabilization", id("ritualcore/basic_altar"), id("ritualcore/relic_stabilization"), "performs"),
                relation("basic_altar_to_aether_calibration", id("ritualcore/basic_altar"), id("ritualcore/aether_calibration"), "performs"),
                relation("aether_calibration_to_spell_core", id("ritualcore/aether_calibration"), id("ritualcore/spell_core_awakening"), "feeds"),
                relation("aether_calibration_to_rift_reveal", id("ritualcore/aether_calibration"), id("ritualcore/rift_crack_reveal"), "feeds"),
                relation("relic_lifecycle_to_relic_stabilization", id("relics/lifecycle"), id("ritualcore/relic_stabilization"), "feeds"),
                relation("curse_stages_to_curse_cleansing_i", id("curses/stages"), id("ritualcore/curse_cleansing_i"), "explains"),
                relation("spell_core_to_focus", id("ritualcore/spell_core_awakening"), id("spellcore/signal_focus"), "unlocks"),
                relation("focus_to_spell_deck", id("spellcore/signal_focus"), id("spellcore/spell_deck"), "uses"),
                relation("spell_deck_to_modifiers", id("spellcore/spell_deck"), id("spellcore/spell_modifiers"), "configures"),
                relation("focus_to_signal_pulse", id("spellcore/signal_focus"), id("spellcore/signal_pulse"), "casts"),
                relation("focus_to_echo_mark", id("spellcore/signal_focus"), id("spellcore/echo_mark"), "casts"),
                relation("focus_to_static_burst", id("spellcore/signal_focus"), id("spellcore/static_burst"), "casts"),
                relation("focus_to_aether_bolt", id("spellcore/signal_focus"), id("spellcore/aether_bolt"), "casts"),
                relation("focus_to_aether_shield", id("spellcore/signal_focus"), id("spellcore/aether_shield"), "casts"),
                relation("focus_to_arcane_lift", id("spellcore/signal_focus"), id("spellcore/arcane_lift"), "casts"),
                relation("focus_to_ash_veil", id("spellcore/signal_focus"), id("spellcore/ash_veil"), "casts"),
                relation("focus_to_dust_lance", id("spellcore/signal_focus"), id("spellcore/dust_lance"), "casts"),
                relation("focus_to_cinder_skin", id("spellcore/signal_focus"), id("spellcore/cinder_skin"), "casts"),
                relation("focus_to_void_step", id("spellcore/signal_focus"), id("spellcore/void_step"), "casts"),
                relation("focus_to_null_bolt", id("spellcore/signal_focus"), id("spellcore/null_bolt"), "casts"),
                relation("focus_to_hollow_cage", id("spellcore/signal_focus"), id("spellcore/hollow_cage"), "casts"),
                relation("focus_to_storm_lance", id("spellcore/signal_focus"), id("spellcore/storm_lance"), "casts"),
                relation("focus_to_static_dash", id("spellcore/signal_focus"), id("spellcore/static_dash"), "casts"),
                relation("focus_to_thunder_cage", id("spellcore/signal_focus"), id("spellcore/thunder_cage"), "casts"),
                relation("focus_to_crystal_wall", id("spellcore/signal_focus"), id("spellcore/crystal_wall"), "casts"),
                relation("focus_to_shard_burst", id("spellcore/signal_focus"), id("spellcore/shard_burst"), "casts"),
                relation("focus_to_resonant_armor", id("spellcore/signal_focus"), id("spellcore/resonant_armor"), "casts"),
                relation("focus_to_blood_surge", id("spellcore/signal_focus"), id("spellcore/blood_surge"), "casts"),
                relation("focus_to_rift_blink", id("spellcore/signal_focus"), id("spellcore/rift_blink"), "casts"),
                relation("focus_to_soul_thread", id("spellcore/signal_focus"), id("spellcore/soul_thread"), "casts"),
                relation("focus_to_decay_touch", id("spellcore/signal_focus"), id("spellcore/decay_touch"), "casts"),
                relation("focus_to_veil_trace", id("spellcore/signal_focus"), id("spellcore/veil_trace"), "casts"),
                relation("focus_to_fracture_shear", id("spellcore/signal_focus"), id("spellcore/fracture_shear"), "casts"),
                relation("echo_rot_to_cleansing", id("cursecore/echo_rot"), id("cursecore/curse_cleansing_bridge"), "cleansed_by"),
                relation("veil_lens_to_field_journal", id("veilbound/item/veil_lens"), id("veilbound/item/field_journal"), "unlocks"),
                relation("first_contact_to_research_desk", id("veilbound/research/fundamentals/first_contact"), id("veilbound/block/research_desk"), "explains"),
                relation("relic_lifecycle_to_phase_anchor", id("relics/lifecycle"), id("relic/phase_anchor"), "explains"),
                relation("phase_anchor_to_void_compass", id("relic/phase_anchor"), id("relic/void_compass"), "pairs_with"),
                relation("blood_circuit_to_relic_lifecycle", id("relic/blood_circuit"), id("relics/lifecycle"), "warns"),
                relation("fracture_rift_to_seal", id("veilbound/block/fracture_rift"), id("veilbound/block/fracture_seal"), "contained_by"),
                relation("deep_gate_to_fracture_heart", id("veilbound/block/deep_veil_gate"), id("veilbound/boss-gate/fracture_heart"), "gates"));
    }

    private static String relicBody(ArcaneRelicDefinition relic) {
        String abilities = relic.discoveredAbilities().isEmpty()
                ? "No decoded abilities recorded yet"
                : relic.discoveredAbilities().stream().map(Identifier::toString).collect(java.util.stream.Collectors.joining(", "));
        String hidden = relic.hiddenAbilities().isEmpty()
                ? "none"
                : relic.hiddenAbilities().stream().map(Identifier::toString).collect(java.util.stream.Collectors.joining(", "));
        return "Relic registry id: " + relic.id() + ". Category: " + title(relic.category())
                + ". Lifecycle: " + title(relic.lifecycle().name()) + ". Aether storage accepts "
                + relic.storage().acceptedTypes().stream().map(Enum::name).collect(java.util.stream.Collectors.joining(", "))
                + " and outputs " + relic.storage().outputType().name() + ". Decoded abilities: " + abilities
                + ". Hidden abilities: " + hidden + ". Upgrade slots: " + relic.upgradeSlots() + ".";
    }

    private static IndexEntryState relicState(ArcaneRelicDefinition relic) {
        return switch (relic.lifecycle()) {
            case UNKNOWN -> IndexEntryState.LOCKED;
            case CORRUPTED -> IndexEntryState.CORRUPTED;
            case FORBIDDEN -> IndexEntryState.LOCKED;
            default -> IndexEntryState.VISIBLE;
        };
    }

    private static Identifier categoryFor(VeilboundBridgeCatalog.Kind kind) {
        return switch (kind) {
            case BLOCK, ITEM, PARTICLE, RESEARCH -> CATEGORY_VEILBOUND;
            case ENTITY -> CATEGORY_ARCANE_MOBS;
            case RESONANCE_CATEGORY, RESONANCE_ASSIGNMENT -> CATEGORY_RESONANCE;
            case RITUAL -> CATEGORY_RITUALS;
            case CONVERGENCE -> CATEGORY_CONVERGENCE;
            case MACHINE_RECIPE -> CATEGORY_AETHER_MACHINES;
            case FRACTURE_TRANSFORM -> CATEGORY_FRACTURE;
            case LANDMARK -> CATEGORY_LANDMARKS;
            case BOSS_GATE -> CATEGORY_BOSS_GATES;
        };
    }

    private static String catalogBody(VeilboundBridgeCatalog.Entry entry) {
        String registry = "ARCANA registry id: " + entry.id() + ".";
        String availability = EchoRuntimeModules.isLoaded(VeilboundBridgeCatalog.MODID)
                ? " ARCANA is loaded, so scans and mission hooks can unlock live discoveries."
                : " ARCANA is absent, so this page stays as locked bridge metadata without classloading ARCANA code.";
        String extra = switch (entry.kind()) {
            case BLOCK -> "ECHO Lens can recognize this block when the optional Lens bridge is present; the Veil Lens remains the authoritative Field Journal scanner.";
            case ITEM -> "This item remains owned by ARCANA and is mirrored here so players can find the official knowledge trail without JEI.";
            case ENTITY -> "Entity knowledge bridges to Lens scans, HoloMap gates where relevant, Grimoire records, and MissionCore objectives.";
            case PARTICLE -> "Particle records are diagnostic notes for scan, ritual, convergence, and fracture feedback.";
            case RESEARCH -> "Field Journal progression owns the unlock; Arcane Index exposes the cross-addon reference and related systems.";
            case RESONANCE_CATEGORY -> "Resonance categories are mirrored for Index search, Lens hints, and ritual/convergence explanations.";
            case RESONANCE_ASSIGNMENT -> "Assignments help translate scan targets into resonance categories without duplicating ARCANA research logic.";
            case RITUAL -> "Veilbound rituals also register through Arcana Core's ritual provider as bridge definitions for shared diagnostics.";
            case CONVERGENCE -> "Convergence entries describe matrix work, stabilizer budget, vessels, pressure, and failure-risk context.";
            case MACHINE_RECIPE -> "Machine recipes are surfaced as official Arcane Index pages so JEI remains optional compatibility.";
            case FRACTURE_TRANSFORM -> "Fracture transforms document terrain rewrite risk and containment context.";
            case LANDMARK -> "Landmarks bridge into HoloMap markers after discovery; exact coordinates come from ECHO Lens cache or ARCANA Veil Lens saved scan coordinates.";
            case BOSS_GATE -> "Boss gates bridge into MissionCore and forbidden Grimoire context without replacing the Field Journal path.";
        };
        return registry + availability + " " + extra;
    }

    private static IndexEntryState catalogState(VeilboundBridgeCatalog.Entry entry) {
        if (!EchoRuntimeModules.isLoaded(VeilboundBridgeCatalog.MODID)) {
            return IndexEntryState.LOCKED;
        }
        return switch (entry.kind()) {
            case BOSS_GATE -> IndexEntryState.LOCKED;
            case FRACTURE_TRANSFORM -> IndexEntryState.CORRUPTED;
            default -> IndexEntryState.VISIBLE;
        };
    }

    private static List<String> catalogTags(VeilboundBridgeCatalog.Entry entry) {
        List<String> tags = new ArrayList<>();
        tags.add("arcana");
        tags.add("veilbound");
        tags.add(VeilboundBridgeCatalog.kindPath(entry.kind()));
        tags.add(entry.id().getNamespace());
        if (entry.kind() == VeilboundBridgeCatalog.Kind.BLOCK
                || entry.kind() == VeilboundBridgeCatalog.Kind.ITEM
                || entry.kind() == VeilboundBridgeCatalog.Kind.ENTITY) {
            tags.add("resonance:" + VeilboundBridgeCatalog.primaryResonance(entry.id()));
        }
        if (entry.kind() == VeilboundBridgeCatalog.Kind.MACHINE_RECIPE) {
            tags.add("recipe");
        }
        if (entry.kind() == VeilboundBridgeCatalog.Kind.BOSS_GATE) {
            tags.add("forbidden");
        }
        return List.copyOf(tags);
    }

    private static ItemStack iconStack(Identifier icon, Item fallback) {
        return BuiltInRegistries.ITEM.getOptional(icon).map(ItemStack::new).orElseGet(() -> new ItemStack(fallback));
    }

    private static Item fallbackIcon(VeilboundBridgeCatalog.Kind kind) {
        return switch (kind) {
            case BLOCK -> Items.CRYING_OBSIDIAN;
            case ITEM -> Items.WRITABLE_BOOK;
            case ENTITY -> Items.ARMOR_STAND;
            case PARTICLE -> Items.GLOWSTONE_DUST;
            case RESEARCH -> Items.KNOWLEDGE_BOOK;
            case RESONANCE_CATEGORY, RESONANCE_ASSIGNMENT -> Items.AMETHYST_SHARD;
            case RITUAL -> Items.CRYING_OBSIDIAN;
            case CONVERGENCE -> Items.AMETHYST_CLUSTER;
            case MACHINE_RECIPE -> Items.REDSTONE;
            case FRACTURE_TRANSFORM -> Items.RESPAWN_ANCHOR;
            case LANDMARK -> Items.FILLED_MAP;
            case BOSS_GATE -> Items.NETHER_STAR;
        };
    }

    private static Identifier linkableItem(VeilboundBridgeCatalog.Entry entry) {
        if (!EchoRuntimeModules.isLoaded(VeilboundBridgeCatalog.MODID)) {
            return null;
        }
        Identifier candidate = switch (entry.kind()) {
            case BLOCK, ITEM -> entry.id();
            case ENTITY, BOSS_GATE -> entry.icon();
            default -> null;
        };
        return candidate != null && BuiltInRegistries.ITEM.getOptional(candidate).isPresent() ? candidate : null;
    }

    private static List<Identifier> linkedItem(Identifier candidate) {
        return candidate != null && BuiltInRegistries.ITEM.getOptional(candidate).isPresent()
                ? List.of(candidate)
                : List.of();
    }

    private static IndexCategory category(Identifier id, String title, String desc, net.minecraft.world.item.Item item, int sort) {
        return new IndexCategory(id, title, desc, new ItemStack(item), sort, EchoArcaneIndex.MODID);
    }

    private static IndexEntry entry(String path, Identifier category, String title, String summary, String body,
            net.minecraft.world.item.Item icon, IndexEntryState state, int sort, String... tags) {
        return new IndexEntry(id(path), category, title, "ECHO: Arcane Index", summary, body,
                new ItemStack(icon), EchoArcaneIndex.MODID, List.of(tags), state,
                List.of(), List.of(), List.of(), sort);
    }

    private static IndexRelation relation(String path, Identifier from, Identifier to, String kind) {
        return new IndexRelation(id("relation/" + path), from, to, kind, title(kind), IndexVisibility.VISIBLE, EchoArcaneIndex.MODID);
    }

    private static Identifier id(String path) {
        return EchoArcaneIndex.id(sanitize(path));
    }

    private static String sanitize(String path) {
        String clean = path == null ? "unknown" : path.trim().toLowerCase(Locale.ROOT);
        clean = clean.replace('\\', '/').replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
        while (clean.contains("//")) {
            clean = clean.replace("//", "/");
        }
        return clean.isBlank() ? "unknown" : clean;
    }

    private static String title(String raw) {
        String text = raw == null ? "" : raw.replace('/', ' ').replace('_', ' ').toLowerCase(Locale.ROOT);
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

    private static String safe(Identifier id) {
        return id == null ? "none" : id.toString();
    }
}
