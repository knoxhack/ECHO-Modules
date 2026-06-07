package com.knoxhack.echologisticsnetwork.block;

import com.knoxhack.echologisticsnetwork.block.entity.LogisticsBlockEntity;
import com.knoxhack.echologisticsnetwork.item.LogisticsToolItem;
import com.knoxhack.echologisticsnetwork.registry.ModBlockEntities;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class LogisticsBlock extends Block implements EntityBlock {
   private final LogisticsKind kind;

   public LogisticsBlock(LogisticsKind kind, Properties properties) {
      super(properties);
      this.kind = kind;
   }

   public LogisticsKind kind() {
      return this.kind;
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new LogisticsBlockEntity(pos, state);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return type == ModBlockEntities.LOGISTICS.get()
         ? (tickLevel, pos, blockState, blockEntity) -> LogisticsBlockEntity.tick(tickLevel, pos, blockState, (LogisticsBlockEntity)blockEntity)
         : null;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      if (level.getBlockEntity(pos) instanceof LogisticsBlockEntity logistics) {
         player.openMenu(logistics);
         player.sendSystemMessage(Component.literal(logistics.statusLine()));
      }
      return InteractionResult.SUCCESS_SERVER;
   }

   @Override
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
      if (!(level.getBlockEntity(pos) instanceof LogisticsBlockEntity logistics)) {
         return InteractionResult.PASS;
      }
      if (stack.getItem() instanceof LogisticsToolItem tool) {
         return level.isClientSide() ? InteractionResult.SUCCESS : tool.applyToBlock(stack, level, pos, player);
      }
      return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
   }

   public enum LogisticsKind implements StringRepresentable {
      LOGISTICS_TERMINAL("logistics_terminal", "Logistics Terminal"),
      SUPPLY_CRATE("supply_crate", "Supply Crate"),
      SMART_STORAGE_LABEL("smart_storage_label", "Smart Storage Label"),
      DRONE_DELIVERY_DOCK("drone_delivery_dock", "Drone Delivery Dock"),
      ROUTE_REQUESTER("route_requester", "Route Requester"),
      LOADOUT_LOCKER("loadout_locker", "Loadout Locker"),
      FACTION_TRADE_DEPOT("faction_trade_depot", "Faction Trade Depot"),
      REMOTE_REWARD_RELAY("remote_reward_relay", "Remote Reward Relay"),
      AUTO_RESTOCK_STATION("auto_restock_station", "Auto-Restock Station");

      private final String name;
      private final String displayName;

      LogisticsKind(String name, String displayName) {
         this.name = name;
         this.displayName = displayName;
      }

      @Override
      public String getSerializedName() {
         return name;
      }

      public String displayName() {
         return displayName;
      }

      public boolean inventory() {
         return this == SUPPLY_CRATE || this == DRONE_DELIVERY_DOCK || this == ROUTE_REQUESTER || this == LOADOUT_LOCKER
            || this == FACTION_TRADE_DEPOT || this == REMOTE_REWARD_RELAY || this == AUTO_RESTOCK_STATION;
      }

      public static LogisticsKind byName(String name) {
         String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
         for (LogisticsKind kind : values()) {
            if (kind.name.equals(normalized)) {
               return kind;
            }
         }
         return LOGISTICS_TERMINAL;
      }
   }
}
