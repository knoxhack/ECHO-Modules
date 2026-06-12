package com.echoplatform.echocore.api.mission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record MissionDefinition(
        Identifier id,
        Identifier chapterId,
        String phaseId,
        String title,
        String briefing,
        String fieldGuide,
        String category,
        String difficulty,
        ItemStack icon,
        MissionKind kind,
        MissionRepeatPolicy repeatPolicy,
        boolean hidden,
        List<Identifier> prerequisites,
        List<ObjectiveDefinition> objectives,
        List<RewardDefinition> rewards,
        Map<String, String> metadata,
        MissionStatusRule statusRule,
        MissionCompletionRule completionRule,
        MissionCompletionHandler completionHandler,
        MissionActionProvider actionProvider,
        MissionActionHandler actionHandler) {
    public MissionDefinition(
            Identifier id,
            Identifier chapterId,
            String title,
            MissionKind kind,
            List<ObjectiveDefinition> objectives,
            List<RewardDefinition> rewards,
            Map<String, String> metadata) {
        this(id, chapterId, "", title, "", "", "", "", ItemStack.EMPTY, kind, MissionRepeatPolicy.ONCE, false,
                List.of(), objectives, rewards, metadata, MissionStatusRule.NONE, MissionCompletionRule.NEVER,
                MissionCompletionHandler.NOOP, MissionActionProvider.NONE, MissionActionHandler.NOOP);
    }

    public MissionDefinition {
        phaseId = phaseId == null ? "" : phaseId;
        title = title == null ? "" : title;
        briefing = briefing == null ? "" : briefing;
        fieldGuide = fieldGuide == null ? "" : fieldGuide;
        category = category == null ? "" : category;
        difficulty = difficulty == null ? "" : difficulty;
        icon = icon == null ? ItemStack.EMPTY : icon.copy();
        kind = kind == null ? MissionKind.CUSTOM : kind;
        repeatPolicy = repeatPolicy == null ? MissionRepeatPolicy.ONCE : repeatPolicy;
        prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
        objectives = objectives == null ? List.of() : List.copyOf(objectives);
        rewards = rewards == null ? List.of() : List.copyOf(rewards);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        statusRule = statusRule == null ? MissionStatusRule.NONE : statusRule;
        completionRule = completionRule == null ? MissionCompletionRule.NEVER : completionRule;
        completionHandler = completionHandler == null ? MissionCompletionHandler.NOOP : completionHandler;
        actionProvider = actionProvider == null ? MissionActionProvider.NONE : actionProvider;
        actionHandler = actionHandler == null ? MissionActionHandler.NOOP : actionHandler;
    }

    public static Builder builder(Identifier id, Identifier chapterId) {
        return new Builder(id, chapterId);
    }

    public static final class Builder {
        private final Identifier id;
        private final Identifier chapterId;
        private String phaseId = "";
        private String title = "";
        private String briefing = "";
        private String fieldGuide = "";
        private String category = "";
        private String difficulty = "";
        private ItemStack icon = ItemStack.EMPTY;
        private MissionKind kind = MissionKind.CUSTOM;
        private MissionRepeatPolicy repeatPolicy = MissionRepeatPolicy.ONCE;
        private boolean hidden;
        private final List<Identifier> prerequisites = new ArrayList<>();
        private final List<ObjectiveDefinition> objectives = new ArrayList<>();
        private final List<RewardDefinition> rewards = new ArrayList<>();
        private final Map<String, String> metadata = new LinkedHashMap<>();
        private MissionStatusRule statusRule = MissionStatusRule.NONE;
        private MissionCompletionRule completionRule = MissionCompletionRule.NEVER;
        private MissionCompletionHandler completionHandler = MissionCompletionHandler.NOOP;
        private MissionActionProvider actionProvider = MissionActionProvider.NONE;
        private MissionActionHandler actionHandler = MissionActionHandler.NOOP;

        private Builder(Identifier id, Identifier chapterId) {
            this.id = id;
            this.chapterId = chapterId;
        }

        public Builder phase(String id, String title, int phaseOrder, int missionOrder) {
            this.phaseId = id == null ? "" : id;
            metadata("phaseId", this.phaseId);
            metadata("phaseTitle", title == null ? "" : title);
            metadata("phaseOrder", Integer.toString(phaseOrder));
            metadata("missionOrder", Integer.toString(missionOrder));
            return this;
        }

        public Builder text(String title, String briefing, String fieldGuide) {
            this.title = title == null ? "" : title;
            this.briefing = briefing == null ? "" : briefing;
            this.fieldGuide = fieldGuide == null ? "" : fieldGuide;
            return this;
        }

        public Builder category(String category, String difficulty) {
            this.category = category == null ? "" : category;
            this.difficulty = difficulty == null ? "" : difficulty;
            return this;
        }

        public Builder icon(ItemStack icon) {
            this.icon = icon == null ? ItemStack.EMPTY : icon.copy();
            return this;
        }

        public Builder kind(MissionKind kind) {
            this.kind = kind == null ? MissionKind.CUSTOM : kind;
            return this;
        }

        public Builder repeatPolicy(MissionRepeatPolicy repeatPolicy) {
            this.repeatPolicy = repeatPolicy == null ? MissionRepeatPolicy.ONCE : repeatPolicy;
            return this;
        }

        public Builder hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        public Builder objective(ObjectiveDefinition objective) {
            if (objective != null) {
                objectives.add(objective);
            }
            return this;
        }

        public Builder reward(RewardDefinition reward) {
            if (reward != null) {
                rewards.add(reward);
            }
            return this;
        }

        public Builder prerequisite(Identifier id) {
            if (id != null && !prerequisites.contains(id)) {
                prerequisites.add(id);
            }
            return this;
        }

        public Builder prerequisites(Collection<Identifier> ids) {
            if (ids != null) {
                ids.forEach(this::prerequisite);
            }
            return this;
        }

        public Builder metadata(String key, String value) {
            if (key != null && value != null) {
                metadata.put(key, value);
            }
            return this;
        }

        public Builder statusRule(MissionStatusRule rule) {
            this.statusRule = rule == null ? MissionStatusRule.NONE : rule;
            return this;
        }

        public Builder completionRule(MissionCompletionRule rule) {
            this.completionRule = rule == null ? MissionCompletionRule.NEVER : rule;
            return this;
        }

        public Builder completionHandler(MissionCompletionHandler handler) {
            this.completionHandler = handler == null ? MissionCompletionHandler.NOOP : handler;
            return this;
        }

        public Builder actionProvider(MissionActionProvider provider) {
            this.actionProvider = provider == null ? MissionActionProvider.NONE : provider;
            return this;
        }

        public Builder actionHandler(MissionActionHandler handler) {
            this.actionHandler = handler == null ? MissionActionHandler.NOOP : handler;
            return this;
        }

        public MissionDefinition build() {
            return new MissionDefinition(id, chapterId, phaseId, title, briefing, fieldGuide, category, difficulty,
                    icon, kind, repeatPolicy, hidden, prerequisites, objectives, rewards, metadata, statusRule,
                    completionRule, completionHandler, actionProvider, actionHandler);
        }
    }

    @FunctionalInterface
    public interface MissionStatusRule {
        MissionStatusRule NONE = (player, mission) -> Optional.empty();

        Optional<MissionStatus> status(Player player, MissionDefinition mission);
    }

    @FunctionalInterface
    public interface MissionCompletionRule {
        MissionCompletionRule NEVER = (player, mission) -> false;

        boolean isComplete(Player player, MissionDefinition mission);
    }

    @FunctionalInterface
    public interface MissionActionProvider {
        MissionActionProvider NONE = (player, mission, status, completeNow) -> List.of();

        List<MissionActionView> actions(Player player, MissionDefinition mission, MissionStatus status, boolean completeNow);
    }

    @FunctionalInterface
    public interface MissionActionHandler {
        MissionActionHandler NOOP = (player, mission, actionId) -> false;

        boolean handle(ServerPlayer player, MissionDefinition mission, String actionId);
    }

    @FunctionalInterface
    public interface MissionCompletionHandler {
        MissionCompletionHandler NOOP = (player, mission) -> {
        };

        void onCompleted(ServerPlayer player, MissionDefinition mission);
    }

    public int phaseOrder() {
        return intMetadata("phaseOrder");
    }

    public int missionOrder() {
        return intMetadata("missionOrder");
    }

    public String phaseTitle() {
        return metadata.getOrDefault("phaseTitle", "");
    }

    private int intMetadata(String key) {
        try {
            return Integer.parseInt(metadata.getOrDefault(key, "0"));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
