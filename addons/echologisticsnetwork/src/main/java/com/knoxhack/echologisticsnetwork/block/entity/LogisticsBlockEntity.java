package com.knoxhack.echologisticsnetwork.block.entity;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echologisticsnetwork.Config;
import com.knoxhack.echologisticsnetwork.block.LogisticsBlock;
import com.knoxhack.echologisticsnetwork.block.LogisticsBlock.LogisticsKind;
import com.knoxhack.echologisticsnetwork.content.FactionDepotOffer;
import com.knoxhack.echologisticsnetwork.content.LogisticsContent;
import com.knoxhack.echologisticsnetwork.content.LoadoutPreset;
import com.knoxhack.echologisticsnetwork.content.SupplyCategory;
import com.knoxhack.echologisticsnetwork.integration.LogisticsMissionHooks;
import com.knoxhack.echologisticsnetwork.menu.LogisticsMenu;
import com.knoxhack.echologisticsnetwork.registry.ModBlockEntities;
import com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService;
import com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService.LogisticsSnapshot;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
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

public class LogisticsBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
   public static final int SLOT_COUNT = 27;
   public static final int DATA_KIND = 0;
   public static final int DATA_STOCK_ROWS = 1;
   public static final int DATA_MISSING_ROWS = 2;
   public static final int DATA_READY_ROWS = 3;
   public static final int DATA_ACTIVE_DELIVERIES = 4;
   public static final int DATA_DEPOT_OFFERS = 5;
   public static final int DATA_COOLDOWN = 6;
   public static final int DATA_REWARD_COUNT = 7;
   public static final int DATA_BLOCK_COUNT = 8;
   public static final int DATA_ENDPOINT_COUNT = 9;
   public static final int DATA_DOCK_ONLINE = 10;
   public static final int DATA_RELAY_ONLINE = 11;
   public static final int DATA_DEPOT_ONLINE = 12;
   public static final int DATA_DEPOT_COOLDOWN = 13;
   public static final int DATA_SELECTED_READY = 14;
   public static final int DATA_SELECTED_MISSING = 15;
   public static final int DATA_CAN_REQUEST = 16;
   public static final int DATA_CAN_DISPATCH = 17;
   public static final int DATA_SELECTED_ENDPOINT_KIND = 18;
   public static final int DATA_FIRST_DELIVERY_ETA = 19;
   public static final int DATA_FIRST_DELIVERY_STATUS = 20;
   public static final int DATA_COUNT = 21;

   private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
   private final ContainerData data = new ContainerData() {
      @Override
      public int get(int index) {
         return switch (index) {
            case DATA_KIND -> LogisticsBlockEntity.this.kind().ordinal();
            case DATA_STOCK_ROWS -> LogisticsBlockEntity.this.lastStockRows;
            case DATA_MISSING_ROWS -> LogisticsBlockEntity.this.lastMissingRows;
            case DATA_READY_ROWS -> LogisticsBlockEntity.this.lastReadyRows;
            case DATA_ACTIVE_DELIVERIES -> LogisticsBlockEntity.this.lastActiveDeliveries;
            case DATA_DEPOT_OFFERS -> LogisticsBlockEntity.this.lastDepotOffers;
            case DATA_COOLDOWN -> LogisticsBlockEntity.this.cooldown;
            case DATA_REWARD_COUNT -> LogisticsBlockEntity.this.lastRewardCount;
            case DATA_BLOCK_COUNT -> LogisticsBlockEntity.this.lastBlockCount;
            case DATA_ENDPOINT_COUNT -> LogisticsBlockEntity.this.lastEndpointCount;
            case DATA_DOCK_ONLINE -> LogisticsBlockEntity.this.lastDockOnline ? 1 : 0;
            case DATA_RELAY_ONLINE -> LogisticsBlockEntity.this.lastRelayOnline ? 1 : 0;
            case DATA_DEPOT_ONLINE -> LogisticsBlockEntity.this.lastDepotOnline ? 1 : 0;
            case DATA_DEPOT_COOLDOWN -> LogisticsBlockEntity.this.lastDepotCooldown;
            case DATA_SELECTED_READY -> LogisticsBlockEntity.this.lastSelectedReady ? 1 : 0;
            case DATA_SELECTED_MISSING -> LogisticsBlockEntity.this.lastSelectedMissing;
            case DATA_CAN_REQUEST -> LogisticsBlockEntity.this.lastCanRequest ? 1 : 0;
            case DATA_CAN_DISPATCH -> LogisticsBlockEntity.this.lastCanDispatch ? 1 : 0;
            case DATA_SELECTED_ENDPOINT_KIND -> LogisticsBlockEntity.this.lastSelectedEndpointKind;
            case DATA_FIRST_DELIVERY_ETA -> LogisticsBlockEntity.this.lastFirstDeliveryEta;
            case DATA_FIRST_DELIVERY_STATUS -> LogisticsBlockEntity.this.lastFirstDeliveryStatus;
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

   private String networkId = "global";
   private String categoryId = "";
   private String loadoutId = "";
   private String depotOfferId = "";
   private int cooldown;
   private int restockCooldown;
   private int lastStockRows;
   private int lastMissingRows;
   private int lastReadyRows;
   private int lastActiveDeliveries;
   private int lastDepotOffers;
   private int lastRewardCount;
   private int lastBlockCount;
   private int lastEndpointCount;
   private boolean lastDockOnline;
   private boolean lastRelayOnline;
   private boolean lastDepotOnline;
   private int lastDepotCooldown;
   private boolean lastSelectedReady;
   private int lastSelectedMissing;
   private boolean lastCanRequest;
   private boolean lastCanDispatch;
   private int lastSelectedEndpointKind = -1;
   private int lastFirstDeliveryEta;
   private int lastFirstDeliveryStatus;
   private String lastManifest = "Idle";
   private UUID ownerUuid;

   public LogisticsBlockEntity(BlockPos pos, BlockState blockState) {
      super(ModBlockEntities.LOGISTICS.get(), pos, blockState);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, LogisticsBlockEntity entity) {
      if (level.isClientSide()) {
         return;
      }
      if (entity.cooldown > 0) {
         entity.cooldown--;
      }
      if (entity.restockCooldown > 0) {
         entity.restockCooldown--;
      }
      if (entity.kind() == LogisticsKind.AUTO_RESTOCK_STATION && entity.restockCooldown <= 0) {
         entity.restockCooldown = 100;
         entity.tryAutoRestock();
      }
      int refreshTicks = Config.backgroundRefreshTicks();
      long stagger = backgroundRefreshStagger(pos, refreshTicks);
      if (Math.floorMod(level.getGameTime() + stagger, refreshTicks) == 0L) {
         entity.refreshSnapshot(null);
      }
   }

   private static long backgroundRefreshStagger(BlockPos pos, int interval) {
      return Math.floorMod(pos == null ? 0L : pos.asLong(), Math.max(1, interval));
   }

   public LogisticsKind kind() {
      return this.getBlockState().getBlock() instanceof LogisticsBlock block ? block.kind() : LogisticsKind.LOGISTICS_TERMINAL;
   }

   public String networkId() {
      return networkId == null || networkId.isBlank() ? "global" : networkId;
   }

   public void setNetworkId(String networkId) {
      this.networkId = sanitizeNetworkId(networkId);
      LogisticsNetworkService.invalidateSnapshots();
      this.setChanged();
   }

   public void setNetworkId(String networkId, @Nullable Player player) {
      setNetworkId(networkId);
      if (player != null) {
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Network set to " + networkId()
            + ". Matching crates, labels, docks, and endpoints now share stock."));
      }
   }

   public String categoryId() {
      return categoryId == null ? "" : categoryId;
   }

   public void setCategoryId(String categoryId) {
      this.categoryId = categoryId == null ? "" : categoryId.strip();
      LogisticsNetworkService.invalidateSnapshots();
      this.setChanged();
   }

   public void setCategoryId(String categoryId, @Nullable Player player) {
      setCategoryId(categoryId);
      if (player != null) {
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Category label set to " + displayCategory()
            + ". This storage now contributes to that supply row."));
      }
   }

   public String loadoutId() {
      if (loadoutId == null || loadoutId.isBlank()) {
         return LogisticsContent.firstLoadoutId();
      }
      return loadoutId;
   }

   public void setLoadoutId(String loadoutId) {
      this.loadoutId = loadoutId == null ? "" : loadoutId.strip();
      LogisticsNetworkService.invalidateSnapshots();
      this.setChanged();
   }

   public void setLoadoutId(String loadoutId, @Nullable Player player) {
      setLoadoutId(loadoutId);
      rememberOwner(player);
      if (player != null) {
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Loadout target set to " + displayLoadout()
            + ". Requests from this block will use that preset."));
      }
   }

   public String depotOfferId() {
      return depotOfferId == null ? "" : depotOfferId;
   }

   public void setDepotOfferId(String depotOfferId) {
      this.depotOfferId = depotOfferId == null ? "" : depotOfferId.strip();
      LogisticsNetworkService.invalidateSnapshots();
      this.setChanged();
   }

   public int cooldownTicks() {
      return Math.max(0, cooldown);
   }

   public void setCooldownTicks(int cooldown) {
      this.cooldown = Math.max(0, cooldown);
      LogisticsNetworkService.invalidateSnapshots();
      this.setChanged();
   }

   public boolean storageNode() {
      return kind().inventory();
   }

   public String statusLine() {
      return "ECHO LOGISTICS // "
         + kind().displayName()
         + " network "
         + networkId()
         + " | category "
         + displayCategory()
         + (kind() == LogisticsKind.FACTION_TRADE_DEPOT
            ? " | offer " + displayDepotOffer()
            : " | loadout " + displayLoadout())
         + " | stock " + lastStockRows
         + " | low " + lastMissingRows
         + " | ready " + lastReadyRows
         + " | drones " + lastActiveDeliveries
         + (lastRewardCount > 0 ? " | rewards " + lastRewardCount : "")
         + " | last: "
         + displayManifest();
   }

   public void cycleCategory(Player player) {
      List<SupplyCategory> categories = LogisticsContent.categories();
      if (categories.isEmpty()) {
         this.categoryId = "";
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // No supply categories loaded. Check datapacks or reload data."));
         return;
      }
      int index = 0;
      for (int i = 0; i < categories.size(); i++) {
         if (categories.get(i).id().toString().equals(categoryId)) {
            index = i + 1;
            break;
         }
      }
      SupplyCategory selected = categories.get(index % categories.size());
      this.categoryId = selected.id().toString();
      this.setChanged();
      player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Category label set to " + selected.title() + "."));
   }

   public void cycleLoadout(Player player) {
      List<LoadoutPreset> loadouts = LogisticsContent.loadouts();
      if (loadouts.isEmpty()) {
         this.loadoutId = "";
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // No loadout presets loaded. Check datapacks or reload data."));
         return;
      }
      int index = 0;
      for (int i = 0; i < loadouts.size(); i++) {
         if (loadouts.get(i).id().toString().equals(loadoutId)) {
            index = i + 1;
            break;
         }
      }
      LoadoutPreset selected = loadouts.get(index % loadouts.size());
      this.loadoutId = selected.id().toString();
      this.setChanged();
      player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Loadout target set to " + selected.title() + "."));
   }

   public boolean handleMenuButton(Player player, int id) {
      if (!(player instanceof ServerPlayer serverPlayer)) {
         return false;
      }
      rememberOwner(serverPlayer);
      return switch (id) {
         case LogisticsMenu.BUTTON_SCAN -> {
            LogisticsSnapshot snapshot = refreshSnapshot(serverPlayer);
            long ready = snapshot.loadoutReadiness().stream().filter(LogisticsNetworkService.LoadoutReadiness::ready).count();
            serverPlayer.sendSystemMessage(Component.literal("ECHO LOGISTICS // Scan complete for " + snapshot.networkId()
               + ". Categories " + snapshot.stockRows().size()
               + ", low stock " + snapshot.missingRows().size()
               + ", ready loadouts " + ready + "/" + snapshot.loadoutReadiness().size()
               + ", active drones " + snapshot.activeDeliveries() + "."));
            EchoCoreServices.discoverVisibleRouteRecords(serverPlayer);
            if (ready > 0 || snapshot.activeDeliveries() > 0) {
               LogisticsMissionHooks.recordNetworkOnline(serverPlayer, snapshot.networkId(), "logistics_scan");
            }
            yield true;
         }
         case LogisticsMenu.BUTTON_REQUEST_LOADOUT -> requestSelectedLoadout(serverPlayer);
         case LogisticsMenu.BUTTON_CLAIM_RELAY -> LogisticsNetworkService.claimRelayRewards(serverPlayer, this);
         case LogisticsMenu.BUTTON_DEPOT_EXCHANGE -> LogisticsNetworkService.performDepotExchange(serverPlayer, this);
         case LogisticsMenu.BUTTON_CYCLE_LOADOUT -> {
            if (kind() == LogisticsKind.FACTION_TRADE_DEPOT) {
               cycleDepotOffer(serverPlayer);
            } else {
               cycleLoadout(serverPlayer);
            }
            yield true;
         }
         case LogisticsMenu.BUTTON_CANCEL_DELIVERIES -> cancelDashboardDeliveries(serverPlayer);
         case LogisticsMenu.BUTTON_REFRESH_OFFERS -> refreshDepotOffers(serverPlayer);
         default -> false;
      };
   }

   public void cycleDepotOffer(Player player) {
      List<FactionDepotOffer> offers = LogisticsContent.offers();
      if (offers.isEmpty()) {
         this.depotOfferId = "";
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // No faction depot offers loaded. Check datapacks or reload data."));
         return;
      }
      int index = 0;
      for (int i = 0; i < offers.size(); i++) {
         if (offers.get(i).id().toString().equals(depotOfferId)) {
            index = i + 1;
            break;
         }
      }
      FactionDepotOffer selected = offers.get(index % offers.size());
      this.depotOfferId = selected.id().toString();
      this.setChanged();
      player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Depot offer selected: "
         + selected.id().getPath().replace('_', ' ')
         + " | cooldown " + ticks(selected.cooldownTicks()) + "."));
   }

   public boolean requestSelectedLoadout(Player player) {
      rememberOwner(player);
      if (cooldown > 0) {
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Request controls cooling down for " + ticks(cooldown) + "."));
         return false;
      }
      boolean dispatched = LogisticsNetworkService.requestDashboardLoadout(player, this.worldPosition, networkId());
      this.cooldown = dispatched ? 40 : 10;
      this.lastManifest = dispatched ? "Request queued: " + displayLoadout() : "Request blocked: " + displayLoadout();
      this.refreshSnapshot(player);
      if (dispatched && player instanceof ServerPlayer serverPlayer) {
         LogisticsMissionHooks.recordRequestLoadout(serverPlayer, loadoutId());
      }
      this.setChanged();
      return dispatched;
   }

   private boolean cancelDashboardDeliveries(ServerPlayer player) {
      int cancelled = LogisticsNetworkService.cancelActiveDeliveries(player, this.worldPosition, networkId());
      player.sendSystemMessage(Component.literal(cancelled > 0
         ? "ECHO LOGISTICS // Cancelled " + cancelled + " active delivery job(s); sealed payload recovery routed to dock."
         : "ECHO LOGISTICS // No cancellable delivery jobs found in network " + networkId() + "."));
      this.lastManifest = cancelled > 0 ? "Cancelled " + cancelled + " delivery job(s)." : "No cancellable deliveries.";
      this.refreshSnapshot(player);
      return true;
   }

   private boolean refreshDepotOffers(ServerPlayer player) {
      LogisticsSnapshot snapshot = refreshSnapshot(player);
      player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Faction offers " + snapshot.depotOffers().size()
         + " | depot " + (snapshot.depotOnline() ? "online" : "offline")
         + (snapshot.depotCooldown() > 0 ? " | cooldown " + ticks(snapshot.depotCooldown()) + "." : ".")));
      return true;
   }

   public boolean insertPayload(List<ItemStack> payload) {
      if (LogisticsNetworkService.insertPayloadIntoContainer(this, payload)) {
         this.lastManifest = "Payload received: " + payload.size() + " stack(s).";
         LogisticsNetworkService.invalidateSnapshots();
         this.setChanged();
         return true;
      }
      return false;
   }

   public void recordManifest(String manifest) {
      if (manifest == null || manifest.isBlank()) {
         return;
      }
      this.lastManifest = manifest;
      this.setChanged();
   }

   public LogisticsSnapshot refreshSnapshot(@Nullable Player player) {
      LogisticsSnapshot snapshot = this.level == null
         ? LogisticsSnapshot.empty(networkId())
         : LogisticsNetworkService.snapshot(this.level, this.worldPosition, networkId(), player, player != null);
      this.lastStockRows = snapshot.stockRows().size();
      this.lastMissingRows = snapshot.missingRows().size();
      this.lastReadyRows = (int)snapshot.loadoutReadiness().stream().filter(readiness -> readiness.ready()).count();
      this.lastActiveDeliveries = snapshot.activeDeliveries();
      this.lastDepotOffers = snapshot.depotOffers().size();
      this.lastRewardCount = player == null ? this.lastRewardCount : LogisticsNetworkService.pendingRelayRewards(player);
      this.lastBlockCount = snapshot.blockCount();
      this.lastEndpointCount = snapshot.endpointCount();
      this.lastDockOnline = snapshot.dockOnline();
      this.lastRelayOnline = snapshot.relayOnline();
      this.lastDepotOnline = snapshot.depotOnline();
      this.lastDepotCooldown = snapshot.depotCooldown();
      this.lastSelectedReady = snapshot.selectedReady();
      this.lastSelectedMissing = snapshot.selectedMissing();
      this.lastCanRequest = snapshot.canRequest();
      this.lastCanDispatch = snapshot.canDispatch();
      this.lastSelectedEndpointKind = snapshot.selectedEndpoint() == null ? -1 : snapshot.selectedEndpoint().kind().ordinal();
      this.lastFirstDeliveryEta = firstDeliveryEta(snapshot);
      this.lastFirstDeliveryStatus = firstDeliveryStatus(snapshot);
      this.setChanged();
      return snapshot;
   }

   private int firstDeliveryEta(LogisticsSnapshot snapshot) {
      if (snapshot.deliveryJobs().isEmpty() || this.level == null) {
         return 0;
      }
      long eta = snapshot.deliveryJobs().getFirst().etaTick() - this.level.getGameTime();
      return (int)Math.max(0L, Math.min(Integer.MAX_VALUE, eta));
   }

   private int firstDeliveryStatus(LogisticsSnapshot snapshot) {
      if (snapshot.deliveryJobs().isEmpty()) {
         return 0;
      }
      String status = snapshot.deliveryJobs().getFirst().status();
      if ("delivered".equals(status)) {
         return 2;
      }
      if ("recovering".equals(status)) {
         return 3;
      }
      if ("empty_payload".equals(status) || "complete".equals(status)) {
         return 4;
      }
      return 1;
   }

   private void tryAutoRestock() {
      if (this.level == null || loadoutId().isBlank()) {
         return;
      }
      Player player = this.level.getNearestPlayer(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, 16.0D, false);
      tryAutoRestock(player);
   }

   public boolean tryAutoRestock(@Nullable Player player) {
      if (this.level == null || loadoutId().isBlank()) {
         return false;
      }
      rememberOwner(player);
      if (LogisticsContent.loadout(loadoutId()).map(loadout -> loadout.restockPolicy().enabled()).orElse(false)
         && this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
         LogisticsNetworkService.FactoryRestockStatus status = LogisticsNetworkService.requestFactoryAutoRestock(
            serverLevel,
            ownerUuid,
            player,
            this.worldPosition,
            networkId(),
            loadoutId());
         this.lastManifest = "Factory restock: " + status.message();
         this.restockCooldown = Math.max(20, status.cooldownTicks());
         this.setChanged();
         return status.dispatched();
      }
      if (player == null) {
         return false;
      }
      LogisticsSnapshot snapshot = LogisticsNetworkService.snapshot(this.level, this.worldPosition, networkId(), null);
      boolean ready = snapshot.loadoutReadiness().stream().anyMatch(readiness -> readiness.presetId().toString().equals(loadoutId()) && readiness.ready());
      boolean empty = items.stream().allMatch(ItemStack::isEmpty);
      if (ready && empty) {
         return requestSelectedLoadout(player);
      }
      return false;
   }

   private void rememberOwner(@Nullable Player player) {
      if (player != null) {
         this.ownerUuid = player.getUUID();
         this.setChanged();
      }
   }

   private String displayCategory() {
      return LogisticsContent.category(categoryId()).map(SupplyCategory::title).orElse(categoryId().isBlank() ? "Any" : categoryId());
   }

   private String displayLoadout() {
      return LogisticsContent.loadout(loadoutId()).map(LoadoutPreset::title).orElse(loadoutId().isBlank() ? "None" : loadoutId());
   }

   private String displayDepotOffer() {
      if (depotOfferId().isBlank()) {
         return "Auto";
      }
      return LogisticsContent.offers().stream()
         .filter(offer -> offer.id().toString().equals(depotOfferId()))
         .findFirst()
         .map(offer -> offer.id().getPath().replace('_', ' '))
         .orElse(depotOfferId());
   }

   private String displayManifest() {
      String manifest = lastManifest == null || lastManifest.isBlank() ? "Idle" : lastManifest.strip();
      manifest = manifest.replace('_', ' ');
      if (manifest.length() <= 96) {
         return manifest;
      }
      return manifest.substring(0, 93) + "...";
   }

   private static String ticks(int ticks) {
      int safeTicks = Math.max(0, ticks);
      if (safeTicks < 20) {
         return safeTicks + "t";
      }
      int seconds = Math.round(safeTicks / 20.0F);
      return safeTicks + "t (~" + seconds + "s)";
   }

   private static String sanitizeNetworkId(String networkId) {
      if (networkId == null || networkId.isBlank()) {
         return "global";
      }
      String sanitized = networkId.strip().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_\\-:.]", "_");
      return sanitized.isBlank() ? "global" : sanitized;
   }

   public ContainerData data() {
      return data;
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
      LogisticsNetworkService.invalidateSnapshots();
   }

   @Override
   public int getContainerSize() {
      return items.size();
   }

   @Override
   public boolean canPlaceItem(int slot, ItemStack stack) {
      return kind().inventory();
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
      return kind().inventory();
   }

   @Override
   protected @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
      return new LogisticsMenu(containerId, inventory, this, data);
   }

   @Override
   public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
      buffer.writeBlockPos(this.getBlockPos());
   }

   @Override
   public void preRemoveSideEffects(BlockPos pos, BlockState state) {
      if (level == null || level.isClientSide()) {
         return;
      }
      Containers.dropContents(level, pos, this);
      for (int slot = 0; slot < items.size(); slot++) {
         items.set(slot, ItemStack.EMPTY);
      }
      LogisticsNetworkService.invalidateSnapshots();
      setChanged();
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      ContainerHelper.loadAllItems(input, items);
      this.networkId = input.getStringOr("network_id", "global");
      this.categoryId = input.getStringOr("category_id", "");
      this.loadoutId = input.getStringOr("loadout_id", "");
      this.depotOfferId = input.getStringOr("depot_offer_id", "");
      this.cooldown = input.getIntOr("cooldown", 0);
      this.restockCooldown = input.getIntOr("restock_cooldown", 0);
      this.lastManifest = input.getStringOr("last_manifest", "Idle");
      this.ownerUuid = parseUuid(input.getStringOr("owner_uuid", "")).orElse(null);
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      ContainerHelper.saveAllItems(output, items);
      output.putString("network_id", networkId());
      output.putString("category_id", categoryId());
      output.putString("loadout_id", loadoutId());
      output.putString("depot_offer_id", depotOfferId());
      output.putInt("cooldown", cooldown);
      output.putInt("restock_cooldown", restockCooldown);
      output.putString("last_manifest", lastManifest == null ? "Idle" : lastManifest);
      if (ownerUuid != null) {
         output.putString("owner_uuid", ownerUuid.toString());
      }
   }

   private static java.util.Optional<UUID> parseUuid(String value) {
      if (value == null || value.isBlank()) {
         return java.util.Optional.empty();
      }
      try {
         return java.util.Optional.of(UUID.fromString(value));
      } catch (IllegalArgumentException exception) {
         return java.util.Optional.empty();
      }
   }
}
