package com.knoxhack.echoarmory.entity;

import com.knoxhack.echoarmory.content.FiringModeDefinition;
import com.knoxhack.echoarmory.item.ArmoryWeaponItem;
import com.knoxhack.echoarmory.registry.ModEntities;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ArmoryProjectileEntity extends Entity {
   private static final EntityDataAccessor<Integer> DATA_KIND =
      SynchedEntityData.defineId(ArmoryProjectileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> DATA_DAMAGE =
      SynchedEntityData.defineId(ArmoryProjectileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_LIFE =
      SynchedEntityData.defineId(ArmoryProjectileEntity.class, EntityDataSerializers.INT);
   @Nullable
   private UUID ownerUuid;
   @Nullable
   private LivingEntity owner;
   private ItemStack sourceStack = ItemStack.EMPTY;

   public ArmoryProjectileEntity(EntityType<? extends ArmoryProjectileEntity> type, Level level) {
      super(type, level);
      setNoGravity(true);
   }

   public static ArmoryProjectileEntity create(
      ServerLevel level,
      Player owner,
      ItemStack sourceStack,
      FiringModeDefinition mode,
      float damage
   ) {
      EntityType<ArmoryProjectileEntity> type = switch (mode.projectileKind()) {
         case VEIL_ARROW -> ModEntities.VEIL_ARROW.get();
         case SIGIL_CHAKRAM -> ModEntities.SIGIL_CHAKRAM.get();
         case ENERGY_BOLT -> ModEntities.ENERGY_BOLT.get();
      };
      ArmoryProjectileEntity projectile = new ArmoryProjectileEntity(type, level);
      projectile.configure(owner, sourceStack, mode.projectileKind(), damage, mode.range(), mode.velocity());
      return projectile;
   }

   public void configure(
      LivingEntity owner,
      ItemStack sourceStack,
      FiringModeDefinition.ProjectileKind kind,
      float damage,
      double range,
      double velocity
   ) {
      this.owner = owner;
      this.ownerUuid = owner.getUUID();
      this.sourceStack = sourceStack.copy();
      entityData.set(DATA_KIND, kind.ordinal());
      entityData.set(DATA_DAMAGE, Math.max(0.0F, damage));
      entityData.set(DATA_LIFE, Math.max(6, (int)Math.ceil(range / Math.max(0.1D, velocity)) + 8));
      Vec3 look = owner.getLookAngle().normalize();
      setPos(owner.getX() + look.x * 0.6D, owner.getEyeY() - 0.12D + look.y * 0.2D, owner.getZ() + look.z * 0.6D);
      setDeltaMovement(look.scale(velocity));
      setYRot(owner.getYRot());
      setXRot(owner.getXRot());
      setOldPosAndRot();
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(DATA_KIND, FiringModeDefinition.ProjectileKind.ENERGY_BOLT.ordinal());
      builder.define(DATA_DAMAGE, 2.0F);
      builder.define(DATA_LIFE, 20);
   }

   @Override
   public void tick() {
      super.tick();
      if (level().isClientSide()) {
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
      HitResult blockHit = level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
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
         serverLevel.sendParticles(particle(), getX(), getY(), getZ(), 2, 0.05D, 0.05D, 0.05D, 0.01D);
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

   public FiringModeDefinition.ProjectileKind projectileKind() {
      FiringModeDefinition.ProjectileKind[] values = FiringModeDefinition.ProjectileKind.values();
      int id = entityData.get(DATA_KIND);
      return id >= 0 && id < values.length ? values[id] : FiringModeDefinition.ProjectileKind.ENERGY_BOLT;
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
      if (ownerEntity != null && !sourceStack.isEmpty()) {
         ArmoryWeaponItem.applyModuleEffects(level, sourceStack, target, ownerEntity);
      }
      Vec3 pushDirection = target.position().subtract(position());
      if (pushDirection.lengthSqr() < 1.0E-6D) {
         pushDirection = getDeltaMovement();
      }
      Vec3 push = pushDirection.normalize().scale(0.18D + damage * 0.012D);
      target.push(push.x, 0.05D, push.z);
      burst(target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D));
      level.playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.35F, 1.45F);
   }

   private void burst(Vec3 pos) {
      if (level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(particle(), pos.x, pos.y, pos.z, 8, 0.12D, 0.12D, 0.12D, 0.02D);
      }
   }

   private ParticleOptions particle() {
      return switch (projectileKind()) {
         case VEIL_ARROW -> ParticleTypes.PORTAL;
         case SIGIL_CHAKRAM -> ParticleTypes.ENCHANT;
         case ENERGY_BOLT -> ParticleTypes.ELECTRIC_SPARK;
      };
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
      entityData.set(DATA_KIND, input.getIntOr("kind", FiringModeDefinition.ProjectileKind.ENERGY_BOLT.ordinal()));
      entityData.set(DATA_DAMAGE, input.getFloatOr("damage", 2.0F));
      entityData.set(DATA_LIFE, input.getIntOr("life", 20));
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
