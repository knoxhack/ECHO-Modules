package com.knoxhack.echoblackboxprotocol.test;

import com.knoxhack.echocore.api.EchoHandoffs;
import com.knoxhack.echocore.api.EchoProgressLedger;
import com.knoxhack.echoblackboxprotocol.EchoBlackboxProtocol;
import com.knoxhack.echoblackboxprotocol.block.BlackboxMachineBlock;
import com.knoxhack.echoblackboxprotocol.entity.BlackboxBossEntity;
import com.knoxhack.echoblackboxprotocol.integration.BlackboxCoreIntegration;
import com.knoxhack.echoblackboxprotocol.integration.BlackboxMissionProvider;
import com.knoxhack.echoblackboxprotocol.integration.BlackboxTerminalIds;
import com.knoxhack.echoblackboxprotocol.item.BlackboxFragmentItem;
import com.knoxhack.echoblackboxprotocol.block.entity.BlackboxMachineBlockEntity;
import com.knoxhack.echoblackboxprotocol.menu.BlackboxMachineMenu;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxDungeon;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxEnding;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxEndings;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxMachineKind;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxProgress;
import com.knoxhack.echoblackboxprotocol.progression.MemoryType;
import com.knoxhack.echoblackboxprotocol.registry.ModBlocks;
import com.knoxhack.echoblackboxprotocol.registry.ModEntities;
import com.knoxhack.echoblackboxprotocol.registry.ModItems;
import com.knoxhack.echoblackboxprotocol.registry.ModMenus;
import com.knoxhack.echoblackboxprotocol.world.BlackboxWorldData;
import com.knoxhack.echoblackboxprotocol.world.DungeonSeeder;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
   private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, EchoBlackboxProtocol.MODID);
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FRAGMENT_DECODING = TEST_FUNCTIONS.register(
      "fragment_decoding", () -> ModGameTests::fragmentDecoding
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ROUTE_GATES = TEST_FUNCTIONS.register(
      "route_gates", () -> ModGameTests::routeGates
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> BOSS_REWARDS = TEST_FUNCTIONS.register(
      "boss_rewards", () -> ModGameTests::bossRewards
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENDINGS = TEST_FUNCTIONS.register(
      "endings", () -> ModGameTests::endings
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_PROVIDER = TEST_FUNCTIONS.register(
      "terminal_provider", () -> ModGameTests::terminalProvider
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DUNGEON_CACHES = TEST_FUNCTIONS.register(
      "dungeon_caches", () -> ModGameTests::dungeonCaches
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_PROCESSING = TEST_FUNCTIONS.register(
      "machine_processing", () -> ModGameTests::machineProcessing
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SURVIVAL_RECIPES = TEST_FUNCTIONS.register(
      "survival_recipes", () -> ModGameTests::survivalRecipes
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ADVANCEMENT_CHAIN = TEST_FUNCTIONS.register(
      "advancement_chain", () -> ModGameTests::advancementChain
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SAGA_GATES = TEST_FUNCTIONS.register(
      "saga_gates", () -> ModGameTests::sagaGates
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_GUI = TEST_FUNCTIONS.register(
      "machine_gui", () -> ModGameTests::machineGui
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DUNGEON_ENCOUNTERS = TEST_FUNCTIONS.register(
      "dungeon_encounters", () -> ModGameTests::dungeonEncounters
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_WORKSTATIONS = TEST_FUNCTIONS.register(
      "machine_workstations", () -> ModGameTests::machineWorkstations
   );
   private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MACHINE_POLISH = TEST_FUNCTIONS.register(
      "machine_polish", () -> ModGameTests::machinePolish
   );

   private ModGameTests() {
   }

   public static void register(IEventBus eventBus) {
      TEST_FUNCTIONS.register(eventBus);
   }

   public static void registerTests(RegisterGameTestsEvent event) {
      Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("blackbox_release"), new TestEnvironmentDefinition[0]);
      register(event, environment, "fragment_decoding", FRAGMENT_DECODING.getId());
      register(event, environment, "route_gates", ROUTE_GATES.getId());
      register(event, environment, "boss_rewards", BOSS_REWARDS.getId());
      register(event, environment, "endings", ENDINGS.getId());
      register(event, environment, "terminal_provider", TERMINAL_PROVIDER.getId());
      register(event, environment, "dungeon_caches", DUNGEON_CACHES.getId());
      register(event, environment, "machine_processing", MACHINE_PROCESSING.getId());
      register(event, environment, "survival_recipes", SURVIVAL_RECIPES.getId());
      register(event, environment, "advancement_chain", ADVANCEMENT_CHAIN.getId());
      register(event, environment, "saga_gates", SAGA_GATES.getId());
      register(event, environment, "machine_gui", MACHINE_GUI.getId());
      register(event, environment, "dungeon_encounters", DUNGEON_ENCOUNTERS.getId());
      register(event, environment, "machine_workstations", MACHINE_WORKSTATIONS.getId());
      register(event, environment, "machine_polish", MACHINE_POLISH.getId());
   }

   private static void fragmentDecoding(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      ItemStack fragment = new ItemStack((ItemLike)ModItems.COMMAND_BLACKBOX_FRAGMENT.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, fragment);
      BlackboxFragmentItem.decode(player, fragment);
      helper.assertTrue(BlackboxProgress.get(player).memoryCount(MemoryType.COMMAND) == 1, "Command fragment should decode into progress");
      helper.assertTrue(
         player.getInventory().contains(new ItemStack((ItemLike)ModItems.COMMAND_MEMORY_RECORD.get())), "Decoded command record should be granted"
      );
      helper.succeed();
   }

   private static void routeGates(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      BlackboxProgress progress = BlackboxProgress.get(player);
      helper.assertFalse(progress.canEnter(BlackboxDungeon.VAULT), "Vault route should start locked before memory proofs");
      progress.decode(player, MemoryType.PERSONAL);
      progress.decode(player, MemoryType.PERSONAL);
      progress.decode(player, MemoryType.SECURITY);
      progress.decode(player, MemoryType.SECURITY);
      helper.assertTrue(BlackboxProgress.get(player).canEnter(BlackboxDungeon.VAULT), "Vault route should unlock from Personal and Security logs");
      progress.completeDungeon(BlackboxDungeon.VAULT);
      progress.decode(player, MemoryType.COMMAND);
      progress.decode(player, MemoryType.COMMAND);
      helper.assertTrue(BlackboxProgress.get(player).canEnter(BlackboxDungeon.BUNKER), "Bunker route should unlock from Vault completion and Command Logs");
      progress.markBossDefeated("false_echo");
      progress.decode(player, MemoryType.ECHO);
      progress.decode(player, MemoryType.ECHO);
      helper.assertTrue(BlackboxProgress.get(player).canEnter(BlackboxDungeon.LABYRINTH), "Labyrinth route should unlock from False ECHO proof and ECHO Logs");
      progress.markBossDefeated("command_remnant");
      progress.decode(player, MemoryType.CORE);
      progress.decode(player, MemoryType.CORE);
      helper.assertTrue(BlackboxProgress.get(player).canEnter(BlackboxDungeon.TEMPLE), "Temple route should unlock from Command Remnant proof and Core Logs");
      helper.succeed();
   }

   private static void bossRewards(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      BlackboxBossEntity boss = (BlackboxBossEntity)((EntityType)ModEntities.FALSE_ECHO.get()).create(helper.getLevel(), EntitySpawnReason.EVENT);
      helper.assertTrue(boss != null, "False ECHO should be spawnable");
      if (boss != null) {
         boss.die(player.damageSources().playerAttack(player));
      }

      helper.assertTrue(BlackboxProgress.get(player).bossDefeated("false_echo"), "False ECHO defeat should be recorded");
      helper.assertTrue(
         player.getInventory().contains(new ItemStack((ItemLike)ModItems.ECHO_IDENTITY_FRAGMENT.get())), "False ECHO should grant identity fragment"
      );
      int count = count(player, (Item)ModItems.ECHO_IDENTITY_FRAGMENT.get());
      BlackboxBossEntity duplicate = (BlackboxBossEntity)((EntityType)ModEntities.FALSE_ECHO.get()).create(helper.getLevel(), EntitySpawnReason.EVENT);
      if (duplicate != null) {
         duplicate.die(player.damageSources().playerAttack(player));
      }

      helper.assertTrue(count(player, (Item)ModItems.ECHO_IDENTITY_FRAGMENT.get()) == count, "False ECHO should not duplicate key drops");
      Player commandPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
      BlackboxBossEntity command = (BlackboxBossEntity)((EntityType)ModEntities.COMMAND_REMNANT.get()).create(helper.getLevel(), EntitySpawnReason.EVENT);
      helper.assertTrue(command != null, "Command Remnant should be spawnable");
      if (command != null) {
         command.die(commandPlayer.damageSources().playerAttack(commandPlayer));
      }

      BlackboxProgress commandProgress = BlackboxProgress.get(commandPlayer);
      helper.assertTrue(commandProgress.bossDefeated("command_remnant"), "Command Remnant defeat should be recorded");
      helper.assertTrue(commandProgress.completed(BlackboxDungeon.BUNKER), "Command Remnant should seal the Bunker route record");
      helper.assertTrue(
         commandPlayer.getInventory().contains(new ItemStack((ItemLike)ModItems.CORE_ACCESS_KEY_MATRIX.get())), "Command Remnant should grant the key matrix"
      );
      helper.succeed();
   }

   private static void endings(GameTestHelper helper) {
      Player restorePlayer = helper.makeMockPlayer(GameType.CREATIVE);
      BlackboxProgress restore = prepareEnding(restorePlayer);
      restore.stability(40);
      restore.falseSignals(8);
      helper.assertTrue(
         BlackboxEndings.apply(restorePlayer, BlackboxEnding.RESTORE, helper.absolutePos(BlockPos.ZERO)), "Restore ending should apply after Guardian and key"
      );
      restore = BlackboxProgress.get(restorePlayer);
      helper.assertTrue(restore.ending() == BlackboxEnding.RESTORE, "Restore ending should persist");
      helper.assertTrue(restore.restoreStabilized(), "Restore should record stabilized recovery state");
      helper.assertTrue(restore.stability() == 100, "Restore should fully stabilize memory pressure");
      helper.assertFalse(restore.echoDistrusts(), "Restore should not inherit Control distrust state");

      Player controlPlayer = helper.makeMockPlayer(GameType.CREATIVE);
      BlackboxProgress control = prepareEnding(controlPlayer);
      helper.assertTrue(
         BlackboxEndings.apply(controlPlayer, BlackboxEnding.CONTROL, helper.absolutePos(BlockPos.ZERO)), "Control ending should apply after Guardian and key"
      );
      control = BlackboxProgress.get(controlPlayer);
      helper.assertTrue(control.corruptionDirected(), "Control should record directed corruption state");
      helper.assertTrue(control.echoDistrusts(), "Control should record ECHO-7 distrust state");
      helper.assertFalse(control.restoreStabilized(), "Control should not inherit Restore state");

      Player destroyPlayer = helper.makeMockPlayer(GameType.CREATIVE);
      BlackboxProgress destroy = prepareEnding(destroyPlayer);
      destroy.falseSignals(5);
      helper.assertTrue(
         BlackboxEndings.apply(destroyPlayer, BlackboxEnding.DESTROY, helper.absolutePos(BlockPos.ZERO)), "Destroy ending should apply after Guardian and key"
      );
      destroy = BlackboxProgress.get(destroyPlayer);
      helper.assertTrue(destroy.nexusSpreadStopped(), "Destroy should record stopped Nexus spread state");
      helper.assertTrue(destroy.falseSignalCount() == 0, "Destroy should clear false signals");
      helper.assertFalse(destroy.mergedIdentity(), "Destroy should not inherit Merge identity state");

      Player mergePlayer = helper.makeMockPlayer(GameType.CREATIVE);
      BlackboxProgress merge = prepareEnding(mergePlayer);
      merge.markBossDefeated("false_echo");
      merge.markBossDefeated("command_remnant");
      helper.assertFalse(
         BlackboxEndings.apply(mergePlayer, BlackboxEnding.MERGE, helper.absolutePos(BlockPos.ZERO)), "Merge should reject incomplete Deleted Logs"
      );
      addMergeProof(mergePlayer, merge);
      helper.assertTrue(
         BlackboxEndings.apply(mergePlayer, BlackboxEnding.MERGE, helper.absolutePos(BlockPos.ZERO)), "Merge should apply with deleted logs and boss proof"
      );
      merge = BlackboxProgress.get(mergePlayer);
      helper.assertTrue(merge.mergedIdentity(), "Merge should record merged identity state");
      helper.assertTrue(merge.falseSignalCount() == 0, "Merge should clear false signals into controlled tear state");
      helper.succeed();
   }

   private static BlackboxProgress prepareEnding(Player player) {
      BlackboxProgress progress = BlackboxProgress.get(player);
      progress.markBossDefeated("nexus_guardian");
      progress.markNexusCoreAccessKey();
      return progress;
   }

   private static void addMergeProof(Player player, BlackboxProgress progress) {
      progress.decode(player, MemoryType.DELETED);
      progress.decode(player, MemoryType.DELETED);
      progress.decode(player, MemoryType.DELETED);
      player.getInventory().add(new ItemStack((ItemLike)ModItems.ECHO_IDENTITY_FRAGMENT.get()));
      player.getInventory().add(new ItemStack((ItemLike)ModItems.MEMORY_STABILIZER_CORE.get()));
      player.getInventory().add(new ItemStack((ItemLike)ModItems.COMMAND_KEY.get()));
      player.getInventory().add(new ItemStack((ItemLike)ModItems.PROTOCOL_EXTRACTOR_SCHEMATIC.get()));
      player.getInventory().add(new ItemStack((ItemLike)ModItems.GUARDIAN_CORE.get()));
      player.getInventory().add(new ItemStack((ItemLike)ModBlocks.PROTOCOL_EXTRACTOR.get()));
   }

   private static void terminalProvider(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      helper.assertTrue(BlackboxMissionProvider.INSTANCE.chapter().id().equals(BlackboxTerminalIds.CHAPTER_ID), "Terminal chapter id should register cleanly");
      helper.assertTrue(!BlackboxMissionProvider.INSTANCE.missions(player).isEmpty(), "Blackbox terminal missions should be available");
      helper.succeed();
   }

   private static void machineProcessing(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      BlackboxEnding worldEnding = BlackboxWorldData.get(helper.getLevel().getServer().overworld()).ending();
      BlackboxProgress progress = BlackboxProgress.get(player);
      progress.stability(100);
      ItemStack deletedRecord = new ItemStack((ItemLike)ModItems.DELETED_MEMORY_RECORD.get());
      helper.assertTrue(
         BlackboxMachineBlock.processRecipe(player, BlackboxMachineKind.PROTOCOL_EXTRACTOR, deletedRecord),
         "Protocol Extractor should process Deleted Memory Records through blackbox_processing recipes"
      );
      helper.assertTrue(deletedRecord.isEmpty(), "Protocol Extractor recipe should consume the Deleted Memory Record");
      helper.assertTrue(player.getInventory().contains(new ItemStack((ItemLike)ModItems.STATIC_FLUID.get())), "Protocol Extractor recipe should grant Static Fluid");
      progress = BlackboxProgress.get(player);
      int expectedStability = 100 - adjustedStabilityCost(worldEnding, 10);
      helper.assertTrue(progress.stability() == expectedStability, "Protocol Extractor recipe should apply stability cost");

      ItemStack commandKey = new ItemStack((ItemLike)ModItems.COMMAND_KEY.get());
      helper.assertTrue(
         BlackboxMachineBlock.processRecipe(player, BlackboxMachineKind.CORE_KEY_ASSEMBLER, commandKey),
         "Core Key Assembler should process Command Key signatures through blackbox_processing recipes"
      );
      helper.assertTrue(commandKey.getCount() == 1, "Core Key Assembler should retain the unique Command Key proof item");
      helper.assertTrue(
         player.getInventory().contains(new ItemStack((ItemLike)ModItems.CORE_ACCESS_KEY_MATRIX.get())),
         "Core Key Assembler recipe should grant the Core Access Key Matrix"
      );
      progress = BlackboxProgress.get(player);
      expectedStability -= adjustedStabilityCost(worldEnding, 12);
      helper.assertTrue(progress.stability() == expectedStability, "Core Key Assembler recipe should apply stability cost after Protocol Extractor processing");
      helper.succeed();
   }

   private static void survivalRecipes(GameTestHelper helper) {
      assertRecipe(helper, "blackbox_decoder");
      assertRecipe(helper, "memory_projector");
      assertRecipe(helper, "archive_terminal");
      assertRecipe(helper, "core_key_assembler");
      assertRecipe(helper, "truth_engine");
      assertRecipe(helper, "memory_stabilizer");
      assertRecipe(helper, "protocol_extractor");
      assertRecipe(helper, "core_brick");
      assertRecipe(helper, "signal_glass");
      assertRecipe(helper, "black_metal_block");
      assertRecipe(helper, "black_metal_from_block");
      assertRecipe(helper, "corrupted_ferrite_block");
      assertRecipe(helper, "corrupted_ferrite_from_block");
      assertRecipe(helper, "static_fluid_from_deleted_record");
      assertRecipe(helper, "core_access_key_matrix");
      assertNoRecipe(helper, "nexus_core_access_key");

      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      BlackboxProgress progress = BlackboxProgress.get(player);
      progress.markBossDefeated("false_echo");
      progress.markBossDefeated("command_remnant");
      progress.decode(player, MemoryType.CORE);
      progress.decode(player, MemoryType.CORE);
      player.getInventory().add(new ItemStack((ItemLike)ModItems.CORE_ACCESS_KEY_LEFT.get()));
      player.getInventory().add(new ItemStack((ItemLike)ModItems.CORE_ACCESS_KEY_RIGHT.get()));
      player.getInventory().add(new ItemStack((ItemLike)ModItems.CORE_ACCESS_KEY_MATRIX.get()));
      player.getInventory().add(new ItemStack((ItemLike)ModItems.ECHO_IDENTITY_FRAGMENT.get()));
      player.getInventory().add(new ItemStack((ItemLike)ModItems.COMMAND_KEY.get()));
      helper.assertFalse(
         BlackboxMachineBlock.canAssembleCoreKey(player, progress), "Nexus Core Access Key assembly should require the Protocol Extractor schematic"
      );
      player.getInventory().add(new ItemStack((ItemLike)ModItems.PROTOCOL_EXTRACTOR_SCHEMATIC.get()));
      helper.assertTrue(BlackboxMachineBlock.canAssembleCoreKey(player, progress), "Nexus Core Access Key should assemble after all boss and schematic proofs");
      helper.succeed();
   }

   private static void advancementChain(GameTestHelper helper) {
      assertAdvancement(helper, "root");
      assertAdvancement(helper, "blackbox_decoder");
      assertAdvancement(helper, "decoded_personal");
      assertAdvancement(helper, "decoded_security");
      assertAdvancement(helper, "decoded_echo");
      assertAdvancement(helper, "decoded_command");
      assertAdvancement(helper, "decoded_core");
      assertAdvancement(helper, "decoded_deleted");
      assertAdvancement(helper, "vault_unlock");
      assertAdvancement(helper, "vault_complete");
      assertAdvancement(helper, "bunker_unlock");
      assertAdvancement(helper, "bunker_complete");
      assertAdvancement(helper, "labyrinth_unlock");
      assertAdvancement(helper, "labyrinth_complete");
      assertAdvancement(helper, "temple_unlock");
      assertAdvancement(helper, "temple_complete");
      assertAdvancement(helper, "core_chamber_unlock");
      assertAdvancement(helper, "core_chamber_complete");
      assertAdvancement(helper, "false_echo");
      assertAdvancement(helper, "command_remnant");
      assertAdvancement(helper, "nexus_core_access_key");
      assertAdvancement(helper, "nexus_guardian");
      assertAdvancement(helper, "ending_restore");
      assertAdvancement(helper, "ending_control");
      assertAdvancement(helper, "ending_destroy");
      assertAdvancement(helper, "ending_merge");
      helper.succeed();
   }

   private static void sagaGates(GameTestHelper helper) {
      helper.assertTrue(
         EchoProgressLedger.empty().withMilestone("nexus:path:restore").hasMilestone(EchoHandoffs.NEXUS_PROTOCOL_COMPLETE),
         "Blackbox Nexus gate should accept legacy Nexus path milestones through Core handoff aliases"
      );
      helper.assertTrue(
         EchoProgressLedger.empty().withMilestone("stationfall:blackbox_recovered").hasMilestone(EchoHandoffs.STATIONFALL_BLACKBOX_RECOVERED),
         "Blackbox Stationfall gate should accept legacy Stationfall handoff milestones through Core handoff aliases"
      );
      helper.assertTrue(
         BlackboxCoreIntegration.sagaGateDecisionForTest(false, null, false, false, false, false), "Blackbox should allow standalone entry when optional saga modules are absent"
      );
      helper.assertTrue(
         BlackboxCoreIntegration.sagaGateDecisionForTest(true, null, false, false, false, false), "Orbital should not block when its final seal API is not detectable"
      );
      helper.assertFalse(
         BlackboxCoreIntegration.sagaGateDecisionForTest(true, Boolean.FALSE, false, false, false, false), "Detectable Orbital final seal should be required when Orbital is installed"
      );
      helper.assertTrue(
         BlackboxCoreIntegration.sagaGateDecisionForTest(true, Boolean.TRUE, false, false, false, false), "Sealed Orbital final network should satisfy the Orbital gate"
      );
      helper.assertFalse(
         BlackboxCoreIntegration.sagaGateDecisionForTest(false, null, true, false, false, false), "Installed Stationfall should require its Blackbox handoff milestone"
      );
      helper.assertTrue(
         BlackboxCoreIntegration.sagaGateDecisionForTest(false, null, true, true, false, false), "Stationfall milestone should satisfy its handoff gate"
      );
      helper.assertFalse(
         BlackboxCoreIntegration.sagaGateDecisionForTest(false, null, false, false, true, false), "Installed Nexus Protocol should require its completion milestone"
      );
      helper.assertTrue(
         BlackboxCoreIntegration.sagaGateDecisionForTest(false, null, false, false, true, true), "Nexus Protocol milestone should satisfy its handoff gate"
      );
      helper.assertFalse(
         BlackboxCoreIntegration.sagaGateDecisionForTest(true, Boolean.TRUE, true, false, true, true), "All installed saga gates must be satisfied together"
      );
      helper.assertTrue(
         BlackboxCoreIntegration.sagaGateDecisionForTest(true, Boolean.TRUE, true, true, true, true), "Satisfied installed saga gates should allow Blackbox entry"
      );
      helper.succeed();
   }

   private static void machineGui(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.CREATIVE);
      BlockPos pos = helper.absolutePos(BlockPos.ZERO);
      helper.getLevel().setBlock(pos, ModBlocks.TRUTH_ENGINE.get().defaultBlockState(), 3);
      BlackboxMachineBlockEntity machine = (BlackboxMachineBlockEntity)helper.getLevel().getBlockEntity(pos);
      BlackboxMachineMenu menu = new BlackboxMachineMenu(1, player.getInventory(), machine, machine.data(), pos);
      helper.assertTrue(ModMenus.BLACKBOX_MACHINE.get() != null, "Blackbox machine menu type should be registered");
      helper.assertTrue(menu.kind() == BlackboxMachineKind.TRUTH_ENGINE, "Blackbox GUI should preserve machine kind");
      helper.assertTrue(menu.player() == player, "Blackbox GUI should expose the owning player for progress readouts");
      helper.assertTrue(menu.ending() == BlackboxEnding.NONE, "Blackbox GUI should read synced player progress safely");
      helper.succeed();
   }

   private static void machinePolish(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      BlackboxProgress progress = BlackboxProgress.get(player);
      progress.decode(player, MemoryType.PERSONAL);
      progress.decode(player, MemoryType.PERSONAL);
      progress.decode(player, MemoryType.SECURITY);
      progress.decode(player, MemoryType.SECURITY);
      progress.decode(player, MemoryType.ECHO);
      progress.decode(player, MemoryType.ECHO);
      progress.decode(player, MemoryType.COMMAND);
      progress.decode(player, MemoryType.COMMAND);
      progress.decode(player, MemoryType.CORE);
      progress.decode(player, MemoryType.CORE);
      progress.completeDungeon(BlackboxDungeon.VAULT);
      progress.markBossDefeated("false_echo");
      progress.markBossDefeated("command_remnant");
      progress.markBossDefeated("nexus_guardian");
      progress.markNexusCoreAccessKey();
      progress.stability(44);
      progress.falseSignals(7);

      BlackboxMachineBlockEntity syncMachine = placeMachine(helper, new BlockPos(0, 2, 52), ModBlocks.TRUTH_ENGINE.get(), BlackboxMachineKind.TRUTH_ENGINE);
      BlackboxMachineMenu menu = new BlackboxMachineMenu(2, player.getInventory(), syncMachine, syncMachine.data(), helper.absolutePos(new BlockPos(0, 2, 52)));
      helper.assertTrue(menu.memoryCount(MemoryType.PERSONAL) == 2, "Machine menu should sync Personal memory count");
      helper.assertTrue(menu.completed(BlackboxDungeon.VAULT), "Machine menu should sync dungeon completion flags");
      helper.assertTrue(menu.bossDefeated("nexus_guardian"), "Machine menu should sync boss proof flags");
      helper.assertTrue(menu.hasNexusCoreAccessKey(), "Machine menu should sync Nexus key state");
      helper.assertTrue(menu.stability() == 44 && menu.falseSignalCount() == 7, "Machine menu should sync stability and false signals");

      BlackboxMachineBlockEntity blocked = placeMachine(helper, new BlockPos(4, 2, 52), ModBlocks.BLACKBOX_DECODER.get(), BlackboxMachineKind.BLACKBOX_DECODER);
      blocked.setItem(BlackboxMachineBlockEntity.INPUT_SLOT, new ItemStack(ModItems.PERSONAL_BLACKBOX_FRAGMENT.get()));
      blocked.setItem(BlackboxMachineBlockEntity.OUTPUT_SLOT, new ItemStack(Items.DIRT));
      int beforePersonal = BlackboxProgress.get(player).memoryCount(MemoryType.PERSONAL);
      helper.assertFalse(blocked.startOperation(player), "Output-blocked Decoder should reject before consuming input");
      helper.assertTrue(blocked.status() == BlackboxMachineBlockEntity.MachineStatus.OUTPUT_BLOCKED, "Blocked output should set OUTPUT_BLOCKED status");
      helper.assertTrue(blocked.getItem(BlackboxMachineBlockEntity.INPUT_SLOT).is(ModItems.PERSONAL_BLACKBOX_FRAGMENT.get()), "Blocked output should preserve input");
      helper.assertTrue(BlackboxProgress.get(player).memoryCount(MemoryType.PERSONAL) == beforePersonal, "Blocked output should not decode progress");

      Player nativePlayer = helper.makeMockPlayer(GameType.SURVIVAL);
      BlackboxProgress nativeProgress = BlackboxProgress.get(nativePlayer);
      nativeProgress.decode(nativePlayer, MemoryType.PERSONAL);
      nativeProgress.decode(nativePlayer, MemoryType.PERSONAL);
      nativeProgress.decode(nativePlayer, MemoryType.SECURITY);
      nativeProgress.decode(nativePlayer, MemoryType.SECURITY);
      BlackboxMachineBlockEntity archive = placeMachine(helper, new BlockPos(8, 2, 52), ModBlocks.ARCHIVE_TERMINAL.get(), BlackboxMachineKind.ARCHIVE_TERMINAL);
      helper.assertTrue(archive.startOperation(nativePlayer), "Archive Terminal should run native route seal operation");
      tickMachine(helper, archive);
      helper.assertTrue(BlackboxProgress.get(nativePlayer).completed(BlackboxDungeon.VAULT), "Native Archive Terminal operation should seal Vault");
      helper.assertTrue(archive.getItem(BlackboxMachineBlockEntity.OUTPUT_SLOT).is(ModItems.CORE_ACCESS_KEY_LEFT.get()), "Archive Terminal should output left key proof");

      nativeProgress = BlackboxProgress.get(nativePlayer);
      nativeProgress.decode(nativePlayer, MemoryType.ECHO);
      nativeProgress.decode(nativePlayer, MemoryType.ECHO);
      nativeProgress.markBossDefeated("false_echo");
      BlackboxMachineBlockEntity projector = placeMachine(helper, new BlockPos(12, 2, 52), ModBlocks.MEMORY_PROJECTOR.get(), BlackboxMachineKind.MEMORY_PROJECTOR);
      helper.assertTrue(projector.startOperation(nativePlayer), "Memory Projector should run native Labyrinth operation");
      tickMachine(helper, projector);
      helper.assertTrue(BlackboxProgress.get(nativePlayer).completed(BlackboxDungeon.LABYRINTH), "Native Memory Projector operation should stabilize Labyrinth");

      nativeProgress = BlackboxProgress.get(nativePlayer);
      nativeProgress.decode(nativePlayer, MemoryType.CORE);
      nativeProgress.decode(nativePlayer, MemoryType.CORE);
      nativeProgress.markBossDefeated("command_remnant");
      nativePlayer.getInventory().add(new ItemStack(ModItems.CORE_ACCESS_KEY_LEFT.get()));
      nativePlayer.getInventory().add(new ItemStack(ModItems.CORE_ACCESS_KEY_RIGHT.get()));
      nativePlayer.getInventory().add(new ItemStack(ModItems.CORE_ACCESS_KEY_MATRIX.get()));
      nativePlayer.getInventory().add(new ItemStack(ModItems.ECHO_IDENTITY_FRAGMENT.get()));
      nativePlayer.getInventory().add(new ItemStack(ModItems.COMMAND_KEY.get()));
      nativePlayer.getInventory().add(new ItemStack(ModItems.PROTOCOL_EXTRACTOR_SCHEMATIC.get()));
      BlackboxMachineBlockEntity assembler = placeMachine(helper, new BlockPos(16, 2, 52), ModBlocks.CORE_KEY_ASSEMBLER.get(), BlackboxMachineKind.CORE_KEY_ASSEMBLER);
      helper.assertTrue(assembler.startOperation(nativePlayer), "Core Key Assembler should run native final key operation with empty input");
      tickMachine(helper, assembler);
      helper.assertTrue(assembler.getItem(BlackboxMachineBlockEntity.OUTPUT_SLOT).is(ModItems.NEXUS_CORE_ACCESS_KEY.get()), "Native assembler should output Nexus Core Access Key");

      BlockPos vault = helper.absolutePos(new BlockPos(24, 2, 52));
      DungeonSeeder.seed(helper.getLevel(), vault, BlackboxDungeon.VAULT);
      BlockPos seededDecoder = vault.east(18).above();
      BlackboxMachineBlockEntity seededMachine = (BlackboxMachineBlockEntity)helper.getLevel().getBlockEntity(seededDecoder);
      seededMachine.setItem(BlackboxMachineBlockEntity.OUTPUT_SLOT, new ItemStack(Items.DIAMOND));
      DungeonSeeder.seed(helper.getLevel(), vault, BlackboxDungeon.VAULT);
      helper.assertTrue(((BlackboxMachineBlockEntity)helper.getLevel().getBlockEntity(seededDecoder)).getItem(BlackboxMachineBlockEntity.OUTPUT_SLOT).is(Items.DIAMOND), "Repeat route seeding should preserve machine inventories");

      BlockPos bunker = helper.absolutePos(new BlockPos(72, 2, 52));
      DungeonSeeder.seed(helper.getLevel(), bunker, BlackboxDungeon.BUNKER);
      helper.assertTrue(((Container)helper.getLevel().getBlockEntity(bunker.east(12).west(4).above())).getItem(0).is(Items.ARROW), "Bunker turret lane dispensers should be loaded");

      BlackboxMachineBlockEntity dropMachine = placeMachine(helper, new BlockPos(112, 2, 52), ModBlocks.MEMORY_STABILIZER.get(), BlackboxMachineKind.MEMORY_STABILIZER);
      dropMachine.setItem(BlackboxMachineBlockEntity.INPUT_SLOT, new ItemStack(ModItems.STATIC_FLUID.get()));
      BlockPos dropPos = dropMachine.getBlockPos();
      ((BlackboxMachineBlock)ModBlocks.MEMORY_STABILIZER.get()).playerDestroy(helper.getLevel(), player, dropPos, dropMachine.getBlockState(), dropMachine, ItemStack.EMPTY);
      helper.runAfterDelay(1L, () -> {
         helper.assertTrue(
            !helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(dropPos).inflate(2.0), entity -> entity.getItem().is(ModItems.STATIC_FLUID.get())).isEmpty(),
            "Breaking a machine should drop stored inventory"
         );
         helper.succeed();
      });
   }

   private static void machineWorkstations(GameTestHelper helper) {
      Player player = helper.makeMockPlayer(GameType.SURVIVAL);
      BlackboxMachineBlockEntity decoder = placeMachine(helper, new BlockPos(0, 2, 40), ModBlocks.BLACKBOX_DECODER.get(), BlackboxMachineKind.BLACKBOX_DECODER);
      decoder.setItem(BlackboxMachineBlockEntity.INPUT_SLOT, new ItemStack(ModItems.PERSONAL_BLACKBOX_FRAGMENT.get()));
      helper.assertTrue(decoder.startOperation(player), "Decoder should accept typed fragments as machine inventory input");
      tickMachine(helper, decoder);
      helper.assertTrue(decoder.getItem(BlackboxMachineBlockEntity.INPUT_SLOT).isEmpty(), "Decoder should consume one fragment");
      helper.assertTrue(decoder.getItem(BlackboxMachineBlockEntity.OUTPUT_SLOT).is(ModItems.PERSONAL_MEMORY_RECORD.get()), "Decoder should output a decoded memory record");
      helper.assertTrue(BlackboxProgress.get(player).memoryCount(MemoryType.PERSONAL) == 1, "Decoder processing should update player memory progress");

      BlackboxMachineBlockEntity badDecoder = placeMachine(helper, new BlockPos(4, 2, 40), ModBlocks.BLACKBOX_DECODER.get(), BlackboxMachineKind.BLACKBOX_DECODER);
      badDecoder.setItem(BlackboxMachineBlockEntity.INPUT_SLOT, new ItemStack(Items.DIRT));
      helper.assertFalse(badDecoder.startOperation(player), "Decoder should reject invalid input without consuming it");
      helper.assertTrue(badDecoder.getItem(BlackboxMachineBlockEntity.INPUT_SLOT).is(Items.DIRT), "Invalid machine input should remain in place");
      helper.assertTrue(badDecoder.status() == BlackboxMachineBlockEntity.MachineStatus.BAD_INPUT, "Invalid input should set machine status");

      BlackboxMachineBlockEntity extractor = placeMachine(helper, new BlockPos(8, 2, 40), ModBlocks.PROTOCOL_EXTRACTOR.get(), BlackboxMachineKind.PROTOCOL_EXTRACTOR);
      extractor.setItem(BlackboxMachineBlockEntity.INPUT_SLOT, new ItemStack(ModItems.DELETED_MEMORY_RECORD.get()));
      helper.assertTrue(extractor.startOperation(player), "Protocol Extractor should accept Deleted Memory Records for timed processing");
      tickMachine(helper, extractor);
      helper.assertTrue(extractor.getItem(BlackboxMachineBlockEntity.OUTPUT_SLOT).is(ModItems.STATIC_FLUID.get()), "Protocol Extractor should output Static Fluid");
      helper.assertTrue(extractor.getItem(BlackboxMachineBlockEntity.OUTPUT_SLOT).getCount() == 2, "Protocol Extractor should honor processing recipe counts");

      BlackboxMachineBlockEntity assembler = placeMachine(helper, new BlockPos(12, 2, 40), ModBlocks.CORE_KEY_ASSEMBLER.get(), BlackboxMachineKind.CORE_KEY_ASSEMBLER);
      assembler.setItem(BlackboxMachineBlockEntity.INPUT_SLOT, new ItemStack(ModItems.COMMAND_KEY.get()));
      helper.assertTrue(assembler.startOperation(player), "Core Key Assembler should process Command Key into a matrix without vanilla crafting");
      tickMachine(helper, assembler);
      helper.assertTrue(assembler.getItem(BlackboxMachineBlockEntity.INPUT_SLOT).is(ModItems.COMMAND_KEY.get()), "Command Key proof should be retained while creating matrix output");
      helper.assertTrue(assembler.getItem(BlackboxMachineBlockEntity.OUTPUT_SLOT).is(ModItems.CORE_ACCESS_KEY_MATRIX.get()), "Core Key Assembler should output matrix from Blackbox processing");

      BlackboxMachineBlockEntity truth = placeMachine(helper, new BlockPos(16, 2, 40), ModBlocks.TRUTH_ENGINE.get(), BlackboxMachineKind.TRUTH_ENGINE);
      truth.setItem(BlackboxMachineBlockEntity.INPUT_SLOT, new ItemStack(ModItems.RESTORE_DIRECTIVE.get()));
      helper.assertFalse(truth.startOperation(player), "Truth Engine should reject ending directives before final proof is complete");
      helper.assertTrue(truth.status() == BlackboxMachineBlockEntity.MachineStatus.LOCKED, "Truth Engine rejection should set locked status");
      BlackboxProgress progress = BlackboxProgress.get(player);
      progress.markNexusCoreAccessKey();
      progress.markBossDefeated("nexus_guardian");
      helper.assertTrue(truth.startOperation(player), "Truth Engine should accept an eligible ending directive");
      tickMachine(helper, truth);
      helper.assertTrue(BlackboxProgress.get(player).ending() == BlackboxEnding.RESTORE, "Truth Engine should commit eligible ending after processing");

      helper.succeed();
   }

   private static void dungeonCaches(GameTestHelper helper) {
      int offset = 0;

      for (BlackboxDungeon dungeon : BlackboxDungeon.values()) {
         BlockPos center = helper.absolutePos(new BlockPos(offset, 2, 0));
         DungeonSeeder.seed(helper.getLevel(), center, dungeon);

         switch (dungeon) {
            case VAULT:
               assertCache(
                  helper,
                  center.north(18).west(5).above(),
                  ModItems.PERSONAL_BLACKBOX_FRAGMENT.get(),
                  ModItems.SECURITY_BLACKBOX_FRAGMENT.get(),
                  ModItems.STATIC_FLUID.get()
               );
               assertCache(
                  helper,
                  center.south(18).east(5).above(),
                  ModItems.ECHO_BLACKBOX_FRAGMENT.get(),
                  ModItems.CORE_BLACKBOX_FRAGMENT.get(),
                  ModItems.CORRUPTED_FERRITE.get()
               );
               break;
            case BUNKER:
               assertCache(helper, center.east(18).north(4).above(), ModItems.COMMAND_BLACKBOX_FRAGMENT.get(), ModItems.BLACK_METAL.get());
               assertCache(helper, center.west(18).south(4).above(), ModItems.CORE_ACCESS_KEY_MATRIX.get(), ModItems.STATIC_FLUID.get());
               break;
            case LABYRINTH:
               assertCache(helper, center.north(18).east(5).above(), ModItems.ECHO_BLACKBOX_FRAGMENT.get(), ModItems.STATIC_FLUID.get());
               assertCache(helper, center.south(18).west(5).above(), ModItems.DELETED_BLACKBOX_FRAGMENT.get(), ModItems.CORRUPTED_FERRITE.get());
               break;
            case TEMPLE:
               assertCache(helper, center.east(18).south(5).above(), ModItems.CORE_BLACKBOX_FRAGMENT.get(), ModItems.CORE_ACCESS_KEY_LEFT.get());
               assertCache(helper, center.west(18).north(5).above(), ModItems.DELETED_BLACKBOX_FRAGMENT.get(), ModItems.CORE_ACCESS_KEY_RIGHT.get());
               break;
            case CORE_CHAMBER:
               assertCache(
                  helper,
                  center.north(18).west(5).above(),
                  ModItems.RESTORE_DIRECTIVE.get(),
                  ModItems.CONTROL_DIRECTIVE.get(),
                  ModItems.DESTROY_DIRECTIVE.get()
               );
               assertCache(helper, center.south(18).east(5).above(), ModItems.DELETED_BLACKBOX_FRAGMENT.get(), ModItems.MERGE_DIRECTIVE.get());
         }

         offset += 80;
      }

      helper.succeed();
   }

   private static void dungeonEncounters(GameTestHelper helper) {
      BlockPos vault = helper.absolutePos(new BlockPos(0, 2, 70));
      DungeonSeeder.seed(helper.getLevel(), vault, BlackboxDungeon.VAULT);
      assertBlock(helper, vault.east(33).above(), ModBlocks.SIGNAL_GLASS.get(), "Vault server hall should include Signal Glass data spine");
      assertBlock(helper, vault.west(38).north(3).above(), Blocks.COBWEB, "Vault containment cells should include corrupted webbing");
      BlockPos vaultCache = vault.north(18).west(5).above();
      Container cache = (Container)helper.getLevel().getBlockEntity(vaultCache);
      cache.setItem(0, new ItemStack(Items.DIAMOND));
      DungeonSeeder.seed(helper.getLevel(), vault, BlackboxDungeon.VAULT);
      helper.assertTrue(((Container)helper.getLevel().getBlockEntity(vaultCache)).getItem(0).is(Items.DIAMOND), "Repeat seeding should not refill modified cache chests");

      BlockPos bunker = helper.absolutePos(new BlockPos(80, 2, 70));
      DungeonSeeder.seed(helper.getLevel(), bunker, BlackboxDungeon.BUNKER);
      assertBlock(helper, bunker.east(12).above(), Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, "Bunker turret lane should include trigger plates");
      assertBlock(helper, bunker.south(18).above(2), ModBlocks.SIGNAL_GLASS.get(), "Bunker war room should include command display");

      BlockPos labyrinth = helper.absolutePos(new BlockPos(160, 2, 70));
      DungeonSeeder.seed(helper.getLevel(), labyrinth, BlackboxDungeon.LABYRINTH);
      assertBlock(helper, labyrinth.west(33).above(), ModBlocks.SIGNAL_GLASS.get(), "Labyrinth mirrored corridor should include Signal Glass echoes");
      assertBlock(helper, labyrinth.west(2).south(10).above(), Blocks.COBWEB, "Labyrinth unstable memory path should include hallucination hazards");

      BlockPos temple = helper.absolutePos(new BlockPos(240, 2, 70));
      DungeonSeeder.seed(helper.getLevel(), temple, BlackboxDungeon.TEMPLE);
      assertBlock(helper, temple.offset(7, 4, 7), ModBlocks.SIGNAL_GLASS.get(), "Temple sentinel posts should be capped with Signal Glass");
      assertBlock(helper, temple.north(13).above(), ModBlocks.SIGNAL_GLASS.get(), "Temple machine lanes should point toward the assembler");

      BlockPos core = helper.absolutePos(new BlockPos(320, 2, 70));
      DungeonSeeder.seed(helper.getLevel(), core, BlackboxDungeon.CORE_CHAMBER);
      assertBlock(helper, core.north(10).above(), Blocks.IRON_BARS, "Core Chamber should include Guardian arena boundary bars");
      assertBlock(helper, core.south(20).above(), ModBlocks.SIGNAL_GLASS.get(), "Core Chamber should include the ending-choice approach");

      BlockPos spawn = helper.absolutePos(new BlockPos(380, 3, 70));
      helper.assertTrue(DungeonSeeder.spawnEncounter(helper.getLevel(), spawn, (EntityType<?>)ModEntities.ARCHIVE_HUSK.get()), "First sparse encounter spawn should succeed");
      helper.assertFalse(DungeonSeeder.spawnEncounter(helper.getLevel(), spawn, (EntityType<?>)ModEntities.ARCHIVE_HUSK.get()), "Second sparse encounter spawn should be suppressed nearby");
      helper.succeed();
   }

   private static BlackboxMachineBlockEntity placeMachine(GameTestHelper helper, BlockPos localPos, Block block, BlackboxMachineKind kind) {
      BlockPos pos = helper.absolutePos(localPos);
      helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
      helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof BlackboxMachineBlockEntity, kind.displayName() + " should create a machine block entity");
      BlackboxMachineBlockEntity machine = (BlackboxMachineBlockEntity)helper.getLevel().getBlockEntity(pos);
      helper.assertTrue(machine.kind() == kind, kind.displayName() + " block entity should expose the correct machine kind");
      return machine;
   }

   private static void tickMachine(GameTestHelper helper, BlackboxMachineBlockEntity machine) {
      for (int i = 0; i <= BlackboxMachineBlockEntity.DEFAULT_OPERATION_TICKS + 10; i++) {
         BlackboxMachineBlockEntity.tick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
      }
   }

   private static void assertCache(GameTestHelper helper, BlockPos cachePos, ItemLike... expectedItems) {
      helper.assertTrue(helper.getLevel().getBlockEntity(cachePos) instanceof Container, "Dungeon cache chest should be seeded at " + cachePos.toShortString());
      Container cache = (Container)helper.getLevel().getBlockEntity(cachePos);

      for (int slot = 0; slot < expectedItems.length; slot++) {
         helper.assertTrue(cache.getItem(slot).is(expectedItems[slot].asItem()), "Dungeon cache slot " + slot + " should contain " + expectedItems[slot].asItem());
      }
   }

   private static void assertBlock(GameTestHelper helper, BlockPos pos, Block expected, String message) {
      helper.assertTrue(
         helper.getLevel().getBlockState(pos).is(expected),
         message + " at " + pos.toShortString() + "; found " + helper.getLevel().getBlockState(pos).getBlock()
      );
   }

   private static void assertRecipe(GameTestHelper helper, String path) {
      Identifier recipeId = id(path);
      boolean exists = helper.getLevel().getServer().getRecipeManager().getRecipes().stream().anyMatch(holder -> holder.id().identifier().equals(recipeId));
      helper.assertTrue(exists, "Required survival recipe missing: " + recipeId);
   }

   private static void assertNoRecipe(GameTestHelper helper, String path) {
      Identifier recipeId = id(path);
      boolean exists = helper.getLevel().getServer().getRecipeManager().getRecipes().stream().anyMatch(holder -> holder.id().identifier().equals(recipeId));
      helper.assertFalse(exists, "Recipe should not be available through vanilla crafting: " + recipeId);
   }

   private static void assertAdvancement(GameTestHelper helper, String path) {
      Identifier advancementId = id(path);
      helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(advancementId) != null, "Required advancement missing: " + advancementId);
   }
   private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment, String testName, Identifier functionId) {
      TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData(
         environment, Identifier.withDefaultNamespace("empty"), 400, 0, true, Rotation.NONE, false, 1, 1, false, 2
      );
      event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
   }

   private static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoBlackboxProtocol.MODID, path);
   }

   private static int adjustedStabilityCost(BlackboxEnding ending, int baseCost) {
      return switch (ending) {
         case RESTORE -> Math.max(0, baseCost - 4);
         case CONTROL -> Math.max(1, baseCost / 2);
         case DESTROY -> baseCost + 4;
         case MERGE -> 0;
         case NONE -> baseCost;
      };
   }

   private static int count(Player player, Item item) {
      int total = 0;

      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (stack.is(item)) {
            total += stack.getCount();
         }
      }

      return total;
   }
}
