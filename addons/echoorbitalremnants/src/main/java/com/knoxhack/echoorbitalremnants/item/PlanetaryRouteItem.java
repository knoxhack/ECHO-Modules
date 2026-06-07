package com.knoxhack.echoorbitalremnants.item;

import com.knoxhack.echoorbitalremnants.Config;
import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.registry.ModEntities;
import com.knoxhack.echoorbitalremnants.suit.SuitEvents;
import com.knoxhack.echoorbitalremnants.world.EuropaCryoOcean;
import com.knoxhack.echoorbitalremnants.world.MarsAshBasin;
import com.knoxhack.echoorbitalremnants.world.ModDimensions;
import com.knoxhack.echoorbitalremnants.world.SaturnRingGraveyard;
import com.knoxhack.echoorbitalremnants.world.TitanMethaneShelf;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class PlanetaryRouteItem extends Item {
    private final Target target;

    public PlanetaryRouteItem(Target target, Properties properties) {
        super(properties);
        this.target = target;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            EchoTerminalProgress progress = EchoTerminalProgress.get(player);
            if (SuitEvents.isOrbitalExposure(player) && progress.hasReturnPoint() && player instanceof ServerPlayer serverPlayer
                    && player.isShiftKeyDown()) {
                ServerLevel returnLevel = serverPlayer.level().getServer().getLevel(ModDimensions.keyFromString(progress.returnDimension()));
                if (returnLevel == null) {
                    returnLevel = serverPlayer.level().getServer().overworld();
                }
                playRouteFeedback(serverPlayer.level(), player.blockPosition(), ParticleTypes.CLOUD, 12, 1.35F);
                serverPlayer.teleportTo(returnLevel, progress.returnX(), progress.returnY(), progress.returnZ(), Set.of(), player.getYRot(), player.getXRot(), false);
                playRouteFeedback(returnLevel, BlockPos.containing(progress.returnX(), progress.returnY(), progress.returnZ()), ParticleTypes.CLOUD, 10, 1.6F);
                sendFeedback(player, target.displayName + " return vector burned.");
                return InteractionResult.SUCCESS_SERVER;
            }
            if (SuitEvents.isOrbitalExposure(player) && player.isShiftKeyDown() && !progress.hasReturnPoint()) {
                sendFeedback(player, target.displayName + " return denied. No docking vector is saved; mark one before drifting farther.");
                return InteractionResult.CONSUME;
            }

            if (Config.DIMENSION_UNLOCKS_ENABLED.get() && !target.unlocked(progress) && !player.hasInfiniteMaterials()) {
                sendFeedback(player, target.displayName + " route locked. " + target.unlockHint());
                return InteractionResult.CONSUME;
            }
            if (Config.MID_GAME_OBJECTIVES_ENABLED.get() && !target.midGameReady(progress) && !player.hasInfiniteMaterials()) {
                sendFeedback(player, target.displayName + " route locked. " + target.midGameLockMessage());
                return InteractionResult.CONSUME;
            }
            if (!SuitEvents.isOrbitalExposure(player) && !player.hasInfiniteMaterials()) {
                sendFeedback(player, target.displayName + " requires orbital staging. Launch to Low Earth Orbit before burning this route.");
                return InteractionResult.CONSUME;
            }

            if (player instanceof ServerPlayer serverPlayer && serverPlayer.level() instanceof ServerLevel serverLevel) {
                progress.setReturnPoint(player);
                ServerLevel targetLevel = ModDimensions.resolve(serverPlayer.level().getServer(), target.dimension, serverLevel);
                double targetX = targetLevel.dimension() == target.dimension ? 0.0D : player.getX() + target.fallbackX;
                double targetY = targetLevel.dimension() == target.dimension ? 96.0D : Config.ORBITAL_ALTITUDE.get();
                double targetZ = targetLevel.dimension() == target.dimension ? 0.0D : player.getZ() + target.fallbackZ;
                BlockPos arrival = BlockPos.containing(targetX, targetY, targetZ);
                if (progress.markRouteArrivalSeeded(player, target.seedToken())) {
                    target.seed(targetLevel, arrival);
                    target.spawnThreats(targetLevel, arrival);
                }
                target.mark(progress, player);
                playRouteFeedback(serverLevel, player.blockPosition(), ParticleTypes.PORTAL, 28, 0.75F);
                serverPlayer.teleportTo(targetLevel, targetX, targetY, targetZ, Set.of(), player.getYRot(), player.getXRot(), false);
                playRouteFeedback(targetLevel, arrival, target.particle(), 36, target.pitch());
                sendFeedback(player, target.displayName + " route acquired. " + target.arrivalMessage
                        + " Return vector saved.", target.displayName + " route burn complete. Return vector saved.");
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

    private static void playRouteFeedback(ServerLevel level, BlockPos pos, net.minecraft.core.particles.ParticleOptions particle, int count, float pitch) {
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7F, pitch);
        level.sendParticles(particle, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, count, 0.55D, 0.65D, 0.55D, 0.04D);
    }

    public enum Target {
        MARS("Mars Ash Basin", ModDimensions.MARS_ASH_BASIN, 2048.0D, -256.0D,
                "Terraforming towers are dead, but the dust still answers.") {
            @Override
            boolean unlocked(EchoTerminalProgress progress) {
                return progress.marsRouteUnlocked();
            }

            @Override
            boolean midGameReady(EchoTerminalProgress progress) {
                return progress.lunarExtractorGateOpen();
            }

            @Override
            String midGameLockMessage() {
                return "Restore three Helium Extractor Nodes before Mars transfer stops guessing.";
            }

            @Override
            void seed(ServerLevel level, BlockPos arrival) {
                MarsAshBasin.seedLandingSite(level, arrival);
            }

            @Override
            void spawnThreats(ServerLevel level, BlockPos arrival) {
                spawn(level, arrival.offset(6, 1, -7), ModEntities.ABANDONED_CAPTAIN.get());
                spawn(level, arrival.offset(-5, 1, 6), ModEntities.BROKEN_ASTRONAUT.get());
                spawn(level, arrival.offset(7, 1, 5), ModEntities.NEXUS_HUSK.get());
            }

            @Override
            void mark(EchoTerminalProgress progress, Player player) {
                progress.markMarsAshBasinVisited(player);
            }
        },
        EUROPA("Europa Cryo Ocean", ModDimensions.EUROPA_CRYO_OCEAN, -1536.0D, 640.0D,
                "Sub-ice labs detected below the fracture line.") {
            @Override
            boolean unlocked(EchoTerminalProgress progress) {
                return progress.europaRouteUnlocked();
            }

            @Override
            boolean midGameReady(EchoTerminalProgress progress) {
                return progress.marsHabitatGateOpen();
            }

            @Override
            String midGameLockMessage() {
                return "Repair three Mars Pressure Consoles before Europa prep stops bleeding pressure.";
            }

            @Override
            void seed(ServerLevel level, BlockPos arrival) {
                EuropaCryoOcean.seedLandingSite(level, arrival);
            }

            @Override
            void spawnThreats(ServerLevel level, BlockPos arrival) {
                spawn(level, arrival.offset(-6, 1, -5), ModEntities.VACUUM_WRAITH.get());
                spawn(level, arrival.offset(6, 1, 5), ModEntities.NEXUS_HUSK.get());
                spawn(level, arrival.offset(0, 1, 9), ModEntities.ECHO_DEFENSE_DRONE.get());
                spawn(level, arrival.offset(2, 2, -9), ModEntities.EUROPA_CRYO_WARDEN.get());
            }

            @Override
            void mark(EchoTerminalProgress progress, Player player) {
                progress.markEuropaCryoOceanVisited(player);
            }
        },
        SATURN("Saturn Ring Graveyard", ModDimensions.SATURN_RING_GRAVEYARD, -2240.0D, -768.0D,
                "Old relay ribs are tumbling through the ring plane.") {
            @Override
            boolean unlocked(EchoTerminalProgress progress) {
                return progress.saturnRouteUnlocked();
            }

            @Override
            boolean midGameReady(EchoTerminalProgress progress) {
                return progress.europaArrayGateOpen();
            }

            @Override
            String midGameLockMessage() {
                return "Calibrate three Europa Thermal Arrays before the Saturn transfer geometry stops shearing.";
            }

            @Override
            void seed(ServerLevel level, BlockPos arrival) {
                SaturnRingGraveyard.seedLandingSite(level, arrival);
            }

            @Override
            void spawnThreats(ServerLevel level, BlockPos arrival) {
                spawn(level, arrival.offset(-7, 1, 4), ModEntities.ECHO_DEFENSE_DRONE.get());
                spawn(level, arrival.offset(6, 1, -5), ModEntities.VACUUM_WRAITH.get());
                spawn(level, arrival.offset(2, 1, 8), ModEntities.SATURN_RELAY_SENTINEL.get());
            }

            @Override
            void mark(EchoTerminalProgress progress, Player player) {
                progress.markSaturnRingGraveyardVisited(player);
            }
        },
        TITAN("Titan Methane Shelf", ModDimensions.TITAN_METHANE_SHELF, 2560.0D, -1024.0D,
                "Methane shelf pressure is barely civil, and the survey dome is still blinking.") {
            @Override
            boolean unlocked(EchoTerminalProgress progress) {
                return progress.titanRouteUnlocked();
            }

            @Override
            boolean midGameReady(EchoTerminalProgress progress) {
                return progress.saturnRelayGateOpen();
            }

            @Override
            String midGameLockMessage() {
                return "Restore three Saturn Ring Relays before Titan descent can keep a return vector.";
            }

            @Override
            void seed(ServerLevel level, BlockPos arrival) {
                TitanMethaneShelf.seedLandingSite(level, arrival);
            }

            @Override
            void spawnThreats(ServerLevel level, BlockPos arrival) {
                spawn(level, arrival.offset(-5, 1, -7), ModEntities.TITAN_METHANE_STALKER.get());
                spawn(level, arrival.offset(6, 1, 5), ModEntities.NEXUS_HUSK.get());
                spawn(level, arrival.offset(0, 1, 9), ModEntities.VACUUM_WRAITH.get());
            }

            @Override
            void mark(EchoTerminalProgress progress, Player player) {
                progress.markTitanMethaneShelfVisited(player);
            }
        };

        private final String displayName;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final double fallbackX;
        private final double fallbackZ;
        private final String arrivalMessage;

        Target(String displayName, net.minecraft.resources.ResourceKey<Level> dimension, double fallbackX, double fallbackZ, String arrivalMessage) {
            this.displayName = displayName;
            this.dimension = dimension;
            this.fallbackX = fallbackX;
            this.fallbackZ = fallbackZ;
            this.arrivalMessage = arrivalMessage;
        }

        abstract boolean unlocked(EchoTerminalProgress progress);

        String unlockHint() {
            return switch (this) {
                case MARS -> "Scan a Helium-3 Cell in the Lunar Scar Zone to resolve Mars transfer.";
                case EUROPA -> "Scan Martian Silica in the Mars Ash Basin to resolve Europa transfer.";
                case SATURN -> "Scan a Cryo Crystal in Europa, or finish Europa survey proof, to resolve Saturn transfer.";
                case TITAN -> "Scan a Saturn Ring Fragment in the ring graveyard to resolve Titan descent.";
            };
        }

        abstract boolean midGameReady(EchoTerminalProgress progress);

        abstract String midGameLockMessage();

        abstract void seed(ServerLevel level, BlockPos arrival);

        abstract void spawnThreats(ServerLevel level, BlockPos arrival);

        abstract void mark(EchoTerminalProgress progress, Player player);

        String seedToken() {
            return dimension.identifier().getPath();
        }

        net.minecraft.core.particles.ParticleOptions particle() {
            return switch (this) {
                case MARS -> ParticleTypes.SMOKE;
                case EUROPA -> ParticleTypes.SNOWFLAKE;
                case SATURN -> ParticleTypes.CLOUD;
                case TITAN -> ParticleTypes.LARGE_SMOKE;
            };
        }

        float pitch() {
            return switch (this) {
                case MARS -> 0.95F;
                case EUROPA -> 1.25F;
                case SATURN -> 1.45F;
                case TITAN -> 0.82F;
            };
        }

        private static void spawn(ServerLevel level, BlockPos pos, net.minecraft.world.entity.EntityType<?> type) {
            Entity entity = type.create(level, EntitySpawnReason.EVENT);
            if (entity != null) {
                entity.setPos(pos.getX(), pos.getY(), pos.getZ());
                level.addFreshEntity(entity);
            }
        }
    }
}
