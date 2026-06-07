package com.knoxhack.echoconvoyprotocol.block;

import com.knoxhack.echoconvoyprotocol.block.entity.ConvoyStationBlockEntity;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.registry.ModBlockEntities;
import com.knoxhack.echoconvoyprotocol.service.ConvoyRouteService;
import com.mojang.serialization.Codec;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class ConvoyBlock extends Block implements EntityBlock {
   private final ConvoyBlockKind kind;

   public ConvoyBlock(ConvoyBlockKind kind, Properties properties) {
      super(properties);
      this.kind = kind;
   }

   public ConvoyBlockKind kind() {
      return kind;
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ConvoyStationBlockEntity(pos, state);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return type == ModBlockEntities.CONVOY_STATION.get()
         ? (tickLevel, pos, blockState, blockEntity) -> ConvoyStationBlockEntity.tick(tickLevel, pos, blockState, (ConvoyStationBlockEntity)blockEntity)
         : null;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      if (player instanceof ServerPlayer serverPlayer && handleRouteBlock(level, pos, serverPlayer)) {
         return InteractionResult.SUCCESS_SERVER;
      }
      if (level.getBlockEntity(pos) instanceof ConvoyStationBlockEntity station) {
         station.linkOwner(player);
      }
      if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
         player.openMenu(provider);
         player.sendSystemMessage(Component.literal("ECHO CONVOY // " + kind.displayName() + " online."));
      }
      return InteractionResult.SUCCESS_SERVER;
   }

   @Override
   protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      if (player instanceof ServerPlayer serverPlayer && handleRouteBlock(level, pos, serverPlayer)) {
         return InteractionResult.SUCCESS_SERVER;
      }
      if (level.getBlockEntity(pos) instanceof ConvoyStationBlockEntity station) {
         station.linkOwner(player);
      }
      if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
         player.openMenu(provider);
         player.sendSystemMessage(Component.literal("ECHO CONVOY // " + kind.displayName() + " access granted."));
      }
      return InteractionResult.SUCCESS_SERVER;
   }

   private boolean handleRouteBlock(Level level, BlockPos pos, ServerPlayer player) {
      if (kind != ConvoyBlockKind.CONVOY_BEACON && kind != ConvoyBlockKind.ROADSIDE_SIGNAL_MARKER) {
         return false;
      }
      ConvoyVehicleEntity vehicle = ConvoyStationBlockEntity.nearestVehicle(level, pos, player);
      if (kind == ConvoyBlockKind.CONVOY_BEACON) {
         return ConvoyRouteService.activateFirstRoute(player, vehicle, pos);
      }
      ConvoyStationBlockEntity marker = level.getBlockEntity(pos) instanceof ConvoyStationBlockEntity station ? station : null;
      return ConvoyRouteService.advanceRouteAtSignal(player, vehicle, pos, marker);
   }

   public enum ConvoyBlockKind implements StringRepresentable {
      VEHICLE_WORKBENCH("vehicle_workbench", "Vehicle Workbench"),
      FUEL_STILL("fuel_still", "Fuel Still"),
      BATTERY_CHARGING_PAD("battery_charging_pad", "Battery Charging Pad"),
      VEHICLE_DOCK("vehicle_dock", "Vehicle Dock"),
      VEHICLE_UPGRADE_BAY("vehicle_upgrade_bay", "Vehicle Upgrade Bay"),
      CONVOY_BEACON("convoy_beacon", "Convoy Beacon"),
      ROADSIDE_SIGNAL_MARKER("roadside_signal_marker", "Roadside Signal Marker"),
      CARGO_ANCHOR("cargo_anchor", "Cargo Anchor"),
      FIELD_REPAIR_STATION("field_repair_station", "Field Repair Station");

      public static final Codec<ConvoyBlockKind> CODEC = StringRepresentable.fromEnum(ConvoyBlockKind::values);
      public static final StreamCodec<RegistryFriendlyByteBuf, ConvoyBlockKind> STREAM_CODEC =
         ByteBufCodecs.idMapper(ConvoyBlockKind::byId, ConvoyBlockKind::ordinal).cast();
      private static final ConvoyBlockKind[] BY_ID = values();
      private final String name;
      private final String displayName;

      ConvoyBlockKind(String name, String displayName) {
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

      public boolean recipeDriven() {
         return this == VEHICLE_WORKBENCH || this == FUEL_STILL || this == BATTERY_CHARGING_PAD || this == FIELD_REPAIR_STATION;
      }

      public static ConvoyBlockKind byId(int id) {
         return id >= 0 && id < BY_ID.length ? BY_ID[id] : VEHICLE_WORKBENCH;
      }

      public static ConvoyBlockKind byName(String name) {
         String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
         for (ConvoyBlockKind kind : values()) {
            if (kind.name.equals(normalized)) {
               return kind;
            }
         }
         return VEHICLE_WORKBENCH;
      }
   }
}
