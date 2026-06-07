package com.knoxhack.echoashfallprotocol.world;

import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.research.ResearchData;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Point of Interest (POI) compatibility data for exploration sites.
 * Player state is stored in QuestData; static POIData instances are profiles only.
 */
public class POIData {
    
    public enum POIType {
        FACTION_HUB,        // Faction-specific safe zone with NPCs
        WORLD_LOCATION,     // Discoverable location
        FAST_TRAVEL,        // Relay station for radio network
        TUTORIAL,           // Teaching/showcase location
        RESOURCE_SITE       // Mining/gathering focused
    }
    
    public enum DangerLevel {
        SAFE(0xFF42D67E, "Safe"),
        MEDIUM(0xFFFFA94D, "Medium"),
        HIGH(0xFFFF6633, "High"),
        CRITICAL(0xFFFF3333, "Critical"),
        EXTREME(0xFFAA0000, "Extreme");
        
        private final int color;
        private final String displayName;
        
        DangerLevel(int color, String displayName) {
            this.color = color;
            this.displayName = displayName;
        }
        
        public int getColor() { return color; }
        public String getDisplayName() { return displayName; }
    }
    
    private final String id;
    private final String name;
    private final String description;
    private final POIType type;
    private final DangerLevel danger;
    private final Identifier associatedFaction; // null for neutral
    private final String[] lootDescriptions;
    private final String[] features;
    private final int researchPoints;
    private final boolean hasFastTravel;
    private final String[] requiredGear;
    
    public POIData(String id, String name, String description, POIType type, 
                   DangerLevel danger, Identifier faction,
                   String[] lootDescriptions, String[] features,
                   int researchPoints, boolean hasFastTravel, String[] requiredGear) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.danger = danger;
        this.associatedFaction = faction;
        this.lootDescriptions = lootDescriptions;
        this.features = features;
        this.researchPoints = researchPoints;
        this.hasFastTravel = hasFastTravel;
        this.requiredGear = requiredGear;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public POIType getType() { return type; }
    public DangerLevel getDangerLevel() { return danger; }
    public Identifier getAssociatedFaction() { return associatedFaction; }
    public String[] getLootDescriptions() { return lootDescriptions; }
    public String[] getFeatures() { return features; }
    public int getResearchPoints() { return researchPoints; }
    public boolean hasFastTravel() { return hasFastTravel; }
    public String[] getRequiredGear() { return requiredGear; }
    
    public boolean isDiscovered() { return false; }
    public boolean isFastTravelUnlocked() { return false; }
    
    public void discover(Player player) {
        String siteId = ExplorationSiteRegistry.normalize(id);
        QuestData quest = QuestData.get(player);
        if (!quest.isPOIDiscovered(siteId)) {
            quest.discoverPOI(siteId);
            quest.recordPOIState(siteId, QuestData.POIObjectiveState.SCANNED);
            // Award research points on first discovery
            if (player instanceof ServerPlayer serverPlayer) {
                ResearchData research = ResearchData.get(player);
                research.addPoints(researchPoints);
                ResearchData.saveAndSync(serverPlayer, research);
                QuestData.saveAndSync(serverPlayer, quest);
            }
            
            // Notify player
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("Discovered: " + name)
                    .withStyle(net.minecraft.ChatFormatting.GREEN)
            );
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("+" + researchPoints + " Research Points")
                    .withStyle(net.minecraft.ChatFormatting.YELLOW)
            );
        }
    }
    
    public void unlockFastTravel(Player player) {
        QuestData quest = QuestData.get(player);
        String siteId = ExplorationSiteRegistry.normalize(id);
        if (hasFastTravel && quest.isPOIDiscovered(siteId)
                && !quest.hasPOIState(siteId, QuestData.POIObjectiveState.REWARD_CLAIMED)) {
            quest.recordPOIState(siteId, QuestData.POIObjectiveState.REWARD_CLAIMED);
            if (player instanceof ServerPlayer serverPlayer) {
                QuestData.saveAndSync(serverPlayer, quest);
            }
            player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal("Fast Travel unlocked: " + name)
                    .withStyle(net.minecraft.ChatFormatting.AQUA)
            );
        }
    }
    
    /**
     * Check if player meets requirements to safely enter
     */
    public boolean canEnterSafely(Player player) {
        String siteId = ExplorationSiteRegistry.normalize(id);
        ExplorationSiteRegistry.SiteProfile profile = ExplorationSiteRegistry.getOrFallback(siteId);
        if (profile.dangerLevel().ordinal() <= DangerLevel.MEDIUM.ordinal()) {
            return true;
        }
        if (!QuestData.get(player).isPOIDiscovered(siteId)) {
            return false;
        }

        boolean hasFilter = player.getInventory().contains(new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get()))
                || player.getInventory().contains(new ItemStack(ModItems.FILTER_CARTRIDGE_ADVANCED.get()))
                || player.getInventory().contains(new ItemStack(ModItems.FILTER_CARTRIDGE_ELITE.get()));
        boolean hasWater = player.getInventory().contains(new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get()));
        boolean hasMedical = player.getInventory().contains(new ItemStack(ModItems.BANDAGE.get()))
                || player.getInventory().contains(new ItemStack(ModItems.RAD_AWAY.get()))
                || player.getInventory().contains(new ItemStack(ModItems.STIM_PACK.get()));

        return switch (profile.hazardProfile()) {
            case TOXIC_AIR -> hasFilter && hasWater && hasMedical;
            case RADIATION, NEXUS_ANOMALY -> hasWater && hasMedical;
            case CRYO_COLD -> hasWater && hasMedical;
            case COMBAT, URBAN_COMBAT -> hasWater && hasMedical;
            default -> hasWater || hasMedical;
        };
    }
    
    /**
     * Get difficulty stars for display (1-5)
     */
    public int getDifficultyStars() {
        return switch (danger) {
            case SAFE -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
            case EXTREME -> 5;
        };
    }
}
