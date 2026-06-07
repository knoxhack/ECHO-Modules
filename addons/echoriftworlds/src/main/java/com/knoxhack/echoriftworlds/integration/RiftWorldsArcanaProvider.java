package com.knoxhack.echoriftworlds.integration;

import com.knoxhack.echoarcanacore.api.ArcanaProviderInterfaces;
import com.knoxhack.echoriftworlds.EchoRiftWorlds;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public enum RiftWorldsArcanaProvider implements ArcanaProviderInterfaces.ArcaneHoloMapProvider,
        ArcanaProviderInterfaces.ArcaneIndexProvider,
        ArcanaProviderInterfaces.GrimoireEntryProvider,
        ArcanaProviderInterfaces.ArcaneMissionProvider,
        ArcanaProviderInterfaces.TerminalArcanaProvider {
    INSTANCE;

    private static final List<Identifier> MARKERS = ids("marker/rift_crack", "marker/pocket_rift",
            "marker/anchored_rift", "marker/void_temple", "marker/ancient_library", "marker/rift_cache",
            "marker/fractured_ruin");
    private static final List<Identifier> PAGES = ids("index/riftworlds/rift_crack",
            "index/riftworlds/pocket_rift", "index/riftworlds/anchored_rift", "index/riftworlds/void_temple",
            "index/riftworlds/ancient_library", "index/riftworlds/rift_portal",
            "index/riftworlds/rift_storm", "index/riftworlds/mirror_realm",
            "index/riftworlds/fractured_island");
    private static final List<Identifier> GRIMOIRE = ids("grimoire/riftworlds/first_rift",
            "grimoire/riftworlds/pocket_worlds", "grimoire/riftworlds/dimensional_bosses");
    private static final List<Identifier> MISSIONS = ids("mission/arcana_riftworlds/first_rift",
            "mission/arcana_riftworlds/stabilization", "mission/arcana_riftworlds/pocket_worlds");

    @Override
    public Identifier id() {
        return EchoRiftWorlds.id("arcana_provider/riftworlds");
    }

    @Override
    public List<Identifier> markerIds(Player player) {
        return MARKERS;
    }

    @Override
    public List<Identifier> pageIds(Player player) {
        return PAGES;
    }

    @Override
    public List<Identifier> grimoireEntryIds(Player player) {
        return GRIMOIRE;
    }

    @Override
    public List<Identifier> missionIds(Player player) {
        return MISSIONS;
    }

    @Override
    public Map<String, String> terminalSummary(Player player) {
        return Map.of("module", "RiftWorlds", "markers_declared", Integer.toString(MARKERS.size()),
                "playable_slice", "rift_crack,pocket_rift",
                "status", "starter_rifts");
    }

    private static List<Identifier> ids(String... paths) {
        return java.util.Arrays.stream(paths).map(EchoRiftWorlds::id).toList();
    }
}
