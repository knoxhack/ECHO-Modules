package com.knoxhack.echoashfallprotocol.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class BoneKnifeItem extends Item {
    private static final float BASE_DAMAGE = 4.0f;

    public BoneKnifeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.isAlive() && target.level() instanceof ServerLevel sl) {
            target.hurtServer(sl, attacker.damageSources().generic(), BASE_DAMAGE - 1.0f);
        }
        super.hurtEnemy(stack, target, attacker);
    }
}
