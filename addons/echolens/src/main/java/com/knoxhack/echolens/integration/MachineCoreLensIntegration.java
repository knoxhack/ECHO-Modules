package com.knoxhack.echolens.integration;

import com.knoxhack.echo.machinecore.EchoMachineRuntimeRegistry;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeSnapshot;
import com.knoxhack.echo.machinecore.EchoMachineUiBridge;
import com.knoxhack.echolens.EchoLens;
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

public final class MachineCoreLensIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private MachineCoreLensIntegration() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            LensProviderRegistry.register(Provider.INSTANCE);
        }
    }

    private enum Provider implements ServerLensProvider {
        INSTANCE;

        @Override
        public Identifier id() {
            return EchoLens.id("machinecore_runtime");
        }

        @Override
        public int priority() {
            return 90;
        }

        @Override
        public LensDataCategory category() {
            return LensDataCategory.MACHINE;
        }

        @Override
        public boolean supports(LensContext context) {
            return context != null
                    && context.hasBlock()
                    && context.level() != null
                    && EchoMachineRuntimeRegistry.snapshot(context.level(), context.blockPos()).isPresent();
        }

        @Override
        public List<LensInfoSection> inspect(LensContext context) {
            return EchoMachineRuntimeRegistry.snapshot(context.level(), context.blockPos())
                    .map(Provider::section)
                    .map(List::of)
                    .orElseGet(List::of);
        }

        private static LensInfoSection section(EchoMachineRuntimeSnapshot snapshot) {
            LensTone tone = tone(snapshot);
            List<LensInfoRow> rows = new ArrayList<>();
            rows.add(row("Machine", snapshot.displayName(), "M", tone, LensVisibility.COMPACT));
            rows.add(row("State", snapshot.state().name(), "S", tone, LensVisibility.COMPACT));
            rows.add(row("Energy", EchoMachineUiBridge.energyLine(snapshot), "E",
                    snapshot.energy().stored() > 0 ? LensTone.GOOD : LensTone.MUTED, LensVisibility.COMPACT));
            rows.add(row("Process", EchoMachineUiBridge.processLine(snapshot), "P",
                    snapshot.process().active() ? LensTone.ECHO : LensTone.MUTED, LensVisibility.EXPANDED));
            rows.add(row("Inventory", EchoMachineUiBridge.inventoryLine(snapshot), "I",
                    snapshot.inventory().occupiedSlots() > 0 ? LensTone.INFO : LensTone.MUTED, LensVisibility.EXPANDED));
            rows.add(row("Fluids", EchoMachineUiBridge.fluidLine(snapshot), "F",
                    snapshot.fluids().supported() ? LensTone.INFO : LensTone.MUTED, LensVisibility.EXPANDED));
            rows.add(row("Sides", EchoMachineUiBridge.sideLine(snapshot), "R", LensTone.INFO, LensVisibility.EXPANDED));
            rows.add(row("Upgrades", EchoMachineUiBridge.upgradeLine(snapshot), "U",
                    snapshot.upgrades().installedCount() > 0 ? LensTone.GOOD : LensTone.MUTED, LensVisibility.EXPANDED));
            rows.add(row("Owner", snapshot.ownerModule().value(), "O", LensTone.MUTED, LensVisibility.DEEP));
            rows.add(row("Block", snapshot.machineBlockId(), "B", LensTone.MUTED, LensVisibility.DEEP));
            rows.add(row("Integrations", Integer.toString(snapshot.integrationRefs().optionalFeatures().size()), "L",
                    LensTone.ECHO, LensVisibility.DEEP));
            return LensInfoSection.of(EchoLens.id("section/machinecore_runtime"), LensDataCategory.MACHINE,
                    "MachineCore Runtime", "M", tone, LensVisibility.COMPACT, rows);
        }

        private static LensInfoRow row(String label, String value, String icon, LensTone tone, LensVisibility visibility) {
            return LensInfoRow.of(label, value, icon, tone, visibility);
        }

        private static LensTone tone(EchoMachineRuntimeSnapshot snapshot) {
            if (snapshot.degraded()) {
                return LensTone.WARNING;
            }
            return switch (snapshot.state()) {
                case ACTIVE -> LensTone.ECHO;
                case IDLE -> LensTone.INFO;
                case OFFLINE, POWER_STARVED, PAUSED -> LensTone.MUTED;
                case DAMAGED, JAMMED, OVERLOADED, MAINTENANCE_REQUIRED -> LensTone.WARNING;
                case LOCKED, UNKNOWN -> LensTone.NEUTRAL;
            };
        }
    }
}
