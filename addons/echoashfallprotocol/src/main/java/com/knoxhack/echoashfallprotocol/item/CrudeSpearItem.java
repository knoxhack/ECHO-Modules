package com.knoxhack.echoashfallprotocol.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class CrudeSpearItem extends Item {
    public CrudeSpearItem(Properties properties) {
        super(properties.stacksTo(1).component(DataComponents.WEAPON, new Weapon(1)));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.level() instanceof ServerLevel sl) {
            target.hurtServer(sl, target.damageSources().generic(), 5.0f);
        }
        super.hurtEnemy(stack, target, attacker);
    }
}
