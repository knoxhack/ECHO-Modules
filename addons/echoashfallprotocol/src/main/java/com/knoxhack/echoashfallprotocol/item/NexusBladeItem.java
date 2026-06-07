package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echoashfallprotocol.gameplay.RadiationHelper;
import com.knoxhack.echoashfallprotocol.item.upgrade.GearUpgradeHandler;
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
 * Nexus Blade — Tier 3 endgame weapon infused with Nexus Crystal energy.
 * 11.0 damage, 1500 durability, applies 15 radiation to target on hit.
 */
public class NexusBladeItem extends Item {

    private static final float BASE_DAMAGE = 11.0f;
    private static final float ARMOR_PIERCE_RATIO = 0.35f;
    private static final float RAD_ON_HIT = 15.0f;

    public NexusBladeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Calculate total damage with upgrade bonus
        float upgradeBonus = GearUpgradeHandler.getBonusDamage(stack);
        float totalDamage = BASE_DAMAGE + upgradeBonus;
        float pierceDamage = totalDamage * ARMOR_PIERCE_RATIO;
        
        if (target.isAlive() && target.level() instanceof ServerLevel sl) {
            target.hurtServer(sl, target.damageSources().magic(), pierceDamage);

            // Apply radiation to player targets (PvP radiation debuff)
            if (target instanceof net.minecraft.server.level.ServerPlayer player) {
                RadiationHelper.addRadiation(player, RAD_ON_HIT);
            }
            sl.playSound(null, target.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 0.5f);
        }
        super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Glowing enchantment sheen
    }
}
