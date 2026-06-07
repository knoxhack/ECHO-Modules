package com.knoxhack.echorecovery.grave;

import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.api.RecoveryIntegrations;
import com.knoxhack.echorecovery.block.entity.GraveBlockEntity;
import com.knoxhack.echorecovery.config.RecoveryConfig;
import com.knoxhack.echorecovery.data.RecoveryWorldData;
import com.knoxhack.echorecovery.integration.RecoveryDataCoreIntegration;
import com.knoxhack.echorecovery.integration.RecoveryIntegrationDispatcher;
import com.knoxhack.echorecovery.integration.RecoveryMissionCoreIntegration;
import com.knoxhack.echorecovery.item.GraveKeyItem;
import com.knoxhack.echorecovery.registry.ModBlocks;
import com.knoxhack.echorecovery.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class GraveManager {
    private GraveManager() {}

    public record PlacementResult(BlockPos pos, String fallbackReason, List<String> hazardNotes, boolean temporaryPlatformCreated) {
        public PlacementResult {
            fallbackReason = fallbackReason == null ? "" : fallbackReason;
            hazardNotes = List.copyOf(hazardNotes == null ? List.of() : hazardNotes);
        }
    }

    public static GraveAccessResult accessGrave(GraveBlockEntity grave, UUID playerId, boolean admin) {
        return accessGrave(grave, null, playerId, admin);
    }

    public static GraveAccessResult accessGrave(GraveBlockEntity grave, Player player, boolean admin) {
        return accessGrave(grave, player, player == null ? null : player.getUUID(), admin);
    }

    private static GraveAccessResult accessGrave(GraveBlockEntity grave, Player player, UUID playerId, boolean admin) {
        if (grave.isRecovered() || isExpired(grave)) {
            return GraveAccessResult.GONE;
        }
        UUID owner = grave.ownerId();
        if (owner.getMostSignificantBits() == 0 && owner.getLeastSignificantBits() == 0) {
            return GraveAccessResult.ALLOWED;
        }
        if (admin && RecoveryConfig.ADMIN_BYPASS.get()) {
            return GraveAccessResult.ALLOWED;
        }
        if (playerId == null) {
            return GraveAccessResult.DENIED;
        }
        if (playerId.equals(owner)) {
            return GraveAccessResult.ALLOWED;
        }
        if (grave.isSharedWith(playerId) && keyRequirementSatisfied(grave, player)) {
            return GraveAccessResult.ALLOWED;
        }
        if (RecoveryConfig.TEAM_ACCESS.get()) {
            if (player != null && sameTeam(grave, player) && keyRequirementSatisfied(grave, player)) {
                return GraveAccessResult.ALLOWED;
            }
        }
        int publicAfter = RecoveryConfig.PUBLIC_ACCESS_AFTER_MINUTES.get();
        if (publicAfter > 0) {
            long ageMinutes = (System.currentTimeMillis() - grave.createdAt()) / 60000L;
            if (ageMinutes >= publicAfter && keyRequirementSatisfied(grave, player)) {
                return GraveAccessResult.ALLOWED;
            }
        }
        if (RecoveryConfig.GRAVE_THEFT.get() && keyRequirementSatisfied(grave, player)) {
            return GraveAccessResult.ALLOWED;
        }
        return GraveAccessResult.DENIED;
    }

    public static boolean canBreak(GraveBlockEntity grave, UUID playerId, boolean admin) {
        if (admin && RecoveryConfig.ADMIN_BYPASS.get()) {
            return true;
        }
        UUID owner = grave.ownerId();
        if (owner.getMostSignificantBits() == 0 && owner.getLeastSignificantBits() == 0) {
            return true;
        }
        return playerId.equals(owner);
    }

    public static boolean recoverGrave(GraveBlockEntity grave, Player player) {
        if (grave.isRecovered() || isExpired(grave)) {
            return false;
        }
        Level level = grave.getLevel();
        if (level == null || level.isClientSide()) {
            return false;
        }
        NonNullList<ItemStack> items = grave.items();
        Inventory inv = player.getInventory();
        List<ItemStack> overflow = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!restoreStack(player, grave, i, stack.copy())) {
                overflow.add(stack.copy());
                if (RecoveryConfig.DROP_OVERFLOW_ITEMS.get()) {
                    items.set(i, ItemStack.EMPTY);
                }
            } else {
                items.set(i, ItemStack.EMPTY);
            }
        }
        if (!overflow.isEmpty() && !RecoveryConfig.DROP_OVERFLOW_ITEMS.get()) {
            grave.setChanged();
            if (level instanceof ServerLevel serverLevel) {
                RecoveryWorldData.getOrCreate(serverLevel).updateFromBlockEntity(grave);
                if (player instanceof ServerPlayer serverPlayer) {
                    RecoveryDataCoreIntegration.recordPartialRecovered(serverPlayer);
                }
            }
            return false;
        }
        int xp = grave.xpStored();
        if (xp > 0 && RecoveryConfig.STORE_XP.get()) {
            player.giveExperiencePoints(xp);
            grave.setXpStored(0);
        }
        level.playSound(null, grave.getBlockPos(), ModSounds.GRAVE_RECOVER.get(), net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        grave.setRecovered(true);
        grave.setChanged();
        level.removeBlock(grave.getBlockPos(), false);
        if (!overflow.isEmpty() && RecoveryConfig.DROP_OVERFLOW_ITEMS.get()) {
            for (ItemStack stack : overflow) {
                Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), stack);
            }
        }
        if (shouldConsumeMatchingKey(grave)) {
            consumeMatchingKey(player, grave);
        }
        if (level instanceof ServerLevel serverLevel) {
            RecoveryWorldData data = RecoveryWorldData.getOrCreate(serverLevel);
            data.markRecovered(grave.ownerId(), grave.getBlockPos());
            if (player instanceof ServerPlayer serverPlayer) {
                RecoveryDataCoreIntegration.recordGraveRecovered(serverPlayer);
                RecoveryMissionCoreIntegration.recordRecovered(serverPlayer);
                RecoveryIntegrations.graveRecovered(serverPlayer, grave.snapshot());
                RecoveryIntegrationDispatcher.onGraveRecovered(serverPlayer, grave.getBlockPos(), grave.graveId().toString());
            }
        }
        return true;
    }

    public static boolean shouldConsumeMatchingKey(GraveBlockEntity grave) {
        return RecoveryConfig.GRAVE_KEY_CONSUMED.get() || isAshfallFieldRecoveryCache(grave);
    }

    private static boolean isAshfallFieldRecoveryCache(GraveBlockEntity grave) {
        return grave != null && DeathHandler.ASHFALL_FIELD_RECOVERY_CACHE.toString().equals(grave.graveTypeId());
    }

    public static boolean isExpired(GraveBlockEntity grave) {
        if (grave == null) {
            return true;
        }
        if (grave.isExpired()) {
            return true;
        }
        long expiresAt = grave.expiresAt();
        if (expiresAt > 0L && System.currentTimeMillis() >= expiresAt) {
            grave.setExpired(true);
            if (grave.getLevel() instanceof ServerLevel serverLevel) {
                RecoveryWorldData.getOrCreate(serverLevel).markExpired(grave.ownerId(), grave.getBlockPos());
                ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(grave.ownerId());
                RecoveryDataCoreIntegration.recordExpired(owner);
                RecoveryIntegrations.graveExpired(owner, grave.snapshot());
            }
            return true;
        }
        return false;
    }

    public static void dropGraveContents(GraveBlockEntity grave, Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        for (ItemStack stack : grave.items()) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
            }
        }
        grave.items().clear();
        int xp = grave.xpStored();
        if (xp > 0 && level instanceof ServerLevel serverLevel) {
            ExperienceOrb.award(serverLevel, net.minecraft.world.phys.Vec3.atCenterOf(pos), xp);
            grave.setXpStored(0);
        }
    }

    public static BlockPos findSafePosition(ServerLevel level, BlockPos origin) {
        return findPlacement(level, origin).pos();
    }

    public static PlacementResult findPlacement(ServerPlayer player, ServerLevel level, BlockPos origin, String deathCause) {
        if (RecoveryConfig.SAFE_PLACEMENT.get()) {
            java.util.Optional<BlockPos> provided = RecoveryIntegrations.findPlacement(player, level, origin, deathCause);
            if (provided.isPresent() && isSafe(level, provided.get())) {
                return new PlacementResult(provided.get(), "integration placement provider",
                        hazardNotes(level, origin), false);
            }
        }
        return findPlacement(level, origin);
    }

    public static PlacementResult findPlacement(ServerLevel level, BlockPos origin) {
        if (!RecoveryConfig.SAFE_PLACEMENT.get()) {
            return new PlacementResult(origin, "safe placement disabled", List.of("Placed at death position by config."), false);
        }
        if (isSafe(level, origin)) {
            return new PlacementResult(origin, "", List.of(), false);
        }
        int radius = RecoveryConfig.SAFE_PLACEMENT_RADIUS.get();
        for (int r = 1; r <= radius; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.abs(dx) != r && Math.abs(dy) != r && Math.abs(dz) != r) {
                            continue;
                        }
                        BlockPos test = origin.offset(dx, dy, dz);
                        if (isSafe(level, test)) {
                            return new PlacementResult(test, "nearby safe block", hazardNotes(level, origin), false);
                        }
                    }
                }
            }
        }
        if (RecoveryConfig.CREATE_TEMPORARY_PLATFORM.get()) {
            BlockPos platform = clampToBuildHeight(level, origin);
            if (level.isLoaded(platform) && level.getWorldBorder().isWithinBounds(platform)) {
                level.setBlock(platform.below(), Blocks.COBBLESTONE.defaultBlockState(), 3);
                level.setBlock(platform, Blocks.AIR.defaultBlockState(), 3);
                return new PlacementResult(platform, "temporary platform", hazardNotes(level, origin), true);
            }
        }
        return new PlacementResult(clampToBuildHeight(level, origin), "unsafe fallback", hazardNotes(level, origin), false);
    }

    private static boolean isSafe(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinY() || pos.getY() >= level.getMinY() + level.getHeight()) {
            return false;
        }
        if (!level.isLoaded(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());
        if (state.isAir() && below.isSolidRender() && state.getFluidState().isEmpty() && below.getFluidState().isEmpty()) {
            return !isDangerous(level, pos);
        }
        return false;
    }

    private static boolean isDangerous(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block b = state.getBlock();
        if (b == Blocks.LAVA || b == Blocks.FIRE || b == Blocks.SOUL_FIRE || b == Blocks.CACTUS || b == Blocks.MAGMA_BLOCK) {
            return true;
        }
        BlockState below = level.getBlockState(pos.below());
        if (below.getBlock() == Blocks.LAVA || below.getBlock() == Blocks.FIRE) {
            return true;
        }
        return false;
    }

    public static boolean hasMatchingKey(Player player, GraveBlockEntity grave) {
        if (player == null || grave == null || !RecoveryConfig.GRAVE_KEY_ENABLED.get()) {
            return false;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            UUID keyId = GraveKeyItem.getGraveId(stack);
            if (keyId != null && keyId.equals(grave.graveId())) {
                return true;
            }
        }
        return false;
    }

    public static boolean consumeMatchingKey(Player player, GraveBlockEntity grave) {
        if (player == null || grave == null || !RecoveryConfig.GRAVE_KEY_ENABLED.get()) {
            return false;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            UUID keyId = GraveKeyItem.getGraveId(stack);
            if (keyId != null && keyId.equals(grave.graveId())) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }

    private static boolean restoreStack(Player player, GraveBlockEntity grave, int graveSlot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        Inventory inventory = player.getInventory();
        int originalSlot = grave.originalSlot(graveSlot);
        if (originalSlot >= 0 && originalSlot < inventory.getContainerSize()) {
            ItemStack current = inventory.getItem(originalSlot);
            if (current.isEmpty()) {
                inventory.setItem(originalSlot, stack);
                return true;
            }
            if (ItemStack.isSameItemSameComponents(current, stack)) {
                int max = current.getMaxStackSize();
                if (current.getCount() + stack.getCount() <= max) {
                    current.grow(stack.getCount());
                    return true;
                }
            }
        }
        return inventory.add(stack);
    }

    public static BlockPos resolveVoidDeathPosition(ServerPlayer player) {
        RecoveryConfig.VoidDeathMode mode = RecoveryConfig.VOID_DEATH_MODE.get();
        if (mode == RecoveryConfig.VoidDeathMode.DISABLED) {
            return player.blockPosition();
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return player.blockPosition();
        }
        if (RecoveryConfig.FALLBACK_TO_BED.get()) {
            if (player.getRespawnConfig() != null) {
                return serverLevel.getRespawnData().pos();
            }
        }
        if (RecoveryConfig.FALLBACK_TO_SPAWN.get()) {
            return serverLevel.getRespawnData().pos();
        }
        return player.blockPosition();
    }

    private static boolean keyRequirementSatisfied(GraveBlockEntity grave, Player player) {
        return !RecoveryConfig.GRAVE_KEY_REQUIRED.get() || hasMatchingKey(player, grave);
    }

    private static boolean sameTeam(GraveBlockEntity grave, Player player) {
        if (grave == null || player == null || player.getTeam() == null) {
            return false;
        }
        return player.level().players().stream()
                .filter(other -> other.getUUID().equals(grave.ownerId()))
                .findFirst()
                .map(owner -> owner.getTeam() != null && owner.getTeam().isAlliedTo(player.getTeam()))
                .orElse(false);
    }

    private static BlockPos clampToBuildHeight(ServerLevel level, BlockPos origin) {
        int min = level.getMinY() + 1;
        int max = level.getMinY() + level.getHeight() - 2;
        return new BlockPos(origin.getX(), Math.max(min, Math.min(max, origin.getY())), origin.getZ());
    }

    private static List<String> hazardNotes(ServerLevel level, BlockPos origin) {
        List<String> notes = new ArrayList<>();
        BlockState state = level.getBlockState(clampToBuildHeight(level, origin));
        BlockState below = level.getBlockState(clampToBuildHeight(level, origin).below());
        if (!state.getFluidState().isEmpty() || !below.getFluidState().isEmpty()) {
            notes.add("Fluid at death site; grave relocated to a safer block.");
        }
        if (isDangerous(level, clampToBuildHeight(level, origin))) {
            notes.add("Hazardous block at death site; safe relocation was attempted.");
        }
        if (!level.getWorldBorder().isWithinBounds(origin)) {
            notes.add("Death site was outside the world border.");
        }
        return notes;
    }
}
