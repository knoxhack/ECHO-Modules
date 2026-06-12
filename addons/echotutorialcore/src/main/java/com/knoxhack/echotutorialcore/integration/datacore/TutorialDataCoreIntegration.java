package com.knoxhack.echotutorialcore.integration.datacore;

import com.echoplatform.echocore.api.DataScope;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.IDataKey;
import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class TutorialDataCoreIntegration {
    public static final IDataKey<String> GUIDE_SUMMARY = IDataKey.string(id("guide_summary"), DataScope.PLAYER, "", true);
    public static final IDataKey<String> LAST_SHOWN_HINT = IDataKey.string(id("last_shown_hint"), DataScope.PLAYER, "", true);
    public static final IDataKey<Long> COMPLETED_FLOW_COUNT = IDataKey.counter(id("completed_flow_count"), DataScope.PLAYER, 0L, true);
    public static final IDataKey<Long> UNREAD_CARD_COUNT = IDataKey.counter(id("unread_card_count"), DataScope.PLAYER, 0L, true);

    private static boolean registered;

    private TutorialDataCoreIntegration() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        EchoCoreServices.registerDataKey(GUIDE_SUMMARY);
        EchoCoreServices.registerDataKey(LAST_SHOWN_HINT);
        EchoCoreServices.registerDataKey(COMPLETED_FLOW_COUNT);
        EchoCoreServices.registerDataKey(UNREAD_CARD_COUNT);
        EchoTutorialCore.LOGGER.info("ECHO: TutorialCore integrated with DataCore. Tutorial state mirrors registered.");
    }

    public static void mirrorPlayer(Player player) {
        if (player == null) {
            return;
        }
        TutorialPlayerData data = TutorialPlayerData.get(player);
        var view = EchoCoreServices.playerData(player);
        view.set(GUIDE_SUMMARY, data.guideMode().name()
                + "|progress=" + data.progressFlags().size()
                + "|cards=" + data.unlockedCardIds().size()
                + "|flows=" + data.completedFlowIds().size());
        view.set(COMPLETED_FLOW_COUNT, (long) data.completedFlowIds().size());
        view.set(UNREAD_CARD_COUNT, (long) data.unreadCardCount());
    }

    public static void recordLastHint(Player player, Identifier hintId) {
        if (player == null || hintId == null) {
            return;
        }
        EchoCoreServices.playerData(player).set(LAST_SHOWN_HINT, hintId.toString());
        mirrorPlayer(player);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, path);
    }
}
