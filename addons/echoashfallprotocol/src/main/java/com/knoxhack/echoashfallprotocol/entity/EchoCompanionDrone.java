package com.knoxhack.echoashfallprotocol.entity;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneMarker;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneMode;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneScanCategory;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneUpgrade;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.entity.drone.CompanionDroneData;
import com.knoxhack.echoashfallprotocol.entity.drone.CompanionDroneStateStore;
import com.knoxhack.echoashfallprotocol.entity.drone.DroneScanService;
import com.knoxhack.echoashfallprotocol.entity.drone.DroneWarningService;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreExplorationRuntime;
import com.knoxhack.echoashfallprotocol.registry.DroneTags;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ECHO-7's portable AI companion drone with repair progression and multiple modes.
 */
public class EchoCompanionDrone extends Mob {

    public enum DroneMode {
        FOLLOW("Follow", "Following at close range"),
        SCOUT("Scout", "Flying ahead to detect threats"),
        COMBAT("Combat", "Engaging hostile entities"),
        SCAVENGE("Scavenge", "Collecting debris and items"),
        PATROL("Patrol", "Circling area for defense");

        private final String displayName;
        private final String description;

        DroneMode(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    // Synched data for client-server sync
    private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(EchoCompanionDrone.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_LIGHT = SynchedEntityData.defineId(EchoCompanionDrone.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_OWNER = SynchedEntityData.defineId(EchoCompanionDrone.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_REPAIR_LEVEL = SynchedEntityData.defineId(EchoCompanionDrone.class, EntityDataSerializers.INT);
    // ECHO-7 voice linkage synced data
    private static final EntityDataAccessor<Integer> DATA_MOOD = SynchedEntityData.defineId(EchoCompanionDrone.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_SPEECH_TEXT = SynchedEntityData.defineId(EchoCompanionDrone.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SPEECH_TICKS = SynchedEntityData.defineId(EchoCompanionDrone.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ALERT_FLASH = SynchedEntityData.defineId(EchoCompanionDrone.class, EntityDataSerializers.INT);

    // Mood enum mirroring EchoPersonality.Mood ordinals (avoids client->server dep cycle)
    public static final int MOOD_PROFESSIONAL = 1;
    public static final int MOOD_CHEERFUL = 0;
    public static final int MOOD_CONCERNED = 2;
    public static final int MOOD_URGENT = 3;
    public static final int MOOD_REFLECTIVE = 4;
    public static final int MOOD_SARCASTIC = 5;

    // Repair thresholds
    public static final int REPAIR_FOLLOW = 25;
    public static final int REPAIR_SCOUT = 50;
    public static final int REPAIR_INVENTORY = 75;
    public static final int REPAIR_FULL = 100;

    private static final double FOLLOW_STOP_DISTANCE_SQR = 9.0D;
    private static final double FOLLOW_SLOW_DISTANCE_SQR = 49.0D;
    private static final double FOLLOW_TELEPORT_DISTANCE_SQR = 1600.0D;
    private static final double SCAVENGE_SCAN_RADIUS = 4.0D;
    private static final double SCAVENGE_COLLECT_DISTANCE_SQR = 3.0D;
    private static final int SCAVENGE_ACTION_COOLDOWN = 40;
    private static final double PATROL_RADIUS = 6.0D;
    private static final double PATROL_RETURN_DISTANCE_SQR = 576.0D;
    private static final double PATROL_THREAT_SCAN_RADIUS = 12.0D;
    private static final int PATROL_TARGET_RESELECT_TICKS = 60;
    private static final int PATROL_ATTACK_COOLDOWN = 20;
    private static final int SCOUT_TRAVEL_TIMEOUT_TICKS = 120;

    private DroneMode currentMode = DroneMode.FOLLOW;
    private final ItemStack[] inventory = new ItemStack[9];
    private int scavengeCooldown = 0;
    private int patrolRetargetTicks = 0;
    private int patrolAttackCooldown = 0;
    private Vec3 patrolTarget = Vec3.ZERO;
    private Vec3 lastStatePosition = Vec3.ZERO;
    private int fieldStateTick = 0;
    private int stuckTicks = 0;
    private int guardPingCooldown = 0;
    private BlockPos scoutTargetPos = null;
    private boolean scoutScanCompleted = false;
    private int scoutTravelTicks = 0;
    
    // Repair materials needed
    private int denseAlloyChunks = 0;
    private int circuitBoards = 0;
    private int powerCells = 0;
    private static final int NEEDED_ALLOY = 3;
    private static final int NEEDED_CIRCUITS = 2;
    private static final int NEEDED_CELLS = 1;
    
    // Intel handler for faction reconnaissance
    private final com.knoxhack.echoashfallprotocol.entity.drone.DroneIntelHandler intelHandler = new com.knoxhack.echoashfallprotocol.entity.drone.DroneIntelHandler();
    
    // Combat AI for faction-aware targeting
    private final com.knoxhack.echoashfallprotocol.entity.drone.DroneCombatAI combatAI = new com.knoxhack.echoashfallprotocol.entity.drone.DroneCombatAI(this);
    private transient Player cachedOwner;

    public EchoCompanionDrone(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        for (int i = 0; i < inventory.length; i++) {
            inventory[i] = ItemStack.EMPTY;
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean isInWall() {
        return false;
    }

    protected boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Player owner = getOwnerAttacker(source.getEntity());
        if (owner != null) {
            speak("Friendly fire lockout.", MOOD_PROFESSIONAL, 30, 0);
            owner.sendSystemMessage(Component.literal("[ECHO-7 // DRONE] Friendly fire lockout engaged."));
            return false;
        }
        if (getOwnerUUID() != null && amount >= getHealth()) {
            setHealth(1.0F);
            setRepairLevel(Math.max(1, getRepairLevel() - 20));
            forceFollowMode();
            if (!recallToOwner()) {
                speak("Critical damage bypassed. Repair required.", MOOD_URGENT, 60, 12);
            }
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MODE, 0);
        builder.define(DATA_LIGHT, false);
        builder.define(DATA_OWNER, "");
        builder.define(DATA_REPAIR_LEVEL, 15); // Start at 15% damaged
        builder.define(DATA_MOOD, MOOD_PROFESSIONAL);
        builder.define(DATA_SPEECH_TEXT, "");
        builder.define(DATA_SPEECH_TICKS, 0);
        builder.define(DATA_ALERT_FLASH, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    public void setOwner(Player player) {
        this.entityData.set(DATA_OWNER, player.getUUID().toString());
        this.cachedOwner = player;
        if (!level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            CompanionDroneData data = CompanionDroneStateStore.get(serverPlayer);
            data.setOwnerUuid(player.getUUID());
            data.setDroneUuid(getUUID());
            data.setDeployed(true);
            data.setLastKnown(level().dimension(), blockPosition());
            CompanionDroneStateStore.save(serverPlayer, data);
        }
    }

    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(DATA_OWNER, uuid != null ? uuid.toString() : "");
        this.cachedOwner = null;
    }

    public UUID getOwnerUUID() {
        String uuidStr = this.entityData.get(DATA_OWNER);
        return uuidStr != null && !uuidStr.isEmpty() ? UUID.fromString(uuidStr) : null;
    }

    private Player getOwnerAttacker(Entity entity) {
        return entity instanceof Player player && player.getUUID().equals(getOwnerUUID()) ? player : null;
    }

    public DroneMode getCurrentMode() {
        int ordinal = this.entityData.get(DATA_MODE);
        DroneMode[] modes = DroneMode.values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : DroneMode.FOLLOW;
    }

    public void setCurrentMode(DroneMode mode) {
        if (mode == null) {
            mode = DroneMode.FOLLOW;
        }
        if (canSwitchToMode(mode)) {
            this.currentMode = mode;
            this.entityData.set(DATA_MODE, mode.ordinal());
            if (mode != DroneMode.COMBAT) {
                clearCombatState();
            }
            syncLegacyModeToFieldState(mode);
        }
    }

    public void cycleMode() {
        DroneMode[] modes = DroneMode.values();
        int start = getCurrentMode().ordinal();
        for (int offset = 1; offset <= modes.length; offset++) {
            DroneMode next = modes[(start + offset) % modes.length];
            if (canSwitchToMode(next)) {
                setCurrentMode(next);
                return;
            }
        }
        forceFollowMode();
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    public void toggleLight() {
        this.entityData.set(DATA_LIGHT, !this.entityData.get(DATA_LIGHT));
    }

    public boolean isLightEnabled() {
        return this.entityData.get(DATA_LIGHT);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            UUID owner = getOwnerUUID();

            // First-time bond: if unowned, claim it
            if (owner == null) {
                setOwnerUUID(player.getUUID());
                serverPlayer.sendSystemMessage(Component.literal("\u00a7b[ECHO-7 // DRONE]\u00a7r Bio-signature registered. Linking to terminal network..."));
                return InteractionResult.SUCCESS;
            }

            // Only owner can interact
            if (!owner.equals(player.getUUID())) {
                return InteractionResult.PASS;
            }

            // Right-click with repair items to repair the drone
            ItemStack held = player.getItemInHand(hand);
            int currentRepair = getRepairLevel();
            if (currentRepair < REPAIR_FULL) {
                if (held.is(com.knoxhack.echoashfallprotocol.registry.ModItems.DENSE_ALLOY_CHUNK.get())) {
                    held.shrink(1);
                    denseAlloyChunks++;
                    if (denseAlloyChunks >= NEEDED_ALLOY) {
                        denseAlloyChunks -= NEEDED_ALLOY;
                        setRepairLevel(currentRepair + 10);
                        recordRepairProgress(serverPlayer, "dense_alloy_chunk");
                        serverPlayer.sendSystemMessage(Component.literal("\u00a7b[ECHO-7 // DRONE]\u00a7r Hull repair applied. Integrity: " + getRepairLevel() + "%"));
                    } else {
                        serverPlayer.sendSystemMessage(Component.literal("\u00a77[ECHO-7 // DRONE]\u00a7r Hull components accepted. (" + denseAlloyChunks + "/" + NEEDED_ALLOY + ")"));
                    }
                    return InteractionResult.SUCCESS;
                }
                if (held.is(com.knoxhack.echoashfallprotocol.registry.ModItems.CIRCUIT_BOARD.get())) {
                    held.shrink(1);
                    circuitBoards++;
                    if (circuitBoards >= NEEDED_CIRCUITS) {
                        circuitBoards -= NEEDED_CIRCUITS;
                        setRepairLevel(currentRepair + 15);
                        recordRepairProgress(serverPlayer, "circuit_board");
                        serverPlayer.sendSystemMessage(Component.literal("\u00a7b[ECHO-7 // DRONE]\u00a7r Systems online. Integrity: " + getRepairLevel() + "%"));
                    } else {
                        serverPlayer.sendSystemMessage(Component.literal("\u00a77[ECHO-7 // DRONE]\u00a7r Circuit components accepted. (" + circuitBoards + "/" + NEEDED_CIRCUITS + ")"));
                    }
                    return InteractionResult.SUCCESS;
                }
                if (held.is(com.knoxhack.echoashfallprotocol.registry.ModItems.ENERGY_CELL.get())) {
                    held.shrink(1);
                    powerCells++;
                    if (powerCells >= NEEDED_CELLS) {
                        powerCells -= NEEDED_CELLS;
                        setRepairLevel(currentRepair + 20);
                        recordRepairProgress(serverPlayer, "energy_cell");
                        serverPlayer.sendSystemMessage(Component.literal("\u00a7b[ECHO-7 // DRONE]\u00a7r Power systems restored. Integrity: " + getRepairLevel() + "%"));
                    } else {
                        serverPlayer.sendSystemMessage(Component.literal("\u00a77[ECHO-7 // DRONE]\u00a7r Power components accepted. (" + powerCells + "/" + NEEDED_CELLS + ")"));
                    }
                    return InteractionResult.SUCCESS;
                }
            }

            // Shift + right-click: cycle drone mode (if unlocked)
            if (player.isShiftKeyDown()) {
                cycleMode();
                serverPlayer.sendSystemMessage(Component.literal("\u00a7b[ECHO-7 // DRONE]\u00a7r Mode: " + getCurrentMode().getDisplayName()), true);
                return InteractionResult.SUCCESS;
            }

            // Plain right-click: point players to the modular terminal owner.
            serverPlayer.sendSystemMessage(Component.literal(
                    "\u00a7b[ECHO-7 // DRONE]\u00a7r Press M with ECHO: Terminal installed for full command access."));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    
    // Repair system methods
    public int getRepairLevel() {
        return this.entityData.get(DATA_REPAIR_LEVEL);
    }
    
    public void setRepairLevel(int level) {
        this.entityData.set(DATA_REPAIR_LEVEL, Math.min(level, REPAIR_FULL));
    }

    private void recordRepairProgress(ServerPlayer player, String source) {
        CompanionDroneData data = CompanionDroneStateStore.get(player);
        data.setHealth(Math.max(data.getHealth(), getRepairLevel()));
        CompanionDroneStateStore.link(player, this, data);
        CompanionDroneStateStore.save(player, data);

        QuestData quest = QuestData.get(player);
        if (quest.getDroneHealth() < getRepairLevel()) {
            quest.setDroneHealth(getRepairLevel());
            QuestData.saveAndSync(player, quest);
        }

        AshfallAdapterCoreExplorationRuntime.droneState(
                player,
                "repair",
                getCurrentMode().name(),
                getRepairLevel() >= REPAIR_FOLLOW,
                Map.of("repairLevel", getRepairLevel(), "source", source));
    }
    
    public boolean isModeUnlocked(DroneMode mode) {
        int repair = getRepairLevel();
        return switch (mode) {
            case FOLLOW -> true;
            case SCOUT, COMBAT -> repair >= REPAIR_SCOUT;
            case SCAVENGE -> repair >= REPAIR_INVENTORY;
            case PATROL -> repair >= REPAIR_FULL;
        };
    }
    
    public boolean canSwitchToMode(DroneMode mode) {
        return isModeUnlocked(mode);
    }

    public void forceFollowMode() {
        this.currentMode = DroneMode.FOLLOW;
        this.entityData.set(DATA_MODE, DroneMode.FOLLOW.ordinal());
        clearCombatState();
        syncLegacyModeToFieldState(DroneMode.FOLLOW);
    }

    public boolean recallToOwner() {
        return recallTo(getOwner());
    }

    public boolean recallTo(Player owner) {
        if (owner == null || owner.isRemoved()) {
            return false;
        }

        forceFollowMode();
        Vec3 target = CompanionDroneStateStore.safeHoverTarget(owner);
        this.teleportTo(target.x, target.y, target.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.speak("Recall acknowledged.", MOOD_PROFESSIONAL, 40, 8);
        if (owner instanceof ServerPlayer serverPlayer) {
            CompanionDroneData data = CompanionDroneStateStore.get(serverPlayer);
            data.setMode(EchoDroneMode.FOLLOW);
            data.setTaskLabel(EchoDroneMode.FOLLOW.taskLabel());
        data.setReturningToOwner(false);
        data.setPathingStuck(false);
        data.clearTarget();
        scoutTargetPos = null;
        scoutScanCompleted = false;
        scoutTravelTicks = 0;
        CompanionDroneStateStore.link(serverPlayer, this, data);
        CompanionDroneStateStore.save(serverPlayer, data);
        }
        return true;
    }

    public void applyFieldAssistantState(CompanionDroneData data) {
        if (data == null) {
            return;
        }
        DroneMode legacy = legacyModeFor(data.getMode());
        if (legacy != null && canSwitchToMode(legacy)) {
            this.currentMode = legacy;
            this.entityData.set(DATA_MODE, legacy.ordinal());
        }
        if (data.hasTargetPosition()) {
            scoutTargetPos = data.targetPosition();
            scoutScanCompleted = false;
            scoutTravelTicks = 0;
        } else if (data.getMode() != EchoDroneMode.SCOUT) {
            scoutTargetPos = null;
            scoutScanCompleted = false;
            scoutTravelTicks = 0;
        }
    }

    public EchoDroneMode getFieldAssistantMode() {
        Player owner = getOwner();
        if (owner instanceof ServerPlayer serverPlayer) {
            return CompanionDroneStateStore.get(serverPlayer).getMode();
        }
        return fieldModeFor(getCurrentMode());
    }

    public void beginScoutAhead(BlockPos target) {
        scoutTargetPos = target == null ? null : target.immutable();
        scoutScanCompleted = false;
        scoutTravelTicks = 0;
        if (canSwitchToMode(DroneMode.SCOUT)) {
            this.currentMode = DroneMode.SCOUT;
            this.entityData.set(DATA_MODE, DroneMode.SCOUT.ordinal());
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        UUID owner = getOwnerUUID();
        if (owner != null) {
            output.putString("OwnerUUID", owner.toString());
        }
        output.putInt("Mode", getCurrentMode().ordinal());
        output.putBoolean("LightEnabled", isLightEnabled());
        output.putInt("RepairLevel", getRepairLevel());
        output.putInt("DenseAlloyChunks", denseAlloyChunks);
        output.putInt("CircuitBoards", circuitBoards);
        output.putInt("PowerCells", powerCells);
        output.putInt("Mood", getMoodId());
        output.putString("SpeechText", getSpeechText());
        output.putInt("SpeechTicks", getSpeechTicks());
        output.putInt("AlertFlash", getAlertFlash());
        ValueOutput.TypedOutputList<ItemStack> cargo = output.list("DroneCargo", ItemStack.CODEC);
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                cargo.add(stack.copy());
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getString("OwnerUUID").ifPresent(uuid -> {
            try {
                setOwnerUUID(UUID.fromString(uuid));
            } catch (IllegalArgumentException ignored) {
                setOwnerUUID(null);
            }
        });
        int modeOrdinal = input.getIntOr("Mode", DroneMode.FOLLOW.ordinal());
        DroneMode[] modes = DroneMode.values();
        DroneMode loadedMode = modeOrdinal >= 0 && modeOrdinal < modes.length ? modes[modeOrdinal] : DroneMode.FOLLOW;
        this.entityData.set(DATA_MODE, loadedMode.ordinal());
        this.currentMode = loadedMode;
        this.entityData.set(DATA_LIGHT, input.getBooleanOr("LightEnabled", false));
        setRepairLevel(input.getIntOr("RepairLevel", 15));
        denseAlloyChunks = input.getIntOr("DenseAlloyChunks", 0);
        circuitBoards = input.getIntOr("CircuitBoards", 0);
        powerCells = input.getIntOr("PowerCells", 0);
        setMoodId(input.getIntOr("Mood", MOOD_PROFESSIONAL));
        this.entityData.set(DATA_SPEECH_TEXT, input.getStringOr("SpeechText", ""));
        this.entityData.set(DATA_SPEECH_TICKS, input.getIntOr("SpeechTicks", 0));
        this.entityData.set(DATA_ALERT_FLASH, input.getIntOr("AlertFlash", 0));
        for (int i = 0; i < inventory.length; i++) {
            inventory[i] = ItemStack.EMPTY;
        }
        int cargoSlot = 0;
        for (ItemStack stack : input.listOrEmpty("DroneCargo", ItemStack.CODEC)) {
            if (!stack.isEmpty() && cargoSlot < inventory.length) {
                inventory[cargoSlot++] = stack.copy();
            }
        }
        if (!isModeUnlocked(getCurrentMode())) {
            forceFollowMode();
        }
    }
    
    @Nullable
    public Player getOwner() {
        UUID ownerId = getOwnerUUID();
        if (ownerId == null) return null;
        if (this.cachedOwner != null && !this.cachedOwner.isRemoved() && ownerId.equals(this.cachedOwner.getUUID())) {
            return this.cachedOwner;
        }
        Player owner = this.level().getPlayerByUUID(ownerId);
        if (owner != null) {
            this.cachedOwner = owner;
        }
        return owner;
    }

    // --- ECHO-7 Voice Linkage ---

    public int getMoodId() { return this.entityData.get(DATA_MOOD); }
    public void setMoodId(int mood) { this.entityData.set(DATA_MOOD, mood); }

    public String getSpeechText() { return this.entityData.get(DATA_SPEECH_TEXT); }
    public int getSpeechTicks() { return this.entityData.get(DATA_SPEECH_TICKS); }
    public boolean isSpeaking() { return getSpeechTicks() > 0 && !getSpeechText().isEmpty(); }

    public int getAlertFlash() { return this.entityData.get(DATA_ALERT_FLASH); }
    public void triggerAlert(int ticks) { this.entityData.set(DATA_ALERT_FLASH, Math.max(getAlertFlash(), ticks)); }

    /**
     * Have the drone "speak" — sets hologram text for a duration.
     * Server-side only. Clients receive via synched data.
     */
    public void speak(String stripped, int moodId, int holdTicks, int alertFlashTicks) {
        if (this.level().isClientSide()) return;
        String clamped = stripped == null ? "" : stripped;
        if (clamped.length() > 140) clamped = clamped.substring(0, 140) + "...";
        this.entityData.set(DATA_SPEECH_TEXT, clamped);
        this.entityData.set(DATA_SPEECH_TICKS, Math.max(20, holdTicks));
        this.entityData.set(DATA_MOOD, moodId);
        if (alertFlashTicks > 0) {
            triggerAlert(alertFlashTicks);
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Reset fall distance to prevent any accumulated fall damage
        this.fallDistance = 0.0F;
        // Ensure we're in a valid mode based on repair level
        if (!isModeUnlocked(getCurrentMode())) {
            forceFollowMode();
        }

        // Server-side countdown for speech + alert
        if (!this.level().isClientSide()) {
            DroneMode mode = getCurrentMode();
            ServerLevel serverLevel = this.level() instanceof ServerLevel currentLevel ? currentLevel : null;
            ServerPlayer owner = getOwner() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            EchoDroneMode fieldMode = owner == null ? fieldModeFor(mode) : CompanionDroneStateStore.get(owner).getMode();
            if (serverLevel != null && owner != null) {
                tickFieldAssistantState(serverLevel, owner, fieldMode);
            }

            if (fieldMode == EchoDroneMode.GUARD && mode != DroneMode.COMBAT && isFieldGuardOnly(owner)) {
                if (serverLevel != null) {
                    tickGuardMode(serverLevel);
                } else {
                    tickOwnerFollow();
                }
            } else if (fieldMode == EchoDroneMode.SCOUT && scoutTargetPos != null && serverLevel != null) {
                tickScoutAhead(serverLevel);
            } else if (mode == DroneMode.FOLLOW || fieldMode == EchoDroneMode.ASSIST
                    || fieldMode == EchoDroneMode.DOCK || fieldMode == EchoDroneMode.RECALL) {
                tickOwnerFollow();
            } else if ((mode == DroneMode.SCAVENGE || fieldMode == EchoDroneMode.SALVAGE) && serverLevel != null) {
                tickScavengeMode(serverLevel);
            } else if (mode == DroneMode.PATROL && serverLevel != null) {
                tickPatrolMode(serverLevel);
            } else if (mode != DroneMode.COMBAT) {
                clearCombatState();
            }

            int speech = this.entityData.get(DATA_SPEECH_TICKS);
            if (speech > 0) {
                this.entityData.set(DATA_SPEECH_TICKS, speech - 1);
                if (speech - 1 <= 0) {
                    this.entityData.set(DATA_SPEECH_TEXT, "");
                }
            }
            int alert = this.entityData.get(DATA_ALERT_FLASH);
            if (alert > 0) {
                this.entityData.set(DATA_ALERT_FLASH, alert - 1);
            }
            
            // Faction intel gathering based on drone mode
            if (serverLevel != null) {
                switch (mode) {
                    case SCOUT -> {
                        intelHandler.tickScoutMode(this, serverLevel);
                    }
                    case COMBAT -> {
                        intelHandler.tickCombatMode(this, serverLevel);
                        combatAI.tickCombat(serverLevel); // Faction-aware targeting + abilities
                    }
                    case PATROL -> intelHandler.tickCombatMode(this, serverLevel);
                    default -> {}
                }
                if (mode == DroneMode.SCOUT || mode == DroneMode.PATROL) {
                    intelHandler.updateDossierFromProximity(this, serverLevel);
                }
                // Always try to intercept transmissions (all modes)
                intelHandler.tryInterceptTransmission(this, serverLevel);
            }
        }
    }

    private void tickFieldAssistantState(ServerLevel level, ServerPlayer owner, EchoDroneMode fieldMode) {
        fieldStateTick++;
        if (guardPingCooldown > 0) {
            guardPingCooldown--;
        }

        CompanionDroneData data = CompanionDroneStateStore.get(owner);
        data.setOwnerUuid(owner.getUUID());
        data.setDroneUuid(getUUID());
        data.setDeployed(true);
        data.setHealth(getRepairLevel());
        data.setLastKnown(level.dimension(), blockPosition());

        updateSignal(data, owner);
        updatePathingStuck(data, owner);
        updateBattery(data, owner, fieldMode, level.getGameTime());

        if (fieldMode == EchoDroneMode.RECALL || data.isReturningToOwner()) {
            tickOwnerFollow();
        }
        if (data.getBatteryPercent() <= 0 && Config.ENABLE_DRONE_BATTERY.get()) {
            data.setMode(EchoDroneMode.FOLLOW);
            data.setTaskLabel("Low power return");
            forceFollowMode();
        }

        if (fieldStateTick % 20 == 0) {
            CompanionDroneStateStore.save(owner, data);
        }
        DroneWarningService.tickWarnings(this, level, owner);
    }

    private void updateSignal(CompanionDroneData data, ServerPlayer owner) {
        if (!Config.ENABLE_DRONE_SIGNAL.get()) {
            data.setSignalQuality(100);
            return;
        }
        double distance = Math.sqrt(distanceToSqr(owner));
        int signal = 100 - (int)Math.max(0.0D, (distance - 8.0D) * 2.5D);
        if (!owner.level().dimension().equals(level().dimension())) {
            signal = 0;
        }
        if (data.hasUpgrade(com.knoxhack.echoashfallprotocol.api.drone.EchoDroneUpgrade.SIGNAL_ANTENNA)) {
            signal += 18;
        }
        data.setSignalQuality(signal);
    }

    private void updateBattery(CompanionDroneData data, ServerPlayer owner, EchoDroneMode fieldMode, long gameTime) {
        if (!Config.ENABLE_DRONE_BATTERY.get()) {
            data.setBatteryPercent(100);
            return;
        }
        int interval = data.hasUpgrade(com.knoxhack.echoashfallprotocol.api.drone.EchoDroneUpgrade.STABILIZED_BATTERY) ? 300 : 220;
        if (gameTime % interval != 0L) {
            return;
        }
        if (distanceToSqr(owner) <= 16.0D || fieldMode == EchoDroneMode.DOCK) {
            data.setBatteryPercent(data.getBatteryPercent() + (fieldMode == EchoDroneMode.DOCK ? 3 : 1));
            return;
        }
        int drain = switch (fieldMode) {
            case SCOUT, SALVAGE, GUARD -> 2;
            default -> 1;
        };
        data.setBatteryPercent(data.getBatteryPercent() - drain);
    }

    private void updatePathingStuck(CompanionDroneData data, ServerPlayer owner) {
        if (fieldStateTick % 20 != 0) {
            return;
        }
        double moved = lastStatePosition == Vec3.ZERO ? Double.MAX_VALUE : lastStatePosition.distanceToSqr(position());
        boolean shouldBeMoving = distanceToSqr(owner) > 100.0D || data.isReturningToOwner();
        if (shouldBeMoving && moved < 0.04D) {
            stuckTicks += 20;
        } else {
            stuckTicks = Math.max(0, stuckTicks - 20);
        }
        lastStatePosition = position();
        boolean stuck = stuckTicks >= 120;
        data.setPathingStuck(stuck);
        if (stuck) {
            Vec3 target = CompanionDroneStateStore.safeHoverTarget(owner);
            setPos(target.x, target.y, target.z);
            setDeltaMovement(Vec3.ZERO);
            speak("Pathing recovered.", MOOD_CONCERNED, 35, 6);
            stuckTicks = 0;
        }
    }

    private void tickScoutAhead(ServerLevel level) {
        Player owner = getOwner();
        if (owner == null || scoutTargetPos == null) {
            forceFollowMode();
            return;
        }
        if (returnToOwnerIfFar(owner, 2500.0D)) {
            return;
        }
        if (owner instanceof ServerPlayer serverPlayer) {
            CompanionDroneData data = CompanionDroneStateStore.get(serverPlayer);
            if (Config.ENABLE_DRONE_SIGNAL.get() && data.getSignalQuality() < 15) {
                data.setMode(EchoDroneMode.FOLLOW);
                data.setTaskLabel("Scout failed: signal blocked");
                data.clearTarget();
                CompanionDroneStateStore.save(serverPlayer, data);
                speak("Scout failed: signal blocked.", MOOD_CONCERNED, 45, 8);
                serverPlayer.sendSystemMessage(Component.literal("[ECHO-7 // DRONE] Scout failed: signal blocked."));
                scoutTargetPos = null;
                scoutScanCompleted = false;
                scoutTravelTicks = 0;
                setLegacyFollowModeOnly();
                return;
            }
        }
        Vec3 target = Vec3.atCenterOf(scoutTargetPos).add(0.0D, 0.75D, 0.0D);
        moveToward(target, 0.28D);
        getLookControl().setLookAt(target.x, target.y, target.z, 25.0F, getMaxHeadXRot());
        scoutTravelTicks++;
        boolean arrived = target.distanceToSqr(position()) <= 4.0D;
        boolean timedOut = scoutTravelTicks >= SCOUT_TRAVEL_TIMEOUT_TICKS;
        if (!scoutScanCompleted && (arrived || timedOut)) {
            if (owner instanceof ServerPlayer serverPlayer) {
                DroneScanService.scanArea(serverPlayer, this, scoutTargetPos, true);
                CompanionDroneData data = CompanionDroneStateStore.get(serverPlayer);
                data.setMode(EchoDroneMode.FOLLOW);
                data.setTaskLabel("Returning from scout");
                data.clearTarget();
                CompanionDroneStateStore.save(serverPlayer, data);
            }
            scoutScanCompleted = true;
            scoutTargetPos = null;
            scoutTravelTicks = 0;
            setLegacyFollowModeOnly();
            return;
        }
        if (scoutScanCompleted) {
            if (owner instanceof ServerPlayer serverPlayer) {
                CompanionDroneData data = CompanionDroneStateStore.get(serverPlayer);
                data.setMode(EchoDroneMode.FOLLOW);
                data.setTaskLabel("Returning from scout");
                data.clearTarget();
                CompanionDroneStateStore.save(serverPlayer, data);
            }
            scoutTargetPos = null;
            scoutScanCompleted = false;
            scoutTravelTicks = 0;
        }
    }

    private void tickGuardMode(ServerLevel level) {
        Player owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            clearCombatState();
            return;
        }
        clearCombatState();
        if (returnToOwnerIfFar(owner, PATROL_RETURN_DISTANCE_SQR)) {
            return;
        }
        Mob threat = findNearestPatrolThreat(level, owner);
        if (threat != null) {
            getLookControl().setLookAt(threat, 30.0F, getMaxHeadXRot());
            moveToward(owner.position().add(0.0D, 1.75D, 0.0D), 0.18D);
            if (guardPingCooldown <= 0) {
                speak(threat.getType().builtInRegistryHolder().is(DroneTags.HOSTILE_PRIORITY)
                                ? "Priority hostile marked." : "Hostile movement detected.",
                        MOOD_URGENT, 42, 12);
                if (owner instanceof ServerPlayer serverPlayer) {
                    DroneScanService.publishTemporaryMarkers(serverPlayer, List.of(new EchoDroneMarker(
                            EchoDroneScanCategory.HOSTILE,
                            threat.getType().builtInRegistryHolder().is(DroneTags.HOSTILE_PRIORITY)
                                    ? "Priority hostile" : "Hostile movement",
                            threat.getDisplayName().getString(),
                            level.dimension(),
                            threat.blockPosition(),
                            level.getGameTime() + Math.max(60, Config.DRONE_MARKER_DURATION_TICKS.get() / 2),
                            true)));
                }
                guardPingCooldown = Config.DRONE_WARNING_COOLDOWN_TICKS.get();
            }
            return;
        }
        if (patrolTarget == Vec3.ZERO || patrolRetargetTicks <= 0 || patrolTarget.distanceToSqr(position()) < 2.0D) {
            retargetPatrol(owner, level);
        }
        if (patrolRetargetTicks > 0) {
            patrolRetargetTicks--;
        }
        moveToward(patrolTarget, 0.18D);
        getLookControl().setLookAt(owner, 20.0F, getMaxHeadXRot());
    }

    private boolean isFieldGuardOnly(ServerPlayer owner) {
        if (owner == null) {
            return true;
        }
        CompanionDroneData data = CompanionDroneStateStore.get(owner);
        return !"Patrolling area".equals(data.getTaskLabel());
    }

    private void tickScavengeMode(ServerLevel level) {
        Player owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            clearCombatState();
            return;
        }
        if (!Config.ENABLE_SALVAGE_MODE.get()) {
            speak("Salvage mode disabled.", MOOD_CONCERNED, 30, 0);
            forceFollowMode();
            return;
        }

        clearCombatState();
        if (scavengeCooldown > 0) {
            scavengeCooldown--;
        }

        if (returnToOwnerIfFar(owner, PATROL_RETURN_DISTANCE_SQR)) {
            return;
        }

        if (hasCargo()) {
            if (owner instanceof ServerPlayer serverPlayer) {
                CompanionDroneData data = CompanionDroneStateStore.get(serverPlayer);
                data.setTaskLabel("Returning salvage");
                CompanionDroneStateStore.save(serverPlayer, data);
            }
            Vec3 returnTarget = CompanionDroneStateStore.safeHoverTarget(owner);
            moveToward(returnTarget, 0.26D);
            getLookControl().setLookAt(owner, 20.0F, getMaxHeadXRot());
            if (distanceToSqr(owner) <= 9.0D) {
                deliverCargo(owner);
            }
            return;
        }

        ItemEntity droppedItem = findNearestDroppedItem(level, owner);
        if (droppedItem != null) {
            moveToward(droppedItem.position().add(0.0D, 0.35D, 0.0D), 0.22D);
            if (scavengeCooldown <= 0 && distanceToSqr(droppedItem) <= SCAVENGE_COLLECT_DISTANCE_SQR) {
                collectDroppedItem(droppedItem, owner);
            }
            return;
        }

        BlockPos debris = findNearestDebrisBlock(level);
        if (debris != null) {
            Vec3 target = Vec3.atCenterOf(debris).add(0.0D, 0.45D, 0.0D);
            moveToward(target, 0.22D);
            if (scavengeCooldown <= 0 && target.distanceToSqr(position()) <= 5.0D) {
                level.destroyBlock(debris, true, this);
                scavengeCooldown = SCAVENGE_ACTION_COOLDOWN;
                speak("Debris salvaged.", MOOD_PROFESSIONAL, 30, 0);
            }
            return;
        }

        double angle = (level.getGameTime() + getId() * 17L) * 0.045D;
        moveToward(owner.position().add(Math.cos(angle) * 3.0D, 1.65D, Math.sin(angle) * 3.0D), 0.14D);
    }

    private void tickPatrolMode(ServerLevel level) {
        Player owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            clearCombatState();
            return;
        }

        if (patrolRetargetTicks > 0) {
            patrolRetargetTicks--;
        }
        if (patrolAttackCooldown > 0) {
            patrolAttackCooldown--;
        }

        if (returnToOwnerIfFar(owner, PATROL_RETURN_DISTANCE_SQR)) {
            return;
        }

        Mob threat = findNearestPatrolThreat(level, owner);
        if (threat != null) {
            setTarget(threat);
            setAggressive(true);
            getLookControl().setLookAt(threat, 30.0F, getMaxHeadXRot());
            moveToward(threat.position().add(0.0D, threat.getBbHeight() * 0.5D, 0.0D), 0.34D);
            if (distanceToSqr(threat) <= 6.25D && patrolAttackCooldown <= 0) {
                doHurtTarget(level, threat);
                patrolAttackCooldown = PATROL_ATTACK_COOLDOWN;
            }
            return;
        }

        clearCombatState();
        if (patrolTarget == Vec3.ZERO || patrolRetargetTicks <= 0 || patrolTarget.distanceToSqr(position()) < 2.0D) {
            retargetPatrol(owner, level);
        }
        moveToward(patrolTarget, 0.24D);
        getLookControl().setLookAt(owner, 20.0F, getMaxHeadXRot());
    }

    private boolean returnToOwnerIfFar(Player owner, double softLimitSqr) {
        double distanceSqr = distanceToSqr(owner);
        if (distanceSqr > FOLLOW_TELEPORT_DISTANCE_SQR) {
            Vec3 target = getOwnerHoverTarget(owner);
            setPos(target.x, target.y, target.z);
            setDeltaMovement(Vec3.ZERO);
            speak("Signal reacquired.", MOOD_CONCERNED, 40, 8);
            return true;
        }
        if (distanceSqr > softLimitSqr) {
            moveToward(getOwnerHoverTarget(owner), 0.34D);
            getLookControl().setLookAt(owner, 20.0F, getMaxHeadXRot());
            return true;
        }
        return false;
    }

    private ItemEntity findNearestDroppedItem(ServerLevel level, Player owner) {
        double radius = Math.max(SCAVENGE_SCAN_RADIUS, Config.DRONE_SALVAGE_RADIUS.get());
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(radius),
                item -> item.isAlive()
                        && !item.hasPickUpDelay()
                        && !item.getItem().isEmpty()
                        && !item.getItem().is(DroneTags.IGNORE_ITEMS)
                        && canStoreCargo(item.getItem(), owner)
                        && (item.getItem().is(DroneTags.SALVAGE_ITEMS) || Config.DRONE_ALLOW_NON_SCRAP_PICKUP.get()));
        ItemEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ItemEntity item : items) {
            double distance = distanceToSqr(item);
            if (distance < nearestDistance) {
                nearest = item;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private BlockPos findNearestDebrisBlock(ServerLevel level) {
        BlockPos center = blockPosition();
        int radius = (int) SCAVENGE_SCAN_RADIUS;
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos cursor : BlockPos.betweenClosed(
                center.offset(-radius, -1, -radius),
                center.offset(radius, 2, radius))) {
            if (!level.isLoaded(cursor) || !level.getBlockState(cursor).is(ModBlocks.DEBRIS_BLOCK.get())) {
                continue;
            }
            double dx = cursor.getX() + 0.5D - getX();
            double dy = cursor.getY() + 0.5D - getY();
            double dz = cursor.getZ() + 0.5D - getZ();
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < nearestDistance) {
                nearest = cursor.immutable();
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private void collectDroppedItem(ItemEntity item, Player owner) {
        ItemStack original = item.getItem();
        ItemStack remaining = original.copy();
        int collected = storeCargo(remaining, cargoLimit(owner));

        if (collected > 0) {
            if (remaining.isEmpty()) {
                item.discard();
            } else {
                item.setItem(remaining);
            }
            if (owner instanceof ServerPlayer serverPlayer) {
                CompanionDroneData data = CompanionDroneStateStore.get(serverPlayer);
                data.setTaskLabel("Returning salvage");
                CompanionDroneStateStore.save(serverPlayer, data);
                serverPlayer.sendSystemMessage(Component.literal("[ECHO-7 // DRONE] Secured "
                        + collected + "x " + original.getHoverName().getString() + ". Returning."));
            }
            speak("Salvage secured.", MOOD_PROFESSIONAL, 30, 0);
        } else {
            speak("Cargo full.", MOOD_CONCERNED, 30, 4);
        }

        scavengeCooldown = Math.max(SCAVENGE_ACTION_COOLDOWN, Config.DRONE_PICKUP_COOLDOWN_TICKS.get());
    }

    private boolean hasCargo() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private int cargoLimit(Player owner) {
        int configured = Math.max(1, Config.DRONE_SALVAGE_MAX_CARRY_STACKS.get());
        if (owner instanceof ServerPlayer serverPlayer
                && CompanionDroneStateStore.get(serverPlayer).hasUpgrade(EchoDroneUpgrade.MICRO_CARGO_POD)) {
            return Math.min(inventory.length, Math.max(2, configured));
        }
        return 1;
    }

    private boolean canStoreCargo(ItemStack stack, Player owner) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ItemStack remaining = stack.copy();
        return storeCargoSimulation(remaining, cargoLimit(owner)) > 0;
    }

    private int storeCargoSimulation(ItemStack remaining, int limit) {
        ItemStack[] snapshot = new ItemStack[inventory.length];
        for (int i = 0; i < inventory.length; i++) {
            snapshot[i] = inventory[i].copy();
        }
        int stored = storeCargo(remaining, limit);
        for (int i = 0; i < inventory.length; i++) {
            inventory[i] = snapshot[i];
        }
        return stored;
    }

    private int storeCargo(ItemStack remaining, int limit) {
        if (remaining == null || remaining.isEmpty()) {
            return 0;
        }
        int before = remaining.getCount();
        int slotLimit = Math.max(1, Math.min(limit, inventory.length));
        for (int i = 0; i < slotLimit && !remaining.isEmpty(); i++) {
            ItemStack existing = inventory[i];
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)) {
                int room = existing.getMaxStackSize() - existing.getCount();
                if (room > 0) {
                    int moved = Math.min(room, remaining.getCount());
                    existing.grow(moved);
                    remaining.shrink(moved);
                }
            }
        }
        for (int i = 0; i < slotLimit && !remaining.isEmpty(); i++) {
            if (inventory[i].isEmpty()) {
                inventory[i] = remaining.split(Math.min(remaining.getMaxStackSize(), remaining.getCount()));
            }
        }
        return before - remaining.getCount();
    }

    private void deliverCargo(Player owner) {
        int delivered = 0;
        int dropped = 0;
        List<ItemStack> overflow = new ArrayList<>();
        for (int i = 0; i < inventory.length; i++) {
            ItemStack cargo = inventory[i];
            if (cargo.isEmpty()) {
                continue;
            }
            int cargoCount = cargo.getCount();
            ItemStack remaining = cargo.copy();
            owner.getInventory().add(remaining);
            int inserted = cargoCount - remaining.getCount();
            delivered += Math.max(0, inserted);
            if (!remaining.isEmpty()) {
                overflow.add(remaining.copy());
                dropped += remaining.getCount();
            }
            inventory[i] = ItemStack.EMPTY;
        }
        for (ItemStack stack : overflow) {
            ItemEntity drop = new ItemEntity(owner.level(), owner.getX(), owner.getY() + 0.35D, owner.getZ(), stack.copy());
            drop.setPickUpDelay(10);
            owner.level().addFreshEntity(drop);
        }
        if (owner instanceof ServerPlayer serverPlayer) {
            CompanionDroneData data = CompanionDroneStateStore.get(serverPlayer);
            data.setTaskLabel(EchoDroneMode.SALVAGE.taskLabel());
            CompanionDroneStateStore.save(serverPlayer, data);
            String suffix = dropped > 0 ? " Dropped " + dropped + " near operator." : "";
            serverPlayer.sendSystemMessage(Component.literal("[ECHO-7 // DRONE] Salvage delivered: "
                    + delivered + "." + suffix));
        }
        speak("Salvage delivered.", MOOD_PROFESSIONAL, 34, 0);
    }

    private Mob findNearestPatrolThreat(ServerLevel level, Player owner) {
        Mob nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Mob mob : level.getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(PATROL_THREAT_SCAN_RADIUS),
                mob -> isPatrolThreat(mob, owner))) {
            double distance = mob.distanceToSqr(owner);
            if (distance < nearestDistance) {
                nearest = mob;
                nearestDistance = distance;
            }
        }

        for (Mob mob : level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(PATROL_THREAT_SCAN_RADIUS),
                mob -> isPatrolThreat(mob, owner))) {
            double distance = mob.distanceToSqr(this);
            if (distance < nearestDistance) {
                nearest = mob;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private boolean isPatrolThreat(Mob mob, Player owner) {
        if (mob == this || !mob.isAlive() || mob.isInvisible()) {
            return false;
        }
        if (mob.isAlliedTo(owner) || mob.isAlliedTo(this)) {
            return false;
        }
        return mob instanceof Monster || mob.getTarget() == owner || mob.getTarget() == this;
    }

    private void retargetPatrol(Player owner, ServerLevel level) {
        double angle = (level.getGameTime() * 0.07D) + getId() * 0.31D;
        patrolTarget = owner.position().add(
                Math.cos(angle) * PATROL_RADIUS,
                1.65D + Math.sin(angle * 0.5D) * 0.35D,
                Math.sin(angle) * PATROL_RADIUS);
        patrolRetargetTicks = PATROL_TARGET_RESELECT_TICKS;
    }

    private void moveToward(Vec3 target, double speed) {
        double dx = target.x - getX();
        double dy = target.y - getY();
        double dz = target.z - getZ();
        double distanceSqr = dx * dx + dy * dy + dz * dz;
        if (distanceSqr < 0.0001D) {
            setDeltaMovement(getDeltaMovement().scale(0.35D));
            return;
        }

        double distance = Math.sqrt(distanceSqr);
        double moveX = dx / distance * speed;
        double moveY = dy / distance * Math.min(speed, 0.28D);
        double moveZ = dz / distance * speed;

        // If blocked by a solid block, rise to clear the obstacle
        if (!level().noCollision(this, getBoundingBox().move(moveX, moveY, moveZ))) {
            moveY = Math.min(speed, 0.28D) + 0.12D;
            moveX *= 0.5D;
            moveZ *= 0.5D;
        }

        Vec3 existing = getDeltaMovement().scale(0.35D);
        setDeltaMovement(existing.add(moveX, moveY, moveZ));
    }

    private void tickOwnerFollow() {
        Player owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            return;
        }

        this.setNoGravity(true);
        this.getNavigation().stop();
        clearCombatState();
        this.getLookControl().setLookAt(owner, 20.0F, this.getMaxHeadXRot());

        Vec3 target = getOwnerHoverTarget(owner);
        double targetX = target.x;
        double targetY = target.y;
        double targetZ = target.z;

        double dx = targetX - this.getX();
        double dy = targetY - this.getY();
        double dz = targetZ - this.getZ();
        double distanceSqr = dx * dx + dy * dy + dz * dz;

        if (distanceSqr > FOLLOW_TELEPORT_DISTANCE_SQR) {
            this.setPos(targetX, targetY, targetZ);
            this.setDeltaMovement(Vec3.ZERO);
            this.speak("Signal reacquired.", MOOD_CONCERNED, 40, 8);
            return;
        }

        if (distanceSqr <= FOLLOW_STOP_DISTANCE_SQR) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.35D));
            return;
        }

        double distance = Math.sqrt(distanceSqr);
        double speed = distanceSqr > 100.0D ? 0.38D : distanceSqr > FOLLOW_SLOW_DISTANCE_SQR ? 0.26D : 0.14D;
        double moveX = dx / distance * speed;
        double moveY = dy / distance * Math.min(speed, 0.24D);
        double moveZ = dz / distance * speed;

        // If blocked by a solid block, rise to clear the obstacle
        if (!level().noCollision(this, getBoundingBox().move(moveX, moveY, moveZ))) {
            moveY = Math.min(speed, 0.24D) + 0.12D;
            moveX *= 0.5D;
            moveZ *= 0.5D;
        }

        Vec3 existing = this.getDeltaMovement().scale(0.28D);
        this.setDeltaMovement(existing.add(moveX, moveY, moveZ));
    }

    private Vec3 getOwnerHoverTarget(Player owner) {
        Vec3 base = CompanionDroneStateStore.safeHoverTarget(owner);
        double bob = Math.sin((tickCount + getId() * 13) * 0.08D) * 0.16D;
        Vec3 candidate = base.add(0.0D, bob, 0.0D);
        if (owner.level().isLoaded(BlockPos.containing(candidate)) && owner.level().noCollision(probeBox(candidate))) {
            return candidate;
        }
        return base;
    }

    private AABB probeBox(Vec3 center) {
        double halfWidth = Math.max(0.25D, getBbWidth() * 0.5D);
        double halfHeight = Math.max(0.25D, getBbHeight() * 0.5D);
        return new AABB(center.x - halfWidth, center.y - halfHeight, center.z - halfWidth,
                center.x + halfWidth, center.y + halfHeight, center.z + halfWidth);
    }

    private void clearCombatState() {
        if (this.getTarget() != null) {
            this.setTarget(null);
        }
        this.setAggressive(false);
        combatAI.clearMark();
    }

    private void setLegacyFollowModeOnly() {
        this.currentMode = DroneMode.FOLLOW;
        this.entityData.set(DATA_MODE, DroneMode.FOLLOW.ordinal());
        clearCombatState();
    }

    private static DroneMode legacyModeFor(EchoDroneMode mode) {
        if (mode == null) {
            return DroneMode.FOLLOW;
        }
        return switch (mode) {
            case FOLLOW, ASSIST, DOCK, RECALL, GUARD -> DroneMode.FOLLOW;
            case SCOUT -> DroneMode.SCOUT;
            case SALVAGE -> DroneMode.SCAVENGE;
        };
    }

    private static EchoDroneMode fieldModeFor(DroneMode mode) {
        if (mode == null) {
            return EchoDroneMode.FOLLOW;
        }
        return switch (mode) {
            case FOLLOW -> EchoDroneMode.FOLLOW;
            case SCOUT -> EchoDroneMode.SCOUT;
            case COMBAT, PATROL -> EchoDroneMode.GUARD;
            case SCAVENGE -> EchoDroneMode.SALVAGE;
        };
    }

    private void syncLegacyModeToFieldState(DroneMode mode) {
        if (level().isClientSide()) {
            return;
        }
        Player owner = getOwner();
        if (!(owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        CompanionDroneData data = CompanionDroneStateStore.get(serverPlayer);
        EchoDroneMode fieldMode = fieldModeFor(mode);
        if ((data.getMode() == EchoDroneMode.GUARD && mode == DroneMode.FOLLOW)
                || (data.getMode() == EchoDroneMode.ASSIST && mode == DroneMode.FOLLOW)) {
            return;
        }
        data.setMode(fieldMode);
        data.setTaskLabel(mode == DroneMode.PATROL ? "Patrolling area" : fieldMode.taskLabel());
        CompanionDroneStateStore.save(serverPlayer, data);
    }

    public boolean hasMarkedTarget(Entity entity) {
        return combatAI.isMarked(entity);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }
}
