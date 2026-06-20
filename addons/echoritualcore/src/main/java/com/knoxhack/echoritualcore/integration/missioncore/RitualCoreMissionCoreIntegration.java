package com.knoxhack.echoritualcore.integration.missioncore;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.IMissionRegistry;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.mission.MissionRewardClaimMode;
import com.echoplatform.echocore.api.mission.ObjectiveDefinition;
import com.echoplatform.echocore.api.mission.RewardDefinition;
import com.knoxhack.echoritualcore.EchoRitualCore;
import com.knoxhack.echoritualcore.api.RitualCoreApi;
import com.knoxhack.echoritualcore.api.RitualCoreEvents;
import com.knoxhack.echoritualcore.registry.ModBlocks;
import com.knoxhack.echoritualcore.registry.ModItems;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class RitualCoreMissionCoreIntegration {
    private static final Identifier CHAPTER_ID = id("arcana_ritualcore");

    private RitualCoreMissionCoreIntegration() {
    }

    public static void register() {
        EchoRitualCore.LOGGER.info("ECHO MissionCore integration loaded for RitualCore.");
        EchoCoreServices.registerMissionContent(EchoRitualCore.MODID, RitualCoreMissionCoreIntegration::registerContent);
        registerEventHooks();
        registerHookCoverage();
    }

    private static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoRitualCore.MODID, new MissionChapterDefinition(
                CHAPTER_ID,
                "ECHO: RitualCore",
                "Build ritual circuits, stabilize relics, cleanse corruption, and learn backlash control.",
                70,
                0xB072FF));

        registerMission(registry, "build_basic_altar", "Build Basic Altar", "Craft and place the first shared ritual center.",
                "Basic altar online.", MissionObjectiveType.OBTAIN_ITEM, id("basic_altar"), safeStack(() -> (ItemLike) ModBlocks.BASIC_ALTAR.get(), Items.CRAFTING_TABLE, 1));
        registerMission(registry, "place_rune_circle", "Place Rune Circle", "Lay a ritual circuit block for array guidance.",
                "Rune circuit placed.", MissionObjectiveType.OBTAIN_ITEM, id("rune_circle"), safeStack(() -> (ItemLike) ModBlocks.RUNE_CIRCLE.get(), Items.REDSTONE, 1));
        registerMission(registry, "complete_aether_calibration", "Complete Aether Calibration",
                "Build a basic array and use Aether Chalk to condense a safe sample.",
                "Aether signal calibrated.", MissionObjectiveType.CUSTOM, RitualCoreApi.AETHER_CALIBRATION,
                safeStack(() -> (ItemLike) ModItems.AETHER_CHALK.get(), Items.GLOWSTONE_DUST, 1));
        registerMission(registry, "complete_relic_stabilization", "Complete Relic Stabilization",
                "Use a Basic Altar and Stability Seal on a damaged RelicTech relic.",
                "Relic stabilized by ritual.", MissionObjectiveType.CUSTOM, RitualCoreApi.RELIC_STABILIZATION,
                safeStack(() -> (ItemLike) ModItems.STABILITY_SEAL.get(), Items.LAPIS_LAZULI, 1));
        registerMission(registry, "cleanse_cursed_relic", "Cleanse Cursed Relic",
                "Sneak-use a Basic Altar with a corrupted relic and Purity Catalyst.",
                "Relic curse cleansed.", MissionObjectiveType.CUSTOM, id("relic_curse_cleansed"),
                safeStack(() -> (ItemLike) ModItems.PURITY_CATALYST.get(), Items.AMETHYST_SHARD, 1));
        registerMission(registry, "awaken_spell_core", "Awaken Spell Core",
                "Use Ritual Focus and Refined Aether Sample in a complete basic array.",
                "Spell core awakened.", MissionObjectiveType.CUSTOM, RitualCoreApi.SPELL_CORE_AWAKENING,
                safeStack(() -> (ItemLike) ModItems.AWAKENED_SPELL_CORE.get(), Items.ENDER_EYE, 1));
        registerMission(registry, "reveal_rift_crack", "Reveal Rift Crack",
                "Spend Refined Aether Sample and Aether Chalk to generate a HoloMap rift trace.",
                "Rift trace plotted.", MissionObjectiveType.CUSTOM, RitualCoreApi.RIFT_CRACK_REVEAL,
                safeStack(() -> (ItemLike) ModItems.REFINED_AETHER_SAMPLE.get(), Items.AMETHYST_SHARD, 1));
        registerMission(registry, "prevent_ritual_backlash", "Prevent Ritual Backlash",
                "Fail safely by checking required ritual catalysts before ignition.",
                "Backlash prevented.", MissionObjectiveType.CUSTOM, id("ritual_failure_checked"),
                safeStack(() -> (ItemLike) ModBlocks.STABILITY_PYLON.get(), Items.LIGHTNING_ROD, 1));
    }

    private static void registerMission(IMissionRegistry registry, String path, String title, String briefing,
            String fieldGuide, MissionObjectiveType type, Identifier target, ItemStack icon) {
        Identifier missionId = id("arcana_ritualcore/" + path);
        ObjectiveDefinition objective = new ObjectiveDefinition(
                id("arcana_ritualcore/" + path + "_objective"),
                type,
                title,
                "",
                icon,
                1,
                false,
                Map.of("target", target.toString()));
        RewardDefinition reward = RewardDefinition.item(
                id("arcana_ritualcore/" + path + "_reward"),
                MissionRewardClaimMode.CLAIMABLE,
                rewardStack(path));
        MissionDefinition mission = MissionDefinition.builder(missionId, CHAPTER_ID)
                .phase(phaseId(path), phaseTitle(path), phaseOrder(path), missionOrder(path))
                .text(title, briefing, fieldGuide)
                .category("RitualCore", "Arcana")
                .icon(icon)
                .objective(objective)
                .reward(reward)
                .kind(MissionKind.SIDE_OP)
                .build();
        registry.registerMission(EchoRitualCore.MODID, mission);
    }

    private static void registerEventHooks() {
        RitualCoreEvents.onComplete(event -> EchoCoreServices.recordMissionObjective(
                event.player(),
                MissionObjectiveType.CUSTOM,
                event.ritualId(),
                1,
                Map.of("source", EchoRitualCore.MODID, "subject", event.subjectId().toString())));
        RitualCoreEvents.onFailure(event -> EchoCoreServices.recordMissionObjective(
                event.player(),
                MissionObjectiveType.CUSTOM,
                id("ritual_failure_checked"),
                1,
                Map.of("source", EchoRitualCore.MODID, "ritual", event.ritualId().toString(), "reason", event.reason())));
    }

    private static void registerHookCoverage() {
        EchoCoreServices.registerMissionHookCoverage(EchoRitualCore.MODID, id("arcana_ritualcore/build_basic_altar"), id("basic_altar"));
        EchoCoreServices.registerMissionHookCoverage(EchoRitualCore.MODID, id("arcana_ritualcore/place_rune_circle"), id("rune_circle"));
        EchoCoreServices.registerMissionHookCoverage(EchoRitualCore.MODID, id("arcana_ritualcore/complete_aether_calibration"), RitualCoreApi.AETHER_CALIBRATION);
        EchoCoreServices.registerMissionHookCoverage(EchoRitualCore.MODID, id("arcana_ritualcore/complete_relic_stabilization"), RitualCoreApi.RELIC_STABILIZATION);
        EchoCoreServices.registerMissionHookCoverage(EchoRitualCore.MODID, id("arcana_ritualcore/cleanse_cursed_relic"), id("relic_curse_cleansed"));
        EchoCoreServices.registerMissionHookCoverage(EchoRitualCore.MODID, id("arcana_ritualcore/awaken_spell_core"), RitualCoreApi.SPELL_CORE_AWAKENING);
        EchoCoreServices.registerMissionHookCoverage(EchoRitualCore.MODID, id("arcana_ritualcore/reveal_rift_crack"), RitualCoreApi.RIFT_CRACK_REVEAL);
        EchoCoreServices.registerMissionHookCoverage(EchoRitualCore.MODID, id("arcana_ritualcore/prevent_ritual_backlash"), id("ritual_failure_checked"));
    }

    private static String phaseId(String path) {
        return switch (path) {
            case "build_basic_altar", "place_rune_circle", "complete_aether_calibration" -> "first_circle";
            case "complete_relic_stabilization", "awaken_spell_core", "reveal_rift_crack" -> "stable_rituals";
            case "cleanse_cursed_relic" -> "corrupted_rituals";
            case "prevent_ritual_backlash" -> "backlash_control";
            default -> "ritualcore";
        };
    }

    private static String phaseTitle(String path) {
        return switch (phaseId(path)) {
            case "first_circle" -> "First Circle";
            case "stable_rituals" -> "Stable Rituals";
            case "corrupted_rituals" -> "Corrupted Rituals";
            case "backlash_control" -> "Backlash Control";
            default -> "RitualCore";
        };
    }

    private static int phaseOrder(String path) {
        return switch (phaseId(path)) {
            case "first_circle" -> 10;
            case "stable_rituals" -> 20;
            case "corrupted_rituals" -> 30;
            case "backlash_control" -> 40;
            default -> 99;
        };
    }

    private static int missionOrder(String path) {
        return switch (path) {
            case "build_basic_altar" -> 10;
            case "place_rune_circle" -> 20;
            case "complete_aether_calibration" -> 30;
            case "complete_relic_stabilization" -> 40;
            case "cleanse_cursed_relic" -> 50;
            case "awaken_spell_core" -> 60;
            case "reveal_rift_crack" -> 70;
            case "prevent_ritual_backlash" -> 80;
            default -> 99;
        };
    }

    private static ItemStack rewardStack(String path) {
        return switch (path) {
            case "build_basic_altar" -> safeStack(() -> (ItemLike) ModItems.AETHER_CHALK.get(), Items.GLOWSTONE_DUST, 4);
            case "place_rune_circle" -> safeStack(() -> (ItemLike) ModItems.RITUAL_FOCUS.get(), Items.ENDER_EYE, 1);
            case "complete_aether_calibration" -> safeStack(() -> (ItemLike) ModItems.REFINED_AETHER_SAMPLE.get(), Items.AMETHYST_SHARD, 2);
            case "complete_relic_stabilization" -> safeStack(() -> (ItemLike) ModItems.REFINED_AETHER_SAMPLE.get(), Items.AMETHYST_SHARD, 2);
            case "cleanse_cursed_relic" -> safeStack(() -> (ItemLike) ModItems.CURSE_ASH.get(), Items.BONE_MEAL, 1);
            case "awaken_spell_core" -> safeStack(() -> (ItemLike) ModItems.AETHER_CHALK.get(), Items.GLOWSTONE_DUST, 4);
            case "reveal_rift_crack" -> safeStack(() -> (ItemLike) ModItems.STABILITY_SEAL.get(), Items.LAPIS_LAZULI, 1);
            default -> safeStack(() -> (ItemLike) ModItems.STABILITY_SEAL.get(), Items.LAPIS_LAZULI, 1);
        };
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRitualCore.MODID, path);
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
