package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echoashfallprotocol.gameplay.AshfallInteractionRules;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Scrap Knife — early game tool with friction-based damage degradation.
 * Damage decreases as durability is lost, simulating a dulling blade.
 */
public class ScrapKnifeItem extends Item {
    private static final float BASE_DAMAGE = 5.0f;

    public ScrapKnifeItem(Properties properties) {
        super(properties.durability(90).component(DataComponents.WEAPON, new Weapon(1)));
    }

    /**
     * Returns the current effectiveness as a percentage.
     * Damage decreases linearly with durability loss.
     */
    public float getEffectivenessPercent(ItemStack stack) {
        if (!stack.isDamageableItem()) return 1.0f;
        float remaining = 1.0f - ((float) stack.getDamageValue() / stack.getMaxDamage());
        return Math.max(0.05f, remaining); // Minimum 5% effectiveness
    }

    public float getCurrentDamage(ItemStack stack) {
        return BASE_DAMAGE * getEffectivenessPercent(stack);
    }

    /**
     * Override to apply dynamic damage based on durability.
     * The knife deals less damage as it gets duller.
     */
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Apply dynamic damage based on current effectiveness
        float currentDamage = getCurrentDamage(stack);
        float baseItemDamage = 1.0f; // Default item damage

        // Calculate extra damage to apply (difference between our dynamic damage and base)
        float extraDamage = Math.max(0, currentDamage - baseItemDamage);

        if (extraDamage > 0 && target.isAlive()) {
            // Apply the extra damage directly
            AshfallInteractionRules.hurtServerSide(target, target.damageSources().generic(), extraDamage);
        }

        // Call parent to handle normal item damage and durability loss
        super.hurtEnemy(stack, target, attacker);
    }
}
