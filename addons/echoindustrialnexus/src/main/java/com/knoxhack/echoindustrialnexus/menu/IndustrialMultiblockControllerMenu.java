package com.knoxhack.echoindustrialnexus.menu;

import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntimeSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockTaskState;
import com.knoxhack.echomultiblockcore.api.MultiblockStatusSnapshot;
import com.knoxhack.echomultiblockcore.api.TaskExecutionSnapshot;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMultiblockControllerBlockEntity;
import com.knoxhack.echoindustrialnexus.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.knoxhack.echocore.api.EchoRuntimeModules;

public class IndustrialMultiblockControllerMenu extends AbstractContainerMenu {
   public static final int GUI_WIDTH = 376;
   public static final int GUI_HEIGHT = 248;
   public static final int BUTTON_FORM_OR_REVALIDATE = 0;
   public static final int BUTTON_CLEAR_QUEUE = 1;
   public static final int BUTTON_RETRY_BLOCKED = 2;
   public static final int BUTTON_TOGGLE_LOGISTICS_RESTOCK = 3;
   public static final int BUTTON_CYCLE_LOGISTICS_RESTOCK_TARGET = 4;
   public static final int BUTTON_REQUEST_LOGISTICS_RESTOCK_NOW = 5;
   public static final int BUTTON_QUEUE_TASK_BASE = 10;
   public static final int BUTTON_QUEUE_TASK_X3_BASE = 40;
   public static final int BUTTON_QUEUE_TASK_X5_BASE = 70;
   public static final int BUTTON_REQUEST_LOGISTICS_BASE = 100;

   public static final int DATA_STATE = 0;
   public static final int DATA_INTEGRITY = 1;
   public static final int DATA_COMPLETION = 2;
   public static final int DATA_ROBOTS = 3;
   public static final int DATA_TASK_COUNT = 4;
   public static final int DATA_WARNING_COUNT = 5;
   public static final int DATA_ACTIVE_PROGRESS = 6;
   public static final int DATA_ACTIVE_DURATION = 7;
   public static final int DATA_QUEUE_CAPACITY = 8;
   public static final int DATA_ACTIVE_TASK_INDEX = 9;
   public static final int DATA_BLOCKED_TASK_INDEX = 10;
   public static final int DATA_FORMED = 11;
   public static final int DATA_LOGISTICS_AVAILABLE = 12;
   public static final int DATA_LOGISTICS_RESTOCK_ENABLED = 13;
   public static final int DATA_LOGISTICS_RESTOCK_TARGET = 14;
   public static final int DATA_COUNT = 15;

   private final IndustrialMultiblockControllerBlockEntity controller;
   private final ContainerData data;
   private final BlockPos controllerPos;
   private final String titleLine;
   private final String statusLine;
   private final String activeTaskLine;
   private final String blockedReason;
   private final String robotLine;
   private final String workcellLine;
   private final String warningLine;
   private final String logisticsLine;
   private final List<Identifier> recipeIds;

   public IndustrialMultiblockControllerMenu(int containerId, Inventory inventory,
                                             IndustrialMultiblockControllerBlockEntity controller) {
      this(containerId, inventory, controller, dataFor(controller), stateFor(controller));
   }

   private IndustrialMultiblockControllerMenu(int containerId, Inventory inventory,
                                              IndustrialMultiblockControllerBlockEntity controller,
                                              ContainerData data,
                                              InitialState initialState) {
      super(ModMenus.INDUSTRIAL_MULTIBLOCK_CONTROLLER.get(), containerId);
      this.controller = controller;
      this.data = data;
      this.controllerPos = initialState.pos();
      this.titleLine = initialState.titleLine();
      this.statusLine = initialState.statusLine();
      this.activeTaskLine = initialState.activeTaskLine();
      this.blockedReason = initialState.blockedReason();
      this.robotLine = initialState.robotLine();
      this.workcellLine = initialState.workcellLine();
      this.warningLine = initialState.warningLine();
      this.logisticsLine = initialState.logisticsLine();
      this.recipeIds = List.copyOf(initialState.recipeIds());
      checkContainerDataCount(data, DATA_COUNT);
      this.addDataSlots(data);
   }

   public static IndustrialMultiblockControllerMenu fromNetwork(int containerId, Inventory inventory,
                                                               RegistryFriendlyByteBuf buffer) {
      InitialState initialState = readState(buffer);
      BlockEntity blockEntity = inventory.player.level().getBlockEntity(initialState.pos());
      IndustrialMultiblockControllerBlockEntity controller = blockEntity instanceof IndustrialMultiblockControllerBlockEntity industrial
         ? industrial
         : null;
      return new IndustrialMultiblockControllerMenu(containerId, inventory, controller,
         new SimpleContainerData(DATA_COUNT), initialState);
   }

   public static void writeClientState(RegistryFriendlyByteBuf buffer, IndustrialMultiblockControllerBlockEntity controller) {
      InitialState state = stateFor(controller);
      buffer.writeBlockPos(state.pos());
      writeString(buffer, state.titleLine());
      writeString(buffer, state.statusLine());
      writeString(buffer, state.activeTaskLine());
      writeString(buffer, state.blockedReason());
      writeString(buffer, state.robotLine());
      writeString(buffer, state.workcellLine());
      writeString(buffer, state.warningLine());
      writeString(buffer, state.logisticsLine());
      buffer.writeVarInt(state.recipeIds().size());
      for (Identifier recipeId : state.recipeIds()) {
         writeString(buffer, recipeId.toString());
      }
   }

   @Override
   public ItemStack quickMoveStack(Player player, int slotIndex) {
      return ItemStack.EMPTY;
   }

   @Override
   public boolean stillValid(Player player) {
      if (controller == null || controller.getLevel() == null) {
         return true;
      }
      return controller.getLevel() == player.level()
         && player.distanceToSqr(controller.getBlockPos().getX() + 0.5D,
         controller.getBlockPos().getY() + 0.5D,
         controller.getBlockPos().getZ() + 0.5D) <= 64.0D;
   }

   @Override
   public boolean clickMenuButton(Player player, int id) {
      return controller != null && controller.handleMenuButton(player, id);
   }

   public BlockPos controllerPos() {
      return controllerPos;
   }

   public String titleLine() {
      return titleLine;
   }

   public String statusLine() {
      return statusLine;
   }

   public String activeTaskLine() {
      return activeTaskLine;
   }

   public String blockedReason() {
      return blockedReason;
   }

   public String robotLine() {
      return robotLine;
   }

   public String workcellLine() {
      return workcellLine;
   }

   public String warningLine() {
      return warningLine;
   }

   public String logisticsLine() {
      return logisticsLine;
   }

   public int recipeCount() {
      return recipeIds.size();
   }

   public Identifier recipeId(int index) {
      return index >= 0 && index < recipeIds.size() ? recipeIds.get(index) : EchoIndustrialNexus.id("missing_task");
   }

   public List<Identifier> recipeIds() {
      return recipeIds;
   }

   public int stateId() {
      return data.get(DATA_STATE);
   }

   public int integrity() {
      return data.get(DATA_INTEGRITY);
   }

   public int completionPermille() {
      return data.get(DATA_COMPLETION);
   }

   public int robotCount() {
      return data.get(DATA_ROBOTS);
   }

   public int taskCount() {
      return data.get(DATA_TASK_COUNT);
   }

   public int warningCount() {
      return data.get(DATA_WARNING_COUNT);
   }

   public int activeProgress() {
      return data.get(DATA_ACTIVE_PROGRESS);
   }

   public int activeDuration() {
      return Math.max(1, data.get(DATA_ACTIVE_DURATION));
   }

   public int queueCapacity() {
      return Math.max(1, data.get(DATA_QUEUE_CAPACITY));
   }

   public int activeTaskIndex() {
      return data.get(DATA_ACTIVE_TASK_INDEX);
   }

   public int blockedTaskIndex() {
      return data.get(DATA_BLOCKED_TASK_INDEX);
   }

   public boolean formed() {
      return data.get(DATA_FORMED) != 0;
   }

   public boolean logisticsAvailable() {
      return data.get(DATA_LOGISTICS_AVAILABLE) != 0;
   }

   public boolean logisticsRestockEnabled() {
      return data.get(DATA_LOGISTICS_RESTOCK_ENABLED) != 0;
   }

   public int logisticsRestockTargetRuns() {
      return data.get(DATA_LOGISTICS_RESTOCK_TARGET);
   }

   private static ContainerData dataFor(IndustrialMultiblockControllerBlockEntity controller) {
      return new ContainerData() {
         @Override
         public int get(int index) {
            TaskExecutionSnapshot focus = focusTask(controller);
            return switch (index) {
               case DATA_STATE -> controller.getState().ordinal();
               case DATA_INTEGRITY -> Math.round(controller.getIntegrity());
               case DATA_COMPLETION -> (int)Math.round(controller.validationCompletion() * 1000.0D);
               case DATA_ROBOTS -> controller.runtimeSnapshot().roboticComponentCount();
               case DATA_TASK_COUNT -> controller.taskQueueSize();
               case DATA_WARNING_COUNT -> controller.runtimeSnapshot().warningCount();
               case DATA_ACTIVE_PROGRESS -> focus == null ? 0 : focus.progressTicks();
               case DATA_ACTIVE_DURATION -> focus == null ? 1 : focus.durationTicks();
               case DATA_QUEUE_CAPACITY -> controller.taskQueueCapacity();
               case DATA_ACTIVE_TASK_INDEX -> recipeIndex(controller, focus == null ? null : focus.taskId());
               case DATA_BLOCKED_TASK_INDEX -> recipeIndex(controller, blockedTask(controller));
               case DATA_FORMED -> controller.isFormedForOperations() ? 1 : 0;
               case DATA_LOGISTICS_AVAILABLE -> EchoRuntimeModules.isLoaded("echologisticsnetwork") ? 1 : 0;
               case DATA_LOGISTICS_RESTOCK_ENABLED -> controller.logisticsAutoRestockEnabled() ? 1 : 0;
               case DATA_LOGISTICS_RESTOCK_TARGET -> controller.logisticsRestockTargetRuns();
               default -> 0;
            };
         }

         @Override
         public void set(int index, int value) {
         }

         @Override
         public int getCount() {
            return DATA_COUNT;
         }
      };
   }

   private static InitialState stateFor(IndustrialMultiblockControllerBlockEntity controller) {
      MultiblockStatusSnapshot status = controller.statusSnapshot();
      MultiblockRuntimeSnapshot runtime = controller.runtimeSnapshot();
      TaskExecutionSnapshot focus = focusTask(controller);
      String active = focus == null
         ? "Task Queue: Idle"
         : focus.displayName() + " / " + focus.state().name() + " / " + focus.progressTicks() + "/" + focus.durationTicks();
      String blocked = focus == null || focus.blockedReason().isBlank()
         ? controller.blockedReasonForDisplay()
         : focus.blockedReason();
      List<Identifier> recipes = controller.availableAutomationRecipes().stream()
         .map(MultiblockAutomationRecipe::id)
         .toList();
      return new InitialState(
         controller.getBlockPos(),
         status.name(),
         status.state().name() + " | Integrity " + Math.round(status.integrity()) + "% | Completion "
            + Math.round(status.completion() * 100.0D) + "%",
         active,
         blocked == null || blocked.isBlank() ? "No blocked task." : blocked,
         "Robotic Arms: " + runtime.roboticComponentCount() + " online",
         workcellLine(focus),
         runtime.warnings().isEmpty() ? "Warnings: none" : "Warning: " + runtime.warnings().get(0),
         "Logistics Restock: " + (controller.logisticsAutoRestockEnabled() ? "ON" : "OFF")
            + " / target x" + controller.logisticsRestockTargetRuns()
            + " / " + controller.logisticsRestockStatusLine(),
         recipes);
   }

   private static InitialState readState(RegistryFriendlyByteBuf buffer) {
      BlockPos pos = buffer.readBlockPos();
      String title = readString(buffer);
      String status = readString(buffer);
      String active = readString(buffer);
      String blocked = readString(buffer);
      String robot = readString(buffer);
      String workcell = readString(buffer);
      String warning = readString(buffer);
      String logistics = readString(buffer);
      int recipeCount = Math.max(0, Math.min(16, buffer.readVarInt()));
      List<Identifier> recipes = new ArrayList<>();
      for (int i = 0; i < recipeCount; i++) {
         Identifier id = Identifier.tryParse(readString(buffer));
         if (id != null) {
            recipes.add(id);
         }
      }
      return new InitialState(pos, title, status, active, blocked, robot, workcell, warning, logistics, recipes);
   }

   private static TaskExecutionSnapshot focusTask(IndustrialMultiblockControllerBlockEntity controller) {
      if (controller == null) {
         return null;
      }
      List<TaskExecutionSnapshot> snapshots = controller.taskSnapshots();
      return snapshots.stream()
         .filter(snapshot -> snapshot.state() == MultiblockTaskState.ACTIVE)
         .findFirst()
         .orElseGet(() -> snapshots.stream()
            .filter(snapshot -> snapshot.state() == MultiblockTaskState.BLOCKED)
            .findFirst()
            .orElseGet(() -> snapshots.stream().findFirst().orElse(null)));
   }

   private static Identifier blockedTask(IndustrialMultiblockControllerBlockEntity controller) {
      if (controller == null) {
         return null;
      }
      return controller.taskSnapshots().stream()
         .filter(snapshot -> snapshot.state() == MultiblockTaskState.BLOCKED)
         .map(TaskExecutionSnapshot::taskId)
         .findFirst()
         .orElse(null);
   }

   private static int recipeIndex(IndustrialMultiblockControllerBlockEntity controller, Identifier recipeId) {
      if (controller == null || recipeId == null) {
         return -1;
      }
      List<Identifier> recipes = controller.availableAutomationRecipes().stream()
         .map(MultiblockAutomationRecipe::id)
         .toList();
      return recipes.indexOf(recipeId);
   }

   private static String workcellLine(TaskExecutionSnapshot focus) {
      if (focus == null) {
         return "Workcells: awaiting task";
      }
      String workcell = focus.workcellId() == null ? focus.recipeCategory() : focus.workcellId().getPath();
      return "Workcell: " + workcell + " | Tool/Robot: " + (focus.robotId() == null ? "unassigned" : focus.robotId());
   }

   private static void writeString(RegistryFriendlyByteBuf buffer, String value) {
      buffer.writeUtf(trim(value, 256), 256);
   }

   private static String readString(RegistryFriendlyByteBuf buffer) {
      return buffer.readUtf(256);
   }

   private static String trim(String value, int maxLength) {
      String normalized = value == null ? "" : value;
      return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
   }

   private record InitialState(BlockPos pos, String titleLine, String statusLine, String activeTaskLine,
                               String blockedReason, String robotLine, String workcellLine, String warningLine,
                               String logisticsLine,
                               List<Identifier> recipeIds) {
      private InitialState {
         pos = pos == null ? BlockPos.ZERO : pos.immutable();
         titleLine = titleLine == null || titleLine.isBlank() ? "Industrial Multiblock" : titleLine.strip();
         statusLine = statusLine == null || statusLine.isBlank() ? "Status unavailable" : statusLine.strip();
         activeTaskLine = activeTaskLine == null || activeTaskLine.isBlank() ? "Task Queue: Idle" : activeTaskLine.strip();
         blockedReason = blockedReason == null || blockedReason.isBlank() ? "No blocked task." : blockedReason.strip();
         robotLine = robotLine == null || robotLine.isBlank() ? "Robotic Arms: unknown" : robotLine.strip();
         workcellLine = workcellLine == null || workcellLine.isBlank() ? "Workcells: unknown" : workcellLine.strip();
         warningLine = warningLine == null || warningLine.isBlank() ? "Warnings: none" : warningLine.strip();
         logisticsLine = logisticsLine == null || logisticsLine.isBlank() ? "Logistics Restock: unavailable" : logisticsLine.strip();
         recipeIds = List.copyOf(recipeIds == null ? List.of() : recipeIds);
      }
   }
}
