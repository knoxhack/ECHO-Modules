package com.knoxhack.echoashfallprotocol.entity;

import com.knoxhack.echoashfallprotocol.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class CrashSurvivor extends PathfinderMob {
    public CrashSurvivor(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack offered = player.getItemInHand(hand);
        if (offered.is(ModItems.EMERGENCY_RATION.get()) || offered.is(ModItems.BANDAGE.get()) || offered.is(ModItems.STIM_PACK.get())) {
            if (!player.level().isClientSide()) {
                offered.shrink(1);
                if (!player.addItem(new ItemStack(ModItems.SCHEMATIC_FRAGMENT.get()))) {
                    player.drop(new ItemStack(ModItems.SCHEMATIC_FRAGMENT.get()), false);
                }
                player.sendSystemMessage(Component.literal("§b[Survivor]§r Take this fragment. Stay alive out there."));
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.level().isClientSide()) {
            player.sendSystemMessage(Component.literal("§7[Survivor] I can trade fragments for food or medical supplies."));
        }
        return InteractionResult.CONSUME;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.FOLLOW_RANGE, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }
}
