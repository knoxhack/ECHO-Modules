package com.knoxhack.echoarmory.integration;

import com.knoxhack.echoarmory.EchoArmory;
import com.knoxhack.echoarmory.registry.ModBlocks;
import com.knoxhack.echoarmory.registry.ModItems;
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
                safeStack(() -> (ItemLike) ModBlocks.ARMORY_BENCH.get(), Items.CRAFTING_TABLE, 1), 0,
                "Scan an Armory loadout", safeStack(() -> Items.IRON_INGOT, Items.IRON_INGOT, 2));
        registerMission(registry, "forge_upgrade", "upgrade", MissionObjectiveType.CRAFT_ITEM,
                "Forge Upgrade", "Upgrade a weapon or armor tier at the correct Armory forge.",
                "The forge accepted the operator upgrade pattern.",
                safeStack(() -> (ItemLike) ModBlocks.WEAPON_FORGE.get(), Items.ANVIL, 1), 1,
                "Upgrade Armory gear", safeStack(() -> (ItemLike) ModItems.RESONANCE_SHARD.get(), Items.AMETHYST_SHARD, 1));
        registerMission(registry, "install_module", "module", MissionObjectiveType.REPAIR_MACHINE,
                "Install Module", "Install any compatible Armory module into a loadout piece.",
                "Module bus handshake completed.",
                safeStack(() -> (ItemLike) ModBlocks.MODULE_UPGRADE_TABLE.get(), Items.SMITHING_TABLE, 1), 2,
                "Install an Armory module", safeStack(() -> (ItemLike) ModItems.STABILITY_RUNE.get(), Items.LAPIS_LAZULI, 1));
        registerMission(registry, "recharge_core", "recharge", MissionObjectiveType.REPAIR_MACHINE,
                "Recharge Core", "Recharge a depleted Armory energy core from AUX reserves.",
                "Energy reserves are field-ready.",
                safeStack(() -> (ItemLike) ModBlocks.ENERGY_CORE_CHARGING_STATION.get(), Items.BLAST_FURNACE, 1), 3,
                "Recharge Armory energy", safeStack(() -> (ItemLike) ModItems.VEIL_CRYSTAL.get(), Items.AMETHYST_SHARD, 1));
        registerMission(registry, "bind_loadout", "bind", MissionObjectiveType.SCAN_ENTITY,
                "Bind Loadout", "Bind gear to an operator kit through the Loadout Terminal.",
                "Operator field kit signature stored.",
                safeStack(() -> (ItemLike) ModBlocks.LOADOUT_TERMINAL.get(), Items.LECTERN, 1), 4,
                "Bind an operator loadout", safeStack(() -> (ItemLike) ModItems.ARMORY_ALLOY_PLATE.get(), Items.IRON_INGOT, 2));
        registerMission(registry, "prepare_route_kit", "prepare", MissionObjectiveType.CUSTOM,
                "Prepare Route Kit", "Bring an Armory route kit to full readiness.",
                "Route-kit readiness is now mission tracked.",
                safeStack(() -> (ItemLike) ModItems.GAS_MASK_FILTER.get(), Items.PAPER, 1), 5,
                "Prepare an Armory route kit", safeStack(() -> (ItemLike) ModItems.AMMO_CRYSTALS.get(), Items.REDSTONE, 8));
        registerMission(registry, "dispatch_route_kit", "dispatch", MissionObjectiveType.ESTABLISH_ROUTE,
                "Dispatch Route Kit", "Queue an Armory kit through a Logistics loadout preset.",
                "Armory and Logistics dispatch lanes are linked.",
                safeStack(() -> (ItemLike) ModBlocks.LOADOUT_TERMINAL.get(), Items.LECTERN, 1), 6,
                "Dispatch an Armory route kit", safeStack(() -> (ItemLike) ModItems.RESONANCE_SHARD.get(), Items.AMETHYST_SHARD, 1));
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
