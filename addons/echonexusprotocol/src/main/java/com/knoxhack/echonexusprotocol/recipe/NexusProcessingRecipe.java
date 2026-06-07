package com.knoxhack.echonexusprotocol.recipe;

import com.knoxhack.echonexusprotocol.block.NexusMachineBlock;
import com.knoxhack.echonexusprotocol.registry.ModRecipes;
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

public record NexusProcessingRecipe(
   NexusMachineBlock.MachineKind machine,
   Ingredient ingredient,
   Item resultItem,
   int count,
   int duration,
   int chargeCost,
   int chargeOutput,
   int corruptionDelta,
   int fieldDelta
) implements Recipe<SingleRecipeInput> {
   public static final MapCodec<NexusProcessingRecipe> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            NexusMachineBlock.MachineKind.CODEC.fieldOf("machine").forGetter(NexusProcessingRecipe::machine),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(NexusProcessingRecipe::ingredient),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("result", Items.AIR).forGetter(NexusProcessingRecipe::resultItem),
            Codec.INT.optionalFieldOf("count", 1).forGetter(NexusProcessingRecipe::count),
            Codec.INT.optionalFieldOf("duration", 160).forGetter(NexusProcessingRecipe::duration),
            Codec.INT.optionalFieldOf("chargeCost", 0).forGetter(NexusProcessingRecipe::chargeCost),
            Codec.INT.optionalFieldOf("chargeOutput", 0).forGetter(NexusProcessingRecipe::chargeOutput),
            Codec.INT.optionalFieldOf("corruptionDelta", 0).forGetter(NexusProcessingRecipe::corruptionDelta),
            Codec.INT.optionalFieldOf("fieldDelta", 0).forGetter(NexusProcessingRecipe::fieldDelta)
         )
         .apply(instance, NexusProcessingRecipe::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, NexusProcessingRecipe> STREAM_CODEC = StreamCodec.composite(
      NexusMachineBlock.MachineKind.STREAM_CODEC,
      NexusProcessingRecipe::machine,
      Ingredient.CONTENTS_STREAM_CODEC,
      NexusProcessingRecipe::ingredient,
      ByteBufCodecs.registry(Registries.ITEM),
      NexusProcessingRecipe::resultItem,
      ByteBufCodecs.VAR_INT.cast(),
      NexusProcessingRecipe::count,
      ByteBufCodecs.VAR_INT.cast(),
      NexusProcessingRecipe::duration,
      ByteBufCodecs.VAR_INT.cast(),
      NexusProcessingRecipe::chargeCost,
      ByteBufCodecs.VAR_INT.cast(),
      NexusProcessingRecipe::chargeOutput,
      ByteBufCodecs.VAR_INT.cast(),
      NexusProcessingRecipe::corruptionDelta,
      ByteBufCodecs.VAR_INT.cast(),
      NexusProcessingRecipe::fieldDelta,
      NexusProcessingRecipe::new
   );

   public boolean matches(NexusMachineBlock.MachineKind kind, ItemStack input, Level level) {
      return this.machine == kind && this.ingredient.test(input);
   }

   public ItemStack result() {
      return this.resultItem != Items.AIR && this.count > 0 ? new ItemStack(this.resultItem, this.count) : ItemStack.EMPTY;
   }

   public boolean matches(SingleRecipeInput input, Level level) {
      return this.ingredient.test(input.item());
   }

   public ItemStack assemble(SingleRecipeInput input) {
      return this.result();
   }

   public boolean showNotification() {
      return true;
   }

   public String group() {
      return "echo7_nexus_processing";
   }

   public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
      return (RecipeSerializer<? extends Recipe<SingleRecipeInput>>)ModRecipes.NEXUS_PROCESSING_SERIALIZER.get();
   }

   public RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
      return (RecipeType<? extends Recipe<SingleRecipeInput>>)ModRecipes.NEXUS_PROCESSING_TYPE.get();
   }

   public PlacementInfo placementInfo() {
      return PlacementInfo.create(this.ingredient);
   }

   public RecipeBookCategory recipeBookCategory() {
      return RecipeBookCategories.CRAFTING_MISC;
   }
}
