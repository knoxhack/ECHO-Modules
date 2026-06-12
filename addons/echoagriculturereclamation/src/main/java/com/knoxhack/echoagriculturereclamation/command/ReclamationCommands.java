package com.knoxhack.echoagriculturereclamation.command;

import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoagriculturereclamation.api.ReclamationFieldQuery;
import com.knoxhack.echoagriculturereclamation.api.ReclamationFieldSnapshot;
import com.knoxhack.echoagriculturereclamation.content.CropSpec;
import com.knoxhack.echoagriculturereclamation.content.ReclamationContent;
import com.knoxhack.echoagriculturereclamation.content.ReclamationMetrics;
import com.knoxhack.echoagriculturereclamation.progress.ReclamationProgress;
import com.knoxhack.echoagriculturereclamation.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;

public final class ReclamationCommands {
   private ReclamationCommands() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
      dispatcher.register(root("reclamation"));
      dispatcher.register(root("agriculturereclamation"));
   }

   private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
      return Commands.literal(name)
         .then(Commands.literal("status").executes(ctx -> status(ctx.getSource().getPlayerOrException())))
         .then(Commands.literal("scan").executes(ctx -> scan(ctx.getSource().getPlayerOrException())))
         .then(Commands.literal("debug")
            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .executes(ctx -> debug(ctx.getSource())))
         .then(Commands.literal("give_seed_capsule")
            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .executes(ctx -> giveSeedCapsule(ctx.getSource().getPlayerOrException())))
         .executes(ctx -> status(ctx.getSource().getPlayerOrException()));
   }

   private static int status(ServerPlayer player) {
      ReclamationMetrics metrics = ReclamationProgress.metrics(player);
      player.sendSystemMessage(Component.literal("FIELD RECLAMATION // Status"));
      player.sendSystemMessage(Component.literal("Seeds known: " + metrics.knownSeeds() + "/" + CropSpec.ALL.size()
         + " | Soil: " + metrics.soilLabel() + " | Food security: " + metrics.foodSecurity() + "%"));
      player.sendSystemMessage(Component.literal("Greenhouse: " + metrics.greenhouseSafety()
         + " | Crop stability: " + metrics.cropStability() + "% | Chunk restoration: " + metrics.restorationScore() + "%"));
      player.sendSystemMessage(Component.literal("Standalone loop: capsule -> seed -> purifier/tray -> stabilized crop -> restored soil."));
      return metrics.restorationScore();
   }

   private static int scan(ServerPlayer player) {
      ReclamationFieldSnapshot snapshot = ReclamationFieldQuery.player(player);
      if (snapshot == null) {
         player.sendSystemMessage(Component.literal("FIELD RECLAMATION // Scan unavailable."));
         return 0;
      }
      player.sendSystemMessage(Component.literal("FIELD RECLAMATION // Local scan "
         + snapshot.center().getX() + " " + snapshot.center().getY() + " " + snapshot.center().getZ()));
      player.sendSystemMessage(Component.literal("Soil " + snapshot.soilState().displayName()
         + " | Restoration " + snapshot.restorationScore() + "% | Greenhouse " + snapshot.greenhouseQuality()));
      player.sendSystemMessage(Component.literal("Crops " + snapshot.cropTargetCount()
         + " | Service targets " + snapshot.serviceTargetCount()
         + " | Drones " + snapshot.deployedDroneCount()));
      player.sendSystemMessage(Component.literal(snapshot.nextAction()));
      return snapshot.restorationScore();
   }

   private static int debug(CommandSourceStack source) {
      ServerPlayer player;
      try {
         player = source.getPlayerOrException();
      } catch (Exception exception) {
         source.sendSuccess(() -> Component.literal("FIELD RECLAMATION // Run debug as a player for local field data."), false);
         return 0;
      }
      ReclamationFieldSnapshot snapshot = ReclamationFieldQuery.player(player);
      ReclamationMetrics metrics = ReclamationProgress.metrics(player);
      source.sendSuccess(() -> Component.literal("FIELD RECLAMATION // Debug"), false);
      source.sendSuccess(() -> Component.literal("Data rules: crops=" + CropSpec.ALL.size()
         + ", hydroTicks=" + ReclamationContent.machines().hydroponicGrowthTicks()
         + ", nutrientCap=" + ReclamationContent.machines().hydroponicNutrientCap()
         + ", greenhouseSafe=" + ReclamationContent.progression().greenhouseSafeThreshold()), false);
      source.sendSuccess(() -> Component.literal("Player metrics: knownSeeds=" + metrics.knownSeeds()
         + ", stability=" + metrics.cropStability()
         + ", food=" + metrics.foodSecurity()
         + ", restoration=" + metrics.restorationScore()), false);
      if (snapshot != null) {
         source.sendSuccess(() -> Component.literal("Chunk=" + snapshot.chunk().x() + "," + snapshot.chunk().z()
            + " controller=" + pos(snapshot.controllerPos())
            + " lastScan=" + snapshot.lastScanTime()
            + " blocker=" + snapshot.lastMeaningfulBlocker()), false);
      }
      source.sendSuccess(() -> Component.literal("Optional stack: ashfall=" + loaded("echoashfallprotocol")
         + ", weather=" + loaded("echoweathercore")
         + ", power=" + loaded("echopowergrid")
         + ", logistics=" + loaded("echologisticsnetwork")
         + ", missioncore=" + loaded("echomissioncore")
         + ", index=" + loaded("echoindex")
         + ", terminal=" + loaded("echoterminal")), false);
      return 1;
   }

   private static int giveSeedCapsule(ServerPlayer player) {
      ItemStack capsule = new ItemStack(ModItems.RECOVERED_SEED_CAPSULE.get());
      if (!player.getInventory().add(capsule)) {
         player.drop(capsule, false);
      }
      player.sendSystemMessage(Component.literal("FIELD RECLAMATION // Test seed capsule delivered."));
      EchoAgricultureReclamation.LOGGER.debug("Gave Reclamation seed capsule to {} via command.", player.getName().getString());
      return 1;
   }

   private static boolean loaded(String modId) {
      return com.echoplatform.echocore.api.EchoRuntimeModules.isLoaded(modId);
   }

   private static String pos(net.minecraft.core.BlockPos pos) {
      if (pos == null || net.minecraft.core.BlockPos.ZERO.equals(pos)) {
         return "none";
      }
      return pos.getX() + "," + pos.getY() + "," + pos.getZ();
   }
}
