package com.knoxhack.echoorbitalremnants.entity;

import com.knoxhack.echoorbitalremnants.progression.EmergencyRocketLaunch;
import com.knoxhack.echoorbitalremnants.progression.LaunchReadiness;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EmergencyRocketEntity extends VehicleEntity {
    private static final EntityDataAccessor<Integer> DATA_LAUNCH_STATE =
            SynchedEntityData.defineId(EmergencyRocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COUNTDOWN_TICKS =
            SynchedEntityData.defineId(EmergencyRocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LAUNCH_TICKS =
            SynchedEntityData.defineId(EmergencyRocketEntity.class, EntityDataSerializers.INT);
    public static final int COUNTDOWN_TICKS = 100;
    public static final int ASCENT_TICKS = 60;
    private int launchTicks;
    @Nullable
    private UUID countdownPassenger;
    @Nullable
    private UUID committedPassenger;
    @Nullable
    private Player countdownPassengerEntity;
    @Nullable
    private Player committedPassengerEntity;
    private double returnX;
    private double returnY;
    private double returnZ;
    private String returnDimension = "minecraft:overworld";

    public EmergencyRocketEntity(EntityType<? extends EmergencyRocketEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public void setLaunchPadPosition(double x, double y, double z, float yRot) {
        setPos(x, y, z);
        setYRot(yRot);
        setXRot(0.0F);
        setOldPosAndRot();
        returnX = x;
        returnY = y;
        returnZ = z;
        returnDimension = level().dimension().identifier().toString();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(DATA_LAUNCH_STATE, LaunchState.PLACED.id());
        entityData.define(DATA_COUNTDOWN_TICKS, 0);
        entityData.define(DATA_LAUNCH_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }

        LaunchState state = launchState();
        if (state == LaunchState.COUNTDOWN) {
            tickCountdown();
        } else if (state == LaunchState.LAUNCHING) {
            tickLaunch();
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult superResult = super.interact(player, hand, location);
        if (superResult != InteractionResult.PASS) {
            return superResult;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.getVehicle() == this) {
            return startCountdown(player) ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        if (launchState() == LaunchState.LAUNCHING) {
            player.sendSystemMessage(Component.literal("ECHO-7 // Rocket is already committed to ascent. Stand clear of the pad."));
            return InteractionResult.CONSUME;
        }
        if (isVehicle()) {
            player.sendSystemMessage(Component.literal("ECHO-7 // Rocket cabin occupied. The rider must start or abort countdown."));
            return InteractionResult.CONSUME;
        }
        if (player.startRiding(this)) {
            playFeedback(SoundEvents.IRON_DOOR_CLOSE, 0.7F, 1.25F);
            sendPadParticles(ParticleTypes.CLOUD, 8, 0.18D, 0.02D);
            player.sendSystemMessage(Component.literal("ECHO-7 // Cabin sealed. Right-click the rocket again to start launch countdown."));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (launchState() == LaunchState.LAUNCHING || isInvulnerableToBase(source)) {
            return false;
        }
        if (source.getEntity() instanceof Player player && player.getAbilities().instabuild) {
            discard();
        } else {
            destroy(level, ModItems.EMERGENCY_ROCKET.get());
        }
        return true;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return !isRemoved();
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return entity.canBeCollidedWith(this) && !isPassengerOfSameVehicle(entity);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return launchState() != LaunchState.LAUNCHING;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return launchState() != LaunchState.LAUNCHING && getPassengers().isEmpty();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0D, 0.95D, 0.0D);
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public boolean canRiderInteract() {
        return true;
    }

    @Override
    protected Item getDropItem() {
        return ModItems.EMERGENCY_ROCKET.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.EMERGENCY_ROCKET.get());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("launch_state", launchState().id());
        output.putInt("countdown_ticks", countdownTicks());
        output.putInt("launch_ticks", launchTicks);
        if (countdownPassenger != null) {
            output.putString("countdown_passenger_uuid", countdownPassenger.toString());
        }
        if (committedPassenger != null) {
            output.putString("committed_passenger_uuid", committedPassenger.toString());
        }
        output.putDouble("return_x", returnX);
        output.putDouble("return_y", returnY);
        output.putDouble("return_z", returnZ);
        output.putString("return_dimension", returnDimension);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setLaunchState(LaunchState.byId(input.getIntOr("launch_state", LaunchState.PLACED.id())));
        setCountdownTicks(input.getIntOr("countdown_ticks", 0));
        launchTicks = input.getIntOr("launch_ticks", 0);
        setLaunchTicks(launchTicks);
        countdownPassenger = readUuid(input.getStringOr("countdown_passenger_uuid", ""));
        committedPassenger = readUuid(input.getStringOr("committed_passenger_uuid", ""));
        returnX = input.getDoubleOr("return_x", getX());
        returnY = input.getDoubleOr("return_y", getY());
        returnZ = input.getDoubleOr("return_z", getZ());
        returnDimension = input.getStringOr("return_dimension", level().dimension().identifier().toString());
    }

    public LaunchState launchState() {
        return LaunchState.byId(entityData.get(DATA_LAUNCH_STATE));
    }

    public int countdownTicks() {
        return entityData.get(DATA_COUNTDOWN_TICKS);
    }

    public int launchTicks() {
        return entityData.get(DATA_LAUNCH_TICKS);
    }

    public int countdownSecondsRemaining() {
        return Math.max(1, (int) Math.ceil(countdownTicks() / 20.0D));
    }

    public void playStagedFeedback() {
        playFeedback(SoundEvents.BEACON_ACTIVATE, 0.55F, 1.65F);
        sendPadParticles(ParticleTypes.CLOUD, 16, 0.28D, 0.03D);
    }

    private boolean startCountdown(Player player) {
        if (launchState() == LaunchState.COUNTDOWN) {
            sendStatus(player, "Countdown already running. T-minus " + countdownSecondsRemaining() + ".");
            return true;
        }
        if (launchState() == LaunchState.LAUNCHING) {
            return false;
        }

        LaunchReadiness readiness = LaunchReadiness.evaluateForLaunch(player);
        if (!readiness.ready()) {
            player.sendSystemMessage(Component.literal("ECHO-7 // Launch hold. Fix these checks before starting countdown:"));
            readiness.missing().stream().limit(8).forEach(player::sendSystemMessage);
            return false;
        }

        countdownPassenger = player.getUUID();
        countdownPassengerEntity = player;
        committedPassenger = null;
        committedPassengerEntity = null;
        setLaunchState(LaunchState.COUNTDOWN);
        setCountdownTicks(COUNTDOWN_TICKS);
        setLaunchTicks(0);
        playFeedback(SoundEvents.NOTE_BLOCK_PLING, 0.7F, 0.65F);
        sendPadParticles(ParticleTypes.CLOUD, 12, 0.2D, 0.02D);
        player.sendSystemMessage(Component.literal("ECHO-7 // Countdown armed. T-minus 5."));
        sendStatus(player, "T-minus 5. Stay seated until ignition.");
        return true;
    }

    private void tickCountdown() {
        if (!(getFirstPassenger() instanceof Player player)) {
            abortCountdown();
            return;
        }

        int ticks = countdownTicks() - 1;
        setCountdownTicks(Math.max(0, ticks));
        if (ticks > 0 && ticks % 20 == 0) {
            playFeedback(SoundEvents.NOTE_BLOCK_HAT, 0.7F, 0.85F + (5 - countdownSecondsRemaining()) * 0.08F);
            sendPadParticles(ParticleTypes.CLOUD, 8 + (5 - countdownSecondsRemaining()) * 2, 0.22D, 0.02D);
            sendStatus(player, "T-minus " + countdownSecondsRemaining() + ".");
        }
        if (ticks <= 0) {
            committedPassenger = player.getUUID();
            committedPassengerEntity = player;
            countdownPassenger = null;
            countdownPassengerEntity = null;
            setLaunchState(LaunchState.LAUNCHING);
            launchTicks = 0;
            setLaunchTicks(0);
            playFeedback(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.15F, 0.72F);
            playFeedback(SoundEvents.GENERIC_EXPLODE, 0.45F, 1.6F);
            sendPadParticles(ParticleTypes.FLAME, 36, 0.35D, 0.07D);
            sendPadParticles(ParticleTypes.CLOUD, 28, 0.5D, 0.08D);
            player.sendSystemMessage(Component.literal("ECHO-7 // Ignition commit. Hold vector."));
            sendStatus(player, "Ascent committed. Orbit handoff in progress.");
        }
    }

    private void tickLaunch() {
        launchTicks++;
        setLaunchTicks(launchTicks);
        setDeltaMovement(0.0D, 0.7D, 0.0D);
        setPos(getX(), getY() + 0.7D, getZ());
        getPassengers().forEach(this::positionRider);
        if (launchTicks % 2 == 0) {
            sendPadParticles(ParticleTypes.FLAME, 8, 0.22D, 0.04D);
            sendPadParticles(ParticleTypes.SMOKE, 5, 0.32D, 0.03D);
        }
        Player statusPlayer = findCommittedPassenger();
        if (launchTicks % 20 == 0 && statusPlayer != null) {
            sendStatus(statusPlayer, "Ascent committed. Orbit handoff in " + Math.max(1, (ASCENT_TICKS - launchTicks + 19) / 20) + "s.");
        }
        if (launchTicks >= ASCENT_TICKS) {
            Player player = findCommittedPassenger();
            if (player != null) {
                player.stopRiding();
                EmergencyRocketLaunch.launchToLowOrbit(player, returnX, returnY, returnZ, returnDimension);
                playFeedback(SoundEvents.BEACON_ACTIVATE, 0.9F, 1.9F);
            }
            discard();
        }
    }

    private void abortCountdown() {
        Player player = findCountdownPassenger();
        setLaunchState(LaunchState.PLACED);
        setCountdownTicks(0);
        setLaunchTicks(0);
        countdownPassenger = null;
        committedPassenger = null;
        countdownPassengerEntity = null;
        committedPassengerEntity = null;
        playFeedback(SoundEvents.UI_BUTTON_CLICK, 0.75F, 0.55F);
        sendPadParticles(ParticleTypes.SMOKE, 12, 0.25D, 0.02D);
        if (player != null) {
            player.sendSystemMessage(Component.literal("ECHO-7 // Countdown aborted. Board again when ready to launch."));
        }
    }

    @Nullable
    private Player findCountdownPassenger() {
        if (getFirstPassenger() instanceof Player player) {
            return player;
        }
        if (countdownPassengerEntity != null && !countdownPassengerEntity.isRemoved()) {
            return countdownPassengerEntity;
        }
        return playerByUuid(countdownPassenger);
    }

    @Nullable
    private Player findCommittedPassenger() {
        if (getFirstPassenger() instanceof Player player) {
            return player;
        }
        if (committedPassengerEntity != null && !committedPassengerEntity.isRemoved()) {
            return committedPassengerEntity;
        }
        return playerByUuid(committedPassenger);
    }

    @Nullable
    private Player playerByUuid(@Nullable UUID uuid) {
        if (uuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(uuid);
        return player != null ? player : null;
    }

    private void sendStatus(Player player, String message) {
        Component component = Component.literal("ECHO-7 // " + message);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(component, true);
        } else {
            player.sendSystemMessage(component);
        }
    }

    private void playFeedback(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, volume, pitch);
    }

    private void playFeedback(net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound, float volume, float pitch) {
        playFeedback(sound.value(), volume, pitch);
    }

    private void sendPadParticles(net.minecraft.core.particles.ParticleOptions particle, int count, double spread, double speed) {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, getX(), getY() - 0.1D, getZ(), count, spread, 0.08D, spread, speed);
        }
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

    private void setLaunchState(LaunchState state) {
        entityData.set(DATA_LAUNCH_STATE, state.id());
    }

    private void setCountdownTicks(int ticks) {
        entityData.set(DATA_COUNTDOWN_TICKS, ticks);
    }

    private void setLaunchTicks(int ticks) {
        entityData.set(DATA_LAUNCH_TICKS, ticks);
    }

    public enum LaunchState {
        PLACED(0),
        COUNTDOWN(1),
        LAUNCHING(2);

        private static final LaunchState[] BY_ID = values();
        private final int id;

        LaunchState(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        static LaunchState byId(int id) {
            return id >= 0 && id < BY_ID.length ? BY_ID[id] : PLACED;
        }
    }
}
