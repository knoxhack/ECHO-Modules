package com.knoxhack.echonexusprotocol.command;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echonexusprotocol.integration.NexusProgression;
import com.knoxhack.echonexusprotocol.world.NexusWorldData;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.ChunkPos;

public class NexusCommandHandler {
   public void register(Object event) {
      CommandDispatcher<CommandSourceStack> dispatcher = commandDispatcher(event);
      if (dispatcher == null) {
         return;
      }
      dispatcher
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                "nexusprotocol"
                                             )
                                             .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)))
                                          .then(
                                             Commands.literal("status")
                                                .executes(context -> status(((CommandSourceStack)context.getSource()).getPlayerOrException()))
                                          ))
                                       .then(
                                          Commands.literal("unlock")
                                             .executes(context -> unlock(((CommandSourceStack)context.getSource()).getPlayerOrException()))
                                       ))
                                    .then(
                                       Commands.literal("monolith")
                                          .executes(context -> monolith(((CommandSourceStack)context.getSource()).getPlayerOrException()))
                                    ))
                                 .then(Commands.literal("warden").executes(context -> warden(((CommandSourceStack)context.getSource()).getPlayerOrException()))))
                              .then(
                                 Commands.literal("guardian").executes(context -> guardian(((CommandSourceStack)context.getSource()).getPlayerOrException()))
                              ))
                           .then(
                              Commands.literal("fragments")
                                 .then(
                                    Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                       .executes(
                                          context -> fragments(
                                             ((CommandSourceStack)context.getSource()).getPlayerOrException(), IntegerArgumentType.getInteger(context, "count")
                                          )
                                       )
                                 )
                           ))
                        .then(
                           ((LiteralArgumentBuilder)Commands.literal("field")
                                 .then(
                                    Commands.literal("set")
                                       .then(
                                          Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                             .executes(
                                                context -> fieldSet(
                                                   ((CommandSourceStack)context.getSource()).getPlayerOrException(),
                                                   IntegerArgumentType.getInteger(context, "value")
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("add")
                                    .then(
                                       Commands.argument("delta", IntegerArgumentType.integer(-100, 100))
                                          .executes(
                                             context -> fieldAdd(
                                                ((CommandSourceStack)context.getSource()).getPlayerOrException(),
                                                IntegerArgumentType.getInteger(context, "delta")
                                             )
                                          )
                                    )
                              )
                        ))
                     .then(
                        ((LiteralArgumentBuilder)Commands.literal("corruption")
                              .then(
                                 Commands.literal("set")
                                    .then(
                                       Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                          .executes(
                                             context -> corruptionSet(
                                                ((CommandSourceStack)context.getSource()).getPlayerOrException(),
                                                IntegerArgumentType.getInteger(context, "value")
                                             )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("add")
                                 .then(
                                    Commands.argument("delta", IntegerArgumentType.integer(-100, 100))
                                       .executes(
                                          context -> corruptionAdd(
                                             ((CommandSourceStack)context.getSource()).getPlayerOrException(), IntegerArgumentType.getInteger(context, "delta")
                                          )
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.literal("research")
                        .then(
                           Commands.argument("id", StringArgumentType.word())
                              .executes(
                                 context -> research(
                                    ((CommandSourceStack)context.getSource()).getPlayerOrException(), StringArgumentType.getString(context, "id")
                                 )
                              )
                        )
                  ))
               .then(
                  Commands.literal("path")
                     .then(
                        Commands.argument("path", StringArgumentType.word())
                           .executes(
                              context -> path(((CommandSourceStack)context.getSource()).getPlayerOrException(), StringArgumentType.getString(context, "path"))
                           )
                     )
               )
         );
   }

   @SuppressWarnings("unchecked")
   private static CommandDispatcher<CommandSourceStack> commandDispatcher(Object event) {
      if (event == null) {
         return null;
      }
      try {
         Object dispatcher = event.getClass().getMethod("getDispatcher").invoke(event);
         return dispatcher instanceof CommandDispatcher<?> liveDispatcher
            ? (CommandDispatcher<CommandSourceStack>)liveDispatcher
            : null;
      } catch (ReflectiveOperationException | RuntimeException exception) {
         return null;
      }
   }

   private static int status(ServerPlayer player) {
      NexusPlayerData playerData = NexusPlayerData.get(player);
      NexusWorldData worldData = NexusWorldData.get(player.level());
      ChunkPos chunk = player.chunkPosition();
      player.sendSystemMessage(
         Component.literal(
            "ECHO-7 // NEXUS STATUS | FIELD "
               + worldData.fieldValue(chunk)
               + "% "
               + worldData.fieldState(chunk)
               + " | CORRUPTION "
               + worldData.corruptionPressure(chunk)
               + "% | QUARANTINE "
               + worldData.quarantineTicks(chunk)
               + "t | STORM "
               + (worldData.hasActiveStorm(chunk, player.level().getGameTime(), 600L) ? "ACTIVE" : "CLEAR")
               + " | TEARS "
               + worldData.realityTearCount(chunk)
         )
      );
      player.sendSystemMessage(
         Component.literal(
            "ECHO-7 // RESEARCH "
               + playerData.researchUnlocks().size()
               + "/6 | SCANS "
               + playerData.scanCount()
               + " | BLACKBOX "
               + playerData.blackboxFragments()
               + " | PATH "
               + (playerData.hasEndingPath() ? playerData.endingPath() : "unresolved")
         )
      );
      return 1;
   }

   private static int unlock(ServerPlayer player) {
      NexusProgression.grantDevelopmentUnlock(player);
      player.sendSystemMessage(Component.literal("ECHO-7 // Nexus Protocol development unlock recorded."));
      return 1;
   }

   private static int monolith(ServerPlayer player) {
      NexusPlayerData data = NexusPlayerData.get(player);
      data.activateBlackboxMonolith();
      NexusPlayerData.saveAndSync(player, data);
      NexusWorldData.get(player.level()).activateBlackboxMonolith();
      EchoCoreServices.recordMilestone(player, "nexus:blackbox_monolith_activated");
      player.sendSystemMessage(Component.literal("ECHO-7 // Blackbox Monolith activated. Forbidden Core Access indexed."));
      return 1;
   }

   private static int warden(ServerPlayer player) {
      NexusPlayerData data = NexusPlayerData.get(player);
      data.markWardenDefeated();
      NexusPlayerData.saveAndSync(player, data);
      NexusWorldData.get(player.level()).markWardenDefeated();
      player.sendSystemMessage(Component.literal("ECHO-7 // Corruption Warden completion recorded."));
      return 1;
   }

   private static int guardian(ServerPlayer player) {
      NexusPlayerData data = NexusPlayerData.get(player);
      data.markGuardianDefeated();
      NexusPlayerData.saveAndSync(player, data);
      NexusWorldData.get(player.level()).markGuardianDefeated();
      player.sendSystemMessage(Component.literal("ECHO-7 // Nexus Guardian completion recorded. Choose a Core path to finish the protocol."));
      return 1;
   }

   private static int fragments(ServerPlayer player, int count) {
      NexusPlayerData data = NexusPlayerData.get(player);

      for (int i = 0; i < count; i++) {
         data.addBlackboxFragment();
      }

      NexusPlayerData.saveAndSync(player, data);
      player.sendSystemMessage(Component.literal("ECHO-7 // Blackbox fragments indexed: +" + count + " (" + data.blackboxFragments() + " total)."));
      return count;
   }

   private static int fieldSet(ServerPlayer player, int value) {
      NexusWorldData data = NexusWorldData.get(player.level());
      data.setFieldValue(player.chunkPosition(), value);
      player.sendSystemMessage(Component.literal("ECHO-7 // Local Nexus Field set to " + data.fieldValue(player.chunkPosition()) + "%."));
      return 1;
   }

   private static int fieldAdd(ServerPlayer player, int delta) {
      NexusWorldData data = NexusWorldData.get(player.level());
      data.addFieldValue(player.chunkPosition(), delta);
      player.sendSystemMessage(Component.literal("ECHO-7 // Local Nexus Field adjusted to " + data.fieldValue(player.chunkPosition()) + "%."));
      return 1;
   }

   private static int corruptionSet(ServerPlayer player, int value) {
      NexusWorldData data = NexusWorldData.get(player.level());
      data.setCorruptionPressure(player.chunkPosition(), value);
      player.sendSystemMessage(Component.literal("ECHO-7 // Local Nexus corruption pressure set to " + data.corruptionPressure(player.chunkPosition()) + "%."));
      return 1;
   }

   private static int corruptionAdd(ServerPlayer player, int delta) {
      NexusWorldData data = NexusWorldData.get(player.level());
      data.addCorruptionPressure(player.chunkPosition(), delta);
      player.sendSystemMessage(
         Component.literal("ECHO-7 // Local Nexus corruption pressure adjusted to " + data.corruptionPressure(player.chunkPosition()) + "%.")
      );
      return 1;
   }

   private static int research(ServerPlayer player, String id) {
      NexusPlayerData data = NexusPlayerData.get(player);
      data.unlockResearch(id);
      NexusPlayerData.saveAndSync(player, data);
      player.sendSystemMessage(Component.literal("ECHO-7 // Nexus research unlocked: " + id));
      return 1;
   }

   private static int path(ServerPlayer player, String requestedPath) {
      String path = requestedPath == null ? "" : requestedPath.trim().toLowerCase(Locale.ROOT);
      String milestone = NexusProgression.milestoneForPath(path);
      if (milestone.isBlank()) {
         player.sendSystemMessage(Component.literal("ECHO-7 // Unknown Nexus path. Use restore, control, destroy, or merge."));
         return 0;
      } else {
         NexusPlayerData data = NexusPlayerData.get(player);
         if (data.hasEndingPath() && !data.endingPath().equals(path)) {
            player.sendSystemMessage(Component.literal("ECHO-7 // Nexus path already committed: " + data.endingPath() + ". Final Core paths are permanent."));
            return 0;
         }
         NexusWorldData worldData = NexusWorldData.get(player.level());
         if (!worldData.endingState().isBlank() && !worldData.endingState().equals(path)) {
            player.sendSystemMessage(Component.literal("ECHO-7 // World ending already committed: " + worldData.endingState() + ". Ending feedback was not reapplied."));
            return 0;
         }
         data.setEndingPath(path);
         NexusPlayerData.saveAndSync(player, data);
         worldData.commitEndingState(path);
         EchoCoreServices.recordMilestone(player, milestone);
         EchoCoreServices.recordMilestone(player, NexusProgression.NEXUS_PROTOCOL_COMPLETE);
         player.sendSystemMessage(Component.literal("ECHO-7 // Nexus path committed: " + path));
         return 1;
      }
   }
}
