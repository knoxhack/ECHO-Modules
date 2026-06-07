package com.knoxhack.echofamiliarcore.integration;

import com.knoxhack.echoarcanacore.api.ArcanaProviderInterfaces;
import com.knoxhack.echofamiliarcore.EchoFamiliarCore;
import com.knoxhack.echofamiliarcore.api.FamiliarCoreApi;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public enum FamiliarCoreArcanaProvider implements ArcanaProviderInterfaces.FamiliarProvider,
        ArcanaProviderInterfaces.ArcaneIndexProvider,
        ArcanaProviderInterfaces.GrimoireEntryProvider,
        ArcanaProviderInterfaces.ArcaneMissionProvider,
        ArcanaProviderInterfaces.TerminalArcanaProvider {
    INSTANCE;

    private static final List<Identifier> FAMILIARS = ids("familiar/aether_wisp", "familiar/spirit_drone",
            "familiar/ash_hound", "familiar/crystal_sprite", "familiar/soul_moth",
            "familiar/storm_raven", "familiar/cursed_imp");
    private static final List<Identifier> PAGES = ids("index/familiarcore/aether_wisp",
            "index/familiarcore/spirit_drone", "index/familiarcore/ash_hound",
            "index/familiarcore/crystal_sprite", "index/familiarcore/storm_raven",
            "index/familiarcore/soul_moth", "index/familiarcore/familiar_bond",
            "index/familiarcore/familiar_commands", "index/familiarcore/familiar_evolution",
            "index/familiarcore/cursed_familiars");
    private static final List<Identifier> GRIMOIRE = ids("grimoire/familiarcore/first_bond",
            "grimoire/familiarcore/commands", "grimoire/familiarcore/cursed_familiars");
    private static final List<Identifier> MISSIONS = ids("mission/arcana_familiarcore/first_bond",
            "mission/arcana_familiarcore/commands", "mission/arcana_familiarcore/ascension");

    @Override
    public Identifier id() {
        return EchoFamiliarCore.id("arcana_provider/familiarcore");
    }

    @Override
    public List<Identifier> familiarIds(Player player) {
        return FAMILIARS;
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
        return Map.of("module", "FamiliarCore", "familiars_declared", Integer.toString(FAMILIARS.size()),
                "active_bond", FamiliarCoreApi.summary(player), "status", "starter_bonds");
    }

    private static List<Identifier> ids(String... paths) {
        return java.util.Arrays.stream(paths).map(EchoFamiliarCore::id).toList();
    }
}
