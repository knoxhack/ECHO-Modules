package com.knoxhack.echoashfallprotocol.endgame;

import com.knoxhack.echoashfallprotocol.block.NexusCoreBlock;
import com.knoxhack.echoashfallprotocol.block.entity.NexusCoreBlockEntity;
import com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreLateRuntime;
import com.knoxhack.echoashfallprotocol.event.PostNexusEventHandler;
import com.knoxhack.echoashfallprotocol.network.NexusStatePacket;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModEffects;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.registry.ModSounds;
import com.knoxhack.echoashfallprotocol.world.NexusCampaignData;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Authoritative server-side handler for the permanent, shared Nexus choice.
 */
public final class NexusChoiceService {

    private static final int CORE_SEARCH_HORIZONTAL = 16;
    private static final int CORE_SEARCH_VERTICAL = 8;

    private NexusChoiceService() {
    }

    public static NexusCoreBlockEntity.NexusChoice parseChoice(String rawChoice) {
        if (rawChoice == null) {
            return null;
        }
        return switch (rawChoice.trim().toLowerCase(Locale.ROOT)) {
            case "restore", "restored" -> NexusCoreBlockEntity.NexusChoice.RESTORE;
            case "destroy", "destroyed" -> NexusCoreBlockEntity.NexusChoice.DESTROY;
            case "control", "controlled" -> NexusCoreBlockEntity.NexusChoice.CONTROL;
            default -> null;
        };
    }

    public static boolean applyChoice(ServerPlayer player, String rawChoice) {
        return applyChoice(player, parseChoice(rawChoice));
    }

    public static boolean applyChoice(ServerPlayer player, NexusCoreBlockEntity.NexusChoice choice) {
        if (choice == null || choice == NexusCoreBlockEntity.NexusChoice.NONE) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Invalid Nexus choice. Use restore, destroy, or control."));
            return false;
        }

        if (!(player.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Nexus decisions can only be made in the Overworld."));
            return false;
        }

        NexusCoreBlockEntity nexusCore = findUnresolvedCore(level, player.blockPosition());
        NexusAccessRules.Status access = NexusAccessRules.evaluate(player, level, nexusCore);
        if (!access.allowed()) {
            player.sendSystemMessage(access.denialMessage());
            if (access.worldResolved()) {
                NexusWorldData resolvedData = NexusWorldData.get(level.getServer().overworld());
                EchoNetSend.toPlayer(player, NexusStatePacket.fromWorldData(
                        resolvedData, NexusCampaignData.get(level.getServer().overworld())), EchoPacketKind.CLIENTBOUND_SYNC);
                PostNexusEventHandler.syncPlayerToWorldChoice(player);
            }
            return false;
        }

        NexusWorldData worldData = NexusWorldData.get(level);
        nexusCore.makeChoice(choice);
        NexusWorldData.WorldState worldState = toWorldState(choice);
        worldData.setChoice(worldState, nexusCore.getBlockPos(), player.getName().getString());

        PostNexusData.NexusPath path = toPostPath(choice);
        AshfallAdapterCoreLateRuntime.endingChoice(player, path, nexusCore.getBlockPos(), "nexus_choice");
        AshfallAdapterCoreLateRuntime.nexusState(
                player,
                NexusCampaignData.get(level),
                worldData,
                worldState.name(),
                "nexus_choice");
        for (ServerPlayer onlinePlayer : level.getServer().getPlayerList().getPlayers()) {
            PostNexusEventHandler.onNexusChoiceMade(onlinePlayer, path);
        }

        syncWorldState(level, worldData);
        applyNexusOutcome(player, level, choice, nexusCore.getBlockPos());
        return true;
    }

    private static NexusCoreBlockEntity findUnresolvedCore(ServerLevel level, BlockPos playerPos) {
        for (BlockPos cursor : BlockPos.betweenClosed(
                playerPos.offset(-CORE_SEARCH_HORIZONTAL, -CORE_SEARCH_VERTICAL, -CORE_SEARCH_HORIZONTAL),
                playerPos.offset(CORE_SEARCH_HORIZONTAL, CORE_SEARCH_VERTICAL, CORE_SEARCH_HORIZONTAL))) {
            BlockState state = level.getBlockState(cursor);
            if (!state.is(ModBlocks.NEXUS_CORE.get())) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(cursor);
            if (be instanceof NexusCoreBlockEntity core && !core.hasChoiceBeenMade()) {
                return core;
            }
        }
        return null;
    }

    private static void syncWorldState(ServerLevel level, NexusWorldData worldData) {
        NexusStatePacket packet = NexusStatePacket.fromWorldData(worldData, NexusCampaignData.get(level.getServer().overworld()));
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            EchoNetSend.toPlayer(player, packet, EchoPacketKind.CLIENTBOUND_SYNC);
        }
    }

    private static NexusWorldData.WorldState toWorldState(NexusCoreBlockEntity.NexusChoice choice) {
        return switch (choice) {
            case RESTORE -> NexusWorldData.WorldState.RESTORED;
            case DESTROY -> NexusWorldData.WorldState.DESTROYED;
            case CONTROL -> NexusWorldData.WorldState.CONTROLLED;
            case NONE -> NexusWorldData.WorldState.NORMAL;
        };
    }

    private static PostNexusData.NexusPath toPostPath(NexusCoreBlockEntity.NexusChoice choice) {
        return switch (choice) {
            case RESTORE -> PostNexusData.NexusPath.RESTORE;
            case DESTROY -> PostNexusData.NexusPath.DESTROY;
            case CONTROL -> PostNexusData.NexusPath.CONTROL;
            case NONE -> PostNexusData.NexusPath.NONE;
        };
    }

    private static void applyNexusOutcome(ServerPlayer player, ServerLevel level,
                                          NexusCoreBlockEntity.NexusChoice choice, BlockPos nexusPos) {
        String broadcastMsg = switch (choice) {
            case RESTORE -> "[WORLD] The Grid has been RESTORED by " + player.getName().getString();
            case DESTROY -> "[WORLD] The Nexus Core has been DESTROYED by " + player.getName().getString();
            case CONTROL -> "[WORLD] The Nexus Core is now CONTROLLED by " + player.getName().getString();
            default -> "";
        };

        if (!broadcastMsg.isEmpty()) {
            level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(broadcastMsg), false);
        }

        switch (choice) {
            case RESTORE -> {
                player.sendSystemMessage(Component.literal("[NEXUS CORE] RESTORATION PROTOCOL INITIATED"));
                player.sendSystemMessage(Component.literal("[ECHO-7] The grid is coming back online. Order will return."));
                player.giveExperienceLevels(30);
                player.addEffect(new MobEffectInstance(net.minecraft.core.Holder.direct(ModEffects.ALLIANCE.get()), -1, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 2));
                player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 600, 1));
                spawnEchoDrones(level, nexusPos, player, 2);
            }
            case DESTROY -> {
                player.sendSystemMessage(Component.literal("[NEXUS CORE] CRITICAL OVERLOAD INITIATED"));
                player.sendSystemMessage(Component.literal("[ECHO-7] The Core is destabilizing. True freedom. True chaos."));
                player.giveExperienceLevels(30);
                ItemStack annihilator = new ItemStack(ModItems.NEXUS_ANNIHILATOR.get());
                if (!player.getInventory().add(annihilator)) {
                    player.drop(annihilator, false);
                }
                level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(20), e -> true)
                        .forEach(e -> e.hurtServer(level, e.damageSources().explosion(null, null), 100.0f));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 12000, 1));
                level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(500),
                                e -> e.getType() == ModEntities.GLOWING_GHOUL.get())
                        .forEach(e -> e.setTarget(player));
            }
            case CONTROL -> {
                player.sendSystemMessage(Component.literal("[NEXUS CORE] CONTROL TRANSFER COMPLETE"));
                player.sendSystemMessage(Component.literal("[ECHO-7] The Core is listening to you now."));
                player.giveExperienceLevels(30);
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 72000, 0));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 72000, 2));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 72000, 1));
                spawnEchoDrones(level, nexusPos, player, 1);
            }
            default -> {
            }
        }

        playFinaleEffects(level, choice, nexusPos);
        applyGlobalEffects(level, choice);
    }

    private static void playFinaleEffects(ServerLevel level, NexusCoreBlockEntity.NexusChoice choice, BlockPos nexusPos) {
        level.playSound(null, nexusPos, ModSounds.ECHO_COMPLETE.get(), SoundSource.BLOCKS, 1.2f, 0.85f);

        switch (choice) {
            case RESTORE -> {
                level.playSound(null, nexusPos, ModSounds.ECHO_MESSAGE.get(), SoundSource.BLOCKS, 1.0f, 1.25f);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        nexusPos.getX() + 0.5D, nexusPos.getY() + 1.5D, nexusPos.getZ() + 0.5D,
                        120, 4.0D, 2.0D, 4.0D, 0.08D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        nexusPos.getX() + 0.5D, nexusPos.getY() + 1.2D, nexusPos.getZ() + 0.5D,
                        80, 3.0D, 1.5D, 3.0D, 0.12D);
                sendFinaleTitle(level, "RESTORE", "The grid breathes again.");
            }
            case DESTROY -> {
                level.sendParticles(ParticleTypes.EXPLOSION,
                        nexusPos.getX() + 0.5D, nexusPos.getY() + 1.5D, nexusPos.getZ() + 0.5D,
                        12, 4.0D, 2.0D, 4.0D, 0.0D);
                level.sendParticles(ParticleTypes.SMOKE,
                        nexusPos.getX() + 0.5D, nexusPos.getY() + 1.5D, nexusPos.getZ() + 0.5D,
                        160, 5.0D, 2.0D, 5.0D, 0.08D);
                sendFinaleTitle(level, "DESTROY", "The old command signal is gone.");
            }
            case CONTROL -> {
                level.playSound(null, nexusPos, ModSounds.ECHO_MESSAGE.get(), SoundSource.BLOCKS, 1.0f, 0.65f);
                level.sendParticles(ParticleTypes.PORTAL,
                        nexusPos.getX() + 0.5D, nexusPos.getY() + 1.5D, nexusPos.getZ() + 0.5D,
                        180, 4.0D, 2.0D, 4.0D, 0.16D);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        nexusPos.getX() + 0.5D, nexusPos.getY() + 1.2D, nexusPos.getZ() + 0.5D,
                        80, 3.0D, 1.5D, 3.0D, 0.12D);
                sendFinaleTitle(level, "CONTROL", "Every beacon answers.");
            }
            default -> {
            }
        }
    }

    private static void sendFinaleTitle(ServerLevel level, String title, String subtitle) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
        }
    }

    private static void applyGlobalEffects(ServerLevel level, NexusCoreBlockEntity.NexusChoice choice) {
        for (ServerPlayer allPlayer : level.getServer().getPlayerList().getPlayers()) {
            switch (choice) {
                case RESTORE -> allPlayer.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 1200, 0));
                case DESTROY -> allPlayer.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
                case CONTROL -> allPlayer.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0));
                default -> {
                }
            }
        }
    }

    private static void spawnEchoDrones(ServerLevel level, BlockPos nexusPos, ServerPlayer owner, int count) {
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            int offsetX = (int) (Math.cos(angle) * 5);
            int offsetZ = (int) (Math.sin(angle) * 5);
            BlockPos spawnPos = nexusPos.offset(offsetX, 2, offsetZ);

            EchoCompanionDrone entity = ModEntities.ECHO_COMPANION_DRONE.get().create(level, EntitySpawnReason.EVENT);
            if (entity != null) {
                entity.setOwner(owner);
                entity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                level.addFreshEntity(entity);
            }
        }
    }
}
