package com.knoxhack.echoindustrialnexus.block.entity;

import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.menu.IndustrialMultiblockControllerMenu;
import com.knoxhack.echoindustrialnexus.progress.IndustrialProgress;
import com.knoxhack.echoindustrialnexus.registry.ModBlockEntities;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.knoxhack.echocore.api.EchoRuntimeModules;

public class IndustrialMultiblockControllerBlockEntity extends MultiblockControllerBlockEntity implements MenuProvider {
   private static final Map<Identifier, String> LOGISTICS_LOADOUTS = Map.of(
      EchoIndustrialNexus.id("process_scrap_into_scrap_plate"), "echologisticsnetwork:industrial_scrap_processing",
      EchoIndustrialNexus.id("press_scrap_plate_into_refined_plate"), "echologisticsnetwork:industrial_plate_press",
      EchoIndustrialNexus.id("weld_reinforced_machine_frame"), "echologisticsnetwork:industrial_frame_welding",
      EchoIndustrialNexus.id("assemble_precision_circuit"), "echologisticsnetwork:industrial_precision_circuit",
      EchoIndustrialNexus.id("encode_recipe_matrix_shard"), "echologisticsnetwork:industrial_matrix_encoding",
      EchoIndustrialNexus.id("stabilize_hybrid_thermal_core"), "echologisticsnetwork:industrial_hybrid_core_stabilization",
      EchoIndustrialNexus.id("forge_core_key_assembly"), "echologisticsnetwork:industrial_core_key_forging"
   );
   private boolean logisticsAutoRestockEnabled;
   private int logisticsRestockTargetRuns = 3;
   private String lastLogisticsRestockStatus = "Auto-restock disabled.";

   public IndustrialMultiblockControllerBlockEntity(BlockPos pos, BlockState blockState) {
      super(ModBlockEntities.INDUSTRIAL_MULTIBLOCK_CONTROLLER.get(), pos, blockState);
   }

   @Override
   public void handlePlayerUse(Player player, boolean diagnosticsOnly) {
      if (player == null || diagnosticsOnly) {
         super.handlePlayerUse(player, diagnosticsOnly);
         return;
      }
      if (isFormedForOperations()) {
         player.openMenu(this);
         player.sendSystemMessage(Component.literal(statusSnapshot().name() + " controller online."));
         return;
      }
      super.handlePlayerUse(player, false);
   }

   @Override
   public Component getDisplayName() {
      return Component.translatable("container.echoindustrialnexus.industrial_multiblock_controller");
   }

   @Override
   public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
      return new IndustrialMultiblockControllerMenu(containerId, inventory, this);
   }

   public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
      IndustrialMultiblockControllerMenu.writeClientState(buffer, this);
   }

   public boolean handleMenuButton(Player player, int id) {
      if (id == IndustrialMultiblockControllerMenu.BUTTON_FORM_OR_REVALIDATE) {
         formOrRevalidate(player);
         return true;
      }
      if (id == IndustrialMultiblockControllerMenu.BUTTON_CLEAR_QUEUE) {
         clearQueue(player);
         return true;
      }
      if (id == IndustrialMultiblockControllerMenu.BUTTON_RETRY_BLOCKED) {
         retryBlocked(player);
         return true;
      }
      if (id == IndustrialMultiblockControllerMenu.BUTTON_TOGGLE_LOGISTICS_RESTOCK) {
         setLogisticsAutoRestockEnabled(!logisticsAutoRestockEnabled(), player);
         return true;
      }
      if (id == IndustrialMultiblockControllerMenu.BUTTON_CYCLE_LOGISTICS_RESTOCK_TARGET) {
         cycleLogisticsRestockTarget(player);
         return true;
      }
      if (id == IndustrialMultiblockControllerMenu.BUTTON_REQUEST_LOGISTICS_RESTOCK_NOW) {
         requestLogisticsAutoRestock(player, firstLogisticsRecipeId());
         return true;
      }
      if (id >= IndustrialMultiblockControllerMenu.BUTTON_REQUEST_LOGISTICS_BASE) {
         Identifier recipeId = recipeIdForIndex(id - IndustrialMultiblockControllerMenu.BUTTON_REQUEST_LOGISTICS_BASE);
         if (recipeId != null) {
            requestLogisticsLoadout(player, recipeId);
         }
         return true;
      }
      if (id >= IndustrialMultiblockControllerMenu.BUTTON_QUEUE_TASK_X5_BASE) {
         Identifier recipeId = recipeIdForIndex(id - IndustrialMultiblockControllerMenu.BUTTON_QUEUE_TASK_X5_BASE);
         if (recipeId != null) {
            queueTasks(recipeId, player, 5);
         }
         return true;
      }
      if (id >= IndustrialMultiblockControllerMenu.BUTTON_QUEUE_TASK_X3_BASE) {
         Identifier recipeId = recipeIdForIndex(id - IndustrialMultiblockControllerMenu.BUTTON_QUEUE_TASK_X3_BASE);
         if (recipeId != null) {
            queueTasks(recipeId, player, 3);
         }
         return true;
      }
      if (id >= IndustrialMultiblockControllerMenu.BUTTON_QUEUE_TASK_BASE) {
         Identifier recipeId = recipeIdForIndex(id - IndustrialMultiblockControllerMenu.BUTTON_QUEUE_TASK_BASE);
         if (recipeId != null) {
            queueTask(recipeId, player);
         }
         return true;
      }
      return false;
   }

   private Identifier recipeIdForIndex(int index) {
      List<MultiblockAutomationRecipe> recipes = availableAutomationRecipes();
      return index >= 0 && index < recipes.size() ? recipes.get(index).id() : null;
   }

   private Identifier firstLogisticsRecipeId() {
      return availableAutomationRecipes().stream()
         .map(MultiblockAutomationRecipe::id)
         .filter(LOGISTICS_LOADOUTS::containsKey)
         .findFirst()
         .orElse(null);
   }

   public void formOrRevalidate(Player player) {
      super.handlePlayerUse(player, false);
      if (isFormedForOperations()) {
         IndustrialProgress.markMultiblockFormed(player, getMultiblockId());
      }
   }

   public boolean logisticsAutoRestockEnabled() {
      return logisticsAutoRestockEnabled;
   }

   public void setLogisticsAutoRestockEnabled(boolean enabled) {
      this.logisticsAutoRestockEnabled = enabled;
      this.lastLogisticsRestockStatus = enabled ? "Auto-restock enabled." : "Auto-restock disabled.";
      setChanged();
   }

   public void setLogisticsAutoRestockEnabled(boolean enabled, Player player) {
      setLogisticsAutoRestockEnabled(enabled);
      if (player != null) {
         player.sendSystemMessage(Component.literal("Factory Logistics auto-restock "
            + (enabled ? "enabled" : "disabled") + " for this controller."));
      }
   }

   public int logisticsRestockTargetRuns() {
      return clampRestockTarget(logisticsRestockTargetRuns);
   }

   public void setLogisticsRestockTargetRuns(int targetRuns) {
      this.logisticsRestockTargetRuns = clampRestockTarget(targetRuns);
      this.lastLogisticsRestockStatus = "Auto-restock target set to x" + logisticsRestockTargetRuns + ".";
      setChanged();
   }

   public void setLogisticsRestockTargetRuns(int targetRuns, Player player) {
      setLogisticsRestockTargetRuns(targetRuns);
      if (player != null) {
         player.sendSystemMessage(Component.literal("Factory Logistics auto-restock target set to x"
            + logisticsRestockTargetRuns() + "."));
      }
   }

   public void cycleLogisticsRestockTarget(Player player) {
      int next = logisticsRestockTargetRuns() == 1 ? 3 : logisticsRestockTargetRuns() == 3 ? 5 : 1;
      setLogisticsRestockTargetRuns(next, player);
   }

   public String logisticsLoadoutIdForRecipe(Identifier recipeId) {
      return recipeId == null ? "" : LOGISTICS_LOADOUTS.getOrDefault(recipeId, "");
   }

   public String logisticsLoadoutIdForRecipe(String recipeId) {
      Identifier parsed = Identifier.tryParse(recipeId == null ? "" : recipeId);
      return logisticsLoadoutIdForRecipe(parsed);
   }

   public int logisticsCurrentInputRuns(String loadoutId) {
      return 0;
   }

   public String logisticsRestockStatusLine() {
      return lastLogisticsRestockStatus == null || lastLogisticsRestockStatus.isBlank()
         ? "Auto-restock disabled."
         : lastLogisticsRestockStatus;
   }

   public void requestLogisticsLoadout(Player player, Identifier recipeId) {
      if (!(player instanceof ServerPlayer serverPlayer)) {
         return;
      }
      if (!EchoRuntimeModules.isLoaded("echologisticsnetwork")) {
         serverPlayer.sendSystemMessage(Component.literal("Logistics request blocked: ECHO Logistics Network is not installed."));
         return;
      }
      String loadoutId = LOGISTICS_LOADOUTS.get(recipeId);
      if (loadoutId == null) {
         serverPlayer.sendSystemMessage(Component.literal("Logistics request blocked: no factory loadout for " + recipeId + "."));
         return;
      }
      try {
         Object result = Class.forName("com.knoxhack.echologisticsnetwork.integration.LogisticsFactoryBridge")
            .getMethod("requestFactoryLoadout", ServerPlayer.class, BlockPos.class, String.class)
            .invoke(null, serverPlayer, getBlockPos(), loadoutId);
         boolean dispatched = Boolean.TRUE.equals(result.getClass().getMethod("dispatched").invoke(result));
         String message = String.valueOf(result.getClass().getMethod("message").invoke(result));
         this.lastLogisticsRestockStatus = message;
         setChanged();
         serverPlayer.sendSystemMessage(Component.literal((dispatched ? "Logistics request accepted: " : "Logistics request blocked: ") + message));
      } catch (InvocationTargetException exception) {
         Throwable cause = exception.getCause() == null ? exception : exception.getCause();
         serverPlayer.sendSystemMessage(Component.literal("Logistics request blocked: " + cause.getMessage()));
      } catch (ReflectiveOperationException | LinkageError exception) {
         serverPlayer.sendSystemMessage(Component.literal("Logistics request blocked: Logistics bridge is unavailable."));
      }
   }

   public void requestLogisticsAutoRestock(Player player, Identifier recipeId) {
      if (!(player instanceof ServerPlayer serverPlayer)) {
         return;
      }
      if (!EchoRuntimeModules.isLoaded("echologisticsnetwork")) {
         lastLogisticsRestockStatus = "ECHO Logistics Network is not installed.";
         serverPlayer.sendSystemMessage(Component.literal("Factory auto-restock blocked: " + lastLogisticsRestockStatus));
         setChanged();
         return;
      }
      String loadoutId = logisticsLoadoutIdForRecipe(recipeId);
      if (loadoutId.isBlank()) {
         lastLogisticsRestockStatus = "No factory loadout is mapped for this controller.";
         serverPlayer.sendSystemMessage(Component.literal("Factory auto-restock blocked: " + lastLogisticsRestockStatus));
         setChanged();
         return;
      }
      try {
         Object result = Class.forName("com.knoxhack.echologisticsnetwork.integration.LogisticsFactoryBridge")
            .getMethod("requestFactoryAutoRestock", ServerPlayer.class, BlockPos.class, String.class)
            .invoke(null, serverPlayer, getBlockPos(), loadoutId);
         boolean dispatched = Boolean.TRUE.equals(result.getClass().getMethod("dispatched").invoke(result));
         String message = String.valueOf(result.getClass().getMethod("message").invoke(result));
         lastLogisticsRestockStatus = message;
         setChanged();
         serverPlayer.sendSystemMessage(Component.literal((dispatched ? "Factory auto-restock dispatched: " : "Factory auto-restock status: ") + message));
         if (dispatched) {
            IndustrialProgress.markLogisticsAutoRestock(serverPlayer, loadoutId);
         }
      } catch (InvocationTargetException exception) {
         Throwable cause = exception.getCause() == null ? exception : exception.getCause();
         lastLogisticsRestockStatus = cause.getMessage();
         setChanged();
         serverPlayer.sendSystemMessage(Component.literal("Factory auto-restock blocked: " + cause.getMessage()));
      } catch (ReflectiveOperationException | LinkageError exception) {
         lastLogisticsRestockStatus = "Logistics bridge is unavailable.";
         setChanged();
         serverPlayer.sendSystemMessage(Component.literal("Factory auto-restock blocked: Logistics bridge is unavailable."));
      }
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.logisticsAutoRestockEnabled = input.getBooleanOr("logistics_auto_restock", false);
      this.logisticsRestockTargetRuns = clampRestockTarget(input.getIntOr("logistics_restock_target_runs", 3));
      this.lastLogisticsRestockStatus = input.getStringOr("last_logistics_restock_status",
         logisticsAutoRestockEnabled ? "Auto-restock enabled." : "Auto-restock disabled.");
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      output.putBoolean("logistics_auto_restock", logisticsAutoRestockEnabled);
      output.putInt("logistics_restock_target_runs", logisticsRestockTargetRuns());
      output.putString("last_logistics_restock_status", logisticsRestockStatusLine());
   }

   private static int clampRestockTarget(int targetRuns) {
      if (targetRuns <= 1) {
         return 1;
      }
      return targetRuns <= 3 ? 3 : 5;
   }
}
