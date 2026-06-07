package com.knoxhack.echoaetherworks.integration;

import com.knoxhack.echoaetherworks.EchoAetherWorks;
import com.knoxhack.echoarcanacore.api.ArcanaProviderInterfaces;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public enum AetherWorksArcanaProvider implements ArcanaProviderInterfaces.AetherMachineProvider,
        ArcanaProviderInterfaces.ArcaneIndexProvider,
        ArcanaProviderInterfaces.GrimoireEntryProvider,
        ArcanaProviderInterfaces.ArcaneMissionProvider,
        ArcanaProviderInterfaces.TerminalArcanaProvider {
    INSTANCE;

    private static final List<Identifier> MACHINES = ids("aether_condenser", "aether_cell", "aether_conduit",
            "crystal_reactor", "arcane_fabricator", "relic_charger", "spell_engraver",
            "ritual_automator", "aether_monitor", "purification_matrix");
    private static final List<Identifier> PAGES = ids("index/aetherworks/aether_condenser",
            "index/aetherworks/aether_cell", "index/aetherworks/aether_conduit",
            "index/aetherworks/crystal_reactor", "index/aetherworks/arcane_fabricator",
            "index/aetherworks/relic_charger", "index/aetherworks/spell_engraver",
            "index/aetherworks/ritual_automator", "index/aetherworks/aether_overload",
            "index/aetherworks/contamination");
    private static final List<Identifier> GRIMOIRE = ids("grimoire/aetherworks/first_aether",
            "grimoire/aetherworks/contamination", "grimoire/aetherworks/overload_control");
    private static final List<Identifier> MISSIONS = ids("mission/arcana_aetherworks/first_aether",
            "mission/arcana_aetherworks/storage", "mission/arcana_aetherworks/overload_control");

    @Override
    public Identifier id() {
        return EchoAetherWorks.id("arcana_provider/aetherworks");
    }

    @Override
    public List<Identifier> machineIds() {
        return MACHINES;
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
        return Map.of("module", "AetherWorks", "machines_declared", Integer.toString(MACHINES.size()),
                "playable_slice", "aether_condenser,aether_cell,aether_conduit",
                "status", "starter_network");
    }

    private static List<Identifier> ids(String... paths) {
        return java.util.Arrays.stream(paths).map(EchoAetherWorks::id).toList();
    }
}
