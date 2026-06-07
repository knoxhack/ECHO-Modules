package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreExplorationRuntime;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Crashbreak elite research reward that decodes into a missing schematic branch.
 */
public class RareTechSchematicItem extends Item {
    public static final int MISSING_CATEGORY_RP = 75;
    public static final int DUPLICATE_ARCHIVE_RP = 125;

    public RareTechSchematicItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (!level.getBlockState(context.getClickedPos()).is(ModBlocks.RESEARCH_LAB.get())) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        DecodeResult result = decodeAtResearchLab(serverPlayer, context.getItemInHand());
        return result.consumed() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public static DecodeResult decodeAtResearchLab(Player player, ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ModItems.RARE_TECH_SCHEMATIC.get())) {
            return new DecodeResult(false, null, 0);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            NativeResult result = AshfallAdapterCoreExplorationRuntime.rareTechSchematicDecoded(
                    serverPlayer,
                    heldHand(serverPlayer, stack),
                    "rare_tech_schematic_item");
            return decodeResult(result);
        }
        return new DecodeResult(false, null, 0);
    }

    private static InteractionHand heldHand(ServerPlayer player, ItemStack stack) {
        if (player.getOffhandItem() == stack) {
            return InteractionHand.OFF_HAND;
        }
        return InteractionHand.MAIN_HAND;
    }

    private static DecodeResult decodeResult(NativeResult result) {
        if (result == null || result.terminalFailure()) {
            return new DecodeResult(false, null, 0);
        }
        boolean decoded = Boolean.TRUE.equals(result.snapshot().get("decodeApplied"));
        int added = numberValue(result.snapshot().get("researchPointsAdded"));
        return new DecodeResult(decoded, schematicType(String.valueOf(result.snapshot().get("unlockedType"))), added);
    }

    private static SchematicFragmentItem.SchematicType schematicType(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (SchematicFragmentItem.SchematicType type : SchematicFragmentItem.SchematicType.values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }

    private static int numberValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    public record DecodeResult(boolean consumed, SchematicFragmentItem.SchematicType unlockedType, int researchPoints) {
    }
}
