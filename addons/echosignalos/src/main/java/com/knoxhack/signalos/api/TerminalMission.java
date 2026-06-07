package com.knoxhack.signalos.api;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A mission card shown by SignalOS. Missions can be registered by Java, JSON, or script bridge.
 */
public record TerminalMission(
        Identifier id,
        Identifier chapterId,
        String title,
        String description,
        List<String> objectives,
        int order,
        Identifier iconItem,
        Identifier completionAdvancement,
        boolean rewardClaim,
        List<Reward> rewards) {
    public TerminalMission {
        id = TerminalIds.requireLowercase(id, "Terminal mission");
        chapterId = TerminalIds.requireLowercase(chapterId, "Terminal mission chapter");
        title = title == null || title.isBlank() ? id.getPath() : title.strip();
        description = description == null ? "" : description.strip();
        objectives = List.copyOf(objectives == null ? List.of() : objectives);
        if (iconItem != null) {
            iconItem = TerminalIds.requireLowercase(iconItem, "Terminal mission icon");
        }
        if (completionAdvancement != null) {
            completionAdvancement = TerminalIds.requireLowercase(completionAdvancement, "Terminal mission advancement");
        }
        rewards = List.copyOf(rewards == null ? List.of() : rewards);
    }

    public List<ItemStack> rewardStacks() {
        return rewards.stream().map(Reward::stack).filter(stack -> !stack.isEmpty()).toList();
    }

    public static Builder builder(String id) {
        return new Builder(TerminalIds.parse(id, "Terminal mission"));
    }

    public static Builder builder(Identifier id) {
        return new Builder(id);
    }

    public record Reward(Identifier itemId, int count, String label) {
        public Reward {
            itemId = TerminalIds.requireLowercase(itemId, "Terminal mission reward");
            count = Math.max(1, Math.min(64, count));
            label = label == null ? "" : label;
        }

        public ItemStack stack() {
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
            return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
        }

        public boolean hasRegisteredItem() {
            return BuiltInRegistries.ITEM.getOptional(itemId)
                    .filter(item -> item != Items.AIR)
                    .isPresent();
        }

        public String displayLabel() {
            if (!label.isBlank()) {
                return label;
            }
            return BuiltInRegistries.ITEM.getOptional(itemId)
                    .filter(item -> item != Items.AIR)
                    .map(Item::getDescriptionId)
                    .orElse(itemId.toString());
        }
    }

    public static final class Builder {
        private final Identifier id;
        private Identifier chapterId;
        private String title = "";
        private String description = "";
        private final List<String> objectives = new ArrayList<>();
        private int order;
        private Identifier iconItem;
        private Identifier completionAdvancement;
        private boolean rewardClaim = true;
        private final List<Reward> rewards = new ArrayList<>();

        private Builder(Identifier id) {
            this.id = id;
        }

        public Builder chapter(String chapterId) {
            this.chapterId = TerminalIds.parse(chapterId, "Terminal mission chapter");
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder objective(String objective) {
            if (objective != null && !objective.isBlank()) {
                objectives.add(objective.strip());
            }
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public Builder icon(String itemId) {
            this.iconItem = itemId == null || itemId.isBlank() ? null : TerminalIds.parse(itemId, "Terminal mission icon");
            return this;
        }

        public Builder completionAdvancement(String advancementId) {
            this.completionAdvancement = advancementId == null || advancementId.isBlank()
                    ? null
                    : TerminalIds.parse(advancementId, "Terminal mission advancement");
            return this;
        }

        public Builder rewardClaim(boolean rewardClaim) {
            this.rewardClaim = rewardClaim;
            return this;
        }

        public Builder reward(String itemId, int count) {
            rewards.add(new Reward(TerminalIds.parse(itemId, "Terminal mission reward"), count, ""));
            return this;
        }

        public Builder reward(String itemId, int count, String label) {
            rewards.add(new Reward(TerminalIds.parse(itemId, "Terminal mission reward"), count, label));
            return this;
        }

        public TerminalMission build() {
            return new TerminalMission(id, chapterId, title, description, objectives, order, iconItem,
                    completionAdvancement, rewardClaim, rewards);
        }
    }
}
