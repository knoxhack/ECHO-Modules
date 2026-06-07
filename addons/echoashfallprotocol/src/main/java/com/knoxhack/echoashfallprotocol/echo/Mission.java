package com.knoxhack.echoashfallprotocol.echo;

import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * Represents a single mission in the ECHO-7 quest system.
 * Missions have natural-feeling descriptions and reward the player upon completion.
 * Enhanced with item requirements, objective icons, and visual rewards.
 * 
 * Extended with block placement, entity kill, and location visit requirements
 * for richer mission objectives with full UI checklist integration.
 */
public record Mission(
        String id,
        String echoMessage,       // What ECHO-7 says when assigning
        String objectiveText,     // Short objective for HUD
        String completionMessage, // What ECHO-7 says on completion
        List<ItemStack> rewards,  // Items given on completion
        Predicate<Player> completionCheck,
        // New fields for enhanced UI
        List<ItemStack> requiredItems,  // Items needed to complete (for crafting missions)
        Identifier objectiveIcon, // Custom icon for this mission type
        MissionCategory category,         // SURVIVAL, CRAFTING, EXPLORATION, COMBAT, TECH
        Difficulty difficulty,            // TRIVIAL, EASY, NORMAL, HARD, EXTREME
        // Full feature additions
        List<String> prerequisites,       // Mission IDs that must be completed first
        boolean isTurnInMission,          // Requires manual item delivery vs auto-complete
        String craftingRecipeId,          // Reference to recipe for visualization
        // Extended requirement types
        List<BlockRequirement> requiredBlocks,        // Blocks that must be placed nearby
        List<EntityKillRequirement> requiredEntityKills,  // Entities to kill
        List<LocationRequirement> requiredLocations,   // Biomes/POIs to visit
        List<EquipmentRequirement> requiredEquipment,  // Gear that must be equipped, not consumed
        PostNexusData.NexusPath requiredPath           // Optional post-Nexus branch gate
) {
    /**
     * Convenience constructor - defaults prerequisites to empty, isTurnInMission to false,
     * craftingRecipeId to null, and extended requirements to empty.
     * Used by most mission definitions that don't need prereq chaining or turn-in flow.
     */
    public Mission(
            String id,
            String echoMessage,
            String objectiveText,
            String completionMessage,
            List<ItemStack> rewards,
            Predicate<Player> completionCheck,
            List<ItemStack> requiredItems,
            Identifier objectiveIcon,
            MissionCategory category,
            Difficulty difficulty
    ) {
        this(id, echoMessage, objectiveText, completionMessage, rewards, completionCheck,
                requiredItems, objectiveIcon, category, difficulty,
                Collections.emptyList(), false, null,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                PostNexusData.NexusPath.NONE);
    }
    
    /**
     * Constructor with all fields including extended requirements.
     */
    public Mission(
            String id,
            String echoMessage,
            String objectiveText,
            String completionMessage,
            List<ItemStack> rewards,
            Predicate<Player> completionCheck,
            List<ItemStack> requiredItems,
            Identifier objectiveIcon,
            MissionCategory category,
            Difficulty difficulty,
                List<String> prerequisites,
                boolean isTurnInMission,
                String craftingRecipeId,
                List<BlockRequirement> requiredBlocks,
                List<EntityKillRequirement> requiredEntityKills,
                List<LocationRequirement> requiredLocations
    ) {
        this(id, echoMessage, objectiveText, completionMessage, rewards, completionCheck,
                requiredItems, objectiveIcon, category, difficulty,
                prerequisites, isTurnInMission, craftingRecipeId,
                requiredBlocks, requiredEntityKills, requiredLocations, Collections.emptyList(),
                PostNexusData.NexusPath.NONE);
    }

    /**
     * Constructor with all fields including extended requirements.
     */
    public Mission(
            String id,
            String echoMessage,
            String objectiveText,
            String completionMessage,
            List<ItemStack> rewards,
            Predicate<Player> completionCheck,
            List<ItemStack> requiredItems,
            Identifier objectiveIcon,
            MissionCategory category,
            Difficulty difficulty,
            List<String> prerequisites,
            boolean isTurnInMission,
            String craftingRecipeId,
            List<BlockRequirement> requiredBlocks,
            List<EntityKillRequirement> requiredEntityKills,
            List<LocationRequirement> requiredLocations,
            List<EquipmentRequirement> requiredEquipment
    ) {
        this(id, echoMessage, objectiveText, completionMessage, rewards, completionCheck,
                requiredItems, objectiveIcon, category, difficulty,
                prerequisites, isTurnInMission, craftingRecipeId,
                requiredBlocks, requiredEntityKills, requiredLocations, requiredEquipment,
                PostNexusData.NexusPath.NONE);
    }

    /**
     * Constructor with all fields including optional post-Nexus path gating.
     */
    public Mission(
            String id,
            String echoMessage,
            String objectiveText,
            String completionMessage,
            List<ItemStack> rewards,
            Predicate<Player> completionCheck,
            List<ItemStack> requiredItems,
            Identifier objectiveIcon,
            MissionCategory category,
            Difficulty difficulty,
            List<String> prerequisites,
            boolean isTurnInMission,
            String craftingRecipeId,
            List<BlockRequirement> requiredBlocks,
            List<EntityKillRequirement> requiredEntityKills,
            List<LocationRequirement> requiredLocations,
            List<EquipmentRequirement> requiredEquipment,
            PostNexusData.NexusPath requiredPath
    ) {
        this.id = id;
        this.echoMessage = cleanMissionText(echoMessage);
        this.objectiveText = cleanObjectiveText(objectiveText);
        this.completionMessage = cleanMissionText(completionMessage);
        this.rewards = rewards != null ? rewards : Collections.emptyList();
        this.completionCheck = completionCheck;
        this.requiredItems = requiredItems != null ? requiredItems : Collections.emptyList();
        this.objectiveIcon = objectiveIcon;
        this.category = category;
        this.difficulty = difficulty;
        this.prerequisites = prerequisites != null ? prerequisites : Collections.emptyList();
        this.isTurnInMission = isTurnInMission;
        this.craftingRecipeId = craftingRecipeId;
        this.requiredBlocks = requiredBlocks != null ? requiredBlocks : Collections.emptyList();
        this.requiredEntityKills = requiredEntityKills != null ? requiredEntityKills : Collections.emptyList();
        this.requiredLocations = requiredLocations != null ? requiredLocations : Collections.emptyList();
        this.requiredEquipment = requiredEquipment != null ? requiredEquipment : Collections.emptyList();
        this.requiredPath = requiredPath != null ? requiredPath : PostNexusData.NexusPath.NONE;
    }

    private static String cleanObjectiveText(String value) {
        String text = cleanMissionText(value)
                .replace(" + Turn In", "")
                .replace("+ Turn In", "")
                .replace(" + turn in", "")
                .replace("+ turn in", "")
                .trim();
        return text.endsWith("+") ? text.substring(0, text.length() - 1).trim() : text;
    }

    private static String cleanMissionText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .replace("fallback command channel", "terminal operation channel")
                .replace("fallback support", "backup support")
                .replace("fallback recon", "backup recon")
                .replace("fallback point", "recovery point")
                .replace("fallback active", "failsafe active")
                .replace("Fallback commands remain:", "Terminal controls remain armed:")
                .trim();
    }
    
    // Legacy constructor for backward compatibility
    public Mission(
            String id,
            String echoMessage,
            String objectiveText,
            String completionMessage,
            List<ItemStack> rewards,
            Predicate<Player> completionCheck,
            List<ItemStack> requiredItems,
            Identifier objectiveIcon,
            MissionCategory category,
            Difficulty difficulty,
            List<String> prerequisites,
            boolean isTurnInMission,
            String craftingRecipeId
    ) {
        this(id, echoMessage, objectiveText, completionMessage, rewards, completionCheck,
                requiredItems, objectiveIcon, category, difficulty,
                prerequisites, isTurnInMission, craftingRecipeId,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                PostNexusData.NexusPath.NONE);
    }

    public Mission(
            String id,
            String echoMessage,
            String objectiveText,
            String completionMessage,
            List<ItemStack> rewards,
            Predicate<Player> completionCheck,
            List<ItemStack> requiredItems,
            Identifier objectiveIcon,
            MissionCategory category,
            Difficulty difficulty,
            List<String> prerequisites,
            boolean isTurnInMission,
            String craftingRecipeId,
            PostNexusData.NexusPath requiredPath
    ) {
        this(id, echoMessage, objectiveText, completionMessage, rewards, completionCheck,
                requiredItems, objectiveIcon, category, difficulty,
                prerequisites, isTurnInMission, craftingRecipeId,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                requiredPath);
    }

    public boolean isComplete(Player player) {
        return completionCheck.test(player);
    }
    
    /**
     * Get the primary objective item (first required item or first reward)
     */
    public ItemStack getObjectiveItem() {
        if (requiredItems != null && !requiredItems.isEmpty()) {
            return requiredItems.get(0);
        }
        if (rewards != null && !rewards.isEmpty()) {
            return rewards.get(0);
        }
        return ItemStack.EMPTY;
    }
    
    /**
     * Check if this is a crafting mission (has required items)
     */
    public boolean isCraftingMission() {
        return requiredItems != null && !requiredItems.isEmpty();
    }

    /**
     * Item requirements are validated only for delivery-style missions.
     * Turn-ins do not consume inventory; this keeps the checklist as proof of progress.
     * Block placement missions use requiredItems as recipe/reference data and
     * validate the placed machine instead.
     */
    public boolean validatesRequiredItems() {
        return isCraftingMission() && !hasBlockRequirements();
    }

    public boolean consumesRequiredItems() {
        return false;
    }
    
    /**
     * Get progress text for display across each distinct required item.
     */
    public String getProgressText(Player player) {
        if (!isCraftingMission()) return "";

        List<String> parts = new ArrayList<>();
        for (ItemProgress progress : getItemProgress(player)) {
            int needed = Math.max(1, progress.need());
            int have = Math.min(needed, progress.have());
            parts.add(have + "/" + needed + " " + progress.item().getHoverName().getString());
        }
        return String.join(", ", parts);
    }

    /**
     * Get progress percentage (0.0 to 1.0) - averaged across all required items.
     */
    public float getProgress(Player player) {
        if (!isCraftingMission()) return isComplete(player) ? 1.0f : 0.0f;
        if (requiredItems.isEmpty()) return 0.0f;

        float total = 0f;
        for (ItemStack required : requiredItems) {
            int need = Math.max(1, required.getCount());
            int have = countItemInInventory(player, required);
            total += Math.min(1.0f, (float) have / (float) need);
        }
        return total / requiredItems.size();
    }

    /**
     * Check if player has all required items ready (counts, not just presence).
     */
    public boolean hasRequiredItems(Player player) {
        if (!isCraftingMission()) return true;

        for (ItemStack required : requiredItems) {
            if (countItemInInventory(player, required) < required.getCount()) return false;
        }
        return true;
    }
    
    /**
     * Check if this mission can be started given completed mission IDs
     */
    public boolean canStart(Set<String> completedIds) {
        if (prerequisites == null || prerequisites.isEmpty()) return true;
        return completedIds.containsAll(prerequisites);
    }
    
    /**
     * Check if this mission can be started for a player
     */
    public boolean canStart(Player player) {
        QuestData qd = player.getData(ModAttachments.QUEST_DATA.get());
        return canStart(qd.getCompletedMissionIds());
    }

    public boolean isPathRestricted() {
        return requiredPath != null && requiredPath != PostNexusData.NexusPath.NONE;
    }

    public boolean matchesRequiredPath(Player player) {
        if (!isPathRestricted()) return true;
        return PostNexusData.get(player).getSelectedPath() == requiredPath;
    }

    public boolean isPathPreview(Player player) {
        if (!isPathRestricted()) return false;
        PostNexusData post = PostNexusData.get(player);
        return post.hasMadeChoice() && post.getSelectedPath() != requiredPath;
    }
    
    /**
     * Get prerequisite mission IDs
     */
    public List<String> getPrerequisites() {
        return prerequisites != null ? prerequisites : Collections.emptyList();
    }
    
    /**
     * Check if this is a turn-in mission (manual item delivery)
     */
    public boolean isTurnInMission() {
        return isTurnInMission;
    }
    
    /**
     * Get the crafting recipe ID for visualization
     */
    public String getCraftingRecipeId() {
        return craftingRecipeId != null ? craftingRecipeId : "";
    }
    
    /**
     * Check if this mission has a crafting recipe to display
     */
    public boolean hasCraftingRecipe() {
        return craftingRecipeId != null && !craftingRecipeId.isEmpty();
    }
    
    /**
     * Count how many of a specific item the player has
     */
    public int countItemInInventory(Player player, ItemStack required) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == required.getItem()) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    /**
     * Get detailed progress for each required item
     */
    public List<ItemProgress> getItemProgress(Player player) {
        if (!isCraftingMission()) return Collections.emptyList();
        
        List<ItemProgress> progress = new ArrayList<>();
        for (ItemStack required : requiredItems) {
            int have = countItemInInventory(player, required);
            int need = required.getCount();
            progress.add(new ItemProgress(required, have, need, have >= need));
        }
        return progress;
    }
    
    /**
     * Record for tracking item progress
     */
    public record ItemProgress(ItemStack item, int have, int need, boolean satisfied) {}
    
    // Mission categories for UI organization
    public enum MissionCategory {
        SURVIVAL("Survival", 0xFF6B6B),
        CRAFTING("Crafting", 0x4ECDC4),
        EXPLORATION("Exploration", 0x95E1D3),
        COMBAT("Combat", 0xF38181),
        TECH("Technology", 0xAA96DA),
        STORY("Story", 0xFFD93D);
        
        private final String displayName;
        private final int color;
        
        MissionCategory(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() { return displayName; }
        public int getColor() { return color; }
    }
    
    // Difficulty ratings
    public enum Difficulty {
        TRIVIAL(0xAAAAAA, "\u00A77"),
        EASY(0x55FF55, "\u00A7a"),
        NORMAL(0xFFFF55, "\u00A7e"),
        HARD(0xFF5555, "\u00A7c"),
        EXTREME(0xAA00AA, "\u00A75");
        
        private final int color;
        private final String chatColor;
        
        Difficulty(int color, String chatColor) {
            this.color = color;
            this.chatColor = chatColor;
        }
        
        public int getColor() { return color; }
        public String getChatColor() { return chatColor; }
    }
    
    // ============================================================================
    // REQUIREMENT TYPE RECORDS
    // ============================================================================
    
    /**
     * Represents a block placement requirement.
     * @param blockId The block ID (e.g., "hand_recycler")
     * @param count How many must be placed (usually 1)
     * @param displayName Human-readable name for UI
     */
    public record BlockRequirement(String blockId, int count, String displayName) {
        public BlockRequirement(String blockId, int count) {
            this(blockId, count, formatBlockName(blockId));
        }
        
        private static String formatBlockName(String id) {
            return Arrays.stream(id.replace("_", " ").split(" "))
                    .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                    .collect(java.util.stream.Collectors.joining(" "));
        }
    }
    
    /**
     * Represents an entity kill requirement.
     * @param entityType The entity type ID (e.g., "minecraft:zombie")
     * @param count How many must be killed
     * @param displayName Human-readable name for UI
     */
    public record EntityKillRequirement(String entityType, int count, String displayName) {
        public EntityKillRequirement(String entityType, int count) {
            this(entityType, count, formatEntityName(entityType));
        }
        
        private static String formatEntityName(String type) {
            String name = type.contains(":") ? type.substring(type.indexOf(":") + 1) : type;
            return Arrays.stream(name.replace("_", " ").split(" "))
                    .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                    .collect(java.util.stream.Collectors.joining(" "));
        }
    }
    
    /**
     * Represents a location/biome visit requirement.
     * @param locationType Type: "biome", "poi", "dimension"
     * @param locationId The location ID (e.g., "minecraft:desert", "radwarden_outpost")
     * @param displayName Human-readable name for UI
     */
    public record LocationRequirement(String locationType, String locationId, String displayName) {
        public LocationRequirement(String locationType, String locationId) {
            this(locationType, locationId, formatLocationName(locationId));
        }
        
        private static String formatLocationName(String id) {
            String name = id.contains(":") ? id.substring(id.indexOf(":") + 1) : id;
            return Arrays.stream(name.replace("_", " ").split(" "))
                    .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                    .collect(java.util.stream.Collectors.joining(" "));
        }
    }

    /**
     * Represents an equipment requirement. These are checked at turn-in time
     * but never consumed from inventory.
     */
    public record EquipmentRequirement(net.minecraft.world.entity.EquipmentSlot slot, ItemStack item, String displayName) {}
    
    // ============================================================================
    // PROGRESS CHECKING METHODS FOR NEW REQUIREMENT TYPES
    // ============================================================================
    
    /**
     * Check if this mission has block placement requirements
     */
    public boolean hasBlockRequirements() {
        return requiredBlocks != null && !requiredBlocks.isEmpty();
    }
    
    /**
     * Check if this mission has entity kill requirements
     */
    public boolean hasEntityKillRequirements() {
        return requiredEntityKills != null && !requiredEntityKills.isEmpty();
    }
    
    /**
     * Check if this mission has location visit requirements
     */
    public boolean hasLocationRequirements() {
        return requiredLocations != null && !requiredLocations.isEmpty();
    }

    public boolean hasEquipmentRequirements() {
        return requiredEquipment != null && !requiredEquipment.isEmpty();
    }

    public boolean hasRequiredEquipment(Player player) {
        if (!hasEquipmentRequirements()) return true;
        for (EquipmentRequirement req : requiredEquipment) {
            ItemStack equipped = player.getItemBySlot(req.slot());
            if (equipped.isEmpty() || equipped.getItem() != req.item().getItem()) return false;
        }
        return true;
    }
    
    /**
     * Check if all block placement requirements are satisfied by inspecting
     * the player's QuestData lifetime placement counter.
     */
    public boolean hasRequiredBlocks(Player player) {
        if (!hasBlockRequirements()) return true;
        QuestData data = player.getData(ModAttachments.QUEST_DATA.get());
        for (BlockRequirement req : requiredBlocks) {
            if (data.getBlockPlaceCount(req.blockId()) < req.count()) return false;
        }
        return true;
    }
    
    /**
     * Check if any requirements exist (items, blocks, kills, or locations)
     */
    public boolean hasAnyRequirements() {
        return isCraftingMission() || hasBlockRequirements() || hasEntityKillRequirements() || hasLocationRequirements() || hasEquipmentRequirements();
    }
    
    /**
     * Get the total number of requirement types this mission has
     */
    public int getRequirementTypeCount() {
        int count = 0;
        if (isCraftingMission()) count++;
        if (hasBlockRequirements()) count++;
        if (hasEntityKillRequirements()) count++;
        if (hasLocationRequirements()) count++;
        if (hasEquipmentRequirements()) count++;
        return count;
    }
}
