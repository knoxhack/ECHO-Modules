package com.knoxhack.echoritualcore.api;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoritualcore.EchoRitualCore;
import com.knoxhack.echoritualcore.block.entity.BasicAltarBlockEntity;
import com.knoxhack.echoritualcore.registry.ModItems;
import com.knoxhack.echoritualcore.ritual.RitualCoreMapMarkers;
import com.knoxhack.echoritualcore.ritual.RitualExecutionContext;
import com.knoxhack.echoritualcore.ritual.RitualItemAccess;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class RitualCoreApi {
    public static final Identifier AETHER_CALIBRATION = EchoRitualCore.id("aether_calibration");
    public static final Identifier RELIC_STABILIZATION = EchoRitualCore.id("relic_stabilization");
    public static final Identifier CURSE_CLEANSING_I = EchoRitualCore.id("curse_cleansing_i");
    public static final Identifier SPELL_CORE_AWAKENING = EchoRitualCore.id("spell_core_awakening");
    public static final Identifier RIFT_CRACK_REVEAL = EchoRitualCore.id("rift_crack_reveal");

    private RitualCoreApi() {
    }

    public static boolean tryAltarRitual(ServerPlayer player, ItemStack focus, boolean alternateMode) {
        return tryAltarRitual(player, focus, alternateMode, player.blockPosition());
    }

    public static boolean tryAltarRitual(ServerPlayer player, ItemStack focus, boolean alternateMode, BlockPos altarPos) {
        RitualExecutionContext context = RitualExecutionContext.create(player, altarPos);
        Identifier ritualId = alternateMode ? CURSE_CLEANSING_I : RELIC_STABILIZATION;
        if (focus == null || focus.isEmpty()) {
            player.sendSystemMessage(Component.translatable("block.echoritualcore.basic_altar.empty_focus"));
            context.updateAltar(ritualId, null, BasicAltarBlockEntity.RESULT_WARNING, "Focus required.");
            return false;
        }
        if (EchoRuntimeModules.isLoaded("echorelictech") && tryRelicTechBridge(player, focus, ritualId, context)) {
            return true;
        }
        if (alternateMode && EchoRuntimeModules.isLoaded("echocursecore") && tryCurseCoreBridge(player, focus, context)) {
            return true;
        }
        if (!alternateMode && focus.is(ModItems.AETHER_CHALK.get())) {
            return performAetherCalibration(context, focus);
        }
        if (!alternateMode && focus.is(ModItems.RITUAL_FOCUS.get())) {
            return performSpellCoreAwakening(context, focus);
        }
        if (!alternateMode && focus.is(ModItems.REFINED_AETHER_SAMPLE.get())) {
            return performRiftCrackReveal(context, focus);
        }
        player.sendSystemMessage(Component.translatable("block.echoritualcore.basic_altar.no_matching_ritual"));
        context.updateAltar(ritualId, Identifier.fromNamespaceAndPath("minecraft", "air"),
                BasicAltarBlockEntity.RESULT_FAILURE, "No valid ritual accepted this focus.");
        RitualCoreEvents.fireFailure(player, ritualId, Identifier.fromNamespaceAndPath("minecraft", "air"),
                "no_matching_ritual", context.altarPos());
        return false;
    }

    private static boolean performAetherCalibration(RitualExecutionContext context, ItemStack focus) {
        Identifier ritualId = AETHER_CALIBRATION;
        if (!requireStructure(context, ritualId)) {
            return true;
        }
        RitualItemAccess items = context.items(focus);
        if (!items.consume(ModItems.AETHER_CHALK.get(), 1)) {
            fail(context, ritualId, "missing_aether_chalk",
                    "block.echoritualcore.basic_altar.missing_aether_chalk");
            return true;
        }
        give(context.player(), new ItemStack(ModItems.REFINED_AETHER_SAMPLE.get()));
        complete(context, ritualId, ritualId, focus, "Aether Calibration complete. Refined sample condensed.");
        context.player().sendSystemMessage(Component.translatable("block.echoritualcore.basic_altar.aether_calibrated"));
        return true;
    }

    private static boolean performSpellCoreAwakening(RitualExecutionContext context, ItemStack focus) {
        Identifier ritualId = SPELL_CORE_AWAKENING;
        if (!requireStructure(context, ritualId)) {
            return true;
        }
        RitualItemAccess items = context.items(focus);
        if (!items.has(ModItems.RITUAL_FOCUS.get(), 1) || !items.has(ModItems.REFINED_AETHER_SAMPLE.get(), 1)) {
            fail(context, ritualId, "missing_spell_core_inputs",
                    "block.echoritualcore.basic_altar.missing_spell_core_inputs");
            return true;
        }
        items.consume(ModItems.RITUAL_FOCUS.get(), 1);
        items.consume(ModItems.REFINED_AETHER_SAMPLE.get(), 1);
        give(context.player(), new ItemStack(ModItems.AWAKENED_SPELL_CORE.get()));
        complete(context, ritualId, ritualId, focus, "Spell Core Awakening complete. Awakened core recovered.");
        context.player().sendSystemMessage(Component.translatable("block.echoritualcore.basic_altar.spell_core_awakened"));
        return true;
    }

    private static boolean performRiftCrackReveal(RitualExecutionContext context, ItemStack focus) {
        Identifier ritualId = RIFT_CRACK_REVEAL;
        if (!requireStructure(context, ritualId)) {
            return true;
        }
        RitualItemAccess items = context.items(focus);
        if (!items.has(ModItems.REFINED_AETHER_SAMPLE.get(), 1) || !items.has(ModItems.AETHER_CHALK.get(), 1)) {
            fail(context, ritualId, "missing_rift_reveal_inputs",
                    "block.echoritualcore.basic_altar.missing_rift_reveal_inputs");
            return true;
        }
        items.consume(ModItems.REFINED_AETHER_SAMPLE.get(), 1);
        items.consume(ModItems.AETHER_CHALK.get(), 1);
        RitualCoreMapMarkers.recordRiftHint(context.player(), context.altarPos());
        complete(context, ritualId, ritualId, focus, "Rift Crack Reveal complete. HoloMap trace generated.");
        context.player().sendSystemMessage(Component.translatable("block.echoritualcore.basic_altar.rift_trace_revealed"));
        return true;
    }

    private static boolean requireStructure(RitualExecutionContext context, Identifier ritualId) {
        if (context.readyForIgnition(ritualId)) {
            return true;
        }
        context.player().sendSystemMessage(Component.translatable("block.echoritualcore.basic_altar.missing_structure",
                String.join(", ", context.structure().missingAnchors())));
        RitualCoreEvents.fireFailure(context.player(), ritualId, ritualId, "missing_structure", context.altarPos());
        return false;
    }

    private static void fail(RitualExecutionContext context, Identifier ritualId, String reason, String langKey) {
        context.updateAltar(ritualId, ritualId, BasicAltarBlockEntity.RESULT_FAILURE, reason);
        RitualCoreEvents.fireFailure(context.player(), ritualId, ritualId, reason, context.altarPos());
        context.player().sendSystemMessage(Component.translatable(langKey));
    }

    public static void complete(RitualExecutionContext context, Identifier ritualId, Identifier subjectId, ItemStack focus,
            String message) {
        context.updateAltar(ritualId, subjectId, BasicAltarBlockEntity.RESULT_COMPLETE, message);
        RitualCoreMapMarkers.recordRitualSite(context.player(), context.altarPos(), ritualId,
                titleFor(ritualId), message);
        RitualCoreEvents.fireComplete(context.player(), ritualId, subjectId, focus, context.altarPos());
        recordMission(context.player(), ritualId, ritualId, ritualId.getPath());
    }

    public static boolean hasItem(ServerPlayer player, Item item, int count) {
        if (count <= 0 || player.getAbilities().instabuild) {
            return true;
        }
        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                found += stack.getCount();
            }
        }
        return found >= count;
    }

    public static void consumeItem(ServerPlayer player, Item item, int count) {
        if (count <= 0 || player.getAbilities().instabuild) {
            return;
        }
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    public static void recordMission(ServerPlayer player, Identifier target, Identifier ritualId, String action) {
        com.echoplatform.echocore.api.EchoCoreServices.recordMissionObjective(
                player,
                com.echoplatform.echocore.api.mission.MissionObjectiveType.CUSTOM,
                target,
                1,
                Map.of("source", EchoRitualCore.MODID, "ritual", ritualId.toString(), "action", action));
    }

    private static void give(ServerPlayer player, ItemStack output) {
        if (output.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(output)) {
            player.drop(output, false);
        }
    }

    private static String titleFor(Identifier ritualId) {
        if (AETHER_CALIBRATION.equals(ritualId)) {
            return "Aether Calibration";
        }
        if (SPELL_CORE_AWAKENING.equals(ritualId)) {
            return "Spell Core Awakening";
        }
        if (RIFT_CRACK_REVEAL.equals(ritualId)) {
            return "Rift Crack Reveal";
        }
        if (CURSE_CLEANSING_I.equals(ritualId)) {
            return "Curse Cleansing I";
        }
        if (RELIC_STABILIZATION.equals(ritualId)) {
            return "Relic Stabilization";
        }
        return "RitualCore";
    }

    private static boolean tryRelicTechBridge(ServerPlayer player, ItemStack focus, Identifier ritualId,
            RitualExecutionContext context) {
        try {
            Class<?> bridge = Class.forName("com.knoxhack.echoritualcore.integration.relictech.RitualCoreRelicTechBridge");
            Object result = bridge.getMethod("tryPerform", ServerPlayer.class, ItemStack.class, Identifier.class,
                            RitualExecutionContext.class)
                    .invoke(null, player, focus, ritualId, context);
            return Boolean.TRUE.equals(result);
        } catch (ClassNotFoundException exception) {
            EchoRitualCore.LOGGER.debug("RelicTech ritual bridge class is not present.");
        } catch (NoSuchMethodException exception) {
            try {
                Class<?> bridge = Class.forName("com.knoxhack.echoritualcore.integration.relictech.RitualCoreRelicTechBridge");
                Object result = bridge.getMethod("tryPerform", ServerPlayer.class, ItemStack.class, Identifier.class)
                        .invoke(null, player, focus, ritualId);
                return Boolean.TRUE.equals(result);
            } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException
                    | NoClassDefFoundError nested) {
                EchoRitualCore.LOGGER.warn("Legacy RelicTech ritual bridge failed.", nested);
            }
        } catch (NoClassDefFoundError exception) {
            EchoRitualCore.LOGGER.debug("RelicTech ritual bridge skipped because an optional class is absent.");
        } catch (IllegalAccessException | InvocationTargetException exception) {
            EchoRitualCore.LOGGER.warn("RelicTech ritual bridge failed.", exception);
        }
        return false;
    }

    private static boolean tryCurseCoreBridge(ServerPlayer player, ItemStack focus, RitualExecutionContext context) {
        Identifier ritualId = CURSE_CLEANSING_I;
        if (!requireStructure(context, ritualId)) {
            return true;
        }
        RitualItemAccess items = context.items(focus);
        if (!items.has(ModItems.PURITY_CATALYST.get(), 1)) {
            fail(context, ritualId, "missing_purity_catalyst",
                    "block.echoritualcore.basic_altar.missing_purity_catalyst");
            return true;
        }
        try {
            Object result = Class.forName("com.knoxhack.echocursecore.api.CurseCoreApi")
                    .getMethod("cleanseFirstMinorCurse", ServerPlayer.class)
                    .invoke(null, player);
            if (!Boolean.TRUE.equals(result)) {
                fail(context, ritualId, "no_active_player_curse",
                        "block.echoritualcore.basic_altar.no_active_curse");
                return true;
            }
            items.consume(ModItems.PURITY_CATALYST.get(), 1);
            complete(context, ritualId, Identifier.fromNamespaceAndPath("echocursecore", "curse_cleansed"),
                    focus, "Curse Cleansing I complete. Player curse stage reduced.");
            player.sendSystemMessage(Component.translatable("block.echoritualcore.basic_altar.player_curse_cleansed"));
            return true;
        } catch (ClassNotFoundException exception) {
            EchoRitualCore.LOGGER.debug("CurseCore ritual bridge class is not present.");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NoClassDefFoundError exception) {
            EchoRitualCore.LOGGER.warn("CurseCore ritual bridge failed.", exception);
        }
        return false;
    }
}
