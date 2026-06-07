package com.knoxhack.echoashfallprotocol.research;

import net.minecraft.world.entity.player.Player;

/**
 * Research Perk definition with effects that apply to players.
 * Three branches matching the first Ashfall faction families.
 */
public class Perk {
    
    public enum Branch {
        RADWARDEN_TECH("Radwarden Tech", "Safety and infrastructure: weapons, armor durability, machine efficiency", 0xFF4DBAF4),
        CRASHBREAK_SALVAGE("Crashbreak Salvage", "Routes and economy: better loot, cheaper trades, faster scavenging", 0xFFFFA94D),
        SPOREBOUND_BIO("Sporebound Bio", "Survival biology: radiation resistance, regeneration, mutation control", 0xFF42D67E);
        
        private final String name;
        private final String description;
        private final int color;
        
        Branch(String name, String description, int color) {
            this.name = name;
            this.description = description;
            this.color = color;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getColor() { return color; }
    }
    
    private final String id;
    private final String name;
    private final String description;
    private final Branch branch;
    private final int tier; // 1-3
    private final int cost; // 50/150/300
    private final String[] prerequisites;
    private final PerkEffect effect;
    
    public Perk(String id, String name, String description, Branch branch, int tier, 
                int cost, String[] prerequisites, PerkEffect effect) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.branch = branch;
        this.tier = tier;
        this.cost = cost;
        this.prerequisites = prerequisites != null ? prerequisites : new String[0];
        this.effect = effect;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Branch getBranch() { return branch; }
    public int getTier() { return tier; }
    public int getCost() { return cost; }
    public String[] getPrerequisites() { return prerequisites; }
    
    /**
     * Check if player meets prerequisites (has required perks unlocked)
     */
    public boolean hasPrerequisites(Player player) {
        ResearchData data = ResearchData.get(player);
        for (String prereq : prerequisites) {
            if (!data.hasPerk(prereq)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check if player can afford and meets requirements to unlock this perk
     */
    public boolean canUnlock(Player player) {
        ResearchData data = ResearchData.get(player);
        return data.hasPoints(cost) && hasPrerequisites(player);
    }
    
    /**
     * Unlock this perk for the player
     * @return true if successfully unlocked
     */
    public boolean unlock(Player player) {
        if (!canUnlock(player)) return false;
        
        ResearchData data = ResearchData.get(player);
        if (data.consumePoints(cost)) {
            data.unlockPerk(id);
            return true;
        }
        return false;
    }
    
    /**
     * Apply perk effect to player (called periodically or on relevant events)
     */
    public void applyEffect(Player player) {
        if (effect != null && ResearchData.get(player).hasPerk(id)) {
            effect.apply(player, tier);
        }
    }
    
    @FunctionalInterface
    public interface PerkEffect {
        void apply(Player player, int tier);
    }
}
