package com.knoxhack.echotutorialcore.client;

import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echotutorialcore.config.TutorialConfig;
import com.knoxhack.echotutorialcore.network.SetGuideModePacket;
import com.knoxhack.echotutorialcore.network.ShowTutorialHintPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class TutorialClientDisplay {
    private TutorialClientDisplay() {}

    public static void showHint(ShowTutorialHintPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || packet == null) return;

        boolean danger = "DANGER".equalsIgnoreCase(packet.typeName());
        TutorialToastOverlay.push(packet.title(), packet.message(), packet.details(), danger);
        if (fallbackToChat()) {
            mc.player.sendSystemMessage(Component.literal("[ECHO-7] ")
                    .append(packet.title())
                    .append(": ")
                    .append(packet.message()));
        }
    }

    public static void showCardToast(Identifier cardId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || cardId == null) return;
        var card = TutorialClientData.card(cardId);
        String title = card == null ? cardId.toString() : card.title();
        TutorialToastOverlay.push("Guide Card", "Unlocked: " + title, "", false);
        if (fallbackToChat()) {
            mc.player.sendSystemMessage(Component.literal("[ECHO Terminal] Guide card unlocked: ").append(title));
        }
    }

    public static void showUnlockCard(Identifier cardId) {
        showCardToast(cardId);
    }

    public static void requestGuideMode(String modeName) {
        EchoNetClientActions.trySendServerboundAction(new SetGuideModePacket(modeName));
    }

    private static boolean fallbackToChat() {
        try {
            return !TutorialConfig.SHOW_TOAST_HINTS.get();
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
