package com.knoxhack.signalos.integration;

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
import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.registry.ModBlocks;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class SignalOsMissionCoreIntegration {
    private static final Identifier CHAPTER = id("signalos");

    private SignalOsMissionCoreIntegration() {
    }

    public static void register() {
        EchoCoreServices.registerMissionContent(SignalOS.MODID, SignalOsMissionCoreIntegration::registerContent);
        SignalOsMissionHooks.registerCoverage();
    }

    public static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(SignalOS.MODID, new MissionChapterDefinition(
                CHAPTER,
                "SignalOS Side Ops",
                "Computer access, rack networking, and drive record workflows.",
                84,
                0xFF38DFF4));
        registerMission(registry, "boot_terminal", "boot", MissionObjectiveType.SCAN_BLOCK,
                "Boot Terminal", "Open a SignalOS terminal or workstation.",
                "SignalOS shell boot verified. Native app missions remain owned by SignalOS.",
                safeStack(() -> (ItemLike) ModBlocks.TERMINAL_ITEM.get(), Items.COMPASS, 1), 0, "Boot a SignalOS terminal", safeStack(() -> (ItemLike) ModBlocks.WORKSTATION_ITEM.get(), Items.CRAFTING_TABLE, 1));
        registerMission(registry, "rack_network_online", "rack", MissionObjectiveType.ESTABLISH_ROUTE,
                "Rack Network Online", "Open or populate a Server Rack with a data drive.",
                "Rack and drive network state is online.",
                safeStack(() -> (ItemLike) ModBlocks.SERVER_RACK_ITEM.get(), Items.OBSERVER, 1), 1, "Bring a rack network online", safeStack(() -> (ItemLike) ModBlocks.NETWORK_RELAY_ITEM.get(), Items.REPEATER, 1));
        registerMission(registry, "drive_record_flow", "record", MissionObjectiveType.UNLOCK_RESEARCH,
                "Drive Record Flow", "Copy a network record to a drive or apply a drive template.",
                "Drive record mutation reached the server-side rack workflow.",
                safeStack(() -> (ItemLike) ModBlocks.DATA_DRIVE.get(), Items.PAPER, 1), 2, "Copy or apply a drive record", safeStack(() -> (ItemLike) ModBlocks.DATA_DRIVE.get(), Items.PAPER, 1));
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
        Identifier target = MissionHookTargets.objectiveTarget(SignalOS.MODID, mission, objectiveKey);
        registry.registerMission(SignalOS.MODID, MissionDefinition.builder(mission, CHAPTER)
                .phase("signalos_side_ops", "SignalOS Side Ops", 0, order)
                .text(title, briefing, fieldGuide)
                .category("SignalOS", "Side Op")
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
        return Identifier.fromNamespaceAndPath(SignalOS.MODID, path);
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
