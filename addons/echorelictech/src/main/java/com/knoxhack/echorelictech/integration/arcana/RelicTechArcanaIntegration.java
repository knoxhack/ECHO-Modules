package com.knoxhack.echorelictech.integration.arcana;

import com.knoxhack.echoarcanacore.api.AetherSignalType;
import com.knoxhack.echoarcanacore.api.AetherStorage;
import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echoarcanacore.api.ArcanaProviderInterfaces;
import com.knoxhack.echoarcanacore.api.ArcaneRelicDefinition;
import com.knoxhack.echoarcanacore.api.RelicLifecycle;
import com.knoxhack.echorelictech.EchoRelicTech;
import com.knoxhack.echorelictech.api.RelicTechApi;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public enum RelicTechArcanaIntegration implements ArcanaProviderInterfaces.RelicProvider,
        ArcanaProviderInterfaces.ArcaneIndexProvider,
        ArcanaProviderInterfaces.GrimoireEntryProvider,
        ArcanaProviderInterfaces.ArcaneLensProvider,
        ArcanaProviderInterfaces.ArcaneHoloMapProvider,
        ArcanaProviderInterfaces.ArcaneMissionProvider,
        ArcanaProviderInterfaces.TerminalArcanaProvider {
    INSTANCE;

    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final List<ArcaneRelicDefinition> STARTER_RELICS = List.of(
            relic("phase_anchor", "unstable_relic", RelicLifecycle.SCANNED,
                    Set.of(AetherSignalType.RIFT_AETHER, AetherSignalType.SIGNAL_AETHER), AetherSignalType.RIFT_AETHER,
                    18.0D, 0.10D, List.of("ability/blink_recall"), List.of("ability/barrier_phase"), 2),
            relic("echo_mirror", "cursed_relic", RelicLifecycle.FORBIDDEN,
                    Set.of(AetherSignalType.SIGNAL_AETHER, AetherSignalType.SOUL_AETHER), AetherSignalType.SIGNAL_AETHER,
                    22.0D, 0.22D, List.of("ability/echo_decoy"), List.of("ability/memory_replay"), 2),
            relic("gravity_clamp", "prototype_relic", RelicLifecycle.DECODED,
                    Set.of(AetherSignalType.RAW_AETHER, AetherSignalType.SIGNAL_AETHER), AetherSignalType.SIGNAL_AETHER,
                    14.0D, 0.06D, List.of("ability/gravity_pull", "ability/gravity_push"), List.of("ability/gravity_well"), 2),
            relic("rift_lantern", "unstable_relic", RelicLifecycle.DECODED,
                    Set.of(AetherSignalType.RIFT_AETHER, AetherSignalType.REFINED_AETHER), AetherSignalType.RIFT_AETHER,
                    10.0D, 0.05D, List.of("ability/reveal_rift_traces"), List.of("ability/weaken_rift_mobs"), 1),
            relic("blood_circuit", "cursed_relic", RelicLifecycle.FORBIDDEN,
                    Set.of(AetherSignalType.CURSED_AETHER, AetherSignalType.RAW_AETHER), AetherSignalType.CURSED_AETHER,
                    28.0D, 0.35D, List.of("ability/health_to_power"), List.of("ability/blood_contract"), 1),
            relic("broken_climate_key", "broken_prototype", RelicLifecycle.SCANNED,
                    Set.of(AetherSignalType.SIGNAL_AETHER, AetherSignalType.RAW_AETHER), AetherSignalType.SIGNAL_AETHER,
                    26.0D, 0.18D, List.of("ability/weather_surge", "ability/storm_calm"), List.of("ability/ash_burst"), 1),
            relic("soul_capacitor", "ancient_battery", RelicLifecycle.DECODED,
                    Set.of(AetherSignalType.SOUL_AETHER, AetherSignalType.REFINED_AETHER), AetherSignalType.SOUL_AETHER,
                    20.0D, 0.14D, List.of("ability/soul_ward"), List.of("ability/familiar_revival"), 3),
            relic("void_compass", "dimensional_key", RelicLifecycle.DECODED,
                    Set.of(AetherSignalType.RIFT_AETHER, AetherSignalType.SIGNAL_AETHER), AetherSignalType.RIFT_AETHER,
                    16.0D, 0.16D, List.of("ability/vault_ping"), List.of("ability/deep_veil_guidance"), 1));

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        ArcanaCoreServices.registerProvider(INSTANCE);
        EchoRelicTech.LOGGER.info("ECHO: RelicTech registered Arcana Core relic provider.");
    }

    @Override
    public Identifier id() {
        return id("arcana_provider/relictech");
    }

    @Override
    public List<ArcaneRelicDefinition> relics() {
        return STARTER_RELICS;
    }

    @Override
    public List<Identifier> pageIds(Player player) {
        return STARTER_RELICS.stream().map(ArcaneRelicDefinition::indexPageId).toList();
    }

    @Override
    public List<Identifier> grimoireEntryIds(Player player) {
        return STARTER_RELICS.stream().map(ArcaneRelicDefinition::grimoirePageId).toList();
    }

    @Override
    public List<String> scanHints(Player player, Identifier targetId) {
        if (targetId == null || !EchoRelicTech.MODID.equals(targetId.getNamespace())) {
            return List.of();
        }
        return STARTER_RELICS.stream()
                .filter(relic -> relic.id().equals(targetId))
                .findFirst()
                .map(relic -> List.of(
                        "Relic lifecycle: " + relic.lifecycle().name().toLowerCase(),
                        "Instability: " + relic.instability(),
                        "Curse risk: " + relic.curseRisk()))
                .orElse(List.of());
    }

    @Override
    public List<Identifier> markerIds(Player player) {
        return List.of(id("layer/relic_vaults"), id("vault/pre_gridfall_research_vault"));
    }

    @Override
    public List<Identifier> missionIds(Player player) {
        return List.of(
                id("arcana_relictech/find_unknown_relic"),
                id("arcana_relictech/scan_unknown_relic"),
                id("arcana_relictech/decode_first_relic"),
                id("arcana_relictech/stabilize_first_relic"),
                id("arcana_relictech/use_relic_ability"),
                id("arcana_relictech/discover_cursed_relic"),
                id("arcana_relictech/recover_legendary_frame"));
    }

    @Override
    public Map<String, String> terminalSummary(Player player) {
        Map<String, String> summary = new LinkedHashMap<>();
        summary.put("module", "ECHO: RelicTech");
        summary.put("starter_relics", Integer.toString(STARTER_RELICS.size()));
        summary.put("index_pages", Integer.toString(STARTER_RELICS.size()));
        summary.put("grimoire_entries", Integer.toString(STARTER_RELICS.size()));
        if (player instanceof ServerPlayer serverPlayer) {
            summary.put("profile", RelicTechApi.getTerminalRelicSummary(serverPlayer));
        }
        return Map.copyOf(summary);
    }

    private static ArcaneRelicDefinition relic(String path, String category, RelicLifecycle lifecycle,
            Set<AetherSignalType> accepted, AetherSignalType output, double instability, double curseRisk,
            List<String> discovered, List<String> hidden, int upgradeSlots) {
        Identifier relicId = id(path);
        return new ArcaneRelicDefinition(
                relicId,
                "item.echorelictech." + path,
                category,
                lifecycle,
                new AetherStorage(0.0D, 100.0D, accepted, output, 8.0D, curseRisk),
                instability,
                curseRisk,
                discovered.stream().map(RelicIds::ability).toList(),
                hidden.stream().map(RelicIds::ability).toList(),
                upgradeSlots,
                Identifier.fromNamespaceAndPath("echoarcaneindex", "relic/" + path),
                Identifier.fromNamespaceAndPath("echogrimoire", "archive/relics/" + path));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRelicTech.MODID, path);
    }

    private static final class RelicIds {
        private static Identifier ability(String path) {
            return id(path);
        }
    }
}
