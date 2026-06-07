package com.knoxhack.echonexusprotocol.block;

import com.knoxhack.echonexusprotocol.block.entity.NexusMachineBlockEntity;
import com.knoxhack.echonexusprotocol.data.NexusPlayerData;
import com.knoxhack.echonexusprotocol.integration.NexusMissionHooks;
import com.knoxhack.echonexusprotocol.item.NexusScannerVisorItem;
import com.knoxhack.echonexusprotocol.registry.ModBlockEntities;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
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

public class NexusMachineBlock extends Block implements EntityBlock {
   private final NexusMachineBlock.MachineKind kind;

   public NexusMachineBlock(NexusMachineBlock.MachineKind kind, Properties properties) {
      super(properties);
      this.kind = kind;
   }

   public NexusMachineBlock.MachineKind kind() {
      return this.kind;
   }

   public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new NexusMachineBlockEntity(pos, state);
   }

   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return type == ModBlockEntities.NEXUS_MACHINE.get()
         ? (tickLevel, pos, blockState, blockEntity) -> NexusMachineBlockEntity.tick(tickLevel, pos, blockState, (NexusMachineBlockEntity)blockEntity)
         : null;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (!level.isClientSide() && level.getBlockEntity(pos) instanceof NexusMachineBlockEntity machine) {
         if (player instanceof ServerPlayer serverPlayer) {
            NexusPlayerData data = NexusPlayerData.get(serverPlayer);
            data.markMachineUsed(this.kind);
            NexusPlayerData.saveAndSync(serverPlayer, data);
            NexusMissionHooks.recordMachine(serverPlayer, this.kind);
         }
         if (player.isShiftKeyDown()) {
            player.sendSystemMessage(Component.literal(machine.statusLine()));
         } else if (machine instanceof MenuProvider provider) {
            player.openMenu(provider);
         }
         return InteractionResult.SUCCESS_SERVER;
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   protected InteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!level.isClientSide() && stack.getItem() instanceof NexusScannerVisorItem) {
         NexusScannerVisorItem.scanBlock(player, pos);
         return InteractionResult.SUCCESS_SERVER;
      } else {
         return this.useWithoutItem(state, level, pos, player, hitResult);
      }
   }

   public static enum MachineKind implements StringRepresentable {
      NEXUS_RECYCLER("nexus_recycler", "Nexus Recycler", true),
      NEXUS_CHARGE_TANK("nexus_charge_tank", "Nexus Charge Tank", false),
      CORRUPTION_FILTER("corruption_filter", "Corruption Filter", false),
      NEXUS_FIELD_STABILIZER("nexus_field_stabilizer", "Nexus Field Stabilizer", false),
      NEXUS_INFUSER("nexus_infuser", "Nexus Infuser", true),
      MEMORY_DECODER("memory_decoder", "Memory Decoder", true),
      REALITY_FORGE("reality_forge", "Reality Forge", true),
      CORRUPTION_REACTOR("corruption_reactor", "Corruption Reactor", false);

      public static final Codec<NexusMachineBlock.MachineKind> CODEC = StringRepresentable.fromEnum(NexusMachineBlock.MachineKind::values);
      public static final StreamCodec<RegistryFriendlyByteBuf, NexusMachineBlock.MachineKind> STREAM_CODEC = ByteBufCodecs.idMapper(
            NexusMachineBlock.MachineKind::byId, Enum::ordinal
         )
         .cast();
      private static final NexusMachineBlock.MachineKind[] BY_ID = values();
      private final String serializedName;
      private final String displayName;
      private final boolean recipeDriven;

      private MachineKind(String serializedName, String displayName, boolean recipeDriven) {
         this.serializedName = serializedName;
         this.displayName = displayName;
         this.recipeDriven = recipeDriven;
      }

      public String displayName() {
         return this.displayName;
      }

      public boolean recipeDriven() {
         return this.recipeDriven;
      }

      public String getSerializedName() {
         return this.serializedName;
      }

      private static NexusMachineBlock.MachineKind byId(int id) {
         return id >= 0 && id < BY_ID.length ? BY_ID[id] : NEXUS_RECYCLER;
      }
   }
}
