package com.knoxhack.echoconvoyprotocol.item;

import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleKind;
import com.knoxhack.echoconvoyprotocol.upgrade.ConvoyUpgradeSlot;
import com.knoxhack.echoconvoyprotocol.upgrade.VehicleUpgradeStats;
import net.minecraft.world.item.Item;

public class VehicleUpgradeItem extends Item {
   private final ConvoyVehicleKind targetKind;
   private final ConvoyUpgradeSlot slot;
   private final VehicleUpgradeStats stats;

   public VehicleUpgradeItem(ConvoyVehicleKind targetKind, ConvoyUpgradeSlot slot, VehicleUpgradeStats stats, Properties properties) {
      super(properties);
      this.targetKind = targetKind;
      this.slot = slot;
      this.stats = stats == null ? VehicleUpgradeStats.NONE : stats;
   }

   public ConvoyVehicleKind targetKind() {
      return targetKind;
   }

   public ConvoyUpgradeSlot slot() {
      return slot;
   }

   public VehicleUpgradeStats stats() {
      return stats;
   }
}
