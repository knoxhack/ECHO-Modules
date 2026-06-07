package com.knoxhack.echotutorialcore.client;

import com.knoxhack.echotutorialcore.network.SyncTutorialContentPacket;
import com.knoxhack.echotutorialcore.network.SyncTutorialProgressPacket;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;

public final class TutorialClientData {
    private static final Map<Identifier, SyncTutorialContentPacket.CardData> CARDS = new LinkedHashMap<>();
    private static final Map<Identifier, SyncTutorialContentPacket.HintData> HINTS = new LinkedHashMap<>();
    private static final Map<Identifier, SyncTutorialContentPacket.FlowData> FLOWS = new LinkedHashMap<>();
    private static final Map<Identifier, SyncTutorialContentPacket.TooltipData> TOOLTIPS = new LinkedHashMap<>();
    private static String guideMode = "NORMAL";
    private static Set<String> progressFlags = Set.of();
    private static Set<String> unlockedCardIds = Set.of();
    private static Set<String> unreadCardIds = Set.of();
    private static Set<String> completedFlowIds = Set.of();
    private static String lastRecommendationReason = "";

    private TutorialClientData() {}

    public static void replaceContent(SyncTutorialContentPacket packet) {
        CARDS.clear();
        HINTS.clear();
        FLOWS.clear();
        TOOLTIPS.clear();
        if (packet == null) {
            return;
        }
        packet.cards().forEach(card -> {
            if (card.id() != null) {
                CARDS.put(card.id(), card);
            }
        });
        packet.hints().forEach(hint -> {
            if (hint.id() != null) {
                HINTS.put(hint.id(), hint);
            }
        });
        packet.flows().forEach(flow -> {
            if (flow.id() != null) {
                FLOWS.put(flow.id(), flow);
            }
        });
        packet.tooltips().forEach(tooltip -> {
            if (tooltip.targetItem() != null) {
                TOOLTIPS.put(tooltip.targetItem(), tooltip);
            }
        });
    }

    public static void applyProgress(SyncTutorialProgressPacket packet) {
        if (packet == null) {
            return;
        }
        guideMode = packet.guideMode();
        progressFlags = Set.copyOf(packet.progressFlags());
        unlockedCardIds = Set.copyOf(packet.unlockedCardIds());
        unreadCardIds = Set.copyOf(packet.unreadCardIds());
        completedFlowIds = Set.copyOf(packet.completedFlowIds());
        lastRecommendationReason = packet.lastRecommendationReason();
    }

    public static List<SyncTutorialContentPacket.CardData> cards() {
        return CARDS.values().stream()
                .filter(TutorialClientData::isVisible)
                .sorted(Comparator.comparingInt(SyncTutorialContentPacket.CardData::priority).reversed())
                .toList();
    }

    public static List<SyncTutorialContentPacket.CardData> cardsByCategory(String category) {
        return cards().stream()
                .filter(card -> card.category().equalsIgnoreCase(category))
                .toList();
    }

    public static SyncTutorialContentPacket.CardData card(Identifier id) {
        return id == null ? null : CARDS.get(id);
    }

    public static SyncTutorialContentPacket.TooltipData tooltip(Identifier itemId) {
        return itemId == null ? null : TOOLTIPS.get(itemId);
    }

    public static boolean isUnread(Identifier cardId) {
        return cardId != null && unreadCardIds.contains(cardId.toString());
    }

    public static boolean isUnlocked(Identifier cardId) {
        return cardId != null && unlockedCardIds.contains(cardId.toString());
    }

    public static boolean isVisible(SyncTutorialContentPacket.CardData card) {
        return card != null && (card.defaultUnlocked() || isUnlocked(card.id()));
    }

    public static int unreadCount() {
        return unreadCardIds.size();
    }

    public static String guideMode() {
        return guideMode;
    }

    public static String lastRecommendationReason() {
        return lastRecommendationReason;
    }

    public static Set<String> progressFlags() {
        return progressFlags;
    }

    public static Set<String> completedFlowIds() {
        return completedFlowIds;
    }

    public static boolean hasContent() {
        return !CARDS.isEmpty() || !HINTS.isEmpty() || !FLOWS.isEmpty() || !TOOLTIPS.isEmpty();
    }
}
