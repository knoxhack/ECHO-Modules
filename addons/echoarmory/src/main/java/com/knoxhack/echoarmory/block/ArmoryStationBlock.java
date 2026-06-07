package com.knoxhack.echoarmory.block;

import com.knoxhack.echoarmory.block.entity.ArmoryStationBlockEntity;
import com.knoxhack.echoarmory.registry.ModBlockEntities;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class ArmoryStationBlock extends Block implements EntityBlock {
   public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
   private final StationKind kind;

   public ArmoryStationBlock(StationKind kind, Properties properties) {
      super(properties);
      this.kind = kind;
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
   }

   public StationKind kind() {
      return kind;
   }

   @Override
   public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ArmoryStationBlockEntity(pos, state);
   }

   @Override
   public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
      return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   @Override
   protected BlockState rotate(BlockState state, Rotation rotation) {
      return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
   }

   @Override
   protected BlockState mirror(BlockState state, Mirror mirror) {
      return state.rotate(mirror.getRotation(state.getValue(FACING)));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING);
   }

   @Override
   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return type == ModBlockEntities.ARMORY_STATION.get()
         ? (tickLevel, pos, blockState, blockEntity) -> ArmoryStationBlockEntity.tick(tickLevel, pos, blockState, (ArmoryStationBlockEntity)blockEntity)
         : null;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      }
      if (level.getBlockEntity(pos) instanceof ArmoryStationBlockEntity station) {
         player.openMenu(station);
      }
      return InteractionResult.SUCCESS_SERVER;
   }

   public enum StationKind implements StringRepresentable {
      ARMORY_BENCH("armory_bench", "Armory Bench"),
      WEAPON_FORGE("weapon_forge", "Weapon Forge"),
      ARMOR_FORGE("armor_forge", "Armor Forge"),
      ENERGY_CORE_CHARGING_STATION("energy_core_charging_station", "Energy Core Charging Station"),
      MODULE_UPGRADE_TABLE("module_upgrade_table", "Module Upgrade Table"),
      SIGIL_ENGRAVER("sigil_engraver", "Sigil Engraver"),
      LOADOUT_TERMINAL("loadout_terminal", "Loadout Terminal"),
      WEAPON_RACK("weapon_rack", "Weapon Rack"),
      ARMOR_STAND("armor_stand", "Armor Stand"),
      VEIL_INFUSER("veil_infuser", "Veil Infuser"),
      CONSTRUCT_DOCK("construct_dock", "Construct Dock");

      private final String name;
      private final String displayName;

      StationKind(String name, String displayName) {
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

      public static StationKind byName(String name) {
         String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
         for (StationKind kind : values()) {
            if (kind.name.equals(normalized)) {
               return kind;
            }
         }
         return ARMORY_BENCH;
      }
   }
}
