package com.knoxhack.echorelictech.api.event;

import com.knoxhack.echorelictech.api.relic.RelicCondition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class RelicTechEvents {
    private RelicTechEvents() {}

    // Analyze
    private static final List<Consumer<AnalyzeEvent>> ANALYZE_LISTENERS = new ArrayList<>();
    // Stabilize / Workbench action
    private static final List<Consumer<WorkbenchEvent>> WORKBENCH_LISTENERS = new ArrayList<>();
    // Relic use
    private static final List<Consumer<UseEvent>> USE_LISTENERS = new ArrayList<>();
    // Contain
    private static final List<Consumer<ContainEvent>> CONTAIN_LISTENERS = new ArrayList<>();
    // Vault discovery
    private static final List<Consumer<VaultDiscoverEvent>> VAULT_LISTENERS = new ArrayList<>();
    // Failure triggered
    private static final List<Consumer<FailureEvent>> FAILURE_LISTENERS = new ArrayList<>();

    public static void onAnalyze(Consumer<AnalyzeEvent> listener) { ANALYZE_LISTENERS.add(listener); }
    public static void onWorkbench(Consumer<WorkbenchEvent> listener) { WORKBENCH_LISTENERS.add(listener); }
    public static void onUse(Consumer<UseEvent> listener) { USE_LISTENERS.add(listener); }
    public static void onContain(Consumer<ContainEvent> listener) { CONTAIN_LISTENERS.add(listener); }
    public static void onVaultDiscover(Consumer<VaultDiscoverEvent> listener) { VAULT_LISTENERS.add(listener); }
    public static void onFailure(Consumer<FailureEvent> listener) { FAILURE_LISTENERS.add(listener); }

    public static void fireAnalyze(ServerPlayer player, ItemStack unidentified, ItemStack result) {
        var e = new AnalyzeEvent(player, unidentified, result);
        for (var l : new ArrayList<>(ANALYZE_LISTENERS)) l.accept(e);
    }

    public static void fireWorkbench(ServerPlayer player, ItemStack relic, RelicCondition from, RelicCondition to) {
        var e = new WorkbenchEvent(player, relic, from, to);
        for (var l : new ArrayList<>(WORKBENCH_LISTENERS)) l.accept(e);
    }

    public static void fireUse(ServerPlayer player, Identifier relicId, ItemStack stack) {
        var e = new UseEvent(player, relicId, stack);
        for (var l : new ArrayList<>(USE_LISTENERS)) l.accept(e);
    }

    public static void fireContain(ServerPlayer player, ItemStack relic) {
        var e = new ContainEvent(player, relic);
        for (var l : new ArrayList<>(CONTAIN_LISTENERS)) l.accept(e);
    }

    public static void fireVaultDiscover(ServerPlayer player, Identifier vaultId, BlockPos pos) {
        var e = new VaultDiscoverEvent(player, vaultId, pos);
        for (var l : new ArrayList<>(VAULT_LISTENERS)) l.accept(e);
    }

    public static void fireFailure(ServerPlayer player, Identifier relicId, String severity) {
        var e = new FailureEvent(player, relicId, severity);
        for (var l : new ArrayList<>(FAILURE_LISTENERS)) l.accept(e);
    }

    public record AnalyzeEvent(ServerPlayer player, ItemStack unidentified, ItemStack result) {}
    public record WorkbenchEvent(ServerPlayer player, ItemStack relic, RelicCondition fromCondition, RelicCondition toCondition) {}
    public record UseEvent(ServerPlayer player, Identifier relicId, ItemStack stack) {}
    public record ContainEvent(ServerPlayer player, ItemStack relic) {}
    public record VaultDiscoverEvent(ServerPlayer player, Identifier vaultId, BlockPos pos) {}
    public record FailureEvent(ServerPlayer player, Identifier relicId, String severity) {}
}
