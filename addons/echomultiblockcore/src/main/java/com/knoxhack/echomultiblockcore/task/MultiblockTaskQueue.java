package com.knoxhack.echomultiblockcore.task;

import com.knoxhack.echomultiblockcore.Config;
import com.knoxhack.echomultiblockcore.api.AutomationRecipeRegistry;
import com.knoxhack.echomultiblockcore.api.MultiblockAutomationRecipe;
import com.knoxhack.echomultiblockcore.api.MultiblockTaskState;
import com.knoxhack.echomultiblockcore.api.TaskExecutionSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class MultiblockTaskQueue {
    public static final int MAX_PERSISTED_TASKS = 8;
    public static final int MAX_CONFIGURED_TASKS = 32;
    private final List<QueuedMultiblockTask> tasks = new ArrayList<>();

    public Optional<QueuedMultiblockTask> enqueue(Identifier taskId, long gameTime) {
        if (taskId == null || tasks.size() >= capacity()) {
            return Optional.empty();
        }
        QueuedMultiblockTask task = new QueuedMultiblockTask(taskId, gameTime);
        tasks.add(task);
        return Optional.of(task);
    }

    public Optional<QueuedMultiblockTask> active() {
        return tasks.stream().filter(task -> task.state() == MultiblockTaskState.ACTIVE).findFirst();
    }

    public Optional<QueuedMultiblockTask> nextRunnable() {
        return tasks.stream()
                .filter(task -> task.state() == MultiblockTaskState.WAITING
                        || task.state() == MultiblockTaskState.BLOCKED)
                .findFirst();
    }

    public boolean hasActiveTask() {
        return active().isPresent();
    }

    public boolean hasBlockedTask() {
        return tasks.stream().anyMatch(task -> task.state() == MultiblockTaskState.BLOCKED);
    }

    public int size() {
        return tasks.size();
    }

    public int capacity() {
        return configuredCapacity();
    }

    public int remainingCapacity() {
        return Math.max(0, capacity() - tasks.size());
    }

    public void clear() {
        tasks.clear();
    }

    public void pauseAll(String reason) {
        tasks.stream()
                .filter(task -> task.state() == MultiblockTaskState.WAITING
                        || task.state() == MultiblockTaskState.BLOCKED
                        || task.state() == MultiblockTaskState.ACTIVE)
                .forEach(task -> task.pause(reason));
    }

    public void resumePaused() {
        tasks.stream()
                .filter(task -> task.state() == MultiblockTaskState.PAUSED)
                .forEach(QueuedMultiblockTask::resume);
    }

    public void retryBlocked() {
        tasks.stream()
                .filter(task -> task.state() == MultiblockTaskState.BLOCKED)
                .forEach(QueuedMultiblockTask::resetForRetry);
    }

    public List<QueuedMultiblockTask> tasks() {
        return List.copyOf(tasks);
    }

    public List<TaskExecutionSnapshot> snapshots() {
        return tasks.stream()
                .map(task -> AutomationRecipeRegistry.byId(task.taskId())
                        .map(task::snapshot)
                        .orElseGet(() -> task.snapshot(task.taskId().toString(), "missing")))
                .toList();
    }

    public String statusLine() {
        if (tasks.isEmpty()) {
            return "Idle";
        }
        QueuedMultiblockTask task = active()
                .or(() -> tasks.stream().filter(candidate -> candidate.state() == MultiblockTaskState.BLOCKED).findFirst())
                .orElse(tasks.get(0));
        return task.taskId().getPath() + " / " + task.state() + " / " + task.progressTicks() + "/" + task.durationTicks()
                + (task.blockedReason().isBlank() ? "" : " / " + task.blockedReason());
    }

    public void pruneCompleted() {
        tasks.removeIf(task -> task.state() == MultiblockTaskState.COMPLETED || task.state() == MultiblockTaskState.FAILED);
    }

    public void load(ValueInput input) {
        tasks.clear();
        int size = Math.min(MAX_CONFIGURED_TASKS, Math.max(0, input.getIntOr("task_queue_size", 0)));
        for (int i = 0; i < size; i++) {
            Identifier id = Identifier.tryParse(input.getStringOr("task_" + i + "_id", ""));
            if (id == null) {
                continue;
            }
            tasks.add(new QueuedMultiblockTask(
                    id,
                    enumOr(input.getStringOr("task_" + i + "_state", MultiblockTaskState.WAITING.name()), MultiblockTaskState.WAITING),
                    input.getIntOr("task_" + i + "_progress", 0),
                    input.getIntOr("task_" + i + "_duration", 1),
                    BlockPos.of(input.getLongOr("task_" + i + "_robot", 0L)),
                    Identifier.tryParse(input.getStringOr("task_" + i + "_robot_id", "")),
                    Identifier.tryParse(input.getStringOr("task_" + i + "_workcell_id", "")),
                    input.getStringOr("task_" + i + "_blocked_reason", ""),
                    input.getIntOr("task_" + i + "_retry_count", 0),
                    input.getBooleanOr("task_" + i + "_inputs_consumed", false),
                    input.getStringOr("task_" + i + "_input_summary", ""),
                    input.getStringOr("task_" + i + "_output_summary", ""),
                    input.getStringOr("task_" + i + "_effect_diagnostic", ""),
                    input.getLongOr("task_" + i + "_created", 0L)));
        }
        if (tasks.isEmpty()) {
            loadLegacy(input);
        }
    }

    public void save(ValueOutput output) {
        int size = Math.min(MAX_CONFIGURED_TASKS, tasks.size());
        output.putInt("task_queue_size", size);
        for (int i = 0; i < size; i++) {
            QueuedMultiblockTask task = tasks.get(i);
            output.putString("task_" + i + "_id", task.taskId().toString());
            output.putString("task_" + i + "_state", task.state().name());
            output.putInt("task_" + i + "_progress", task.progressTicks());
            output.putInt("task_" + i + "_duration", task.durationTicks());
            output.putLong("task_" + i + "_robot", task.robotPos().asLong());
            output.putString("task_" + i + "_robot_id", task.robotId() == null ? "" : task.robotId().toString());
            output.putString("task_" + i + "_workcell_id", task.workcellId() == null ? "" : task.workcellId().toString());
            output.putString("task_" + i + "_blocked_reason", task.blockedReason());
            output.putInt("task_" + i + "_retry_count", task.retryCount());
            output.putBoolean("task_" + i + "_inputs_consumed", task.inputsConsumed());
            output.putString("task_" + i + "_input_summary", task.inputSummary());
            output.putString("task_" + i + "_output_summary", task.outputSummary());
            output.putString("task_" + i + "_effect_diagnostic", task.effectDiagnostic());
            output.putLong("task_" + i + "_created", task.createdGameTime());
        }
    }

    private void loadLegacy(ValueInput input) {
        Identifier id = Identifier.tryParse(input.getStringOr("queued_task", ""));
        if (id == null) {
            return;
        }
        tasks.add(new QueuedMultiblockTask(
                id,
                enumOr(input.getStringOr("task_state", MultiblockTaskState.WAITING.name()), MultiblockTaskState.WAITING),
                input.getIntOr("task_progress", 0),
                input.getIntOr("task_duration", 1),
                BlockPos.of(input.getLongOr("assigned_robot", 0L)),
                input.getStringOr("task_blocked_reason", ""),
                0,
                0L));
    }

    private static int configuredCapacity() {
        try {
            return Math.max(1, Math.min(MAX_CONFIGURED_TASKS, Config.TASK_QUEUE_CAPACITY.get()));
        } catch (RuntimeException exception) {
            return MAX_PERSISTED_TASKS;
        }
    }

    private static MultiblockTaskState enumOr(String raw, MultiblockTaskState fallback) {
        try {
            return MultiblockTaskState.valueOf(raw == null ? "" : raw.strip().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
