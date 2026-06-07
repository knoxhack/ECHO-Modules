package com.knoxhack.echoindustrialnexus.recipe;

import com.knoxhack.echoindustrialnexus.block.IndustrialMachineBlock;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;
import com.knoxhack.echoindustrialnexus.registry.ModFluids;
import com.knoxhack.echoindustrialnexus.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public record IndustrialProcessingRecipe(
   IndustrialMachineBlock.MachineKind machine,
   Ingredient ingredient,
   Item resultItem,
   int count,
   Item catalystItem,
   int catalystCount,
   Item byproductItem,
   int byproductCount,
   int byproductChance,
   int duration,
   int fluxCost,
   int fluxGeneration,
   int heat,
   int inputFluidId,
   int inputFluidAmount,
   int outputFluidId,
   int outputFluidAmount
) implements Recipe<SingleRecipeInput> {
   private static final StreamCodec<RegistryFriendlyByteBuf, Item> ITEM_STREAM_CODEC = ByteBufCodecs.registry(Registries.ITEM);
   public static final MapCodec<IndustrialProcessingRecipe> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            IndustrialMachineBlock.MachineKind.CODEC.fieldOf("machine").forGetter(IndustrialProcessingRecipe::machine),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(IndustrialProcessingRecipe::ingredient),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(IndustrialProcessingRecipe::resultItem),
            Codec.INT.optionalFieldOf("count", 1).forGetter(IndustrialProcessingRecipe::count),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("catalyst", Items.AIR).forGetter(IndustrialProcessingRecipe::catalystItem),
            Codec.INT.optionalFieldOf("catalystCount", 0).forGetter(IndustrialProcessingRecipe::catalystCount),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("byproduct", Items.AIR).forGetter(IndustrialProcessingRecipe::byproductItem),
            Codec.INT.optionalFieldOf("byproductCount", 0).forGetter(IndustrialProcessingRecipe::byproductCount),
            Codec.INT.optionalFieldOf("byproductChance", 100).forGetter(IndustrialProcessingRecipe::byproductChance),
            Codec.INT.optionalFieldOf("duration", 160).forGetter(IndustrialProcessingRecipe::duration),
            Codec.INT.optionalFieldOf("fluxCost", 80).forGetter(IndustrialProcessingRecipe::fluxCost),
            Codec.INT.optionalFieldOf("fluxGeneration", 0).forGetter(IndustrialProcessingRecipe::fluxGeneration),
            Codec.INT.optionalFieldOf("heat", 1).forGetter(IndustrialProcessingRecipe::heat),
            FluidFields.CODEC.forGetter(IndustrialProcessingRecipe::fluidFields)
         )
         .apply(
            instance,
            (
               machine,
               ingredient,
               resultItem,
               count,
               catalystItem,
               catalystCount,
               byproductItem,
               byproductCount,
               byproductChance,
               duration,
               fluxCost,
               fluxGeneration,
               heat,
               fluidFields
            ) -> new IndustrialProcessingRecipe(
               machine,
               ingredient,
               resultItem,
               count,
               catalystItem,
               catalystCount,
               byproductItem,
               byproductCount,
               byproductChance,
               duration,
               fluxCost,
               fluxGeneration,
               heat,
               fluidFields.inputFluidId(),
               fluidFields.inputFluidAmount(),
               fluidFields.outputFluidId(),
               fluidFields.outputFluidAmount()
            )
         )
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, IndustrialProcessingRecipe> STREAM_CODEC = StreamCodec.of(
      IndustrialProcessingRecipe::write,
      IndustrialProcessingRecipe::read
   );

   private static void write(RegistryFriendlyByteBuf buffer, IndustrialProcessingRecipe recipe) {
      IndustrialMachineBlock.MachineKind.STREAM_CODEC.encode(buffer, recipe.machine);
      Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
      ITEM_STREAM_CODEC.encode(buffer, recipe.resultItem);
      buffer.writeVarInt(recipe.count);
      ITEM_STREAM_CODEC.encode(buffer, recipe.catalystItem);
      buffer.writeVarInt(recipe.catalystCount);
      ITEM_STREAM_CODEC.encode(buffer, recipe.byproductItem);
      buffer.writeVarInt(recipe.byproductCount);
      buffer.writeVarInt(recipe.byproductChance);
      buffer.writeVarInt(recipe.duration);
      buffer.writeVarInt(recipe.fluxCost);
      buffer.writeVarInt(recipe.fluxGeneration);
      buffer.writeVarInt(recipe.heat);
      buffer.writeVarInt(recipe.inputFluidId);
      buffer.writeVarInt(recipe.inputFluidAmount);
      buffer.writeVarInt(recipe.outputFluidId);
      buffer.writeVarInt(recipe.outputFluidAmount);
   }

   private static IndustrialProcessingRecipe read(RegistryFriendlyByteBuf buffer) {
      return new IndustrialProcessingRecipe(
         IndustrialMachineBlock.MachineKind.STREAM_CODEC.decode(buffer),
         Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
         ITEM_STREAM_CODEC.decode(buffer),
         buffer.readVarInt(),
         ITEM_STREAM_CODEC.decode(buffer),
         buffer.readVarInt(),
         ITEM_STREAM_CODEC.decode(buffer),
         buffer.readVarInt(),
         buffer.readVarInt(),
         buffer.readVarInt(),
         buffer.readVarInt(),
         buffer.readVarInt(),
         buffer.readVarInt(),
         buffer.readVarInt(),
         buffer.readVarInt(),
         buffer.readVarInt(),
         buffer.readVarInt()
      );
   }

   public boolean matches(SingleRecipeInput input, Level level) {
      return this.ingredient.test(input.item());
   }

   public boolean matches(IndustrialMachineBlock.MachineKind kind, ItemStack input, Level level) {
      return this.machine == kind && this.matches(new SingleRecipeInput(input), level);
   }

   public boolean matchesFluid(IndustrialMachineBlock.MachineKind kind, int fluidId) {
      return this.machine == kind && this.inputFluidId > IndustrialMachineBlockEntity.FLUID_NONE && this.inputFluidId == fluidId;
   }

   private FluidFields fluidFields() {
      return new FluidFields(this.inputFluidId, ModFluids.identifierFor(this.inputFluidId), this.inputFluidAmount,
         this.outputFluidId, ModFluids.identifierFor(this.outputFluidId), this.outputFluidAmount);
   }

   public ItemStack result() {
      return new ItemStack(this.resultItem, this.count);
   }

   public ItemStack byproduct() {
      return this.byproductItem != Items.AIR && this.byproductCount > 0 ? new ItemStack(this.byproductItem, this.byproductCount) : ItemStack.EMPTY;
   }

   public ItemStack catalyst() {
      return this.catalystItem != Items.AIR && this.catalystCount > 0 ? new ItemStack(this.catalystItem, this.catalystCount) : ItemStack.EMPTY;
   }

   public boolean requiresCatalyst() {
      return !this.catalyst().isEmpty();
   }

   public ItemStack assemble(SingleRecipeInput input) {
      return this.result();
   }

   public boolean showNotification() {
      return true;
   }

   public String group() {
      return "echo_industrial_processing";
   }

   public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
      return (RecipeSerializer<? extends Recipe<SingleRecipeInput>>)ModRecipes.INDUSTRIAL_PROCESSING_SERIALIZER.get();
   }

   public RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
      return (RecipeType<? extends Recipe<SingleRecipeInput>>)ModRecipes.INDUSTRIAL_PROCESSING_TYPE.get();
   }

   public PlacementInfo placementInfo() {
      return PlacementInfo.create(this.ingredient);
   }

   public RecipeBookCategory recipeBookCategory() {
      return RecipeBookCategories.CRAFTING_MISC;
   }

   private record FluidFields(int inputFluidId, String inputFluid, int inputFluidAmount, int outputFluidId, String outputFluid, int outputFluidAmount) {
      private FluidFields {
         int inputAliasId = ModFluids.idForName(inputFluid);
         int outputAliasId = ModFluids.idForName(outputFluid);
         inputFluidId = inputFluidId > IndustrialMachineBlockEntity.FLUID_NONE ? inputFluidId : inputAliasId;
         outputFluidId = outputFluidId > IndustrialMachineBlockEntity.FLUID_NONE ? outputFluidId : outputAliasId;
         inputFluid = inputFluid == null ? "" : inputFluid;
         outputFluid = outputFluid == null ? "" : outputFluid;
      }

      private static final MapCodec<FluidFields> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               Codec.INT.optionalFieldOf("inputFluidId", IndustrialMachineBlockEntity.FLUID_NONE).forGetter(FluidFields::inputFluidId),
               Codec.STRING.optionalFieldOf("inputFluid", "").forGetter(FluidFields::inputFluid),
               Codec.INT.optionalFieldOf("inputFluidAmount", 0).forGetter(FluidFields::inputFluidAmount),
               Codec.INT.optionalFieldOf("outputFluidId", IndustrialMachineBlockEntity.FLUID_NONE).forGetter(FluidFields::outputFluidId),
               Codec.STRING.optionalFieldOf("outputFluid", "").forGetter(FluidFields::outputFluid),
               Codec.INT.optionalFieldOf("outputFluidAmount", 0).forGetter(FluidFields::outputFluidAmount)
            )
            .apply(instance, FluidFields::new)
      );
   }
}
