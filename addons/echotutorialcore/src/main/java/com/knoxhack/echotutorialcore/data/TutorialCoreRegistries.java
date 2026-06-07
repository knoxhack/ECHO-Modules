package com.knoxhack.echotutorialcore.data;

import com.knoxhack.echotutorialcore.api.card.TutorialCard;
import com.knoxhack.echotutorialcore.api.hint.TutorialHint;
import com.knoxhack.echotutorialcore.api.tooltip.TutorialTooltip;
import com.knoxhack.echotutorialcore.api.trigger.TutorialFlow;
import com.knoxhack.echotutorialcore.api.trigger.TutorialStep;
import com.knoxhack.echotutorialcore.server.TutorialConditionResolver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;

public final class TutorialCoreRegistries {
    private static final Map<Identifier, TutorialCard> CARDS = new LinkedHashMap<>();
    private static final Map<Identifier, TutorialHint> HINTS = new LinkedHashMap<>();
    private static final Map<Identifier, TutorialFlow> FLOWS = new LinkedHashMap<>();
    private static final Map<Identifier, TutorialTooltip> TOOLTIPS = new LinkedHashMap<>();
    private static final List<String> VALIDATION_WARNINGS = new ArrayList<>();

    private TutorialCoreRegistries() {}

    public static void registerCard(TutorialCard card) {
        if (card != null && card.id() != null) {
            if (CARDS.containsKey(card.id())) {
                VALIDATION_WARNINGS.add("Duplicate tutorial card id: " + card.id());
            }
            CARDS.put(card.id(), card);
        }
    }

    public static void registerHint(TutorialHint hint) {
        if (hint != null && hint.id() != null) {
            if (HINTS.containsKey(hint.id())) {
                VALIDATION_WARNINGS.add("Duplicate tutorial hint id: " + hint.id());
            }
            HINTS.put(hint.id(), hint);
        }
    }

    public static void registerFlow(TutorialFlow flow) {
        if (flow != null && flow.id() != null) {
            if (FLOWS.containsKey(flow.id())) {
                VALIDATION_WARNINGS.add("Duplicate tutorial flow id: " + flow.id());
            }
            FLOWS.put(flow.id(), flow);
        }
    }

    public static void registerTooltip(TutorialTooltip tooltip) {
        if (tooltip != null && tooltip.targetItem() != null) {
            if (TOOLTIPS.containsKey(tooltip.targetItem())) {
                VALIDATION_WARNINGS.add("Duplicate tutorial tooltip target: " + tooltip.targetItem());
            }
            TOOLTIPS.put(tooltip.targetItem(), tooltip);
        }
    }

    public static Optional<TutorialCard> getCard(Identifier id) {
        return Optional.ofNullable(CARDS.get(id));
    }

    public static Optional<TutorialHint> getHint(Identifier id) {
        return Optional.ofNullable(HINTS.get(id));
    }

    public static Optional<TutorialFlow> getFlow(Identifier id) {
        return Optional.ofNullable(FLOWS.get(id));
    }

    public static List<TutorialCard> getCardsByCategory(com.knoxhack.echotutorialcore.api.TutorialCategory category) {
        return CARDS.values().stream()
                .filter(c -> c.category() == category)
                .sorted((a, b) -> Integer.compare(b.priority(), a.priority()))
                .collect(Collectors.toList());
    }

    public static List<TutorialCard> allCards() {
        return List.copyOf(CARDS.values());
    }

    public static List<TutorialHint> allHints() {
        return List.copyOf(HINTS.values());
    }

    public static List<TutorialFlow> allFlows() {
        return List.copyOf(FLOWS.values());
    }

    public static Optional<TutorialTooltip> getTooltip(Identifier itemId) {
        return Optional.ofNullable(TOOLTIPS.get(itemId));
    }

    public static List<TutorialTooltip> allTooltips() {
        return List.copyOf(TOOLTIPS.values());
    }

    public static void clearAll() {
        CARDS.clear();
        HINTS.clear();
        FLOWS.clear();
        TOOLTIPS.clear();
        VALIDATION_WARNINGS.clear();
    }

    public static List<String> validate() {
        List<String> warnings = new ArrayList<>(VALIDATION_WARNINGS);
        for (TutorialCard card : CARDS.values()) {
            for (Identifier related : card.related()) {
                if (!CARDS.containsKey(related)) {
                    warnings.add("Card " + card.id() + " references missing related card " + related);
                }
            }
            for (String trigger : card.unlockTriggers()) {
                if (trigger == null || trigger.isBlank()) {
                    warnings.add("Card " + card.id() + " has a blank unlock trigger.");
                }
            }
        }
        for (TutorialHint hint : HINTS.values()) {
            if (hint.actionCardId() != null && !CARDS.containsKey(hint.actionCardId())) {
                warnings.add("Hint " + hint.id() + " references missing action card " + hint.actionCardId());
            }
            for (String condition : hint.conditions()) {
                if (!TutorialConditionResolver.isKnownConditionKey(condition)) {
                    warnings.add("Hint " + hint.id() + " has unknown condition key '" + condition + "'.");
                }
            }
        }
        for (TutorialFlow flow : FLOWS.values()) {
            Set<String> stepIds = new HashSet<>();
            for (TutorialStep step : flow.steps()) {
                String stepId = step.id() == null ? "" : step.id();
                if (!stepId.isBlank() && !stepIds.add(stepId)) {
                    warnings.add("Flow " + flow.id() + " has duplicate step id " + stepId);
                }
                if (step.type() == null) {
                    warnings.add("Flow " + flow.id() + " has a step with no trigger type.");
                }
            }
            for (Identifier cardId : flow.unlockCards()) {
                if (!CARDS.containsKey(cardId)) {
                    warnings.add("Flow " + flow.id() + " unlocks missing card " + cardId);
                }
            }
        }
        VALIDATION_WARNINGS.clear();
        VALIDATION_WARNINGS.addAll(warnings);
        return List.copyOf(warnings);
    }

    public static List<String> validationWarnings() {
        return List.copyOf(VALIDATION_WARNINGS);
    }

    public static void resetForTests() {
        clearAll();
    }

    public static int cardCount() {
        return CARDS.size();
    }

    public static int hintCount() {
        return HINTS.size();
    }

    public static int flowCount() {
        return FLOWS.size();
    }

    public static int tooltipCount() {
        return TOOLTIPS.size();
    }
}
