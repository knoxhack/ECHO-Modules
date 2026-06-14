package com.knoxhack.echodeepreachprotocol.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Energy bolt fired by Lattice Sentinels.
 */
public class LatticeBoltEntity extends SmallFireball {
    public LatticeBoltEntity(EntityType<? extends LatticeBoltEntity> type, Level level) {
        super(type, level);
    }

    public LatticeBoltEntity(Level level, LivingEntity owner, Vec3 movement) {
        super(level, owner, movement);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = result.getEntity();
            if (entity instanceof LivingEntity target) {
                DamageSource source = this.getOwner() instanceof LivingEntity owner
                        ? owner.damageSources().magic()
                        : this.damageSources().magic();
                target.hurtServer(serverLevel, source, 5.0F);
            }
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
