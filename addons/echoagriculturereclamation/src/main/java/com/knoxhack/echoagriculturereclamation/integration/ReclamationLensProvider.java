package com.knoxhack.echoagriculturereclamation.integration;

import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoagriculturereclamation.api.ReclamationFieldQuery;
import com.knoxhack.echoagriculturereclamation.api.ReclamationFieldSnapshot;
import com.knoxhack.echoagriculturereclamation.block.HydroponicTrayBlock;
import com.knoxhack.echoagriculturereclamation.block.ReclamationCropBlock;
import com.knoxhack.echoagriculturereclamation.block.ReclamationMachineBlock;
import com.knoxhack.echoagriculturereclamation.block.entity.HydroponicTrayBlockEntity;
import com.knoxhack.echoagriculturereclamation.block.entity.ReclamationMachineBlockEntity;
import com.knoxhack.echoagriculturereclamation.content.SoilState;
import com.knoxhack.echolens.api.LensContext;
import com.knoxhack.echolens.api.LensDataCategory;
import com.knoxhack.echolens.api.LensInfoRow;
import com.knoxhack.echolens.api.LensInfoSection;
import com.knoxhack.echolens.api.LensTone;
import com.knoxhack.echolens.api.LensVisibility;
import com.knoxhack.echolens.api.ServerLensProvider;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public enum ReclamationLensProvider implements ServerLensProvider {
   INSTANCE;

   @Override
   public Identifier id() {
      return Identifier.fromNamespaceAndPath(EchoAgricultureReclamation.MODID, "lens/field_reclamation");
   }

   @Override
   public int priority() {
      return 260;
   }

   @Override
   public LensDataCategory category() {
      return LensDataCategory.MACHINE;
   }

   @Override
   public boolean supports(LensContext context) {
      if (context == null || !context.hasBlock()) {
         return false;
      }
      return context.blockState().getBlock() instanceof ReclamationMachineBlock
         || context.blockState().getBlock() instanceof ReclamationCropBlock
         || context.blockState().getBlock() instanceof HydroponicTrayBlock
         || SoilState.fromBlock(context.blockState()) != SoilState.DEAD;
   }

   @Override
   public List<LensInfoSection> inspect(LensContext context) {
      if (!supports(context)) {
         return List.of();
      }
      ReclamationFieldSnapshot snapshot = ReclamationFieldQuery.at(context.level(), context.blockPos(), context.player());
      List<LensInfoRow> rows = new ArrayList<>();
      rows.add(row("Soil", snapshot.soilState().displayName(), "S", LensTone.INFO, LensVisibility.COMPACT));
      rows.add(row("Restoration", snapshot.restorationScore() + "%", "R", snapshot.restored() ? LensTone.GOOD : LensTone.NEUTRAL, LensVisibility.COMPACT));
      rows.add(row("Greenhouse", snapshot.greenhouseQuality(), "G", "safe".equals(snapshot.greenhouseQuality()) ? LensTone.GOOD : LensTone.WARNING, LensVisibility.COMPACT));
      rows.add(row("Next", snapshot.nextAction(), ">", LensTone.ECHO, LensVisibility.EXPANDED));
      if (context.level().getBlockEntity(context.blockPos()) instanceof ReclamationMachineBlockEntity machine) {
         rows.add(row("Machine", machine.machineKind() == null ? "Unknown" : machine.machineKind().displayName(), "M", LensTone.INFO, LensVisibility.COMPACT));
         rows.add(row("Status", machine.statusLine(), "!", machine.blockedReason().isBlank() ? LensTone.GOOD : LensTone.WARNING, LensVisibility.EXPANDED));
      } else if (context.level().getBlockEntity(context.blockPos()) instanceof HydroponicTrayBlockEntity tray) {
         rows.add(row("Tray", tray.statusLine(), "H", LensTone.INFO, LensVisibility.EXPANDED));
      }
      return List.of(LensInfoSection.of(
         Identifier.fromNamespaceAndPath(EchoAgricultureReclamation.MODID, "lens/field_reclamation/status"),
         LensDataCategory.MACHINE,
         "FIELD Reclamation",
         "F",
         LensTone.ECHO,
         LensVisibility.COMPACT,
         rows
      ));
   }

   @Override
   public List<LensInfoRow> deepScanSignals(LensContext context) {
      if (!supports(context)) {
         return List.of();
      }
      ReclamationFieldSnapshot snapshot = ReclamationFieldQuery.at(context.level(), context.blockPos(), context.player());
      return List.of(
         row("Crop targets", String.valueOf(snapshot.cropTargetCount()), "C", LensTone.INFO, LensVisibility.DEEP),
         row("Drones", snapshot.deployedDroneCount() + " deployed / " + snapshot.serviceTargetCount() + " targets", "D", LensTone.INFO, LensVisibility.DEEP),
         row("Last blocker", snapshot.lastMeaningfulBlocker(), "!", snapshot.lastMeaningfulBlocker().isBlank() ? LensTone.MUTED : LensTone.WARNING, LensVisibility.DEEP)
      );
   }

   private static LensInfoRow row(String label, String value, String icon, LensTone tone, LensVisibility visibility) {
      return LensInfoRow.of(label, value == null || value.isBlank() ? "-" : value, icon, tone, visibility);
   }
}
