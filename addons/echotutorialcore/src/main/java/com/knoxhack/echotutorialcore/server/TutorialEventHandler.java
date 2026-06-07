package com.knoxhack.echotutorialcore.server;

import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.trigger.TutorialTriggerType;
import com.knoxhack.echotutorialcore.config.TutorialConfig;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import com.knoxhack.echotutorialcore.network.TutorialNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.state.BlockState;

public final class TutorialEventHandler {
    private TutorialEventHandler() {}

    public static void onPlayerLogin(Object event) {
        ServerPlayer player = EchoBackendWorldEventBridge.playerEventServerPlayer(event);
        if (player != null) {
            TutorialProgressManager.markProgress(player, Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "entered_world"));
            TutorialNetworking.sendSyncContent(player);
            TutorialNetworking.sendSyncProgress(player);
        }
    }

    public static void onPlayerLogout(Object event) {
        ServerPlayer player = EchoBackendWorldEventBridge.playerEventServerPlayer(event);
        if (player != null) {
            TutorialPlayerData data = TutorialPlayerData.get(player);
            data.resetPopupCount();
            TutorialPlayerData.save(player, data);
        }
    }

    public static void onPlayerDeath(Object event) {
        ServerPlayer player = entity(event) instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        if (player == null) return;
        DamageSource source = source(event);
        String causeKey = source == null ? "unknown" : source.getMsgId();
        long time = player.level().getGameTime();

        TutorialPlayerData data = TutorialPlayerData.get(player);
        data.recordDeath(causeKey, time);
        TutorialProgressManager.saveMirrorAndSync(player, data);
        TutorialFlowManager.reportTrigger(player, TutorialTriggerType.DEATH,
                Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, causeKey.replace('.', '_')));

        int threshold = TutorialConfig.REPEATED_DEATH_THRESHOLD.get();
        if (data.repeatedDeathCount() >= threshold) {
            TutorialMistakeDetector.reportRepeatedFailure(player);
        }
    }

    public static void onBlockPlace(Object event) {
        if (!(EchoBackendWorldEventBridge.blockEventEntity(event) instanceof ServerPlayer player)) return;
        BlockState placedBlock = placedBlock(event);
        if (placedBlock == null) return;
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(placedBlock.getBlock());
        TutorialFlowManager.reportTrigger(player, TutorialTriggerType.PLACE_BLOCK, blockId);
        TutorialProgressManager.markProgress(player, Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "placed_first_machine"));
    }

    private static Object entity(Object event) {
        return invokeNoArg(event, "getEntity");
    }

    private static DamageSource source(Object event) {
        Object value = invokeNoArg(event, "getSource");
        return value instanceof DamageSource damageSource ? damageSource : null;
    }

    private static BlockState placedBlock(Object event) {
        Object value = invokeNoArg(event, "getPlacedBlock");
        return value instanceof BlockState blockState ? blockState : null;
    }

    private static Object invokeNoArg(Object event, String methodName) {
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
