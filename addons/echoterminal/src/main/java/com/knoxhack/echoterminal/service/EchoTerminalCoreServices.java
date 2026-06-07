package com.knoxhack.echoterminal.service;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.TerminalPlacementService;
import com.knoxhack.echocore.api.TerminalRewardService;
import com.knoxhack.echoterminal.block.entity.EchoTerminalBlockEntity;
import com.knoxhack.echoterminal.registry.ModBlocks;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class EchoTerminalCoreServices {
    private static final int TERMINAL_SEARCH_RADIUS = 48;
    private static final int TERMINAL_VERTICAL_SEARCH_RADIUS = 16;
    private static final Map<UUID, TerminalReference> TERMINAL_CACHE = new ConcurrentHashMap<>();

    private EchoTerminalCoreServices() {
    }

    public static void rememberTerminal(Player player, BlockPos pos) {
        if (player != null && pos != null) {
            TERMINAL_CACHE.put(player.getUUID(), new TerminalReference(player.level().dimension(), pos.immutable()));
        }
    }

    public static void register() {
        EchoCoreServices.registerTerminalPlacementService(new TerminalPlacementService() {
            @Override
            public boolean placeTerminal(Level level, BlockPos pos, Player owner) {
                if (level == null || pos == null || level.isClientSide()) {
                    return false;
                }
                level.setBlockAndUpdate(pos, ModBlocks.ECHO_TERMINAL_BLOCK.get().defaultBlockState());
                if (level.getBlockEntity(pos) instanceof EchoTerminalBlockEntity terminal && owner != null) {
                    terminal.setOwnerIfMissing(owner);
                    rememberTerminal(owner, pos);
                }
                return true;
            }

            @Override
            public BlockState structureBlockState() {
                return ModBlocks.ECHO_TERMINAL_BLOCK.get().defaultBlockState();
            }

            @Override
            public boolean isTerminalBlock(BlockState state) {
                return state != null && state.is(ModBlocks.ECHO_TERMINAL_BLOCK.get());
            }
        });

        EchoCoreServices.registerTerminalRewardService(new TerminalRewardService() {
            @Override
            public boolean storeRewards(ServerPlayer player, String missionId, List<ItemStack> rewards) {
                EchoTerminalBlockEntity terminal = findOwnedTerminal(player, true);
                if (terminal == null) {
                    return false;
                }
                boolean stored = terminal.storeRewards(missionId, rewards);
                if (stored) {
                    player.sendSystemMessage(Component.literal("[ECHO-7] Support cache stored in terminal."), true);
                }
                return stored;
            }

            @Override
            public boolean claimRewards(ServerPlayer player) {
                EchoTerminalBlockEntity terminal = findOwnedTerminal(player, true);
                return terminal != null && terminal.claimAllRewards(player);
            }

            @Override
            public int pendingRewardCount(Player player) {
                EchoTerminalBlockEntity terminal = findOwnedTerminal(player, false);
                return terminal == null ? 0 : terminal.getStoredRewardCount();
            }
        });
    }

    private static EchoTerminalBlockEntity findOwnedTerminal(Player player, boolean allowSearch) {
        if (player == null) {
            return null;
        }
        EchoTerminalBlockEntity cached = cachedTerminal(player);
        if (cached != null) {
            return cached;
        }
        if (!allowSearch) {
            return null;
        }
        BlockPos center = player.blockPosition();
        EchoTerminalBlockEntity unowned = null;
        for (int dy = -TERMINAL_VERTICAL_SEARCH_RADIUS; dy <= TERMINAL_VERTICAL_SEARCH_RADIUS; dy++) {
            for (int dx = -TERMINAL_SEARCH_RADIUS; dx <= TERMINAL_SEARCH_RADIUS; dx++) {
                for (int dz = -TERMINAL_SEARCH_RADIUS; dz <= TERMINAL_SEARCH_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (player.level().getBlockEntity(pos) instanceof EchoTerminalBlockEntity terminal) {
                        if (terminal.isExplicitOwner(player)) {
                            rememberTerminal(player, pos);
                            return terminal;
                        }
                        if (unowned == null && terminal.isOwner(player)) {
                            unowned = terminal;
                        }
                    }
                }
            }
        }
        if (unowned != null) {
            unowned.setOwnerIfMissing(player);
            rememberTerminal(player, unowned.getBlockPos());
            return unowned;
        }
        return null;
    }

    private static EchoTerminalBlockEntity cachedTerminal(Player player) {
        TerminalReference cached = TERMINAL_CACHE.get(player.getUUID());
        if (cached == null) {
            return null;
        }
        if (!cached.dimension().equals(player.level().dimension())) {
            return null;
        }
        if (player.level().getBlockEntity(cached.pos()) instanceof EchoTerminalBlockEntity terminal
                && terminal.isExplicitOwner(player)) {
            return terminal;
        }
        TERMINAL_CACHE.remove(player.getUUID(), cached);
        return null;
    }

    private record TerminalReference(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
