package com.knoxhack.echoarcanacore.api;

import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class ArcanaProviderInterfaces {
    private ArcanaProviderInterfaces() {
    }

    public interface TerminalArcanaProvider {
        Identifier id();
        Map<String, String> terminalSummary(Player player);
    }

    public interface ArcaneIndexProvider {
        Identifier id();
        List<Identifier> pageIds(Player player);
    }

    public interface ArcaneLensProvider {
        Identifier id();
        List<String> scanHints(Player player, Identifier targetId);
    }

    public interface ArcaneHoloMapProvider {
        Identifier id();
        List<Identifier> markerIds(Player player);
    }

    public interface ArcaneMissionProvider {
        Identifier id();
        List<Identifier> missionIds(Player player);
    }

    public interface GrimoireEntryProvider {
        Identifier id();
        List<Identifier> grimoireEntryIds(Player player);
    }

    public interface VeilboundRuntimeProvider {
        Identifier id();
        VeilboundRuntimeSnapshot snapshot(Player player);
    }

    public interface ArcaneProgressionProvider {
        Identifier id();
        boolean record(ServerPlayer player, ArcaneProgressionHook hook, Identifier subject);
    }

    public interface RelicProvider {
        Identifier id();
        List<ArcaneRelicDefinition> relics();
    }

    public interface SpellProvider {
        Identifier id();
        List<SpellDefinition> spells();
    }

    public interface RitualProvider {
        Identifier id();
        List<RitualDefinition> rituals();
    }

    public interface CurseProvider {
        Identifier id();
        List<CurseDefinition> curses();
    }

    public interface FamiliarProvider {
        Identifier id();
        List<Identifier> familiarIds(Player player);
    }

    public interface AetherMachineProvider {
        Identifier id();
        List<Identifier> machineIds();
    }

    public interface VeilboundBridgeProvider {
        Identifier id();
        List<Identifier> blockIds();
        List<Identifier> itemIds();
        List<Identifier> entityIds();
        List<Identifier> particleIds();
        List<Identifier> researchIds();
        List<Identifier> resonanceCategories();
        List<Identifier> resonanceAssignments();
        List<Identifier> ritualIds();
        List<Identifier> convergenceIds();
        List<Identifier> machineRecipeIds();
        List<Identifier> fractureTransformIds();
        List<Identifier> pressureSources(Player player);
        List<Identifier> landmarkIds(Player player);
        List<Identifier> bossGateIds(Player player);
    }
}
