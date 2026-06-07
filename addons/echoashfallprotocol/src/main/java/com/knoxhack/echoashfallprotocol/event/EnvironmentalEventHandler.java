package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.gameplay.DifficultyProfile;
import com.knoxhack.echoashfallprotocol.gameplay.MachineGameplayHelper;
import com.knoxhack.echoashfallprotocol.gameplay.RadiationHelper;
import com.knoxhack.echoashfallprotocol.integration.AshfallDiscoveryProvider;
import com.knoxhack.echoashfallprotocol.network.EnvironmentalSyncPacket;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModSounds;
import com.knoxhack.echoashfallprotocol.survival.ColdData;
import com.knoxhack.echoashfallprotocol.survival.HazardZoneManager;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Environmental Event System - triggers weather and hazard events.
 * RADIATION_STORM: Rad spikes in radiation zones
 * TOXIC_STORM: Acid rain expedition weather
 * BLACKOUT: Power failures requiring repair
 * ASH_STORM: Visibility/filter/hydration pressure
 * CRYO_FRONT: Shelter-sensitive cold front
 * NEXUS_SURGE: Post-Nexus anomaly pressure
 *
 * Uses persistent EnvironmentalEventData (saved to world data folder).
 */
public class EnvironmentalEventHandler {

    private static final int EVENT_CHECK_INTERVAL = 18000; // Check every 15 minutes
    private static final float BASE_EVENT_CHANCE = 0.2f; // 20% base chance when conditions met

    // Nexus state modifiers
    private static final float RESTORED_MULTIPLIER = 0.3f;   // 70% fewer events
    private static final float DESTROYED_MULTIPLIER = 2.5f;  // 150% more events
    private static final float CONTROLLED_MULTIPLIER = 0.5f; // 50% fewer events

    public static void onSleepFinished(Object event) {
        if (booleanValue(event, "isCanceled") || !(eventValue(event, "getLevel") instanceof ServerLevel level)) {
            return;
        }
        advanceEventsForSleep(level, skippedTicks(eventValue(event, "getAdjustment"), level));
    }

    public static void onLevelTick(Object event) {
        if (!(eventValue(event, "getLevel") instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;

        long gameTime = level.getGameTime();

        // Get persistent world data
        EnvironmentalEventData eventData = EnvironmentalEventData.get(level);

        EnvironmentalEventType previousEvent = eventData.getCurrentEvent();
        eventData.tick(level);
        if (previousEvent != EnvironmentalEventType.NONE && eventData.getCurrentEvent() == EnvironmentalEventType.NONE) {
            restoreEventWeather(level, eventData);
            syncEventToPlayers(level, eventData, gameTime);
        }

        // Try to trigger new event
        if (gameTime % EVENT_CHECK_INTERVAL == 0 && eventData.canTriggerEvent()) {
            tryTriggerEvent(level, eventData, gameTime);
        }

        // Apply active event effects
        if (eventData.getCurrentEvent() != EnvironmentalEventType.NONE) {
            applyEventEffects(level, eventData);
            // Sync event state to clients every 5 seconds (100 ticks) during active events
            if (gameTime % 100 == 0) {
                syncEventToPlayers(level, eventData, gameTime);
            }
        }
    }

    public static void onPlayerTick(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player)) return;

        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        // Get persistent world data
        EnvironmentalEventData eventData = EnvironmentalEventData.get(serverLevel);

        // Apply player-specific event effects
        switch (eventData.getCurrentEvent()) {
            case RADIATION_STORM -> {
                // Radiation storm exposure is centralized in SurvivalTickHandler/HazardZoneManager.
            }
            case TOXIC_STORM -> {
                applyToxicStormEffects(player, eventData);
            }
            case BLACKOUT -> {
                // No direct player effect - handled by machine effects
            }
            case ASH_STORM -> applyAshStormEffects(player, eventData);
            case CRYO_FRONT -> applyCryoFrontEffects(player, eventData);
            case NEXUS_SURGE -> applyNexusSurgeEffects(player, eventData);
            default -> {
                // No active event
            }
        }
    }

    private static void tryTriggerEvent(ServerLevel level, EnvironmentalEventData data, long gameTime) {
        // Get Nexus state for event frequency modification
        NexusWorldData nexusData = NexusWorldData.get(level);
        float nexusMultiplier = getNexusEventMultiplier(nexusData);

        float reactivity = (float) Config.EVENT_REACTIVITY_MULTIPLIER.get().doubleValue();
        if (reactivity < 0.0f) reactivity = 0.0f;

        float finalChance = BASE_EVENT_CHANCE * reactivity * nexusMultiplier;
        if (level.getRandom().nextFloat() > finalChance) return;

        EnvironmentalEventType eventToTrigger = selectWeightedEvent(level, nexusData);

        if (eventToTrigger != EnvironmentalEventType.NONE) {
            startEvent(level, data, eventToTrigger, gameTime, nexusData);
        }
    }

    private static EnvironmentalEventType selectWeightedEvent(ServerLevel level, NexusWorldData nexusData) {
        float total = 0.0F;
        for (EnvironmentalEventProfile profile : EnvironmentalEventProfiles.activeProfiles()) {
            if (!canSelectEvent(profile.type(), nexusData)) {
                continue;
            }
            total += Math.max(0.0F, profile.weightFor(nexusData));
        }
        if (total <= 0.0F) {
            return EnvironmentalEventType.NONE;
        }

        float roll = level.getRandom().nextFloat() * total;
        for (EnvironmentalEventProfile profile : EnvironmentalEventProfiles.activeProfiles()) {
            if (!canSelectEvent(profile.type(), nexusData)) {
                continue;
            }
            roll -= Math.max(0.0F, profile.weightFor(nexusData));
            if (roll <= 0.0F) {
                return profile.type();
            }
        }
        return EnvironmentalEventType.NONE;
    }

    private static boolean canSelectEvent(EnvironmentalEventType type, NexusWorldData nexusData) {
        if (!EnvironmentalEventProfiles.isEnabled(type)) {
            return false;
        }
        if (type == EnvironmentalEventType.NEXUS_SURGE) {
            return nexusData.hasChoiceBeenMade() || !nexusData.getNexusPos().equals(BlockPos.ZERO);
        }
        return type != EnvironmentalEventType.NONE;
    }

    public static boolean forceStartEvent(ServerLevel level, EnvironmentalEventType type) {
        if (type == EnvironmentalEventType.NONE) {
            return false;
        }
        EnvironmentalEventData data = EnvironmentalEventData.get(level);
        if (data.getCurrentEvent() != EnvironmentalEventType.NONE) {
            clearActiveEvent(level);
        }
        startEvent(level, data, type, level.getGameTime(), NexusWorldData.get(level));
        return true;
    }

    public static boolean advanceEventsForSleep(ServerLevel level, long skippedTicks) {
        if (skippedTicks <= 0 || level.dimension() != Level.OVERWORLD) {
            return false;
        }

        EnvironmentalEventData data = EnvironmentalEventData.get(level);
        EnvironmentalEventType type = data.getCurrentEvent();
        if (type == EnvironmentalEventType.NONE) {
            return false;
        }

        long gameTime = level.getGameTime();
        boolean completed = data.advanceActiveEventForSleep(gameTime, skippedTicks);
        if (completed) {
            restoreEventWeather(level, data);
        } else if (forcesWeather(type)) {
            forceEventWeather(level, type, data.getRemainingEventTicks(gameTime));
        }
        syncEventToPlayers(level, data, gameTime);
        return true;
    }

    public static boolean clearActiveEvent(ServerLevel level) {
        EnvironmentalEventData data = EnvironmentalEventData.get(level);
        if (data.getCurrentEvent() == EnvironmentalEventType.NONE) {
            return false;
        }
        restoreEventWeather(level, data);
        data.clearEventWithoutSurvivalCount();
        syncEventToPlayers(level, data, level.getGameTime());
        return true;
    }

    private static void startEvent(ServerLevel level, EnvironmentalEventData data, EnvironmentalEventType type,
                                   long gameTime, NexusWorldData nexusData) {
        EnvironmentalEventProfile profile = EnvironmentalEventProfiles.get(type);
        int duration = profile == null ? EnvironmentalEventData.MAX_EVENT_DURATION : profile.durationTicks();
        data.startEvent(type, gameTime, duration, level.getRandom().nextLong());
        if (forcesWeather(type)) {
            data.captureWeatherSnapshot(level);
            forceEventWeather(level, type, data.getEventDuration());
        }
        broadcastEventAlert(level, type, nexusData);
        if (profile != null) {
            for (ServerPlayer player : level.players()) {
                EchoCoreServices.discoverFeature(player, AshfallDiscoveryProvider.eventId(profile.commandAlias()));
            }
        }
        playEventSound(level, type);
        syncEventToPlayers(level, data, gameTime);
    }

    private static float getNexusEventMultiplier(NexusWorldData nexusData) {
        return switch (nexusData.getState()) {
            case RESTORED -> RESTORED_MULTIPLIER;
            case DESTROYED -> DESTROYED_MULTIPLIER;
            case CONTROLLED -> CONTROLLED_MULTIPLIER;
            default -> 1.0f; // NORMAL
        };
    }

    private static void broadcastEventAlert(ServerLevel level, EnvironmentalEventType event, NexusWorldData nexusData) {
        Component alert = event.getAlertMessage();
        EnvironmentalEventStatus status = EnvironmentalEventStatus.fromData(EnvironmentalEventData.get(level), level.getGameTime());

        // Add Nexus context to alert based on state
        String nexusContext = "";
        if (nexusData.isDestroyed()) {
            nexusContext = " [The Grid is shattered...]";
        } else if (nexusData.isRestored()) {
            nexusContext = " [The Grid stabilizes the sector.]";
        } else if (nexusData.isControlled()) {
            nexusContext = " [You feel the pulse of the Core.]";
        }

        final Component finalAlert = Component.literal(alert.getString() + nexusContext
                + " Counter: " + status.counterGuidance());

        for (Player player : level.players()) {
            player.sendSystemMessage(finalAlert);
        }
    }

    private static void syncEventToPlayers(ServerLevel level, EnvironmentalEventData data, long gameTime) {
        EnvironmentalSyncPacket packet = new EnvironmentalSyncPacket(
            data.getCurrentEvent().name(),
            data.getCurrentEvent() != EnvironmentalEventType.NONE ? data.getEventStartTime() : 0,
            data.getCurrentEvent() != EnvironmentalEventType.NONE ? data.getEventDuration() : 0,
            gameTime,
            data.getCurrentEvent() != EnvironmentalEventType.NONE ? data.getEventIntensity() : 0.0F,
            data.getEventPhase(gameTime),
            data.getCurrentEvent() != EnvironmentalEventType.NONE ? data.getEventSeed() : 0L,
            data.getEventsSurvived(EnvironmentalEventType.RADIATION_STORM),
            data.getEventsSurvived(EnvironmentalEventType.TOXIC_STORM),
            data.getEventsSurvived(EnvironmentalEventType.BLACKOUT),
            data.getEventsSurvived(EnvironmentalEventType.ASH_STORM),
            data.getEventsSurvived(EnvironmentalEventType.CRYO_FRONT),
            data.getEventsSurvived(EnvironmentalEventType.NEXUS_SURGE)
        );
        for (Player player : level.players()) {
            if (player instanceof ServerPlayer serverPlayer) {
                EchoNetSend.toPlayer(serverPlayer, packet, EchoPacketKind.CLIENTBOUND_SYNC);
            }
        }
    }

    private static void applyEventEffects(ServerLevel level, EnvironmentalEventData data) {
        // Global event effects that apply to the world
        switch (data.getCurrentEvent()) {
            case RADIATION_STORM -> {
                ensureEventWeather(level, data);
            }
            case TOXIC_STORM -> {
                ensureEventWeather(level, data);
            }
            case CRYO_FRONT -> {
                ensureEventWeather(level, data);
            }
            case ASH_STORM, NEXUS_SURGE -> {
                ensureEventWeather(level, data);
            }
            case BLACKOUT -> {
                ensureEventWeather(level, data);
                // Machines lose power - handled in machine tick methods
            }
            default -> {
            }
        }
    }

    private static void ensureEventWeather(ServerLevel level, EnvironmentalEventData data) {
        EnvironmentalEventType type = data.getCurrentEvent();
        if (!forcesWeather(type)) {
            return;
        }
        data.captureWeatherSnapshot(level);
        EnvironmentalEventProfile profile = EnvironmentalEventProfiles.get(type);
        boolean shouldRain = profile != null
                && (profile.weatherMode() == EnvironmentalEventProfile.WeatherMode.RAIN
                || profile.weatherMode() == EnvironmentalEventProfile.WeatherMode.THUNDER);
        boolean shouldThunder = profile != null && profile.shouldThunder();
        if (level.isRaining() != shouldRain || level.isThundering() != shouldThunder) {
            forceEventWeather(level, type, data.getRemainingEventTicks(level.getGameTime()));
        }
    }

    private static void forceEventWeather(ServerLevel level, EnvironmentalEventType type, int remainingTicks) {
        EnvironmentalEventProfile profile = EnvironmentalEventProfiles.get(type);
        if (profile == null) {
            return;
        }
        int weatherTicks = Math.max(20, remainingTicks);
        var weather = level.getWeatherData();
        if (profile.weatherMode() == EnvironmentalEventProfile.WeatherMode.DRY
                || profile.weatherMode() == EnvironmentalEventProfile.WeatherMode.BLACKOUT) {
            weather.setRainTime(0);
            weather.setThunderTime(0);
            weather.setClearWeatherTime(weatherTicks);
            weather.setRaining(false);
            weather.setThundering(false);
            return;
        }
        weather.setClearWeatherTime(0);
        weather.setRainTime(weatherTicks);
        weather.setThunderTime(profile.shouldThunder() ? weatherTicks : 0);
        weather.setRaining(true);
        weather.setThundering(profile.shouldThunder());
    }

    private static void restoreEventWeather(ServerLevel level, EnvironmentalEventData data) {
        if (!data.isForcedWeatherEvent() || !data.hasWeatherSnapshot()) {
            data.clearWeatherSnapshot();
            return;
        }
        boolean restoreRain = data.wasRainingBeforeEvent();
        boolean restoreThunder = restoreRain && data.wasThunderingBeforeEvent();
        var weather = level.getWeatherData();
        weather.setClearWeatherTime(data.getPreviousClearWeatherTime());
        weather.setRainTime(data.getPreviousRainTime() > 0 ? data.getPreviousRainTime() : 6000);
        weather.setThunderTime(restoreThunder ? Math.max(20, data.getPreviousThunderTime()) : 0);
        weather.setRaining(restoreRain);
        weather.setThundering(restoreThunder);
        data.clearWeatherSnapshot();
    }

    private static boolean forcesWeather(EnvironmentalEventType type) {
        EnvironmentalEventProfile profile = EnvironmentalEventProfiles.get(type);
        return profile != null && profile.weatherMode() != EnvironmentalEventProfile.WeatherMode.NONE;
    }

    private static long skippedTicks(Object adjustment, ServerLevel level) {
        if (adjustment == null) {
            return 0L;
        }
        String adjustmentType = adjustment.getClass().getSimpleName();
        if (adjustmentType.contains("Marker")) {
            return ticksUntilNextDay(level.getGameTime());
        }
        Object ticks = eventValue(adjustment, "ticks");
        if (!(ticks instanceof Number number)) {
            return 0L;
        }
        long tickCount = number.longValue();
        if (adjustmentType.contains("Absolute")) {
            return Math.max(0L, tickCount - level.getGameTime());
        }
        return Math.max(0L, tickCount);
    }

    private static long ticksUntilNextDay(long gameTime) {
        long dayTime = Math.floorMod(gameTime, 24000L);
        return dayTime == 0L ? 24000L : 24000L - dayTime;
    }

    private static void playEventSound(ServerLevel level, EnvironmentalEventType type) {
        for (Player player : level.players()) {
            switch (type) {
                case RADIATION_STORM -> level.playSound(null, player.blockPosition(), ModSounds.RADIATION_STORM.get(), SoundSource.WEATHER, 0.6f, 0.75f);
                case TOXIC_STORM -> level.playSound(null, player.blockPosition(), ModSounds.TOXIC_STORM.get(), SoundSource.WEATHER, 0.7f, 0.85f);
                case ASH_STORM -> level.playSound(null, player.blockPosition(), ModSounds.ASH_STORM.get(), SoundSource.WEATHER, 0.55f, 0.65f);
                case CRYO_FRONT -> level.playSound(null, player.blockPosition(), ModSounds.CRYO_FRONT.get(), SoundSource.WEATHER, 0.55f, 1.2f);
                case NEXUS_SURGE -> level.playSound(null, player.blockPosition(), ModSounds.NEXUS_SURGE.get(), SoundSource.WEATHER, 0.7f, 1.0f);
                case BLACKOUT -> level.playSound(null, player.blockPosition(), ModSounds.BLACKOUT.get(), SoundSource.WEATHER, 0.65f, 0.7f);
                case NONE -> {
                }
            }
        }
    }

    private static void applyAshStormEffects(ServerPlayer player, EnvironmentalEventData data) {
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 100 != 0) {
            return;
        }
        boolean exposed = level.canSeeSky(player.blockPosition());
        if (!exposed) {
            return;
        }

        SurvivalData survival = player.getData(ModAttachments.SURVIVAL_DATA.get());
        int pressure = Math.max(1, Math.round(data.getEventIntensity()));
        survival.decrementHydration(pressure);
        if (survival.hasMask() && !survival.isFilterDepleted()) {
            survival.decrementFilter(pressure * 2);
        }
        player.setData(ModAttachments.SURVIVAL_DATA.get(), survival);
        player.syncData(ModAttachments.SURVIVAL_DATA.get());

        if (player.tickCount % 600 == 0 && level.getRandom().nextFloat() < 0.08F * data.getEventIntensity()) {
            spawnThreatNear(player, ModEntities.ASH_WRAITH.get(), 18.0D);
        }
    }

    private static void applyToxicStormEffects(ServerPlayer player, EnvironmentalEventData data) {
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 80 != 0) {
            return;
        }
        if (!level.canSeeSky(player.blockPosition()) || HazardZoneManager.hasAcidProtection(player)) {
            return;
        }

        SurvivalData survival = player.getData(ModAttachments.SURVIVAL_DATA.get());
        int pressure = Math.max(1, Math.round(DifficultyProfile.scaleHydrationDecay(
                player.level(), data.getEventIntensity())));
        survival.decrementHydration(pressure);
        if (survival.hasMask() && !survival.isFilterDepleted()) {
            survival.decrementFilter(pressure);
        }
        player.setData(ModAttachments.SURVIVAL_DATA.get(), survival);
        player.syncData(ModAttachments.SURVIVAL_DATA.get());

        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 0, false, false));
        player.hurtServer(level, player.damageSources().magic(),
                DifficultyProfile.scaleSurvivalHazard(player.level(), 1.0F));
    }

    private static void applyCryoFrontEffects(ServerPlayer player, EnvironmentalEventData data) {
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 100 != 0) {
            return;
        }
        if (!level.canSeeSky(player.blockPosition()) || HazardZoneManager.hasThermalProtection(player)) {
            return;
        }

        ColdData coldData = player.getData(ModAttachments.COLD_DATA.get());
        coldData.reduceTemperature(Math.max(1, Math.round(DifficultyProfile.scaleSurvivalHazard(
                player.level(), 2.0F * data.getEventIntensity()))));
        player.setData(ModAttachments.COLD_DATA.get(), coldData);
        player.syncData(ModAttachments.COLD_DATA.get());
    }

    private static void applyNexusSurgeEffects(ServerPlayer player, EnvironmentalEventData data) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.tickCount % 100 == 0 && level.canSeeSky(player.blockPosition())
                && (isInRadiationZone(player) || isNearNexusSource(player, 8))) {
            RadiationHelper.addEnvironmentalRadiation(player, 0.6F * data.getEventIntensity());
        }
        if (player.tickCount % 200 == 0) {
            surgeNearbyMachine(level, player.blockPosition(), data.getEventIntensity());
        }
    }

    private static void surgeNearbyMachine(ServerLevel level, BlockPos center, float intensity) {
        int radius = 14;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!MachineGameplayHelper.isMachineBlock(level.getBlockState(cursor))) {
                        continue;
                    }
                    double dist = center.distSqr(cursor);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = cursor.immutable();
                    }
                }
            }
        }
        if (best == null) {
            return;
        }
        long duration = Math.max(200L, Math.round(900.0F * Math.max(0.5F, intensity)));
        MachineGameplayHelper.addNexusSurge(level, best, duration);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, best.getX() + 0.5D, best.getY() + 1.0D, best.getZ() + 0.5D,
                6, 0.4D, 0.4D, 0.4D, 0.04D);
    }

    private static void spawnThreatNear(ServerPlayer player, net.minecraft.world.entity.EntityType<? extends Mob> type,
                                        double radius) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
        double distance = 8.0D + level.getRandom().nextDouble() * radius;
        BlockPos target = player.blockPosition().offset(
                (int) Math.round(Math.cos(angle) * distance),
                0,
                (int) Math.round(Math.sin(angle) * distance));
        BlockPos spawnPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target);
        Mob mob = type.create(level, EntitySpawnReason.EVENT);
        if (mob == null) {
            return;
        }
        mob.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        mob.setTarget(player);
        level.addFreshEntity(mob);
    }

    private static boolean isNearNexusSource(Player player, int radius) {
        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    var state = player.level().getBlockState(cursor);
                    if (state.is(ModBlocks.NEXUS_CORE.get())
                            || state.is(ModBlocks.NEXUS_CRACKED_SOIL.get())
                            || state.is(ModBlocks.ENERGIZED_FISSURE.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isInRadiationZone(Player player) {
        if (player == null) return false;
        if (com.knoxhack.echoashfallprotocol.survival.RadiationUtil.isPlayerIrradiated(player)) {
            return true;
        }

        String biomeId = player.level().getBiome(player.blockPosition()).getRegisteredName();
        if (biomeId.contains("radiation") || biomeId.contains("nexus_scar") || biomeId.contains("reactor")) {
            return true;
        }

        if (hasNearbyRadiationSource(player, 6)) {
            return true;
        }

        SurvivalData survival = player.getData(ModAttachments.SURVIVAL_DATA.get());
        return survival != null && survival.getRadiationLevel() > 35;
    }

    public static boolean isInToxicZone(Player player) {
        if (player == null) return false;
        String biomeId = player.level().getBiome(player.blockPosition()).getRegisteredName();
        if (biomeId.contains("toxic") || biomeId.contains("swamp")) {
            return true;
        }
        return hasNearbyToxicSource(player, 5);
    }

    public static boolean isInCryogenicZone(Player player) {
        if (player == null) return false;
        String biomeId = player.level().getBiome(player.blockPosition()).getRegisteredName();
        return biomeId.contains("cryogenic") || biomeId.contains("frozen") || biomeId.contains("cold");
    }

    private static boolean hasNearbyRadiationSource(Player player, int radius) {
        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    var state = player.level().getBlockState(cursor);
                    if (state.is(ModBlocks.RADIATION_BLOCK.get())
                            || state.is(ModBlocks.IRRADIATED_CRUST.get())
                            || state.is(ModBlocks.NEXUS_CRACKED_SOIL.get())
                            || state.is(ModBlocks.TOXIC_WASTE_BARREL.get())
                            || state.is(ModBlocks.NUCLEAR_GRASS.get())
                            || state.is(ModBlocks.NUCLEAR_TALL_GRASS.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasNearbyToxicSource(Player player, int radius) {
        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    var state = player.level().getBlockState(cursor);
                    if (state.is(ModBlocks.TOXIC_PUDDLE.get())
                            || state.is(ModBlocks.ACIDIC_SLUDGE.get())
                            || state.is(ModBlocks.CONTAMINATED_SOIL.get())
                            || state.is(ModBlocks.TOXIC_WASTE_BARREL.get())
                            || state.is(ModBlocks.TOXIC_GRASS.get())
                            || state.is(ModBlocks.TOXIC_TALL_GRASS.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if a specific event type is currently active for a level.
     */
    public static boolean isEventActive(Level level, EnvironmentalEventType type) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        EnvironmentalEventData data = EnvironmentalEventData.get(serverLevel);
        return data.getCurrentEvent() == type;
    }

    public static boolean isStormRainAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        EnvironmentalEventType type = EnvironmentalEventData.get(serverLevel).getCurrentEvent();
        EnvironmentalEventProfile profile = EnvironmentalEventProfiles.get(type);
        return profile != null && profile.forcesVanillaWeather() && serverLevel.canSeeSky(pos);
    }

    private static Object eventValue(Object event, String methodName) {
        if (event == null) {
            return null;
        }
        try {
            return event.getClass().getMethod(methodName).invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static boolean booleanValue(Object event, String methodName) {
        Object value = eventValue(event, methodName);
        return value instanceof Boolean bool && bool;
    }
}
