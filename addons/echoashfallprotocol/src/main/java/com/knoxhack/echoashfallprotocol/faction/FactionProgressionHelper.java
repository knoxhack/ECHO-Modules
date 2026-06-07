package com.knoxhack.echoashfallprotocol.faction;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.research.ResearchData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Applies early rewards from Echo Core Ashfall standing.
 */
public final class FactionProgressionHelper {
    private FactionProgressionHelper() {
    }

    public static void syncMilestones(ServerPlayer player) {
        for (Identifier factionId : AshfallFactionMap.all()) {
            syncMilestones(player, factionId);
        }
    }

    public static void syncMilestones(ServerPlayer player, Identifier factionId) {
        QuestData quest = QuestData.get(player);
        ResearchData research = ResearchData.get(player);
        int reputation = EchoCoreServices.factionProfile(player, factionId)
                .map(profile -> profile.reputation())
                .orElse(0);
        String prefix = "faction_" + factionId.getPath();
        String displayName = AshfallFactionMap.displayName(factionId);

        if (reputation >= 25 && !quest.isAssetDiscovered(prefix + "_safehouse")) {
            quest.discoverAsset(prefix + "_safehouse");
            String schematic = schematicFor(factionId);
            if (!research.hasSchematic(schematic)) {
                research.unlockSchematic(schematic);
                ResearchData.saveAndSync(player, research);
            }
            player.sendSystemMessage(Component.literal("\u00A76[ECHO-7]\u00A7r " + displayName
                    + " allied status reached. " + schematic + " schematics archived."));
        }

        if (reputation >= 50 && !quest.isAssetDiscovered(prefix + "_priority")) {
            quest.discoverAsset(prefix + "_priority");
            player.sendSystemMessage(Component.literal("\u00A7b[ECHO-7]\u00A7r " + displayName
                    + " priority status granted. Supply lanes opened."));
        }

        if (reputation >= 75 && !quest.isAssetDiscovered(prefix + "_elite")) {
            quest.discoverAsset(prefix + "_elite");
            player.sendSystemMessage(Component.literal("\u00A7d[ECHO-7]\u00A7r Elite standing with "
                    + displayName + " confirmed. High-value contracts unlocked."));
        }

        if (reputation >= 100 && !quest.isAssetDiscovered(prefix + "_command")) {
            quest.discoverAsset(prefix + "_command");
            player.sendSystemMessage(Component.literal("\u00A7a[ECHO-7]\u00A7r " + displayName
                    + " command-tier support active."));
        }

        QuestData.saveAndSync(player, quest);
    }

    private static String schematicFor(Identifier factionId) {
        Identifier canonical = AshfallFactionMap.canonicalOrDefault(factionId);
        if (AshfallBiomeFactions.RADWARDEN_COMPACT.equals(canonical)) {
            return "weapons";
        }
        if (AshfallBiomeFactions.CRASHBREAK_SALVAGE.equals(canonical)) {
            return "machines";
        }
        return "medical";
    }
}
