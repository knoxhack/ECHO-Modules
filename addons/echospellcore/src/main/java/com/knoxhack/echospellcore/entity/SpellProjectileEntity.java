package com.knoxhack.echospellcore.entity;

import com.knoxhack.echospellcore.client.SpellPredictionClientState;
import com.knoxhack.echospellcore.registry.ModEntities;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class SpellProjectileEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(SpellProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(SpellProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE =
            SynchedEntityData.defineId(SpellProjectileEntity.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private LivingEntity owner;

    public SpellProjectileEntity(EntityType<? extends SpellProjectileEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static SpellProjectileEntity create(ServerLevel level, Player owner, SpellProjectileKind kind,
            double range, double velocity, float damage) {
        SpellProjectileEntity projectile = new SpellProjectileEntity(ModEntities.SPELL_PROJECTILE.get(), level);
        projectile.configure(owner, kind, range, velocity, damage);
        return projectile;
    }

    public void configure(LivingEntity owner, SpellProjectileKind kind, double range, double velocity, float damage) {
        this.owner = owner;
        this.ownerUuid = owner.getUUID();
        entityData.set(DATA_KIND, kind.ordinal());
        entityData.set(DATA_DAMAGE, Math.max(1.0F, damage));
        entityData.set(DATA_LIFE, Math.max(6, (int) Math.ceil(range / Math.max(0.1D, velocity)) + 8));
        Vec3 look = owner.getLookAngle().normalize();
        setPos(owner.getX() + look.x * 0.65D, owner.getEyeY() - 0.10D + look.y * 0.2D,
                owner.getZ() + look.z * 0.65D);
        setDeltaMovement(look.scale(velocity));
        setYRot(owner.getYRot());
        setXRot(owner.getXRot());
        setOldPosAndRot();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_KIND, SpellProjectileKind.AETHER_BOLT.ordinal());
        builder.define(DATA_DAMAGE, 5.0F);
        builder.define(DATA_LIFE, 30);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            clientPredictionTick();
            return;
        }
        int life = entityData.get(DATA_LIFE) - 1;
        entityData.set(DATA_LIFE, life);
        if (life <= 0) {
            discard();
            return;
        }
        Vec3 start = position();
        Vec3 movement = getDeltaMovement();
        Vec3 end = start.add(movement);
        HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            burst(blockHit.getLocation());
            discard();
            return;
        }
        LivingEntity target = findTarget(movement);
        move(MoverType.SELF, movement);
        if (target != null && level() instanceof ServerLevel serverLevel) {
            hit(serverLevel, target);
            discard();
        } else if (tickCount % 2 == 0 && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(projectileKind().trailParticle(), getX(), getY(), getZ(),
                    2, 0.05D, 0.05D, 0.05D, 0.01D);
            if (tickCount % 4 == 0) {
                serverLevel.sendParticles(projectileKind().accentParticle(), getX(), getY(), getZ(),
                        1, 0.03D, 0.03D, 0.03D, 0.006D);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        discard();
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    public SpellProjectileKind projectileKind() {
        SpellProjectileKind[] values = SpellProjectileKind.values();
        int id = entityData.get(DATA_KIND);
        return id >= 0 && id < values.length ? values[id] : SpellProjectileKind.AETHER_BOLT;
    }

    public int remainingLife() {
        return entityData.get(DATA_LIFE);
    }

    private LivingEntity findTarget(Vec3 movement) {
        AABB area = getBoundingBox().expandTowards(movement).inflate(0.35D);
        LivingEntity ownerEntity = owner();
        return level().getEntitiesOfClass(LivingEntity.class, area, entity ->
                        entity.isAlive()
                                && entity != ownerEntity
                                && !entity.isSpectator()
                                && (ownerEntity == null || !entity.isAlliedTo(ownerEntity)))
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(this)))
                .orElse(null);
    }

    private void hit(ServerLevel level, LivingEntity target) {
        LivingEntity ownerEntity = owner();
        float damage = Math.max(1.0F, entityData.get(DATA_DAMAGE));
        DamageSource source = ownerEntity instanceof Player player ? player.damageSources().magic() : damageSources().magic();
        target.hurtServer(level, source, damage);
        projectileKind().applyHitEffects(target);
        Vec3 pushDirection = target.position().subtract(position());
        if (pushDirection.lengthSqr() < 1.0E-6D) {
            pushDirection = getDeltaMovement();
        }
        Vec3 push = pushDirection.normalize().scale(0.12D + damage * 0.01D);
        target.push(push.x, 0.05D, push.z);
        burst(target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_CLUSTER_HIT, SoundSource.PLAYERS,
                0.45F, projectileKind().pitch());
    }

    private void clientPredictionTick() {
        SpellPredictionClientState.reconcile(this);
        Vec3 movement = getDeltaMovement();
        if (movement.lengthSqr() > 1.0E-6D) {
            move(MoverType.SELF, movement);
        }
        SpellProjectileKind kind = projectileKind();
        if (tickCount % 2 == 0) {
            level().addParticle(kind.trailParticle(), getX(), getY(), getZ(),
                    -movement.x * 0.02D, -movement.y * 0.02D, -movement.z * 0.02D);
        }
        if (tickCount % 4 == 0) {
            level().addParticle(kind.accentParticle(), getX(), getY(), getZ(),
                    0.0D, 0.01D, 0.0D);
        }
    }

    private void burst(Vec3 pos) {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(projectileKind().trailParticle(), pos.x, pos.y, pos.z,
                    12, 0.16D, 0.16D, 0.16D, 0.025D);
            serverLevel.sendParticles(projectileKind().accentParticle(), pos.x, pos.y, pos.z,
                    5, 0.11D, 0.11D, 0.11D, 0.018D);
        }
    }

    @Nullable
    private LivingEntity owner() {
        if (owner != null && !owner.isRemoved()) {
            return owner;
        }
        if (ownerUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(ownerUuid);
        if (entity instanceof LivingEntity living) {
            owner = living;
            return living;
        }
        return null;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("kind", entityData.get(DATA_KIND));
        output.putFloat("damage", entityData.get(DATA_DAMAGE));
        output.putInt("life", entityData.get(DATA_LIFE));
        if (ownerUuid != null) {
            output.putString("owner_uuid", ownerUuid.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        entityData.set(DATA_KIND, input.getIntOr("kind", SpellProjectileKind.AETHER_BOLT.ordinal()));
        entityData.set(DATA_DAMAGE, input.getFloatOr("damage", 5.0F));
        entityData.set(DATA_LIFE, input.getIntOr("life", 30));
        ownerUuid = readUuid(input.getStringOr("owner_uuid", ""));
    }

    @Nullable
    private static UUID readUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
