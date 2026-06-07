package com.knoxhack.echoritualcore.integration.arcana;

import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echoarcanacore.api.ArcanaProviderInterfaces;
import com.knoxhack.echoarcanacore.api.RitualDefinition;
import com.knoxhack.echoarcanacore.api.RitualFamily;
import com.knoxhack.echoritualcore.EchoRitualCore;
import com.knoxhack.echoritualcore.registry.ModBlocks;
import com.knoxhack.echoritualcore.registry.ModItems;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public enum RitualCoreArcanaIntegration implements ArcanaProviderInterfaces.RitualProvider,
        ArcanaProviderInterfaces.ArcaneIndexProvider,
        ArcanaProviderInterfaces.GrimoireEntryProvider,
        ArcanaProviderInterfaces.ArcaneLensProvider,
        ArcanaProviderInterfaces.ArcaneHoloMapProvider,
        ArcanaProviderInterfaces.ArcaneMissionProvider,
        ArcanaProviderInterfaces.TerminalArcanaProvider {
    INSTANCE;

    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private static final List<RitualDefinition> RITUALS = List.of(
            ritual("aether_calibration", RitualFamily.STABILIZATION, 12.0D, 0.5D,
                    List.of(id("aether_chalk")), List.of("time:any"), "Calibrates a ritual circuit and teaches safe altar diagnostics."),
            ritual("relic_stabilization", RitualFamily.RELIC_AWAKENING, 35.0D, 2.0D,
                    List.of(id("stability_seal")), List.of("center:basic_altar", "focus:identified_relic"),
                    "Stabilizes a damaged or corrupted RelicTech relic without deleting the held stack."),
            ritual("curse_cleansing_i", RitualFamily.CURSE_CLEANSING, 28.0D, 1.5D,
                    List.of(id("purity_catalyst")), List.of("center:basic_altar", "focus:corrupted_relic"),
                    "Cleanses active relic corruption and prepares future CurseCore stage reduction."),
            ritual("spell_core_awakening", RitualFamily.SPELL_ENGRAVING, 45.0D, 3.0D,
                    List.of(id("ritual_focus"), id("refined_aether_sample")), List.of("center:basic_altar", "array:complete"),
                    "Awakens a starter spell core shell for the future SpellCore focus path."),
            ritual("rift_crack_reveal", RitualFamily.RIFT_OPENING, 40.0D, 4.0D,
                    List.of(id("refined_aether_sample"), id("aether_chalk")), List.of("center:basic_altar", "array:complete"),
                    "Generates an imprecise HoloMap rift trace and Lens follow-up target."));

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        ArcanaCoreServices.registerProvider(INSTANCE);
        EchoRitualCore.LOGGER.info("ECHO: RitualCore registered Arcana Core ritual provider.");
    }

    @Override
    public Identifier id() {
        return id("arcana_provider/ritualcore");
    }

    @Override
    public List<RitualDefinition> rituals() {
        return RITUALS;
    }

    @Override
    public List<Identifier> pageIds(Player player) {
        return RITUALS.stream().map(RitualDefinition::indexPageId).toList();
    }

    @Override
    public List<Identifier> grimoireEntryIds(Player player) {
        return RITUALS.stream().map(RitualDefinition::grimoirePageId).toList();
    }

    @Override
    public List<String> scanHints(Player player, Identifier targetId) {
        if (targetId == null || !EchoRitualCore.MODID.equals(targetId.getNamespace())) {
            return List.of();
        }
        return switch (targetId.getPath()) {
            case "basic_altar" -> List.of("Ritual center: basic altar", "Complete array: four Rune Circles plus one Offering Pedestal.",
                    "Use Aether Chalk, Ritual Focus, Refined Aether Sample, or RelicTech relics as focus items.");
            case "offering_pedestal" -> List.of("Ritual input node", "Stores one item for nearby Basic Altar rituals.");
            case "stability_pylon" -> List.of("Stability support", "Improves altar diagnostics and future instability checks.");
            case "corrupted_altar" -> List.of("Forbidden ritual center", "Backlash risk high; safe-read guidance required.");
            default -> List.of();
        };
    }

    @Override
    public List<Identifier> markerIds(Player player) {
        return List.of(id("map/ritual_site"), id("map/rift_hint"));
    }

    @Override
    public List<Identifier> missionIds(Player player) {
        return List.of(
                id("arcana_ritualcore/build_basic_altar"),
                id("arcana_ritualcore/complete_aether_calibration"),
                id("arcana_ritualcore/complete_relic_stabilization"),
                id("arcana_ritualcore/cleanse_cursed_relic"),
                id("arcana_ritualcore/awaken_spell_core"),
                id("arcana_ritualcore/reveal_rift_crack"),
                id("arcana_ritualcore/prevent_ritual_backlash"));
    }

    @Override
    public Map<String, String> terminalSummary(Player player) {
        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("module", "ECHO: RitualCore");
        summary.put("registered_rituals", Integer.toString(RITUALS.size()));
        summary.put("playable_rituals", "5");
        summary.put("active_center", "basic_altar");
        summary.put("required_array", "four_rune_circles_plus_pedestal");
        summary.put("relictech_bridge", "relic_stabilization, curse_cleansing_i");
        summary.put("holomap_bridge", "ritual_sites, rift_crack_reveal");
        return Map.copyOf(summary);
    }

    private static RitualDefinition ritual(String path, RitualFamily family, double aether, double instability,
            List<Identifier> requiredItems, List<String> conditions, String summary) {
        return new RitualDefinition(
                id(path),
                "ritual.echoritualcore." + path,
                family,
                "basic_altar",
                id("structure/" + path),
                id("basic_altar"),
                List.of(id("offering_pedestal")),
                requiredItems,
                List.of(),
                aether,
                List.of(),
                conditions,
                null,
                List.of(id("rune_circle")),
                List.of(summary),
                instability,
                id("failure/" + path + "_backlash"),
                id("effect/" + path),
                0.0D,
                path.contains("rift") ? 0.5D : 0.0D,
                path.contains("curse") ? 0.02D : 0.0D,
                Identifier.fromNamespaceAndPath("echoarcaneindex", "ritual/" + path),
                Identifier.fromNamespaceAndPath("echogrimoire", "archive/rituals/" + path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRitualCore.MODID, path);
    }
}
