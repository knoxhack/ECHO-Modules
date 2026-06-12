package com.knoxhack.echoplayercore.teleport;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoplayercore.config.PlayerCoreConfig;
import com.knoxhack.echoplayercore.data.PlayerCoreSavedData;
import com.knoxhack.echoplayercore.data.TeleportLocation;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public final class TeleportService {
    private TeleportService() {
    }

    public static boolean teleportTo(ServerPlayer player, TeleportLocation target, TeleportReason reason) {
        if (player == null || target == null) {
            return false;
        }
        if (!PlayerCoreConfig.enabled()) {
            return false;
        }

        ServerLevel currentLevel = (ServerLevel) player.level();
        ServerLevel targetLevel = player.level().getServer().getLevel(target.dimension());
        if (targetLevel == null) {
            player.sendSystemMessage(Component.translatable("echoplayercore.message.teleport_failed")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        if (shouldStoreBack(reason)) {
            rememberBackLocation(player, reason);
        }

        double x = target.x();
        double y = target.y();
        double z = target.z();
        float yaw = target.yaw();
        float pitch = target.pitch();

        if (currentLevel != targetLevel) {
            player.teleportTo(targetLevel, x, y, z, Set.of(), yaw, pitch, false);
        } else {
            player.teleportTo(x, y, z);
            player.setYRot(yaw);
            player.setXRot(pitch);
        }

        return true;
    }

    public static void rememberBackLocation(ServerPlayer player, TeleportReason reason) {
        if (player == null || !PlayerCoreConfig.backEnabled()) {
            return;
        }
        if (reason == TeleportReason.BACK) {
            return;
        }
        if (!shouldStoreBack(reason)) {
            return;
        }
        TeleportLocation loc = TeleportLocation.fromPlayer(player, reason.name());
        PlayerCoreSavedData.get(((ServerLevel) player.level()).getServer().overworld())
                .getOrCreate(player.getUUID())
                .setLastBackLocation(loc);
    }

    private static boolean shouldStoreBack(TeleportReason reason) {
        if (!PlayerCoreConfig.backEnabled()) {
            return false;
        }
        return switch (reason) {
            case HOME -> PlayerCoreConfig.storeBackOnHome();
            case SPAWN -> PlayerCoreConfig.storeBackOnSpawn();
            case RTP -> PlayerCoreConfig.storeBackOnRtp();
            case DEATH -> PlayerCoreConfig.allowBackAfterDeath();
            default -> PlayerCoreConfig.allowBackAfterTeleport();
        };
    }

    public static boolean isSafeSurface(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BlockState ground = level.getBlockState(pos.below());
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());

        if (!feet.isAir() || !head.isAir()) {
            return false;
        }
        if (ground.isAir() || ground.getCollisionShape(level, pos.below()).isEmpty()) {
            return false;
        }
        if (PlayerCoreConfig.rtpAvoidLava()) {
            if (ground.is(Blocks.LAVA) || feet.is(Blocks.LAVA) || head.is(Blocks.LAVA)) {
                return false;
            }
        }
        if (PlayerCoreConfig.rtpAvoidWater()) {
            FluidState fluid = level.getFluidState(pos);
            FluidState belowFluid = level.getFluidState(pos.below());
            if (!fluid.isEmpty() || !belowFluid.isEmpty()) {
                return false;
            }
        }
        if (PlayerCoreConfig.rtpAvoidPowderSnow()) {
            if (ground.is(Blocks.POWDER_SNOW) || feet.is(Blocks.POWDER_SNOW)) {
                return false;
            }
        }
        if (PlayerCoreConfig.rtpAvoidCactus()) {
            if (ground.is(Blocks.CACTUS) || feet.is(Blocks.CACTUS)) {
                return false;
            }
        }
        if (PlayerCoreConfig.rtpAvoidDeepDark()) {
            if (level.getBiome(pos).is(net.minecraft.resources.Identifier.parse("minecraft:deep_dark"))) {
                return false;
            }
        }
        if (player != null && PlayerCoreConfig.rtpAvoidHighRadiation()) {
            if (EchoRuntimeModules.isLoaded("echoworldcore")) {
                try {
                    com.echoplatform.echocore.api.WorldHazardSnapshot snapshot =
                            com.echoplatform.echocore.api.EchoCoreServices.hazardService().hazardSnapshot(player);
                    if (snapshot != null && !snapshot.safeZone()) {
                        return false;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return true;
    }
}
