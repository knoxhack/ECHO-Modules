package com.knoxhack.echo.adaptercore;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.SleepFinishedTimeEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * AdapterCore backend bridge for world and level event payloads.
 */
public final class EchoBackendWorldEventBridge {
    private EchoBackendWorldEventBridge() {
    }

    public static ServerLevel chunkLoadServerLevel(Object event) {
        if (event instanceof ChunkEvent.Load load && load.getLevel() instanceof ServerLevel level) {
            return level;
        }
        return null;
    }

    public static boolean isNewServerChunkLoad(Object event) {
        return event instanceof ChunkEvent.Load load
                && load.isNewChunk()
                && !load.getLevel().isClientSide()
                && load.getLevel() instanceof ServerLevel;
    }

    public static ChunkAccess chunkLoadChunk(Object event) {
        if (event instanceof ChunkEvent.Load load) {
            return load.getChunk();
        }
        return null;
    }

    public static ServerLevel postTickServerLevel(Object event) {
        if (event instanceof LevelTickEvent.Post post && post.getLevel() instanceof ServerLevel level) {
            return level;
        }
        return null;
    }

    public static ServerPlayer loggedInServerPlayer(Object event) {
        if (event instanceof PlayerEvent.PlayerLoggedInEvent loggedIn && loggedIn.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    public static ServerPlayer playerEventServerPlayer(Object event) {
        if (event instanceof PlayerEvent playerEvent && playerEvent.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    public static ServerPlayer advancementServerPlayer(Object event) {
        if (event instanceof AdvancementEvent.AdvancementEarnEvent advancement && advancement.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    public static Identifier advancementId(Object event) {
        return event instanceof AdvancementEvent.AdvancementEarnEvent advancement ? advancement.getAdvancement().id() : null;
    }

    public static ServerPlayer postTickServerPlayer(Object event) {
        if (event instanceof PlayerTickEvent.Post post && post.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    public static net.minecraft.server.MinecraftServer serverTickServer(Object event) {
        if (event instanceof ServerTickEvent.Pre pre) {
            return pre.getServer();
        }
        if (event instanceof ServerTickEvent.Post post) {
            return post.getServer();
        }
        return null;
    }

    public static net.minecraft.server.MinecraftServer serverStartingServer(Object event) {
        return event instanceof ServerStartingEvent starting ? starting.getServer() : null;
    }

    public static net.minecraft.server.MinecraftServer serverStartedServer(Object event) {
        return event instanceof ServerStartedEvent started ? started.getServer() : null;
    }

    public static boolean isServerStopping(Object event) {
        return event instanceof ServerStoppingEvent;
    }

    public static Player postTickPlayer(Object event) {
        return event instanceof PlayerTickEvent.Post post ? post.getEntity() : null;
    }

    public static ServerPlayer rightClickBlockServerPlayer(Object event) {
        if (event instanceof PlayerInteractEvent.RightClickBlock rightClick && rightClick.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    public static BlockPos rightClickBlockPos(Object event) {
        return event instanceof PlayerInteractEvent.RightClickBlock rightClick ? rightClick.getPos() : null;
    }

    public static boolean addServerReloadListener(Object event, Identifier id, PreparableReloadListener listener) {
        if (event instanceof AddServerReloadListenersEvent reload && id != null && listener != null) {
            reload.addListener(id, listener);
            return true;
        }
        return false;
    }

    public static Level entityJoinLevel(Object event) {
        return event instanceof EntityJoinLevelEvent join ? join.getLevel() : null;
    }

    public static Entity entityJoinEntity(Object event) {
        return event instanceof EntityJoinLevelEvent join ? join.getEntity() : null;
    }

    public static ServerPlayer entityInteractServerPlayer(Object event) {
        if (event instanceof PlayerInteractEvent.EntityInteract interact && interact.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    public static Entity entityInteractTarget(Object event) {
        return event instanceof PlayerInteractEvent.EntityInteract interact ? interact.getTarget() : null;
    }

    public static InteractionHand entityInteractHand(Object event) {
        return event instanceof PlayerInteractEvent.EntityInteract interact ? interact.getHand() : InteractionHand.MAIN_HAND;
    }

    public static void cancelEntityInteract(Object event, InteractionResult result) {
        if (event instanceof PlayerInteractEvent.EntityInteract interact) {
            interact.setCanceled(true);
            interact.setCancellationResult(result == null ? InteractionResult.SUCCESS : result);
        }
    }

    public static Level blockEventLevel(Object event) {
        if (event instanceof BreakBlockEvent breakEvent && breakEvent.getLevel() instanceof Level level) {
            return level;
        }
        if (event instanceof BlockEvent blockEvent && blockEvent.getLevel() instanceof Level level) {
            return level;
        }
        return null;
    }

    public static BlockPos blockEventPos(Object event) {
        if (event instanceof BreakBlockEvent breakEvent) {
            return breakEvent.getPos();
        }
        return event instanceof BlockEvent blockEvent ? blockEvent.getPos() : null;
    }

    public static Entity blockEventEntity(Object event) {
        return event instanceof BlockEvent.EntityPlaceEvent place ? place.getEntity() : null;
    }

    public static ServerPlayer blockBreakServerPlayer(Object event) {
        return event instanceof BreakBlockEvent breakEvent && breakEvent.getPlayer() instanceof ServerPlayer player
                ? player
                : null;
    }

    public static ServerLevel sleepFinishedServerLevel(Object event) {
        return event instanceof SleepFinishedTimeEvent sleep && sleep.getLevel() instanceof ServerLevel level
                ? level
                : null;
    }

    public static boolean sleepFinishedCanceled(Object event) {
        return event instanceof SleepFinishedTimeEvent sleep && sleep.isCanceled();
    }

    public static long sleepFinishedSkippedTicks(Object event, ServerLevel level) {
        if (!(event instanceof SleepFinishedTimeEvent sleep) || level == null) {
            return 0L;
        }
        return switch (sleep.getAdjustment()) {
            case net.neoforged.neoforge.common.util.ClockAdjustment.Relative relative -> Math.max(0L, relative.ticks());
            case net.neoforged.neoforge.common.util.ClockAdjustment.Absolute absolute ->
                    Math.max(0L, absolute.ticks() - level.getGameTime());
            case net.neoforged.neoforge.common.util.ClockAdjustment.Marker ignored ->
                    ticksUntilNextDay(level.getGameTime());
        };
    }

    private static long ticksUntilNextDay(long gameTime) {
        long dayTime = Math.floorMod(gameTime, 24000L);
        return dayTime == 0L ? 24000L : 24000L - dayTime;
    }

    public static Level explosionLevel(Object event) {
        return event instanceof ExplosionEvent.Detonate detonate ? detonate.getLevel() : null;
    }

    public static java.util.List<BlockPos> explosionAffectedBlocks(Object event) {
        return event instanceof ExplosionEvent.Detonate detonate ? detonate.getAffectedBlocks() : java.util.List.of();
    }

    public static void cancel(Object event) {
        if (event instanceof net.neoforged.bus.api.ICancellableEvent cancellable) {
            cancellable.setCanceled(true);
        }
    }

    public static void cancelRightClickBlock(Object event, InteractionResult result) {
        if (event instanceof PlayerInteractEvent.RightClickBlock rightClick) {
            rightClick.setCancellationResult(result == null ? InteractionResult.FAIL : result);
            rightClick.setCanceled(true);
        }
    }

    public static Player cloneNewPlayer(Object event) {
        return event instanceof PlayerEvent.Clone clone ? clone.getEntity() : null;
    }

    public static Player cloneOriginalPlayer(Object event) {
        return event instanceof PlayerEvent.Clone clone ? clone.getOriginal() : null;
    }

    public static ServerPlayer livingDamageServerPlayer(Object event) {
        if (event instanceof LivingDamageEvent.Pre damage && damage.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    public static Entity livingDamageSourceEntity(Object event) {
        return event instanceof LivingDamageEvent.Pre damage ? damage.getSource().getEntity() : null;
    }

    public static float livingDamageAmount(Object event) {
        return event instanceof LivingDamageEvent.Pre damage ? damage.getNewDamage() : 0.0F;
    }

    public static void setLivingDamageAmount(Object event, float amount) {
        if (event instanceof LivingDamageEvent.Pre damage) {
            damage.setNewDamage(amount);
        }
    }

    public static ItemStack itemCraftedStack(Object event) {
        return event instanceof PlayerEvent.ItemCraftedEvent crafted ? crafted.getCrafting() : ItemStack.EMPTY;
    }

    public static Player itemCraftedPlayer(Object event) {
        return event instanceof PlayerEvent.ItemCraftedEvent crafted ? crafted.getEntity() : null;
    }

    public static ServerPlayer itemPickupServerPlayer(Object event) {
        if (event instanceof ItemEntityPickupEvent.Post pickup && pickup.getPlayer() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    public static ItemStack itemPickupOriginalStack(Object event) {
        if (event instanceof ItemEntityPickupEvent.Post pickup) {
            return pickup.getOriginalStack();
        }
        if (event instanceof ItemEntity itemEntity) {
            return itemEntity.getItem();
        }
        return ItemStack.EMPTY;
    }
}
