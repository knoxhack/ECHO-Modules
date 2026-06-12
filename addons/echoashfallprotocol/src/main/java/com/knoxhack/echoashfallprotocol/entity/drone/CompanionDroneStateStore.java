package com.knoxhack.echoashfallprotocol.entity.drone;

import com.echoplatform.echocore.api.DataScope;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.IDataKey;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneMode;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CompanionDroneStateStore {
    public static final Identifier DATA_KEY_ID =
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "drone/state");
    private static final IDataKey<CompoundTag> DATA_KEY =
            IDataKey.record(DATA_KEY_ID, DataScope.PLAYER, CompoundTag.CODEC, new CompoundTag(), true);

    private CompanionDroneStateStore() {
    }

    public static void registerDataKey() {
        EchoCoreServices.registerDataKey(DATA_KEY);
    }

    public static CompanionDroneData get(Player player) {
        CompanionDroneData data = player.getData(ModAttachments.COMPANION_DRONE_DATA.get());
        if (data.getOwnerUuid() == null && player != null) {
            data.setOwnerUuid(player.getUUID());
        }
        migrateFromQuest(player, data);
        return data;
    }

    public static void save(ServerPlayer player, CompanionDroneData data) {
        if (player == null || data == null) {
            return;
        }
        data.setOwnerUuid(player.getUUID());
        player.setData(ModAttachments.COMPANION_DRONE_DATA.get(), data);
        player.syncData(ModAttachments.COMPANION_DRONE_DATA.get());
        try {
            EchoCoreServices.dataService().player(player).set(DATA_KEY, data.toTag());
        } catch (RuntimeException exception) {
            EchoAshfallProtocol.LOGGER.debug("Companion Drone DataCore mirror unavailable.", exception);
        }
    }

    public static void hydrateFromDataCore(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CompanionDroneData attachment = get(player);
        try {
            CompoundTag mirror = EchoCoreServices.dataService().player(player).get(DATA_KEY);
            if (mirror != null && !mirror.isEmpty()
                    && (attachment.getDroneUuid() == null || mirror.getIntOr("schemaVersion", 0) >= CompanionDroneData.SCHEMA_VERSION)) {
                attachment.readTag(mirror);
                attachment.setOwnerUuid(player.getUUID());
                save(player, attachment);
            }
        } catch (RuntimeException exception) {
            EchoAshfallProtocol.LOGGER.debug("Companion Drone DataCore hydration skipped.", exception);
        }
    }

    public static List<EchoCompanionDrone> findOwned(ServerPlayer player, double range) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return List.of();
        }
        AABB area = range <= 0.0D ? player.getBoundingBox().inflate(128.0D)
                : player.getBoundingBox().inflate(range, Math.max(32.0D, range * 0.5D), range);
        return level.getEntitiesOfClass(EchoCompanionDrone.class, area,
                drone -> !drone.isRemoved() && drone.isAlive() && player.getUUID().equals(drone.getOwnerUUID()))
                .stream()
                .sorted(Comparator.comparingDouble(drone -> drone.distanceToSqr(player)))
                .toList();
    }

    public static EchoCompanionDrone findByStoredUuid(ServerPlayer player) {
        CompanionDroneData data = get(player);
        UUID droneId = data.getDroneUuid();
        if (droneId == null || player.level().getServer() == null) {
            return null;
        }
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            if (level.getEntity(droneId) instanceof EchoCompanionDrone drone
                    && player.getUUID().equals(drone.getOwnerUUID())
                    && !drone.isRemoved()) {
                return drone;
            }
        }
        return null;
    }

    public static EchoCompanionDrone nearestOwned(ServerPlayer player) {
        EchoCompanionDrone stored = findByStoredUuid(player);
        if (stored != null) {
            return stored;
        }
        List<EchoCompanionDrone> owned = findOwned(player, 160.0D);
        return owned.isEmpty() ? null : owned.get(0);
    }

    public static EchoCompanionDrone ensureDrone(ServerPlayer player, boolean allowRespawn, boolean feedback) {
        if (player == null) {
            return null;
        }
        CompanionDroneData data = get(player);
        hydrateFromDataCore(player);
        List<EchoCompanionDrone> local = findOwned(player, 192.0D);
        EchoCompanionDrone stored = findByStoredUuid(player);
        List<EchoCompanionDrone> candidates = new ArrayList<>();
        if (stored != null) {
            candidates.add(stored);
        }
        for (EchoCompanionDrone drone : local) {
            if (!candidates.contains(drone)) {
                candidates.add(drone);
            }
        }
        EchoCompanionDrone primary = candidates.isEmpty() ? null : candidates.get(0);
        if (primary == null && allowRespawn) {
            if (feedback) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "[ECHO-7 // DRONE] Drone signal lost. Reconstructing local link..."), true);
            }
            primary = spawnRecoveredDrone(player, data);
        }
        if (primary != null) {
            link(player, primary, data);
            dedupe(player, primary, candidates);
            save(player, data);
        }
        return primary;
    }

    public static EchoCompanionDrone spawnRecoveredDrone(ServerPlayer player, CompanionDroneData data) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        EchoCompanionDrone drone = new EchoCompanionDrone(ModEntities.ECHO_COMPANION_DRONE.get(), level);
        Vec3 pos = safeHoverTarget(player);
        drone.setPos(pos.x, pos.y, pos.z);
        drone.setOwnerUUID(player.getUUID());
        drone.setRepairLevel(Math.max(data.getHealth(), QuestData.get(player).getDroneHealth()));
        drone.forceFollowMode();
        drone.setNoGravity(true);
        if (level.addFreshEntity(drone)) {
            EchoAshfallProtocol.LOGGER.info("Recovered Companion Drone link for {} at {}.", player.getScoreboardName(), drone.blockPosition());
            return drone;
        }
        return null;
    }

    public static void link(ServerPlayer player, EchoCompanionDrone drone, CompanionDroneData data) {
        if (player == null || drone == null || data == null) {
            return;
        }
        drone.setOwnerUUID(player.getUUID());
        data.setOwnerUuid(player.getUUID());
        data.setDroneUuid(drone.getUUID());
        data.setHealth(Math.max(data.getHealth(), drone.getRepairLevel()));
        data.setDeployed(true);
        data.setLastKnown(drone.level().dimension(), drone.blockPosition());
        drone.applyFieldAssistantState(data);
    }

    public static void dedupe(ServerPlayer player, EchoCompanionDrone primary, List<EchoCompanionDrone> candidates) {
        if (player == null || primary == null || !com.knoxhack.echoashfallprotocol.Config.ALLOW_ONE_DRONE_PER_PLAYER.get()) {
            return;
        }
        for (EchoCompanionDrone drone : candidates) {
            if (drone != primary && !drone.isRemoved()) {
                EchoAshfallProtocol.LOGGER.info("Removing duplicate Companion Drone {} for {}.", drone.getUUID(), player.getScoreboardName());
                drone.discard();
            }
        }
    }

    public static Vec3 safeHoverTarget(Player owner) {
        Vec3 look = owner.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0.0D, look.z);
        if (flatLook.lengthSqr() < 0.001D) {
            flatLook = Vec3.directionFromRotation(0.0F, owner.getYRot());
            flatLook = new Vec3(flatLook.x, 0.0D, flatLook.z);
        }
        flatLook = flatLook.normalize();
        Vec3 side = new Vec3(-flatLook.z, 0.0D, flatLook.x).normalize();
        Vec3[] candidates = new Vec3[] {
                owner.position().subtract(flatLook.scale(2.75D)).add(side.scale(1.35D)).add(0.0D, 2.05D, 0.0D),
                owner.position().subtract(flatLook.scale(2.75D)).subtract(side.scale(1.35D)).add(0.0D, 2.05D, 0.0D),
                owner.position().subtract(flatLook.scale(3.15D)).add(0.0D, 2.2D, 0.0D),
                owner.position().add(side.scale(2.0D)).add(0.0D, 2.0D, 0.0D)
        };
        for (Vec3 target : candidates) {
            Vec3 fromEye = target.subtract(owner.getEyePosition());
            if (fromEye.lengthSqr() > 0.001D && fromEye.normalize().dot(owner.getLookAngle()) > 0.15D) {
                continue;
            }
            BlockPos pos = BlockPos.containing(target);
            if (owner.level().isLoaded(pos) && owner.level().noCollision(primaryProbe(owner, target))) {
                return target;
            }
        }
        return owner.position().add(0.0D, 1.85D, 0.0D);
    }

    private static AABB primaryProbe(Player owner, Vec3 target) {
        AABB box = owner.getBoundingBox().inflate(0.1D).move(target.subtract(owner.position()));
        return new AABB(box.minX, target.y - 0.25D, box.minZ, box.maxX, target.y + 0.25D, box.maxZ);
    }

    private static void migrateFromQuest(Player player, CompanionDroneData data) {
        if (player == null || data == null) {
            return;
        }
        if (data.getOwnerUuid() == null) {
            data.setOwnerUuid(player.getUUID());
        }
        QuestData quest = QuestData.get(player);
        if (data.getHealth() < quest.getDroneHealth()) {
            data.setHealth(quest.getDroneHealth());
        }
        if (quest.isDroneDeployed()) {
            data.setDeployed(true);
        }
        if (data.getMode() == EchoDroneMode.FOLLOW && quest.getDroneHealth() >= 25) {
            data.setTaskLabel(EchoDroneMode.FOLLOW.taskLabel());
        }
    }

    public static boolean sameDimension(ServerPlayer player, EchoCompanionDrone drone) {
        return player != null && drone != null && player.level().dimension().equals(drone.level().dimension());
    }

    public static boolean serverHasStoredDrone(ServerPlayer player) {
        MinecraftServer server = player == null ? null : player.level().getServer();
        return server != null && findByStoredUuid(player) != null;
    }
}
