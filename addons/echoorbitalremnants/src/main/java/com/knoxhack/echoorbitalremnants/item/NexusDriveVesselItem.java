package com.knoxhack.echoorbitalremnants.item;

import com.knoxhack.echoorbitalremnants.Config;
import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.registry.ModEntities;
import com.knoxhack.echoorbitalremnants.suit.SuitEvents;
import com.knoxhack.echoorbitalremnants.world.ModDimensions;
import com.knoxhack.echoorbitalremnants.world.NexusAnomalyBelt;
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

public class NexusDriveVesselItem extends Item {
    public NexusDriveVesselItem(Properties properties) {
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
                playNexusFeedback(serverPlayer.level(), player.blockPosition(), 0.8F);
                serverPlayer.teleportTo(returnLevel, progress.returnX(), progress.returnY(), progress.returnZ(), Set.of(), player.getYRot(), player.getXRot(), false);
                playNexusFeedback(returnLevel, BlockPos.containing(progress.returnX(), progress.returnY(), progress.returnZ()), 1.5F);
                player.sendSystemMessage(Component.literal("ECHO-7 // Nexus return vector held. Somehow."));
                sendStatus(player, "Nexus return vector burned.");
                return InteractionResult.SUCCESS_SERVER;
            }
            if (SuitEvents.isOrbitalExposure(player) && player.isShiftKeyDown() && !progress.hasReturnPoint()) {
                sendFeedback(player, "Nexus return denied. No docking vector is saved.");
                return InteractionResult.CONSUME;
            }
            if (Config.DIMENSION_UNLOCKS_ENABLED.get() && !progress.deepSpaceProtocolUnlocked() && !player.hasInfiniteMaterials()) {
                sendFeedback(player, "Nexus Drive sealed. Unlock Deep Space Protocol with Titan Survey Core or Nexus Drive telemetry.");
                return InteractionResult.CONSUME;
            }
            if (Config.MID_GAME_OBJECTIVES_ENABLED.get() && !progress.europaArrayGateOpen() && !player.hasInfiniteMaterials()) {
                sendFeedback(player, "Nexus Drive sealed. Complete Europa Thermal Array repairs before Nexus entry can hold.");
                return InteractionResult.CONSUME;
            }
            if (Config.MID_GAME_OBJECTIVES_ENABLED.get() && !progress.saturnRelayGateOpen() && !player.hasInfiniteMaterials()) {
                sendFeedback(player, "Nexus Drive sealed. Complete Saturn Ring Relay repairs before Nexus entry can hold.");
                return InteractionResult.CONSUME;
            }
            if (Config.MID_GAME_OBJECTIVES_ENABLED.get() && !progress.titanPumpGateOpen() && !player.hasInfiniteMaterials()) {
                sendFeedback(player, "Nexus Drive sealed. Complete Titan Methane Pump repairs before Nexus entry can hold.");
                return InteractionResult.CONSUME;
            }
            if (!SuitEvents.isOrbitalExposure(player) && !player.hasInfiniteMaterials()) {
                sendFeedback(player, "Nexus Drive requires orbital staging. Launch to Low Earth Orbit first.");
                return InteractionResult.CONSUME;
            }
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.level() instanceof ServerLevel serverLevel) {
                progress.setReturnPoint(player);
                ServerLevel targetLevel = ModDimensions.resolve(serverPlayer.level().getServer(), ModDimensions.NEXUS_ANOMALY_BELT, serverLevel);
                double targetX = targetLevel.dimension() == ModDimensions.NEXUS_ANOMALY_BELT ? 0.0D : player.getX() + 1536.0D;
                double targetY = targetLevel.dimension() == ModDimensions.NEXUS_ANOMALY_BELT ? 96.0D : Config.ORBITAL_ALTITUDE.get();
                double targetZ = targetLevel.dimension() == ModDimensions.NEXUS_ANOMALY_BELT ? 0.0D : player.getZ() - 512.0D;
                BlockPos target = BlockPos.containing(targetX, targetY, targetZ);
                if (progress.markRouteArrivalSeeded(player, "nexus_anomaly_belt")) {
                    NexusAnomalyBelt.seedEntrySite(targetLevel, target);
                    spawnAnomalyThreats(targetLevel, target);
                }
                progress.markAnomalyBeltEntered(player);
                playNexusFeedback(serverLevel, player.blockPosition(), 0.65F);
                serverPlayer.teleportTo(targetLevel, targetX, targetY, targetZ, Set.of(), player.getYRot(), player.getXRot(), false);
                playNexusFeedback(targetLevel, target, 1.9F);
                player.sendSystemMessage(Component.literal("ECHO-7 // Nexus Anomaly Belt entered. Geometry no longer agrees with itself."));
                sendStatus(player, "Nexus entry committed. Return vector saved.");
            }
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void sendFeedback(Player player, String message) {
        Component component = Component.literal("ECHO-7 // " + message);
        player.sendSystemMessage(component);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(component, true);
        }
    }

    private static void sendStatus(Player player, String message) {
        Component component = Component.literal("ECHO-7 // " + message);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(component, true);
        } else {
            player.sendSystemMessage(component);
        }
    }

    private static void playNexusFeedback(ServerLevel level, BlockPos pos, float pitch) {
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.8F, pitch);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 42, 0.65D, 0.85D, 0.65D, 0.05D);
    }

    private static void spawnAnomalyThreats(ServerLevel level, BlockPos target) {
        for (int i = 0; i < 4; i++) {
            Entity entity = (i == 0 ? ModEntities.ECHO_ZERO.get() : i == 1 ? ModEntities.VACUUM_WRAITH.get() : ModEntities.NEXUS_HUSK.get()).create(level, EntitySpawnReason.EVENT);
            if (entity != null) {
                entity.setPos(target.getX() - 7 + i * 4, target.getY() + (i % 2), target.getZ() + 8 - i * 3);
                level.addFreshEntity(entity);
            }
        }
    }
}
