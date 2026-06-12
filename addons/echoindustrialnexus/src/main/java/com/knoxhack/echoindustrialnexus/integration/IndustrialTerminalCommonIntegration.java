package com.knoxhack.echoindustrialnexus.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMultiblockControllerBlockEntity;
import com.knoxhack.echoindustrialnexus.network.IndustrialFactorySnapshotPacket;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echoterminal.api.TerminalActionRegistry;
import com.knoxhack.echoterminal.api.TerminalArchiveEntry;
import com.knoxhack.echoterminal.api.TerminalArchiveRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionActions;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.echoplatform.echocore.api.EchoRuntimeModules;

public final class IndustrialTerminalCommonIntegration {
   private static boolean registered;

   private IndustrialTerminalCommonIntegration() {
   }

   public static void register() {
      if (registered) {
         return;
      }
      registered = true;
      boolean missionCoreLoaded = EchoRuntimeModules.isLoaded("echomissioncore");
      if (missionCoreLoaded) {
         IndustrialMissionCoreIntegration.register();
      } else {
         TerminalMissionRegistry.register(IndustrialMissionProvider.INSTANCE);
      }
      TerminalMissionActions.registerForTab(IndustrialTerminalIds.ECHO_TAB);
      registerFactoryActions();
      registerArchive();
      EchoIndustrialNexus.LOGGER.info("ECHO Industrial Nexus terminal chapter registered.");
   }

   private static void registerFactoryActions() {
      TerminalActionRegistry.register(IndustrialTerminalIds.ECHO_TAB, IndustrialTerminalIds.FACTORY_SYNC,
         (player, payload) -> sendFactorySnapshot(player));
      TerminalActionRegistry.register(IndustrialTerminalIds.ECHO_TAB, IndustrialTerminalIds.FACTORY_REVALIDATE,
         (player, payload) -> withController(player, payload, (controller, parsed) -> controller.formOrRevalidate(player)));
      TerminalActionRegistry.register(IndustrialTerminalIds.ECHO_TAB, IndustrialTerminalIds.FACTORY_CLEAR_QUEUE,
         (player, payload) -> withController(player, payload, (controller, parsed) -> controller.clearQueue(player)));
      TerminalActionRegistry.register(IndustrialTerminalIds.ECHO_TAB, IndustrialTerminalIds.FACTORY_RETRY_BLOCKED,
         (player, payload) -> withController(player, payload, (controller, parsed) -> controller.retryBlocked(player)));
      TerminalActionRegistry.register(IndustrialTerminalIds.ECHO_TAB, IndustrialTerminalIds.FACTORY_QUEUE_TASK,
         (player, payload) -> withController(player, payload, (controller, parsed) -> {
            if (parsed.recipeId() == null) {
               player.sendSystemMessage(Component.literal("Factory command blocked: missing recipe id."));
               return;
            }
            controller.queueTasks(parsed.recipeId(), player, parsed.quantity());
         }));
      TerminalActionRegistry.register(IndustrialTerminalIds.ECHO_TAB, IndustrialTerminalIds.FACTORY_REQUEST_LOGISTICS,
         (player, payload) -> withController(player, payload, (controller, parsed) -> {
            if (parsed.recipeId() == null) {
               player.sendSystemMessage(Component.literal("Logistics request blocked: missing recipe id."));
               return;
            }
            controller.requestLogisticsLoadout(player, parsed.recipeId());
         }));
      TerminalActionRegistry.register(IndustrialTerminalIds.ECHO_TAB, IndustrialTerminalIds.FACTORY_TOGGLE_LOGISTICS_RESTOCK,
         (player, payload) -> withController(player, payload,
            (controller, parsed) -> controller.setLogisticsAutoRestockEnabled(!controller.logisticsAutoRestockEnabled(), player)));
      TerminalActionRegistry.register(IndustrialTerminalIds.ECHO_TAB, IndustrialTerminalIds.FACTORY_SET_LOGISTICS_RESTOCK_TARGET,
         (player, payload) -> withController(player, payload,
            (controller, parsed) -> controller.setLogisticsRestockTargetRuns(parsed.quantity(), player)));
      TerminalActionRegistry.register(IndustrialTerminalIds.ECHO_TAB, IndustrialTerminalIds.FACTORY_REQUEST_LOGISTICS_RESTOCK_NOW,
         (player, payload) -> withController(player, payload,
            (controller, parsed) -> controller.requestLogisticsAutoRestock(player,
               parsed.recipeId() == null ? controller.availableAutomationRecipes().stream()
                  .map(com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe::id)
                  .findFirst()
                  .orElse(null) : parsed.recipeId())));
   }

   private static void sendFactorySnapshot(ServerPlayer player) {
      EchoNetSend.toPlayer(player, IndustrialFactorySnapshotPacket.current(player));
   }

   private static void withController(ServerPlayer player, String payload, FactoryCommand command) {
      FactoryPayload parsed = FactoryPayload.parse(player, payload);
      if (player == null || parsed == null) {
         return;
      }
      ServerLevel level = parsed.level(player);
      if (level == null) {
         player.sendSystemMessage(Component.literal("Factory command blocked: dimension is unavailable."));
         sendFactorySnapshot(player);
         return;
      }
      if (parsed.controllerPos() == null || !level.isLoaded(parsed.controllerPos())) {
         player.sendSystemMessage(Component.literal("Factory command blocked: facility is unloaded."));
         sendFactorySnapshot(player);
         return;
      }
      BlockEntity blockEntity = level.getBlockEntity(parsed.controllerPos());
      if (blockEntity instanceof IndustrialMultiblockControllerBlockEntity controller) {
         command.accept(controller, parsed);
      } else {
         player.sendSystemMessage(Component.literal("Factory command blocked: target is not an Industrial controller."));
      }
      sendFactorySnapshot(player);
   }

   private static void registerArchive() {
      TerminalArchiveRegistry.register(new TerminalArchiveEntry(IndustrialTerminalIds.id("archive/thermal_flux"), "Industrial Nexus", "Thermal Flux", "RECOVERED", List.of("Recovered industrial heat-energy can be routed through Flux Ducts.", "Conversion into other ECHO energy forms remains intentionally inefficient."), false));
      TerminalArchiveRegistry.register(new TerminalArchiveEntry(IndustrialTerminalIds.id("archive/overheating"), "Industrial Nexus", "Overheating", "ACTIVE", List.of("Cool, Warm, Hot, Critical, and Meltdown states define machine risk.", "Heat sinks, coolant cells, scrubbers, and shutdown modules reduce failure chains."), false));
      TerminalArchiveRegistry.register(new TerminalArchiveEntry(IndustrialTerminalIds.id("archive/factory_control"), "Industrial Nexus", "Factory Control", "ACTIVE", List.of("Factory Controllers scan nearby machines, ducts, and Thermal Flux storage.", "Linked machines surface alerts through terminal missions and exo-suit telemetry."), false));
      TerminalArchiveRegistry.register(new TerminalArchiveEntry(IndustrialTerminalIds.id("archive/nexus_thermal_risk"), "Industrial Nexus", "Nexus Thermal Risk", "WARNING", List.of("Nexus materials in non-stabilized machines can trigger field drift.", "Use Nexus-safe ducts, stabilizer upgrades, and corruption-safe recyclers."), false));
      TerminalArchiveRegistry.register(new TerminalArchiveEntry(IndustrialTerminalIds.id("archive/furnace_warden"), "Industrial Nexus", "Furnace Warden", "HOSTILE", List.of("Industrial guardian active.", "It was not built to protect people. It was built to protect production."), false));
   }

   @FunctionalInterface
   private interface FactoryCommand {
      void accept(IndustrialMultiblockControllerBlockEntity controller, FactoryPayload payload);
   }

   private record FactoryPayload(Identifier dimension, BlockPos controllerPos, Identifier recipeId, int quantity) {
      static FactoryPayload parse(ServerPlayer player, String payload) {
         try {
            JsonObject json = payload == null || payload.isBlank()
               ? new JsonObject()
               : JsonParser.parseString(payload).getAsJsonObject();
            Identifier dimension = json.has("dimension")
               ? Identifier.parse(json.get("dimension").getAsString())
               : (player == null ? Level.OVERWORLD.identifier() : player.level().dimension().identifier());
            BlockPos pos = pos(json.get("controller_pos"));
            Identifier recipe = json.has("recipe_id") && !json.get("recipe_id").getAsString().isBlank()
               ? Identifier.parse(json.get("recipe_id").getAsString())
               : null;
            int quantity = json.has("quantity") ? json.get("quantity").getAsInt() : 1;
            return new FactoryPayload(dimension, pos, recipe, Math.max(1, Math.min(5, quantity)));
         } catch (RuntimeException exception) {
            if (player != null) {
               player.sendSystemMessage(Component.literal("Factory command blocked: malformed terminal payload."));
            }
            EchoIndustrialNexus.LOGGER.warn("Ignoring malformed Industrial terminal action payload: {}", payload);
            return null;
         }
      }

      ServerLevel level(ServerPlayer player) {
         if (player == null) {
            return null;
         }
         MinecraftServer server = player.level().getServer();
         if (server == null) {
            return null;
         }
         return server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
      }

      private static BlockPos pos(com.google.gson.JsonElement element) {
         if (element == null || element.isJsonNull()) {
            return null;
         }
         if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() < 3) {
               return null;
            }
            return new BlockPos(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
         }
         if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            return BlockPos.of(element.getAsLong());
         }
         String raw = element.getAsString().replace(',', ' ').trim();
         String[] parts = raw.split("\\s+");
         if (parts.length >= 3) {
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
         }
         return BlockPos.of(Long.parseLong(raw));
      }
   }
}
