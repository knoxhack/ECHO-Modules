package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreEarlyEventRuntime;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Map;

public abstract class WaterBottleItem extends Item {
    private static final Component NO_BENEFIT_MESSAGE =
            Component.translatable("message.EchoAshfallProtocol.water.no_benefit");

    private final int hydrationGain;
    private final int foodNutrition;
    private final float foodSaturation;

    protected WaterBottleItem(Properties properties, int hydrationGain, int foodNutrition, float foodSaturation) {
        super(properties);
        this.hydrationGain = hydrationGain;
        this.foodNutrition = foodNutrition;
        this.foodSaturation = foodSaturation;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!canBenefit(player)) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                AshfallAdapterCoreEarlyEventRuntime.waterBottleNoBenefit(
                        serverPlayer,
                        player.getItemInHand(hand),
                        hand);
            } else if (!level.isClientSide()) {
                player.sendSystemMessage(NO_BENEFIT_MESSAGE);
            }
            return InteractionResult.FAIL;
        }

        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            if (!canBenefit(player)) {
                return stack;
            }
            if (!level.isClientSide()) {
                InteractionHand hand = activeHand(player, stack);
                NativeResult result = applyWaterEffects(level, player, stack, hand);
                if (!result.mutated()) {
                    return stack;
                }
                return player.getItemInHand(hand);
            }
        }

        return super.finishUsingItem(stack, level, livingEntity);
    }

    protected NativeResult applyWaterEffects(Level level, Player player, ItemStack stack, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            return AshfallAdapterCoreEarlyEventRuntime.waterBottleDrunk(
                    serverPlayer,
                    stack,
                    hand,
                    runtimeHydrationGain(),
                    foodNutrition,
                    foodSaturation,
                    false);
        }

        return NativeResult.unsupported(
                "Water bottle use requires a live server player target.",
                Map.of("itemId", stack.getItem().toString()));
    }

    protected int runtimeHydrationGain() {
        return hydrationGain;
    }

    protected boolean canBenefit(Player player) {
        return improvesHydration(player) || canFoodBenefit(player);
    }

    protected boolean improvesHydration(Player player) {
        int hydration = player.getData(ModAttachments.SURVIVAL_DATA.get()).getHydration();
        return hydrationAfterUse(hydration) > hydration;
    }

    protected int hydrationAfterUse(int hydration) {
        return clampHydration(hydration + hydrationGain);
    }

    protected final int clampHydration(int hydration) {
        return Math.max(0, Math.min(SurvivalData.MAX_HYDRATION, hydration));
    }

    private boolean canFoodBenefit(Player player) {
        return foodNutrition > 0 && player.getFoodData().needsFood();
    }

    private InteractionHand activeHand(Player player, ItemStack stack) {
        if (player.getUseItem() == stack) {
            return player.getUsedItemHand();
        }
        if (player.getOffhandItem() == stack) {
            return InteractionHand.OFF_HAND;
        }
        return InteractionHand.MAIN_HAND;
    }
}
