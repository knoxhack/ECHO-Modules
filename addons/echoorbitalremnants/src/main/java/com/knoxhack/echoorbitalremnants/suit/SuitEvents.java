package com.knoxhack.echoorbitalremnants.suit;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.echoplatform.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echoorbitalremnants.Config;
import com.knoxhack.echoorbitalremnants.network.OrbitalEventVisualPayload;
import com.knoxhack.echoorbitalremnants.progression.OrbitalEventType;
import com.knoxhack.echoorbitalremnants.progression.EchoTerminalProgress;
import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModEntities;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import com.knoxhack.echoorbitalremnants.world.ModDimensions;
import com.knoxhack.echoorbitalremnants.world.OrbitalDebrisField;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SuitEvents {
    private static final Map<UUID, BlockScanCache> BLOCK_SCAN_CACHE = new HashMap<>();
    private static final int MAX_LOCAL_AMBIENT_THREATS = 6;

    public void onPlayerTick(Object event) {
        Player player = EchoBackendWorldEventBridge.postTickPlayer(event);
        if (player == null) {
            return;
        }
        boolean exposed = isOrbitalExposure(player);
        SuitState state = SuitState.get(player);
        if (!player.level().isClientSide()) {
            state.syncStationPower(EchoTerminalProgress.get(player), hasItem(player, ModItems.STATION_POWER_MATRIX.get()));
        }
        boolean fullSuit = SuitState.hasFullPressureSuit(player);
        boolean magneticBoots = SuitState.hasMagneticBoots(player);
        boolean oxygenBooster = SuitState.hasOxygenBooster(player);
        state.updateEnvironment(fullSuit, magneticBoots, exposed);

        if (exposed && player.tickCount % Config.tunedSurvivalInterval(Config.OXYGEN_DRAIN_TICKS) == 0) {
            state.tickVacuum(player, fullSuit, magneticBoots, true, oxygenBooster);
            tryAutoRecovery(player, state);
        } else if (!exposed && player.tickCount % 80 == 0) {
            state.tickVacuum(player, fullSuit, magneticBoots, false, oxygenBooster);
        }

        if (exposed && player.tickCount % Config.tunedSurvivalInterval(Config.RADIATION_GAIN_TICKS) == 0) {
            state.addRadiation(SuitState.hasRadiationVisor(player));
        }

        if (exposed && !player.level().isClientSide() && player.tickCount % 100 == 0) {
            applyDimensionHazards(player, state);
        }

        if (exposed
                && player.level().dimension() == ModDimensions.EUROPA_CRYO_OCEAN
                && !SuitState.hasThermalLiner(player)
                && player.tickCount % 100 == 0) {
            state.compromisePressure(3);
        }

        if (exposed && !magneticBoots) {
            Vec3 motion = player.getDeltaMovement();
            if (motion.y < -0.08D) {
                player.setDeltaMovement(motion.x, -0.08D, motion.z);
                player.fallDistance = 0.0F;
            }
        }

        if (!player.level().isClientSide() && exposed) {
            handleServerOrbitalEffects(player, state);
            if (player.tickCount % 160 == 0) {
                pulseNearbyRouteObjective(player);
            }
            if (player.tickCount % 3600 == 0 && player.level() instanceof ServerLevel serverLevel) {
                OrbitalDebrisField.seedAmbientDebris(serverLevel, player.blockPosition());
            }
        }

        state.save(player);
    }

    public void onRouteCacheOpen(Object event) {
        ServerPlayer player = EchoBackendWorldEventBridge.rightClickBlockServerPlayer(event);
        BlockPos pos = EchoBackendWorldEventBridge.rightClickBlockPos(event);
        if (player == null || pos == null || !ModDimensions.isSpaceLevel(player.level())) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (blockEntity == null || player.level().getBlockState(pos).getBlock() != Blocks.CHEST) {
            return;
        }
        if (!nearbyBlock(player, 3,
                ModBlocks.DOCKING_BEACON.get(),
                ModBlocks.LUNAR_TITANIUM_BLOCK.get(),
                ModBlocks.OXYGEN_PIPE.get(),
                ModBlocks.FROZEN_CABLE.get(),
                ModBlocks.SATURN_RING_RELAY.get(),
                ModBlocks.METHANE_ICE.get(),
                ModBlocks.NEXUS_DUST_BLOCK.get())) {
            return;
        }
        long now = player.level().getGameTime();
        long last = player.getPersistentData().getLongOr("echoorbitalremnants_route_cache_feedback_tick", -200L);
        long lastPos = player.getPersistentData().getLongOr("echoorbitalremnants_route_cache_feedback_pos", Long.MIN_VALUE);
        if (lastPos == pos.asLong() && now - last < 80L) {
            return;
        }
        player.getPersistentData().putLong("echoorbitalremnants_route_cache_feedback_tick", now);
        player.getPersistentData().putLong("echoorbitalremnants_route_cache_feedback_pos", pos.asLong());
        player.sendSystemMessage(Component.literal("ECHO-7 // Recovery cache opened: route proof, crafting support, and survival recovery stock."));
        player.level().playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.55F, 1.25F);
    }

    public void onClone(Object event) {
        Player player = EchoBackendWorldEventBridge.cloneNewPlayer(event);
        Player original = EchoBackendWorldEventBridge.cloneOriginalPlayer(event);
        if (player == null || original == null) {
            return;
        }
        player.getPersistentData().put("echoorbitalremnants_suit",
                original.getPersistentData().getCompoundOrEmpty("echoorbitalremnants_suit").copy());
        player.getPersistentData().put("echoorbitalremnants_progress",
                original.getPersistentData().getCompoundOrEmpty("echoorbitalremnants_progress").copy());
    }

    public static boolean isOrbitalExposure(Player player) {
        return ModDimensions.isSpaceLevel(player.level()) || player.getY() >= Config.ORBITAL_ALTITUDE.get() - 32;
    }

    public static boolean isOrbitalProgressionScan(Player player) {
        if (ModDimensions.isSpaceLevel(player.level())) {
            return true;
        }
        if (player.getY() < Config.ORBITAL_ALTITUDE.get() - 32) {
            return false;
        }
        return player.hasInfiniteMaterials() || EchoTerminalProgress.get(player).lowOrbitReached();
    }

    private static void tryAutoRecovery(Player player, SuitState state) {
        if (state.oxygen() <= 20 && state.backupAirAvailable() && consume(player, ModItems.EMERGENCY_OXYGEN_CELL.get())) {
            state.useEmergencyOxygen();
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("ECHO-7 // Backup air cartridge engaged. Breathe, then move."));
        }

        if (state.pressure() <= 25 && state.autoSealAvailable() && consume(player, ModItems.SUIT_SEALANT_PATCH.get())) {
            state.applySealantPatch();
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("ECHO-7 // Auto-seal patch applied. Pressure recovering; do not test it."));
        }
    }

    private static void handleServerOrbitalEffects(Player player, SuitState state) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (player.tickCount % 100 == 0) {
            applyStationPowerSupport(player, state);
        }

        if (state.critical()
                && player.tickCount % Config.tunedSurvivalInterval(Config.VACUUM_DAMAGE_TICKS) == 0
                && !player.hasInfiniteMaterials()) {
            player.hurtServer(serverLevel, player.damageSources().drown(), 2.0F);
        }

        int frequency = Config.tunedOrbitalEventFrequency();
        if (frequency > 0 && player.tickCount % frequency == 0) {
            OrbitalEventType[] events = OrbitalEventType.values();
            OrbitalEventType event = events[player.getRandom().nextInt(events.length)];
            player.sendSystemMessage(event.diagnosticMessage());
            sendOrbitalEventVisual(player, event);
            applyEventPressure(player, state, event);
            spawnOrbitalThreat(serverLevel, player, event);
        }

        if (Config.FEATURE_THREATS_ENABLED.get()
                && player.tickCount % 600 == 0
                && isFeatureThreatZone(player)
                && player.getRandom().nextInt(100) < Config.DEEP_SITE_THREAT_CHANCE.get()) {
            spawnFeatureThreat(serverLevel, player);
        }
    }

    private static void applyStationPowerSupport(Player player, SuitState state) {
        if (player.level().dimension() != ModDimensions.LOW_EARTH_ORBIT
                || state.stationPower() < 46
                || !nearStationPowerInfrastructure(player)) {
            return;
        }

        int power = state.stationPower();
        int oxygenRecovery = power >= 82 ? 2 : 1;
        int pressureRecovery = power >= 70 ? 2 : 1;
        int radiationRecovery = power >= 82 ? 1 : 0;
        state.stabilizeFromStationPower(oxygenRecovery, pressureRecovery, radiationRecovery);
    }

    private static void applyDimensionHazards(Player player, SuitState state) {
        EchoTerminalProgress progress = EchoTerminalProgress.get(player);
        if (player.level().dimension() == ModDimensions.LOW_EARTH_ORBIT && !progress.orbitSurveyComplete()
                && nearbyBlock(player, 7, ModBlocks.BROKEN_SOLAR_PANEL.get(), ModBlocks.ORBITAL_PLATING.get(), ModBlocks.SATELLITE_PLATING.get(), ModBlocks.STATION_RELAY_NODE.get())) {
            state.drainOxygen(Config.tunedHazardDrain(nearbyBlock(player, 5, ModBlocks.DOCKING_BEACON.get()) ? 3 : 2));
            state.compromisePressure(Config.tunedHazardDrain(2));
        }
        if (player.level().dimension() == ModDimensions.LUNAR_SCAR_ZONE && !progress.moonSurveyComplete()
                && nearbyBlock(player, 7, ModBlocks.NEXUS_TOUCHED_STONE.get(), ModBlocks.NEXUS_GROWTH.get(), ModBlocks.NEXUS_DUST_BLOCK.get(), ModBlocks.SURVEY_MARKER.get(), ModBlocks.HELIUM_EXTRACTOR_NODE.get())) {
            for (int i = 0; i < Config.tunedHazardDrain(1); i++) {
                state.addRadiation(SuitState.hasRadiationVisor(player));
            }
        }
        if (player.level().dimension() == ModDimensions.MARS_ASH_BASIN && !progress.marsSurveyComplete()
                && !hasItem(player, ModItems.MARTIAN_PRESSURE_VALVE.get()) && !hasItem(player, ModItems.PRESSURE_REGULATOR.get())
                && !nearbyBlock(player, 6, ModBlocks.SIGNAL_RELAY.get(), ModBlocks.OXYGEN_PIPE.get(), ModBlocks.MARS_PRESSURE_CONSOLE.get())) {
            state.compromisePressure(Config.tunedHazardDrain(nearbyBlock(player, 7, ModBlocks.MARTIAN_DUST.get(), ModBlocks.MARTIAN_BASALT.get()) ? 3 : 2));
        }
        if (player.level().dimension() == ModDimensions.EUROPA_CRYO_OCEAN && !progress.europaSurveyComplete()
                && !nearbyBlock(player, 6, ModBlocks.THERMAL_VENT.get(), ModBlocks.EUROPA_THERMAL_ARRAY.get())) {
            state.drainOxygen(Config.tunedHazardDrain(1));
            if (!SuitState.hasThermalLiner(player)) {
                state.compromisePressure(Config.tunedHazardDrain(nearbyBlock(player, 7, ModBlocks.CRYO_ICE.get(), ModBlocks.PACKED_CRYO_ICE.get(), ModBlocks.FROZEN_CABLE.get()) ? 2 : 1));
            }
        }
        if (player.level().dimension() == ModDimensions.SATURN_RING_GRAVEYARD && !progress.saturnSurveyComplete()
                && nearbyBlock(player, 7, ModBlocks.SATURN_ICE_RUBBLE.get(), ModBlocks.SATURN_RING_RELAY.get(), ModBlocks.ORBITAL_PLATING.get())) {
            state.drainOxygen(Config.tunedHazardDrain(1));
            if (!nearbyBlock(player, 5, ModBlocks.FACTION_RELAY_HUB.get(), ModBlocks.FACTION_VENDOR_KIOSK.get())) {
                state.compromisePressure(Config.tunedHazardDrain(1));
            }
        }
        if (player.level().dimension() == ModDimensions.TITAN_METHANE_SHELF && !progress.titanSurveyComplete()
                && !nearbyBlock(player, 6, ModBlocks.TITAN_METHANE_PUMP.get(), ModBlocks.FACTION_RELAY_HUB.get())) {
            state.compromisePressure(Config.tunedHazardDrain(2));
            if (!SuitState.hasThermalLiner(player)) {
                state.drainOxygen(Config.tunedHazardDrain(1));
            }
        }
        if (player.level().dimension() == ModDimensions.NEXUS_ANOMALY_BELT && !progress.nexusStabilized()
                && nearbyBlock(player, 8, ModBlocks.NEXUS_ANCHOR.get(), ModBlocks.NEXUS_GROWTH.get(), ModBlocks.NEXUS_TOUCHED_STONE.get())) {
            for (int i = 0; i < Config.tunedHazardDrain(1); i++) {
                state.addRadiation(false);
            }
            if (progress.echoZeroEncountered()) {
                state.compromisePressure(Config.tunedHazardDrain(1));
            }
        }
    }

    private static void applyEventPressure(Player player, SuitState state, OrbitalEventType event) {
        switch (event) {
            case DEBRIS_STORM -> state.compromisePressure(Config.tunedHazardDrain(4));
            case SOLAR_FLARE -> {
                for (int i = 0; i < Config.tunedHazardDrain(2); i++) {
                    state.addRadiation(SuitState.hasRadiationVisor(player));
                }
            }
            case STATION_BLACKOUT -> state.drainOxygen(stationBlackoutDrain(player, state));
            case NEXUS_PULSE -> {
                for (int i = 0; i < Config.tunedHazardDrain(1); i++) {
                    state.addRadiation(false);
                }
                state.compromisePressure(Config.tunedHazardDrain(3));
            }
        }
    }

    private static int stationBlackoutDrain(Player player, SuitState state) {
        int drain = Config.tunedHazardDrain(5);
        if (player.level().dimension() != ModDimensions.LOW_EARTH_ORBIT
                || state.stationPower() < 70
                || !nearStationPowerInfrastructure(player)) {
            return drain;
        }

        int reduction = state.stationPower() >= 95 ? 3 : 2;
        return Math.max(1, drain - reduction);
    }

    private static void sendOrbitalEventVisual(Player player, OrbitalEventType event) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        int overlay = switch (event) {
            case DEBRIS_STORM -> 0x665A5550;
            case SOLAR_FLARE -> 0x66FFD166;
            case STATION_BLACKOUT -> 0x99000000;
            case NEXUS_PULSE -> 0x664D28FF;
        };
        int particle = switch (event) {
            case DEBRIS_STORM -> 0xFFB7B0A0;
            case SOLAR_FLARE -> 0xFFFFF0A0;
            case STATION_BLACKOUT -> 0xFF66E8FF;
            case NEXUS_PULSE -> 0xFFE09CFF;
        };
        EchoNetSend.toPlayer(serverPlayer, new OrbitalEventVisualPayload(
                event.name(),
                overlay,
                particle,
                1.0F,
                player.getRandom().nextLong()), EchoPacketKind.CLIENTBOUND_SYNC);
    }

    private static void spawnOrbitalThreat(ServerLevel level, Player player, OrbitalEventType event) {
        if (localThreatCapReached(level, player)) {
            return;
        }
        Entity entity = switch (event) {
            case DEBRIS_STORM, SOLAR_FLARE -> ModEntities.ECHO_DEFENSE_DRONE.get().create(level, EntitySpawnReason.EVENT);
            case STATION_BLACKOUT -> ModEntities.VACUUM_WRAITH.get().create(level, EntitySpawnReason.EVENT);
            case NEXUS_PULSE -> ModEntities.BROKEN_ASTRONAUT.get().create(level, EntitySpawnReason.EVENT);
        };
        if (entity == null) {
            return;
        }
        double dx = player.getRandom().nextInt(11) - 5;
        double dz = player.getRandom().nextInt(11) - 5;
        entity.setPos(player.getX() + dx, player.getY() + 1.0D, player.getZ() + dz);
        entity.setYRot(player.getYRot());
        level.addFreshEntity(entity);
    }

    private static void spawnFeatureThreat(ServerLevel level, Player player) {
        if (localThreatCapReached(level, player)) {
            return;
        }
        Entity entity;
        if (player.level().dimension() == ModDimensions.SATURN_RING_GRAVEYARD) {
            entity = ModEntities.SATURN_RELAY_SENTINEL.get().create(level, EntitySpawnReason.EVENT);
        } else if (player.level().dimension() == ModDimensions.TITAN_METHANE_SHELF) {
            entity = ModEntities.TITAN_METHANE_STALKER.get().create(level, EntitySpawnReason.EVENT);
        } else if (player.level().dimension() == ModDimensions.LOW_EARTH_ORBIT || player.level().dimension() == ModDimensions.EUROPA_CRYO_OCEAN) {
            entity = player.level().dimension() == ModDimensions.EUROPA_CRYO_OCEAN && player.getRandom().nextInt(4) == 0
                    ? ModEntities.EUROPA_CRYO_WARDEN.get().create(level, EntitySpawnReason.EVENT)
                    : ModEntities.ECHO_DEFENSE_DRONE.get().create(level, EntitySpawnReason.EVENT);
        } else if (player.level().dimension() == ModDimensions.MARS_ASH_BASIN) {
            entity = ModEntities.BROKEN_ASTRONAUT.get().create(level, EntitySpawnReason.EVENT);
        } else if (player.level().dimension() == ModDimensions.LUNAR_SCAR_ZONE || player.level().dimension() == ModDimensions.NEXUS_ANOMALY_BELT) {
            entity = ModEntities.NEXUS_HUSK.get().create(level, EntitySpawnReason.EVENT);
        } else {
            entity = ModEntities.VACUUM_WRAITH.get().create(level, EntitySpawnReason.EVENT);
        }
        if (entity == null) {
            return;
        }
        double dx = player.getRandom().nextInt(9) - 4;
        double dz = player.getRandom().nextInt(9) - 4;
        entity.setPos(player.getX() + dx, player.getY() + 1.0D, player.getZ() + dz);
        level.addFreshEntity(entity);
    }

    public static boolean localThreatCapReached(ServerLevel level, Player player) {
        AABB area = new AABB(player.getX() - 18.0D, player.getY() - 8.0D, player.getZ() - 18.0D,
                player.getX() + 18.0D, player.getY() + 8.0D, player.getZ() + 18.0D);
        return level.getEntities((Entity) null, area, SuitEvents::isOrbitalThreat).size() >= MAX_LOCAL_AMBIENT_THREATS;
    }

    private static boolean isOrbitalThreat(Entity entity) {
        return entity.getType() == ModEntities.ECHO_DEFENSE_DRONE.get()
                || entity.getType() == ModEntities.VACUUM_WRAITH.get()
                || entity.getType() == ModEntities.BROKEN_ASTRONAUT.get()
                || entity.getType() == ModEntities.NEXUS_HUSK.get()
                || entity.getType() == ModEntities.LUNAR_NEXUS_HUSK.get()
                || entity.getType() == ModEntities.EUROPA_CRYO_WARDEN.get()
                || entity.getType() == ModEntities.SATURN_RELAY_SENTINEL.get()
                || entity.getType() == ModEntities.TITAN_METHANE_STALKER.get();
    }

    public static boolean isFeatureThreatZone(Player player) {
        return nearbyBlock(player, 8,
                ModBlocks.BROKEN_SOLAR_PANEL.get(),
                ModBlocks.DOCKING_BEACON.get(),
                ModBlocks.STATION_RELAY_NODE.get(),
                ModBlocks.NEXUS_TOUCHED_STONE.get(),
                ModBlocks.HELIUM_EXTRACTOR_NODE.get(),
                ModBlocks.MARTIAN_DUST.get(),
                ModBlocks.MARTIAN_BASALT.get(),
                ModBlocks.MARS_PRESSURE_CONSOLE.get(),
                ModBlocks.FROZEN_CABLE.get(),
                ModBlocks.EUROPA_THERMAL_ARRAY.get(),
                ModBlocks.SATURN_ICE_RUBBLE.get(),
                ModBlocks.SATURN_RING_RELAY.get(),
                ModBlocks.TITAN_THOLIN_DUST.get(),
                ModBlocks.TITAN_METHANE_PUMP.get(),
                ModBlocks.NEXUS_GROWTH.get(),
                ModBlocks.NEXUS_ANCHOR.get());
    }

    private static void pulseNearbyRouteObjective(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockPos objective = nearbyBlockPos(player, 10,
                ModBlocks.STATION_RELAY_NODE.get(),
                ModBlocks.HELIUM_EXTRACTOR_NODE.get(),
                ModBlocks.MARS_PRESSURE_CONSOLE.get(),
                ModBlocks.EUROPA_THERMAL_ARRAY.get(),
                ModBlocks.SATURN_RING_RELAY.get(),
                ModBlocks.TITAN_METHANE_PUMP.get(),
                ModBlocks.NEXUS_ANCHOR.get());
        if (objective == null) {
            return;
        }
        serverLevel.playSound(null, objective, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.28F, routePulsePitch(player));
        serverLevel.sendParticles(routePulseParticle(player), objective.getX() + 0.5D, objective.getY() + 1.0D, objective.getZ() + 0.5D,
                8, 0.35D, 0.28D, 0.35D, 0.01D);
    }

    private static net.minecraft.core.particles.ParticleOptions routePulseParticle(Player player) {
        if (player.level().dimension() == ModDimensions.EUROPA_CRYO_OCEAN) {
            return ParticleTypes.SNOWFLAKE;
        }
        if (player.level().dimension() == ModDimensions.TITAN_METHANE_SHELF) {
            return ParticleTypes.LARGE_SMOKE;
        }
        if (player.level().dimension() == ModDimensions.NEXUS_ANOMALY_BELT) {
            return ParticleTypes.REVERSE_PORTAL;
        }
        return ParticleTypes.ELECTRIC_SPARK;
    }

    private static float routePulsePitch(Player player) {
        if (player.level().dimension() == ModDimensions.SATURN_RING_GRAVEYARD) {
            return 1.75F;
        }
        if (player.level().dimension() == ModDimensions.TITAN_METHANE_SHELF) {
            return 0.75F;
        }
        if (player.level().dimension() == ModDimensions.NEXUS_ANOMALY_BELT) {
            return 1.95F;
        }
        return 1.35F;
    }

    private static boolean consume(Player player, net.minecraft.world.item.Item item) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == item) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static boolean hasItem(Player player, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    private static boolean nearStationPowerInfrastructure(Player player) {
        return nearbyBlock(player, 7,
                ModBlocks.STATION_LIFE_SUPPORT_CORE.get(),
                ModBlocks.STATION_RELAY_NODE.get(),
                ModBlocks.STATION_WALL_PANEL.get(),
                ModBlocks.DOCKING_BEACON.get(),
                ModBlocks.OXYGEN_PIPE.get());
    }

    private static boolean nearbyBlock(Player player, int radius, Block... blocks) {
        String key = scanKey(player, radius, blocks);
        BlockScanCache cache = BLOCK_SCAN_CACHE.get(player.getUUID());
        if (cache != null && cache.tick + 40 >= player.tickCount && cache.key.equals(key)) {
            return cache.found;
        }
        boolean found = nearbyBlockPosUncached(player, radius, blocks) != null;
        BLOCK_SCAN_CACHE.put(player.getUUID(), new BlockScanCache(key, player.tickCount, found));
        return found;
    }

    private static BlockPos nearbyBlockPos(Player player, int radius, Block... blocks) {
        return nearbyBlockPosUncached(player, radius, blocks);
    }

    private static BlockPos nearbyBlockPosUncached(Player player, int radius, Block... blocks) {
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -3, -radius), center.offset(radius, 3, radius))) {
            Block current = player.level().getBlockState(pos).getBlock();
            for (Block block : blocks) {
                if (current == block) {
                    return pos.immutable();
                }
            }
        }
        return null;
    }

    private static String scanKey(Player player, int radius, Block... blocks) {
        BlockPos pos = player.blockPosition();
        StringBuilder builder = new StringBuilder(player.level().dimension().identifier().toString())
                .append('|').append(pos.getX() >> 2)
                .append('|').append(pos.getY() >> 2)
                .append('|').append(pos.getZ() >> 2)
                .append('|').append(radius);
        for (Block block : blocks) {
            builder.append('|').append(System.identityHashCode(block));
        }
        return builder.toString();
    }

    private record BlockScanCache(String key, int tick, boolean found) {
    }
}
