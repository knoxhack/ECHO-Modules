package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echoashfallprotocol.gameplay.RadiationHelper;
import com.knoxhack.echoashfallprotocol.item.upgrade.GearUpgradeHandler;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Consumer;

/**
 * Nexus Annihilator — Tier 4 weapon granted to players who choose DESTRUCTION.
 * +15 damage, area effect (3-block radius), applies heavy radiation to targets.
 */
public class NexusAnnihilatorItem extends Item {

    private static final float BASE_DAMAGE = 15.0f;
    private static final float ARMOR_PIERCE_RATIO = 0.5f;
    private static final float RAD_ON_HIT = 25.0f;
    private static final double AOE_RADIUS = 3.0;

    public NexusAnnihilatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(target.level() instanceof ServerLevel sl)) {
            super.hurtEnemy(stack, target, attacker);
            return;
        }

        // Calculate total damage with upgrade bonus
        float upgradeBonus = GearUpgradeHandler.getBonusDamage(stack);
        float totalDamage = BASE_DAMAGE + upgradeBonus;

        // Primary target damage with armor piercing
        float pierceDamage = totalDamage * ARMOR_PIERCE_RATIO;
        target.hurtServer(sl, target.damageSources().magic(), pierceDamage);

        // Apply radiation to player targets
        if (target instanceof net.minecraft.server.level.ServerPlayer player) {
            RadiationHelper.addRadiation(player, RAD_ON_HIT);
        }

        // Area of Effect damage to nearby entities
        AABB aoeBox = target.getBoundingBox().inflate(AOE_RADIUS);
        List<LivingEntity> nearbyEntities = sl.getEntitiesOfClass(LivingEntity.class, aoeBox,
                entity -> entity != target && entity != attacker && entity.isAlive());

        for (LivingEntity nearby : nearbyEntities) {
            // Half damage to secondary targets (with upgrade bonus)
            nearby.hurtServer(sl, nearby.damageSources().magic(), totalDamage * 0.5f);

            // Apply radiation to player targets in AOE
            if (nearby instanceof net.minecraft.server.level.ServerPlayer player) {
                RadiationHelper.addRadiation(player, RAD_ON_HIT * 0.5f);
            }
        }

        // AOE visual + audio feedback
        sl.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1.0, target.getZ(),
                3, 0.5, 0.5, 0.5, 0.0);
        sl.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5f, 1.4f);

        super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Glowing enchantment sheen
    }
}
