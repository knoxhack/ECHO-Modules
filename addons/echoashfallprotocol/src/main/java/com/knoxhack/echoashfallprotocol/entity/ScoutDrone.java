package com.knoxhack.echoashfallprotocol.entity;

import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

/**
 * Player-deployable Scout Drone.
 * Follows its owner, auto-scavenges nearby Debris Blocks, picks up items, and can defend.
 */
public class ScoutDrone extends PathfinderMob {

    public enum DroneMode {
        FOLLOW("Follow Mode", "Following owner and picking up items"),
        SCAVENGE("Scavenge Mode", "Breaking debris blocks within range"),
        DEFENSE("Defense Mode", "Attacking hostile mobs near owner");

        private final String displayName;
        private final String description;

        DroneMode(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    private static final EntityDataAccessor<String> DATA_OWNER = SynchedEntityData.defineId(ScoutDrone.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(ScoutDrone.class, EntityDataSerializers.INT);

    private DroneMode currentMode = DroneMode.FOLLOW;

    public ScoutDrone(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, "");
        builder.define(DATA_MODE, DroneMode.FOLLOW.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3D, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    public void setOwner(Player player) {
        this.entityData.set(DATA_OWNER, player.getUUID().toString());
    }

    public UUID getOwnerUUID() {
        String owner = this.entityData.get(DATA_OWNER);
        return owner != null && !owner.isEmpty() ? UUID.fromString(owner) : null;
    }

    private Player getOwnerAttacker(Entity entity) {
        return entity instanceof Player player && player.getUUID().equals(getOwnerUUID()) ? player : null;
    }

    public DroneMode getMode() {
        int ordinal = this.entityData.get(DATA_MODE);
        DroneMode[] modes = DroneMode.values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : DroneMode.FOLLOW;
    }

    public void setMode(DroneMode mode) {
        if (mode == null) {
            mode = DroneMode.FOLLOW;
        }
        this.currentMode = mode;
        this.entityData.set(DATA_MODE, mode.ordinal());
    }

    public void cycleMode() {
        DroneMode[] modes = DroneMode.values();
        int nextIndex = (getMode().ordinal() + 1) % modes.length;
        setMode(modes[nextIndex]);

        // Notify owner of mode change
        UUID ownerId = getOwnerUUID();
        if (ownerId != null && this.level() instanceof ServerLevel serverLevel) {
            Player owner = serverLevel.getPlayerByUUID(ownerId);
            if (owner != null) {
                owner.sendSystemMessage(Component.literal(
                        "§b[DRONE]§r Mode: §e" + getMode().getDisplayName() + "§r - " + getMode().getDescription()));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        UUID ownerId = getOwnerUUID();
        if (ownerId != null) {
            output.putString("OwnerUUID", ownerId.toString());
        }
        output.putInt("Mode", getMode().ordinal());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getString("OwnerUUID").ifPresent(uuid -> {
            try {
                this.entityData.set(DATA_OWNER, UUID.fromString(uuid).toString());
            } catch (IllegalArgumentException ignored) {
                this.entityData.set(DATA_OWNER, "");
            }
        });
        int modeOrdinal = input.getIntOr("Mode", DroneMode.FOLLOW.ordinal());
        DroneMode[] modes = DroneMode.values();
        setMode(modeOrdinal >= 0 && modeOrdinal < modes.length ? modes[modeOrdinal] : DroneMode.FOLLOW);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Player owner = getOwnerAttacker(source.getEntity());
        if (owner != null) {
            owner.sendSystemMessage(Component.literal("[DRONE] Friendly fire lockout engaged."));
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (getOwnerUUID() != null) {
            level.addFreshEntity(new ItemEntity(level, getX(), getY(), getZ(),
                    new ItemStack(ModItems.SCOUT_DRONE_ITEM.get())));
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide()) {
            if (getMode() != DroneMode.DEFENSE) {
                this.setTarget(null);
                this.setAggressive(false);
            }

            // Mode-specific behavior every tick
            handleItemPickup();

            if (this.level().getGameTime() % 40 == 0) {
                switch (getMode()) {
                    case FOLLOW:
                        handleFollowMode();
                        break;
                    case SCAVENGE:
                        handleScavengeMode();
                        break;
                    case DEFENSE:
                        handleDefenseMode();
                        break;
                }
            }
        }
    }

    private void handleFollowMode() {
        // Follow owner if set
        UUID ownerId = getOwnerUUID();
        if (ownerId != null && this.level() instanceof ServerLevel serverLevel) {
            Player owner = serverLevel.getPlayerByUUID(ownerId);
            if (owner != null && this.distanceTo(owner) > 6.0) {
                this.getNavigation().moveTo(owner, 1.2);
            }
        }
    }

    private void handleScavengeMode() {
        // Break debris blocks
        scavengeNearbyDebris();

        // Also try to follow owner in this mode, but more loosely
        UUID ownerId = getOwnerUUID();
        if (ownerId != null && this.level() instanceof ServerLevel serverLevel) {
            Player owner = serverLevel.getPlayerByUUID(ownerId);
            if (owner != null && this.distanceTo(owner) > 12.0) {
                this.getNavigation().moveTo(owner, 1.2);
            }
        }
    }

    private void handleDefenseMode() {
        // Follow owner closely
        UUID ownerId = getOwnerUUID();
        if (ownerId != null && this.level() instanceof ServerLevel serverLevel) {
            Player owner = serverLevel.getPlayerByUUID(ownerId);
            if (owner != null && this.distanceTo(owner) > 4.0) {
                this.getNavigation().moveTo(owner, 1.4);
            }
        }

        if (this.getTarget() == null) {
            this.setAggressive(false);
            return;
        }

        this.setAggressive(true);
        this.getNavigation().moveTo(this.getTarget(), 1.3D);
    }

    private void handleItemPickup() {
        UUID ownerId = getOwnerUUID();
        if (ownerId == null || !(this.level() instanceof ServerLevel)) return;

        Player owner = ((ServerLevel) this.level()).getPlayerByUUID(ownerId);
        if (owner == null) return;

        // Check for nearby item entities
        this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(3.0),
                item -> item.isAlive() && !item.getItem().isEmpty()).forEach(item -> {
            ItemStack stack = item.getItem();
            item.discard();

            // Try to add to owner inventory
            if (!owner.getInventory().add(stack)) {
                // If inventory full, drop at owner position
                owner.drop(stack, false);
            } else {
                owner.sendSystemMessage(Component.literal(
                        "§e[DRONE]§r Retrieved: §f" + stack.getCount() + "x " + stack.getHoverName().getString()));
            }
        });
    }

    private void scavengeNearbyDebris() {
        BlockPos dronePos = this.blockPosition();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos target = dronePos.offset(dx, dy, dz);
                    BlockState state = this.level().getBlockState(target);
                    if (state.is(ModBlocks.DEBRIS_BLOCK.get())) {
                        // Break block and notify owner
                        this.level().destroyBlock(target, true, this);
                        UUID ownerId = getOwnerUUID();
                        if (ownerId != null && this.level() instanceof ServerLevel serverLevel) {
                            Player owner = serverLevel.getPlayerByUUID(ownerId);
                            if (owner != null) {
                                owner.sendSystemMessage(Component.literal(
                                        "§e[DRONE]§r Debris salvaged at " + target.toShortString()));
                            }
                        }
                        return;
                    }
                }
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.2);
    }
}
