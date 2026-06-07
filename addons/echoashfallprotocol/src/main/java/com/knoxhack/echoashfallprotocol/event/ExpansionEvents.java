package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echoashfallprotocol.faction.AshfallBiomeFactions;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionContractProgression;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionMap;
import com.knoxhack.echoashfallprotocol.research.ResearchData;
import com.knoxhack.echoashfallprotocol.world.POIRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Legacy event handlers for exploration, Echo Core faction, and research progression.
 *
 * <p>This class is intentionally not self-registering. Several of these hooks have active
 * replacements elsewhere, and registering the whole legacy bundle can double-award rewards or
 * run stale survival ticks. Keep it quarantined until individual behavior is migrated on purpose.
 */
public final class ExpansionEvents {
    
    private ExpansionEvents() {
    }
    
    /**
     * Award research points and reputation for killing hostile entities
     */
    public void onEntityKill(Object event) {
        Object source = eventValue(event, "getSource");
        if (!(eventValue(source, "getEntity") instanceof ServerPlayer player)) return;
        
        Object entity = eventValue(event, "getEntity");
        if (!(entity instanceof net.minecraft.world.entity.Entity killed)) return;
        var entityName = killed.getType().getDescriptionId();
        
        // Award research points for killing certain entities
        int researchPoints = 0;
        int repGain = 0;
        Identifier faction = null;
        
        // Check for faction-aligned kills
        if (entityName.contains("rad_zombie") || entityName.contains("feral_human") || entityName.contains("ash_wraith")) {
            researchPoints = 2;
            repGain = 1;
            faction = AshfallBiomeFactions.SPOREBOUND_SANCTUM;
        } else if (entityName.contains("city_stalker") || entityName.contains("scavenger_bandit")) {
            researchPoints = 3;
            repGain = 1;
            faction = AshfallBiomeFactions.CRASHBREAK_SALVAGE;
        } else if (entityName.contains("irradiated_wolf") || entityName.contains("rust_walker")) {
            researchPoints = 2;
            repGain = 1;
            faction = AshfallBiomeFactions.RADWARDEN_COMPACT;
        }
        
        if (researchPoints > 0) {
            ResearchData research = ResearchData.get(player);
            research.addPoints(researchPoints);
            ResearchData.saveAndSync(player, research);
        }
        
        if (faction != null && repGain > 0) {
            EchoCoreServices.addFactionReputation(player, faction, repGain);
            EchoCoreServices.markFactionContacted(player, faction);
            AshfallFactionContractProgression.progressKill(player, entityName);
        }
        
        // Chance to drop schematic fragments from certain enemies
        if (entityName.contains("city_stalker") || entityName.contains("scavenger_bandit")) {
            if (player.getRandom().nextFloat() < 0.05f) { // 5% chance
                // Would drop schematic fragment item
                // Implementation would use ItemEntity spawn
            }
        }
    }
    
    /**
     * Award research points for discovering new structures/biomes
     */
    public void onPOIDiscovery(Player player, String poiId) {
        if (player instanceof ServerPlayer serverPlayer) {
            // Award research points
            ResearchData research = ResearchData.get(serverPlayer);
            research.addPoints(10);
            ResearchData.saveAndSync(serverPlayer, research);
            
            // Award reputation based on POI faction
            var poi = POIRegistry.get(poiId);
            if (poi != null && poi.getAssociatedFaction() != null) {
                EchoCoreServices.addFactionReputation(serverPlayer, poi.getAssociatedFaction(), 5);
                EchoCoreServices.markFactionContacted(serverPlayer, poi.getAssociatedFaction());
                AshfallFactionContractProgression.progressPoi(serverPlayer, poiId);
            }
            
            // Send discovery message
            String displayName = poi != null ? poi.getName() : com.knoxhack.echoashfallprotocol.world.ExplorationSiteRegistry.getOrFallback(poiId).displayName();
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "Discovered: " + displayName).withStyle(net.minecraft.ChatFormatting.GREEN));
        }
    }
    
    /**
     * Award research for activating relay stations
     */
    public void onStationActivate(ServerPlayer player) {
        ResearchData research = ResearchData.get(player);
        research.addPoints(15);
        ResearchData.saveAndSync(player, research);
        
        Identifier faction = AshfallFactionMap.forPoi("relay_station");
        EchoCoreServices.addFactionReputation(player, faction, 3);
        EchoCoreServices.markFactionContacted(player, faction);
        AshfallFactionContractProgression.progressRepair(player, "relay");
    }
    
    /**
     * Check if player is in Cryogenic Ruins biome for cold damage
     */
    public void onPlayerTick(Player player) {
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer)) return;
        
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        
        // Check if in Cryogenic Ruins biome
        if (com.knoxhack.echoashfallprotocol.world.CryogenicRuinsBiome.isInBiome(level, pos)) {
            var coldData = com.knoxhack.echoashfallprotocol.survival.ColdData.get(player);
            coldData.update(player);
        }
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
