package com.knoxhack.echo.npcore.entity;

import com.knoxhack.echo.npcore.EchoNpcCore;
import com.knoxhack.echo.npcore.config.EchoNpcCoreConfig;
import com.knoxhack.echo.npcore.profile.EchoNpcProfile;
import com.knoxhack.echo.npcore.profile.EchoNpcBehaviorSettings;
import com.knoxhack.echo.npcore.profile.EchoNpcProfileManager;
import com.knoxhack.echo.npcore.service.EchoNpcInteractionService;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EchoNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> DATA_PROFILE_ID =
            SynchedEntityData.defineId(EchoNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_VISUAL_PROFILE_ID =
            SynchedEntityData.defineId(EchoNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SOURCE_ENTITY_TYPE =
            SynchedEntityData.defineId(EchoNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SOURCE_PROFESSION =
            SynchedEntityData.defineId(EchoNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_PERSISTENT_NPC_ID =
            SynchedEntityData.defineId(EchoNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_FACTION_ID =
            SynchedEntityData.defineId(EchoNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_RELATIONSHIP =
            SynchedEntityData.defineId(EchoNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_DEBUG =
            SynchedEntityData.defineId(EchoNpcEntity.class, EntityDataSerializers.BOOLEAN);
    private BlockPos homePos = BlockPos.ZERO;
    private boolean homeSet;
    private long interactingUntil;
    private long lastAmbientLine;

    public EchoNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PROFILE_ID, EchoNpcProfile.FALLBACK_ID.toString());
        builder.define(DATA_VISUAL_PROFILE_ID, EchoNpcProfile.FALLBACK_ID.toString());
        builder.define(DATA_SOURCE_ENTITY_TYPE, "");
        builder.define(DATA_SOURCE_PROFESSION, "");
        builder.define(DATA_PERSISTENT_NPC_ID, UUID.randomUUID().toString());
        builder.define(DATA_FACTION_ID, "echonpcore:survivors");
        builder.define(DATA_RELATIONSHIP, "neutral");
        builder.define(DATA_DEBUG, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new HomeBoundWanderGoal(this, 0.55D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer serverPlayer) {
            beginInteraction(serverPlayer);
            EchoNpcInteractionService.open(serverPlayer, this);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        tickNpcBehavior(level);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("NpcProfileId", npcProfileId().toString());
        output.putString("VisualProfileId", visualProfileId().toString());
        output.putString("SourceEntityType", sourceEntityType());
        output.putString("SourceProfession", sourceProfession());
        output.putString("PersistentNpcId", persistentNpcId());
        output.putString("FactionId", factionId().toString());
        output.putString("Relationship", relationshipLabel());
        output.putBoolean("DebugFlags", debugFlags());
        output.putBoolean("HomeSet", homeSet);
        output.putInt("HomeX", homePos.getX());
        output.putInt("HomeY", homePos.getY());
        output.putInt("HomeZ", homePos.getZ());
        output.putLong("LastAmbientLine", lastAmbientLine);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_PROFILE_ID, input.getStringOr("NpcProfileId", EchoNpcProfile.FALLBACK_ID.toString()));
        this.entityData.set(DATA_VISUAL_PROFILE_ID, input.getStringOr("VisualProfileId", this.entityData.get(DATA_PROFILE_ID)));
        this.entityData.set(DATA_SOURCE_ENTITY_TYPE, input.getStringOr("SourceEntityType", ""));
        this.entityData.set(DATA_SOURCE_PROFESSION, input.getStringOr("SourceProfession", ""));
        this.entityData.set(DATA_PERSISTENT_NPC_ID, input.getStringOr("PersistentNpcId", UUID.randomUUID().toString()));
        this.entityData.set(DATA_FACTION_ID, input.getStringOr("FactionId", "echonpcore:survivors"));
        this.entityData.set(DATA_RELATIONSHIP, input.getStringOr("Relationship", "neutral"));
        this.entityData.set(DATA_DEBUG, input.getBooleanOr("DebugFlags", false));
        this.homeSet = input.getBooleanOr("HomeSet", false);
        this.homePos = new BlockPos(
                input.getIntOr("HomeX", blockPosition().getX()),
                input.getIntOr("HomeY", blockPosition().getY()),
                input.getIntOr("HomeZ", blockPosition().getZ()));
        this.lastAmbientLine = input.getLongOr("LastAmbientLine", 0L);
        refreshNameplate();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public void configureProfile(Identifier profileId) {
        EchoNpcProfile profile = EchoNpcProfileManager.getOrFallback(profileId);
        this.entityData.set(DATA_PROFILE_ID, profile.id().toString());
        this.entityData.set(DATA_VISUAL_PROFILE_ID, profile.visualProfile().toString());
        this.entityData.set(DATA_FACTION_ID, profile.faction().toString());
        ensureHome();
        refreshNameplate();
    }

    public void setHome(BlockPos pos) {
        homePos = pos == null ? blockPosition() : pos.immutable();
        homeSet = true;
    }

    public BlockPos homePos() {
        ensureHome();
        return homePos;
    }

    public boolean homeSet() {
        return homeSet;
    }

    public void beginInteraction(ServerPlayer player) {
        if (player == null) {
            return;
        }
        interactingUntil = Math.max(interactingUntil, player.level().getGameTime() + 200L);
        getNavigation().stop();
        getLookControl().setLookAt(player, 30.0F, 30.0F);
    }

    public void endInteraction() {
        interactingUntil = 0L;
        getNavigation().stop();
    }

    public void configureConvertedSource(String sourceEntityType, String sourceProfession) {
        this.entityData.set(DATA_SOURCE_ENTITY_TYPE, clean(sourceEntityType));
        this.entityData.set(DATA_SOURCE_PROFESSION, clean(sourceProfession));
    }

    public Identifier npcProfileId() {
        return parse(this.entityData.get(DATA_PROFILE_ID), EchoNpcProfile.FALLBACK_ID);
    }

    public Identifier visualProfileId() {
        return parse(this.entityData.get(DATA_VISUAL_PROFILE_ID), npcProfileId());
    }

    public String sourceEntityType() {
        return this.entityData.get(DATA_SOURCE_ENTITY_TYPE);
    }

    public String sourceProfession() {
        return this.entityData.get(DATA_SOURCE_PROFESSION);
    }

    public String persistentNpcId() {
        String value = this.entityData.get(DATA_PERSISTENT_NPC_ID);
        if (value == null || value.isBlank()) {
            value = UUID.randomUUID().toString();
            this.entityData.set(DATA_PERSISTENT_NPC_ID, value);
        }
        return value;
    }

    public Identifier factionId() {
        return parse(this.entityData.get(DATA_FACTION_ID), Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "survivors"));
    }

    public String relationshipLabel() {
        String value = this.entityData.get(DATA_RELATIONSHIP);
        return value == null || value.isBlank() ? "neutral" : value;
    }

    public boolean debugFlags() {
        return this.entityData.get(DATA_DEBUG);
    }

    public double interactionRange() {
        EchoNpcProfile profile = EchoNpcProfileManager.getOrFallback(npcProfileId());
        return profile.interactionRange() > 0.0D ? profile.interactionRange() : EchoNpcCoreConfig.interactionRange();
    }

    private void tickNpcBehavior(ServerLevel level) {
        EchoNpcProfile profile = EchoNpcProfileManager.getOrFallback(npcProfileId());
        EchoNpcBehaviorSettings behavior = profile.behavior();
        ensureHome();
        long now = level.getGameTime();
        if (interactingUntil > now || behavior.stationary()) {
            getNavigation().stop();
        } else if (behavior.homebound() && behavior.returnRadius() > 0
                && distanceToSqr(homePos.getX() + 0.5D, homePos.getY(), homePos.getZ() + 0.5D)
                > (double) behavior.returnRadius() * behavior.returnRadius()) {
            getNavigation().moveTo(homePos.getX() + 0.5D, homePos.getY(), homePos.getZ() + 0.5D, 0.75D);
        }
        emitAmbientLine(level, profile, behavior, now);
    }

    private boolean interacting(ServerLevel level) {
        return level != null && interactingUntil > level.getGameTime();
    }

    private void emitAmbientLine(ServerLevel level, EchoNpcProfile profile, EchoNpcBehaviorSettings behavior, long now) {
        if (profile.ambientLines().isEmpty() || behavior.ambientCooldown() <= 0
                || now - lastAmbientLine < behavior.ambientCooldown()) {
            return;
        }
        Player player = level.getNearestPlayer(this, 6.0D);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        String line = profile.ambientLines().get(getRandom().nextInt(profile.ambientLines().size()));
        serverPlayer.sendSystemMessage(Component.literal("<" + profile.displayName() + "> " + line), true);
        lastAmbientLine = now;
    }

    private void ensureHome() {
        if (!homeSet) {
            homePos = blockPosition().immutable();
            homeSet = true;
        }
    }

    public void refreshNameplate() {
        if (!hasCustomName()) {
            EchoNpcProfile profile = EchoNpcProfileManager.getOrFallback(npcProfileId());
            setCustomName(Component.literal(profile.displayName()));
            setCustomNameVisible(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.25D);
    }

    private static Identifier parse(String raw, Identifier fallback) {
        Identifier parsed = Identifier.tryParse(raw == null ? "" : raw);
        return parsed == null ? fallback : parsed;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class HomeBoundWanderGoal extends Goal {
        private final EchoNpcEntity npc;
        private final double speed;
        private double targetX;
        private double targetY;
        private double targetZ;

        private HomeBoundWanderGoal(EchoNpcEntity npc, double speed) {
            this.npc = npc;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!(npc.level() instanceof ServerLevel level) || npc.interacting(level) || npc.isVehicle()) {
                return false;
            }
            EchoNpcBehaviorSettings behavior = EchoNpcProfileManager.getOrFallback(npc.npcProfileId()).behavior();
            if (!behavior.canWander() || npc.getRandom().nextInt(90) != 0) {
                return false;
            }
            npc.ensureHome();
            return chooseTarget(level, behavior.wanderRadius());
        }

        @Override
        public boolean canContinueToUse() {
            if (!(npc.level() instanceof ServerLevel level) || npc.interacting(level)) {
                return false;
            }
            EchoNpcBehaviorSettings behavior = EchoNpcProfileManager.getOrFallback(npc.npcProfileId()).behavior();
            return behavior.canWander() && !npc.getNavigation().isDone();
        }

        @Override
        public void start() {
            npc.getNavigation().moveTo(targetX, targetY, targetZ, speed);
        }

        @Override
        public void stop() {
            targetX = 0.0D;
            targetY = 0.0D;
            targetZ = 0.0D;
        }

        private boolean chooseTarget(ServerLevel level, int radius) {
            int safeRadius = Math.max(1, radius);
            int radiusSq = safeRadius * safeRadius;
            for (int attempts = 0; attempts < 12; attempts++) {
                int dx = npc.getRandom().nextInt(safeRadius * 2 + 1) - safeRadius;
                int dz = npc.getRandom().nextInt(safeRadius * 2 + 1) - safeRadius;
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        npc.homePos().offset(dx, 0, dz));
                targetX = surface.getX() + 0.5D;
                targetY = surface.getY();
                targetZ = surface.getZ() + 0.5D;
                return true;
            }
            return false;
        }
    }
}
