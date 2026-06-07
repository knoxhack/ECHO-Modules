package com.knoxhack.echotutorialcore.server;

import com.knoxhack.echotutorialcore.api.card.TutorialCard;
import com.knoxhack.echotutorialcore.data.TutorialCoreRegistries;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import com.knoxhack.echotutorialcore.integration.soundcore.TutorialSoundCoreIntegration;
import com.knoxhack.echotutorialcore.network.TutorialNetworking;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class TutorialCardManager {
    private TutorialCardManager() {}

    public static void unlockCard(Player player, Identifier cardId) {
        if (player == null || cardId == null) return;
        TutorialPlayerData data = TutorialPlayerData.get(player);
        boolean added = data.unlockCardIfNew(cardId);
        if (player instanceof ServerPlayer sp) {
            TutorialPlayerData.saveAndSync(sp, data);
            TutorialNetworking.sendSyncProgress(sp);
            if (added) {
                TutorialNetworking.sendUnlockCard(sp, cardId);
                TutorialSoundCoreIntegration.playCardUnlock();
            }
        } else {
            TutorialPlayerData.save(player, data);
        }
    }

    public static void markCardRead(Player player, Identifier cardId) {
        if (player == null || cardId == null) return;
        TutorialPlayerData data = TutorialPlayerData.get(player);
        data.markCardRead(cardId);
        if (player instanceof ServerPlayer sp) {
            TutorialPlayerData.saveAndSync(sp, data);
            TutorialNetworking.sendSyncProgress(sp);
        } else {
            TutorialPlayerData.save(player, data);
        }
    }

    public static void dismissCard(Player player, Identifier cardId) {
        if (player == null || cardId == null) return;
        TutorialPlayerData data = TutorialPlayerData.get(player);
        data.dismissCard(cardId);
        if (player instanceof ServerPlayer sp) {
            TutorialPlayerData.saveAndSync(sp, data);
            TutorialNetworking.sendSyncProgress(sp);
        } else {
            TutorialPlayerData.save(player, data);
        }
    }

    public static int unreadCount(Player player) {
        if (player == null) return 0;
        return TutorialPlayerData.get(player).unreadCardCount();
    }

    public static boolean isCardUnlocked(Player player, Identifier cardId) {
        if (player == null || cardId == null) return false;
        TutorialPlayerData data = TutorialPlayerData.get(player);
        if (data.isCardUnlocked(cardId)) return true;
        TutorialCard card = TutorialCoreRegistries.getCard(cardId).orElse(null);
        return card != null && card.defaultUnlocked();
    }

    public static boolean isCardVisible(Player player, Identifier cardId) {
        if (player == null || cardId == null) return false;
        if (isCardUnlocked(player, cardId)) return true;
        TutorialCard card = TutorialCoreRegistries.getCard(cardId).orElse(null);
        return card != null && card.defaultUnlocked();
    }

    public static List<TutorialCard> getVisibleCards(Player player, com.knoxhack.echotutorialcore.api.TutorialCategory category) {
        return TutorialCoreRegistries.getCardsByCategory(category).stream()
                .filter(c -> isCardVisible(player, c.id()))
                .toList();
    }

    public static List<TutorialCard> getVisibleCards(Player player) {
        return TutorialCoreRegistries.allCards().stream()
                .filter(c -> isCardVisible(player, c.id()))
                .sorted((a, b) -> Integer.compare(b.priority(), a.priority()))
                .toList();
    }

    public static List<TutorialCard> getRecommendedCards(Player player, int limit) {
        return TutorialCoreRegistries.allCards().stream()
                .filter(c -> isCardVisible(player, c.id()))
                .sorted((a, b) -> Integer.compare(b.priority(), a.priority()))
                .limit(limit)
                .toList();
    }
}
