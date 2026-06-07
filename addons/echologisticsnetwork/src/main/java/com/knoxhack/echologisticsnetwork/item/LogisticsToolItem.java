package com.knoxhack.echologisticsnetwork.item;

import com.knoxhack.echologisticsnetwork.block.entity.LogisticsBlockEntity;
import com.knoxhack.echologisticsnetwork.content.LoadoutCardSelection;
import com.knoxhack.echologisticsnetwork.content.LoadoutPreset;
import com.knoxhack.echologisticsnetwork.content.LogisticsContent;
import com.knoxhack.echologisticsnetwork.content.RemoteRequestSelection;
import com.knoxhack.echologisticsnetwork.content.RouteManifestSelection;
import com.knoxhack.echologisticsnetwork.content.SupplyCategory;
import com.knoxhack.echologisticsnetwork.content.SupplyTagSelection;
import com.knoxhack.echologisticsnetwork.integration.LogisticsMissionHooks;
import com.knoxhack.echologisticsnetwork.registry.ModDataComponents;
import com.knoxhack.echologisticsnetwork.service.LogisticsNetworkService;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class LogisticsToolItem extends Item {
   private final Mode mode;

   public LogisticsToolItem(Mode mode, Properties properties) {
      super(properties);
      this.mode = mode;
   }

   @Override
   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      return switch (mode) {
         case SUPPLY_TAG -> cycleSupplyTag(stack, player);
         case LOADOUT_CARD -> cycleLoadoutCard(stack, player);
         case ROUTE_MANIFEST -> writeRouteManifest(stack, player);
         case REMOTE_REQUEST_TABLET -> scanTablet(level, player, stack);
      };
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      }
      if (context.getLevel().isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      return applyToBlock(context.getItemInHand(), context.getLevel(), context.getClickedPos(), player);
   }

   public InteractionResult applyToBlock(ItemStack stack, Level level, BlockPos pos, Player player) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (!(blockEntity instanceof LogisticsBlockEntity logistics)) {
         return InteractionResult.PASS;
      }
      return switch (mode) {
         case SUPPLY_TAG -> applySupplyTag(stack, logistics, player);
         case LOADOUT_CARD -> applyLoadoutCard(stack, logistics, player);
         case ROUTE_MANIFEST -> applyRouteManifest(stack, logistics, player);
         case REMOTE_REQUEST_TABLET -> bindTablet(stack, logistics, player);
      };
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      switch (mode) {
         case SUPPLY_TAG -> {
            SupplyTagSelection selection = supplyTagSelection(stack);
            tooltip.accept(Component.translatable("tooltip.echologisticsnetwork.supply_tag.category", displayCategory(selection.categoryId())));
            tooltip.accept(Component.translatable("tooltip.echologisticsnetwork.supply_tag.use"));
            tooltip.accept(Component.translatable("tooltip.echologisticsnetwork.supply_tag.sneak"));
         }
         case LOADOUT_CARD -> {
            LoadoutCardSelection selection = loadoutCardSelection(stack);
            tooltip.accept(Component.translatable("tooltip.echologisticsnetwork.loadout_card.loadout", displayLoadout(selection.loadoutId())));
            tooltip.accept(Component.translatable("tooltip.echologisticsnetwork.loadout_card.use"));
            tooltip.accept(Component.translatable("tooltip.echologisticsnetwork.loadout_card.sneak"));
         }
         case ROUTE_MANIFEST -> {
            RouteManifestSelection selection = routeManifestSelection(stack);
            tooltip.accept(Component.translatable("tooltip.echologisticsnetwork.route_manifest.network", selection.networkId()));
            tooltip.accept(Component.translatable("tooltip.echologisticsnetwork.route_manifest.use"));
            tooltip.accept(Component.translatable("tooltip.echologisticsnetwork.route_manifest.sneak"));
         }
         case REMOTE_REQUEST_TABLET -> {
            RemoteRequestSelection selection = remoteRequestSelection(stack);
            tooltip.accept(Component.translatable("tooltip.echologisticsnetwork.remote_request_tablet.network",
               selection.bound() ? selection.networkId() : Component.translatable("tooltip.echologisticsnetwork.unbound")));
            tooltip.accept(Component.translatable("tooltip.echologisticsnetwork.remote_request_tablet.loadout", displayLoadout(selection.loadoutId())));
            tooltip.accept(Component.translatable("tooltip.echologisticsnetwork.remote_request_tablet.target",
               selection.targetPos().map(BlockPos::toShortString).orElse(Component.translatable("tooltip.echologisticsnetwork.unbound").getString())));
            tooltip.accept(Component.translatable("tooltip.echologisticsnetwork.remote_request_tablet.use"));
            tooltip.accept(Component.translatable("tooltip.echologisticsnetwork.remote_request_tablet.sneak"));
         }
      }
   }

   private static InteractionResult cycleSupplyTag(ItemStack stack, Player player) {
      SupplyTagSelection current = supplyTagSelection(stack);
      SupplyTagSelection next = player.isShiftKeyDown() ? SupplyTagSelection.EMPTY : new SupplyTagSelection(nextCategory(current.categoryId()));
      stack.set(ModDataComponents.SUPPLY_TAG_SELECTION.get(), next);
      player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Supply Tag now labels " + displayCategory(next.categoryId()) + ". Use it on a Logistics block to apply."));
      return InteractionResult.SUCCESS_SERVER;
   }

   private static InteractionResult cycleLoadoutCard(ItemStack stack, Player player) {
      LoadoutCardSelection current = loadoutCardSelection(stack);
      LoadoutCardSelection next = player.isShiftKeyDown() ? LoadoutCardSelection.EMPTY : new LoadoutCardSelection(nextLoadout(current.loadoutId()));
      stack.set(ModDataComponents.LOADOUT_CARD_SELECTION.get(), next);
      player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Loadout Card now requests " + displayLoadout(next.loadoutId()) + ". Use it on a locker, requester, or restock station."));
      return InteractionResult.SUCCESS_SERVER;
   }

   private static InteractionResult writeRouteManifest(ItemStack stack, Player player) {
      String networkId = player.isShiftKeyDown() ? "global" : playerNetwork(player);
      RouteManifestSelection selection = new RouteManifestSelection(networkId);
      stack.set(ModDataComponents.ROUTE_MANIFEST_SELECTION.get(), selection);
      player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Route Manifest written for network " + selection.networkId() + ". Use it on Logistics blocks to link the route."));
      return InteractionResult.SUCCESS_SERVER;
   }

   private static InteractionResult scanTablet(Level level, Player player, ItemStack stack) {
      RemoteRequestSelection selection = remoteRequestSelection(stack);
      if (!selection.bound()) {
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Remote Request Tablet is unbound. Use it on a locker, requester, or restock station first."));
         return InteractionResult.SUCCESS_SERVER;
      }
      BlockPos targetPos = selection.targetPos().orElse(player.blockPosition());
      if (!player.isShiftKeyDown() && !selection.loadoutId().isBlank()) {
         return dispatchTabletRequest(level, player, selection, targetPos);
      }
      LogisticsNetworkService.LogisticsSnapshot snapshot = LogisticsNetworkService.snapshot(level, targetPos, selection.networkId(), player);
      Optional<LogisticsNetworkService.LoadoutReadiness> readiness = selection.loadoutId().isBlank()
         ? Optional.empty()
         : snapshot.loadoutReadiness().stream().filter(row -> row.presetId().toString().equals(selection.loadoutId())).findFirst();
      String loadoutStatus = readiness.map(row -> row.ready()
            ? row.title() + " ready"
            : row.title() + " missing " + row.missingCount())
         .orElse("no preset bound");
      player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Remote scan " + snapshot.networkId()
         + " | categories " + snapshot.stockRows().size()
         + " | low stock " + snapshot.missingRows().size()
         + " | drones " + snapshot.activeDeliveries()
         + " | " + loadoutStatus + "."));
      return InteractionResult.SUCCESS_SERVER;
   }

   private static InteractionResult dispatchTabletRequest(Level level, Player player, RemoteRequestSelection selection, BlockPos targetPos) {
      if (!(level.getBlockEntity(targetPos) instanceof LogisticsBlockEntity logistics)) {
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Bound tablet target is offline or missing. Rebind at a live Logistics block."));
         return InteractionResult.SUCCESS_SERVER;
      }
      if (!selection.networkId().equals(logistics.networkId())) {
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Bound tablet network mismatch. Rebind to update route data."));
         return InteractionResult.SUCCESS_SERVER;
      }
      if (!logistics.loadoutId().equals(selection.loadoutId())) {
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Bound target loadout changed; dispatching stored tablet preset."));
      }
      boolean dispatched = LogisticsNetworkService.requestLoadout(player, targetPos, targetPos, selection.loadoutId());
      logistics.refreshSnapshot(player);
      if (!dispatched) {
         player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Remote request could not dispatch. Sneak-use the tablet for a readiness scan."));
      }
      return InteractionResult.SUCCESS_SERVER;
   }

   private static InteractionResult applySupplyTag(ItemStack stack, LogisticsBlockEntity logistics, Player player) {
      SupplyTagSelection selection = supplyTagSelection(stack);
      if (player.isShiftKeyDown()) {
         selection = SupplyTagSelection.EMPTY;
         stack.set(ModDataComponents.SUPPLY_TAG_SELECTION.get(), selection);
      } else if (selection.categoryId().isBlank()) {
         selection = new SupplyTagSelection(nextCategory(""));
         stack.set(ModDataComponents.SUPPLY_TAG_SELECTION.get(), selection);
      }
      logistics.setCategoryId(selection.categoryId(), player);
      LogisticsMissionHooks.recordLabelSupplies(player, selection.categoryId());
      return InteractionResult.SUCCESS_SERVER;
   }

   private static InteractionResult applyLoadoutCard(ItemStack stack, LogisticsBlockEntity logistics, Player player) {
      LoadoutCardSelection selection = loadoutCardSelection(stack);
      if (player.isShiftKeyDown()) {
         selection = LoadoutCardSelection.EMPTY;
         stack.set(ModDataComponents.LOADOUT_CARD_SELECTION.get(), selection);
      } else if (selection.loadoutId().isBlank()) {
         selection = new LoadoutCardSelection(nextLoadout(""));
         stack.set(ModDataComponents.LOADOUT_CARD_SELECTION.get(), selection);
      }
      logistics.setLoadoutId(selection.loadoutId(), player);
      return InteractionResult.SUCCESS_SERVER;
   }

   private static InteractionResult applyRouteManifest(ItemStack stack, LogisticsBlockEntity logistics, Player player) {
      RouteManifestSelection selection = stack.get(ModDataComponents.ROUTE_MANIFEST_SELECTION.get());
      if (selection == null || player.isShiftKeyDown()) {
         selection = new RouteManifestSelection(player.isShiftKeyDown() ? "global" : playerNetwork(player));
         stack.set(ModDataComponents.ROUTE_MANIFEST_SELECTION.get(), selection);
      }
      logistics.setNetworkId(selection.networkId(), player);
      return InteractionResult.SUCCESS_SERVER;
   }

   private static InteractionResult bindTablet(ItemStack stack, LogisticsBlockEntity logistics, Player player) {
      RemoteRequestSelection selection = new RemoteRequestSelection(logistics.networkId(), logistics.loadoutId(), logistics.getBlockPos());
      stack.set(ModDataComponents.REMOTE_REQUEST_SELECTION.get(), selection);
      player.sendSystemMessage(Component.literal("ECHO LOGISTICS // Remote Request Tablet bound to "
         + selection.networkId() + " | " + displayLoadout(selection.loadoutId()) + " @ " + logistics.getBlockPos().toShortString()
         + ". Use normally to request, sneak-use to scan."));
      return InteractionResult.SUCCESS_SERVER;
   }

   private static SupplyTagSelection supplyTagSelection(ItemStack stack) {
      SupplyTagSelection selection = stack.get(ModDataComponents.SUPPLY_TAG_SELECTION.get());
      return selection == null ? SupplyTagSelection.EMPTY : selection;
   }

   private static LoadoutCardSelection loadoutCardSelection(ItemStack stack) {
      LoadoutCardSelection selection = stack.get(ModDataComponents.LOADOUT_CARD_SELECTION.get());
      return selection == null ? LoadoutCardSelection.EMPTY : selection;
   }

   private static RouteManifestSelection routeManifestSelection(ItemStack stack) {
      RouteManifestSelection selection = stack.get(ModDataComponents.ROUTE_MANIFEST_SELECTION.get());
      return selection == null ? RouteManifestSelection.GLOBAL : selection;
   }

   private static RemoteRequestSelection remoteRequestSelection(ItemStack stack) {
      RemoteRequestSelection selection = stack.get(ModDataComponents.REMOTE_REQUEST_SELECTION.get());
      return selection == null ? RemoteRequestSelection.EMPTY : selection;
   }

   private static String nextCategory(String current) {
      List<SupplyCategory> categories = LogisticsContent.categories();
      if (categories.isEmpty()) {
         return "";
      }
      int index = -1;
      for (int i = 0; i < categories.size(); i++) {
         if (categories.get(i).id().toString().equals(current)) {
            index = i;
            break;
         }
      }
      return categories.get((index + 1) % categories.size()).id().toString();
   }

   private static String nextLoadout(String current) {
      List<LoadoutPreset> loadouts = LogisticsContent.loadouts();
      if (loadouts.isEmpty()) {
         return "";
      }
      int index = -1;
      for (int i = 0; i < loadouts.size(); i++) {
         if (loadouts.get(i).id().toString().equals(current)) {
            index = i;
            break;
         }
      }
      return loadouts.get((index + 1) % loadouts.size()).id().toString();
   }

   private static String displayCategory(String categoryId) {
      return LogisticsContent.category(categoryId).map(SupplyCategory::title).orElse(categoryId == null || categoryId.isBlank() ? "Any" : categoryId);
   }

   private static String displayLoadout(String loadoutId) {
      if (loadoutId == null || loadoutId.isBlank()) {
         return "Default";
      }
      return LogisticsContent.loadout(loadoutId).map(LoadoutPreset::title).orElse(loadoutId);
   }

   private static String playerNetwork(Player player) {
      return player.getUUID().toString().substring(0, 8);
   }

   public enum Mode {
      SUPPLY_TAG,
      LOADOUT_CARD,
      ROUTE_MANIFEST,
      REMOTE_REQUEST_TABLET
   }
}
