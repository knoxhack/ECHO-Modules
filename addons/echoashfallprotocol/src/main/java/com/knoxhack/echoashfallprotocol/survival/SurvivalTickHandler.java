package com.knoxhack.echoashfallprotocol.survival;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreHazardRuntime;
import com.knoxhack.echoashfallprotocol.gameplay.DifficultyProfile;
import com.knoxhack.echoashfallprotocol.gameplay.RadiationHelper;
import com.knoxhack.echoashfallprotocol.item.GasMaskItem;
import com.knoxhack.echoashfallprotocol.item.HazmatArmorItem;
import com.knoxhack.echoashfallprotocol.network.GraceCountdownPacket;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.world.StartingDropPodData;
import com.echoplatform.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetSend;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side tick handler for all survival systems:
 * - Toxic-zone filter drain
 * - Radiation accumulation/decay
 * - Hydration loss
 * - Environmental effects
 */
public class SurvivalTickHandler {

    private static final int TICK_INTERVAL = 20; // Once per second
    private static final String GRACE_PERIOD_NOTICE_SHOWN_KEY = "ashes_of_tomorrow.grace_period_notice_shown";
    private static final String SURVIVAL_WARNING_TICK_KEY = "ashes_of_tomorrow.survival_warning_tick";
    private static final String HYDRATION_DECAY_TICK_KEY = "ashes_of_tomorrow.hydration_decay_tick";
    private static final String HYDRATION_DECAY_REMAINDER_KEY = "ashes_of_tomorrow.hydration_decay_remainder";
    private static final String TOXIC_AIR_WARNING_TICK_KEY = "ashes_of_tomorrow.toxic_air_warning_tick";
    private static final String LAST_MUTATION_ROLL_TICK_KEY = "ashes_of_tomorrow.last_mutation_roll_tick";
    private static final String SEVERE_RADIATION_TICKS_KEY = "ashes_of_tomorrow.severe_radiation_ticks";
    private static final String OPENING_PROMPT_PREFIX = "opening:";
    private static final String DAMAGE_HINT_PREFIX = "death_hint:";
    private static final int GRACE_ENDING_WARNING_TICKS = 20 * 60; // 1 minute
    private static final int SURVIVAL_WARNING_COOLDOWN_TICKS = 20 * 12;
    private static final int OPENING_WATER_PROMPT_TICKS = 20 * 90;
    private static final int POD_RETURN_RADIUS_BLOCKS = 48;
    private static final int SEVERE_RADIATION_SUSTAINED_TICKS = 20 * 30;
    private static final float SCRUBBER_RADIATION_DECAY_MULTIPLIER = 4.0f;
    private static final int OPENING_BASICS_CACHE_TICKS = 100;
    private static final int HAZARD_SCAN_CACHE_TICKS = 60;
    private static final int CACHE_REUSE_RADIUS_BLOCKS = 4;
    private static final int RADIATION_HISTORY_SAMPLE_TICKS = 100;
    private static final long SLOW_SECTION_WARN_NANOS = 12_000_000L;
    private static final long SLOW_SUBSECTION_WARN_NANOS = 8_000_000L;
    private static final long SLOW_LOG_COOLDOWN_TICKS = 200L;
    private static final String LAST_GRACE_COUNTDOWN_SECOND_KEY = "ashes_of_tomorrow.last_grace_countdown_second";
    private static final Map<UUID, CachedOpeningBasics> OPENING_BASICS_CACHE = new HashMap<>();
    private static final Map<UUID, CachedHazardSnapshot> HAZARD_SNAPSHOT_CACHE = new HashMap<>();
    private static final Map<String, Long> SLOW_LOG_TICKS = new HashMap<>();

    public static void onPlayerTick(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player)) return;
        long currentTick = player.level().getGameTime();
        if (currentTick % TICK_INTERVAL != 0) return;
        long tickStart = System.nanoTime();

        SurvivalData data = player.getData(ModAttachments.SURVIVAL_DATA.get());
        boolean changed = false;
        int graceTicks = Math.max(0, Config.NEW_PLAYER_GRACE_TICKS.get());

        if (!data.isGraceInitialized()) {
            data.initializeGrace(currentTick);
            changed = true;
        }

        boolean graceActive = data.isGraceActive(currentTick, graceTicks);
        changed |= handleGraceMessages(player, data, currentTick, graceTicks, graceActive);

        syncGraceCountdown(player, data, currentTick, graceTicks, graceActive);
        handleOpeningGuidance(player, data, currentTick, graceTicks, graceActive);
        handleGraceExpiryNotice(player, data, currentTick, graceTicks, graceActive);

        HazardZoneManager.HazardSnapshot hazardState = cachedHazardSnapshot(player, currentTick);
        boolean hazardChanged = data.setHazardSnapshot(hazardState);
        changed |= hazardChanged;
        if (hazardChanged) {
            AshfallAdapterCoreHazardRuntime.hazardRouteCheck(player, hazardState, "survival_tick_hazard_snapshot");
        }

        // === AIR FILTER SYSTEM ===
        changed |= handleAirFilter(player, data, graceActive, hazardState, currentTick);

        // === RADIATION SYSTEM ===
        changed |= handleRadiation(player, data, graceActive, hazardState, currentTick);

        // === CRYO / TEMPERATURE SYSTEM ===
        handleCold(player, hazardState, graceActive);

        // === HYDRATION SYSTEM ===
        if (shouldRunHydrationDecay(player, currentTick)) {
            changed |= handleHydration(player, data, graceActive);
        }

        if (!graceActive) {
            sendSurvivalFeedback(player, data, currentTick, hazardState);
        }

        if (changed) {
            player.setData(ModAttachments.SURVIVAL_DATA.get(), data);
            player.syncData(ModAttachments.SURVIVAL_DATA.get());
        }
        logSlow(player, "survival.tick", tickStart, currentTick, SLOW_SECTION_WARN_NANOS);
    }

    public static void onBlockPlace(Object event) {
        if (eventValue(event, "getEntity") instanceof ServerPlayer player) {
            invalidatePassiveCaches(player);
        }
    }

    public static void onBlockBreak(Object event) {
        if (eventValue(event, "getPlayer") instanceof ServerPlayer player) {
            invalidatePassiveCaches(player);
        }
    }

    private static boolean handleGraceMessages(ServerPlayer player, SurvivalData data, long currentTick, int graceTicks, boolean graceActive) {
        if (graceTicks <= 0) return false;

        boolean changed = false;
        if (graceActive && !data.isGraceStartMessageSent()) {
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.grace.start"));
            data.setGraceStartMessageSent(true);
            changed = true;
        }

        long remaining = data.getGraceTicksRemaining(currentTick, graceTicks);
        if (graceActive && remaining <= GRACE_ENDING_WARNING_TICKS && !data.isGraceEndingMessageSent()) {
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.grace.expiring"));
            data.setGraceEndingMessageSent(true);
            changed = true;
        }

        return changed;
    }

    private static void handleOpeningGuidance(ServerPlayer player, SurvivalData data, long currentTick, int graceTicks, boolean graceActive) {
        if (!graceActive || graceTicks <= 0 || !data.isGraceInitialized()) {
            return;
        }

        long elapsed = currentTick - data.getGraceStartTick();
        OpeningBasics basics = cachedOpeningBasics(player, data, currentTick);

        if (elapsed >= OPENING_WATER_PROMPT_TICKS && !basics.hasCleanWaterHandled() && markSpecialOnce(player, OPENING_PROMPT_PREFIX + "drink_water")) {
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.opening.drink_water"), true);
        }

        if (!basics.hasCleanWaterHandled() && isTooFarFromStartingPod(player) && markSpecialOnce(player, OPENING_PROMPT_PREFIX + "return_pod")) {
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.opening.return_pod"), true);
        }

        if (isNightApproaching(player) && !basics.hasShelter() && markSpecialOnce(player, OPENING_PROMPT_PREFIX + "shelter")) {
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.opening.shelter"), true);
        }

        sendCheckpointPrompt(player, data, currentTick, graceTicks, 20 * 60 * 5, "5m", basics);
        sendCheckpointPrompt(player, data, currentTick, graceTicks, 20 * 60 * 2, "2m", basics);
        sendCheckpointPrompt(player, data, currentTick, graceTicks, 20 * 60, "1m", basics);
    }

    private static void sendCheckpointPrompt(ServerPlayer player, SurvivalData data, long currentTick, int graceTicks,
                                             int thresholdTicks, String label, OpeningBasics basics) {
        long remaining = data.getGraceTicksRemaining(currentTick, graceTicks);
        if (remaining > thresholdTicks || basics.isStable()) {
            return;
        }

        String marker = OPENING_PROMPT_PREFIX + "buffer_" + label;
        if (markSpecialOnce(player, marker)) {
            player.sendSystemMessage(Component.translatable(
                    "message.EchoAshfallProtocol.opening.buffer_checkpoint",
                    label,
                    basics.missingList()), true);
        }
    }

    private static boolean handleAirFilter(ServerPlayer player, SurvivalData data, boolean graceActive,
                                           HazardZoneManager.HazardSnapshot hazardState, long currentTick) {
        boolean hasGasMask = player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GasMaskItem;
        boolean hasFullHazmatSuit = HazardZoneManager.hasFullHazmat(player);
        boolean hasMask = hasGasMask || hasFullHazmatSuit;
        boolean changed = data.hasMask() != hasMask;
        data.setHasMask(hasMask);

        boolean inSafeZone = hazardState.safeZone();

        if (hasFullHazmatSuit || graceActive || !hazardState.toxicAir()) {
            if (data.getToxicAirWarningTicks() > 0) {
                data.setToxicAirWarningTicks(0);
                changed = true;
            }
            return changed;
        }

        if (hasGasMask && data.getAirFilterLife() > 0) {
            // Filters drain only while actually filtering toxic hazard air.
            int degradeAmount = Math.max(0, Math.round(Config.AIR_FILTER_DECAY_RATE.get()
                    * Math.max(1.0f, hazardState.toxicIntensity())));
            if (degradeAmount > 0) {
                data.decrementFilter(degradeAmount);
                changed = true;
            }
            if (data.getToxicAirWarningTicks() > 0) {
                data.setToxicAirWarningTicks(0);
                changed = true;
            }
            return changed;
        } else if ((!hasGasMask || data.isFilterDepleted()) && !inSafeZone) {
            int warningTicks = data.getToxicAirWarningTicks();
            int warningLimit = Math.max(0, Config.TOXIC_AIR_WARNING_TICKS.get());
            if (warningTicks < warningLimit) {
                data.setToxicAirWarningTicks(warningTicks + TICK_INTERVAL);
                sendToxicAirWarning(player, currentTick);
                return true;
            }
            // No protection and not in safe zone - take damage.
            if (currentTick % 60 == 0) { // Every 3 seconds
                sendDamageHintOnce(player, "toxic_air", Component.translatable("message.EchoAshfallProtocol.death_hint.toxic_air"));
                float damage = DifficultyProfile.scaleSurvivalHazard(player.level(), 1.0f);
                player.hurtServer((ServerLevel) player.level(), player.damageSources().magic(), damage);
            }
            // Apply breathing debuffs
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, false));
        } else if (inSafeZone && !hasGasMask) {
            if (player.level().getGameTime() % 200 == 0) {
                player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.hazard.scrubber_safe"), true);
            }
        }

        return changed;
    }

    private static boolean handleRadiation(ServerPlayer player, SurvivalData data, boolean graceActive,
                                           HazardZoneManager.HazardSnapshot hazardState, long currentTick) {
        float beforeRadiation = data.getRadiationLevel();
        boolean nearRadiation = hazardState.radiationZone();
        if (nearRadiation) {
            recordRadiationZoneScout(player);
        }

        if (nearRadiation && !hazardState.safeZone() && !graceActive) {
            // Check if player has rad resistance mutation
            MutationData mutations = player.getData(ModAttachments.MUTATION_DATA.get());
            double baseRadRate = Config.RADIATION_ACCUMULATION_RATE.get();
            if (baseRadRate < 0) baseRadRate = 1.0;
            float radAmount = (float) (baseRadRate * Math.max(0.25f, hazardState.radiationIntensity()));
            if (mutations.hasMutation(MutationData.MutationType.RAD_RESISTANCE)) {
                radAmount *= 0.5f;
            }
            radAmount = RadiationHelper.scaleIncomingRadiation(player, radAmount);
            radAmount *= Math.max(0.0f, 1.0f - HazardZoneManager.radiationResistance(player));
            if (radAmount > 0.0f) {
                sendDamageHintOnce(player, "radiation", Component.translatable("message.EchoAshfallProtocol.death_hint.radiation"));
                data.addRadiation(radAmount);
            }
        } else {
            // Natural radiation decay (configurable via Config.RADIATION_DECAY_RATE)
            double decayRate = Config.RADIATION_DECAY_RATE.get();
            float decayAmount = decayRate > 0 ? (float) decayRate : 0.2f;
            if (hazardState.safeZone()) {
                decayAmount *= SCRUBBER_RADIATION_DECAY_MULTIPLIER;
            }
            data.decayRadiation(decayAmount);
        }

        if (!graceActive) {
            // Apply radiation effects at high levels
            if (data.getRadiationLevel() > 75.0f) {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0, false, false));
            }
            if (data.getRadiationLevel() > 90.0f) {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, false));
            }

            handleMutationRoll(player, data, currentTick);

            // Apply ongoing passive mutation benefits every second
            MutationManager.applyMutationEffects(player);
            // Apply ongoing side effects from mutations
            MutationData mutations = player.getData(ModAttachments.MUTATION_DATA.get());
            MutationManager.applySideEffects(player, mutations);
        }

        // Modernization - Sample radiation for the Terminal Graph
        boolean radiationChanged = Math.abs(data.getRadiationLevel() - beforeRadiation) > 0.001F;
        if (radiationChanged) {
            AshfallAdapterCoreHazardRuntime.radiationChanged(
                    player, beforeRadiation, data.getRadiationLevel(), hazardState, "survival_tick");
        }
        boolean sampleHistory = currentTick % RADIATION_HISTORY_SAMPLE_TICKS == 0;
        if (radiationChanged || sampleHistory) {
            data.updateRadiationHistory(data.getRadiationLevel());
        }

        return radiationChanged || sampleHistory;
    }

    private static void handleCold(ServerPlayer player, HazardZoneManager.HazardSnapshot hazardState, boolean graceActive) {
        ColdData coldData = player.getData(ModAttachments.COLD_DATA.get());
        if (coldData.update(player, hazardState, graceActive)) {
            player.setData(ModAttachments.COLD_DATA.get(), coldData);
            player.syncData(ModAttachments.COLD_DATA.get());
        }
    }

    private static boolean handleHydration(ServerPlayer player, SurvivalData data, boolean graceActive) {
        if (graceActive) {
            return false;
        }

        // Hydration decreases over time (configurable)
        int decayRate = Config.HYDRATION_DECAY_RATE.get();
        if (decayRate > 0) {
            int scaledDecay = consumeScaledHydrationDecay(player, decayRate);
            if (scaledDecay > 0) {
                data.decrementHydration(scaledDecay);
            }
        }

        int penaltyLevel = Math.max(0, Math.min(100, Config.HYDRATION_PENALTY_LEVEL.get()));
        if (data.getHydration() <= penaltyLevel) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 40, 0, false, false));
        }
        if (data.getHydration() <= 0) {
            // Critical dehydration - take damage.
            if (player.level().getGameTime() % 80 == 0) {
                sendDamageHintOnce(player, "dehydration", Component.translatable("message.EchoAshfallProtocol.death_hint.dehydration"));
                player.hurtServer((net.minecraft.server.level.ServerLevel) player.level(), player.damageSources().starve(), 1.0f);
            }
        }

        return true;
    }

    private static int consumeScaledHydrationDecay(ServerPlayer player, int baseDecay) {
        float scaled = Math.max(0.0f, DifficultyProfile.scaleHydrationDecay(player.level(), baseDecay));
        int wholeDecay = (int) Math.floor(scaled);
        float fractionalDecay = scaled - wholeDecay;
        if (fractionalDecay <= 0.0f) {
            return wholeDecay;
        }

        CompoundTag playerData = player.getPersistentData();
        float accumulated = playerData.getFloat(HYDRATION_DECAY_REMAINDER_KEY).orElse(0.0f) + fractionalDecay;
        int extraDecay = (int) Math.floor(accumulated);
        playerData.putFloat(HYDRATION_DECAY_REMAINDER_KEY, accumulated - extraDecay);
        return wholeDecay + extraDecay;
    }

    private static void sendToxicAirWarningLegacy(ServerPlayer player, long currentTick) {
        CompoundTag playerData = player.getPersistentData();
        long nextWarningTick = playerData.getLong(TOXIC_AIR_WARNING_TICK_KEY).orElse(0L);
        if (currentTick < nextWarningTick) {
            return;
        }
        player.sendSystemMessage(Component.literal("\u00A7c[ECHO-7]\u00A7r Toxic air exposure. Seal mask or reach scrubbed air."), true);
        playerData.putLong(TOXIC_AIR_WARNING_TICK_KEY, currentTick + SURVIVAL_WARNING_COOLDOWN_TICKS);
    }

    private static void handleMutationRollLegacy(ServerPlayer player, SurvivalData data, long currentTick) {
        double mutationThreshold = Config.MUTATION_THRESHOLD.get();
        if (mutationThreshold < 0) mutationThreshold = 50.0;
        if (data.getRadiationLevel() <= mutationThreshold) {
            player.getPersistentData().putLong(SEVERE_RADIATION_TICKS_KEY, 0L);
            return;
        }

        CompoundTag playerData = player.getPersistentData();
        long sustainedTicks = playerData.getLong(SEVERE_RADIATION_TICKS_KEY).orElse(0L) + TICK_INTERVAL;
        playerData.putLong(SEVERE_RADIATION_TICKS_KEY, sustainedTicks);
        long lastRoll = playerData.getLong(LAST_MUTATION_ROLL_TICK_KEY).orElse(0L);
        if (sustainedTicks >= SEVERE_RADIATION_SUSTAINED_TICKS && currentTick - lastRoll >= 200L) {
            playerData.putLong(LAST_MUTATION_ROLL_TICK_KEY, currentTick);
            MutationManager.tryMutate(player, data.getRadiationLevel());
        }
    }

    private static boolean shouldRunHydrationDecay(ServerPlayer player, long currentTick) {
        int intervalTicks = Math.max(TICK_INTERVAL, Config.HYDRATION_DECAY_INTERVAL_TICKS.get());
        CompoundTag playerData = player.getPersistentData();
        long nextDecayTick = playerData.getLong(HYDRATION_DECAY_TICK_KEY).orElse(0L);
        if (currentTick < nextDecayTick) {
            return false;
        }
        playerData.putLong(HYDRATION_DECAY_TICK_KEY, currentTick + intervalTicks);
        return true;
    }

    private static void syncGraceCountdown(ServerPlayer player, SurvivalData data, long currentTick, int graceTicks, boolean graceActive) {
        CompoundTag playerData = player.getPersistentData();
        if (graceTicks <= 0 || !data.isGraceInitialized()) {
            return;
        }

        long remainingTicks = data.getGraceTicksRemaining(currentTick, graceTicks);
        if (graceActive && remainingTicks > 0L) {
            long displayedSecond = (remainingTicks + 19L) / 20L;
            long lastDisplayedSecond = playerData.getLong(LAST_GRACE_COUNTDOWN_SECOND_KEY).orElse(Long.MIN_VALUE);
            if (displayedSecond != lastDisplayedSecond) {
                playerData.putLong(LAST_GRACE_COUNTDOWN_SECOND_KEY, displayedSecond);
                EchoNetSend.toPlayer(player, new GraceCountdownPacket(remainingTicks, true), EchoPacketKind.CLIENTBOUND_SYNC);
            }
            return;
        }

        boolean graceExpired = currentTick - data.getGraceStartTick() >= graceTicks;
        long lastDisplayedSecond = playerData.getLong(LAST_GRACE_COUNTDOWN_SECOND_KEY).orElse(Long.MIN_VALUE);
        if (graceExpired && lastDisplayedSecond != 0L
                && !playerData.getBoolean(GRACE_PERIOD_NOTICE_SHOWN_KEY).orElse(false)) {
            playerData.putLong(LAST_GRACE_COUNTDOWN_SECOND_KEY, 0L);
            EchoNetSend.toPlayer(player, new GraceCountdownPacket(0L, false), EchoPacketKind.CLIENTBOUND_SYNC);
        }
    }

    private static void handleGraceExpiryNotice(ServerPlayer player, SurvivalData data, long currentTick, int graceTicks, boolean graceActive) {
        CompoundTag playerData = player.getPersistentData();
        if (graceTicks <= 0 || !data.isGraceInitialized()) {
            return;
        }

        if (graceActive) {
            playerData.putBoolean(GRACE_PERIOD_NOTICE_SHOWN_KEY, false);
            return;
        }

        boolean graceExpired = currentTick - data.getGraceStartTick() >= graceTicks;
        if (graceExpired && !playerData.getBoolean(GRACE_PERIOD_NOTICE_SHOWN_KEY).orElse(false)) {
            OpeningBasics basics = cachedOpeningBasics(player, data, currentTick);
            Component message = basics.isStable()
                    ? Component.translatable("message.EchoAshfallProtocol.grace.expired.ready")
                    : Component.translatable("message.EchoAshfallProtocol.grace.expired.missing", basics.missingList());
            player.sendSystemMessage(message, true);
            playerData.putBoolean(GRACE_PERIOD_NOTICE_SHOWN_KEY, true);
        }
    }

    private static void sendSurvivalFeedback(ServerPlayer player, SurvivalData data, long currentTick,
                                             HazardZoneManager.HazardSnapshot hazardState) {
        CompoundTag playerData = player.getPersistentData();
        long nextWarningTick = playerData.getLong(SURVIVAL_WARNING_TICK_KEY).orElse(0L);
        if (currentTick < nextWarningTick) {
            return;
        }

        String message = null;
        if (hazardState.radiationStorm() && !hazardState.stormSheltered()) {
            message = "\u00A7c[ECHO-7]\u00A7r Radiation storm overhead. Get under cover or deploy a scrubber.";
        } else if (hazardState.nexusAnomaly()) {
            message = "\u00A7d[ECHO-7 // NEXUS]\u00A7r Anomaly pressure rising. Hazmat shielding and RadAway advised.";
        } else if (hazardState.acidContact()) {
            message = "\u00A7c[ECHO-7]\u00A7r Acid contact detected. Leave sludge and seal armor before continuing.";
        } else if (hazardState.cryoCold() && !HazardZoneManager.hasThermalProtection(player)) {
            message = "\u00A7b[ECHO-7]\u00A7r Cryo cold draining heat. Use thermal lining, fire, shelter, or scrubbed air.";
        } else if (hazardState.toxicAir() && (!data.hasMask() || data.isFilterDepleted())) {
            message = "\u00A7c[ECHO-7]\u00A7r Toxic air detected. Equip a gas mask with filter charge or leave the hazard zone.";
        } else if (data.hasMask() && data.getAirFilterLife() <= SurvivalData.MAX_AIR_FILTER * 0.15f) {
            message = "\u00A7e[ECHO-7]\u00A7r Air filter low. Replace cartridge before the next toxic route.";
        } else if (data.getRadiationLevel() >= 70.0f) {
            message = "\u00A7c[ECHO-7]\u00A7r Radiation spike. Retreat, use RadAway, then decontaminate salvage.";
        } else if (data.getHydration() <= Config.HYDRATION_WARNING_LEVEL.get()) {
            message = "\u00A7b[ECHO-7]\u00A7r Water reserve low. Drink clean water before the next expedition.";
        } else {
            MutationData mutations = player.getData(ModAttachments.MUTATION_DATA.get());
            if (MutationManager.shouldShowInstabilityWarning(data, mutations)) {
                message = "\u00A7d[ECHO-7]\u00A7r Mutation instability rising. Use Field Med Bay support before combat.";
            }
        }

        if (message != null) {
            player.sendSystemMessage(Component.literal(message), true);
            playerData.putLong(SURVIVAL_WARNING_TICK_KEY,
                    currentTick + Math.max(SURVIVAL_WARNING_COOLDOWN_TICKS, Config.HAZARD_WARNING_COOLDOWN_TICKS.get()));
        }
    }

    private static void sendToxicAirWarning(ServerPlayer player, long currentTick) {
        CompoundTag playerData = player.getPersistentData();
        long nextWarningTick = playerData.getLong(TOXIC_AIR_WARNING_TICK_KEY).orElse(0L);
        if (currentTick < nextWarningTick) {
            return;
        }
        player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.warning.toxic_air"), true);
        playerData.putLong(TOXIC_AIR_WARNING_TICK_KEY, currentTick + 40L);
    }

    private static void handleMutationRoll(ServerPlayer player, SurvivalData data, long currentTick) {
        CompoundTag playerData = player.getPersistentData();
        double severeThreshold = Math.max(Config.MUTATION_SEVERE_THRESHOLD.get(), Config.MUTATION_THRESHOLD.get());
        int severeTicks = playerData.getInt(SEVERE_RADIATION_TICKS_KEY).orElse(0);
        if (data.getRadiationLevel() < severeThreshold) {
            if (severeTicks > 0) {
                playerData.putInt(SEVERE_RADIATION_TICKS_KEY, 0);
            }
            return;
        }

        severeTicks += TICK_INTERVAL;
        playerData.putInt(SEVERE_RADIATION_TICKS_KEY, severeTicks);
        long lastRollTick = playerData.getLong(LAST_MUTATION_ROLL_TICK_KEY).orElse((long) -Config.MUTATION_ROLL_COOLDOWN_TICKS.get());
        int cooldownTicks = Math.max(200, Config.MUTATION_ROLL_COOLDOWN_TICKS.get());
        if (severeTicks >= SEVERE_RADIATION_SUSTAINED_TICKS && currentTick - lastRollTick >= cooldownTicks) {
            MutationManager.tryMutate(player, data.getRadiationLevel());
            playerData.putLong(LAST_MUTATION_ROLL_TICK_KEY, currentTick);
            playerData.putInt(SEVERE_RADIATION_TICKS_KEY, 0);
        }
    }

    private static boolean markSpecialOnce(ServerPlayer player, String marker) {
        QuestData quest = QuestData.get(player);
        if (quest.hasVisitedLocation("special", marker)) {
            return false;
        }
        quest.visitLocation("special", marker);
        QuestData.saveAndSync(player, quest);
        return true;
    }

    private static void recordRadiationZoneScout(ServerPlayer player) {
        QuestData quest = QuestData.get(player);
        if (quest.hasVisitedLocation("biome", "radiation_zone")) {
            return;
        }
        quest.visitLocation("biome", "radiation_zone");
        quest.visitLocation("special", "hazard:radiation_zone");
        QuestData.saveAndSync(player, quest);
        AshfallAdapterCoreHazardRuntime.hazardRouteCheck(player, HazardZoneManager.scan(player), "radiation_zone_scout");
        player.sendSystemMessage(Component.literal("\u00A76[ECHO-7]\u00A7r Radiation-zone scout recorded. Retreat before exposure sustains."), true);
    }

    private static void sendDamageHintOnce(ServerPlayer player, String marker, Component message) {
        if (markSpecialOnce(player, DAMAGE_HINT_PREFIX + marker)) {
            player.sendSystemMessage(message, true);
        }
    }

    private static boolean isTooFarFromStartingPod(ServerPlayer player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return false;
        }
        return StartingDropPodData.get(level)
                .findForPlayer(player.getUUID())
                .map(entry -> entry.interior().distSqr(player.blockPosition()) > POD_RETURN_RADIUS_BLOCKS * POD_RETURN_RADIUS_BLOCKS)
                .orElse(false);
    }

    private static boolean isNightApproaching(ServerPlayer player) {
        long dayTime = player.level().getGameTime() % 24000L;
        return dayTime >= 11000L && dayTime <= 13500L;
    }

    private static boolean hasBlockNearPlayer(ServerPlayer player, Block block, int radius) {
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -3, -radius), center.offset(radius, 3, radius))) {
            if (player.level().getBlockState(pos).is(block)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBedNearPlayer(ServerPlayer player, int radius) {
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -3, -radius), center.offset(radius, 3, radius))) {
            if (player.level().getBlockState(pos).isBed(player.level(), pos, player)) {
                return true;
            }
        }
        return false;
    }

    private static OpeningBasics cachedOpeningBasics(ServerPlayer player, SurvivalData data, long currentTick) {
        UUID playerId = player.getUUID();
        CachedOpeningBasics cached = OPENING_BASICS_CACHE.get(playerId);
        BlockPos currentPos = player.blockPosition();
        ResourceKey<Level> dimension = player.level().dimension();
        if (cached != null && cached.validFor(dimension, currentPos, currentTick, OPENING_BASICS_CACHE_TICKS)) {
            return cached.basics();
        }

        long start = System.nanoTime();
        OpeningBasics basics = OpeningBasics.of(player, data);
        OPENING_BASICS_CACHE.put(playerId, new CachedOpeningBasics(dimension, currentPos.immutable(), currentTick, basics));
        logSlow(player, "survival.opening_basics", start, currentTick, SLOW_SUBSECTION_WARN_NANOS);
        return basics;
    }

    private static HazardZoneManager.HazardSnapshot cachedHazardSnapshot(ServerPlayer player, long currentTick) {
        UUID playerId = player.getUUID();
        CachedHazardSnapshot cached = HAZARD_SNAPSHOT_CACHE.get(playerId);
        BlockPos currentPos = player.blockPosition();
        ResourceKey<Level> dimension = player.level().dimension();
        if (cached != null && cached.validFor(dimension, currentPos, currentTick, HAZARD_SCAN_CACHE_TICKS)) {
            return cached.snapshot();
        }

        long start = System.nanoTime();
        HazardZoneManager.HazardSnapshot snapshot = HazardZoneManager.scan(player);
        HAZARD_SNAPSHOT_CACHE.put(playerId, new CachedHazardSnapshot(dimension, currentPos.immutable(), currentTick, snapshot));
        logSlow(player, "survival.hazard_scan", start, currentTick, SLOW_SUBSECTION_WARN_NANOS);
        return snapshot;
    }

    private static void invalidatePassiveCaches(ServerPlayer player) {
        UUID playerId = player.getUUID();
        OPENING_BASICS_CACHE.remove(playerId);
        HAZARD_SNAPSHOT_CACHE.remove(playerId);
    }

    private static boolean cacheUsable(ResourceKey<Level> cachedDimension, BlockPos cachedPos,
                                       ResourceKey<Level> currentDimension, BlockPos currentPos,
                                       long cachedTick, long currentTick, int maxAgeTicks) {
        return cachedDimension.equals(currentDimension)
                && currentTick - cachedTick <= maxAgeTicks
                && cachedPos.distSqr(currentPos) <= CACHE_REUSE_RADIUS_BLOCKS * CACHE_REUSE_RADIUS_BLOCKS;
    }

    private static void logSlow(ServerPlayer player, String section, long startNanos, long currentTick, long thresholdNanos) {
        long elapsed = System.nanoTime() - startNanos;
        if (elapsed < thresholdNanos) {
            return;
        }
        String key = section + ":" + player.getUUID();
        long lastLogTick = SLOW_LOG_TICKS.getOrDefault(key, Long.MIN_VALUE);
        if (lastLogTick != Long.MIN_VALUE && currentTick - lastLogTick < SLOW_LOG_COOLDOWN_TICKS) {
            return;
        }
        SLOW_LOG_TICKS.put(key, currentTick);
        EchoAshfallProtocol.LOGGER.warn("{} for {} took {} ms.",
                section,
                player.getName().getString(),
                String.format(java.util.Locale.ROOT, "%.2f", elapsed / 1_000_000.0D));
    }

    private record CachedOpeningBasics(
            ResourceKey<Level> dimension,
            BlockPos position,
            long gameTime,
            OpeningBasics basics) {
        boolean validFor(ResourceKey<Level> currentDimension, BlockPos currentPos, long currentTick, int maxAgeTicks) {
            return cacheUsable(dimension, position, currentDimension, currentPos, gameTime, currentTick, maxAgeTicks);
        }
    }

    private record CachedHazardSnapshot(
            ResourceKey<Level> dimension,
            BlockPos position,
            long gameTime,
            HazardZoneManager.HazardSnapshot snapshot) {
        boolean validFor(ResourceKey<Level> currentDimension, BlockPos currentPos, long currentTick, int maxAgeTicks) {
            return cacheUsable(dimension, position, currentDimension, currentPos, gameTime, currentTick, maxAgeTicks);
        }
    }

    private record OpeningBasics(boolean hasMask, boolean hasCleanWaterHandled, boolean hasShelter, boolean hasTool) {
        static OpeningBasics of(ServerPlayer player, SurvivalData data) {
            QuestData quest = QuestData.get(player);
            boolean mask = player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GasMaskItem
                    || HazmatArmorItem.hasFullSet(getArmorItems(player));
            boolean hasCleanWater = player.getInventory().contains(new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get()));
            boolean cleanWaterHandled = quest.hasVisitedLocation("special", "water:clean_consumed")
                    || (data.getHydration() >= 80 && !hasCleanWater);
            boolean shelter = hasBlockNearPlayer(player, ModBlocks.ASH_CAMPFIRE.get(), 16)
                    || hasBlockNearPlayer(player, Blocks.CHEST, 12)
                    || hasBedNearPlayer(player, 16);
            boolean tool = player.getInventory().contains(new ItemStack(Items.WOODEN_SWORD))
                    || player.getInventory().contains(new ItemStack(ModItems.SCRAP_KNIFE.get()))
                    || player.getInventory().contains(new ItemStack(ModItems.ASHBONE_SHIV.get()))
                    || player.getInventory().contains(new ItemStack(ModItems.SCAVENGER_SPEAR.get()));
            return new OpeningBasics(mask, cleanWaterHandled, shelter, tool);
        }

        boolean hasMaskAndWater() {
            return hasMask && hasCleanWaterHandled;
        }

        boolean isStable() {
            return hasCleanWaterHandled && hasShelter && hasTool;
        }

        String missingList() {
            List<String> missing = new java.util.ArrayList<>();
            if (!hasCleanWaterHandled) missing.add("water");
            if (!hasShelter) missing.add("shelter");
            if (!hasTool) missing.add("tool");
            return missing.isEmpty() ? "none" : String.join(", ", missing);
        }
    }

    private static Iterable<ItemStack> getArmorItems(ServerPlayer player) {
        return List.of(
            player.getItemBySlot(EquipmentSlot.HEAD),
            player.getItemBySlot(EquipmentSlot.CHEST),
            player.getItemBySlot(EquipmentSlot.LEGS),
            player.getItemBySlot(EquipmentSlot.FEET)
        );
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

}
