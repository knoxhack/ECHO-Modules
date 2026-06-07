package com.knoxhack.echoashfallprotocol.faction;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreExplorationRuntime;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.research.ResearchData;
import com.knoxhack.echoashfallprotocol.world.ExplorationSiteRegistry;
import java.util.Random;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Event handlers that feed Echo Core Ashfall faction state.
 */
public class FactionEvents {
    private static final Random RANDOM = new Random();

    public static void onMissionComplete(Player player, String missionId, int difficulty) {
        ResearchData research = ResearchData.get(player);
        research.addPoints(10 + (difficulty * 5));

        Identifier helpedFaction = AshfallFactionMap.resolveFactionId(missionId);
        if (player instanceof ServerPlayer serverPlayer) {
            int reputation = 5 + difficulty;
            EchoCoreServices.addFactionReputation(serverPlayer, helpedFaction, reputation);
            AshfallAdapterCoreExplorationRuntime.reputationUpdated(
                    serverPlayer, helpedFaction, reputation, "mission_complete");
            applyDiplomaticConsequences(serverPlayer, helpedFaction, reputation);
            ResearchData.saveAndSync(serverPlayer, research);
            FactionProgressionHelper.syncMilestones(serverPlayer);
        }
    }

    private static void applyDiplomaticConsequences(ServerPlayer player, Identifier helpedFaction, int amount) {
        FactionDiplomacy diplomacy = player.getData(
                com.knoxhack.echoashfallprotocol.registry.ModAttachments.FACTION_DIPLOMACY.get());
        for (Identifier other : AshfallFactionMap.all()) {
            if (other.equals(helpedFaction)) {
                continue;
            }
            FactionDiplomacy.FactionPair pair = FactionDiplomacy.FactionPair.fromFactions(helpedFaction, other);
            if (pair == null || diplomacy.getRelation(pair) >= 0) {
                continue;
            }
            int reputationLoss = Math.max(1, amount / 3);
            EchoCoreServices.addFactionReputation(player, other, -reputationLoss);
            AshfallAdapterCoreExplorationRuntime.reputationUpdated(
                    player, other, -reputationLoss, "diplomatic_consequence");
            diplomacy.modifyRelation(pair, -1);
            if (reputationLoss >= 3) {
                player.sendSystemMessage(Component.literal("\u00A78[ECHO-7]\u00A7r "
                        + AshfallFactionMap.displayName(other) + " disapproves of your aid to "
                        + AshfallFactionMap.displayName(helpedFaction) + "."));
            }
        }
        FactionDiplomacy.saveAndSync(player, diplomacy);
    }

    public static void onPOIDiscovered(Player player, String poiId) {
        ExplorationSiteRegistry.SiteProfile profile = ExplorationSiteRegistry.getOrFallback(poiId);
        String normalizedId = profile.id();
        ResearchData research = ResearchData.get(player);
        int points = switch (normalizedId) {
            case "drop_pod" -> 5;
            case "train_yard", "survivor_cache", "crash_zone_wasteland" -> 10;
            case "bio_lab", "data_center_ruin", "ruined_cityscape" -> 15;
            case "military_vault" -> 20;
            case "reactor_ruin" -> 25;
            case "industrial_factory", "cryogenic_ruins", "subway_station", "toxic_swamp", "radiation_zone" -> 18;
            default -> Math.max(10, profile.researchPoints() / 2);
        };
        research.addPoints(points);

        Identifier discoveredFaction = profile.faction() == null
                ? AshfallFactionMap.forPoi(normalizedId)
                : profile.faction();

        if (player instanceof ServerPlayer serverPlayer) {
            EchoCoreServices.addFactionReputation(serverPlayer, discoveredFaction, 2);
            AshfallAdapterCoreExplorationRuntime.reputationUpdated(
                    serverPlayer, discoveredFaction, 2, "poi_discovered");
            EchoCoreServices.markFactionContacted(serverPlayer, discoveredFaction);
            AshfallFactionContractProgression.progressPoi(serverPlayer, normalizedId);
            FactionNpcPopulationHandler.onPoiDiscovered(serverPlayer, normalizedId);
            ResearchData.saveAndSync(serverPlayer, research);
            FactionProgressionHelper.syncMilestones(serverPlayer);
        }
    }

    public static void onLivingDeath(Object event) {
        Object source = eventValue(event, "getSource");
        if (!(eventValue(source, "getEntity") instanceof ServerPlayer player)) {
            return;
        }
        if (!(eventValue(event, "getEntity") instanceof LivingEntity killed)) {
            return;
        }

        String entityName = killed.getType().getDescriptionId();
        Identifier affectedFaction = AshfallFactionMap.forEntity(entityName);

        if (entityName.contains("feral_human") || entityName.contains("mutated") || entityName.contains("toxic")) {
            EchoCoreServices.addFactionReputation(player, AshfallBiomeFactions.SPOREBOUND_SANCTUM, -2);
            AshfallAdapterCoreExplorationRuntime.reputationUpdated(
                    player, AshfallBiomeFactions.SPOREBOUND_SANCTUM, -2, "entity_killed");
        }

        if (entityName.contains("military") || entityName.contains("soldier")) {
            maybeDropFragment(killed, ModItems.SCHEMATIC_FRAGMENT_WEAPONS.get(), ModItems.SCHEMATIC_FRAGMENT_ARMOR.get());
            EchoCoreServices.addFactionReputation(player, AshfallBiomeFactions.RADWARDEN_COMPACT, -1);
            AshfallAdapterCoreExplorationRuntime.reputationUpdated(
                    player, AshfallBiomeFactions.RADWARDEN_COMPACT, -1, "entity_killed");
        }

        if (entityName.contains("scavenger") || entityName.contains("bandit")) {
            maybeDropFragment(killed, ModItems.SCHEMATIC_FRAGMENT_MACHINES.get(), ModItems.SCHEMATIC_FRAGMENT_MEDICAL.get());
        }

        AshfallFactionContractProgression.progressKill(player, entityName);
        FactionProgressionHelper.syncMilestones(player, affectedFaction);
    }

    private static void maybeDropFragment(LivingEntity entity, net.minecraft.world.item.Item first,
            net.minecraft.world.item.Item second) {
        if (RANDOM.nextFloat() >= 0.05F) {
            return;
        }
        ItemStack fragment = new ItemStack(RANDOM.nextBoolean() ? first : second);
        var itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                entity.level(), entity.getX(), entity.getY(), entity.getZ(), fragment);
        entity.level().addFreshEntity(itemEntity);
    }

    public static void onBlockBreak(Object event) {
        if (!(eventValue(event, "getPlayer") instanceof Player player)) {
            return;
        }
        if (!(eventValue(event, "getState") instanceof BlockState state)) {
            return;
        }
        if (state.getBlock() instanceof com.knoxhack.echoashfallprotocol.block.ResearchLabBlock
                && !player.level().isClientSide()) {
            // Research lab passive-use hooks live in the research system; this event keeps the extension point active.
        }
    }

    public static void onPlayerClone(Object event) {
        // Native attachment hosts own copied Ashfall faction data.
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
