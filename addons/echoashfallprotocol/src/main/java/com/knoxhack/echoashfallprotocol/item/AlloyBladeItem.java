package com.knoxhack.echoashfallprotocol.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Alloy Blade — Tier 2 weapon crafted from Dense Alloy Chunks.
 * 8.0 base damage, 350 durability, 20% armor piercing via direct damage.
 */
public class AlloyBladeItem extends Item {

    private static final float BASE_DAMAGE = 8.0f;
    private static final float ARMOR_PIERCE_RATIO = 0.20f;

    public AlloyBladeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        float pierceDamage = BASE_DAMAGE * ARMOR_PIERCE_RATIO;
        if (target.isAlive() && pierceDamage > 0 && target.level() instanceof ServerLevel sl) {
            var src = (attacker instanceof Player p) ? p.damageSources().playerAttack(p) : attacker.damageSources().generic();
            target.hurtServer(sl, src, pierceDamage);
            sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.1f);
        }
        super.hurtEnemy(stack, target, attacker);
    }
}
