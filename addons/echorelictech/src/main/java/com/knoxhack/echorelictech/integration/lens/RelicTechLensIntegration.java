package com.knoxhack.echorelictech.integration.lens;

import com.knoxhack.echolens.api.BlockLensProvider;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.api.MachineLensProvider;
import com.knoxhack.echolens.registry.LensProviderRegistry;
import com.knoxhack.echorelictech.EchoRelicTech;
import com.knoxhack.echorelictech.block.entity.ContainmentLockerBlockEntity;
import com.knoxhack.echorelictech.block.entity.NullBatteryDockBlockEntity;
import com.knoxhack.echorelictech.block.entity.PrototypeWorkbenchBlockEntity;
import com.knoxhack.echorelictech.block.entity.RelicAnalyzerBlockEntity;
import com.knoxhack.echorelictech.registry.ModBlocks;
import com.knoxhack.echorelictech.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

public class RelicTechLensIntegration {
    public static void register() {
        EchoRelicTech.LOGGER.info("ECHO Lens integration loaded for RelicTech.");
        try {
            LensProviderRegistry.register(new RelicMachineLensProvider());
        } catch (RuntimeException | LinkageError exception) {
            EchoRelicTech.LOGGER.warn("Lens integration could not fully register.", exception);
        }
    }

    private static final class RelicMachineLensProvider implements BlockLensProvider, MachineLensProvider {
        private static final Identifier ID = Identifier.fromNamespaceAndPath(EchoRelicTech.MODID, "relic_machines");

        @Override
        public Identifier id() {
            return ID;
        }

        @Override
        public int priority() {
            return 300;
        }

        @Override
        public LensDataCategory category() {
            return LensDataCategory.MACHINE;
        }

        @Override
        public List<LensInfoSection> inspectBlock(LensContext context, BlockState state, BlockPos pos) {
            if (context == null || context.level() == null || pos == null || state == null) {
                return List.of();
            }
            List<LensInfoSection> sections = new ArrayList<>();
            var block = state.getBlock();
            var blockEntity = context.level().getBlockEntity(pos);
            if (block == ModBlocks.RELIC_ANALYZER.get() && blockEntity instanceof RelicAnalyzerBlockEntity be) {
                List<LensInfoRow> rows = new ArrayList<>();
                rows.add(row("Status", be.hasOutput() ? "Analysis complete" : be.getInput().isEmpty() ? "Idle" : "Analyzing...", "A"));
                rows.add(row("Input", be.getInput().isEmpty() ? "None" : be.getInput().getHoverName().getString(), "I"));
                sections.add(section("analyzer", "Relic Analyzer", rows));
            } else if (block == ModBlocks.PROTOTYPE_WORKBENCH.get()
                    && blockEntity instanceof PrototypeWorkbenchBlockEntity be) {
                List<LensInfoRow> rows = new ArrayList<>();
                rows.add(row("Relic", be.getRelicSlot().isEmpty() ? "None" : be.getRelicSlot().getHoverName().getString(), "R"));
                rows.add(row("Material", be.getMaterialSlot().isEmpty() ? "None" : be.getMaterialSlot().getHoverName().getString(), "M"));
                sections.add(section("workbench", "Prototype Workbench", rows));
            } else if (block == ModBlocks.CONTAINMENT_LOCKER.get()
                    && blockEntity instanceof ContainmentLockerBlockEntity be) {
                int occupied = 0;
                for (int i = 0; i < be.getContainerSize(); i++) {
                    if (!be.getItem(i).isEmpty()) {
                        occupied++;
                    }
                }
                sections.add(section("locker", "Containment Locker",
                        List.of(row("Occupied", occupied + "/" + be.getContainerSize(), "O"))));
            } else if (block == ModBlocks.NULL_BATTERY_DOCK.get()
                    && blockEntity instanceof NullBatteryDockBlockEntity be) {
                List<LensInfoRow> rows = new ArrayList<>();
                rows.add(row("Battery", be.getBattery().isEmpty() ? "None" : be.getBattery().getHoverName().getString(), "B"));
                rows.add(row("Charge", String.valueOf(be.getBattery().getOrDefault(ModDataComponents.NULL_CHARGE.get(), 0)), "C"));
                sections.add(section("dock", "Null Battery Dock", rows));
            }
            return sections;
        }

        private static LensInfoRow row(String label, String value, String icon) {
            return LensInfoRow.of(label, value, icon, LensTone.INFO, LensVisibility.EXPANDED);
        }

        private static LensInfoSection section(String path, String title, List<LensInfoRow> rows) {
            return LensInfoSection.of(
                    Identifier.fromNamespaceAndPath(EchoRelicTech.MODID, path),
                    LensDataCategory.MACHINE,
                    title,
                    "#",
                    LensTone.INFO,
                    LensVisibility.EXPANDED,
                    rows);
        }
    }
}
