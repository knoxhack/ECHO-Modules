package com.knoxhack.echorelictech.item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;

public final class EchoMirrorDecoyTracker {
    private static final Map<UUID, List<TrackedDecoy>> ACTIVE_DECOYS = new ConcurrentHashMap<>();
    private static final int DECOY_LIFETIME_TICKS = 200; // 10 seconds
    private static final int DECOY_COUNT = 3;

    private EchoMirrorDecoyTracker() {}

    public static void spawnDecoys(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        UUID playerId = player.getUUID();

        // Clean up any existing decoys for this player first
        clearDecoys(playerId, level);

        List<TrackedDecoy> decoys = new ArrayList<>();
        long spawnTime = level.getGameTime();

        for (int i = 0; i < DECOY_COUNT; i++) {
            double angle = (Math.PI * 2 / DECOY_COUNT) * i + (Math.random() * 0.5);
            double distance = 2.0 + Math.random() * 2.0;
            double dx = Math.cos(angle) * distance;
            double dz = Math.sin(angle) * distance;

            ArmorStand decoy = new ArmorStand(level,
                player.getX() + dx,
                player.getY(),
                player.getZ() + dz
            );

            decoy.setInvulnerable(true);
            decoy.setInvisible(true);
            decoy.setNoGravity(true);
            decoy.setSilent(true);
            decoy.setCustomNameVisible(false);

            // Copy player equipment to make decoy recognizable to mobs
            decoy.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).copy());
            decoy.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).copy());
            decoy.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).copy());
            decoy.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).copy());
            decoy.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND).copy());
            decoy.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND).copy());

            // Give invisibility effect so only equipment is visible
            decoy.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, DECOY_LIFETIME_TICKS, 0, false, false));

            level.addFreshEntity(decoy);
            decoys.add(new TrackedDecoy(decoy.getUUID(), spawnTime));
        }

        ACTIVE_DECOYS.put(playerId, decoys);
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, List<TrackedDecoy>>> playerIter = ACTIVE_DECOYS.entrySet().iterator();

        while (playerIter.hasNext()) {
            Map.Entry<UUID, List<TrackedDecoy>> entry = playerIter.next();
            List<TrackedDecoy> decoys = entry.getValue();
            Iterator<TrackedDecoy> decoyIter = decoys.iterator();

            while (decoyIter.hasNext()) {
                TrackedDecoy tracked = decoyIter.next();
                Entity entity = level.getEntity(tracked.entityId);
                if (entity == null || entity.isRemoved() || now - tracked.spawnTime >= DECOY_LIFETIME_TICKS) {
                    if (entity != null && !entity.isRemoved()) {
                        entity.discard();
                    }
                    decoyIter.remove();
                }
            }

            if (decoys.isEmpty()) {
                playerIter.remove();
            }
        }
    }

    public static void clearPlayerDecoys(ServerPlayer player) {
        clearDecoys(player.getUUID(), (ServerLevel) player.level());
    }

    private static void clearDecoys(UUID playerId, ServerLevel level) {
        List<TrackedDecoy> decoys = ACTIVE_DECOYS.remove(playerId);
        if (decoys == null) return;
        for (TrackedDecoy tracked : decoys) {
            Entity entity = level.getEntity(tracked.entityId);
            if (entity != null && !entity.isRemoved()) {
                entity.discard();
            }
        }
    }

    private record TrackedDecoy(UUID entityId, long spawnTime) {}
}
