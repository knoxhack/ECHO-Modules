package com.knoxhack.echolens.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.IMissionRegistry;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionHookTargets;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.mission.MissionRewardClaimMode;
import com.echoplatform.echocore.api.mission.ObjectiveDefinition;
import com.echoplatform.echocore.api.mission.RewardDefinition;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeEnvironmentBridge;
import com.knoxhack.echolens.EchoLens;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LensMissionCoreIntegration {
    private static final Identifier CHAPTER = id("lens");

    private LensMissionCoreIntegration() {
    }

    public static void register() {
        EchoCoreServices.registerMissionContent(EchoLens.MODID, LensMissionCoreIntegration::registerContent);
        LensMissionHooks.registerCoverage();
        if (EchoNativeRuntimeEnvironmentBridge.isNativeLoaderActive()) {
            EchoLens.LOGGER.info("ECHO: Lens MissionCore rows registered with Native-safe stack fallbacks.");
        }
    }

    public static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoLens.MODID, new MissionChapterDefinition(
                CHAPTER,
                "Lens Side Ops",
                "Deep scans, machine diagnostics, and Index shortcut verification.",
                78,
                0xFF66D9EF));
        registerMission(registry, "verified_deep_scan", "scan", MissionObjectiveType.SCAN_BLOCK,
                "Verified Deep Scan", "Complete a server-assisted Deep Scan.",
                "The Lens target was verified server-side.",
                Items.SPYGLASS, 0, "Complete a verified Deep Scan",
                Items.AMETHYST_SHARD, 2, "Amethyst Shard x2");
        registerMission(registry, "machine_diagnostic", "diagnostic", MissionObjectiveType.SCAN_BLOCK,
                "Machine Diagnostic", "Deep-scan a block target for machine or container diagnostics.",
                "Machine diagnostic context was accepted.",
                Items.REDSTONE_TORCH, 1, "Deep-scan a machine",
                Items.REDSTONE, 4, "Redstone Dust x4");
        registerMission(registry, "index_shortcut", "shortcut", MissionObjectiveType.UNLOCK_RESEARCH,
                "Index Shortcut", "Use a Lens-to-Index recipe, use, or track shortcut.",
                "Lens shortcut telemetry reached the Index.",
                Items.BOOK, 2, "Use an Index shortcut",
                Items.EXPERIENCE_BOTTLE, 1, "Bottle o' Enchanting");
    }

    private static void registerMission(
            IMissionRegistry registry,
            String missionPath,
            String objectiveKey,
            MissionObjectiveType type,
            String title,
            String briefing,
            String fieldGuide,
            Item icon,
            int order,
            String objectiveLabel,
            Item reward,
            int rewardCount,
            String rewardLabel) {
        Identifier mission = id(missionPath);
        Identifier target = MissionHookTargets.objectiveTarget(EchoLens.MODID, mission, objectiveKey);
        ItemStack iconStack = stack(icon, 1);
        registry.registerMission(EchoLens.MODID, MissionDefinition.builder(mission, CHAPTER)
                .phase("lens_side_ops", "Lens Side Ops", 0, order)
                .text(title, briefing, fieldGuide)
                .category("Lens", "Side Op")
                .icon(iconStack)
                .kind(MissionKind.SIDE_OP)
                .objective(new ObjectiveDefinition(
                        id(missionPath + "/" + objectiveKey),
                        type,
                        objectiveLabel,
                        "",
                        iconStack,
                        1,
                        false,
                        Map.of("target", target.toString())))
                .reward(reward(id(missionPath + "/reward"), reward, rewardCount, rewardLabel))
                .build());
    }

    private static RewardDefinition reward(Identifier id, Item item, int count, String label) {
        int safeCount = Math.max(1, count);
        ItemStack stack = stack(item, safeCount);
        return new RewardDefinition(
                id,
                MissionRewardClaimMode.CLAIMABLE,
                stack,
                label,
                "",
                Map.of("item", itemId(item), "count", Integer.toString(safeCount)));
    }

    private static ItemStack stack(Item item, int count) {
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        try {
            return new ItemStack(item, Math.max(1, count));
        } catch (RuntimeException | LinkageError exception) {
            EchoLens.LOGGER.debug("Lens MissionCore stack {} deferred because item components are not bound yet.",
                    itemId(item));
            return ItemStack.EMPTY;
        }
    }

    private static String itemId(Item item) {
        if (item == null || item == Items.AIR) {
            return "";
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? "" : id.toString();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoLens.MODID, path);
    }
}
