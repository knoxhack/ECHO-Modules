package com.knoxhack.echofamiliarcore.entity;

import com.knoxhack.echofamiliarcore.api.FamiliarCoreApi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class ArcanaFamiliarEntity extends PathfinderMob {
    public static final int COMMAND_FOLLOW = 0;
    public static final int COMMAND_STAY = 1;
    public static final int COMMAND_SCOUT = 2;
    public static final int COMMAND_DEFEND = 3;
    public static final int KIND_AETHER_WISP = 0;
    public static final int KIND_SPIRIT_DRONE = 1;

    private static final EntityDataAccessor<String> DATA_OWNER =
            SynchedEntityData.defineId(ArcanaFamiliarEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_COMMAND =
            SynchedEntityData.defineId(ArcanaFamiliarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(ArcanaFamiliarEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_STATUS =
            SynchedEntityData.defineId(ArcanaFamiliarEntity.class, EntityDataSerializers.STRING);

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private Player cachedOwner;

    protected ArcanaFamiliarEntity(EntityType<? extends PathfinderMob> type, Level level, int kind) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 0;
        setFamiliarKind(kind);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.FLYING_SPEED, 0.62D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, "");
        builder.define(DATA_COMMAND, COMMAND_FOLLOW);
        builder.define(DATA_KIND, KIND_AETHER_WISP);
        builder.define(DATA_STATUS, "synchronized");
    }

    @Override
    protected void registerGoals() {
    }

    public void bindTo(ServerPlayer player) {
        ownerUuid = player.getUUID();
        cachedOwner = player;
        entityData.set(DATA_OWNER, ownerUuid.toString());
        entityData.set(DATA_COMMAND, COMMAND_FOLLOW);
        entityData.set(DATA_STATUS, "following " + player.getScoreboardName());
        setCustomName(Component.literal(displayName()));
        setCustomNameVisible(false);
    }

    public int command() {
        return entityData.get(DATA_COMMAND);
    }

    public int familiarKind() {
        return entityData.get(DATA_KIND);
    }

    public String statusLine() {
        return entityData.get(DATA_STATUS);
    }

    public String displayName() {
        return familiarKind() == KIND_SPIRIT_DRONE ? "Spirit Drone" : "Aether Wisp";
    }

    public void setCommandFromMenu(ServerPlayer player, int command) {
        if (player == null || !isOwner(player)) {
            return;
        }
        int safeCommand = Math.max(COMMAND_FOLLOW, Math.min(COMMAND_DEFEND, command));
        entityData.set(DATA_COMMAND, safeCommand);
        entityData.set(DATA_STATUS, commandLabel(safeCommand));
        player.sendSystemMessage(Component.translatable("message.echofamiliarcore.command",
                displayName(), commandLabel(safeCommand)));
    }

    public static ArcanaFamiliarEntity summonOrRefresh(ServerPlayer player, int kind) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        ArcanaFamiliarEntity existing = nearestOwned(level, player, kind);
        if (existing != null) {
            existing.bindTo(player);
            existing.teleportTo(player.getX() + 0.8D, player.getY() + 1.1D, player.getZ() + 0.8D);
            return existing;
        }
        ArcanaFamiliarEntity familiar = kind == KIND_SPIRIT_DRONE
                ? com.knoxhack.echofamiliarcore.registry.ModEntities.SPIRIT_DRONE.get().create(level, EntitySpawnReason.EVENT)
                : com.knoxhack.echofamiliarcore.registry.ModEntities.AETHER_WISP.get().create(level, EntitySpawnReason.EVENT);
        if (familiar == null) {
            return null;
        }
        familiar.bindTo(player);
        familiar.setPos(player.getX() + 0.8D, player.getY() + 1.1D, player.getZ() + 0.8D);
        return level.addFreshEntity(familiar) ? familiar : null;
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        setTarget(null);
        if (!level().isClientSide()) {
            serverTick();
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
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !isOwner(player)) {
            player.sendSystemMessage(Component.translatable("message.echofamiliarcore.not_owner"));
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown()) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, operator) ->
                            new com.knoxhack.echofamiliarcore.menu.FamiliarCommandMenu(containerId, inventory, serverPlayer),
                    Component.translatable("screen.echofamiliarcore.familiar_command")));
            return InteractionResult.SUCCESS_SERVER;
        }
        int next = (command() + 1) % 4;
        setCommandFromMenu(serverPlayer, next);
        FamiliarCoreApi.recordCommand(serverPlayer, familiarKind() == KIND_SPIRIT_DRONE
                ? FamiliarCoreApi.SPIRIT_DRONE : FamiliarCoreApi.AETHER_WISP, commandLabel(next));
        return InteractionResult.SUCCESS_SERVER;
    }

    private void serverTick() {
        Player owner = owner();
        if (owner == null || !owner.isAlive()) {
            if (tickCount > 200) {
                discard();
            }
            return;
        }
        if (tickCount % 80 == 0 && owner instanceof ServerPlayer serverPlayer) {
            FamiliarCoreApi.pulse(serverPlayer);
        }
        if (distanceToSqr(owner) > 900.0D) {
            teleportTo(owner.getX() + 0.8D, owner.getY() + 1.1D, owner.getZ() + 0.8D);
        }
        switch (command()) {
            case COMMAND_STAY -> getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0D);
            case COMMAND_SCOUT -> scout(owner);
            case COMMAND_DEFEND -> defend(owner);
            default -> follow(owner);
        }
        if (tickCount % 20 == 0) {
            supportPulse(owner);
        }
    }

    private void follow(Player owner) {
        Vec3 target = owner.position().add(0.9D, 1.15D, 0.9D);
        if (distanceToSqr(target) > 2.25D) {
            getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.05D);
        }
    }

    private void scout(Player owner) {
        double angle = (owner.tickCount + getId() * 17) * 0.08D;
        Vec3 target = owner.position().add(Math.cos(angle) * 5.0D, 2.0D, Math.sin(angle) * 5.0D);
        getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.15D);
    }

    private void defend(Player owner) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            follow(owner);
            return;
        }
        AABB area = owner.getBoundingBox().inflate(8.0D);
        Monster threat = serverLevel.getEntitiesOfClass(Monster.class, area, Entity::isAlive)
                .stream()
                .min(Comparator.comparingDouble(monster -> monster.distanceToSqr(owner)))
                .orElse(null);
        if (threat == null) {
            follow(owner);
            return;
        }
        Vec3 target = threat.position().add(0.0D, threat.getBbHeight() * 0.65D, 0.0D);
        getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.25D);
        if (distanceToSqr(threat) < 3.5D && tickCount % 20 == 0) {
            threat.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, false, true));
            threat.push(threat.getX() - getX(), 0.05D, threat.getZ() - getZ());
        }
    }

    private void supportPulse(Player owner) {
        int tier = FamiliarCoreApi.evolutionTier(owner);
        int rank = FamiliarCoreApi.upgradeRank(owner);
        int warding = FamiliarCoreApi.upgradeRank(owner, FamiliarCoreApi.UPGRADE_WARDING);
        int scouting = FamiliarCoreApi.upgradeRank(owner, FamiliarCoreApi.UPGRADE_SCOUTING);
        int duration = 80 + (tier + rank) * 8;
        if (familiarKind() == KIND_AETHER_WISP) {
            owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false));
            if (scouting > 0) {
                owner.addEffect(new MobEffectInstance(MobEffects.SPEED, duration / 2, Math.min(1, scouting - 1),
                        false, true));
            }
        } else {
            owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, Math.min(2, tier / 4 + warding / 2),
                    false, true));
        }
    }

    private void setFamiliarKind(int kind) {
        entityData.set(DATA_KIND, kind == KIND_SPIRIT_DRONE ? KIND_SPIRIT_DRONE : KIND_AETHER_WISP);
    }

    private boolean isOwner(Player player) {
        return player != null && ownerUuid != null && ownerUuid.equals(player.getUUID());
    }

    @Nullable
    private Player owner() {
        if (cachedOwner != null && cachedOwner.isAlive()) {
            return cachedOwner;
        }
        if (ownerUuid == null && !entityData.get(DATA_OWNER).isBlank()) {
            ownerUuid = readUuid(entityData.get(DATA_OWNER));
        }
        if (ownerUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(ownerUuid);
        if (entity instanceof Player player) {
            cachedOwner = player;
            return player;
        }
        return null;
    }

    @Nullable
    private static ArcanaFamiliarEntity nearestOwned(ServerLevel level, ServerPlayer player, int kind) {
        return ownedFamiliars(level, player, kind, 64.0D)
                .stream()
                .min(Comparator.comparingDouble(familiar -> familiar.distanceToSqr(player)))
                .orElse(null);
    }

    public static List<ArcanaFamiliarEntity> ownedFamiliars(ServerLevel level, ServerPlayer player, int kind, double radius) {
        if (level == null || player == null) {
            return List.of();
        }
        AABB area = player.getBoundingBox().inflate(radius);
        Map<UUID, ArcanaFamiliarEntity> familiars = new LinkedHashMap<>();
        for (ArcanaFamiliarEntity familiar : level.getEntitiesOfClass(ArcanaFamiliarEntity.class, area,
                familiar -> familiar.familiarKind() == kind && familiar.isOwner(player))) {
            familiars.putIfAbsent(familiar.getUUID(), familiar);
        }
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ArcanaFamiliarEntity familiar
                    && familiar.familiarKind() == kind
                    && familiar.isOwner(player)
                    && area.intersects(familiar.getBoundingBox())) {
                familiars.putIfAbsent(familiar.getUUID(), familiar);
            }
        }
        return new ArrayList<>(familiars.values());
    }

    private static String commandLabel(int command) {
        return switch (command) {
            case COMMAND_STAY -> "stay";
            case COMMAND_SCOUT -> "scout";
            case COMMAND_DEFEND -> "defend";
            default -> "follow";
        };
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("owner_uuid", ownerUuid == null ? "" : ownerUuid.toString());
        output.putInt("command", command());
        output.putInt("kind", familiarKind());
        output.putString("status", statusLine());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        ownerUuid = readUuid(input.getStringOr("owner_uuid", ""));
        entityData.set(DATA_OWNER, ownerUuid == null ? "" : ownerUuid.toString());
        entityData.set(DATA_COMMAND, input.getIntOr("command", COMMAND_FOLLOW));
        entityData.set(DATA_KIND, input.getIntOr("kind", KIND_AETHER_WISP));
        entityData.set(DATA_STATUS, input.getStringOr("status", "synchronized"));
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
