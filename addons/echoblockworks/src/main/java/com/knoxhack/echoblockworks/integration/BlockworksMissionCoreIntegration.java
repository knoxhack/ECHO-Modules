package com.knoxhack.echoblockworks.integration;

import com.knoxhack.echoblockworks.EchoBlockworks;
import com.knoxhack.echoblockworks.registry.ModBlocks;
import com.knoxhack.echoblockworks.registry.ModItems;
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
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class BlockworksMissionCoreIntegration {
    private static final Identifier CHAPTER = id("blockworks");
    private static boolean registered;
    private static boolean hookCoverageRegistered;

    private BlockworksMissionCoreIntegration() {
    }

    public static void register() {
        registerHookCoverage();
        registerWhenReady();
    }

    public static boolean registerWhenReady() {
        if (registered) {
            return true;
        }
        if (!itemStackComponentsBound()) {
            return false;
        }
        try {
            EchoCoreServices.registerMissionContent(EchoBlockworks.MODID, BlockworksMissionCoreIntegration::registerContent);
            registered = true;
            return true;
        } catch (RuntimeException | LinkageError exception) {
            registered = false;
            EchoBlockworks.LOGGER.warn("Blockworks MissionCore content is not ready yet; it will be retried.", exception);
            return false;
        }
    }

    private static void registerHookCoverage() {
        if (!hookCoverageRegistered) {
            BlockworksMissionHooks.registerCoverage();
            hookCoverageRegistered = true;
        }
    }

    private static boolean itemStackComponentsBound() {
        try {
            return !new ItemStack(Items.STONE).isEmpty();
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }

    public static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoBlockworks.MODID, new MissionChapterDefinition(
                CHAPTER,
                "Blockworks Side Ops",
                "Blockworks table conversion, pattern cutting, and showcase discovery.",
                80,
                0xFF9CC7D8));
        registerMission(registry, "use_table", "table", MissionObjectiveType.CUSTOM,
                "Use Blockworks Table", "Craft or use a Blockworks Table conversion workflow.",
                "The table conversion loop is online.",
                safeStack(() -> (ItemLike) ModBlocks.BLOCKWORKS_TABLE.get(), Items.STONECUTTER, 1), 0, "Use the Blockworks Table", safeStack(() -> Items.STONECUTTER, Items.STONECUTTER, 1));
        registerMission(registry, "convert_variant", "convert", MissionObjectiveType.CRAFT_ITEM,
                "Convert Variant", "Convert any Blockworks variant through the table.",
                "A Blockworks palette conversion has been recorded.",
                safeStack(() -> (ItemLike) ModBlocks.BLOCKWORKS_TABLE.get(), Items.STONECUTTER, 1), 1, "Convert a Blockworks variant", safeStack(() -> Items.BRICKS, Items.BRICKS, 4));
        registerMission(registry, "use_pattern_cutter", "cutter", MissionObjectiveType.PLACE_BLOCK,
                "Use Pattern Cutter", "Cycle a placed Blockworks block with the Echo Pattern Cutter.",
                "Pattern cutter state transfer verified.",
                safeStack(() -> (ItemLike) ModItems.ECHO_PATTERN_CUTTER.get(), Items.SHEARS, 1), 2, "Use the Pattern Cutter", safeStack(() -> (ItemLike) ModItems.ECHO_PATTERN_CUTTER.get(), Items.SHEARS, 1));
        registerMission(registry, "discover_showcase_site", "showcase", MissionObjectiveType.DISCOVER_STRUCTURE,
                "Discover Showcase Site", "Find or interact with a generated Blockworks showcase site.",
                "Showcase discovery route recorded.",
                safeStack(() -> Items.LODESTONE, Items.LODESTONE, 1), 3, "Discover a showcase site", safeStack(() -> Items.LANTERN, Items.LANTERN, 2));
    }

    private static void registerMission(
            IMissionRegistry registry,
            String missionPath,
            String objectiveKey,
            MissionObjectiveType type,
            String title,
            String briefing,
            String fieldGuide,
            ItemStack icon,
            int order,
            String objectiveLabel,
            ItemStack reward) {
        Identifier mission = id(missionPath);
        Identifier target = MissionHookTargets.objectiveTarget(EchoBlockworks.MODID, mission, objectiveKey);
        registry.registerMission(EchoBlockworks.MODID, MissionDefinition.builder(mission, CHAPTER)
                .phase("blockworks_side_ops", "Blockworks Side Ops", 0, order)
                .text(title, briefing, fieldGuide)
                .category("Blockworks", "Side Op")
                .icon(icon)
                .kind(MissionKind.SIDE_OP)
                .objective(new ObjectiveDefinition(
                        id(missionPath + "/" + objectiveKey),
                        type,
                        objectiveLabel,
                        "",
                        icon,
                        1,
                        false,
                        Map.of("target", target.toString())))
                .reward(RewardDefinition.item(id(missionPath + "/reward"), MissionRewardClaimMode.CLAIMABLE, reward))
                .build());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoBlockworks.MODID, path);
    }

    private static ItemStack safeStack(Supplier<? extends ItemLike> item, ItemLike fallback, int count) {
        if (!EchoCoreServices.itemStackComponentsBound()) {
            return ItemStack.EMPTY;
        }
        try {
            ItemLike value = nativeLoaderActive() || item == null ? fallback : item.get();
            return value == null ? ItemStack.EMPTY : new ItemStack(value, Math.max(1, count));
        } catch (RuntimeException | LinkageError ignored) {
            return fallback == null ? ItemStack.EMPTY : new ItemStack(fallback, Math.max(1, count));
        }
    }

    private static boolean nativeLoaderActive() {
        return Boolean.getBoolean("echo.native.loader")
                || !System.getProperty("echo.native.moduleIds", "").isBlank()
                || !System.getProperty("echo.native.moduleClasspath", "").isBlank()
                || !System.getProperty("echo.native.moduleClasspathFile", "").isBlank();
    }
}
