package com.knoxhack.echomultiblockcore.integration;

import com.knoxhack.echo.machinecore.EchoMachineAutomationHook;
import com.knoxhack.echo.machinecore.EchoMachineAutomationHookKind;
import com.knoxhack.echo.machinecore.EchoMachineFailureKind;
import com.knoxhack.echo.machinecore.EchoMachineFailureState;
import com.knoxhack.echo.machinecore.EchoMachineId;
import com.knoxhack.echo.machinecore.EchoMachineIntegrationRefs;
import com.knoxhack.echo.machinecore.EchoMachineKind;
import com.knoxhack.echo.machinecore.EchoMachineMaintenanceProfile;
import com.knoxhack.echo.machinecore.EchoMachineProfile;
import com.knoxhack.echo.machinecore.EchoMachineRecipeBinding;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeSnapshot;
import com.knoxhack.echo.machinecore.EchoMachineState;
import com.knoxhack.echo.platformcore.EchoFeatureId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.recipecore.EchoRecipeId;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.CapabilityThroughput;
import com.knoxhack.echomultiblockcore.api.InstalledMultiblockUpgrade;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echomultiblockcore.api.MultiblockCapability;
import com.knoxhack.echomultiblockcore.api.MultiblockRuntimeSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockState;
import com.knoxhack.echomultiblockcore.api.MultiblockTaskState;
import com.knoxhack.echomultiblockcore.api.TaskExecutionSnapshot;
import com.knoxhack.echomultiblockcore.block.entity.MultiblockControllerBlockEntity;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MultiblockMachineCoreAdapter {
    private static final EchoModuleId OWNER = EchoModuleId.of(EchoMultiblockCore.MODID);

    private MultiblockMachineCoreAdapter() {
    }

    public static boolean supports(BlockEntity blockEntity) {
        return blockEntity instanceof MultiblockControllerBlockEntity;
    }

    public static EchoMachineRuntimeSnapshot runtimeSnapshot(MultiblockControllerBlockEntity controller) {
        MultiblockRuntimeSnapshot snapshot = controller.runtimeSnapshot();
        Identifier definitionId = definitionId(snapshot);
        Identifier blockId = blockId(controller);
        return new EchoMachineRuntimeSnapshot(
                EchoMachineId.of(definitionId.toString()),
                OWNER,
                blockId.toString(),
                EchoMachineKind.MULTIBLOCK,
                machineState(snapshot.state()),
                snapshot.displayName(),
                failureStates(snapshot),
                inventory(controller, snapshot),
                energy(snapshot),
                EchoMachineRuntimeSnapshot.FluidContract.empty(),
                process(controller, snapshot),
                side(snapshot),
                upgrades(snapshot),
                savedState(controller, snapshot),
                integrationRefs(snapshot),
                runtimeAttributes(snapshot, blockId)
        );
    }

    public static EchoMachineProfile profile(MultiblockControllerBlockEntity controller) {
        MultiblockRuntimeSnapshot snapshot = controller.runtimeSnapshot();
        Identifier definitionId = definitionId(snapshot);
        return new EchoMachineProfile(
                EchoMachineId.of(definitionId.toString()),
                EchoMachineKind.MULTIBLOCK,
                machineState(snapshot.state()),
                OWNER,
                null,
                List.of(recipeBinding(controller, snapshot)),
                List.of(),
                new EchoMachineMaintenanceProfile(
                        maintenanceWear(snapshot),
                        20 * 60 * 30,
                        List.of("multiblock_core", snapshot.category(), snapshot.role().name().toLowerCase(java.util.Locale.ROOT)),
                        true,
                        Map.of(
                                "source", "MultiblockMachineCoreAdapter",
                                "definitionId", definitionId.toString(),
                                "integrity", Integer.toString(Math.round(snapshot.integrity())),
                                "state", snapshot.state().name())),
                failureStates(snapshot),
                automationHooks(snapshot),
                integrationRefs(snapshot),
                List.of(),
                Map.of(
                        "source", "MultiblockMachineCoreAdapter",
                        "definitionId", definitionId.toString(),
                        "displayName", snapshot.displayName())
        );
    }

    private static Identifier definitionId(MultiblockRuntimeSnapshot snapshot) {
        return snapshot.definitionId() == null ? EchoMultiblockCore.id("industrial_assembly_line") : snapshot.definitionId();
    }

    private static Identifier blockId(MultiblockControllerBlockEntity controller) {
        Identifier id = controller == null ? null : BuiltInRegistries.BLOCK.getKey(controller.getBlockState().getBlock());
        return id == null ? EchoMultiblockCore.id("multiblock_controller") : id;
    }

    private static EchoMachineState machineState(MultiblockState state) {
        return switch (state) {
            case FORMED -> EchoMachineState.IDLE;
            case ACTIVE, VALIDATING -> EchoMachineState.ACTIVE;
            case PAUSED -> EchoMachineState.PAUSED;
            case DAMAGED -> EchoMachineState.DAMAGED;
            case JAMMED -> EchoMachineState.JAMMED;
            case OVERLOADED -> EchoMachineState.OVERLOADED;
            case OFFLINE -> EchoMachineState.OFFLINE;
            case UNBUILT, INCOMPLETE -> EchoMachineState.LOCKED;
        };
    }

    private static List<EchoMachineFailureState> failureStates(MultiblockRuntimeSnapshot snapshot) {
        return switch (snapshot.state()) {
            case UNBUILT, INCOMPLETE -> List.of(failure(EchoMachineFailureKind.STRUCTURE_INVALID, 0.75D,
                    "Multiblock structure is not formed", "structure_incomplete", snapshot));
            case JAMMED -> List.of(failure(EchoMachineFailureKind.JAM, 0.7D,
                    "Multiblock task queue is jammed", "task_queue_jammed", snapshot));
            case DAMAGED -> List.of(failure(EchoMachineFailureKind.WEAR, 0.65D,
                    "Multiblock integrity is degraded", "integrity_damaged", snapshot));
            case OVERLOADED -> List.of(failure(EchoMachineFailureKind.OVERHEAT, 0.8D,
                    "Multiblock capability demand exceeds available throughput", "capability_overload", snapshot));
            case OFFLINE -> List.of(failure(EchoMachineFailureKind.STRUCTURE_INVALID, 1.0D,
                    "Multiblock controller is offline", "controller_offline", snapshot));
            case PAUSED -> List.of(failure(EchoMachineFailureKind.AUTOMATION_BLOCKED, 0.25D,
                    "Multiblock automation is paused", "queue_paused", snapshot));
            case FORMED, ACTIVE, VALIDATING -> {
                if (snapshot.warningCount() > 0) {
                    yield List.of(failure(EchoMachineFailureKind.STRUCTURE_INVALID, 0.35D,
                            "Multiblock has validation warnings", "validation_warnings", snapshot));
                }
                yield List.of(failure(EchoMachineFailureKind.NONE, 0.0D,
                        "Multiblock facility nominal", "nominal", snapshot));
            }
        };
    }

    private static EchoMachineFailureState failure(EchoMachineFailureKind kind, double severity,
            String summary, String detail, MultiblockRuntimeSnapshot snapshot) {
        return new EchoMachineFailureState(kind, severity, summary, detail, List.of(), Map.of(
                "definitionId", definitionId(snapshot).toString(),
                "state", snapshot.state().name(),
                "integrity", Integer.toString(Math.round(snapshot.integrity())),
                "warningCount", Integer.toString(snapshot.warningCount())));
    }

    private static EchoMachineRuntimeSnapshot.InventoryContract inventory(MultiblockControllerBlockEntity controller,
            MultiblockRuntimeSnapshot snapshot) {
        List<EchoMachineRuntimeSnapshot.SlotSnapshot> slots = List.of(
                controlSlot(0, "controller", definitionId(snapshot).toString(), snapshot.displayName(), true,
                        Map.of("blockId", blockId(controller).toString())),
                controlSlot(1, "task_queue", recipeContract(controller, snapshot), "Task Queue",
                        snapshot.taskCount() > 0, Map.of(
                                "queueSize", Integer.toString(controller.taskQueueSize()),
                                "queueCapacity", Integer.toString(controller.taskQueueCapacity()),
                                "blocked", Boolean.toString(controller.hasBlockedTasks()))),
                controlSlot(2, "upgrades", EchoMultiblockCore.id("upgrades").toString(), "Upgrade Slots",
                        !snapshot.installedUpgrades().isEmpty(), Map.of(
                                "installed", Integer.toString(snapshot.installedUpgrades().size()))),
                controlSlot(3, "robotics", MultiblockCapability.ROBOTICS.id().toString(), "Robotics",
                        snapshot.roboticComponentCount() > 0, Map.of(
                                "roboticComponentCount", Integer.toString(snapshot.roboticComponentCount()))));
        int occupied = (int) slots.stream().filter(EchoMachineRuntimeSnapshot.SlotSnapshot::occupied).count();
        return new EchoMachineRuntimeSnapshot.InventoryContract(slots.size(), occupied, slots,
                Map.of("contract", "multiblock_controller_inventory", "definitionId", definitionId(snapshot).toString()));
    }

    private static EchoMachineRuntimeSnapshot.SlotSnapshot controlSlot(int index, String role, String itemId,
            String itemName, boolean occupied, Map<String, String> attributes) {
        return new EchoMachineRuntimeSnapshot.SlotSnapshot(index, role, itemId, itemName,
                occupied ? 1 : 0, occupied, attributes);
    }

    private static EchoMachineRuntimeSnapshot.EnergyContract energy(MultiblockRuntimeSnapshot snapshot) {
        Optional<CapabilityThroughput> power = snapshot.capabilityRuntime().throughput().stream()
                .filter(line -> MultiblockCapability.POWER_INPUT.id().equals(line.capabilityId()))
                .findFirst();
        if (power.isEmpty()) {
            return EchoMachineRuntimeSnapshot.EnergyContract.empty();
        }
        CapabilityThroughput line = power.get();
        return new EchoMachineRuntimeSnapshot.EnergyContract(
                line.capabilityId().toString(),
                line.unit(),
                line.available(),
                Math.max(line.required(), line.available()),
                true,
                false,
                Map.of(
                        "contract", "multiblock_capability_power",
                        "definitionId", definitionId(snapshot).toString(),
                        "required", Integer.toString(line.required()),
                        "throughput", Integer.toString(line.throughput()),
                        "satisfied", Boolean.toString(line.satisfied()))
        );
    }

    private static EchoMachineRuntimeSnapshot.ProcessContract process(MultiblockControllerBlockEntity controller,
            MultiblockRuntimeSnapshot snapshot) {
        Optional<TaskExecutionSnapshot> task = currentTask(snapshot);
        int progress = task.map(TaskExecutionSnapshot::progressTicks)
                .orElseGet(() -> (int) Math.round(snapshot.completion() * 100.0D));
        int max = task.map(TaskExecutionSnapshot::durationTicks).orElse(100);
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("contract", "multiblock_controller_process");
        attributes.put("definitionId", definitionId(snapshot).toString());
        attributes.put("category", snapshot.category());
        attributes.put("role", snapshot.role().name());
        attributes.put("state", snapshot.state().name());
        attributes.put("integrity", Integer.toString(Math.round(snapshot.integrity())));
        attributes.put("completionPercent", Integer.toString((int) Math.round(snapshot.completion() * 100.0D)));
        attributes.put("taskCount", Integer.toString(snapshot.taskCount()));
        attributes.put("warningCount", Integer.toString(snapshot.warningCount()));
        attributes.put("capabilitySummary", snapshot.capabilityRuntime().summary());
        attributes.put("progressionTitle", snapshot.progressionTitle());
        attributes.put("featuredRecipeSummary", snapshot.featuredRecipeSummary());
        if (!snapshot.blockedReasonCode().isBlank()) {
            attributes.put("blockedReasonCode", snapshot.blockedReasonCode());
        }
        task.ifPresent(value -> {
            attributes.put("taskId", value.taskId().toString());
            attributes.put("taskState", value.state().name());
            attributes.put("taskCategory", value.recipeCategory());
            attributes.put("inputSummary", value.inputSummary());
            attributes.put("outputSummary", value.outputSummary());
            attributes.put("effectDiagnostic", value.effectDiagnostic());
        });
        return new EchoMachineRuntimeSnapshot.ProcessContract(
                processStatus(snapshot, task),
                processActive(snapshot, task),
                progress,
                max,
                percent(progress, max),
                recipeContract(controller, snapshot),
                Map.copyOf(attributes)
        );
    }

    private static EchoMachineRuntimeSnapshot.SideConfigurationContract side(MultiblockRuntimeSnapshot snapshot) {
        return new EchoMachineRuntimeSnapshot.SideConfigurationContract(
                "Multiblock controller lanes",
                List.of("controller", "upgrades", "power_input"),
                List.of("task_queue", "maintenance"),
                List.of("robotics", "diagnostics", "remote_status"),
                Map.of("contract", "multiblock_controller_sides", "definitionId", definitionId(snapshot).toString())
        );
    }

    private static EchoMachineRuntimeSnapshot.UpgradeContract upgrades(MultiblockRuntimeSnapshot snapshot) {
        List<EchoMachineRuntimeSnapshot.SlotSnapshot> slots = snapshot.installedUpgrades().stream()
                .map(MultiblockMachineCoreAdapter::upgradeSlot)
                .toList();
        int capacity = Math.max(slots.size(), snapshot.installedUpgrades().size());
        return new EchoMachineRuntimeSnapshot.UpgradeContract(capacity, slots.size(), slots,
                Map.of("contract", "multiblock_upgrade_slots", "definitionId", definitionId(snapshot).toString()));
    }

    private static EchoMachineRuntimeSnapshot.SlotSnapshot upgradeSlot(InstalledMultiblockUpgrade upgrade) {
        int index = Math.max(0, upgrade.tier() - 1);
        return new EchoMachineRuntimeSnapshot.SlotSnapshot(index, "upgrade", upgrade.upgradeId().toString(),
                upgrade.upgradeId().getPath(), 1, true, Map.of(
                        "slotId", upgrade.slotId().toString(),
                        "tier", Integer.toString(upgrade.tier()),
                        "position", position(upgrade.worldPosition())));
    }

    private static EchoMachineRuntimeSnapshot.SavedStateContract savedState(MultiblockControllerBlockEntity controller,
            MultiblockRuntimeSnapshot snapshot) {
        return new EchoMachineRuntimeSnapshot.SavedStateContract(
                "multiblock_controller_v4",
                List.of("runtime_schema_version", "multiblock_id", "state", "integrity", "structure_version",
                        "construction_progress", "upgrade_count", "task_queue_size", "validation_dirty",
                        "queued_task", "task_state", "task_progress", "task_duration", "assigned_robot",
                        "task_blocked_reason"),
                4,
                energy(snapshot).stored(),
                Math.max(0, 100 - Math.round(snapshot.integrity())),
                machineState(snapshot.state()).serializedName(),
                side(snapshot).label(),
                snapshot.state() == MultiblockState.PAUSED,
                Map.of(
                        "source", "MultiblockMachineCoreAdapter",
                        "definitionId", definitionId(snapshot).toString(),
                        "queueCapacity", Integer.toString(controller.taskQueueCapacity()),
                        "queueRemaining", Integer.toString(controller.taskQueueRemainingCapacity()))
        );
    }

    private static EchoMachineRecipeBinding recipeBinding(MultiblockControllerBlockEntity controller,
            MultiblockRuntimeSnapshot snapshot) {
        return new EchoMachineRecipeBinding(
                EchoRecipeId.of(recipeContract(controller, snapshot)),
                null,
                definitionId(snapshot).getPath(),
                snapshot.progressionTier(),
                List.of(),
                Map.of(
                        "definitionId", definitionId(snapshot).toString(),
                        "source", "MultiblockMachineCoreAdapter",
                        "featuredRecipeSummary", snapshot.featuredRecipeSummary())
        );
    }

    private static List<EchoMachineAutomationHook> automationHooks(MultiblockRuntimeSnapshot snapshot) {
        return List.of(
                hook("signal_input", EchoMachineAutomationHookKind.SIGNAL_INPUT, "task_queue"),
                hook("signal_output", EchoMachineAutomationHookKind.SIGNAL_OUTPUT, "runtime_events"),
                hook("power_input", EchoMachineAutomationHookKind.POWER_INPUT, "capability_power_input"),
                hook("maintenance", EchoMachineAutomationHookKind.MAINTENANCE, "integrity_service"),
                hook("remote_status", EchoMachineAutomationHookKind.REMOTE_STATUS, definitionId(snapshot).getPath())
        );
    }

    private static EchoMachineAutomationHook hook(String id, EchoMachineAutomationHookKind kind, String detail) {
        return new EchoMachineAutomationHook(id, kind, null,
                List.of(
                        EchoFeatureId.of("echolens.deep_scan"),
                        EchoFeatureId.of("echoterminal.recipe_provider"),
                        EchoFeatureId.of("echoindex.machine_entries"),
                        EchoFeatureId.of("echoholomap.machine_markers"),
                        EchoFeatureId.of("echomultiblockcore.facility_runtime"),
                        EchoFeatureId.of("echomultiblockcore.robotic_automation")),
                true,
                Map.of("detail", detail));
    }

    private static EchoMachineIntegrationRefs integrationRefs(MultiblockRuntimeSnapshot snapshot) {
        return new EchoMachineIntegrationRefs(null, null, null,
                List.of(
                        EchoFeatureId.of("echolens.deep_scan"),
                        EchoFeatureId.of("echoterminal.recipe_provider"),
                        EchoFeatureId.of("echoindex.machine_entries"),
                        EchoFeatureId.of("echoholomap.machine_markers"),
                        EchoFeatureId.of("echomultiblockcore.facility_terminal"),
                        EchoFeatureId.of("echomultiblockcore.progression_routes")),
                List.of(),
                Map.of(
                        "definitionId", definitionId(snapshot).toString(),
                        "category", snapshot.category(),
                        "primaryUse", snapshot.primaryUse(),
                        "recipe", recipeFallback(snapshot).toString())
        );
    }

    private static Map<String, String> runtimeAttributes(MultiblockRuntimeSnapshot snapshot, Identifier blockId) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("source", "MultiblockMachineCoreAdapter");
        attributes.put("definitionId", definitionId(snapshot).toString());
        attributes.put("controllerBlockId", blockId.toString());
        attributes.put("category", snapshot.category());
        attributes.put("role", snapshot.role().name());
        attributes.put("state", snapshot.state().name());
        attributes.put("integrity", Integer.toString(Math.round(snapshot.integrity())));
        attributes.put("completionPercent", Integer.toString((int) Math.round(snapshot.completion() * 100.0D)));
        attributes.put("matchedBlockCount", Integer.toString(snapshot.matchedBlockCount()));
        attributes.put("roboticComponentCount", Integer.toString(snapshot.roboticComponentCount()));
        attributes.put("taskCount", Integer.toString(snapshot.taskCount()));
        attributes.put("warningCount", Integer.toString(snapshot.warningCount()));
        attributes.put("position", position(snapshot.controllerPos()));
        attributes.put("dimension", snapshot.dimension().identifier().toString());
        attributes.put("facilityTier", Integer.toString(snapshot.facilityTier()));
        attributes.put("facilityStage", snapshot.facilityStage());
        attributes.put("facilityRoute", snapshot.facilityRoute());
        attributes.put("primaryUse", snapshot.primaryUse());
        if (!snapshot.progressionTitle().isBlank()) {
            attributes.put("progressionTitle", snapshot.progressionTitle());
        }
        if (!snapshot.constructionProgress().isBlank()) {
            attributes.put("constructionProgress", snapshot.constructionProgress());
        }
        return Map.copyOf(attributes);
    }

    private static Optional<TaskExecutionSnapshot> currentTask(MultiblockRuntimeSnapshot snapshot) {
        return snapshot.tasks().stream()
                .filter(task -> task.state() == MultiblockTaskState.ACTIVE)
                .findFirst()
                .or(() -> snapshot.tasks().stream().findFirst());
    }

    private static String processStatus(MultiblockRuntimeSnapshot snapshot, Optional<TaskExecutionSnapshot> task) {
        if (task.isPresent()) {
            return task.get().displayName() + " " + task.get().state().name();
        }
        if (snapshot.state() == MultiblockState.FORMED && snapshot.taskCount() <= 0) {
            return "Facility formed";
        }
        return snapshot.state().name();
    }

    private static boolean processActive(MultiblockRuntimeSnapshot snapshot, Optional<TaskExecutionSnapshot> task) {
        if (task.isPresent()) {
            MultiblockTaskState state = task.get().state();
            return state == MultiblockTaskState.ACTIVE || state == MultiblockTaskState.RETRYING;
        }
        return snapshot.state() == MultiblockState.ACTIVE || snapshot.state() == MultiblockState.VALIDATING;
    }

    private static String recipeContract(MultiblockControllerBlockEntity controller, MultiblockRuntimeSnapshot snapshot) {
        return currentTask(snapshot)
                .map(TaskExecutionSnapshot::recipeId)
                .or(() -> controller.progression().stream()
                        .flatMap(progression -> progression.featuredRecipes().stream())
                        .findFirst())
                .or(() -> controller.availableAutomationRecipes().stream()
                        .map(MultiblockAutomationRecipe::id)
                        .findFirst())
                .orElseGet(() -> recipeFallback(snapshot))
                .toString();
    }

    private static Identifier recipeFallback(MultiblockRuntimeSnapshot snapshot) {
        return Identifier.fromNamespaceAndPath(EchoMultiblockCore.MODID, "facility/" + definitionId(snapshot).getPath());
    }

    private static int percent(int value, int max) {
        return max <= 0 ? 0 : Math.max(0, Math.min(100, (int) Math.round(value * 100.0D / max)));
    }

    private static double maintenanceWear(MultiblockRuntimeSnapshot snapshot) {
        return Math.max(0.0D, (100.0D - snapshot.integrity()) / 1000.0D);
    }

    private static String position(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
