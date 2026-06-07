package com.knoxhack.echoarcanacore.integration.veilbound;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class VeilboundBridgeCatalog {
    public static final String MODID = "arcanaveil";

    private static final List<Entry> BLOCKS = List.of(
            block("research_desk", "Research Desk", "Turns field observations into usable theories."),
            block("theory_board", "Theory Board", "Plans Veilbound research paths without replacing the Field Journal."),
            block("ritual_basin", "Ritual Basin", "Veilbound ritual center with pressure and pedestal requirements."),
            block("focus_pedestal", "Focus Pedestal", "Pedestal input for Veilbound rituals and convergence work."),
            block("convergence_matrix", "Convergence Matrix", "High-risk matrix that aligns items, pressure, vessels, and stabilizers."),
            block("stabilizer_pillar", "Stabilizer Pillar", "Budgets convergence instability through stabilizer geometry."),
            block("resonance_extractor", "Resonance Extractor", "Extracts readable resonance from Veil-saturated material."),
            block("veil_condenser", "Veil Condenser", "Condenses ambient pressure into storage media."),
            block("pattern_etcher", "Pattern Etcher", "Etches sigil control plates for constructs and machines."),
            block("arcane_loom", "Arcane Loom", "Weaves memory thread and Veilbound fabric."),
            block("thought_vessel", "Thought Vessel", "Stores impressions, patterns, and automation memories."),
            block("warding_obelisk", "Warding Obelisk", "Persistent ward anchor and guardian gate component."),
            block("fracture_seal", "Fracture Seal", "Suppresses fracture pressure before terrain rewriting spreads."),
            block("construct_workbench", "Construct Workbench", "Assembles Sigil Construct cores and behavior plates."),
            block("resonance_vessel", "Resonance Vessel", "Stores stable resonance for convergence budgets."),
            block("veil_monitor", "Veil Monitor", "Reads local Veil and fracture pressure diagnostics."),
            block("fracture_detector", "Fracture Detector", "Detects unsafe fracture pressure and nearby rifts."),
            block("fractured_stone", "Fractured Stone", "Terrain transformed by fracture pressure."),
            block("fracture_rift", "Fracture Rift", "World fracture source, scan target, and containment priority."),
            block("veil_monolith", "Veil Monolith", "Ancient landmark node in the Veil network."),
            block("harmonic_spring", "Harmonic Spring", "Stabilizing landmark with high-value resonance behavior."),
            block("deep_veil_gate", "Deep Veil Gate", "Late-game gate into the deepest Veilbound path."),
            block("ancient_observatory_marker", "Ancient Observatory", "Landmark marker for observatory structures."),
            block("buried_research_vault_marker", "Buried Research Vault", "Landmark marker for buried archive caches."),
            block("sealed_archive_marker", "Sealed Archive", "Landmark marker for forbidden archive structures."),
            block("abandoned_ritual_circle_marker", "Abandoned Ritual Circle", "Landmark marker for field ritual ruins."),
            block("fractured_grove_marker", "Fractured Grove", "Landmark marker for fracture-contaminated groves."));

    private static final List<Entry> ITEMS = List.of(
            item("field_journal", "Field Journal", "Veilbound-specific research UI and campaign record."),
            item("veil_lens", "Veil Lens", "Primary Veilbound scanner for observations and resonance readings."),
            item("resonance_shard", "Resonance Shard", "Early readable shard of stable Veil resonance."),
            item("blank_theory_page", "Blank Theory Page", "Paper substrate for theory crafting."),
            item("ink_of_revelation", "Ink of Revelation", "Archive ink used by late Veilbound recipes."),
            item("warding_chalk", "Warding Chalk", "Ritual chalk for stabilization and containment."),
            item("ritual_chalk", "Ritual Chalk", "Basic ritual circuit medium."),
            item("stabilizer_core", "Stabilizer Core", "Core component for stabilizer pillars and convergence budgets."),
            item("fracture_sample", "Fracture Sample", "Unsafe fracture matter used for detection and containment research."),
            item("pure_resonance_crystal", "Pure Resonance Crystal", "Stabilized crystal for rituals and convergence."),
            item("distorted_resonance_crystal", "Distorted Resonance Crystal", "Fracture-touched crystal with higher risk."),
            item("sigil_plate", "Sigil Plate", "Etched control plate for constructs."),
            item("construct_core", "Construct Core", "Core component for Sigil Construct awakening."),
            item("memory_thread", "Memory Thread", "Woven thread that carries observation patterns."),
            item("veil_compass", "Veil Compass", "Navigation aid for gates and landmarks."),
            item("research_notes", "Research Notes", "Recovered field notes for theory work."),
            item("harmonic_alloy_ingot", "Harmonic Alloy Ingot", "Stable alloy for late Veil machines."),
            item("fractured_alloy_ingot", "Fractured Alloy Ingot", "Unstable alloy for gates and forbidden work."),
            item("veil_wisp_spawn_egg", "Veil Wisp Spawn Egg", "Debug/summon item for Veil Wisp."),
            item("fractured_wisp_spawn_egg", "Fractured Wisp Spawn Egg", "Debug/summon item for Fractured Wisp."),
            item("hollow_scholar_spawn_egg", "Hollow Scholar Spawn Egg", "Debug/summon item for Hollow Scholar."),
            item("sigil_construct_spawn_egg", "Sigil Construct Spawn Egg", "Debug/summon item for Sigil Construct."),
            item("fracture_crawler_spawn_egg", "Fracture Crawler Spawn Egg", "Debug/summon item for Fracture Crawler."),
            item("mirror_shade_spawn_egg", "Mirror Shade Spawn Egg", "Debug/summon item for Mirror Shade."),
            item("veilbound_guardian_spawn_egg", "Veilbound Guardian Spawn Egg", "Debug/summon item for Veilbound Guardian."),
            item("unwritten_one_spawn_egg", "Unwritten One Spawn Egg", "Debug/summon item for the Unwritten One."),
            item("fracture_heart_spawn_egg", "Fracture Heart Spawn Egg", "Debug/summon item for the Fracture Heart."));

    private static final List<Entry> ENTITIES = List.of(
            entity("veil_wisp", "Veil Wisp", "A minor Veil entity and early resonance signal."),
            entity("fractured_wisp", "Fractured Wisp", "A pressure-corrupted wisp tied to fracture zones."),
            entity("hollow_scholar", "Hollow Scholar", "Archive-haunting scholar entity and lore bridge."),
            entity("sigil_construct", "Sigil Construct", "Player-built helper and construct systems bridge."),
            entity("fracture_crawler", "Fracture Crawler", "Hostile fracture creature from unsafe pressure zones."),
            entity("mirror_shade", "Mirror Shade", "Signal-shadow entity tied to forbidden reflections."),
            entity("veilbound_guardian", "Veilbound Guardian", "Boss gate guardian for Deep Veil access."),
            entity("unwritten_one", "Unwritten One", "Forbidden archive boss and warning-gated knowledge source."),
            entity("fracture_heart", "Fracture Heart", "Endgame fracture boss and path resolution target."));

    private static final List<Entry> PARTICLES = List.of(
            particle("veil_spark", "Veil Spark", "Small Veil scan and machine spark."),
            particle("resonance_glint", "Resonance Glint", "Readable resonance feedback."),
            particle("fracture_mote", "Fracture Mote", "Fracture pressure particulate."),
            particle("warding_ring", "Warding Ring", "Containment and warding circle feedback."),
            particle("ritual_glyph", "Ritual Glyph", "Ritual circuit visual feedback."),
            particle("veil_beam", "Veil Beam", "Focused Veil energy beam."),
            particle("fracture_ribbon", "Fracture Ribbon", "High-pressure fracture visual ribbon."));

    private static final List<Entry> RESEARCH = List.of(
            research("constructs/maintenance_protocols", "Maintenance Protocols", "Teach constructs to maintain rituals, repair wards, and move materials without duplicating held items."),
            research("constructs/sigil_constructs", "Sigil Constructs", "Build simple deterministic helpers from cores and behavior plates."),
            research("deep_veil/gatework", "Gatework", "Bind fractured alloy, memory, and warding into a gate that can withstand Deep Veil contact."),
            research("deep_veil/guardian_oath", "Guardian Oath", "A warding obelisk can call a Veilbound Guardian; defeating it proves the gate will hear you."),
            research("deep_veil/landmark_cartography", "Landmark Cartography", "Map observatories, vaults, archives, ritual circles, groves, springs, monoliths, gates, and fracture rifts as one connected Veil network."),
            research("deep_veil/observatories", "Ancient Observatories", "Find observatory markers, monoliths, archives, and deep gates."),
            research("endgame_paths/architect", "Veil Architect", "A final path for reshaping late Veil systems after boss victories."),
            research("endgame_paths/exploit", "Exploit the Veil", "Stronger outputs at the cost of higher fracture pressure and risk."),
            research("endgame_paths/final_choice", "Final Choice", "Seal, harmonize, exploit, or architect the Veil after the deepest answer."),
            research("endgame_paths/harmonize", "Harmonize with the Veil", "Efficient pressure use, recovery, and stable high-tier work."),
            research("endgame_paths/path_modifiers", "Path Modifiers", "Final path modifiers for machines, rituals, and convergence pressure."),
            research("endgame_paths/seal", "Seal the Veil", "Close fractures, lower backlash, and sacrifice some high-pressure output."),
            research("field_studies/patterns_in_common_things", "Patterns in Common Things", "Compare block, item, mob, and place observations."),
            research("field_studies/theory_crafting", "Theory Crafting", "Use the Research Desk and Theory Board to turn observations into theory pages."),
            research("fractures/cleansing", "Cleansing", "Suppress fracture pressure before it starts rewriting terrain."),
            research("fractures/containment_geometry", "Containment Geometry", "Use obelisks, seals, and stabilizers as a bounded containment grid."),
            research("fractures/fracture_detection", "Fracture Detection", "Detect unsafe fracture pressure and choose containment before it spreads."),
            research("fractures/fracture_heart", "The Fracture Heart", "Record proof that the deepest fracture pattern can be defeated."),
            research("fundamentals/first_contact", "First Contact", "Scan ordinary things until hidden resonances become visible."),
            research("fundamentals/veil_pressure", "Veil Pressure", "Read local Veil pressure and fracture pressure safely."),
            research("resonance_engineering/condensing", "Veil Condensing", "Compress ambient pressure into safe storage media."),
            research("resonance_engineering/convergence", "Convergence Matrix", "Align items, pressure, vessels, and stabilizers."),
            research("resonance_engineering/extraction", "Resonance Extraction", "Extract useful crystals from Veil-saturated material."),
            research("resonance_engineering/memory", "Thought Vessel", "Store impressions, patterns, and automation memories."),
            research("resonance_engineering/patterns", "Pattern Etching", "Inscribe sigils into plates without copying old control systems."),
            research("resonance_engineering/storage", "Resonance Storage", "Hold a stable resonance reserve for convergence work."),
            research("resonance_engineering/threads", "Resonance Threads", "Weave memory threads and machine fabric from stable resonance."),
            research("rituals/risky_invocation", "Risky Invocation", "Use controlled instability to call dangerous Veil structures into the world."),
            research("rituals/ritual_foundations", "Ritual Foundations", "Anchor a basin, place focus pedestals, and channel pressure carefully."),
            research("rituals/warding_rites", "Warding Rites", "Shape ritual pressure into persistent ward anchors and fracture seals."),
            research("warding/stabilization", "Stabilization Geometry", "Reduce convergence instability with symmetrical stabilizers and warding chalk."));

    private static final List<Entry> RESONANCE_CATEGORIES = List.of(
            resonanceCategory("beast"), resonanceCategory("bloom"), resonanceCategory("chaos"),
            resonanceCategory("decay"), resonanceCategory("flame"), resonanceCategory("frost"),
            resonanceCategory("light"), resonanceCategory("matter"), resonanceCategory("metal"),
            resonanceCategory("mind"), resonanceCategory("motion"), resonanceCategory("order"),
            resonanceCategory("shadow"), resonanceCategory("spirit"), resonanceCategory("time"),
            resonanceCategory("void"));

    private static final List<Entry> RESONANCE_ASSIGNMENTS = List.of(
            assignment("amethyst"), assignment("construct_core"), assignment("convergence_matrix"),
            assignment("copper_ingot"), assignment("deep_veil_gate"), assignment("distorted_resonance_crystal"),
            assignment("fracture_rift"), assignment("fracture_seal"), assignment("fractured_alloy"),
            assignment("fractured_stone"), assignment("glowstone_dust"), assignment("harmonic_alloy"),
            assignment("harmonic_spring"), assignment("iron_ingot"), assignment("memory_thread"),
            assignment("oak_log"), assignment("obsidian"), assignment("pure_resonance_crystal"),
            assignment("quartz"), assignment("redstone"), assignment("research_desk"),
            assignment("resonance_shard"), assignment("ritual_basin"), assignment("stabilizer_core"),
            assignment("stone"), assignment("unwritten_one"), assignment("veil_wisp"),
            assignment("warding_obelisk"), assignment("water_bucket"), assignment("zombie"));

    private static final List<Entry> RITUALS = List.of(
            ritual("construct_repair_core", "Construct Repair Core", "Repairs or prepares construct cores through Veilbound ritual work."),
            ritual("deep_veil_gate", "Deep Veil Gate", "Ritual gatework for the deepest Veil path."),
            ritual("fracture_rift_focus", "Fracture Rift Focus", "Focuses a rift into a readable but dangerous field target."),
            ritual("fracture_seal", "Fracture Seal", "Creates a seal for fracture containment."),
            ritual("harmonic_spring", "Harmonic Spring", "Calls or stabilizes harmonic spring landmark work."),
            ritual("path_attunement", "Path Attunement", "Attunes late-game path consequences."),
            ritual("pure_resonance_crystal", "Pure Resonance Crystal", "Early ritual for stable crystal creation."),
            ritual("stabilizer_core", "Stabilizer Core", "Creates stabilizer cores for convergence safety."),
            ritual("veil_monolith", "Veil Monolith", "Shapes monolith landmark work through ritual pressure."),
            ritual("warding_obelisk", "Warding Obelisk", "Raises a persistent ward anchor."));

    private static final List<Entry> CONVERGENCE = List.of(
            convergence("construct_core", "Construct Core", "Converges stabilizer, sigils, memory, and crystal into a construct core."),
            convergence("deep_veil_gate", "Deep Veil Gate", "Converges alloy, memory, and revelation into gate material."),
            convergence("fracture_seal_array", "Fracture Seal Array", "Aligns seals for larger containment geometry."),
            convergence("fractured_alloy", "Fractured Alloy", "Creates unstable alloy from fracture and harmonic materials."),
            convergence("guardian_key", "Guardian Key", "Prepares gate logic for guardian challenge flow."),
            convergence("harmonic_alloy", "Harmonic Alloy", "Creates stable alloy for safe high-tier work."),
            convergence("memory_lattice", "Memory Lattice", "Converges notes and thread into readable archive memory."),
            convergence("path_catalyst", "Path Catalyst", "Prepares late-game path catalyst work."),
            convergence("warding_anchor", "Warding Anchor", "Creates anchor material for persistent wards."));

    private static final List<Entry> MACHINE_RECIPES = List.of(
            machineRecipe("condense_distorted_crystal"), machineRecipe("condense_fracture_to_pure"),
            machineRecipe("condense_pure_crystal"), machineRecipe("condense_stabilizer_core"),
            machineRecipe("etch_fracture_safe_plate"), machineRecipe("etch_harmonic_sigil_plates"),
            machineRecipe("etch_sigil_plate"), machineRecipe("extract_resonance_from_glowstone"),
            machineRecipe("extract_resonance_from_quartz"), machineRecipe("extract_resonance_shard"),
            machineRecipe("record_thought"), machineRecipe("recover_notes_from_thread"),
            machineRecipe("stabilize_fracture_sample"), machineRecipe("weave_memory_thread"),
            machineRecipe("weave_path_thread"), machineRecipe("weave_theory_thread"));

    private static final List<Entry> FRACTURE_TRANSFORMS = List.of(
            fractureTransform("andesite"), fractureTransform("calcite"), fractureTransform("cobbled_deepslate"),
            fractureTransform("deepslate"), fractureTransform("diorite"), fractureTransform("granite"),
            fractureTransform("stone"), fractureTransform("tuff"));

    private static final List<Entry> LANDMARKS = List.of(
            landmark("abandoned_ritual_circle", "Abandoned Ritual Circle", "A field ritual ruin and early Veil network marker."),
            landmark("ancient_observatory", "Ancient Observatory", "Pre-operator observatory structure for Deep Veil work."),
            landmark("buried_research_vault", "Buried Research Vault", "Recovered archive cache for research acceleration."),
            landmark("deep_veil_gate", "Deep Veil Gate", "Late-game gate landmark and boss route focus."),
            landmark("fracture_rift", "Fracture Rift", "Fracture source landmark and containment priority."),
            landmark("fractured_grove", "Fractured Grove", "Natural area contaminated by fracture pressure."),
            landmark("harmonic_spring", "Harmonic Spring", "Stable high-resonance landmark."),
            landmark("sealed_archive", "Sealed Archive", "Forbidden archive landmark."),
            landmark("veil_monolith", "Veil Monolith", "Ancient Veil network monolith."));

    private static final List<Entry> BOSS_GATES = List.of(
            bossGate("deep_veil_gate", "Deep Veil Gate", "Gate route requiring Veilbound preparation."),
            bossGate("veilbound_guardian", "Veilbound Guardian", "Guardian proof before deeper gatework."),
            bossGate("unwritten_one", "The Unwritten One", "Forbidden archive challenge."),
            bossGate("fracture_heart", "Fracture Heart", "Endgame fracture path challenge."));

    private static final Map<String, String> PRIMARY_RESONANCE = Map.ofEntries(
            Map.entry("research_desk", "mind"),
            Map.entry("ritual_basin", "order"),
            Map.entry("convergence_matrix", "time"),
            Map.entry("fracture_rift", "chaos"),
            Map.entry("fracture_seal", "order"),
            Map.entry("deep_veil_gate", "void"),
            Map.entry("veil_wisp", "spirit"),
            Map.entry("unwritten_one", "void"),
            Map.entry("resonance_shard", "light"),
            Map.entry("pure_resonance_crystal", "order"),
            Map.entry("distorted_resonance_crystal", "chaos"));

    private VeilboundBridgeCatalog() {
    }

    public enum Kind {
        BLOCK,
        ITEM,
        ENTITY,
        PARTICLE,
        RESEARCH,
        RESONANCE_CATEGORY,
        RESONANCE_ASSIGNMENT,
        RITUAL,
        CONVERGENCE,
        MACHINE_RECIPE,
        FRACTURE_TRANSFORM,
        LANDMARK,
        BOSS_GATE
    }

    public record Entry(Kind kind, Identifier id, String title, String summary, Identifier icon, int sortOrder) {
    }

    public static List<Entry> allEntries() {
        List<Entry> entries = new ArrayList<>();
        entries.addAll(BLOCKS);
        entries.addAll(ITEMS);
        entries.addAll(ENTITIES);
        entries.addAll(PARTICLES);
        entries.addAll(RESEARCH);
        entries.addAll(RESONANCE_CATEGORIES);
        entries.addAll(RESONANCE_ASSIGNMENTS);
        entries.addAll(RITUALS);
        entries.addAll(CONVERGENCE);
        entries.addAll(MACHINE_RECIPES);
        entries.addAll(FRACTURE_TRANSFORMS);
        entries.addAll(LANDMARKS);
        entries.addAll(BOSS_GATES);
        return List.copyOf(entries);
    }

    public static List<Entry> entries(Kind kind) {
        return allEntries().stream().filter(entry -> entry.kind() == kind).toList();
    }

    public static Optional<Entry> target(Identifier id) {
        if (id == null) {
            return Optional.empty();
        }
        return allEntries().stream()
                .filter(entry -> entry.kind() == Kind.BLOCK || entry.kind() == Kind.ITEM || entry.kind() == Kind.ENTITY)
                .filter(entry -> entry.id().equals(id))
                .findFirst();
    }

    public static Optional<Entry> landmarkForBlock(Identifier blockId) {
        if (blockId == null) {
            return Optional.empty();
        }
        String path = blockId.getPath();
        if (path.endsWith("_marker")) {
            path = path.substring(0, path.length() - "_marker".length());
        }
        String targetPath = path;
        return LANDMARKS.stream().filter(entry -> entry.id().getPath().equals(targetPath)).findFirst();
    }

    public static String primaryResonance(Identifier id) {
        if (id == null) {
            return "matter";
        }
        return PRIMARY_RESONANCE.getOrDefault(id.getPath(), "matter");
    }

    public static String indexPagePath(Entry entry) {
        if (entry == null) {
            return "veilbound/unknown";
        }
        return "veilbound/" + kindPath(entry.kind()) + "/" + entryPath(entry);
    }

    public static Identifier discoveryId(Entry entry) {
        String kind = entry == null ? "unknown" : kindPath(entry.kind());
        String path = entry == null ? "unknown" : entryPath(entry);
        return Identifier.fromNamespaceAndPath("echoarcanacore", "veilbound/discovery/" + kind + "/" + path);
    }

    public static String entryPath(Entry entry) {
        if (entry == null) {
            return "unknown";
        }
        String path = sanitize(entry.id().getPath());
        String expectedPrefix = kindPath(entry.kind()).replace('-', '_');
        if (path.startsWith(expectedPrefix + "/")) {
            return path.substring(expectedPrefix.length() + 1);
        }
        return path;
    }

    public static Identifier contentId(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static String kindTitle(Kind kind) {
        return title(kindPath(kind));
    }

    public static String kindPath(Kind kind) {
        return (kind == null ? "unknown" : kind.name().toLowerCase(Locale.ROOT)).replace('_', '-');
    }

    public static String sanitize(String value) {
        String clean = value == null ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
        clean = clean.replace('\\', '/').replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
        while (clean.contains("//")) {
            clean = clean.replace("//", "/");
        }
        return clean.isBlank() ? "unknown" : clean;
    }

    public static String title(String raw) {
        String text = raw == null ? "" : raw.replace('/', ' ').replace('_', ' ').replace('-', ' ').toLowerCase(Locale.ROOT);
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
        return builder.isEmpty() ? "Unknown" : builder.toString();
    }

    private static Entry block(String path, String title, String summary) {
        return entry(Kind.BLOCK, path, title, summary, path, 100);
    }

    private static Entry item(String path, String title, String summary) {
        return entry(Kind.ITEM, path, title, summary, path, 200);
    }

    private static Entry entity(String path, String title, String summary) {
        return entry(Kind.ENTITY, path, title, summary, path + "_spawn_egg", 300);
    }

    private static Entry particle(String path, String title, String summary) {
        return entry(Kind.PARTICLE, "particle/" + path, title, summary, "veil_lens", 350);
    }

    private static Entry research(String path, String title, String summary) {
        return entry(Kind.RESEARCH, "research/" + path, title, summary, "field_journal", 400);
    }

    private static Entry resonanceCategory(String path) {
        return entry(Kind.RESONANCE_CATEGORY, "resonance_category/" + path, title(path),
                "Veilbound resonance category owned by ARCANA and mirrored into Arcane Index.", "resonance_shard", 500);
    }

    private static Entry assignment(String path) {
        return entry(Kind.RESONANCE_ASSIGNMENT, "resonance_assignment/" + path, title(path),
                "Resonance assignment used by Veil Lens scans and Field Journal research.", iconForAssignment(path), 600);
    }

    private static Entry ritual(String path, String title, String summary) {
        return entry(Kind.RITUAL, "ritual/" + path, title, summary, "ritual_basin", 700);
    }

    private static Entry convergence(String path, String title, String summary) {
        return entry(Kind.CONVERGENCE, "convergence/" + path, title, summary, "convergence_matrix", 800);
    }

    private static Entry machineRecipe(String path) {
        String machine = path.contains("condense") ? "veil_condenser"
                : path.contains("etch") ? "pattern_etcher"
                : path.contains("extract") ? "resonance_extractor"
                : path.contains("weave") ? "arcane_loom"
                : "thought_vessel";
        return entry(Kind.MACHINE_RECIPE, "machine_recipe/" + path, title(path),
                "Veilbound machine recipe mirrored into Arcane Index as official knowledge.", machine, 900);
    }

    private static Entry fractureTransform(String path) {
        return entry(Kind.FRACTURE_TRANSFORM, "fracture_transform/" + path, title(path),
                "Fracture pressure can transform " + title(path) + " into fractured stone.", "fractured_stone", 1000);
    }

    private static Entry landmark(String path, String title, String summary) {
        String icon = switch (path) {
            case "ancient_observatory", "buried_research_vault", "sealed_archive", "abandoned_ritual_circle",
                    "fractured_grove" -> path + "_marker";
            default -> path;
        };
        return entry(Kind.LANDMARK, "landmark/" + path, title, summary, icon, 1100);
    }

    private static Entry bossGate(String path, String title, String summary) {
        String icon = switch (path) {
            case "veilbound_guardian", "unwritten_one", "fracture_heart" -> path + "_spawn_egg";
            default -> path;
        };
        return entry(Kind.BOSS_GATE, "boss_gate/" + path, title, summary, icon, 1200);
    }

    private static Entry entry(Kind kind, String path, String title, String summary, String iconPath, int baseSort) {
        return new Entry(kind, contentId(path), title, summary, contentId(iconPath), baseSort + Math.floorMod(path.hashCode(), 99));
    }

    private static String iconForAssignment(String path) {
        return switch (path) {
            case "amethyst" -> "resonance_shard";
            case "copper_ingot", "iron_ingot", "glowstone_dust", "obsidian", "quartz", "redstone", "stone",
                    "water_bucket", "oak_log", "zombie" -> "veil_lens";
            case "fractured_alloy" -> "fractured_alloy_ingot";
            case "harmonic_alloy" -> "harmonic_alloy_ingot";
            default -> path;
        };
    }
}
