package com.knoxhack.echoprimecore.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.mission.IMissionRegistry;
import com.echoplatform.echocore.api.mission.MissionChapterDefinition;
import com.echoplatform.echocore.api.mission.MissionDefinition;
import com.echoplatform.echocore.api.mission.MissionKind;
import com.echoplatform.echocore.api.mission.MissionObjectiveType;
import com.echoplatform.echocore.api.mission.ObjectiveDefinition;
import com.echoplatform.echocore.api.mission.RewardDefinition;
import com.knoxhack.echoprimecore.EchoPrimeCore;
import com.knoxhack.echoprimecore.PrimeIds;
import com.knoxhack.echoprimecore.progression.PrimePlayerData;
import com.knoxhack.echoprimecore.registry.ModItems;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class PrimeMissionBridge {
    private PrimeMissionBridge() {
    }

    public static void register(PrimeIntegrationRegistry registry) {
        EchoCoreServices.registerMissionContent(EchoPrimeCore.MODID, PrimeMissionBridge::registerContent);
    }

    private static void registerContent(IMissionRegistry registry) {
        registry.registerChapter(EchoPrimeCore.MODID, new MissionChapterDefinition(
                PrimeIds.CHAPTER_SURVIVAL,
                "Prime Survival",
                "Stable overworld survival route: signal, ruin, first tech, and branch discovery.",
                0,
                0xFF65E6D6));
        mission(registry, "prime_survival_begin", "Prime Survival: Begin",
                "Spawn normally, stabilize, and open your Prime Field Manual.",
                "World state is stable. Start with survival basics, then find a Signal Shard.",
                MissionObjectiveType.CUSTOM,
                EchoPrimeCore.id("started"),
                EchoPrimeCore.id("started"),
                new ItemStack(ModItems.PRIME_FIELD_MANUAL.get()),
                0,
                List.of());
        mission(registry, "first_signal", "First Signal",
                "Find a Signal Shard and craft a Crude Scanner.",
                "Prime signals are weak. A crude scanner is enough to resolve a nearby relay ruin.",
                MissionObjectiveType.CRAFT_ITEM,
                EchoPrimeCore.id("crude_scanner"),
                EchoPrimeCore.id("lens_online"),
                new ItemStack(ModItems.CRUDE_SCANNER.get()),
                10,
                List.of(EchoPrimeCore.id("mission/prime_survival_begin")));
        mission(registry, "first_ruin", "First Ruin",
                "Use the Crude Scanner and investigate the abandoned relay post.",
                "The first relay post teaches HoloMap markers, ruin discoveries, and cache loot.",
                MissionObjectiveType.DISCOVER_STRUCTURE,
                EchoPrimeCore.id("abandoned_relay_post"),
                EchoPrimeCore.id("first_ruin"),
                new ItemStack(ModItems.RELAY_FRAGMENT.get()),
                20,
                List.of(EchoPrimeCore.id("mission/first_signal")));
        mission(registry, "first_tech", "First Tech",
                "Loot Relay Fragment and Circuit Plate to reveal starter tech recipes.",
                "Index now exposes Prime Circuit and Machine Frame route hints.",
                MissionObjectiveType.OBTAIN_ITEM,
                EchoPrimeCore.id("relay_fragment"),
                EchoPrimeCore.id("first_signal"),
                new ItemStack(ModItems.CIRCUIT_PLATE.get()),
                30,
                List.of(EchoPrimeCore.id("mission/first_ruin")));
        mission(registry, "powergrid_online", "PowerGrid Online",
                "Open the power route when PowerGrid is installed.",
                "Use power cells, relay coils, and route cards to bring a grid online.",
                MissionObjectiveType.CUSTOM,
                EchoPrimeCore.id("powergrid_online"),
                EchoPrimeCore.id("powergrid_online"),
                new ItemStack(ModItems.BASIC_POWER_CELL.get()),
                40,
                List.of(EchoPrimeCore.id("mission/first_tech")));
        mission(registry, "storage_online", "Storage Online",
                "Open storage and logistics progression when those systems are installed.",
                "Storage chips and crates bridge Prime survival into networked logistics.",
                MissionObjectiveType.CUSTOM,
                EchoPrimeCore.id("storage_online"),
                EchoPrimeCore.id("storage_online"),
                new ItemStack(ModItems.STORAGE_CHIP.get()),
                50,
                List.of(EchoPrimeCore.id("mission/first_tech")));
        mission(registry, "base_online", "Base Online",
                "Open base progression when BaseGrid is installed.",
                "Field workbench and base anchors turn a shelter into an ECHO system.",
                MissionObjectiveType.CUSTOM,
                EchoPrimeCore.id("basegrid_online"),
                EchoPrimeCore.id("basegrid_online"),
                new ItemStack(ModItems.MACHINE_FRAME.get()),
                60,
                List.of(EchoPrimeCore.id("mission/first_tech")));
        mission(registry, "branch_discovery", "Branch Discovery",
                "Inspect installed route cards and choose the next survival route.",
                "Prime Core keeps routes dormant until their modules are installed and their flags open.",
                MissionObjectiveType.CUSTOM,
                EchoPrimeCore.id("first_machine"),
                EchoPrimeCore.id("first_machine"),
                new ItemStack(ModItems.PRIME_CIRCUIT.get()),
                70,
                List.of(EchoPrimeCore.id("mission/first_tech")));
    }

    private static void mission(IMissionRegistry registry, String path, String title, String briefing,
            String guide, MissionObjectiveType type, Identifier objectiveTarget, Identifier completionFlag,
            ItemStack icon, int order, List<Identifier> prerequisites) {
        Identifier id = EchoPrimeCore.id("mission/" + path);
        registry.registerMission(EchoPrimeCore.MODID, MissionDefinition.builder(id, PrimeIds.CHAPTER_SURVIVAL)
                .phase("prime_survival", "Prime Survival", 0, order)
                .text(title, briefing, guide)
                .category("Prime Survival", "Survival")
                .icon(icon)
                .prerequisites(prerequisites)
                .objective(ObjectiveDefinition.simple(
                        EchoPrimeCore.id("objective/" + path),
                        type,
                        title,
                        guide,
                        icon,
                        1))
                .reward(RewardDefinition.text(
                        EchoPrimeCore.id("reward/" + path),
                        "Prime route update",
                        "Updates the Prime dashboard and route readiness."))
                .kind(MissionKind.MAIN)
                .completionRule((player, mission) -> hasFlag(player, completionFlag))
                .metadata("prime_flag", completionFlag.toString())
                .metadata("objective_target", objectiveTarget.toString())
                .build());
    }

    private static boolean hasFlag(Player player, Identifier flag) {
        return player != null && PrimePlayerData.get(player).hasFlag(flag);
    }
}
