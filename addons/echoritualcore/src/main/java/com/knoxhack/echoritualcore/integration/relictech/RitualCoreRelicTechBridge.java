package com.knoxhack.echoritualcore.integration.relictech;

import com.knoxhack.echorelictech.api.RelicTechApi;
import com.knoxhack.echorelictech.api.event.RelicTechEvents;
import com.knoxhack.echorelictech.api.relic.RelicCondition;
import com.knoxhack.echorelictech.api.relic.RelicInstanceData;
import com.knoxhack.echorelictech.registry.ModDataComponents;
import com.knoxhack.echoritualcore.block.entity.BasicAltarBlockEntity;
import com.knoxhack.echoritualcore.api.RitualCoreApi;
import com.knoxhack.echoritualcore.api.RitualCoreEvents;
import com.knoxhack.echoritualcore.registry.ModItems;
import com.knoxhack.echoritualcore.ritual.RitualExecutionContext;
import com.knoxhack.echoritualcore.ritual.RitualItemAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class RitualCoreRelicTechBridge {
    private RitualCoreRelicTechBridge() {
    }

    public static boolean tryPerform(ServerPlayer player, ItemStack focus, Identifier ritualId) {
        return tryPerform(player, focus, ritualId, RitualExecutionContext.create(player, player.blockPosition()));
    }

    public static boolean tryPerform(ServerPlayer player, ItemStack focus, Identifier ritualId, RitualExecutionContext context) {
        if (!RelicTechApi.isRelic(focus)) {
            return false;
        }
        if (RitualCoreApi.RELIC_STABILIZATION.equals(ritualId)) {
            return stabilizeRelic(context, focus);
        }
        if (RitualCoreApi.CURSE_CLEANSING_I.equals(ritualId)) {
            return cleanseRelicCurse(context, focus);
        }
        return false;
    }

    private static boolean stabilizeRelic(RitualExecutionContext context, ItemStack relic) {
        ServerPlayer player = context.player();
        RelicInstanceData data = relic.get(ModDataComponents.RELIC_DATA.get());
        if (data == null || !data.identified() || data.condition() == RelicCondition.UNKNOWN) {
            fail(context, RitualCoreApi.RELIC_STABILIZATION, data, "unidentified",
                    "block.echoritualcore.basic_altar.relic_unidentified");
            return true;
        }
        if (!context.readyForIgnition(RitualCoreApi.RELIC_STABILIZATION)) {
            fail(context, RitualCoreApi.RELIC_STABILIZATION, data, "missing_structure",
                    "block.echoritualcore.basic_altar.missing_structure");
            return true;
        }
        if (data.condition() == RelicCondition.STABILIZED || data.condition() == RelicCondition.CONTAINED) {
            fail(context, RitualCoreApi.RELIC_STABILIZATION, data, "already_stable",
                    "block.echoritualcore.basic_altar.relic_already_stable");
            return true;
        }
        int seals = data.condition() == RelicCondition.CORRUPTED ? 2 : 1;
        RitualItemAccess items = context.items(relic);
        if (!items.has(ModItems.STABILITY_SEAL.get(), seals)) {
            fail(context, RitualCoreApi.RELIC_STABILIZATION, data, "missing_stability_seal",
                    "block.echoritualcore.basic_altar.missing_stability_seal");
            return true;
        }
        items.consume(ModItems.STABILITY_SEAL.get(), seals);
        RelicCondition previous = data.condition();
        relic.set(ModDataComponents.RELIC_DATA.get(), cleanState(data, RelicCondition.STABILIZED, 120));
        RelicTechEvents.fireWorkbench(player, relic, previous, RelicCondition.STABILIZED);
        RitualCoreApi.complete(context, RitualCoreApi.RELIC_STABILIZATION, data.relicId(), relic,
                "Relic Stabilization complete. Lifecycle risk collapsed into a stable state.");
        player.sendSystemMessage(Component.translatable("block.echoritualcore.basic_altar.relic_stabilized"));
        return true;
    }

    private static boolean cleanseRelicCurse(RitualExecutionContext context, ItemStack relic) {
        ServerPlayer player = context.player();
        RelicInstanceData data = relic.get(ModDataComponents.RELIC_DATA.get());
        if (data == null || !data.identified()) {
            fail(context, RitualCoreApi.CURSE_CLEANSING_I, data, "unidentified",
                    "block.echoritualcore.basic_altar.relic_unidentified");
            return true;
        }
        if (!context.readyForIgnition(RitualCoreApi.CURSE_CLEANSING_I)) {
            fail(context, RitualCoreApi.CURSE_CLEANSING_I, data, "missing_structure",
                    "block.echoritualcore.basic_altar.missing_structure");
            return true;
        }
        boolean activeCurse = data.condition() == RelicCondition.CORRUPTED || data.corruptionFlag() || data.overclockFlag();
        if (!activeCurse) {
            fail(context, RitualCoreApi.CURSE_CLEANSING_I, data, "no_active_curse",
                    "block.echoritualcore.basic_altar.no_active_curse");
            return true;
        }
        RitualItemAccess items = context.items(relic);
        if (!items.has(ModItems.PURITY_CATALYST.get(), 1)) {
            fail(context, RitualCoreApi.CURSE_CLEANSING_I, data, "missing_purity_catalyst",
                    "block.echoritualcore.basic_altar.missing_purity_catalyst");
            return true;
        }
        items.consume(ModItems.PURITY_CATALYST.get(), 1);
        RelicCondition previous = data.condition();
        RelicCondition target = previous == RelicCondition.CORRUPTED ? RelicCondition.DAMAGED : previous;
        relic.set(ModDataComponents.RELIC_DATA.get(), cleanState(data, target, 80));
        if (previous != target) {
            RelicTechEvents.fireWorkbench(player, relic, previous, target);
        }
        RitualCoreApi.complete(context, RitualCoreApi.CURSE_CLEANSING_I, data.relicId(), relic,
                "Curse Cleansing I complete. Relic curse signature grounded.");
        RitualCoreApi.recordMission(player, Identifier.fromNamespaceAndPath("echoritualcore", "relic_curse_cleansed"),
                RitualCoreApi.CURSE_CLEANSING_I, "curse_cleansing_i");
        player.sendSystemMessage(Component.translatable("block.echoritualcore.basic_altar.relic_cleansed"));
        return true;
    }

    private static RelicInstanceData cleanState(RelicInstanceData data, RelicCondition target, int cooldown) {
        return new RelicInstanceData(
                data.relicId(),
                target,
                Math.max(0, data.instabilityModifier() - 1),
                data.boundPos(),
                data.boundDimension(),
                data.charge(),
                false,
                false,
                data.containmentFlag() && target == RelicCondition.CONTAINED,
                data.identified(),
                cooldown);
    }

    private static void fail(RitualExecutionContext context, Identifier ritualId, RelicInstanceData data, String reason, String langKey) {
        Identifier subject = data == null ? Identifier.fromNamespaceAndPath("echorelictech", "unknown") : data.relicId();
        context.updateAltar(ritualId, subject, BasicAltarBlockEntity.RESULT_FAILURE, reason);
        RitualCoreEvents.fireFailure(context.player(), ritualId, subject, reason, context.altarPos());
        if ("missing_structure".equals(reason)) {
            context.player().sendSystemMessage(Component.translatable(langKey,
                    String.join(", ", context.structure().missingAnchors())));
        } else {
            context.player().sendSystemMessage(Component.translatable(langKey));
        }
    }
}
