package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echo.adaptercore.EchoCanonicalContentIds;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeEvent;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.echo.EchoMessages;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.gameplay.RadiationHelper;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.survival.HazardZoneManager;
import com.knoxhack.echoashfallprotocol.survival.MutationData;
import com.knoxhack.echoashfallprotocol.survival.MutationManager;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AshfallAdapterCoreHazardRuntime {
    private static final String RUNTIME_HOST_ID = "echoashfallprotocol:hazard_runtime";
    private static final String LAST_EVENT_KEY = "ashes_of_tomorrow.adaptercore.last_hazard_event";
    private static final String LAST_EVENT_TICK_KEY = "ashes_of_tomorrow.adaptercore.last_hazard_event_tick";
    private static final AshfallAdapterCoreRuntimeTruthBridge.RuntimeBinding RUNTIME_BINDING =
            AshfallAdapterCoreRuntimeTruthBridge.binding(
                    RUNTIME_HOST_ID,
                    "hazard",
                    LAST_EVENT_KEY,
                    LAST_EVENT_TICK_KEY,
                    Set.of(
                            EchoCanonicalContentIds.EVENT_ASHFALL_RADIATION_CHANGED,
                            EchoCanonicalContentIds.EVENT_ASHFALL_MUTATION_GAINED,
                            EchoCanonicalContentIds.EVENT_ASHFALL_TREATMENT_APPLIED,
                            EchoCanonicalContentIds.EVENT_ASHFALL_MED_BAY_USED,
                            EchoCanonicalContentIds.EVENT_ASHFALL_CLEANSER_USED,
                            EchoCanonicalContentIds.EVENT_ASHFALL_SCRUBBER_USED,
                            EchoCanonicalContentIds.EVENT_ASHFALL_LAB_OBJECTIVE,
                            EchoCanonicalContentIds.EVENT_ASHFALL_VAULT_OBJECTIVE,
                            EchoCanonicalContentIds.EVENT_ASHFALL_HAZARD_ROUTE_CHECK,
                            EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED),
                    Set.of(
                            EchoCanonicalContentIds.ITEM_RAD_AWAY,
                            EchoCanonicalContentIds.ITEM_FILTER_CARTRIDGE_BASIC,
                            EchoCanonicalContentIds.ITEM_FILTER_CARTRIDGE_ADVANCED,
                            EchoCanonicalContentIds.ITEM_FILTER_CARTRIDGE_ELITE,
                            EchoCanonicalContentIds.ITEM_GAS_MASK,
                            EchoCanonicalContentIds.ITEM_MUTAGEN_VIAL,
                            EchoCanonicalContentIds.BLOCK_FIELD_MED_BAY,
                            EchoCanonicalContentIds.BLOCK_RADIATION_CLEANSER,
                            EchoCanonicalContentIds.BLOCK_ATMOSPHERIC_SCRUBBER,
                            "echoashfallprotocol:use_rad_away",
                            "echoashfallprotocol:use_field_med_bay",
                            "echoashfallprotocol:build_radiation_cleanser",
                            "echoashfallprotocol:build_atmospheric_scrubber",
                            "echoashfallprotocol:enter_bio_lab",
                            "echoashfallprotocol:survey_reactor_ruin",
                            "echoashfallprotocol:clear_military_vault",
                            "echoashfallprotocol:scout_radiation_zone"),
                    AshfallAdapterCoreHazardRuntime::apply);

    private AshfallAdapterCoreHazardRuntime() {
    }

    public static NativeResult radiationChanged(
            ServerPlayer player,
            float before,
            float after,
            HazardZoneManager.HazardSnapshot snapshot,
            String source) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", after >= before ? "radiation/exposure" : "radiation/decay");
        payload.put("source", safe(source));
        payload.put("beforeRadiation", before);
        payload.put("afterRadiation", after);
        payload.put("delta", after - before);
        payload.putAll(hazardPayload(snapshot));
        return publish(player, "ashfall.radiation_changed", payload);
    }

    public static NativeResult mutationGained(
            ServerPlayer player,
            MutationData.MutationType mutation,
            int mutationCount,
            List<String> sideEffects,
            float radiationLevel) {
        return publish(player, "ashfall.mutation_gained", Map.of(
                "target", "mutation/" + sanitizeTarget(mutation.getId()),
                "source", "mutation_roll",
                "mutationId", mutation.getId(),
                "mutationName", mutation.getDisplayName(),
                "mutationCount", mutationCount,
                "sideEffects", sideEffects == null ? List.of() : List.copyOf(sideEffects),
                "radiationLevel", radiationLevel));
    }

    public static NativeResult treatmentApplied(
            ServerPlayer player,
            String treatment,
            float beforeRadiation,
            float afterRadiation,
            String source) {
        return publish(player, "ashfall.treatment_applied", Map.of(
                "target", "treatment/" + sanitizeTarget(treatment),
                "source", safe(source),
                "treatment", safe(treatment),
                "beforeRadiation", beforeRadiation,
                "afterRadiation", afterRadiation,
                "radiationRemoved", Math.max(0.0f, beforeRadiation - afterRadiation)));
    }

    public static NativeResult radAwayUsed(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        return publish(player, EchoCanonicalContentIds.EVENT_ASHFALL_TREATMENT_APPLIED, Map.of(
                "target", "treatment/rad_away",
                "source", "radaway_item_use",
                "treatment", "rad_away",
                "itemId", EchoCanonicalContentIds.ITEM_RAD_AWAY,
                "item", itemId(stack),
                "hand", hand == null ? InteractionHand.MAIN_HAND.name() : hand.name(),
                "runtimeMutation", true,
                "radiationRemovalFraction", 0.5F,
                "regenerationTicks", 100,
                "regenerationAmplifier", 1), null, false);
    }

    public static NativeResult filterCartridgeUsed(
            ServerPlayer player,
            ItemStack stack,
            InteractionHand hand,
            String tierName,
            int tierLevel,
            int refillAmount) {
        String itemId = itemId(stack);
        return publish(player, EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED, Map.of(
                "target", safe(itemId),
                "itemId", safe(itemId),
                "item", safe(itemId),
                "source", "filter_cartridge_item_use",
                "hand", hand == null ? InteractionHand.MAIN_HAND.name() : hand.name(),
                "filterCartridgeUse", true,
                "filterTierName", safe(tierName),
                "filterTier", Math.max(0, tierLevel),
                "refillAmount", Math.max(0, refillAmount)), null, false);
    }

    public static NativeResult mutagenUsed(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        return publish(player, EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED, Map.of(
                "target", EchoCanonicalContentIds.ITEM_MUTAGEN_VIAL,
                "itemId", EchoCanonicalContentIds.ITEM_MUTAGEN_VIAL,
                "item", itemId(stack),
                "source", "mutagen_item_use",
                "mutagenUse", true,
                "hand", hand == null ? InteractionHand.MAIN_HAND.name() : hand.name(),
                "radiationAmount", 40.0F,
                "nauseaTicks", 200,
                "weaknessTicks", 200), null, false);
    }

    public static NativeResult medBayUsed(
            ServerPlayer player,
            BlockPos pos,
            int energyStored,
            int mutationCount) {
        return publish(player, EchoCanonicalContentIds.EVENT_ASHFALL_MED_BAY_USED, Map.of(
                "target", "medical:field_med_bay_used",
                "source", "field_med_bay_tick",
                "energyStored", energyStored,
                "mutationCount", mutationCount,
                "pos", positionSnapshot(pos)), pos, true);
    }

    public static void radiationCleanserUsed(Level level, BlockPos pos, Item inputItem, Item outputItem) {
        if (!(level instanceof ServerLevel) || pos == null || !level.isLoaded(pos)) {
            return;
        }
        AABB area = new AABB(pos).inflate(6.0D);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            radiationCleanserUsed(player, pos, itemId(inputItem), itemId(outputItem));
        }
    }

    public static NativeResult radiationCleanserUsed(
            ServerPlayer player,
            BlockPos pos,
            String inputItem,
            String outputItem) {
        return publish(player, "ashfall.cleanser_used", Map.of(
                "target", "machine/radiation_cleanser_used",
                "source", "radiation_cleanser_cycle",
                "inputItem", safe(inputItem),
                "outputItem", safe(outputItem),
                "pos", positionSnapshot(pos)), pos, true);
    }

    public static NativeResult atmosphericScrubberUsed(
            ServerPlayer player,
            BlockPos pos,
            float beforeRadiation,
            float afterRadiation,
            int radius) {
        return publish(player, "ashfall.scrubber_used", Map.of(
                "target", "hazard/atmospheric_scrubber_used",
                "source", "atmospheric_scrubber_tick",
                "beforeRadiation", beforeRadiation,
                "afterRadiation", afterRadiation,
                "radius", radius,
                "pos", positionSnapshot(pos)), pos, true);
    }

    public static NativeResult labObjective(
            ServerPlayer player,
            String objective,
            @Nullable BlockPos pos,
            String source) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", "lab/" + sanitizeTarget(objective));
        payload.put("source", safe(source));
        payload.put("objective", safe(objective));
        if (pos != null) {
            payload.put("pos", positionSnapshot(pos));
        }
        return publish(player, "ashfall.lab_objective", payload, pos, true);
    }

    public static NativeResult hazardRouteObjective(
            ServerPlayer player,
            String siteId,
            String route,
            String hazardProfile,
            String source) {
        return publish(player, "ashfall.vault_objective", Map.of(
                "target", routeObjectiveTarget(siteId, route),
                "source", safe(source),
                "siteId", safe(siteId),
                "route", safe(route),
                "hazardProfile", safe(hazardProfile)));
    }

    public static NativeResult hazardRouteCheck(
            ServerPlayer player,
            HazardZoneManager.HazardSnapshot snapshot,
            String source) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", "hazard/" + sanitizeTarget(snapshot == null ? "none" : snapshot.primaryType().name()));
        payload.put("source", safe(source));
        payload.putAll(hazardPayload(snapshot));
        return publish(player, "ashfall.hazard_route_check", payload);
    }

    private static NativeResult publish(ServerPlayer player, String eventId, Map<String, Object> payload) {
        return publish(player, eventId, payload, null, true);
    }

    private static NativeResult publish(
            ServerPlayer player,
            String eventId,
            Map<String, Object> payload,
            BlockPos requiredLoadedPos,
            boolean dedupeSameTick) {
        return AshfallAdapterCoreRuntimeTruthBridge.publish(
                RUNTIME_BINDING,
                player,
                eventId,
                payload,
                requiredLoadedPos,
                dedupeSameTick);
    }

    private static NativeResult apply(ServerPlayer player, NativeEvent event, NativeMutationContext context) {
        Map<String, Object> payload = event.payload();
        String target = stringValue(payload, "target");
        Map<String, Object> resultSnapshot = new LinkedHashMap<>();

        boolean changed = false;

        switch (event.eventId()) {
            case "ashfall.radiation_changed" -> changed |= applyRadiationChanged(player, payload);
            case "ashfall.mutation_gained" -> {
                String mutationId = stringValue(payload, "mutationId");
                changed |= markSpecial(player, "mutation:" + sanitizeTarget(mutationId));
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "mutation/" + sanitizeTarget(mutationId), 1, payload);
            }
            case "ashfall.treatment_applied" -> {
                String treatment = stringValue(payload, "treatment");
                boolean runtimeMutation = booleanValue(payload, "runtimeMutation");
                boolean treatmentHadEffect = !runtimeMutation
                        || floatValue(payload, "radiationRemoved", 0.0F) > 0.0F;
                if (runtimeMutation && isRadAway(treatment)) {
                    treatmentHadEffect = applyRadAwayUse(player, payload, resultSnapshot);
                    changed |= treatmentHadEffect;
                }
                if (!runtimeMutation || treatmentHadEffect) {
                    changed |= markSpecial(player, "medical:" + sanitizeTarget(treatment));
                    changed |= recordMission(player, MissionObjectiveType.CUSTOM, "treatment/" + sanitizeTarget(treatment), 1, payload);
                }
                if ("rad_away".equals(treatment) || "radaway".equals(treatment)) {
                    if (!runtimeMutation || treatmentHadEffect) {
                        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:rad_away", 1, payload);
                        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:use_rad_away", 1, payload);
                        changed |= recordMission(player, MissionObjectiveType.DELIVER_ITEM, EchoCanonicalContentIds.ITEM_RAD_AWAY, 1, payload);
                    }
                }
            }
            case EchoCanonicalContentIds.EVENT_ASHFALL_MED_BAY_USED ->
                    changed |= applyMedBayUse(player, payload, resultSnapshot);
            case "ashfall.cleanser_used" -> {
                changed |= markSpecial(player, "machine:radiation_cleanser_used");
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "radiation_cleanser", 1, payload);
                changed |= recordMission(player, MissionObjectiveType.PLACE_BLOCK, "echoashfallprotocol:radiation_cleanser", 1, payload);
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:build_radiation_cleanser", 1, payload);
            }
            case "ashfall.scrubber_used" -> {
                changed |= markSpecial(player, "hazard:atmospheric_scrubber_used");
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "atmospheric_scrubber", 1, payload);
                changed |= recordMission(player, MissionObjectiveType.PLACE_BLOCK, "echoashfallprotocol:atmospheric_scrubber", 1, payload);
                changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:build_atmospheric_scrubber", 1, payload);
            }
            case "ashfall.lab_objective" -> {
                String objective = stringValue(payload, "objective");
                changed |= markSpecial(player, "lab:" + sanitizeTarget(objective));
                changed |= recordMission(player, MissionObjectiveType.UNLOCK_RESEARCH, "lab/" + sanitizeTarget(objective), 1, payload);
            }
            case "ashfall.vault_objective" -> changed |= applyHazardRouteObjective(player, payload);
            case "ashfall.hazard_route_check" -> changed |= applyHazardRouteCheck(player, payload);
            case "player.item_used" -> {
                if (booleanValue(payload, "filterCartridgeUse")) {
                    changed |= applyFilterCartridgeUse(player, payload, resultSnapshot);
                } else if (booleanValue(payload, "mutagenUse")) {
                    changed |= applyMutagenUse(player, payload, resultSnapshot);
                } else {
                    changed |= recordMission(player, MissionObjectiveType.CUSTOM, target, 1, payload);
                }
            }
            default -> changed |= recordMission(player, MissionObjectiveType.CUSTOM, event.eventId(), 1, payload);
        }

        if (changed) {
            CompoundTag playerData = player.getPersistentData();
            playerData.putString(LAST_EVENT_KEY, event.eventId());
            playerData.putLong(LAST_EVENT_TICK_KEY, context.gameTime());
        }

        resultSnapshot.put("eventId", event.eventId());
        resultSnapshot.put("target", target);
        resultSnapshot.put("playerId", player.getUUID().toString());
        resultSnapshot.put("nativeInterface", "EchoNativeRuntimeHost.Events");
        resultSnapshot.put("nativeMethod", "publish");
        resultSnapshot.put("realNativeStateMutated", changed);

        if ("FAILED".equals(stringValue(resultSnapshot, "resultStatus"))) {
            return NativeResult.failed(
                    "AdapterCore hazard runtime event attempted a mutation and failed.",
                    Map.copyOf(resultSnapshot));
        }

        return new NativeResult(changed, changed ? "MUTATED" : "NOOP",
                changed
                        ? "Published AdapterCore hazard runtime event and mutated state."
                        : "AdapterCore hazard runtime event was valid but no state change was needed.",
                Map.copyOf(resultSnapshot));
    }

    private static boolean applyRadiationChanged(ServerPlayer player, Map<String, Object> payload) {
        float delta = floatValue(payload, "delta", 0.0f);
        boolean changed = false;
        if (delta > 0.0f) {
            changed |= markSpecial(player, "hazard:radiation_exposure");
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "radiation/exposure", 1, payload);
            if (Boolean.TRUE.equals(payload.get("radiationZone"))) {
                changed |= markLocation(player, "biome", "radiation_zone");
                changed |= recordMission(player, MissionObjectiveType.ENTER_REGION, "radiation_zone", 1, payload);
            }
            return changed;
        }
        if (delta < 0.0f) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "radiation/decay", 1, payload);
        }
        return changed;
    }

    private static boolean applyMutagenUse(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        InteractionHand hand = interactionHandValue(payload);
        ItemStack mutagen = player.getItemInHand(hand);
        if (mutagen.isEmpty() || !mutagen.is(ModItems.MUTAGEN_VIAL.get())) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Mutagen use failed: no vial in hand."), true);
            resultSnapshot.put("mutagenApplied", false);
            resultSnapshot.put("resultStatus", "FAILED");
            resultSnapshot.put("failureReason", "missing mutagen vial in selected hand");
            resultSnapshot.put("hudOrEventEmitted", true);
            return false;
        }

        SurvivalData survivalData = player.getData(ModAttachments.SURVIVAL_DATA.get());
        float beforeRadiation = survivalData.getRadiationLevel();
        float scaledRadiation = RadiationHelper.scaleIncomingRadiation(
                player,
                Math.max(0.0F, floatValue(payload, "radiationAmount", 40.0F)));
        survivalData.addRadiation(scaledRadiation);
        float afterRadiation = survivalData.getRadiationLevel();
        boolean radiationChanged = afterRadiation != beforeRadiation;
        if (radiationChanged) {
            player.setData(ModAttachments.SURVIVAL_DATA.get(), survivalData);
            player.syncData(ModAttachments.SURVIVAL_DATA.get());
        }

        MutationData mutationDataBefore = player.getData(ModAttachments.MUTATION_DATA.get());
        int mutationCountBefore = mutationDataBefore.getMutationCount();
        int itemCountBefore = mutagen.getCount();
        boolean nauseaApplied = player.addEffect(new MobEffectInstance(
                MobEffects.NAUSEA,
                Math.max(1, numberValue(payload, "nauseaTicks", 200)),
                1,
                false,
                false));
        boolean weaknessApplied = player.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS,
                Math.max(1, numberValue(payload, "weaknessTicks", 200)),
                0,
                false,
                false));

        MutationManager.tryMutate(player, 100.0F);
        int mutationCountAfter = player.getData(ModAttachments.MUTATION_DATA.get()).getMutationCount();

        if (!player.getAbilities().instabuild) {
            mutagen.shrink(1);
        }
        boolean itemConsumed = mutagen.getCount() != itemCountBefore;
        player.sendSystemMessage(Component.literal(EchoMessages.getMessage(EchoMessages.Context.MUTAGEN_USED)));

        boolean changed = radiationChanged || nauseaApplied || weaknessApplied || itemConsumed
                || mutationCountAfter != mutationCountBefore;
        if (changed) {
            changed |= markSpecial(player, "mutation:mutagen_used");
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "mutation:mutagen_used", 1, payload);
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, EchoCanonicalContentIds.ITEM_MUTAGEN_VIAL, 1, payload);
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:use_mutagen_vial", 1, payload);
        }

        resultSnapshot.put("mutagenApplied", true);
        resultSnapshot.put("radiationBefore", beforeRadiation);
        resultSnapshot.put("radiationAfter", afterRadiation);
        resultSnapshot.put("radiationAdded", Math.max(0.0F, afterRadiation - beforeRadiation));
        resultSnapshot.put("mutationCountBefore", mutationCountBefore);
        resultSnapshot.put("mutationCountAfter", mutationCountAfter);
        resultSnapshot.put("nauseaApplied", nauseaApplied);
        resultSnapshot.put("weaknessApplied", weaknessApplied);
        resultSnapshot.put("itemConsumed", itemConsumed);
        resultSnapshot.put("hudOrEventEmitted", true);
        return changed;
    }

    private static boolean applyRadAwayUse(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        InteractionHand hand = interactionHandValue(payload);
        ItemStack dose = player.getItemInHand(hand);
        SurvivalData survivalData = player.getData(ModAttachments.SURVIVAL_DATA.get());
        float beforeRadiation = survivalData.getRadiationLevel();
        int doseCountBefore = dose.getCount();

        resultSnapshot.put("radAwayRuntime", true);
        resultSnapshot.put("hand", hand.name());
        resultSnapshot.put("itemId", stringValue(payload, "itemId"));
        resultSnapshot.put("radAwayCountBefore", doseCountBefore);
        resultSnapshot.put("radiationBefore", beforeRadiation);
        resultSnapshot.put("hasRegenerationBefore", player.hasEffect(MobEffects.REGENERATION));

        if (dose.isEmpty() || !dose.is(ModItems.RAD_AWAY.get())) {
            player.sendSystemMessage(Component.literal("[ECHO-7] RadAway use failed: no dose in hand."), true);
            resultSnapshot.put("resultStatus", "FAILED");
            resultSnapshot.put("failureReason", "missing RadAway dose in selected hand");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("radAwayCountAfter", dose.getCount());
            resultSnapshot.put("radiationAfter", beforeRadiation);
            resultSnapshot.put("hasRegenerationAfter", player.hasEffect(MobEffects.REGENERATION));
            return false;
        }

        if (beforeRadiation <= 0.0F) {
            player.sendSystemMessage(Component.literal("\u00A7e[ECHO-7]\u00A7r No measurable radiation detected. RadAway dose held."), true);
            resultSnapshot.put("noopReason", "no measurable radiation detected");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("radAwayCountAfter", dose.getCount());
            resultSnapshot.put("radiationAfter", beforeRadiation);
            resultSnapshot.put("hasRegenerationAfter", player.hasEffect(MobEffects.REGENERATION));
            return false;
        }

        float removalFraction = Math.max(0.0F, Math.min(1.0F, floatValue(payload, "radiationRemovalFraction", 0.5F)));
        survivalData.decayRadiation(beforeRadiation * removalFraction);
        float afterRadiation = survivalData.getRadiationLevel();
        if (afterRadiation >= beforeRadiation) {
            player.sendSystemMessage(Component.literal("\u00A7e[ECHO-7]\u00A7r RadAway dose could not bind to the current exposure state."), true);
            resultSnapshot.put("noopReason", "radiation treatment produced no measurable reduction");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("radAwayCountAfter", dose.getCount());
            resultSnapshot.put("radiationAfter", afterRadiation);
            resultSnapshot.put("hasRegenerationAfter", player.hasEffect(MobEffects.REGENERATION));
            return false;
        }

        player.setData(ModAttachments.SURVIVAL_DATA.get(), survivalData);
        player.syncData(ModAttachments.SURVIVAL_DATA.get());
        boolean regenerationApplied = player.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                Math.max(1, numberValue(payload, "regenerationTicks", 100)),
                Math.max(0, numberValue(payload, "regenerationAmplifier", 1)),
                false,
                false));
        if (!player.getAbilities().instabuild) {
            dose.shrink(1);
        }
        boolean itemConsumed = dose.getCount() != doseCountBefore;

        player.level().playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.5f, 0.9f);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3f, 0.6f);
        player.sendSystemMessage(Component.literal(EchoMessages.getMessage(EchoMessages.Context.RADAWAY_USED)));

        resultSnapshot.put("radiationAfter", afterRadiation);
        resultSnapshot.put("radiationRemoved", Math.max(0.0F, beforeRadiation - afterRadiation));
        resultSnapshot.put("regenerationApplied", regenerationApplied);
        resultSnapshot.put("hasRegenerationAfter", player.hasEffect(MobEffects.REGENERATION));
        resultSnapshot.put("itemConsumed", itemConsumed);
        resultSnapshot.put("radAwayCountAfter", dose.getCount());
        resultSnapshot.put("hudOrEventEmitted", true);
        return true;
    }

    private static boolean applyFilterCartridgeUse(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        InteractionHand hand = interactionHandValue(payload);
        ItemStack cartridge = player.getItemInHand(hand);
        String itemId = stringValue(payload, "itemId");
        SurvivalData survivalData = player.getData(ModAttachments.SURVIVAL_DATA.get());
        int beforeLife = survivalData.getAirFilterLife();
        int beforeTier = survivalData.getFilterTier();
        int cartridgeCountBefore = cartridge.getCount();
        resultSnapshot.put("filterCartridgeRuntime", true);
        resultSnapshot.put("hand", hand.name());
        resultSnapshot.put("itemId", itemId);
        resultSnapshot.put("headSlotItemId", itemId(player.getItemBySlot(EquipmentSlot.HEAD)));
        resultSnapshot.put("airFilterLifeBefore", beforeLife);
        resultSnapshot.put("filterTierBefore", beforeTier);
        resultSnapshot.put("cartridgeCountBefore", cartridgeCountBefore);

        if (cartridge.isEmpty() || !itemId.equals(itemId(cartridge))) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Filter Cartridge use failed: no matching cartridge in hand."), true);
            resultSnapshot.put("resultStatus", "FAILED");
            resultSnapshot.put("failureReason", "missing selected filter cartridge in hand");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("airFilterLifeAfter", beforeLife);
            resultSnapshot.put("filterTierAfter", beforeTier);
            resultSnapshot.put("cartridgeCountAfter", cartridge.getCount());
            return false;
        }

        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.GAS_MASK.get())) {
            player.sendSystemMessage(Component.literal(
                    "[ECHO-7] Gas Mask required. Equip a Gas Mask before installing filter cartridges."));
            resultSnapshot.put("resultStatus", "FAILED");
            resultSnapshot.put("failureReason", "missing equipped Gas Mask");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("airFilterLifeAfter", beforeLife);
            resultSnapshot.put("filterTierAfter", beforeTier);
            resultSnapshot.put("cartridgeCountAfter", cartridge.getCount());
            return false;
        }

        if (beforeLife >= SurvivalData.MAX_AIR_FILTER) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Air filter already at maximum capacity."));
            resultSnapshot.put("noopReason", "air filter already at maximum capacity");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("airFilterLifeAfter", beforeLife);
            resultSnapshot.put("filterTierAfter", beforeTier);
            resultSnapshot.put("cartridgeCountAfter", cartridge.getCount());
            return false;
        }

        int refillAmount = Math.max(0, numberValue(payload, "refillAmount", 0));
        int tier = Math.max(0, numberValue(payload, "filterTier", 0));
        survivalData.setAirFilterLife(Math.min(beforeLife + refillAmount, SurvivalData.MAX_AIR_FILTER));
        survivalData.setFilterTier(tier);
        int afterLife = survivalData.getAirFilterLife();
        int afterTier = survivalData.getFilterTier();
        if (afterLife == beforeLife && afterTier == beforeTier) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Filter cartridge produced no measurable refill."));
            resultSnapshot.put("noopReason", "filter cartridge produced no measurable state change");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("airFilterLifeAfter", afterLife);
            resultSnapshot.put("filterTierAfter", afterTier);
            resultSnapshot.put("cartridgeCountAfter", cartridge.getCount());
            return false;
        }

        player.setData(ModAttachments.SURVIVAL_DATA.get(), survivalData);
        player.syncData(ModAttachments.SURVIVAL_DATA.get());
        cartridge.shrink(1);

        boolean changed = true;
        changed |= markSpecial(player, "filter:cartridge_installed");
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "filter/cartridge_installed", 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:fix_mask_filter", 1, payload);
        changed |= recordMission(player, MissionObjectiveType.DELIVER_ITEM, itemId, 1, payload);
        int actualAdded = Math.max(0, afterLife - beforeLife);
        player.sendSystemMessage(Component.literal(String.format(
                "[ECHO-7] %s filter installed. Air capacity restored: +%d%%",
                stringValue(payload, "filterTierName"),
                (actualAdded * 100) / SurvivalData.MAX_AIR_FILTER)));
        resultSnapshot.put("airFilterLifeAfter", afterLife);
        resultSnapshot.put("filterTierAfter", afterTier);
        resultSnapshot.put("airFilterAdded", actualAdded);
        resultSnapshot.put("itemConsumed", cartridge.getCount() != cartridgeCountBefore);
        resultSnapshot.put("cartridgeCountAfter", cartridge.getCount());
        resultSnapshot.put("hudOrEventEmitted", true);
        return changed;
    }

    private static boolean applyMedBayUse(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        QuestData quest = QuestData.get(player);
        boolean markerBefore = quest.hasVisitedLocation("special", "medical:field_med_bay_used");
        boolean treatmentRecorded = false;
        if (!markerBefore) {
            quest.visitLocation("special", "medical:field_med_bay_used");
            QuestData.saveAndSync(player, quest);
            player.sendSystemMessage(Component.literal("\u00A7a[ECHO-7]\u00A7r Field Med Bay treatment pulse recorded."), true);
            treatmentRecorded = true;
        }

        boolean changed = treatmentRecorded;
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "medical:field_med_bay_used", 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:use_field_med_bay", 1, payload);

        resultSnapshot.put("medBayMarkerBefore", markerBefore);
        resultSnapshot.put("firstTreatmentRecorded", treatmentRecorded);
        resultSnapshot.put("medBayMarkerAfter", quest.hasVisitedLocation("special", "medical:field_med_bay_used"));
        resultSnapshot.put("energyStored", numberValue(payload, "energyStored", 0));
        resultSnapshot.put("mutationCount", numberValue(payload, "mutationCount", 0));
        return changed;
    }

    private static boolean applyHazardRouteObjective(ServerPlayer player, Map<String, Object> payload) {
        String siteId = sanitizeTarget(stringValue(payload, "siteId"));
        String route = sanitizeTarget(stringValue(payload, "route"));
        String target = stringValue(payload, "target");
        boolean changed = false;
        if (!siteId.isBlank()) {
            changed |= markLocation(player, "poi", siteId);
            changed |= recordMission(player, MissionObjectiveType.DISCOVER_STRUCTURE, "echoashfallprotocol:" + siteId, 1, payload);
        }
        if ("echoashfallprotocol:enter_bio_lab".equals(target)) {
            changed |= markSpecial(player, "hazard:bio_lab_entered");
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, target, 1, payload);
        } else if ("echoashfallprotocol:survey_reactor_ruin".equals(target)) {
            changed |= markSpecial(player, "hazard:reactor_ruin_surveyed");
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, target, 1, payload);
        } else if ("echoashfallprotocol:clear_military_vault".equals(target)) {
            changed |= markSpecial(player, "hazard:military_vault_surveyed");
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, target, 1, payload);
        } else if ("radiation_zone".equals(route) || "radiation_zone".equals(siteId)) {
            changed |= markLocation(player, "biome", "radiation_zone");
            changed |= recordMission(player, MissionObjectiveType.ENTER_REGION, "radiation_zone", 1, payload);
        }
        return changed;
    }

    private static boolean applyHazardRouteCheck(ServerPlayer player, Map<String, Object> payload) {
        String primaryHazard = sanitizeTarget(stringValue(payload, "primaryHazard"));
        boolean changed = false;
        if (!"none".equals(primaryHazard) && !primaryHazard.isBlank()) {
            changed |= markSpecial(player, "hazard:" + primaryHazard);
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "hazard/" + primaryHazard, 1, payload);
        }
        if (Boolean.TRUE.equals(payload.get("radiationZone"))) {
            changed |= markLocation(player, "biome", "radiation_zone");
            changed |= recordMission(player, MissionObjectiveType.ENTER_REGION, "radiation_zone", 1, payload);
        }
        return changed;
    }

    private static boolean markSpecial(ServerPlayer player, String marker) {
        return markLocation(player, "special", marker);
    }

    private static boolean markLocation(ServerPlayer player, String category, String marker) {
        if (marker == null || marker.isBlank()) {
            return false;
        }
        QuestData quest = QuestData.get(player);
        if (quest.hasVisitedLocation(category, marker)) {
            return false;
        }
        quest.visitLocation(category, marker);
        QuestData.saveAndSync(player, quest);
        return true;
    }

    private static boolean recordMission(
            ServerPlayer player,
            MissionObjectiveType type,
            String target,
            int count,
            Map<String, Object> payload) {
        Identifier targetId = targetId(target);
        if (targetId == null) {
            return false;
        }
        AshfallAdapterCoreRuntimeGuards.ensureMissionContentReady(player, "hazard");
        return EchoCoreServices.recordMissionObjective(
                player,
                type,
                targetId,
                Math.max(1, count),
                Map.of(
                        "source", EchoAshfallProtocol.MODID,
                        "adapterCoreEvent", String.valueOf(payload.getOrDefault("source", "hazard_runtime"))));
    }

    private static NativeMutationContext context(ServerPlayer player, String eventId) {
        String dimensionId = player.level() instanceof ServerLevel level
                ? level.dimension().identifier().toString()
                : "unknown";
        return new NativeMutationContext(
                EchoAshfallProtocol.MODID,
                dimensionId,
                "event." + eventId,
                "SERVER",
                player.level().getGameTime(),
                Map.of(
                        "nativeInterface", "EchoNativeRuntimeHost.Events",
                        "nativeMethod", "publish",
                        "hostRuntime", "native_loader",
                        "runtimeLane", "Native Loader",
                        "compatibilityFallback", "legacy_backend"));
    }

    private static String routeObjectiveTarget(String siteId, String route) {
        String normalizedSite = sanitizeTarget(siteId);
        String normalizedRoute = sanitizeTarget(route);
        if ("bio_lab".equals(normalizedSite) || "sporebound_sanctum".equals(normalizedSite)) {
            return "echoashfallprotocol:enter_bio_lab";
        }
        if ("reactor_ruin".equals(normalizedSite) || "relay_station_east".equals(normalizedSite)
                || "reactor_ruin".equals(normalizedRoute)) {
            return "echoashfallprotocol:survey_reactor_ruin";
        }
        if ("military_vault".equals(normalizedSite) || "radwarden_outpost".equals(normalizedSite)
                || "military_vault".equals(normalizedRoute)) {
            return "echoashfallprotocol:clear_military_vault";
        }
        if ("radiation_zone".equals(normalizedSite) || "radiation_zone".equals(normalizedRoute)) {
            return "echoashfallprotocol:scout_radiation_zone";
        }
        return "hazard_route/" + normalizedRoute;
    }

    private static Map<String, Object> hazardPayload(@Nullable HazardZoneManager.HazardSnapshot snapshot) {
        if (snapshot == null) {
            return Map.of(
                    "primaryHazard", "NONE",
                    "hazardSeverity", "NONE",
                    "hazardReason", "",
                    "radiationZone", false,
                    "safeZone", false);
        }
        return Map.ofEntries(
                Map.entry("primaryHazard", snapshot.primaryType().name()),
                Map.entry("hazardSeverity", snapshot.severity().name()),
                Map.entry("hazardReason", safe(snapshot.reason())),
                Map.entry("safeZone", snapshot.safeZone()),
                Map.entry("toxicAir", snapshot.toxicAir()),
                Map.entry("radiationZone", snapshot.radiationZone()),
                Map.entry("cryoCold", snapshot.cryoCold()),
                Map.entry("acidContact", snapshot.acidContact()),
                Map.entry("nexusAnomaly", snapshot.nexusAnomaly()),
                Map.entry("radiationStorm", snapshot.radiationStorm()),
                Map.entry("stormSheltered", snapshot.stormSheltered()),
                Map.entry("primaryIntensity", snapshot.primaryIntensity()),
                Map.entry("radiationIntensity", snapshot.radiationIntensity()));
    }

    private static Identifier targetId(String target) {
        if (target == null || target.isBlank()) {
            return null;
        }
        Identifier parsed = Identifier.tryParse(target);
        if (parsed != null) {
            return parsed;
        }
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, sanitizeTarget(target));
    }

    private static String sanitizeTarget(String target) {
        return target == null || target.isBlank()
                ? "unknown"
                : target.replace(':', '/').replace(' ', '_').toLowerCase(java.util.Locale.ROOT);
    }

    private static String stringValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static float floatValue(Map<String, Object> payload, String key, float fallback) {
        Object value = payload.get(key);
        return value instanceof Number number ? number.floatValue() : fallback;
    }

    private static int numberValue(Map<String, Object> payload, String key, int fallback) {
        Object value = payload.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static boolean booleanValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value instanceof Boolean bool && bool;
    }

    private static boolean isRadAway(String treatment) {
        return "rad_away".equals(treatment) || "radaway".equals(treatment);
    }

    private static InteractionHand interactionHandValue(Map<String, Object> payload) {
        try {
            return InteractionHand.valueOf(stringValue(payload, "hand"));
        } catch (IllegalArgumentException exception) {
            return InteractionHand.MAIN_HAND;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "minecraft:air";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static String itemId(Item item) {
        if (item == null) {
            return "minecraft:air";
        }
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static Map<String, Object> positionSnapshot(BlockPos pos) {
        if (pos == null) {
            return Map.of("x", 0, "y", 0, "z", 0);
        }
        return Map.of(
                "x", pos.getX(),
                "y", pos.getY(),
                "z", pos.getZ());
    }
}
