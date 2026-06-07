package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreExplorationRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Data Log items containing pre-fall lore, survivor stories, and world history.
 * Reading these adds the lore to ECHO-7's intel database.
 */
public class DataLogItem extends Item {
    
    private final DataLogType logType;
    private final String loreTitle;
    private final String[] lorePages;
    
    public DataLogItem(Properties properties, DataLogType logType, String loreTitle, String[] lorePages) {
        super(properties.stacksTo(1));
        this.logType = logType;
        this.loreTitle = loreTitle;
        this.lorePages = lorePages;
    }
    
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            String content = String.join("\n\n", lorePages);
            NativeResult result = AshfallAdapterCoreExplorationRuntime.dataLogRecovered(
                    serverPlayer,
                    logType.name().toLowerCase(Locale.ROOT),
                    loreTitle,
                    content,
                    loreId(),
                    hand,
                    BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            return result.terminalFailure() ? InteractionResult.FAIL : InteractionResult.SUCCESS;
        }
        
        return InteractionResult.SUCCESS;
    }
        
    public DataLogType getLogType() {
        return logType;
    }
    
    public String getLoreTitle() {
        return loreTitle;
    }

    private String loreId() {
        return "datalog_" + logType.name().toLowerCase(Locale.ROOT) + "_"
                + loreTitle.toLowerCase(Locale.ROOT).replace(" ", "_");
    }
    
    public enum DataLogType {
        PREFALL_HISTORY("Pre-Fall History", "Documents from before the Gridfall"),
        NEXUS_ARCHIVES("Nexus Archives", "AI system logs and diagnostics"),
        SURVIVOR_JOURNAL("Survivor Journal", "Personal accounts from other survivors"),
        TECHNICAL_MANUAL("Technical Manual", "Old World technical documentation"),
        RESEARCH_DATA("Research Data", "Scientific findings and experiments");
        
        private final String displayName;
        private final String description;
        
        DataLogType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
}
