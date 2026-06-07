package com.knoxhack.echomultiblockcore.block.entity;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.EchoMultiblockController;
import com.knoxhack.echomultiblockcore.api.AutomationEffectHandlers;
import com.knoxhack.echomultiblockcore.api.AutomationEffectInvocation;
import com.knoxhack.echomultiblockcore.api.AutomationEffectResult;
import com.knoxhack.echomultiblockcore.api.AutomationExecutionPlan;
import com.knoxhack.echomultiblockcore.api.AutomationRecipeRegistry;
import com.knoxhack.echomultiblockcore.api.AutomationTaskContext;
import com.knoxhack.echomultiblockcore.api.AutomationTaskHandler;
import com.knoxhack.echomultiblockcore.api.AutomationTaskHandlers;
import com.knoxhack.echomultiblockcore.api.AutoBuilderPlan;
import com.knoxhack.echomultiblockcore.api.AutoBuilderResult;
import com.knoxhack.echomultiblockcore.api.CapabilityRequirement;
import com.knoxhack.echomultiblockcore.api.InstalledMultiblockUpgrade;
import com.knoxhack.echomultiblockcore.api.MultiblockCapability;
import com.knoxhack.echomultiblockcore.api.MultiblockCapabilityRuntime;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echomultiblockcore.api.MultiblockDefinition;
import com.knoxhack.echomultiblockcore.api.MultiblockIntegrationServices;
import com.knoxhack.echomultiblockcore.api.MultiblockProgressionDefinition;
import com.knoxhack.echomultiblockcore.api.MultiblockProgressionRegistry;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntime;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntimeSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockState;
import com.knoxhack.echomultiblockcore.api.MultiblockStatusSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockTaskState;
import com.knoxhack.echomultiblockcore.api.MultiblockUpgradeRegistry;
import com.knoxhack.echomultiblockcore.api.LensMultiblockScan;
import com.knoxhack.echomultiblockcore.api.RobotToolType;
import com.knoxhack.echomultiblockcore.api.RobotAnimationState;
import com.knoxhack.echomultiblockcore.api.RobotPoseSnapshot;
import com.knoxhack.echomultiblockcore.api.TaskExecutionSnapshot;
import com.knoxhack.echomultiblockcore.api.UpgradeModifier;
import com.knoxhack.echomultiblockcore.api.ValidationOptions;
import com.knoxhack.echomultiblockcore.api.ValidationResult;
import com.knoxhack.echomultiblockcore.api.WorkcellType;
import com.knoxhack.echomultiblockcore.block.MultiblockControllerBlock;
import com.knoxhack.echomultiblockcore.block.MultiblockCrateBlock;
import com.knoxhack.echomultiblockcore.content.MultiblockContent;
import com.knoxhack.echomultiblockcore.event.MultiblockActivatedEvent;
import com.knoxhack.echomultiblockcore.event.MultiblockBrokenEvent;
import com.knoxhack.echomultiblockcore.event.MultiblockDamagedEvent;
import com.knoxhack.echomultiblockcore.event.MultiblockFormedEvent;
import com.knoxhack.echomultiblockcore.event.RoboticTaskBlockedEvent;
import com.knoxhack.echomultiblockcore.event.RoboticTaskCompletedEvent;
import com.knoxhack.echomultiblockcore.event.RoboticTaskFailedEvent;
import com.knoxhack.echomultiblockcore.event.RoboticTaskStartedEvent;
import com.knoxhack.echomultiblockcore.integration.runtimeguard.MultiblockRuntimeGuardHooks;
import com.knoxhack.echomultiblockcore.integration.MultiblockMissionHooks;
import com.knoxhack.echomultiblockcore.menu.MultiblockControllerMenu;
import com.knoxhack.echomultiblockcore.network.RobotAnimationSync;
import com.knoxhack.echomultiblockcore.registry.ModBlockEntities;
import com.knoxhack.echomultiblockcore.registry.ModBlocks;
import com.knoxhack.echomultiblockcore.runtime.MultiblockDiagnostics;
import com.knoxhack.echomultiblockcore.runtime.AutoBuilderService;
import com.knoxhack.echomultiblockcore.runtime.MultiblockCapabilityService;
import com.knoxhack.echomultiblockcore.runtime.MultiblockIntegrityService;
import com.knoxhack.echomultiblockcore.runtime.MultiblockRuntimeBuilder;
import com.knoxhack.echomultiblockcore.runtime.MultiblockRuntimeManager;
import com.knoxhack.echomultiblockcore.runtime.ValidationCache;
import com.knoxhack.echomultiblockcore.task.MultiblockTaskQueue;
import com.knoxhack.echomultiblockcore.task.QueuedMultiblockTask;
import com.knoxhack.echomultiblockcore.task.AutomationTransaction;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class MultiblockControllerBlockEntity extends BlockEntity implements EchoMultiblockController, MenuProvider {
    private static final int RUNTIME_SCHEMA_VERSION = 4;
    private static final int REVALIDATE_INTERVAL = 100;
    private static final int AUTOMATED_QUEUE_RETRY_INTERVAL = 40;

    private Identifier multiblockId;
    private MultiblockState state = MultiblockState.UNBUILT;
    private float integrity = 0.0F;
    private MultiblockRuntime runtime;
    private long structureVersion;
    private long lastQueueRetryTick = -1L;
    private MultiblockCapabilityRuntime capabilityRuntime = MultiblockCapabilityRuntime.EMPTY;
    private final List<InstalledMultiblockUpgrade> installedUpgrades = new ArrayList<>();
    private final List<String> damageGroups = new ArrayList<>();
    private final List<RobotAnimationState> robotAnimations = new ArrayList<>();
    private String constructionProgress = "";
    private boolean validationQueued;

    private final ValidationCache validationCache = new ValidationCache();
    private final MultiblockTaskQueue taskQueue = new MultiblockTaskQueue();

    public MultiblockControllerBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CONTROLLER.get(), pos, blockState);
    }

    protected MultiblockControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        multiblockId = blockState.getBlock() instanceof MultiblockControllerBlock controller
                ? controller.defaultDefinitionId()
                : EchoMultiblockCore.id("industrial_assembly_line");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MultiblockControllerBlockEntity controller) {
        if (level.isClientSide()) {
            return;
        }
        controller.serverTick((ServerLevel) level);
    }

    @Override
    public Identifier getMultiblockId() {
        return multiblockId;
    }

    @Override
    public MultiblockState getState() {
        return state;
    }

    @Override
    public ValidationResult validateStructure() {
        return validateStructure(false);
    }

    public ValidationResult validateStructure(boolean force) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return ValidationResult.error(multiblockId, worldPosition, "Controller is not attached to a server level.");
        }
        Optional<MultiblockDefinition> definition = definition();
        if (definition.isEmpty()) {
            return ValidationResult.error(multiblockId, worldPosition, "Unknown multiblock definition " + multiblockId + ".");
        }
        return validationCache.validate(serverLevel, worldPosition, definition.get(), structureVersion,
                force ? ValidationOptions.FORCED : ValidationOptions.DEFAULT);
    }

    @Override
    public void onStructureFormed() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ValidationResult result = validateStructure(true);
        Optional<MultiblockDefinition> definition = definition();
        if (!result.valid() || definition.isEmpty()) {
            return;
        }
        MultiblockDefinition def = definition.get();
        integrity = integrity <= 0.0F ? def.integrityRules().max() : Math.min(integrity, def.integrityRules().max());
        state = MultiblockIntegrityService.stateFor(def, integrity, true);
        rebuildRuntime(def, result);
        MultiblockRuntimeManager.recordFormed(serverLevel, multiblockId, worldPosition, integrity, state);
        EchoBackendLifecycleBridge.postGameEvent(new MultiblockFormedEvent(serverLevel, multiblockId, worldPosition, runtime, runtimeSnapshot()));
        EchoBackendLifecycleBridge.postGameEvent(new MultiblockActivatedEvent(serverLevel, multiblockId, worldPosition));
        syncBlock();
    }

    @Override
    public void onStructureBroken() {
        MultiblockRuntimeSnapshot before = runtimeSnapshot();
        if (level instanceof ServerLevel serverLevel) {
            MultiblockRuntimeManager.remove(serverLevel, worldPosition);
            EchoBackendLifecycleBridge.postGameEvent(new MultiblockBrokenEvent(serverLevel, multiblockId, worldPosition, before));
        }
        releaseActiveRobot();
        runtime = null;
        capabilityRuntime = MultiblockCapabilityRuntime.EMPTY;
        robotAnimations.clear();
        constructionProgress = "";
        state = MultiblockState.INCOMPLETE;
        clearTaskInternal();
        validationCache.markDirty();
        syncBlock();
    }

    @Override
    public void tickFormedStructure() {
        if (level instanceof ServerLevel serverLevel) {
            tickQueue(serverLevel);
        }
    }

    @Override
    public float getIntegrity() {
        return integrity;
    }

    @Override
    public void setIntegrity(float value) {
        MultiblockRuntimeSnapshot before = runtimeSnapshot();
        integrity = Math.max(0.0F, Math.min(100.0F, value));
        updateStateFromIntegrity("generic", before);
        syncBlock();
    }

    @Override
    public List<MultiblockCapability> getCapabilities() {
        return definition().map(MultiblockDefinition::capabilities).orElse(List.of());
    }

    @Override
    public Optional<MultiblockRuntime> getRuntime() {
        return Optional.ofNullable(runtime);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.echomultiblockcore.controller");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MultiblockControllerMenu(containerId, inventory, this, menuData());
    }

    public ContainerData menuData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                ValidationResult result = validationCache.lastResult();
                return switch (index) {
                    case MultiblockControllerMenu.DATA_STATE -> state.ordinal();
                    case MultiblockControllerMenu.DATA_INTEGRITY -> Math.round(integrity);
                    case MultiblockControllerMenu.DATA_COMPLETION -> result == null ? 0 : (int) Math.round(result.completion() * 100.0D);
                    case MultiblockControllerMenu.DATA_ROBOTS -> runtime == null ? discoverRobots().size() : runtime.installedRoboticComponents().size();
                    case MultiblockControllerMenu.DATA_QUEUE -> taskQueue.size();
                    case MultiblockControllerMenu.DATA_BLOCKED -> taskQueue.hasBlockedTask() ? 1 : 0;
                    case MultiblockControllerMenu.DATA_CAPABILITY_OK -> capabilityRuntime.satisfied() ? 1 : 0;
                    case MultiblockControllerMenu.DATA_UPGRADES -> installedUpgrades.size();
                    case MultiblockControllerMenu.DATA_DAMAGE_GROUPS -> damageGroups.size();
                    case MultiblockControllerMenu.DATA_REPAIR_ACTIONS -> repairActions().size();
                    case MultiblockControllerMenu.DATA_PROGRESSION_TIER -> progression().map(MultiblockProgressionDefinition::tier).orElse(0);
                    case MultiblockControllerMenu.DATA_FEATURED_RECIPES -> progression().map(value -> value.featuredRecipes().size()).orElse(0);
                    case MultiblockControllerMenu.DATA_QUEUE_CAPACITY -> taskQueue.capacity();
                    case MultiblockControllerMenu.DATA_BLOCKED_TASKS -> (int) taskQueue.tasks().stream()
                            .filter(task -> task.state() == MultiblockTaskState.BLOCKED)
                            .count();
                    case MultiblockControllerMenu.DATA_ACTIVE_TASKS -> (int) taskQueue.tasks().stream()
                            .filter(task -> task.state() == MultiblockTaskState.ACTIVE)
                            .count();
                    case MultiblockControllerMenu.DATA_CAPABILITY_DIAGNOSTICS -> capabilityRuntime.diagnostics().size();
                    case MultiblockControllerMenu.DATA_UPGRADE_SLOTS -> definition()
                            .map(value -> value.upgradeSlotRules().size())
                            .orElse(0);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return MultiblockControllerMenu.DATA_COUNT;
            }
        };
    }

    public boolean handleMenuButton(Player player, int id) {
        if (id >= MultiblockControllerMenu.BUTTON_RECIPE_BASE && id < MultiblockControllerMenu.BUTTON_RECIPE_LIMIT) {
            int index = id - MultiblockControllerMenu.BUTTON_RECIPE_BASE;
            List<MultiblockAutomationRecipe> recipes = AutomationRecipeRegistry.all();
            if (index >= 0 && index < recipes.size()) {
                queueRecipe(recipes.get(index).id(), player);
                return true;
            }
            tell(player, "TASK BLOCKED: Recipe selection is no longer available.");
            return true;
        }
        switch (id) {
            case MultiblockControllerMenu.BUTTON_VALIDATE -> {
                ValidationResult result = validateStructure(true);
                if (result.valid()) {
                    onStructureFormed();
                }
                sendDiagnostics(player);
                return true;
            }
            case MultiblockControllerMenu.BUTTON_START -> {
                availableAutomationRecipes().stream()
                        .sorted(java.util.Comparator
                                .comparingInt(MultiblockAutomationRecipe::repairPriority).reversed()
                                .thenComparing(recipe -> recipe.id().toString()))
                        .findFirst()
                        .ifPresent(recipe -> queueRecipe(recipe.id(), player));
                return true;
            }
            case MultiblockControllerMenu.BUTTON_CLEAR -> {
                clearQueue(player);
                return true;
            }
            case MultiblockControllerMenu.BUTTON_RETRY -> {
                retryBlocked(player);
                return true;
            }
            case MultiblockControllerMenu.BUTTON_PAUSE -> {
                pauseQueue(player);
                return true;
            }
            case MultiblockControllerMenu.BUTTON_RESUME -> {
                resumeQueue(player);
                return true;
            }
            case MultiblockControllerMenu.BUTTON_REPAIR -> {
                AutomationRecipeRegistry.all().stream()
                        .filter(recipe -> recipe.integrityRepair() > 0 && recipe.allowsMultiblock(multiblockId))
                        .sorted(java.util.Comparator
                                .comparingInt(MultiblockAutomationRecipe::repairPriority).reversed()
                                .thenComparing(recipe -> recipe.id().toString()))
                        .findFirst()
                        .ifPresent(recipe -> queueRecipe(recipe.id(), player));
                return true;
            }
            case MultiblockControllerMenu.BUTTON_AUTOBUILD -> {
                runAutoBuilder(player, 16);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public void openControllerUi(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(this, worldPosition);
        } else {
            sendDiagnostics(player);
        }
    }

    public Identifier definitionId() {
        return multiblockId;
    }

    public void handlePlayerUse(Player player, boolean diagnosticsOnly) {
        if (player == null) {
            return;
        }
        if (diagnosticsOnly) {
            openControllerUi(player);
            return;
        }
        if (isFormed() && multiblockId.equals(EchoMultiblockCore.id("industrial_assembly_line"))) {
            queueTask(EchoMultiblockCore.id("assemble_reinforced_machine_frame"), player);
            return;
        }
        ValidationResult result = validateStructure(true);
        if (result.valid()) {
            onStructureFormed();
            MultiblockMissionHooks.recordStructureValidated(player, multiblockId);
            sendLines(player, List.of(
                    "STRUCTURE LINK ESTABLISHED",
                    "Frame Integrity: " + Math.round(integrity <= 0.0F ? 100.0F : integrity) + "%",
                    "Robotics: " + discoverRobots().size() + " arm(s) online",
                    "Task Queue: " + taskQueue.statusLine()));
        } else {
            state = result.completion() <= 0.0D ? MultiblockState.UNBUILT : MultiblockState.INCOMPLETE;
            runtime = null;
            sendDiagnostics(player);
            syncBlock();
        }
    }

    public void inspectWithBlueprint(Player player, Identifier definitionId) {
        if (player == null) {
            return;
        }
        Identifier previous = multiblockId;
        if (definitionId != null && !definitionId.equals(multiblockId)) {
            multiblockId = definitionId;
            structureVersion++;
            validationCache.markDirty();
        }
        sendDiagnostics(player);
        if (!previous.equals(multiblockId)) {
            multiblockId = previous;
            structureVersion++;
            validationCache.markDirty();
        }
    }

    public boolean setDefinitionId(Identifier definitionId, Player player) {
        if (definitionId == null || MultiblockContent.definition(definitionId).isEmpty()) {
            tell(player, "Unknown multiblock definition: " + definitionId);
            return false;
        }
        multiblockId = definitionId;
        runtime = null;
        structureVersion++;
        validationCache.markDirty();
        tell(player, "Controller definition set to " + definitionId + ".");
        syncBlock();
        return true;
    }

    public void markValidationDirty() {
        structureVersion++;
        validationCache.markDirty();
        MultiblockRuntimeGuardHooks.markDirty(level, worldPosition, "DEBUG");
        if (isFormed()) {
            state = MultiblockState.VALIDATING;
        }
        setChanged();
    }

    public boolean queueTask(Identifier taskId, Player player) {
        return queueTasks(taskId, player, 1) > 0;
    }

    public int queueTasks(Identifier taskId, Player player, int quantity) {
        Optional<MultiblockAutomationRecipe> recipe = AutomationRecipeRegistry.byId(taskId);
        if (recipe.isEmpty()) {
            tell(player, "Unknown multiblock automation recipe: " + taskId);
            return 0;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            tell(player, "TASK BLOCKED: Controller is not loaded on the server.");
            return 0;
        }
        if (!isFormed()) {
            tell(player, "TASK BLOCKED: Structure is not formed.");
            return 0;
        }
        if (!recipe.get().allowsMultiblock(multiblockId)) {
            tell(player, "TASK BLOCKED: Recipe is not allowed for " + multiblockId + ".");
            return 0;
        }
        taskQueue.pruneCompleted();
        int requested = Math.max(1, Math.min(quantity, taskQueue.capacity()));
        int accepted = Math.min(requested, taskQueue.remainingCapacity());
        if (accepted <= 0) {
            tell(player, "TASK BLOCKED: Task queue is full.");
            return 0;
        }
        QueuedMultiblockTask firstQueued = null;
        for (int i = 0; i < accepted; i++) {
            QueuedMultiblockTask queued = taskQueue.enqueue(recipe.get().id(), serverLevel.getGameTime()).orElse(null);
            if (queued == null) {
                break;
            }
            if (firstQueued == null) {
                firstQueued = queued;
            }
        }
        if (firstQueued == null) {
            tell(player, "TASK BLOCKED: Task queue is full.");
            return 0;
        }
        if (taskQueue.hasActiveTask()) {
            tell(player, accepted + " task(s) queued: " + recipe.get().displayName() + ".");
            syncBlock();
            return accepted;
        }
        boolean started = tryStartQueuedTask(serverLevel, firstQueued, player);
        if (!started && accepted > 1) {
            tell(player, (accepted - 1) + " additional task(s) waiting behind the blocked task.");
        } else if (started && accepted > 1) {
            tell(player, (accepted - 1) + " additional task(s) queued.");
        }
        return accepted;
    }

    public boolean queueRecipe(Identifier recipeId, Player player) {
        return queueTask(recipeId, player);
    }

    public int queueRecipes(Identifier recipeId, Player player, int quantity) {
        return queueTasks(recipeId, player, quantity);
    }

    public void clearTask(Player player) {
        clearQueue(player);
    }

    public void clearQueue(Player player) {
        clearTaskInternal();
        tell(player, "Task queue cleared.");
        syncBlock();
    }

    public void pauseQueue(Player player) {
        releaseActiveRobot();
        taskQueue.pauseAll("Paused by operator.");
        tell(player, "Task queue paused.");
        syncBlock();
    }

    public void resumeQueue(Player player) {
        taskQueue.resumePaused();
        startNextRunnableNow(player);
        tell(player, "Task queue resumed.");
        syncBlock();
    }

    public void retryBlocked(Player player) {
        taskQueue.retryBlocked();
        startNextRunnableNow(player);
        tell(player, "Blocked tasks reset for retry.");
        syncBlock();
    }

    public boolean installUpgrade(Identifier upgradeId, Player player) {
        if (upgradeId == null) {
            tell(player, "UPGRADE BLOCKED: Missing upgrade id.");
            return false;
        }
        var definition = MultiblockUpgradeRegistry.byId(upgradeId);
        if (definition.isEmpty()) {
            tell(player, "UPGRADE BLOCKED: Unknown upgrade " + upgradeId + ".");
            return false;
        }
        if (!definition.get().allows(multiblockId)) {
            tell(player, "UPGRADE BLOCKED: Upgrade is not compatible with " + multiblockId + ".");
            return false;
        }
        if (installedUpgrades.stream().anyMatch(upgrade -> upgrade.upgradeId().equals(upgradeId))) {
            tell(player, "UPGRADE BLOCKED: " + definition.get().displayName() + " is already installed.");
            return false;
        }
        Identifier slotId = firstAvailableUpgradeSlot(upgradeId).orElse(null);
        if (slotId == null) {
            tell(player, "UPGRADE BLOCKED: No compatible upgrade slot is free on " + multiblockId + ".");
            return false;
        }
        installedUpgrades.add(new InstalledMultiblockUpgrade(upgradeId, slotId, worldPosition, 1));
        constructionProgress = "Installed " + definition.get().displayName() + ".";
        validationCache.markDirty();
        tell(player, "UPGRADE INSTALLED: " + definition.get().displayName());
        syncBlock();
        return true;
    }

    public boolean removeLastUpgrade(Player player) {
        if (installedUpgrades.isEmpty()) {
            tell(player, "UPGRADE BLOCKED: No upgrades are installed.");
            return false;
        }
        InstalledMultiblockUpgrade removed = installedUpgrades.remove(installedUpgrades.size() - 1);
        constructionProgress = "Removed upgrade " + removed.upgradeId() + ".";
        validationCache.markDirty();
        tell(player, "UPGRADE REMOVED: " + removed.upgradeId());
        syncBlock();
        return true;
    }

    public AutoBuilderResult runAutoBuilder(Player player, int maxPlacements) {
        if (!(level instanceof ServerLevel serverLevel)) {
            AutoBuilderResult result = AutoBuilderResult.blocked("Controller is not loaded on the server.");
            tell(player, result.message());
            return result;
        }
        ValidationResult validation = validateStructure(true);
        MultiblockCapabilityRuntime builderCapabilities = MultiblockCapabilityService.evaluate(
                definition().orElse(null), null, validation.matchedBlocks(), level, installedUpgrades);
        boolean hasAutoBuilder = capabilityRuntime.nodes().stream()
                .anyMatch(node -> node.capabilityId().equals(MultiblockCapability.AUTO_BUILDER.id()));
        hasAutoBuilder = hasAutoBuilder || builderCapabilities.nodes().stream()
                .anyMatch(node -> node.capabilityId().equals(MultiblockCapability.AUTO_BUILDER.id()));
        boolean requireComponent;
        try {
            requireComponent = com.knoxhack.echomultiblockcore.Config.REQUIRE_AUTO_BUILDER_COMPONENT.get();
        } catch (RuntimeException exception) {
            requireComponent = true;
        }
        if (requireComponent && !hasAutoBuilder && (player == null || !player.isCreative())) {
            AutoBuilderResult result = AutoBuilderResult.blocked("Missing Auto Builder capability node.");
            tell(player, result.message());
            return result;
        }
        if (validation.valid()) {
            AutoBuilderResult result = AutoBuilderResult.blocked("Structure is already complete.");
            tell(player, result.message());
            return result;
        }
        int cappedPlacements;
        try {
            cappedPlacements = Math.min(maxPlacements, com.knoxhack.echomultiblockcore.Config.AUTO_BUILDER_MAX_PLACEMENTS.get());
        } catch (RuntimeException exception) {
            cappedPlacements = maxPlacements;
        }
        AutoBuilderPlan plan = AutoBuilderService.plan(validation, cappedPlacements);
        AutoBuilderResult result = AutoBuilderService.execute(serverLevel, plan,
                findCrate(MultiblockCrateBlock.CrateKind.INPUT).orElse(null), player, cappedPlacements);
        constructionProgress = result.message();
        if (result.success()) {
            structureVersion++;
            validationCache.markDirty();
            MultiblockMissionHooks.recordAutoBuilder(player, multiblockId.toString());
        }
        tell(player, result.message());
        syncBlock();
        return result;
    }

    public boolean isFormedForOperations() {
        return isFormed();
    }

    public double validationCompletion() {
        ValidationResult result = validationCache.lastResult();
        return result == null ? 0.0D : result.completion();
    }

    public int taskQueueSize() {
        return taskQueue.size();
    }

    public int taskQueueCapacity() {
        return taskQueue.capacity();
    }

    public int taskQueueRemainingCapacity() {
        return taskQueue.remainingCapacity();
    }

    public boolean hasBlockedTasks() {
        return taskQueue.hasBlockedTask();
    }

    public List<TaskExecutionSnapshot> taskSnapshots() {
        return taskQueue.snapshots();
    }

    public List<MultiblockAutomationRecipe> availableAutomationRecipes() {
        return AutomationRecipeRegistry.all().stream()
                .filter(recipe -> recipe.allowsMultiblock(multiblockId))
                .toList();
    }

    public String blockedReasonForDisplay() {
        return firstBlockedReason();
    }

    public MultiblockStatusSnapshot statusSnapshot() {
        ValidationResult result = validationCache.lastResult();
        Optional<MultiblockProgressionDefinition> progression = progression();
        MultiblockDefinition definition = definition().orElse(null);
        TaskExecutionSnapshot lastTask = lastTask(tasksOrQueue());
        return new MultiblockStatusSnapshot(
                multiblockId,
                definition == null ? multiblockId.toString() : definition.displayName(),
                state,
                integrity,
                result == null ? 0.0D : result.completion(),
                worldPosition,
                List.of(),
                discoverRobots().stream()
                        .map(arm -> arm.getRobotRuntimeId(worldPosition) + " " + arm.getRobotState())
                        .toList(),
                taskQueue.snapshots().stream()
                        .map(snapshot -> snapshot.taskId().getPath() + " " + snapshot.state() + " "
                                + snapshot.progressTicks() + "/" + snapshot.durationTicks())
                        .toList(),
                warnings(),
                progression.map(MultiblockProgressionDefinition::title).orElse(""),
                progression.map(this::progressionHint).orElse(""),
                progression.map(MultiblockProgressionDefinition::tier).orElse(0),
                definition == null ? "" : definition.facilityStage(),
                definition == null ? "" : definition.facilityRoute(),
                definition == null ? "" : definition.primaryUse(),
                blockedReasonCode(firstBlockedReason()),
                lastTask == null ? null : lastTask.taskId(),
                lastTask == null ? "" : lastTask.state().name(),
                integrationHints(definition, progression));
    }

    public MultiblockRuntimeSnapshot runtimeSnapshot() {
        ValidationResult result = validationCache.lastResult();
        MultiblockDefinition definition = definition().orElse(null);
        List<TaskExecutionSnapshot> tasks = taskQueue.snapshots();
        List<String> warnings = warnings();
        Optional<MultiblockProgressionDefinition> progression = progression();
        return new MultiblockRuntimeSnapshot(
                multiblockId,
                worldPosition,
                state,
                integrity,
                result == null ? 0.0D : result.completion(),
                runtime == null ? (result == null ? 0 : result.matchedBlocks().size()) : runtime.matchedBlocks().size(),
                runtime == null ? discoverRobots().size() : runtime.installedRoboticComponents().size(),
                tasks,
                warnings,
                result == null ? 0L : result.validationTime(),
                level == null ? net.minecraft.world.level.Level.OVERWORLD : level.dimension(),
                definition == null ? multiblockId.getPath() : definition.displayName(),
                definition == null ? "general" : definition.category(),
                definition == null ? com.knoxhack.echomultiblockcore.api.MultiblockRole.INFRASTRUCTURE : definition.role(),
                definition == null ? 0xFF00D8FF : definition.previewColor(),
                tasks.size(),
                warnings.size(),
                capabilityRuntime,
                installedUpgrades,
                damageGroups,
                repairActions(),
                robotAnimations,
                constructionProgress,
                progression.map(MultiblockProgressionDefinition::id).orElse(null),
                progression.map(MultiblockProgressionDefinition::tier).orElse(0),
                progression.map(MultiblockProgressionDefinition::title).orElse(""),
                progression.map(MultiblockProgressionDefinition::featuredRecipeSummary).orElse(""),
                progression.map(MultiblockProgressionDefinition::tier).orElse(0),
                definition == null ? "" : definition.facilityStage(),
                definition == null ? "" : definition.facilityRoute(),
                definition == null ? "" : definition.primaryUse(),
                blockedReasonCode(firstBlockedReason()),
                lastTask(tasks) == null ? null : lastTask(tasks).taskId(),
                lastTask(tasks) == null ? "" : lastTask(tasks).state().name(),
                integrationHints(definition, progression));
    }

    public LensMultiblockScan scanSnapshot() {
        ValidationResult result = validateStructure(false);
        MultiblockDefinition definition = definition().orElse(null);
        return new LensMultiblockScan(
                multiblockId,
                definition == null ? multiblockId.getPath() : definition.displayName(),
                state,
                result.completion(),
                worldPosition,
                result.groupedIssues().stream()
                        .map(issue -> issue.count() + "x " + issue.kind() + " " + issue.expected())
                        .toList(),
                discoverRobots().stream()
                        .map(arm -> arm.getRobotRuntimeId(worldPosition) + " " + arm.getRobotState()
                                + " heat=" + arm.getHeat() + "/" + arm.getMaxHeat())
                        .collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), list -> {
                            if (definition != null && !definition.primaryUse().isBlank()) {
                                list.add("Use: " + definition.primaryUse());
                            }
                            if (!capabilityRuntime.diagnostics().isEmpty()) {
                                list.add("Capability bottlenecks: " + capabilityRuntime.diagnostics().size());
                                capabilityRuntime.diagnostics().stream()
                                        .limit(2)
                                        .map(diagnostic -> diagnostic.capabilityId().getPath() + ": " + diagnostic.message())
                                        .forEach(list::add);
                            }
                            if (!installedUpgrades.isEmpty()) {
                                list.add("Upgrades installed: " + installedUpgrades.size());
                            }
                            String blocked = firstBlockedReason();
                            if (!blocked.isBlank()) {
                                list.add("Blocked: " + blockedReasonCode(blocked) + " / " + blocked);
                            }
                            progression().ifPresent(progression -> {
                                list.add("Progression tier " + progression.tier() + ": " + progression.title());
                                list.add(progressionHint(progression));
                            });
                            if (definition != null) {
                                definition.integrationHints().stream().limit(3).forEach(hint -> list.add("Hint: " + hint));
                            }
                            return List.copyOf(list);
                        })),
                taskQueue.snapshots().stream()
                        .map(snapshot -> snapshot.taskId().getPath() + " " + snapshot.state()
                                + " " + snapshot.progressTicks() + "/" + snapshot.durationTicks())
                        .toList());
    }

    private List<TaskExecutionSnapshot> tasksOrQueue() {
        return taskQueue.snapshots();
    }

    private static TaskExecutionSnapshot lastTask(List<TaskExecutionSnapshot> tasks) {
        return tasks == null || tasks.isEmpty() ? null : tasks.get(tasks.size() - 1);
    }

    private static List<String> integrationHints(MultiblockDefinition definition, Optional<MultiblockProgressionDefinition> progression) {
        List<String> hints = new ArrayList<>();
        if (definition != null) {
            hints.addAll(definition.integrationHints());
            if (!definition.facilityRoute().isBlank()) {
                hints.add("Route: " + definition.facilityRoute());
            }
            if (!definition.primaryUse().isBlank()) {
                hints.add("Use: " + definition.primaryUse());
            }
        }
        progression.ifPresent(value -> hints.add("Progression: T" + value.tier() + " " + value.title()));
        return List.copyOf(hints);
    }

    private static String blockedReasonCode(String reason) {
        if (reason == null || reason.isBlank()) {
            return "";
        }
        String lower = reason.toLowerCase(Locale.ROOT);
        if (lower.contains("power") || lower.contains("capability")) {
            return "capability";
        }
        if (lower.contains("input") || lower.contains("ingredient") || lower.contains("material")) {
            return "input";
        }
        if (lower.contains("output") || lower.contains("full")) {
            return "output";
        }
        if (lower.contains("robot") || lower.contains("tool") || lower.contains("arm")) {
            return "robotics";
        }
        if (lower.contains("upgrade")) {
            return "upgrade";
        }
        if (lower.contains("formed") || lower.contains("structure")) {
            return "structure";
        }
        return "runtime";
    }

    public List<String> diagnosticLines() {
        ValidationResult result = validateStructure(true);
        List<String> lines = new ArrayList<>(MultiblockDiagnostics.lines(
                result.valid() ? state.name() : "INCOMPLETE",
                definition().orElse(null),
                result,
                integrity,
                discoverRobots().size(),
                taskQueue.statusLine(),
                firstBlockedReason()));
        if (!capabilityRuntime.diagnostics().isEmpty()) {
            lines.add("Capabilities:");
            capabilityRuntime.diagnostics().forEach(diagnostic -> lines.add("- " + diagnostic.message()));
        }
        if (!installedUpgrades.isEmpty()) {
            lines.add("Upgrades: " + installedUpgrades.size() + " installed");
        }
        if (!damageGroups.isEmpty()) {
            lines.add("Damage: " + String.join(", ", damageGroups));
        }
        if (!constructionProgress.isBlank()) {
            lines.add("Construction: " + constructionProgress);
        }
        progression().ifPresent(progression -> {
            lines.add("Progression: Tier " + progression.tier() + " // " + progression.title());
            lines.add("Next: " + progressionHint(progression));
        });
        return lines;
    }

    private void serverTick(ServerLevel serverLevel) {
        if (!isFormed()) {
            return;
        }
        ValidationResult previous = validationCache.lastResult();
        if (previous == null || validationCache.isDirty()
                || serverLevel.getGameTime() - previous.validationTime() >= REVALIDATE_INTERVAL) {
            if (previous != null && requestRuntimeGuardValidation(serverLevel,
                    validationCache.isDirty() ? "BLOCK_CHANGED" : "SCHEDULED_IDLE")) {
                return;
            }
            applyValidationResult(serverLevel, validateStructure(false));
        }
        tickFormedStructure();
    }

    private boolean requestRuntimeGuardValidation(ServerLevel serverLevel, String priorityName) {
        if (validationQueued) {
            return true;
        }
        boolean queued = MultiblockRuntimeGuardHooks.requestValidation(this, serverLevel, priorityName,
                () -> runRuntimeGuardValidation(serverLevel));
        if (queued) {
            validationQueued = true;
            if (state != MultiblockState.VALIDATING) {
                state = MultiblockState.VALIDATING;
                syncBlock();
            }
        }
        return queued;
    }

    private void runRuntimeGuardValidation(ServerLevel serverLevel) {
        validationQueued = false;
        if (level != serverLevel || isRemoved()) {
            return;
        }
        applyValidationResult(serverLevel, validateStructure(false));
    }

    private void applyValidationResult(ServerLevel serverLevel, ValidationResult result) {
        Optional<MultiblockDefinition> definition = definition();
        if (!result.valid()) {
            damageFromValidation(serverLevel, result);
        } else if (definition.isPresent()) {
            state = MultiblockIntegrityService.stateFor(definition.get(), integrity, true);
            rebuildRuntime(definition.get(), result);
        }
    }

    private void tickQueue(ServerLevel serverLevel) {
        Optional<QueuedMultiblockTask> active = taskQueue.active();
        if (active.isPresent()) {
            tickActiveTask(serverLevel, active.get());
            return;
        }
        if (serverLevel.getGameTime() - lastQueueRetryTick >= AUTOMATED_QUEUE_RETRY_INTERVAL) {
            lastQueueRetryTick = serverLevel.getGameTime();
            taskQueue.nextRunnable().ifPresent(task -> tryStartQueuedTask(serverLevel, task, null));
        }
    }

    private void startNextRunnableNow(Player player) {
        if (!(level instanceof ServerLevel serverLevel) || !isFormed()) {
            return;
        }
        taskQueue.nextRunnable().ifPresent(task -> tryStartQueuedTask(serverLevel, task, player));
    }

    private void tickActiveTask(ServerLevel serverLevel, QueuedMultiblockTask queued) {
        Optional<MultiblockAutomationRecipe> recipe = AutomationRecipeRegistry.byId(queued.taskId());
        if (recipe.isEmpty()) {
            failTask(serverLevel, queued, "Automation recipe disappeared.");
            return;
        }
        notifyTaskTick(serverLevel, queued, recipe.get());
        AutomationEffectResult tickEffect = runAutomationEffects(serverLevel, queued, recipe.get(), null, "tick",
                AutomationEffectHandlers::onTick);
        if (!handleRuntimeEffectResult(serverLevel, queued, tickEffect)) {
            return;
        }
        if (!drawActiveTaskPower(serverLevel, queued, recipe.get())) {
            return;
        }
        queued.incrementProgress();
        if (queued.progressTicks() % 20 == 0 && serverLevel.getBlockEntity(queued.robotPos()) instanceof RoboticArmBlockEntity arm) {
            int heat = scaledHeat(recipe.get());
            if (heat > 0 && upgradeModifier(UpgradeModifier.Type.EMERGENCY_SHUTDOWN) > 0.0D
                    && arm.getHeat() + heat >= Math.ceil(arm.getMaxHeat() * 0.85D)) {
                String reason = "Emergency shutdown module paused automation before robotic overheat.";
                queued.pause(reason);
                state = MultiblockState.JAMMED;
                postBlocked(serverLevel, queued, reason);
                syncBlock();
                return;
            }
            arm.addHeat(heat);
            if (arm.getHeat() >= arm.getMaxHeat()) {
                queued.pause("Robotic arm is cooling.");
                state = MultiblockState.JAMMED;
                postBlocked(serverLevel, queued, "Robotic arm is cooling.");
                syncBlock();
                return;
            }
        }
        if (queued.progressTicks() >= queued.durationTicks()) {
            completeTask(serverLevel, queued, recipe.get());
        } else {
            setChanged();
        }
    }

    private void completeTask(ServerLevel serverLevel, QueuedMultiblockTask queued, MultiblockAutomationRecipe recipe) {
        MultiblockRuntimeSnapshot before = runtimeSnapshot();
        TaskStart start = prepareTask(recipe, queued.robotPos(), queued.inputsConsumed());
        if (!start.ready()) {
            blockTask(serverLevel, queued, start.reason(), null);
            releaseRobot(queued.robotPos());
            return;
        }
        AutomationTransaction.Commit commit = queued.inputsConsumed()
                ? start.transaction().produceOnly(start.output())
                : start.transaction().commit(start.input(), start.output());
        if (!commit.completed()) {
            blockTask(serverLevel, queued, commit.reason(), null);
            releaseRobot(queued.robotPos());
            return;
        }
        if (recipe.integrityRepair() > 0) {
            integrity = Math.min(definition().map(def -> (float) def.integrityRules().max()).orElse(100.0F),
                    integrity + recipe.integrityRepair());
        }
        AutomationEffectResult completeEffect = runAutomationEffects(serverLevel, queued, recipe, null, "complete",
                AutomationEffectHandlers::onComplete);
        if (!handleRuntimeEffectResult(serverLevel, queued, completeEffect)) {
            return;
        }
        notifyTaskComplete(serverLevel, queued, recipe);
        if (serverLevel.getBlockEntity(queued.robotPos()) instanceof RoboticArmBlockEntity arm) {
            arm.clearTask();
            RobotAnimationSync.play(serverLevel, worldPosition, queued.robotPos(), "retract", worldPosition, 40, recipe.id());
        }
        robotAnimations.clear();
        queued.complete();
        state = MultiblockIntegrityService.stateFor(definition().orElse(null), integrity, true);
        TaskExecutionSnapshot snapshot = queued.snapshot(recipe);
        EchoBackendLifecycleBridge.postGameEvent(new RoboticTaskCompletedEvent(serverLevel, multiblockId, recipe.id(), worldPosition, queued.robotPos(),
                snapshot, before, runtimeSnapshot()));
        MultiblockMissionHooks.recordTaskCompleted(serverLevel, worldPosition, recipe.id(), recipe.integrityRepair() > 0);
        taskQueue.pruneCompleted();
        taskQueue.nextRunnable().ifPresent(next -> tryStartQueuedTask(serverLevel, next, null));
        syncBlock();
    }

    private boolean tryStartQueuedTask(ServerLevel serverLevel, QueuedMultiblockTask queued, Player player) {
        MultiblockRuntimeSnapshot before = runtimeSnapshot();
        Optional<MultiblockAutomationRecipe> recipe = AutomationRecipeRegistry.byId(queued.taskId());
        if (recipe.isEmpty()) {
            failTask(serverLevel, queued, "Automation recipe disappeared.");
            tell(player, "TASK FAILED: Automation recipe disappeared.");
            return false;
        }
        if (!isFormed()) {
            blockTask(serverLevel, queued, "Structure is not formed.", player);
            return false;
        }
        if (!recipe.get().allowsMultiblock(multiblockId)) {
            blockTask(serverLevel, queued, "Recipe is not allowed for this multiblock.", player);
            return false;
        }
        if (!hasRequiredUpgrades(recipe.get())) {
            blockTask(serverLevel, queued, "Required upgrade missing for " + recipe.get().displayName() + ".", player);
            return false;
        }
        MultiblockCapabilityRuntime taskCapabilities = capabilityRuntimeFor(recipe.get());
        if (!taskCapabilities.satisfied()) {
            capabilityRuntime = taskCapabilities;
            blockTask(serverLevel, queued, taskCapabilities.diagnostics().stream()
                    .findFirst()
                    .map(com.knoxhack.echomultiblockcore.api.CapabilityDiagnostic::message)
                    .orElse("Required capability is unavailable."), player);
            return false;
        }
        if (!hasPowerForStart(serverLevel, queued, recipe.get(), player)) {
            return false;
        }
        TaskStart start = prepareTask(recipe.get(), null, queued.inputsConsumed());
        if (!start.ready()) {
            blockTask(serverLevel, queued, start.reason(), player);
            return false;
        }
        AutomationEffectResult beforeStartEffect = runAutomationEffects(serverLevel, queued, recipe.get(), start.plan(), player,
                "before_start", AutomationEffectHandlers::beforeStart);
        if (!handleStartEffectResult(serverLevel, queued, beforeStartEffect, player)) {
            return false;
        }
        if (!canStartTaskHandlers(serverLevel, recipe.get(), start.plan())) {
            blockTask(serverLevel, queued, "Task-specific gate blocked start.", player);
            return false;
        }
        AutomationEffectResult startEffect = runAutomationEffects(serverLevel, queued, recipe.get(), start.plan(), player,
                "start", AutomationEffectHandlers::onStart);
        if (!handleStartEffectResult(serverLevel, queued, startEffect, player)) {
            return false;
        }
        if (!queued.inputsConsumed() && recipe.get().consumeInputsOnStart()) {
            if (!start.transaction().consume(start.input())) {
                blockTask(serverLevel, queued, "Inputs changed before task start.", player);
                return false;
            }
            queued.markInputsConsumed();
        }
        queued.start(scaledDuration(recipe.get()), start.robot().getBlockPos(), start.plan().robotId(), start.plan().workcellId(),
                start.plan().inputLine(), start.plan().outputLine());
        start.robot().assignTask(recipe.get());
        start.robot().setTargetPos(start.workPos());
        notifyTaskStart(serverLevel, recipe.get(), start.plan());
        state = MultiblockState.ACTIVE;
        RobotPoseSnapshot pose = poseFor(recipe.get(), start.robot().getBlockPos(), start.workPos());
        RobotAnimationSync.play(serverLevel, worldPosition, queued.robotPos(), recipe.get().animation(), start.workPos(),
                queued.durationTicks(), recipe.get().id(), recipe.get().animationProfile(), pose, 0);
        robotAnimations.clear();
        robotAnimations.add(new RobotAnimationState(start.robot().getRobotRuntimeId(worldPosition), queued.robotPos(),
                start.workPos(), recipe.get().animation(), queued.durationTicks(), 0, pose, recipe.get().id()));
        EchoBackendLifecycleBridge.postGameEvent(new RoboticTaskStartedEvent(serverLevel, multiblockId, worldPosition,
                queued.snapshot(recipe.get()), before, runtimeSnapshot()));
        tell(player, "TASK STARTED: " + recipe.get().displayName());
        syncBlock();
        return true;
    }

    private TaskStart prepareTask(MultiblockAutomationRecipe recipe, BlockPos preferredRobot) {
        return prepareTask(recipe, preferredRobot, false);
    }

    private TaskStart prepareTask(MultiblockAutomationRecipe recipe, BlockPos preferredRobot, boolean inputsAlreadyConsumed) {
        Optional<MultiblockCrateBlockEntity> input = inputsAlreadyConsumed || recipe.inputs().isEmpty()
                ? Optional.empty()
                : findCrate(MultiblockCrateBlock.CrateKind.INPUT);
        if (!inputsAlreadyConsumed && !recipe.inputs().isEmpty() && input.isEmpty()) {
            return TaskStart.blocked("Missing input crate.");
        }
        Optional<MultiblockCrateBlockEntity> output = recipe.outputs().isEmpty()
                ? Optional.empty()
                : findCrate(MultiblockCrateBlock.CrateKind.OUTPUT);
        if (!recipe.outputs().isEmpty() && output.isEmpty()) {
            return TaskStart.blocked("Missing output crate.");
        }
        AutomationTransaction transaction = new AutomationTransaction(recipe.inputs(), recipe.outputs());
        AutomationTransaction.Check check = inputsAlreadyConsumed
                ? transaction.checkOutput(output.orElse(null))
                : transaction.check(input.orElse(null), output.orElse(null));
        if (!check.ready()) {
            return TaskStart.blocked(check.reason());
        }
        Optional<MultiblockRuntime.RuntimeWorkcell> workcell = workcellFor(recipe);
        if (workcell.isEmpty()) {
            return TaskStart.blocked("Required workcell missing: " + recipe.requiredWorkcell() + ".");
        }
        Optional<RoboticArmBlockEntity> robot = robotFor(recipe, preferredRobot, workcell.get().worldPosition());
        if (robot.isEmpty()) {
            return TaskStart.blocked(robotDiagnostic(recipe, workcell.get().worldPosition()));
        }
        AutomationExecutionPlan plan = new AutomationExecutionPlan(
                recipe.id(),
                robot.get().getRobotRuntimeId(worldPosition),
                robot.get().getBlockPos(),
                workcell.get().id(),
                workcell.get().type(),
                workcell.get().worldPosition(),
                transaction.inputSummary(),
                recipe.outputs().isEmpty() && recipe.integrityRepair() > 0
                        ? List.of("Repair +" + recipe.integrityRepair() + "% integrity")
                        : transaction.outputSummary());
        return new TaskStart(true, "", robot.get(), input.orElse(null), output.orElse(null),
                workcell.get().worldPosition(), transaction, plan);
    }

    private Optional<RoboticArmBlockEntity> robotFor(MultiblockAutomationRecipe recipe, BlockPos preferredRobot, BlockPos workPos) {
        List<RoboticArmBlockEntity> candidates = discoverRobots();
        if (preferredRobot != null && !preferredRobot.equals(BlockPos.ZERO)) {
            candidates = candidates.stream()
                    .filter(arm -> arm.getBlockPos().equals(preferredRobot))
                    .toList();
        }
        return candidates.stream()
                .filter(arm -> arm.canPerform(recipe))
                .filter(arm -> arm.getBlockPos().distManhattan(workPos) <= arm.getReach() + 4 + (int) Math.round(Math.max(0.0D, upgradeModifier(UpgradeModifier.Type.REACH_BONUS))))
                .findFirst();
    }

    private String robotDiagnostic(MultiblockAutomationRecipe recipe, BlockPos workPos) {
        List<RoboticArmBlockEntity> robots = discoverRobots();
        if (robots.isEmpty()) {
            return "Missing robotic arm.";
        }
        List<RobotToolType> requiredTools = recipe == null ? List.of() : recipe.requiredTools();
        boolean hasTool = robots.stream().anyMatch(arm -> requiredTools == null || requiredTools.isEmpty()
                || arm.getInstalledTools().stream().anyMatch(requiredTools::contains));
        if (hasTool) {
            return "No robotic arm can reach " + (recipe == null ? "required" : recipe.requiredWorkcell()) + " Workcell.";
        }
        String label = requiredTools == null || requiredTools.isEmpty()
                ? "compatible tool head"
                : requiredTools.stream()
                        .map(type -> type.name().replace('_', ' '))
                        .reduce((left, right) -> left + " or " + right)
                        .orElse("compatible tool head");
        return "Required robotic tool missing: " + label + ".";
    }

    private Optional<MultiblockRuntime.RuntimeWorkcell> workcellFor(MultiblockAutomationRecipe recipe) {
        if (runtime == null || runtime.workcells().isEmpty()) {
            return Optional.empty();
        }
        WorkcellType requiredType = recipe == null ? WorkcellType.ASSEMBLY : recipe.requiredWorkcell();
        return runtime.workcells().stream()
                .filter(workcell -> workcell.type() == requiredType)
                .filter(workcell -> workcellAllows(workcell, recipe))
                .findFirst()
                .or(() -> runtime.workcells().stream()
                        .filter(workcell -> workcell.type() == requiredType)
                        .findFirst());
    }

    private boolean workcellAllows(MultiblockRuntime.RuntimeWorkcell runtimeWorkcell, MultiblockAutomationRecipe recipe) {
        if (runtimeWorkcell == null || recipe == null) {
            return true;
        }
        return definition().stream()
                .flatMap(definition -> definition.workcells().stream())
                .filter(workcell -> workcell.id().equals(runtimeWorkcell.id()))
                .findFirst()
                .map(workcell -> workcell.allowedTaskTypes().isEmpty()
                        || workcell.allowedTaskTypes().contains(recipe.id())
                        || workcell.allowedTaskTypes().contains(recipe.category()))
                .orElse(true);
    }

    private void blockTask(ServerLevel serverLevel, QueuedMultiblockTask queued, String reason, Player player) {
        reason = reason == null || reason.isBlank() ? "Task blocked." : reason;
        MultiblockRuntimeSnapshot beforeSnapshot = runtimeSnapshot();
        String previousReason = queued.blockedReason();
        MultiblockTaskState previousState = queued.state();
        queued.block(reason);
        if (!reason.equals(previousReason) || previousState != MultiblockTaskState.BLOCKED) {
            postBlocked(serverLevel, queued, reason, beforeSnapshot);
        }
        tell(player, "TASK BLOCKED: " + reason);
        syncBlock();
    }

    private void postBlocked(ServerLevel serverLevel, QueuedMultiblockTask queued, String reason) {
        postBlocked(serverLevel, queued, reason, null);
    }

    private void postBlocked(ServerLevel serverLevel, QueuedMultiblockTask queued, String reason, MultiblockRuntimeSnapshot beforeSnapshot) {
        Optional<MultiblockAutomationRecipe> recipe = AutomationRecipeRegistry.byId(queued.taskId());
        String display = recipe.map(MultiblockAutomationRecipe::displayName).orElse(queued.taskId().toString());
        String category = recipe.map(value -> value.category().toString()).orElse("missing");
        recipe.ifPresent(value -> notifyTaskFail(serverLevel, queued, value, reason));
        EchoBackendLifecycleBridge.postGameEvent(new RoboticTaskBlockedEvent(serverLevel, multiblockId, worldPosition,
                recipe.map(queued::snapshot).orElseGet(() -> queued.snapshot(display, category)), reason, beforeSnapshot, runtimeSnapshot()));
    }

    private void failTask(ServerLevel serverLevel, QueuedMultiblockTask queued, String reason) {
        MultiblockRuntimeSnapshot before = runtimeSnapshot();
        queued.fail(reason);
        releaseRobot(queued.robotPos());
        Optional<MultiblockAutomationRecipe> recipe = AutomationRecipeRegistry.byId(queued.taskId());
        String display = recipe.map(MultiblockAutomationRecipe::displayName).orElse(queued.taskId().toString());
        String category = recipe.map(value -> value.category().toString()).orElse("missing");
        recipe.ifPresent(value -> notifyTaskFail(serverLevel, queued, value, reason));
        EchoBackendLifecycleBridge.postGameEvent(new RoboticTaskFailedEvent(serverLevel, multiblockId, worldPosition,
                recipe.map(queued::snapshot).orElseGet(() -> queued.snapshot(display, category)), reason, before, runtimeSnapshot()));
        syncBlock();
    }

    private void damageFromValidation(ServerLevel serverLevel, ValidationResult result) {
        MultiblockRuntimeSnapshot beforeSnapshot = runtimeSnapshot();
        float previousIntegrity = integrity;
        integrity = MultiblockIntegrityService.integrityFromValidation(result);
        MultiblockDefinition definition = definition().orElse(null);
        state = MultiblockIntegrityService.stateFor(definition, integrity, false);
        runtime = null;
        if (previousIntegrity != integrity) {
            EchoBackendLifecycleBridge.postGameEvent(new MultiblockDamagedEvent(serverLevel, multiblockId, worldPosition,
                    integrity, "validation", beforeSnapshot, runtimeSnapshot()));
        }
        if (state == MultiblockState.OFFLINE) {
            onStructureBroken();
        } else {
            MultiblockRuntimeManager.recordFormed(serverLevel, multiblockId, worldPosition, integrity, state);
            syncBlock();
        }
    }

    private void rebuildRuntime(MultiblockDefinition definition, ValidationResult result) {
        List<BlockPos> robots = discoverRobots(result.matchedBlocks()).stream().map(BlockEntity::getBlockPos).toList();
        capabilityRuntime = MultiblockCapabilityService.evaluate(definition, null, result.matchedBlocks(), level, installedUpgrades);
        if (!capabilityRuntime.satisfied()) {
            state = MultiblockState.OVERLOADED;
        }
        updateDamageGroups(result);
        runtime = MultiblockRuntimeBuilder.build(definition, result, integrity, robots, taskQueue, capabilityRuntime,
                installedUpgrades, damageGroups, robotAnimations, constructionProgress);
        if (level instanceof ServerLevel serverLevel) {
            MultiblockRuntimeManager.recordFormed(serverLevel, multiblockId, worldPosition, integrity, state);
        }
    }

    private List<RoboticArmBlockEntity> discoverRobots() {
        List<BlockPos> candidates = runtime == null ? List.of() : runtime.matchedBlocks();
        return discoverRobots(candidates);
    }

    private List<RoboticArmBlockEntity> discoverRobots(List<BlockPos> candidates) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.addAll(candidates == null ? List.of() : candidates);
        if (positions.isEmpty()) {
            BlockPos.betweenClosedStream(worldPosition.offset(-6, -2, -6), worldPosition.offset(6, 5, 6))
                    .forEach(pos -> positions.add(pos.immutable()));
        }
        List<RoboticArmBlockEntity> arms = new ArrayList<>();
        if (level == null) {
            return arms;
        }
        for (BlockPos pos : positions) {
            if (level.getBlockEntity(pos) instanceof RoboticArmBlockEntity arm) {
                arms.add(arm);
            }
        }
        if (arms.isEmpty() && !positions.isEmpty()) {
            BlockPos.betweenClosedStream(worldPosition.offset(-6, -2, -6), worldPosition.offset(6, 5, 6))
                    .forEach(pos -> {
                        if (level.getBlockEntity(pos) instanceof RoboticArmBlockEntity arm) {
                            arms.add(arm);
                        }
                    });
        }
        return arms;
    }

    private Optional<MultiblockCrateBlockEntity> findCrate(MultiblockCrateBlock.CrateKind kind) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        if (runtime != null) {
            positions.addAll(runtime.matchedBlocks());
        }
        if (positions.isEmpty()) {
            BlockPos.betweenClosedStream(worldPosition.offset(-6, -2, -6), worldPosition.offset(6, 5, 6))
                    .forEach(pos -> positions.add(pos.immutable()));
        }
        if (level == null) {
            return Optional.empty();
        }
        for (BlockPos pos : positions) {
            if (level.getBlockEntity(pos) instanceof MultiblockCrateBlockEntity crate && crate.kind() == kind) {
                return Optional.of(crate);
            }
        }
        if (!positions.isEmpty()) {
            for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-6, -2, -6), worldPosition.offset(6, 5, 6))) {
                if (level.getBlockEntity(pos) instanceof MultiblockCrateBlockEntity crate && crate.kind() == kind) {
                    return Optional.of(crate);
                }
            }
        }
        return Optional.empty();
    }

    private void updateStateFromIntegrity(String source, MultiblockRuntimeSnapshot beforeSnapshot) {
        state = MultiblockIntegrityService.stateFor(definition().orElse(null), integrity, isFormed());
        if (level instanceof ServerLevel serverLevel) {
            EchoBackendLifecycleBridge.postGameEvent(new MultiblockDamagedEvent(serverLevel, multiblockId, worldPosition,
                    integrity, source, beforeSnapshot, runtimeSnapshot()));
            MultiblockRuntimeManager.recordFormed(serverLevel, multiblockId, worldPosition, integrity, state);
        }
    }

    private Optional<MultiblockDefinition> definition() {
        return MultiblockContent.definition(multiblockId);
    }

    private boolean isFormed() {
        return state == MultiblockState.FORMED || state == MultiblockState.ACTIVE || state == MultiblockState.DAMAGED
                || state == MultiblockState.JAMMED || state == MultiblockState.OVERLOADED || state == MultiblockState.VALIDATING;
    }

    private int scaledDuration(MultiblockAutomationRecipe recipe) {
        double multiplier;
        try {
            multiplier = com.knoxhack.echomultiblockcore.Config.ROBOTIC_TASK_SPEED_MULTIPLIER.get();
        } catch (RuntimeException exception) {
            multiplier = 1.0D;
        }
        multiplier *= Math.max(0.1D, 1.0D + upgradeModifier(UpgradeModifier.Type.SPEED_MULTIPLIER));
        return Math.max(20, (int) Math.round(recipe.durationTicks() / Math.max(0.1D, multiplier)));
    }

    private MultiblockCapabilityRuntime capabilityRuntimeFor(MultiblockAutomationRecipe recipe) {
        ValidationResult result = validationCache.lastResult();
        List<BlockPos> matched = runtime == null
                ? (result == null ? List.of() : result.matchedBlocks())
                : runtime.matchedBlocks();
        MultiblockCapabilityRuntime evaluated = MultiblockCapabilityService.evaluate(
                definition().orElse(null), recipe, matched, level, installedUpgrades);
        if (recipe == null || recipe.capabilityCosts().isEmpty()) {
            capabilityRuntime = evaluated;
        }
        return evaluated;
    }

    private boolean hasPowerForStart(ServerLevel serverLevel, QueuedMultiblockTask queued,
            MultiblockAutomationRecipe recipe, Player player) {
        long epPerTick = powerCostPerTick(recipe);
        if (epPerTick <= 0L) {
            return true;
        }
        long available = MultiblockIntegrationServices.availablePower(serverLevel, worldPosition);
        if (available >= epPerTick) {
            return true;
        }
        blockTask(serverLevel, queued,
                "Power-starved: requires " + epPerTick + " EP/t from echo:power_input; available "
                        + available + " EP.",
                player);
        return false;
    }

    private boolean drawActiveTaskPower(ServerLevel serverLevel, QueuedMultiblockTask queued,
            MultiblockAutomationRecipe recipe) {
        long epPerTick = powerCostPerTick(recipe);
        if (epPerTick <= 0L) {
            return true;
        }
        long drawn = MultiblockIntegrationServices.drawPower(serverLevel, worldPosition, epPerTick, false);
        if (drawn >= epPerTick) {
            return true;
        }
        String reason = "Power-starved: drew " + drawn + "/" + epPerTick
                + " EP this tick from echo:power_input.";
        queued.pause(reason);
        state = MultiblockState.JAMMED;
        postBlocked(serverLevel, queued, reason);
        syncBlock();
        return false;
    }

    private long powerCostPerTick(MultiblockAutomationRecipe recipe) {
        if (recipe == null || recipe.capabilityCosts().isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (CapabilityRequirement requirement : recipe.capabilityCosts()) {
            if (requirement == null || !MultiblockCapability.POWER_INPUT.id().equals(requirement.capabilityId())) {
                continue;
            }
            long cost = requirement.throughput() > 0 ? requirement.throughput() : requirement.amount();
            if (cost > 0L) {
                total = saturatedAdd(total, cost);
            }
        }
        if (total <= 0L) {
            return 0L;
        }
        double multiplier = Math.max(0.1D, 1.0D + upgradeModifier(UpgradeModifier.Type.POWER_COST_MULTIPLIER));
        return Math.max(1L, (long)Math.ceil(total * multiplier));
    }

    private boolean hasRequiredUpgrades(MultiblockAutomationRecipe recipe) {
        if (recipe == null || recipe.requiredUpgrades().isEmpty()) {
            return true;
        }
        Set<Identifier> installed = installedUpgrades.stream()
                .map(InstalledMultiblockUpgrade::upgradeId)
                .collect(java.util.stream.Collectors.toSet());
        return installed.containsAll(recipe.requiredUpgrades());
    }

    private RobotPoseSnapshot poseFor(MultiblockAutomationRecipe recipe, BlockPos robotPos, BlockPos targetPos) {
        double dx = targetPos.getX() - robotPos.getX();
        double dz = targetPos.getZ() - robotPos.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float reachPitch = (float) Math.max(-55.0D, Math.min(55.0D,
                -12.0D * Math.max(1.0D, Math.sqrt(dx * dx + dz * dz))));
        float tool = recipe == null ? 0.25F : switch (recipe.animation()) {
            case "weld", "repair" -> 0.1F;
            case "grab_item" -> 0.9F;
            case "cut" -> 0.35F;
            default -> 0.5F;
        };
        return new RobotPoseSnapshot(yaw, reachPitch, 28.0F, -18.0F, tool);
    }

    private double upgradeModifier(UpgradeModifier.Type type) {
        if (type == null || installedUpgrades.isEmpty()) {
            return 0.0D;
        }
        double total = 0.0D;
        for (InstalledMultiblockUpgrade upgrade : installedUpgrades) {
            total += MultiblockUpgradeRegistry.byId(upgrade.upgradeId()).stream()
                    .flatMap(definition -> definition.modifiers().stream())
                    .filter(modifier -> modifier.type() == type)
                    .mapToDouble(modifier -> modifier.value() * Math.max(1, upgrade.tier()))
                    .sum();
        }
        return total;
    }

    private int scaledHeat(MultiblockAutomationRecipe recipe) {
        if (recipe == null) {
            return 0;
        }
        return Math.max(0, (int) Math.round(recipe.heatPerSecond()
                - upgradeModifier(UpgradeModifier.Type.HEAT_REDUCTION)));
    }

    private Optional<Identifier> firstAvailableUpgradeSlot(Identifier upgradeId) {
        MultiblockDefinition definition = definition().orElse(null);
        if (definition == null || definition.upgradeSlotRules().isEmpty()) {
            return Optional.of(EchoMultiblockCore.id("operator_slot_" + installedUpgrades.size()));
        }
        Set<Identifier> occupied = installedUpgrades.stream()
                .map(InstalledMultiblockUpgrade::slotId)
                .collect(java.util.stream.Collectors.toSet());
        return definition.upgradeSlotRules().stream()
                .filter(slot -> !occupied.contains(slot.slotId()))
                .filter(slot -> slot.allowedUpgrades().isEmpty() || slot.allowedUpgrades().contains(upgradeId))
                .map(com.knoxhack.echomultiblockcore.api.UpgradeSlotRule::slotId)
                .findFirst();
    }

    private static long saturatedAdd(long left, long right) {
        if (left <= 0L) {
            return Math.max(0L, right);
        }
        if (right <= 0L) {
            return left;
        }
        if (left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private List<String> repairActions() {
        List<String> actions = new ArrayList<>();
        if (integrity < 100.0F) {
            actions.add("Queue a repair automation recipe or install an integrity upgrade.");
        }
        if (state == MultiblockState.JAMMED) {
            actions.add("Clear the jam, cool robotic arms, then retry blocked tasks.");
        }
        if (capabilityRuntime != null && !capabilityRuntime.satisfied()) {
            actions.add("Add bus nodes or upgrades for blocked capability requirements.");
        }
        return List.copyOf(actions);
    }

    private void updateDamageGroups(ValidationResult result) {
        damageGroups.clear();
        if (integrity < 100.0F) {
            damageGroups.add("integrity " + Math.round(integrity) + "%");
        }
        if (result != null && !result.missingBlocks().isEmpty()) {
            damageGroups.add("missing components " + result.missingBlocks().size());
        }
        if (result != null && !result.wrongBlocks().isEmpty()) {
            damageGroups.add("wrong components " + result.wrongBlocks().size());
        }
        if (capabilityRuntime != null && !capabilityRuntime.satisfied()) {
            damageGroups.add("capability deficit");
        }
    }

    private List<String> warnings() {
        ValidationResult result = validationCache.lastResult();
        return result == null ? List.of() : result.warnings();
    }

    public Optional<MultiblockProgressionDefinition> progression() {
        return MultiblockProgressionRegistry.byFacility(multiblockId);
    }

    public String progressionTitleForDisplay() {
        return progression().map(MultiblockProgressionDefinition::title).orElse("");
    }

    public String progressionHintForDisplay() {
        return progression().map(this::progressionHint).orElse("");
    }

    private String progressionHint(MultiblockProgressionDefinition progression) {
        if (progression == null) {
            return "";
        }
        if (!progression.prerequisites().isEmpty()) {
            return "Prerequisites: " + progression.prerequisites().stream()
                    .map(id -> id.getPath().replace('_', ' '))
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("none");
        }
        if (!progression.featuredRecipes().isEmpty()) {
            return "Featured: " + progression.featuredRecipeSummary();
        }
        return progression.guideText().isBlank() ? "Form this facility to continue the progression chain." : progression.guideText();
    }

    private String firstBlockedReason() {
        return taskQueue.tasks().stream()
                .map(QueuedMultiblockTask::blockedReason)
                .filter(reason -> reason != null && !reason.isBlank())
                .findFirst()
                .orElse("");
    }

    private void clearTaskInternal() {
        releaseActiveRobot();
        taskQueue.clear();
    }

    private void releaseActiveRobot() {
        taskQueue.active().ifPresent(task -> releaseRobot(task.robotPos()));
    }

    private void releaseRobot(BlockPos pos) {
        if (level != null && pos != null && !pos.equals(BlockPos.ZERO)
                && level.getBlockEntity(pos) instanceof RoboticArmBlockEntity arm) {
            arm.clearTask();
        }
    }

    private boolean canStartTaskHandlers(ServerLevel serverLevel, MultiblockAutomationRecipe recipe, AutomationExecutionPlan plan) {
        AutomationTaskContext context = taskContext(serverLevel, recipe, plan);
        for (AutomationTaskHandler handler : AutomationTaskHandlers.handlersFor(recipe)) {
            try {
                if (!handler.canStart(context, recipe, plan)) {
                    return false;
                }
            } catch (RuntimeException exception) {
                return false;
            }
        }
        return true;
    }

    private AutomationEffectResult runAutomationEffects(ServerLevel serverLevel, QueuedMultiblockTask queued,
            MultiblockAutomationRecipe recipe, Player actor, String phase,
            Function<AutomationEffectInvocation, AutomationEffectResult> invoker) {
        return runAutomationEffects(serverLevel, queued, recipe, null, actor, phase, invoker);
    }

    private AutomationEffectResult runAutomationEffects(ServerLevel serverLevel, QueuedMultiblockTask queued,
            MultiblockAutomationRecipe recipe, AutomationExecutionPlan plan, Player actor, String phase,
            Function<AutomationEffectInvocation, AutomationEffectResult> invoker) {
        if (recipe == null || recipe.effects().isEmpty()) {
            return AutomationEffectResult.allow();
        }
        AutomationExecutionPlan effectivePlan = plan == null ? planFromQueued(recipe, queued) : plan;
        AutomationEffectResult latest = AutomationEffectResult.allow();
        for (Identifier effectId : recipe.effects()) {
            AutomationEffectInvocation invocation = new AutomationEffectInvocation(
                    serverLevel, this, worldPosition, actor, effectId, recipe, effectivePlan, queued.snapshot(recipe), phase);
            AutomationEffectResult result = invoker.apply(invocation);
            result = result == null ? AutomationEffectResult.allow() : result;
            if (!result.reason().isBlank()) {
                queued.recordEffectDiagnostic(result.reason());
                latest = result;
            }
            if (!result.allowed()) {
                return result;
            }
        }
        return latest;
    }

    private boolean handleStartEffectResult(ServerLevel serverLevel, QueuedMultiblockTask queued,
            AutomationEffectResult result, Player player) {
        if (result == null || result.allowed()) {
            return true;
        }
        if (result.failed()) {
            failTask(serverLevel, queued, result.reason());
            tell(player, "TASK FAILED: " + result.reason());
        } else {
            blockTask(serverLevel, queued, result.reason(), player);
        }
        return false;
    }

    private boolean handleRuntimeEffectResult(ServerLevel serverLevel, QueuedMultiblockTask queued,
            AutomationEffectResult result) {
        if (result == null || result.allowed()) {
            return true;
        }
        if (result.failed()) {
            failTask(serverLevel, queued, result.reason());
        } else {
            releaseRobot(queued.robotPos());
            blockTask(serverLevel, queued, result.reason(), null);
        }
        return false;
    }

    private void notifyTaskStart(ServerLevel serverLevel, MultiblockAutomationRecipe recipe, AutomationExecutionPlan plan) {
        AutomationTaskContext context = taskContext(serverLevel, recipe, plan);
        for (AutomationTaskHandler handler : AutomationTaskHandlers.handlersFor(recipe)) {
            handler.onStart(context, recipe, plan);
        }
    }

    private void notifyTaskTick(ServerLevel serverLevel, QueuedMultiblockTask queued, MultiblockAutomationRecipe recipe) {
        AutomationExecutionPlan plan = planFromQueued(recipe, queued);
        AutomationTaskContext context = taskContext(serverLevel, recipe, plan);
        for (AutomationTaskHandler handler : AutomationTaskHandlers.handlersFor(recipe)) {
            handler.onTick(context, recipe, plan);
        }
    }

    private void notifyTaskComplete(ServerLevel serverLevel, QueuedMultiblockTask queued, MultiblockAutomationRecipe recipe) {
        AutomationExecutionPlan plan = planFromQueued(recipe, queued);
        AutomationTaskContext context = taskContext(serverLevel, recipe, plan);
        for (AutomationTaskHandler handler : AutomationTaskHandlers.handlersFor(recipe)) {
            handler.onComplete(context, recipe, plan);
        }
    }

    private void notifyTaskFail(ServerLevel serverLevel, QueuedMultiblockTask queued, MultiblockAutomationRecipe recipe, String reason) {
        AutomationExecutionPlan plan = planFromQueued(recipe, queued);
        AutomationTaskContext context = taskContext(serverLevel, recipe, plan);
        for (AutomationTaskHandler handler : AutomationTaskHandlers.handlersFor(recipe)) {
            handler.onFail(context, recipe, plan, reason);
        }
        runAutomationEffects(serverLevel, queued, recipe, plan, null, "fail", AutomationEffectHandlers::onFail);
    }

    private AutomationTaskContext taskContext(ServerLevel serverLevel, MultiblockAutomationRecipe recipe, AutomationExecutionPlan plan) {
        return new AutomationTaskContext(serverLevel, this, worldPosition, plan == null ? BlockPos.ZERO : plan.robotPos(),
                recipe == null ? null : recipe.id(), plan);
    }

    private AutomationExecutionPlan planFromQueued(MultiblockAutomationRecipe recipe, QueuedMultiblockTask queued) {
        return new AutomationExecutionPlan(
                recipe.id(),
                queued.robotId(),
                queued.robotPos(),
                queued.workcellId(),
                recipe.requiredWorkcell(),
                BlockPos.ZERO,
                queued.inputSummary().isBlank() ? List.of() : List.of(queued.inputSummary()),
                queued.outputSummary().isBlank() ? List.of() : List.of(queued.outputSummary()));
    }

    private void sendDiagnostics(Player player) {
        sendLines(player, diagnosticLines());
    }

    private void sendLines(Player player, List<String> lines) {
        for (String line : lines) {
            tell(player, line);
        }
    }

    private void tell(Player player, String message) {
        if (player != null && message != null && !message.isBlank()) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    private void syncBlock() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int schemaVersion = input.getIntOr("runtime_schema_version", 1);
        multiblockId = Identifier.tryParse(input.getStringOr("multiblock_id", multiblockId.toString()));
        if (multiblockId == null) {
            multiblockId = EchoMultiblockCore.id("industrial_assembly_line");
        }
        state = enumOr(MultiblockState.class, input.getStringOr("state", MultiblockState.UNBUILT.name()), MultiblockState.UNBUILT);
        integrity = input.getFloatOr("integrity", 0.0F);
        structureVersion = Math.max(0L, input.getLongOr("structure_version", 0L));
        if (schemaVersion < RUNTIME_SCHEMA_VERSION) {
            structureVersion++;
        }
        constructionProgress = input.getStringOr("construction_progress", "");
        installedUpgrades.clear();
        int upgradeCount = Math.max(0, Math.min(16, input.getIntOr("upgrade_count", 0)));
        for (int i = 0; i < upgradeCount; i++) {
            Identifier upgradeId = Identifier.tryParse(input.getStringOr("upgrade_" + i + "_id", ""));
            Identifier slotId = Identifier.tryParse(input.getStringOr("upgrade_" + i + "_slot", ""));
            if (upgradeId != null) {
                installedUpgrades.add(new InstalledMultiblockUpgrade(
                        upgradeId,
                        slotId == null ? EchoMultiblockCore.id("slot_" + i) : slotId,
                        BlockPos.of(input.getLongOr("upgrade_" + i + "_pos", worldPosition.asLong())),
                        input.getIntOr("upgrade_" + i + "_tier", 1)));
            }
        }
        taskQueue.load(input);
        validationCache.markDirty();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("runtime_schema_version", RUNTIME_SCHEMA_VERSION);
        output.putString("multiblock_id", multiblockId.toString());
        output.putString("state", state.name());
        output.putFloat("integrity", integrity);
        output.putLong("structure_version", structureVersion);
        output.putString("construction_progress", constructionProgress);
        output.putInt("upgrade_count", installedUpgrades.size());
        for (int i = 0; i < installedUpgrades.size(); i++) {
            InstalledMultiblockUpgrade upgrade = installedUpgrades.get(i);
            output.putString("upgrade_" + i + "_id", upgrade.upgradeId().toString());
            output.putString("upgrade_" + i + "_slot", upgrade.slotId().toString());
            output.putLong("upgrade_" + i + "_pos", upgrade.worldPosition().asLong());
            output.putInt("upgrade_" + i + "_tier", upgrade.tier());
        }
        taskQueue.save(output);
        saveLegacyTaskKeys(output);
    }

    private void saveLegacyTaskKeys(ValueOutput output) {
        QueuedMultiblockTask first = taskQueue.tasks().isEmpty() ? null : taskQueue.tasks().get(0);
        output.putBoolean("validation_dirty", validationCache.isDirty());
        output.putString("queued_task", first == null ? "" : first.taskId().toString());
        output.putString("task_state", first == null ? MultiblockTaskState.WAITING.name() : first.state().name());
        output.putInt("task_progress", first == null ? 0 : first.progressTicks());
        output.putInt("task_duration", first == null ? 1 : first.durationTicks());
        output.putLong("assigned_robot", first == null ? 0L : first.robotPos().asLong());
        output.putString("task_blocked_reason", first == null ? "" : first.blockedReason());
    }

    private static <E extends Enum<E>> E enumOr(Class<E> type, String raw, E fallback) {
        try {
            return Enum.valueOf(type, raw == null ? "" : raw.strip().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private record TaskStart(
            boolean ready,
            String reason,
            RoboticArmBlockEntity robot,
            MultiblockCrateBlockEntity input,
            MultiblockCrateBlockEntity output,
            BlockPos workPos,
            AutomationTransaction transaction,
            AutomationExecutionPlan plan) {
        static TaskStart blocked(String reason) {
            return new TaskStart(false, reason, null, null, null, BlockPos.ZERO, null, null);
        }
    }
}
