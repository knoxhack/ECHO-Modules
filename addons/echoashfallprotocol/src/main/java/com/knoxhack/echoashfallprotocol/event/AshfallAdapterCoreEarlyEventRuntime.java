package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echo.adaptercore.EchoCanonicalContentIds;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeAction;
import com.knoxhack.echo.adaptercore.EchoRuntimeActionDispatcher.EchoRuntimeActionOutcome;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostCapabilities;
import com.knoxhack.echo.adaptercore.EchoRuntimeHostRegistry;
import com.knoxhack.echo.adaptercore.EchoUnsupportedRuntimeHost;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeEvent;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationContext;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePlayerRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativePosition;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.IObjectiveView;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.echo.EchoMessages;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.survival.ColdData;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AshfallAdapterCoreEarlyEventRuntime {
    private static final String RUNTIME_HOST_ID = EchoAshfallProtocol.MODID + ":early_event_runtime";
    private static final String LAST_EVENT_KEY = "ashes_of_tomorrow.adaptercore.last_early_event";
    private static final String LAST_EVENT_TICK_KEY = "ashes_of_tomorrow.adaptercore.last_early_event_tick";
    private static final Identifier FIELD_MANUAL_MISSION_ID =
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "secure_crash_outpost");
    private static final EarlyEventRuntimeHost RUNTIME_HOST = new EarlyEventRuntimeHost();
    private static volatile boolean runtimeRegistered;

    private AshfallAdapterCoreEarlyEventRuntime() {
    }

    public static void onItemObtained(Object event) {
        if (!(eventValue(event, "getPlayer") instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = itemStackValue(event, "getOriginalStack");
        if (stack.isEmpty()) {
            return;
        }
        itemObtained(player, stack, "pickup");
    }

    public static void onRecipeCrafted(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = itemStackValue(event, "getCrafting");
        if (stack.isEmpty()) {
            return;
        }
        recipeCrafted(player, stack);
        if (stack.is(ModItems.CLEAN_WATER_BOTTLE.get())) {
            waterFiltered(player, "recipe_crafted");
        }
    }

    public static void onItemConsumed(Object event) {
        if (!(eventValue(event, "getEntity") instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = itemStackValue(event, "getItem");
        if (stack.isEmpty()) {
            return;
        }
        itemConsumed(player, stack);
    }

    public static void onPlayerWakeUp(Object event) {
        if (eventValue(event, "getEntity") instanceof ServerPlayer player) {
            shelterSlept(player, booleanValue(event, "wakeImmediately"), booleanValue(event, "updateLevel"));
        }
    }

    public static NativeResult itemObtained(ServerPlayer player, ItemStack stack, String source) {
        String id = itemId(stack);
        return publish(player, EchoCanonicalContentIds.EVENT_PLAYER_ITEM_COLLECTED, Map.of(
                "target", itemId(stack),
                "item", id,
                "itemId", id,
                "count", Math.max(1, stack.getCount()),
                "source", source));
    }

    public static NativeResult itemConsumed(ServerPlayer player, ItemStack stack) {
        return itemUsed(player, stack, "living_entity_use_item_finish", Map.of());
    }

    public static NativeResult handWarmerUsed(
            ServerPlayer player,
            ItemStack stack,
            InteractionHand hand,
            int warmthDelta) {
        return itemUsed(player, stack, "hand_warmer_item_use", Map.of(
                "handWarmerUse", true,
                "hand", hand == null ? InteractionHand.MAIN_HAND.name() : hand.name(),
                "warmthDelta", Math.max(1, warmthDelta),
                "marker", "cold:warmed_up"));
    }

    public static NativeResult waterBottleDrunk(
            ServerPlayer player,
            ItemStack stack,
            int hydrationDelta,
            int foodNutrition,
            float foodSaturation,
            boolean nausea) {
        return waterBottleDrunk(
                player,
                stack,
                InteractionHand.MAIN_HAND,
                hydrationDelta,
                foodNutrition,
                foodSaturation,
                nausea);
    }

    public static NativeResult waterBottleDrunk(
            ServerPlayer player,
            ItemStack stack,
            InteractionHand hand,
            int hydrationDelta,
            int foodNutrition,
            float foodSaturation,
            boolean nausea) {
        return itemUsed(player, stack, "water_bottle_finish", Map.of(
                "waterBottleUse", true,
                "hand", handName(hand),
                "hydrationDelta", Math.max(0, hydrationDelta),
                "foodNutrition", Math.max(0, foodNutrition),
                "foodSaturation", Math.max(0.0F, foodSaturation),
                "nauseaTicks", nausea ? 100 : 0));
    }

    public static NativeResult waterBottleNoBenefit(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        return itemUsed(player, stack, "water_bottle_no_benefit", Map.of(
                "waterBottleUse", true,
                "waterBottleNoBenefit", true,
                "hand", handName(hand)));
    }

    public static NativeResult crudeFilterUsed(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        return itemUsed(player, stack, "crude_filter_item_use", Map.of(
                "crudeFilterUse", true,
                "hand", hand == null ? InteractionHand.MAIN_HAND.name() : hand.name(),
                "inputItem", EchoCanonicalContentIds.ITEM_DIRTY_WATER_BOTTLE,
                "outputItem", EchoCanonicalContentIds.ITEM_FILTERED_WATER_BOTTLE,
                "filterDamageDelta", 1,
                "marker", "water:emergency_filtered"));
    }

    public static NativeResult gasMaskUsed(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        return itemUsed(player, stack, "gas_mask_item_use", Map.of(
                "gasMaskUse", true,
                "hand", handName(hand),
                "marker", "equipment:gas_mask_equipped"));
    }

    public static NativeResult fieldManualUsed(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        return itemUsed(player, stack, "field_manual_item_use", Map.of(
                "fieldManualUse", true,
                "hand", handName(hand),
                "marker", "tutorial:read_field_manual"));
    }

    public static NativeResult bandageUsed(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        return itemUsed(player, stack, "bandage_item_use", Map.of(
                "bandageUse", true,
                "hand", hand == null ? InteractionHand.MAIN_HAND.name() : hand.name(),
                "healAmount", 2.0F,
                "marker", "medical:bandage_used"));
    }

    public static NativeResult stimPackUsed(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        return itemUsed(player, stack, "stim_pack_item_use", Map.of(
                "stimPackUse", true,
                "hand", hand == null ? InteractionHand.MAIN_HAND.name() : hand.name(),
                "marker", "medical:stim_pack_used",
                "regenerationTicks", 160,
                "speedTicks", 100));
    }

    private static NativeResult itemUsed(
            ServerPlayer player,
            ItemStack stack,
            String source,
            Map<String, Object> extraPayload) {
        String id = itemId(stack);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", id);
        payload.put("item", id);
        payload.put("itemId", id);
        payload.put("count", 1);
        payload.put("source", source);
        if (extraPayload != null) {
            payload.putAll(extraPayload);
        }
        return publish(player, EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED, payload);
    }

    public static NativeResult recipeCrafted(ServerPlayer player, ItemStack stack) {
        return publish(player, "player.recipe_crafted", Map.of(
                "target", itemId(stack),
                "count", Math.max(1, stack.getCount()),
                "source", "item_crafted_event"));
    }

    public static NativeResult blockPlaced(ServerPlayer player, Identifier blockId, BlockPos pos) {
        if (blockId == null) {
            return new NativeResult(false, "SKIPPED_INVALID_BLOCK",
                    "AdapterCore block-placement event skipped for missing block id.", Map.of(
                    "realNativeStateMutated", false));
        }
        return publish(player, "player.block_placed", Map.of(
                "target", blockId.toString(),
                "block", blockId.toString(),
                "blockId", blockId.toString(),
                "count", 1,
                "pos", positionSnapshot(pos)), pos, true);
    }

    public static NativeResult dirtyWaterCollected(ServerPlayer player, BlockPos pos) {
        return specialMarker(player, "water:dirty_collected", Map.of(
                "source", "dirty_water_collected",
                "pos", positionSnapshot(pos)));
    }

    public static NativeResult waterFiltered(ServerPlayer player, String source) {
        return specialMarker(player, "water:emergency_filtered", Map.of("source", source));
    }

    public static NativeResult shelterSlept(ServerPlayer player, boolean wakeImmediately, boolean updateLevel) {
        return publish(player, "player.shelter_slept", Map.of(
                "target", "ashfall:sleep_shelter",
                "marker", "shelter:slept",
                "wakeImmediately", wakeImmediately,
                "updateLevel", updateLevel));
    }

    public static NativeResult specialMarker(ServerPlayer player, String marker, Map<String, Object> payload) {
        Map<String, Object> eventPayload = new LinkedHashMap<>();
        eventPayload.put("target", marker);
        eventPayload.put("count", 1);
        eventPayload.put("marker", marker);
        if (payload != null) {
            eventPayload.putAll(payload);
        }
        return publish(player, "ashfall.special_marker", eventPayload, null, true);
    }

    private static NativeResult publish(ServerPlayer player, String eventId, Map<String, Object> payload) {
        return publish(player, eventId, payload, null, false);
    }

    private static NativeResult publish(
            ServerPlayer player,
            String eventId,
            Map<String, Object> payload,
            BlockPos requiredLoadedPos,
            boolean dedupeSameTick) {
        Optional<NativeResult> guard = AshfallAdapterCoreRuntimeGuards.guardPublish(
                player,
                "early_event",
                eventId,
                payload,
                requiredLoadedPos,
                dedupeSameTick);
        if (guard.isPresent()) {
            NativeResult guardedResult = guard.get();
            if (player != null) {
                ledgerGuardedResult(player, eventId, payload, requiredLoadedPos, guardedResult);
            }
            return guardedResult;
        }
        ensureRuntimeRegistered();
        NativeMutationContext mutationContext = context(player, eventId);
        NativeEvent event = new NativeEvent(eventId, new NativePlayerRef(player.getUUID().toString()), payload);
        EchoRuntimeAction action = new EchoRuntimeAction(
                eventId,
                RUNTIME_HOST_ID,
                payload,
                event.player(),
                mutationContext.dimensionId(),
                playerPosition(player, mutationContext.dimensionId()),
                blockRef(requiredLoadedPos, mutationContext.dimensionId()),
                mutationContext);
        return EchoRuntimeActionDispatcher.global().dispatch(action, (host, dispatchedAction) -> {
            EarlyEventRuntimeHost earlyHost = (EarlyEventRuntimeHost) host;
            Map<String, Object> before = mutationSummary(player, eventId, payload, "before");
            NativeResult rawResult = earlyHost.publishForPlayer(player, event, mutationContext);
            NativeResult result = recordNativeLoaderBackendEvent(player, event, rawResult);
            if (result == null) {
                result = rawResult;
            }
            Map<String, Object> after = mutationSummary(player, eventId, payload, "after");
            return EchoRuntimeActionOutcome.of(before, result, after, result.mutated(), hudOrEventEmitted(result));
        });
    }

    private static NativeResult recordNativeLoaderBackendEvent(
            ServerPlayer player,
            NativeEvent event,
            NativeResult result) {
        if (result == null) {
            return null;
        }
        try {
            NativeLoaderEchoRuntimeHost nativeHost = NativeLoaderRuntimeHostFactory.createBackendFirst();
            return nativeHost.recordExternalRuntimeEvent(event, result);
        } catch (Throwable throwable) {
            Map<String, Object> snapshot = new LinkedHashMap<>(result.snapshot());
            snapshot.put("nativeLoaderBackendAttached", false);
            snapshot.put("nativeLoaderBackendCallAttempted", true);
            snapshot.put("nativeLoaderBackendCallFailure", true);
            snapshot.put("nativeLoaderRuntimeHostId", NativeLoaderEchoRuntimeHost.RUNTIME_HOST_ID);
            snapshot.put("nativeLoaderBridgeFailureClass", throwable.getClass().getName());
            snapshot.put("nativeLoaderBridgeFailureMessage",
                    throwable.getMessage() == null ? throwable.getClass().getName() : throwable.getMessage());
            return new NativeResult(result.mutated(), result.status(), result.message(), Map.copyOf(snapshot));
        }
    }

    private static NativeResult apply(ServerPlayer player, NativeEvent event, NativeMutationContext context) {
        Map<String, Object> payload = event.payload();
        String target = stringValue(payload, "target");
        int count = numberValue(payload, "count", 1);

        boolean changed = false;
        Map<String, Object> resultSnapshot = new LinkedHashMap<>();

        switch (event.eventId()) {
            case "player.item_obtained", "player.item_collected" -> {
                changed |= recordMission(player, MissionObjectiveType.OBTAIN_ITEM, target, count, payload);
                changed |= recordMission(player, MissionObjectiveType.DELIVER_ITEM, target, count, payload);
                changed |= recordEarlyInventoryPredicates(player, payload);
            }
            case "player.item_consumed", "player.item_used" -> {
                if (booleanValue(payload, "crudeFilterUse")) {
                    changed |= applyCrudeFilterUse(player, payload, resultSnapshot);
                } else if (booleanValue(payload, "gasMaskUse")
                        || EchoCanonicalContentIds.ITEM_GAS_MASK.equals(target)) {
                    changed |= applyGasMaskUse(player, payload, resultSnapshot);
                } else if (booleanValue(payload, "fieldManualUse")
                        || EchoCanonicalContentIds.ITEM_FIELD_MANUAL.equals(target)) {
                    changed |= applyFieldManualUse(player, payload, resultSnapshot);
                } else if (booleanValue(payload, "bandageUse")) {
                    changed |= applyBandageUse(player, payload, resultSnapshot);
                } else if (booleanValue(payload, "stimPackUse")) {
                    changed |= applyStimPackUse(player, payload, resultSnapshot);
                } else if (booleanValue(payload, "handWarmerUse")
                        || EchoCanonicalContentIds.ITEM_HAND_WARMER.equals(target)) {
                    changed |= applyHandWarmerUse(player, payload, resultSnapshot);
                } else if (booleanValue(payload, "waterBottleUse")) {
                    changed |= applyWaterBottleUse(player, target, payload, resultSnapshot);
                } else {
                    changed |= recordMission(player, MissionObjectiveType.CUSTOM, target, 1, payload);
                    changed |= recordMission(player, MissionObjectiveType.CUSTOM, "consume/" + sanitizeTarget(target), 1, payload);
                    if ("echoashfallprotocol:clean_water_bottle".equals(target)) {
                        changed |= recordMission(player, MissionObjectiveType.OBTAIN_ITEM, target, 1, payload);
                    }
                }
            }
            case "player.recipe_crafted" -> {
                changed |= recordMission(player, MissionObjectiveType.CRAFT_ITEM, target, count, payload);
                changed |= recordMission(player, MissionObjectiveType.OBTAIN_ITEM, target, count, payload);
                changed |= recordMission(player, MissionObjectiveType.DELIVER_ITEM, target, count, payload);
                changed |= recordEarlyInventoryPredicates(player, payload);
            }
            case "player.block_placed" -> {
                // MissionBlockPlaceTracker owns PLACE_BLOCK counts; this write closes the AdapterCore event bridge.
            }
            case "player.shelter_slept" -> {
                if (markSpecial(player, "shelter:slept")) {
                    changed = true;
                    changed |= recordMission(player, MissionObjectiveType.CUSTOM, "ashfall:sleep_shelter", 1, payload);
                }
            }
            case "ashfall.special_marker" -> {
                String marker = stringValue(payload, "marker");
                if (markSpecial(player, marker)) {
                    changed = true;
                    changed |= recordMission(player, MissionObjectiveType.CUSTOM, marker, 1, payload);
                    changed |= recordMission(player, MissionObjectiveType.ENTER_REGION, marker, 1, payload);
                }
            }
            default -> changed |= recordMission(player, MissionObjectiveType.CUSTOM, event.eventId(), count, payload);
        }

        if (changed) {
            CompoundTag playerData = player.getPersistentData();
            playerData.putString(LAST_EVENT_KEY, event.eventId());
            playerData.putLong(LAST_EVENT_TICK_KEY, context.gameTime());
        }

        resultSnapshot.put("eventId", event.eventId());
        resultSnapshot.put("target", target);
        resultSnapshot.put("count", count);
        resultSnapshot.put("playerId", player.getUUID().toString());
        resultSnapshot.put("nativeInterface", "EchoNativeRuntimeHost.Events");
        resultSnapshot.put("nativeMethod", "publish");
        resultSnapshot.put("realNativeStateMutated", changed);
        if ("FAILED".equals(stringValue(resultSnapshot, "resultStatus"))) {
            return NativeResult.failed(
                    "AdapterCore early gameplay event attempted a mutation and failed.",
                    Map.copyOf(resultSnapshot));
        }
        return new NativeResult(changed, changed ? "MUTATED" : "NOOP",
                changed
                        ? "Published AdapterCore early gameplay event and mutated state."
                        : "AdapterCore early gameplay event was valid but no state change was needed.",
                Map.copyOf(resultSnapshot));
    }

    private static boolean markSpecial(ServerPlayer player, String marker) {
        if (marker == null || marker.isBlank()) {
            return false;
        }
        QuestData quest = QuestData.get(player);
        if (quest.hasVisitedLocation("special", marker)) {
            return false;
        }
        quest.visitLocation("special", marker);
        QuestData.saveAndSync(player, quest);
        return true;
    }

    private static boolean applyHandWarmerUse(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        InteractionHand hand = interactionHandValue(payload);
        ItemStack warmer = player.getItemInHand(hand);
        ColdData coldData = ColdData.get(player);
        int before = coldData.getTemperature();
        int countBefore = warmer.getCount();
        resultSnapshot.put("handWarmerRuntime", true);
        resultSnapshot.put("hand", hand.name());
        resultSnapshot.put("coldTemperatureBefore", before);
        resultSnapshot.put("handWarmerCountBefore", countBefore);

        if (warmer.isEmpty() || !warmer.is(ModItems.HAND_WARMER.get())) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Hand Warmer use failed: no warmer in hand."), true);
            resultSnapshot.put("resultStatus", "FAILED");
            resultSnapshot.put("failureReason", "missing Hand Warmer in selected hand");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("coldTemperatureAfter", before);
            resultSnapshot.put("handWarmerCountAfter", warmer.getCount());
            return false;
        }

        coldData.addTemperature(numberValue(payload, "warmthDelta", 25));
        int after = coldData.getTemperature();
        if (before == after) {
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.hand_warmer.noop")
                    .withStyle(ChatFormatting.GRAY));
            resultSnapshot.put("noopReason", "body temperature is already stable");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("coldTemperatureAfter", after);
            resultSnapshot.put("handWarmerCountAfter", warmer.getCount());
            return false;
        }

        player.setData(ModAttachments.COLD_DATA.get(), coldData);
        if (!player.getAbilities().instabuild) {
            warmer.shrink(1);
        }
        player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.hand_warmer.activated")
                .withStyle(ChatFormatting.AQUA));

        boolean changed = true;
        changed |= markSpecial(player, "cold:warmed_up");
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "cold:warmed_up", 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:use_hand_warmer", 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:warm_up_after_exposure", 1, payload);
        changed |= recordMission(player, MissionObjectiveType.DELIVER_ITEM, EchoCanonicalContentIds.ITEM_HAND_WARMER, 1, payload);
        resultSnapshot.put("coldTemperatureAfter", after);
        resultSnapshot.put("handWarmerCountAfter", warmer.getCount());
        resultSnapshot.put("itemConsumed", warmer.getCount() != countBefore);
        resultSnapshot.put("hudOrEventEmitted", true);
        return changed;
    }

    private static boolean applyGasMaskUse(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        InteractionHand hand = interactionHandValue(payload);
        ItemStack mask = player.getItemInHand(hand);
        ItemStack headBefore = player.getItemBySlot(EquipmentSlot.HEAD).copy();
        ItemStack handBefore = mask.copy();
        resultSnapshot.put("gasMaskRuntime", true);
        resultSnapshot.put("hand", hand.name());
        resultSnapshot.put("headSlotBefore", itemId(headBefore));
        resultSnapshot.put("handItemBefore", itemId(handBefore));
        resultSnapshot.put("handCountBefore", handBefore.getCount());

        if (mask.isEmpty() || !mask.is(ModItems.GAS_MASK.get())) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Gas Mask equip failed: no mask in selected hand."), true);
            resultSnapshot.put("resultStatus", "FAILED");
            resultSnapshot.put("failureReason", "missing Gas Mask in selected hand");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("headSlotAfter", itemId(player.getItemBySlot(EquipmentSlot.HEAD)));
            resultSnapshot.put("handItemAfter", itemId(player.getItemInHand(hand)));
            resultSnapshot.put("handCountAfter", player.getItemInHand(hand).getCount());
            return false;
        }

        if (headBefore.isEmpty()) {
            ItemStack equipped = mask.copy();
            equipped.setCount(1);
            player.setItemSlot(EquipmentSlot.HEAD, equipped);
            mask.shrink(1);
        } else {
            ItemStack equipped = mask.copy();
            equipped.setCount(1);
            player.setItemSlot(EquipmentSlot.HEAD, equipped);
            player.setItemInHand(hand, headBefore);
        }

        player.level().playSound(null, player.blockPosition(),
                SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.sendSystemMessage(Component.literal("[ECHO-7] Gas mask seal confirmed."));

        boolean inventoryChanged = !sameStack(headBefore, player.getItemBySlot(EquipmentSlot.HEAD))
                || !sameStack(handBefore, player.getItemInHand(hand));
        boolean changed = inventoryChanged;
        changed |= markSpecial(player, "equipment:gas_mask_equipped");
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, EchoCanonicalContentIds.ITEM_GAS_MASK, 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:equip_gas_mask", 1, payload);
        resultSnapshot.put("headSlotAfter", itemId(player.getItemBySlot(EquipmentSlot.HEAD)));
        resultSnapshot.put("handItemAfter", itemId(player.getItemInHand(hand)));
        resultSnapshot.put("handCountAfter", player.getItemInHand(hand).getCount());
        resultSnapshot.put("equipmentChanged", inventoryChanged);
        resultSnapshot.put("hudOrEventEmitted", true);
        return changed;
    }

    private static boolean applyFieldManualUse(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        InteractionHand hand = interactionHandValue(payload);
        ItemStack manual = player.getItemInHand(hand);
        int manualCountBefore = manual.getCount();
        resultSnapshot.put("fieldManualRuntime", true);
        resultSnapshot.put("hand", hand.name());
        resultSnapshot.put("fieldManualCountBefore", countItem(player, ModItems.FIELD_MANUAL.get()));

        if (manual.isEmpty() || !manual.is(ModItems.FIELD_MANUAL.get())) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Field Manual read failed: no manual in selected hand."), true);
            resultSnapshot.put("resultStatus", "FAILED");
            resultSnapshot.put("failureReason", "missing Field Manual in selected hand");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("fieldManualCountAfter", countItem(player, ModItems.FIELD_MANUAL.get()));
            return false;
        }

        boolean alreadyRecorded = fieldManualAlreadyRecorded(player);
        boolean advanced = !alreadyRecorded && AshfallAdapterCoreMissionTriggerRuntime.itemUsed(
                player,
                EchoCanonicalContentIds.ITEM_FIELD_MANUAL);
        player.sendSystemMessage(Component.translatable(
                advanced
                        ? "message.EchoAshfallProtocol.field_manual.read"
                        : "message.EchoAshfallProtocol.field_manual.read_again")
                .withStyle(advanced ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        if (!advanced) {
            resultSnapshot.put("noopReason", alreadyRecorded
                    ? "field manual route objective already recorded"
                    : "field manual route objective unavailable");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("fieldManualCountAfter", countItem(player, ModItems.FIELD_MANUAL.get()));
            return false;
        }

        if (!player.getAbilities().instabuild) {
            manual.shrink(1);
        }
        resultSnapshot.put("missionAdvanced", true);
        resultSnapshot.put("itemConsumed", manual.getCount() != manualCountBefore);
        resultSnapshot.put("fieldManualCountAfter", countItem(player, ModItems.FIELD_MANUAL.get()));
        resultSnapshot.put("hudOrEventEmitted", true);
        return true;
    }

    private static boolean fieldManualAlreadyRecorded(ServerPlayer player) {
        if (!EchoCoreServices.missionCoreAvailable()) {
            return false;
        }
        try {
            return EchoCoreServices.missionService()
                    .mission(player, FIELD_MANUAL_MISSION_ID)
                    .stream()
                    .flatMap(mission -> mission.objectives().stream())
                    .filter(objective -> objective.id().getPath().endsWith("/read_field_manual"))
                    .anyMatch(IObjectiveView::complete);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static boolean applyWaterBottleUse(
            ServerPlayer player,
            String target,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        boolean changed = false;
        InteractionHand hand = interactionHandValue(payload);
        ItemStack water = player.getItemInHand(hand);
        resultSnapshot.put("waterBottleRuntime", true);
        resultSnapshot.put("hand", hand.name());
        resultSnapshot.put("itemId", target);
        resultSnapshot.put("waterBottleCountBefore", water.getCount());
        resultSnapshot.put("glassBottleCountBefore", countItem(player, Items.GLASS_BOTTLE));
        resultSnapshot.put("hydrationBefore", player.getData(ModAttachments.SURVIVAL_DATA.get()).getHydration());
        resultSnapshot.put("foodLevelBefore", player.getFoodData().getFoodLevel());
        resultSnapshot.put("foodSaturationBefore", player.getFoodData().getSaturationLevel());

        if (water.isEmpty() || !target.equals(itemId(water))) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Water bottle use failed: no matching bottle in hand."), true);
            resultSnapshot.put("resultStatus", "FAILED");
            resultSnapshot.put("failureReason", "missing selected water bottle in hand");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("waterBottleCountAfter", water.getCount());
            resultSnapshot.put("glassBottleCountAfter", countItem(player, Items.GLASS_BOTTLE));
            resultSnapshot.put("hydrationAfter", player.getData(ModAttachments.SURVIVAL_DATA.get()).getHydration());
            resultSnapshot.put("foodLevelAfter", player.getFoodData().getFoodLevel());
            resultSnapshot.put("foodSaturationAfter", player.getFoodData().getSaturationLevel());
            return false;
        }

        if (booleanValue(payload, "waterBottleNoBenefit")) {
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.water.no_benefit"));
            resultSnapshot.put("noopReason", "hydration and hunger are already stable");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("waterBottleCountAfter", water.getCount());
            resultSnapshot.put("glassBottleCountAfter", countItem(player, Items.GLASS_BOTTLE));
            resultSnapshot.put("hydrationAfter", player.getData(ModAttachments.SURVIVAL_DATA.get()).getHydration());
            resultSnapshot.put("foodLevelAfter", player.getFoodData().getFoodLevel());
            resultSnapshot.put("foodSaturationAfter", player.getFoodData().getSaturationLevel());
            return false;
        }

        int hydrationDelta = numberValue(payload, "hydrationDelta", 0);
        if (hydrationDelta > 0) {
            SurvivalData survivalData = player.getData(ModAttachments.SURVIVAL_DATA.get());
            int before = survivalData.getHydration();
            survivalData.addHydration(hydrationDelta);
            int after = survivalData.getHydration();
            if (before != after) {
                player.setData(ModAttachments.SURVIVAL_DATA.get(), survivalData);
                player.syncData(ModAttachments.SURVIVAL_DATA.get());
                changed = true;
            }
        }

        int foodNutrition = numberValue(payload, "foodNutrition", 0);
        if (foodNutrition > 0 && player.getFoodData().needsFood()) {
            int beforeFood = player.getFoodData().getFoodLevel();
            float beforeSaturation = player.getFoodData().getSaturationLevel();
            player.getFoodData().eat(foodNutrition, floatValue(payload, "foodSaturation", 0.0F));
            if (player.getFoodData().getFoodLevel() != beforeFood
                    || Float.compare(player.getFoodData().getSaturationLevel(), beforeSaturation) != 0) {
                changed = true;
            }
        }

        int nauseaTicks = numberValue(payload, "nauseaTicks", 0);
        if (nauseaTicks > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, nauseaTicks, 0));
            changed = true;
        }

        if (EchoCanonicalContentIds.ITEM_CLEAN_WATER_BOTTLE.equals(target)) {
            changed |= markSpecial(player, "water:clean_consumed");
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:drink_clean_water", 1, payload);
        }

        if (!changed) {
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.water.no_benefit"));
            resultSnapshot.put("noopReason", "water bottle produced no measurable state change");
            resultSnapshot.put("hudOrEventEmitted", true);
            resultSnapshot.put("waterBottleCountAfter", water.getCount());
            resultSnapshot.put("glassBottleCountAfter", countItem(player, Items.GLASS_BOTTLE));
            resultSnapshot.put("hydrationAfter", player.getData(ModAttachments.SURVIVAL_DATA.get()).getHydration());
            resultSnapshot.put("foodLevelAfter", player.getFoodData().getFoodLevel());
            resultSnapshot.put("foodSaturationAfter", player.getFoodData().getSaturationLevel());
            return false;
        }

        changed |= recordMission(player, MissionObjectiveType.CUSTOM, target, 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "consume/" + sanitizeTarget(target), 1, payload);
        if (EchoCanonicalContentIds.ITEM_CLEAN_WATER_BOTTLE.equals(target)) {
            changed |= recordMission(player, MissionObjectiveType.OBTAIN_ITEM, target, 1, payload);
        }

        boolean bottleConverted = convertWaterBottleToGlass(player, hand, water);
        String feedbackKey = waterFeedbackKey(target);
        if (!feedbackKey.isBlank()) {
            player.sendSystemMessage(Component.translatable(feedbackKey));
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK.value(),
                SoundSource.PLAYERS, 0.5F, 1.0F);

        resultSnapshot.put("waterBottleConverted", bottleConverted);
        resultSnapshot.put("waterBottleCountAfter", countItem(player, waterItem(target)));
        resultSnapshot.put("glassBottleCountAfter", countItem(player, Items.GLASS_BOTTLE));
        resultSnapshot.put("hydrationAfter", player.getData(ModAttachments.SURVIVAL_DATA.get()).getHydration());
        resultSnapshot.put("foodLevelAfter", player.getFoodData().getFoodLevel());
        resultSnapshot.put("foodSaturationAfter", player.getFoodData().getSaturationLevel());
        resultSnapshot.put("nauseaApplied", nauseaTicks > 0);
        resultSnapshot.put("hudOrEventEmitted", true);
        return changed;
    }

    private static boolean convertWaterBottleToGlass(ServerPlayer player, InteractionHand hand, ItemStack water) {
        if (player.getAbilities().instabuild) {
            return false;
        }
        if (water.getCount() <= 1) {
            player.setItemInHand(hand, new ItemStack(Items.GLASS_BOTTLE));
            return true;
        }
        water.shrink(1);
        ItemStack glassBottle = new ItemStack(Items.GLASS_BOTTLE);
        if (!player.getInventory().add(glassBottle)) {
            player.drop(glassBottle, false);
        }
        return true;
    }

    private static Item waterItem(String itemId) {
        return switch (itemId) {
            case EchoCanonicalContentIds.ITEM_CLEAN_WATER_BOTTLE -> ModItems.CLEAN_WATER_BOTTLE.get();
            case EchoCanonicalContentIds.ITEM_DIRTY_WATER_BOTTLE -> ModItems.DIRTY_WATER_BOTTLE.get();
            case EchoCanonicalContentIds.ITEM_FILTERED_WATER_BOTTLE -> ModItems.FILTERED_WATER_BOTTLE.get();
            case EchoCanonicalContentIds.ITEM_BOILED_WATER_BOTTLE -> ModItems.BOILED_WATER_BOTTLE.get();
            default -> Items.AIR;
        };
    }

    private static String waterFeedbackKey(String itemId) {
        return switch (itemId) {
            case EchoCanonicalContentIds.ITEM_CLEAN_WATER_BOTTLE -> "message.EchoAshfallProtocol.water.clean_consumed";
            case EchoCanonicalContentIds.ITEM_FILTERED_WATER_BOTTLE -> "message.EchoAshfallProtocol.water.filtered_consumed";
            case EchoCanonicalContentIds.ITEM_BOILED_WATER_BOTTLE -> "message.EchoAshfallProtocol.water.boiled_consumed";
            default -> "";
        };
    }

    private static boolean applyCrudeFilterUse(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        int dirtySlot = findInventorySlot(player, ModItems.DIRTY_WATER_BOTTLE.get());
        if (dirtySlot < 0) {
            player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.filter.no_dirty_water"));
            resultSnapshot.put("crudeFilterApplied", false);
            resultSnapshot.put("failureReason", "no dirty water in inventory");
            resultSnapshot.put("hudOrEventEmitted", true);
            return false;
        }

        ItemStack dirtyWater = player.getInventory().getItem(dirtySlot);
        dirtyWater.shrink(1);
        ItemStack filteredWater = new ItemStack(ModItems.FILTERED_WATER_BOTTLE.get(), 1);
        if (dirtyWater.isEmpty()) {
            player.getInventory().setItem(dirtySlot, filteredWater);
        } else if (!player.getInventory().add(filteredWater)) {
            player.drop(filteredWater, false);
        }

        InteractionHand hand = interactionHandValue(payload);
        ItemStack filter = player.getItemInHand(hand);
        if (!filter.isEmpty()) {
            filter.hurtAndBreak(Math.max(1, numberValue(payload, "filterDamageDelta", 1)), player, hand);
        }

        boolean changed = true;
        changed |= markSpecial(player, "water:emergency_filtered");
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "water:emergency_filtered", 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:secure_emergency_water_loop", 1, payload);
        changed |= recordMission(player, MissionObjectiveType.OBTAIN_ITEM,
                EchoCanonicalContentIds.ITEM_FILTERED_WATER_BOTTLE, 1, payload);
        changed |= recordMission(player, MissionObjectiveType.DELIVER_ITEM,
                EchoCanonicalContentIds.ITEM_FILTERED_WATER_BOTTLE, 1, payload);
        player.sendSystemMessage(Component.translatable("message.EchoAshfallProtocol.filter.crude_used"));
        resultSnapshot.put("crudeFilterApplied", true);
        resultSnapshot.put("hudOrEventEmitted", true);
        return changed;
    }

    private static boolean applyBandageUse(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        InteractionHand hand = interactionHandValue(payload);
        ItemStack bandage = player.getItemInHand(hand);
        if (bandage.isEmpty() || !bandage.is(ModItems.BANDAGE.get())) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Bandage use failed: no bandage in hand."), true);
            resultSnapshot.put("bandageApplied", false);
            resultSnapshot.put("failureReason", "missing bandage in selected hand");
            resultSnapshot.put("hudOrEventEmitted", true);
            return false;
        }

        boolean hadPoison = player.hasEffect(MobEffects.POISON);
        float beforeHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        if (!hadPoison && beforeHealth >= maxHealth) {
            player.sendSystemMessage(Component.literal("[ECHO-7] No treatable wounds detected. Bandage held."), true);
            resultSnapshot.put("bandageApplied", false);
            resultSnapshot.put("reason", "no treatable wounds or poison");
            resultSnapshot.put("hudOrEventEmitted", true);
            return false;
        }

        if (hadPoison) {
            player.removeEffect(MobEffects.POISON);
        }
        if (beforeHealth < maxHealth) {
            player.heal(Math.max(0.0F, floatValue(payload, "healAmount", 2.0F)));
        }
        if (!player.getAbilities().instabuild) {
            bandage.shrink(1);
        }

        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 0.8F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4F, 1.5F);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    4, 0.3D, 0.3D, 0.3D, 0.1D);
        }
        player.sendSystemMessage(Component.literal(EchoMessages.getMessage(EchoMessages.Context.BANDAGE_USED)));

        boolean changed = true;
        changed |= markSpecial(player, "medical:bandage_used");
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "medical:bandage_used", 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, EchoCanonicalContentIds.ITEM_BANDAGE, 1, payload);
        resultSnapshot.put("bandageApplied", true);
        resultSnapshot.put("poisonRemoved", hadPoison);
        resultSnapshot.put("healthBefore", beforeHealth);
        resultSnapshot.put("healthAfter", player.getHealth());
        resultSnapshot.put("hudOrEventEmitted", true);
        return changed;
    }

    private static boolean applyStimPackUse(
            ServerPlayer player,
            Map<String, Object> payload,
            Map<String, Object> resultSnapshot) {
        InteractionHand hand = interactionHandValue(payload);
        ItemStack stimPack = player.getItemInHand(hand);
        if (stimPack.isEmpty() || !stimPack.is(ModItems.STIM_PACK.get())) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Stim Pack use failed: no dose in hand."), true);
            resultSnapshot.put("stimPackApplied", false);
            resultSnapshot.put("failureReason", "missing stim pack in selected hand");
            resultSnapshot.put("hudOrEventEmitted", true);
            return false;
        }

        int regenerationTicks = Math.max(1, numberValue(payload, "regenerationTicks", 160));
        int speedTicks = Math.max(1, numberValue(payload, "speedTicks", 100));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenerationTicks, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, speedTicks, 0, false, true));
        if (!player.getAbilities().instabuild) {
            stimPack.shrink(1);
        }

        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.3F, 1.8F);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    8, 0.4D, 0.4D, 0.4D, 0.1D);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    player.getX(), player.getY() + 0.5D, player.getZ(),
                    6, 0.3D, 0.5D, 0.3D, 0.1D);
        }
        player.sendSystemMessage(Component.literal(EchoMessages.getMessage(EchoMessages.Context.STIMPAK_USED)));

        boolean changed = true;
        changed |= markSpecial(player, "medical:stim_pack_used");
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, EchoCanonicalContentIds.ITEM_STIM_PACK, 1, payload);
        changed |= recordMission(player, MissionObjectiveType.CUSTOM, "echoashfallprotocol:use_stim_pack", 1, payload);
        resultSnapshot.put("stimPackApplied", true);
        resultSnapshot.put("regenerationTicks", regenerationTicks);
        resultSnapshot.put("speedTicks", speedTicks);
        resultSnapshot.put("hudOrEventEmitted", true);
        return changed;
    }

    private static boolean hudOrEventEmitted(NativeResult result) {
        if (result == null) {
            return false;
        }
        return result.mutated() || Boolean.TRUE.equals(result.snapshot().get("hudOrEventEmitted"));
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
        AshfallAdapterCoreRuntimeGuards.ensureMissionContentReady(player, "early_event");
        return EchoCoreServices.recordMissionObjective(
                player,
                type,
                targetId,
                Math.max(1, count),
                Map.of(
                        "source", EchoAshfallProtocol.MODID,
                        "adapterCoreEvent", String.valueOf(payload.getOrDefault("source", "early_event_runtime"))));
    }

    private static boolean recordEarlyInventoryPredicates(ServerPlayer player, Map<String, Object> payload) {
        boolean changed = false;
        if (hasAny(player, ModItems.EMERGENCY_RATION.get(), ModItems.WILD_BERRY.get())) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "ashfall:food_buffer", 1, payload);
        }
        if (countItem(player, ModItems.EMERGENCY_RATION.get()) >= 4
                || countItem(player, ModItems.WILD_BERRY.get()) >= 12) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "ashfall:ration_buffer", 1, payload);
        }
        if (hasAll(player, ModItems.ASHBONE_SHIV.get(), ModItems.SCAVENGER_SPEAR.get(), ModItems.HIDE_WRAP.get())) {
            changed |= recordMission(player, MissionObjectiveType.CUSTOM, "ashfall:wasteland_field_kit", 1, payload);
        }
        return changed;
    }

    private static boolean hasAny(ServerPlayer player, Item... items) {
        for (Item item : items) {
            if (countItem(player, item) > 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAll(ServerPlayer player, Item... items) {
        for (Item item : items) {
            if (countItem(player, item) <= 0) {
                return false;
            }
        }
        return true;
    }

    private static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int findInventorySlot(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                return slot;
            }
        }
        return -1;
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
                        "compatibilityFallback", "legacy_event_bus"));
    }

    private static void ensureRuntimeRegistered() {
        if (runtimeRegistered) {
            return;
        }
        synchronized (AshfallAdapterCoreEarlyEventRuntime.class) {
            if (runtimeRegistered) {
                return;
            }
            EchoRuntimeHostRegistry.global().register(RUNTIME_HOST, new EchoRuntimeHostCapabilities(
                    RUNTIME_HOST_ID,
                    Set.of("EchoNativeRuntimeHost.Events"),
                    Set.of(
                            EchoCanonicalContentIds.EVENT_PLAYER_ITEM_OBTAINED,
                            EchoCanonicalContentIds.EVENT_PLAYER_ITEM_CONSUMED,
                            EchoCanonicalContentIds.EVENT_PLAYER_RECIPE_CRAFTED,
                            EchoCanonicalContentIds.EVENT_PLAYER_BLOCK_PLACED,
                            "player.shelter_slept",
                            EchoCanonicalContentIds.EVENT_ASHFALL_SPECIAL_MARKER),
                    Set.of(
                            EchoCanonicalContentIds.ITEM_CLEAN_WATER_BOTTLE,
                            EchoCanonicalContentIds.ITEM_DIRTY_WATER_BOTTLE,
                            EchoCanonicalContentIds.ITEM_FILTERED_WATER_BOTTLE,
                            EchoCanonicalContentIds.ITEM_BOILED_WATER_BOTTLE,
                            EchoCanonicalContentIds.ITEM_HAND_WARMER,
                            EchoCanonicalContentIds.ITEM_CRUDE_FILTER,
                            EchoCanonicalContentIds.ITEM_BANDAGE,
                            EchoCanonicalContentIds.ITEM_STIM_PACK,
                            "ashfall:food_buffer",
                            "ashfall:ration_buffer",
                            "ashfall:wasteland_field_kit"),
                    true,
                    true,
                    true));
            runtimeRegistered = true;
        }
    }

    private static NativePosition playerPosition(ServerPlayer player, String dimensionId) {
        Vec3 position = player.position();
        return new NativePosition(
                dimensionId,
                position.x(),
                position.y(),
                position.z(),
                player.getYRot(),
                player.getXRot());
    }

    private static NativeBlockRef blockRef(BlockPos pos, String dimensionId) {
        return pos == null ? null : new NativeBlockRef(dimensionId, pos.getX(), pos.getY(), pos.getZ());
    }

    private static void ledgerGuardedResult(
            ServerPlayer player,
            String eventId,
            Map<String, Object> payload,
            BlockPos requiredLoadedPos,
            NativeResult result) {
        String dimensionId = player.level() instanceof ServerLevel level
                ? level.dimension().identifier().toString()
                : "unknown";
        com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().append(
                eventId,
                RUNTIME_HOST_ID,
                payload,
                new com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationTarget(
                        new NativePlayerRef(player.getUUID().toString()),
                        dimensionId,
                        playerPosition(player, dimensionId),
                        blockRef(requiredLoadedPos, dimensionId)),
                mutationSummary(player, eventId, payload, "before_guard"),
                mutationSummary(player, eventId, payload, "after_guard"),
                result,
                false,
                false);
    }

    private static Map<String, Object> mutationSummary(
            ServerPlayer player,
            String eventId,
            Map<String, Object> payload,
            String phase) {
        CompoundTag playerData = player.getPersistentData();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("phase", phase);
        summary.put("eventId", eventId);
        summary.put("target", stringValue(payload, "target"));
        summary.put("marker", stringValue(payload, "marker"));
        summary.put("lastEvent", playerData.getStringOr(LAST_EVENT_KEY, ""));
        summary.put("lastEventTick", playerData.getLongOr(LAST_EVENT_TICK_KEY, Long.MIN_VALUE));
        summary.put("gameTime", player.level().getGameTime());
        summary.put("survivalHydration", player.getData(ModAttachments.SURVIVAL_DATA.get()).getHydration());
        summary.put("coldTemperature", ColdData.get(player).getTemperature());
        summary.put("foodLevel", player.getFoodData().getFoodLevel());
        summary.put("foodSaturation", player.getFoodData().getSaturationLevel());
        summary.put("cleanWaterCount", countItem(player, ModItems.CLEAN_WATER_BOTTLE.get()));
        summary.put("dirtyWaterCount", countItem(player, ModItems.DIRTY_WATER_BOTTLE.get()));
        summary.put("filteredWaterCount", countItem(player, ModItems.FILTERED_WATER_BOTTLE.get()));
        summary.put("boiledWaterCount", countItem(player, ModItems.BOILED_WATER_BOTTLE.get()));
        summary.put("glassBottleCount", countItem(player, Items.GLASS_BOTTLE));
        summary.put("health", player.getHealth());
        summary.put("maxHealth", player.getMaxHealth());
        summary.put("hasPoison", player.hasEffect(MobEffects.POISON));
        summary.put("hasRegeneration", player.hasEffect(MobEffects.REGENERATION));
        summary.put("hasSpeed", player.hasEffect(MobEffects.SPEED));
        summary.put("bandageCount", countItem(player, ModItems.BANDAGE.get()));
        summary.put("stimPackCount", countItem(player, ModItems.STIM_PACK.get()));
        summary.put("handWarmerCount", countItem(player, ModItems.HAND_WARMER.get()));
        summary.put("gasMaskInventoryCount", countItem(player, ModItems.GAS_MASK.get()));
        summary.put("fieldManualCount", countItem(player, ModItems.FIELD_MANUAL.get()));
        summary.put("headSlotItemId", itemId(player.getItemBySlot(EquipmentSlot.HEAD)));
        summary.put("headSlotDamage", player.getItemBySlot(EquipmentSlot.HEAD).getDamageValue());
        summary.put("headSlotCount", player.getItemBySlot(EquipmentSlot.HEAD).getCount());
        summary.put("mainHandItemId", itemId(player.getMainHandItem()));
        summary.put("mainHandCount", player.getMainHandItem().getCount());
        summary.put("mainHandDamage", player.getMainHandItem().getDamageValue());
        return Map.copyOf(summary);
    }

    private static final class EarlyEventRuntimeHost extends EchoUnsupportedRuntimeHost {
        private final ThreadLocal<ServerPlayer> activePlayer = new ThreadLocal<>();

        EarlyEventRuntimeHost() {
            super(RUNTIME_HOST_ID);
        }

        NativeResult publishForPlayer(ServerPlayer player, NativeEvent event, NativeMutationContext context) {
            activePlayer.set(player);
            try {
                return events().publish(event, context);
            } finally {
                activePlayer.remove();
            }
        }

        @Override
        public EchoNativeRuntimeHost.Events events() {
            return (event, context) -> {
                ServerPlayer player = activePlayer.get();
                if (player == null) {
                    return NativeResult.unsupported("Early-event runtime requires a live server player target.", Map.of(
                            "runtimeHostId", runtimeHostId(),
                            "nativeInterface", "EchoNativeRuntimeHost.Events",
                            "nativeMethod", "publish",
                            "eventId", event == null ? "" : event.eventId(),
                            "failureReason", "missing live server player target"));
                }
                return apply(player, event, context);
            };
        }
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
                : target.replace(':', '/').replace(' ', '_');
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static boolean sameStack(ItemStack first, ItemStack second) {
        return first.getCount() == second.getCount()
                && ItemStack.isSameItemSameComponents(first, second);
    }

    private static String stringValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static int numberValue(Map<String, Object> payload, String key, int fallback) {
        Object value = payload.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static float floatValue(Map<String, Object> payload, String key, float fallback) {
        Object value = payload.get(key);
        return value instanceof Number number ? number.floatValue() : fallback;
    }

    private static boolean booleanValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value instanceof Boolean bool ? bool : Boolean.parseBoolean(String.valueOf(value));
    }

    private static boolean booleanValue(Object event, String methodName) {
        Object value = eventValue(event, methodName);
        return value instanceof Boolean bool && bool;
    }

    private static ItemStack itemStackValue(Object event, String methodName) {
        Object value = eventValue(event, methodName);
        return value instanceof ItemStack stack ? stack : ItemStack.EMPTY;
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

    private static InteractionHand interactionHandValue(Map<String, Object> payload) {
        try {
            return InteractionHand.valueOf(stringValue(payload, "hand"));
        } catch (IllegalArgumentException exception) {
            return InteractionHand.MAIN_HAND;
        }
    }

    private static String handName(InteractionHand hand) {
        return hand == null ? InteractionHand.MAIN_HAND.name() : hand.name();
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
