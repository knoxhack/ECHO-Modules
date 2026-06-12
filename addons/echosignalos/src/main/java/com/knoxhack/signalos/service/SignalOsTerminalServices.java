package com.knoxhack.signalos.service;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoServiceRegistry;
import com.echoplatform.echocore.api.TerminalPlacementService;
import com.echoplatform.echocore.api.TerminalRewardService;
import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.api.SignalOsDriveFileSystem;
import com.knoxhack.signalos.api.SignalOsDriveData;
import com.knoxhack.signalos.api.SignalOsDriveResultCode;
import com.knoxhack.signalos.api.SignalOsDriveWriteResult;
import com.knoxhack.signalos.block.entity.SignalOsTerminalBlockEntity;
import com.knoxhack.signalos.menu.SignalOsTerminalMenu;
import com.knoxhack.signalos.network.SignalOsTerminalSync;
import com.knoxhack.signalos.platform.SignalOsModuleAccess;
import com.knoxhack.signalos.registry.ModBlocks;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class SignalOsTerminalServices {
    private static final int TERMINAL_SEARCH_RADIUS = 48;
    private static final int TERMINAL_VERTICAL_SEARCH_RADIUS = 16;
    private static final Component CONTAINER_TITLE = Component.translatable("container.signalos.terminal");
    private static final Map<UUID, TerminalReference> TERMINAL_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, String> LAST_ACTION_STATUS = new ConcurrentHashMap<>();
    private static final TerminalPlacementService ECHO_CORE_PLACEMENT_SERVICE = new SignalOsPlacementService();
    private static final TerminalRewardService ECHO_CORE_REWARD_SERVICE = new SignalOsRewardService();
    private static final AtomicBoolean ECHO_TERMINAL_SKIP_LOGGED = new AtomicBoolean(false);
    private static final AtomicBoolean FOREIGN_PROVIDER_LOGGED = new AtomicBoolean(false);

    private SignalOsTerminalServices() {
    }

    public static void rememberTerminal(Player player, BlockPos pos) {
        if (player != null && pos != null && player.level() != null) {
            TERMINAL_CACHE.put(player.getUUID(), new TerminalReference(player.level().dimension(), pos.immutable()));
        }
    }

    public static boolean registerEchoCoreServices() {
        if (SignalOsModuleAccess.isLoaded("echoterminal")) {
            if (ECHO_TERMINAL_SKIP_LOGGED.compareAndSet(false, true)) {
                SignalOS.LOGGER.info("SignalOS detected echoterminal; Echo Core terminal placement/reward services remain owned by ECHO Terminal.");
            }
            return false;
        }
        Optional<TerminalPlacementService> existingPlacement =
                EchoServiceRegistry.find(TerminalPlacementService.class);
        Optional<TerminalRewardService> existingReward =
                EchoServiceRegistry.find(TerminalRewardService.class);
        if (ownedByAnotherProvider(existingPlacement, ECHO_CORE_PLACEMENT_SERVICE)
                || ownedByAnotherProvider(existingReward, ECHO_CORE_REWARD_SERVICE)) {
            if (FOREIGN_PROVIDER_LOGGED.compareAndSet(false, true)) {
                SignalOS.LOGGER.info(
                        "SignalOS found an existing Echo Core terminal provider; SignalOS will not replace cross-addon terminal ownership.");
            }
            return false;
        }
        if (existingPlacement.isEmpty()) {
            EchoCoreServices.registerTerminalPlacementService(ECHO_CORE_PLACEMENT_SERVICE);
        }
        if (existingReward.isEmpty()) {
            EchoCoreServices.registerTerminalRewardService(ECHO_CORE_REWARD_SERVICE);
        }
        return true;
    }

    public static boolean storeRewards(ServerPlayer player, String missionId, List<ItemStack> rewards) {
        if (player == null) {
            return false;
        }
        if (!hasUsableRewards(rewards)) {
            player.sendSystemMessage(Component.literal("[SignalOS] Mission cache has no valid rewards."), true);
            return false;
        }
        SignalOsTerminalBlockEntity terminal = findOwnedTerminal(player, true);
        if (terminal == null) {
            player.sendSystemMessage(Component.literal("[SignalOS] No owned SignalOS Terminal found for reward storage."), true);
            return false;
        }
        boolean stored = terminal.storeRewards(missionId, rewards);
        if (stored) {
            player.sendSystemMessage(Component.literal("[SignalOS] Reward stored in terminal inbox."), true);
        } else {
            player.sendSystemMessage(Component.literal("[SignalOS] Terminal reward inbox is full."), true);
        }
        return stored;
    }

    public static boolean claimRewards(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        SignalOsTerminalBlockEntity terminal = findOwnedTerminal(player, true);
        if (terminal == null) {
            player.sendSystemMessage(Component.literal("[SignalOS] No owned SignalOS Terminal found for reward claim."), true);
            return false;
        }
        if (!terminal.hasStoredRewards()) {
            player.sendSystemMessage(Component.literal("[SignalOS] Reward inbox is empty."), true);
            return false;
        }
        return terminal.claimAllRewards(player);
    }

    public static int pendingRewardCount(Player player) {
        SignalOsTerminalBlockEntity terminal = findOwnedTerminal(player, false);
        return terminal == null ? 0 : terminal.storedRewardCount();
    }

    public static boolean openBlockTerminal(ServerPlayer player, Level level, BlockPos pos) {
        if (player == null || level == null || pos == null || level.isClientSide()) {
            return false;
        }
        if (!(level.getBlockEntity(pos) instanceof SignalOsTerminalBlockEntity terminal)) {
            return false;
        }
        terminal.setOwnerIfMissing(player);
        terminal.recordActivity();
        rememberTerminal(player, pos);
        MenuProvider menuProvider = new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new SignalOsTerminalMenu(
                        containerId,
                        playerInventory,
                        ContainerLevelAccess.create(level, pos),
                        terminal),
                CONTAINER_TITLE);
        player.openMenu(menuProvider, pos);
        SignalOsTerminalSync.send(player);
        return true;
    }

    public static boolean openRemoteTerminal(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        SignalOsTerminalBlockEntity terminal = findOwnedTerminal(player, true);
        if (terminal == null) {
            player.sendSystemMessage(Component.literal("[SignalOS] No owned SignalOS Terminal found for remote access."), true);
            return false;
        }
        terminal.recordActivity();
        MenuProvider menuProvider = new SimpleMenuProvider(
                (containerId, playerInventory, p) -> SignalOsTerminalMenu.remote(containerId, playerInventory, terminal),
                CONTAINER_TITLE);
        player.openMenu(menuProvider);
        SignalOsTerminalSync.send(player);
        return true;
    }

    public static boolean hasActiveDrive(Player player, boolean allowSearch) {
        SignalOsTerminalBlockEntity terminal = findOwnedTerminal(player, allowSearch);
        return terminal != null && terminal.hasActiveDrive();
    }

    public static SignalOsDriveData activeDriveData(Player player, boolean allowSearch) {
        SignalOsTerminalBlockEntity terminal = findOwnedTerminal(player, allowSearch);
        return terminal == null || !terminal.hasActiveDrive() ? SignalOsDriveData.EMPTY : terminal.activeDriveData();
    }

    public static SignalOsDriveFileSystem activeDriveFileSystem(Player player, boolean allowSearch) {
        return SignalOsDriveFileSystem.of(activeDriveData(player, allowSearch));
    }

    public static boolean updateActiveDrive(ServerPlayer player, UnaryOperator<SignalOsDriveData> updater) {
        if (player == null || updater == null) {
            return false;
        }
        SignalOsTerminalBlockEntity terminal = findOwnedTerminal(player, true);
        if (terminal == null) {
            player.sendSystemMessage(Component.literal("[SignalOS] No owned SignalOS Terminal found."), true);
            return false;
        }
        if (!terminal.hasActiveDrive()) {
            player.sendSystemMessage(Component.literal("[SignalOS] Insert a data drive into the terminal first."), true);
            return false;
        }
        if (!terminal.activeDriveData().isV2Supported()) {
            player.sendSystemMessage(Component.literal("[SignalOS] Active drive is not a supported V2 drive."), true);
            return false;
        }
        return terminal.updateActiveDrive(updater);
    }

    public static SignalOsDriveWriteResult updateActiveDriveFileSystem(ServerPlayer player,
            Function<SignalOsDriveFileSystem, SignalOsDriveWriteResult> updater) {
        if (player == null || updater == null) {
            return SignalOsDriveWriteResult.failure(SignalOsDriveResultCode.ERROR, SignalOsDriveData.EMPTY,
                    "[SignalOS] Drive action failed.");
        }
        SignalOsTerminalBlockEntity terminal = findOwnedTerminal(player, true);
        if (terminal == null) {
            return SignalOsDriveWriteResult.failure(SignalOsDriveResultCode.NO_ACTIVE_DRIVE, SignalOsDriveData.EMPTY,
                    "[SignalOS] No owned SignalOS Terminal found.");
        }
        if (!terminal.hasActiveDrive()) {
            return SignalOsDriveWriteResult.failure(SignalOsDriveResultCode.NO_ACTIVE_DRIVE, SignalOsDriveData.EMPTY,
                    "[SignalOS] Insert a V2 SignalOS Data Drive first.");
        }
        SignalOsDriveFileSystem fileSystem = SignalOsDriveFileSystem.of(terminal.activeDriveData());
        SignalOsDriveWriteResult result = updater.apply(fileSystem);
        if (result != null && result.success()) {
            terminal.updateActiveDrive(ignored -> result.drive());
            return result;
        }
        return result == null
                ? SignalOsDriveWriteResult.failure(SignalOsDriveResultCode.ERROR, terminal.activeDriveData(),
                        "[SignalOS] Drive action failed.")
                : result;
    }

    public static void recordActionStatus(ServerPlayer player, String message) {
        if (player != null && message != null && !message.isBlank()) {
            LAST_ACTION_STATUS.put(player.getUUID(), message.strip());
        }
    }

    public static String lastActionStatus(Player player) {
        return player == null ? "" : LAST_ACTION_STATUS.getOrDefault(player.getUUID(), "");
    }

    public static SignalOsTerminalBlockEntity findOwnedTerminal(Player player, boolean allowSearch) {
        if (player == null) {
            return null;
        }
        SignalOsTerminalBlockEntity cached = cachedTerminal(player);
        if (cached != null) {
            return cached;
        }
        if (!allowSearch) {
            return null;
        }
        BlockPos center = player.blockPosition();
        for (int dy = -TERMINAL_VERTICAL_SEARCH_RADIUS; dy <= TERMINAL_VERTICAL_SEARCH_RADIUS; dy++) {
            for (int dx = -TERMINAL_SEARCH_RADIUS; dx <= TERMINAL_SEARCH_RADIUS; dx++) {
                for (int dz = -TERMINAL_SEARCH_RADIUS; dz <= TERMINAL_SEARCH_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (player.level().getBlockEntity(pos) instanceof SignalOsTerminalBlockEntity terminal
                            && terminal.isExplicitOwner(player)) {
                        rememberTerminal(player, pos);
                        return terminal;
                    }
                }
            }
        }
        return null;
    }

    private static SignalOsTerminalBlockEntity cachedTerminal(Player player) {
        TerminalReference cached = TERMINAL_CACHE.get(player.getUUID());
        if (cached == null) {
            return null;
        }
        if (!cached.dimension().equals(player.level().dimension())) {
            TERMINAL_CACHE.remove(player.getUUID(), cached);
            return null;
        }
        if (player.level().getBlockEntity(cached.pos()) instanceof SignalOsTerminalBlockEntity terminal
                && terminal.isExplicitOwner(player)) {
            return terminal;
        }
        TERMINAL_CACHE.remove(player.getUUID(), cached);
        return null;
    }

    private static boolean hasUsableRewards(List<ItemStack> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return false;
        }
        for (ItemStack reward : rewards) {
            if (reward != null && !reward.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static <T> boolean ownedByAnotherProvider(Optional<T> existing, T signalOsProvider) {
        return existing.isPresent() && existing.get() != signalOsProvider;
    }

    private record TerminalReference(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private static final class SignalOsPlacementService implements TerminalPlacementService {
        @Override
        public boolean placeTerminal(Level level, BlockPos pos, Player owner) {
            if (level == null || pos == null || level.isClientSide()) {
                return false;
            }
            level.setBlockAndUpdate(pos, ModBlocks.TERMINAL.get().defaultBlockState());
            if (level.getBlockEntity(pos) instanceof SignalOsTerminalBlockEntity terminal && owner != null) {
                terminal.setOwnerIfMissing(owner);
                rememberTerminal(owner, pos);
            }
            return true;
        }

        @Override
        public BlockState structureBlockState() {
            return ModBlocks.TERMINAL.get().defaultBlockState();
        }

        @Override
        public boolean isTerminalBlock(BlockState state) {
            return state != null && ModBlocks.isComputerAccessBlock(state.getBlock());
        }
    }

    private static final class SignalOsRewardService implements TerminalRewardService {
        @Override
        public boolean storeRewards(ServerPlayer player, String missionId, List<ItemStack> rewards) {
            return SignalOsTerminalServices.storeRewards(player, missionId, rewards);
        }

        @Override
        public boolean claimRewards(ServerPlayer player) {
            return SignalOsTerminalServices.claimRewards(player);
        }

        @Override
        public int pendingRewardCount(Player player) {
            return SignalOsTerminalServices.pendingRewardCount(player);
        }
    }
}
