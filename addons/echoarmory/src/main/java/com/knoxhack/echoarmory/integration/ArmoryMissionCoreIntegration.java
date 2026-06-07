package com.knoxhack.echoarmory.integration;

import com.knoxhack.echoarmory.EchoArmory;
import com.knoxhack.echoarmory.registry.ModBlocks;
import com.knoxhack.echoarmory.registry.ModItems;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.IMissionRegistry;
import com.knoxhack.echocore.api.mission.MissionChapterDefinition;
import com.knoxhack.echocore.api.mission.MissionDefinition;
import com.knoxhack.echocore.api.mission.MissionHookTargets;
import com.knoxhack.echocore.api.mission.MissionKind;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echocore.api.mission.MissionRewardClaimMode;
import com.knoxhack.echocore.api.mission.ObjectiveDefinition;
import com.knoxhack.echocore.api.mission.RewardDefinition;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ArmoryMissionCoreIntegration {
    private static final Identifier CHAPTER = id("armory");

    private ArmoryMissionCoreIntegration() {
    }

    public static void register() {
        EchoCoreServices.registerMissionContent(EchoArmory.MODID, ArmoryMissionCoreIntegration::registerContent);
        ArmoryMissionHooks.registerCoverage();
    }

    public static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoArmory.MODID, new MissionChapterDefinition(
                CHAPTER,
                "Armory Side Ops",
                "Loadout inspection, upgrades, module work, energy recharge, and field kit binding.",
                70,
                0xFF8EDBFF));
        registerMission(registry, "inspect_loadout", "scan", MissionObjectiveType.SCAN_ENTITY,
                "Inspect Loadout", "Scan any Armory station with gear loaded.",
                "The loadout inspection feed is now linked into MissionCore.",
                new ItemStack(ModBlocks.ARMORY_BENCH.get()), 0,
                "Scan an Armory loadout", new ItemStack(Items.IRON_INGOT, 2));
        registerMission(registry, "forge_upgrade", "upgrade", MissionObjectiveType.CRAFT_ITEM,
                "Forge Upgrade", "Upgrade a weapon or armor tier at the correct Armory forge.",
                "The forge accepted the operator upgrade pattern.",
                new ItemStack(ModBlocks.WEAPON_FORGE.get()), 1,
                "Upgrade Armory gear", new ItemStack(ModItems.RESONANCE_SHARD.get(), 1));
        registerMission(registry, "install_module", "module", MissionObjectiveType.REPAIR_MACHINE,
                "Install Module", "Install any compatible Armory module into a loadout piece.",
                "Module bus handshake completed.",
                new ItemStack(ModBlocks.MODULE_UPGRADE_TABLE.get()), 2,
                "Install an Armory module", new ItemStack(ModItems.STABILITY_RUNE.get(), 1));
        registerMission(registry, "recharge_core", "recharge", MissionObjectiveType.REPAIR_MACHINE,
                "Recharge Core", "Recharge a depleted Armory energy core from AUX reserves.",
                "Energy reserves are field-ready.",
                new ItemStack(ModBlocks.ENERGY_CORE_CHARGING_STATION.get()), 3,
                "Recharge Armory energy", new ItemStack(ModItems.VEIL_CRYSTAL.get(), 1));
        registerMission(registry, "bind_loadout", "bind", MissionObjectiveType.SCAN_ENTITY,
                "Bind Loadout", "Bind gear to an operator kit through the Loadout Terminal.",
                "Operator field kit signature stored.",
                new ItemStack(ModBlocks.LOADOUT_TERMINAL.get()), 4,
                "Bind an operator loadout", new ItemStack(ModItems.ARMORY_ALLOY_PLATE.get(), 2));
        registerMission(registry, "prepare_route_kit", "prepare", MissionObjectiveType.CUSTOM,
                "Prepare Route Kit", "Bring an Armory route kit to full readiness.",
                "Route-kit readiness is now mission tracked.",
                new ItemStack(ModItems.GAS_MASK_FILTER.get()), 5,
                "Prepare an Armory route kit", new ItemStack(ModItems.AMMO_CRYSTALS.get(), 8));
        registerMission(registry, "dispatch_route_kit", "dispatch", MissionObjectiveType.ESTABLISH_ROUTE,
                "Dispatch Route Kit", "Queue an Armory kit through a Logistics loadout preset.",
                "Armory and Logistics dispatch lanes are linked.",
                new ItemStack(ModBlocks.LOADOUT_TERMINAL.get()), 6,
                "Dispatch an Armory route kit", new ItemStack(ModItems.RESONANCE_SHARD.get(), 1));
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
        Identifier target = MissionHookTargets.objectiveTarget(EchoArmory.MODID, mission, objectiveKey);
        Identifier objectiveId = id(missionPath + "/" + objectiveKey);
        if ("inspect_loadout".equals(missionPath)) {
            objectiveId = id("inspect_loadout/scan");
        }
        registry.registerMission(EchoArmory.MODID, MissionDefinition.builder(mission, CHAPTER)
                .phase("armory_side_ops", "Armory Side Ops", 0, order)
                .text(title, briefing, fieldGuide)
                .category("Armory", "Side Op")
                .icon(icon)
                .kind(MissionKind.SIDE_OP)
                .objective(new ObjectiveDefinition(
                        objectiveId,
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
        return Identifier.fromNamespaceAndPath(EchoArmory.MODID, path);
    }
}
