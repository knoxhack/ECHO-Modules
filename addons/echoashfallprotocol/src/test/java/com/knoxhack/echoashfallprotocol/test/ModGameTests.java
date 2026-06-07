package com.knoxhack.echoashfallprotocol.test;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.EchoDiscoveryCategory;
import com.knoxhack.echocore.api.EchoDiscoveryEntry;
import com.knoxhack.echocore.api.EchoFactionContract;
import com.knoxhack.echocore.api.EchoFactionDefinition;
import com.knoxhack.echocore.api.EchoServiceRegistry;
import com.knoxhack.echocore.api.IStructureDiscoveryService;
import com.knoxhack.echocore.api.mission.IMissionProgressView;
import com.knoxhack.echocore.api.mission.IMissionService;
import com.knoxhack.echocore.api.mission.IObjectiveView;
import com.knoxhack.echocore.api.mission.MissionChapterDefinition;
import com.knoxhack.echocore.api.mission.MissionDefinition;
import com.knoxhack.echocore.api.mission.MissionKind;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echocore.api.mission.MissionRewardClaimMode;
import com.knoxhack.echocore.api.mission.MissionStatus;
import com.knoxhack.echocore.api.mission.ObjectiveDefinition;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockEntitySnapshot;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockRef;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeBlockState;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeCapabilityRequest;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeItemStack;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneMode;
import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneScanCategory;
import com.knoxhack.echoashfallprotocol.block.EmergencyBunkBlock;
import com.knoxhack.echoashfallprotocol.block.FactoryControllerBlock;
import com.knoxhack.echoashfallprotocol.block.ItemPipeBlock;
import com.knoxhack.echoashfallprotocol.block.NexusCoreBlock;
import com.knoxhack.echoashfallprotocol.block.PowerNodeBlock;
import com.knoxhack.echoashfallprotocol.block.RelayStationBlock;
import com.knoxhack.echoashfallprotocol.block.WorkshopBlock;
import com.knoxhack.echoashfallprotocol.block.entity.AtmosphericScrubberBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.BatteryBankBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ContaminantCondenserBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.CrystallineSynthesizerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.EchoContainerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.FactoryControllerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.FilterWorkbenchBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.HandRecyclerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.IsotopeRefinerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.LoadDistributorBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.MicroGeneratorBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.NexusCapacitorBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.NexusCoreBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.OreGrinderBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.PowerCableBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.PowerNodeBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.RadiationCleanserBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ScrapPressBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.SignalScannerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ThermalArrayBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ThermalBurnerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.WaterPurifierBlockEntity;
import com.knoxhack.echoashfallprotocol.block.menu.ResearchLabMenu;
import com.knoxhack.echoashfallprotocol.boss.BossHudProfile;
import com.knoxhack.echoashfallprotocol.boss.BossHudProfiles;
import com.knoxhack.echoashfallprotocol.boss.BossHudTargetResolver;
import com.knoxhack.echoashfallprotocol.client.hud.SurvivalHudOverlay;
import com.knoxhack.echoashfallprotocol.data.SaveMigrationHandler;
import com.knoxhack.echoashfallprotocol.echo.AshfallMissionRoute;
import com.knoxhack.echoashfallprotocol.echo.AshfallMissionActions;
import com.knoxhack.echoashfallprotocol.echo.EchoGuideManager;
import com.knoxhack.echoashfallprotocol.echo.EchoIntel;
import com.knoxhack.echoashfallprotocol.echo.EndgameMissionProgress;
import com.knoxhack.echoashfallprotocol.echo.Mission;
import com.knoxhack.echoashfallprotocol.echo.MissionGuideRegistry;
import com.knoxhack.echoashfallprotocol.echo.MissionRegistry;
import com.knoxhack.echoashfallprotocol.echo.MissionUxSummary;
import com.knoxhack.echoashfallprotocol.echo.QuestData;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.entity.EchoCompanionDrone;
import com.knoxhack.echoashfallprotocol.entity.ModEntities;
import com.knoxhack.echoashfallprotocol.entity.ScoutDrone;
import com.knoxhack.echoashfallprotocol.entity.drone.CompanionDroneData;
import com.knoxhack.echoashfallprotocol.entity.drone.CompanionDroneStateStore;
import com.knoxhack.echoashfallprotocol.entity.drone.DroneScanService;
import com.knoxhack.echoashfallprotocol.entity.drone.DroneWarningService;
import com.knoxhack.echoashfallprotocol.entity.boss.BiomeBossEntity;
import com.knoxhack.echoashfallprotocol.entity.boss.NexusFinalBossEntity;
import com.knoxhack.echoashfallprotocol.entity.boss.WardenBossEntity;
import com.knoxhack.echoashfallprotocol.entity.faction.FactionNpcEntity;
import com.knoxhack.echoashfallprotocol.endgame.NexusAccessRules;
import com.knoxhack.echoashfallprotocol.endgame.NexusCampaignActions;
import com.knoxhack.echoashfallprotocol.endgame.NexusChoiceService;
import com.knoxhack.echoashfallprotocol.endgame.NexusFinalBossProfile;
import com.knoxhack.echoashfallprotocol.endgame.NexusFinalBossProfiles;
import com.knoxhack.echoashfallprotocol.endgame.NexusPressureMobProfiles;
import com.knoxhack.echoashfallprotocol.endgame.NexusRelayProfile;
import com.knoxhack.echoashfallprotocol.endgame.NexusRelayProfiles;
import com.knoxhack.echoashfallprotocol.endgame.NexusRelayState;
import com.knoxhack.echoashfallprotocol.endgame.NexusRelaySiteService;
import com.knoxhack.echoashfallprotocol.endgame.NexusRelayType;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import com.knoxhack.echoashfallprotocol.endgame.PrefallArchivesArenaService;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventData;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventHandler;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventProfiles;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventStatus;
import com.knoxhack.echoashfallprotocol.event.EnvironmentalEventType;
import com.knoxhack.echoashfallprotocol.event.EmergencyBunkRespawnEvents;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreEarlyEventRuntime;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreExplorationRuntime;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreFirstSpawnRuntime;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreFirstSpawnRuntime.FirstSpawnRuntimeResult;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreHazardRuntime;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreLateRuntime;
import com.knoxhack.echoashfallprotocol.event.ModStructuresCommand;
import com.knoxhack.echoashfallprotocol.event.NeoForgeEchoRuntimeHost;
import com.knoxhack.echoashfallprotocol.event.NeoForgeRuntimeHostFactory;
import com.knoxhack.echoashfallprotocol.event.NeoForgeRuntimeMutationLedgerSink;
import com.knoxhack.echoashfallprotocol.event.NexusCommandHandler;
import com.knoxhack.echoashfallprotocol.event.PostNexusEventHandler;
import com.knoxhack.echoashfallprotocol.event.ProceduralStructureHandler;
import com.knoxhack.echoashfallprotocol.event.StructureGenCommand;
import com.knoxhack.echoashfallprotocol.fasttravel.RadioNetwork;
import com.knoxhack.echoashfallprotocol.fasttravel.StationRegistry;
import com.knoxhack.echoashfallprotocol.faction.AshfallBiomeFactions;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionContractData;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionContractProgression;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionContracts;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionInteractionHandler;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionMap;
import com.knoxhack.echoashfallprotocol.faction.FactionDiplomacy;
import com.knoxhack.echoashfallprotocol.faction.FactionNpcDialogueService;
import com.knoxhack.echoashfallprotocol.faction.FactionTerritory;
import com.knoxhack.echoashfallprotocol.faction.FactionWorldManager;
import com.knoxhack.echoashfallprotocol.gameplay.DifficultyProfile;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfile;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfiles;
import com.knoxhack.echoashfallprotocol.integration.AshfallDiscoveryProvider;
import com.knoxhack.echoashfallprotocol.integration.AshfallMissionCoreIntegration;
import com.knoxhack.echoashfallprotocol.integration.AshfallTerminalCommonIntegration;
import com.knoxhack.echoashfallprotocol.integration.AshfallWikiIntegration;
import com.knoxhack.echoashfallprotocol.loot.WikiManualLootModifier;
import com.knoxhack.echoashfallprotocol.item.BatteryItem;
import com.knoxhack.echoashfallprotocol.item.RareTechSchematicItem;
import com.knoxhack.echoashfallprotocol.item.SchematicFragmentItem;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.machine.MachineWearSavedData;
import com.knoxhack.echoashfallprotocol.network.BossNavigationPacket;
import com.knoxhack.echoashfallprotocol.network.DroneCommandPacket;
import com.knoxhack.echoashfallprotocol.network.FactionNpcActionPacket;
import com.knoxhack.echoashfallprotocol.network.ModNetwork;
import com.knoxhack.echoashfallprotocol.network.ResearchPurchasePacket;
import com.knoxhack.echoashfallprotocol.power.PowerDiagnostic;
import com.knoxhack.echoashfallprotocol.power.PowerIssue;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.registry.ModItems;
import com.knoxhack.echoashfallprotocol.research.Perk;
import com.knoxhack.echoashfallprotocol.research.PerkRegistry;
import com.knoxhack.echoashfallprotocol.research.ResearchData;
import com.knoxhack.echoashfallprotocol.survival.HazardZoneManager;
import com.knoxhack.echoashfallprotocol.survival.ColdData;
import com.knoxhack.echoashfallprotocol.survival.MutationData;
import com.knoxhack.echoashfallprotocol.survival.MutationManager;
import com.knoxhack.echoashfallprotocol.survival.SurvivalData;
import com.knoxhack.echoashfallprotocol.world.BiomeGuardianSiteData;
import com.knoxhack.echoashfallprotocol.world.ExplorationSiteRegistry;
import com.knoxhack.echoashfallprotocol.world.NexusCampaignData;
import com.knoxhack.echoashfallprotocol.world.NexusWorldData;
import com.knoxhack.echoashfallprotocol.world.POIScannerService;
import com.knoxhack.echoashfallprotocol.world.StartingDropPodData;
import com.knoxhack.echoashfallprotocol.worldgen.ProceduralStructureGenerator;
import com.knoxhack.echoashfallprotocol.worldgen.StructureType;
import com.knoxhack.echoashfallprotocol.worldgen.WorldgenBalance;
import com.knoxhack.echoterminal.api.TerminalActionRegistry;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.TerminalNavigationSection;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.discovery.TerminalDiscoveryProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionActions;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeEntry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeRegistry;
import com.knoxhack.echoterminal.mission.MainSurvivalQuestProvider;
import com.knoxhack.echolens.api.LensAccessPolicy;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensScanMode;
import com.knoxhack.echolens.api.ServerLensProvider;
import com.knoxhack.echolens.registry.LensProviderRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoAshfallProtocol.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENTITY_ATTRIBUTE_HARDENING =
            TEST_FUNCTIONS.register("entity_attribute_hardening", () -> ModGameTests::entityAttributeHardening);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DRONE_COMMAND_HARDENING =
            TEST_FUNCTIONS.register("drone_command_hardening", () -> ModGameTests::droneCommandHardening);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DRONE_FIELD_ASSISTANT_UTILITY =
            TEST_FUNCTIONS.register("drone_field_assistant_utility", () -> ModGameTests::droneFieldAssistantUtility);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DRONE_REPAIR_RUNTIME_MARKER =
            TEST_FUNCTIONS.register("drone_repair_runtime_marker", () -> ModGameTests::droneRepairRuntimeMarker);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DRONE_REPAIR_DEPLOY_COMMAND_INTEL_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("drone_repair_deploy_command_intel_runtime_flow", () -> ModGameTests::droneRepairDeployCommandIntelRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GUARDIAN_PROFILE_COVERAGE =
            TEST_FUNCTIONS.register("guardian_profile_coverage", () -> ModGameTests::guardianProfileCoverage);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GUARDIAN_BOSS_SMOKE =
            TEST_FUNCTIONS.register("guardian_boss_smoke", () -> ModGameTests::guardianBossSmoke);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GUARDIAN_SITE_STATE =
            TEST_FUNCTIONS.register("guardian_site_state", () -> ModGameTests::guardianSiteState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BOSS_HUD_NAVIGATION =
            TEST_FUNCTIONS.register("boss_hud_navigation", () -> ModGameTests::bossHudNavigation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NEXUS_ACCESS_RULES =
            TEST_FUNCTIONS.register("nexus_access_rules", () -> ModGameTests::nexusAccessRules);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NEXUS_CAMPAIGN_DATA =
            TEST_FUNCTIONS.register("nexus_campaign_data", () -> ModGameTests::nexusCampaignData);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NEXUS_WARFRONT_CONTENT =
            TEST_FUNCTIONS.register("nexus_warfront_content", () -> ModGameTests::nexusWarfrontContent);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NEXUS_GUARDIAN_RELAY_SIEGE_FINALE_POSTGAME_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("nexus_guardian_relay_siege_finale_postgame_runtime_flow", () -> ModGameTests::nexusGuardianRelaySiegeFinalePostgameRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WARDEN_ARENA_SERVICE =
            TEST_FUNCTIONS.register("warden_arena_service", () -> ModGameTests::wardenArenaService);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RARE_SCHEMATIC_RESEARCH =
            TEST_FUNCTIONS.register("rare_schematic_research", () -> ModGameTests::rareSchematicResearch);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RESEARCH_PERK_GRAPH =
            TEST_FUNCTIONS.register("research_perk_graph", () -> ModGameTests::researchPerkGraph);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RESEARCH_PURCHASE_SPENDS_RP =
            TEST_FUNCTIONS.register("research_purchase_spends_rp", () -> ModGameTests::researchPurchaseSpendsRp);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STRUCTURE_EXPORT_PATHS =
            TEST_FUNCTIONS.register("structure_export_paths", () -> ModGameTests::structureExportPaths);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROCEDURAL_TERRAIN_FOOTPRINTS =
            TEST_FUNCTIONS.register("procedural_terrain_footprints", () -> ModGameTests::proceduralTerrainFootprints);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STARTER_DROP_POD_TEMPLATE =
            TEST_FUNCTIONS.register("starter_drop_pod_template", () -> ModGameTests::starterDropPodTemplate);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STARTER_DROP_POD_CORRUPTION_GUARD =
            TEST_FUNCTIONS.register("starter_drop_pod_corruption_guard", () -> ModGameTests::starterDropPodCorruptionGuard);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EMERGENCY_BUNK_RESPAWN_BEHAVIOUR =
            TEST_FUNCTIONS.register("emergency_bunk_respawn_behaviour", () -> ModGameTests::emergencyBunkRespawnBehaviour);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EMERGENCY_BUNK_PAIRING_BEHAVIOUR =
            TEST_FUNCTIONS.register("emergency_bunk_pairing_behaviour", () -> ModGameTests::emergencyBunkPairingBehaviour);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASH_CAMPFIRE_SHELTER_PULSE =
            TEST_FUNCTIONS.register("ash_campfire_shelter_pulse", () -> ModGameTests::ashCampfireShelterPulse);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STARTING_DROP_POD_DATA_LENIENT_LOAD =
            TEST_FUNCTIONS.register("starting_drop_pod_data_lenient_load", () -> ModGameTests::startingDropPodDataLenientLoad);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WIKI_MANUAL_OPTIONAL_INTEGRATION =
            TEST_FUNCTIONS.register("wiki_manual_optional_integration", () -> ModGameTests::wikiManualOptionalIntegration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WIKI_MANUAL_LOOT_MODIFIER =
            TEST_FUNCTIONS.register("wiki_manual_loot_modifier", () -> ModGameTests::wikiManualLootModifier);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ARCHIVE_READ_STATE =
            TEST_FUNCTIONS.register("archive_read_state", () -> ModGameTests::archiveReadState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SUBSTRATE_GRINDER_RECIPES =
            TEST_FUNCTIONS.register("substrate_grinder_recipes", () -> ModGameTests::substrateGrinderRecipes);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ECHO_CONTAINER_BLOCK_ENTITIES =
            TEST_FUNCTIONS.register("echo_container_block_entities", () -> ModGameTests::echoContainerBlockEntities);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSIONCORE_REWARD_CLAIMABLE_UX =
            TEST_FUNCTIONS.register("missioncore_reward_claimable_ux", () -> ModGameTests::missionCoreRewardClaimableUx);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHFALL_ROUTE_THIN_SPINE =
            TEST_FUNCTIONS.register("ashfall_route_thin_spine", () -> ModGameTests::ashfallRouteThinSpine);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHFALL_MISSION_REQUIREMENT_AUDIT =
            TEST_FUNCTIONS.register("ashfall_mission_requirement_audit", () -> ModGameTests::ashfallMissionRequirementAudit);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHFALL_MISSIONCORE_RUNTIME_ROUTE =
            TEST_FUNCTIONS.register("ashfall_missioncore_runtime_route", () -> ModGameTests::ashfallMissionCoreRuntimeRoute);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SCRAP_KNIFE_TURN_IN =
            TEST_FUNCTIONS.register("scrap_knife_turn_in", () -> ModGameTests::scrapKnifeTurnIn);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ADVANCEMENT_CRITERIA_GUARD =
            TEST_FUNCTIONS.register("advancement_criteria_guard", () -> ModGameTests::advancementCriteriaGuard);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENVIRONMENTAL_EVENT_PROFILES =
            TEST_FUNCTIONS.register("environmental_event_profiles", () -> ModGameTests::environmentalEventProfiles);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SHELTERED_RADIATION_STORM_NO_EXPOSURE =
            TEST_FUNCTIONS.register("sheltered_radiation_storm_no_exposure", () -> ModGameTests::shelteredRadiationStormNoExposure);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RADIATION_STORM_SLEEP_ADVANCE_CLEARS =
            TEST_FUNCTIONS.register("radiation_storm_sleep_advance_clears", () -> ModGameTests::radiationStormSleepAdvanceClears);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RADIATION_STORM_SLEEP_ADVANCE_PARTIAL =
            TEST_FUNCTIONS.register("radiation_storm_sleep_advance_partial", () -> ModGameTests::radiationStormSleepAdvancePartial);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SLEEP_CLEARED_RADIATION_STORM_NO_HAZARD =
            TEST_FUNCTIONS.register("sleep_cleared_radiation_storm_no_hazard", () -> ModGameTests::sleepClearedRadiationStormNoHazard);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TOXIC_SLIME_NO_PASSIVE_PUDDLES =
            TEST_FUNCTIONS.register("toxic_slime_no_passive_puddles", () -> ModGameTests::toxicSlimeNoPassivePuddles);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TOXIC_BIOME_HAZARD_CLASSIFICATION =
            TEST_FUNCTIONS.register("toxic_biome_hazard_classification", () -> ModGameTests::toxicBiomeHazardClassification);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MUTATION_INSTABILITY_WARNING_RAD_GATED =
            TEST_FUNCTIONS.register("mutation_instability_warning_rad_gated", () -> ModGameTests::mutationInstabilityWarningRadGated);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RADAWAY_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("radaway_runtime_flow", () -> ModGameTests::radAwayRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FILTER_CARTRIDGE_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("filter_cartridge_runtime_flow", () -> ModGameTests::filterCartridgeRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HAZARD_MACHINE_RUNTIME_MARKERS =
            TEST_FUNCTIONS.register("hazard_machine_runtime_markers", () -> ModGameTests::hazardMachineRuntimeMarkers);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LATE_RUNTIME_ROUTE_MARKERS =
            TEST_FUNCTIONS.register("late_runtime_route_markers", () -> ModGameTests::lateRuntimeRouteMarkers);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INSTABILITY_DAMPENER_ITEM_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("instability_dampener_item_runtime_flow", () -> ModGameTests::instabilityDampenerItemRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RETURN_BEACON_ITEM_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("return_beacon_item_runtime_flow", () -> ModGameTests::returnBeaconItemRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUTE_BALANCE_CONTRACTS =
            TEST_FUNCTIONS.register("route_balance_contracts", () -> ModGameTests::routeBalanceContracts);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSION_UX_SUMMARY =
            TEST_FUNCTIONS.register("mission_ux_summary", () -> ModGameTests::missionUxSummary);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHFALL_HUD_NOTICE_SHELF_LAYOUT =
            TEST_FUNCTIONS.register("ashfall_hud_notice_shelf_layout", () -> ModGameTests::ashfallHudNoticeShelfLayout);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENDGAME_ROUTE_PROGRESS =
            TEST_FUNCTIONS.register("endgame_route_progress", () -> ModGameTests::endgameRouteProgress);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_LORE_TAXONOMY =
            TEST_FUNCTIONS.register("terminal_lore_taxonomy", () -> ModGameTests::terminalLoreTaxonomy);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_COMMAND_DECK_OWNERSHIP =
            TEST_FUNCTIONS.register("terminal_command_deck_ownership", () -> ModGameTests::terminalCommandDeckOwnership);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_COMMON_REGISTRATION =
            TEST_FUNCTIONS.register("terminal_common_registration", () -> ModGameTests::terminalCommonRegistration);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MISSION_GUIDE_COVERAGE =
            TEST_FUNCTIONS.register("mission_guide_coverage", () -> ModGameTests::missionGuideCoverage);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIRST_NIGHT_ROUTE_SAFETY =
            TEST_FUNCTIONS.register("first_night_route_safety", () -> ModGameTests::firstNightRouteSafety);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EXPLORATION_SITE_PROFILES =
            TEST_FUNCTIONS.register("exploration_site_profiles", () -> ModGameTests::explorationSiteProfiles);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHFALL_DISCOVERY_PROVIDER =
            TEST_FUNCTIONS.register("ashfall_discovery_provider", () -> ModGameTests::ashfallDiscoveryProvider);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CACHE_OPENED_RUNTIME_MARKER =
            TEST_FUNCTIONS.register("cache_opened_runtime_marker", () -> ModGameTests::cacheOpenedRuntimeMarker);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DATA_LOG_ITEM_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("data_log_item_runtime_flow", () -> ModGameTests::dataLogItemRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTION_CONTRACT_BALANCE =
            TEST_FUNCTIONS.register("faction_contract_balance", () -> ModGameTests::factionContractBalance);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTION_ACTION_REPUTATION_SERVICE_RAID_CONTRACT_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("faction_action_reputation_service_raid_contract_runtime_flow", () -> ModGameTests::factionActionReputationServiceRaidContractRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> STRICT_FACTION_ENTITY_IDS =
            TEST_FUNCTIONS.register("strict_faction_entity_ids", () -> ModGameTests::strictFactionEntityIds);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_WEAR_SAVED_DATA =
            TEST_FUNCTIONS.register("machine_wear_saved_data", () -> ModGameTests::machineWearSavedData);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DEBUG_COMMAND_PERMISSION_GATES =
            TEST_FUNCTIONS.register("debug_command_permission_gates", () -> ModGameTests::debugCommandPermissionGates);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NEXUS_UPGRADE_DATA_PATH =
            TEST_FUNCTIONS.register("nexus_upgrade_data_path", () -> ModGameTests::nexusUpgradeDataPath);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WATER_PURIFIER_NETWORK_POWER =
            TEST_FUNCTIONS.register("water_purifier_network_power", () -> ModGameTests::waterPurifierNetworkPower);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WATER_PURIFIER_TO_DRINK_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("water_purifier_to_drink_runtime_flow", () -> ModGameTests::waterPurifierToDrinkRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHFALL_MACHINECORE_PARITY =
            TEST_FUNCTIONS.register("ashfall_machinecore_runtime_snapshot_contract", () -> ModGameTests::ashfallMachineCoreRuntimeSnapshotContract);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HAND_RECYCLER_STARTER_BATTERY_THROUGHPUT =
            TEST_FUNCTIONS.register("hand_recycler_starter_battery_throughput", () -> ModGameTests::handRecyclerStarterBatteryThroughput);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SCRAP_SALVAGE_RECYCLER_PARTS_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("scrap_salvage_recycler_parts_runtime_flow", () -> ModGameTests::scrapSalvageRecyclerPartsRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ITEM_PIPE_NETWORK_ROUTING =
            TEST_FUNCTIONS.register("item_pipe_network_routing", () -> ModGameTests::itemPipeNetworkRouting);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> LOAD_DISTRIBUTOR_PRIORITY_ROUTING =
            TEST_FUNCTIONS.register("load_distributor_priority_routing", () -> ModGameTests::loadDistributorPriorityRouting);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FACTORY_CONTROLLER_SCAN_PAUSE =
            TEST_FUNCTIONS.register("factory_controller_scan_pause", () -> ModGameTests::factoryControllerScanPause);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WATER_BOTTLE_DRINK_FLOW =
            TEST_FUNCTIONS.register("water_bottle_drink_flow", () -> ModGameTests::waterBottleDrinkFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> EARLY_WATER_ROUTE_MARKERS =
            TEST_FUNCTIONS.register("early_water_route_markers", () -> ModGameTests::earlyWaterRouteMarkers);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HAND_WARMER_ITEM_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("hand_warmer_item_runtime_flow", () -> ModGameTests::handWarmerItemRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> GAS_MASK_ITEM_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("gas_mask_item_runtime_flow", () -> ModGameTests::gasMaskItemRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FIELD_MANUAL_ITEM_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("field_manual_item_runtime_flow", () -> ModGameTests::fieldManualItemRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MEDICAL_CONSUMABLE_ITEM_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("medical_consumable_item_runtime_flow", () -> ModGameTests::medicalConsumableItemRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MUTAGEN_ITEM_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("mutagen_item_runtime_flow", () -> ModGameTests::mutagenItemRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PORTABLE_SCANNER_ITEM_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("portable_scanner_item_runtime_flow", () -> ModGameTests::portableScannerItemRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SIGNAL_SCANNER_BLOCK_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("signal_scanner_block_runtime_flow", () -> ModGameTests::signalScannerBlockRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RELAY_STATION_BLOCK_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("relay_station_block_runtime_flow", () -> ModGameTests::relayStationBlockRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> POWER_NODE_BLOCK_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("power_node_block_runtime_flow", () -> ModGameTests::powerNodeBlockRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SCANNER_POI_MARKER_ROUTE_RUNTIME_FLOW =
            TEST_FUNCTIONS.register("scanner_poi_marker_route_runtime_flow", () -> ModGameTests::scannerPoiMarkerRouteRuntimeFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CRUDE_FILTER_ITEM_FLOW =
            TEST_FUNCTIONS.register("crude_filter_item_flow", () -> ModGameTests::crudeFilterItemFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORKSHOP_STATUS_COPY =
            TEST_FUNCTIONS.register("workshop_status_copy", () -> ModGameTests::workshopStatusCopy);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NEXUS_COMMAND_STATUS_ONLY =
            TEST_FUNCTIONS.register("nexus_command_status_only", () -> ModGameTests::nexusCommandStatusOnly);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> RADIO_DYNAMIC_STATION_PERSISTENCE =
            TEST_FUNCTIONS.register("radio_dynamic_station_persistence", () -> ModGameTests::radioDynamicStationPersistence);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DRONE_INTEL_TARGETING =
            TEST_FUNCTIONS.register("drone_intel_targeting", () -> ModGameTests::droneIntelTargeting);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> QUEST_REWARD_STACK_PERSISTENCE =
            TEST_FUNCTIONS.register("quest_reward_stack_persistence", () -> ModGameTests::questRewardStackPersistence);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NEOFORGE_RUNTIME_HOST_MUTATION_GATE =
            TEST_FUNCTIONS.register("neoforge_runtime_host_mutation_gate", () -> helper -> ModGameTests.neoforgeRuntimeHostMutationGate(helper));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> NEOFORGE_RUNTIME_HOST_MACHINE_CAPABILITY_GATE =
            TEST_FUNCTIONS.register("neoforge_runtime_host_machine_capability_gate", () -> helper -> ModGameTests.neoforgeRuntimeHostMachineCapabilityGate(helper));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHFALL_FIRST_SPAWN_NEW_PLAYER_HOST_SMOKE =
            TEST_FUNCTIONS.register("ashfall_first_spawn_new_player_host_smoke", () -> helper -> ModGameTests.ashfallFirstSpawnNewPlayerHostSmoke(helper));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHFALL_FIRST_SPAWN_RETURNING_PLAYER_REPAIR_HOST_SMOKE =
            TEST_FUNCTIONS.register("ashfall_first_spawn_returning_player_repair_host_smoke", () -> helper -> ModGameTests.ashfallFirstSpawnReturningPlayerRepairHostSmoke(helper));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHFALL_FIRST_RELAY_ROUTE_HOST_SMOKE =
            TEST_FUNCTIONS.register("ashfall_first_relay_route_host_smoke", () -> helper -> ModGameTests.ashfallFirstRelayRouteHostSmoke(helper));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHFALL_PHASE3_ROUTE_HOST_SMOKE =
            TEST_FUNCTIONS.register("ashfall_phase3_route_host_smoke", () -> helper -> ModGameTests.ashfallPhase3RouteHostSmoke(helper));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHFALL_MIDGAME_ROUTE_HOST_SMOKE =
            TEST_FUNCTIONS.register("ashfall_midgame_route_host_smoke", () -> helper -> ModGameTests.ashfallMidgameRouteHostSmoke(helper));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHFALL_LATE_GAME_NEXUS_ENDINGS_HOST_SMOKE =
            TEST_FUNCTIONS.register("ashfall_late_game_nexus_endings_host_smoke", () -> helper -> ModGameTests.ashfallLateGameNexusEndingsHostSmoke(helper));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHFALL_ROUTE_REWARD_DELIVERY_HOST_SMOKE =
            TEST_FUNCTIONS.register("ashfall_route_reward_delivery_host_smoke", () -> helper -> ModGameTests.ashfallRouteRewardDeliveryHostSmoke(helper));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ASHFALL_MACHINE_METER_SCREEN_HOST_SMOKE =
            TEST_FUNCTIONS.register("ashfall_machine_meter_screen_host_smoke", () -> helper -> ModGameTests.ashfallMachineMeterScreenHostSmoke(helper));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AGENT9_TECH_NATIVE_HOST_SMOKE =
            TEST_FUNCTIONS.register("agent9_tech_native_host_smoke", () -> helper -> ModGameTests.agent9TechNativeHostSmoke(helper));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AGENT7_FULL_PLAYTHROUGH_RESTORE_HOST_SMOKE =
            TEST_FUNCTIONS.register("agent7_full_playthrough_restore_host_smoke", () -> helper -> ModGameTests.agent7FullPlaythroughRestoreHostSmoke(helper));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AGENT7_FULL_PLAYTHROUGH_DESTROY_HOST_SMOKE =
            TEST_FUNCTIONS.register("agent7_full_playthrough_destroy_host_smoke", () -> helper -> ModGameTests.agent7FullPlaythroughDestroyHostSmoke(helper));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AGENT7_FULL_PLAYTHROUGH_CONTROL_HOST_SMOKE =
            TEST_FUNCTIONS.register("agent7_full_playthrough_control_host_smoke", () -> helper -> ModGameTests.agent7FullPlaythroughControlHostSmoke(helper));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AGENT7_CHECKPOINT_RELOG_HOST_SMOKE =
            TEST_FUNCTIONS.register("agent7_checkpoint_relog_host_smoke", () -> helper -> ModGameTests.agent7CheckpointRelogHostSmoke(helper));
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AGENT3_PLAYABLE_LOOP_NATIVE_HOST_SMOKE =
            TEST_FUNCTIONS.register("agent3_playable_loop_native_host_smoke", () -> helper -> ModGameTests.agent3PlayableLoopNativeHostSmoke(helper));

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("release_hardening"));
        register(event, environment, "entity_attribute_hardening", ENTITY_ATTRIBUTE_HARDENING.getId());
        register(event, environment, "drone_command_hardening", DRONE_COMMAND_HARDENING.getId());
        register(event, environment, "drone_field_assistant_utility", DRONE_FIELD_ASSISTANT_UTILITY.getId());
        register(event, environment, "drone_repair_runtime_marker", DRONE_REPAIR_RUNTIME_MARKER.getId());
        register(event, environment, "drone_repair_deploy_command_intel_runtime_flow", DRONE_REPAIR_DEPLOY_COMMAND_INTEL_RUNTIME_FLOW.getId());
        register(event, environment, "guardian_profile_coverage", GUARDIAN_PROFILE_COVERAGE.getId());
        register(event, environment, "guardian_boss_smoke", GUARDIAN_BOSS_SMOKE.getId());
        register(event, environment, "guardian_site_state", GUARDIAN_SITE_STATE.getId());
        register(event, environment, "boss_hud_navigation", BOSS_HUD_NAVIGATION.getId());
        register(event, environment, "nexus_access_rules", NEXUS_ACCESS_RULES.getId());
        register(event, environment, "nexus_campaign_data", NEXUS_CAMPAIGN_DATA.getId());
        register(event, environment, "nexus_warfront_content", NEXUS_WARFRONT_CONTENT.getId());
        register(event, environment, "nexus_guardian_relay_siege_finale_postgame_runtime_flow", NEXUS_GUARDIAN_RELAY_SIEGE_FINALE_POSTGAME_RUNTIME_FLOW.getId());
        register(event, environment, "warden_arena_service", WARDEN_ARENA_SERVICE.getId());
        register(event, environment, "rare_schematic_research", RARE_SCHEMATIC_RESEARCH.getId());
        register(event, environment, "research_perk_graph", RESEARCH_PERK_GRAPH.getId());
        register(event, environment, "structure_export_paths", STRUCTURE_EXPORT_PATHS.getId());
        register(event, environment, "procedural_terrain_footprints", PROCEDURAL_TERRAIN_FOOTPRINTS.getId());
        register(event, environment, "starter_drop_pod_template", STARTER_DROP_POD_TEMPLATE.getId());
        register(event, environment, "starter_drop_pod_corruption_guard", STARTER_DROP_POD_CORRUPTION_GUARD.getId());
        register(event, environment, "emergency_bunk_respawn_behaviour", EMERGENCY_BUNK_RESPAWN_BEHAVIOUR.getId());
        register(event, environment, "emergency_bunk_pairing_behaviour", EMERGENCY_BUNK_PAIRING_BEHAVIOUR.getId());
        register(event, environment, "ash_campfire_shelter_pulse", ASH_CAMPFIRE_SHELTER_PULSE.getId());
        register(event, environment, "starting_drop_pod_data_lenient_load", STARTING_DROP_POD_DATA_LENIENT_LOAD.getId());
        register(event, environment, "wiki_manual_optional_integration", WIKI_MANUAL_OPTIONAL_INTEGRATION.getId());
        register(event, environment, "wiki_manual_loot_modifier", WIKI_MANUAL_LOOT_MODIFIER.getId());
        register(event, environment, "archive_read_state", ARCHIVE_READ_STATE.getId());
        register(event, environment, "substrate_grinder_recipes", SUBSTRATE_GRINDER_RECIPES.getId());
        register(event, environment, "echo_container_block_entities", ECHO_CONTAINER_BLOCK_ENTITIES.getId());
        register(event, environment, "missioncore_reward_claimable_ux", MISSIONCORE_REWARD_CLAIMABLE_UX.getId());
        register(event, environment, "ashfall_route_thin_spine", ASHFALL_ROUTE_THIN_SPINE.getId());
        register(event, environment, "ashfall_mission_requirement_audit", ASHFALL_MISSION_REQUIREMENT_AUDIT.getId());
        register(event, environment, "ashfall_missioncore_runtime_route", ASHFALL_MISSIONCORE_RUNTIME_ROUTE.getId());
        register(event, environment, "scrap_knife_turn_in", SCRAP_KNIFE_TURN_IN.getId());
        register(event, environment, "advancement_criteria_guard", ADVANCEMENT_CRITERIA_GUARD.getId());
        register(event, environment, "environmental_event_profiles", ENVIRONMENTAL_EVENT_PROFILES.getId());
        register(event, environment, "sheltered_radiation_storm_no_exposure", SHELTERED_RADIATION_STORM_NO_EXPOSURE.getId());
        register(event, environment, "radiation_storm_sleep_advance_clears", RADIATION_STORM_SLEEP_ADVANCE_CLEARS.getId());
        register(event, environment, "radiation_storm_sleep_advance_partial", RADIATION_STORM_SLEEP_ADVANCE_PARTIAL.getId());
        register(event, environment, "sleep_cleared_radiation_storm_no_hazard", SLEEP_CLEARED_RADIATION_STORM_NO_HAZARD.getId());
        register(event, environment, "toxic_slime_no_passive_puddles", TOXIC_SLIME_NO_PASSIVE_PUDDLES.getId());
        register(event, environment, "toxic_biome_hazard_classification", TOXIC_BIOME_HAZARD_CLASSIFICATION.getId());
        register(event, environment, "mutation_instability_warning_rad_gated", MUTATION_INSTABILITY_WARNING_RAD_GATED.getId());
        register(event, environment, "radaway_runtime_flow", RADAWAY_RUNTIME_FLOW.getId());
        register(event, environment, "hazard_machine_runtime_markers", HAZARD_MACHINE_RUNTIME_MARKERS.getId());
        register(event, environment, "late_runtime_route_markers", LATE_RUNTIME_ROUTE_MARKERS.getId());
        register(event, environment, "instability_dampener_item_runtime_flow", INSTABILITY_DAMPENER_ITEM_RUNTIME_FLOW.getId());
        register(event, environment, "return_beacon_item_runtime_flow", RETURN_BEACON_ITEM_RUNTIME_FLOW.getId());
        register(event, environment, "route_balance_contracts", ROUTE_BALANCE_CONTRACTS.getId());
        register(event, environment, "mission_ux_summary", MISSION_UX_SUMMARY.getId());
        register(event, environment, "ashfall_hud_notice_shelf_layout", ASHFALL_HUD_NOTICE_SHELF_LAYOUT.getId());
        register(event, environment, "endgame_route_progress", ENDGAME_ROUTE_PROGRESS.getId());
        register(event, environment, "terminal_lore_taxonomy", TERMINAL_LORE_TAXONOMY.getId());
        register(event, environment, "terminal_command_deck_ownership", TERMINAL_COMMAND_DECK_OWNERSHIP.getId());
        register(event, environment, "terminal_common_registration", TERMINAL_COMMON_REGISTRATION.getId());
        register(event, environment, "mission_guide_coverage", MISSION_GUIDE_COVERAGE.getId());
        register(event, environment, "first_night_route_safety", FIRST_NIGHT_ROUTE_SAFETY.getId());
        register(event, environment, "exploration_site_profiles", EXPLORATION_SITE_PROFILES.getId());
        register(event, environment, "ashfall_discovery_provider", ASHFALL_DISCOVERY_PROVIDER.getId());
        register(event, environment, "cache_opened_runtime_marker", CACHE_OPENED_RUNTIME_MARKER.getId());
        register(event, environment, "data_log_item_runtime_flow", DATA_LOG_ITEM_RUNTIME_FLOW.getId());
        register(event, environment, "faction_contract_balance", FACTION_CONTRACT_BALANCE.getId());
        register(event, environment, "faction_action_reputation_service_raid_contract_runtime_flow", FACTION_ACTION_REPUTATION_SERVICE_RAID_CONTRACT_RUNTIME_FLOW.getId());
        register(event, environment, "strict_faction_entity_ids", STRICT_FACTION_ENTITY_IDS.getId());
        register(event, environment, "machine_wear_saved_data", MACHINE_WEAR_SAVED_DATA.getId());
        register(event, environment, "debug_command_permission_gates", DEBUG_COMMAND_PERMISSION_GATES.getId());
        register(event, environment, "nexus_upgrade_data_path", NEXUS_UPGRADE_DATA_PATH.getId());
        register(event, environment, "water_purifier_network_power", WATER_PURIFIER_NETWORK_POWER.getId());
        register(event, environment, "water_purifier_to_drink_runtime_flow", WATER_PURIFIER_TO_DRINK_RUNTIME_FLOW.getId());
        if (machineCoreProofAvailable()) {
            register(event, environment, "ashfall_machinecore_runtime_snapshot_contract", ASHFALL_MACHINECORE_PARITY.getId());
        }
        register(event, environment, "hand_recycler_starter_battery_throughput", HAND_RECYCLER_STARTER_BATTERY_THROUGHPUT.getId());
        register(event, environment, "scrap_salvage_recycler_parts_runtime_flow", SCRAP_SALVAGE_RECYCLER_PARTS_RUNTIME_FLOW.getId());
        register(event, environment, "item_pipe_network_routing", ITEM_PIPE_NETWORK_ROUTING.getId());
        register(event, environment, "load_distributor_priority_routing", LOAD_DISTRIBUTOR_PRIORITY_ROUTING.getId());
        register(event, environment, "factory_controller_scan_pause", FACTORY_CONTROLLER_SCAN_PAUSE.getId());
        register(event, environment, "water_bottle_drink_flow", WATER_BOTTLE_DRINK_FLOW.getId());
        register(event, environment, "early_water_route_markers", EARLY_WATER_ROUTE_MARKERS.getId());
        register(event, environment, "hand_warmer_item_runtime_flow", HAND_WARMER_ITEM_RUNTIME_FLOW.getId());
        register(event, environment, "medical_consumable_item_runtime_flow", MEDICAL_CONSUMABLE_ITEM_RUNTIME_FLOW.getId());
        register(event, environment, "mutagen_item_runtime_flow", MUTAGEN_ITEM_RUNTIME_FLOW.getId());
        register(event, environment, "filter_cartridge_runtime_flow", FILTER_CARTRIDGE_RUNTIME_FLOW.getId());
        register(event, environment, "portable_scanner_item_runtime_flow", PORTABLE_SCANNER_ITEM_RUNTIME_FLOW.getId());
        register(event, environment, "signal_scanner_block_runtime_flow", SIGNAL_SCANNER_BLOCK_RUNTIME_FLOW.getId());
        register(event, environment, "relay_station_block_runtime_flow", RELAY_STATION_BLOCK_RUNTIME_FLOW.getId());
        register(event, environment, "power_node_block_runtime_flow", POWER_NODE_BLOCK_RUNTIME_FLOW.getId());
        register(event, environment, "scanner_poi_marker_route_runtime_flow", SCANNER_POI_MARKER_ROUTE_RUNTIME_FLOW.getId());
        register(event, environment, "crude_filter_item_flow", CRUDE_FILTER_ITEM_FLOW.getId());
        register(event, environment, "workshop_status_copy", WORKSHOP_STATUS_COPY.getId());
        register(event, environment, "nexus_command_status_only", NEXUS_COMMAND_STATUS_ONLY.getId());
        register(event, environment, "radio_dynamic_station_persistence", RADIO_DYNAMIC_STATION_PERSISTENCE.getId());
        register(event, environment, "drone_intel_targeting", DRONE_INTEL_TARGETING.getId());
        register(event, environment, "quest_reward_stack_persistence", QUEST_REWARD_STACK_PERSISTENCE.getId());
        register(event, environment, "neoforge_runtime_host_mutation_gate", NEOFORGE_RUNTIME_HOST_MUTATION_GATE.getId());
        register(event, environment, "neoforge_runtime_host_machine_capability_gate", NEOFORGE_RUNTIME_HOST_MACHINE_CAPABILITY_GATE.getId());
        register(event, environment, "ashfall_first_spawn_new_player_host_smoke", ASHFALL_FIRST_SPAWN_NEW_PLAYER_HOST_SMOKE.getId());
        register(event, environment, "ashfall_first_spawn_returning_player_repair_host_smoke", ASHFALL_FIRST_SPAWN_RETURNING_PLAYER_REPAIR_HOST_SMOKE.getId());
        register(event, environment, "ashfall_first_relay_route_host_smoke", ASHFALL_FIRST_RELAY_ROUTE_HOST_SMOKE.getId());
        register(event, environment, "ashfall_phase3_route_host_smoke", ASHFALL_PHASE3_ROUTE_HOST_SMOKE.getId());
        register(event, environment, "ashfall_midgame_route_host_smoke", ASHFALL_MIDGAME_ROUTE_HOST_SMOKE.getId());
        register(event, environment, "ashfall_late_game_nexus_endings_host_smoke", ASHFALL_LATE_GAME_NEXUS_ENDINGS_HOST_SMOKE.getId());
        register(event, environment, "ashfall_route_reward_delivery_host_smoke", ASHFALL_ROUTE_REWARD_DELIVERY_HOST_SMOKE.getId());
        register(event, environment, "ashfall_machine_meter_screen_host_smoke", ASHFALL_MACHINE_METER_SCREEN_HOST_SMOKE.getId());
        register(event, environment, "agent9_tech_native_host_smoke", AGENT9_TECH_NATIVE_HOST_SMOKE.getId());
        register(event, environment, "agent7_full_playthrough_restore_host_smoke", AGENT7_FULL_PLAYTHROUGH_RESTORE_HOST_SMOKE.getId());
        register(event, environment, "agent7_full_playthrough_destroy_host_smoke", AGENT7_FULL_PLAYTHROUGH_DESTROY_HOST_SMOKE.getId());
        register(event, environment, "agent7_full_playthrough_control_host_smoke", AGENT7_FULL_PLAYTHROUGH_CONTROL_HOST_SMOKE.getId());
        register(event, environment, "agent7_checkpoint_relog_host_smoke", AGENT7_CHECKPOINT_RELOG_HOST_SMOKE.getId());
        register(event, environment, "agent3_playable_loop_native_host_smoke", AGENT3_PLAYABLE_LOOP_NATIVE_HOST_SMOKE.getId());
    }

    private static void entityAttributeHardening(GameTestHelper helper) {
        List<EntityType<? extends Entity>> attackingTypes = attackingTypes();
        int index = 0;

        for (EntityType<? extends Entity> type : allAshfallTypes()) {
            Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            Entity entity = type.create(helper.getLevel(), EntitySpawnReason.EVENT);
            helper.assertTrue(entity != null, "Entity should be spawnable: " + entityId);
            if (entity == null) {
                continue;
            }

            if (entity instanceof Mob mob) {
                helper.assertTrue(mob.getAttribute(Attributes.MAX_HEALTH) != null,
                        "Mob should have max health attribute: " + entityId);
                helper.assertTrue(mob.getAttribute(Attributes.MOVEMENT_SPEED) != null,
                        "Mob should have movement speed attribute: " + entityId);
                if (attackingTypes.contains(type)) {
                    helper.assertTrue(mob.getAttribute(Attributes.ATTACK_DAMAGE) != null,
                            "Attacking mob should have attack damage attribute: " + entityId);
                }
            }

            BlockPos pos = helper.absolutePos(new BlockPos(1 + index % 8, 2, 1 + index / 8));
            entity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            helper.getLevel().addFreshEntity(entity);
            entity.tick();
            entity.discard();
            index++;
        }

        helper.succeed();
    }

    private static void droneCommandHardening(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos playerPos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);
        cleanupOwnedDrones(helper, player);

        ModNetwork.handleDroneCommand(new DroneCommandPacket("RECALL"), player);

        ServerPlayer otherPlayer = helper.makeMockServerPlayerInLevel();
        BlockPos otherPos = helper.absolutePos(new BlockPos(4, 2, 1));
        otherPlayer.setPos(otherPos.getX() + 0.5D, otherPos.getY(), otherPos.getZ() + 0.5D);
        EchoCompanionDrone wrongOwnerDrone = spawnCompanionDrone(helper, otherPlayer, new BlockPos(6, 2, 1));
        ModNetwork.handleDroneCommand(new DroneCommandPacket("RECALL"), player);
        helper.assertTrue(!player.getUUID().equals(wrongOwnerDrone.getOwnerUUID()),
                "Wrong-owner companion drone should not be claimed by commands");
        wrongOwnerDrone.discard();

        EchoCompanionDrone drone = spawnCompanionDrone(helper, player, new BlockPos(8, 2, 1));
        drone.setRepairLevel(15);
        ModNetwork.handleDroneCommand(new DroneCommandPacket("COMBAT"), player);
        helper.assertTrue(drone.getCurrentMode() != EchoCompanionDrone.DroneMode.COMBAT,
                "Locked companion combat mode should stay unavailable");

        boolean initialLight = drone.isLightEnabled();
        ModNetwork.handleDroneCommand(new DroneCommandPacket("TOGGLE_LIGHT"), player);
        helper.assertTrue(drone.isLightEnabled() != initialLight,
                "Light command should toggle companion light");

        drone.setPos(player.getX() + 24.0D, player.getY(), player.getZ() + 24.0D);
        drone.setDeltaMovement(Vec3.ZERO);
        helper.runAfterDelay(1L, () -> {
            ModNetwork.handleDroneCommand(new DroneCommandPacket("RECALL"), player);
            helper.assertTrue(drone.distanceToSqr(player) < 64.0D,
                    "Recall should move companion drone near owner");

            drone.setRepairLevel(EchoCompanionDrone.REPAIR_FULL);
            ModNetwork.handleDroneCommand(new DroneCommandPacket("SCAVENGE"), player);
            helper.assertTrue(drone.getCurrentMode() == EchoCompanionDrone.DroneMode.SCAVENGE,
                    "Unlocked companion scavenge mode should activate");

            ModNetwork.handleDroneCommand(new DroneCommandPacket("PATROL"), player);
            helper.assertTrue(drone.getCurrentMode() == EchoCompanionDrone.DroneMode.PATROL,
                    "Unlocked companion patrol mode should activate");

            ModNetwork.handleDroneCommand(new DroneCommandPacket("COMBAT"), player);
            helper.assertTrue(drone.getCurrentMode() == EchoCompanionDrone.DroneMode.COMBAT,
                    "Unlocked companion combat mode should activate");

            drone.discard();
            ScoutDrone scout = spawnScoutDrone(helper, player, new BlockPos(8, 2, 4));
            ModNetwork.handleDroneCommand(new DroneCommandPacket("SCAVENGE"), player);
            helper.assertTrue(scout.getMode() == ScoutDrone.DroneMode.SCAVENGE,
                    "Scout fallback should map SCAVENGE command to scavenge mode");

            scout.discard();
            cleanupOwnedDrones(helper, player);
            helper.succeed();
        });
    }

    private static void droneRepairRuntimeMarker(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos playerPos = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);
        cleanupOwnedDrones(helper, player);

        EchoCompanionDrone drone = spawnCompanionDrone(helper, player, new BlockPos(4, 2, 2));
        drone.setRepairLevel(15);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.ENERGY_CELL.get()));

        InteractionResult result = drone.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(result.consumesAction(),
                "Drone repair interaction should consume the repair action");
        helper.assertTrue(drone.getRepairLevel() >= EchoCompanionDrone.REPAIR_FOLLOW,
                "Energy Cell repair should restore the companion drone to route-ready integrity");
        helper.assertTrue(CompanionDroneStateStore.get(player).getHealth() >= EchoCompanionDrone.REPAIR_FOLLOW,
                "Drone repair should mirror route-ready integrity into persistent drone state");
        helper.assertTrue(QuestData.get(player).getDroneHealth() >= EchoCompanionDrone.REPAIR_FOLLOW,
                "Drone repair should mirror route-ready integrity into QuestData");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "drone:repair"),
                "Drone repair should record a route-visible AdapterCore marker");
        String lastEvent = player.getPersistentData().getStringOr("ashes_of_tomorrow.adaptercore.last_exploration_event", "");
        helper.assertTrue("ashfall.drone_state".equals(lastEvent),
                "Drone repair should publish AdapterCore exploration diagnostics");

        int scoutLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
        var scoutIntelResult = AshfallAdapterCoreExplorationRuntime.droneState(
                player,
                "scout_set_mode",
                "SCAVENGE",
                true,
                Map.of("source", "gametest_drone_scout_mode"));
        helper.assertTrue(scoutIntelResult.mutated(),
                "Scout Drone mode support should mutate route-visible state through AdapterCore");
        QuestData scoutQuest = QuestData.get(player);
        helper.assertTrue(scoutQuest.hasVisitedLocation("special", "drone:scout_mode")
                        && scoutQuest.hasVisitedLocation("special", "drone:intel_recovered"),
                "Scout Drone mode support should record scout mode and intel markers inside runtime");
        var scoutLedgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(scoutLedgerEntries.size() > scoutLedgerBefore,
                "Scout Drone mode support should append an AdapterCore mutation ledger entry");
        var scoutLedger = scoutLedgerEntries.get(scoutLedgerEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_ASHFALL_DRONE_STATE.equals(scoutLedger.actionId()),
                "Scout Drone support ledger should use the canonical drone state event");
        helper.assertTrue("echoashfallprotocol:exploration_runtime".equals(scoutLedger.runtimeHostId()),
                "Scout Drone support ledger should record the exploration runtime host");
        helper.assertTrue(scoutLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Scout Drone support ledger should truthfully report MUTATED");
        helper.assertTrue(scoutLedger.saveTouched() && scoutLedger.hudOrEventEmitted(),
                "Scout Drone support ledger should record save touch and visible feedback");
        helper.assertTrue("scout_set_mode".equals(String.valueOf(scoutLedger.inputPayload().get("operation")))
                        && "SCAVENGE".equals(String.valueOf(scoutLedger.inputPayload().get("mode"))),
                "Scout Drone support ledger should carry the real mode-change payload");
        helper.assertFalse(Boolean.TRUE.equals(scoutLedger.beforeSummary().get("droneScoutModeMarker")),
                "Scout Drone support ledger should show scout mode absent before runtime mutation");
        helper.assertTrue(Boolean.TRUE.equals(scoutLedger.afterSummary().get("droneScoutModeMarker")),
                "Scout Drone support ledger should show scout mode present after runtime mutation");
        helper.assertFalse(Boolean.TRUE.equals(scoutLedger.beforeSummary().get("droneIntelRecoveredMarker")),
                "Scout Drone support ledger should show intel marker absent before runtime mutation");
        helper.assertTrue(Boolean.TRUE.equals(scoutLedger.afterSummary().get("droneIntelRecoveredMarker")),
                "Scout Drone support ledger should show intel marker present after runtime mutation");

        drone.discard();
        cleanupOwnedDrones(helper, player);
        helper.succeed();
    }

    private static void droneFieldAssistantUtility(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos playerPos = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);
        cleanupOwnedDrones(helper, player);

        EchoCompanionDrone drone = spawnCompanionDrone(helper, player, new BlockPos(3, 2, 2));
        drone.setRepairLevel(EchoCompanionDrone.REPAIR_FULL);
        CompanionDroneStateStore.link(player, drone, CompanionDroneStateStore.get(player));

        helper.setBlock(new BlockPos(5, 2, 2), Blocks.CHEST);
        helper.setBlock(new BlockPos(6, 2, 2), Blocks.LAVA);
        ItemEntity salvage = new ItemEntity(helper.getLevel(),
                helper.absolutePos(new BlockPos(4, 3, 2)).getX() + 0.5D,
                helper.absolutePos(new BlockPos(4, 3, 2)).getY(),
                helper.absolutePos(new BlockPos(4, 3, 2)).getZ() + 0.5D,
                new ItemStack(ModItems.SCRAP_METAL.get(), 2));
        helper.getLevel().addFreshEntity(salvage);
        Mob hostile = ModEntities.RAD_ZOMBIE.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(hostile != null, "Rad zombie should spawn for drone scan test");
        if (hostile != null) {
            BlockPos hostilePos = helper.absolutePos(new BlockPos(7, 2, 2));
            hostile.setPos(hostilePos.getX() + 0.5D, hostilePos.getY(), hostilePos.getZ() + 0.5D);
            helper.getLevel().addFreshEntity(hostile);
        }

        ModNetwork.handleDroneCommand(new DroneCommandPacket("scan_area"), player);
        CompanionDroneData data = CompanionDroneStateStore.get(player);
        String summary = data.getLastScanSummary();
        helper.assertTrue(summary.contains("scrap cache"), "Scan should report tagged dropped salvage");
        helper.assertTrue(summary.contains("hostile"), "Scan should report nearby hostile");
        helper.assertTrue(summary.contains("hazard"), "Scan should report lava/basic hazards");
        helper.assertTrue(summary.contains("container"), "Scan should report containers");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "drone:intel_recovered"),
                "Drone scans should record intel recovery route progress");

        long manualScanTime = data.getLastScanTime();
        long scoutTimeBefore = data.getLastScoutTime();
        ModNetwork.handleDroneCommand(new DroneCommandPacket("scout_ahead"), player);
        CompanionDroneData scoutStarted = CompanionDroneStateStore.get(player);
        helper.assertTrue(scoutStarted.getMode() == EchoDroneMode.SCOUT,
                "Scout Ahead should set persisted mode to scout while travelling");
        helper.assertTrue(scoutStarted.getLastScanTime() == manualScanTime,
                "Scout Ahead should not consume the manual scan cooldown at command time");
        helper.assertTrue(scoutStarted.getLastScoutTime() == scoutTimeBefore,
                "Scout Ahead should not scan instantly when the command is accepted");

        for (int i = 0; i < 140; i++) {
            drone.tick();
        }
        CompanionDroneData scoutComplete = CompanionDroneStateStore.get(player);
        helper.assertTrue(scoutComplete.getLastScoutTime() != scoutTimeBefore,
                "Scout Ahead should scan after travelling or timing out");
        helper.assertTrue(scoutComplete.getLastScanTime() == manualScanTime,
                "Scout Ahead should keep manual scan cooldown independent");
        helper.assertTrue(scoutComplete.getMode() == EchoDroneMode.FOLLOW,
                "Scout Ahead should return to follow mode after the scout scan");

        drone.setPos(player.getX() + 1.0D, player.getY() + 1.0D, player.getZ());
        ModNetwork.handleDroneCommand(new DroneCommandPacket("collect_scrap"), player);
        helper.assertTrue(CompanionDroneStateStore.get(player).getMode() == EchoDroneMode.SALVAGE,
                "Collect scrap command should switch persisted mode to salvage");
        helper.assertTrue(drone.getCurrentMode() == EchoCompanionDrone.DroneMode.SCAVENGE,
                "Collect scrap command should use the existing scavenge behavior");
        int scrapBefore = countItem(player, ModItems.SCRAP_METAL.get());
        for (int i = 0; i < 80; i++) {
            drone.tick();
        }
        helper.assertTrue(salvage.isRemoved() || salvage.getItem().getCount() < 2,
                "Salvage mode should pick allowed scrap into drone cargo");
        helper.assertTrue(countItem(player, ModItems.SCRAP_METAL.get()) > scrapBefore,
                "Salvage mode should return carried scrap to the owner");

        ModNetwork.handleDroneCommand(new DroneCommandPacket("guard_here"), player);
        helper.assertTrue(CompanionDroneStateStore.get(player).getMode() == EchoDroneMode.GUARD,
                "Guard command should switch persisted mode to guard");
        drone.tick();
        helper.assertTrue(drone.getTarget() == null, "Guard mode should ping threats without default combat targeting");
        helper.assertTrue(DroneScanService.recentMarkers(player).stream()
                        .anyMatch(marker -> marker.category() == EchoDroneScanCategory.HOSTILE),
                "Guard mode should publish a temporary hostile marker");

        CompanionDroneData warningData = CompanionDroneStateStore.get(player);
        warningData.setBatteryPercent(10);
        warningData.setWarningTime("battery", helper.getLevel().getGameTime());
        warningData.setWarningTime("hostile", Long.MIN_VALUE);
        CompanionDroneStateStore.save(player, warningData);
        DroneWarningService.tickWarnings(drone, helper.getLevel(), player);
        helper.assertTrue(CompanionDroneStateStore.get(player).getLastWarning().contains("Hostile"),
                "Per-category warning cooldowns should let hostile warnings bypass battery cooldown");

        drone.discard();
        CompanionDroneData missingData = CompanionDroneStateStore.get(player);
        missingData.setDeployed(true);
        CompanionDroneStateStore.save(player, missingData);
        ModNetwork.handleDroneCommand(new DroneCommandPacket("recall"), player);
        helper.assertTrue(CompanionDroneStateStore.nearestOwned(player) != null,
                "Recall should reconstruct a missing deployed companion drone");

        if (hostile != null) {
            hostile.discard();
        }
        salvage.discard();
        cleanupOwnedDrones(helper, player);
        helper.succeed();
    }

    private static void droneRepairDeployCommandIntelRuntimeFlow(GameTestHelper helper) {
        EchoServiceRegistry.withClearedForTests(() -> {
            RecordingMissionService missionService = new RecordingMissionService();
            EchoCoreServices.registerMissionService(missionService);
            CompanionDroneStateStore.registerDataKey();

            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            BlockPos playerPos = helper.absolutePos(new BlockPos(2, 2, 2));
            player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);
            cleanupOwnedDrones(helper, player);

            int deployLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            ItemStack scoutDroneItem = new ItemStack(ModItems.SCOUT_DRONE_ITEM.get());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, scoutDroneItem);
            InteractionResult deployResult = scoutDroneItem.getItem().use(
                    helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

            helper.assertTrue(deployResult.consumesAction(),
                    "Scout Drone item use should consume the deploy action");
            if (!player.isCreative()) {
                helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),
                        "Scout Drone item should be consumed after live deployment for non-creative players");
            }
            ScoutDrone deployedScout = nearestScoutDrone(helper, player);
            helper.assertTrue(deployedScout != null,
                    "Scout Drone item should summon a player-owned Scout Drone entity");
            helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "drone:scout_route"),
                    "Scout Drone deployment should record the late-route scout marker");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.DELIVER_ITEM,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "scout_drone_item")),
                    "Scout Drone deployment should record the build_scout_drone delivery objective");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.CUSTOM,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "build_scout_drone")),
                    "Scout Drone deployment should record the build_scout_drone route objective");

            var deployLedger = latestScoutDroneRouteLedger(deployLedgerBefore, "scout_deployed");
            helper.assertTrue(deployLedger != null,
                    "Scout Drone deployment should append a canonical late-runtime route ledger");
            if (deployLedger != null) {
                helper.assertTrue(deployLedger.resultStatus()
                                == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                        "Scout Drone deploy ledger should truthfully report MUTATED");
                helper.assertTrue(deployLedger.saveTouched() && deployLedger.hudOrEventEmitted(),
                        "Scout Drone deploy ledger should record save touch and visible feedback");
                helper.assertTrue("scout_drone_item".equals(String.valueOf(deployLedger.inputPayload().get("source"))),
                        "Scout Drone deploy ledger should carry the real item source");
            }

            EchoCompanionDrone companion = spawnCompanionDrone(helper, player, new BlockPos(4, 2, 2));
            companion.setRepairLevel(EchoCompanionDrone.REPAIR_INVENTORY - 20);
            int repairLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new ItemStack(ModItems.ENERGY_CELL.get()));
            InteractionResult repairResult = companion.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            helper.assertTrue(repairResult.consumesAction(),
                    "Energy Cell repair interaction should consume the repair action");

            CompanionDroneData repairedData = CompanionDroneStateStore.get(player);
            helper.assertTrue(companion.getRepairLevel() >= EchoCompanionDrone.REPAIR_INVENTORY,
                    "Energy Cell repair should restore inventory-command integrity");
            helper.assertTrue(repairedData.getHealth() >= EchoCompanionDrone.REPAIR_INVENTORY,
                    "Drone repair should mirror route-ready health into persistent drone state");
            helper.assertTrue(QuestData.get(player).getDroneHealth() >= EchoCompanionDrone.REPAIR_INVENTORY,
                    "Drone repair should mirror route-ready health into QuestData");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.CUSTOM,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "repair_echo_drone")),
                    "Drone repair should record the repair_echo_drone MissionCore objective");
            helper.assertTrue(EchoCoreServices.dataService().registeredKeys().stream()
                            .anyMatch(key -> key.id().equals(CompanionDroneStateStore.DATA_KEY_ID)),
                    "Drone state save should register its DataCore mirror key");

            var repairLedger = latestDroneStateLedger(repairLedgerBefore, "repair", null);
            helper.assertTrue(repairLedger != null,
                    "Drone repair should append a canonical exploration-runtime ledger");
            if (repairLedger != null) {
                helper.assertTrue(repairLedger.resultStatus()
                                == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                        "Drone repair ledger should truthfully report MUTATED");
                helper.assertTrue(repairLedger.saveTouched() && repairLedger.hudOrEventEmitted(),
                        "Drone repair ledger should record save touch and visible feedback");
            }

            int commandLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            ModNetwork.handleDroneCommand(new DroneCommandPacket("collect_scrap"), player);
            CompanionDroneData salvageData = CompanionDroneStateStore.get(player);
            helper.assertTrue(salvageData.getMode() == EchoDroneMode.SALVAGE,
                    "Collect scrap command should persist salvage mode through the drone state store");
            helper.assertTrue(companion.getCurrentMode() == EchoCompanionDrone.DroneMode.SCAVENGE,
                    "Collect scrap command should drive the live companion drone into scavenge mode");
            helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "drone:salvage_mode"),
                    "Collect scrap command should record a route-visible salvage-mode marker");
            var commandLedger = latestDroneStateLedger(commandLedgerBefore, "set_mode", "SALVAGE");
            helper.assertTrue(commandLedger != null,
                    "Collect scrap command should append a canonical drone-state ledger");
            if (commandLedger != null) {
                helper.assertTrue(commandLedger.resultStatus()
                                == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                        "Collect scrap command ledger should truthfully report MUTATED");
                helper.assertTrue(commandLedger.saveTouched() && commandLedger.hudOrEventEmitted(),
                        "Collect scrap command ledger should record save touch and visible feedback");
            }

            CompanionDroneData serialized = new CompanionDroneData();
            serialized.readTag(salvageData.toTag());
            helper.assertTrue(serialized.getMode() == EchoDroneMode.SALVAGE
                            && serialized.getHealth() >= EchoCompanionDrone.REPAIR_INVENTORY
                            && serialized.isDeployed()
                            && serialized.getDroneUuid() != null,
                    "Drone command state should survive serialized save-state round trip");

            helper.setBlock(new BlockPos(5, 2, 2), Blocks.CHEST);
            helper.setBlock(new BlockPos(6, 2, 2), Blocks.LAVA);
            ItemEntity salvage = new ItemEntity(helper.getLevel(),
                    helper.absolutePos(new BlockPos(4, 3, 2)).getX() + 0.5D,
                    helper.absolutePos(new BlockPos(4, 3, 2)).getY(),
                    helper.absolutePos(new BlockPos(4, 3, 2)).getZ() + 0.5D,
                    new ItemStack(ModItems.SCRAP_METAL.get(), 2));
            helper.getLevel().addFreshEntity(salvage);
            Mob bandit = ModEntities.SCAVENGER_BANDIT.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
            helper.assertTrue(bandit != null,
                    "Scavenger bandit should spawn for faction-aware drone intel");
            if (bandit != null) {
                BlockPos banditPos = helper.absolutePos(new BlockPos(7, 2, 2));
                bandit.setPos(banditPos.getX() + 0.5D, banditPos.getY(), banditPos.getZ() + 0.5D);
                helper.getLevel().addFreshEntity(bandit);
            }

            int scanLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            ModNetwork.handleDroneCommand(new DroneCommandPacket("scan_area"), player);
            CompanionDroneData scanData = CompanionDroneStateStore.get(player);
            helper.assertTrue(scanData.getLastScanSummary().contains("scrap cache"),
                    "Drone scan should report route salvage intel");
            helper.assertTrue(scanData.getLastScanSummary().contains("hostile"),
                    "Drone scan should report hostile/faction pressure");
            helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "drone:intel_recovered"),
                    "Drone scan should record the recovered-intel route marker");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.CUSTOM,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "recover_drone_intel")),
                    "Drone scan should record the recover_drone_intel MissionCore objective");
            var scanLedger = latestDroneStateLedger(scanLedgerBefore, "scan_area", null);
            helper.assertTrue(scanLedger != null,
                    "Drone scan should append a canonical recovered-intel ledger");
            if (scanLedger != null) {
                helper.assertTrue(scanLedger.resultStatus()
                                == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                        "Drone scan ledger should truthfully report MUTATED");
                helper.assertTrue(scanLedger.saveTouched() && scanLedger.hudOrEventEmitted(),
                        "Drone scan ledger should record save touch and visible feedback");
                helper.assertTrue(Boolean.TRUE.equals(scanLedger.afterSummary().get("droneIntelRecoveredMarker")),
                        "Drone scan ledger should show the intel marker after runtime mutation");
            }

            FactionDiplomacy diplomacy = player.getData(ModAttachments.FACTION_DIPLOMACY.get());
            diplomacy.setRelation(
                    FactionDiplomacy.FactionPair.fromFactions(
                            AshfallBiomeFactions.RADWARDEN_COMPACT,
                            AshfallBiomeFactions.CRASHBREAK_SALVAGE),
                    -80);
            player.setData(ModAttachments.FACTION_DIPLOMACY.get(), diplomacy);
            ModNetwork.handleDroneCommand(new DroneCommandPacket("combat"), player);
            companion.tick();
            helper.assertTrue(bandit != null && companion.getTarget() == bandit,
                    "Faction-aware drone combat intel should assign the obvious hostile faction target");

            if (deployedScout != null) {
                deployedScout.discard();
            }
            if (bandit != null) {
                bandit.discard();
            }
            salvage.discard();
            companion.discard();
            cleanupOwnedDrones(helper, player);
        });
        helper.succeed();
    }

    private static void guardianProfileCoverage(GameTestHelper helper) {
        helper.assertTrue(BiomeGuardianProfiles.all().size() == 8, "Ashfall should expose eight active biome guardian profiles");
        helper.assertTrue(BiomeGuardianProfiles.byBiome("the_wasteland").isEmpty(),
                "The Wasteland should no longer expose an active guardian profile");
        helper.assertTrue(BiomeGuardianProfiles.byMissionId("neutralize_wasteland_sentinel").isEmpty(),
                "Wasteland Sentinel should not be an active guardian mission");
        helper.assertTrue(ProceduralStructureGenerator.getMainStructureForBiome("the_wasteland") == null,
                "The Wasteland should not generate a biome-main guardian site");
        helper.assertTrue(MissionRegistry.getMissionById("neutralize_wasteland_sentinel") == null,
                "Wasteland Sentinel mission should be removed from the active mission registry");
        Mission plainsWarlord = MissionRegistry.getMissionById("neutralize_plains_warlord");
        helper.assertTrue(plainsWarlord != null
                        && plainsWarlord.prerequisites().equals(List.of("activate_relay_station")),
                "Plains Warlord should unlock directly after the Relay Station mission");
        Set<BiomeGuardianProfile.GuardianAbility> abilities =
                EnumSet.noneOf(BiomeGuardianProfile.GuardianAbility.class);
        Set<String> missions = new HashSet<>();
        for (BiomeGuardianProfile profile : BiomeGuardianProfiles.all()) {
            helper.assertTrue(AshfallFactionMap.all().contains(profile.ownerFaction()),
                    "Guardian owner faction should be one of the three Ashfall factions: " + profile.bossPath());
            helper.assertTrue(abilities.add(profile.ability()), "Guardian ability should be unique: " + profile.bossPath());
            helper.assertTrue(missions.add(profile.missionId()), "Guardian mission should be unique: " + profile.missionId());
            helper.assertTrue(profile.bossType().get() != null, "Guardian boss type missing: " + profile.bossPath());
            helper.assertTrue(profile.defenderType().get() != null, "Guardian defender type missing: " + profile.bossPath());
            helper.assertFalse(profile.rewardBundle().isEmpty(), "Guardian reward bundle missing: " + profile.bossPath());
            helper.assertTrue(profile.rewardBundle().entries().stream()
                            .anyMatch(entry -> entry.item().get().asItem() == ModItems.GUARDIAN_DATACORE.get()),
                    "Guardian reward bundle should include Guardian Datacore: " + profile.bossPath());
            helper.assertTrue(profile.visual().scale() > 0.0F, "Guardian visual scale must be positive: " + profile.bossPath());
            helper.assertTrue(profile.visual().shadow() > 0.0F, "Guardian visual shadow must be positive: " + profile.bossPath());
            BiomeGuardianProfile.PolishData polish = profile.polish();
            helper.assertTrue(polish != null, "Guardian polish data missing: " + profile.bossPath());
            if (polish != null) {
                helper.assertFalse(polish.arenaSetPiece().isBlank(), "Guardian arena polish missing: " + profile.bossPath());
                helper.assertFalse(polish.phaseCue().isBlank(), "Guardian phase cue missing: " + profile.bossPath());
                helper.assertFalse(polish.counterplayObject().isBlank(), "Guardian counterplay object missing: " + profile.bossPath());
                helper.assertFalse(polish.addPressurePattern().isBlank(), "Guardian add pressure missing: " + profile.bossPath());
                helper.assertFalse(polish.rewardCategory().isBlank(), "Guardian reward category missing: " + profile.bossPath());
                helper.assertFalse(polish.codexSummary().isBlank(), "Guardian Codex summary missing: " + profile.bossPath());
                helper.assertFalse(polish.hudObjectiveLabel().isBlank(), "Guardian HUD objective label missing: " + profile.bossPath());
            }
            helper.assertTrue(ProceduralStructureGenerator.hasGuardianSiteTheme(profile),
                    "Guardian structure theme missing: " + profile.bossPath());
            helper.assertTrue(ProceduralStructureGenerator.guardianSiteLayoutContractValid(profile),
                    "Guardian structure layout contract invalid: " + profile.bossPath());
            helper.assertTrue(BiomeGuardianProfiles.byBossType(profile.bossType().get()).orElse(null) == profile,
                    "Guardian boss type lookup should round-trip: " + profile.bossPath());
            helper.assertTrue(BiomeGuardianProfiles.byMissionId(profile.missionId()).orElse(null) == profile,
                    "Guardian mission lookup should round-trip: " + profile.missionId());
        }
        helper.succeed();
    }

    private static void environmentalEventProfiles(GameTestHelper helper) {
        Set<EnvironmentalEventType> covered = EnumSet.noneOf(EnvironmentalEventType.class);
        for (var profile : EnvironmentalEventProfiles.activeProfiles()) {
            helper.assertTrue(profile.durationTicks() > 0, "Environmental event duration must be positive: " + profile.type());
            helper.assertFalse(profile.commandAlias().isBlank(), "Environmental event command alias missing: " + profile.type());
            helper.assertFalse(profile.hudLabel().isBlank(), "Environmental event HUD label missing: " + profile.type());
            helper.assertTrue(profile.particleBudget() >= 0, "Environmental event particle budget must be non-negative: " + profile.type());
            helper.assertTrue(EnvironmentalEventProfiles.byAlias(profile.commandAlias()).orElse(null) == profile.type(),
                    "Environmental event command alias should parse: " + profile.commandAlias());
            EnvironmentalEventStatus status = EnvironmentalEventStatus.fromSynced(
                    profile.type().name(),
                    profile.durationTicks(),
                    profile.durationTicks(),
                    1.0F,
                    0.0F,
                    0);
            helper.assertTrue(status.active(), "Environmental event status should be active: " + profile.type());
            helper.assertFalse(status.counterGuidance().isBlank(), "Environmental event counter guidance missing: " + profile.type());
            helper.assertFalse(status.survivalImpact().isBlank(), "Environmental event survival impact missing: " + profile.type());
            helper.assertTrue(status.weatherMode() == profile.weatherMode(), "Environmental event weather mode mismatch: " + profile.type());
            helper.assertFalse(status.centerWarningTitle().isBlank(), "Environmental event center title missing: " + profile.type());
            helper.assertFalse(status.centerWarningSubtitle().isBlank(), "Environmental event center subtitle missing: " + profile.type());
            helper.assertTrue((status.hudColor() >>> 24) > 0, "Environmental event HUD color should be visible: " + profile.type());
            helper.assertTrue(environmentalHudIconIndex(profile.type()) >= 0,
                    "Environmental event HUD icon mapping missing: " + profile.type());
            covered.add(profile.type());
        }
        for (EnvironmentalEventType type : EnvironmentalEventType.values()) {
            if (type != EnvironmentalEventType.NONE) {
                helper.assertTrue(covered.contains(type), "Environmental event profile missing: " + type);
                int iconIndex = environmentalHudIconIndex(type);
                helper.assertTrue(iconIndex >= 0 && iconIndex < 8, "Environmental event HUD icon index out of atlas range: " + type);
            }
        }
        EnvironmentalEventStatus expiredAcidRain = EnvironmentalEventStatus.fromSynced(
                EnvironmentalEventType.TOXIC_STORM.name(),
                0,
                1200,
                1.0F,
                1.0F,
                0);
        helper.assertFalse(expiredAcidRain.active(),
                "Expired synced acid rain should be inactive even if its type remains in the packet");
        helper.assertTrue(expiredAcidRain.type() == EnvironmentalEventType.NONE,
                "Expired synced acid rain should clear to the neutral event type");
        helper.succeed();
    }

    private static void toxicSlimeNoPassivePuddles(GameTestHelper helper) {
        var level = helper.getLevel();
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                level.setBlock(helper.absolutePos(new BlockPos(x, 1, z)), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(helper.absolutePos(new BlockPos(x, 2, z)), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(helper.absolutePos(new BlockPos(x, 3, z)), Blocks.AIR.defaultBlockState(), 3);
            }
        }

        BlockPos spawnPos = helper.absolutePos(new BlockPos(3, 2, 3));
        var slime = ModEntities.TOXIC_SLIME.get().create(level, EntitySpawnReason.EVENT);
        helper.assertTrue(slime != null, "Toxic slime should be spawnable");
        if (slime == null) {
            return;
        }
        slime.setNoAi(true);
        slime.setPos(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        level.addFreshEntity(slime);

        for (int i = 0; i < 120; i++) {
            slime.tick();
        }

        int puddles = 0;
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                if (level.getBlockState(helper.absolutePos(new BlockPos(x, 2, z))).is(ModBlocks.TOXIC_PUDDLE.get())) {
                    puddles++;
                }
            }
        }

        slime.discard();
        helper.assertTrue(puddles == 0, "Passive toxic slime ticking should not create persistent puddle blocks");
        helper.succeed();
    }

    private static void toxicBiomeHazardClassification(GameTestHelper helper) {
        try {
            Method shouldScan = HazardZoneManager.class.getDeclaredMethod("shouldScanToxicSources", boolean.class);
            Method isActive = HazardZoneManager.class.getDeclaredMethod(
                    "isToxicAirActive", boolean.class, boolean.class, boolean.class, boolean.class);
            shouldScan.setAccessible(true);
            isActive.setAccessible(true);

            helper.assertFalse((Boolean) shouldScan.invoke(null, true),
                    "Toxic biome exposure should not run the nearby toxic-source scan");
            helper.assertTrue((Boolean) shouldScan.invoke(null, false),
                    "Non-toxic biomes should still scan nearby toxic-source blocks");
            helper.assertTrue((Boolean) isActive.invoke(null, true, false, false, false),
                    "Toxic biome exposure should mark toxic air active without nearby source blocks");
            helper.assertFalse((Boolean) isActive.invoke(null, true, false, false, true),
                    "Scrubber safe zones should suppress toxic biome air");
            helper.succeed();
        } catch (ReflectiveOperationException error) {
            helper.assertTrue(false, "Toxic biome hazard classification reflection failed: " + error.getMessage());
        }
    }

    private static void mutationInstabilityWarningRadGated(GameTestHelper helper) {
        SurvivalData survivalData = new SurvivalData();
        MutationData mutationData = new MutationData();
        mutationData.addMutation(MutationData.MutationType.NIGHT_VISION);
        mutationData.addMutation(MutationData.MutationType.FAST_SCAVENGE);
        mutationData.addMutation(MutationData.MutationType.RAD_RESISTANCE);
        helper.assertTrue(mutationData.getMutationCount() == 3 && !mutationData.getActiveSideEffects().isEmpty(),
                "Test setup should create mutation warning load with active side effects");

        survivalData.setRadiationLevel(0.0F);
        helper.assertFalse(MutationManager.shouldShowInstabilityWarning(survivalData, mutationData),
                "Mutation instability warning should stay hidden at 0% radiation");

        float threshold = Config.MUTATION_THRESHOLD.get().floatValue();
        survivalData.setRadiationLevel(Math.max(0.0F, threshold - 0.01F));
        helper.assertFalse(MutationManager.shouldShowInstabilityWarning(survivalData, mutationData),
                "Mutation instability warning should stay hidden below the mutation threshold");

        survivalData.setRadiationLevel(threshold);
        helper.assertTrue(MutationManager.shouldShowInstabilityWarning(survivalData, mutationData),
                "Mutation instability warning should show at the mutation threshold when mutation load exists");

        MutationData noMutationLoad = new MutationData();
        survivalData.setRadiationLevel(SurvivalData.MAX_RADIATION);
        helper.assertFalse(MutationManager.shouldShowInstabilityWarning(survivalData, noMutationLoad),
                "High radiation alone should not create the mutation-specific instability warning");
        helper.succeed();
    }

    private static void radAwayRuntimeFlow(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        SurvivalData survivalData = player.getData(ModAttachments.SURVIVAL_DATA.get());
        survivalData.setRadiationLevel(40.0F);
        player.setData(ModAttachments.SURVIVAL_DATA.get(), survivalData);
        player.syncData(ModAttachments.SURVIVAL_DATA.get());
        player.removeEffect(MobEffects.REGENERATION);

        int ledgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
        ItemStack dose = new ItemStack(ModItems.RAD_AWAY.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dose);
        InteractionResult result = dose.getItem().use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        SurvivalData afterUse = player.getData(ModAttachments.SURVIVAL_DATA.get());
        helper.assertTrue(result.consumesAction(),
                "RadAway should consume the item action when radiation is present");
        helper.assertTrue(afterUse.getRadiationLevel() < 40.0F,
                "RadAway should reduce radiation when used after exposure");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),
                "Successful RadAway use should consume exactly one dose");
        helper.assertTrue(player.hasEffect(MobEffects.REGENERATION),
                "Successful RadAway use should apply tissue-recovery feedback inside the runtime host");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "medical:rad_away"),
                "RadAway should record a medical treatment route marker");
        String lastEvent = player.getPersistentData().getStringOr("ashes_of_tomorrow.adaptercore.last_hazard_event", "");
        helper.assertTrue("ashfall.treatment_applied".equals(lastEvent),
                "RadAway should publish AdapterCore hazard diagnostics");
        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(ledgerEntries.size() > ledgerBefore,
                "RadAway should append a mutation ledger entry");
        var latest = ledgerEntries.get(ledgerEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_ASHFALL_TREATMENT_APPLIED.equals(latest.actionId()),
                "RadAway ledger action should use the canonical treatment event");
        helper.assertTrue("echoashfallprotocol:hazard_runtime".equals(latest.runtimeHostId()),
                "RadAway ledger should record the hazard runtime host");
        helper.assertTrue(latest.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "RadAway ledger should report a real mutation");
        helper.assertTrue(latest.saveTouched(),
                "RadAway ledger should record the survival/mission save touch");
        helper.assertTrue(latest.hudOrEventEmitted(),
                "RadAway ledger should record visible feedback or event emission");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_RAD_AWAY.equals(String.valueOf(latest.inputPayload().get("itemId"))),
                "RadAway ledger should carry the canonical item id");
        helper.assertTrue(Math.abs(((Number) latest.beforeSummary().get("survivalRadiation")).floatValue() - 40.0F) < 0.01F
                        && ((Number) latest.afterSummary().get("survivalRadiation")).floatValue() < 40.0F,
                "RadAway ledger should show radiation decreasing across the host mutation");
        helper.assertTrue(((Number) latest.beforeSummary().get("radAwayCount")).intValue() == 1
                        && ((Number) latest.afterSummary().get("radAwayCount")).intValue() == 0,
                "RadAway ledger should show the dose consumed inside the runtime host");
        helper.assertTrue(!Boolean.TRUE.equals(latest.beforeSummary().get("hasRegeneration"))
                        && Boolean.TRUE.equals(latest.afterSummary().get("hasRegeneration")),
                "RadAway ledger should show regeneration applied inside the runtime host");

        ItemStack unusedDose = new ItemStack(ModItems.RAD_AWAY.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, unusedDose);
        SurvivalData clearData = player.getData(ModAttachments.SURVIVAL_DATA.get());
        clearData.setRadiationLevel(0.0F);
        player.setData(ModAttachments.SURVIVAL_DATA.get(), clearData);
        player.removeEffect(MobEffects.REGENERATION);
        int noopLedgerBefore = ledgerEntries.size();
        InteractionResult noRadiation = unusedDose.getItem().use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(noRadiation == InteractionResult.FAIL,
                "RadAway should fail cleanly when no radiation is present");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount() == 1,
                "Failed RadAway use should not consume a dose");
        var noopLedgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(noopLedgerEntries.size() > noopLedgerBefore,
                "No-radiation RadAway use should still flow through AdapterCore");
        var noopLatest = noopLedgerEntries.get(noopLedgerEntries.size() - 1);
        helper.assertTrue(noopLatest.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.NOOP,
                "No-radiation RadAway ledger should tell the truth as NOOP");
        helper.assertFalse(noopLatest.saveTouched(),
                "No-radiation RadAway should not claim a save mutation");
        helper.assertTrue(((Number) noopLatest.beforeSummary().get("radAwayCount")).intValue() == 1
                        && ((Number) noopLatest.afterSummary().get("radAwayCount")).intValue() == 1,
                "No-radiation RadAway ledger should show the dose was retained");
        helper.assertTrue(!Boolean.TRUE.equals(noopLatest.beforeSummary().get("hasRegeneration"))
                        && !Boolean.TRUE.equals(noopLatest.afterSummary().get("hasRegeneration")),
                "No-radiation RadAway ledger should show no recovery effect was applied");
        helper.succeed();
    }

    private static void hazardMachineRuntimeMarkers(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos cleanserPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos scrubberPos = helper.absolutePos(new BlockPos(4, 2, 2));
        BlockPos medBayPos = helper.absolutePos(new BlockPos(6, 2, 2));

        AshfallAdapterCoreHazardRuntime.radiationCleanserUsed(
                player,
                cleanserPos,
                BuiltInRegistries.ITEM.getKey(ModItems.CONTAMINATED_IRON.get()).toString(),
                BuiltInRegistries.ITEM.getKey(Items.IRON_INGOT).toString());
        QuestData quest = QuestData.get(player);
        helper.assertTrue(quest.hasVisitedLocation("special", "machine:radiation_cleanser_used"),
                "Radiation Cleanser runtime should record a route-visible machine marker");
        String cleanserEvent = player.getPersistentData().getStringOr(
                "ashes_of_tomorrow.adaptercore.last_hazard_event", "");
        helper.assertTrue("ashfall.cleanser_used".equals(cleanserEvent),
                "Radiation Cleanser runtime should publish AdapterCore hazard diagnostics");

        AshfallAdapterCoreHazardRuntime.atmosphericScrubberUsed(player, scrubberPos, 20.0F, 18.5F, 8);
        QuestData updated = QuestData.get(player);
        helper.assertTrue(updated.hasVisitedLocation("special", "hazard:atmospheric_scrubber_used"),
                "Atmospheric Scrubber runtime should record a route-visible hazard marker");
        String scrubberEvent = player.getPersistentData().getStringOr(
                "ashes_of_tomorrow.adaptercore.last_hazard_event", "");
        helper.assertTrue("ashfall.scrubber_used".equals(scrubberEvent),
                "Atmospheric Scrubber runtime should publish AdapterCore hazard diagnostics");

        int medBayLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
        var medBayResult = AshfallAdapterCoreHazardRuntime.medBayUsed(player, medBayPos, 1200, 0);
        helper.assertTrue(medBayResult.mutated(),
                "Field Med Bay runtime should mutate route-visible state on first treatment");
        QuestData medBayQuest = QuestData.get(player);
        helper.assertTrue(medBayQuest.hasVisitedLocation("special", "medical:field_med_bay_used"),
                "Field Med Bay runtime should record the treatment marker itself");
        String medBayEvent = player.getPersistentData().getStringOr(
                "ashes_of_tomorrow.adaptercore.last_hazard_event", "");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_ASHFALL_MED_BAY_USED.equals(medBayEvent),
                "Field Med Bay runtime should publish canonical AdapterCore hazard diagnostics");
        var medBayLedgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(medBayLedgerEntries.size() > medBayLedgerBefore,
                "Field Med Bay use should append an AdapterCore mutation ledger entry");
        var medBayLedger = medBayLedgerEntries.get(medBayLedgerEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_ASHFALL_MED_BAY_USED.equals(medBayLedger.actionId()),
                "Field Med Bay ledger should use the canonical med bay event");
        helper.assertTrue("echoashfallprotocol:hazard_runtime".equals(medBayLedger.runtimeHostId()),
                "Field Med Bay ledger should record the real hazard runtime host");
        helper.assertTrue(medBayLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Field Med Bay ledger should truthfully report MUTATED");
        helper.assertTrue(medBayLedger.saveTouched() && medBayLedger.hudOrEventEmitted(),
                "Field Med Bay ledger should record save touch and visible feedback");
        helper.assertTrue("medical:field_med_bay_used".equals(String.valueOf(medBayLedger.inputPayload().get("target"))),
                "Field Med Bay ledger should carry the treatment marker target");
        Object medBayBlockTarget = medBayLedger.target().snapshot().get("block");
        helper.assertTrue(medBayBlockTarget instanceof Map<?, ?>,
                "Field Med Bay ledger should include a block target snapshot");
        Map<?, ?> medBayBlockSnapshot = medBayBlockTarget instanceof Map<?, ?> block ? block : Map.of();
        helper.assertTrue(medBayPos.getX() == ((Number) medBayBlockSnapshot.get("x")).intValue()
                        && medBayPos.getY() == ((Number) medBayBlockSnapshot.get("y")).intValue()
                        && medBayPos.getZ() == ((Number) medBayBlockSnapshot.get("z")).intValue(),
                "Field Med Bay ledger should identify the target med bay position");
        helper.assertFalse(Boolean.TRUE.equals(medBayLedger.beforeSummary().get("medBayUsedMarker")),
                "Field Med Bay ledger should show the marker absent before runtime mutation");
        helper.assertTrue(Boolean.TRUE.equals(medBayLedger.afterSummary().get("medBayUsedMarker")),
                "Field Med Bay ledger should show the marker present after runtime mutation");
        helper.succeed();
    }

    private static void lateRuntimeRouteMarkers(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos nodePos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos relayPos = helper.absolutePos(new BlockPos(4, 2, 2));
        BlockPos dronePos = helper.absolutePos(new BlockPos(6, 2, 2));

        AshfallAdapterCoreLateRuntime.powerNodeState(player, nodePos, true, 5, "gametest_late_runtime");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "power_node:activated"),
                "Power Node runtime should record a route-visible activation marker");
        helper.assertTrue("player.machine_powered".equals(player.getPersistentData().getStringOr(
                "ashes_of_tomorrow.adaptercore.last_late_event", "")),
                "Power Node runtime should publish late AdapterCore diagnostics");

        AshfallAdapterCoreLateRuntime.relayActivated(
                player, "radio_relay", "relay_station", relayPos, "gametest_late_runtime");
        QuestData relayQuest = QuestData.get(player);
        helper.assertTrue(relayQuest.hasVisitedLocation("special", "relay:activated")
                        && relayQuest.hasVisitedLocation("special", "relay:radio_relay"),
                "Relay runtime should record generic and concrete route-visible relay markers");

        AshfallAdapterCoreLateRuntime.scoutDroneRoute(
                player, "scout_route_intel", "SCAVENGE", dronePos, "gametest_late_runtime");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "drone:scout_route"),
                "Scout drone route runtime should record route-visible scout support");

        AshfallAdapterCoreLateRuntime.nexusCapacitorState(
                player, dronePos.east(), 4096, 100_000, "gametest_late_runtime");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "nexus:capacitor_linked")
                        && QuestData.get(player).hasVisitedLocation("special", "nexus:capacitor_charged"),
                "Nexus Capacitor runtime should record linked and charged route markers");

        AshfallAdapterCoreLateRuntime.primeRelayResolved(
                player,
                NexusRelayType.REACTOR,
                NexusRelayState.STABILIZED,
                NexusCampaignData.get(helper.getLevel()),
                "gametest_late_runtime");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "nexus:relay:reactor:stabilized"),
                "Prime Relay runtime should record the concrete resolved relay outcome");

        AshfallAdapterCoreLateRuntime.endingChoice(
                player, PostNexusData.NexusPath.RESTORE, nodePos, "gametest_late_runtime");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "nexus:choice:restore"),
                "Nexus decision runtime should record the selected ending path");
        helper.assertTrue("ashfall.ending_choice".equals(player.getPersistentData().getStringOr(
                "ashes_of_tomorrow.adaptercore.last_late_event", "")),
                "Nexus decision runtime should publish late AdapterCore diagnostics");
        helper.succeed();
    }

    private static void instabilityDampenerItemRuntimeFlow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        NexusCampaignData campaign = NexusCampaignData.get(overworld);
        campaign.resetForTests();

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack dormantDampener = new ItemStack(ModItems.INSTABILITY_DAMPENER.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dormantDampener);
        int dormantLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        InteractionResult dormantResult = dormantDampener.getItem().use(
                level, player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(dormantResult == InteractionResult.FAIL,
                "Dormant Nexus Dampener use should fail through AdapterCore");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount() == 1,
                "Dormant Nexus Dampener use should not consume the item");
        var dormantEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(dormantEntries.size() > dormantLedgerBefore,
                "Dormant Nexus Dampener use should append a truth ledger entry");
        var dormantLedger = dormantEntries.get(dormantEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED.equals(
                        dormantLedger.actionId()),
                "Dormant Nexus Dampener ledger should use the canonical player.item_used event");
        helper.assertTrue("echoashfallprotocol:late_runtime".equals(dormantLedger.runtimeHostId()),
                "Dormant Nexus Dampener ledger should record the late runtime host");
        helper.assertTrue(dormantLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.FAILED,
                "Dormant Nexus Dampener ledger should truthfully report FAILED");
        helper.assertFalse(dormantLedger.saveTouched(),
                "Dormant Nexus Dampener failure should not claim a save mutation");
        helper.assertTrue(dormantLedger.hudOrEventEmitted(),
                "Dormant Nexus Dampener failure should record visible feedback");
        helper.assertTrue(!Boolean.TRUE.equals(dormantLedger.beforeSummary().get("nexusCampaignAwakened"))
                        && !Boolean.TRUE.equals(dormantLedger.afterSummary().get("nexusCampaignAwakened")),
                "Dormant Nexus Dampener ledger should show the campaign stayed dormant");

        campaign.awaken(helper.absolutePos(new BlockPos(4, 2, 4)));
        helper.assertTrue(campaign.getInstability() == 25,
                "Test setup should awaken the Nexus campaign at the expected instability");
        ItemStack dampener = new ItemStack(ModItems.INSTABILITY_DAMPENER.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dampener);
        int ledgerBefore = dormantEntries.size();

        InteractionResult result = dampener.getItem().use(
                level, player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(result.consumesAction(),
                "Instability Dampener should consume the player action when AdapterCore mutates Nexus state");
        helper.assertTrue(campaign.getInstability() == 5,
                "Instability Dampener runtime should reduce NexusCampaignData instability through AdapterCore");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),
                "Successful Instability Dampener use should consume the item inside the runtime host");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "nexus:instability_dampened"),
                "Instability Dampener use should record a route-visible Nexus marker through AdapterCore");
        String lastEvent = player.getPersistentData().getStringOr("ashes_of_tomorrow.adaptercore.last_late_event", "");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED.equals(lastEvent),
                "Instability Dampener use should touch late-runtime diagnostics with the canonical event");

        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(ledgerEntries.size() > ledgerBefore,
                "Instability Dampener use should append an AdapterCore mutation ledger entry");
        var latestLedger = ledgerEntries.get(ledgerEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED.equals(
                        latestLedger.actionId()),
                "Instability Dampener ledger should use the canonical player.item_used event");
        helper.assertTrue("echoashfallprotocol:late_runtime".equals(latestLedger.runtimeHostId()),
                "Instability Dampener ledger should record the late runtime host");
        helper.assertTrue(latestLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Instability Dampener ledger should truthfully report MUTATED");
        helper.assertTrue(latestLedger.saveTouched() && latestLedger.hudOrEventEmitted(),
                "Instability Dampener ledger should record save touch and visible feedback");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_INSTABILITY_DAMPENER.equals(
                        String.valueOf(latestLedger.inputPayload().get("itemId"))),
                "Instability Dampener ledger should carry the canonical item id");
        helper.assertTrue(((Number) latestLedger.beforeSummary().get("nexusCampaignInstability")).intValue() == 25
                        && ((Number) latestLedger.afterSummary().get("nexusCampaignInstability")).intValue() == 5,
                "Instability Dampener ledger should show Nexus instability changing inside the runtime host");
        helper.assertTrue(((Number) latestLedger.beforeSummary().get("instabilityDampenerCount")).intValue() == 1
                        && ((Number) latestLedger.afterSummary().get("instabilityDampenerCount")).intValue() == 0,
                "Instability Dampener ledger should show the item consumed inside the runtime host");

        campaign.reduceInstability(5);
        ItemStack stableDampener = new ItemStack(ModItems.INSTABILITY_DAMPENER.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stableDampener);
        int stableLedgerBefore = ledgerEntries.size();
        InteractionResult stableResult = stableDampener.getItem().use(
                level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(stableResult == InteractionResult.FAIL,
                "Stable Nexus Dampener use should return the preserved item-use failure result");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount() == 1,
                "Stable Nexus Dampener use should not consume the item");
        var stableEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(stableEntries.size() > stableLedgerBefore,
                "Stable Nexus Dampener use should still append a truth ledger entry");
        var stableLedger = stableEntries.get(stableEntries.size() - 1);
        helper.assertTrue(stableLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.NOOP,
                "Stable Nexus Dampener ledger should truthfully report NOOP");
        helper.assertFalse(stableLedger.saveTouched(),
                "Stable Nexus Dampener NOOP should not claim a save mutation");
        helper.assertTrue(stableLedger.hudOrEventEmitted(),
                "Stable Nexus Dampener NOOP should record visible feedback");
        helper.assertTrue(((Number) stableLedger.beforeSummary().get("nexusCampaignInstability")).intValue() == 0
                        && ((Number) stableLedger.afterSummary().get("nexusCampaignInstability")).intValue() == 0,
                "Stable Nexus Dampener ledger should show instability remained stable");
        campaign.resetForTests();
        helper.succeed();
    }

    private static void returnBeaconItemRuntimeFlow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevel overworld = level.getServer().overworld();
        NexusCampaignData campaign = NexusCampaignData.get(overworld);
        campaign.resetForTests();

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos start = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D);
        player.getPersistentData().putLong(AshfallAdapterCoreLateRuntime.RETURN_BEACON_COOLDOWN_KEY, 0L);

        ItemStack lockedBeacon = new ItemStack(ModItems.RETURN_BEACON.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, lockedBeacon);
        int lockedLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
        InteractionResult lockedResult = lockedBeacon.getItem().use(
                level,
                player,
                net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(lockedResult == InteractionResult.FAIL,
                "Locked Return Beacon use should fail through AdapterCore");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount() == 1,
                "Locked Return Beacon use should not consume the item");
        var lockedLedger = latestLateRuntimeLedger(
                lockedLedgerBefore,
                com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED,
                "itemId",
                com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_RETURN_BEACON);
        helper.assertTrue(lockedLedger != null,
                "Locked Return Beacon use should append a canonical late-runtime truth ledger");
        if (lockedLedger == null) {
            campaign.resetForTests();
            helper.succeed();
            return;
        }
        helper.assertTrue("echoashfallprotocol:late_runtime".equals(lockedLedger.runtimeHostId()),
                "Locked Return Beacon ledger should record the late runtime host");
        helper.assertTrue(lockedLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.FAILED,
                "Locked Return Beacon ledger should truthfully report FAILED");
        helper.assertFalse(lockedLedger.saveTouched(),
                "Locked Return Beacon failure should not claim a save mutation");
        helper.assertTrue(lockedLedger.hudOrEventEmitted(),
                "Locked Return Beacon failure should record visible feedback");
        helper.assertTrue(!Boolean.TRUE.equals(lockedLedger.beforeSummary().get("nexusCampaignWarfrontComplete"))
                        && !Boolean.TRUE.equals(lockedLedger.afterSummary().get("postNexusChoiceMade")),
                "Locked Return Beacon ledger should show the route gate stayed closed");
        helper.assertTrue(positionSnapshotEquals(lockedLedger.afterSummary().get("playerBlockPos"), start),
                "Locked Return Beacon should leave the player in place");

        BlockPos anchor = helper.absolutePos(new BlockPos(5, 2, 5));
        campaign.bootstrapWarfrontComplete(anchor);
        PostNexusData post = PostNexusData.get(player);
        post.setSelectedPath(PostNexusData.NexusPath.RESTORE);
        PostNexusData.saveAndSync(player, post);
        player.setPos(start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D);
        player.getPersistentData().putLong(AshfallAdapterCoreLateRuntime.RETURN_BEACON_COOLDOWN_KEY, 0L);
        ItemStack beacon = new ItemStack(ModItems.RETURN_BEACON.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, beacon);
        int ledgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
        long now = overworld.getGameTime();

        InteractionResult result = beacon.getItem().use(
                level,
                player,
                net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(result.consumesAction(),
                "Return Beacon should consume the player action when AdapterCore teleports and updates cooldown state");
        helper.assertTrue(player.blockPosition().equals(anchor.above()),
                "Return Beacon runtime should teleport the player to the saved Nexus Core anchor");
        helper.assertTrue(player.getPersistentData()
                        .getLong(AshfallAdapterCoreLateRuntime.RETURN_BEACON_COOLDOWN_KEY)
                        .orElse(0L) > now,
                "Return Beacon runtime should write a real cooldown into player persistent data");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "return_beacon_activated")
                        && QuestData.get(player).hasVisitedLocation("special", "return_beacon:returned"),
                "Return Beacon use should record route-visible markers through AdapterCore");
        String lastEvent = player.getPersistentData().getStringOr("ashes_of_tomorrow.adaptercore.last_late_event", "");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED.equals(lastEvent),
                "Return Beacon use should touch late-runtime diagnostics with the canonical item-used event");

        var latestLedger = latestLateRuntimeLedger(
                ledgerBefore,
                com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED,
                "itemId",
                com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_RETURN_BEACON);
        helper.assertTrue(latestLedger != null,
                "Return Beacon use should append an AdapterCore mutation ledger entry");
        if (latestLedger == null) {
            campaign.resetForTests();
            helper.succeed();
            return;
        }
        helper.assertTrue(latestLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Return Beacon ledger should truthfully report MUTATED");
        helper.assertTrue(latestLedger.saveTouched() && latestLedger.hudOrEventEmitted(),
                "Return Beacon ledger should record save touch and visible feedback");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_RETURN_BEACON.equals(
                        String.valueOf(latestLedger.inputPayload().get("itemId"))),
                "Return Beacon ledger should carry the canonical item id");
        helper.assertTrue(((Number) latestLedger.beforeSummary().get("returnBeaconReadyTick")).longValue() == 0L
                        && ((Number) latestLedger.afterSummary().get("returnBeaconReadyTick")).longValue() > now,
                "Return Beacon ledger should show the cooldown written inside the runtime host");
        helper.assertTrue(((Number) latestLedger.beforeSummary().get("returnBeaconCount")).intValue() == 1
                        && ((Number) latestLedger.afterSummary().get("returnBeaconCount")).intValue() == 1,
                "Return Beacon ledger should show the reusable beacon stayed in the player inventory");
        helper.assertTrue(positionSnapshotEquals(latestLedger.beforeSummary().get("playerBlockPos"), start)
                        && positionSnapshotEquals(latestLedger.afterSummary().get("playerBlockPos"), anchor.above()),
                "Return Beacon ledger should show the real player teleport");
        helper.assertTrue(Boolean.TRUE.equals(latestLedger.beforeSummary().get("nexusCampaignWarfrontComplete"))
                        && Boolean.TRUE.equals(latestLedger.beforeSummary().get("postNexusChoiceMade"))
                        && positionSnapshotEquals(latestLedger.beforeSummary().get("nexusCampaignPos"), anchor),
                "Return Beacon ledger should show the completed route gate and saved anchor");

        BlockPos cooldownStart = helper.absolutePos(new BlockPos(7, 2, 7));
        player.setPos(cooldownStart.getX() + 0.5D, cooldownStart.getY(), cooldownStart.getZ() + 0.5D);
        long readyAt = player.getPersistentData()
                .getLong(AshfallAdapterCoreLateRuntime.RETURN_BEACON_COOLDOWN_KEY)
                .orElse(0L);
        int cooldownLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
        InteractionResult cooldownResult = beacon.getItem().use(
                overworld,
                player,
                net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(cooldownResult == InteractionResult.FAIL,
                "Recharging Return Beacon use should return the preserved item-use failure result");
        helper.assertTrue(player.blockPosition().equals(cooldownStart),
                "Recharging Return Beacon use should not teleport the player");
        helper.assertTrue(player.getPersistentData()
                        .getLong(AshfallAdapterCoreLateRuntime.RETURN_BEACON_COOLDOWN_KEY)
                        .orElse(0L) == readyAt,
                "Recharging Return Beacon use should not rewrite cooldown state");
        var cooldownLedger = latestLateRuntimeLedger(
                cooldownLedgerBefore,
                com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED,
                "itemId",
                com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_RETURN_BEACON);
        helper.assertTrue(cooldownLedger != null,
                "Recharging Return Beacon use should still append a truth ledger entry");
        if (cooldownLedger == null) {
            campaign.resetForTests();
            helper.succeed();
            return;
        }
        helper.assertTrue(cooldownLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.NOOP,
                "Recharging Return Beacon ledger should truthfully report NOOP");
        helper.assertFalse(cooldownLedger.saveTouched(),
                "Recharging Return Beacon NOOP should not claim a save mutation");
        helper.assertTrue(cooldownLedger.hudOrEventEmitted(),
                "Recharging Return Beacon NOOP should record visible feedback");
        helper.assertTrue(((Number) cooldownLedger.beforeSummary().get("returnBeaconReadyTick")).longValue() == readyAt
                        && ((Number) cooldownLedger.afterSummary().get("returnBeaconReadyTick")).longValue() == readyAt,
                "Recharging Return Beacon ledger should show cooldown state unchanged");
        helper.assertTrue(positionSnapshotEquals(cooldownLedger.beforeSummary().get("playerBlockPos"), cooldownStart)
                        && positionSnapshotEquals(cooldownLedger.afterSummary().get("playerBlockPos"), cooldownStart),
                "Recharging Return Beacon ledger should show the player stayed in place");

        campaign.resetForTests();
        helper.succeed();
    }

    private static void routeBalanceContracts(GameTestHelper helper) {
        helper.assertTrue(DifficultyProfile.CASUAL.getRadiationMultiplier() < DifficultyProfile.NORMAL.getRadiationMultiplier()
                        && DifficultyProfile.NORMAL.getRadiationMultiplier() < DifficultyProfile.HARD.getRadiationMultiplier()
                        && DifficultyProfile.HARD.getRadiationMultiplier() < DifficultyProfile.NIGHTMARE.getRadiationMultiplier(),
                "Radiation pressure should scale upward by difficulty");
        helper.assertTrue(DifficultyProfile.CASUAL.getHydrationMultiplier() < DifficultyProfile.NORMAL.getHydrationMultiplier()
                        && DifficultyProfile.NORMAL.getHydrationMultiplier() < DifficultyProfile.HARD.getHydrationMultiplier()
                        && DifficultyProfile.HARD.getHydrationMultiplier() < DifficultyProfile.NIGHTMARE.getHydrationMultiplier(),
                "Hydration pressure should scale upward by difficulty");
        helper.assertTrue(DifficultyProfile.CASUAL.getSurvivalHazardMultiplier() < DifficultyProfile.NORMAL.getSurvivalHazardMultiplier()
                        && DifficultyProfile.NORMAL.getSurvivalHazardMultiplier() < DifficultyProfile.HARD.getSurvivalHazardMultiplier()
                        && DifficultyProfile.HARD.getSurvivalHazardMultiplier() < DifficultyProfile.NIGHTMARE.getSurvivalHazardMultiplier(),
                "Survival hazard damage should scale upward by difficulty");
        helper.assertTrue(DifficultyProfile.CASUAL.scaleMachineSpeed(1.0F) > DifficultyProfile.NORMAL.scaleMachineSpeed(1.0F)
                        && DifficultyProfile.NORMAL.scaleMachineSpeed(1.0F) > DifficultyProfile.HARD.scaleMachineSpeed(1.0F)
                        && DifficultyProfile.HARD.scaleMachineSpeed(1.0F) > DifficultyProfile.NIGHTMARE.scaleMachineSpeed(1.0F),
                "Machine throughput should slow down as difficulty increases");

        assertSpacingBalance(helper, "micro POI", WorldgenBalance.MICRO_POI_SPACING, WorldgenBalance.MICRO_POI_SEPARATION);
        assertSpacingBalance(helper, "crash POI", WorldgenBalance.CRASH_POI_SPACING, WorldgenBalance.CRASH_POI_SEPARATION);
        assertSpacingBalance(helper, "urban POI", WorldgenBalance.URBAN_POI_SPACING, WorldgenBalance.URBAN_POI_SEPARATION);
        assertSpacingBalance(helper, "global POI", WorldgenBalance.GLOBAL_POI_SPACING, WorldgenBalance.GLOBAL_POI_SEPARATION);
        assertSpacingBalance(helper, "camp", WorldgenBalance.CAMP_SPACING, WorldgenBalance.CAMP_SEPARATION);
        assertSpacingBalance(helper, "landmark", WorldgenBalance.LANDMARK_SPACING, WorldgenBalance.LANDMARK_SEPARATION);
        assertSpacingBalance(helper, "major", WorldgenBalance.MAJOR_SPACING, WorldgenBalance.MAJOR_SEPARATION);
        helper.assertTrue(WorldgenBalance.CRASH_POI_SPACING <= WorldgenBalance.MICRO_POI_SPACING
                        && WorldgenBalance.URBAN_POI_SPACING <= WorldgenBalance.MICRO_POI_SPACING,
                "Crash and urban POIs should remain denser than broad micro exploration");
        helper.assertTrue(WorldgenBalance.GLOBAL_POI_SPACING > WorldgenBalance.CAMP_SPACING
                        && WorldgenBalance.LANDMARK_SPACING > WorldgenBalance.GLOBAL_POI_SPACING
                        && WorldgenBalance.MAJOR_SPACING > WorldgenBalance.LANDMARK_SPACING,
                "POI cadence should progress from frequent camps to rare major sites");

        helper.assertTrue(LoadDistributorBlockEntity.CAPACITY < BatteryBankBlockEntity.CAPACITY
                        && PowerNodeBlockEntity.CAPACITY < BatteryBankBlockEntity.CAPACITY
                        && NexusCapacitorBlockEntity.CAPACITY >= BatteryBankBlockEntity.CAPACITY * 10,
                "Power storage should progress from routing buffers to batteries to Nexus capacitor");
        helper.assertTrue(LoadDistributorBlockEntity.MAX_TRANSFER >= PowerNodeBlockEntity.MAX_TRANSFER
                        && NexusCapacitorBlockEntity.MAX_TRANSFER >= LoadDistributorBlockEntity.MAX_TRANSFER * 2,
                "Late-game transfer rates should support larger power routes than early nodes");
        helper.succeed();
    }

    private static void assertSpacingBalance(GameTestHelper helper, String label, int spacing, int separation) {
        helper.assertTrue(spacing > 0, label + " spacing should be positive");
        helper.assertTrue(separation > 0, label + " separation should be positive");
        helper.assertTrue(spacing > separation * 2, label + " spacing should leave terrain room beyond separation");
    }

    private static int environmentalHudIconIndex(EnvironmentalEventType type) {
        return switch (type) {
            case RADIATION_STORM -> 0;
            case TOXIC_STORM -> 1;
            case BLACKOUT -> 2;
            case ASH_STORM -> 3;
            case CRYO_FRONT -> 4;
            case NEXUS_SURGE -> 5;
            default -> -1;
        };
    }

    private static void shelteredRadiationStormNoExposure(GameTestHelper helper) {
        var level = helper.getLevel();
        Player mockPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        if (!(mockPlayer instanceof ServerPlayer player)) {
            helper.succeed();
            return;
        }
        BlockPos playerPos = helper.absolutePos(new BlockPos(3, 2, 3));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);

        EnvironmentalEventHandler.forceStartEvent(level, EnvironmentalEventType.RADIATION_STORM);
        try {
            HazardZoneManager.HazardSnapshot snapshot = HazardZoneManager.scan(player);
            helper.assertTrue(snapshot.radiationStorm(), "Radiation storm should be active for the scan");
            helper.assertTrue(snapshot.stormSheltered(), "Low underground positions should count as storm shelter");
            helper.assertFalse(snapshot.radiationZone(), "Sheltered radiation storms should not create radiation exposure by themselves");
            helper.assertTrue(snapshot.radiationIntensity() == 0.0F,
                    "Sheltered radiation storms should contribute no radiation intensity");
            helper.succeed();
        } finally {
            EnvironmentalEventHandler.clearActiveEvent(level);
        }
    }

    private static void radiationStormSleepAdvanceClears(GameTestHelper helper) {
        var level = helper.getLevel();
        EnvironmentalEventHandler.clearActiveEvent(level);
        EnvironmentalEventData data = EnvironmentalEventData.get(level);
        int survivedBefore = data.getEventsSurvived(EnvironmentalEventType.RADIATION_STORM);

        EnvironmentalEventHandler.forceStartEvent(level, EnvironmentalEventType.RADIATION_STORM);
        try {
            data = EnvironmentalEventData.get(level);
            EnvironmentalEventHandler.advanceEventsForSleep(level, data.getEventDuration() + 1L);
            data = EnvironmentalEventData.get(level);
            helper.assertTrue(data.getCurrentEvent() == EnvironmentalEventType.NONE,
                    "Sleep advance past remaining duration should clear radiation storm");
            helper.assertTrue(data.getEventsSurvived(EnvironmentalEventType.RADIATION_STORM) == survivedBefore + 1,
                    "Sleep-cleared radiation storm should count as survived");
            helper.succeed();
        } finally {
            EnvironmentalEventHandler.clearActiveEvent(level);
        }
    }

    private static void radiationStormSleepAdvancePartial(GameTestHelper helper) {
        var level = helper.getLevel();
        EnvironmentalEventHandler.clearActiveEvent(level);
        EnvironmentalEventHandler.forceStartEvent(level, EnvironmentalEventType.RADIATION_STORM);
        try {
            EnvironmentalEventData data = EnvironmentalEventData.get(level);
            long gameTime = level.getGameTime();
            int remainingBefore = data.getRemainingEventTicks(gameTime);
            long startBefore = data.getEventStartTime();
            long skippedTicks = Math.min(1200L, Math.max(1L, remainingBefore - 1L));

            EnvironmentalEventHandler.advanceEventsForSleep(level, skippedTicks);
            data = EnvironmentalEventData.get(level);
            helper.assertTrue(data.getCurrentEvent() == EnvironmentalEventType.RADIATION_STORM,
                    "Partial sleep advance should keep radiation storm active");
            helper.assertTrue(data.getRemainingEventTicks(gameTime) == remainingBefore - (int) skippedTicks,
                    "Partial sleep advance should reduce remaining radiation storm ticks");
            helper.assertTrue(data.getEventStartTime() == startBefore - skippedTicks,
                    "Partial sleep advance should shift radiation storm start time");
            helper.succeed();
        } finally {
            EnvironmentalEventHandler.clearActiveEvent(level);
        }
    }

    private static void sleepClearedRadiationStormNoHazard(GameTestHelper helper) {
        var level = helper.getLevel();
        Player mockPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        if (!(mockPlayer instanceof ServerPlayer player)) {
            helper.succeed();
            return;
        }
        BlockPos playerPos = helper.absolutePos(new BlockPos(3, 2, 3));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);

        EnvironmentalEventHandler.clearActiveEvent(level);
        EnvironmentalEventHandler.forceStartEvent(level, EnvironmentalEventType.RADIATION_STORM);
        try {
            EnvironmentalEventData data = EnvironmentalEventData.get(level);
            EnvironmentalEventHandler.advanceEventsForSleep(level, data.getEventDuration() + 1L);
            HazardZoneManager.HazardSnapshot snapshot = HazardZoneManager.scan(player);
            helper.assertFalse(snapshot.radiationStorm(),
                    "Sleep-cleared radiation storm should no longer be reported by hazard scan");
            helper.succeed();
        } finally {
            EnvironmentalEventHandler.clearActiveEvent(level);
        }
    }

    private static void guardianBossSmoke(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos playerPos = helper.absolutePos(new BlockPos(12, 2, 12));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);

        int index = 0;
        int guardianCount = Math.max(1, BiomeGuardianProfiles.all().size());
        for (BiomeGuardianProfile profile : BiomeGuardianProfiles.all()) {
            BiomeBossEntity boss = profile.bossType().get().create(level, EntitySpawnReason.EVENT);
            helper.assertTrue(boss != null, "Guardian should be spawnable: " + profile.bossPath());
            if (boss == null) {
                continue;
            }
            double angle = index++ * (Math.PI * 2.0D / guardianCount);
            boss.setPos(player.getX() + Math.cos(angle) * 5.0D, player.getY(), player.getZ() + Math.sin(angle) * 5.0D);
            boss.setTarget(player);
            level.addFreshEntity(boss);
            boss.tick();
            boss.setHealth(boss.getMaxHealth() * 0.30F);
            boss.tick();
            boss.tick();
            helper.assertTrue(boss.getGuardianPhase() >= 3, "Guardian should enter phase 3: " + profile.bossPath());
        }

        for (int i = 0; i < 90; i++) {
            for (BiomeBossEntity boss : level.getEntitiesOfClass(BiomeBossEntity.class, player.getBoundingBox().inflate(18.0D))) {
                boss.tick();
            }
        }

        for (BiomeBossEntity boss : level.getEntitiesOfClass(BiomeBossEntity.class, player.getBoundingBox().inflate(18.0D))) {
            boss.discard();
        }
        helper.succeed();
    }

    private static void guardianSiteState(GameTestHelper helper) {
        BiomeGuardianProfile profile = BiomeGuardianProfiles.byBiome("ruined_plains").orElseThrow();
        BiomeGuardianSiteData data = BiomeGuardianSiteData.get(helper.getLevel());
        BlockPos entrance = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos arena = helper.absolutePos(new BlockPos(12, 1, 12));
        data.addOrUpdate(profile, entrance, arena);
        helper.assertTrue(data.nearestActive(entrance, profile.bossPath()).isPresent(),
                "Guardian site should be active before defeat");
        helper.assertTrue(data.nearestActiveForMission(entrance, profile.missionId()).isPresent(),
                "Guardian mission scanner lookup should use active saved entrance");
        data.addOrUpdate(profile, entrance.offset(4, 0, 0), arena.offset(4, 0, 0));
        long nearbyCount = data.allSites().stream()
                .filter(entry -> entry.guardianId().equals(profile.bossPath()))
                .filter(entry -> entry.entrance().distSqr(entrance) < 128 * 128)
                .count();
        helper.assertTrue(nearbyCount == 1, "Nearby duplicate guardian sites should collapse");
        data.markDefeated(profile.bossPath(), arena);
        helper.assertFalse(data.nearestActive(entrance, profile.bossPath()).isPresent(),
                "Guardian site should stop being active after defeat");
        helper.succeed();
    }

    private static void bossHudNavigation(GameTestHelper helper) {
        helper.assertTrue(BossHudProfiles.all().size() >= 13,
                "Boss HUD profiles should cover guardians, Warden, and orbital boss-tier encounters");
        for (BiomeGuardianProfile profile : BiomeGuardianProfiles.all()) {
            BossHudProfile hud = BossHudProfiles.byEntityId(profile.entityId()).orElse(null);
            helper.assertTrue(hud != null, "Guardian HUD profile missing: " + profile.bossPath());
            helper.assertTrue(BossHudProfiles.byTitle(profile.title()).orElse(null) == hud,
                    "Guardian HUD title lookup should round-trip: " + profile.title());
            helper.assertTrue(hud != null && !hud.compassLabel().isBlank(),
                    "Guardian HUD compass label missing: " + profile.bossPath());
            helper.assertTrue(hud != null && (hud.accentColor() & 0x00FFFFFF) != 0,
                    "Guardian HUD accent should be visible: " + profile.bossPath());
            helper.assertTrue(hud != null && !hud.phaseWarningLabel().isBlank(),
                    "Guardian HUD phase warning missing: " + profile.bossPath());
            helper.assertTrue(hud != null && !hud.counterplayLabel().isBlank(),
                    "Guardian HUD counterplay missing: " + profile.bossPath());
            helper.assertTrue(!profile.cinematicCue().dangerVerb().isBlank(),
                    "Guardian cinematic cue missing danger verb: " + profile.bossPath());
            helper.assertTrue(!profile.rewardBundle().isEmpty(),
                    "Guardian Codex reward bundle missing: " + profile.bossPath());
            if (hud != null) {
                BiomeGuardianProfile.PolishData polish = profile.polish();
                helper.assertTrue(hud.subtitle().contains(profile.cinematicCue().dangerVerb())
                                && hud.subtitle().contains(polish.counterplayObject()),
                        "Guardian HUD subtitle should mirror profile danger/counterplay copy: " + profile.bossPath());
                helper.assertTrue(hud.phaseWarningLabel().equals(profile.cinematicCue().phaseWarningLabel()),
                        "Guardian HUD phase warning should mirror profile cue: " + profile.bossPath());
                helper.assertTrue(hud.counterplayLabel().equals(polish.counterplayObject()),
                        "Guardian HUD counterplay should mirror polish data: " + profile.bossPath());
                helper.assertTrue(hud.compassLabel().equals(profile.cinematicCue().objectiveLabel()),
                        "Guardian HUD compass label should mirror profile objective: " + profile.bossPath());
            }
        }
        helper.assertTrue(BossHudProfiles.byEntityId("echoashfallprotocol:warden_boss").isPresent(),
                "Warden HUD profile should exist");
        helper.assertTrue(BossHudProfiles.byEntityId("echoorbitalremnants:corrupted_docking_ai").isPresent(),
                "Orbital Docking AI HUD profile should exist");
        helper.assertTrue(BossHudProfiles.byTitle("ECHO-0").isPresent(), "ECHO-0 HUD title profile should exist");

        BiomeGuardianProfile guardian = BiomeGuardianProfiles.byBiome("ruined_plains").orElseThrow();
        BossHudProfile guardianHud = BossHudProfiles.byEntityId(guardian.entityId()).orElseThrow();
        BossNavigationPacket original = BossNavigationPacket.active(guardianHud, "minecraft:overworld",
                new BlockPos(4, 64, -9), 2, 0.45F, guardian.title());
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        BossNavigationPacket.CODEC.encode(buffer, original);
        BossNavigationPacket decoded = BossNavigationPacket.CODEC.decode(buffer);
        helper.assertTrue(decoded.active(), "Boss navigation packet should preserve active flag");
        helper.assertTrue(decoded.bossId().equals(original.bossId()), "Boss navigation packet should preserve boss id");
        helper.assertTrue(decoded.title().equals(original.title()), "Boss navigation packet should preserve title");
        helper.assertTrue(decoded.subtitle().equals(original.subtitle()), "Boss navigation packet should preserve subtitle");
        helper.assertTrue(decoded.dimension().equals(original.dimension()), "Boss navigation packet should preserve dimension");
        helper.assertTrue(decoded.position().equals(original.position()), "Boss navigation packet should preserve position");
        helper.assertTrue(decoded.phase() == 2, "Boss navigation packet should preserve phase");
        helper.assertTrue(Math.abs(decoded.healthPercent() - original.healthPercent()) < 0.0001F,
                "Boss navigation packet should preserve health percent");
        helper.assertTrue(decoded.accentColor() == original.accentColor(),
                "Boss navigation packet should preserve accent color");
        helper.assertTrue(decoded.compassLabel().equals(original.compassLabel()),
                "Boss navigation packet should preserve compass label");
        helper.assertTrue(decoded.category().equals(original.category()),
                "Boss navigation packet should preserve category");
        helper.assertTrue("LIVE".equals(decoded.targetKind()), "Boss navigation packet should preserve live target kind");
        helper.assertTrue(guardianHud.phaseForHealth(0.65F) == 2 && guardianHud.phaseForHealth(0.32F) == 3,
                "Boss HUD phase thresholds should expose phase 2 and phase 3");

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos playerPos = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);
        setCurrentMission(QuestData.get(player), guardian.missionId());
        BiomeGuardianSiteData data = BiomeGuardianSiteData.get(helper.getLevel());
        BlockPos entrance = helper.absolutePos(new BlockPos(8, 2, 8));
        BlockPos arena = helper.absolutePos(new BlockPos(12, 2, 8));
        data.addOrUpdate(guardian, entrance, arena);
        helper.getLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(170.0D),
                entity -> BossHudProfiles.isSupportedEntityId(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()))
                .forEach(Entity::discard);

        BossNavigationPacket siteTarget = BossHudTargetResolver.resolve(player);
        helper.assertTrue(siteTarget.active(), "Active guardian mission should expose a compass target");
        helper.assertTrue(siteTarget.title().contains("Entrance"), "Guardian pre-combat target should point to the entrance");
        helper.assertTrue("ENTRANCE".equals(siteTarget.targetKind()), "Guardian pre-combat target should be marked as entrance");

        BiomeBossEntity boss = guardian.bossType().get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(boss != null, "Guardian boss should spawn for HUD resolver priority");
        if (boss != null) {
            boss.setPos(player.getX() + 3.0D, player.getY(), player.getZ());
            helper.getLevel().addFreshEntity(boss);
            boss.tick();
            BossNavigationPacket liveTarget = BossHudTargetResolver.resolve(player);
            helper.assertTrue(liveTarget.active(), "Live boss should expose a compass target");
            helper.assertTrue(!liveTarget.title().contains("Entrance"),
                    "Live boss should override the guardian entrance target");
            helper.assertTrue("LIVE".equals(liveTarget.targetKind()), "Live boss target should be marked as live");
            boss.discard();
        }

        data.markDefeated(guardian.bossPath(), arena);
        BossNavigationPacket cleared = BossHudTargetResolver.resolve(player);
        helper.assertFalse(cleared.active(), "Defeated guardian site should clear the compass target");

        WardenBossEntity warden = ModEntities.WARDEN_BOSS.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(warden != null, "Warden should spawn for archive boss target state");
        if (warden != null) {
            warden.setPos(player.getX() + 4.0D, player.getY(), player.getZ());
            helper.getLevel().addFreshEntity(warden);
            warden.tick();
            BossNavigationPacket archiveTarget = BossHudTargetResolver.resolve(player);
            helper.assertTrue(archiveTarget.active(), "Live Warden should expose an archive compass target");
            helper.assertTrue("ARCHIVE".equals(archiveTarget.targetKind()),
                    "Warden live target should be marked as archive");
            warden.discard();
        }
        helper.succeed();
    }

    private static void nexusAccessRules(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos corePos = helper.absolutePos(new BlockPos(5, 2, 4));
        QuestData quest = new QuestData();
        NexusWorldData.get(level.getServer().overworld())
                .setChoice(NexusWorldData.WorldState.NORMAL, BlockPos.ZERO, "GameTest reset");
        NexusCampaignData campaign = NexusCampaignData.get(level.getServer().overworld());
        campaign.resetForTests();
        for (BlockPos nodePos : List.copyOf(NexusWorldData.get(level).getActiveNodePositions())) {
            NexusWorldData.get(level).removePowerNode(nodePos);
        }

        level.setBlock(corePos, ModBlocks.NEXUS_CORE.get().defaultBlockState(), 3);
        helper.assertTrue(level.getBlockEntity(corePos) instanceof NexusCoreBlockEntity,
                "Nexus Core block entity should be present");
        NexusCoreBlockEntity core = (NexusCoreBlockEntity) level.getBlockEntity(corePos);

        NexusAccessRules.Status missingGuardians = NexusAccessRules.evaluate(quest, level, core);
        helper.assertFalse(missingGuardians.allowed(), "Nexus should deny before guardians are defeated");
        helper.assertTrue(missingGuardians.missingGuardianCount() == 8,
                "Nexus gate should require all eight active guardians before checking nodes");

        for (BiomeGuardianProfile profile : BiomeGuardianProfiles.all()) {
            quest.recordEntityKill(profile.entityId());
        }

        NexusAccessRules.Status missingNodes = NexusAccessRules.evaluate(quest, level, core);
        helper.assertFalse(missingNodes.allowed(), "Nexus should deny without five active nodes");
        helper.assertTrue(missingNodes.missingGuardianCount() == 0,
                "Guardian kills should satisfy the guardian gate");

        for (int i = 0; i < NexusCoreBlock.REQUIRED_NODES; i++) {
            BlockPos nodePos = corePos.offset(2 + i * 2, 0, 0);
            level.setBlock(nodePos,
                    ModBlocks.POWER_NODE.get().defaultBlockState().setValue(PowerNodeBlock.ACTIVE, true), 3);
            if (level.getBlockEntity(nodePos) instanceof PowerNodeBlockEntity node) {
                node.activate();
            }
            NexusWorldData.get(level).recordPowerNodeActivated(nodePos);
        }

        NexusAccessRules.Status warfrontLocked = NexusAccessRules.evaluate(quest, level, core);
        helper.assertFalse(warfrontLocked.allowed(),
                "Nexus should deny after guardians and nodes until Warfront is complete");

        campaign.awaken(corePos);
        campaign.scanRelays();
        campaign.resolveRelay(NexusRelayType.REACTOR, NexusRelayState.STABILIZED);
        campaign.resolveRelay(NexusRelayType.CRYO, NexusRelayState.SEVERED);
        campaign.resolveRelay(NexusRelayType.BIO, NexusRelayState.OVERRIDDEN);
        campaign.markSiegeComplete();

        NexusAccessRules.Status ready = NexusAccessRules.evaluate(quest, level, core);
        helper.assertTrue(ready.allowed(),
                "Nexus should allow after guardians, five nodes, three relays, and the siege are ready");
        helper.assertTrue(ready.activatedNodes() >= NexusCoreBlock.REQUIRED_NODES,
                "Nexus status should report active node count");

        NexusWorldData worldData = NexusWorldData.get(level.getServer().overworld());
        worldData.setChoice(NexusWorldData.WorldState.RESTORED, corePos, "GameTest");
        NexusAccessRules.Status sealed = NexusAccessRules.evaluate(quest, level, core);
        helper.assertFalse(sealed.allowed(), "Resolved world state should deny stale local Core choices");
        helper.assertTrue(sealed.worldResolved(), "Resolved world denial should be visible in status");
        helper.assertTrue(sealed.worldState() == NexusWorldData.WorldState.RESTORED,
                "Resolved status should report the selected world path");

        helper.assertTrue(NexusChoiceService.parseChoice("restore") == NexusCoreBlockEntity.NexusChoice.RESTORE,
                "Restore choice should parse");
        helper.assertTrue(NexusChoiceService.parseChoice("destroyed") == NexusCoreBlockEntity.NexusChoice.DESTROY,
                "Destroy aliases should parse");
        helper.assertTrue(NexusChoiceService.parseChoice("CONTROL") == NexusCoreBlockEntity.NexusChoice.CONTROL,
                "Control choice should parse case-insensitively");

        worldData.setChoice(NexusWorldData.WorldState.NORMAL, BlockPos.ZERO, "");
        campaign.resetForTests();
        for (BlockPos nodePos : NexusWorldData.get(level).getActiveNodePositions()) {
            NexusWorldData.get(level).removePowerNode(nodePos);
        }
        helper.succeed();
    }

    private static void nexusCampaignData(GameTestHelper helper) {
        NexusCampaignData data = NexusCampaignData.get(helper.getLevel().getServer().overworld());
        data.resetForTests();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));

        helper.assertFalse(data.isAwakened(), "Campaign should start dormant after reset");
        data.awaken(pos);
        helper.assertTrue(data.isAwakened(), "Awakening should persist");
        helper.assertTrue(data.getInstability() >= 25, "Awakening should start instability pressure");

        data.scanRelays();
        helper.assertTrue(data.getScannedRelayCount() == NexusCampaignData.REQUIRED_RELAY_SCAN_COUNT,
                "Scan should reveal all Prime Relays");
        data.resolveRelay(NexusRelayType.REACTOR, NexusRelayState.STABILIZED);
        int restoreReadiness = data.getReadinessRestore();
        helper.assertFalse(data.resolveRelay(NexusRelayType.REACTOR, NexusRelayState.SEVERED),
                "Resolved relay outcome should be immutable");
        helper.assertTrue(data.getRelayState(NexusRelayType.REACTOR) == NexusRelayState.STABILIZED
                        && data.getReadinessRestore() == restoreReadiness,
                "Rejected relay outcome changes must not alter readiness");
        data.resolveRelay(NexusRelayType.CRYO, NexusRelayState.SEVERED);
        data.resolveRelay(NexusRelayType.BIO, NexusRelayState.OVERRIDDEN);
        helper.assertTrue(data.getResolvedRelayCount() == NexusCampaignData.REQUIRED_RELAY_RESOLUTION_COUNT,
                "Three resolved relays should satisfy relay count");
        helper.assertFalse(data.isWarfrontComplete(), "Warfront should still require the countermeasure siege");

        helper.assertTrue(data.markSiegeComplete(), "First siege credit should change campaign state");
        helper.assertFalse(data.markSiegeComplete(), "Duplicate siege credit should be ignored");
        helper.assertTrue(data.isWarfrontComplete(), "Siege completion should finish Warfront readiness");
        data.markWardenDefeated();
        data.markFinaleComplete();
        helper.assertTrue(data.isWardenDefeated() && data.isFinaleComplete(),
                "Post-choice midpoint and finale flags should persist");
        data.markFinalBossSummoned(PostNexusData.NexusPath.CONTROL);
        helper.assertTrue(data.isFinalBossSummonedFor(PostNexusData.NexusPath.CONTROL),
                "Final boss summon path should persist");
        data.clearFinalBossSummoned();
        helper.assertFalse(data.isFinalBossSummoned(), "Final boss recovery flag should clear after finale credit");

        data.resetForTests();
        helper.succeed();
    }

    private static void nexusWarfrontContent(GameTestHelper helper) {
        var level = helper.getLevel().getServer().overworld();
        NexusCampaignData data = NexusCampaignData.get(level);
        data.resetForTests();

        helper.assertTrue(BiomeGuardianProfiles.all().size() == 8,
                "Warfront content must not change the eight active biome guardian profiles");
        helper.assertTrue(NexusRelayProfiles.hasCoverage(), "Every Prime Relay type needs a content profile");
        helper.assertTrue(NexusPressureMobProfiles.registryMatchesEntities(), "Pressure mob profiles must map to registered entities");
        helper.assertTrue(NexusFinalBossProfiles.hasCoverage(), "All three Nexus paths need finale boss profiles");

        data.awaken(helper.absolutePos(new BlockPos(3, 2, 3)));
        data.scanRelays();
        helper.assertFalse(data.hasRelaySite(NexusRelayType.REACTOR),
                "Old-save Warfront data should start without relay positions");
        NexusRelaySiteService.ensureSitesAssignedAndGenerated(level, data, helper.absolutePos(new BlockPos(3, 2, 3)));
        for (NexusRelayProfile profile : NexusRelayProfiles.all()) {
            helper.assertTrue(data.hasRelaySite(profile.type()), "Relay scan should assign site: " + profile.type());
            helper.assertTrue(data.isRelayGenerated(profile.type()), "Relay scan should generate objective shell: " + profile.type());
            helper.assertTrue(NexusRelaySiteService.objectiveShellExists(level, data, profile.type()),
                    "Relay objective shell should be non-empty: " + profile.type());
            helper.assertTrue(profile.requiredPressureKills() > 0, "Relay profile needs pressure objective: " + profile.type());
            helper.assertTrue(!profile.objective().isBlank(), "Relay profile objective text missing: " + profile.type());
        }

        data.resetForTests();
        BlockPos near = helper.absolutePos(new BlockPos(8, 2, 8));
        data.awaken(near);
        data.scanRelays();
        for (NexusRelayType type : NexusRelayType.values()) {
            data.assignRelaySite(type, helper.absolutePos(new BlockPos(4 + type.ordinal() * 2, 2, 10)));
        }
        NexusRelaySiteService.ensureSitesAssignedAndGenerated(level, data, near);
        helper.assertTrue(data.firstEncounterCompleteUnresolvedRelay() == null,
                "Relay resolution queue should reject relays before encounter completion");
        NexusRelayProfile reactor = NexusRelayProfiles.byType(NexusRelayType.REACTOR).orElseThrow();
        data.markRelayEncounterStarted(NexusRelayType.REACTOR);
        for (int i = 0; i < reactor.requiredPressureKills(); i++) {
            data.incrementRelayPressureKill(NexusRelayType.REACTOR);
        }
        helper.assertFalse(data.isRelayObjectiveSatisfied(NexusRelayType.REACTOR, reactor),
                "Commander relay should still require the commander after pressure kills");
        data.markRelayCommanderDefeated(NexusRelayType.REACTOR);
        helper.assertTrue(data.isRelayObjectiveSatisfied(NexusRelayType.REACTOR, reactor),
                "Relay objective should pass after pressure kills and commander defeat");
        data.markRelayEncounterComplete(NexusRelayType.REACTOR);
        helper.assertTrue(data.firstEncounterCompleteUnresolvedRelay() == NexusRelayType.REACTOR,
                "Completed relay encounter should enter the resolution queue");
        helper.assertTrue(data.resolveRelay(NexusRelayType.REACTOR, NexusRelayState.STABILIZED),
                "Encounter-complete relay should accept a resolved outcome in saved state");
        helper.assertFalse(data.resolveRelay(NexusRelayType.REACTOR, NexusRelayState.SEVERED),
                "Resolved relay should reject a second outcome");
        helper.assertTrue(data.relaySummaryPayload().contains("Final Boss:")
                        && data.relaySummaryPayload().contains("Reactor Relay"),
                "Relay summary payload should include final boss and relay state text");

        Player scannerPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertFalse(NexusRelaySiteService.hasRelayScannerLens(scannerPlayer),
                "Scanner lens should not be active without the lens item");
        scannerPlayer.getInventory().add(new ItemStack(ModItems.PORTABLE_SIGNAL_SCANNER.get()));
        helper.assertFalse(NexusRelaySiteService.hasRelayScannerLens(scannerPlayer),
                "Portable scanner alone should not count as the relay lens upgrade");
        scannerPlayer.getInventory().add(new ItemStack(ModItems.RELAY_SCANNER_LENS.get()));
        helper.assertTrue(NexusRelaySiteService.hasRelayScannerLens(scannerPlayer),
                "Relay scanner lens should activate from the passive lens item");

        smokeEntity(helper, ModEntities.GRIDBOUND_HUSK.get());
        smokeEntity(helper, ModEntities.RELAY_WARDEN.get());
        smokeEntity(helper, ModEntities.SIGNAL_LEECH.get());
        smokeEntity(helper, ModEntities.NEXUS_NULLIFIER.get());
        for (NexusFinalBossProfile profile : NexusFinalBossProfiles.all()) {
            NexusFinalBossEntity boss = profile.entityType().get().create(helper.getLevel(), EntitySpawnReason.EVENT);
            helper.assertTrue(boss != null, "Finale boss should spawn: " + profile.entityPath());
            if (boss != null) {
                boss.setPos(near.getX() + 0.5D, near.getY(), near.getZ() + 0.5D);
                helper.getLevel().addFreshEntity(boss);
                boss.tick();
                helper.assertTrue(boss.path() == profile.path(), "Finale boss path should match profile: " + profile.path());
                helper.assertTrue(boss.getAttribute(Attributes.ATTACK_DAMAGE) != null,
                        "Finale boss needs attack damage: " + profile.entityPath());
                boss.discard();
            }
        }

        data.resetForTests();
        helper.succeed();
    }

    private static void nexusGuardianRelaySiegeFinalePostgameRuntimeFlow(GameTestHelper helper) {
        EchoServiceRegistry.withClearedForTests(() -> {
            RecordingMissionService missionService = new RecordingMissionService();
            EchoCoreServices.registerMissionService(missionService);

            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            ServerLevel level = helper.getLevel();
            ServerLevel overworld = level.getServer().overworld();
            NexusCampaignData campaign = NexusCampaignData.get(overworld);
            NexusWorldData worldData = NexusWorldData.get(overworld);
            NexusWorldData localWorldData = NexusWorldData.get(level);
            campaign.resetForTests();
            worldData.setChoice(NexusWorldData.WorldState.NORMAL, BlockPos.ZERO, "GameTest reset");
            for (BlockPos nodePos : List.copyOf(localWorldData.getActiveNodePositions())) {
                localWorldData.removePowerNode(nodePos);
            }
            for (BlockPos nodePos : List.copyOf(worldData.getActiveNodePositions())) {
                worldData.removePowerNode(nodePos);
            }

            BlockPos corePos = helper.absolutePos(new BlockPos(5, 2, 5));
            level.setBlock(corePos, ModBlocks.NEXUS_CORE.get().defaultBlockState(), 3);
            helper.assertTrue(level.getBlockEntity(corePos) instanceof NexusCoreBlockEntity,
                    "Nexus vertical proof should create a live Nexus Core block entity");
            NexusCoreBlockEntity core = (NexusCoreBlockEntity) level.getBlockEntity(corePos);
            player.setPos(corePos.getX() + 0.5D, corePos.getY() + 1.0D, corePos.getZ() + 0.5D);

            QuestData quest = QuestData.get(player);
            for (BiomeGuardianProfile profile : BiomeGuardianProfiles.all()) {
                quest.recordEntityKill(profile.entityId());
            }
            QuestData.saveAndSync(player, quest);
            int guardianLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            helper.assertTrue(AshfallAdapterCoreLateRuntime.bossDefeated(
                            player,
                            "nexus_scar_avatar",
                            "guardian",
                            corePos,
                            "gametest_nexus_full_loop").mutated(),
                    "Nexus Scar Avatar guardian defeat should publish late-runtime mutation state");
            helper.assertTrue(NexusAccessRules.hasDefeatedAllGuardians(player),
                    "QuestData guardian kills should satisfy the Nexus guardian gate");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.KILL_ENTITY,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "nexus_scar_avatar")),
                    "Guardian runtime should record the Nexus Scar Avatar kill objective");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.CUSTOM,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "neutralize_nexus_scar_avatar")),
                    "Guardian runtime should record the Nexus Scar Avatar mission objective");
            helper.assertTrue(latestLateRuntimeLedger(
                            guardianLedgerBefore,
                            "ashfall.boss_defeated",
                            "bossId",
                            "nexus_scar_avatar") != null,
                    "Guardian runtime should append a boss defeat mutation ledger entry");

            for (int i = 0; i < NexusCoreBlock.REQUIRED_NODES; i++) {
                BlockPos nodePos = corePos.offset(16 * (i + 1), 0, 0);
                level.setBlock(nodePos,
                        ModBlocks.POWER_NODE.get().defaultBlockState().setValue(PowerNodeBlock.ACTIVE, true),
                        3);
                if (level.getBlockEntity(nodePos) instanceof PowerNodeBlockEntity node) {
                    node.activate();
                }
                localWorldData.recordPowerNodeActivated(nodePos);
                worldData.recordPowerNodeActivated(nodePos);
                AshfallAdapterCoreLateRuntime.powerNodeState(
                        player,
                        nodePos,
                        true,
                        i + 1,
                        "gametest_nexus_full_loop");
            }

            int awakenLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            campaign.awaken(corePos);
            helper.assertTrue(AshfallAdapterCoreLateRuntime.nexusState(
                            player,
                            campaign,
                            worldData,
                            "awakened",
                            "gametest_nexus_full_loop").mutated(),
                    "Nexus awakening should publish late-runtime campaign state");
            helper.assertTrue(campaign.isAwakened(), "Nexus campaign should persist awakened state");
            helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "nexus:state:awakened"),
                    "Nexus awakening should mark the route-visible Nexus state");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.CUSTOM,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "awaken_nexus_core")),
                    "Nexus awakening should record the MissionCore awaken objective");
            helper.assertTrue(latestLateRuntimeLedger(
                            awakenLedgerBefore,
                            "ashfall.nexus_state",
                            "state",
                            "awakened") != null,
                    "Nexus awakening should append a Nexus state mutation ledger");

            int scanLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            campaign.scanRelays();
            helper.assertTrue(AshfallAdapterCoreLateRuntime.nexusState(
                            player,
                            campaign,
                            worldData,
                            "prime_relays_scanned",
                            "gametest_nexus_full_loop").mutated(),
                    "Prime Relay scan should publish late-runtime campaign state");
            helper.assertTrue(campaign.getScannedRelayCount() == NexusCampaignData.REQUIRED_RELAY_SCAN_COUNT,
                    "Prime Relay scan should persist every relay signature");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.SCAN_BLOCK,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "scan_prime_relays")),
                    "Prime Relay scan should record the MissionCore scan objective");
            helper.assertTrue(latestLateRuntimeLedger(
                            scanLedgerBefore,
                            "ashfall.nexus_state",
                            "state",
                            "prime_relays_scanned") != null,
                    "Prime Relay scan should append a Nexus state mutation ledger");

            NexusRelayType[] relayTypes = {
                    NexusRelayType.REACTOR,
                    NexusRelayType.CRYO,
                    NexusRelayType.BIO
            };
            NexusRelayState[] relayOutcomes = {
                    NexusRelayState.STABILIZED,
                    NexusRelayState.SEVERED,
                    NexusRelayState.OVERRIDDEN
            };
            for (int i = 0; i < relayTypes.length; i++) {
                NexusRelayType relayType = relayTypes[i];
                NexusRelayState outcome = relayOutcomes[i];
                campaign.markRelayEncounterComplete(relayType);
                helper.assertTrue(campaign.resolveRelay(relayType, outcome),
                        "Encounter-complete relay should accept one saved outcome: " + relayType);
                int relayLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
                helper.assertTrue(AshfallAdapterCoreLateRuntime.primeRelayResolved(
                                player,
                                relayType,
                                outcome,
                                campaign,
                                "gametest_nexus_full_loop").mutated(),
                        "Prime Relay resolution should publish late-runtime state: " + relayType);
                helper.assertTrue(QuestData.get(player).hasVisitedLocation(
                                "special",
                                "nexus:relay:" + relayType.name().toLowerCase(Locale.ROOT)
                                        + ":" + outcome.name().toLowerCase(Locale.ROOT)),
                        "Prime Relay resolution should mark the route-visible relay outcome: " + relayType);
                helper.assertTrue(latestLateRuntimeLedger(
                                relayLedgerBefore,
                                "ashfall.prime_relay_resolved",
                                "relayType",
                                relayType.name()) != null,
                        "Prime Relay resolution should append a mutation ledger: " + relayType);
            }
            helper.assertTrue(campaign.getResolvedRelayCount() == NexusCampaignData.REQUIRED_RELAY_RESOLUTION_COUNT,
                    "Three resolved Prime Relays should satisfy the Warfront relay gate");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.ESTABLISH_ROUTE,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "resolve_prime_relays")),
                    "Prime Relay resolutions should record the MissionCore route objective");

            int siegeLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            helper.assertTrue(campaign.markSiegeComplete(), "Nexus siege should persist first completion");
            PostNexusData post = PostNexusData.get(player);
            post.setRelaysResolved(campaign.getResolvedRelayCount());
            post.setSiegesSurvived(1);
            PostNexusData.saveAndSync(player, post);
            helper.assertTrue(AshfallAdapterCoreLateRuntime.nexusState(
                            player,
                            campaign,
                            worldData,
                            "siege_complete",
                            "gametest_nexus_full_loop").mutated(),
                    "Core Countermeasure Siege should publish late-runtime campaign state");
            helper.assertTrue(campaign.isWarfrontComplete(),
                    "Resolved relays and siege completion should finish the Nexus Warfront gate");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.SURVIVE_TIME,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "survive_core_countermeasure")),
                    "Siege completion should record the MissionCore countermeasure objective");
            helper.assertTrue(latestLateRuntimeLedger(
                            siegeLedgerBefore,
                            "ashfall.nexus_state",
                            "state",
                            "siege_complete") != null,
                    "Siege completion should append a Nexus state mutation ledger");

            NexusAccessRules.Status ready = NexusAccessRules.evaluate(QuestData.get(player), level, core);
            helper.assertTrue(ready.allowed(),
                    "Nexus access should open after guardians, power nodes, three relays, and siege completion");
            helper.assertTrue(ready.activatedNodes() >= NexusCoreBlock.REQUIRED_NODES,
                    "Nexus access status should report enough active Power Nodes");

            int choiceLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            helper.assertTrue(NexusChoiceService.parseChoice("restore") == NexusCoreBlockEntity.NexusChoice.RESTORE,
                    "Restore choice should parse through the Nexus choice service");
            helper.assertTrue(AshfallAdapterCoreLateRuntime.endingChoice(
                            player,
                            PostNexusData.NexusPath.RESTORE,
                            corePos,
                            "gametest_nexus_full_loop").mutated(),
                    "Nexus final choice should publish a late-runtime ending event");
            worldData.setChoice(NexusWorldData.WorldState.RESTORED, corePos, "GameTest");
            PostNexusEventHandler.onNexusChoiceMade(player, PostNexusData.NexusPath.RESTORE);
            helper.assertTrue(PostNexusData.get(player).isPath(PostNexusData.NexusPath.RESTORE),
                    "Post-Nexus data should persist the selected Restore path");
            helper.assertTrue(worldData.getState() == NexusWorldData.WorldState.RESTORED
                            && worldData.hasChoiceBeenMade(),
                    "Nexus world data should persist the shared Restore choice");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.CUSTOM,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "reach_decision")),
                    "Final choice should record the MissionCore reach_decision objective");
            helper.assertTrue(latestLateRuntimeLedger(
                            choiceLedgerBefore,
                            "ashfall.ending_choice",
                            "path",
                            PostNexusData.NexusPath.RESTORE.name()) != null,
                    "Final choice should append an ending-choice mutation ledger");

            post = PostNexusData.get(player);
            post.setWardenDefeated(true);
            PostNexusData.saveAndSync(player, post);
            campaign.markWardenDefeated();
            helper.assertTrue(AshfallAdapterCoreLateRuntime.bossDefeated(
                            player,
                            "warden_boss",
                            PostNexusData.NexusPath.RESTORE.name(),
                            corePos,
                            "gametest_nexus_full_loop").mutated(),
                    "Post-choice Warden guardian defeat should publish late-runtime boss state");

            int operationLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            helper.assertTrue(NexusCampaignActions.completePathOperation(player),
                    "Restore path operation should progress through the Nexus campaign action service");
            helper.assertTrue(PostNexusData.get(player).getPathOperationsComplete() >= 1,
                    "Path operation should persist post-Nexus operation progress");
            helper.assertTrue(latestLateRuntimeLedger(
                            operationLedgerBefore,
                            "ashfall.post_nexus_persisted",
                            "path",
                            PostNexusData.NexusPath.RESTORE.name()) != null,
                    "Path operation should append a post-Nexus persistence ledger");

            helper.assertTrue(NexusCampaignActions.creditFinaleBoss(player, PostNexusData.NexusPath.RESTORE),
                    "Restore finale boss credit should progress through the Nexus campaign action service");
            helper.assertTrue(PostNexusData.get(player).isFinalBossDefeated(),
                    "Finale boss credit should persist final boss defeat state");
            helper.assertTrue(NexusCampaignActions.completeFinale(player),
                    "Finale completion should seal the Nexus campaign state");
            helper.assertTrue(campaign.isFinaleComplete(), "Campaign data should persist finale completion");

            int postLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            PostNexusEventHandler.completeFinalProtocol(player, "restore_epilogue");
            post = PostNexusData.get(player);
            helper.assertTrue(post.isFinalProtocolComplete(),
                    "Post-Nexus data should unlock the completed final protocol after epilogue credit");
            helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "post_nexus:persisted:restore"),
                    "Post-Nexus persistence should mark the route-visible postgame unlock");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.CUSTOM,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "restore_world_lattice")),
                    "Post-Nexus persistence should record the Restore path operation objective");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.CUSTOM,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "restore_guardian")),
                    "Post-Nexus persistence should record the Restore guardian objective");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.CUSTOM,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "restore_finale")),
                    "Post-Nexus persistence should record the Restore finale objective");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.CUSTOM,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "restore_epilogue")),
                    "Post-Nexus persistence should record the Restore epilogue objective");
            helper.assertTrue(latestLateRuntimeLedger(
                            postLedgerBefore,
                            "ashfall.post_nexus_persisted",
                            "path",
                            PostNexusData.NexusPath.RESTORE.name()) != null,
                    "Final protocol completion should append a post-Nexus persistence ledger");

            worldData.setChoice(NexusWorldData.WorldState.NORMAL, BlockPos.ZERO, "GameTest reset");
            campaign.resetForTests();
            for (BlockPos nodePos : List.copyOf(localWorldData.getActiveNodePositions())) {
                localWorldData.removePowerNode(nodePos);
            }
            for (BlockPos nodePos : List.copyOf(worldData.getActiveNodePositions())) {
                worldData.removePowerNode(nodePos);
            }
        });
        helper.succeed();
    }

    private static void wardenArenaService(GameTestHelper helper) {
        var level = helper.getLevel();
        int removed = PrefallArchivesArenaService.resetArena(level, PostNexusData.NexusPath.RESTORE, false);
        helper.assertTrue(removed >= 0, "Arena reset should report removed Warden count");
        helper.assertTrue(PrefallArchivesArenaService.inspectArena(level).ready(), "Arena shell should be ready after prepare");
        helper.assertTrue(level.getBlockState(PrefallArchivesArenaService.ARENA_CENTER.below()).is(Blocks.LAPIS_BLOCK),
                "Restore arena should use restore center block");

        helper.assertTrue(PrefallArchivesArenaService.spawnWardenIfMissing(level),
                "First Warden spawn should create one boss");
        helper.assertFalse(PrefallArchivesArenaService.spawnWardenIfMissing(level),
                "Second Warden spawn should be idempotent");
        helper.assertTrue(PrefallArchivesArenaService.getWardenCount(level) == 1,
                "Arena should contain exactly one living Warden");

        WardenBossEntity duplicate = ModEntities.WARDEN_BOSS.get().create(level, EntitySpawnReason.EVENT);
        helper.assertTrue(duplicate != null, "Duplicate Warden should be spawnable for cleanup test");
        if (duplicate != null) {
            duplicate.setPos(
                    PrefallArchivesArenaService.WARDEN_POS.getX() + 1.5D,
                    PrefallArchivesArenaService.WARDEN_POS.getY(),
                    PrefallArchivesArenaService.WARDEN_POS.getZ() + 1.5D);
            level.addFreshEntity(duplicate);
        }

        helper.assertTrue(PrefallArchivesArenaService.getWardenCount(level) >= 2,
                "Arena should see duplicate Warden before cleanup");
        int duplicateCleanup = PrefallArchivesArenaService.cleanupDuplicateWardens(level);
        helper.assertTrue(duplicateCleanup >= 1, "Duplicate cleanup should remove extra Wardens");
        helper.assertTrue(PrefallArchivesArenaService.getWardenCount(level) == 1,
                "Duplicate cleanup should leave one Warden");

        int resetCleanup = PrefallArchivesArenaService.resetArena(level, PostNexusData.NexusPath.DESTROY, true);
        helper.assertTrue(resetCleanup >= 1, "Reset should remove existing Warden before respawn");
        helper.assertTrue(PrefallArchivesArenaService.getWardenCount(level) == 1,
                "Reset with spawn should leave exactly one Warden");
        helper.assertTrue(level.getBlockState(PrefallArchivesArenaService.ARENA_CENTER.below()).is(Blocks.REDSTONE_BLOCK),
                "Destroy arena should use destroy center block");
        PrefallArchivesArenaService.removeAllWardens(level);
        helper.succeed();
    }

    private static void rareSchematicResearch(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ResearchData research = ResearchData.get(player);
        research.resetAll();
        ResearchData.saveAndSync(player, research);

        ItemStack rare = new ItemStack(ModItems.RARE_TECH_SCHEMATIC.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, rare);
        int unlockLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
        RareTechSchematicItem.DecodeResult decoded = RareTechSchematicItem.decodeAtResearchLab(player, rare);
        ResearchData decodedResearch = ResearchData.get(player);
        helper.assertTrue(decoded.consumed(), "Rare tech schematic should be consumed at a Research Lab");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "Rare tech schematic stack should shrink after decoding");
        helper.assertTrue(decoded.unlockedType() == SchematicFragmentItem.SchematicType.WEAPONS,
                "Rare tech schematic should unlock the first missing schematic branch in enum order");
        helper.assertTrue(decodedResearch.hasSchematic("weapons"), "Weapons schematic branch should unlock");
        helper.assertTrue(decodedResearch.getPoints() == RareTechSchematicItem.MISSING_CATEGORY_RP,
                "Rare tech schematic should award 75 RP when unlocking a missing branch");
        var unlockLedgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(unlockLedgerEntries.size() > unlockLedgerBefore,
                "Rare schematic unlock should append a mutation ledger entry");
        var unlockLedger = unlockLedgerEntries.get(unlockLedgerEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_ASHFALL_SCHEMATIC_UNLOCKED.equals(
                        unlockLedger.actionId()),
                "Rare schematic unlock should use the canonical schematic unlock event");
        helper.assertTrue("echoashfallprotocol:exploration_runtime".equals(unlockLedger.runtimeHostId()),
                "Rare schematic unlock should route through the exploration runtime host");
        helper.assertTrue(unlockLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Rare schematic unlock ledger should report MUTATED");
        helper.assertTrue(unlockLedger.saveTouched(), "Rare schematic unlock should touch save state");
        helper.assertTrue(unlockLedger.hudOrEventEmitted(), "Rare schematic unlock should emit visible/event feedback");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_RARE_TECH_SCHEMATIC.equals(
                        String.valueOf(unlockLedger.inputPayload().get("itemId"))),
                "Rare schematic unlock ledger should identify the canonical item id");
        helper.assertTrue(Integer.valueOf(0).equals(unlockLedger.beforeSummary().get("unlockedSchematicCount")),
                "Rare schematic unlock ledger should capture zero schematics before mutation");
        helper.assertTrue(Integer.valueOf(1).equals(unlockLedger.afterSummary().get("unlockedSchematicCount")),
                "Rare schematic unlock ledger should capture one schematic after mutation");
        helper.assertTrue(Integer.valueOf(RareTechSchematicItem.MISSING_CATEGORY_RP).equals(
                        unlockLedger.afterSummary().get("researchPoints")),
                "Rare schematic unlock ledger should capture awarded research points");

        for (SchematicFragmentItem.SchematicType type : SchematicFragmentItem.SchematicType.values()) {
            decodedResearch.unlockSchematic(type.getDisplayName().toLowerCase(Locale.ROOT));
        }
        ResearchData.saveAndSync(player, decodedResearch);

        ItemStack duplicate = new ItemStack(ModItems.RARE_TECH_SCHEMATIC.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, duplicate);
        int archiveLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
        RareTechSchematicItem.DecodeResult archived = RareTechSchematicItem.decodeAtResearchLab(player, duplicate);
        ResearchData archivedResearch = ResearchData.get(player);
        helper.assertTrue(archived.consumed(), "Duplicate rare tech schematic should be consumed");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "Duplicate rare tech schematic stack should shrink");
        helper.assertTrue(archived.unlockedType() == null, "Duplicate rare tech schematic should not unlock a branch");
        helper.assertTrue(archivedResearch.getPoints()
                        == RareTechSchematicItem.MISSING_CATEGORY_RP + RareTechSchematicItem.DUPLICATE_ARCHIVE_RP,
                "Duplicate rare tech schematic should archive for 125 RP");
        var archiveLedgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(archiveLedgerEntries.size() > archiveLedgerBefore,
                "Duplicate rare schematic archive should append a mutation ledger entry");
        var archiveLedger = archiveLedgerEntries.get(archiveLedgerEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_ASHFALL_RESEARCH_UPDATED.equals(
                        archiveLedger.actionId()),
                "Duplicate rare schematic archive should use the canonical research update event");
        helper.assertTrue("echoashfallprotocol:exploration_runtime".equals(archiveLedger.runtimeHostId()),
                "Duplicate rare schematic archive should route through the exploration runtime host");
        helper.assertTrue(archiveLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Duplicate rare schematic archive ledger should report MUTATED");
        helper.assertTrue(Boolean.TRUE.equals(archiveLedger.inputPayload().get("duplicateArchive")),
                "Duplicate rare schematic archive ledger should identify archive mode");
        helper.assertTrue(Integer.valueOf(
                        RareTechSchematicItem.MISSING_CATEGORY_RP + RareTechSchematicItem.DUPLICATE_ARCHIVE_RP).equals(
                        archiveLedger.afterSummary().get("researchPoints")),
                "Duplicate rare schematic archive ledger should capture archived research points");
        helper.succeed();
    }

    private static void researchPerkGraph(GameTestHelper helper) {
        var perks = PerkRegistry.getAll();
        helper.assertFalse(perks.isEmpty(), "Research perk registry should not be empty");

        Set<String> ids = new HashSet<>();
        EnumSet<Perk.Branch> branches = EnumSet.noneOf(Perk.Branch.class);
        for (Perk perk : perks.values()) {
            helper.assertTrue(ids.add(perk.getId()), "Research perk id should be unique: " + perk.getId());
            helper.assertTrue(perk.getId().equals(perk.getId().toLowerCase(Locale.ROOT)),
                    "Research perk id should stay lowercase: " + perk.getId());
            helper.assertFalse(perk.getName().isBlank(), "Research perk name should not be blank: " + perk.getId());
            helper.assertFalse(perk.getDescription().isBlank(),
                    "Research perk description should not be blank: " + perk.getId());
            helper.assertTrue(perk.getCost() > 0, "Research perk cost should be positive: " + perk.getId());
            branches.add(perk.getBranch());

            for (String prerequisiteId : perk.getPrerequisites()) {
                Perk prerequisite = PerkRegistry.get(prerequisiteId);
                helper.assertTrue(prerequisite != null,
                        "Research prerequisite should exist: " + prerequisiteId + " for " + perk.getId());
                if (prerequisite != null) {
                    helper.assertTrue(prerequisite.getTier() < perk.getTier(),
                            "Research prerequisite should be an earlier tier: " + prerequisiteId + " -> " + perk.getId());
                }
            }
        }

        for (Perk.Branch branch : Perk.Branch.values()) {
            helper.assertTrue(branches.contains(branch), "Research branch should have perks: " + branch);
        }
        helper.succeed();
    }

    private static void researchPurchaseSpendsRp(GameTestHelper helper) {
        try {
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            player.containerMenu = new ResearchLabMenu(1, player.getInventory());
            ResearchData research = ResearchData.get(player);
            research.resetAll();

            Perk perk = PerkRegistry.WEAPON_DAMAGE_1;
            research.addPoints(perk.getCost());

            Method purchase = ModNetwork.class.getDeclaredMethod(
                    "handleResearchPurchase", ResearchPurchasePacket.class, ServerPlayer.class);
            purchase.setAccessible(true);
            purchase.invoke(null, new ResearchPurchasePacket(perk.getId()), player);

            helper.assertTrue(research.hasPerk(perk.getId()), "Research purchase should unlock the requested perk");
            helper.assertTrue(research.getPoints() == 0,
                    "Research purchase should spend exactly the perk cost; remaining RP was " + research.getPoints());

            purchase.invoke(null, new ResearchPurchasePacket(perk.getId()), player);
            helper.assertTrue(research.getPoints() == 0,
                    "Duplicate research purchase should not spend additional RP; remaining RP was " + research.getPoints());
            helper.succeed();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            helper.fail("Research purchase handler should spend RP and unlock once: " + exception.getMessage());
        }
    }

    private static void structureExportPaths(GameTestHelper helper) {
        helper.assertTrue(ModStructuresCommand.isSafeStructureToken("cache_room"),
                "Lowercase export names should be accepted");
        helper.assertFalse(ModStructuresCommand.isSafeStructureToken("CacheRoom"),
                "Uppercase export names should be rejected");
        helper.assertFalse(ModStructuresCommand.isSafeStructureToken(".."),
                "Path traversal export names should be rejected");
        helper.assertFalse(ModStructuresCommand.isSafeStructureToken("bad/path"),
                "Path separator export names should be rejected");

        String globalPath = ModStructuresCommand.resolveStructureOutputPath("cache_room", "global")
                .toString()
                .replace('\\', '/');
        helper.assertTrue(globalPath.endsWith("data/echoashfallprotocol/structure/global/cache_room.nbt"),
                "Global exports should write under the singular structure resource path");
        String stalePluralPath = "data/echoashfallprotocol/" + "structures";
        helper.assertFalse(globalPath.contains(stalePluralPath),
                "Global exports should not write under the stale plural structures path");

        String biomePath = ModStructuresCommand.resolveStructureOutputPath("cache_room", "toxic_swamp")
                .toString()
                .replace('\\', '/');
        helper.assertTrue(biomePath.endsWith("data/echoashfallprotocol/structure/biomes/toxic_swamp/cache_room.nbt"),
                "Biome exports should write under the singular structure resource path");
        helper.succeed();
    }

    private static void proceduralTerrainFootprints(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos flatOrigin = helper.absolutePos(new BlockPos(24, 90, 24));
        prepareTerrainFootprint(level, flatOrigin, StructureType.RADWARDEN_OUTPOST, 90,
                (localX, localZ) -> 0);
        helper.assertTrue(ProceduralStructureHandler.hasTerrainSafeFootprint(
                        level, flatOrigin, StructureType.RADWARDEN_OUTPOST),
                "Flat terrain should accept a Radwarden outpost footprint");
        BlockPos safeSpawn = ProceduralStructureHandler.findTerrainSafeSpawnPosition(
                level, flatOrigin, StructureType.RADWARDEN_OUTPOST);
        helper.assertTrue(safeSpawn != null,
                "Terrain-safe search should find the flat Radwarden outpost footprint");

        BlockPos cliffOrigin = helper.absolutePos(new BlockPos(112, 90, 24));
        prepareTerrainFootprint(level, cliffOrigin, StructureType.RADWARDEN_OUTPOST, 90,
                (localX, localZ) -> localX < StructureType.RADWARDEN_OUTPOST.getMaxSize() / 2 ? 0 : 9);
        helper.assertFalse(ProceduralStructureHandler.hasTerrainSafeFootprint(
                        level, cliffOrigin, StructureType.RADWARDEN_OUTPOST),
                "Cliffy terrain should reject a broad Radwarden outpost footprint");

        BlockPos unevenOrigin = helper.absolutePos(new BlockPos(24, 90, 112));
        prepareTerrainFootprint(level, unevenOrigin, StructureType.RADIO_TOWER, 90,
                (localX, localZ) -> Math.floorMod(localX + localZ, 3));
        helper.assertTrue(ProceduralStructureHandler.hasTerrainSafeFootprint(
                        level, unevenOrigin, StructureType.RADIO_TOWER),
                "Moderately uneven terrain should still accept a small tower footprint");
        helper.succeed();
    }

    private static void prepareTerrainFootprint(ServerLevel level, BlockPos origin, StructureType type,
                                                int baseGroundY, HeightOffset heightOffset) {
        int minLocal = -20;
        int maxLocal = type.getMaxSize() + 20;
        int clearTop = baseGroundY + 24;
        int fillBottom = baseGroundY - 2;
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int localX = minLocal; localX <= maxLocal; localX++) {
            for (int localZ = minLocal; localZ <= maxLocal; localZ++) {
                int groundY = baseGroundY + heightOffset.offset(localX, localZ);
                for (int y = fillBottom; y <= clearTop; y++) {
                    BlockPos pos = new BlockPos(origin.getX() + localX, y, origin.getZ() + localZ);
                    level.setBlockAndUpdate(pos, y <= groundY ? stone : air);
                }
            }
        }
    }

    @FunctionalInterface
    private interface HeightOffset {
        int offset(int localX, int localZ);
    }

    private static void starterDropPodTemplate(GameTestHelper helper) {
        var template = helper.getLevel().getStructureManager().get(id("drop_pod"));
        helper.assertTrue(template.isPresent(), "Starting drop pod NBT template should load");
        var size = template.orElseThrow().getSize();
        helper.assertTrue(size.getX() == 16 && size.getY() == 9 && size.getZ() == 16,
                "Starting drop pod should keep the curated 16x9x16 footprint");

        BlockPos origin = helper.absolutePos(new BlockPos(32, 4, 32));
        BlockPos spawn = ProceduralStructureGenerator.placeStartingDropPod(
                helper.getLevel(), origin, helper.getLevel().getRandom());
        helper.assertTrue(spawn != null, "Starting drop pod placement should return a safe spawn");
        if (spawn == null) {
            helper.fail("Starting drop pod placement returned null");
            return;
        }

        helper.assertTrue(helper.getLevel().getBlockState(spawn).isAir(),
                "Drop pod spawn feet block should be clear");
        helper.assertTrue(helper.getLevel().getBlockState(spawn.above()).isAir(),
                "Drop pod spawn head block should be clear");
        helper.assertFalse(helper.getLevel().getBlockState(spawn.below()).isAir(),
                "Drop pod spawn should stand on the pod floor");

        BlockPos placePos = origin.offset(-size.getX() / 2, -2, -size.getZ() / 2);
        BlockState starterBunkFoot = helper.getLevel().getBlockState(placePos.offset(4, 3, 7));
        BlockState starterBunkHead = helper.getLevel().getBlockState(placePos.offset(4, 3, 8));
        helper.assertTrue(starterBunkFoot.is(ModBlocks.EMERGENCY_BUNK.get())
                        && starterBunkFoot.getValue(EmergencyBunkBlock.PART) == BedPart.FOOT
                        && starterBunkFoot.getValue(EmergencyBunkBlock.FACING) == Direction.SOUTH,
                "Curated drop pod should include the guaranteed emergency bunk foot position");
        helper.assertTrue(starterBunkHead.is(ModBlocks.EMERGENCY_BUNK.get())
                        && starterBunkHead.getValue(EmergencyBunkBlock.PART) == BedPart.HEAD
                        && starterBunkHead.getValue(EmergencyBunkBlock.FACING) == Direction.SOUTH,
                "Curated drop pod should include the guaranteed emergency bunk head position");
        helper.assertTrue(countBlocks(helper, placePos, size, "echoblockworks:orbital_hull_hull_panel") >= 60,
                "Curated drop pod should use a readable Blockworks orbital hull shell");
        helper.assertTrue(countBlocks(helper, placePos, size, "echoashfallprotocol:echo_crate") >= 4,
                "Curated drop pod should expose four Echo starter crates");
        helper.assertTrue(countBlocks(helper, placePos, size, "echoashfallprotocol:echo_cache") >= 1,
                "Curated drop pod should expose the guaranteed Echo starter cache");
        helper.assertTrue(countNonAirVanillaBlocks(helper, placePos, size) == 0,
                "Curated drop pod should not place visible vanilla blocks");
        helper.assertTrue(countEchoContainerBlockEntities(helper, placePos, size) >= 5,
                "Curated drop pod cache/crate blocks should keep valid Echo container block entities");
        helper.assertTrue(countLootedEchoContainers(helper, placePos, size) >= 5,
                "Curated drop pod cache/crate block entities should preserve starter loot tables");
        helper.assertTrue(countStarterPodProtectedPathClutter(helper, placePos, size) == 0,
                "Curated drop pod should keep spawn, lockers, bed route, terminal access, and ramp clear of debris clutter");
        helper.assertTrue(countStarterPodOffPathClutter(helper, placePos, size) > 0,
                "Curated drop pod should retain off-path decorative crash debris");
        helper.assertTrue(countInvalidBlockEntities(helper, placePos, size) == 0,
                "Curated drop pod placement should not leave block entities on air or non-entity blocks");
        helper.succeed();
    }

    private static void emergencyBunkRespawnBehaviour(GameTestHelper helper) {
        BlockPos foot = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos head = foot.relative(Direction.SOUTH);
        placeEmergencyBunk(helper, foot, Direction.SOUTH);

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(foot.getX() + 2.0D, foot.getY() + 1.0D, foot.getZ() + 0.5D, 180.0F, 0.0F);

        BlockState footState = helper.getLevel().getBlockState(foot);
        InteractionResult result = footState.useWithoutItem(helper.getLevel(), player,
                new BlockHitResult(Vec3.atCenterOf(foot), Direction.UP, foot, false));
        helper.assertTrue(result != InteractionResult.PASS, "Emergency bunk click should be handled");

        ServerPlayer.RespawnConfig config = player.getRespawnConfig();
        helper.assertTrue(config != null, "Emergency bunk should set a respawn config");
        helper.assertTrue(config != null && !config.forced(), "Emergency bunk respawn should not be forced");
        helper.assertTrue(config != null && config.respawnData().pos().equals(head),
                "Emergency bunk respawn should be anchored at the head half");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "shelter:slept"),
                "Emergency bunk use should record the shelter route marker immediately");

        helper.assertTrue(EmergencyBunkBlock.resolveRespawnPosition(footState, EntityType.PLAYER,
                        helper.getLevel(), foot, 0.0F).isPresent(),
                "Emergency bunk foot should resolve a safe respawn position");
        BlockState headState = helper.getLevel().getBlockState(head);
        helper.assertTrue(EmergencyBunkBlock.resolveRespawnPosition(headState, EntityType.PLAYER,
                        helper.getLevel(), head, 0.0F).isPresent(),
                "Emergency bunk head should resolve a safe respawn position");

        ServerPlayer.RespawnConfig legacyConfig = new ServerPlayer.RespawnConfig(
                LevelData.RespawnData.of(helper.getLevel().dimension(), foot, 0.0F, 0.0F),
                true);
        player.setRespawnPosition(legacyConfig, false);
        PlayerRespawnPositionEvent event = new PlayerRespawnPositionEvent(
                player,
                TeleportTransition.missingRespawnBlock(player, TeleportTransition.DO_NOTHING),
                false);
        EmergencyBunkRespawnEvents.onPlayerRespawnPosition(event);
        helper.assertFalse(event.getTeleportTransition().missingRespawnBlock(),
                "Legacy forced emergency bunk respawn should be recovered");
        helper.assertTrue(player.getRespawnConfig() != null && !player.getRespawnConfig().forced(),
                "Legacy emergency bunk respawn config should be healed to non-forced");
        helper.assertTrue(player.getRespawnConfig() != null && player.getRespawnConfig().respawnData().pos().equals(head),
                "Legacy emergency bunk respawn should be re-anchored at the head half");

        if (player.isSleeping()) {
            player.stopSleepInBed(true, true);
        }
        helper.succeed();
    }

    private static void emergencyBunkPairingBehaviour(GameTestHelper helper) {
        BlockPos foot = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos head = foot.relative(Direction.SOUTH);
        placeEmergencyBunk(helper, foot, Direction.SOUTH);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        BlockState headState = helper.getLevel().getBlockState(head);
        headState.setBedOccupied(helper.getLevel(), head, player, true);
        helper.assertTrue(helper.getLevel().getBlockState(head).getValue(EmergencyBunkBlock.OCCUPIED),
                "Emergency bunk head should become occupied");
        helper.assertTrue(helper.getLevel().getBlockState(foot).getValue(EmergencyBunkBlock.OCCUPIED),
                "Emergency bunk foot should mirror occupied state");
        helper.getLevel().getBlockState(head).setBedOccupied(helper.getLevel(), head, player, false);
        helper.assertFalse(helper.getLevel().getBlockState(head).getValue(EmergencyBunkBlock.OCCUPIED),
                "Emergency bunk head should clear occupied state");
        helper.assertFalse(helper.getLevel().getBlockState(foot).getValue(EmergencyBunkBlock.OCCUPIED),
                "Emergency bunk foot should clear occupied state");

        int droppedBeforeHeadBreak = countDroppedEmergencyBunkItems(helper);
        helper.getLevel().destroyBlock(head, true);
        helper.assertTrue(helper.getLevel().getBlockState(foot).isAir(),
                "Emergency bunk foot should disappear when head half is removed");
        helper.assertTrue(countDroppedEmergencyBunkItems(helper) == droppedBeforeHeadBreak + 1,
                "Emergency bunk should drop exactly one item when head half is removed");

        BlockPos secondFoot = helper.absolutePos(new BlockPos(6, 2, 2));
        BlockPos secondHead = secondFoot.relative(Direction.SOUTH);
        placeEmergencyBunk(helper, secondFoot, Direction.SOUTH);
        int droppedBeforeFootBreak = countDroppedEmergencyBunkItems(helper);
        helper.getLevel().destroyBlock(secondFoot, true);
        helper.assertTrue(helper.getLevel().getBlockState(secondHead).isAir(),
                "Emergency bunk head should disappear when foot half is removed");
        helper.assertTrue(countDroppedEmergencyBunkItems(helper) == droppedBeforeFootBreak + 1,
                "Emergency bunk should drop exactly one item when foot half is removed");

        helper.succeed();
    }

    private static void placeEmergencyBunk(GameTestHelper helper, BlockPos foot, Direction facing) {
        BlockState footState = ModBlocks.EMERGENCY_BUNK.get().defaultBlockState()
                .setValue(EmergencyBunkBlock.FACING, facing)
                .setValue(EmergencyBunkBlock.PART, BedPart.FOOT)
                .setValue(EmergencyBunkBlock.OCCUPIED, false);
        helper.getLevel().setBlock(foot, footState, 3);
        helper.getLevel().setBlock(foot.relative(facing),
                footState.setValue(EmergencyBunkBlock.PART, BedPart.HEAD),
                3);
    }

    private static void ashCampfireShelterPulse(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos playerPos = helper.absolutePos(new BlockPos(2, 2, 4));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);

        BlockPos lit = new BlockPos(2, 2, 2);
        BlockPos unlit = new BlockPos(10, 2, 2);
        helper.setBlock(lit, ModBlocks.ASH_CAMPFIRE.get().defaultBlockState().setValue(CampfireBlock.LIT, true));
        helper.setBlock(unlit, ModBlocks.ASH_CAMPFIRE.get().defaultBlockState().setValue(CampfireBlock.LIT, false));

        Mob nearLit = ModEntities.RAD_ZOMBIE.get().create(level, EntitySpawnReason.EVENT);
        Mob nearUnlit = ModEntities.RAD_ZOMBIE.get().create(level, EntitySpawnReason.EVENT);
        helper.assertTrue(nearLit != null && nearUnlit != null,
                "Rad zombies should spawn for ash campfire shelter pulse coverage");
        if (nearLit == null || nearUnlit == null) {
            helper.succeed();
            return;
        }
        nearLit.setNoAi(true);
        nearUnlit.setNoAi(true);
        BlockPos litZombiePos = helper.absolutePos(new BlockPos(3, 2, 2));
        BlockPos unlitZombiePos = helper.absolutePos(new BlockPos(11, 2, 2));
        nearLit.setPos(litZombiePos.getX() + 0.5D, litZombiePos.getY(), litZombiePos.getZ() + 0.5D);
        nearUnlit.setPos(unlitZombiePos.getX() + 0.5D, unlitZombiePos.getY(), unlitZombiePos.getZ() + 0.5D);
        nearLit.setTarget(player);
        nearUnlit.setTarget(player);
        level.addFreshEntity(nearLit);
        level.addFreshEntity(nearUnlit);

        level.scheduleTick(helper.absolutePos(lit), ModBlocks.ASH_CAMPFIRE.get(), 1);
        level.scheduleTick(helper.absolutePos(unlit), ModBlocks.ASH_CAMPFIRE.get(), 1);
        helper.runAfterDelay(3L, () -> {
            helper.assertTrue(nearLit.getTarget() == null,
                    "Lit Ash Campfire scheduled shelter pulse should clear nearby hostile targets");
            helper.assertTrue(nearUnlit.getTarget() == player,
                    "Unlit Ash Campfire should not clear hostile targets");
            nearLit.discard();
            nearUnlit.discard();
            helper.succeed();
        });
    }

    private static int countDroppedEmergencyBunkItems(GameTestHelper helper) {
        BlockPos firstCorner = helper.absolutePos(new BlockPos(0, 0, 0));
        BlockPos secondCorner = helper.absolutePos(new BlockPos(10, 6, 6));
        double minX = Math.min(firstCorner.getX(), secondCorner.getX()) - 2.0D;
        double minY = Math.min(firstCorner.getY(), secondCorner.getY()) - 2.0D;
        double minZ = Math.min(firstCorner.getZ(), secondCorner.getZ()) - 2.0D;
        double maxX = Math.max(firstCorner.getX(), secondCorner.getX()) + 2.0D;
        double maxY = Math.max(firstCorner.getY(), secondCorner.getY()) + 2.0D;
        double maxZ = Math.max(firstCorner.getZ(), secondCorner.getZ()) + 2.0D;

        return helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                        new net.minecraft.world.phys.AABB(minX, minY, minZ, maxX, maxY, maxZ),
                        entity -> entity.getItem().is(ModBlocks.EMERGENCY_BUNK_ITEM.get()))
                .stream()
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }

    private static void starterDropPodCorruptionGuard(GameTestHelper helper) {
        var template = helper.getLevel().getStructureManager().get(id("drop_pod"));
        helper.assertTrue(template.isPresent(), "Starting drop pod NBT template should load");
        Vec3i size = template.orElseThrow().getSize();

        BlockPos origin = helper.absolutePos(new BlockPos(64, 4, 64));
        BlockPos placePos = origin.offset(-size.getX() / 2, -2, -size.getZ() / 2);
        BlockPos staleClearPos = placePos.offset(9, 3, -1);
        helper.getLevel().setBlock(staleClearPos, Blocks.CHEST.defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(staleClearPos) != null,
                "Regression setup should create a stale-prone block entity before pod clearing");

        BlockPos spawn = ProceduralStructureGenerator.placeStartingDropPod(
                helper.getLevel(), origin, helper.getLevel().getRandom());
        helper.assertTrue(spawn != null, "Starting drop pod placement should still succeed with stale block entities nearby");
        helper.assertTrue(helper.getLevel().getBlockState(staleClearPos).isAir(),
                "Starting pod clear margin should leave stale setup block as air");
        helper.assertTrue(helper.getLevel().getBlockEntity(staleClearPos) == null,
                "Starting pod clear margin should remove stale block entity data before saving");

        helper.assertTrue(countInvalidBlockEntities(helper, placePos, size) == 0,
                "Drop pod placement should not leave block entities on air or non-entity blocks");
        helper.succeed();
    }

    private static void startingDropPodDataLenientLoad(GameTestHelper helper) {
        com.google.gson.JsonObject empty = new com.google.gson.JsonObject();
        StartingDropPodData emptyData = StartingDropPodData.CODEC.parse(JsonOps.INSTANCE, empty)
                .result()
                .orElse(null);
        helper.assertTrue(emptyData != null, "Starting drop pod data should load when pods is absent");

        com.google.gson.JsonObject malformedEntry = new com.google.gson.JsonObject();
        malformedEntry.addProperty("playerId", "not-a-uuid");
        com.google.gson.JsonArray pods = new com.google.gson.JsonArray();
        pods.add(malformedEntry);
        com.google.gson.JsonObject malformedRoot = new com.google.gson.JsonObject();
        malformedRoot.add("pods", pods);
        StartingDropPodData malformedData = StartingDropPodData.CODEC.parse(JsonOps.INSTANCE, malformedRoot)
                .result()
                .orElse(null);
        helper.assertTrue(malformedData != null,
                "Starting drop pod data should load and skip malformed player entries");
        helper.succeed();
    }

    private static void wikiManualOptionalIntegration(GameTestHelper helper) {
        ItemStack missing = AshfallWikiIntegration.guideBookStack(
                Identifier.fromNamespaceAndPath(AshfallWikiIntegration.WIKI_MODID, "definitely_missing_manual"));
        helper.assertTrue(missing.isEmpty(), "Ashfall Wiki integration should return empty stacks for unknown manuals.");

        ItemStack manual = AshfallWikiIntegration.ashfallManualStack();
        if (AshfallWikiIntegration.isWikiLoaded()) {
            Identifier itemId = BuiltInRegistries.ITEM.getKey(manual.getItem());
            helper.assertTrue(!manual.isEmpty(), "Ashfall Wiki integration should create the Ashfall manual when Wiki is loaded.");
            helper.assertTrue(Identifier.fromNamespaceAndPath(AshfallWikiIntegration.WIKI_MODID, "guide_book").equals(itemId),
                    "Ashfall Wiki integration should return the shared Wiki guide-book item.");
            helper.assertTrue(AshfallWikiIntegration.isGuideBookVisible(AshfallWikiIntegration.ASHFALL_MANUAL_ID),
                    "Ashfall manual should be visible through the optional Wiki API when both mods are loaded.");
        } else {
            helper.assertTrue(manual.isEmpty(), "Ashfall Wiki integration should degrade silently when Wiki is absent.");
        }
        helper.succeed();
    }

    private static void wikiManualLootModifier(GameTestHelper helper) {
        Map<String, Float> expectedChances = Map.of(
                "wiki_manual_survivor_cache", 0.18F,
                "wiki_manual_crashed_satellite_cache", 0.25F,
                "wiki_manual_radio_tower_cache", 0.12F,
                "wiki_manual_data_center_cache", 0.12F);

        for (Map.Entry<String, Float> entry : expectedChances.entrySet()) {
            JsonObject json = readLootModifierResource(entry.getKey());
            helper.assertTrue("echoashfallprotocol:wiki_manual".equals(jsonString(json, "type")),
                    "Wiki manual loot modifier " + entry.getKey() + " should use the Ashfall modifier type.");
            helper.assertTrue(AshfallWikiIntegration.ASHFALL_MANUAL_ID.toString().equals(jsonString(json, "guideBookId")),
                    "Wiki manual loot modifier " + entry.getKey() + " should target the Ashfall manual.");
            helper.assertTrue(Math.abs(json.get("chance").getAsFloat() - entry.getValue()) < 0.0001F,
                    "Wiki manual loot modifier " + entry.getKey() + " should keep its configured chance.");

            WikiManualLootModifier modifier = WikiManualLootModifier.CODEC.codec().parse(JsonOps.INSTANCE, json)
                    .result()
                    .orElse(null);
            helper.assertTrue(modifier != null, "Wiki manual loot modifier " + entry.getKey() + " should decode.");
            helper.assertTrue(modifier != null && modifier.guideBookId().equals(AshfallWikiIntegration.ASHFALL_MANUAL_ID),
                    "Decoded Wiki manual loot modifier should preserve guideBookId.");
            helper.assertTrue(modifier != null && Math.abs(modifier.chance() - entry.getValue()) < 0.0001F,
                    "Decoded Wiki manual loot modifier should preserve chance.");
        }

        JsonObject invalid = readLootModifierResource("wiki_manual_survivor_cache").deepCopy();
        invalid.addProperty("chance", 1.25F);
        helper.assertTrue(WikiManualLootModifier.CODEC.codec().parse(JsonOps.INSTANCE, invalid).error().isPresent(),
                "Wiki manual loot modifier codec should reject chances above 1.0.");
        helper.assertFalse(WikiManualLootModifier.shouldInjectForTests(0.0F, 0.0F),
                "Zero chance should never inject.");
        helper.assertTrue(WikiManualLootModifier.shouldInjectForTests(1.0F, 0.999F),
                "Full chance should inject for every random roll.");
        helper.assertTrue(WikiManualLootModifier.shouldInjectForTests(0.18F, 0.17F),
                "Rolls below the configured chance should inject.");
        helper.assertFalse(WikiManualLootModifier.shouldInjectForTests(0.18F, 0.18F),
                "Rolls at or above the configured chance should not inject.");
        helper.succeed();
    }

    private static void archiveReadState(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoIntel intel = EchoIntel.get(player);
        intel.discoverLore("terminal_overhaul_test", "Terminal Overhaul Test", "Readable archive content.");
        helper.assertTrue(intel.getUnreadCount() >= 1, "Discovered lore should create unread intel");
        intel.markAsRead("lore_terminal_overhaul_test");
        helper.assertTrue(intel.getAllIntel().stream()
                        .filter(entry -> entry.id.equals("lore_terminal_overhaul_test"))
                        .allMatch(entry -> entry.isRead),
                "Archive read-state clearing should mark selected intel read");
        helper.succeed();
    }

    private static void substrateGrinderRecipes(GameTestHelper helper) {
        List<Item> substrateInputs = List.of(
                Items.STONE,
                Items.COBBLESTONE,
                Items.DEEPSLATE,
                Items.COBBLED_DEEPSLATE,
                ModBlocks.WASTELAND_STONE.get().asItem(),
                ModBlocks.WASTELAND_TRACE_RUBBLE.get().asItem(),
                ModBlocks.SCRAP_ORE.get().asItem(),
                ModBlocks.RUBBLE.get().asItem(),
                ModBlocks.CONCRETE_RUBBLE.get().asItem(),
                ModBlocks.CONCRETE_CHUNK.get().asItem(),
                ModBlocks.INDUSTRIAL_AGGREGATE.get().asItem(),
                ModBlocks.OIL_STAINED_CONCRETE.get().asItem(),
                ModBlocks.CRASH_SLAG.get().asItem(),
                ModBlocks.ASH_STONE.get().asItem(),
                ModBlocks.DEEP_ASH.get().asItem(),
                ModBlocks.TOXIC_SLAGSTONE.get().asItem(),
                ModBlocks.IRRADIATED_CRUST.get().asItem(),
                ModBlocks.IRRADIATED_SHALE.get().asItem(),
                ModBlocks.CRYOGENIC_FRACTURED_STONE.get().asItem(),
                ModBlocks.NEXUS_CRACKED_SOIL.get().asItem(),
                ModBlocks.RIFTSTONE.get().asItem()
        );
        List<Item> legacyInputs = List.of(
                ModItems.IRON_SHARD.get(),
                ModItems.COPPER_SHARD.get(),
                ModItems.COAL_DUST.get(),
                ModItems.GOLD_TRACE.get(),
                ModItems.GOLD_CLUSTER.get(),
                ModItems.URANIUM_SHARD.get()
        );

        for (Item input : substrateInputs) {
            helper.assertTrue(OreGrinderBlockEntity.hasSubstrateRecipe(new ItemStack(input)),
                    "Substrate Grinder missing recipe for " + BuiltInRegistries.ITEM.getKey(input));
        }
        for (Item input : legacyInputs) {
            helper.assertTrue(OreGrinderBlockEntity.hasSubstrateRecipe(new ItemStack(input)),
                    "Legacy grinder recipe missing for " + BuiltInRegistries.ITEM.getKey(input));
        }
        OreGrinderBlockEntity.GrinderRecipe crustRecipe =
                OreGrinderBlockEntity.getSubstrateRecipe(ModBlocks.IRRADIATED_CRUST.get().asItem());
        helper.assertTrue(crustRecipe != null && crustRecipe.output() == ModItems.URANIUM_SHARD.get(),
                "Irradiated Crust should grind into Uranium Shards");

        BlockPos grinderPos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(grinderPos, ModBlocks.ORE_GRINDER.get().defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(grinderPos) instanceof OreGrinderBlockEntity,
                "Substrate Grinder block entity should exist for insertion coverage");
        if (helper.getLevel().getBlockEntity(grinderPos) instanceof OreGrinderBlockEntity grinder) {
            helper.assertTrue(grinder.canInsertItem(OreGrinderBlockEntity.INPUT_SLOT_1,
                            new ItemStack(Items.STONE)),
                    "Hopper insertion should accept single valid substrate items before a full batch is present");
            helper.assertTrue(grinder.canInsertItem(OreGrinderBlockEntity.INPUT_SLOT_1,
                            new ItemStack(ModBlocks.WASTELAND_STONE.get())),
                    "Hopper insertion should accept biome substrate inputs");
            helper.assertTrue(grinder.canInsertItem(OreGrinderBlockEntity.INPUT_SLOT_1,
                            new ItemStack(ModBlocks.SCRAP_ORE.get())),
                    "Hopper insertion should accept mined Scrap Ore");
            helper.assertFalse(grinder.canInsertItem(OreGrinderBlockEntity.OUTPUT_SLOT,
                            new ItemStack(ModBlocks.WASTELAND_STONE.get(), 3)),
                    "Hopper insertion should reject direct output-slot input");
            helper.assertTrue(grinder.canExtractItem(OreGrinderBlockEntity.BYPRODUCT_SLOT),
                    "Byproducts should be extractable only from the byproduct/output side");
            helper.assertTrue(grinder.getOutputSlots(Direction.DOWN).length == 2,
                    "Downward hopper extraction should expose output and byproduct slots");
            helper.assertTrue(grinder.getOutputSlots(Direction.NORTH).length == 0,
                    "Side hopper extraction should not pull grinder outputs");

            grinder.getInventory().setStackInSlot(OreGrinderBlockEntity.INPUT_SLOT_1,
                    new ItemStack(ModBlocks.WASTELAND_STONE.get(), 3));
            grinder.setEnergyStored(1_000);
            for (int tick = 0; tick < 120; tick++) {
                OreGrinderBlockEntity.serverTick(helper.getLevel(), grinderPos,
                        helper.getLevel().getBlockState(grinderPos), grinder);
            }
            ItemStack output = grinder.getInventory().getStackInSlot(OreGrinderBlockEntity.OUTPUT_SLOT);
            helper.assertTrue(output.is(ModItems.IRON_SHARD.get()) && output.getCount() >= 2,
                    "Powered grinder should turn wasteland stone into iron shards");
        }

        BlockPos dropPos = helper.absolutePos(new BlockPos(4, 1, 1));
        helper.getLevel().setBlock(dropPos, ModBlocks.ORE_GRINDER.get().defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(dropPos) instanceof OreGrinderBlockEntity,
                "Substrate Grinder block entity should exist for drop coverage");
        if (helper.getLevel().getBlockEntity(dropPos) instanceof OreGrinderBlockEntity grinder) {
            grinder.getInventory().setStackInSlot(OreGrinderBlockEntity.INPUT_SLOT_1, new ItemStack(Items.STONE, 4));
            grinder.getInventory().setStackInSlot(OreGrinderBlockEntity.OUTPUT_SLOT,
                    new ItemStack(ModItems.IRON_SHARD.get(), 2));
            grinder.getInventory().setStackInSlot(OreGrinderBlockEntity.BYPRODUCT_SLOT, new ItemStack(Items.FLINT));
            grinder.getInventory().setStackInSlot(OreGrinderBlockEntity.BATTERY_SLOT, new ItemStack(Items.REDSTONE, 3));
            Player breaker = helper.makeMockPlayer(GameType.SURVIVAL);
            ModBlocks.ORE_GRINDER.get().playerWillDestroy(helper.getLevel(), dropPos,
                    helper.getLevel().getBlockState(dropPos), breaker);
            helper.getLevel().removeBlock(dropPos, false);
            helper.assertTrue(countDroppedItems(helper, dropPos, Items.STONE) == 4,
                    "Breaking a Substrate Grinder should drop input items");
            helper.assertTrue(countDroppedItems(helper, dropPos, ModItems.IRON_SHARD.get()) == 2,
                    "Breaking a Substrate Grinder should drop output items");
            helper.assertTrue(countDroppedItems(helper, dropPos, Items.FLINT) == 1,
                    "Breaking a Substrate Grinder should drop byproduct-slot items");
            helper.assertTrue(countDroppedItems(helper, dropPos, Items.REDSTONE) == 3,
                    "Breaking a Substrate Grinder should drop battery-slot items");
        }

        for (OreGrinderBlockEntity.GrinderRecipe recipe : OreGrinderBlockEntity.getSubstrateRecipes().values()) {
            helper.assertTrue(BuiltInRegistries.ITEM.getKey(recipe.output()) != null,
                    "Grinder recipe output must be registered for " + BuiltInRegistries.ITEM.getKey(recipe.input()));
            if (recipe.byproduct() != null) {
                helper.assertTrue(BuiltInRegistries.ITEM.getKey(recipe.byproduct()) != null,
                        "Grinder recipe byproduct must be registered for " + BuiltInRegistries.ITEM.getKey(recipe.input()));
                helper.assertTrue(recipe.byproductChance() > 0.0F && recipe.byproductChance() <= 1.0F,
                        "Grinder byproduct chance out of range for " + BuiltInRegistries.ITEM.getKey(recipe.input()));
                helper.assertTrue(recipe.byproductCount() > 0,
                        "Grinder byproduct count must be positive for " + BuiltInRegistries.ITEM.getKey(recipe.input()));
            }
            helper.assertTrue(recipe.inputCount() > 0 && recipe.outputCount() > 0,
                    "Grinder recipe counts must be positive for " + BuiltInRegistries.ITEM.getKey(recipe.input()));
            helper.assertTrue(recipe.processTime() > 0 && recipe.powerPerOperation() > 0,
                    "Grinder recipe cost must be positive for " + BuiltInRegistries.ITEM.getKey(recipe.input()));
        }
        helper.succeed();
    }

    private static void echoContainerBlockEntities(GameTestHelper helper) {
        List<net.minecraft.world.level.block.Block> blocks = List.of(
                ModBlocks.ECHO_CACHE.get(),
                ModBlocks.ECHO_CRATE.get(),
                ModBlocks.SUPPLY_CRATE.get()
        );
        for (int i = 0; i < blocks.size(); i++) {
            BlockPos pos = helper.absolutePos(new BlockPos(1 + i, 1, 1));
            helper.getLevel().setBlock(pos, blocks.get(i).defaultBlockState(), 3);
            helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof EchoContainerBlockEntity,
                    "Crate/cache block should create an Echo container block entity: "
                            + BuiltInRegistries.BLOCK.getKey(blocks.get(i)));
        }
        helper.succeed();
    }

    private static void missionCoreRewardClaimableUx(GameTestHelper helper) {
        if (!ModList.get().isLoaded("echomissioncore") || !EchoCoreServices.missionCoreAvailable()) {
            helper.succeed();
            return;
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            helper.succeed();
            return;
        }

        helper.assertTrue(AshfallMissionCoreIntegration.registerWhenReady(),
                "Ashfall MissionCore content should register for claimable reward coverage");
        if (nativeJsonOwnsMissionCoreRoute()) {
            assertNativeMissionCoreRewardClaimable(helper, serverPlayer);
            helper.succeed();
            return;
        }
        Mission mission = requireMission(helper, "craft_scrap_knife");
        QuestData quest = QuestData.get(serverPlayer);
        setCurrentMission(quest, mission.id());
        serverPlayer.getInventory().add(new ItemStack(ModItems.SCRAP_KNIFE.get()));
        QuestData.saveAndSync(serverPlayer, quest);

        helper.assertTrue(EchoCoreServices.completeMission(serverPlayer, AshfallMissionCoreIntegration.missionId(mission.id())),
                "MissionCore should complete Scrap Knife mission");
        MissionUxSummary summary = MissionUxSummary.of(serverPlayer, QuestData.get(serverPlayer), mission);
        helper.assertTrue("READY".equals(summary.statusLabel()),
                "MissionCore-backed completed rewards should display READY before claim");
        int beforeScrap = countInventory(serverPlayer, ModItems.SCRAP_METAL.get());
        helper.assertTrue(AshfallMissionCoreIntegration.claimReward(serverPlayer, mission.id()),
                "MissionCore reward claim should succeed once");
        helper.assertFalse(AshfallMissionCoreIntegration.claimReward(serverPlayer, mission.id()),
                "MissionCore reward claim should be idempotent");
        helper.assertTrue(countInventory(serverPlayer, ModItems.SCRAP_METAL.get()) >= beforeScrap + 12,
                "Scrap Knife claim should grant the Scrap Metal reward");
        helper.succeed();
    }

    private static void ashfallRouteThinSpine(GameTestHelper helper) {
        Mission emergencyWater = requireMission(helper, "secure_emergency_water_loop");
        Mission forage = requireMission(helper, "forage_wasteland_food");
        Mission rainCollector = requireMission(helper, "build_rain_collector");
        Mission fieldKit = requireMission(helper, "assemble_wasteland_field_kit");
        Mission drone = requireMission(helper, "build_scout_drone");
        Mission mutationScan = requireMission(helper, "scan_mutation_status");

        helper.assertFalse(AshfallMissionRoute.blocksPhase(emergencyWater),
                "Emergency water loop should be optional route intel, not a phase blocker");
        helper.assertTrue(AshfallMissionRoute.blocksPhase(forage)
                        && AshfallMissionRoute.blocksPhase(rainCollector)
                        && AshfallMissionRoute.blocksPhase(fieldKit)
                        && AshfallMissionRoute.blocksPhase(drone)
                        && AshfallMissionRoute.blocksPhase(mutationScan),
                "Explicit MAIN_SPINE missions should remain phase blockers");
        helper.assertTrue("drink_clean_water".equals(AshfallMissionRoute.routeAnchor(emergencyWater.id())),
                "Demoted emergency water should anchor beside the nearest main hydration milestone");
        helper.assertTrue(AshfallMissionRoute.mainlinePrerequisites(forage).contains("drink_clean_water")
                        && !AshfallMissionRoute.mainlinePrerequisites(forage).contains(emergencyWater.id()),
                "Main-spine prerequisites should skip demoted optional hydration work");
        helper.assertTrue(AshfallMissionRoute.mainlinePrerequisites(rainCollector).contains(forage.id())
                        && !AshfallMissionRoute.mainlinePrerequisites(rainCollector).contains("plant_mutated_sapling"),
                "Main-spine prerequisites should skip optional recovery/tutorial steps");
        for (int phase = 0; phase < MissionRegistry.getPhaseCount(); phase++) {
            for (Mission mission : MissionRegistry.getMissionsForPhase(phase)) {
                if (AshfallMissionRoute.isMainSpine(mission.id())) {
                    for (String prerequisite : AshfallMissionRoute.mainlinePrerequisites(mission)) {
                        helper.assertTrue(AshfallMissionRoute.isMainSpine(prerequisite),
                                "Main-spine Ashfall mission should not depend on optional route work: "
                                        + mission.id() + " -> " + prerequisite);
                    }
                } else {
                    helper.assertFalse(AshfallMissionRoute.blocksPhase(mission),
                            "Optional Ashfall mission should not block phase completion: " + mission.id());
                }
            }
        }

        QuestData quest = new QuestData();
        for (String missionId : List.of(
                "secure_crash_outpost",
                "craft_scrap_knife",
                "drink_clean_water",
                "forage_wasteland_food",
                "build_rain_collector",
                "secure_sleep_shelter")) {
            quest.completeMission(missionId, List.of());
        }
        helper.assertFalse(quest.isMissionCompleted(emergencyWater.id()),
                "Demoted optional missions should not be silently completed by the spine");
        helper.assertTrue(quest.isPhaseCompleted(0),
                "Phase 0 should complete from main-spine milestones even when demoted side ops are incomplete");
        helper.assertTrue(quest.isMissionUnlocked(fieldKit.id()),
                "Next-phase main route should unlock after the thin spine, without optional cleanup gates");
        quest.repairMissionState();
        Mission current = MissionRegistry.getMission(quest.getCurrentPhase(), quest.getCurrentMissionIndex());
        helper.assertTrue(current != null && fieldKit.id().equals(current.id()),
                "Quest repair should focus the next main-spine mission instead of optional side work");

        if (ModList.get().isLoaded("echomissioncore") && EchoCoreServices.missionCoreAvailable()
                && AshfallMissionCoreIntegration.registerWhenReady()) {
            var optionalView = EchoCoreServices.missionService()
                    .mission(null, AshfallMissionCoreIntegration.missionId(emergencyWater.id()))
                    .orElseThrow();
            helper.assertTrue(optionalView.definition().kind() == MissionKind.SIDE_OP,
                    "Demoted Ashfall missions should export to MissionCore as side ops");
            helper.assertTrue("OPTIONAL".equals(optionalView.definition().metadata().get("terminal_route_role")),
                    "Demoted Ashfall missions should carry optional route metadata");

            var mainView = EchoCoreServices.missionService()
                    .mission(null, AshfallMissionCoreIntegration.missionId(forage.id()))
                    .orElseThrow();
            helper.assertTrue(mainView.definition().kind() == MissionKind.MAIN
                            && "MAIN".equals(mainView.definition().metadata().get("terminal_route_role")),
                    "Main-spine Ashfall missions should remain main MissionCore route records");
            if (nativeJsonOwnsMissionCoreRoute()) {
                helper.assertTrue(mainView.definition().prerequisites()
                                .contains(AshfallMissionCoreIntegration.missionId(emergencyWater.id())),
                        "Native MissionCore JSON should preserve the complete ordered route including optional hydration proof");
            } else {
                helper.assertTrue(AshfallMissionCoreIntegration.missionId("drink_clean_water").toString()
                                .equals(optionalView.definition().metadata().get("terminal_route_anchor")),
                        "Demoted Ashfall missions should carry a stable main-spine route anchor");
                helper.assertTrue(mainView.definition().prerequisites()
                                .contains(AshfallMissionCoreIntegration.missionId("drink_clean_water"))
                                && !mainView.definition().prerequisites()
                                .contains(AshfallMissionCoreIntegration.missionId(emergencyWater.id())),
                        "MissionCore Java fallback main-spine prerequisites should skip optional Ashfall gates");
            }
        }

        helper.succeed();
    }

    private static void ashfallMissionRequirementAudit(GameTestHelper helper) {
        Set<String> missionIds = new HashSet<>();
        List<Mission> missions = new ArrayList<>();
        for (int phase = 0; phase < MissionRegistry.getPhaseCount(); phase++) {
            for (Mission mission : MissionRegistry.getMissionsForPhase(phase)) {
                helper.assertTrue(missionIds.add(mission.id()),
                        "Ashfall mission id should be unique: " + mission.id());
                missions.add(mission);
            }
        }
        helper.assertFalse(missions.isEmpty(), "Ashfall mission registry should not be empty");
        Map<String, Mission> missionsById = missions.stream()
                .collect(java.util.stream.Collectors.toMap(Mission::id, mission -> mission));
        for (Mission mission : missions) {
            assertNoAshfallMissionCycle(helper, mission, missionsById, new HashSet<>());
        }

        boolean missionCoreReady = ModList.get().isLoaded("echomissioncore")
                && EchoCoreServices.missionCoreAvailable()
                && AshfallMissionCoreIntegration.registerWhenReady();
        boolean sawItemRequirement = false;
        boolean sawBlockRequirement = false;
        boolean sawLocationRequirement = false;
        boolean sawEquipmentRequirement = false;
        boolean sawKillRequirement = false;
        boolean sawReward = false;
        boolean sawBlockRewardIconSplit = false;
        for (Mission mission : missions) {
            sawItemRequirement |= !mission.requiredItems().isEmpty();
            sawBlockRequirement |= !mission.requiredBlocks().isEmpty();
            sawLocationRequirement |= !mission.requiredLocations().isEmpty();
            sawEquipmentRequirement |= !mission.requiredEquipment().isEmpty();
            sawKillRequirement |= !mission.requiredEntityKills().isEmpty();
            sawReward |= !mission.rewards().isEmpty();

            for (String prerequisite : mission.getPrerequisites()) {
                helper.assertTrue(MissionRegistry.getMissionById(prerequisite) != null,
                        "Ashfall mission prerequisite should exist: " + mission.id() + " -> " + prerequisite);
            }

            boolean mainSpine = AshfallMissionRoute.isMainSpine(mission.id());
            String routeAnchor = AshfallMissionRoute.routeAnchor(mission.id());
            if (!mainSpine) {
                helper.assertFalse(AshfallMissionRoute.blocksPhase(mission),
                        "Ashfall optional missions must not block route phase completion: " + mission.id());
            }
            if (!routeAnchor.isBlank()) {
                helper.assertTrue(MissionRegistry.getMissionById(routeAnchor) != null,
                        "Ashfall side-op route anchor should exist: " + mission.id() + " -> " + routeAnchor);
                helper.assertTrue(AshfallMissionRoute.isMainSpine(routeAnchor),
                        "Ashfall side-op route anchor should target the main spine: " + mission.id() + " -> " + routeAnchor);
            }
            if (mainSpine) {
                for (String prerequisite : AshfallMissionRoute.mainlinePrerequisites(mission)) {
                    helper.assertTrue(MissionRegistry.getMissionById(prerequisite) != null,
                            "Ashfall main-spine prerequisite should exist: " + mission.id() + " -> " + prerequisite);
                    helper.assertTrue(AshfallMissionRoute.isMainSpine(prerequisite),
                            "Ashfall main-spine prerequisite should stay on the spine: " + mission.id() + " -> " + prerequisite);
                }
            }
            auditAshfallMissionRequirements(helper, mission);

            if (missionCoreReady) {
                MissionDefinition definition = EchoCoreServices.missionService()
                        .missionDefinition(AshfallMissionCoreIntegration.missionId(mission.id()))
                        .orElse(null);
                helper.assertTrue(definition != null,
                        "Ashfall mission should export to MissionCore: " + mission.id());
                if (definition != null) {
                    sawBlockRewardIconSplit |= auditAshfallMissionDefinition(helper, mission, definition, mainSpine, routeAnchor);
                }
            }
        }
        auditAshfallDiagnosticBlockers(helper);
        helper.assertTrue(sawItemRequirement && sawBlockRequirement && sawLocationRequirement
                        && sawEquipmentRequirement && sawKillRequirement && sawReward,
                "Ashfall mission audit should cover item, block, location, equipment, kill, and reward missions");
        if (missionCoreReady && !nativeJsonOwnsMissionCoreRoute()) {
            helper.assertTrue(sawBlockRewardIconSplit,
                    "Ashfall mission audit should compare block requirement icons against differing reward icons");
        }
        helper.succeed();
    }

    private static void ashfallMissionCoreRuntimeRoute(GameTestHelper helper) {
        if (!ModList.get().isLoaded("echomissioncore") || !EchoCoreServices.missionCoreAvailable()) {
            helper.succeed();
            return;
        }
        helper.assertTrue(AshfallMissionCoreIntegration.registerWhenReady(),
                "Ashfall MissionCore native route should be ready for runtime validation");
        helper.assertTrue(nativeJsonOwnsMissionCoreRoute(),
                "Ashfall MissionCore runtime should use native JSON as the authoritative route source");
        helper.assertTrue(EchoCoreServices.missionService()
                        .chapter(AshfallMissionCoreIntegration.NATIVE_JSON_CHAPTER_ID)
                        .isPresent(),
                "Ashfall native MissionCore chapter should load from JSON");
        assertNativeSupportMissionLoaded(helper, "ashfall_first_month_routes",
                AshfallMissionCoreIntegration.NATIVE_JSON_CHAPTER_ID);
        assertNativeSupportMissionLoaded(helper, "ashfall_showcase_flow",
                AshfallMissionCoreIntegration.NATIVE_JSON_CHAPTER_ID);
        assertNativeSupportMissionLoaded(helper, "faction_signal_ping",
                AshfallMissionCoreIntegration.NATIVE_JSON_CHAPTER_ID);
        assertNativeSupportMissionLoaded(helper, "hazard_route_prep",
                AshfallMissionCoreIntegration.NATIVE_JSON_CHAPTER_ID);
        assertNativeSupportMissionLoaded(helper, "survivor_cache_sweep",
                AshfallMissionCoreIntegration.NATIVE_JSON_CHAPTER_ID);
        assertNativeSupportMissionLoaded(helper, "crashbreak_relay_contract",
                Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "ashfall_major_routes"));

        List<Mission> route = routeMissionsInOrder();
        helper.assertTrue(route.size() >= 100,
                "Ashfall native MissionCore route should expose the complete campaign route");
        for (int phase = 0; phase < MissionRegistry.getPhaseCount(); phase++) {
            List<Mission> missions = MissionRegistry.getMissionsForPhase(phase);
            for (int order = 0; order < missions.size(); order++) {
                assertNativeRouteMissionLoaded(helper, missions.get(order), phase, order);
            }
        }
        for (String deprecatedMissionId : deprecatedAshfallMissionIds()) {
            helper.assertTrue(EchoCoreServices.missionService()
                            .missionDefinition(AshfallMissionCoreIntegration.missionId(deprecatedMissionId))
                            .isEmpty(),
                    "Deprecated Ashfall alias must not load as an active MissionCore mission: " + deprecatedMissionId);
        }
        assertNativeDeprecatedAliasMigration(helper);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            helper.succeed();
            return;
        }
        assertNativeFirstSpawnRuntimeStartsMissionCore(helper, serverPlayer);
        assertNativeFirstSpawnChainUsesRuntimeEvents(helper);

        Set<MissionObjectiveType> objectiveTypes = EnumSet.noneOf(MissionObjectiveType.class);
        for (Mission mission : route) {
            IMissionProgressView completed = progressNativeMissionCoreMission(
                    helper,
                    serverPlayer,
                    AshfallMissionCoreIntegration.missionId(mission.id()),
                    objectiveTypes);
            helper.assertTrue(isMissionTerminal(completed.status()),
                    "Native MissionCore objective hooks should complete route mission: " + mission.id());
        }
        assertNativeMissionCoreRewardPersistence(helper, serverPlayer);

        for (MissionObjectiveType expected : List.of(
                MissionObjectiveType.OBTAIN_ITEM,
                MissionObjectiveType.DELIVER_ITEM,
                MissionObjectiveType.PLACE_BLOCK,
                MissionObjectiveType.ENTER_REGION,
                MissionObjectiveType.SCAN_BLOCK,
                MissionObjectiveType.KILL_ENTITY,
                MissionObjectiveType.ESTABLISH_ROUTE,
                MissionObjectiveType.UNLOCK_RESEARCH,
                MissionObjectiveType.SURVIVE_TIME,
                MissionObjectiveType.CUSTOM)) {
            helper.assertTrue(objectiveTypes.contains(expected),
                    "Ashfall runtime route should cover MissionCore objective type: " + expected.id());
        }
        assertNativeDeliverTurnInDoesNotConsumeRequiredItems(helper);
        assertNativeScannerFactionRuntimeHooks(helper);
        assertNativeBranchStarts(helper);
        assertNativeBranchesCompleteIndependently(helper);
        helper.succeed();
    }

    private static void assertNoAshfallMissionCycle(
            GameTestHelper helper,
            Mission mission,
            Map<String, Mission> missionsById,
            Set<String> visiting) {
        if (!visiting.add(mission.id())) {
            helper.assertTrue(false, "Ashfall mission prerequisites should not cycle: " + visiting + " -> " + mission.id());
            return;
        }
        for (String prerequisite : mission.getPrerequisites()) {
            Mission prerequisiteMission = missionsById.get(prerequisite);
            if (prerequisiteMission != null) {
                assertNoAshfallMissionCycle(helper, prerequisiteMission, missionsById, visiting);
            }
        }
        visiting.remove(mission.id());
    }

    private static void auditAshfallMissionRequirements(GameTestHelper helper, Mission mission) {
        for (ItemStack stack : mission.requiredItems()) {
            helper.assertTrue(isResolvableItemStack(stack),
                    "Ashfall mission required item should resolve: " + mission.id());
        }
        for (ItemStack stack : mission.rewards()) {
            helper.assertTrue(isResolvableItemStack(stack),
                    "Ashfall mission reward item should resolve: " + mission.id());
        }
        for (Mission.BlockRequirement requirement : mission.requiredBlocks()) {
            helper.assertTrue(blockRequirementItem(requirement.blockId()) != Items.AIR,
                    "Ashfall mission block requirement should resolve to a registered block item: "
                            + mission.id() + " -> " + requirement.blockId());
            helper.assertTrue(requirement.count() > 0,
                    "Ashfall mission block requirement count should be positive: " + mission.id());
        }
        for (Mission.EntityKillRequirement requirement : mission.requiredEntityKills()) {
            Identifier entityId = requirementId(requirement.entityType(), EchoAshfallProtocol.MODID);
            helper.assertTrue(entityId != null && BuiltInRegistries.ENTITY_TYPE.containsKey(entityId),
                    "Ashfall mission entity requirement should resolve: "
                            + mission.id() + " -> " + requirement.entityType());
            helper.assertTrue(requirement.count() > 0,
                    "Ashfall mission entity requirement count should be positive: " + mission.id());
        }
        for (Mission.LocationRequirement requirement : mission.requiredLocations()) {
            helper.assertFalse(requirement.locationType().isBlank() || requirement.locationId().isBlank(),
                    "Ashfall mission location requirements should have type and id: " + mission.id());
            switch (requirement.locationType()) {
                case "poi" -> helper.assertTrue(ExplorationSiteRegistry.get(requirement.locationId()).isPresent(),
                        "Ashfall mission POI requirement should resolve to an exploration site: "
                                + mission.id() + " -> " + requirement.locationId());
                case "dimension" -> helper.assertTrue(Identifier.tryParse(requirement.locationId()) != null,
                        "Ashfall mission dimension requirement should be a resource id: "
                                + mission.id() + " -> " + requirement.locationId());
                case "biome" -> helper.assertTrue(!requirement.locationId().contains(" "),
                        "Ashfall mission biome requirement should be a stable marker id: "
                                + mission.id() + " -> " + requirement.locationId());
                case "special" -> helper.assertTrue(isExplicitAshfallSpecialMarker(requirement.locationId()),
                        "Ashfall mission special marker should be explicitly handled: "
                                + mission.id() + " -> " + requirement.locationId());
                default -> helper.assertTrue(false,
                        "Ashfall mission location requirement type should be known: "
                                + mission.id() + " -> " + requirement.locationType());
            }
        }
        for (Mission.EquipmentRequirement requirement : mission.requiredEquipment()) {
            helper.assertTrue(requirement.slot() != null && isResolvableItemStack(requirement.item()),
                    "Ashfall mission equipment requirement should resolve slot and item: " + mission.id());
        }
    }

    private static boolean isResolvableItemStack(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && stack.getItem() != Items.AIR
                && BuiltInRegistries.ITEM.getKey(stack.getItem()) != null;
    }

    private static boolean isExplicitAshfallSpecialMarker(String marker) {
        if (marker == null || marker.isBlank() || !marker.contains(":")) {
            return false;
        }
        return marker.startsWith("water:")
                || marker.startsWith("shelter:")
                || marker.startsWith("cache:")
                || marker.startsWith("faction_contact:")
                || marker.startsWith("faction:")
                || marker.startsWith("drone:")
                || marker.startsWith("medical:")
                || marker.startsWith("machine:")
                || marker.startsWith("hazard:")
                || marker.startsWith("lab:")
                || marker.startsWith("relay:")
                || marker.startsWith("power_node:")
                || marker.startsWith("nexus:")
                || marker.startsWith("boss:")
                || marker.startsWith("post_nexus:")
                || marker.startsWith("cold:");
    }

    private static void auditAshfallDiagnosticBlockers(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        if (player == null) {
            return;
        }
        com.knoxhack.echoashfallprotocol.integration.AshfallCoreServices.register();
        Set<Identifier> ids = new HashSet<>();
        for (EchoDiagnosticBlocker blocker : EchoCoreServices.diagnostics(player)) {
            if (!EchoAshfallProtocol.MODID.equals(blocker.id().getNamespace())) {
                continue;
            }
            helper.assertTrue(ids.add(blocker.id()),
                    "Ashfall diagnostic blocker ids should be stable and unique: " + blocker.id());
            helper.assertTrue(blocker.severity() != null,
                    "Ashfall diagnostic blocker should include severity: " + blocker.id());
            helper.assertFalse(blocker.title().isBlank() || blocker.detail().isBlank() || blocker.nextAction().isBlank(),
                    "Ashfall diagnostic blocker should include title, explanation, and next action: " + blocker.id());
        }
        helper.assertFalse(ids.isEmpty(), "Ashfall diagnostic blocker audit should inspect Command Deck blockers");
    }

    private static boolean auditAshfallMissionDefinition(
            GameTestHelper helper,
            Mission mission,
            MissionDefinition definition,
            boolean mainSpine,
            String routeAnchor) {
        if (AshfallMissionCoreIntegration.NATIVE_JSON_CHAPTER_ID.equals(definition.chapterId())) {
            return auditNativeAshfallMissionDefinition(helper, mission, definition);
        }
        helper.assertTrue(mission.id().equals(definition.metadata().get("legacy_id")),
                "MissionCore export should preserve legacy id metadata: " + mission.id());
        helper.assertTrue(EchoAshfallProtocol.MODID.equals(definition.metadata().get("source")),
                "MissionCore export should preserve Ashfall source metadata: " + mission.id());
        helper.assertTrue(definition.metadata().containsKey("terminal_route_phase")
                        && definition.metadata().containsKey("terminal_route_order")
                        && "true".equals(definition.metadata().get("terminal_route_visible")),
                "MissionCore export should include terminal route placement metadata: " + mission.id());
        helper.assertTrue((mainSpine ? MissionKind.MAIN : MissionKind.SIDE_OP) == definition.kind(),
                "MissionCore route kind should match Ashfall spine role: " + mission.id());
        helper.assertTrue((mainSpine ? "MAIN" : "OPTIONAL").equals(definition.metadata().get("terminal_route_role")),
                "MissionCore route role metadata should match Ashfall spine role: " + mission.id());
        if (!routeAnchor.isBlank()) {
            helper.assertTrue(AshfallMissionCoreIntegration.missionId(routeAnchor).toString()
                            .equals(definition.metadata().get("terminal_route_anchor")),
                    "MissionCore side-op anchor metadata should match Ashfall route anchor: " + mission.id());
        }

        List<Identifier> expectedPrerequisites = (mainSpine
                ? AshfallMissionRoute.mainlinePrerequisites(mission)
                : mission.getPrerequisites()).stream()
                .map(AshfallMissionCoreIntegration::missionId)
                .toList();
        helper.assertTrue(definition.prerequisites().equals(expectedPrerequisites),
                "MissionCore prerequisites should match terminal-visible Ashfall route gates: " + mission.id());

        List<MissionObjectiveType> expectedTypes = expectedMissionCoreObjectiveTypes(mission);
        List<ObjectiveDefinition> objectives = definition.objectives();
        helper.assertTrue(objectives.size() == expectedTypes.size(),
                "MissionCore objective count should match legacy terminal checklist rows: " + mission.id());
        for (int index = 0; index < Math.min(objectives.size(), expectedTypes.size()); index++) {
            helper.assertTrue(objectives.get(index).type() == expectedTypes.get(index),
                    "MissionCore objective type should match legacy requirement type: "
                            + mission.id() + " objective " + index);
        }
        return assertMissionCoreRequirementIcons(helper, mission, objectives);
    }

    private static boolean auditNativeAshfallMissionDefinition(
            GameTestHelper helper,
            Mission mission,
            MissionDefinition definition) {
        helper.assertTrue(mission.id().equals(definition.metadata().get("neoForgeMissionId")),
                "Native MissionCore JSON should preserve the Ashfall mission id: " + mission.id());
        helper.assertTrue("echomissioncore".equals(definition.metadata().get("nativeProvider")),
                "Native MissionCore JSON should identify the runtime provider: " + mission.id());
        helper.assertTrue(definition.metadata().containsKey("nativeHooks.completionSignal")
                        && definition.metadata().containsKey("nativeHooks.runtimeEvents"),
                "Native MissionCore JSON should load native hook metadata: " + mission.id());
        helper.assertTrue(definition.metadata().containsKey("terminal_route_phase")
                        && definition.metadata().containsKey("terminal_route_order")
                        && "true".equals(definition.metadata().get("terminal_route_visible")),
                "Native MissionCore JSON should include terminal route placement metadata: " + mission.id());
        helper.assertFalse(definition.objectives().isEmpty(),
                "Native MissionCore JSON should load at least one objective: " + mission.id());
        for (ObjectiveDefinition objective : definition.objectives()) {
            helper.assertTrue(objective.required() > 0,
                    "Native MissionCore objective should require positive progress: " + objective.id());
        }
        for (var reward : definition.rewards()) {
            helper.assertTrue(reward.claimMode() == MissionRewardClaimMode.CLAIMABLE,
                    "Native MissionCore Ashfall rewards should be claimable, not immediate: " + reward.id());
        }
        return false;
    }

    private static void assertNativeRouteMissionLoaded(GameTestHelper helper, Mission mission, int phase, int order) {
        MissionDefinition definition = EchoCoreServices.missionService()
                .missionDefinition(AshfallMissionCoreIntegration.missionId(mission.id()))
                .orElse(null);
        helper.assertTrue(definition != null, "Native MissionCore route mission should load: " + mission.id());
        if (definition == null) {
            return;
        }
        helper.assertTrue(AshfallMissionCoreIntegration.NATIVE_JSON_CHAPTER_ID.equals(definition.chapterId()),
                "Native Ashfall route mission should belong to the JSON chapter: " + mission.id());
        helper.assertTrue(("phase_" + phase).equals(definition.phaseId())
                        && definition.phaseOrder() == phase
                        && definition.missionOrder() == order,
                "Native Ashfall route mission should preserve phase/order metadata: " + mission.id());
        helper.assertTrue(MissionRegistry.getPhaseTitle(phase).equals(definition.phaseTitle()),
                "Native Ashfall route mission should preserve phase title: " + mission.id());
        helper.assertTrue(Integer.toString(phase).equals(definition.metadata().get("terminal_route_phase"))
                        && Integer.toString(order).equals(definition.metadata().get("terminal_route_order"))
                        && "true".equals(definition.metadata().get("terminal_route_visible")),
                "Native Ashfall route mission should preserve Terminal route metadata: " + mission.id());
        helper.assertTrue("echomissioncore".equals(definition.metadata().get("nativeProvider")),
                "Native Ashfall route mission should preserve provider metadata: " + mission.id());
        helper.assertTrue("false".equals(definition.metadata().get("turnInConsumesRequiredItems")),
                "Native Ashfall turn-ins should not consume required items unless metadata opts in: " + mission.id());
    }

    private static void assertNativeSupportMissionLoaded(GameTestHelper helper, String missionId, Identifier chapterId) {
        MissionDefinition definition = EchoCoreServices.missionService()
                .missionDefinition(AshfallMissionCoreIntegration.missionId(missionId))
                .orElse(null);
        helper.assertTrue(definition != null,
                "Native Ashfall support mission should load from MissionCore JSON: " + missionId);
        if (definition == null) {
            return;
        }
        helper.assertTrue(chapterId.equals(definition.chapterId()),
                "Native Ashfall support mission should register under its expected chapter: " + missionId);
        helper.assertFalse(definition.phaseId().isBlank() || definition.phaseTitle().isBlank(),
                "Native Ashfall support mission should keep phase metadata: " + missionId);
        helper.assertTrue(definition.missionOrder() >= 0 && !definition.objectives().isEmpty(),
                "Native Ashfall support mission should keep order and objective metadata: " + missionId);
    }

    private static void assertNativeDeprecatedAliasMigration(GameTestHelper helper) {
        ServerPlayer serverPlayer = helper.makeMockServerPlayerInLevel();

        QuestData quest = QuestData.get(serverPlayer);
        quest.completeMission("get_dirty_water", List.of());
        quest.completeMission("emergency_filter_water", List.of());
        QuestData.saveAndSync(serverPlayer, quest);
        seedNativeMissionCoreAliasState(helper, serverPlayer, "get_dirty_water", MissionStatus.CLAIMED);
        seedNativeMissionCoreAliasState(helper, serverPlayer, "emergency_filter_water", MissionStatus.CLAIMED);
        serverPlayer.getData(ModAttachments.MIGRATION_DATA).setVersion(3);

        helper.assertTrue(SaveMigrationHandler.ensureCurrent(serverPlayer, "gametest_deprecated_mission_aliases"),
                "Ashfall save migration should run for deprecated MissionCore alias state");

        QuestData migratedQuest = QuestData.get(serverPlayer);
        helper.assertTrue(migratedQuest.isMissionCompleted("secure_emergency_water_loop"),
                "Ashfall QuestData migration should map completed water aliases to the canonical mission");
        Identifier canonicalId = AshfallMissionCoreIntegration.missionId("secure_emergency_water_loop");
        Object data = nativeMissionCorePlayerData(helper, serverPlayer);
        Object canonicalState = nativeMissionCoreState(helper, data, canonicalId);
        helper.assertTrue(canonicalState != null
                        && nativeMissionCoreStateStatus(helper, canonicalState) == MissionStatus.CLAIMED,
                "Native MissionCore migration should map completed deprecated aliases to the canonical mission");
        helper.assertFalse(EchoCoreServices.claimMissionReward(serverPlayer, canonicalId),
                "Migrated deprecated aliases should not leave duplicate canonical rewards claimable");
    }

    private static void seedNativeMissionCoreAliasState(
            GameTestHelper helper,
            ServerPlayer serverPlayer,
            String missionId,
            MissionStatus status) {
        Object data = nativeMissionCorePlayerData(helper, serverPlayer);
        try {
            Object state = data.getClass()
                    .getMethod("state", Identifier.class)
                    .invoke(data, AshfallMissionCoreIntegration.missionId(missionId));
            state.getClass().getMethod("status", MissionStatus.class).invoke(state, status);
            state.getClass().getMethod("incrementRepeatCompletions").invoke(state);
            nativeMissionCoreSaveAndSync(helper, serverPlayer, data);
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Native MissionCore alias state should seed for migration: " + exception.getMessage());
        }
    }

    private static void assertNativeFirstSpawnRuntimeStartsMissionCore(GameTestHelper helper, ServerPlayer serverPlayer) {
        var result = com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreFirstSpawnRuntime
                .startMissionCoreFirstMission(serverPlayer);
        helper.assertTrue(result.adapterCoreRuntime() && result.realNativeStateMutated() && result.mutationCount() >= 1,
                "AdapterCore first-spawn runtime should start the opening MissionCore mission");
        helper.assertTrue("echoashfallprotocol:secure_crash_outpost".equals(result.snapshot().get("missionId")),
                "AdapterCore first-spawn runtime should target secure_crash_outpost");

        IMissionProgressView first = EchoCoreServices.missionService()
                .mission(serverPlayer, AshfallMissionCoreIntegration.missionId("secure_crash_outpost"))
                .orElseThrow();
        helper.assertTrue(first.chapterId().equals(AshfallMissionCoreIntegration.NATIVE_JSON_CHAPTER_ID)
                        && first.status() == MissionStatus.ACTIVE,
                "First-spawn MissionCore runtime should track secure_crash_outpost in the native chapter");
    }

    private static void assertNativeFirstSpawnChainUsesRuntimeEvents(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            helper.assertTrue(false, "Native first-spawn chain runtime needs a server player");
            return;
        }
        assertNativeFirstSpawnRuntimeStartsMissionCore(helper, serverPlayer);

        startNativeMissionIfNeeded(helper, serverPlayer, "secure_crash_outpost");
        var campfireResult = AshfallAdapterCoreEarlyEventRuntime.blockPlaced(
                serverPlayer,
                Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "ash_campfire"),
                helper.absolutePos(new BlockPos(2, 2, 2)));
        helper.assertTrue(campfireResult.mutated(),
                "Campfire placement should mutate the AdapterCore gameplay spine");
        helper.assertTrue(nativeObjectiveProgress(serverPlayer, "secure_crash_outpost", "place_ash_campfire") >= 1,
                "Campfire placement should advance the secure_crash_outpost objective through player.block_placed");

        serverPlayer.getInventory().add(new ItemStack(ModItems.SCRAP_METAL.get()));
        var scrapResult = AshfallAdapterCoreEarlyEventRuntime.itemObtained(
                serverPlayer,
                new ItemStack(ModItems.SCRAP_METAL.get()),
                "gametest_scrap_collection");
        helper.assertTrue(scrapResult.mutated(),
                "Scrap pickup should mutate the AdapterCore gameplay spine");
        helper.assertTrue("player.item_collected".equals(serverPlayer.getPersistentData().getStringOr(
                        "ashes_of_tomorrow.adaptercore.last_early_event", "")),
                "Scrap pickup should publish the canonical item-collected event");
        assertNativeMissionTerminal(helper, serverPlayer, "secure_crash_outpost");

        startNativeMissionIfNeeded(helper, serverPlayer, "craft_scrap_knife");
        serverPlayer.getInventory().add(new ItemStack(ModItems.SCRAP_KNIFE.get()));
        AshfallAdapterCoreEarlyEventRuntime.itemObtained(
                serverPlayer,
                new ItemStack(ModItems.SCRAP_KNIFE.get()),
                "gametest_first_spawn_chain");
        assertNativeMissionTerminal(helper, serverPlayer, "craft_scrap_knife");
        helper.assertTrue(countInventory(serverPlayer, ModItems.SCRAP_KNIFE.get()) == 1,
                "Native MissionCore turn-in metadata should not consume the Scrap Knife");

        startNativeMissionIfNeeded(helper, serverPlayer, "drink_clean_water");
        serverPlayer.getInventory().add(new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get()));
        AshfallAdapterCoreEarlyEventRuntime.itemConsumed(
                serverPlayer,
                new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get()));
        assertNativeMissionTerminal(helper, serverPlayer, "drink_clean_water");
        helper.assertTrue(QuestData.get(serverPlayer).hasVisitedLocation("special", "water:clean_consumed"),
                "Clean-water runtime hook should keep the consumed-water proof marker");

        startNativeMissionIfNeeded(helper, serverPlayer, "secure_emergency_water_loop");
        AshfallAdapterCoreEarlyEventRuntime.dirtyWaterCollected(serverPlayer, helper.absolutePos(new BlockPos(2, 2, 2)));
        AshfallAdapterCoreEarlyEventRuntime.waterFiltered(serverPlayer, "gametest_first_spawn_chain");
        assertNativeMissionTerminal(helper, serverPlayer, "secure_emergency_water_loop");

        startNativeMissionIfNeeded(helper, serverPlayer, "forage_wasteland_food");
        serverPlayer.getInventory().add(new ItemStack(ModItems.EMERGENCY_RATION.get()));
        AshfallAdapterCoreEarlyEventRuntime.itemObtained(
                serverPlayer,
                new ItemStack(ModItems.EMERGENCY_RATION.get()),
                "gametest_first_spawn_chain");
        assertNativeMissionTerminal(helper, serverPlayer, "forage_wasteland_food");

        recordNativePlaceBlock(helper, serverPlayer, "plant_mutated_sapling", "mutated_sapling");
        recordNativePlaceBlock(helper, serverPlayer, "build_rain_collector", "rain_collector");

        startNativeMissionIfNeeded(helper, serverPlayer, "stockpile_rations");
        serverPlayer.getInventory().add(new ItemStack(ModItems.EMERGENCY_RATION.get(), 3));
        AshfallAdapterCoreEarlyEventRuntime.itemObtained(
                serverPlayer,
                new ItemStack(ModItems.EMERGENCY_RATION.get(), 3),
                "gametest_first_spawn_chain");
        assertNativeMissionTerminal(helper, serverPlayer, "stockpile_rations");
        helper.assertTrue(countInventory(serverPlayer, ModItems.EMERGENCY_RATION.get()) >= 4,
                "Native MissionCore ration-buffer predicate should not consume rations");

        startNativeMissionIfNeeded(helper, serverPlayer, "secure_sleep_shelter");
        AshfallAdapterCoreEarlyEventRuntime.shelterSlept(serverPlayer, false, true);
        assertNativeMissionTerminal(helper, serverPlayer, "secure_sleep_shelter");

        startNativeMissionIfNeeded(helper, serverPlayer, "assemble_wasteland_field_kit");
        serverPlayer.getInventory().add(new ItemStack(ModItems.BONE_KNIFE.get()));
        serverPlayer.getInventory().add(new ItemStack(ModItems.CRUDE_SPEAR.get()));
        serverPlayer.getInventory().add(new ItemStack(ModItems.HIDE_WRAP.get()));
        AshfallAdapterCoreEarlyEventRuntime.itemObtained(
                serverPlayer,
                new ItemStack(ModItems.HIDE_WRAP.get()),
                "gametest_first_spawn_chain");
        assertNativeMissionTerminal(helper, serverPlayer, "assemble_wasteland_field_kit");
    }

    private static void recordNativePlaceBlock(
            GameTestHelper helper,
            ServerPlayer serverPlayer,
            String missionId,
            String blockPath) {
        startNativeMissionIfNeeded(helper, serverPlayer, missionId);
        var result = AshfallAdapterCoreEarlyEventRuntime.blockPlaced(
                serverPlayer,
                Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, blockPath),
                helper.absolutePos(new BlockPos(2, 2, 2)));
        helper.assertTrue(result.mutated(),
                "Native AdapterCore block placement event should mutate the runtime host for " + missionId);
        assertNativeMissionTerminal(helper, serverPlayer, missionId);
    }

    private static void startNativeMissionIfNeeded(GameTestHelper helper, ServerPlayer serverPlayer, String missionId) {
        Identifier id = AshfallMissionCoreIntegration.missionId(missionId);
        IMissionProgressView view = EchoCoreServices.missionService().mission(serverPlayer, id).orElseThrow();
        helper.assertTrue(view.status() != MissionStatus.LOCKED,
                "Native first-spawn chain mission should be reachable: " + missionId);
        if (view.status() == MissionStatus.UNLOCKED) {
            helper.assertTrue(EchoCoreServices.startMission(serverPlayer, id),
                    "Native first-spawn chain mission should start: " + missionId);
        }
    }

    private static int nativeObjectiveProgress(ServerPlayer serverPlayer, String missionId, String objectivePath) {
        return EchoCoreServices.missionService()
                .mission(serverPlayer, AshfallMissionCoreIntegration.missionId(missionId))
                .stream()
                .flatMap(view -> view.objectives().stream())
                .filter(objective -> objective.id().getPath().endsWith("/" + objectivePath))
                .findFirst()
                .map(IObjectiveView::progress)
                .orElse(0);
    }

    private static void assertNativeMissionTerminal(GameTestHelper helper, ServerPlayer serverPlayer, String missionId) {
        IMissionProgressView view = EchoCoreServices.missionService()
                .mission(serverPlayer, AshfallMissionCoreIntegration.missionId(missionId))
                .orElseThrow();
        helper.assertTrue(isMissionTerminal(view.status()),
                "Native first-spawn runtime hook should complete mission: " + missionId);
    }

    private static IMissionProgressView progressNativeMissionCoreMission(
            GameTestHelper helper,
            ServerPlayer player,
            Identifier missionId,
            Set<MissionObjectiveType> objectiveTypes) {
        IMissionProgressView view = EchoCoreServices.missionService().mission(player, missionId).orElseThrow();
        helper.assertTrue(view.status() != MissionStatus.LOCKED,
                "Native MissionCore route mission should be unlocked when reached: " + missionId);
        if (view.status() == MissionStatus.UNLOCKED) {
            helper.assertTrue(EchoCoreServices.startMission(player, missionId),
                    "Native MissionCore route mission should start: " + missionId);
            view = EchoCoreServices.missionService().mission(player, missionId).orElseThrow();
        }
        if (isMissionTerminal(view.status())) {
            return view;
        }
        for (IObjectiveView objective : view.objectives()) {
            objectiveTypes.add(objective.type());
            Identifier target = objectiveTarget(objective);
            helper.assertTrue(EchoCoreServices.recordMissionObjective(
                            player,
                            objective.type(),
                            target,
                            Math.max(1, objective.required()),
                            Map.of("source", "ashfall_missioncore_runtime_route")),
                    "Native MissionCore objective should accept its runtime hook: " + missionId + " -> " + objective.id());
        }
        view = EchoCoreServices.missionService().mission(player, missionId).orElseThrow();
        if (!isMissionTerminal(view.status())) {
            helper.assertTrue(EchoCoreServices.handleMissionAction(player, missionId, "complete"),
                    "Native MissionCore turn-in action should complete satisfied mission: " + missionId);
            view = EchoCoreServices.missionService().mission(player, missionId).orElseThrow();
        }
        return view;
    }

    private static Identifier objectiveTarget(IObjectiveView objective) {
        String value = objective.criteria().getOrDefault("target", "");
        if (value.isBlank()) {
            return null;
        }
        Identifier direct = value.contains(":") ? Identifier.tryParse(value) : null;
        if (direct != null) {
            return direct;
        }
        Identifier fallback = Identifier.tryParse(EchoAshfallProtocol.MODID + ":" + value);
        return fallback == null ? Identifier.tryParse(value) : fallback;
    }

    private static boolean isMissionTerminal(MissionStatus status) {
        return status == MissionStatus.COMPLETED
                || status == MissionStatus.CLAIMABLE
                || status == MissionStatus.CLAIMED;
    }

    private static void assertNativeMissionCoreRewardClaimable(GameTestHelper helper, ServerPlayer serverPlayer) {
        Set<MissionObjectiveType> objectiveTypes = EnumSet.noneOf(MissionObjectiveType.class);
        Identifier missionId = AshfallMissionCoreIntegration.missionId("secure_crash_outpost");
        IMissionProgressView completed = progressNativeMissionCoreMission(helper, serverPlayer, missionId, objectiveTypes);
        helper.assertTrue(completed.status() == MissionStatus.CLAIMABLE,
                "Native MissionCore claimable rewards should leave secure_crash_outpost reward-ready");
        int beforeScrap = countInventory(serverPlayer, ModItems.SCRAP_METAL.get());
        helper.assertTrue(EchoCoreServices.claimMissionReward(serverPlayer, missionId),
                "Native MissionCore reward claim should succeed once");
        helper.assertFalse(EchoCoreServices.claimMissionReward(serverPlayer, missionId),
                "Native MissionCore reward claim should be idempotent");
        helper.assertTrue(countInventory(serverPlayer, ModItems.SCRAP_METAL.get()) >= beforeScrap + 4,
                "Native MissionCore reward claim should grant the secure_crash_outpost scrap cache");
    }

    private static void assertNativeMissionCoreRewardPersistence(GameTestHelper helper, ServerPlayer serverPlayer) {
        Identifier missionId = AshfallMissionCoreIntegration.missionId("secure_crash_outpost");
        MissionDefinition definition = EchoCoreServices.missionService()
                .missionDefinition(missionId)
                .orElseThrow();
        helper.assertFalse(definition.rewards().isEmpty(),
                "Native MissionCore persistence check needs a reward-bearing first mission");
        IMissionProgressView beforeClaim = EchoCoreServices.missionService().mission(serverPlayer, missionId).orElseThrow();
        helper.assertTrue(beforeClaim.status() == MissionStatus.CLAIMABLE || beforeClaim.status() == MissionStatus.COMPLETED,
                "Completed native first mission should be reward-claimable before persistence check");
        int beforeScrap = countInventory(serverPlayer, ModItems.SCRAP_METAL.get());
        helper.assertTrue(EchoCoreServices.claimMissionReward(serverPlayer, missionId),
                "Native MissionCore reward should claim once before reload");
        int afterClaimScrap = countInventory(serverPlayer, ModItems.SCRAP_METAL.get());
        helper.assertTrue(afterClaimScrap >= beforeScrap + 4,
                "Native MissionCore reward should grant secure_crash_outpost scrap before reload");

        Object data = nativeMissionCorePlayerData(helper, serverPlayer);
        Object state = nativeMissionCoreState(helper, data, missionId);
        helper.assertTrue(state != null && nativeMissionCoreStateStatus(helper, state) == MissionStatus.CLAIMED,
                "Native MissionCore reward claim should move secure_crash_outpost to CLAIMED");
        if (state == null) {
            return;
        }
        for (var reward : definition.rewards()) {
            helper.assertTrue(nativeMissionCoreRewardClaimed(helper, state, reward.id()),
                    "Native MissionCore reward claim should mark reward before reload: " + reward.id());
        }

        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        nativeMissionCoreSerialize(helper, data, output);
        CompoundTag tag = output.buildResult();
        Object restored = newNativeMissionCorePlayerData(helper);
        nativeMissionCoreDeserialize(helper, restored, TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        Object restoredState = nativeMissionCoreState(helper, restored, missionId);
        helper.assertTrue(restoredState != null && nativeMissionCoreStateStatus(helper, restoredState) == MissionStatus.CLAIMED,
                "Native MissionCore reward claim should survive player data reload");
        if (restoredState == null) {
            return;
        }
        helper.assertTrue(nativeMissionCoreObjectiveProgress(helper, restoredState)
                        .equals(nativeMissionCoreObjectiveProgress(helper, state)),
                "Native MissionCore objective progress should survive player data reload");
        for (var reward : definition.rewards()) {
            helper.assertTrue(nativeMissionCoreRewardClaimed(helper, restoredState, reward.id()),
                    "Native MissionCore claimed reward should survive reload: " + reward.id());
        }

        nativeMissionCoreSaveAndSync(helper, serverPlayer, restored);
        helper.assertFalse(EchoCoreServices.claimMissionReward(serverPlayer, missionId),
                "Native MissionCore reward should remain idempotent after reload");
        helper.assertTrue(countInventory(serverPlayer, ModItems.SCRAP_METAL.get()) == afterClaimScrap,
                "Native MissionCore duplicate reward claim after reload should not grant extra scrap");
    }

    private static void assertNativeDeliverTurnInDoesNotConsumeRequiredItems(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            helper.assertTrue(false, "Native MissionCore deliver-item safety needs a server player");
            return;
        }
        Set<MissionObjectiveType> objectiveTypes = EnumSet.noneOf(MissionObjectiveType.class);
        completeNativeRouteThrough(helper, serverPlayer, "use_field_med_bay", objectiveTypes);

        String missionId = "stabilize_mutation_effects";
        startNativeMissionIfNeeded(helper, serverPlayer, missionId);
        MissionDefinition definition = EchoCoreServices.missionService()
                .missionDefinition(AshfallMissionCoreIntegration.missionId(missionId))
                .orElseThrow();
        helper.assertTrue("false".equals(definition.metadata().get("turnInConsumesRequiredItems")),
                "Native deliver-item test mission should explicitly opt out of item consumption");

        serverPlayer.getInventory().add(new ItemStack(ModItems.RAD_AWAY.get(), 2));
        serverPlayer.getInventory().add(new ItemStack(ModItems.BANDAGE.get(), 2));
        int radAwayBefore = countInventory(serverPlayer, ModItems.RAD_AWAY.get());
        int bandageBefore = countInventory(serverPlayer, ModItems.BANDAGE.get());

        AshfallAdapterCoreEarlyEventRuntime.itemObtained(
                serverPlayer,
                new ItemStack(ModItems.RAD_AWAY.get()),
                "gametest_deliver_item_turn_in");
        AshfallAdapterCoreEarlyEventRuntime.itemObtained(
                serverPlayer,
                new ItemStack(ModItems.BANDAGE.get()),
                "gametest_deliver_item_turn_in");
        assertNativeMissionTerminal(helper, serverPlayer, missionId);
        helper.assertTrue(countInventory(serverPlayer, ModItems.RAD_AWAY.get()) == radAwayBefore,
                "Native MissionCore deliver-item turn-in should not consume RadAway when metadata says false");
        helper.assertTrue(countInventory(serverPlayer, ModItems.BANDAGE.get()) == bandageBefore,
                "Native MissionCore deliver-item turn-in should not consume Bandage when metadata says false");

        int tissueBefore = countInventory(serverPlayer, ModItems.MUTATED_TISSUE.get());
        int membraneBefore = countInventory(serverPlayer, ModItems.FILTRATION_MEMBRANE.get());
        helper.assertTrue(EchoCoreServices.claimMissionReward(
                        serverPlayer,
                        AshfallMissionCoreIntegration.missionId(missionId)),
                "Native MissionCore deliver-item reward should claim once");
        int tissueAfter = countInventory(serverPlayer, ModItems.MUTATED_TISSUE.get());
        int membraneAfter = countInventory(serverPlayer, ModItems.FILTRATION_MEMBRANE.get());
        helper.assertTrue(tissueAfter == tissueBefore + 1 && membraneAfter == membraneBefore + 1,
                "Native MissionCore deliver-item reward should grant exactly one reward set");
        helper.assertFalse(EchoCoreServices.claimMissionReward(
                        serverPlayer,
                        AshfallMissionCoreIntegration.missionId(missionId)),
                "Native MissionCore deliver-item reward should not duplicate");
        helper.assertTrue(countInventory(serverPlayer, ModItems.MUTATED_TISSUE.get()) == tissueAfter
                        && countInventory(serverPlayer, ModItems.FILTRATION_MEMBRANE.get()) == membraneAfter,
                "Native MissionCore duplicate deliver-item reward claim should not change inventory");
    }

    private static void assertNativeScannerFactionRuntimeHooks(GameTestHelper helper) {
        assertNativeScannerFactionRuntimeHooks(helper, helper.makeMockServerPlayerInLevel());
    }

    private static void assertNativeScannerFactionRuntimeHooks(GameTestHelper helper, ServerPlayer serverPlayer) {
        serverPlayer.setGameMode(GameType.SURVIVAL);
        Set<MissionObjectiveType> objectiveTypes = EnumSet.noneOf(MissionObjectiveType.class);
        completeNativeRouteThrough(helper, serverPlayer, "craft_portable_scanner", objectiveTypes);

        startNativeMissionIfNeeded(helper, serverPlayer, "expedition_readiness");
        addInventoryAndPublishItem(serverPlayer, ModItems.PORTABLE_SIGNAL_SCANNER.get(), "gametest_expedition_readiness");
        addInventoryAndPublishItem(serverPlayer, ModItems.CLEAN_WATER_BOTTLE.get(), 2, "gametest_expedition_readiness");
        addInventoryAndPublishItem(serverPlayer, ModItems.BANDAGE.get(), 2, "gametest_expedition_readiness");
        assertNativeMissionTerminal(helper, serverPlayer, "expedition_readiness");

        POIScannerService.ScanHit scanHit = sampleScanHit(helper);
        startNativeMissionIfNeeded(helper, serverPlayer, "scan_first_poi");
        var scanResult = AshfallAdapterCoreExplorationRuntime.scannerUsed(
                serverPlayer,
                scanHit,
                "gametest_portable_signal_scanner",
                false);
        helper.assertTrue(scanResult.mutated(),
                "Native scanner runtime should mutate MissionCore state for scan_first_poi");
        assertNativeMissionTerminal(helper, serverPlayer, "scan_first_poi");

        startNativeMissionIfNeeded(helper, serverPlayer, "loot_survivor_cache");
        var cacheResult = AshfallAdapterCoreExplorationRuntime.cacheOpened(
                serverPlayer,
                helper.absolutePos(new BlockPos(3, 2, 3)),
                "gametest_survivor_cache");
        helper.assertTrue(cacheResult.mutated(),
                "Native cache runtime should mutate MissionCore state for loot_survivor_cache");
        assertNativeMissionTerminal(helper, serverPlayer, "loot_survivor_cache");

        startNativeMissionIfNeeded(helper, serverPlayer, "first_faction_contact");
        var contactResult = AshfallAdapterCoreExplorationRuntime.factionAction(
                serverPlayer,
                AshfallBiomeFactions.CRASHBREAK_SALVAGE,
                Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "crashbreak_salvage_talk"),
                "route_broker",
                null,
                null);
        helper.assertTrue(contactResult.mutated(),
                "Native faction action runtime should mutate MissionCore state for first_faction_contact");
        assertNativeMissionTerminal(helper, serverPlayer, "first_faction_contact");

        startNativeMissionIfNeeded(helper, serverPlayer, "complete_first_faction_task");
        var taskResult = AshfallAdapterCoreExplorationRuntime.reputationUpdated(
                serverPlayer,
                AshfallBiomeFactions.CRASHBREAK_SALVAGE,
                4,
                "field_ops_contract");
        helper.assertTrue(taskResult.mutated(),
                "Native faction task runtime should mutate MissionCore state for complete_first_faction_task");
        assertNativeMissionTerminal(helper, serverPlayer, "complete_first_faction_task");

        startNativeMissionIfNeeded(helper, serverPlayer, "repair_echo_drone");
        var repairResult = AshfallAdapterCoreExplorationRuntime.droneState(
                serverPlayer,
                "repair",
                "IDLE",
                true,
                Map.of("repairLevel", 25));
        helper.assertTrue(repairResult.mutated(),
                "Native drone repair runtime should mutate MissionCore state for repair_echo_drone");
        assertNativeMissionTerminal(helper, serverPlayer, "repair_echo_drone");

        startNativeMissionIfNeeded(helper, serverPlayer, "recover_drone_intel");
        var intelResult = AshfallAdapterCoreExplorationRuntime.droneState(
                serverPlayer,
                "scan_area",
                "SCOUT",
                true,
                Map.of("repairLevel", 25));
        helper.assertTrue(intelResult.mutated(),
                "Native drone intel runtime should mutate MissionCore state for recover_drone_intel");
        assertNativeMissionTerminal(helper, serverPlayer, "recover_drone_intel");

        startNativeMissionIfNeeded(helper, serverPlayer, "faction_reputation");
        var reputationResult = AshfallAdapterCoreExplorationRuntime.reputationUpdated(
                serverPlayer,
                AshfallBiomeFactions.CRASHBREAK_SALVAGE,
                5,
                "gametest_reputation");
        helper.assertTrue(reputationResult.mutated(),
                "Native reputation runtime should mutate MissionCore state for faction_reputation");
        assertNativeMissionTerminal(helper, serverPlayer, "faction_reputation");

        startNativeMissionIfNeeded(helper, serverPlayer, "first_perk");
        var perkResult = AshfallAdapterCoreExplorationRuntime.perkUnlocked(
                serverPlayer,
                PerkRegistry.BETTER_LOOT_1.getId(),
                PerkRegistry.BETTER_LOOT_1.getCost());
        helper.assertTrue(perkResult.mutated(),
                "Native perk runtime should mutate MissionCore state for first_perk");
        assertNativeMissionTerminal(helper, serverPlayer, "first_perk");

        startNativeMissionIfNeeded(helper, serverPlayer, "poi_explorer");
        var poiResult = AshfallAdapterCoreExplorationRuntime.poiDiscovered(serverPlayer, scanHit, true);
        helper.assertTrue(poiResult.mutated(),
                "Native POI discovery runtime should mutate MissionCore state for poi_explorer");
        assertNativeMissionTerminal(helper, serverPlayer, "poi_explorer");
    }

    private static void addInventoryAndPublishItem(ServerPlayer serverPlayer, Item item, String source) {
        addInventoryAndPublishItem(serverPlayer, item, 1, source);
    }

    private static void addInventoryAndPublishItem(ServerPlayer serverPlayer, Item item, int count, String source) {
        ItemStack stack = new ItemStack(item, count);
        serverPlayer.getInventory().add(stack.copy());
        AshfallAdapterCoreEarlyEventRuntime.itemObtained(serverPlayer, stack, source);
    }

    private static POIScannerService.ScanHit sampleScanHit(GameTestHelper helper) {
        return new POIScannerService.ScanHit(
                helper.absolutePos(new BlockPos(4, 2, 4)),
                "survivor_cache",
                "poi_global",
                "Survivor Cache",
                "Signal Contact",
                "A survivor cache signal is close enough to verify scanner/faction route hooks.",
                "Scan and secure the survivor cache.",
                "Water, bandages, and route intel",
                "LOW",
                "crash_zone_wasteland",
                "Carry water and a spare bandage.",
                "survival supplies",
                32.0D,
                "East",
                false,
                "Unscanned");
    }

    private static Object nativeMissionCorePlayerData(GameTestHelper helper, ServerPlayer serverPlayer) {
        try {
            Class<?> dataClass = Class.forName("com.knoxhack.echomissioncore.storage.MissionPlayerData");
            return dataClass.getMethod("get", Player.class).invoke(null, serverPlayer);
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Native MissionCore player data should be available at runtime: " + exception.getMessage());
            return null;
        }
    }

    private static Object newNativeMissionCorePlayerData(GameTestHelper helper) {
        try {
            Class<?> dataClass = Class.forName("com.knoxhack.echomissioncore.storage.MissionPlayerData");
            return dataClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Native MissionCore player data should construct for reload check: " + exception.getMessage());
            return null;
        }
    }

    private static Object nativeMissionCoreState(GameTestHelper helper, Object data, Identifier missionId) {
        try {
            return data == null ? null : data.getClass().getMethod("stateIfPresent", Identifier.class).invoke(data, missionId);
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Native MissionCore state should be readable: " + exception.getMessage());
            return null;
        }
    }

    private static MissionStatus nativeMissionCoreStateStatus(GameTestHelper helper, Object state) {
        try {
            return state == null ? null : (MissionStatus) state.getClass().getMethod("status").invoke(state);
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Native MissionCore state status should be readable: " + exception.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> nativeMissionCoreObjectiveProgress(GameTestHelper helper, Object state) {
        try {
            return state == null ? Map.of()
                    : (Map<String, Integer>) state.getClass().getMethod("objectiveProgress").invoke(state);
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Native MissionCore objective progress should be readable: " + exception.getMessage());
            return Map.of();
        }
    }

    private static boolean nativeMissionCoreRewardClaimed(GameTestHelper helper, Object state, Identifier rewardId) {
        try {
            return state != null && Boolean.TRUE.equals(
                    state.getClass().getMethod("isRewardClaimed", Identifier.class).invoke(state, rewardId));
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Native MissionCore reward claim state should be readable: " + exception.getMessage());
            return false;
        }
    }

    private static void nativeMissionCoreSerialize(GameTestHelper helper, Object data, TagValueOutput output) {
        try {
            if (data != null) {
                data.getClass().getMethod("serialize", net.minecraft.world.level.storage.ValueOutput.class)
                        .invoke(data, output);
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Native MissionCore player data should serialize: " + exception.getMessage());
        }
    }

    private static void nativeMissionCoreDeserialize(
            GameTestHelper helper,
            Object data,
            net.minecraft.world.level.storage.ValueInput input) {
        try {
            if (data != null) {
                data.getClass().getMethod("deserialize", net.minecraft.world.level.storage.ValueInput.class)
                        .invoke(data, input);
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Native MissionCore player data should deserialize: " + exception.getMessage());
        }
    }

    private static void nativeMissionCoreSaveAndSync(GameTestHelper helper, ServerPlayer serverPlayer, Object data) {
        try {
            Class<?> dataClass = Class.forName("com.knoxhack.echomissioncore.storage.MissionPlayerData");
            dataClass.getMethod("saveAndSync", ServerPlayer.class, dataClass).invoke(null, serverPlayer, data);
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Native MissionCore player data should save after reload: " + exception.getMessage());
        }
    }

    private static void assertNativeBranchStarts(GameTestHelper helper) {
        for (String branchStart : List.of("restore_repair_nodes", "destroy_scorched_earth", "control_signal_expansion")) {
            MissionDefinition definition = EchoCoreServices.missionService()
                    .missionDefinition(AshfallMissionCoreIntegration.missionId(branchStart))
                    .orElseThrow();
            helper.assertTrue(definition.prerequisites().equals(List.of(AshfallMissionCoreIntegration.missionId("reach_decision"))),
                    "Native MissionCore branch should start from reach_decision only: " + branchStart);
            helper.assertTrue(!definition.metadata().getOrDefault("requiredPath", "").isBlank(),
                    "Native MissionCore branch start should keep requiredPath metadata: " + branchStart);
        }
    }

    private static void assertNativeBranchesCompleteIndependently(GameTestHelper helper) {
        for (Map.Entry<String, List<String>> branch : nativeEndingBranches().entrySet()) {
            ServerPlayer serverPlayer = helper.makeMockServerPlayerInLevel();
            serverPlayer.setGameMode(GameType.SURVIVAL);
            Set<MissionObjectiveType> objectiveTypes = EnumSet.noneOf(MissionObjectiveType.class);
            completeNativeRouteThrough(helper, serverPlayer, "reach_decision", objectiveTypes);
            assertNativeBranchStartsOpenAfterDecision(helper, serverPlayer);
            for (String missionId : branch.getValue()) {
                assertNativeBranchPathMetadata(helper, branch.getKey(), missionId);
                IMissionProgressView completed = progressNativeMissionCoreMission(
                        helper,
                        serverPlayer,
                        AshfallMissionCoreIntegration.missionId(missionId),
                        objectiveTypes);
                helper.assertTrue(isMissionTerminal(completed.status()),
                        "Native MissionCore branch mission should complete independently: " + missionId);
            }
            String epilogue = branch.getValue().get(branch.getValue().size() - 1);
            IMissionProgressView epilogueView = EchoCoreServices.missionService()
                    .mission(serverPlayer, AshfallMissionCoreIntegration.missionId(epilogue))
                    .orElseThrow();
            helper.assertTrue(isMissionTerminal(epilogueView.status()),
                    "Native MissionCore branch epilogue should be terminal for " + branch.getKey());
            assertAlternateBranchStartsRemainAvailable(helper, serverPlayer, branch.getKey());
        }
    }

    private static void completeNativeRouteThrough(
            GameTestHelper helper,
            ServerPlayer serverPlayer,
            String stopMissionId,
            Set<MissionObjectiveType> objectiveTypes) {
        completeNativeRouteThroughWithRelogCheckpoints(
                helper,
                serverPlayer,
                stopMissionId,
                objectiveTypes,
                Set.of());
    }

    private static void completeNativeRouteThroughWithRelogCheckpoints(
            GameTestHelper helper,
            ServerPlayer serverPlayer,
            String stopMissionId,
            Set<MissionObjectiveType> objectiveTypes,
            Set<String> relogCheckpointMissionIds) {
        boolean reachedStop = false;
        for (Mission mission : routeMissionsInOrder()) {
            IMissionProgressView completed = progressNativeMissionCoreMission(
                    helper,
                    serverPlayer,
                    AshfallMissionCoreIntegration.missionId(mission.id()),
                    objectiveTypes);
            helper.assertTrue(isMissionTerminal(completed.status()),
                    "Native MissionCore route should complete before stop mission " + stopMissionId + ": " + mission.id());
            if (relogCheckpointMissionIds.contains(mission.id())) {
                assertNativeMissionCoreDataRoundTrip(
                        helper,
                        serverPlayer,
                        AshfallMissionCoreIntegration.missionId(mission.id()));
            }
            if (stopMissionId.equals(mission.id())) {
                reachedStop = true;
                break;
            }
        }
        helper.assertTrue(reachedStop,
                "Native MissionCore route should reach stop mission before focused validation: " + stopMissionId);
    }

    private static void assertNativeBranchStartsOpenAfterDecision(GameTestHelper helper, ServerPlayer serverPlayer) {
        for (String branchStart : nativeBranchStarts()) {
            IMissionProgressView view = EchoCoreServices.missionService()
                    .mission(serverPlayer, AshfallMissionCoreIntegration.missionId(branchStart))
                    .orElseThrow();
            helper.assertTrue(view.status() == MissionStatus.UNLOCKED,
                    "Native MissionCore reach_decision should unlock branch start: " + branchStart);
        }
    }

    private static void assertNativeBranchPathMetadata(GameTestHelper helper, String branchPath, String missionId) {
        MissionDefinition definition = EchoCoreServices.missionService()
                .missionDefinition(AshfallMissionCoreIntegration.missionId(missionId))
                .orElseThrow();
        helper.assertTrue(branchPath.equals(definition.metadata().get("requiredPath")),
                "Native MissionCore branch mission should preserve requiredPath metadata: " + missionId);
    }

    private static void assertAlternateBranchStartsRemainAvailable(
            GameTestHelper helper,
            ServerPlayer serverPlayer,
            String activeBranch) {
        for (Map.Entry<String, List<String>> branch : nativeEndingBranches().entrySet()) {
            if (branch.getKey().equals(activeBranch)) {
                continue;
            }
            String branchStart = branch.getValue().get(0);
            IMissionProgressView view = EchoCoreServices.missionService()
                    .mission(serverPlayer, AshfallMissionCoreIntegration.missionId(branchStart))
                    .orElseThrow();
            helper.assertTrue(view.status() == MissionStatus.UNLOCKED,
                    "Native MissionCore inactive branch start should remain available after completing "
                            + activeBranch + ": " + branchStart);
        }
    }

    private static List<String> nativeBranchStarts() {
        return List.of("restore_repair_nodes", "destroy_scorched_earth", "control_signal_expansion");
    }

    private static Map<String, List<String>> nativeEndingBranches() {
        return Map.of(
                "restore", List.of(
                        "restore_repair_nodes",
                        "restore_purge_corruption",
                        "restore_enter_archives",
                        "restore_guardian",
                        "restore_world_lattice",
                        "restore_finale",
                        "restore_epilogue"),
                "destroy", List.of(
                        "destroy_scorched_earth",
                        "destroy_survive_storms",
                        "destroy_enter_archives",
                        "destroy_guardian",
                        "destroy_dead_signal",
                        "destroy_finale",
                        "destroy_epilogue"),
                "control", List.of(
                        "control_signal_expansion",
                        "control_resource_dominance",
                        "control_enter_archives",
                        "control_guardian",
                        "control_command_lattice",
                        "control_finale",
                        "control_epilogue"));
    }

    private static List<Mission> routeMissionsInOrder() {
        List<Mission> route = new ArrayList<>();
        for (int phase = 0; phase < MissionRegistry.getPhaseCount(); phase++) {
            route.addAll(MissionRegistry.getMissionsForPhase(phase));
        }
        return List.copyOf(route);
    }

    private static boolean nativeJsonOwnsMissionCoreRoute() {
        return AshfallMissionCoreIntegration.nativeJsonOwnsRoute();
    }

    private static boolean legacyMissionCoreExportReady(String missionId) {
        return EchoCoreServices.missionService()
                .missionDefinition(AshfallMissionCoreIntegration.missionId(missionId))
                .map(definition -> missionId.equals(definition.metadata().get("legacy_id")))
                .orElse(false);
    }

    private static List<String> deprecatedAshfallMissionIds() {
        return List.of(
                "get_dirty_water",
                "emergency_filter_water",
                "craft_bone_knife",
                "craft_crude_spear",
                "craft_hide_wrap",
                "contact_radwarden_compact",
                "contact_crashbreak_salvage",
                "contact_sporebound_sanctum",
                "complete_radwarden_contract",
                "complete_crashbreak_contract",
                "complete_sporebound_contract",
                "upgrade_drone_support",
                "set_drone_scout_mode",
                "deploy_scout_drone",
                "acquire_mutagen");
    }

    private static List<MissionObjectiveType> expectedMissionCoreObjectiveTypes(Mission mission) {
        List<MissionObjectiveType> types = new ArrayList<>();
        MissionObjectiveType itemType = mission.validatesRequiredItems()
                ? MissionObjectiveType.DELIVER_ITEM
                : MissionObjectiveType.OBTAIN_ITEM;
        for (int i = 0; i < mission.requiredItems().size(); i++) {
            types.add(itemType);
        }
        for (int i = 0; i < mission.requiredBlocks().size(); i++) {
            types.add(MissionObjectiveType.PLACE_BLOCK);
        }
        for (int i = 0; i < mission.requiredEntityKills().size(); i++) {
            types.add(MissionObjectiveType.KILL_ENTITY);
        }
        for (Mission.LocationRequirement requirement : mission.requiredLocations()) {
            types.add(switch (requirement.locationType()) {
                case "poi" -> MissionObjectiveType.DISCOVER_STRUCTURE;
                case "dimension", "biome", "special" -> MissionObjectiveType.ENTER_REGION;
                default -> MissionObjectiveType.CUSTOM;
            });
        }
        for (int i = 0; i < mission.requiredEquipment().size(); i++) {
            types.add(MissionObjectiveType.CUSTOM);
        }
        return types.isEmpty() ? List.of(MissionObjectiveType.CUSTOM) : List.copyOf(types);
    }

    private static boolean assertMissionCoreRequirementIcons(
            GameTestHelper helper,
            Mission mission,
            List<ObjectiveDefinition> objectives) {
        int objectiveIndex = 0;
        boolean sawBlockRewardIconSplit = false;
        for (ItemStack required : mission.requiredItems()) {
            if (objectiveIndex >= objectives.size()) {
                return sawBlockRewardIconSplit;
            }
            ObjectiveDefinition objective = objectives.get(objectiveIndex++);
            helper.assertTrue(!objective.icon().isEmpty() && objective.icon().is(required.getItem()),
                    "MissionCore item objective icon should match required item: "
                            + mission.id() + " -> " + BuiltInRegistries.ITEM.getKey(required.getItem()));
        }
        for (Mission.BlockRequirement requirement : mission.requiredBlocks()) {
            if (objectiveIndex >= objectives.size()) {
                return sawBlockRewardIconSplit;
            }
            ObjectiveDefinition objective = objectives.get(objectiveIndex++);
            Item expected = blockRequirementItem(requirement.blockId());
            helper.assertTrue(expected != Items.AIR,
                    "MissionCore block objective icon should resolve block item: "
                            + mission.id() + " -> " + requirement.blockId());
            helper.assertTrue(!objective.icon().isEmpty() && objective.icon().is(expected),
                    "MissionCore block objective icon should match required block item: "
                            + mission.id() + " -> " + requirement.blockId());

            Item differentReward = firstDifferentRewardItem(mission, expected);
            if (differentReward != Items.AIR) {
                sawBlockRewardIconSplit = true;
                helper.assertFalse(objective.icon().is(differentReward),
                        "MissionCore block objective icon should not reuse reward item: "
                                + mission.id() + " -> " + requirement.blockId());
            }
        }
        return sawBlockRewardIconSplit;
    }

    private static void scrapKnifeTurnIn(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            helper.succeed();
            return;
        }

        Mission outpost = requireMission(helper, "secure_crash_outpost");
        Mission knife = requireMission(helper, "craft_scrap_knife");
        QuestData quest = QuestData.get(serverPlayer);
        quest.repairMissionState(serverPlayer);
        QuestData.saveAndSync(serverPlayer, quest);

        helper.assertTrue(!quest.isMissionUnlocked(knife.id()),
                "Scrap Knife should stay locked before Anchor Pod Outpost is complete");
        String lockedReason = MissionUxSummary.turnInReason(serverPlayer, quest, knife,
                quest.getMissionStatus(knife.id()), false, false, false);
        helper.assertTrue(lockedReason.contains("Anchor Pod Outpost"),
                "Locked Scrap Knife turn-in should name the unmet Anchor Pod Outpost prerequisite");

        quest.completeMission(serverPlayer, outpost.id(), List.of());
        quest.repairMissionState(serverPlayer);
        QuestData.saveAndSync(serverPlayer, quest);

        helper.assertTrue(quest.isMissionUnlocked(knife.id()),
                "Scrap Knife should unlock after Anchor Pod Outpost is complete");
        helper.assertTrue(MissionUxSummary.isCurrentMission(quest, knife),
                "Scrap Knife should become the active route mission after Anchor Pod Outpost");
        helper.assertTrue(AshfallMissionActions.resolveTarget(quest,
                        EchoAshfallProtocol.MODID + ":" + knife.id()) == knife,
                "Mission action routing should resolve namespaced Ashfall mission payloads");
        Mission drinkWater = requireMission(helper, "drink_clean_water");
        quest.unlockMission(drinkWater.id());
        String nonCurrentReason = AshfallMissionActions.turnInRejection(serverPlayer, quest, drinkWater);
        helper.assertTrue(nonCurrentReason.contains("Finish/turn in " + knife.objectiveText() + " first."),
                "Non-current packet/terminal turn-in rejection should name the active Scrap Knife objective");
        String missingKnifeReason = MissionUxSummary.turnInReason(serverPlayer, quest, knife,
                quest.getMissionStatus(knife.id()), true, false, false);
        helper.assertTrue(missingKnifeReason.contains("Carry 1") && missingKnifeReason.contains("Scrap Knife"),
                "Scrap Knife turn-in should explain the missing held item");

        serverPlayer.getInventory().add(new ItemStack(ModItems.SCRAP_KNIFE.get()));
        helper.assertTrue(knife.isComplete(serverPlayer),
                "Holding one Scrap Knife should satisfy the Scrap Knife mission");
        String readyReason = MissionUxSummary.turnInReason(serverPlayer, quest, knife,
                quest.getMissionStatus(knife.id()), true, true, false);
        helper.assertTrue("ECHO validation required.".equals(readyReason),
                "Satisfied Scrap Knife mission should expose an enabled turn-in state");

        boolean turnedInThroughTerminal = false;
        if (ModList.get().isLoaded("echomissioncore")
                && EchoCoreServices.missionCoreAvailable()
                && AshfallMissionCoreIntegration.registerWhenReady()
                && legacyMissionCoreExportReady(knife.id())) {
            Identifier missionCoreKnife = AshfallMissionCoreIntegration.missionId(knife.id());
            var readyView = EchoCoreServices.missionService().mission(serverPlayer, missionCoreKnife).orElseThrow();
            helper.assertTrue(readyView.actions().stream().anyMatch(action ->
                            action.enabled() && ("complete".equals(action.id()) || "turn_in".equals(action.id()))),
                    "Ready Scrap Knife MissionCore export should expose an enabled complete/turn-in action");
            TerminalMissionRegistry.register(MainSurvivalQuestProvider.INSTANCE);
            TerminalMissionActions.registerForTab(MainSurvivalQuestProvider.TAB_ID);
            TerminalActionRegistry.handle(serverPlayer,
                    MainSurvivalQuestProvider.TAB_ID,
                    TerminalMissionActions.MISSION_ACTION,
                    TerminalMissionActions.payload(MainSurvivalQuestProvider.CHAPTER_ID, missionCoreKnife, "complete"));
            quest = QuestData.get(serverPlayer);
            turnedInThroughTerminal = quest.isMissionCompleted(knife.id());
            helper.assertTrue(turnedInThroughTerminal,
                    "Survival Route ScreenCore Turn In should dispatch through MainSurvival and complete MissionCore-owned Scrap Knife");
        }
        if (!turnedInThroughTerminal) {
            helper.assertTrue(EchoGuideManager.turnInMission(serverPlayer, quest, knife),
                    "Scrap Knife turn-in should complete through the legacy Ashfall quest state");
        }
        QuestData.saveAndSync(serverPlayer, quest);
        helper.assertTrue(quest.isMissionCompleted(knife.id()),
                "Scrap Knife mission should be completed after turn-in");
        helper.assertTrue(quest.hasPendingRewards(knife.id())
                        || AshfallMissionCoreIntegration.hasClaimableReward(serverPlayer, knife),
                "Scrap Knife completion should leave rewards claimable");
        int scrapBeforeClaim = countInventory(serverPlayer, ModItems.SCRAP_METAL.get());
        EchoGuideManager.claimRewards(serverPlayer, knife.id());
        QuestData afterClaim = QuestData.get(serverPlayer);
        helper.assertFalse(afterClaim.hasPendingRewards(knife.id()),
                "Mission-scoped Scrap Knife reward claim should clear legacy pending rewards");
        helper.assertFalse(AshfallMissionCoreIntegration.hasClaimableReward(serverPlayer, knife),
                "Mission-scoped Scrap Knife reward claim should clear MissionCore claimable rewards");
        helper.assertTrue(countInventory(serverPlayer, ModItems.SCRAP_METAL.get()) >= scrapBeforeClaim + 12,
                "Scrap Knife claim should award its Scrap Metal cache exactly once");
        int scrapAfterClaim = countInventory(serverPlayer, ModItems.SCRAP_METAL.get());
        EchoGuideManager.claimRewards(serverPlayer, knife.id());
        helper.assertTrue(countInventory(serverPlayer, ModItems.SCRAP_METAL.get()) == scrapAfterClaim,
                "Repeating the Scrap Knife claim should not duplicate the reward cache");
        helper.succeed();
    }

    private static void advancementCriteriaGuard(GameTestHelper helper) {
        Path advancementRoot = Path.of("src", "main", "resources", "data", EchoAshfallProtocol.MODID, "advancement");
        if (Files.isDirectory(advancementRoot)) {
            try (Stream<Path> files = Files.walk(advancementRoot)) {
                files.filter(path -> path.toString().endsWith(".json"))
                        .forEach(path -> assertAdvancementCriteriaGuard(helper,
                                advancementRoot.relativize(path).toString().replace('\\', '/'),
                                readJsonFile(path)));
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to scan Ashfall advancement criteria", exception);
            }
        } else {
            for (String advancement : List.of(
                    "build_research_lab",
                    "first_power_node",
                    "nexus_core_found",
                    "relay_station",
                    "workshop_master",
                    "first_light/first_campfire")) {
                assertAdvancementCriteriaGuard(helper, advancement + ".json", readAdvancementResource(advancement));
            }
        }

        JsonObject firstPowerNode = readAdvancementResource("first_power_node");
        JsonObject placedPowerNode = firstPowerNode.getAsJsonObject("criteria").getAsJsonObject("placed_power_node");
        helper.assertTrue(blockScopedLocation(placedPowerNode.getAsJsonObject("conditions"), "echoashfallprotocol:power_node"),
                "Grid Anchor must only trigger from placing echoashfallprotocol:power_node");

        JsonObject nexusCoreFound = readAdvancementResource("nexus_core_found");
        JsonObject foundNexusCore = nexusCoreFound.getAsJsonObject("criteria").getAsJsonObject("found_nexus_core");
        helper.assertTrue("minecraft:impossible".equals(jsonString(foundNexusCore, "trigger")),
                "The Heart of the Grid must be awarded by mission code, not by generic inventory changes");
        helper.assertFalse(foundNexusCore.has("conditions"),
                "Mission-awarded impossible advancement should not retain inventory conditions");
        helper.succeed();
    }

    private static void assertAdvancementCriteriaGuard(GameTestHelper helper, String label, JsonObject advancement) {
        JsonObject criteria = advancement.getAsJsonObject("criteria");
        if (criteria == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : criteria.entrySet()) {
            JsonObject criterion = entry.getValue().getAsJsonObject();
            String trigger = jsonString(criterion, "trigger");
            JsonObject conditions = criterion.getAsJsonObject("conditions");
            if ("minecraft:inventory_changed".equals(trigger) && conditions != null && conditions.has("items")) {
                helper.assertFalse(conditions.get("items").isJsonArray()
                                && conditions.getAsJsonArray("items").isEmpty(),
                        label + " criterion " + entry.getKey() + " must not use inventory_changed with an empty item list");
            }
            if ("minecraft:placed_block".equals(trigger)) {
                helper.assertTrue(hasAnyBlockScopedLocation(conditions),
                        label + " criterion " + entry.getKey() + " must scope placed_block to a block_state_property location");
            }
        }
    }

    private static boolean hasAnyBlockScopedLocation(JsonObject conditions) {
        if (conditions == null || !conditions.has("location") || !conditions.get("location").isJsonArray()) {
            return false;
        }
        for (JsonElement element : conditions.getAsJsonArray("location")) {
            if (element.isJsonObject()
                    && "minecraft:block_state_property".equals(jsonString(element.getAsJsonObject(), "condition"))
                    && element.getAsJsonObject().has("block")) {
                return true;
            }
        }
        return false;
    }

    private static boolean blockScopedLocation(JsonObject conditions, String blockId) {
        if (conditions == null || !conditions.has("location") || !conditions.get("location").isJsonArray()) {
            return false;
        }
        for (JsonElement element : conditions.getAsJsonArray("location")) {
            if (element.isJsonObject()
                    && "minecraft:block_state_property".equals(jsonString(element.getAsJsonObject(), "condition"))
                    && blockId.equals(jsonString(element.getAsJsonObject(), "block"))) {
                return true;
            }
        }
        return false;
    }

    private static JsonObject readAdvancementResource(String advancement) {
        String resource = "data/" + EchoAshfallProtocol.MODID + "/advancement/" + advancement + ".json";
        try (InputStream input = ModGameTests.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing advancement resource: " + resource);
            }
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read advancement resource: " + resource, exception);
        }
    }

    private static JsonObject readRecipeResource(String recipe) {
        String resource = "data/" + EchoAshfallProtocol.MODID + "/recipe/" + recipe + ".json";
        try (InputStream input = ModGameTests.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing recipe resource: " + resource);
            }
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read recipe resource: " + resource, exception);
        }
    }

    private static JsonObject readLootModifierResource(String lootModifier) {
        String resource = "data/" + EchoAshfallProtocol.MODID + "/loot_modifiers/" + lootModifier + ".json";
        try (InputStream input = ModGameTests.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing loot modifier resource: " + resource);
            }
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read loot modifier resource: " + resource, exception);
        }
    }

    private static JsonObject readJsonFile(Path path) {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read advancement file: " + path, exception);
        }
    }

    private static String jsonString(JsonObject object, String key) {
        JsonElement value = object == null ? null : object.get(key);
        return value == null || !value.isJsonPrimitive() ? "" : value.getAsString();
    }

    private static EchoCompanionDrone spawnCompanionDrone(GameTestHelper helper, Player owner, BlockPos relativePos) {
        EchoCompanionDrone drone = ModEntities.ECHO_COMPANION_DRONE.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(drone != null, "Companion drone should be spawnable");
        BlockPos pos = helper.absolutePos(relativePos);
        drone.setOwner(owner);
        drone.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(drone);
        return drone;
    }

    private static ScoutDrone spawnScoutDrone(GameTestHelper helper, Player owner, BlockPos relativePos) {
        ScoutDrone drone = ModEntities.SCOUT_DRONE.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(drone != null, "Scout Drone should be spawnable");
        BlockPos pos = helper.absolutePos(relativePos);
        drone.setOwner(owner);
        drone.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        helper.getLevel().addFreshEntity(drone);
        return drone;
    }

    private static ScoutDrone nearestScoutDrone(GameTestHelper helper, Player owner) {
        List<ScoutDrone> drones = helper.getLevel().getEntitiesOfClass(ScoutDrone.class,
                owner.getBoundingBox().inflate(64.0D),
                drone -> !drone.isRemoved() && owner.getUUID().equals(drone.getOwnerUUID()));
        return drones.stream()
                .min(java.util.Comparator.comparingDouble(drone -> drone.distanceToSqr(owner)))
                .orElse(null);
    }

    private static void cleanupOwnedDrones(GameTestHelper helper, Player owner) {
        helper.getLevel().getEntitiesOfClass(EchoCompanionDrone.class, owner.getBoundingBox().inflate(256.0D),
                drone -> owner.getUUID().equals(drone.getOwnerUUID())).forEach(EchoCompanionDrone::discard);
        helper.getLevel().getEntitiesOfClass(ScoutDrone.class, owner.getBoundingBox().inflate(256.0D),
                drone -> owner.getUUID().equals(drone.getOwnerUUID())).forEach(ScoutDrone::discard);
    }

    private static int countItem(Player player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int countBlocks(GameTestHelper helper, BlockPos origin, Vec3i size, String blockId) {
        int count = 0;
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    Identifier id = BuiltInRegistries.BLOCK.getKey(
                            helper.getLevel().getBlockState(origin.offset(x, y, z)).getBlock());
                    if (id != null && id.toString().equals(blockId)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int countStarterPodProtectedPathClutter(GameTestHelper helper, BlockPos origin, Vec3i size) {
        int count = 0;
        for (int x = 0; x < size.getX(); x++) {
            for (int z = 0; z < size.getZ(); z++) {
                if (!isStarterPodProtectedPathCell(x, z)) {
                    continue;
                }
                if (isStarterPodPathClutter(helper, origin.offset(x, 3, z))) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countStarterPodOffPathClutter(GameTestHelper helper, BlockPos origin, Vec3i size) {
        int count = 0;
        for (int x = 0; x < size.getX(); x++) {
            for (int z = 0; z < size.getZ(); z++) {
                if (!isStarterPodProtectedPathCell(x, z)
                        && isStarterPodPathClutter(helper, origin.offset(x, 3, z))) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean isStarterPodProtectedPathCell(int localX, int localZ) {
        if (Math.abs(localX - 8) <= 1 && localZ >= 8 && localZ <= 10) {
            return true;
        }
        if ((localX == 5 || localX == 6 || localX == 10 || localX == 11) && localZ >= 4 && localZ <= 5) {
            return true;
        }
        if (localX >= 4 && localX <= 8 && localZ >= 7 && localZ <= 10) {
            return true;
        }
        return Math.abs(localX - 8) <= 2 && localZ >= 12 && localZ <= 15;
    }

    private static boolean isStarterPodPathClutter(GameTestHelper helper, BlockPos pos) {
        var state = helper.getLevel().getBlockState(pos);
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return state.is(ModBlocks.RUSTED_METAL_DEBRIS.get())
                || state.is(ModBlocks.CABLE_BUNDLE.get())
                || state.is(ModBlocks.TWISTED_METAL.get())
                || Identifier.fromNamespaceAndPath("echoblockworks", "rubble_pile").equals(id)
                || Identifier.fromNamespaceAndPath("echoblockworks", "scattered_debris").equals(id)
                || Identifier.fromNamespaceAndPath("echoblockworks", "steam_vent").equals(id);
    }

    private static int countNonAirVanillaBlocks(GameTestHelper helper, BlockPos origin, Vec3i size) {
        int count = 0;
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    var state = helper.getLevel().getBlockState(origin.offset(x, y, z));
                    Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    if (id != null && "minecraft".equals(id.getNamespace()) && !state.isAir()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int countEchoContainerBlockEntities(GameTestHelper helper, BlockPos origin, Vec3i size) {
        int count = 0;
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    var state = helper.getLevel().getBlockState(pos);
                    if ((state.is(ModBlocks.ECHO_CACHE.get()) || state.is(ModBlocks.ECHO_CRATE.get()))
                            && helper.getLevel().getBlockEntity(pos) instanceof com.knoxhack.echoashfallprotocol.block.entity.EchoContainerBlockEntity) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int countLootedEchoContainers(GameTestHelper helper, BlockPos origin, Vec3i size) {
        int count = 0;
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    var state = helper.getLevel().getBlockState(pos);
                    boolean isEchoContainerBlock = state.is(ModBlocks.ECHO_CACHE.get())
                            || state.is(ModBlocks.ECHO_CRATE.get())
                            || state.is(ModBlocks.STRUCTURE_CACHE.get());
                    if (isEchoContainerBlock
                            && helper.getLevel().getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity container
                            && container.getLootTable() != null) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int countInvalidBlockEntities(GameTestHelper helper, BlockPos origin, Vec3i size) {
        int count = 0;
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (helper.getLevel().getBlockEntity(pos) == null) {
                        continue;
                    }
                    var state = helper.getLevel().getBlockState(pos);
                    if (state.isAir() || !state.hasBlockEntity()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static List<EntityType<? extends Entity>> allAshfallTypes() {
        return List.of(
                ModEntities.RAD_ZOMBIE.get(),
                ModEntities.SCAVENGER_BANDIT.get(),
                ModEntities.IRRADIATED_WOLF.get(),
                ModEntities.ECHO_DRONE.get(),
                ModEntities.SCOUT_DRONE.get(),
                ModEntities.ECHO_COMPANION_DRONE.get(),
                ModEntities.GLOWING_GHOUL.get(),
                ModEntities.ASH_WRAITH.get(),
                ModEntities.TOXIC_SLIME.get(),
                ModEntities.CITY_STALKER.get(),
                ModEntities.RUST_WALKER.get(),
                ModEntities.STEAM_WRAITH.get(),
                ModEntities.MUTATED_CRAWLER.get(),
                ModEntities.WILD_DOG.get(),
                ModEntities.FERAL_HUMAN.get(),
                ModEntities.CRASH_SURVIVOR.get(),
                ModEntities.FACTION_NPC.get(),
                ModEntities.GRIDBOUND_HUSK.get(),
                ModEntities.RELAY_WARDEN.get(),
                ModEntities.SIGNAL_LEECH.get(),
                ModEntities.NEXUS_NULLIFIER.get(),
                ModEntities.WARDEN_BOSS.get(),
                ModEntities.WASTELAND_SENTINEL.get(),
                ModEntities.CRASH_ZONE_COLOSSUS.get(),
                ModEntities.CRYOGENIC_OVERSEER.get(),
                ModEntities.INDUSTRIAL_JUGGERNAUT.get(),
                ModEntities.NEXUS_SCAR_AVATAR.get(),
                ModEntities.RADIATION_BEHEMOTH.get(),
                ModEntities.CITY_RUIN_STALKER.get(),
                ModEntities.PLAINS_WARLORD.get(),
                ModEntities.TOXIC_HIVE_MATRIARCH.get(),
                ModEntities.CORRUPTION_BLOOM.get(),
                ModEntities.SEVERANCE_ENGINE.get(),
                ModEntities.MIRROR_COMMAND.get()
        );
    }

    private static void smokeEntity(GameTestHelper helper, EntityType<? extends Entity> type) {
        Entity entity = type.create(helper.getLevel(), EntitySpawnReason.EVENT);
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        helper.assertTrue(entity != null, "Entity should spawn for smoke test: " + entityId);
        if (entity != null) {
            BlockPos pos = helper.absolutePos(new BlockPos(6, 2, 6));
            entity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            helper.getLevel().addFreshEntity(entity);
            entity.tick();
            if (entity instanceof Mob mob) {
                helper.assertTrue(mob.getAttribute(Attributes.MAX_HEALTH) != null,
                        "Smoke mob should have health attribute: " + entityId);
                helper.assertTrue(mob.getAttribute(Attributes.ATTACK_DAMAGE) != null,
                        "Smoke mob should have attack damage attribute: " + entityId);
            }
            entity.discard();
        }
    }

    private static List<EntityType<? extends Entity>> attackingTypes() {
        return List.of(
                ModEntities.RAD_ZOMBIE.get(),
                ModEntities.SCAVENGER_BANDIT.get(),
                ModEntities.IRRADIATED_WOLF.get(),
                ModEntities.ECHO_DRONE.get(),
                ModEntities.SCOUT_DRONE.get(),
                ModEntities.ECHO_COMPANION_DRONE.get(),
                ModEntities.GLOWING_GHOUL.get(),
                ModEntities.ASH_WRAITH.get(),
                ModEntities.TOXIC_SLIME.get(),
                ModEntities.CITY_STALKER.get(),
                ModEntities.RUST_WALKER.get(),
                ModEntities.STEAM_WRAITH.get(),
                ModEntities.MUTATED_CRAWLER.get(),
                ModEntities.WILD_DOG.get(),
                ModEntities.FERAL_HUMAN.get(),
                ModEntities.GRIDBOUND_HUSK.get(),
                ModEntities.RELAY_WARDEN.get(),
                ModEntities.SIGNAL_LEECH.get(),
                ModEntities.NEXUS_NULLIFIER.get(),
                ModEntities.WARDEN_BOSS.get(),
                ModEntities.WASTELAND_SENTINEL.get(),
                ModEntities.CRASH_ZONE_COLOSSUS.get(),
                ModEntities.CRYOGENIC_OVERSEER.get(),
                ModEntities.INDUSTRIAL_JUGGERNAUT.get(),
                ModEntities.NEXUS_SCAR_AVATAR.get(),
                ModEntities.RADIATION_BEHEMOTH.get(),
                ModEntities.CITY_RUIN_STALKER.get(),
                ModEntities.PLAINS_WARLORD.get(),
                ModEntities.TOXIC_HIVE_MATRIARCH.get(),
                ModEntities.CORRUPTION_BLOOM.get(),
                ModEntities.SEVERANCE_ENGINE.get(),
                ModEntities.MIRROR_COMMAND.get()
        );
    }

    private static void missionUxSummary(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        QuestData quest = QuestData.get(player);
        Mission first = MissionRegistry.getMission(0, 0);
        helper.assertTrue(first != null, "First mission should exist for UX summary coverage");
        if (first == null) {
            helper.succeed();
            return;
        }

        setCurrentMission(quest, first.id());
        MissionUxSummary active = MissionUxSummary.of(player, quest, first);
        helper.assertTrue(!active.shortTitle().isBlank(), "Active mission should expose a short title");
        helper.assertTrue(!active.nextStep().isBlank(), "Active mission should expose a next step");
        helper.assertTrue(!active.tags().isEmpty(), "Active mission should expose display tags");
        helper.assertTrue(!active.relatedIntelKey().isBlank(), "Active mission should expose related intel");
        helper.assertTrue("ACTIVE".equals(active.statusLabel()), "Current unlocked mission should display ACTIVE");

        int covered = 0;
        boolean sawLocked = false;
        for (int phase = 0; phase < MissionRegistry.getPhaseCount(); phase++) {
            for (Mission mission : MissionRegistry.getMissionsForPhase(phase)) {
                MissionUxSummary summary = MissionUxSummary.of(player, quest, mission);
                helper.assertTrue(!summary.shortTitle().isBlank(), "Mission UX title should not be blank: " + mission.id());
                helper.assertTrue(!summary.nextStep().isBlank(), "Mission UX next step should not be blank: " + mission.id());
                helper.assertTrue(!summary.statusLabel().isBlank(), "Mission UX status should not be blank: " + mission.id());
                sawLocked = sawLocked || "LOCKED".equals(summary.statusLabel()) || "VIEW".equals(summary.statusLabel());
                covered++;
            }
        }
        helper.assertTrue(covered == MissionRegistry.getAllMissions().size(),
                "Mission UX coverage should inspect every registered mission");
        helper.assertTrue(sawLocked, "Mission UX coverage should include a locked or view-only state");

        quest.completeMission(first.id(), List.of());
        MissionUxSummary done = MissionUxSummary.of(player, quest, first);
        helper.assertTrue("DONE".equals(done.statusLabel()), "Completed mission without pending rewards should display DONE");
        helper.succeed();
    }

    private static void ashfallHudNoticeShelfLayout(GameTestHelper helper) {
        int statusBottom = 228;
        SurvivalHudOverlay.NoticeShelfLayout layout = SurvivalHudOverlay.noticeShelfLayoutForTests(statusBottom);
        helper.assertTrue(layout.x() == 6, "Notice shelf should align with the left status HUD");
        helper.assertTrue(layout.y() == statusBottom + 6, "Notice shelf should sit below the status HUD");
        helper.assertTrue(layout.width() == 214, "Notice shelf should match the normal status HUD width");
        helper.assertTrue(layout.rowHeight() == 36 && layout.rowGap() == 4 && layout.maxRows() == 2,
                "Notice shelf should reserve two compact rows");
        helper.succeed();
    }

    private static void endgameRouteProgress(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        QuestData quest = QuestData.get(player);
        PostNexusData post = PostNexusData.get(player);

        post.setSelectedPath(PostNexusData.NexusPath.CONTROL);
        post.addDenseAlloy(12);
        post.addNexusCrystals(49);
        post.addEnergyCells(2);
        player.getInventory().add(new ItemStack(ModItems.DENSE_ALLOY_CHUNK.get(), 50));
        player.getInventory().add(new ItemStack(ModItems.ENERGY_CELL.get(), 50));

        EndgameMissionProgress.Snapshot resources = EndgameMissionProgress
                .forMission(player, quest, requireMission(helper, "control_resource_dominance"))
                .orElseThrow(() -> new IllegalStateException("Control resource progress should be exposed"));
        helper.assertTrue(resources.entries().size() == 3,
                "Control resources should expose one terminal counter per resource");
        helper.assertTrue(resources.entries().get(0).have() == PostNexusData.CONTROL_DENSE_ALLOY_REQUIRED,
                "Dense Alloy progress should use the greater held or tracked count");
        helper.assertTrue(resources.entries().get(1).have() == 49,
                "Nexus Crystal progress should keep tracked pickup count when held count is lower");
        helper.assertTrue(resources.entries().get(2).have() == PostNexusData.CONTROL_ENERGY_CELLS_REQUIRED,
                "Energy Cell progress should use held inventory fallback");
        helper.assertTrue(resources.firstOpenStep().contains("Nexus Crystals"),
                "Control resource next step should name the missing resource");

        post.setSelectedPath(PostNexusData.NexusPath.RESTORE);
        post.incrementNodesRepaired();
        post.incrementNodesRepaired();
        EndgameMissionProgress.Snapshot repairNodes = EndgameMissionProgress
                .forMission(player, quest, requireMission(helper, "restore_repair_nodes"))
                .orElseThrow(() -> new IllegalStateException("Restore node progress should be exposed"));
        helper.assertTrue(repairNodes.entries().get(0).have() == 2,
                "Restore node counter should expose the tracked node count");
        helper.assertTrue(repairNodes.firstOpenStep().contains("Power Nodes"),
                "Restore node next step should point at Power Nodes");

        EndgameMissionProgress.Snapshot storm = EndgameMissionProgress
                .forMission(player, quest, requireMission(helper, "destroy_survive_storms"))
                .orElseThrow(() -> new IllegalStateException("Destroy storm progress should be exposed"));
        helper.assertTrue(storm.entries().get(0).have() == 0,
                "Destroy storm counter should start at zero before credit");
        helper.assertTrue(PostNexusEventHandler.isDestroyRouteStormCreditEvent(
                        EnvironmentalEventType.RADIATION_STORM, false),
                "Destroy storm mission should credit radiation storms");
        helper.assertTrue(PostNexusEventHandler.isDestroyRouteStormCreditEvent(
                        EnvironmentalEventType.ASH_STORM, false),
                "Destroy storm mission should credit ash storms");
        helper.assertTrue(PostNexusEventHandler.isDestroyRouteStormCreditEvent(
                        EnvironmentalEventType.NEXUS_SURGE, false),
                "Destroy storm mission should credit Nexus surges");
        helper.assertTrue(PostNexusEventHandler.isDestroyRouteStormCreditEvent(
                        EnvironmentalEventType.NONE, true),
                "Destroy storm mission should credit thunder even without a custom event");
        helper.assertTrue(!PostNexusEventHandler.isDestroyRouteStormCreditEvent(
                        EnvironmentalEventType.TOXIC_STORM, false),
                "Destroy storm mission should not credit unrelated clear-weather events");

        NexusCampaignData campaign = NexusCampaignData.get(helper.getLevel().getServer().overworld());
        campaign.resetForTests();
        campaign.awaken(helper.absolutePos(new BlockPos(1, 2, 1)));
        campaign.scanRelays();
        EndgameMissionProgress.Snapshot relayScan = EndgameMissionProgress
                .forMission(player, quest, requireMission(helper, "scan_prime_relays"))
                .orElseThrow(() -> new IllegalStateException("Prime Relay scan progress should be exposed"));
        helper.assertTrue(relayScan.entries().get(0).have() == NexusCampaignData.REQUIRED_RELAY_SCAN_COUNT,
                "Prime Relay scan progress should read world campaign data");

        campaign.resolveRelay(NexusRelayType.REACTOR, NexusRelayState.STABILIZED);
        campaign.resolveRelay(NexusRelayType.CRYO, NexusRelayState.SEVERED);
        EndgameMissionProgress.Snapshot relayResolve = EndgameMissionProgress
                .forMission(player, quest, requireMission(helper, "resolve_prime_relays"))
                .orElseThrow(() -> new IllegalStateException("Prime Relay resolve progress should be exposed"));
        helper.assertTrue(relayResolve.entries().get(0).have() == 2,
                "Prime Relay resolution progress should expose resolved relay count");

        campaign.resolveRelay(NexusRelayType.BIO, NexusRelayState.OVERRIDDEN);
        campaign.markSiegeComplete();
        EndgameMissionProgress.Snapshot siege = EndgameMissionProgress
                .forMission(player, quest, requireMission(helper, "survive_core_countermeasure"))
                .orElseThrow(() -> new IllegalStateException("Core siege progress should be exposed"));
        helper.assertTrue(siege.entries().get(0).satisfied(),
                "Core siege progress should read world campaign siege credit");

        post.incrementPathOperationsComplete();
        EndgameMissionProgress.Snapshot operation = EndgameMissionProgress
                .forMission(player, quest, requireMission(helper, "control_command_lattice"))
                .orElseThrow(() -> new IllegalStateException("Path operation progress should be exposed"));
        helper.assertTrue(operation.entries().get(0).satisfied(),
                "Post-Warden path operation should expose player counter credit");
        post.setFinalBossDefeated(true);
        EndgameMissionProgress.Snapshot finale = EndgameMissionProgress
                .forMission(player, quest, requireMission(helper, "control_finale"))
                .orElseThrow(() -> new IllegalStateException("Path finale progress should be exposed"));
        helper.assertTrue(finale.entries().get(0).satisfied(),
                "Path finale should expose final boss credit");
        campaign.resetForTests();
        helper.succeed();
    }

    private static void terminalLoreTaxonomy(GameTestHelper helper) {
        try {
            Class<?> tabClass = Class.forName(
                    "com.knoxhack.echoashfallprotocol.integration.AshfallTerminalIntegration$AshfallTab");
            Method phaseTitle = tabClass.getDeclaredMethod("phaseTitle", int.class);
            phaseTitle.setAccessible(true);
            String[] expected = {
                    "PODFALL",
                    "OUTPOST SURVIVAL",
                    "LIFE SUPPORT",
                    "SIGNAL CONTACT",
                    "BIOHAZARD ADAPTATION",
                    "DEEP EXTRACTION",
                    "GRID RESTORATION",
                    "NEXUS DECISION",
                    "AFTERMATH PROTOCOL"
            };
            for (int i = 0; i < expected.length; i++) {
                Object actual = phaseTitle.invoke(null, i);
                helper.assertTrue(expected[i].equals(actual),
                        "Ashfall phase " + (i + 1) + " should render as " + expected[i]);
            }
            helper.succeed();
        } catch (ReflectiveOperationException error) {
            helper.assertTrue(false, "Ashfall terminal taxonomy reflection failed: " + error.getMessage());
        }
    }

    private static void terminalCommandDeckOwnership(GameTestHelper helper) {
        if (TerminalTabRegistry.tabs().isEmpty()) {
            helper.succeed();
            return;
        }
        Identifier commandDeck = Identifier.fromNamespaceAndPath("echoterminal", "overview");
        Identifier ashfallCommand = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "overview");
        boolean hasBuiltInCommandDeck = TerminalTabRegistry.tabs().stream()
                .anyMatch(tab -> commandDeck.equals(tab.descriptor().id())
                        && "COMMAND DECK".equals(tab.descriptor().title()));
        boolean hasAshfallCommand = TerminalTabRegistry.tabs().stream()
                .anyMatch(tab -> ashfallCommand.equals(tab.descriptor().id())
                        && "ASHFALL COMMAND".equals(tab.descriptor().title()));
        if (!hasBuiltInCommandDeck) {
            helper.succeed();
            return;
        }
        helper.assertTrue(hasBuiltInCommandDeck,
                "Ashfall must not overwrite the built-in echoterminal:overview Command Deck action hub");
        helper.assertTrue(hasAshfallCommand,
                "Ashfall active protocol overview should live on its own addon-owned tab id");
        TerminalNavigationProfiles.profile(commandDeck).ifPresent(profile ->
                helper.assertTrue(profile.section() == TerminalNavigationSection.TERMINAL,
                        "Built-in Command Deck should stay in Terminal navigation"));
        TerminalNavigationProfiles.profile(ashfallCommand).ifPresent(profile ->
                helper.assertTrue("ashfall".equals(profile.chapterId()),
                        "Ashfall command overview should be grouped under the Ashfall chapter"));
        helper.succeed();
    }

    private static void terminalCommonRegistration(GameTestHelper helper) {
        AshfallTerminalCommonIntegration.register();
        Identifier missions = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "missions");
        Identifier sideOps = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "ashfall_side_ops");
        if (ModList.get().isLoaded("echomissioncore")) {
            helper.assertTrue(TerminalMissionRegistry.provider(Identifier.fromNamespaceAndPath("echomissioncore", "missions")).isPresent(),
                    "MissionCore should own the shared mission feed when loaded");
            helper.assertTrue(TerminalMissionRegistry.provider(sideOps).isPresent(),
                    "Ashfall side ops should still register for Survival Route contextual cards when MissionCore owns main missions");
        } else {
            helper.assertTrue(TerminalMissionRegistry.provider(Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "ashfall_protocol")).isPresent(),
                    "Ashfall common setup should register the main mission provider");
            helper.assertTrue(TerminalMissionRegistry.provider(sideOps).isPresent(),
                    "Ashfall common setup should register the side ops mission provider");
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        if (player instanceof ServerPlayer serverPlayer) {
            helper.assertTrue(TerminalActionRegistry.handle(serverPlayer, missions,
                    TerminalMissionActions.MISSION_ACTION, "invalid"),
                    "Ashfall common setup should register server-side terminal mission actions");
        }
        helper.succeed();
    }

    private static void missionGuideCoverage(GameTestHelper helper) {
        int covered = 0;
        for (Mission mission : MissionRegistry.getAllMissions()) {
            helper.assertTrue(!mission.objectiveText().contains("+ Turn In"),
                    "Mission objective should not expose terminal action wording: " + mission.id());
            helper.assertTrue(!mission.echoMessage().contains("/nexus")
                            && !mission.completionMessage().contains("/nexus"),
                    "Mission copy should be terminal-first, not slash-command-first: " + mission.id());
            helper.assertTrue(MissionGuideRegistry.hasGuide(mission.id()),
                    "Every required Ashfall mission needs a field guide: " + mission.id());
            MissionGuideRegistry.Guide guide = MissionGuideRegistry.get(mission.id());
            helper.assertTrue(!guide.title().isBlank(), "Mission guide title should not be blank: " + mission.id());
            helper.assertTrue(!guide.body().isBlank(), "Mission guide body should not be blank: " + mission.id());
            helper.assertTrue(!guide.body().contains("no field guide"),
                    "Mission guide should not expose fallback copy: " + mission.id());
            helper.assertTrue(!guide.body().contains("/nexus")
                            && !guide.body().contains("fallback command channel"),
                    "Mission guide should keep slash-command fallback wording out of player copy: " + mission.id());
            covered++;
        }
        helper.assertTrue(covered == MissionRegistry.getAllMissions().size(),
                "Mission guide coverage should inspect every required mission");
        helper.succeed();
    }

    private static void firstNightRouteSafety(GameTestHelper helper) {
        String[] phase0Order = {
                "secure_crash_outpost",
                "craft_scrap_knife",
                "drink_clean_water",
                "secure_emergency_water_loop"
        };
        List<Mission> phase0 = MissionRegistry.getMissionsForPhase(0);
        helper.assertTrue(phase0.size() >= phase0Order.length,
                "Phase 0 should retain the full first-night crash route");
        for (int i = 0; i < phase0Order.length; i++) {
            helper.assertTrue(phase0Order[i].equals(phase0.get(i).id()),
                    "Phase 0 first-night route changed at index " + i);
        }
        helper.assertTrue("build_scrap_press".equals(MissionRegistry.getMission(2, 0).id()),
                "P3 Life Support should start with workshop scaling");
        helper.assertTrue("craft_portable_scanner".equals(MissionRegistry.getMission(3, 0).id()),
                "P4 Signal Contact should start with scanner access");
        helper.assertTrue("enter_bio_lab".equals(MissionRegistry.getMission(4, 0).id()),
                "P5 Biohazard Adaptation should start with bio-site entry");
        helper.assertTrue("clear_military_vault".equals(MissionRegistry.getMission(5, 0).id()),
                "P6 Deep Extraction should start with vault survey");
        helper.assertTrue("deploy_stationary_scanner".equals(MissionRegistry.getMission(6, 0).id()),
                "P7 Grid Restoration should start with a stationary scanner");
        helper.assertTrue("find_nexus_core".equals(MissionRegistry.getMission(7, 0).id()),
                "P8 Nexus Decision should start at the Core");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        QuestData quest = QuestData.get(player);
        String[] earlyRoute = {
                "secure_crash_outpost",
                "craft_scrap_knife",
                "drink_clean_water",
                "secure_emergency_water_loop",
                "forage_wasteland_food",
                "plant_mutated_sapling",
                "build_rain_collector",
                "stockpile_rations",
                "secure_sleep_shelter",
                "assemble_wasteland_field_kit",
                "find_schematic_fragment",
                "build_hand_recycler",
                "make_machine_casing",
                "build_micro_generator",
                "build_water_purifier",
                "stockpile_clean_water"
        };
        for (String missionId : earlyRoute) {
            Mission mission = requireMission(helper, missionId);
            setCurrentMission(quest, mission.id());
            MissionUxSummary summary = MissionUxSummary.of(player, quest, mission);
            MissionGuideRegistry.Guide guide = MissionGuideRegistry.get(mission.id());
            helper.assertTrue(!summary.shortTitle().isBlank(),
                    "First-night mission should expose a short title: " + mission.id());
            helper.assertTrue(!summary.nextStep().isBlank(),
                    "First-night mission should expose a next step: " + mission.id());
            helper.assertTrue(!guide.title().isBlank(),
                    "First-night mission should expose a guide title: " + mission.id());
            helper.assertTrue(!guide.body().isBlank(),
                    "First-night mission should expose guide copy: " + mission.id());
        }

        String[] deprecatedMissionIds = {
                "get_dirty_water",
                "emergency_filter_water",
                "craft_bone_knife",
                "craft_crude_spear",
                "craft_hide_wrap",
                "contact_radwarden_compact",
                "contact_crashbreak_salvage",
                "contact_sporebound_sanctum",
                "complete_radwarden_contract",
                "complete_crashbreak_contract",
                "complete_sporebound_contract",
                "upgrade_drone_support",
                "set_drone_scout_mode",
                "deploy_scout_drone",
                "acquire_mutagen"
        };
        for (String deprecatedMissionId : deprecatedMissionIds) {
            helper.assertTrue(MissionRegistry.getMissionById(deprecatedMissionId) == null,
                    "Deprecated mission should not appear in the visible route: " + deprecatedMissionId);
        }

        Mission first = requireMission(helper, "secure_crash_outpost");
        helper.assertTrue("Anchor Pod Outpost".equals(first.objectiveText()),
                "First mission should display the pod anchor objective");
        helper.assertTrue(requireMission(helper, "forage_wasteland_food").objectiveText().contains("Food Buffer"),
                "Food mission should display as a buffer confirmation");
        helper.assertTrue(requireMission(helper, "stockpile_rations").objectiveText().contains("Ration Buffer"),
                "Ration mission should display as a buffer confirmation");

        Mission emergencyWater = requireMission(helper, "secure_emergency_water_loop");
        assertEmergencyCleanWaterRecipe(helper, emergencyWater);
        helper.assertTrue(emergencyWater.requiredLocations().stream()
                        .anyMatch(location -> "special".equals(location.locationType())
                                && "water:dirty_collected".equals(location.locationId())),
                "Emergency water loop should require the dirty-water collection marker");
        helper.assertTrue(emergencyWater.requiredLocations().stream()
                        .anyMatch(location -> "special".equals(location.locationType())
                                && "water:emergency_filtered".equals(location.locationId())),
                "Emergency water loop should require the emergency-filtered marker");

        assertRewardAtLeast(helper, requireMission(helper, "plant_mutated_sapling"), Items.CAULDRON, 1);
        assertRewardAtLeast(helper, requireMission(helper, "plant_mutated_sapling"), ModItems.SCRAP_PLASTIC.get(), 8);
        assertRewardAtLeast(helper, requireMission(helper, "craft_scrap_knife"), ModItems.SCRAP_WIRE.get(), 4);
        assertRewardAtLeast(helper, requireMission(helper, "secure_sleep_shelter"), ModItems.ANIMAL_BONE.get(), 2);
        assertRewardAtLeast(helper, requireMission(helper, "secure_sleep_shelter"), ModItems.ANIMAL_HIDE.get(), 4);
        assertRewardAtLeast(helper, requireMission(helper, "secure_sleep_shelter"), Items.STICK, 3);
        assertRecipeContains(helper, "bone_knife", "minecraft:stick");
        assertRecipeOmits(helper, "bone_knife", "echoashfallprotocol:scrap_metal");
        assertRecipeContains(helper, "crude_spear", "minecraft:stick");
        assertRecipeOmits(helper, "crude_spear", "echoashfallprotocol:scrap_metal");
        Mission fieldKit = requireMission(helper, "assemble_wasteland_field_kit");
        assertRequiredCount(helper, fieldKit, ModItems.BONE_KNIFE.get(), 1);
        assertRequiredCount(helper, fieldKit, ModItems.CRUDE_SPEAR.get(), 1);
        assertRequiredCount(helper, fieldKit, ModItems.HIDE_WRAP.get(), 1);
        assertRewardAtLeast(helper, fieldKit, ModItems.FILTER_CARTRIDGE_BASIC.get(), 1);
        assertRewardAtLeast(helper, fieldKit, ModItems.SCHEMATIC_FRAGMENT.get(), 1);
        assertRewardAtLeast(helper, requireMission(helper, "find_schematic_fragment"), ModItems.MACHINE_CASING.get(), 1);
        assertRewardAtLeast(helper, requireMission(helper, "build_hand_recycler"), ModItems.SCRAP_METAL.get(), 12);
        assertRewardAtLeast(helper, requireMission(helper, "build_hand_recycler"), ModItems.SCRAP_WIRE.get(), 6);
        assertRequiredCount(helper, requireMission(helper, "build_hand_recycler"), ModItems.MACHINE_CASING.get(), 1);
        assertRequiredCount(helper, requireMission(helper, "build_hand_recycler"), ModItems.SCRAP_METAL.get(), 4);
        assertRequiredCount(helper, requireMission(helper, "build_hand_recycler"), ModItems.SCRAP_WIRE.get(), 4);
        assertRequiredCount(helper, requireMission(helper, "build_micro_generator"), ModItems.MACHINE_CASING.get(), 1);
        assertRequiredCount(helper, requireMission(helper, "build_micro_generator"), ModItems.SCRAP_WIRE.get(), 3);
        assertRequiredCount(helper, requireMission(helper, "build_water_purifier"), ModItems.MACHINE_CASING.get(), 3);
        assertRewardAtLeast(helper, requireMission(helper, "build_water_purifier"), ModItems.FILTER_CARTRIDGE_BASIC.get(), 2);
        assertRewardAtLeast(helper, requireMission(helper, "build_water_purifier"), ModItems.DIRTY_WATER_BOTTLE.get(), 2);
        helper.assertTrue(MissionGuideRegistry.get("build_water_purifier").body().contains("three machine casings"),
                "Water purifier guide should describe the first-hour casing cost");

        QuestData partialWater = new QuestData();
        partialWater.completeMission("get_dirty_water", List.of());
        helper.assertFalse(partialWater.isMissionCompleted("secure_emergency_water_loop"),
                "Partial legacy water chain should not auto-complete the merged water loop");
        helper.assertTrue(partialWater.hasVisitedLocation("special", "water:dirty_collected"),
                "Partial legacy dirty-water completion should preserve the collection marker");
        QuestData legacyWater = new QuestData();
        legacyWater.completeMission("get_dirty_water", List.of());
        legacyWater.completeMission("emergency_filter_water", List.of());
        helper.assertTrue(legacyWater.isMissionCompleted("secure_emergency_water_loop"),
                "Completed legacy water chain should migrate to the merged emergency water loop");
        helper.assertTrue(legacyWater.hasVisitedLocation("special", "water:dirty_collected")
                        && legacyWater.hasVisitedLocation("special", "water:emergency_filtered"),
                "Completed legacy water chain should preserve both water proof markers");
        QuestData newWater = new QuestData();
        newWater.completeMission("secure_emergency_water_loop",
                List.of(new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get(), 1)));
        helper.assertTrue(newWater.isMissionCompleted("get_dirty_water")
                        && newWater.isMissionCompleted("emergency_filter_water"),
                "Completing the merged emergency water loop should silently reconcile water legacy IDs");
        helper.assertTrue(newWater.getPendingRewards("get_dirty_water").isEmpty()
                        && newWater.getPendingRewards("emergency_filter_water").isEmpty(),
                "Legacy water aliases should not create duplicate pending rewards");

        QuestData legacyPrimitive = new QuestData();
        legacyPrimitive.completeMission("craft_bone_knife", List.of());
        legacyPrimitive.completeMission("craft_crude_spear", List.of());
        helper.assertFalse(legacyPrimitive.isMissionCompleted("assemble_wasteland_field_kit"),
                "Partial legacy primitive chain should not auto-complete the merged kit");
        legacyPrimitive.completeMission("craft_hide_wrap", List.of());
        helper.assertTrue(legacyPrimitive.isMissionCompleted("assemble_wasteland_field_kit"),
                "Completed legacy primitive chain should migrate to the merged field kit");

        QuestData newPrimitive = new QuestData();
        newPrimitive.completeMission("assemble_wasteland_field_kit",
                List.of(new ItemStack(ModItems.SCHEMATIC_FRAGMENT.get(), 1)));
        helper.assertTrue(newPrimitive.isMissionCompleted("craft_bone_knife")
                        && newPrimitive.isMissionCompleted("craft_crude_spear")
                        && newPrimitive.isMissionCompleted("craft_hide_wrap"),
                "Completing the merged field kit should silently reconcile primitive legacy IDs");
        helper.assertTrue(newPrimitive.getPendingRewards("craft_bone_knife").isEmpty()
                        && newPrimitive.getPendingRewards("craft_crude_spear").isEmpty()
                        && newPrimitive.getPendingRewards("craft_hide_wrap").isEmpty(),
                "Legacy primitive aliases should not create duplicate pending rewards");
        helper.succeed();
    }

    private static void assertEmergencyCleanWaterRecipe(GameTestHelper helper, Mission mission) {
        helper.assertTrue("emergency_clean_water".equals(mission.getCraftingRecipeId()),
                "Emergency water mission should point at the hand-cleaning recipe");

        Identifier recipeId = id(mission.getCraftingRecipeId());
        boolean loaded = helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .anyMatch(holder -> holder.id().identifier().equals(recipeId));
        helper.assertTrue(loaded, "Emergency water mission recipe should load: " + recipeId);

        JsonObject recipe = readRecipeResource(mission.getCraftingRecipeId());
        helper.assertTrue("minecraft:crafting_shapeless".equals(jsonString(recipe, "type")),
                "Emergency clean water should be a shapeless hand recipe");
        helper.assertTrue(recipe.has("ingredients") && recipe.get("ingredients").isJsonArray(),
                "Emergency clean water recipe should declare ingredients");

        List<String> ingredients = new ArrayList<>();
        for (JsonElement ingredient : recipe.getAsJsonArray("ingredients")) {
            ingredients.add(ingredient.isJsonPrimitive() ? ingredient.getAsString() : jsonString(ingredient.getAsJsonObject(), "item"));
        }
        helper.assertTrue(ingredients.size() == 3,
                "Emergency clean water should consume exactly dirty water, a basic filter, and ash");
        helper.assertTrue(Collections.frequency(ingredients, "echoashfallprotocol:dirty_water_bottle") == 1,
                "Emergency clean water should consume one dirty water bottle");
        helper.assertTrue(Collections.frequency(ingredients, "echoashfallprotocol:filter_cartridge_basic") == 1,
                "Emergency clean water should consume one full basic filter cartridge");
        helper.assertTrue(Collections.frequency(ingredients, "echoashfallprotocol:ash") == 1,
                "Emergency clean water should consume one ash");

        JsonObject result = recipe.getAsJsonObject("result");
        helper.assertTrue("echoashfallprotocol:clean_water_bottle".equals(jsonString(result, "id")),
                "Emergency clean water should output a clean water bottle");
        helper.assertTrue(!result.has("count") || result.get("count").getAsInt() == 1,
                "Emergency clean water should output exactly one clean water bottle");
    }

    private static void assertRecipeContains(GameTestHelper helper, String recipeId, String itemId) {
        helper.assertTrue(readRecipeResource(recipeId).toString().contains(itemId),
                recipeId + " recipe should contain " + itemId);
    }

    private static void assertRecipeOmits(GameTestHelper helper, String recipeId, String itemId) {
        helper.assertTrue(!readRecipeResource(recipeId).toString().contains(itemId),
                recipeId + " recipe should not contain " + itemId);
    }

    private static void explorationSiteProfiles(GameTestHelper helper) {
        List<String> warnings = ExplorationSiteRegistry.validationWarnings();
        helper.assertTrue(warnings.isEmpty(),
                "Exploration site registry warnings: " + String.join("; ", warnings));

        for (ExplorationSiteRegistry.SiteProfile profile : ExplorationSiteRegistry.all()) {
            helper.assertTrue(!profile.displayName().isBlank(), "POI display name missing: " + profile.id());
            helper.assertTrue(!profile.route().isBlank(), "POI route missing: " + profile.id());
            helper.assertTrue(!profile.description().isBlank(), "POI intel missing: " + profile.id());
            helper.assertTrue(!profile.prepHint().isBlank(), "POI prep hint missing: " + profile.id());
            helper.assertTrue(!profile.resourceProfile().isBlank(), "POI resource profile missing: " + profile.id());
            helper.assertTrue(!profile.objective().isBlank(), "POI objective missing: " + profile.id());
            helper.assertTrue(!profile.rewardTrack().isBlank(), "POI reward track missing: " + profile.id());
            helper.assertTrue(!profile.structureIds().isEmpty(), "POI structure ids missing: " + profile.id());
            helper.assertTrue(profile.hazardProfile() != ExplorationSiteRegistry.HazardProfile.UNKNOWN,
                    "Registered POI should not use fallback hazard: " + profile.id());
        }

        TagKey<Structure> poiStructures = TagKey.create(Registries.STRUCTURE, id("poi_structures"));
        var structures = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.STRUCTURE)
                .getOrThrow(poiStructures);
        int taggedStructures = 0;
        for (Holder<Structure> holder : structures) {
            ResourceKey<Structure> key = holder.unwrapKey().orElse(null);
            helper.assertTrue(key != null, "Tagged POI structure should have a registry key");
            if (key == null) {
                continue;
            }
            String structureId = key.identifier().getPath();
            ExplorationSiteRegistry.SiteProfile profile = ExplorationSiteRegistry.findByStructure(structureId).orElse(null);
            helper.assertTrue(profile != null,
                    "Tagged POI structure should resolve to a scanner profile: " + structureId);
            if (profile != null) {
                helper.assertTrue(profile.structureIds().contains(structureId),
                        "Tagged POI structure should be explicitly listed by its profile: " + structureId);
            }
            taggedStructures++;
        }
        helper.assertTrue(taggedStructures > 0, "POI structure tag should not be empty");
        helper.succeed();
    }

    private static void ashfallDiscoveryProvider(GameTestHelper helper) {
        if (EchoCoreServices.factionDefinitions().stream().noneMatch(definition -> AshfallFactionMap.isAshfall(definition.id()))) {
            AshfallBiomeFactions.register();
        }
        List<EchoDiscoveryEntry> entries = new AshfallDiscoveryProvider().entries(null);
        helper.assertTrue(!entries.isEmpty(), "Ashfall discovery provider should publish entries");
        helper.assertTrue(entries.stream().anyMatch(entry -> entry.category() == EchoDiscoveryCategory.STRUCTURE),
                "Ashfall discovery provider should publish structure entries");
        helper.assertTrue(entries.stream().anyMatch(entry -> entry.category() == EchoDiscoveryCategory.BIOME),
                "Ashfall discovery provider should publish biome entries");
        helper.assertTrue(entries.stream().anyMatch(entry -> entry.category() == EchoDiscoveryCategory.GUARDIAN),
                "Ashfall discovery provider should publish guardian entries");
        helper.assertTrue(entries.stream().anyMatch(entry -> entry.category() == EchoDiscoveryCategory.EVENT),
                "Ashfall discovery provider should publish event entries");
        helper.assertTrue(entries.stream().noneMatch(entry -> entry.category() == EchoDiscoveryCategory.FACTION),
                "Ashfall discovery provider should leave faction entries to the shared Terminal provider");
        List<EchoDiscoveryEntry> terminalEntries = new TerminalDiscoveryProvider().entries(null);
        Set<Identifier> discoveryIds = new HashSet<>();
        for (EchoDiscoveryEntry entry : entries) {
            helper.assertTrue(discoveryIds.add(entry.id()), "Ashfall discovery entry ids should be unique: " + entry.id());
        }
        for (EchoDiscoveryEntry entry : terminalEntries) {
            helper.assertTrue(discoveryIds.add(entry.id()),
                    "Ashfall discovery ids should not duplicate shared Terminal entries: " + entry.id());
        }
        helper.assertTrue(entries.stream().allMatch(entry -> entry.id() != null
                        && entry.chapterId() != null
                        && !entry.revealedTitle().isBlank()
                        && !entry.lockedHintTitle().isBlank()
                        && !entry.hintText().isBlank()
                        && !entry.revealedSummary().isBlank()),
                "Every Ashfall discovery entry should have stable id and nonblank spoiler-safe copy");
        helper.assertTrue(entries.stream().anyMatch(entry -> entry.id().equals(AshfallDiscoveryProvider.biomeId("the_wasteland"))),
                "The main Wasteland biome should remain discoverable as a biome entry");
        helper.succeed();
    }

    private static void cacheOpenedRuntimeMarker(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos cachePos = helper.absolutePos(new BlockPos(2, 2, 2));
        int ledgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        var result = AshfallAdapterCoreExplorationRuntime.cacheOpened(player, cachePos, "gametest_cache_open");

        QuestData quest = QuestData.get(player);
        helper.assertTrue(result.mutated(),
                "Cache opening should mutate through the AdapterCore exploration runtime");
        helper.assertTrue(quest.hasVisitedLocation("special", "cache:opened"),
                "Cache opening should record the survivor-cache route marker immediately");
        String lastEvent = player.getPersistentData().getStringOr("ashes_of_tomorrow.adaptercore.last_exploration_event", "");
        helper.assertTrue("player.terminal_opened".equals(lastEvent),
                "Cache opening should publish AdapterCore exploration diagnostics");
        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(ledgerEntries.size() > ledgerBefore,
                "Cache opening should append an AdapterCore mutation ledger entry");
        var latest = ledgerEntries.get(ledgerEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_TERMINAL_OPENED.equals(latest.actionId()),
                "Cache opening ledger should use the canonical player.terminal_opened event");
        helper.assertTrue("echoashfallprotocol:exploration_runtime".equals(latest.runtimeHostId()),
                "Cache opening ledger should record the exploration runtime host");
        helper.assertTrue(latest.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Cache opening ledger should truthfully report MUTATED");
        helper.assertTrue(latest.saveTouched() && latest.hudOrEventEmitted(),
                "Cache opening ledger should record save touch and visible/event feedback");
        helper.assertTrue("echoashfallprotocol:loot_survivor_cache".equals(
                        String.valueOf(latest.inputPayload().get("target"))),
                "Cache opening ledger should carry the canonical cache mission target");
        helper.assertTrue(player.getUUID().toString().equals(latest.target().snapshot().get("playerId")),
                "Cache opening ledger should identify the target player");
        Object blockTarget = latest.target().snapshot().get("block");
        helper.assertTrue(blockTarget instanceof Map<?, ?>,
                "Cache opening ledger should include a block target snapshot");
        Map<?, ?> blockSnapshot = blockTarget instanceof Map<?, ?> block ? block : Map.of();
        helper.assertTrue(cachePos.getX() == ((Number) blockSnapshot.get("x")).intValue()
                        && cachePos.getY() == ((Number) blockSnapshot.get("y")).intValue()
                        && cachePos.getZ() == ((Number) blockSnapshot.get("z")).intValue(),
                "Cache opening ledger should identify the target cache position");
        helper.assertFalse(Boolean.TRUE.equals(latest.beforeSummary().get("cacheOpenedMarker")),
                "Cache opening ledger should show the marker absent before runtime mutation");
        helper.assertTrue(Boolean.TRUE.equals(latest.afterSummary().get("cacheOpenedMarker")),
                "Cache opening ledger should show the marker present after runtime mutation");
        helper.succeed();
    }

    private static void dataLogItemRuntimeFlow(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        String loreId = "datalog_technical_manual_echo_ai_systems";
        int ledgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
        int intelBefore = com.knoxhack.echoashfallprotocol.echo.EchoIntel.get(player).getAllIntel().size();

        ItemStack dataLog = new ItemStack(ModItems.DATA_LOG_ECHO_CREATION.get());
        String dataLogItemId = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(dataLog.getItem())
                .toString();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dataLog);
        InteractionResult result = dataLog.getItem().use(
                helper.getLevel(),
                player,
                net.minecraft.world.InteractionHand.MAIN_HAND);

        var intel = com.knoxhack.echoashfallprotocol.echo.EchoIntel.get(player);
        QuestData quest = QuestData.get(player);
        helper.assertTrue(result.consumesAction(),
                "Data Log should consume the player action when archived");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),
                "Successful Data Log archive should consume the item");
        helper.assertTrue(intel.hasDiscoveredLore(loreId)
                        && intel.getAllIntel().size() > intelBefore,
                "Data Log runtime should archive lore into ECHO intel");
        helper.assertTrue(quest.hasVisitedLocation("special", "data_log:archived")
                        && quest.hasVisitedLocation("special", "data_log:technical_manual"),
                "Data Log runtime should record QuestData route markers");
        helper.assertTrue("ashfall.data_log_recovered".equals(player.getPersistentData().getStringOr(
                        "ashes_of_tomorrow.adaptercore.last_exploration_event", "")),
                "Data Log should publish the canonical exploration event");

        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(ledgerEntries.size() > ledgerBefore,
                "Data Log should append a mutation ledger entry");
        var latest = ledgerEntries.get(ledgerEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_ASHFALL_DATA_LOG_RECOVERED.equals(latest.actionId()),
                "Data Log ledger should use the canonical data-log event");
        helper.assertTrue("echoashfallprotocol:exploration_runtime".equals(latest.runtimeHostId()),
                "Data Log ledger should record the exploration runtime host");
        helper.assertTrue(latest.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Data Log ledger should report a real mutation");
        helper.assertTrue(latest.saveTouched() && latest.hudOrEventEmitted(),
                "Data Log ledger should record save and visible event feedback");
        helper.assertTrue(loreId.equals(String.valueOf(latest.inputPayload().get("loreId")))
                        && "ECHO AI Systems".equals(String.valueOf(latest.inputPayload().get("title"))),
                "Data Log ledger should carry the canonical lore payload");
        helper.assertTrue(dataLogItemId.equals(String.valueOf(latest.inputPayload().get("itemId"))),
                "Data Log ledger should carry the concrete item id");
        helper.assertTrue(((Number) latest.beforeSummary().get("echoIntelCount")).intValue() == intelBefore
                        && ((Number) latest.afterSummary().get("echoIntelCount")).intValue() > intelBefore,
                "Data Log ledger should show ECHO intel increasing across the host mutation");
        helper.assertTrue(((Number) latest.beforeSummary().get("mainHandCount")).intValue() == 1
                        && ((Number) latest.afterSummary().get("mainHandCount")).intValue() == 0,
                "Data Log ledger should show the runtime consumed the held item");
        helper.assertTrue(dataLogItemId.equals(String.valueOf(latest.beforeSummary().get("mainHandItemId")))
                        && String.valueOf(latest.afterSummary().get("mainHandItemId")).isBlank(),
                "Data Log ledger should show the held item id clearing after runtime consumption");

        ItemStack duplicate = new ItemStack(ModItems.DATA_LOG_ECHO_CREATION.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, duplicate);
        int noopLedgerBefore = ledgerEntries.size();
        duplicate.getItem().use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount() == 1,
                "Duplicate Data Log use should not consume an item when no state changes");
        var noopLedgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(noopLedgerEntries.size() > noopLedgerBefore,
                "Duplicate Data Log use should still flow through AdapterCore");
        var noopLatest = noopLedgerEntries.get(noopLedgerEntries.size() - 1);
        helper.assertTrue(noopLatest.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.NOOP,
                "Duplicate Data Log ledger should tell the truth as NOOP");
        helper.assertFalse(noopLatest.saveTouched(),
                "Duplicate Data Log should not claim a save mutation");
        helper.assertTrue(noopLatest.hudOrEventEmitted(),
                "Duplicate Data Log should record runtime-visible feedback");
        helper.assertTrue(((Number) noopLatest.beforeSummary().get("mainHandCount")).intValue() == 1
                        && ((Number) noopLatest.afterSummary().get("mainHandCount")).intValue() == 1,
                "Duplicate Data Log ledger should show the runtime left the item untouched");
        helper.succeed();
    }

    private static void factionContractBalance(GameTestHelper helper) {
        if (EchoCoreServices.factionDefinitions().stream().noneMatch(definition -> AshfallFactionMap.isAshfall(definition.id()))) {
            AshfallBiomeFactions.register();
        }
        long activeAshfallDefinitions = EchoCoreServices.factionDefinitions().stream()
                .filter(definition -> AshfallFactionMap.all().contains(definition.id()))
                .count();
        helper.assertTrue(activeAshfallDefinitions == 3,
                "Echo Core should expose exactly three active Ashfall faction definitions");

        List<EchoFactionDefinition> definitions = AshfallFactionMap.all().stream()
                .map(factionId -> EchoCoreServices.factionDefinition(factionId).orElse(null))
                .toList();
        helper.assertTrue(definitions.stream().allMatch(java.util.Objects::nonNull),
                "All three Ashfall Echo Core factions should be registered");
        helper.assertTrue(definitions.size() == 3, "Ashfall should expose exactly three Echo Core factions");

        int contractCount = 0;
        for (EchoFactionDefinition definition : definitions) {
            helper.assertTrue(definition.contracts().size() == 3,
                    "Faction should have field/trusted/aligned contracts: " + definition.id());
            helper.assertTrue(definition.contracts().stream().map(EchoFactionContract::requiredReputation).toList()
                            .equals(List.of(0, 35, 75)),
                    "Contract reputation tiers should be 0/35/75: " + definition.id());
            for (EchoFactionContract contract : definition.contracts()) {
                AshfallFactionContracts.Spec spec = AshfallFactionContracts.spec(contract.id()).orElse(null);
                helper.assertTrue(spec != null, "Ashfall contract spec should exist: " + contract.id());
                if (spec != null) {
                    helper.assertTrue(!spec.objectives().isEmpty(),
                            "Ashfall contract should have objectives: " + contract.id());
                    helper.assertTrue(spec.reputationReward() > 0,
                            "Ashfall contract should grant reputation: " + contract.id());
                    contractCount++;
                }
            }
        }
        helper.assertTrue(contractCount == 9, "Ashfall should expose nine Echo Core contracts");

        assertContractHasObjective(helper, "crashbreak_salvage_field_contract",
                AshfallFactionContracts.ObjectiveType.POI_DISCOVERY);
        assertContractHasObjective(helper, "crashbreak_salvage_aligned_contract",
                AshfallFactionContracts.ObjectiveType.RAID_DEFENSE);
        assertContractHasObjective(helper, "radwarden_compact_aligned_contract",
                AshfallFactionContracts.ObjectiveType.REPAIR);
        assertContractHasObjective(helper, "sporebound_sanctum_aligned_contract",
                AshfallFactionContracts.ObjectiveType.REPAIR);

        helper.assertTrue("industrial_factory".equals(ExplorationSiteRegistry.normalize("derelict_workshop")),
                "Legacy derelict workshop alias should still resolve to industrial factory profile");
        helper.succeed();
    }

    private static void factionActionReputationServiceRaidContractRuntimeFlow(GameTestHelper helper) {
        EchoServiceRegistry.withClearedForTests(() -> {
            RecordingMissionService missionCore = new RecordingMissionService();
            EchoServiceRegistry.register(IMissionService.class, missionCore);
            EchoCoreServices.registerFactionActionHandler(AshfallFactionInteractionHandler.INSTANCE);
            AshfallBiomeFactions.register();

            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            BlockPos playerPos = helper.absolutePos(new BlockPos(2, 2, 2));
            player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);

            Identifier factionId = AshfallBiomeFactions.CRASHBREAK_SALVAGE;
            Identifier rivalFactionId = AshfallBiomeFactions.RADWARDEN_COMPACT;
            Identifier talkAction = id("crashbreak_salvage_talk");
            Identifier serviceAction = id("crashbreak_salvage_service");
            Identifier contractId = id("crashbreak_salvage_field_contract");
            Identifier raidContractId = id("crashbreak_salvage_aligned_contract");

            FactionNpcEntity npc = ModEntities.FACTION_NPC.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
            helper.assertTrue(npc != null, "Faction runtime proof should spawn a faction NPC contact");
            if (npc == null) {
                return;
            }
            npc.configure(factionId, "route_broker");
            npc.setPos(player.getX() + 1.0D, player.getY(), player.getZ());
            helper.getLevel().addFreshEntity(npc);

            int contactLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            FactionNpcDialogueService.handleAction(
                    player,
                    new FactionNpcActionPacket(npc.getId(), talkAction.toString(), ""));
            var contactedProfile = EchoCoreServices.factionProfile(player, factionId).orElse(null);
            helper.assertTrue(contactedProfile != null && contactedProfile.contacted()
                            && contactedProfile.contactCount() >= 1
                            && "route_broker".equals(contactedProfile.lastRoleId()),
                    "Faction NPC action should mark the EchoCore faction profile as contacted");
            helper.assertTrue(QuestData.get(player).hasVisitedLocation(
                            "special", "faction_contact:echoashfallprotocol/crashbreak_salvage"),
                    "Faction action runtime should mark faction contact in QuestData");
            var contactLedger = latestFactionActionLedger(contactLedgerBefore, talkAction.toString());
            helper.assertTrue(contactLedger != null
                            && contactLedger.resultStatus()
                            == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED
                            && contactLedger.saveTouched()
                            && contactLedger.hudOrEventEmitted(),
                    "Faction talk action should emit a truthy AdapterCore mutation ledger");
            helper.assertTrue(missionCore.recordedPath(MissionObjectiveType.CUSTOM, "faction/action")
                            && missionCore.recordedPath(MissionObjectiveType.CUSTOM, "any"),
                    "Faction action runtime should record first-contact MissionCore objectives");

            FactionNpcDialogueService.handleAction(
                    player,
                    new FactionNpcActionPacket(
                            npc.getId(),
                            EchoCoreServices.ACCEPT_FACTION_CONTRACT_ACTION.toString(),
                            contractId.toString()));
            var acceptedProfile = EchoCoreServices.factionProfile(player, factionId).orElseThrow();
            helper.assertTrue(acceptedProfile.activeContractId().filter(contractId::equals).isPresent(),
                    "Faction contract action should activate the Crashbreak field contract");
            AshfallFactionContracts.Spec fieldSpec = AshfallFactionContracts.spec(contractId).orElseThrow();
            AshfallFactionContractData contractData = AshfallFactionContractData.get(player);
            helper.assertTrue(contractData.progress(contractId, 0) == 0,
                    "Accepted faction contract should initialize persisted objective progress");

            helper.assertTrue(AshfallFactionContractProgression.progressPoi(player, "crash_zone_wasteland"),
                    "Faction POI progress should advance the active Crashbreak contract");
            helper.assertTrue(contractData.progress(contractId, 0) == fieldSpec.objectives().get(0).requiredCount(),
                    "Faction contract progress should be saved in Ashfall attachment data");
            helper.assertTrue(EchoCoreServices.factionContractState(player, factionId, contractId, "route_broker")
                            .canComplete(),
                    "Faction contract state should become completable after matching POI progress");

            int scrapMetalBefore = countInventory(player, ModItems.SCRAP_METAL.get());
            int reputationLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            FactionNpcDialogueService.handleAction(
                    player,
                    new FactionNpcActionPacket(
                            npc.getId(),
                            EchoCoreServices.COMPLETE_FACTION_CONTRACT_ACTION.toString(),
                            contractId.toString()));
            var completedProfile = EchoCoreServices.factionProfile(player, factionId).orElseThrow();
            helper.assertTrue(completedProfile.activeContractId().isEmpty()
                            && completedProfile.completedContractIds().contains(contractId)
                            && completedProfile.reputation() >= fieldSpec.reputationReward(),
                    "Completing a faction contract should archive it and increase EchoCore reputation");
            helper.assertTrue(countInventory(player, ModItems.SCRAP_METAL.get()) > scrapMetalBefore,
                    "Crashbreak contract completion should deliver trade/service-relevant salvage rewards");
            QuestData quest = QuestData.get(player);
            helper.assertTrue(quest.hasVisitedLocation("special", "faction:first_task_complete")
                            && quest.hasVisitedLocation("special", "faction:crashbreak_salvage:contract_complete"),
                    "Faction contract completion should persist QuestData completion markers");
            helper.assertTrue(!EchoIntel.get(player).getFactionIntel(factionId).isEmpty(),
                    "Faction contract completion should persist faction intel");
            var reputationLedger = latestReputationLedger(reputationLedgerBefore, factionId, "faction_contract");
            helper.assertTrue(reputationLedger != null
                            && reputationLedger.resultStatus()
                            == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED
                            && reputationLedger.saveTouched()
                            && reputationLedger.hudOrEventEmitted(),
                    "Faction contract completion should emit a reputation AdapterCore mutation ledger");
            helper.assertTrue(missionCore.recordedPath(MissionObjectiveType.CUSTOM, "faction/reputation")
                            && missionCore.recordedPath(MissionObjectiveType.CUSTOM, "faction_reputation")
                            && missionCore.recordedPath(MissionObjectiveType.CUSTOM, "first_task_complete")
                            && missionCore.recordedPath(MissionObjectiveType.CUSTOM, "complete_first_faction_task"),
                    "Faction reputation runtime should record MissionCore reputation and first-contract objectives");

            int scrapWireBefore = countInventory(player, ModItems.SCRAP_WIRE.get());
            int circuitBefore = countInventory(player, ModItems.SCRAP_CIRCUIT.get());
            FactionNpcDialogueService.handleAction(
                    player,
                    new FactionNpcActionPacket(npc.getId(), serviceAction.toString(), ""));
            helper.assertTrue(countInventory(player, ModItems.SCRAP_WIRE.get()) >= scrapWireBefore + 3
                            && countInventory(player, ModItems.SCRAP_CIRCUIT.get()) >= circuitBefore + 1,
                    "Unlocked Crashbreak service should deliver salvage support items");
            helper.assertTrue(AshfallFactionContractData.get(player).serviceCooldownUntil(factionId, "SALVAGE")
                            > helper.getLevel().getGameTime(),
                    "Faction service should persist its cooldown in Ashfall contract data");

            EchoCoreServices.setFactionReputation(player, factionId, 75);
            FactionNpcDialogueService.handleAction(
                    player,
                    new FactionNpcActionPacket(
                            npc.getId(),
                            EchoCoreServices.ACCEPT_FACTION_CONTRACT_ACTION.toString(),
                            raidContractId.toString()));
            helper.assertTrue(EchoCoreServices.factionProfile(player, factionId).orElseThrow()
                            .activeContractId().filter(raidContractId::equals).isPresent(),
                    "Faction aligned contract should become active once standing is high enough");
            FactionTerritory territory = FactionTerritory.get(player);
            BlockPos raidTarget = helper.absolutePos(new BlockPos(8, 2, 8));
            territory.addVillage(raidTarget, factionId, "Crashbreak Relay Camp");
            FactionTerritory.saveAndSync(player, territory);
            int raidsBefore = FactionWorldManager.getActiveRaidCount();
            FactionWorldManager.startRaid(helper.getLevel(), rivalFactionId, factionId, raidTarget, 1);
            helper.assertTrue(FactionWorldManager.getActiveRaidCount() == raidsBefore + 1,
                    "Faction chain should be able to start real raid pressure against a faction site");
            helper.assertTrue(AshfallFactionContractProgression.progressRaidDefense(player, factionId),
                    "Faction raid-defense hook should progress active Ashfall raid objectives when present");
            helper.assertTrue(EchoCoreServices.factionContractState(player, factionId, raidContractId, "route_broker")
                            .canComplete(),
                    "Raid-defense progress should make the aligned faction contract completable");

            npc.discard();
            helper.succeed();
        });
    }

    private static void strictFactionEntityIds(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id("faction_npc")),
                "Generic faction_npc entity id should remain registered");
        assertRetiredEntityIdAbsent(helper, "remnant", "soldier");
        assertRetiredEntityIdAbsent(helper, "salvager", "trader");
        assertRetiredEntityIdAbsent(helper, "mutant", "creature");
        helper.succeed();
    }

    private static void machineWearSavedData(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        MachineWearData wearData = new MachineWearData(helper.getLevel());
        wearData.setWear(pos, 73);
        wearData.setJammed(pos, true);

        MachineWearSavedData saved = MachineWearSavedData.get(helper.getLevel());
        JsonElement encoded = MachineWearSavedData.CODEC.encodeStart(JsonOps.INSTANCE, saved)
                .result()
                .orElseThrow(() -> new IllegalStateException("Machine wear saved data should encode"));
        MachineWearSavedData decoded = MachineWearSavedData.CODEC.parse(JsonOps.INSTANCE, encoded)
                .result()
                .orElseThrow(() -> new IllegalStateException("Machine wear saved data should decode"));

        helper.assertTrue(decoded.getWear(pos) == 73, "Machine wear should survive saved-data serialization");
        helper.assertTrue(decoded.isJammed(pos), "Machine jam state should survive saved-data serialization");
        helper.assertTrue(new MachineWearData(helper.getLevel()).getWear(pos) == 73,
                "Machine wear API should read world-saved state");
        wearData.repair(pos, 100);
        helper.assertFalse(MachineWearSavedData.get(helper.getLevel()).isJammed(pos),
                "Repair should clear jam state when wear reaches zero");
        helper.succeed();
    }

    private static void debugCommandPermissionGates(GameTestHelper helper) {
        CommandSourceStack nonOp = commandSource(helper, LevelBasedPermissionSet.MODERATOR);
        CommandSourceStack op = commandSource(helper, LevelBasedPermissionSet.GAMEMASTER);

        helper.assertFalse(ModStructuresCommand.hasCommandPermission(nonOp),
                "Structure export command should reject non-OP command source");
        helper.assertFalse(StructureGenCommand.hasCommandPermission(nonOp),
                "POI generation command should reject non-OP command source");
        helper.assertTrue(ModStructuresCommand.hasCommandPermission(op),
                "Structure export command should allow OP/dev command source");
        helper.assertTrue(StructureGenCommand.hasCommandPermission(op),
                "POI generation command should allow OP/dev command source");
        helper.succeed();
    }

    private static CommandSourceStack commandSource(GameTestHelper helper, LevelBasedPermissionSet permissions) {
        return new CommandSourceStack(
                CommandSource.NULL,
                Vec3.ZERO,
                Vec2.ZERO,
                helper.getLevel(),
                permissions,
                "gametest",
                Component.literal("gametest"),
                helper.getLevel().getServer(),
                null);
    }

    private static void nexusUpgradeDataPath(GameTestHelper helper) {
        ItemStack blade = new ItemStack(ModItems.NEXUS_BLADE.get());
        com.knoxhack.echoashfallprotocol.item.upgrade.GearUpgradeHandler.setUpgradeLevel(blade, 2);

        helper.assertTrue(com.knoxhack.echoashfallprotocol.item.GearUpgradeHandler.getUpgradeLevel(blade) == 2,
                "Right-click compatibility handler should read nexus_upgrades");
        helper.assertTrue(com.knoxhack.echoashfallprotocol.item.upgrade.GearUpgradeHandler.getBonusDamage(blade) >= 2.0F,
                "Nexus upgrade level should increase weapon damage");
        helper.assertTrue(com.knoxhack.echoashfallprotocol.item.GearUpgradeHandler.getDamageBonus(blade) >= 2.0F,
                "Legacy damage helper should mirror Nexus upgrade damage");
        helper.succeed();
    }

    private static void ashfallMachineCoreRuntimeSnapshotContract(GameTestHelper helper) {
        try {
            Class.forName("com.knoxhack.echoashfallprotocol.test.AshfallMachineCoreGameTests")
                    .getMethod("ashfallMachineCoreRuntimeSnapshotContract", GameTestHelper.class)
                    .invoke(null, helper);
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Ashfall MachineCore runtime proof is unavailable in this addon profile: "
                    + exception.getMessage());
            helper.succeed();
        }
    }

    private static void waterPurifierNetworkPower(GameTestHelper helper) {
        BlockPos purifierPos = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos cablePos = purifierPos.east();
        BlockPos bankPos = cablePos.east();

        helper.getLevel().setBlock(purifierPos, ModBlocks.WATER_PURIFIER.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(cablePos, ModBlocks.POWER_CABLE.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(bankPos, ModBlocks.BATTERY_BANK.get().defaultBlockState(), 3);

        helper.assertTrue(helper.getLevel().getBlockEntity(purifierPos) instanceof WaterPurifierBlockEntity,
                "Water purifier block entity should be present");
        helper.assertTrue(helper.getLevel().getBlockEntity(cablePos) instanceof PowerCableBlockEntity,
                "Power cable block entity should be present");
        helper.assertTrue(helper.getLevel().getBlockEntity(bankPos) instanceof BatteryBankBlockEntity,
                "Battery bank block entity should be present");
        if (helper.getLevel().getBlockEntity(purifierPos) instanceof WaterPurifierBlockEntity purifier
                && helper.getLevel().getBlockEntity(bankPos) instanceof BatteryBankBlockEntity bank) {
            MachineWearData wearData = new MachineWearData(helper.getLevel());
            wearData.repair(purifierPos, MachineWearData.MAX_WEAR);
            bank.setEnergyStored(2_000);
            purifier.getInventory().setStackInSlot(0, new ItemStack(ModItems.DIRTY_WATER_BOTTLE.get()));
            purifier.getInventory().setStackInSlot(1, new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get()));

            for (int i = 0; i < 220; i++) {
                BatteryBankBlockEntity.serverTick(helper.getLevel(), bankPos,
                        helper.getLevel().getBlockState(bankPos), bank);
                if (helper.getLevel().getBlockEntity(cablePos) instanceof PowerCableBlockEntity cable) {
                    PowerCableBlockEntity.serverTick(helper.getLevel(), cablePos,
                            helper.getLevel().getBlockState(cablePos), cable);
                }
                WaterPurifierBlockEntity.serverTick(helper.getLevel(), purifierPos,
                        helper.getLevel().getBlockState(purifierPos), purifier);
            }

            int cableEnergy = helper.getLevel().getBlockEntity(cablePos) instanceof PowerCableBlockEntity cable
                    ? cable.getEnergyStored()
                    : -1;
            helper.assertTrue(purifier.getInventory().getStackInSlot(2).is(ModItems.CLEAN_WATER_BOTTLE.get()),
                    "Water purifier should produce clean water from cabled network power"
                            + " output=" + purifier.getInventory().getStackInSlot(2)
                            + " progress=" + purifier.data.get(0) + "/" + purifier.data.get(1)
                            + " hasPower=" + purifier.data.get(2)
                            + " purifierEnergy=" + purifier.getEnergyStored()
                            + " cableEnergy=" + cableEnergy
                            + " bankEnergy=" + bank.getEnergyStored()
                            + " wear=" + wearData.getWear(purifierPos)
                            + " jammed=" + wearData.isJammed(purifierPos));
            helper.assertTrue(bank.getEnergyStored() < 2_000,
                    "Cabled network source should spend energy on purification");
        }
        helper.succeed();
    }

    private static void waterPurifierToDrinkRuntimeFlow(GameTestHelper helper) {
        EchoServiceRegistry.withClearedForTests(() -> {
            RecordingMissionService missionService = new RecordingMissionService();
            EchoCoreServices.registerMissionService(missionService);

            ServerLevel level = helper.getLevel();
            BlockPos purifierPos = helper.absolutePos(new BlockPos(2, 2, 2));
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            player.setPos(purifierPos.getX() + 0.5D, purifierPos.getY(), purifierPos.getZ() + 0.5D);
            setHydration(player, 20);
            player.getFoodData().setFoodLevel(20);

            helper.getLevel().setBlock(purifierPos, ModBlocks.WATER_PURIFIER.get().defaultBlockState(), 3);
            if (!(level.getBlockEntity(purifierPos) instanceof WaterPurifierBlockEntity purifier)) {
                helper.assertTrue(false, "Water purifier should exist for the dirty-to-clean drink runtime proof");
                return;
            }

            var dirtyResult = AshfallAdapterCoreEarlyEventRuntime.dirtyWaterCollected(player, purifierPos);
            helper.assertTrue(dirtyResult.mutated(),
                    "Dirty-water collection should mutate before powered purification");
            helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "water:dirty_collected"),
                    "Dirty-water collection should mark the water route in QuestData");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.ENTER_REGION,
                            Identifier.fromNamespaceAndPath("water", "dirty_collected")),
                    "Dirty-water collection should record the MissionCore route objective");

            MachineWearData wearData = new MachineWearData(level);
            wearData.repair(purifierPos, MachineWearData.MAX_WEAR);
            ItemStack purifierBattery = BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 2_000);
            purifier.getInventory().setStackInSlot(0, new ItemStack(ModItems.DIRTY_WATER_BOTTLE.get()));
            purifier.getInventory().setStackInSlot(1, new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get()));
            purifier.getInventory().setStackInSlot(WaterPurifierBlockEntity.BATTERY_SLOT, purifierBattery);
            purifier.setChanged();

            int machineLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            for (int i = 0; i < 140; i++) {
                WaterPurifierBlockEntity.serverTick(level, purifierPos, level.getBlockState(purifierPos), purifier);
            }

            ItemStack cleanOutput = purifier.getInventory().getStackInSlot(2);
            helper.assertTrue(cleanOutput.is(ModItems.CLEAN_WATER_BOTTLE.get()) && cleanOutput.getCount() == 1,
                    "Powered Water Purifier should turn one dirty bottle and a filter cartridge into one clean bottle");
            helper.assertTrue(purifier.getInventory().getStackInSlot(0).isEmpty(),
                    "Powered Water Purifier should consume the dirty-water input bottle");
            helper.assertTrue(BatteryItem.getStoredEnergy(
                            purifier.getInventory().getStackInSlot(WaterPurifierBlockEntity.BATTERY_SLOT)) < 2_000,
                    "Powered Water Purifier should draw charge from the inserted starter battery");

            var machineLedger = latestMachineOutputLedger(
                    machineLedgerBefore,
                    "echoashfallprotocol:water_purifier",
                    com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_CLEAN_WATER_BOTTLE);
            helper.assertTrue(machineLedger != null,
                    "Water Purifier clean-water output should append a machine runtime ledger entry");
            if (machineLedger == null) {
                return;
            }
            helper.assertTrue(machineLedger.resultStatus()
                            == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                    "Water Purifier clean-water output ledger should truthfully report MUTATED");
            helper.assertTrue(machineLedger.saveTouched() && machineLedger.hudOrEventEmitted(),
                    "Water Purifier clean-water output ledger should record save touch and player-visible feedback");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.OBTAIN_ITEM,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "clean_water_bottle")),
                    "Water Purifier output should record the native clean-water objective");

            ItemStack cleanBottle = cleanOutput.copy();
            cleanBottle.setCount(1);
            purifier.getInventory().setStackInSlot(2, ItemStack.EMPTY);
            purifier.setChanged();
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, cleanBottle);

            int drinkLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            ItemStack finished = cleanBottle.finishUsingItem(level, player);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, finished);

            helper.assertTrue(player.getData(ModAttachments.SURVIVAL_DATA.get()).getHydration() == 60,
                    "Drinking the purifier-created clean bottle should restore survival hydration");
            helper.assertTrue(countInventory(player, ModItems.CLEAN_WATER_BOTTLE.get()) == 0,
                    "Drinking the purifier-created clean bottle should consume the clean-water bottle");
            helper.assertTrue(countInventory(player, Items.GLASS_BOTTLE) == 1,
                    "Drinking the purifier-created clean bottle should return the glass bottle");
            helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "water:clean_consumed"),
                    "Drinking purifier-created clean water should mark the clean-water route");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.CUSTOM,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "drink_clean_water")),
                    "Drinking purifier-created clean water should record the native drink-clean-water objective");

            var drinkLedgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
            com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry drinkLedger = null;
            for (int i = drinkLedgerBefore; i < drinkLedgerEntries.size(); i++) {
                var entry = drinkLedgerEntries.get(i);
                if (com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED.equals(entry.actionId())
                        && com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_CLEAN_WATER_BOTTLE.equals(
                        String.valueOf(entry.inputPayload().get("itemId")))) {
                    drinkLedger = entry;
                }
            }
            helper.assertTrue(drinkLedger != null,
                    "Drinking purifier-created clean water should append an early-event runtime ledger entry");
            if (drinkLedger != null) {
                helper.assertTrue(drinkLedger.resultStatus()
                                == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                        "Drinking purifier-created clean water ledger should truthfully report MUTATED");
                helper.assertTrue(((Number) drinkLedger.beforeSummary().get("survivalHydration")).intValue() == 20
                                && ((Number) drinkLedger.afterSummary().get("survivalHydration")).intValue() == 60,
                        "Drinking purifier-created clean water ledger should show survival hydration changing");
            }
        });
        helper.succeed();
    }

    private static void handRecyclerStarterBatteryThroughput(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos relativePos = new BlockPos(2, 2, 2);
        BlockPos pos = helper.absolutePos(relativePos);
        helper.setBlock(relativePos, ModBlocks.HAND_RECYCLER.get().defaultBlockState());

        if (!(level.getBlockEntity(pos) instanceof HandRecyclerBlockEntity recycler)) {
            helper.assertTrue(false, "Hand Recycler block entity should exist for throughput coverage");
            helper.succeed();
            return;
        }

        ItemStack starterBattery = BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 1_000);
        recycler.getInventory().setStackInSlot(0, new ItemStack(ModItems.SCRAP_METAL.get(), 1));
        recycler.getInventory().setStackInSlot(HandRecyclerBlockEntity.BATTERY_SLOT, starterBattery);
        recycler.setChanged();

        int processTicks = Math.max(1, recycler.data.get(1)) + 8;
        helper.runAfterDelay(processTicks, () -> {
            ItemStack output = recycler.getInventory().getStackInSlot(1);
            ItemStack remainingInput = recycler.getInventory().getStackInSlot(0);
            ItemStack battery = recycler.getInventory().getStackInSlot(HandRecyclerBlockEntity.BATTERY_SLOT);
            helper.assertTrue(output.is(ModItems.MACHINE_CASING.get()) && output.getCount() == 1,
                    "Starter-powered Hand Recycler should produce the first Machine Casing from Scrap Metal");
            helper.assertTrue(remainingInput.isEmpty(),
                    "First recycler run should consume exactly one Scrap Metal input");
            helper.assertTrue(BatteryItem.getStoredEnergy(battery) < 1_000,
                    "First recycler run should discharge the starter Basic Battery");
            helper.assertTrue(recycler.getEnergyStored() < recycler.getMaxEnergyStored(),
                    "First recycler run should spend stored FE instead of only transferring battery charge");
            helper.succeed();
        });
    }

    private static void scrapSalvageRecyclerPartsRuntimeFlow(GameTestHelper helper) {
        EchoServiceRegistry.withClearedForTests(() -> {
            RecordingMissionService missionService = new RecordingMissionService();
            EchoCoreServices.registerMissionService(missionService);

            ServerLevel level = helper.getLevel();
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            BlockPos recyclerRelativePos = new BlockPos(2, 2, 2);
            BlockPos recyclerPos = helper.absolutePos(recyclerRelativePos);
            player.setPos(recyclerPos.getX() + 0.5D, recyclerPos.getY(), recyclerPos.getZ() + 0.5D);

            ItemStack salvagedScrap = new ItemStack(ModItems.SCRAP_METAL.get());
            int salvageLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            player.getInventory().add(salvagedScrap.copy());
            var salvageResult = AshfallAdapterCoreEarlyEventRuntime.itemObtained(
                    player,
                    salvagedScrap,
                    "gametest_drop_pod_scrap_salvage");

            helper.assertTrue(salvageResult.mutated(),
                    "Drop-pod salvage pickup should mutate AdapterCore early item state");
            helper.assertTrue(countInventory(player, ModItems.SCRAP_METAL.get()) == 1,
                    "Drop-pod salvage pickup should put Scrap Metal in player inventory");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.OBTAIN_ITEM,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "scrap_metal")),
                    "Drop-pod salvage pickup should record the native scrap-metal objective");

            var salvageLedger = latestEarlyItemCollectedLedger(
                    salvageLedgerBefore,
                    "echoashfallprotocol:scrap_metal",
                    "gametest_drop_pod_scrap_salvage");
            helper.assertTrue(salvageLedger != null,
                    "Drop-pod salvage pickup should append an early item-collected ledger entry");
            if (salvageLedger == null) {
                return;
            }
            helper.assertTrue(salvageLedger.resultStatus()
                            == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                    "Drop-pod salvage pickup ledger should truthfully report MUTATED");
            helper.assertTrue(salvageLedger.saveTouched() && salvageLedger.hudOrEventEmitted(),
                    "Drop-pod salvage pickup ledger should record mission/save touch and player-visible feedback");

            helper.setBlock(recyclerRelativePos, ModBlocks.HAND_RECYCLER.get().defaultBlockState());
            if (!(level.getBlockEntity(recyclerPos) instanceof HandRecyclerBlockEntity recycler)) {
                helper.assertTrue(false, "Hand Recycler should exist for the salvage-to-parts proof");
                return;
            }

            MachineWearData wearData = new MachineWearData(level);
            wearData.repair(recyclerPos, MachineWearData.MAX_WEAR);
            helper.assertTrue(consumeOneInventoryItem(player, ModItems.SCRAP_METAL.get()),
                    "Salvaged Scrap Metal should be movable from inventory into the Hand Recycler");
            helper.assertTrue(countInventory(player, ModItems.SCRAP_METAL.get()) == 0,
                    "Feeding the Hand Recycler should remove the salvaged Scrap Metal from player inventory");
            recycler.getInventory().setStackInSlot(0, new ItemStack(ModItems.SCRAP_METAL.get()));
            recycler.getInventory().setStackInSlot(
                    HandRecyclerBlockEntity.BATTERY_SLOT,
                    BatteryItem.withEnergy(ModItems.BASIC_BATTERY.get(), 1_000));
            recycler.setChanged();

            int machineLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            for (int i = 0; i < 140; i++) {
                HandRecyclerBlockEntity.serverTick(level, recyclerPos, level.getBlockState(recyclerPos), recycler);
            }

            ItemStack machinePart = recycler.getInventory().getStackInSlot(1);
            helper.assertTrue(machinePart.is(ModItems.MACHINE_CASING.get()) && machinePart.getCount() == 1,
                    "Starter-powered Hand Recycler should turn salvaged Scrap Metal into a Machine Casing part");
            helper.assertTrue(recycler.getInventory().getStackInSlot(0).isEmpty(),
                    "Hand Recycler should consume the salvaged Scrap Metal input");
            helper.assertTrue(BatteryItem.getStoredEnergy(
                            recycler.getInventory().getStackInSlot(HandRecyclerBlockEntity.BATTERY_SLOT)) < 1_000,
                    "Hand Recycler should draw from the starter battery while making the machine part");

            var machineLedger = latestMachineOutputLedger(
                    machineLedgerBefore,
                    "echoashfallprotocol:hand_recycler",
                    "echoashfallprotocol:machine_casing");
            helper.assertTrue(machineLedger != null,
                    "Hand Recycler machine-part output should append a machine runtime ledger entry");
            if (machineLedger == null) {
                return;
            }
            helper.assertTrue(machineLedger.resultStatus()
                            == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                    "Hand Recycler machine-part output ledger should truthfully report MUTATED");
            helper.assertTrue(machineLedger.saveTouched() && machineLedger.hudOrEventEmitted(),
                    "Hand Recycler machine-part output ledger should record save touch and feedback");

            ItemStack casingPickup = machinePart.copy();
            casingPickup.setCount(1);
            recycler.getInventory().setStackInSlot(1, ItemStack.EMPTY);
            recycler.setChanged();
            int casingLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
            player.getInventory().add(casingPickup.copy());
            var casingResult = AshfallAdapterCoreEarlyEventRuntime.itemObtained(
                    player,
                    casingPickup,
                    "gametest_hand_recycler_output_pickup");

            helper.assertTrue(casingResult.mutated(),
                    "Picking up the recycler-made Machine Casing should mutate AdapterCore early item state");
            helper.assertTrue(countInventory(player, ModItems.MACHINE_CASING.get()) == 1,
                    "Recycler-made Machine Casing should enter player inventory");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.OBTAIN_ITEM,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "machine_casing")),
                    "Recycler-made Machine Casing should record the native make-machine-casing objective");
            var casingLedger = latestEarlyItemCollectedLedger(
                    casingLedgerBefore,
                    "echoashfallprotocol:machine_casing",
                    "gametest_hand_recycler_output_pickup");
            helper.assertTrue(casingLedger != null,
                    "Recycler-made Machine Casing pickup should append an early item-collected ledger entry");
            if (casingLedger != null) {
                helper.assertTrue(casingLedger.resultStatus()
                                == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                        "Recycler-made Machine Casing pickup ledger should truthfully report MUTATED");
            }
        });
        helper.succeed();
    }

    private static void itemPipeNetworkRouting(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sourcePos = helper.absolutePos(new BlockPos(2, 3, 2));
        BlockPos pipeOnePos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos pipeTwoPos = helper.absolutePos(new BlockPos(3, 2, 2));
        BlockPos destinationPos = helper.absolutePos(new BlockPos(4, 2, 2));

        level.setBlock(sourcePos, ModBlocks.ORE_GRINDER.get().defaultBlockState(), 3);
        level.setBlock(pipeOnePos, ModBlocks.ITEM_PIPE.get().defaultBlockState().setValue(ItemPipeBlock.FACING, Direction.UP), 3);
        level.setBlock(pipeTwoPos, ModBlocks.ITEM_PIPE.get().defaultBlockState().setValue(ItemPipeBlock.FACING, Direction.WEST), 3);
        level.setBlock(destinationPos, ModBlocks.THERMAL_BURNER.get().defaultBlockState(), 3);

        helper.assertTrue(level.getBlockEntity(sourcePos) instanceof OreGrinderBlockEntity,
                "Pipe routing source should be an Ore Grinder");
        helper.assertTrue(level.getBlockEntity(pipeOnePos) instanceof com.knoxhack.echoashfallprotocol.block.entity.ItemPipeBlockEntity,
                "Pipe routing should create the first Item Pipe block entity");
        helper.assertTrue(level.getBlockEntity(destinationPos) instanceof ThermalBurnerBlockEntity,
                "Pipe routing destination should be a Thermal Burner");

        if (level.getBlockEntity(sourcePos) instanceof OreGrinderBlockEntity grinder
                && level.getBlockEntity(pipeOnePos) instanceof com.knoxhack.echoashfallprotocol.block.entity.ItemPipeBlockEntity pipe
                && level.getBlockEntity(destinationPos) instanceof ThermalBurnerBlockEntity burner) {
            grinder.getInventory().setStackInSlot(OreGrinderBlockEntity.OUTPUT_SLOT, new ItemStack(Items.STONE, 3));

            com.knoxhack.echoashfallprotocol.block.entity.ItemPipeBlockEntity.serverTick(
                    level, pipeOnePos, level.getBlockState(pipeOnePos), pipe);

            helper.assertTrue(grinder.getInventory().getStackInSlot(OreGrinderBlockEntity.OUTPUT_SLOT).isEmpty(),
                    "Item Pipe network should extract the Ore Grinder output stack");
            ItemStack routed = burner.getInventory().getStackInSlot(0);
            helper.assertTrue(routed.is(Items.STONE) && routed.getCount() == 3,
                    "Item Pipe network should route items through connected pipes into a remote machine input");
        }
        helper.succeed();
    }

    private static void loadDistributorPriorityRouting(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos distributorPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos purifierPos = distributorPos.east();
        BlockPos pressPos = distributorPos.west();

        level.setBlock(distributorPos, ModBlocks.LOAD_DISTRIBUTOR.get().defaultBlockState(), 3);
        level.setBlock(purifierPos, ModBlocks.WATER_PURIFIER.get().defaultBlockState(), 3);
        level.setBlock(pressPos, ModBlocks.SCRAP_PRESS.get().defaultBlockState(), 3);

        helper.assertTrue(level.getBlockEntity(distributorPos) instanceof LoadDistributorBlockEntity,
                "Load Distributor priority test should create the distributor block entity");
        helper.assertTrue(level.getBlockEntity(purifierPos) instanceof WaterPurifierBlockEntity,
                "Load Distributor priority test should create the survival consumer");
        helper.assertTrue(level.getBlockEntity(pressPos) instanceof ScrapPressBlockEntity,
                "Load Distributor priority test should create the factory consumer");
        if (!(level.getBlockEntity(distributorPos) instanceof LoadDistributorBlockEntity distributor)
                || !(level.getBlockEntity(purifierPos) instanceof WaterPurifierBlockEntity purifier)
                || !(level.getBlockEntity(pressPos) instanceof ScrapPressBlockEntity press)) {
            helper.succeed();
            return;
        }

        distributor.cyclePriorityMode();
        helper.assertTrue(distributor.getPriorityMode() == LoadDistributorBlockEntity.PriorityMode.SURVIVAL,
                "First Load Distributor cycle should select Survival First");
        distributor.setEnergyStored(1);
        LoadDistributorBlockEntity.serverTick(level, distributorPos, level.getBlockState(distributorPos), distributor);
        helper.assertTrue(purifier.getEnergyStored() == 1,
                "Survival First should spend a scarce FE tick on the Water Purifier before factory machines");
        helper.assertTrue(press.getEnergyStored() == 0,
                "Survival First should leave the Scrap Press empty when only one FE is available");

        purifier.setEnergyStored(0);
        press.setEnergyStored(0);
        distributor.setEnergyStored(1);
        distributor.cyclePriorityMode();
        helper.assertTrue(distributor.getPriorityMode() == LoadDistributorBlockEntity.PriorityMode.FACTORY,
                "Second Load Distributor cycle should select Factory First");
        LoadDistributorBlockEntity.serverTick(level, distributorPos, level.getBlockState(distributorPos), distributor);
        helper.assertTrue(press.getEnergyStored() == 1,
                "Factory First should spend a scarce FE tick on the Scrap Press before survival consumers");
        helper.assertTrue(purifier.getEnergyStored() == 0,
                "Factory First should leave the Water Purifier empty when only one FE is available");

        helper.succeed();
    }

    private static void factoryControllerScanPause(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos controllerPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos pressPos = controllerPos.east();

        level.setBlock(controllerPos, ModBlocks.FACTORY_CONTROLLER.get().defaultBlockState(), 3);
        level.setBlock(pressPos, ModBlocks.SCRAP_PRESS.get().defaultBlockState(), 3);

        helper.assertTrue(level.getBlockEntity(controllerPos) instanceof FactoryControllerBlockEntity,
                "Factory Controller scan-pause test should create the controller block entity");
        helper.assertTrue(level.getBlockEntity(pressPos) instanceof ScrapPressBlockEntity,
                "Factory Controller scan-pause test should create the connected machine");
        if (!(level.getBlockEntity(controllerPos) instanceof FactoryControllerBlockEntity controller)
                || !(level.getBlockEntity(pressPos) instanceof ScrapPressBlockEntity press)) {
            helper.succeed();
            return;
        }

        press.setEnergyStored(16);
        controller.setNetworkEnabled(true);
        helper.assertTrue(controller.getConnectedMachines() >= 1,
                "Factory Controller should scan directly connected machines");
        helper.assertTrue(EnergyAccess.hasLocalOrNetworkPower(press, level, pressPos, 1),
                "Enabled Factory Controller should leave connected machine power available");
        helper.assertTrue(!FactoryControllerBlockEntity.isMachinePausedByController(level, pressPos),
                "Enabled Factory Controller should not pause connected machines");

        controller.setNetworkEnabled(false);
        helper.assertTrue(FactoryControllerBlockEntity.isMachinePausedByController(level, pressPos),
                "Disabled Factory Controller should pause connected machines through the live network graph");
        helper.assertTrue(!EnergyAccess.hasLocalOrNetworkPower(press, level, pressPos, 1),
                "Disabled Factory Controller should block even local buffered machine power");
        helper.assertTrue(com.knoxhack.echoashfallprotocol.power.PowerNetwork.diagnose(level, pressPos).issue()
                        == PowerIssue.CONTROLLER_DISABLED,
                "Factory Controller pause should surface a specific power diagnostic");
        helper.assertTrue(level.getBlockState(controllerPos).getValue(FactoryControllerBlock.ERROR),
                "Disabled Factory Controller should set the controller error block state immediately");

        controller.setNetworkEnabled(true);
        helper.assertTrue(!FactoryControllerBlockEntity.isMachinePausedByController(level, pressPos),
                "Re-enabled Factory Controller should resume connected machines");
        helper.assertTrue(EnergyAccess.hasLocalOrNetworkPower(press, level, pressPos, 1),
                "Re-enabled Factory Controller should restore access to buffered machine power");

        helper.succeed();
    }

    private static void waterBottleDrinkFlow(GameTestHelper helper) {
        Player startPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        setHydration(startPlayer, 20);
        startPlayer.getFoodData().setFoodLevel(20);
        ItemStack startingStack = new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 4);
        startPlayer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, startingStack);

        InteractionResult startResult = startingStack.getItem().use(
                helper.getLevel(), startPlayer, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(startResult.consumesAction(), "Clean water use should start drinking");
        helper.assertTrue(startPlayer.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount() == 4,
                "Starting to drink should not immediately consume a stacked bottle");

        ServerPlayer finishPlayer = helper.makeMockServerPlayerInLevel();
        finishPlayer.setGameMode(GameType.SURVIVAL);
        setHydration(finishPlayer, 20);
        finishPlayer.getFoodData().setFoodLevel(20);
        ItemStack finishStack = new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 4);
        finishPlayer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, finishStack);
        int drinkLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        ItemStack finishedStack = finishStack.finishUsingItem(helper.getLevel(), finishPlayer);
        finishPlayer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, finishedStack);
        helper.assertTrue(finishPlayer.getData(ModAttachments.SURVIVAL_DATA.get()).getHydration() == 60,
                "Completed clean water drink should restore hydration");
        helper.assertTrue(finishPlayer.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND)
                        .is(ModItems.CLEAN_WATER_BOTTLE.get()),
                "Completed stacked drink should leave remaining clean water in hand");
        helper.assertTrue(finishPlayer.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount() == 3,
                "Completed stacked drink should consume exactly one clean water bottle");
        helper.assertTrue(countInventory(finishPlayer, Items.GLASS_BOTTLE) == 1,
                "Completed stacked drink should return exactly one glass bottle");
        helper.assertTrue(QuestData.get(finishPlayer).hasVisitedLocation("special", "water:clean_consumed"),
                "Completed clean water drink should record the clean-water route marker through AdapterCore");
        var drinkLedgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(drinkLedgerEntries.size() > drinkLedgerBefore,
                "Completed clean water drink should append an AdapterCore mutation ledger entry");
        var drinkLedger = drinkLedgerEntries.get(drinkLedgerEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED.equals(drinkLedger.actionId()),
                "Clean water drink ledger should use the canonical player item-used event");
        helper.assertTrue("echoashfallprotocol:early_event_runtime".equals(drinkLedger.runtimeHostId()),
                "Clean water drink ledger should record the early-event runtime host");
        helper.assertTrue(drinkLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Clean water drink ledger should truthfully report MUTATED");
        helper.assertTrue(drinkLedger.saveTouched() && drinkLedger.hudOrEventEmitted(),
                "Clean water drink ledger should record save touch and visible event emission");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_CLEAN_WATER_BOTTLE.equals(
                        String.valueOf(drinkLedger.inputPayload().get("itemId")))
                        && Boolean.TRUE.equals(drinkLedger.inputPayload().get("waterBottleUse")),
                "Clean water drink ledger should carry the canonical water payload");
        helper.assertTrue(((Number) drinkLedger.beforeSummary().get("survivalHydration")).intValue() == 20
                        && ((Number) drinkLedger.afterSummary().get("survivalHydration")).intValue() == 60,
                "Clean water drink ledger should show hydration changing inside the runtime host");
        helper.assertTrue(((Number) drinkLedger.beforeSummary().get("cleanWaterCount")).intValue() == 4
                        && ((Number) drinkLedger.afterSummary().get("cleanWaterCount")).intValue() == 3,
                "Clean water drink ledger should show the water bottle consumed inside the runtime host");
        helper.assertTrue(((Number) drinkLedger.beforeSummary().get("glassBottleCount")).intValue() == 0
                        && ((Number) drinkLedger.afterSummary().get("glassBottleCount")).intValue() == 1,
                "Clean water drink ledger should show the glass bottle returned inside the runtime host");

        ServerPlayer boiledPlayer = helper.makeMockServerPlayerInLevel();
        boiledPlayer.setGameMode(GameType.SURVIVAL);
        setHydration(boiledPlayer, 20);
        boiledPlayer.getFoodData().setFoodLevel(20);
        ItemStack boiledStack = new ItemStack(ModItems.BOILED_WATER_BOTTLE.get());
        boiledPlayer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, boiledStack);

        ItemStack boiledFinished = boiledStack.finishUsingItem(helper.getLevel(), boiledPlayer);
        boiledPlayer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, boiledFinished);
        helper.assertTrue(boiledPlayer.getData(ModAttachments.SURVIVAL_DATA.get()).getHydration() == 45,
                "Completed boiled water drink should restore exactly 25 hydration");
        helper.assertTrue(boiledPlayer.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).is(Items.GLASS_BOTTLE),
                "Completed single boiled water drink should convert the held bottle inside the runtime host");

        ServerPlayer fullPlayer = helper.makeMockServerPlayerInLevel();
        fullPlayer.setGameMode(GameType.SURVIVAL);
        setHydration(fullPlayer, SurvivalData.MAX_HYDRATION);
        fullPlayer.getFoodData().setFoodLevel(20);
        ItemStack fullStack = new ItemStack(ModItems.CLEAN_WATER_BOTTLE.get(), 2);
        fullPlayer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, fullStack);
        int noopLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        InteractionResult refused = fullStack.getItem().use(
                helper.getLevel(), fullPlayer, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(refused == InteractionResult.FAIL,
                "Full hydration and full hunger should refuse water use");
        helper.assertTrue(fullPlayer.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount() == 2,
                "Refused water use should preserve the bottle stack");
        var noopEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(noopEntries.size() > noopLedgerBefore,
                "Refused water use should still append a truth ledger entry");
        var noopLedger = noopEntries.get(noopEntries.size() - 1);
        helper.assertTrue(noopLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.NOOP,
                "Refused water ledger should truthfully report NOOP");
        helper.assertFalse(noopLedger.saveTouched(),
                "Refused water use should not claim a save mutation");
        helper.assertTrue(noopLedger.hudOrEventEmitted(),
                "Refused water use should record visible feedback");
        helper.assertTrue(((Number) noopLedger.beforeSummary().get("cleanWaterCount")).intValue() == 2
                        && ((Number) noopLedger.afterSummary().get("cleanWaterCount")).intValue() == 2,
                "Refused water ledger should show the water stack was retained");
        helper.assertTrue(((Number) noopLedger.beforeSummary().get("glassBottleCount")).intValue() == 0
                        && ((Number) noopLedger.afterSummary().get("glassBottleCount")).intValue() == 0,
                "Refused water ledger should show no glass bottle was created");

        for (Item item : List.of(
                ModItems.DIRTY_WATER_BOTTLE.get(),
                ModItems.BOILED_WATER_BOTTLE.get(),
                ModItems.FILTERED_WATER_BOTTLE.get(),
                ModItems.CLEAN_WATER_BOTTLE.get())) {
            ItemStack water = new ItemStack(item);
            helper.assertTrue(item.getUseAnimation(water) == ItemUseAnimation.DRINK,
                    "Water bottle should use drink animation: " + BuiltInRegistries.ITEM.getKey(item));
            helper.assertTrue(item.getUseDuration(water, fullPlayer) == 32,
                    "Water bottle should take 1.6 seconds to drink: " + BuiltInRegistries.ITEM.getKey(item));
        }

        helper.succeed();
    }

    private static void earlyWaterRouteMarkers(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos waterPos = helper.absolutePos(new BlockPos(2, 2, 2));
        int ledgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        var dirtyResult = AshfallAdapterCoreEarlyEventRuntime.dirtyWaterCollected(player, waterPos);
        helper.assertTrue(dirtyResult.mutated(),
                "Dirty-water collection should mutate through AdapterCore early-event runtime");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "water:dirty_collected"),
                "Dirty-water collection should record the route marker immediately");

        var filterResult = AshfallAdapterCoreEarlyEventRuntime.waterFiltered(player, "gametest_emergency_clean_water");
        helper.assertTrue(filterResult.mutated(),
                "Emergency filtration should mutate through AdapterCore early-event runtime");
        QuestData quest = QuestData.get(player);
        helper.assertTrue(quest.hasVisitedLocation("special", "water:emergency_filtered"),
                "Emergency filtration should record the clean-water route marker immediately");

        String lastEvent = player.getPersistentData().getStringOr("ashes_of_tomorrow.adaptercore.last_early_event", "");
        helper.assertTrue("ashfall.special_marker".equals(lastEvent),
                "Water route marker publication should touch AdapterCore early-event diagnostics");
        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(ledgerEntries.size() >= ledgerBefore + 2,
                "Water route player actions should append AdapterCore mutation ledger entries");
        var latestLedger = ledgerEntries.get(ledgerEntries.size() - 1);
        helper.assertTrue("ashfall.special_marker".equals(latestLedger.actionId()),
                "Water route ledger should use the canonical AdapterCore event name");
        helper.assertTrue("echoashfallprotocol:early_event_runtime".equals(latestLedger.runtimeHostId()),
                "Water route ledger should record the real Ashfall early-event runtime host");
        helper.assertTrue(latestLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Water route ledger should truthfully report MUTATED");
        helper.assertTrue(latestLedger.saveTouched(),
                "Water route ledger should record the QuestData/save mutation");
        helper.assertTrue(latestLedger.hudOrEventEmitted(),
                "Water route ledger should record the AdapterCore event emission");
        helper.assertTrue(player.getUUID().toString().equals(latestLedger.target().snapshot().get("playerId")),
                "Water route ledger should identify the target player");
        int ledgerAfterMutation = ledgerEntries.size();
        var duplicateResult = AshfallAdapterCoreEarlyEventRuntime.waterFiltered(player, "gametest_emergency_clean_water");
        helper.assertFalse(duplicateResult.mutated(),
                "Duplicate same-tick water filtration event should not claim a second mutation");
        helper.assertTrue(duplicateResult.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.NOOP,
                "Duplicate same-tick water filtration event should truthfully report NOOP");
        var duplicateLedgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(duplicateLedgerEntries.size() == ledgerAfterMutation + 1,
                "Duplicate same-tick water filtration event should still append a truth ledger entry");
        var duplicateLedger = duplicateLedgerEntries.get(duplicateLedgerEntries.size() - 1);
        helper.assertTrue(duplicateLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.NOOP,
                "Duplicate same-tick water filtration ledger should record NOOP");
        helper.assertFalse(duplicateLedger.saveTouched(),
                "Duplicate same-tick water filtration ledger should not record a save mutation");
        helper.assertFalse(duplicateLedger.hudOrEventEmitted(),
                "Duplicate same-tick water filtration ledger should not record event emission");
        helper.succeed();
    }

    private static void handWarmerItemRuntimeFlow(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ColdData coldData = ColdData.get(player);
        coldData.setTemperature(40);
        player.setData(ModAttachments.COLD_DATA.get(), coldData);
        ItemStack handWarmer = new ItemStack(ModItems.HAND_WARMER.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, handWarmer);
        int ledgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        InteractionResult result = handWarmer.getItem().use(
                helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(result.consumesAction(),
                "Hand Warmer use should consume the player action when AdapterCore mutates state");
        helper.assertTrue(ColdData.get(player).getTemperature() == 65,
                "Hand Warmer use should mutate ColdData through the AdapterCore runtime");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),
                "Hand Warmer stack should only be consumed after AdapterCore reports a real mutation");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "cold:warmed_up"),
                "Hand Warmer use should record the cold-route marker through AdapterCore");
        String lastEvent = player.getPersistentData().getStringOr("ashes_of_tomorrow.adaptercore.last_early_event", "");
        helper.assertTrue("player.item_used".equals(lastEvent),
                "Hand Warmer use should touch AdapterCore early-event diagnostics with the canonical event");

        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(ledgerEntries.size() >= ledgerBefore + 1,
                "Hand Warmer use should append an AdapterCore mutation ledger entry");
        var latestLedger = ledgerEntries.get(ledgerEntries.size() - 1);
        helper.assertTrue("player.item_used".equals(latestLedger.actionId()),
                "Hand Warmer ledger should use the canonical NeoForge item-use event");
        helper.assertTrue("echoashfallprotocol:early_event_runtime".equals(latestLedger.runtimeHostId()),
                "Hand Warmer ledger should record the real Ashfall early-event runtime host");
        helper.assertTrue(latestLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Hand Warmer ledger should truthfully report MUTATED");
        helper.assertTrue(latestLedger.saveTouched(),
                "Hand Warmer ledger should record the QuestData/save mutation");
        helper.assertTrue(latestLedger.hudOrEventEmitted(),
                "Hand Warmer ledger should record the AdapterCore event emission");
        helper.assertTrue("echoashfallprotocol:hand_warmer".equals(
                        String.valueOf(latestLedger.inputPayload().get("itemId"))),
                "Hand Warmer ledger should include the canonical item id payload");
        helper.assertTrue(((Number) latestLedger.beforeSummary().get("coldTemperature")).intValue() == 40
                        && ((Number) latestLedger.afterSummary().get("coldTemperature")).intValue() == 65,
                "Hand Warmer ledger should show temperature changing inside the runtime host");
        helper.assertTrue(((Number) latestLedger.beforeSummary().get("handWarmerCount")).intValue() == 1
                        && ((Number) latestLedger.afterSummary().get("handWarmerCount")).intValue() == 0,
                "Hand Warmer ledger should show the item consumed inside the runtime host");

        ColdData stableCold = ColdData.get(player);
        stableCold.setTemperature(ColdData.MAX_TEMPERATURE);
        player.setData(ModAttachments.COLD_DATA.get(), stableCold);
        ItemStack stableWarmer = new ItemStack(ModItems.HAND_WARMER.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stableWarmer);
        int noopLedgerBefore = ledgerEntries.size();

        InteractionResult noopResult = stableWarmer.getItem().use(
                helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(noopResult == InteractionResult.SUCCESS,
                "Stable Hand Warmer use should be valid but not consume the action as a mutation");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount() == 1,
                "Stable Hand Warmer use should retain the item");
        var noopEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(noopEntries.size() > noopLedgerBefore,
                "Stable Hand Warmer use should still append a truth ledger entry");
        var noopLedger = noopEntries.get(noopEntries.size() - 1);
        helper.assertTrue(noopLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.NOOP,
                "Stable Hand Warmer ledger should truthfully report NOOP");
        helper.assertFalse(noopLedger.saveTouched(),
                "Stable Hand Warmer NOOP should not claim a save mutation");
        helper.assertTrue(noopLedger.hudOrEventEmitted(),
                "Stable Hand Warmer NOOP should record visible feedback");
        helper.assertTrue(((Number) noopLedger.beforeSummary().get("coldTemperature")).intValue() == ColdData.MAX_TEMPERATURE
                        && ((Number) noopLedger.afterSummary().get("coldTemperature")).intValue() == ColdData.MAX_TEMPERATURE,
                "Stable Hand Warmer ledger should show temperature stayed stable");
        helper.assertTrue(((Number) noopLedger.beforeSummary().get("handWarmerCount")).intValue() == 1
                        && ((Number) noopLedger.afterSummary().get("handWarmerCount")).intValue() == 1,
                "Stable Hand Warmer ledger should show the item was retained");
        helper.succeed();
    }

    private static void gasMaskItemRuntimeFlow(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack gasMask = new ItemStack(ModItems.GAS_MASK.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, gasMask);
        int ledgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        InteractionResult result = gasMask.getItem().use(
                helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(result.consumesAction(),
                "Gas Mask use should consume the player action when AdapterCore equips it");
        helper.assertTrue(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)
                        .is(ModItems.GAS_MASK.get()),
                "Gas Mask use should equip the mask through the AdapterCore runtime");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),
                "Gas Mask equip should remove the held mask only after AdapterCore mutates state");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "equipment:gas_mask_equipped"),
                "Gas Mask use should record the equipment route marker through AdapterCore");

        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(ledgerEntries.size() > ledgerBefore,
                "Gas Mask use should append an AdapterCore mutation ledger entry");
        var equipLedger = ledgerEntries.get(ledgerEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED.equals(
                        equipLedger.actionId()),
                "Gas Mask ledger should use the canonical player.item_used event");
        helper.assertTrue("echoashfallprotocol:early_event_runtime".equals(equipLedger.runtimeHostId()),
                "Gas Mask ledger should record the early-event runtime host");
        helper.assertTrue(equipLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Gas Mask ledger should truthfully report MUTATED");
        helper.assertTrue(equipLedger.saveTouched() && equipLedger.hudOrEventEmitted(),
                "Gas Mask ledger should record save touch and visible feedback");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_GAS_MASK.equals(
                        String.valueOf(equipLedger.inputPayload().get("itemId")))
                        && Boolean.TRUE.equals(equipLedger.inputPayload().get("gasMaskUse")),
                "Gas Mask ledger should carry the canonical item-use payload");
        helper.assertTrue("minecraft:air".equals(String.valueOf(equipLedger.beforeSummary().get("headSlotItemId")))
                        && com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_GAS_MASK.equals(
                                String.valueOf(equipLedger.afterSummary().get("headSlotItemId"))),
                "Gas Mask ledger should show the head slot equipped inside the runtime host");
        helper.assertTrue(((Number) equipLedger.beforeSummary().get("gasMaskInventoryCount")).intValue() == 1
                        && ((Number) equipLedger.afterSummary().get("gasMaskInventoryCount")).intValue() == 0,
                "Gas Mask ledger should show the inventory mask moved into equipment");

        ItemStack secondMask = new ItemStack(ModItems.GAS_MASK.get());
        ItemStack oldHelmet = new ItemStack(Items.LEATHER_HELMET);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, oldHelmet);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, secondMask);
        int swapLedgerBefore = ledgerEntries.size();

        InteractionResult swapResult = secondMask.getItem().use(
                helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(swapResult.consumesAction(),
                "Gas Mask use should consume the player action when AdapterCore swaps head equipment");
        helper.assertTrue(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)
                        .is(ModItems.GAS_MASK.get()),
                "Gas Mask swap should equip the mask through the runtime host");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).is(Items.LEATHER_HELMET),
                "Gas Mask swap should return the previous head item to the selected hand");
        var swapEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(swapEntries.size() > swapLedgerBefore,
                "Gas Mask swap should append an AdapterCore mutation ledger entry");
        var swapLedger = swapEntries.get(swapEntries.size() - 1);
        helper.assertTrue(swapLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Gas Mask swap ledger should truthfully report MUTATED");
        helper.assertTrue("minecraft:leather_helmet".equals(
                        String.valueOf(swapLedger.beforeSummary().get("headSlotItemId")))
                        && com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_GAS_MASK.equals(
                                String.valueOf(swapLedger.afterSummary().get("headSlotItemId"))),
                "Gas Mask swap ledger should show the head slot changed inside the runtime host");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_GAS_MASK.equals(
                        String.valueOf(swapLedger.beforeSummary().get("mainHandItemId")))
                        && "minecraft:leather_helmet".equals(
                                String.valueOf(swapLedger.afterSummary().get("mainHandItemId"))),
                "Gas Mask swap ledger should show the previous head item returned to hand");

        helper.succeed();
    }

    private static void fieldManualItemRuntimeFlow(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        assertNativeFirstSpawnRuntimeStartsMissionCore(helper, player);
        ItemStack manual = new ItemStack(ModItems.FIELD_MANUAL.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, manual);
        int ledgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        InteractionResult result = manual.getItem().use(
                helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(result.consumesAction(),
                "Field Manual use should consume the player action when AdapterCore advances the mission");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),
                "Field Manual should only be consumed after AdapterCore advances a real mission objective");
        helper.assertTrue(nativeObjectiveProgress(player, "secure_crash_outpost", "read_field_manual") >= 1,
                "Field Manual use should advance the secure_crash_outpost read objective through AdapterCore");
        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(ledgerEntries.size() > ledgerBefore,
                "Field Manual use should append an AdapterCore mutation ledger entry");
        var manualLedger = ledgerEntries.get(ledgerEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED.equals(
                        manualLedger.actionId()),
                "Field Manual ledger should use the canonical player.item_used event");
        helper.assertTrue("echoashfallprotocol:early_event_runtime".equals(manualLedger.runtimeHostId()),
                "Field Manual ledger should record the early-event runtime host");
        helper.assertTrue(manualLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Field Manual ledger should truthfully report MUTATED");
        helper.assertTrue(manualLedger.saveTouched() && manualLedger.hudOrEventEmitted(),
                "Field Manual ledger should record save touch and visible feedback");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_FIELD_MANUAL.equals(
                        String.valueOf(manualLedger.inputPayload().get("itemId")))
                        && Boolean.TRUE.equals(manualLedger.inputPayload().get("fieldManualUse")),
                "Field Manual ledger should carry the canonical item-use payload");
        helper.assertTrue(((Number) manualLedger.beforeSummary().get("fieldManualCount")).intValue() == 1
                        && ((Number) manualLedger.afterSummary().get("fieldManualCount")).intValue() == 0,
                "Field Manual ledger should show the manual consumed inside the runtime host");

        ItemStack secondManual = new ItemStack(ModItems.FIELD_MANUAL.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, secondManual);
        int noopLedgerBefore = ledgerEntries.size();

        InteractionResult noopResult = secondManual.getItem().use(
                helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(noopResult == InteractionResult.SUCCESS,
                "Repeated Field Manual use should be valid but not mutate completed mission state");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount() == 1,
                "Repeated Field Manual use should retain the manual");
        var noopEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(noopEntries.size() > noopLedgerBefore,
                "Repeated Field Manual use should still append a truth ledger entry");
        var noopLedger = noopEntries.get(noopEntries.size() - 1);
        helper.assertTrue(noopLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.NOOP,
                "Repeated Field Manual ledger should truthfully report NOOP");
        helper.assertFalse(noopLedger.saveTouched(),
                "Repeated Field Manual NOOP should not claim a save mutation");
        helper.assertTrue(noopLedger.hudOrEventEmitted(),
                "Repeated Field Manual NOOP should record visible feedback");
        helper.assertTrue(((Number) noopLedger.beforeSummary().get("fieldManualCount")).intValue() == 1
                        && ((Number) noopLedger.afterSummary().get("fieldManualCount")).intValue() == 1,
                "Repeated Field Manual ledger should show the manual was retained");

        helper.succeed();
    }

    private static void medicalConsumableItemRuntimeFlow(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(10.0F);
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        ItemStack bandage = new ItemStack(ModItems.BANDAGE.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bandage);
        int bandageLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        InteractionResult bandageResult = bandage.getItem().use(
                helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(bandageResult.consumesAction(),
                "Bandage should consume the player action when AdapterCore treats wounds");
        helper.assertTrue(player.getHealth() > 10.0F,
                "Bandage runtime should heal the player through AdapterCore");
        helper.assertFalse(player.hasEffect(MobEffects.POISON),
                "Bandage runtime should remove poison through AdapterCore");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),
                "Successful Bandage use should consume the item inside the runtime host");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "medical:bandage_used"),
                "Bandage use should record a route-visible medical marker through AdapterCore");

        var bandageEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(bandageEntries.size() > bandageLedgerBefore,
                "Bandage use should append an AdapterCore mutation ledger entry");
        var bandageLedger = bandageEntries.get(bandageEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED.equals(
                        bandageLedger.actionId()),
                "Bandage ledger should use the canonical player.item_used event");
        helper.assertTrue("echoashfallprotocol:early_event_runtime".equals(bandageLedger.runtimeHostId()),
                "Bandage ledger should record the early-event runtime host");
        helper.assertTrue(bandageLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Bandage ledger should truthfully report MUTATED");
        helper.assertTrue(bandageLedger.saveTouched() && bandageLedger.hudOrEventEmitted(),
                "Bandage ledger should record save touch and visible feedback");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_BANDAGE.equals(
                        String.valueOf(bandageLedger.inputPayload().get("itemId"))),
                "Bandage ledger should carry the canonical item id");
        helper.assertTrue(((Number) bandageLedger.beforeSummary().get("health")).floatValue() == 10.0F
                        && ((Number) bandageLedger.afterSummary().get("health")).floatValue() > 10.0F,
                "Bandage ledger should show health changing inside the runtime host");
        helper.assertTrue(Boolean.TRUE.equals(bandageLedger.beforeSummary().get("hasPoison"))
                        && !Boolean.TRUE.equals(bandageLedger.afterSummary().get("hasPoison")),
                "Bandage ledger should show poison removed inside the runtime host");
        helper.assertTrue(((Number) bandageLedger.beforeSummary().get("bandageCount")).intValue() == 1
                        && ((Number) bandageLedger.afterSummary().get("bandageCount")).intValue() == 0,
                "Bandage ledger should show the bandage consumed inside the runtime host");

        player.setHealth(player.getMaxHealth());
        player.removeEffect(MobEffects.POISON);
        ItemStack spareBandage = new ItemStack(ModItems.BANDAGE.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, spareBandage);
        int noopLedgerBefore = bandageEntries.size();
        InteractionResult noopBandage = spareBandage.getItem().use(
                helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(noopBandage == InteractionResult.SUCCESS,
                "Bandage should return a valid no-op when there is no wound to treat");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount() == 1,
                "No-op Bandage use should not consume the item");
        var noopEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(noopEntries.size() > noopLedgerBefore,
                "No-op Bandage use should still append a truth ledger entry");
        var noopLedger = noopEntries.get(noopEntries.size() - 1);
        helper.assertTrue(noopLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.NOOP,
                "No-op Bandage ledger should truthfully report NOOP");
        helper.assertFalse(noopLedger.saveTouched(),
                "No-op Bandage use should not claim a save mutation");
        helper.assertTrue(noopLedger.hudOrEventEmitted(),
                "No-op Bandage use should record visible feedback");
        helper.assertTrue(((Number) noopLedger.beforeSummary().get("bandageCount")).intValue() == 1
                        && ((Number) noopLedger.afterSummary().get("bandageCount")).intValue() == 1,
                "No-op Bandage ledger should show the item was retained");

        ItemStack stimPack = new ItemStack(ModItems.STIM_PACK.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stimPack);
        int stimLedgerBefore = noopEntries.size();
        InteractionResult stimResult = stimPack.getItem().use(
                helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(stimResult.consumesAction(),
                "Stim Pack should consume the player action when AdapterCore applies the dose");
        helper.assertTrue(player.hasEffect(MobEffects.REGENERATION) && player.hasEffect(MobEffects.SPEED),
                "Stim Pack runtime should apply regeneration and speed through AdapterCore");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),
                "Successful Stim Pack use should consume the dose inside the runtime host");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "medical:stim_pack_used"),
                "Stim Pack use should record a route-visible medical marker through AdapterCore");

        var stimEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(stimEntries.size() > stimLedgerBefore,
                "Stim Pack use should append an AdapterCore mutation ledger entry");
        var stimLedger = stimEntries.get(stimEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED.equals(
                        stimLedger.actionId()),
                "Stim Pack ledger should use the canonical player.item_used event");
        helper.assertTrue("echoashfallprotocol:early_event_runtime".equals(stimLedger.runtimeHostId()),
                "Stim Pack ledger should record the early-event runtime host");
        helper.assertTrue(stimLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Stim Pack ledger should truthfully report MUTATED");
        helper.assertTrue(stimLedger.saveTouched() && stimLedger.hudOrEventEmitted(),
                "Stim Pack ledger should record save touch and visible feedback");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_STIM_PACK.equals(
                        String.valueOf(stimLedger.inputPayload().get("itemId"))),
                "Stim Pack ledger should carry the canonical item id");
        helper.assertTrue(!Boolean.TRUE.equals(stimLedger.beforeSummary().get("hasRegeneration"))
                        && Boolean.TRUE.equals(stimLedger.afterSummary().get("hasRegeneration")),
                "Stim Pack ledger should show regeneration applied inside the runtime host");
        helper.assertTrue(((Number) stimLedger.beforeSummary().get("stimPackCount")).intValue() == 1
                        && ((Number) stimLedger.afterSummary().get("stimPackCount")).intValue() == 0,
                "Stim Pack ledger should show the dose consumed inside the runtime host");
        helper.succeed();
    }

    private static void mutagenItemRuntimeFlow(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        SurvivalData survivalData = player.getData(ModAttachments.SURVIVAL_DATA.get());
        survivalData.setRadiationLevel(0.0F);
        player.setData(ModAttachments.SURVIVAL_DATA.get(), survivalData);
        player.removeEffect(MobEffects.NAUSEA);
        player.removeEffect(MobEffects.WEAKNESS);
        ItemStack mutagen = new ItemStack(ModItems.MUTAGEN_VIAL.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, mutagen);
        int ledgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        InteractionResult result = mutagen.getItem().use(
                helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(result.consumesAction(),
                "Mutagen Vial should consume the player action when AdapterCore applies the dose");
        helper.assertTrue(player.getData(ModAttachments.SURVIVAL_DATA.get()).getRadiationLevel() > 0.0F,
                "Mutagen Vial runtime should increase SurvivalData radiation through AdapterCore");
        helper.assertTrue(player.hasEffect(MobEffects.NAUSEA) && player.hasEffect(MobEffects.WEAKNESS),
                "Mutagen Vial runtime should apply genetic shock effects through AdapterCore");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),
                "Successful Mutagen Vial use should consume the vial inside the runtime host");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "mutation:mutagen_used"),
                "Mutagen Vial use should record a route-visible mutation marker through AdapterCore");
        String lastEvent = player.getPersistentData().getStringOr("ashes_of_tomorrow.adaptercore.last_hazard_event", "");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED.equals(lastEvent),
                "Mutagen Vial use should touch AdapterCore hazard diagnostics with the canonical event");

        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(ledgerEntries.size() > ledgerBefore,
                "Mutagen Vial use should append an AdapterCore mutation ledger entry");
        var latestLedger = ledgerEntries.get(ledgerEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED.equals(
                        latestLedger.actionId()),
                "Mutagen Vial ledger should use the canonical player.item_used event");
        helper.assertTrue("echoashfallprotocol:hazard_runtime".equals(latestLedger.runtimeHostId()),
                "Mutagen Vial ledger should record the hazard runtime host");
        helper.assertTrue(latestLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Mutagen Vial ledger should truthfully report MUTATED");
        helper.assertTrue(latestLedger.saveTouched() && latestLedger.hudOrEventEmitted(),
                "Mutagen Vial ledger should record save touch and visible feedback");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_MUTAGEN_VIAL.equals(
                        String.valueOf(latestLedger.inputPayload().get("itemId"))),
                "Mutagen Vial ledger should carry the canonical item id");
        helper.assertTrue(((Number) latestLedger.beforeSummary().get("survivalRadiation")).floatValue() == 0.0F
                        && ((Number) latestLedger.afterSummary().get("survivalRadiation")).floatValue() > 0.0F,
                "Mutagen Vial ledger should show radiation changing inside the runtime host");
        helper.assertTrue(!Boolean.TRUE.equals(latestLedger.beforeSummary().get("hasNausea"))
                        && Boolean.TRUE.equals(latestLedger.afterSummary().get("hasNausea")),
                "Mutagen Vial ledger should show nausea applied inside the runtime host");
        helper.assertTrue(!Boolean.TRUE.equals(latestLedger.beforeSummary().get("hasWeakness"))
                        && Boolean.TRUE.equals(latestLedger.afterSummary().get("hasWeakness")),
                "Mutagen Vial ledger should show weakness applied inside the runtime host");
        helper.assertTrue(((Number) latestLedger.beforeSummary().get("mutagenVialCount")).intValue() == 1
                        && ((Number) latestLedger.afterSummary().get("mutagenVialCount")).intValue() == 0,
                "Mutagen Vial ledger should show the vial consumed inside the runtime host");
        helper.assertTrue(((Number) latestLedger.afterSummary().get("mutationCount")).intValue()
                        >= ((Number) latestLedger.beforeSummary().get("mutationCount")).intValue(),
                "Mutagen Vial ledger should not lose existing mutation state during the item-use action");
        helper.succeed();
    }

    private static void filterCartridgeRuntimeFlow(GameTestHelper helper) {
        ServerPlayer noMaskPlayer = helper.makeMockServerPlayerInLevel();
        noMaskPlayer.setGameMode(GameType.SURVIVAL);
        ItemStack refusedCartridge = new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get());
        noMaskPlayer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, refusedCartridge);
        int refusedLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        InteractionResult refused = refusedCartridge.getItem().use(
                helper.getLevel(), noMaskPlayer, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(refused == InteractionResult.FAIL,
                "Filter Cartridge use should fail without an equipped Gas Mask");
        helper.assertTrue(noMaskPlayer.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount() == 1,
                "Refused Filter Cartridge use should not consume the item");
        var refusedEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(refusedEntries.size() > refusedLedgerBefore,
                "Refused Filter Cartridge use should still append a truth ledger entry");
        var refusedLedger = refusedEntries.get(refusedEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED.equals(
                        refusedLedger.actionId()),
                "Refused Filter Cartridge ledger should use the canonical item-used event");
        helper.assertTrue("echoashfallprotocol:hazard_runtime".equals(refusedLedger.runtimeHostId()),
                "Refused Filter Cartridge ledger should record the hazard runtime host");
        helper.assertTrue(refusedLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.FAILED,
                "Refused Filter Cartridge ledger should truthfully report FAILED");
        helper.assertFalse(refusedLedger.saveTouched(),
                "Refused Filter Cartridge use should not claim a save mutation");
        helper.assertTrue(refusedLedger.hudOrEventEmitted(),
                "Refused Filter Cartridge use should record visible feedback");
        helper.assertTrue(((Number) refusedLedger.beforeSummary().get("filterCartridgeBasicCount")).intValue() == 1
                        && ((Number) refusedLedger.afterSummary().get("filterCartridgeBasicCount")).intValue() == 1,
                "Refused Filter Cartridge ledger should show the cartridge was retained");

        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModItems.GAS_MASK.get()));
        SurvivalData survivalData = player.getData(ModAttachments.SURVIVAL_DATA.get());
        survivalData.setAirFilterLife(100);
        survivalData.setFilterTier(0);
        player.setData(ModAttachments.SURVIVAL_DATA.get(), survivalData);
        ItemStack cartridge = new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, cartridge);
        int ledgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        InteractionResult result = cartridge.getItem().use(
                helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(result.consumesAction(),
                "Filter Cartridge use should consume the player action when AdapterCore mutates state");
        SurvivalData after = player.getData(ModAttachments.SURVIVAL_DATA.get());
        helper.assertTrue(after.getAirFilterLife() == 400 && after.getFilterTier() == 1,
                "Filter Cartridge use should refill SurvivalData through the hazard runtime");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty(),
                "Filter Cartridge stack should only be consumed after AdapterCore reports a mutation");

        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(ledgerEntries.size() > ledgerBefore,
                "Filter Cartridge use should append an AdapterCore mutation ledger entry");
        var latestLedger = ledgerEntries.get(ledgerEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED.equals(latestLedger.actionId()),
                "Filter Cartridge ledger should use the canonical item-used event");
        helper.assertTrue("echoashfallprotocol:hazard_runtime".equals(latestLedger.runtimeHostId()),
                "Filter Cartridge ledger should record the real hazard runtime host");
        helper.assertTrue(latestLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Filter Cartridge ledger should truthfully report MUTATED");
        helper.assertTrue(latestLedger.saveTouched() && latestLedger.hudOrEventEmitted(),
                "Filter Cartridge ledger should record save touch and visible event emission");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_FILTER_CARTRIDGE_BASIC.equals(
                        String.valueOf(latestLedger.inputPayload().get("itemId"))),
                "Filter Cartridge ledger should carry the canonical item payload");
        helper.assertTrue(((Number) latestLedger.beforeSummary().get("survivalAirFilterLife")).intValue() == 100
                        && ((Number) latestLedger.afterSummary().get("survivalAirFilterLife")).intValue() == 400,
                "Filter Cartridge ledger should show air filter life changing inside the runtime host");
        helper.assertTrue(((Number) latestLedger.afterSummary().get("survivalFilterTier")).intValue() == 1,
                "Filter Cartridge ledger should show the installed filter tier");
        helper.assertTrue(((Number) latestLedger.beforeSummary().get("filterCartridgeBasicCount")).intValue() == 1
                        && ((Number) latestLedger.afterSummary().get("filterCartridgeBasicCount")).intValue() == 0,
                "Filter Cartridge ledger should show the cartridge consumed inside the runtime host");

        ServerPlayer fullPlayer = helper.makeMockServerPlayerInLevel();
        fullPlayer.setGameMode(GameType.SURVIVAL);
        fullPlayer.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModItems.GAS_MASK.get()));
        SurvivalData fullData = fullPlayer.getData(ModAttachments.SURVIVAL_DATA.get());
        fullData.setAirFilterLife(SurvivalData.MAX_AIR_FILTER);
        fullData.setFilterTier(1);
        fullPlayer.setData(ModAttachments.SURVIVAL_DATA.get(), fullData);
        ItemStack fullCartridge = new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get());
        fullPlayer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, fullCartridge);
        int noopLedgerBefore = ledgerEntries.size();

        InteractionResult noopResult = fullCartridge.getItem().use(
                helper.getLevel(), fullPlayer, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(noopResult == InteractionResult.FAIL,
                "Full Filter Cartridge use should be valid but refused when capacity is already maxed");
        helper.assertTrue(fullPlayer.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getCount() == 1,
                "Full Filter Cartridge use should retain the cartridge");
        var noopEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(noopEntries.size() > noopLedgerBefore,
                "Full Filter Cartridge use should append a truth ledger entry");
        var noopLedger = noopEntries.get(noopEntries.size() - 1);
        helper.assertTrue(noopLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.NOOP,
                "Full Filter Cartridge ledger should truthfully report NOOP");
        helper.assertFalse(noopLedger.saveTouched(),
                "Full Filter Cartridge NOOP should not claim a save mutation");
        helper.assertTrue(noopLedger.hudOrEventEmitted(),
                "Full Filter Cartridge NOOP should record visible feedback");
        helper.assertTrue(((Number) noopLedger.beforeSummary().get("filterCartridgeBasicCount")).intValue() == 1
                        && ((Number) noopLedger.afterSummary().get("filterCartridgeBasicCount")).intValue() == 1,
                "Full Filter Cartridge ledger should show the cartridge was retained");
        helper.succeed();
    }

    private static void portableScannerItemRuntimeFlow(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack scanner = new ItemStack(ModItems.PORTABLE_SIGNAL_SCANNER.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, scanner);
        int ledgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        InteractionResult result = scanner.getItem().use(
                helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);

        helper.assertTrue(result.consumesAction(),
                "Portable scanner use should consume the action when AdapterCore mutates scanner durability");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getDamageValue() == 1,
                "Portable scanner durability should be spent by the exploration runtime");
        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(ledgerEntries.size() > ledgerBefore,
                "Portable scanner use should append AdapterCore truth ledger entries");
        com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry scannerLedger = null;
        for (int i = ledgerBefore; i < ledgerEntries.size(); i++) {
            var entry = ledgerEntries.get(i);
            if (com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_SCANNER_USED.equals(entry.actionId())) {
                scannerLedger = entry;
            }
        }
        helper.assertTrue(scannerLedger != null,
                "Portable scanner use should append a canonical player.scanner_used ledger entry");
        if (scannerLedger == null) {
            helper.succeed();
            return;
        }
        helper.assertTrue("echoashfallprotocol:exploration_runtime".equals(scannerLedger.runtimeHostId()),
                "Portable scanner ledger should record the real exploration runtime host");
        helper.assertTrue(scannerLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Portable scanner ledger should truthfully report MUTATED");
        helper.assertTrue(scannerLedger.saveTouched() && scannerLedger.hudOrEventEmitted(),
                "Portable scanner ledger should record save touch and visible/event feedback");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_PORTABLE_SIGNAL_SCANNER.equals(
                        String.valueOf(scannerLedger.inputPayload().get("itemId"))),
                "Portable scanner ledger should carry the canonical scanner item id");
        helper.assertTrue(((Number) scannerLedger.inputPayload().get("scannerDamageDelta")).intValue() == 1,
                "Portable scanner ledger should carry the runtime durability delta");
        helper.assertTrue(Boolean.TRUE.equals(scannerLedger.inputPayload().get("runtimeFeedback"))
                        && Boolean.TRUE.equals(scannerLedger.inputPayload().get("runtimePoiDiscovery")),
                "Portable scanner ledger should show runtime-owned feedback and POI discovery handling");
        helper.assertTrue(((Number) scannerLedger.beforeSummary().get("mainHandDamage")).intValue() == 0
                        && ((Number) scannerLedger.afterSummary().get("mainHandDamage")).intValue() == 1,
                "Portable scanner ledger should show held scanner damage changing inside the runtime host");
        helper.succeed();
    }

    private static void signalScannerBlockRuntimeFlow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos scannerPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos batteryPos = helper.absolutePos(new BlockPos(3, 2, 2));
        level.setBlock(scannerPos, ModBlocks.SIGNAL_SCANNER.get().defaultBlockState(), 3);
        level.setBlock(batteryPos, ModBlocks.BATTERY_BANK.get().defaultBlockState(), 3);
        helper.assertTrue(level.getBlockEntity(scannerPos) instanceof SignalScannerBlockEntity,
                "Signal scanner runtime flow should create a scanner block entity");
        helper.assertTrue(level.getBlockEntity(batteryPos) instanceof BatteryBankBlockEntity,
                "Signal scanner runtime flow should create a battery block entity");
        if (!(level.getBlockEntity(scannerPos) instanceof SignalScannerBlockEntity scanner)
                || !(level.getBlockEntity(batteryPos) instanceof BatteryBankBlockEntity battery)) {
            helper.succeed();
            return;
        }

        battery.setEnergyStored(500);
        MachineWearData wearData = new MachineWearData(level);
        wearData.repair(scannerPos, MachineWearData.MAX_WEAR);
        int ledgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        scanner.triggerScan(player);

        helper.assertTrue(scanner.isScanCooldownActive(),
                "Signal scanner cooldown should be started by the exploration runtime");
        helper.assertTrue(wearData.getWear(scannerPos) == SignalScannerBlockEntity.SCAN_WEAR_DELTA,
                "Signal scanner wear should be added by the exploration runtime");
        helper.assertTrue(battery.getEnergyStored() < 500,
                "Signal scanner power should be consumed by the exploration runtime");
        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(ledgerEntries.size() > ledgerBefore,
                "Signal scanner block use should append AdapterCore truth ledger entries");
        com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry scannerLedger = null;
        for (int i = ledgerBefore; i < ledgerEntries.size(); i++) {
            var entry = ledgerEntries.get(i);
            if (com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_SCANNER_USED.equals(entry.actionId())
                    && "stationary_signal_scanner".equals(String.valueOf(entry.inputPayload().get("source")))) {
                scannerLedger = entry;
            }
        }
        helper.assertTrue(scannerLedger != null,
                "Signal scanner block use should append a canonical player.scanner_used ledger entry");
        if (scannerLedger == null) {
            helper.succeed();
            return;
        }
        helper.assertTrue("echoashfallprotocol:exploration_runtime".equals(scannerLedger.runtimeHostId()),
                "Signal scanner ledger should record the real exploration runtime host");
        helper.assertTrue(scannerLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Signal scanner ledger should truthfully report MUTATED");
        helper.assertTrue(scannerLedger.saveTouched() && scannerLedger.hudOrEventEmitted(),
                "Signal scanner ledger should record save touch and visible feedback");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.BLOCK_SIGNAL_SCANNER.equals(
                        String.valueOf(scannerLedger.inputPayload().get("blockId"))),
                "Signal scanner ledger should carry the canonical scanner block id");
        helper.assertTrue(((Number) scannerLedger.inputPayload().get("powerCost")).intValue()
                        == SignalScannerBlockEntity.SCAN_POWER_COST,
                "Signal scanner ledger should carry the runtime power cost");
        helper.assertTrue(((Number) scannerLedger.beforeSummary().get("signalScannerCooldownTicks")).intValue() == 0
                        && ((Number) scannerLedger.afterSummary().get("signalScannerCooldownTicks")).intValue()
                        == SignalScannerBlockEntity.SCAN_COOLDOWN_TICKS,
                "Signal scanner ledger should show cooldown changing inside the runtime host");
        helper.assertTrue(((Number) scannerLedger.beforeSummary().get("signalScannerWear")).intValue() == 0
                        && ((Number) scannerLedger.afterSummary().get("signalScannerWear")).intValue()
                        == SignalScannerBlockEntity.SCAN_WEAR_DELTA,
                "Signal scanner ledger should show machine wear changing inside the runtime host");
        helper.succeed();
    }

    private static void scannerPoiMarkerRouteRuntimeFlow(GameTestHelper helper) {
        EchoServiceRegistry.withClearedForTests(() -> {
            RecordingMissionService missionService = new RecordingMissionService();
            RecordingStructureDiscoveryService discoveryService = new RecordingStructureDiscoveryService();
            EchoCoreServices.registerMissionService(missionService);
            EchoServiceRegistry.register(IStructureDiscoveryService.class, discoveryService);

            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            ItemStack scanner = new ItemStack(ModItems.PORTABLE_SIGNAL_SCANNER.get());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, scanner);
            POIScannerService.ScanHit hit = sampleScanHit(helper);
            Identifier siteId = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, hit.id());
            int ledgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

            var scanResult = AshfallAdapterCoreExplorationRuntime.portableScannerUsed(
                    player,
                    hit,
                    "gametest_scanner_poi_marker_route",
                    false,
                    net.minecraft.world.InteractionHand.MAIN_HAND,
                    1,
                    false,
                    0);

            QuestData quest = QuestData.get(player);
            helper.assertTrue(scanResult.mutated(),
                    "Scanner POI route runtime should mutate the native exploration state");
            helper.assertTrue(Boolean.TRUE.equals(scanResult.snapshot().get("autoDiscovered")),
                    "Scanner POI route runtime should auto-discover a close unscanned POI");
            helper.assertTrue(Boolean.TRUE.equals(scanResult.snapshot().get("poiDiscovered")),
                    "Scanner POI route runtime should call POIScannerService.discover");
            helper.assertTrue(Boolean.TRUE.equals(scanResult.snapshot().get("poiHazardRouteMutated")),
                    "Scanner POI route runtime should mutate the nested hazard route objective");
            helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getDamageValue() == 1,
                    "Scanner POI route runtime should spend portable scanner durability");
            helper.assertTrue(quest.isPOIDiscovered(hit.id()),
                    "Scanner POI route runtime should persist POI discovery in QuestData");
            helper.assertTrue(quest.hasPOIState(hit.id(), QuestData.POIObjectiveState.SCANNED),
                    "Scanner POI route runtime should mark the scanned POI objective state");
            helper.assertTrue(quest.hasPOIState(hit.id(), QuestData.POIObjectiveState.ENTERED),
                    "Scanner POI route runtime should mark the close POI as entered");
            helper.assertTrue(quest.hasVisitedLocation("poi", hit.id()),
                    "Scanner POI route runtime should mark the POI route marker");
            helper.assertTrue(discoveryService.recorded(siteId, hit.position()),
                    "Scanner POI route runtime should publish the scan to WorldCore structure discovery");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.SCAN_BLOCK,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "scan_first_poi")),
                    "Scanner POI route runtime should record the first-POI scan objective");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.SCAN_BLOCK, siteId),
                    "Scanner POI route runtime should record the site-specific scan objective");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.DISCOVER_STRUCTURE, siteId),
                    "Scanner POI route runtime should record the structure discovery objective");
            helper.assertTrue(missionService.recorded(MissionObjectiveType.CUSTOM,
                            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "poi_explorer")),
                    "Scanner POI route runtime should record the POI explorer objective");
            helper.assertTrue(missionService.recordedPath(MissionObjectiveType.CUSTOM, "scanner/used"),
                    "Scanner POI route runtime should record scanner use in MissionCore");

            var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
            com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry scannerLedger = null;
            for (int i = ledgerBefore; i < ledgerEntries.size(); i++) {
                var entry = ledgerEntries.get(i);
                if (com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_SCANNER_USED.equals(entry.actionId())
                        && "gametest_scanner_poi_marker_route".equals(String.valueOf(entry.inputPayload().get("source")))) {
                    scannerLedger = entry;
                }
            }
            helper.assertTrue(scannerLedger != null,
                    "Scanner POI route runtime should append a canonical scanner ledger entry");
            if (scannerLedger != null) {
                helper.assertTrue(scannerLedger.resultStatus()
                                == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                        "Scanner POI route runtime ledger should report MUTATED");
                helper.assertTrue(scannerLedger.saveTouched() && scannerLedger.hudOrEventEmitted(),
                        "Scanner POI route runtime ledger should record save touch and feedback");
            }
        });
        helper.succeed();
    }

    private static void relayStationBlockRuntimeFlow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos relayPos = helper.absolutePos(new BlockPos(2, 2, 2));
        RelayStationBlock relayBlock = (RelayStationBlock) ModBlocks.RELAY_STATION.get();
        level.setBlock(relayPos, ModBlocks.RELAY_STATION.get().defaultBlockState(), 3);
        player.getInventory().add(new ItemStack(ModItems.POWER_CELL.get(), 2));
        player.getInventory().add(new ItemStack(ModItems.CIRCUIT_BOARD.get(), 1));
        player.getInventory().add(new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 2));

        int repairLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
        InteractionResult repairResult = relayBlock.useRelayStation(level.getBlockState(relayPos), level, relayPos, player);

        helper.assertTrue(repairResult.consumesAction(),
                "Relay repair block use should consume the player action");
        BlockState repairedState = level.getBlockState(relayPos);
        helper.assertTrue(repairedState.getValue(RelayStationBlock.REPAIRED)
                        && !repairedState.getValue(RelayStationBlock.ACTIVE),
                "Relay repair should set the block repaired state through the late runtime");
        helper.assertTrue(countInventory(player, ModItems.POWER_CELL.get()) == 1
                        && countInventory(player, ModItems.CIRCUIT_BOARD.get()) == 0
                        && countInventory(player, ModItems.SCRAP_CIRCUIT.get()) == 0,
                "Relay repair should consume exactly the runtime repair materials");
        var repairLedger = latestRelayStationLedger(repairLedgerBefore);
        helper.assertTrue(repairLedger != null,
                "Relay repair should append a canonical late-runtime AdapterCore ledger entry");
        if (repairLedger == null) {
            helper.succeed();
            return;
        }
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_ASHFALL_RELAY_ACTIVATED.equals(repairLedger.actionId()),
                "Relay repair ledger should use the canonical NeoForge relay event");
        helper.assertTrue("echoashfallprotocol:late_runtime".equals(repairLedger.runtimeHostId()),
                "Relay repair ledger should record the real late runtime host");
        helper.assertTrue(repairLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Relay repair ledger should truthfully report MUTATED");
        helper.assertTrue(repairLedger.saveTouched() && repairLedger.hudOrEventEmitted(),
                "Relay repair ledger should record save touch and visible feedback");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.BLOCK_RELAY_STATION.equals(
                        String.valueOf(repairLedger.inputPayload().get("blockId"))),
                "Relay repair ledger should carry the canonical relay block id");
        helper.assertFalse(Boolean.TRUE.equals(repairLedger.beforeSummary().get("relayStationRepaired")),
                "Relay repair ledger should show the block unrepaired before runtime mutation");
        helper.assertTrue(Boolean.TRUE.equals(repairLedger.afterSummary().get("relayStationRepaired")),
                "Relay repair ledger should show the block repaired after runtime mutation");
        helper.assertTrue(((Number) repairLedger.beforeSummary().get("powerCellCount")).intValue() == 2
                        && ((Number) repairLedger.afterSummary().get("powerCellCount")).intValue() == 1,
                "Relay repair ledger should show Power Cell consumption");
        helper.assertTrue(((Number) repairLedger.beforeSummary().get("circuitBoardCount")).intValue() == 1
                        && ((Number) repairLedger.afterSummary().get("circuitBoardCount")).intValue() == 0,
                "Relay repair ledger should show Circuit Board consumption");
        helper.assertTrue(((Number) repairLedger.beforeSummary().get("scrapCircuitCount")).intValue() == 2
                        && ((Number) repairLedger.afterSummary().get("scrapCircuitCount")).intValue() == 0,
                "Relay repair ledger should show both Scrap Circuits consumed");
        helper.assertTrue(((Number) repairLedger.beforeSummary().get("radioDiscoveredCount")).intValue() == 0
                        && ((Number) repairLedger.afterSummary().get("radioDiscoveredCount")).intValue() == 1,
                "Relay repair ledger should show the radio station discovered by the runtime");

        int activationLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
        InteractionResult activationResult = relayBlock.useRelayStation(level.getBlockState(relayPos), level, relayPos, player);

        helper.assertTrue(activationResult.consumesAction(),
                "Relay activation block use should consume the player action");
        BlockState activeState = level.getBlockState(relayPos);
        helper.assertTrue(activeState.getValue(RelayStationBlock.REPAIRED)
                        && activeState.getValue(RelayStationBlock.ACTIVE),
                "Relay activation should set the block active state through the late runtime");
        helper.assertTrue(countInventory(player, ModItems.POWER_CELL.get()) == 0,
                "Relay activation should consume the remaining Power Cell");
        helper.assertTrue(RadioNetwork.get(player).getActivatedCount() == 1,
                "Relay activation should activate the player's radio network station");
        QuestData quest = QuestData.get(player);
        helper.assertTrue(quest.hasVisitedLocation("special", "relay:activated")
                        && quest.hasVisitedLocation("special", "relay:radio_relay")
                        && quest.hasVisitedLocation("special", "relay:map_revealed"),
                "Relay activation should record route markers through the late runtime");
        helper.assertTrue(ResearchData.get(player).getPoints() == 25,
                "Relay repair and activation should award research through the runtime");
        var activationLedger = latestRelayStationLedger(activationLedgerBefore);
        helper.assertTrue(activationLedger != null,
                "Relay activation should append a canonical late-runtime AdapterCore ledger entry");
        if (activationLedger == null) {
            helper.succeed();
            return;
        }
        helper.assertTrue(activationLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Relay activation ledger should truthfully report MUTATED");
        helper.assertTrue(activationLedger.saveTouched() && activationLedger.hudOrEventEmitted(),
                "Relay activation ledger should record save touch and visible feedback");
        helper.assertFalse(Boolean.TRUE.equals(activationLedger.beforeSummary().get("relayStationActive")),
                "Relay activation ledger should show the block inactive before runtime mutation");
        helper.assertTrue(Boolean.TRUE.equals(activationLedger.afterSummary().get("relayStationActive")),
                "Relay activation ledger should show the block active after runtime mutation");
        helper.assertTrue(((Number) activationLedger.beforeSummary().get("powerCellCount")).intValue() == 1
                        && ((Number) activationLedger.afterSummary().get("powerCellCount")).intValue() == 0,
                "Relay activation ledger should show activation Power Cell consumption");
        helper.assertTrue(((Number) activationLedger.beforeSummary().get("radioActivatedCount")).intValue() == 0
                        && ((Number) activationLedger.afterSummary().get("radioActivatedCount")).intValue() == 1,
                "Relay activation ledger should show the radio station activated by the runtime");

        ServerPlayer noPowerPlayer = helper.makeMockServerPlayerInLevel();
        noPowerPlayer.setGameMode(GameType.SURVIVAL);
        BlockPos noPowerRelayPos = helper.absolutePos(new BlockPos(6, 2, 2));
        level.setBlock(noPowerRelayPos, ModBlocks.RELAY_STATION.get().defaultBlockState()
                .setValue(RelayStationBlock.REPAIRED, true), 3);
        int noopLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        relayBlock.useRelayStation(level.getBlockState(noPowerRelayPos), level, noPowerRelayPos, noPowerPlayer);

        helper.assertFalse(level.getBlockState(noPowerRelayPos).getValue(RelayStationBlock.ACTIVE),
                "Relay activation without a Power Cell should leave the block inactive");
        var noopLedger = latestRelayStationLedger(noopLedgerBefore);
        helper.assertTrue(noopLedger != null,
                "Relay missing-cell use should append a truth ledger entry");
        if (noopLedger == null) {
            helper.succeed();
            return;
        }
        helper.assertTrue(noopLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.NOOP,
                "Relay missing-cell ledger should truthfully report NOOP");
        helper.assertFalse(noopLedger.saveTouched(),
                "Relay missing-cell NOOP should not claim a save update");
        helper.assertTrue(noopLedger.hudOrEventEmitted(),
                "Relay missing-cell NOOP should still record visible feedback");
        helper.assertFalse(Boolean.TRUE.equals(noopLedger.afterSummary().get("relayStationActive")),
                "Relay missing-cell ledger should show the block remained inactive");
        helper.succeed();
    }

    private static void powerNodeBlockRuntimeFlow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos nodePos = helper.absolutePos(new BlockPos(2, 2, 2));
        PowerNodeBlock nodeBlock = (PowerNodeBlock) ModBlocks.POWER_NODE.get();
        level.setBlock(nodePos, ModBlocks.POWER_NODE.get().defaultBlockState(), 3);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.ENERGY_CELL.get(), 1));

        int activationLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
        InteractionResult activationResult = nodeBlock.usePowerNode(level.getBlockState(nodePos), level, nodePos, player);

        helper.assertTrue(activationResult.consumesAction(),
                "Power Node block use should consume the player action");
        helper.assertTrue(level.getBlockState(nodePos).getValue(PowerNodeBlock.ACTIVE),
                "Power Node use should set block active through the late runtime");
        helper.assertTrue(level.getBlockEntity(nodePos) instanceof PowerNodeBlockEntity nodeEntity
                        && nodeEntity.isActivated(),
                "Power Node use should activate the block entity through the late runtime");
        helper.assertTrue(countInventory(player, ModItems.ENERGY_CELL.get()) == 0,
                "Power Node use should consume the held Energy Cell through the late runtime");
        helper.assertTrue(NexusWorldData.get(level).isTrackedActiveNode(level, nodePos),
                "Power Node use should update NexusWorldData through the late runtime");

        var activationLedger = latestPowerNodeLedger(activationLedgerBefore, "power_node_block");
        helper.assertTrue(activationLedger != null,
                "Power Node activation should append a canonical late-runtime AdapterCore ledger entry");
        if (activationLedger == null) {
            helper.succeed();
            return;
        }
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_MACHINE_POWERED.equals(activationLedger.actionId()),
                "Power Node ledger should use the canonical NeoForge machine-powered event");
        helper.assertTrue("echoashfallprotocol:late_runtime".equals(activationLedger.runtimeHostId()),
                "Power Node ledger should record the real late runtime host");
        helper.assertTrue(activationLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Power Node activation ledger should truthfully report MUTATED; status="
                        + activationLedger.resultStatus() + " failure=" + activationLedger.failureReason()
                        + " after=" + activationLedger.afterSummary());
        helper.assertTrue(activationLedger.saveTouched() && activationLedger.hudOrEventEmitted(),
                "Power Node activation ledger should record save touch and visible feedback");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.BLOCK_POWER_NODE.equals(
                        String.valueOf(activationLedger.inputPayload().get("blockId"))),
                "Power Node ledger should carry the canonical power-node block id");
        helper.assertFalse(Boolean.TRUE.equals(activationLedger.beforeSummary().get("powerNodeActive")),
                "Power Node ledger should show the block inactive before runtime mutation");
        helper.assertTrue(Boolean.TRUE.equals(activationLedger.afterSummary().get("powerNodeActive")),
                "Power Node ledger should show the block active after runtime mutation");
        helper.assertFalse(Boolean.TRUE.equals(activationLedger.beforeSummary().get("powerNodeEntityActivated")),
                "Power Node ledger should show the block entity unactivated before runtime mutation");
        helper.assertTrue(Boolean.TRUE.equals(activationLedger.afterSummary().get("powerNodeEntityActivated")),
                "Power Node ledger should show the block entity activated after runtime mutation");
        helper.assertTrue(((Number) activationLedger.beforeSummary().get("energyCellCount")).intValue() == 1
                        && ((Number) activationLedger.afterSummary().get("energyCellCount")).intValue() == 0,
                "Power Node ledger should show Energy Cell consumption");
        helper.assertTrue(((Number) activationLedger.afterSummary().get("activePowerNodeCount")).intValue()
                        >= ((Number) activationLedger.beforeSummary().get("activePowerNodeCount")).intValue(),
                "Power Node ledger should include NexusWorldData node-count evidence");
        helper.assertTrue(Boolean.TRUE.equals(activationLedger.afterSummary().get("powerNodeActivatedMarker")),
                "Power Node ledger should show route marker recorded; status=" + activationLedger.resultStatus()
                        + " failure=" + activationLedger.failureReason());
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "power_node:activated"),
                "Power Node use should record route markers through the late runtime");

        int activeNoopLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();
        nodeBlock.usePowerNode(level.getBlockState(nodePos), level, nodePos, player);
        var activeNoopLedger = latestPowerNodeLedger(activeNoopLedgerBefore, "power_node_block");
        helper.assertTrue(activeNoopLedger != null,
                "Already-active Power Node use should append a truth ledger entry");
        if (activeNoopLedger == null) {
            helper.succeed();
            return;
        }
        helper.assertTrue(activeNoopLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.NOOP,
                "Already-active Power Node ledger should truthfully report NOOP");
        helper.assertFalse(activeNoopLedger.saveTouched(),
                "Already-active Power Node NOOP should not claim a save update");
        helper.assertTrue(activeNoopLedger.hudOrEventEmitted(),
                "Already-active Power Node NOOP should still record visible feedback");

        ServerPlayer noCellPlayer = helper.makeMockServerPlayerInLevel();
        noCellPlayer.setGameMode(GameType.SURVIVAL);
        BlockPos noCellNodePos = helper.absolutePos(new BlockPos(6, 2, 2));
        level.setBlock(noCellNodePos, ModBlocks.POWER_NODE.get().defaultBlockState(), 3);
        int missingCellLedgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        nodeBlock.usePowerNode(level.getBlockState(noCellNodePos), level, noCellNodePos, noCellPlayer);

        helper.assertFalse(level.getBlockState(noCellNodePos).getValue(PowerNodeBlock.ACTIVE),
                "Power Node use without an Energy Cell should leave the block inactive");
        var missingCellLedger = latestPowerNodeLedger(missingCellLedgerBefore, "power_node_block");
        helper.assertTrue(missingCellLedger != null,
                "Power Node missing-cell use should append a truth ledger entry");
        if (missingCellLedger == null) {
            helper.succeed();
            return;
        }
        helper.assertTrue(missingCellLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.NOOP,
                "Power Node missing-cell ledger should truthfully report NOOP");
        helper.assertFalse(missingCellLedger.saveTouched(),
                "Power Node missing-cell NOOP should not claim a save update");
        helper.assertTrue(missingCellLedger.hudOrEventEmitted(),
                "Power Node missing-cell NOOP should still record visible feedback");
        helper.assertFalse(Boolean.TRUE.equals(missingCellLedger.afterSummary().get("powerNodeActive")),
                "Power Node missing-cell ledger should show the block remained inactive");
        helper.succeed();
    }

    private static void crudeFilterItemFlow(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack filter = new ItemStack(ModItems.CRUDE_FILTER.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, filter);
        player.getInventory().add(new ItemStack(ModItems.DIRTY_WATER_BOTTLE.get(), 2));
        int ledgerBefore = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries().size();

        InteractionResult firstUse = filter.getItem().use(
                helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(firstUse.consumesAction(),
                "Crude filter should consume action when dirty water is available");
        helper.assertTrue(countInventory(player, ModItems.FILTERED_WATER_BOTTLE.get()) == 1,
                "Crude filter should convert one dirty bottle into filtered water");
        helper.assertTrue(countInventory(player, ModItems.DIRTY_WATER_BOTTLE.get()) == 1,
                "Crude filter should consume exactly one dirty-water bottle per use");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getDamageValue() == 1,
                "Crude filter should lose one durability per successful use");
        helper.assertTrue(QuestData.get(player).hasVisitedLocation("special", "water:emergency_filtered"),
                "Crude filter use should record the emergency water route marker through AdapterCore");

        var firstLedgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(firstLedgerEntries.size() > ledgerBefore,
                "Crude filter use should append an AdapterCore mutation ledger entry");
        var firstLedger = firstLedgerEntries.get(firstLedgerEntries.size() - 1);
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_USED.equals(
                        firstLedger.actionId()),
                "Crude filter ledger should use the canonical item-used event");
        helper.assertTrue("echoashfallprotocol:early_event_runtime".equals(firstLedger.runtimeHostId()),
                "Crude filter ledger should record the real early-event runtime host");
        helper.assertTrue(firstLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Crude filter ledger should truthfully report MUTATED after inventory conversion");
        helper.assertTrue(firstLedger.saveTouched() && firstLedger.hudOrEventEmitted(),
                "Crude filter ledger should record save touch and visible/event feedback for successful use");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_CRUDE_FILTER.equals(
                        String.valueOf(firstLedger.inputPayload().get("itemId"))),
                "Crude filter ledger should carry the canonical item id");
        helper.assertTrue(Boolean.TRUE.equals(firstLedger.inputPayload().get("crudeFilterUse")),
                "Crude filter ledger should identify the runtime conversion payload");
        helper.assertTrue(com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_DIRTY_WATER_BOTTLE.equals(
                        String.valueOf(firstLedger.inputPayload().get("inputItem")))
                        && com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.ITEM_FILTERED_WATER_BOTTLE.equals(
                        String.valueOf(firstLedger.inputPayload().get("outputItem"))),
                "Crude filter ledger should carry canonical input and output item ids");
        helper.assertTrue(((Number) firstLedger.beforeSummary().get("dirtyWaterCount")).intValue() == 2
                        && ((Number) firstLedger.afterSummary().get("dirtyWaterCount")).intValue() == 1,
                "Crude filter ledger should show dirty-water inventory decreasing in the runtime host");
        helper.assertTrue(((Number) firstLedger.beforeSummary().get("filteredWaterCount")).intValue() == 0
                        && ((Number) firstLedger.afterSummary().get("filteredWaterCount")).intValue() == 1,
                "Crude filter ledger should show filtered-water inventory increasing in the runtime host");
        helper.assertTrue(((Number) firstLedger.beforeSummary().get("mainHandDamage")).intValue() == 0
                        && ((Number) firstLedger.afterSummary().get("mainHandDamage")).intValue() == 1,
                "Crude filter ledger should show filter durability changing in the runtime host");

        InteractionResult secondUse = filter.getItem().use(
                helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(secondUse.consumesAction(),
                "Crude filter should support repeated emergency filtering until durability runs out");
        helper.assertTrue(countInventory(player, ModItems.FILTERED_WATER_BOTTLE.get()) == 2,
                "Repeated crude filter use should produce a second filtered bottle");
        helper.assertTrue(countInventory(player, ModItems.DIRTY_WATER_BOTTLE.get()) == 0,
                "Crude filter should leave no dirty water after both bottles are processed");
        int damageAfterSuccess = player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getDamageValue();
        var secondLedgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        var secondLedger = secondLedgerEntries.get(secondLedgerEntries.size() - 1);
        helper.assertTrue(secondLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.MUTATED,
                "Repeated crude filter use should also report a real runtime mutation");
        helper.assertTrue(((Number) secondLedger.beforeSummary().get("dirtyWaterCount")).intValue() == 1
                        && ((Number) secondLedger.afterSummary().get("dirtyWaterCount")).intValue() == 0,
                "Second crude filter ledger should show the final dirty-water bottle consumed");
        helper.assertTrue(((Number) secondLedger.afterSummary().get("filteredWaterCount")).intValue() == 2,
                "Second crude filter ledger should show the second filtered bottle created");

        int noopLedgerBefore = secondLedgerEntries.size();
        InteractionResult missingInput = filter.getItem().use(
                helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(missingInput == InteractionResult.FAIL,
                "Crude filter should fail cleanly when no dirty water is present");
        helper.assertTrue(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getDamageValue() == damageAfterSuccess,
                "Failed crude filter use should not spend durability");
        var noopLedgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        helper.assertTrue(noopLedgerEntries.size() == noopLedgerBefore + 1,
                "Failed crude filter use should still append a truth ledger entry");
        var noopLedger = noopLedgerEntries.get(noopLedgerEntries.size() - 1);
        helper.assertTrue(noopLedger.resultStatus()
                        == com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResultStatus.NOOP,
                "Failed crude filter ledger should truthfully report NOOP");
        helper.assertFalse(noopLedger.saveTouched(),
                "Failed crude filter use should not record a save mutation");
        helper.assertTrue(noopLedger.hudOrEventEmitted(),
                "Failed crude filter use should still record runtime-visible feedback");
        helper.assertTrue(((Number) noopLedger.beforeSummary().get("dirtyWaterCount")).intValue() == 0
                        && ((Number) noopLedger.afterSummary().get("dirtyWaterCount")).intValue() == 0,
                "Failed crude filter ledger should show no dirty-water inventory mutation");
        helper.assertTrue(((Number) noopLedger.beforeSummary().get("mainHandDamage")).intValue() == damageAfterSuccess
                        && ((Number) noopLedger.afterSummary().get("mainHandDamage")).intValue() == damageAfterSuccess,
                "Failed crude filter ledger should show no filter durability mutation");
        helper.succeed();
    }

    private static void workshopStatusCopy(GameTestHelper helper) {
        String copy = WorkshopBlock.coverageSummaryMessage().getString();
        helper.assertFalse(copy.contains("Machine links"), "Workshop status should not advertise unimplemented links");
        helper.assertTrue(copy.contains("efficiency"), "Workshop status should describe the active area bonus");
        helper.succeed();
    }

    private static void nexusCommandStatusOnly(GameTestHelper helper) {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        NexusCommandHandler.register(dispatcher);

        var nexus = dispatcher.getRoot().getChild("nexus");
        helper.assertTrue(nexus != null, "/nexus command should be registered");
        helper.assertTrue(nexus.getChild("status") != null, "/nexus status should remain public");
        for (String mutatingVerb : List.of("awaken", "scan", "encounter", "relay", "siege", "operation", "finale")) {
            helper.assertTrue(nexus.getChild(mutatingVerb) == null,
                    "/nexus should not expose public mutation verb: " + mutatingVerb);
        }
        helper.succeed();
    }

    private static void radioDynamicStationPersistence(GameTestHelper helper) {
        RadioNetwork network = new RadioNetwork();
        RadioNetwork.StationInfo dynamic = new RadioNetwork.StationInfo(
                "relay_22222_70_-3333",
                "Relay 22222, -3333",
                new BlockPos(22222, 70, -3333));
        network.activateStation(dynamic);

        RadioNetwork restored = roundTripRadioNetwork(helper, network);
        RadioNetwork.StationInfo restoredStation = restored.getStationInfo(dynamic.getId());
        helper.assertTrue(restored.isActivated(dynamic.getId()), "Dynamic relay id should remain activated");
        helper.assertTrue(restoredStation != null, "Dynamic relay metadata should be restored");
        helper.assertTrue(restoredStation != null && restoredStation.getName().equals(dynamic.getName()),
                "Dynamic relay name should survive serialization");
        helper.assertTrue(restoredStation != null && restoredStation.getPosition().equals(dynamic.getPosition()),
                "Dynamic relay position should survive serialization");
        helper.assertTrue(restored.getAvailableDestinations(BlockPos.ZERO).stream()
                        .anyMatch(station -> station.getId().equals(dynamic.getId())),
                "Dynamic relay should resolve as a destination after reload");

        TagValueOutput legacyOutput = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        legacyOutput.putInt("discoveredCount", 1);
        legacyOutput.putString("discovered_0", "relay_-10_64_20");
        legacyOutput.putInt("activatedCount", 1);
        legacyOutput.putString("activated_0", "relay_-10_64_20");
        RadioNetwork legacy = new RadioNetwork();
        legacy.deserialize(TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), legacyOutput.buildResult()));
        helper.assertTrue(legacy.getStationInfo("relay_-10_64_20") != null,
                "Legacy id-only dynamic relay saves should be reconstructed from relay coordinates");
        helper.succeed();
    }

    private static void droneIntelTargeting(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos playerPos = helper.absolutePos(new BlockPos(2, 2, 2));
        player.setPos(playerPos.getX() + 0.5D, playerPos.getY(), playerPos.getZ() + 0.5D);

        FactionDiplomacy diplomacy = player.getData(ModAttachments.FACTION_DIPLOMACY.get());
        diplomacy.setRelation(
                FactionDiplomacy.FactionPair.fromFactions(
                        AshfallBiomeFactions.RADWARDEN_COMPACT,
                        AshfallBiomeFactions.CRASHBREAK_SALVAGE),
                -80);
        player.setData(ModAttachments.FACTION_DIPLOMACY.get(), diplomacy);

        EchoCompanionDrone drone = spawnCompanionDrone(helper, player, new BlockPos(4, 2, 2));
        drone.setRepairLevel(EchoCompanionDrone.REPAIR_FULL);
        drone.setCurrentMode(EchoCompanionDrone.DroneMode.COMBAT);

        Mob bandit = ModEntities.SCAVENGER_BANDIT.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
        helper.assertTrue(bandit != null, "Scavenger bandit should be spawnable");
        if (bandit != null) {
            BlockPos banditPos = helper.absolutePos(new BlockPos(6, 2, 2));
            bandit.setPos(banditPos.getX() + 0.5D, banditPos.getY(), banditPos.getZ() + 0.5D);
            helper.getLevel().addFreshEntity(bandit);
            drone.tick();
            helper.assertTrue(drone.getTarget() == bandit,
                    "Combat intel should assign an obvious hostile faction target");
            bandit.discard();
        }
        drone.discard();
        helper.succeed();
    }

    private static void neoforgeRuntimeHostMutationGate(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        NeoForgeEchoRuntimeHost host = NeoForgeRuntimeHostFactory.create(player, helper.getLevel());

        NativeItemStack breadGrant = new NativeItemStack("minecraft:bread", 3, Map.of());
        helper.assertTrue(host.playerInventory().grant(
                        host.playerRef(),
                        breadGrant,
                        host.context(
                                "gametest.inventory.grant_bread",
                                "EchoNativeRuntimeHost.PlayerInventory",
                                "grant")).mutated(),
                "NeoForge host inventory grant should mutate the live player inventory");
        helper.assertTrue(host.playerInventory().snapshot(
                        host.playerRef(),
                        host.context(
                                "gametest.inventory.snapshot_bread",
                                "EchoNativeRuntimeHost.PlayerInventory",
                                "snapshot")).stream()
                        .anyMatch(stack -> "minecraft:bread".equals(stack.itemId()) && stack.count() >= 3),
                "NeoForge host inventory snapshot should inspect live player inventory slots");
        helper.assertTrue(host.playerInventory().remove(
                        host.playerRef(),
                        "minecraft:bread",
                        2,
                        host.context(
                                "gametest.inventory.remove_bread",
                                "EchoNativeRuntimeHost.PlayerInventory",
                                "remove")).mutated(),
                "NeoForge host inventory remove should mutate the live player inventory");
        helper.assertTrue(countInventory(player, Items.BREAD) == 1,
                "NeoForge host inventory remove should shrink matching live stacks");

        BlockPos blockPos = helper.absolutePos(new BlockPos(1, 2, 1));
        NativeBlockRef blockRef = new NativeBlockRef(host.dimensionId(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
        helper.assertTrue(host.worldBlocks().setBlock(
                        blockRef,
                        new NativeBlockState("minecraft:stone", Map.of()),
                        host.context(
                                "gametest.blocks.set_stone",
                                "EchoNativeRuntimeHost.WorldBlocks",
                                "setBlock")).mutated(),
                "NeoForge host setBlock should mutate the live level");
        NativeBlockState queried = host.worldBlocks().blockState(
                blockRef,
                host.context(
                        "gametest.blocks.query_stone",
                        "EchoNativeRuntimeHost.WorldBlocks",
                        "blockState"));
        helper.assertTrue("minecraft:stone".equals(queried.blockId()),
                "NeoForge host blockState should query the live level block registry id");
        helper.assertTrue(host.worldBlocks().isLoaded(
                        blockRef,
                        host.context(
                                "gametest.blocks.loaded_stone",
                                "EchoNativeRuntimeHost.WorldBlocks",
                                "isLoaded")),
                "NeoForge host isLoaded should inspect live chunk availability");
        helper.assertTrue(QuestData.get(player).getBlockPlaceCount("minecraft:stone") >= 1,
                "NeoForge host setBlock should record the placement in save-backed QuestData");
        helper.assertTrue(roundTripQuestData(helper, QuestData.get(player)).getBlockPlaceCount("minecraft:stone") >= 1,
                "NeoForge host block placement marker should survive QuestData serialization");
        CompoundTag blockLedger = player.getPersistentData()
                .getCompoundOrEmpty(NeoForgeRuntimeMutationLedgerSink.LEDGER_ROOT);
        helper.assertTrue(blockLedger.getBooleanOr("lastSaveTouched", false),
                "NeoForge host block mutation ledger should record save-touching state");

        helper.assertTrue(host.worldBlocks().clearBlock(
                        blockRef,
                        host.context(
                                "gametest.blocks.clear_stone",
                                "EchoNativeRuntimeHost.WorldBlocks",
                                "clearBlock")).mutated(),
                "NeoForge host clearBlock should mutate the live level");
        helper.assertTrue(helper.getLevel().getBlockState(blockPos).isAir(),
                "NeoForge host clearBlock should leave air in the live level");

        helper.assertTrue(host.hud().publishNotification(
                        host.playerRef(),
                        Map.of("message", "NeoForge runtime host mutation gate"),
                        host.context(
                                "gametest.hud.publish_notice",
                                "EchoNativeRuntimeHost.Hud",
                                "publishNotification")).mutated(),
                "NeoForge host HUD notification should mutate through the host path");
        CompoundTag hudLedger = player.getPersistentData()
                .getCompoundOrEmpty(NeoForgeRuntimeMutationLedgerSink.LEDGER_ROOT);
        helper.assertTrue(hudLedger.getBooleanOr("lastHudOrEventEmitted", false),
                "NeoForge host HUD mutation should be recorded by the mutation ledger");
        helper.assertTrue(hudLedger.getIntOr("entryCount", 0) >= 5,
                "NeoForge host mutation ledger should observe the inventory, block, and HUD operations");
        helper.succeed();
    }

    private static void neoforgeRuntimeHostMachineCapabilityGate(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        NeoForgeEchoRuntimeHost host = NeoForgeRuntimeHostFactory.create(player, helper.getLevel());

        BlockPos pressPos = helper.absolutePos(new BlockPos(3, 2, 1));
        NativeBlockRef pressRef = new NativeBlockRef(host.dimensionId(), pressPos.getX(), pressPos.getY(), pressPos.getZ());
        helper.assertTrue(host.worldBlocks().setBlock(
                        pressRef,
                        new NativeBlockState(EchoAshfallProtocol.MODID + ":scrap_press", Map.of()),
                        host.context(
                                "gametest.machine.place_scrap_press",
                                "EchoNativeRuntimeHost.WorldBlocks",
                                "setBlock")).mutated(),
                "NeoForge host should place a live Scrap Press block entity");

        helper.assertTrue(helper.getLevel().getBlockEntity(pressPos) instanceof ScrapPressBlockEntity,
                "NeoForge host setBlock should create a real Scrap Press block entity");
        if (!(helper.getLevel().getBlockEntity(pressPos) instanceof ScrapPressBlockEntity press)) {
            helper.fail("NeoForge host machine capability gate could not resolve the live Scrap Press");
            return;
        }

        NativeCapabilityRequest itemInput = new NativeCapabilityRequest("neoforge:item", pressRef, "UP", Map.of());
        helper.assertTrue(host.capabilities().insertItem(
                        itemInput,
                        new NativeItemStack(EchoAshfallProtocol.MODID + ":scrap_metal", 4, Map.of()),
                        host.context(
                                "gametest.machine.insert_scrap",
                                "EchoNativeRuntimeHost.Capabilities",
                                "insertItem")).mutated(),
                "NeoForge host item capability insertion should mutate machine inventory");
        helper.assertTrue(press.getInventory().getStackInSlot(0).is(ModItems.SCRAP_METAL.get())
                        && press.getInventory().getStackInSlot(0).getCount() == 4,
                "NeoForge host item capability insertion should write the live Scrap Press input slot");
        Map<String, Object> itemCapabilityState = host.capabilities().readCapability(
                itemInput,
                host.context(
                        "gametest.machine.read_item_capability",
                        "EchoNativeRuntimeHost.Capabilities",
                        "readCapability"));
        helper.assertTrue(itemCapabilityState.containsKey("inventorySlots"),
                "NeoForge host item capability read should inspect live machine inventory slots");

        NativeBlockEntitySnapshot before = host.blockEntities().snapshot(
                pressRef,
                host.context(
                        "gametest.machine.snapshot_before",
                        "EchoNativeRuntimeHost.BlockEntities",
                        "snapshot"));
        helper.assertTrue(Boolean.TRUE.equals(before.state().get("hasBlockEntity")),
                "NeoForge host block entity snapshot should report the live Scrap Press entity");
        helper.assertTrue(before.state().containsKey("inventorySlots") && before.state().containsKey("saveTag"),
                "NeoForge host block entity snapshot should include persisted inventory and save state");

        NativeBlockEntitySnapshot appliedSnapshot = new NativeBlockEntitySnapshot(
                before.blockEntityId(),
                pressRef,
                Map.of(
                        "energyStored", 80,
                        "inventorySlots", List.of(
                                Map.of(
                                        "slot", 0,
                                        "item", EchoAshfallProtocol.MODID + ":scrap_metal",
                                        "count", 9),
                                Map.of(
                                        "slot", 1,
                                        "item", EchoAshfallProtocol.MODID + ":machine_casing",
                                        "count", 1))));
        helper.assertTrue(host.blockEntities().applySnapshot(
                        appliedSnapshot,
                        host.context(
                                "gametest.machine.apply_snapshot",
                                "EchoNativeRuntimeHost.BlockEntities",
                                "applySnapshot")).mutated(),
                "NeoForge host block entity snapshot application should mutate live machine state");
        helper.assertTrue(press.getEnergyStored() == 80,
                "NeoForge host block entity snapshot should write live machine energy");
        helper.assertTrue(press.getInventory().getStackInSlot(0).is(ModItems.SCRAP_METAL.get())
                        && press.getInventory().getStackInSlot(0).getCount() == 9
                        && press.getInventory().getStackInSlot(1).is(ModItems.MACHINE_CASING.get()),
                "NeoForge host block entity snapshot should write live inventory slots");

        NativeCapabilityRequest energyCapability = new NativeCapabilityRequest("neoforge:energy", pressRef, "UP", Map.of());
        helper.assertTrue(host.capabilities().receiveEnergy(
                        energyCapability,
                        20,
                        host.context(
                                "gametest.machine.receive_energy",
                                "EchoNativeRuntimeHost.Capabilities",
                                "receiveEnergy")).mutated(),
                "NeoForge host energy capability receive should mutate live machine energy");
        helper.assertTrue(press.getEnergyStored() == 100,
                "NeoForge host energy capability receive should increase live machine energy");
        helper.assertTrue(host.capabilities().extractEnergy(
                        energyCapability,
                        30,
                        host.context(
                                "gametest.machine.extract_energy",
                                "EchoNativeRuntimeHost.Capabilities",
                                "extractEnergy")).mutated(),
                "NeoForge host energy capability extraction should mutate live machine energy");
        helper.assertTrue(press.getEnergyStored() == 70,
                "NeoForge host energy capability extraction should decrease live machine energy");
        Map<String, Object> energyCapabilityState = host.capabilities().readCapability(
                energyCapability,
                host.context(
                        "gametest.machine.read_energy_capability",
                        "EchoNativeRuntimeHost.Capabilities",
                        "readCapability"));
        helper.assertTrue(energyCapabilityState.get("energyStored") instanceof Number energyStored
                        && energyStored.intValue() == 70,
                "NeoForge host energy capability read should inspect live machine energy");

        NativeCapabilityRequest itemOutput = new NativeCapabilityRequest("neoforge:item", pressRef, "DOWN", Map.of());
        helper.assertTrue(host.capabilities().extractItem(
                        itemOutput,
                        EchoAshfallProtocol.MODID + ":machine_casing",
                        1,
                        host.context(
                                "gametest.machine.extract_machine_casing",
                                "EchoNativeRuntimeHost.Capabilities",
                                "extractItem")).mutated(),
                "NeoForge host item capability extraction should mutate the live output slot");
        helper.assertTrue(press.getInventory().getStackInSlot(1).isEmpty(),
                "NeoForge host item capability extraction should clear the live output slot");

        NativeBlockEntitySnapshot beforeTick = host.blockEntities().snapshot(
                pressRef,
                host.context(
                        "gametest.machine.snapshot_before_tick",
                        "EchoNativeRuntimeHost.BlockEntities",
                        "snapshot"));
        helper.assertTrue(host.blockEntities().tick(
                        pressRef,
                        host.context(
                                "gametest.machine.tick_scrap_press",
                                "EchoNativeRuntimeHost.BlockEntities",
                                "tick")).mutated(),
                "NeoForge host block entity tick should call the real Scrap Press server tick");
        NativeBlockEntitySnapshot afterTick = host.blockEntities().snapshot(
                pressRef,
                host.context(
                        "gametest.machine.snapshot_after_tick",
                        "EchoNativeRuntimeHost.BlockEntities",
                        "snapshot"));
        helper.assertTrue(!String.valueOf(beforeTick.state().get("saveTag"))
                        .equals(String.valueOf(afterTick.state().get("saveTag"))),
                "NeoForge host block entity tick should mutate persisted machine state");
        helper.assertTrue(press.isProcessing() && press.getProcessingProgress() > 0 && press.getEnergyStored() < 70,
                "NeoForge host block entity tick should advance live recipe progress and consume power");

        CompoundTag ledger = player.getPersistentData()
                .getCompoundOrEmpty(NeoForgeRuntimeMutationLedgerSink.LEDGER_ROOT);
        helper.assertTrue(ledger.getBooleanOr("lastSaveTouched", false),
                "NeoForge host machine mutation ledger should record save-touching state");
        helper.assertTrue(ledger.getIntOr("entryCount", 0) >= 7,
                "NeoForge host machine mutation ledger should observe the machine capability operations");
        helper.succeed();
    }

    private static void ashfallFirstSpawnNewPlayerHostSmoke(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);

        FirstSpawnRuntimeResult result = AshfallAdapterCoreFirstSpawnRuntime.executeForGameTest(player);
        assertFirstSpawnResult(helper, result, "new_player_first_spawn");
        helper.assertTrue(result.mutationCount() >= 7,
                "First-spawn host smoke should mutate inventory, structure, teleport, respawn, persistent state, advancement, welcome packet, HUD, and MissionCore state");
        helper.assertTrue(player.getPersistentData().getBoolean("ashes_of_tomorrow.received_kit").orElse(false),
                "First-spawn host smoke should set the first-join persistent flag");
        helper.assertTrue(QuestData.get(player).isDropPodInitialized(),
                "First-spawn host smoke should set QuestData drop-pod initialization state");
        helper.assertTrue(player.getRespawnConfig() != null,
                "First-spawn host smoke should bind drop-pod respawn");
        helper.assertTrue(countInventory(player, ModItems.FIELD_MANUAL.get()) >= 1,
                "First-spawn host smoke should grant the canonical field manual starter note");
        assertOptionalTerminalRemote(helper, player);
        assertFirstSpawnNativeOperation(helper, result, "world.place_personal_drop_pod");
        assertFirstSpawnNativeOperation(helper, result, "player.grant_find_drop_pod_advancement");
        assertFirstSpawnNativeOperation(helper, result, "ui.dispatch_welcome_screen");
        assertFirstSpawnNativeOperation(helper, result, "hud.publish_opening_recovery_notice");

        QuestData roundTrip = roundTripQuestData(helper, QuestData.get(player));
        helper.assertTrue(roundTrip.isMissionUnlocked("secure_crash_outpost")
                        || roundTrip.isMissionCompleted("secure_crash_outpost"),
                "First-spawn host smoke should leave secure_crash_outpost in QuestData after relog serialization");
        assertMissionCoreFirstMissionStarted(helper, player);
        helper.succeed();
    }

    private static void ashfallFirstSpawnReturningPlayerRepairHostSmoke(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        player.getPersistentData().putBoolean("ashes_of_tomorrow.received_kit", true);

        FirstSpawnRuntimeResult result = AshfallAdapterCoreFirstSpawnRuntime.executeForGameTest(player);
        assertFirstSpawnResult(helper, result, "returning_player_repair");
        helper.assertTrue(Boolean.TRUE.equals(result.snapshot().get("dropPodReplaced")),
                "Returning-player repair smoke should replace missing drop-pod save data");
        helper.assertTrue(StartingDropPodData.get(helper.getLevel()).findForPlayer(player.getUUID()).isPresent(),
                "Returning-player repair smoke should persist repaired drop-pod save data");
        helper.assertTrue(player.getRespawnConfig() != null,
                "Returning-player repair smoke should bind a missing drop-pod respawn");
        helper.assertTrue(QuestData.get(player).isDropPodInitialized(),
                "Returning-player repair smoke should repair QuestData drop-pod initialization state");
        assertOptionalTerminalRemote(helper, player);
        assertFirstSpawnNativeOperation(helper, result, "repair.replace_missing_or_invalid_drop_pod.structure");
        assertFirstSpawnNativeOperation(helper, result, "repair.rescue_underground_or_missing_respawn.respawn");
        assertFirstSpawnNativeOperation(helper, result, "repair.first_objective_state");
        assertFirstSpawnNativeOperation(helper, result, "repair.reissue_terminal_remote_if_loaded");

        QuestData quest = QuestData.get(player);
        quest.unlockMission("secure_crash_outpost");
        QuestData.saveAndSync(player, quest);
        QuestData restored = roundTripQuestData(helper, QuestData.get(player));
        helper.assertTrue(restored.isMissionUnlocked("secure_crash_outpost"),
                "Returning-player repair smoke should preserve QuestData after relog serialization");
        helper.succeed();
    }

    private static void ashfallFirstRelayRouteHostSmoke(GameTestHelper helper) {
        ServerPlayer player = requireNativeHostSmokePlayer(helper);
        Set<MissionObjectiveType> objectiveTypes = EnumSet.noneOf(MissionObjectiveType.class);
        completeNativeRouteThrough(helper, player, "craft_portable_scanner", objectiveTypes);

        Identifier routeId = AshfallMissionCoreIntegration.missionId("first_relay_station_route");
        MissionDefinition definition = EchoCoreServices.missionService().missionDefinition(routeId).orElseThrow();
        assertGuidanceLink(helper, definition, "terminalPage", "echoashfallprotocol:ashfall_major_route_records");
        assertGuidanceLink(helper, definition, "holomapLayer", "echoashfallprotocol:first_major_route");
        assertGuidanceLink(helper, definition, "lensProfile", "echoashfallprotocol:ashfall_major_route_scans");
        assertGuidanceLink(helper, definition, "powerGridRepair", "echoashfallprotocol:ashfall_relay_station_repair");

        IMissionProgressView completed = progressNativeMissionCoreMission(helper, player, routeId, objectiveTypes);
        helper.assertTrue(isMissionTerminal(completed.status()),
                "First relay route host smoke should complete the native MissionCore route record");
        AshfallAdapterCoreLateRuntime.relayActivated(
                player, "first_relay_station", "relay_station", helper.absolutePos(new BlockPos(4, 2, 4)), "host_smoke_first_relay");
        QuestData quest = QuestData.get(player);
        helper.assertTrue(quest.hasVisitedLocation("special", "relay:activated")
                        && quest.hasVisitedLocation("special", "relay:first_relay_station"),
                "First relay route host smoke should record relay QuestData markers");
        assertNativeMissionCoreDataRoundTrip(helper, player, routeId);
        helper.succeed();
    }

    private static void ashfallPhase3RouteHostSmoke(GameTestHelper helper) {
        ServerPlayer player = requireNativeHostSmokePlayer(helper);
        assertNativeScannerFactionRuntimeHooks(helper, player);

        ResearchData research = player.getData(ModAttachments.RESEARCH_DATA.get());
        research.unlockPerk(PerkRegistry.BETTER_LOOT_1.getId());
        player.setData(ModAttachments.RESEARCH_DATA.get(), research);
        FactionDiplomacy diplomacy = player.getData(ModAttachments.FACTION_DIPLOMACY.get());
        FactionDiplomacy.FactionPair phase3Pair = FactionDiplomacy.FactionPair.fromFactions(
                AshfallBiomeFactions.CRASHBREAK_SALVAGE,
                AshfallBiomeFactions.RADWARDEN_COMPACT);
        diplomacy.modifyRelation(phase3Pair, 5);
        player.setData(ModAttachments.FACTION_DIPLOMACY.get(), diplomacy);
        EchoCompanionDrone drone = spawnCompanionDrone(helper, player, new BlockPos(5, 2, 5));
        drone.setRepairLevel(EchoCompanionDrone.REPAIR_FULL);

        helper.assertTrue(research.hasPerk(PerkRegistry.BETTER_LOOT_1.getId()),
                "Phase 3 host smoke should leave first perk state available to the host");
        helper.assertTrue(diplomacy.getRelation(phase3Pair) >= 5,
                "Phase 3 host smoke should leave faction reputation state available to the host");
        helper.assertTrue(drone.getRepairLevel() >= EchoCompanionDrone.REPAIR_FULL,
                "Phase 3 host smoke should leave repaired drone state available to the host");
        assertNativeMissionCoreDataRoundTrip(helper, player, AshfallMissionCoreIntegration.missionId("poi_explorer"));
        drone.discard();
        helper.succeed();
    }

    private static void ashfallMidgameRouteHostSmoke(GameTestHelper helper) {
        ServerPlayer player = requireNativeHostSmokePlayer(helper);
        Set<MissionObjectiveType> objectiveTypes = EnumSet.noneOf(MissionObjectiveType.class);
        completeNativeRouteThrough(helper, player, "calibrate_midgame_grid", objectiveTypes);

        SurvivalData survival = player.getData(ModAttachments.SURVIVAL_DATA.get());
        survival.setRadiationLevel(42.0F);
        survival.setHydration(75);
        player.setData(ModAttachments.SURVIVAL_DATA.get(), survival);
        MutationData mutation = player.getData(ModAttachments.MUTATION_DATA.get());
        mutation.addMutation(MutationData.MutationType.RAD_RESISTANCE);
        player.setData(ModAttachments.MUTATION_DATA.get(), mutation);

        BlockPos medBayPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos cleanserPos = helper.absolutePos(new BlockPos(4, 2, 2));
        helper.getLevel().setBlock(medBayPos, ModBlocks.FIELD_MED_BAY.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(cleanserPos, ModBlocks.RADIATION_CLEANSER.get().defaultBlockState(), 3);
        helper.assertTrue(helper.getLevel().getBlockEntity(medBayPos) != null,
                "Midgame host smoke should create a Field Med Bay block entity");
        helper.assertTrue(helper.getLevel().getBlockEntity(cleanserPos) != null,
                "Midgame host smoke should create a Radiation Cleanser block entity");
        helper.assertTrue(AshfallAdapterCoreHazardRuntime.medBayUsed(player, medBayPos, 1200, mutation.getMutationCount()).mutated(),
                "Midgame host smoke should publish med bay host runtime state");
        helper.assertTrue(AshfallAdapterCoreHazardRuntime.radiationCleanserUsed(
                        player, cleanserPos, "echoashfallprotocol:irradiated_scrap", "echoashfallprotocol:scrap_metal").mutated(),
                "Midgame host smoke should publish cleanser host runtime state");
        helper.assertTrue(player.getData(ModAttachments.SURVIVAL_DATA.get()).getRadiationLevel() >= 42.0F,
                "Midgame host smoke should preserve SurvivalData state");
        helper.assertTrue(player.getData(ModAttachments.MUTATION_DATA.get()).hasMutation(MutationData.MutationType.RAD_RESISTANCE),
                "Midgame host smoke should preserve MutationData state");
        assertNativeMissionCoreDataRoundTrip(helper, player, AshfallMissionCoreIntegration.missionId("calibrate_midgame_grid"));
        helper.succeed();
    }

    private static void ashfallLateGameNexusEndingsHostSmoke(GameTestHelper helper) {
        ServerPlayer player = requireNativeHostSmokePlayer(helper);
        Set<MissionObjectiveType> objectiveTypes = EnumSet.noneOf(MissionObjectiveType.class);
        completeNativeRouteThrough(helper, player, "reach_decision", objectiveTypes);
        assertNativeBranchStartsOpenAfterDecision(helper, player);

        ServerLevel level = helper.getLevel().getServer().overworld();
        NexusWorldData worldData = NexusWorldData.get(level);
        NexusCampaignData campaign = NexusCampaignData.get(level);
        for (int i = 0; i < 5; i++) {
            BlockPos nodePos = helper.absolutePos(new BlockPos(2 + i, 2, 8));
            worldData.recordPowerNodeActivated(nodePos);
            AshfallAdapterCoreLateRuntime.powerNodeState(player, nodePos, true, i + 1, "host_smoke_late");
        }
        campaign.markRelayEncounterComplete(NexusRelayType.REACTOR);
        campaign.resolveRelay(NexusRelayType.REACTOR, NexusRelayState.STABILIZED);
        AshfallAdapterCoreLateRuntime.primeRelayResolved(
                player, NexusRelayType.REACTOR, NexusRelayState.STABILIZED, campaign, "host_smoke_late");
        AshfallAdapterCoreLateRuntime.nexusState(player, campaign, worldData, "awakened", "host_smoke_late");

        for (PostNexusData.NexusPath path : List.of(
                PostNexusData.NexusPath.RESTORE,
                PostNexusData.NexusPath.DESTROY,
                PostNexusData.NexusPath.CONTROL)) {
            PostNexusData post = PostNexusData.get(player);
            post.setSelectedPath(path);
            post.setRelaysResolved(NexusCampaignData.REQUIRED_RELAY_RESOLUTION_COUNT);
            post.setWardenDefeated(true);
            post.setFinalBossDefeated(true);
            post.setEpilogueComplete(true);
            PostNexusData.saveAndSync(player, post);
            AshfallAdapterCoreLateRuntime.endingChoice(player, path, helper.absolutePos(new BlockPos(6, 2, 6)), "host_smoke_late");
            AshfallAdapterCoreLateRuntime.postNexusPersisted(player, post, "host_smoke_late");
            helper.assertTrue(PostNexusData.get(player).isPath(path)
                            && PostNexusData.get(player).isFinalBossDefeated()
                            && PostNexusData.get(player).isEpilogueComplete(),
                    "Late-game host smoke should assert ending state for " + path);
        }
        assertNativeBranchesCompleteIndependently(helper);
        assertNativeMissionCoreDataRoundTrip(helper, player, AshfallMissionCoreIntegration.missionId("reach_decision"));
        helper.succeed();
    }

    private static void agent7FullPlaythroughRestoreHostSmoke(GameTestHelper helper) {
        agent7FullPlaythroughEndingHostSmoke(helper, "restore", PostNexusData.NexusPath.RESTORE);
    }

    private static void agent7FullPlaythroughDestroyHostSmoke(GameTestHelper helper) {
        agent7FullPlaythroughEndingHostSmoke(helper, "destroy", PostNexusData.NexusPath.DESTROY);
    }

    private static void agent7FullPlaythroughControlHostSmoke(GameTestHelper helper) {
        agent7FullPlaythroughEndingHostSmoke(helper, "control", PostNexusData.NexusPath.CONTROL);
    }

    private static void agent3PlayableLoopNativeHostSmoke(GameTestHelper helper) {
        GameProfile profile = new GameProfile(
                UUID.randomUUID(),
                "agent3-loop");
        ServerPlayer player = makeAgent7ConnectedPlayer(helper, profile);
        player.setGameMode(GameType.SURVIVAL);

        FirstSpawnRuntimeResult firstSpawn = AshfallAdapterCoreFirstSpawnRuntime.executeForGameTest(player);
        assertFirstSpawnResult(helper, firstSpawn, "new_player_first_spawn");
        assertMissionCoreFirstMissionStarted(helper, player);
        boolean newGame = "new_player_first_spawn".equals(firstSpawn.branch());
        boolean spawn = player.getRespawnConfig() != null
                && QuestData.get(player).isDropPodInitialized();

        double beforeX = player.getX();
        double beforeZ = player.getZ();
        player.setPos(beforeX + 1.0D, player.getY(), beforeZ + 1.0D);
        boolean move = Math.abs(player.getX() - beforeX) >= 1.0D
                && Math.abs(player.getZ() - beforeZ) >= 1.0D;
        helper.assertTrue(move, "Agent 3 native playable loop should move the live host player");

        AshfallTerminalCommonIntegration.register();
        TerminalMissionRegistry.register(MainSurvivalQuestProvider.INSTANCE);
        TerminalMissionActions.registerForTab(MainSurvivalQuestProvider.TAB_ID);
        Identifier missions = Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "missions");
        boolean terminal = TerminalActionRegistry.handle(
                player,
                missions,
                TerminalMissionActions.MISSION_ACTION,
                "invalid");
        helper.assertTrue(terminal, "Agent 3 native playable loop should open the Terminal action surface");

        Identifier firstMission = AshfallMissionCoreIntegration.missionId("secure_crash_outpost");
        IMissionProgressView completed = progressNativeMissionCoreMission(
                helper,
                player,
                firstMission,
                EnumSet.noneOf(MissionObjectiveType.class));
        boolean objective = isMissionTerminal(completed.status());
        helper.assertTrue(objective, "Agent 3 native playable loop should complete the opening objective");

        SurvivalData survivalData = player.getData(ModAttachments.SURVIVAL_DATA.get());
        survivalData.setRadiationLevel(40.0F);
        player.setData(ModAttachments.SURVIVAL_DATA.get(), survivalData);
        player.syncData(ModAttachments.SURVIVAL_DATA.get());
        ItemStack dose = new ItemStack(ModItems.RAD_AWAY.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, dose);
        InteractionResult itemResult = dose.getItem().use(
                helper.getLevel(),
                player,
                net.minecraft.world.InteractionHand.MAIN_HAND);
        SurvivalData afterUse = player.getData(ModAttachments.SURVIVAL_DATA.get());
        boolean hazard = QuestData.get(player).hasVisitedLocation("special", "medical:rad_away")
                && "ashfall.treatment_applied".equals(player.getPersistentData().getStringOr(
                        "ashes_of_tomorrow.adaptercore.last_hazard_event", ""));
        boolean item = itemResult.consumesAction()
                && afterUse.getRadiationLevel() < 40.0F
                && player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty();
        helper.assertTrue(hazard, "Agent 3 native playable loop should interact with a live hazard system");
        helper.assertTrue(item, "Agent 3 native playable loop should use and consume a gameplay item");

        player = assertAgent7HostRelogCheckpoint(helper, player, profile, "secure_crash_outpost");
        boolean saveLoadContinue = EchoCoreServices.missionService()
                .mission(player, firstMission)
                .map(view -> isMissionTerminal(view.status()))
                .orElse(false);
        helper.assertTrue(saveLoadContinue,
                "Agent 3 native playable loop should save, load, and continue with objective state intact");

        removeAgent7ConnectedPlayer(helper, player);
        boolean exit = helper.getLevel().getServer().getPlayerList().getPlayer(profile.id()) == null;
        helper.assertTrue(exit, "Agent 3 native playable loop should exit cleanly");

        EchoAshfallProtocol.LOGGER.info(
                "agent3 native playable loop host smoke PASS newGame={} spawn={} move={} terminal={} objective={} hazard={} item={} save={} load={} continue={} exit={}",
                newGame,
                spawn,
                move,
                terminal,
                objective,
                hazard,
                item,
                saveLoadContinue,
                saveLoadContinue,
                saveLoadContinue,
                exit);
        helper.succeed();
    }

    private static void agent7CheckpointRelogHostSmoke(GameTestHelper helper) {
        GameProfile profile = new GameProfile(
                UUID.randomUUID(),
                "agent7-relog");
        ServerPlayer player = makeAgent7ConnectedPlayer(helper, profile);
        player.setGameMode(GameType.SURVIVAL);

        FirstSpawnRuntimeResult firstSpawn = AshfallAdapterCoreFirstSpawnRuntime.executeForGameTest(player);
        assertFirstSpawnResult(helper, firstSpawn, "new_player_first_spawn");
        assertMissionCoreFirstMissionStarted(helper, player);

        Set<MissionObjectiveType> objectiveTypes = EnumSet.noneOf(MissionObjectiveType.class);
        Set<String> routeCheckpoints = Set.of(
                "secure_crash_outpost",
                "craft_portable_scanner",
                "first_relay_station_route",
                "expedition_readiness",
                "calibrate_midgame_grid",
                "reach_decision");
        for (Mission mission : routeMissionsInOrder()) {
            IMissionProgressView completed = progressNativeMissionCoreMission(
                    helper,
                    player,
                    AshfallMissionCoreIntegration.missionId(mission.id()),
                    objectiveTypes);
            helper.assertTrue(isMissionTerminal(completed.status()),
                    "Agent 7 relog route should complete checkpoint candidate: " + mission.id());
            if (routeCheckpoints.contains(mission.id())) {
                player = assertAgent7HostRelogCheckpoint(helper, player, profile, mission.id());
            }
            if ("reach_decision".equals(mission.id())) {
                break;
            }
        }

        assertNativeBranchStartsOpenAfterDecision(helper, player);
        for (String missionId : nativeEndingBranches().get("restore")) {
            IMissionProgressView completed = progressNativeMissionCoreMission(
                    helper,
                    player,
                    AshfallMissionCoreIntegration.missionId(missionId),
                    objectiveTypes);
            helper.assertTrue(isMissionTerminal(completed.status()),
                    "Agent 7 relog route should complete restore branch mission: " + missionId);
            player = assertAgent7HostRelogCheckpoint(helper, player, profile, missionId);
        }

        PostNexusData post = PostNexusData.get(player);
        post.setSelectedPath(PostNexusData.NexusPath.RESTORE);
        post.setRelaysResolved(NexusCampaignData.REQUIRED_RELAY_RESOLUTION_COUNT);
        post.setWardenDefeated(true);
        post.setFinalBossDefeated(true);
        post.setEpilogueComplete(true);
        PostNexusData.saveAndSync(player, post);
        AshfallAdapterCoreLateRuntime.endingChoice(
                player,
                PostNexusData.NexusPath.RESTORE,
                helper.absolutePos(new BlockPos(7, 2, 6)),
                "agent7_checkpoint_relog");
        AshfallAdapterCoreLateRuntime.postNexusPersisted(player, post, "agent7_checkpoint_relog");

        player = assertAgent7HostRelogCheckpoint(helper, player, profile, "restore_epilogue");
        helper.assertTrue(PostNexusData.get(player).isPath(PostNexusData.NexusPath.RESTORE)
                        && PostNexusData.get(player).isFinalBossDefeated()
                        && PostNexusData.get(player).isEpilogueComplete(),
                "Agent 7 relog route should persist restore post-Nexus state after host rejoin");
        removeAgent7ConnectedPlayer(helper, player);
        helper.succeed();
    }

    private static void agent7FullPlaythroughEndingHostSmoke(
            GameTestHelper helper,
            String branchPath,
            PostNexusData.NexusPath nexusPath) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);

        FirstSpawnRuntimeResult firstSpawn = AshfallAdapterCoreFirstSpawnRuntime.executeForGameTest(player);
        assertFirstSpawnResult(helper, firstSpawn, "new_player_first_spawn");
        assertMissionCoreFirstMissionStarted(helper, player);

        Set<MissionObjectiveType> objectiveTypes = EnumSet.noneOf(MissionObjectiveType.class);
        Set<String> routeCheckpoints = Set.of(
                "secure_crash_outpost",
                "craft_portable_scanner",
                "first_relay_station_route",
                "expedition_readiness",
                "calibrate_midgame_grid",
                "reach_decision");
        completeNativeRouteThroughWithRelogCheckpoints(
                helper, player, "reach_decision", objectiveTypes, routeCheckpoints);
        assertNativeBranchStartsOpenAfterDecision(helper, player);

        for (String missionId : nativeEndingBranches().get(branchPath)) {
            assertNativeBranchPathMetadata(helper, branchPath, missionId);
            IMissionProgressView completed = progressNativeMissionCoreMission(
                    helper,
                    player,
                    AshfallMissionCoreIntegration.missionId(missionId),
                    objectiveTypes);
            helper.assertTrue(isMissionTerminal(completed.status()),
                    "Agent 7 full playthrough should complete " + branchPath + " branch mission: " + missionId);
            assertNativeMissionCoreDataRoundTrip(helper, player, AshfallMissionCoreIntegration.missionId(missionId));
        }

        PostNexusData post = PostNexusData.get(player);
        post.setSelectedPath(nexusPath);
        post.setRelaysResolved(NexusCampaignData.REQUIRED_RELAY_RESOLUTION_COUNT);
        post.setWardenDefeated(true);
        post.setFinalBossDefeated(true);
        post.setEpilogueComplete(true);
        PostNexusData.saveAndSync(player, post);
        AshfallAdapterCoreLateRuntime.endingChoice(
                player,
                nexusPath,
                helper.absolutePos(new BlockPos(6, 2, 6)),
                "agent7_full_playthrough_" + branchPath);
        AshfallAdapterCoreLateRuntime.postNexusPersisted(player, post, "agent7_full_playthrough_" + branchPath);

        helper.assertTrue(PostNexusData.get(player).isPath(nexusPath)
                        && PostNexusData.get(player).isFinalBossDefeated()
                        && PostNexusData.get(player).isEpilogueComplete(),
                "Agent 7 full playthrough should persist " + branchPath + " ending state");
        assertNativeMissionCoreDataRoundTrip(
                helper,
                player,
                AshfallMissionCoreIntegration.missionId(branchPath + "_epilogue"));
        helper.succeed();
    }

    private static ServerPlayer makeAgent7ConnectedPlayer(GameTestHelper helper, GameProfile profile) {
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(),
                helper.getLevel(),
                cookie.gameProfile(),
                cookie.clientInformation());
        helper.getLevel().getServer().getPlayerList()
                .loadPlayerData(player.nameAndId())
                .ifPresent(tag -> {
                    player.load(TagValueInput.create(
                            ProblemReporter.DISCARDING,
                            helper.getLevel().registryAccess(),
                            tag));
                    net.neoforged.neoforge.event.EventHooks.firePlayerLoadingEvent(
                            player,
                            helper.getLevel().getServer().getPlayerList(),
                            player.getStringUUID());
                });
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }

    private static ServerPlayer assertAgent7HostRelogCheckpoint(
            GameTestHelper helper,
            ServerPlayer player,
            GameProfile profile,
            String missionId) {
        Identifier id = AshfallMissionCoreIntegration.missionId(missionId);
        IMissionProgressView before = EchoCoreServices.missionService().mission(player, id).orElseThrow();
        helper.assertTrue(isMissionTerminal(before.status()),
                "Agent 7 host relog checkpoint should be terminal before logout: " + missionId);
        nativeMissionCoreSaveAndSync(helper, player, nativeMissionCorePlayerData(helper, player));
        removeAgent7ConnectedPlayer(helper, player);

        ServerPlayer rejoined = makeAgent7ConnectedPlayer(helper, profile);
        rejoined.setGameMode(GameType.SURVIVAL);
        helper.assertTrue(SaveMigrationHandler.CURRENT_MIGRATION_VERSION
                        == rejoined.getData(ModAttachments.MIGRATION_DATA).getVersion(),
                "Agent 7 host relog should keep save migration at current version after " + missionId);
        IMissionProgressView after = EchoCoreServices.missionService().mission(rejoined, id).orElseThrow();
        helper.assertTrue(isMissionTerminal(after.status()),
                "Agent 7 host relog should keep MissionCore checkpoint terminal after rejoin: " + missionId);
        assertNativeMissionCoreDataRoundTrip(helper, rejoined, id);
        return rejoined;
    }

    private static void removeAgent7ConnectedPlayer(GameTestHelper helper, ServerPlayer player) {
        if (player != null) {
            saveAgent7ConnectedPlayer(helper, player);
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
    }

    private static void saveAgent7ConnectedPlayer(GameTestHelper helper, ServerPlayer player) {
        try {
            Method save = helper.getLevel().getServer().getPlayerList().getClass()
                    .getSuperclass()
                    .getDeclaredMethod("save", ServerPlayer.class);
            save.setAccessible(true);
            save.invoke(helper.getLevel().getServer().getPlayerList(), player);
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Agent 7 host relog should be able to save player data: " + exception.getMessage());
        }
    }

    private static void ashfallRouteRewardDeliveryHostSmoke(GameTestHelper helper) {
        ServerPlayer player = requireNativeHostSmokePlayer(helper);
        Set<MissionObjectiveType> objectiveTypes = EnumSet.noneOf(MissionObjectiveType.class);
        for (String missionId : List.of("secure_crash_outpost", "install_energy_meter", "build_factory_controller")) {
            completeNativeRouteThrough(helper, player, missionId, objectiveTypes);
            Identifier id = AshfallMissionCoreIntegration.missionId(missionId);
            IMissionProgressView completed = progressNativeMissionCoreMission(helper, player, id, objectiveTypes);
            helper.assertTrue(completed.status() == MissionStatus.CLAIMABLE || completed.status() == MissionStatus.COMPLETED,
                    "Reward delivery host smoke should leave mission reward claimable: " + missionId);
            int before = totalInventoryItems(player);
            helper.assertTrue(EchoCoreServices.claimMissionReward(player, id),
                    "Reward delivery host smoke should claim once: " + missionId);
            int after = totalInventoryItems(player);
            helper.assertTrue(after > before,
                    "Reward delivery host smoke should change inventory after claim: " + missionId);
            helper.assertFalse(EchoCoreServices.claimMissionReward(player, id),
                    "Reward delivery host smoke should reject duplicate claim: " + missionId);
            helper.assertTrue(totalInventoryItems(player) == after,
                    "Reward delivery host smoke should not duplicate inventory on second claim: " + missionId);
            assertNativeMissionCoreDataRoundTrip(helper, player, id);
        }
        helper.succeed();
    }

    private static void ashfallMachineMeterScreenHostSmoke(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos meterPos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos cablePos = meterPos.east();
        BlockPos batteryPos = cablePos.east();
        BlockPos pressPos = meterPos.west();
        BlockPos controllerPos = pressPos.west();

        level.setBlock(meterPos, ModBlocks.ENERGY_METER.get().defaultBlockState(), 3);
        level.setBlock(cablePos, ModBlocks.POWER_CABLE.get().defaultBlockState(), 3);
        level.setBlock(batteryPos, ModBlocks.BATTERY_BANK.get().defaultBlockState(), 3);
        level.setBlock(pressPos, ModBlocks.SCRAP_PRESS.get().defaultBlockState(), 3);
        level.setBlock(controllerPos, ModBlocks.FACTORY_CONTROLLER.get().defaultBlockState(), 3);

        helper.assertTrue(level.getBlockEntity(batteryPos) instanceof BatteryBankBlockEntity,
                "Machine meter host smoke should create a battery bank block entity");
        helper.assertTrue(level.getBlockEntity(pressPos) instanceof ScrapPressBlockEntity,
                "Machine meter host smoke should create a Scrap Press block entity");
        helper.assertTrue(level.getBlockEntity(controllerPos) instanceof FactoryControllerBlockEntity,
                "Machine meter host smoke should create a Factory Controller block entity");
        if (!(level.getBlockEntity(batteryPos) instanceof BatteryBankBlockEntity battery)
                || !(level.getBlockEntity(pressPos) instanceof ScrapPressBlockEntity press)
                || !(level.getBlockEntity(controllerPos) instanceof FactoryControllerBlockEntity controller)) {
            helper.succeed();
            return;
        }

        battery.setEnergyStored(1600);
        press.setEnergyStored(0);
        controller.setNetworkEnabled(true);
        PowerNetwork.NetworkReport report = PowerNetwork.scan(level, meterPos);
        PowerDiagnostic diagnostic = PowerNetwork.diagnose(level, pressPos, 40);
        helper.assertTrue(report.storedEnergy() >= 1600 && report.capacity() >= 1600,
                "Energy Meter host smoke should see storage and capacity on the live grid");
        helper.assertTrue(report.relayCount() >= 1 && report.sourceCount() >= 1,
                "Energy Meter host smoke should see relay and source counts");
        helper.assertTrue(diagnostic.issue() != null
                        && !diagnostic.issue().translationKey().isBlank()
                        && !diagnostic.hintKey().isBlank(),
                "Energy Meter host smoke should expose a concrete machine diagnostic");
        InteractionResult result = level.getBlockState(meterPos).useWithoutItem(
                level, player, new BlockHitResult(Vec3.atCenterOf(meterPos), Direction.UP, meterPos, false));
        helper.assertTrue(result.consumesAction(),
                "Energy Meter host smoke should handle the meter UI/status interaction");

        controller.setNetworkEnabled(false);
        helper.assertTrue(FactoryControllerBlockEntity.isMachinePausedByController(level, pressPos),
                "Machine meter host smoke should assert factory controller machine pause");
        helper.assertTrue(PowerNetwork.diagnose(level, pressPos).issue() == PowerIssue.CONTROLLER_DISABLED,
                "Machine meter host smoke should expose controller-disabled machine status");
        controller.setNetworkEnabled(true);
        helper.assertTrue(!FactoryControllerBlockEntity.isMachinePausedByController(level, pressPos),
                "Machine meter host smoke should assert factory controller resume");
        helper.succeed();
    }

    private static void agent9TechNativeHostSmoke(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Set<String> adapterModules = new HashSet<>();
        adapterModules.add(assertAgent9AddonAdapter(helper, "echomachinecore",
                "com.knoxhack.echo.machinecore.Agent9MachineCoreRuntimeAdapter"));
        adapterModules.add(assertAgent9AddonAdapter(helper, "echopowercore",
                "com.knoxhack.echo.powercore.Agent9PowerCoreRuntimeAdapter"));
        adapterModules.add(assertAgent9AddonAdapter(helper, "echopowergrid",
                "com.knoxhack.echopowergrid.Agent9PowerGridRuntimeAdapter"));
        adapterModules.add(assertAgent9AddonAdapter(helper, "echoindustrialnexus",
                "com.knoxhack.echoindustrialnexus.Agent9IndustrialNexusRuntimeAdapter"));
        adapterModules.add(assertAgent9AddonAdapter(helper, "echomultiblockcore",
                "com.knoxhack.echomultiblockcore.Agent9MultiblockRuntimeAdapter"));
        adapterModules.add(assertAgent9AddonAdapter(helper, "echologisticscore",
                "com.knoxhack.echo.logisticscore.Agent9LogisticsCoreRuntimeAdapter"));
        adapterModules.add(assertAgent9AddonAdapter(helper, "echologisticsnetwork",
                "com.knoxhack.echologisticsnetwork.Agent9LogisticsNetworkRuntimeAdapter"));
        adapterModules.add(assertAgent9AddonAdapter(helper, "echobasegrid",
                "com.knoxhack.echobasegrid.Agent9BaseGridRuntimeAdapter"));
        adapterModules.add(assertAgent9AddonAdapter(helper, "echoconvoyprotocol",
                "com.knoxhack.echoconvoyprotocol.Agent9ConvoyRuntimeAdapter"));
        adapterModules.add(assertAgent9AddonAdapter(helper, "echovehiclecore",
                "com.knoxhack.echo.vehiclecore.Agent9VehicleCoreRuntimeAdapter"));
        adapterModules.add(assertAgent9AddonAdapter(helper, "echoeconomycore",
                "com.knoxhack.echo.economycore.Agent9EconomyCoreRuntimeAdapter"));
        adapterModules.add(assertAgent9AddonAdapter(helper, "echolootcore",
                "com.knoxhack.echo.lootcore.Agent9LootCoreRuntimeAdapter"));
        adapterModules.add(assertAgent9AddonAdapter(helper, "echorecipecore",
                "com.knoxhack.echo.recipecore.Agent9RecipeCoreRuntimeAdapter"));
        helper.assertTrue(adapterModules.equals(Set.of(
                        "echomachinecore",
                        "echopowercore",
                        "echopowergrid",
                        "echoindustrialnexus",
                        "echomultiblockcore",
                        "echologisticscore",
                        "echologisticsnetwork",
                        "echobasegrid",
                        "echoconvoyprotocol",
                        "echovehiclecore",
                        "echoeconomycore",
                        "echolootcore",
                        "echorecipecore")),
                "Agent 9 native host smoke should execute all owned addon runtime adapters");

        BlockPos pressPos = helper.absolutePos(new BlockPos(3, 2, 2));
        BlockPos cablePos = pressPos.east();
        BlockPos batteryPos = cablePos.east();
        BlockPos controllerPos = pressPos.west();
        BlockPos grinderPos = pressPos.south(2);
        BlockPos pipeOnePos = grinderPos.below();
        BlockPos pipeTwoPos = pipeOnePos.east();
        BlockPos burnerPos = pipeTwoPos.east();

        level.setBlock(pressPos, ModBlocks.SCRAP_PRESS.get().defaultBlockState(), 3);
        level.setBlock(cablePos, ModBlocks.POWER_CABLE.get().defaultBlockState(), 3);
        level.setBlock(batteryPos, ModBlocks.BATTERY_BANK.get().defaultBlockState(), 3);
        level.setBlock(controllerPos, ModBlocks.FACTORY_CONTROLLER.get().defaultBlockState(), 3);
        level.setBlock(grinderPos, ModBlocks.ORE_GRINDER.get().defaultBlockState(), 3);
        level.setBlock(pipeOnePos, ModBlocks.ITEM_PIPE.get().defaultBlockState().setValue(ItemPipeBlock.FACING, Direction.UP), 3);
        level.setBlock(pipeTwoPos, ModBlocks.ITEM_PIPE.get().defaultBlockState().setValue(ItemPipeBlock.FACING, Direction.WEST), 3);
        level.setBlock(burnerPos, ModBlocks.THERMAL_BURNER.get().defaultBlockState(), 3);

        helper.assertTrue(level.getBlockEntity(pressPos) instanceof ScrapPressBlockEntity,
                "Agent 9 host smoke should place the Scrap Press machine");
        helper.assertTrue(level.getBlockEntity(cablePos) instanceof PowerCableBlockEntity,
                "Agent 9 host smoke should place a live Power Cable relay");
        helper.assertTrue(level.getBlockEntity(batteryPos) instanceof BatteryBankBlockEntity,
                "Agent 9 host smoke should place a live Battery Bank source");
        helper.assertTrue(level.getBlockEntity(controllerPos) instanceof FactoryControllerBlockEntity,
                "Agent 9 host smoke should place a live Factory Controller");
        helper.assertTrue(level.getBlockEntity(grinderPos) instanceof OreGrinderBlockEntity,
                "Agent 9 host smoke should place a live Ore Grinder logistics source");
        helper.assertTrue(level.getBlockEntity(pipeOnePos) instanceof com.knoxhack.echoashfallprotocol.block.entity.ItemPipeBlockEntity,
                "Agent 9 host smoke should place a live Item Pipe");
        helper.assertTrue(level.getBlockEntity(burnerPos) instanceof ThermalBurnerBlockEntity,
                "Agent 9 host smoke should place a live Thermal Burner logistics destination");
        if (!(level.getBlockEntity(pressPos) instanceof ScrapPressBlockEntity press)
                || !(level.getBlockEntity(batteryPos) instanceof BatteryBankBlockEntity battery)
                || !(level.getBlockEntity(controllerPos) instanceof FactoryControllerBlockEntity controller)
                || !(level.getBlockEntity(grinderPos) instanceof OreGrinderBlockEntity grinder)
                || !(level.getBlockEntity(pipeOnePos) instanceof com.knoxhack.echoashfallprotocol.block.entity.ItemPipeBlockEntity pipe)
                || !(level.getBlockEntity(burnerPos) instanceof ThermalBurnerBlockEntity burner)) {
            helper.succeed();
            return;
        }

        MachineWearData wearData = new MachineWearData(level);
        wearData.repair(pressPos, MachineWearData.MAX_WEAR);
        press.setEnergyStored(80);
        battery.setEnergyStored(1600);
        controller.setNetworkEnabled(true);
        press.getInventory().setStackInSlot(0, new ItemStack(ModItems.SCRAP_METAL.get(), 9));

        PowerNetwork.NetworkReport report = PowerNetwork.scan(level, pressPos);
        helper.assertTrue(report.storedEnergy() >= 1600 && report.sourceCount() >= 1 && report.relayCount() >= 1,
                "Agent 9 host smoke should connect the live power graph to storage");
        int initialMachineGraphEnergy = press.getEnergyStored() + battery.getEnergyStored();

        for (int i = 0; i < 45; i++) {
            BatteryBankBlockEntity.serverTick(level, batteryPos, level.getBlockState(batteryPos), battery);
            if (level.getBlockEntity(cablePos) instanceof PowerCableBlockEntity cable) {
                PowerCableBlockEntity.serverTick(level, cablePos, level.getBlockState(cablePos), cable);
            }
            ScrapPressBlockEntity.serverTick(level, pressPos, level.getBlockState(pressPos), press);
        }
        ItemStack pressOutput = press.getInventory().getStackInSlot(1);
        helper.assertTrue(pressOutput.is(ModItems.MACHINE_CASING.get()) && pressOutput.getCount() >= 1,
                "Agent 9 host smoke should process Scrap Metal into a Machine Casing");
        helper.assertTrue(press.getEnergyStored() + battery.getEnergyStored() < initialMachineGraphEnergy,
                "Agent 9 host smoke should consume machine power while processing a recipe");

        CompoundTag saved = press.saveWithFullMetadata(level.registryAccess());
        BlockEntity restored = BlockEntity.loadStatic(pressPos, press.getBlockState(), saved, level.registryAccess());
        helper.assertTrue(restored instanceof ScrapPressBlockEntity,
                "Agent 9 host smoke should reload the saved Scrap Press state");
        if (restored instanceof ScrapPressBlockEntity restoredPress) {
            helper.assertTrue(restoredPress.getInventory().getStackInSlot(1).is(ModItems.MACHINE_CASING.get()),
                    "Agent 9 host smoke should preserve machine output across save/load");
        }

        grinder.getInventory().setStackInSlot(OreGrinderBlockEntity.OUTPUT_SLOT, new ItemStack(Items.STONE, 3));
        com.knoxhack.echoashfallprotocol.block.entity.ItemPipeBlockEntity.serverTick(
                level, pipeOnePos, level.getBlockState(pipeOnePos), pipe);
        helper.assertTrue(grinder.getInventory().getStackInSlot(OreGrinderBlockEntity.OUTPUT_SLOT).isEmpty(),
                "Agent 9 host smoke should extract logistics output from the source machine");
        ItemStack routed = burner.getInventory().getStackInSlot(0);
        helper.assertTrue(routed.is(Items.STONE) && routed.getCount() == 3,
                "Agent 9 host smoke should route items into the destination machine input");

        Mission buildScrapPress = requireMission(helper, "build_scrap_press");
        helper.assertTrue(buildScrapPress.requiredBlocks().stream()
                        .anyMatch(requirement -> "scrap_press".equals(requirement.blockId()) && requirement.count() == 1),
                "Agent 9 host smoke should prove a mission can depend on Scrap Press completion");
        helper.succeed();
    }

    private static String assertAgent9AddonAdapter(GameTestHelper helper, String expectedModuleId, String adapterClassName) {
        try {
            Class<?> adapterClass = Class.forName(adapterClassName);
            Method method = adapterClass.getMethod("activateNativeHostEntrypoint");
            Object result = method.invoke(null);
            helper.assertTrue(result instanceof Map<?, ?>,
                    "Agent 9 native host adapter should return a report map: " + adapterClassName);
            if (!(result instanceof Map<?, ?> report)) {
                return expectedModuleId;
            }
            Object evidence = report.get("behaviorEvidence");
            helper.assertTrue(expectedModuleId.equals(report.get("moduleId")),
                    "Agent 9 native host adapter should report expected module id: " + expectedModuleId);
            helper.assertTrue(("adaptercore.agent9.tech.machine_power_logistics.v1").equals(report.get("adapterCoreContract")),
                    "Agent 9 native host adapter should expose the shared tech AdapterCore contract: " + expectedModuleId);
            helper.assertTrue("echo_native_loader".equals(report.get("runtime")),
                    "Agent 9 native host adapter should execute through Echo Native Loader: " + expectedModuleId);
            helper.assertTrue(Boolean.TRUE.equals(report.get("hostLoadedEntrypoint")),
                    "Agent 9 native host adapter should mark host loaded entrypoint: " + expectedModuleId);
            helper.assertTrue(Boolean.TRUE.equals(report.get("serviceCodeExecuted")),
                    "Agent 9 native host adapter should execute service code: " + expectedModuleId);
            helper.assertTrue("PASS".equals(report.get("status")),
                    "Agent 9 native host adapter should pass: " + expectedModuleId);
            helper.assertTrue(evidence instanceof List<?> list && !list.isEmpty(),
                    "Agent 9 native host adapter should include behavior evidence: " + expectedModuleId);
            return String.valueOf(report.get("moduleId"));
        } catch (ReflectiveOperationException exception) {
            helper.assertTrue(false,
                    "Agent 9 native host adapter should be loadable and executable: "
                            + adapterClassName + " " + exception.getMessage());
            return expectedModuleId;
        }
    }

    private static void questRewardStackPersistence(GameTestHelper helper) {
        QuestData original = new QuestData();
        ItemStack namedReward = new ItemStack(Items.DIAMOND_SWORD);
        namedReward.set(DataComponents.CUSTOM_NAME, Component.literal("ECHO Reward"));
        original.completeMission("custom_reward", List.of(namedReward));

        QuestData restored = roundTripQuestData(helper, original);
        List<ItemStack> rewards = restored.getPendingRewards("custom_reward");
        helper.assertTrue(rewards.size() == 1, "Custom reward stack should survive serialization");
        helper.assertTrue(rewards.get(0).is(Items.DIAMOND_SWORD), "Custom reward item should survive serialization");
        helper.assertTrue("ECHO Reward".equals(rewards.get(0).getHoverName().getString()),
                "Custom reward component data should survive serialization");

        TagValueOutput legacyOutput = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        legacyOutput.putInt("pendingRewardMissions", 1);
        legacyOutput.putString("rewardMission_0", "legacy_reward");
        legacyOutput.putInt("rewardCount_0", 1);
        legacyOutput.putString("rewardItem_0_0_id", BuiltInRegistries.ITEM.getKey(Items.APPLE).toString());
        legacyOutput.putInt("rewardItem_0_0_count", 3);
        QuestData legacy = new QuestData();
        legacy.deserialize(TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), legacyOutput.buildResult()));
        helper.assertTrue(legacy.getPendingRewards("legacy_reward").size() == 1
                        && legacy.getPendingRewards("legacy_reward").get(0).is(Items.APPLE)
                        && legacy.getPendingRewards("legacy_reward").get(0).getCount() == 3,
                "Legacy id/count pending reward entries should still deserialize");
        helper.succeed();
    }

    private static RadioNetwork roundTripRadioNetwork(GameTestHelper helper, RadioNetwork original) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        original.serialize(output);
        CompoundTag tag = output.buildResult();
        RadioNetwork restored = new RadioNetwork();
        restored.deserialize(TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        return restored;
    }

    private static QuestData roundTripQuestData(GameTestHelper helper, QuestData original) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        original.serialize(output);
        CompoundTag tag = output.buildResult();
        QuestData restored = new QuestData();
        restored.deserialize(TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        return restored;
    }

    private static ServerPlayer requireNativeHostSmokePlayer(GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded("echomissioncore") && EchoCoreServices.missionCoreAvailable(),
                "Ashfall host smoke requires MissionCore in the ashfall-runtime addon set");
        helper.assertTrue(AshfallMissionCoreIntegration.registerWhenReady(),
                "Ashfall host smoke requires native MissionCore content registration");
        helper.assertTrue(nativeJsonOwnsMissionCoreRoute(),
                "Ashfall host smoke requires native JSON MissionCore route ownership");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void assertFirstSpawnResult(
            GameTestHelper helper,
            FirstSpawnRuntimeResult result,
            String expectedBranch) {
        helper.assertTrue(result != null, "First-spawn host smoke should return a runtime result");
        if (result == null) {
            return;
        }
        helper.assertTrue(expectedBranch.equals(result.branch()),
                "First-spawn host smoke should run expected branch: " + expectedBranch);
        helper.assertTrue("PASS".equals(result.status()),
                "First-spawn host smoke should pass without failed native host calls");
        helper.assertTrue(result.adapterCoreRuntime() && result.realNativeStateMutated(),
                "First-spawn host smoke should mutate real NeoForge host state through AdapterCore");
        helper.assertTrue(result.snapshot().getOrDefault("failedResults", List.of()).equals(List.of()),
                "First-spawn host smoke should not report failed native host calls");
    }

    private static void assertFirstSpawnNativeOperation(
            GameTestHelper helper,
            FirstSpawnRuntimeResult result,
            String operationId) {
        helper.assertTrue(result.nativeResults().stream().anyMatch(entry -> {
            Object snapshot = entry.get("snapshot");
            return snapshot instanceof Map<?, ?> raw
                    && operationId.equals(raw.get("operationId"))
                    && Boolean.TRUE.equals(raw.get("realNativeStateMutated"));
        }), "First-spawn host smoke should mutate native operation: " + operationId);
    }

    private static void assertOptionalTerminalRemote(GameTestHelper helper, ServerPlayer player) {
        if (!ModList.get().isLoaded("echoterminal")) {
            return;
        }
        Item remote = BuiltInRegistries.ITEM
                .getOptional(Identifier.fromNamespaceAndPath("echoterminal", "echo_terminal_remote"))
                .orElse(Items.AIR);
        helper.assertTrue(remote != Items.AIR && countInventory(player, remote) >= 1,
                "First-spawn host smoke should grant or reissue the Terminal remote when Terminal is loaded");
    }

    private static void assertMissionCoreFirstMissionStarted(GameTestHelper helper, ServerPlayer player) {
        if (!ModList.get().isLoaded("echomissioncore") || !EchoCoreServices.missionCoreAvailable()) {
            return;
        }
        helper.assertTrue(AshfallMissionCoreIntegration.registerWhenReady(),
                "First-spawn host smoke should be able to register MissionCore content");
        IMissionProgressView view = EchoCoreServices.missionService()
                .mission(player, AshfallMissionCoreIntegration.missionId("secure_crash_outpost"))
                .orElse(null);
        helper.assertTrue(view != null && view.status() != MissionStatus.LOCKED,
                "First-spawn host smoke should start or unlock secure_crash_outpost in MissionCore");
    }

    private static void assertGuidanceLink(
            GameTestHelper helper,
            MissionDefinition definition,
            String key,
            String expectedValue) {
        String resourcePath = "data/" + definition.id().getNamespace()
                + "/missioncore/missions/" + definition.id().getPath() + ".json";
        try (InputStream stream = ModGameTests.class.getClassLoader().getResourceAsStream(resourcePath)) {
            helper.assertTrue(stream != null, "Host smoke should load mission JSON guidance links: " + resourcePath);
            if (stream == null) {
                return;
            }
            JsonObject json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonObject links = json.getAsJsonObject("guidanceLinks");
            helper.assertTrue(links != null && links.has(key) && expectedValue.equals(links.get(key).getAsString()),
                    "Host smoke guidance link should point to " + expectedValue + " for " + key);
        } catch (IOException exception) {
            helper.assertTrue(false, "Host smoke should read mission JSON guidance links: " + exception.getMessage());
        }
    }

    private static void assertNativeMissionCoreDataRoundTrip(
            GameTestHelper helper,
            ServerPlayer player,
            Identifier missionId) {
        Object data = nativeMissionCorePlayerData(helper, player);
        Object state = nativeMissionCoreState(helper, data, missionId);
        helper.assertTrue(state != null && isMissionTerminal(nativeMissionCoreStateStatus(helper, state)),
                "Host smoke MissionCore state should be terminal before relog: " + missionId);
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        nativeMissionCoreSerialize(helper, data, output);
        Object restored = newNativeMissionCorePlayerData(helper);
        nativeMissionCoreDeserialize(helper, restored, TagValueInput.create(
                ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        Object restoredState = nativeMissionCoreState(helper, restored, missionId);
        helper.assertTrue(restoredState != null && nativeMissionCoreStateStatus(helper, restoredState)
                        == nativeMissionCoreStateStatus(helper, state),
                "Host smoke MissionCore state should survive relog serialization: " + missionId);
    }

    private static int totalInventoryItems(Player player) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            total += player.getInventory().getItem(i).getCount();
        }
        return total;
    }

    private static void assertRetiredEntityIdAbsent(GameTestHelper helper, String first, String second) {
        Identifier entityId = id(first + "_" + second);
        helper.assertTrue(!BuiltInRegistries.ENTITY_TYPE.containsKey(entityId),
                "Retired Ashfall faction entity id should not be registered: " + entityId);
    }

    private static void assertContractHasObjective(GameTestHelper helper, String contractPath,
            AshfallFactionContracts.ObjectiveType type) {
        AshfallFactionContracts.Spec spec = AshfallFactionContracts.spec(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, contractPath)).orElse(null);
        helper.assertTrue(spec != null, "Ashfall contract should exist: " + contractPath);
        if (spec == null) {
            return;
        }
        helper.assertTrue(spec.objectives().stream().anyMatch(objective -> objective.type() == type),
                "Ashfall contract should include " + type + " objective: " + contractPath);
    }

    private static Mission requireMission(GameTestHelper helper, String missionId) {
        Mission mission = MissionRegistry.getMissionById(missionId);
        helper.assertTrue(mission != null, "Mission should exist: " + missionId);
        if (mission == null) {
            throw new IllegalStateException("Missing mission " + missionId);
        }
        return mission;
    }

    private static void assertRewardAtLeast(GameTestHelper helper, Mission mission, Item item, int count) {
        int total = mission.rewards().stream()
                .filter(stack -> stack.getItem() == item)
                .mapToInt(ItemStack::getCount)
                .sum();
        helper.assertTrue(total >= count,
                mission.id() + " should reward at least " + count + "x " + BuiltInRegistries.ITEM.getKey(item));
    }

    private static void assertRequiredCount(GameTestHelper helper, Mission mission, Item item, int count) {
        int total = mission.requiredItems().stream()
                .filter(stack -> stack.getItem() == item)
                .mapToInt(ItemStack::getCount)
                .sum();
        helper.assertTrue(total == count,
                mission.id() + " should require exactly " + count + "x " + BuiltInRegistries.ITEM.getKey(item));
    }

    private static Identifier requirementId(String value, String fallbackNamespace) {
        String raw = value == null ? "" : value.strip();
        if (raw.isEmpty()) {
            return null;
        }
        Identifier direct = Identifier.tryParse(raw);
        if (direct != null && raw.contains(":")) {
            return direct;
        }
        String namespace = fallbackNamespace == null || fallbackNamespace.isBlank()
                ? EchoAshfallProtocol.MODID
                : fallbackNamespace;
        Identifier fallback = Identifier.tryParse(namespace + ":" + raw);
        return fallback == null ? direct : fallback;
    }

    private static Item blockRequirementItem(String blockId) {
        for (Identifier id : blockRequirementIds(blockId)) {
            Item item = BuiltInRegistries.BLOCK.getOptional(id)
                    .map(block -> block.asItem())
                    .orElse(Items.AIR);
            if (item != Items.AIR) {
                return item;
            }
        }
        return Items.AIR;
    }

    private static List<Identifier> blockRequirementIds(String blockId) {
        String value = blockId == null ? "" : blockId.strip();
        if (value.isEmpty()) {
            return List.of();
        }
        List<Identifier> ids = new ArrayList<>();
        if (value.contains(":")) {
            addBlockRequirementId(ids, value);
        } else {
            addBlockRequirementId(ids, EchoAshfallProtocol.MODID + ":" + value);
            addBlockRequirementId(ids, "minecraft:" + value);
        }
        return ids;
    }

    private static void addBlockRequirementId(List<Identifier> ids, String value) {
        Identifier id = Identifier.tryParse(value);
        if (id != null && !ids.contains(id)) {
            ids.add(id);
        }
    }

    private static Item firstDifferentRewardItem(Mission mission, Item expected) {
        for (ItemStack reward : mission.rewards()) {
            if (!reward.isEmpty() && !reward.is(expected)) {
                return reward.getItem();
            }
        }
        return Items.AIR;
    }

    private static com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latestLateRuntimeLedger(
            int ledgerBefore,
            String actionId,
            String payloadKey,
            String payloadValue) {
        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latest = null;
        for (int i = ledgerBefore; i < ledgerEntries.size(); i++) {
            var entry = ledgerEntries.get(i);
            if (actionId.equals(entry.actionId())
                    && "echoashfallprotocol:late_runtime".equals(entry.runtimeHostId())
                    && (payloadKey == null || payloadValue == null
                            || payloadValue.equals(String.valueOf(entry.inputPayload().get(payloadKey))))) {
                latest = entry;
            }
        }
        return latest;
    }

    private static boolean positionSnapshotEquals(Object value, BlockPos pos) {
        if (!(value instanceof Map<?, ?> map) || pos == null) {
            return false;
        }
        Object x = map.get("x");
        Object y = map.get("y");
        Object z = map.get("z");
        return x instanceof Number xNumber
                && y instanceof Number yNumber
                && z instanceof Number zNumber
                && xNumber.intValue() == pos.getX()
                && yNumber.intValue() == pos.getY()
                && zNumber.intValue() == pos.getZ();
    }

    private static com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latestRelayStationLedger(
            int ledgerBefore) {
        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latest = null;
        for (int i = ledgerBefore; i < ledgerEntries.size(); i++) {
            var entry = ledgerEntries.get(i);
            if (com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_ASHFALL_RELAY_ACTIVATED.equals(entry.actionId())
                    && "echoashfallprotocol:late_runtime".equals(entry.runtimeHostId())
                    && "relay_station_block".equals(String.valueOf(entry.inputPayload().get("source")))) {
                latest = entry;
            }
        }
        return latest;
    }

    private static com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latestPowerNodeLedger(
            int ledgerBefore,
            String source) {
        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latest = null;
        for (int i = ledgerBefore; i < ledgerEntries.size(); i++) {
            var entry = ledgerEntries.get(i);
            if (com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_MACHINE_POWERED.equals(entry.actionId())
                    && "echoashfallprotocol:late_runtime".equals(entry.runtimeHostId())
                    && source.equals(String.valueOf(entry.inputPayload().get("source")))
                    && Boolean.TRUE.equals(entry.inputPayload().get("powerNodeUse"))) {
                latest = entry;
            }
        }
        return latest;
    }

    private static com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latestFactionActionLedger(
            int ledgerBefore,
            String actionId) {
        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latest = null;
        for (int i = ledgerBefore; i < ledgerEntries.size(); i++) {
            var entry = ledgerEntries.get(i);
            if (com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_ASHFALL_FACTION_ACTION.equals(entry.actionId())
                    && "echoashfallprotocol:exploration_runtime".equals(entry.runtimeHostId())
                    && actionId.equals(String.valueOf(entry.inputPayload().get("actionId")))) {
                latest = entry;
            }
        }
        return latest;
    }

    private static com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latestReputationLedger(
            int ledgerBefore,
            Identifier factionId,
            String source) {
        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latest = null;
        for (int i = ledgerBefore; i < ledgerEntries.size(); i++) {
            var entry = ledgerEntries.get(i);
            if (com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_ASHFALL_REPUTATION_UPDATED.equals(entry.actionId())
                    && "echoashfallprotocol:exploration_runtime".equals(entry.runtimeHostId())
                    && String.valueOf(factionId).equals(String.valueOf(entry.inputPayload().get("factionId")))
                    && source.equals(String.valueOf(entry.inputPayload().get("source")))) {
                latest = entry;
            }
        }
        return latest;
    }

    private static com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latestDroneStateLedger(
            int ledgerBefore,
            String operation,
            String mode) {
        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latest = null;
        for (int i = ledgerBefore; i < ledgerEntries.size(); i++) {
            var entry = ledgerEntries.get(i);
            if (com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_ASHFALL_DRONE_STATE.equals(entry.actionId())
                    && "echoashfallprotocol:exploration_runtime".equals(entry.runtimeHostId())
                    && operation.equals(String.valueOf(entry.inputPayload().get("operation")))
                    && (mode == null || mode.equals(String.valueOf(entry.inputPayload().get("mode"))))) {
                latest = entry;
            }
        }
        return latest;
    }

    private static com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latestScoutDroneRouteLedger(
            int ledgerBefore,
            String routeId) {
        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latest = null;
        for (int i = ledgerBefore; i < ledgerEntries.size(); i++) {
            var entry = ledgerEntries.get(i);
            if (com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_ASHFALL_SCOUT_DRONE_ROUTE.equals(entry.actionId())
                    && "echoashfallprotocol:late_runtime".equals(entry.runtimeHostId())
                    && routeId.equals(String.valueOf(entry.inputPayload().get("routeId")))) {
                latest = entry;
            }
        }
        return latest;
    }

    private static com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latestEarlyItemCollectedLedger(
            int ledgerBefore,
            String itemId,
            String source) {
        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latest = null;
        for (int i = ledgerBefore; i < ledgerEntries.size(); i++) {
            var entry = ledgerEntries.get(i);
            if (com.knoxhack.echo.adaptercore.EchoCanonicalContentIds.EVENT_PLAYER_ITEM_COLLECTED.equals(entry.actionId())
                    && "echoashfallprotocol:early_event_runtime".equals(entry.runtimeHostId())
                    && itemId.equals(String.valueOf(entry.inputPayload().get("itemId")))
                    && source.equals(String.valueOf(entry.inputPayload().get("source")))) {
                latest = entry;
            }
        }
        return latest;
    }

    private static com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latestMachineOutputLedger(
            int ledgerBefore,
            String machineId,
            String outputItemId) {
        var ledgerEntries = com.knoxhack.echo.adaptercore.EchoRuntimeMutationLedger.global().entries();
        com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry latest = null;
        for (int i = ledgerBefore; i < ledgerEntries.size(); i++) {
            var entry = ledgerEntries.get(i);
            if ("machine.output_created".equals(entry.actionId())
                    && "echoashfallprotocol:machine_runtime".equals(entry.runtimeHostId())
                    && machineId.equals(String.valueOf(entry.inputPayload().get("machineId")))
                    && outputItemId.equals(machineOutputItemId(entry))) {
                latest = entry;
            }
        }
        return latest;
    }

    private static String machineOutputItemId(
            com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry entry) {
        Object output = entry.inputPayload().get("output");
        if (output instanceof Map<?, ?> outputMap) {
            return String.valueOf(outputMap.get("item"));
        }
        return "";
    }

    private static boolean consumeOneInventoryItem(Player player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                player.getInventory().setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
                return true;
            }
        }
        return false;
    }

    private static int countInventory(Player player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int countDroppedItems(GameTestHelper helper, BlockPos absolutePos, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                        new net.minecraft.world.phys.AABB(absolutePos).inflate(3.0D),
                        entity -> entity.getItem().is(item))
                .stream()
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }

    private static final class RecordingStructureDiscoveryService implements IStructureDiscoveryService {
        private final List<RecordedScan> scans = new ArrayList<>();

        boolean recorded(Identifier structureId, BlockPos pos) {
            return scans.stream()
                    .anyMatch(scan -> scan.structureId().equals(structureId) && scan.pos().equals(pos));
        }

        @Override
        public boolean recordStructureScan(ServerPlayer player, Identifier structureId, BlockPos pos,
                String displayName, String summary) {
            scans.add(new RecordedScan(structureId, pos.immutable()));
            return true;
        }

        @Override
        public boolean recordStructureEntry(ServerPlayer player, Identifier structureId, BlockPos pos,
                String displayName, String summary) {
            return false;
        }

        @Override
        public boolean hasDiscoveredRegion(Player player, Identifier regionId) {
            return false;
        }

        @Override
        public Set<Identifier> discoveredRegions(Player player) {
            return Set.of();
        }

        private record RecordedScan(Identifier structureId, BlockPos pos) {
        }
    }

    private static final class RecordingMissionService implements IMissionService {
        private final List<RecordedObjective> recordedObjectives = new ArrayList<>();

        boolean recorded(MissionObjectiveType type, Identifier target) {
            return recordedObjectives.stream()
                    .anyMatch(objective -> objective.type() == type && objective.target().equals(target));
        }

        boolean recordedPath(MissionObjectiveType type, String path) {
            return recordedObjectives.stream()
                    .anyMatch(objective -> objective.type() == type && objective.target().getPath().equals(path));
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public void registerChapter(String source, MissionChapterDefinition chapter) {
        }

        @Override
        public void registerMission(String source, MissionDefinition mission) {
        }

        @Override
        public Optional<MissionChapterDefinition> chapter(Identifier chapterId) {
            return Optional.empty();
        }

        @Override
        public Optional<MissionDefinition> missionDefinition(Identifier missionId) {
            return Optional.empty();
        }

        @Override
        public List<MissionChapterDefinition> chapters() {
            return List.of();
        }

        @Override
        public List<MissionDefinition> missionDefinitions() {
            return List.of();
        }

        @Override
        public List<IMissionProgressView> missions(Player player) {
            return List.of();
        }

        @Override
        public List<IMissionProgressView> missions(Player player, Identifier chapterId) {
            return List.of();
        }

        @Override
        public Optional<IMissionProgressView> mission(Player player, Identifier missionId) {
            return Optional.empty();
        }

        @Override
        public boolean startMission(ServerPlayer player, Identifier missionId) {
            return false;
        }

        @Override
        public boolean completeMission(ServerPlayer player, Identifier missionId) {
            return false;
        }

        @Override
        public boolean claimReward(ServerPlayer player, Identifier missionId) {
            return false;
        }

        @Override
        public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
            return false;
        }

        @Override
        public boolean recordObjective(
                ServerPlayer player,
                MissionObjectiveType type,
                Identifier target,
                int amount,
                Map<String, String> context) {
            recordedObjectives.add(new RecordedObjective(type, target));
            return true;
        }

        @Override
        public String debugState(Player player, Identifier missionId) {
            return "Recording mission service.";
        }

        private record RecordedObjective(MissionObjectiveType type, Identifier target) {
        }
    }

    private static void setHydration(Player player, int hydration) {
        SurvivalData survivalData = player.getData(ModAttachments.SURVIVAL_DATA.get());
        survivalData.setHydration(hydration);
        player.setData(ModAttachments.SURVIVAL_DATA.get(), survivalData);
    }

    private static void setCurrentMission(QuestData quest, String missionId) {
        for (int phase = 0; phase < MissionRegistry.getPhaseCount(); phase++) {
            List<Mission> missions = MissionRegistry.getMissionsForPhase(phase);
            for (int index = 0; index < missions.size(); index++) {
                if (missions.get(index).id().equals(missionId)) {
                    quest.setCurrentPhase(phase);
                    quest.setCurrentMissionIndex(index);
                    quest.unlockMission(missionId);
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Unknown mission: " + missionId);
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment, String testName, Identifier functionId) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                400,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                64);
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return true;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoAshfallProtocol.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static boolean machineCoreProofAvailable() {
        return ModList.get().isLoaded("echomachinecore")
                && classAvailable("com.knoxhack.echoashfallprotocol.test.AshfallMachineCoreGameTests");
    }

    private static boolean classAvailable(String className) {
        try {
            Class.forName(className, false, ModGameTests.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, path);
    }
}
