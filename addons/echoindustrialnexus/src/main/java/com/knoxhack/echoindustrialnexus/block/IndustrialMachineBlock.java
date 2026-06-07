package com.knoxhack.echoindustrialnexus.block;

import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;
import com.knoxhack.echoindustrialnexus.registry.ModBlockEntities;
import com.knoxhack.echoindustrialnexus.registry.ModItems;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class IndustrialMachineBlock extends Block implements EntityBlock {
   public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
   private final IndustrialMachineBlock.MachineKind kind;

   public IndustrialMachineBlock(IndustrialMachineBlock.MachineKind kind, Properties properties) {
      super(properties);
      this.kind = kind;
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(ACTIVE, false));
   }

   public IndustrialMachineBlock.MachineKind kind() {
      return this.kind;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{ACTIVE});
   }

   public @Nullable BlockEntity newBlockEntity(BlockPos worldPosition, BlockState blockState) {
      return new IndustrialMachineBlockEntity(worldPosition, blockState);
   }

   public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> type) {
      return type == ModBlockEntities.INDUSTRIAL_MACHINE.get()
         ? (tickLevel, pos, state, blockEntity) -> IndustrialMachineBlockEntity.tick(tickLevel, pos, state, (IndustrialMachineBlockEntity)blockEntity)
         : null;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (level.getBlockEntity(pos) instanceof IndustrialMachineBlockEntity machine) {
         if (this.kind.factoryController()) {
            player.sendSystemMessage(Component.literal(machine.factoryControllerLine()));
            return InteractionResult.SUCCESS_SERVER;
         } else if (!player.isShiftKeyDown()) {
            player.openMenu(machine);
            player.sendSystemMessage(Component.literal(machine.statusLine()));
            return InteractionResult.SUCCESS_SERVER;
         } else {
            ItemStack extracted = machine.extractToPlayer(player);
            if (extracted.isEmpty()) {
               player.sendSystemMessage(Component.literal(machine.statusLine()));
            }

            return InteractionResult.SUCCESS_SERVER;
         }
      } else {
         return InteractionResult.SUCCESS_SERVER;
      }
   }

   protected InteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (stack.is((Item)ModItems.THERMAL_WRENCH.get()) || stack.is((Item)ModItems.FLUX_MULTIMETER.get())) {
         return InteractionResult.PASS;
      } else if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (level.getBlockEntity(pos) instanceof IndustrialMachineBlockEntity machine) {
         if (machine.insertFromHand(player, hand, stack)) {
            return InteractionResult.SUCCESS_SERVER;
         } else {
            player.sendSystemMessage(Component.literal(machine.statusLine()));
            return InteractionResult.CONSUME;
         }
      } else {
         return InteractionResult.CONSUME;
      }
   }

   @Override
   public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool) {
      if (!level.isClientSide() && blockEntity instanceof IndustrialMachineBlockEntity machine) {
         Containers.dropContents(level, pos, machine);
         machine.clearContent();
      }

      super.playerDestroy(level, player, pos, state, blockEntity, tool);
   }

   public static enum MachineKind implements StringRepresentable {
      SCRAP_DYNAMO("scrap_dynamo", "Scrap Dynamo"),
      THERMAL_ARRAY("thermal_array", "Thermal Array"),
      GEOTHERMAL_PUMP("geothermal_pump", "Geothermal Pump"),
      REACTOR_HEAT_EXCHANGER("reactor_heat_exchanger", "Reactor Heat Exchanger"),
      SOLAR_CONCENTRATOR("solar_concentrator", "Solar Concentrator"),
      STATIC_HEAT_EXCHANGER("static_heat_exchanger", "Static Heat Exchanger"),
      FURNACE_WARDEN_CORE("furnace_warden_core", "Furnace Warden Core"),
      ORE_GRINDER("ore_grinder", "Ore Grinder"),
      SALVAGE_SHREDDER("salvage_shredder", "Salvage Shredder"),
      ALLOY_KILN("alloy_kiln", "Alloy Kiln"),
      SUBSTRATE_GRINDER("substrate_grinder", "Substrate Grinder"),
      FLUID_REFINER("fluid_refiner", "Fluid Refiner"),
      WATER_PURIFIER("water_purifier", "Water Purifier"),
      FILTER_PRESS("filter_press", "Filter Press"),
      COMPONENT_ASSEMBLER("component_assembler", "Component Assembler"),
      INDUSTRIAL_RECYCLER("industrial_recycler", "Industrial Recycler"),
      CORRUPTION_SAFE_RECYCLER("corruption_safe_recycler", "Corruption-Safe Recycler"),
      NEXUS_THERMAL_INJECTOR("nexus_thermal_injector", "Nexus-Thermal Injector"),
      REALITY_FURNACE("reality_furnace", "Reality Furnace"),
      FACTORY_CONTROLLER("factory_controller", "Factory Controller"),
      FLUX_CAPACITOR_BANK("flux_capacitor_bank", "Flux Capacitor Bank"),
      REINFORCED_CAPACITOR("reinforced_capacitor", "Reinforced Capacitor"),
      STABILIZED_FLUX_BANK("stabilized_flux_bank", "Stabilized Flux Bank"),
      HYBRID_FLUX_BANK("hybrid_flux_bank", "Hybrid Flux Bank"),
      CORE_FLUX_BANK("core_flux_bank", "Core Flux Bank"),
      INDUSTRIAL_SCRUBBER("industrial_scrubber", "Industrial Scrubber");

      public static final Codec<IndustrialMachineBlock.MachineKind> CODEC = StringRepresentable.fromEnum(IndustrialMachineBlock.MachineKind::values);
      public static final StreamCodec<RegistryFriendlyByteBuf, IndustrialMachineBlock.MachineKind> STREAM_CODEC = ByteBufCodecs.idMapper(
            IndustrialMachineBlock.MachineKind::byId, Enum::ordinal
         )
         .cast();
      private static final IndustrialMachineBlock.MachineKind[] BY_ID = values();
      private final String serializedName;
      private final String displayName;

      private MachineKind(String serializedName, String displayName) {
         this.serializedName = serializedName;
         this.displayName = displayName;
      }

      public String displayName() {
         return this.displayName;
      }

      public boolean recipeDriven() {
         return this == ORE_GRINDER
            || this == SALVAGE_SHREDDER
            || this == ALLOY_KILN
            || this == SUBSTRATE_GRINDER
            || this == FLUID_REFINER
            || this == WATER_PURIFIER
            || this == FILTER_PRESS
            || this == COMPONENT_ASSEMBLER
            || this == INDUSTRIAL_RECYCLER
            || this == CORRUPTION_SAFE_RECYCLER
            || this == NEXUS_THERMAL_INJECTOR
            || this == REALITY_FURNACE;
      }

      public boolean generator() {
         return this == SCRAP_DYNAMO
            || this == THERMAL_ARRAY
            || this == GEOTHERMAL_PUMP
            || this == REACTOR_HEAT_EXCHANGER
            || this == SOLAR_CONCENTRATOR
            || this == STATIC_HEAT_EXCHANGER
            || this == FURNACE_WARDEN_CORE;
      }

      public boolean storesFlux() {
         return this == FLUX_CAPACITOR_BANK
            || this == REINFORCED_CAPACITOR
            || this == STABILIZED_FLUX_BANK
            || this == HYBRID_FLUX_BANK
            || this == CORE_FLUX_BANK;
      }

      public boolean handlesNexusMaterials() {
         return this == NEXUS_THERMAL_INJECTOR
            || this == STATIC_HEAT_EXCHANGER
            || this == CORRUPTION_SAFE_RECYCLER
            || this == REALITY_FURNACE
            || this == HYBRID_FLUX_BANK
            || this == CORE_FLUX_BANK;
      }

      public boolean factoryController() {
         return this == FACTORY_CONTROLLER;
      }

      public boolean usesFluidHandling() {
         return this == FLUID_REFINER
            || this == WATER_PURIFIER
            || this == INDUSTRIAL_SCRUBBER
            || this == NEXUS_THERMAL_INJECTOR
            || this == STATIC_HEAT_EXCHANGER
            || this == REACTOR_HEAT_EXCHANGER;
      }

      public String getSerializedName() {
         return this.serializedName;
      }

      private static IndustrialMachineBlock.MachineKind byId(int id) {
         return id >= 0 && id < BY_ID.length ? BY_ID[id] : ORE_GRINDER;
      }
   }
}
