package com.knoxhack.echoashfallprotocol.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import com.knoxhack.echoashfallprotocol.research.ResearchData;

import java.util.function.Consumer;

/**
 * Schematic Fragment - rare item that unlocks recipe categories at Research Lab.
 * Five types: Weapons, Armor, Machines, Medical, Energy
 */
public class SchematicFragmentItem extends Item {
    
    public enum SchematicType {
        WEAPONS("Weapons", "Unlocks advanced weapon recipes", ChatFormatting.RED, 0xFFE25959),
        ARMOR("Armor", "Unlocks protective gear recipes", ChatFormatting.BLUE, 0xFF4DBAF4),
        MACHINES("Machines", "Unlocks machine crafting recipes", ChatFormatting.GOLD, 0xFFFFA94D),
        MEDICAL("Medical", "Unlocks medical item recipes", ChatFormatting.GREEN, 0xFF42D67E),
        ENERGY("Energy", "Unlocks power system recipes", ChatFormatting.YELLOW, 0xFFF0C94B);
        
        private final String displayName;
        private final String description;
        private final ChatFormatting color;
        private final int hexColor;
        
        SchematicType(String displayName, String description, ChatFormatting color, int hexColor) {
            this.displayName = displayName;
            this.description = description;
            this.color = color;
            this.hexColor = hexColor;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
        
        public ChatFormatting getChatColor() {
            return color;
        }
        
        public int getHexColor() {
            return hexColor;
        }
    }
    
    private final SchematicType type;
    
    public SchematicFragmentItem(Properties properties, SchematicType type) {
        super(properties);
        this.type = type;
    }
    
    public SchematicType getType() {
        return type;
    }
        
    /**
     * Check if this schematic can unlock recipes for a player
     */
    public boolean canUnlockFor(Player player) {
        ResearchData research = ResearchData.get(player);
        return !research.hasSchematic(type.getDisplayName().toLowerCase());
    }
    
    /**
     * Unlock this schematic category for a player
     * @return true if newly unlocked, false if already had it
     */
    public boolean unlockFor(Player player) {
        ResearchData research = ResearchData.get(player);
        return research.unlockSchematic(type.getDisplayName().toLowerCase());
    }
}
