package com.knoxhack.echonexusprotocol.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeEnvironmentBridge;
import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import com.knoxhack.echonexusprotocol.registry.ModBlocks;
import com.knoxhack.echonexusprotocol.registry.ModItems;
import com.knoxhack.echoterminal.api.mission.TerminalMissionAction;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionPresentation;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRequirement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionReward;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRoutePlacement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class NexusTerminalMissionProvider implements TerminalMissionProvider {
   public static final NexusTerminalMissionProvider INSTANCE = new NexusTerminalMissionProvider();
   private static final String ACTION_SCAN = "scan";
   private static final String ACTION_CLAIM_CACHE = "claim_cache";
   private static final String ACTION_NEXT_STEP = "next_step";
   private static final int ACCENT = -8524801;

   private NexusTerminalMissionProvider() {
   }

   public TerminalMissionChapter chapter() {
      return new TerminalMissionChapter(
         NexusTerminalIds.CHAPTER_ID,
         "NEXUS PROTOCOL",
         "Chapter IV research chain for Nexus Charge, corruption, field stabilization, matter rewriting, Blackbox memory, and Core endings.",
         400,
         -8524801,
         true
      );
   }

   public List<TerminalMissionDefinition> missions(Player player) {
      NexusPlayerData data = NexusPlayerData.get(player);
      return List.of(NexusTerminalMissionProvider.NexusMission.values()).stream().map(mission -> definition(player, data, mission)).toList();
   }

   public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
      NexusTerminalMissionProvider.NexusMission mission = mission(missionId);
      if (mission == null) {
         return new TerminalMissionSnapshot(
            missionId,
            TerminalMissionStatus.LOCKED,
            0.0F,
            "UNKNOWN",
            "No Nexus mission index entry exists for this signal.",
            "The active terminal could not resolve this Nexus record.",
            List.of()
         );
      } else {
         NexusPlayerData data = NexusPlayerData.get(player);
         boolean available = isAvailable(player, data, mission);
         boolean complete = isComplete(data, mission);
         boolean claimed = data.isTerminalCacheClaimed(mission.path());
         TerminalMissionStatus status = !available
            ? TerminalMissionStatus.LOCKED
            : (complete ? (claimed ? TerminalMissionStatus.CLAIMED : TerminalMissionStatus.CLAIMABLE) : TerminalMissionStatus.UNLOCKED);
         return new TerminalMissionSnapshot(
            mission.id(),
            status,
            progress(player, data, mission),
            statusLabel(status),
            available ? "" : lockedReason(player, data, mission),
            actionHint(player, data, mission, available, complete, claimed),
            actions(player, data, mission, available, complete, claimed)
         );
      }
   }

   public TerminalMissionPresentation presentation(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
      return new TerminalMissionPresentation(
         definition.title(), definition.briefing(), snapshot.actionHint(), definition.phaseTitle(), switch (snapshot.status()) {
            case CLAIMABLE, CLAIMED, COMPLETED -> "success";
            case UNLOCKED -> "active";
            case LOCKED, VIEW_ONLY -> "muted";
            default -> throw new MatchException(null, null);
         }, List.of(definition.category(), definition.difficulty(), snapshot.statusLabel()), "echonexusprotocol:" + definition.id().getPath()
      );
   }

   public TerminalMissionRole role(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
      return TerminalMissionRole.MAIN;
   }

   @Override
   public Optional<TerminalMissionRoutePlacement> routePlacement(
      Player player,
      TerminalMissionDefinition definition,
      TerminalMissionSnapshot snapshot,
      TerminalMissionRole role
   ) {
      NexusMission mission = mission(definition == null ? null : definition.id());
      if (mission == null) {
         return Optional.empty();
      }
      int phase = mission.phaseOrder() >= 5 ? 14 : 13;
      return Optional.of(TerminalMissionRoutePlacement.main(
         phase, mission.phaseOrder() * 100 + mission.order()));
   }

   public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
      NexusTerminalMissionProvider.NexusMission mission = mission(missionId);
      if (mission == null) {
         return false;
      } else if (ACTION_SCAN.equals(actionId)) {
         NexusPlayerData data = NexusPlayerData.get(player);
         if (mission != NexusTerminalMissionProvider.NexusMission.SIGNAL_BENEATH || !isAvailable(player, data, mission)) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Follow the current Nexus objective before scanning another route."), true);
            return true;
         }
         data.unlockResearch(NexusPlayerData.RESEARCH_NEXUS_THEORY);
         data.markScanned(NexusTerminalIds.id("terminal_field_scan"));
         data.markGearUsed("terminal_field_scan");
         NexusPlayerData.saveAndSync(player, data);
         NexusMissionHooks.recordScan(player, "terminal_field_scan");
         player.sendSystemMessage(Component.literal("ECHO-7 // Nexus field scan indexed."));
         return true;
      } else if (actionId != null && actionId.startsWith("choose_")) {
         return commitEnding(player, mission, actionId.substring("choose_".length()));
      } else if (!"claim_cache".equals(actionId)) {
         return false;
      } else {
         NexusPlayerData data = NexusPlayerData.get(player);
         if (!isAvailable(player, data, mission) || !isComplete(data, mission)) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Nexus support cache locked. Complete every listed requirement first."), true);
            return true;
         } else if (!data.markTerminalCacheClaimed(mission.path())) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Nexus support cache already claimed."), true);
            return true;
         } else {
            NexusPlayerData.saveAndSync(player, data);
            List<ItemStack> rewards = rewards(mission);
            if (!EchoCoreServices.storeTerminalRewards(player, mission.id().toString(), rewards)) {
               for (ItemStack reward : rewards) {
                  ItemStack copy = reward.copy();
                  if (!player.getInventory().add(copy)) {
                     player.drop(copy, false);
                  }
               }
            }

            return true;
         }
      }
   }

   private static boolean commitEnding(ServerPlayer player, NexusTerminalMissionProvider.NexusMission mission, String requestedPath) {
      NexusPlayerData data = NexusPlayerData.get(player);
      if (mission != NexusTerminalMissionProvider.NexusMission.REBUILDS_WORLD || !isAvailable(player, data, mission) || !data.guardianDefeated()) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Final Nexus choice locked. Defeat the Nexus Guardian before selecting a Core path."), true);
         return true;
      } else if (data.hasEndingPath()) {
         player.sendSystemMessage(Component.literal("[ECHO-7] Nexus path already committed: " + data.endingPath()), true);
         return true;
      } else {
         String path = requestedPath == null ? "" : requestedPath.trim().toLowerCase(Locale.ROOT);
         String milestone = NexusProgression.milestoneForPath(path);
         if (milestone.isBlank()) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Unknown Nexus path."), true);
            return true;
         }
         if (player.level() instanceof ServerLevel serverLevel) {
            NexusWorldData worldData = NexusWorldData.get(serverLevel);
            if (!worldData.endingState().isBlank() && !worldData.endingState().equals(path)) {
               player.sendSystemMessage(Component.literal("[ECHO-7] Final path already committed for this world: " + worldData.endingState()), true);
               return true;
            }
         }
         data.setEndingPath(path);
         data.setFinalChoiceState("committed");
         NexusPlayerData.saveAndSync(player, data);
         NexusMissionHooks.recordEndingPath(player, path);
         if (player.level() instanceof ServerLevel serverLevel) {
            NexusWorldData.get(serverLevel).commitEndingState(path);
         }
         EchoCoreServices.recordMilestone(player, milestone);
         EchoCoreServices.recordMilestone(player, NexusProgression.NEXUS_PROTOCOL_COMPLETE);
         player.sendSystemMessage(Component.literal(endingLine(path)));
         return true;
      }
   }
   private static TerminalMissionDefinition definition(Player player, NexusPlayerData data, NexusTerminalMissionProvider.NexusMission mission) {
      return new TerminalMissionDefinition(
         mission.id(),
         NexusTerminalIds.CHAPTER_ID,
         mission.phaseId(),
         mission.phaseTitle(),
         mission.phaseOrder(),
         mission.order(),
         mission.title(),
         mission.briefing(),
         mission.guide(),
         mission.category(),
         mission.difficulty(),
         icon(mission),
         prerequisites(mission),
         requirements(player, data, mission),
         rewards(mission).stream().map(TerminalMissionReward::of).toList()
      );
   }

   private static List<TerminalMissionRequirement> requirements(Player player, NexusPlayerData data, NexusTerminalMissionProvider.NexusMission mission) {
      return switch (mission) {
         case SIGNAL_BENEATH -> List.of(
            requirement(
               "Stationfall handoff",
               NexusProgression.isNexusUnlocked(player) ? "Blackbox recovery accepted." : NexusProgression.STATIONFALL_GATE + " missing.",
               stack(() -> (ItemLike)ModItems.BLACKBOX_FRAGMENT.get(), Items.OBSIDIAN, 1),
               NexusProgression.isNexusUnlocked(player) ? 1 : 0,
               1
            )
         );
         case DIRTY_CHARGE -> List.of(
            requirement(
               "Nexus Recycler",
               data.hasUsedMachine("nexus_recycler") ? "Recycler telemetry logged." : "Process salvage in a Nexus Recycler.",
               stack(() -> (ItemLike)ModBlocks.NEXUS_RECYCLER.get(), Items.FURNACE, 1),
               data.hasUsedMachine("nexus_recycler") ? 1 : 0,
               1
            )
         );
         case STABILIZE_CAMP -> List.of(
            requirement(
               "Field Stabilizer",
               data.hasUsedMachine("nexus_field_stabilizer") ? "Chunk field raised by stabilizer." : "Power and run a Nexus Field Stabilizer.",
               stack(() -> (ItemLike)ModBlocks.NEXUS_FIELD_STABILIZER.get(), Items.BEACON, 1),
               data.hasUsedMachine("nexus_field_stabilizer") ? 1 : 0,
               1
            ),
            requirement(
               "Corruption Filter",
               data.hasUsedMachine("corruption_filter") ? "Filter telemetry logged." : "Use a Corruption Filter to clean contamination.",
               stack(() -> (ItemLike)ModBlocks.CORRUPTION_FILTER.get(), Items.HOPPER, 1),
               data.hasUsedMachine("corruption_filter") ? 1 : 0,
               1
            )
         );
         case TOWER_SPEAKS -> List.of(
            requirement(
               "Memory Decoder",
               data.hasUsedMachine("memory_decoder") ? "Decoder route indexed." : "Run a Memory Decoder recipe.",
               stack(() -> (ItemLike)ModBlocks.MEMORY_DECODER.get(), Items.LODESTONE, 1),
               data.hasUsedMachine("memory_decoder") ? 1 : 0,
               1
            )
         );
         case DELETED_HISTORY -> List.of(
            requirement(
               "Blackbox fragments",
               data.blackboxFragments() >= 3 ? "Three fragments decoded." : "Recover Blackbox Fragments from vaults and bosses.",
               stack(() -> (ItemLike)ModItems.BLACKBOX_FRAGMENT.get(), Items.OBSIDIAN, 1),
               data.blackboxFragments(),
               3
            )
         );
         case QUARANTINE_FAILED -> List.of(
            requirement(
               "Corruption Warden",
               data.wardenDefeated() ? "Containment boss defeated." : "Raid a Corruption Containment Lab.",
               stack(() -> (ItemLike)ModItems.REACTOR_CORE.get(), Items.FIRE_CHARGE, 1),
               data.wardenDefeated() ? 1 : 0,
               1
            )
         );
         case MONOLITH_REMEMBERS -> List.of(
            requirement(
               "Blackbox Monolith",
               data.blackboxMonolithActivated() ? "Monolith activated." : "Activate the Blackbox Monolith.",
               stack(() -> (ItemLike)ModBlocks.BLACKBOX_PLATE.get(), Items.SCULK, 1),
               data.blackboxMonolithActivated() ? 1 : 0,
               1
            )
         );
         case REALITY_FORGE -> List.of(
            requirement(
               "Reality Forge",
               data.hasUsedMachine("reality_forge") ? "Matter rewrite completed." : "Complete a Reality Forge transmutation.",
               stack(() -> (ItemLike)ModBlocks.REALITY_FORGE.get(), Items.ANVIL, 1),
               data.hasUsedMachine("reality_forge") ? 1 : 0,
               1
            )
         );
         case CORE_DOOR -> List.of(
            requirement(
               "Core entry",
               data.coreEntered() ? "Nexus dimension route opened." : "Use the Core Access Key or Core Key Assembly.",
               stack(() -> (ItemLike)ModItems.CORE_KEY_ASSEMBLY.get(), Items.TRIPWIRE_HOOK, 1),
               data.coreEntered() ? 1 : 0,
               1
            )
         );
         case REBUILDS_WORLD -> List.of(
            requirement(
               "Ending path",
               data.hasEndingPath() ? "Path " + data.endingPath() + " committed." : "Defeat Nexus Guardian and choose a path.",
               stack(() -> (ItemLike)ModItems.STABLE_NEXUS_CORE.get(), Items.NETHER_STAR, 1),
               data.hasEndingPath() ? 1 : 0,
               1
            )
         );
      };
   }

   private static List<TerminalMissionAction> actions(
      Player player, NexusPlayerData data, NexusTerminalMissionProvider.NexusMission mission, boolean available, boolean complete, boolean claimed
   ) {
      if (!available) {
         return List.of(TerminalMissionAction.disabled(ACTION_NEXT_STEP, nextActionLabel(mission), lockedReason(player, data, mission)));
      } else if (mission == NexusTerminalMissionProvider.NexusMission.REBUILDS_WORLD && !complete && data.guardianDefeated() && !data.hasEndingPath()) {
         return List.of(TerminalMissionAction.enabled("choose_restore", "RESTORE"), TerminalMissionAction.enabled("choose_control", "CONTROL"), TerminalMissionAction.enabled("choose_destroy", "DESTROY"), TerminalMissionAction.enabled("choose_merge", "MERGE"));
      } else if (!complete && mission == NexusTerminalMissionProvider.NexusMission.SIGNAL_BENEATH) {
         return List.of(TerminalMissionAction.enabled(ACTION_SCAN, "SCAN FIELD"));
      } else if (!complete) {
         return List.of(TerminalMissionAction.disabled(ACTION_NEXT_STEP, nextActionLabel(mission), mission.guide()));
      } else {
         return List.of(
            claimed
               ? TerminalMissionAction.disabled(ACTION_CLAIM_CACHE, "CLAIM CACHE", "Support cache already claimed.")
               : TerminalMissionAction.enabled(ACTION_CLAIM_CACHE, "CLAIM CACHE")
         );
      }
   }

   private static boolean isAvailable(Player player, NexusPlayerData data, NexusTerminalMissionProvider.NexusMission mission) {
      if (mission == NexusTerminalMissionProvider.NexusMission.SIGNAL_BENEATH) {
         return NexusProgression.isNexusUnlocked(player) || data.scanCount() > 0 || data.hasUsedGear("nexus_scanner_visor");
      } else {
         NexusTerminalMissionProvider.NexusMission previous = mission.previous();
         return previous != null && isAvailable(player, data, previous) && isComplete(data, previous);
      }
   }

   private static boolean isComplete(NexusPlayerData data, NexusTerminalMissionProvider.NexusMission mission) {
      return switch (mission) {
         case SIGNAL_BENEATH -> data.scanCount() > 0 || data.hasUsedGear("nexus_scanner_visor");
         case DIRTY_CHARGE -> data.hasUsedMachine("nexus_recycler");
         case STABILIZE_CAMP -> data.hasUsedMachine("nexus_field_stabilizer") && data.hasUsedMachine("corruption_filter");
         case TOWER_SPEAKS -> data.hasUsedMachine("memory_decoder");
         case DELETED_HISTORY -> data.blackboxFragments() >= 3;
         case QUARANTINE_FAILED -> data.wardenDefeated();
         case MONOLITH_REMEMBERS -> data.blackboxMonolithActivated();
         case REALITY_FORGE -> data.hasUsedMachine("reality_forge");
         case CORE_DOOR -> data.coreEntered();
         case REBUILDS_WORLD -> data.hasEndingPath();
      };
   }

   private static float progress(Player player, NexusPlayerData data, NexusTerminalMissionProvider.NexusMission mission) {
      if (!isAvailable(player, data, mission)) {
         return 0.0F;
      } else if (isComplete(data, mission)) {
         return 1.0F;
      } else {
         return switch (mission) {
            case SIGNAL_BENEATH -> Math.min(1.0F, (data.scanCount() > 0 || data.hasUsedGear("nexus_scanner_visor")) ? 1.0F : 0.0F);
            case STABILIZE_CAMP -> (data.hasUsedMachine("nexus_field_stabilizer") ? 0.5F : 0.0F) + (data.hasUsedMachine("corruption_filter") ? 0.5F : 0.0F);
            case DELETED_HISTORY -> Math.min(1.0F, data.blackboxFragments() / 3.0F);
            case REBUILDS_WORLD -> data.guardianDefeated() ? 0.9F : 0.0F;
            default -> 0.0F;
         };
      }
   }

   private static String lockedReason(Player player, NexusPlayerData data, NexusTerminalMissionProvider.NexusMission mission) {
      if (mission == NexusTerminalMissionProvider.NexusMission.SIGNAL_BENEATH) {
         return "Recover Stationfall's blackbox handoff (" + NexusProgression.STATIONFALL_GATE
            + ") or enable the Nexus development unlock before starting Chapter IV.";
      } else {
         NexusTerminalMissionProvider.NexusMission previous = mission.previous();
         return previous == null ? "Unlock the Nexus route first." : "Complete " + previous.title() + " first.";
      }
   }

   private static String actionHint(
      Player player, NexusPlayerData data, NexusTerminalMissionProvider.NexusMission mission, boolean available, boolean complete, boolean claimed
   ) {
      if (!available) {
         return lockedReason(player, data, mission);
      } else if (complete) {
         return claimed ? "Continue to the next Nexus objective." : "Claim the support cache before moving on.";
      } else if (mission == NexusTerminalMissionProvider.NexusMission.REBUILDS_WORLD && data.guardianDefeated() && !data.hasEndingPath()) {
         return "Guardian proof accepted. Choose exactly one final Core path; Restore, Control, Destroy, and Merge permanently alter the save.";
      } else {
         return mission.guide();
      }
   }

   private static String nextActionLabel(NexusTerminalMissionProvider.NexusMission mission) {
      return switch (mission) {
         case SIGNAL_BENEATH -> "SCAN FIELD";
         case DIRTY_CHARGE -> "BUILD RECYCLER";
         case STABILIZE_CAMP -> "STABILIZE FIELD";
         case TOWER_SPEAKS -> "DECODE MEMORY";
         case DELETED_HISTORY -> "RECOVER FRAGMENTS";
         case QUARANTINE_FAILED -> "DEFEAT WARDEN";
         case MONOLITH_REMEMBERS -> "ACTIVATE MONOLITH";
         case REALITY_FORGE -> "USE FORGE";
         case CORE_DOOR -> "OPEN CORE";
         case REBUILDS_WORLD -> "CHOOSE PATH";
      };
   }

   private static List<String> prerequisites(NexusTerminalMissionProvider.NexusMission mission) {
      NexusTerminalMissionProvider.NexusMission previous = mission.previous();
      return previous == null ? List.of() : List.of(previous.title());
   }

   private static String statusLabel(TerminalMissionStatus status) {
      return switch (status) {
         case CLAIMABLE -> "CACHE READY";
         case CLAIMED -> "CLAIMED";
         case COMPLETED -> "COMPLETE";
         case UNLOCKED -> "ACTIVE";
         case LOCKED -> "LOCKED";
         case VIEW_ONLY -> "VIEW";
         default -> throw new MatchException(null, null);
      };
   }

   private static TerminalMissionRequirement requirement(String label, String detail, ItemLike icon, int have, int need) {
      int safeNeed = Math.max(1, need);
      int safeHave = Math.max(0, Math.min(safeNeed, have));
      return TerminalMissionRequirement.custom(label, detail, new ItemStack(icon), safeHave, safeNeed, safeHave >= safeNeed);
   }

   private static TerminalMissionRequirement requirement(String label, String detail, ItemStack icon, int have, int need) {
      int safeNeed = Math.max(1, need);
      int safeHave = Math.max(0, Math.min(safeNeed, have));
      return TerminalMissionRequirement.custom(label, detail, icon, safeHave, safeNeed, safeHave >= safeNeed);
   }

   private static List<ItemStack> rewards(NexusTerminalMissionProvider.NexusMission mission) {
      return switch (mission) {
         case SIGNAL_BENEATH -> stacks(
            stack(() -> (ItemLike)ModItems.NEXUS_SHARD.get(), Items.ECHO_SHARD, 2),
            stack(() -> (ItemLike)ModItems.SIGNAL_WIRE.get(), Items.REDSTONE, 2)
         );
         case DIRTY_CHARGE -> stacks(
            stack(() -> (ItemLike)ModItems.FILTER_MEMBRANE.get(), Items.PAPER, 1),
            stack(() -> (ItemLike)ModItems.FIELD_MEMBRANE.get(), Items.PHANTOM_MEMBRANE, 1)
         );
         case STABILIZE_CAMP -> stacks(
            stack(() -> (ItemLike)ModItems.PURITY_CHARGE.get(), Items.GLOWSTONE_DUST, 2),
            stack(() -> (ItemLike)ModItems.STABILIZED_PURITY_CHARGE.get(), Items.AMETHYST_SHARD, 1)
         );
         case TOWER_SPEAKS -> stacks(
            stack(() -> (ItemLike)ModItems.MEMORY_SHARD.get(), Items.PRISMARINE_SHARD, 2),
            stack(() -> (ItemLike)ModItems.DATA_FRAGMENT.get(), Items.PAPER, 3)
         );
         case DELETED_HISTORY -> stacks(
            stack(() -> (ItemLike)ModItems.BLACKBOX_FRAGMENT.get(), Items.OBSIDIAN, 1),
            stack(() -> (ItemLike)ModItems.REALITY_DUST.get(), Items.REDSTONE, 1)
         );
         case QUARANTINE_FAILED -> stacks(
            stack(() -> (ItemLike)ModItems.CORRUPTED_FERRITE.get(), Items.RAW_IRON, 3),
            stack(() -> (ItemLike)ModItems.FIELD_ANCHOR.get(), Items.ENDER_PEARL, 1)
         );
         case MONOLITH_REMEMBERS -> stacks(stack(() -> (ItemLike)ModItems.CORE_ACCESS_KEY.get(), Items.TRIPWIRE_HOOK, 1));
         case REALITY_FORGE -> stacks(
            stack(() -> (ItemLike)ModItems.STABILIZED_ALLOY.get(), Items.IRON_INGOT, 3),
            stack(() -> (ItemLike)ModItems.CORE_GLASS.get(), Items.GLASS, 2)
         );
         case CORE_DOOR -> stacks(
            stack(() -> (ItemLike)ModItems.STABILIZED_PURITY_CHARGE.get(), Items.AMETHYST_SHARD, 2),
            stack(() -> (ItemLike)ModItems.COLLAPSE_CHARGE.get(), Items.GUNPOWDER, 2)
         );
         case REBUILDS_WORLD -> stacks(
            stack(() -> (ItemLike)ModItems.STABLE_NEXUS_CORE.get(), Items.NETHER_STAR, 1),
            stack(() -> (ItemLike)ModItems.REALITY_DUST.get(), Items.REDSTONE, 4)
         );
      };
   }

   private static ItemStack icon(NexusTerminalMissionProvider.NexusMission mission) {
      return switch (mission) {
         case SIGNAL_BENEATH -> stack(() -> (ItemLike)ModItems.NEXUS_SCANNER_VISOR.get(), Items.SPYGLASS, 1);
         case DIRTY_CHARGE -> stack(() -> (ItemLike)ModBlocks.NEXUS_RECYCLER.get(), Items.FURNACE, 1);
         case STABILIZE_CAMP -> stack(() -> (ItemLike)ModBlocks.NEXUS_FIELD_STABILIZER.get(), Items.BEACON, 1);
         case TOWER_SPEAKS -> stack(() -> (ItemLike)ModBlocks.MEMORY_DECODER.get(), Items.LODESTONE, 1);
         case DELETED_HISTORY -> stack(() -> (ItemLike)ModItems.BLACKBOX_FRAGMENT.get(), Items.OBSIDIAN, 1);
         case QUARANTINE_FAILED -> stack(() -> (ItemLike)ModItems.REACTOR_CORE.get(), Items.FIRE_CHARGE, 1);
         case MONOLITH_REMEMBERS -> stack(() -> (ItemLike)ModBlocks.BLACKBOX_PLATE.get(), Items.SCULK, 1);
         case REALITY_FORGE -> stack(() -> (ItemLike)ModBlocks.REALITY_FORGE.get(), Items.ANVIL, 1);
         case CORE_DOOR -> stack(() -> (ItemLike)ModItems.CORE_KEY_ASSEMBLY.get(), Items.TRIPWIRE_HOOK, 1);
         case REBUILDS_WORLD -> stack(() -> (ItemLike)ModItems.STABLE_NEXUS_CORE.get(), Items.NETHER_STAR, 1);
      };
   }

   private static List<ItemStack> stacks(ItemStack... stacks) {
      List<ItemStack> result = new ArrayList<>();

      for (ItemStack stack : stacks) {
         if (!stack.isEmpty()) {
            result.add(stack);
         }
      }

      return List.copyOf(result);
   }

   private static ItemStack stack(ItemLike item, int count) {
      if (!EchoCoreServices.itemStackComponentsBound()) {
         return ItemStack.EMPTY;
      }
      return new ItemStack(item, count);
   }

   private static ItemStack stack(Supplier<? extends ItemLike> item, ItemLike fallback, int count) {
      if (!EchoCoreServices.itemStackComponentsBound()) {
         return ItemStack.EMPTY;
      }
      ItemLike resolved = fallback;
      if (!nativeLoaderActive()) {
         try {
            resolved = item.get();
         } catch (Throwable ignored) {
            resolved = fallback;
         }
      }
      return resolved == null ? ItemStack.EMPTY : new ItemStack(resolved, Math.max(1, count));
   }

   private static boolean nativeLoaderActive() {
      if (Boolean.getBoolean("echo.native.loader")) {
         return true;
      }
      if (!System.getProperty("echo.native.moduleIds", "").isBlank()
         || !System.getProperty("echo.native.moduleClasspath", "").isBlank()
         || !System.getProperty("echo.native.moduleClasspathFile", "").isBlank()) {
         return true;
      }
      try {
         return EchoNativeRuntimeEnvironmentBridge.isNativeLoaderActive();
      } catch (RuntimeException | LinkageError ignored) {
         return false;
      }
   }

   private static String endingLine(String path) {
      return switch (path) {
         case "restore" -> "ECHO-7 // Restore path committed. The field is damaged, but it is no longer lost.";
         case "control" -> "ECHO-7 // Control path committed. You did not save the world. You placed your hand on its throat.";
         case "destroy" -> "ECHO-7 // Destroy path committed. Silence is not peace, but it is finally silence.";
         case "merge" -> "ECHO-7 // Merge path committed. User identity no longer singular. Welcome back.";
         default -> "ECHO-7 // Nexus path committed: " + path;
      };
   }

   private static NexusTerminalMissionProvider.NexusMission mission(Identifier id) {
      if (id == null) {
         return null;
      } else {
         for (NexusTerminalMissionProvider.NexusMission mission : NexusTerminalMissionProvider.NexusMission.values()) {
            if (mission.id().equals(id)) {
               return mission;
            }
         }

         return null;
      }
   }

   private static enum NexusMission {
      SIGNAL_BENEATH(
         "the_signal_beneath",
         "SIGNAL DISCOVERY",
         0,
         0,
         "The Signal Beneath",
         "Start Chapter IV by validating the impossible signal and learning to read Nexus Field telemetry.",
         "Recover Stationfall's blackbox handoff, then use a Nexus Scanner Visor or the SCAN FIELD action to unlock Nexus Theory.",
         "Research",
         "Guide"
      ),
      DIRTY_CHARGE(
         "dirty_charge",
         "SIGNAL DISCOVERY",
         0,
         1,
         "Dirty Charge",
         "Build a Nexus Recycler and process a charge source so ECHO-7 can classify stable charge versus contamination.",
         "Place a Nexus Recycler, insert salvage or a Nexus Shard, and let one recipe finish. If it stalls, check charge, output, and the machine status line.",
         "Machine",
         "Containment"
      ),
      STABILIZE_CAMP(
         "stabilize_the_camp",
         "CONTAINMENT",
         1,
         0,
         "Stabilize the Camp",
         "Turn the first unsafe lab into a survivable base chunk.",
         "Run a Nexus Field Stabilizer and a Corruption Filter near your machines. Use the Field Map to pick the safest adjacent work chunk before dirty processing.",
         "Field",
         "Containment"
      ),
      TOWER_SPEAKS(
         "the_tower_still_speaks",
         "MEMORY",
         2,
         0,
         "The Tower Still Speaks",
         "Use old-world relay traces to find the first real memory route.",
         "Find or repair a Signal Relay Tower, then run a Memory Decoder recipe. Data Vault loot is the most reliable fragment route.",
         "Memory",
         "Route"
      ),
      DELETED_HISTORY(
         "deleted_history",
         "MEMORY",
         2,
         1,
         "Deleted History",
         "Data Vaults hold the missing records ECHO-7 was not supposed to remember.",
         "Recover and decode three Blackbox Fragments from Data Vaults, containment labs, or Warden support loot. Claim caches before the Monolith route.",
         "Memory",
         "Story"
      ),
      QUARANTINE_FAILED(
         "quarantine_failed",
         "FORBIDDEN ACCESS",
         3,
         0,
         "Quarantine Failed",
         "Enter a Corruption Containment Lab and end the failed quarantine protocol.",
         "Bring Purity Charges, a Field Anchor, and Nexus armor if possible. The Warden telegraphs pressure pulses before Static Crawlers join the fight.",
         "Boss",
         "Danger"
      ),
      MONOLITH_REMEMBERS(
         "the_monolith_remembers",
         "FORBIDDEN ACCESS",
         3,
         1,
         "The Monolith Remembers",
         "Activate the Blackbox Monolith and let the Nexus admit what caused the Collapse.",
         "Use the Blackbox Monolith Core block. Expect an anomaly storm and Forbidden Core Access unlock.",
         "Story",
         "Endgame"
      ),
      REALITY_FORGE(
         "reality_forge",
         "MATTER REWRITING",
         4,
         0,
         "Reality Forge",
         "Build the Reality Forge and prove matter rewriting can be contained.",
         "Complete one Reality Forge recipe in a stabilized chunk. Do not run it in a Critical or Collapsed field; recover the map target first.",
         "Crafting",
         "Endgame"
      ),
      CORE_DOOR(
         "the_core_door",
         "CORE CHAMBER",
         5,
         0,
         "The Core Door",
         "Assemble the Core Access Key route and cross into the Nexus dimension.",
         "Use the Core Access Key or Core Key Assembly. From the Nexus, use it again to return to your saved overworld position before the Guardian push.",
         "Route",
         "Finale"
      ),
      REBUILDS_WORLD(
         "what_rebuilds_the_world",
         "ENDING",
         6,
         0,
         "What Rebuilds the World",
         "Defeat the Nexus Guardian, then decide what kind of world survives the Core.",
         "After Guardian defeat, choose exactly one path: Restore, Control, Destroy, or Merge. The Terminal will not allow a second interpretation.",
         "Story",
         "Complete"
      );

      private final String path;
      private final String phaseTitle;
      private final int phaseOrder;
      private final int order;
      private final String title;
      private final String briefing;
      private final String guide;
      private final String category;
      private final String difficulty;

      private NexusMission(
         String path, String phaseTitle, int phaseOrder, int order, String title, String briefing, String guide, String category, String difficulty
      ) {
         this.path = path;
         this.phaseTitle = phaseTitle;
         this.phaseOrder = phaseOrder;
         this.order = order;
         this.title = title;
         this.briefing = briefing;
         this.guide = guide;
         this.category = category;
         this.difficulty = difficulty;
      }

      Identifier id() {
         return NexusTerminalIds.id(this.path);
      }

      String path() {
         return this.path;
      }

      String phaseId() {
         return this.phaseTitle.toLowerCase(Locale.ROOT).replace(' ', '_');
      }

      String phaseTitle() {
         return this.phaseTitle;
      }

      int phaseOrder() {
         return this.phaseOrder;
      }

      int order() {
         return this.order;
      }

      String title() {
         return this.title;
      }

      String briefing() {
         return this.briefing;
      }

      String guide() {
         return this.guide;
      }

      String category() {
         return this.category;
      }

      String difficulty() {
         return this.difficulty;
      }

      NexusTerminalMissionProvider.NexusMission previous() {
         int index = this.ordinal() - 1;
         return index < 0 ? null : values()[index];
      }
   }
}
