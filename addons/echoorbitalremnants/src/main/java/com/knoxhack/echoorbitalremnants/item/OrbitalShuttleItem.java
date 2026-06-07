package com.knoxhack.echoorbitalremnants.item;

import com.knoxhack.echoorbitalremnants.Config;
import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.registry.ModEntities;
import com.knoxhack.echoorbitalremnants.suit.SuitEvents;
import com.knoxhack.echoorbitalremnants.world.LunarScarZone;
import com.knoxhack.echoorbitalremnants.world.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import java.util.Set;

public class OrbitalShuttleItem extends Item {
    public OrbitalShuttleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            EchoTerminalProgress progress = EchoTerminalProgress.get(player);
            if (SuitEvents.isOrbitalExposure(player) && progress.hasReturnPoint() && player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
                ServerLevel returnLevel = serverPlayer.level().getServer().getLevel(ModDimensions.keyFromString(progress.returnDimension()));
                if (returnLevel == null) {
                    returnLevel = serverPlayer.level().getServer().overworld();
                }
                playShuttleFeedback(serverPlayer.level(), player.blockPosition(), 1.25F);
                serverPlayer.teleportTo(returnLevel, progress.returnX(), progress.returnY(), progress.returnZ(), Set.of(), player.getYRot(), player.getXRot(), false);
                playShuttleFeedback(returnLevel, BlockPos.containing(progress.returnX(), progress.returnY(), progress.returnZ()), 1.55F);
                sendFeedback(player, "Shuttle return burn complete.");
                return InteractionResult.SUCCESS_SERVER;
            }
            if (SuitEvents.isOrbitalExposure(player) && player.isShiftKeyDown() && !progress.hasReturnPoint()) {
                sendFeedback(player, "Shuttle return denied. No docking vector is saved.");
                return InteractionResult.CONSUME;
            }
            if (Config.DIMENSION_UNLOCKS_ENABLED.get() && !progress.lunarSignalUnlocked() && !player.hasInfiniteMaterials()) {
                sendFeedback(player, "Shuttle lockout. Restore Station Life Support, then SCAN to resolve the Lunar Signal.");
                return InteractionResult.CONSUME;
            }
            if (Config.MID_GAME_OBJECTIVES_ENABLED.get() && !progress.stationNetworkGateOpen() && !player.hasInfiniteMaterials()) {
                sendFeedback(player, "Shuttle lockout. Repair three Station Relay Nodes to restore the Station Network.");
                return InteractionResult.CONSUME;
            }
            if (!SuitEvents.isOrbitalExposure(player) && !player.hasInfiniteMaterials()) {
                sendFeedback(player, "Shuttle requires orbital staging. Launch to Low Earth Orbit first.");
                return InteractionResult.CONSUME;
            }
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.level() instanceof ServerLevel serverLevel) {
                progress.setReturnPoint(player);
                ServerLevel targetLevel = ModDimensions.resolve(serverPlayer.level().getServer(), ModDimensions.LUNAR_SCAR_ZONE, serverLevel);
                double targetX = targetLevel.dimension() == ModDimensions.LUNAR_SCAR_ZONE ? 0.0D : player.getX() + 768.0D;
                double targetY = targetLevel.dimension() == ModDimensions.LUNAR_SCAR_ZONE ? 96.0D : Config.ORBITAL_ALTITUDE.get();
                double targetZ = targetLevel.dimension() == ModDimensions.LUNAR_SCAR_ZONE ? 0.0D : player.getZ() + 128.0D;
                BlockPos target = BlockPos.containing(targetX, targetY, targetZ);
                if (progress.markRouteArrivalSeeded(player, "lunar_scar_zone")) {
                    LunarScarZone.seedLandingSite(targetLevel, target);
                    spawnNexusHusks(targetLevel, target);
                }
                progress.markLunarSignalInvestigated(player);
                playShuttleFeedback(serverLevel, player.blockPosition(), 0.95F);
                serverPlayer.teleportTo(targetLevel, targetX, targetY, targetZ, Set.of(), player.getYRot(), player.getXRot(), false);
                playShuttleFeedback(targetLevel, target, 1.35F);
                sendFeedback(player, "Lunar Scar Zone acquired. Gravity irregularities confirmed. Return vector saved.",
                        "Lunar route burn complete. Return vector saved.");
            }
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void sendFeedback(Player player, String message) {
        sendFeedback(player, message, message);
    }

    private static void sendFeedback(Player player, String message, String status) {
        Component component = Component.literal("ECHO-7 // " + message);
        player.sendSystemMessage(component);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal("ECHO-7 // " + status), true);
        }
    }

    private static void playShuttleFeedback(ServerLevel level, BlockPos pos, float pitch) {
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, pitch);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                26, 0.55D, 0.65D, 0.55D, 0.03D);
    }

    private static void spawnNexusHusks(ServerLevel level, BlockPos target) {
        for (int i = 0; i < 3; i++) {
            Entity entity = (i == 0 ? ModEntities.LUNAR_NEXUS_HUSK.get() : ModEntities.NEXUS_HUSK.get()).create(level, EntitySpawnReason.EVENT);
            if (entity != null) {
                entity.setPos(target.getX() + 5 - i * 4, target.getY(), target.getZ() - 6 + i * 3);
                level.addFreshEntity(entity);
            }
        }
    }
}
