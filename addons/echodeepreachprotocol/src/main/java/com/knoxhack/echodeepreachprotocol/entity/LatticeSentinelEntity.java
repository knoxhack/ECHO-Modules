package com.knoxhack.echodeepreachprotocol.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Hostile sentinel of The Lattice. Shoots simple projectiles at players.
 */
public class LatticeSentinelEntity extends Monster {
    private int attackCooldown;

    public LatticeSentinelEntity(EntityType<? extends LatticeSentinelEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && !this.level().isClientSide() && this.attackCooldown <= 0) {
            double distSqr = this.distanceToSqr(target);
            if (distSqr < 256.0D && this.hasLineOfSight(target)) {
                this.performRangedAttack((ServerLevel) this.level(), target);
                this.attackCooldown = 40;
            }
        }
    }

    private void performRangedAttack(ServerLevel level, LivingEntity target) {
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333D) - this.getY(1.5D);
        double dz = target.getZ() - this.getZ();
        Vec3 power = new Vec3(dx, dy, dz).normalize();
        LatticeBoltEntity bolt = new LatticeBoltEntity(level, this, power);
        bolt.setPos(this.getX(), this.getY(1.0D), this.getZ());
        level.addFreshEntity(bolt);
    }
}
