package com.knoxhack.echobasegrid.service;

import com.knoxhack.echobasegrid.api.ClaimActionResult;
import com.knoxhack.echobasegrid.api.ClaimMember;
import com.knoxhack.echobasegrid.api.ClaimPermission;
import com.knoxhack.echobasegrid.api.ClaimRecord;
import com.knoxhack.echobasegrid.api.ClaimRole;
import com.knoxhack.echobasegrid.config.BaseGridConfig;
import com.knoxhack.echobasegrid.data.BaseGridSavedData;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public final class BaseGridClaimService {
    private BaseGridClaimService() {
    }

    public static Optional<ClaimRecord> claimAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || pos == null) {
            return Optional.empty();
        }
        ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        return claim(serverLevel, dimension(level), chunk.x(), chunk.z());
    }

    public static Optional<ClaimRecord> claim(ServerLevel level, String dimension, int chunkX, int chunkZ) {
        if (level == null) {
            return Optional.empty();
        }
        return BaseGridSavedData.get(level).claim(dimension, chunkX, chunkZ);
    }

    public static boolean can(ServerPlayer player, Level level, BlockPos pos, ClaimPermission permission) {
        if (!BaseGridConfig.ENABLED.get()) {
            return true;
        }
        Optional<ClaimRecord> claim = claimAt(level, pos);
        if (claim.isEmpty()) {
            return true;
        }
        if (player != null && canBypass(player)) {
            return true;
        }
        UUID playerId = player == null ? null : player.getUUID();
        return claim.get().allows(playerId, permission == null ? ClaimPermission.BUILD : permission);
    }

    public static boolean canBypass(ServerPlayer player) {
        return player != null
                && BaseGridConfig.OPS_BYPASS.get()
                && player.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static ClaimActionResult claim(ServerPlayer player, String dimension, int chunkX, int chunkZ) {
        if (!BaseGridConfig.ENABLED.get()) {
            return ClaimActionResult.failure("Base Grid Offline", "Claiming is disabled on this server.");
        }
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return ClaimActionResult.failure("Signal Missing", "No server player is attached to this claim request.");
        }
        String safeDimension = cleanDimension(player, dimension);
        BaseGridSavedData data = BaseGridSavedData.get(level);
        Optional<ClaimRecord> existing = data.claim(safeDimension, chunkX, chunkZ);
        if (existing.isPresent()) {
            return existing.get().ownedBy(player.getUUID())
                    ? ClaimActionResult.success("Already Claimed", "This chunk already belongs to your Base Grid.")
                    : ClaimActionResult.failure("Claim Occupied", "Another operator controls this chunk.");
        }
        int maxClaims = Math.max(0, BaseGridConfig.MAX_CLAIMS_PER_PLAYER.get());
        if (!canBypass(player) && data.claimCount(player.getUUID()) >= maxClaims) {
            return ClaimActionResult.failure("Bandwidth Exhausted",
                    "Your Base Grid claim limit is " + maxClaims + " chunk(s).");
        }
        long gameTime = level.getGameTime();
        data.put(new ClaimRecord(safeDimension, chunkX, chunkZ, player.getUUID(), player.getScoreboardName(),
                java.util.Map.of(), gameTime, gameTime));
        return ClaimActionResult.success("Chunk Claimed", "Base Grid now controls chunk " + chunkX + ", " + chunkZ + ".");
    }

    public static ClaimActionResult unclaim(ServerPlayer player, String dimension, int chunkX, int chunkZ) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return ClaimActionResult.failure("Signal Missing", "No server player is attached to this claim request.");
        }
        String safeDimension = cleanDimension(player, dimension);
        BaseGridSavedData data = BaseGridSavedData.get(level);
        Optional<ClaimRecord> claim = data.claim(safeDimension, chunkX, chunkZ);
        if (claim.isEmpty()) {
            return ClaimActionResult.failure("No Claim", "This chunk is not claimed.");
        }
        if (!claim.get().ownedBy(player.getUUID()) && !canBypass(player)) {
            return ClaimActionResult.failure("Access Denied", "Only the owner can release this claim.");
        }
        data.remove(safeDimension, chunkX, chunkZ);
        return ClaimActionResult.success("Chunk Released", "Base Grid released chunk " + chunkX + ", " + chunkZ + ".");
    }

    public static ClaimActionResult addMember(ServerPlayer player, String dimension, int chunkX, int chunkZ,
            UUID targetId, String targetName) {
        if (targetId == null) {
            return ClaimActionResult.failure("No Member", "Select an online player to trust.");
        }
        return mutateManagedClaim(player, dimension, chunkX, chunkZ, claim -> {
            ServerPlayer target = onlinePlayer(player, targetId);
            if (target == null) {
                return ClaimActionResult.failure("Member Offline", "Only online players can be added to a claim.");
            }
            UUID safeTargetId = target.getUUID();
            String safeTargetName = target.getScoreboardName();
            if (claim.ownerId().equals(safeTargetId)) {
                return ClaimActionResult.failure("Already Owner", "The owner already controls this claim.");
            }
            if (claim.members().containsKey(safeTargetId)) {
                return ClaimActionResult.success("Already Trusted", "That player is already trusted here.");
            }
            int maxMembers = Math.max(0, BaseGridConfig.MAX_MEMBERS.get());
            if (!canBypass(player) && claim.members().size() >= maxMembers) {
                return ClaimActionResult.failure("Member Limit", "This claim can trust " + maxMembers + " member(s).");
            }
            ClaimMember member = new ClaimMember(safeTargetId, safeTargetName, ClaimRole.MEMBER, ClaimRole.MEMBER.defaultPermissions());
            BaseGridSavedData.get((ServerLevel) player.level()).put(claim.withMember(member, player.level().getGameTime()));
            return ClaimActionResult.success("Member Added", member.playerName() + " can now use this Base Grid claim.");
        });
    }

    public static ClaimActionResult removeMember(ServerPlayer player, String dimension, int chunkX, int chunkZ,
            UUID targetId) {
        if (targetId == null) {
            return ClaimActionResult.failure("No Member", "Select a member to remove.");
        }
        return mutateManagedClaim(player, dimension, chunkX, chunkZ, claim -> {
            if (!claim.members().containsKey(targetId)) {
                return ClaimActionResult.failure("Member Missing", "That player is not trusted on this claim.");
            }
            BaseGridSavedData.get((ServerLevel) player.level()).put(claim.withoutMember(targetId, player.level().getGameTime()));
            return ClaimActionResult.success("Member Removed", "The selected member no longer has claim access.");
        });
    }

    public static ClaimActionResult setRole(ServerPlayer player, String dimension, int chunkX, int chunkZ,
            UUID targetId, ClaimRole role) {
        if (targetId == null) {
            return ClaimActionResult.failure("No Member", "Select a member to update.");
        }
        ClaimRole safeRole = role == null ? ClaimRole.MEMBER : role;
        return mutateManagedClaim(player, dimension, chunkX, chunkZ, claim -> {
            ClaimMember member = claim.members().get(targetId);
            if (member == null) {
                return ClaimActionResult.failure("Member Missing", "That player is not trusted on this claim.");
            }
            BaseGridSavedData.get((ServerLevel) player.level()).put(
                    claim.withUpdatedMember(member.withRole(safeRole), player.level().getGameTime()));
            return ClaimActionResult.success("Role Updated", member.playerName() + " is now " + safeRole.label() + ".");
        });
    }

    public static ClaimActionResult togglePermission(ServerPlayer player, String dimension, int chunkX, int chunkZ,
            UUID targetId, ClaimPermission permission) {
        if (targetId == null) {
            return ClaimActionResult.failure("No Member", "Select a member to update.");
        }
        ClaimPermission safePermission = permission == null ? ClaimPermission.BUILD : permission;
        return mutateManagedClaim(player, dimension, chunkX, chunkZ, claim -> {
            ClaimMember member = claim.members().get(targetId);
            if (member == null) {
                return ClaimActionResult.failure("Member Missing", "That player is not trusted on this claim.");
            }
            ClaimMember next = member.withPermissionToggled(safePermission);
            BaseGridSavedData.get((ServerLevel) player.level()).put(
                    claim.withUpdatedMember(next, player.level().getGameTime()));
            String state = next.permissions().contains(safePermission) ? "enabled" : "disabled";
            return ClaimActionResult.success("Permission Updated",
                    safePermission.label() + " is now " + state + " for " + member.playerName() + ".");
        });
    }

    public static String dimension(Level level) {
        return level == null ? "minecraft:overworld" : level.dimension().identifier().toString();
    }

    public static String cleanDimension(ServerPlayer player, String dimension) {
        return dimension(player == null ? null : player.level());
    }

    private static ClaimActionResult mutateManagedClaim(ServerPlayer player, String dimension, int chunkX, int chunkZ,
            ClaimMutation mutation) {
        if (!BaseGridConfig.ENABLED.get()) {
            return ClaimActionResult.failure("Base Grid Offline", "Claiming is disabled on this server.");
        }
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return ClaimActionResult.failure("Signal Missing", "No server player is attached to this claim request.");
        }
        String safeDimension = cleanDimension(player, dimension);
        Optional<ClaimRecord> claim = BaseGridSavedData.get(level).claim(safeDimension, chunkX, chunkZ);
        if (claim.isEmpty()) {
            return ClaimActionResult.failure("No Claim", "This chunk is not claimed.");
        }
        boolean manager = claim.get().allows(player.getUUID(), ClaimPermission.MANAGE) || canBypass(player);
        if (!manager) {
            return ClaimActionResult.failure("Access Denied", "Claim management requires owner or manager access.");
        }
        return mutation.apply(claim.get());
    }

    private static ServerPlayer onlinePlayer(ServerPlayer requester, UUID targetId) {
        if (requester == null || targetId == null || requester.level().getServer() == null) {
            return null;
        }
        return requester.level().getServer().getPlayerList().getPlayer(targetId);
    }

    @FunctionalInterface
    private interface ClaimMutation {
        ClaimActionResult apply(ClaimRecord claim);
    }
}
