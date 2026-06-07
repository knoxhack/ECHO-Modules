package com.knoxhack.echopowergrid.integration.lens;

import com.knoxhack.echopowergrid.EchoPowerGrid;
import com.knoxhack.echopowergrid.api.EchoEnergyStorage;
import com.knoxhack.echopowergrid.api.EchoPowerGridApi;
import com.knoxhack.echopowergrid.api.EchoPowerNetwork;
import com.knoxhack.echopowergrid.api.EchoPowerNodeType;
import com.knoxhack.echopowergrid.api.PowerGridNodeSummary;
import com.knoxhack.echopowergrid.api.PowerGridRouteSummary;
import com.knoxhack.echopowergrid.api.PowerGridSnapshot;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.api.ServerLensProvider;
import com.knoxhack.echolens.registry.LensProviderRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PowerGridLensIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private PowerGridLensIntegration() {}

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            LensProviderRegistry.register(Provider.INSTANCE);
        }
    }

    private enum Provider implements ServerLensProvider {
        INSTANCE;

        @Override
        public Identifier id() {
            return Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID, "power_grid_scan");
        }

        @Override
        public int priority() {
            return 80;
        }

        @Override
        public LensDataCategory category() {
            return LensDataCategory.MACHINE;
        }

        @Override
        public boolean supports(LensContext context) {
            if (context == null || !context.hasBlock() || context.level() == null) {
                return false;
            }
            BlockEntity be = context.level().getBlockEntity(context.blockPos());
            return be instanceof EchoEnergyStorage || EchoPowerGridApi.getNetwork(context.level(), context.blockPos()).isPresent();
        }

        @Override
        public List<LensInfoSection> inspect(LensContext context) {
            BlockEntity be = context.level().getBlockEntity(context.blockPos());
            PowerGridSnapshot snap = EchoPowerGridApi.getSnapshot(context.level(), context.blockPos());
            List<LensInfoRow> rows = new ArrayList<>();
            long available = EchoPowerGridApi.getAvailablePower(context.level(), context.blockPos());

            if (be instanceof EchoEnergyStorage storage) {
                rows.add(row("Energy", storage.getEnergyStored() + "/" + storage.getMaxEnergyStored(), "E",
                    storage.getEnergyStored() > 0 ? LensTone.GOOD : LensTone.WARNING, LensVisibility.COMPACT));
                rows.add(row("Input", Long.toString(storage.getMaxInput()), "I", LensTone.INFO, LensVisibility.COMPACT));
                rows.add(row("Output", Long.toString(storage.getMaxOutput()), "O", LensTone.INFO, LensVisibility.COMPACT));
            }

            rows.add(row("Network State", snap.state().toString(), "S", toneForState(snap.state()), LensVisibility.COMPACT));
            rows.add(row("Generation", snap.totalGeneration() + " EP/t", "G", LensTone.INFO, LensVisibility.COMPACT));
            rows.add(row("Demand", snap.totalDemand() + " EP/t", "D", LensTone.INFO, LensVisibility.COMPACT));
            rows.add(row("Stored", snap.totalStored() + "/" + snap.totalCapacity(), "B", LensTone.INFO, LensVisibility.COMPACT));
            rows.add(row("Available", available + " EP", "A",
                available > 0 ? LensTone.GOOD : LensTone.WARNING, LensVisibility.COMPACT));
            rows.add(row("Nodes", Integer.toString(snap.nodeCount()), "N", LensTone.MUTED, LensVisibility.EXPANDED));
            rows.add(row("Quality", snap.quality().toString(), "Q", LensTone.INFO, LensVisibility.EXPANDED));
            if (context.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                EchoPowerGridApi.loadedNodeSummaries(serverLevel).stream()
                        .filter(summary -> summary.pos().equals(context.blockPos()))
                        .findFirst()
                        .ifPresent(summary -> addNodeRows(rows, summary));
                EchoPowerGridApi.alerts(serverLevel).stream()
                        .filter(alert -> alert.pos().equals(context.blockPos()))
                        .findFirst()
                        .ifPresent(alert -> rows.add(row("Alert", alert.code(), "!", LensTone.DANGER, LensVisibility.COMPACT)));
            }

            return List.of(LensInfoSection.of(
                Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID, "lens/power_grid"),
                LensDataCategory.MACHINE,
                "ECHO Power Grid",
                "P",
                toneForState(snap.state()),
                LensVisibility.COMPACT,
                rows
            ));
        }

        private static LensTone toneForState(com.knoxhack.echopowergrid.api.EchoGridState state) {
            return switch (state) {
                case STABLE, CHARGING -> LensTone.GOOD;
                case DISCHARGING -> LensTone.INFO;
                case BROWNOUT -> LensTone.WARNING;
                case OVERLOADED, TRIPPED, EMERGENCY -> LensTone.DANGER;
                default -> LensTone.MUTED;
            };
        }

        private static void addNodeRows(List<LensInfoRow> rows, PowerGridNodeSummary summary) {
            rows.add(row("Node", summary.type().name(), "P", summary.online() ? LensTone.GOOD : LensTone.WARNING,
                    LensVisibility.EXPANDED));
            rows.add(row("Transfer", limit(summary.transferLimit()), "T", LensTone.INFO, LensVisibility.EXPANDED));
            addProgressionRows(rows, summary);
            if (summary.tripped()) {
                rows.add(row("Blocked", "Breaker tripped", "!", LensTone.DANGER, LensVisibility.COMPACT));
            }
        }

        private static void addProgressionRows(List<LensInfoRow> rows, PowerGridNodeSummary summary) {
            if (summary.type() == EchoPowerNodeType.STORAGE && summary.capacity() >= 160000L) {
                rows.add(row("Index", "field_batteries", "I", LensTone.INFO, LensVisibility.EXPANDED));
                rows.add(row("Mission", "grid_commissioning_depth", "M", LensTone.GOOD, LensVisibility.EXPANDED));
            } else if (summary.type() == EchoPowerNodeType.SUBSTATION && summary.transferLimit() >= 3000L) {
                rows.add(row("HoloMap", "factory_substation_anchor", "H", LensTone.GOOD, LensVisibility.EXPANDED));
                rows.add(row("Index", "factory_substations", "I", LensTone.INFO, LensVisibility.EXPANDED));
            } else if (summary.type() == EchoPowerNodeType.CABLE && summary.transferLimit() >= 1200L) {
                rows.add(row("HoloMap", "high_voltage_trunk", "H", LensTone.INFO, LensVisibility.EXPANDED));
                rows.add(row("Index", "high_voltage_distribution", "I", LensTone.INFO, LensVisibility.EXPANDED));
            } else if (summary.type() == EchoPowerNodeType.GENERATOR && summary.localGeneration() >= 30L) {
                rows.add(row("Index", "reinforced_power", "I", LensTone.INFO, LensVisibility.EXPANDED));
            }
            if (summary.type() == EchoPowerNodeType.GENERATOR
                    || summary.type() == EchoPowerNodeType.STORAGE
                    || summary.type() == EchoPowerNodeType.SUBSTATION) {
                rows.add(row("Next Step", "Open Terminal Power Grid tab", ">", LensTone.MUTED,
                        LensVisibility.EXPANDED));
            }
        }

        private static String limit(long value) {
            return value >= Long.MAX_VALUE / 8L ? "unlimited" : value + " EP/t";
        }

        private static LensInfoRow row(String label, String value, String icon, LensTone tone, LensVisibility visibility) {
            return LensInfoRow.of(label, value, icon, tone, visibility);
        }
    }
}
