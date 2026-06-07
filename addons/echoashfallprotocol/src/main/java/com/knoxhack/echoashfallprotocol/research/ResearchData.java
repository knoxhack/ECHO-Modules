package com.knoxhack.echoashfallprotocol.research;

import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echo.adaptercore.EchoValueIOSerializable;

import java.util.HashSet;
import java.util.Set;

/**
 * Player research point system for unlocking perks and technologies.
 * Points are earned through: Research Lab blocks, missions, POI discovery.
 */
public class ResearchData implements EchoValueIOSerializable {
    
    // Maximum research points cap
    public static final int MAX_POINTS = 1000;
    
    // Current research points
    private int points = 0;
    
    // Total points earned (lifetime, for achievements)
    private int totalEarned = 0;
    
    // Unlocked perk IDs
    private final Set<String> unlockedPerks = new HashSet<>();
    
    // Unlocked schematic categories
    private final Set<String> unlockedSchematics = new HashSet<>();
    
    public ResearchData() {
        // Start with 0 research points
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchData> STREAM_CODEC = StreamCodec.of(
            ResearchData::writeSync,
            ResearchData::readSync
    );

    private static void writeSync(RegistryFriendlyByteBuf buf, ResearchData data) {
        buf.writeVarInt(data.points);
        buf.writeVarInt(data.totalEarned);
        writeStringSet(buf, data.unlockedPerks);
        writeStringSet(buf, data.unlockedSchematics);
    }

    private static ResearchData readSync(RegistryFriendlyByteBuf buf) {
        ResearchData data = new ResearchData();
        data.points = buf.readVarInt();
        data.totalEarned = buf.readVarInt();
        readStringSet(buf, data.unlockedPerks);
        readStringSet(buf, data.unlockedSchematics);
        return data;
    }

    private static void writeStringSet(RegistryFriendlyByteBuf buf, Set<String> values) {
        buf.writeVarInt(values.size());
        for (String value : values) {
            buf.writeUtf(value);
        }
    }

    private static void readStringSet(RegistryFriendlyByteBuf buf, Set<String> values) {
        values.clear();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String value = buf.readUtf();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
    }
    
    /**
     * Get current research points
     */
    public int getPoints() {
        return points;
    }
    
    /**
     * Get total lifetime research points earned
     */
    public int getTotalEarned() {
        return totalEarned;
    }
    
    /**
     * Add research points (capped at MAX_POINTS)
     * @return Actual points added
     */
    public int addPoints(int amount) {
        if (amount <= 0) return 0;
        
        int oldPoints = points;
        points = Math.min(MAX_POINTS, points + amount);
        int added = points - oldPoints;
        
        totalEarned += added;
        return added;
    }
    
    /**
     * Consume research points
     * @return true if points were available and consumed
     */
    public boolean consumePoints(int amount) {
        if (amount <= 0) return true;
        if (points < amount) return false;
        
        points -= amount;
        return true;
    }
    
    /**
     * Set points directly (for admin/debug commands)
     */
    public void setPoints(int amount) {
        this.points = Math.max(0, Math.min(MAX_POINTS, amount));
    }
    
    /**
     * Check if player has enough points
     */
    public boolean hasPoints(int amount) {
        return points >= amount;
    }
    
    /**
     * Unlock a perk
     * @return true if newly unlocked, false if already had it
     */
    public boolean unlockPerk(String perkId) {
        return unlockedPerks.add(perkId);
    }
    
    /**
     * Check if perk is unlocked
     */
    public boolean hasPerk(String perkId) {
        return unlockedPerks.contains(perkId);
    }
    
    /**
     * Get all unlocked perks
     */
    public Set<String> getUnlockedPerks() {
        return new HashSet<>(unlockedPerks);
    }

    /**
     * Clear unlocked perks for admin/debug reset commands.
     */
    public void clearPerks() {
        unlockedPerks.clear();
    }
    
    /**
     * Unlock a schematic category
     */
    public boolean unlockSchematic(String category) {
        return unlockedSchematics.add(category);
    }
    
    /**
     * Check if schematic category is unlocked
     */
    public boolean hasSchematic(String category) {
        return unlockedSchematics.contains(category);
    }
    
    /**
     * Get all unlocked schematics
     */
    public Set<String> getUnlockedSchematics() {
        return new HashSet<>(unlockedSchematics);
    }

    /**
     * Clear unlocked schematics for admin/debug reset commands.
     */
    public void clearSchematics() {
        unlockedSchematics.clear();
    }

    /**
     * Reset all research state for admin/debug commands.
     */
    public void resetAll() {
        points = 0;
        totalEarned = 0;
        unlockedPerks.clear();
        unlockedSchematics.clear();
    }
    
    /**
     * Calculate points needed for next tier (for UI display)
     */
    public int getPointsForNextTier() {
        if (points < 50) return 50;
        if (points < 200) return 200;
        if (points < 500) return 500;
        return MAX_POINTS;
    }
    
    public int getCurrentTier() {
        if (points < 50) return 0;
        if (points < 200) return 1;
        if (points < 500) return 2;
        return 3;
    }
    
    @Override
    public void serialize(ValueOutput output) {
        output.putInt("points", points);
        output.putInt("totalEarned", totalEarned);
        
        output.putInt("perkCount", unlockedPerks.size());
        int i = 0;
        for (String perk : unlockedPerks) {
            output.putString("perk_" + i++, perk);
        }
        
        output.putInt("schematicCount", unlockedSchematics.size());
        i = 0;
        for (String schematic : unlockedSchematics) {
            output.putString("schematic_" + i++, schematic);
        }
    }
    
    @Override
    public void deserialize(ValueInput input) {
        points = input.getIntOr("points", 0);
        totalEarned = input.getIntOr("totalEarned", 0);
        
        unlockedPerks.clear();
        int perkCount = input.getIntOr("perkCount", 0);
        for (int i = 0; i < perkCount; i++) {
            String perk = input.getStringOr("perk_" + i, "");
            if (!perk.isEmpty()) unlockedPerks.add(perk);
        }
        
        unlockedSchematics.clear();
        int schematicCount = input.getIntOr("schematicCount", 0);
        for (int i = 0; i < schematicCount; i++) {
            String schematic = input.getStringOr("schematic_" + i, "");
            if (!schematic.isEmpty()) unlockedSchematics.add(schematic);
        }
    }
    
    /**
     * Get or create research data for a player
     */
    public static ResearchData get(Player player) {
        return player.getData(ModAttachments.RESEARCH_DATA.get());
    }

    public static void saveAndSync(ServerPlayer player, ResearchData research) {
        player.setData(ModAttachments.RESEARCH_DATA.get(), research);
        player.syncData(ModAttachments.RESEARCH_DATA.get());
    }

    public static void syncToClient(ServerPlayer player) {
        player.syncData(ModAttachments.RESEARCH_DATA.get());
    }
}
