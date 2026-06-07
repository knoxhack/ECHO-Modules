package com.knoxhack.echoriftworlds.api;

import com.knoxhack.echoarcanacore.api.AetherSignalType;
import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echoriftworlds.EchoRiftWorlds;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class RiftWorldsApi {
    public static final Identifier RIFT_CRACK = EchoRiftWorlds.id("rift_crack");
    public static final Identifier POCKET_RIFT = EchoRiftWorlds.id("pocket_rift");
    public static final ResourceKey<Level> POCKET_RIFT_LEVEL = ResourceKey.create(Registries.DIMENSION, POCKET_RIFT);
    private static final String POCKET_ROOT = "echoriftworlds_pocket";
    private static final long INSTANCE_TTL_TICKS = 20L * 60L * 20L;

    public record PocketRiftInstance(String id, ResourceKey<Level> level, BlockPos center, long expiresAt) {
    }

    private RiftWorldsApi() {
    }

    public static void scanRiftCrack(ServerPlayer player, BlockPos pos) {
        if (player == null) {
            return;
        }
        ArcanaCoreServices.aether().addAether(player, 10.0D, AetherSignalType.RIFT_AETHER);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 160, 0, false, false));
        player.sendSystemMessage(Component.translatable("message.echoriftworlds.rift_crack_scanned"));
        record(player, RIFT_CRACK, "scan");
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D,
                    18, 0.35D, 0.25D, 0.35D, 0.02D);
            serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.7F, 0.65F);
        }
    }

    public static void triggerPocketEncounter(ServerPlayer player, BlockPos pos) {
        if (player == null) {
            return;
        }
        ArcanaCoreServices.aether().addAether(player, 16.0D, AetherSignalType.RIFT_AETHER);
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 180, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 180, 0, false, true));
        player.sendSystemMessage(Component.translatable("message.echoriftworlds.pocket_rift_entered"));
        record(player, POCKET_RIFT, "pocket_encounter");
        if (player.level() instanceof ServerLevel serverLevel) {
            ServerLevel pocketLevel = pocketLevel(serverLevel);
            BlockPos center = buildPocketEncounter(pocketLevel, player, serverLevel, pos);
            player.teleportTo(pocketLevel, center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D,
                    java.util.Set.of(), player.getYRot(), player.getXRot(), false);
            pocketLevel.sendParticles(ParticleTypes.PORTAL,
                    center.getX() + 0.5D, center.getY() + 1.2D, center.getZ() + 0.5D,
                    32, 0.5D, 0.45D, 0.5D, 0.08D);
            pocketLevel.playSound(null, center, SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 0.45F, 1.35F);
        }
    }

    public static boolean returnFromPocket(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        CompoundTag root = pocketRoot(player);
        if (!root.getBooleanOr("active", false)) {
            player.sendSystemMessage(Component.translatable("message.echoriftworlds.pocket_no_anchor"));
            return false;
        }
        BlockPos returnPos = new BlockPos(root.getIntOr("return_x", player.getBlockX()),
                root.getIntOr("return_y", player.getBlockY()), root.getIntOr("return_z", player.getBlockZ()));
        ResourceKey<Level> returnKey = dimensionKey(root.getStringOr("return_dimension", serverLevel.dimension().identifier().toString()));
        ServerLevel returnLevel = serverLevel.getServer().getLevel(returnKey);
        if (returnLevel == null) {
            returnLevel = serverLevel;
        }
        completePocketInstance(player, "returned");
        root.putLong("last_returned", returnLevel.getGameTime());
        player.teleportTo(returnLevel, returnPos.getX() + 0.5D, returnPos.getY() + 1.0D, returnPos.getZ() + 0.5D,
                java.util.Set.of(), player.getYRot(), player.getXRot(), false);
        returnLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                returnPos.getX() + 0.5D, returnPos.getY() + 1.1D, returnPos.getZ() + 0.5D,
                24, 0.35D, 0.35D, 0.35D, 0.06D);
        returnLevel.playSound(null, returnPos, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.55F, 0.9F);
        player.sendSystemMessage(Component.translatable("message.echoriftworlds.pocket_returned"));
        record(player, POCKET_RIFT, "pocket_return");
        return true;
    }

    public static boolean hasActivePocket(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        refreshPocketLifecycle(player);
        return pocketRoot(player).getBooleanOr("active", false);
    }

    public static PocketRiftInstance activePocketInstance(ServerPlayer player) {
        if (player == null || !hasActivePocket(player)) {
            return null;
        }
        CompoundTag root = pocketRoot(player);
        BlockPos center = new BlockPos(root.getIntOr("center_x", player.getBlockX()),
                root.getIntOr("center_y", player.getBlockY()), root.getIntOr("center_z", player.getBlockZ()));
        return new PocketRiftInstance(root.getStringOr("instance_id", ""),
                POCKET_RIFT_LEVEL,
                center,
                root.getLongOr("expires_at", player.level().getGameTime()));
    }

    public static boolean expirePocketInstance(ServerPlayer player) {
        if (player == null || !hasActivePocket(player)) {
            return false;
        }
        CompoundTag root = pocketRoot(player);
        root.putBoolean("active", false);
        root.putString("state", "expired");
        root.putLong("last_expired", player.level().getGameTime());
        root.putInt("expired_instances", root.getIntOr("expired_instances", 0) + 1);
        player.sendSystemMessage(Component.translatable("message.echoriftworlds.pocket_expired"));
        record(player, POCKET_RIFT, "pocket_expired");
        return true;
    }

    public static boolean refreshPocketLifecycle(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        CompoundTag root = pocketRoot(player);
        if (!root.getBooleanOr("active", false)) {
            return false;
        }
        long expiresAt = root.getLongOr("expires_at", Long.MAX_VALUE);
        if (player.level().getGameTime() < expiresAt) {
            return true;
        }
        root.putBoolean("active", false);
        root.putString("state", "expired");
        root.putLong("last_expired", player.level().getGameTime());
        root.putInt("expired_instances", root.getIntOr("expired_instances", 0) + 1);
        record(player, POCKET_RIFT, "pocket_expired");
        return false;
    }

    public static boolean abandonPocketInstance(ServerPlayer player) {
        if (player == null || !hasActivePocket(player)) {
            return false;
        }
        CompoundTag root = pocketRoot(player);
        root.putBoolean("active", false);
        root.putString("state", "abandoned");
        root.putLong("last_abandoned", player.level().getGameTime());
        root.putInt("abandoned_instances", root.getIntOr("abandoned_instances", 0) + 1);
        record(player, POCKET_RIFT, "pocket_abandoned");
        return true;
    }

    public static boolean completePocketInstance(ServerPlayer player, String reason) {
        if (player == null) {
            return false;
        }
        CompoundTag root = pocketRoot(player);
        if (!root.getBooleanOr("active", false)) {
            return false;
        }
        root.putBoolean("active", false);
        root.putString("state", reason == null || reason.isBlank() ? "completed" : reason);
        root.putLong("last_completed", player.level().getGameTime());
        root.putInt("completed_instances", root.getIntOr("completed_instances", 0) + 1);
        record(player, POCKET_RIFT, "pocket_completed");
        return true;
    }

    public static int completedPocketInstances(ServerPlayer player) {
        return player == null ? 0 : Math.max(0, pocketRoot(player).getIntOr("completed_instances", 0));
    }

    public static int openedPocketInstances(ServerPlayer player) {
        return player == null ? 0 : Math.max(0, pocketRoot(player).getIntOr("opened_instances", 0));
    }

    public static int expiredPocketInstances(ServerPlayer player) {
        return player == null ? 0 : Math.max(0, pocketRoot(player).getIntOr("expired_instances", 0));
    }

    public static int abandonedPocketInstances(ServerPlayer player) {
        return player == null ? 0 : Math.max(0, pocketRoot(player).getIntOr("abandoned_instances", 0));
    }

    public static int cleanedPocketInstances(ServerPlayer player) {
        return player == null ? 0 : Math.max(0, pocketRoot(player).getIntOr("cleaned_instances", 0));
    }

    public static int supersededPocketInstances(ServerPlayer player) {
        return player == null ? 0 : Math.max(0, pocketRoot(player).getIntOr("superseded_instances", 0));
    }

    public static String pocketInstanceState(ServerPlayer player) {
        if (player == null) {
            return "none";
        }
        refreshPocketLifecycle(player);
        return pocketRoot(player).getStringOr("state", "none");
    }

    public static boolean cleanupPocketChamber(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        CompoundTag root = pocketRoot(player);
        if (root.getBooleanOr("active", false)) {
            return false;
        }
        String state = root.getStringOr("state", "");
        if (!"returned".equals(state) && !"expired".equals(state) && !"abandoned".equals(state)
                && !"superseded".equals(state)) {
            return false;
        }
        ServerLevel pocketLevel = serverLevel.getServer().getLevel(POCKET_RIFT_LEVEL);
        if (pocketLevel == null || !pocketLevel.dimension().equals(POCKET_RIFT_LEVEL)) {
            return false;
        }
        BlockPos center = new BlockPos(root.getIntOr("center_x", player.getBlockX()),
                root.getIntOr("center_y", player.getBlockY()), root.getIntOr("center_z", player.getBlockZ()));
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -1; dy <= 4; dy++) {
                    pocketLevel.setBlock(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        root.putString("state", "cleaned");
        root.putLong("last_cleaned", serverLevel.getGameTime());
        root.putInt("cleaned_instances", root.getIntOr("cleaned_instances", 0) + 1);
        record(player, POCKET_RIFT, "pocket_cleaned");
        return true;
    }

    public static long activePocketRemainingTicks(ServerPlayer player) {
        if (player == null || !hasActivePocket(player)) {
            return 0L;
        }
        return Math.max(0L, pocketRoot(player).getLongOr("expires_at", player.level().getGameTime())
                - player.level().getGameTime());
    }

    public static boolean inPocketRiftLevel(ServerPlayer player) {
        return player != null && player.level().dimension().equals(POCKET_RIFT_LEVEL);
    }

    private static ServerLevel pocketLevel(ServerLevel fallback) {
        ServerLevel pocket = fallback.getServer().getLevel(POCKET_RIFT_LEVEL);
        return pocket == null ? fallback : pocket;
    }

    private static BlockPos buildPocketEncounter(ServerLevel level, ServerPlayer player, ServerLevel returnLevel,
            BlockPos source) {
        CompoundTag root = pocketRoot(player);
        if (root.getBooleanOr("active", false)) {
            root.putBoolean("active", false);
            root.putString("state", "superseded");
            root.putLong("last_expired", level.getGameTime());
            root.putInt("superseded_instances", root.getIntOr("superseded_instances", 0) + 1);
        }
        int instanceIndex = root.getIntOr("next_instance_index", 0) + 1;
        root.putInt("next_instance_index", instanceIndex);
        root.putInt("opened_instances", root.getIntOr("opened_instances", 0) + 1);
        BlockPos center = level.dimension().equals(POCKET_RIFT_LEVEL)
                ? pocketCenter(player, level, instanceIndex)
                : fallbackCenter(level, source);
        String instanceId = player.getUUID() + ":" + instanceIndex;
        root.putBoolean("active", true);
        root.putString("instance_id", instanceId);
        root.putInt("instance_index", instanceIndex);
        root.putString("return_dimension", returnLevel.dimension().identifier().toString());
        root.putInt("return_x", source.getX());
        root.putInt("return_y", source.getY());
        root.putInt("return_z", source.getZ());
        root.putInt("center_x", center.getX());
        root.putInt("center_y", center.getY());
        root.putInt("center_z", center.getZ());
        root.putLong("created_at", level.getGameTime());
        root.putLong("expires_at", level.getGameTime() + INSTANCE_TTL_TICKS);
        root.putString("state", "active");

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -1; dy <= 4; dy++) {
                    BlockPos target = center.offset(dx, dy, dz);
                    boolean wall = Math.abs(dx) == 3 || Math.abs(dz) == 3 || dy == -1 || dy == 4;
                    if (wall) {
                        level.setBlock(target, (dy == -1 ? Blocks.CRYING_OBSIDIAN : Blocks.TINTED_GLASS)
                                .defaultBlockState(), 3);
                    } else {
                        level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        level.setBlock(center.below(), Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
        level.setBlock(center.north(2), Blocks.END_ROD.defaultBlockState(), 3);
        level.setBlock(center.south(2), Blocks.END_ROD.defaultBlockState(), 3);
        level.setBlock(center.east(2), Blocks.END_ROD.defaultBlockState(), 3);
        level.setBlock(center.west(2), Blocks.END_ROD.defaultBlockState(), 3);
        level.addFreshEntity(new ItemEntity(level, center.getX() + 0.5D, center.getY() + 1.1D, center.getZ() + 0.5D,
                new ItemStack(Items.AMETHYST_SHARD, 2)));
        var leech = EntityType.ENDERMITE.create(level, EntitySpawnReason.EVENT);
        if (leech != null) {
            leech.setPos(center.getX() + 1.5D, center.getY() + 1.0D, center.getZ() + 1.5D);
            level.addFreshEntity(leech);
        }
        return center;
    }

    private static BlockPos fallbackCenter(ServerLevel level, BlockPos source) {
        int minY = level.getMinY() + 8;
        int maxY = level.getMaxY() - 14;
        int targetY = Math.max(minY, Math.min(maxY, source.getY() + 18));
        return new BlockPos(source.getX(), targetY, source.getZ());
    }

    private static BlockPos pocketCenter(ServerPlayer player, ServerLevel level, int instanceIndex) {
        int hash = player.getUUID().hashCode();
        int lane = Math.floorMod(hash, 128);
        int ring = Math.floorMod((hash >>> 7) + instanceIndex * 17, 128);
        int x = lane * 48 - 3072;
        int z = ring * 48 - 3072;
        int y = Math.max(level.getMinY() + 16, Math.min(level.getMaxY() - 16, 72));
        return new BlockPos(x, y, z);
    }

    private static ResourceKey<Level> dimensionKey(String id) {
        Identifier parsed = Identifier.tryParse(id);
        return parsed == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, parsed);
    }

    private static CompoundTag pocketRoot(ServerPlayer player) {
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(POCKET_ROOT);
        player.getPersistentData().put(POCKET_ROOT, root);
        return root;
    }

    public static void record(ServerPlayer player, Identifier subject, String action) {
        if (player == null || subject == null) {
            return;
        }
        EchoCoreServices.recordMissionObjective(player, MissionObjectiveType.CUSTOM, subject, 1,
                Map.of("source", EchoRiftWorlds.MODID, "action", action == null ? "" : action));
    }
}
