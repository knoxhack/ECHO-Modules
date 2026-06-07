package com.knoxhack.echoarmory.block.entity;

import com.knoxhack.echoarmory.api.ArmoryReports;
import com.knoxhack.echoarmory.api.StationOperationPreview;
import com.knoxhack.echoarmory.block.ArmoryStationBlock;
import com.knoxhack.echoarmory.block.ArmoryStationBlock.StationKind;
import com.knoxhack.echoarmory.data.ArmoryLoadout;
import com.knoxhack.echoarmory.data.CosmeticTrim;
import com.knoxhack.echoarmory.data.EnergyState;
import com.knoxhack.echoarmory.data.InstabilityState;
import com.knoxhack.echoarmory.integration.ArmoryMissionHooks;
import com.knoxhack.echoarmory.item.ArmoryData;
import com.knoxhack.echoarmory.item.ArmoryGearItem;
import com.knoxhack.echoarmory.menu.ArmoryStationMenu;
import com.knoxhack.echoarmory.registry.ModBlockEntities;
import com.knoxhack.echoarmory.registry.ModDataComponents;
import com.knoxhack.echoarmory.registry.ModItems;
import com.knoxhack.echoarmory.service.ArmoryReadinessService;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class ArmoryStationBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
   public static final int SLOT_COUNT = 12;
   public static final int GEAR_SLOT = 0;
   public static final int MODULE_SLOT = 1;
   public static final int AUX_SLOT = 2;
   public static final int DATA_KIND = 0;
   public static final int DATA_PROGRESS = 1;
   public static final int DATA_ENERGY = 2;
   public static final int DATA_MODULES = 3;
   public static final int DATA_INSTABILITY = 4;
   public static final int DATA_ENERGY_CAPACITY = 5;
   public static final int DATA_PREVIEW_ACCEPTED = 6;
   public static final int DATA_FUEL_COUNT = 7;
   public static final int DATA_COUNT = 8;
   private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
   private final ContainerData data = new ContainerData() {
      @Override
      public int get(int index) {
         ItemStack gear = items.get(GEAR_SLOT);
         return switch (index) {
            case DATA_KIND -> kind().ordinal();
            case DATA_PROGRESS -> progress;
            case DATA_ENERGY -> gear.getOrDefault(ModDataComponents.ENERGY_STATE.get(), com.knoxhack.echoarmory.data.EnergyState.EMPTY).stored();
            case DATA_MODULES -> ArmoryData.modules(gear).modules().size();
            case DATA_INSTABILITY -> gear.getOrDefault(ModDataComponents.INSTABILITY_STATE.get(), InstabilityState.STABLE).instability();
            case DATA_ENERGY_CAPACITY -> gear.getOrDefault(ModDataComponents.ENERGY_STATE.get(), EnergyState.EMPTY).capacity();
            case DATA_PREVIEW_ACCEPTED -> preview().accepted() ? 1 : 0;
            case DATA_FUEL_COUNT -> preview().fuelCount();
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
   private int progress;
   private String lastAction = "Idle";

   public ArmoryStationBlockEntity(BlockPos pos, BlockState blockState) {
      super(ModBlockEntities.ARMORY_STATION.get(), pos, blockState);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, ArmoryStationBlockEntity entity) {
      if (!level.isClientSide() && entity.progress > 0) {
         entity.progress = Math.max(0, entity.progress - 1);
         entity.setChanged();
      }
   }

   public StationKind kind() {
      return getBlockState().getBlock() instanceof ArmoryStationBlock block ? block.kind() : StationKind.ARMORY_BENCH;
   }

   public boolean handleMenuButton(Player player, int id) {
      if (!(player instanceof ServerPlayer serverPlayer)) {
         return false;
      }
      return switch (id) {
         case ArmoryStationMenu.BUTTON_SCAN -> scan(serverPlayer);
         case ArmoryStationMenu.BUTTON_APPLY -> apply(serverPlayer);
         case ArmoryStationMenu.BUTTON_CYCLE -> cycle(serverPlayer);
         default -> false;
      };
   }

   private boolean scan(ServerPlayer player) {
      ItemStack gear = items.get(GEAR_SLOT);
      if (!gear.isEmpty()) {
         ArmoryData.initialize(gear);
      }
      ArmoryMissionHooks.recordInspectLoadout(player, kind().getSerializedName());
      player.sendSystemMessage(Component.literal("ECHO ARMORY // " + kind().displayName() + " scan: " + statusLine()));
      return true;
   }

   private boolean apply(ServerPlayer player) {
      ItemStack gear = items.get(GEAR_SLOT);
      if (gear.isEmpty()) {
         player.sendSystemMessage(Component.literal("ECHO ARMORY // Insert gear in the first slot."));
         return true;
      }
      ArmoryData.initialize(gear);
      if (!ArmoryData.factionGateSatisfied(player, gear)) {
         ArmoryData.gear(gear).ifPresent(definition -> player.sendSystemMessage(Component.literal("ECHO ARMORY // " + ArmoryData.factionGateLine(definition))));
         return true;
      }
      StationOperationPreview preview = preview();
      if (!preview.accepted() && kind() != StationKind.ARMORY_BENCH) {
         player.sendSystemMessage(Component.literal("ECHO ARMORY // " + preview.blocker()));
         return true;
      }
      boolean changed = switch (kind()) {
         case MODULE_UPGRADE_TABLE, VEIL_INFUSER, CONSTRUCT_DOCK -> installModule(player, gear, items.get(MODULE_SLOT));
         case ENERGY_CORE_CHARGING_STATION -> recharge(player, gear);
         case ARMORY_BENCH -> repair(player, gear);
         case WEAPON_FORGE -> upgrade(player, gear, true);
         case ARMOR_FORGE -> upgrade(player, gear, false);
         case SIGIL_ENGRAVER -> engrave(player, gear);
         case LOADOUT_TERMINAL -> bindLoadout(player, gear);
         case WEAPON_RACK, ARMOR_STAND -> storeOnly(player, gear);
      };
      if (changed) {
         progress = 80;
         setChanged();
      }
      return true;
   }

   private boolean cycle(ServerPlayer player) {
      ItemStack gear = items.get(GEAR_SLOT);
      if (gear.isEmpty()) {
         return true;
      }
      ArmoryData.initialize(gear);
      com.knoxhack.echoarmory.data.ArmoryStance stance = gear.getOrDefault(ModDataComponents.STANCE.get(), com.knoxhack.echoarmory.data.ArmoryStance.BALANCED).next();
      gear.set(ModDataComponents.STANCE.get(), stance);
      player.sendSystemMessage(Component.literal("ECHO ARMORY // Stance set to " + stance.label() + "."));
      setChanged();
      return true;
   }

   private boolean installModule(ServerPlayer player, ItemStack gear, ItemStack module) {
      if (!(module.getItem() instanceof ArmoryGearItem gearItem) || gearItem.gearKind() != ArmoryGearItem.ArmoryGearKind.MODULE) {
         player.sendSystemMessage(Component.literal("ECHO ARMORY // Insert a compatible module in the second slot."));
         return false;
      }
      boolean installed = ArmoryData.installModule(player, gear, module);
      player.sendSystemMessage(Component.literal(installed
         ? "ECHO ARMORY // Module installed. " + statusLine()
         : "ECHO ARMORY // Module rejected: duplicate, incompatible, locked, or no open slot."));
      if (installed) {
         lastAction = "module install";
         ArmoryMissionHooks.recordInstallModule(player, kind().getSerializedName());
         recordReadyKit(player);
      }
      return installed;
   }

   private boolean recharge(ServerPlayer player, ItemStack gear) {
      if (ArmoryData.rechargeWithFuel(gear, items.get(AUX_SLOT))) {
         lastAction = "recharge";
         ArmoryMissionHooks.recordRechargeCore(player, kind().getSerializedName());
         recordReadyKit(player);
         player.sendSystemMessage(Component.literal("ECHO ARMORY // Energy core charged using AUX reserve. " + statusLine()));
         return true;
      }
      player.sendSystemMessage(Component.literal("ECHO ARMORY // Recharge requires damaged energy gear and one Veil Crystal or Resonance Shard in AUX."));
      return false;
   }

   private boolean repair(ServerPlayer player, ItemStack gear) {
      if (ArmoryData.repairWithPlate(gear, items.get(AUX_SLOT))) {
         lastAction = "repair";
         player.sendSystemMessage(Component.literal("ECHO ARMORY // Gear repaired with Armory Alloy Plate. " + statusLine()));
         return true;
      }
      ArmoryData.initialize(gear);
      lastAction = "tune";
      player.sendSystemMessage(Component.literal("ECHO ARMORY // Gear tuned. Add damaged Armory gear and an Armory Alloy Plate in AUX to repair."));
      return true;
   }

   private boolean upgrade(ServerPlayer player, ItemStack gear, boolean weaponStation) {
      ArmoryData.gear(gear).ifPresent(definition -> {
         ArmoryGearItem.ArmoryGearKind expected = weaponStation ? ArmoryGearItem.ArmoryGearKind.WEAPON : ArmoryGearItem.ArmoryGearKind.ARMOR;
         if (gear.getItem() instanceof ArmoryGearItem gearItem && gearItem.gearKind() != expected) {
            player.sendSystemMessage(Component.literal("ECHO ARMORY // " + (weaponStation ? "Weapon Forge" : "Armor Forge") + " cannot upgrade " + definition.title() + "."));
         }
      });
      if (!(gear.getItem() instanceof ArmoryGearItem gearItem)
         || gearItem.gearKind() != (weaponStation ? ArmoryGearItem.ArmoryGearKind.WEAPON : ArmoryGearItem.ArmoryGearKind.ARMOR)) {
         return false;
      }
      if (ArmoryData.upgradeTier(gear, items.get(AUX_SLOT), weaponStation)) {
         lastAction = "tier upgrade";
         ArmoryMissionHooks.recordForgeUpgrade(player, kind().getSerializedName());
         recordReadyKit(player);
         player.sendSystemMessage(Component.literal("ECHO ARMORY // Gear tier upgraded. " + statusLine()));
         return true;
      }
      player.sendSystemMessage(Component.literal("ECHO ARMORY // Upgrade requires AUX material: Tier 1 weapon uses Resonance Shard, Tier 1 armor uses Alloy Plate, Tier 2 uses Veil Crystal, Tier 3 uses Blackbox Fragment."));
      return false;
   }

   private boolean engrave(ServerPlayer player, ItemStack gear) {
      String sigil = items.get(AUX_SLOT).isEmpty() ? kind().getSerializedName() : ArmoryData.displayId(items.get(AUX_SLOT));
      int color = sigil.hashCode() | 0xFF000000;
      gear.set(ModDataComponents.COSMETIC_TRIM.get(), new CosmeticTrim(sigil, color));
      lastAction = "engrave";
      player.sendSystemMessage(Component.literal("ECHO ARMORY // Sigil engraved: " + sigil + "."));
      return true;
   }

   private boolean bindLoadout(ServerPlayer player, ItemStack gear) {
      java.util.Optional<ArmoryReadinessService.Report> selected = ArmoryReadinessService.bestReport(player);
      ArmoryLoadout marker = selected
         .map(report -> new ArmoryLoadout(report.loadout().id().toString(), report.loadout().title()))
         .orElseGet(() -> new ArmoryLoadout("manual:" + player.getUUID().toString().substring(0, 8), player.getScoreboardName() + " field kit"));
      gear.set(ModDataComponents.ARMORY_LOADOUT.get(), marker);
      lastAction = "loadout bind";
      ArmoryMissionHooks.recordBindLoadout(player, kind().getSerializedName());
      selected.ifPresent(report -> {
         if (report.ready()) {
            ArmoryMissionHooks.recordPrepareRouteKit(player, report.loadout().id().toString());
         }
      });
      player.sendSystemMessage(Component.literal(selected
         .map(report -> "ECHO ARMORY // Gear bound to " + report.loadout().title() + ". " + report.state() + ": " + report.firstBlocker())
         .orElse("ECHO ARMORY // Gear bound to current operator loadout.")));
      return true;
   }

   private boolean storeOnly(ServerPlayer player, ItemStack gear) {
      ArmoryData.initialize(gear);
      lastAction = "storage scan";
      player.sendSystemMessage(Component.literal("ECHO ARMORY // Display storage scan complete. This station stores gear but does not transform it."));
      return true;
   }

   public String statusLine() {
      ItemStack gear = items.get(GEAR_SLOT);
      if (gear.isEmpty()) {
         return kind().displayName() + " idle. Insert gear, then apply modules or energy.";
      }
      StationOperationPreview preview = preview();
      return ArmoryData.displayId(gear)
         + " | modules " + ArmoryData.modules(gear).modules().size()
         + " | energy " + data.get(DATA_ENERGY)
         + " | instability " + data.get(DATA_INSTABILITY)
         + " | preview " + (preview.accepted() ? preview.result() : preview.blocker())
         + " | last " + lastAction;
   }

   public StationOperationPreview preview() {
      return ArmoryReports.stationPreview(kind(), items.get(GEAR_SLOT), items.get(MODULE_SLOT), items.get(AUX_SLOT));
   }

   private static void recordReadyKit(ServerPlayer player) {
      ArmoryReadinessService.bestReport(player)
         .filter(ArmoryReadinessService.Report::ready)
         .ifPresent(report -> ArmoryMissionHooks.recordPrepareRouteKit(player, report.loadout().id().toString()));
   }

   @Override
   protected Component getDefaultName() {
      return Component.literal("ECHO " + kind().displayName());
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return items;
   }

   @Override
   protected void setItems(NonNullList<ItemStack> replacement) {
      for (int i = 0; i < Math.min(items.size(), replacement.size()); i++) {
         items.set(i, replacement.get(i));
      }
   }

   @Override
   public int getContainerSize() {
      return items.size();
   }

   @Override
   public boolean canPlaceItem(int slot, ItemStack stack) {
      if (slot == GEAR_SLOT) {
         return stack.getItem() instanceof ArmoryGearItem gearItem
            && gearItem.gearKind() != ArmoryGearItem.ArmoryGearKind.MODULE;
      }
      if (slot == MODULE_SLOT) {
         return stack.getItem() instanceof ArmoryGearItem gearItem
            && gearItem.gearKind() == ArmoryGearItem.ArmoryGearKind.MODULE;
      }
      if (slot == AUX_SLOT) {
         return ArmoryData.isRechargeFuel(stack)
            || stack.is(ModItems.ARMORY_ALLOY_PLATE.get())
            || stack.is(ModItems.BLACKBOX_FRAGMENT.get());
      }
      return true;
   }

   @Override
   public int[] getSlotsForFace(Direction side) {
      int[] slots = new int[items.size()];
      for (int i = 0; i < slots.length; i++) {
         slots[i] = i;
      }
      return slots;
   }

   @Override
   public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
      return canPlaceItem(slot, stack);
   }

   @Override
   public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
      if (progress > 0 && (slot == GEAR_SLOT || slot == MODULE_SLOT || slot == AUX_SLOT)) {
         return false;
      }
      return true;
   }

   @Override
   protected @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
      return new ArmoryStationMenu(containerId, inventory, this, data);
   }

   public ContainerData data() {
      return data;
   }

   public boolean isOperationActive() {
      return progress > 0;
   }

   public boolean isProtectedOperationSlot(int slot) {
      return slot == GEAR_SLOT || slot == MODULE_SLOT || slot == AUX_SLOT;
   }

   @Override
   public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
      buffer.writeBlockPos(getBlockPos());
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      ContainerHelper.loadAllItems(input, items);
      progress = input.getIntOr("progress", 0);
      lastAction = input.getStringOr("last_action", "Idle");
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      ContainerHelper.saveAllItems(output, items);
      output.putInt("progress", progress);
      output.putString("last_action", lastAction == null ? "Idle" : lastAction);
   }
}
