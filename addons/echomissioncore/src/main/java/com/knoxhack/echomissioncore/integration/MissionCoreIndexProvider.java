package com.knoxhack.echomissioncore.integration;

import com.echoplatform.echocore.api.index.IIndexContentProvider;
import com.echoplatform.echocore.api.index.IndexBuildContext;
import com.echoplatform.echocore.api.index.IndexContentSnapshot;
import com.echoplatform.echocore.api.index.IndexSourceFact;
import com.echoplatform.echocore.api.index.IndexSourceKind;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.RewardDefinition;
import com.knoxhack.echomissioncore.EchoMissionCore;
import com.knoxhack.echomissioncore.service.MissionCoreService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public enum MissionCoreIndexProvider implements IIndexContentProvider {
    INSTANCE;

    @Override
    public Identifier id() {
        return Identifier.fromNamespaceAndPath(EchoMissionCore.MODID, "provider/index_recipes");
    }

    @Override
    public IndexContentSnapshot snapshot(IndexBuildContext context) {
        Player player = context == null ? null : context.player();
        return new IndexContentSnapshot(id(), List.of(), List.of(), List.of(), List.of(), List.of(),
                sourceFacts(player), List.of(), List.of());
    }

    public List<IndexSourceFact> sourceFacts(Player player) {
        List<IndexSourceFact> facts = new ArrayList<>();
        for (MissionDefinition mission : MissionCoreService.INSTANCE.missionDefinitions()) {
            for (RewardDefinition reward : mission.rewards()) {
                IndexSourceFact fact = rewardFact(mission, reward);
                if (fact != null) {
                    facts.add(fact);
                }
            }
        }
        return List.copyOf(facts);
    }

    private static IndexSourceFact rewardFact(MissionDefinition mission, RewardDefinition reward) {
        ItemStack stack = rewardStack(reward);
        if (stack.isEmpty()) {
            return null;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return null;
        }
        ItemStack icon = mission.icon().isEmpty() ? stack : mission.icon();
        return new IndexSourceFact(
                itemId,
                rewardSourceId(mission, reward),
                IndexSourceKind.MISSION_REWARD,
                "Mission Reward: " + mission.title(),
                rewardNotes(mission, reward),
                icon,
                mission.id().getNamespace());
    }

    private static List<String> rewardNotes(MissionDefinition mission, RewardDefinition reward) {
        List<String> notes = new ArrayList<>();
        notes.add("Source type: " + IndexSourceKind.MISSION_REWARD.label());
        notes.add("Mission: " + mission.title());
        if (!reward.label().isBlank()) {
            notes.add("Reward: " + reward.label());
        }
        if (!reward.detail().isBlank()) {
            notes.add(reward.detail());
        }
        if (!mission.briefing().isBlank()) {
            notes.add(mission.briefing());
        }
        if (!mission.fieldGuide().isBlank()) {
            notes.add(mission.fieldGuide());
        }
        notes.add("Chapter: " + mission.chapterId());
        if (!mission.phaseTitle().isBlank()) {
            notes.add("Phase: " + mission.phaseTitle());
        }
        if (!mission.category().isBlank()) {
            notes.add("Category: " + mission.category());
        }
        if (!mission.difficulty().isBlank()) {
            notes.add("Difficulty: " + mission.difficulty());
        }
        notes.add("Repeat: " + mission.repeatPolicy().name().toLowerCase(Locale.ROOT));
        if (mission.hidden()) {
            notes.add("Hidden until discovered or unlocked.");
        }
        if (!mission.prerequisites().isEmpty()) {
            notes.add("Unlocks after: " + joinIds(mission.prerequisites()));
        }
        if (!reward.metadata().isEmpty()) {
            notes.add("Reward metadata: " + compactMetadata(reward.metadata()));
        }
        if (!mission.metadata().isEmpty()) {
            notes.add("Mission metadata: " + compactMetadata(mission.metadata()));
        }
        return List.copyOf(notes);
    }

    private static Identifier rewardSourceId(MissionDefinition mission, RewardDefinition reward) {
        return Identifier.fromNamespaceAndPath(EchoMissionCore.MODID,
                "source/mission_reward/" + sanitize(mission.id().getNamespace())
                        + "/" + sanitize(mission.id().getPath())
                        + "/" + sanitize(reward.id().getNamespace())
                        + "/" + sanitize(reward.id().getPath()));
    }

    private static ItemStack rewardStack(RewardDefinition reward) {
        ItemStack stack = reward.stack();
        if (stack.isEmpty()) {
            Identifier itemId = Identifier.tryParse(reward.metadata().getOrDefault("item", ""));
            stack = stackForId(itemId, count(reward.metadata()));
        }
        return stack;
    }

    private static ItemStack stackForId(Identifier id, int count) {
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
        if (item != Items.AIR) {
            return new ItemStack(item, Math.max(1, count));
        }
        Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        if (block != null && block.asItem() != Items.AIR) {
            return new ItemStack(block.asItem(), Math.max(1, count));
        }
        return ItemStack.EMPTY;
    }

    private static int count(Map<String, String> metadata) {
        try {
            return Math.max(1, Integer.parseInt(metadata.getOrDefault("count", "1")));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static String joinIds(List<Identifier> ids) {
        return ids.stream().map(Identifier::toString).reduce((left, right) -> left + ", " + right).orElse("");
    }

    private static String compactMetadata(Map<String, String> metadata) {
        return metadata.entrySet().stream()
                .limit(6)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private static String sanitize(String path) {
        String clean = path == null ? "unknown" : path.trim().toLowerCase(Locale.ROOT);
        clean = clean.replace('\\', '/').replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
        while (clean.contains("//")) {
            clean = clean.replace("//", "/");
        }
        return clean.isBlank() ? "unknown" : clean;
    }
}
