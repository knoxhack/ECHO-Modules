package com.knoxhack.echorelictech.integration.missioncore;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.IMissionRegistry;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.mission.MissionRewardClaimMode;
import com.echoplatform.echocore.api.mission.ObjectiveDefinition;
import com.echoplatform.echocore.api.mission.RewardDefinition;
import com.knoxhack.echorelictech.EchoRelicTech;
import com.knoxhack.echorelictech.api.event.RelicTechEvents;
import com.knoxhack.echorelictech.registry.ModBlocks;
import com.knoxhack.echorelictech.registry.ModItems;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public class RelicTechMissionCoreIntegration {
    private static final Identifier CHAPTER_ID = id("arcana_relictech");

    public static void register() {
        EchoRelicTech.LOGGER.info("ECHO MissionCore integration loaded for RelicTech.");
        EchoCoreServices.registerMissionContent(EchoRelicTech.MODID, RelicTechMissionCoreIntegration::registerContent);
        registerEventHooks();
        registerHookCoverage();
    }

    private static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoRelicTech.MODID, new MissionChapterDefinition(
                CHAPTER_ID,
                "ECHO: RelicTech",
                "Recover, decode, stabilize, and risk pre-Gridfall relic devices.",
                60,
                0xFFAA44));

        registerMission(registry,
                "find_unknown_relic",
                "Find Unknown Relic",
                "Find an unidentified relic in a vault.",
                "Relic recovered.",
                MissionObjectiveType.OBTAIN_ITEM,
                id("unidentified_relic"),
                safeStack(() -> (ItemLike) ModItems.UNIDENTIFIED_RELIC.get(), Items.ENDER_EYE, 1));

        registerMission(registry,
                "scan_unknown_relic",
                "Scan Unknown Relic",
                "Use a Relic Analyzer or Lens route to expose a relic signature.",
                "Unknown relic signature scanned.",
                MissionObjectiveType.CUSTOM,
                id("unknown_relic_scanned"),
                safeStack(() -> (ItemLike) ModItems.RELIC_DIAGNOSTIC_REPORT.get(), Items.PAPER, 1));

        registerMission(registry,
                "decode_first_relic",
                "Decode First Relic",
                "Use a Relic Analyzer to identify a relic.",
                "Relic decoded.",
                MissionObjectiveType.CUSTOM,
                id("first_relic_decoded"),
                safeStack(() -> (ItemLike) ModBlocks.RELIC_ANALYZER.get(), Items.LODESTONE, 1));

        registerMission(registry,
                "stabilize_first_relic",
                "Stabilize First Relic",
                "Use a Prototype Workbench to stabilize a relic.",
                "Relic stabilized.",
                MissionObjectiveType.CUSTOM,
                id("first_relic_stabilized"),
                safeStack(() -> (ItemLike) ModBlocks.PROTOTYPE_WORKBENCH.get(), Items.SMITHING_TABLE, 1));

        registerMission(registry,
                "charge_null_battery",
                "Charge a Null Battery",
                "Use a Null Battery Dock and Null Cells to prepare relic power.",
                "Null Battery charged.",
                MissionObjectiveType.CUSTOM,
                id("null_battery_charged"),
                safeStack(() -> (ItemLike) ModItems.NULL_BATTERY.get(), Items.REDSTONE, 1));

        registerMission(registry,
                "use_relic_ability",
                "Use Relic Ability",
                "Activate any decoded RelicTech device.",
                "Relic activation logged.",
                MissionObjectiveType.CUSTOM,
                id("relic_ability_used"),
                safeStack(() -> (ItemLike) ModItems.PHASE_ANCHOR.get(), Items.ENDER_PEARL, 1));

        registerMission(registry,
                "survive_relic_backlash",
                "Survive Relic Backlash",
                "Trigger and survive a relic malfunction.",
                "Relic backlash logged.",
                MissionObjectiveType.CUSTOM,
                id("relic_backlash_survived"),
                safeStack(() -> (ItemLike) ModItems.PHASE_ANCHOR.get(), Items.ENDER_PEARL, 1));

        registerMission(registry,
                "bind_relic",
                "Bind Relic",
                "Bind a Phase Anchor or future bound relic.",
                "Relic binding logged.",
                MissionObjectiveType.CUSTOM,
                id("phase_anchor"),
                safeStack(() -> (ItemLike) ModItems.PHASE_ANCHOR.get(), Items.ENDER_PEARL, 1));

        registerMission(registry,
                "use_guardian_lens",
                "Use a Guardian Lens",
                "Scan relic signatures with a Guardian Lens.",
                "Guardian scan logged.",
                MissionObjectiveType.CUSTOM,
                id("guardian_lens"),
                safeStack(() -> (ItemLike) ModItems.GUARDIAN_LENS.get(), Items.SPYGLASS, 1));

        registerMission(registry,
                "use_echo_mirror",
                "Use an Echo Mirror",
                "Deploy an Echo Mirror projection.",
                "Echo projection logged.",
                MissionObjectiveType.CUSTOM,
                id("echo_mirror"),
                safeStack(() -> (ItemLike) ModItems.ECHO_MIRROR.get(), Items.GLASS_PANE, 1));

        registerMission(registry,
                "use_gravity_clamp",
                "Use Gravity Clamp",
                "Push or pull nearby entities with a Gravity Clamp.",
                "Gravity clamp pulse logged.",
                MissionObjectiveType.CUSTOM,
                id("gravity_clamp"),
                safeStack(() -> (ItemLike) ModItems.GRAVITY_CLAMP.get(), Items.IRON_INGOT, 1));

        registerMission(registry,
                "use_rift_lantern",
                "Use Rift Lantern",
                "Reveal nearby hostile signatures with a Rift Lantern.",
                "Rift lantern sweep logged.",
                MissionObjectiveType.CUSTOM,
                id("rift_lantern"),
                safeStack(() -> (ItemLike) ModItems.RIFT_LANTERN.get(), Items.SOUL_LANTERN, 1));

        registerMission(registry,
                "use_void_compass",
                "Use Void Compass",
                "Locate a pre-Gridfall relic vault with a Void Compass.",
                "Vault coordinate logged.",
                MissionObjectiveType.CUSTOM,
                id("void_compass"),
                safeStack(() -> (ItemLike) ModItems.VOID_COMPASS.get(), Items.COMPASS, 1));

        registerMission(registry,
                "use_matter_stitcher",
                "Use a Matter Stitcher",
                "Repair armor or recover health with a Matter Stitcher.",
                "Matter stitch logged.",
                MissionObjectiveType.CUSTOM,
                id("matter_stitcher"),
                safeStack(() -> (ItemLike) ModItems.MATTER_STITCHER.get(), Items.SHEARS, 1));

        registerMission(registry,
                "contain_relic",
                "Contain a Relic",
                "Store a risky relic in a Containment Locker.",
                "Relic containment logged.",
                MissionObjectiveType.CUSTOM,
                id("relic_contained"),
                safeStack(() -> (ItemLike) ModBlocks.CONTAINMENT_LOCKER.get(), Items.CHEST, 1));

        registerMission(registry,
                "discover_cursed_relic",
                "Discover Cursed Relic",
                "Decode or activate a forbidden relic.",
                "Cursed relic discovery logged.",
                MissionObjectiveType.CUSTOM,
                id("cursed_relic_discovered"),
                safeStack(() -> (ItemLike) ModItems.BLOOD_CIRCUIT.get(), Items.REDSTONE, 1));

        registerMission(registry,
                "recover_legendary_frame",
                "Recover Legendary Relic Frame",
                "Recover a Legendary Relic Frame from late relic operations.",
                "Legendary frame recovered.",
                MissionObjectiveType.OBTAIN_ITEM,
                id("legendary_relic_frame"),
                safeStack(() -> (ItemLike) ModItems.LEGENDARY_RELIC_FRAME.get(), Items.NETHER_STAR, 1));

        registerMission(registry,
                "discover_vault",
                "Discover a Relic Vault",
                "Locate a pre-Gridfall vault record.",
                "Vault discovery logged.",
                MissionObjectiveType.DISCOVER_STRUCTURE,
                id("pre_gridfall_research_vault"),
                safeStack(() -> (ItemLike) ModBlocks.RELIC_VAULT_DOOR.get(), Items.IRON_DOOR, 1));
    }

    private static void registerMission(
            IMissionRegistry registry,
            String path,
            String title,
            String briefing,
            String fieldGuide,
            MissionObjectiveType type,
            Identifier target,
            ItemStack icon) {
        Identifier missionId = id("arcana_relictech/" + path);
        ObjectiveDefinition objective = new ObjectiveDefinition(
                id("arcana_relictech/" + path + "_objective"),
                type,
                title,
                "",
                icon,
                1,
                false,
                Map.of("target", target.toString()));
        RewardDefinition reward = RewardDefinition.item(
                id("arcana_relictech/" + path + "_relic_reward"),
                MissionRewardClaimMode.CLAIMABLE,
                rewardStack(path));
        MissionDefinition mission = MissionDefinition.builder(missionId, CHAPTER_ID)
                .phase(phaseId(path), phaseTitle(path), phaseOrder(path), missionOrder(path))
                .text(title, briefing, fieldGuide)
                .category("RelicTech", "Field")
                .icon(icon)
                .objective(objective)
                .reward(reward)
                .kind(MissionKind.SIDE_OP)
                .build();
        registry.registerMission(EchoRelicTech.MODID, mission);
    }

    private static int missionOrder(String path) {
        return switch (path) {
            case "find_unknown_relic" -> 10;
            case "scan_unknown_relic" -> 20;
            case "decode_first_relic" -> 30;
            case "stabilize_first_relic" -> 40;
            case "charge_null_battery" -> 42;
            case "use_relic_ability" -> 50;
            case "survive_relic_backlash" -> 55;
            case "bind_relic" -> 58;
            case "use_guardian_lens" -> 60;
            case "use_echo_mirror" -> 70;
            case "use_gravity_clamp" -> 72;
            case "use_rift_lantern" -> 74;
            case "use_void_compass" -> 76;
            case "use_matter_stitcher" -> 80;
            case "contain_relic" -> 90;
            case "discover_cursed_relic" -> 95;
            case "recover_legendary_frame" -> 98;
            case "discover_vault" -> 100;
            default -> 99;
        };
    }

    private static String phaseId(String path) {
        return switch (path) {
            case "find_unknown_relic", "scan_unknown_relic" -> "r1_unknown_artifacts";
            case "decode_first_relic" -> "r2_decoding";
            case "stabilize_first_relic", "charge_null_battery" -> "r3_stabilization";
            case "use_relic_ability", "survive_relic_backlash", "contain_relic", "discover_cursed_relic" -> "r4_risk_and_curse";
            case "bind_relic", "use_guardian_lens", "use_echo_mirror", "use_gravity_clamp", "use_rift_lantern",
                    "use_void_compass", "use_matter_stitcher" -> "r5_binding";
            case "recover_legendary_frame", "discover_vault" -> "r6_legendary_relics";
            default -> "relic_ops";
        };
    }

    private static String phaseTitle(String path) {
        return switch (phaseId(path)) {
            case "r1_unknown_artifacts" -> "R1: Unknown Artifacts";
            case "r2_decoding" -> "R2: Decoding";
            case "r3_stabilization" -> "R3: Stabilization";
            case "r4_risk_and_curse" -> "R4: Risk and Curse";
            case "r5_binding" -> "R5: Binding";
            case "r6_legendary_relics" -> "R6: Legendary Relics";
            default -> "Relic Operations";
        };
    }

    private static int phaseOrder(String path) {
        return switch (phaseId(path)) {
            case "r1_unknown_artifacts" -> 10;
            case "r2_decoding" -> 20;
            case "r3_stabilization" -> 30;
            case "r4_risk_and_curse" -> 40;
            case "r5_binding" -> 50;
            case "r6_legendary_relics" -> 60;
            default -> 99;
        };
    }

    private static ItemStack rewardStack(String path) {
        return switch (path) {
            case "find_unknown_relic" -> safeStack(() -> (ItemLike) ModItems.RELIC_SHARD.get(), Items.AMETHYST_SHARD, 4);
            case "scan_unknown_relic" -> safeStack(() -> (ItemLike) ModItems.RELIC_DIAGNOSTIC_REPORT.get(), Items.PAPER, 1);
            case "decode_first_relic" -> safeStack(() -> (ItemLike) ModItems.PRE_GRIDFALL_CIRCUIT.get(), Items.REDSTONE, 2);
            case "stabilize_first_relic" -> safeStack(() -> (ItemLike) ModItems.STABILIZED_RIFTSTONE.get(), Items.AMETHYST_SHARD, 1);
            case "charge_null_battery" -> safeStack(() -> (ItemLike) ModItems.NULL_CELL.get(), Items.REDSTONE, 3);
            case "contain_relic" -> safeStack(() -> (ItemLike) ModItems.CONTAINMENT_GLASS.get(), Items.GLASS, 2);
            case "discover_cursed_relic" -> safeStack(() -> (ItemLike) ModItems.FORBIDDEN_PROTOTYPE_FILE.get(), Items.PAPER, 1);
            case "recover_legendary_frame" -> safeStack(() -> (ItemLike) ModItems.LEGENDARY_RELIC_FRAME.get(), Items.NETHER_STAR, 1);
            case "discover_vault" -> safeStack(() -> (ItemLike) ModItems.RELIC_DIAGNOSTIC_REPORT.get(), Items.PAPER, 1);
            default -> safeStack(() -> (ItemLike) ModItems.RELIC_SHARD.get(), Items.AMETHYST_SHARD, 2);
        };
    }

    private static void registerEventHooks() {
        RelicTechEvents.onAnalyze(event -> EchoCoreServices.recordMissionObjective(
                event.player(),
                MissionObjectiveType.REPAIR_MACHINE,
                id("relic_analyzer"),
                1,
                Map.of("source", EchoRelicTech.MODID)));
        RelicTechEvents.onAnalyze(event -> {
            EchoCoreServices.recordMissionObjective(
                    event.player(),
                    MissionObjectiveType.CUSTOM,
                    id("unknown_relic_scanned"),
                    1,
                    Map.of("source", EchoRelicTech.MODID));
            EchoCoreServices.recordMissionObjective(
                    event.player(),
                    MissionObjectiveType.CUSTOM,
                    id("first_relic_decoded"),
                    1,
                    Map.of("source", EchoRelicTech.MODID));
            if (event.result().is(ModItems.ECHO_MIRROR.get()) || event.result().is(ModItems.BLOOD_CIRCUIT.get())) {
                EchoCoreServices.recordMissionObjective(
                        event.player(),
                        MissionObjectiveType.CUSTOM,
                        id("cursed_relic_discovered"),
                        1,
                        Map.of("source", EchoRelicTech.MODID));
            }
        });

        RelicTechEvents.onWorkbench(event -> EchoCoreServices.recordMissionObjective(
                event.player(),
                MissionObjectiveType.REPAIR_MACHINE,
                id("prototype_workbench"),
                1,
                Map.of("source", EchoRelicTech.MODID)));
        RelicTechEvents.onWorkbench(event -> {
            if (event.toCondition() == com.knoxhack.echorelictech.api.relic.RelicCondition.STABILIZED) {
                EchoCoreServices.recordMissionObjective(
                        event.player(),
                        MissionObjectiveType.CUSTOM,
                        id("first_relic_stabilized"),
                        1,
                        Map.of("source", EchoRelicTech.MODID));
            }
        });

        RelicTechEvents.onVaultDiscover(event -> EchoCoreServices.recordMissionObjective(
                event.player(),
                MissionObjectiveType.DISCOVER_STRUCTURE,
                event.vaultId(),
                1,
                Map.of("source", EchoRelicTech.MODID)));

        RelicTechEvents.onUse(event -> EchoCoreServices.recordMissionObjective(
                event.player(),
                MissionObjectiveType.CUSTOM,
                event.relicId(),
                1,
                Map.of("source", EchoRelicTech.MODID, "action", "relic_use")));
        RelicTechEvents.onUse(event -> {
            EchoCoreServices.recordMissionObjective(
                    event.player(),
                    MissionObjectiveType.CUSTOM,
                    id("relic_ability_used"),
                    1,
                    Map.of("source", EchoRelicTech.MODID, "relic", event.relicId().toString()));
            if (event.relicId().equals(id("echo_mirror")) || event.relicId().equals(id("blood_circuit"))) {
                EchoCoreServices.recordMissionObjective(
                        event.player(),
                        MissionObjectiveType.CUSTOM,
                        id("cursed_relic_discovered"),
                        1,
                        Map.of("source", EchoRelicTech.MODID, "relic", event.relicId().toString()));
            }
        });

        RelicTechEvents.onContain(event -> EchoCoreServices.recordMissionObjective(
                event.player(),
                MissionObjectiveType.CUSTOM,
                id("relic_contained"),
                1,
                Map.of("source", EchoRelicTech.MODID)));

        RelicTechEvents.onFailure(event -> EchoCoreServices.recordMissionObjective(
                event.player(),
                MissionObjectiveType.CUSTOM,
                id("relic_backlash_survived"),
                1,
                Map.of("source", EchoRelicTech.MODID, "severity", event.severity())));
    }

    private static void registerHookCoverage() {
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/find_unknown_relic"), id("unidentified_relic"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/scan_unknown_relic"), id("unknown_relic_scanned"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/decode_first_relic"), id("first_relic_decoded"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/stabilize_first_relic"), id("first_relic_stabilized"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/charge_null_battery"), id("null_battery_charged"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/use_relic_ability"), id("relic_ability_used"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/survive_relic_backlash"), id("relic_backlash_survived"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/bind_relic"), id("phase_anchor"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/use_guardian_lens"), id("guardian_lens"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/use_echo_mirror"), id("echo_mirror"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/use_gravity_clamp"), id("gravity_clamp"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/use_rift_lantern"), id("rift_lantern"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/use_void_compass"), id("void_compass"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/use_matter_stitcher"), id("matter_stitcher"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/contain_relic"), id("relic_contained"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/discover_cursed_relic"), id("cursed_relic_discovered"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/recover_legendary_frame"), id("legendary_relic_frame"));
        EchoCoreServices.registerMissionHookCoverage(EchoRelicTech.MODID, id("arcana_relictech/discover_vault"), id("pre_gridfall_research_vault"));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRelicTech.MODID, path);
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
