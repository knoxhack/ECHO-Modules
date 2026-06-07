package com.knoxhack.echoblackboxprotocol.recipe;

import com.knoxhack.echoblackboxprotocol.progression.BlackboxMachineKind;
import com.knoxhack.echoblackboxprotocol.registry.ModRecipes;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public record BlackboxProcessingRecipe(BlackboxMachineKind machine, Ingredient ingredient, Item resultItem, int count, int duration, int stabilityCost)
   implements Recipe<SingleRecipeInput> {
   public static final MapCodec<BlackboxProcessingRecipe> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            BlackboxMachineKind.CODEC.fieldOf("machine").forGetter(BlackboxProcessingRecipe::machine),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(BlackboxProcessingRecipe::ingredient),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(BlackboxProcessingRecipe::resultItem),
            Codec.INT.optionalFieldOf("count", 1).forGetter(BlackboxProcessingRecipe::count),
            Codec.INT.optionalFieldOf("duration", 160).forGetter(BlackboxProcessingRecipe::duration),
            Codec.INT.optionalFieldOf("stabilityCost", 5).forGetter(BlackboxProcessingRecipe::stabilityCost)
         )
         .apply(instance, BlackboxProcessingRecipe::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, BlackboxProcessingRecipe> STREAM_CODEC = StreamCodec.composite(
      BlackboxMachineKind.STREAM_CODEC,
      BlackboxProcessingRecipe::machine,
      Ingredient.CONTENTS_STREAM_CODEC,
      BlackboxProcessingRecipe::ingredient,
      ByteBufCodecs.registry(Registries.ITEM),
      BlackboxProcessingRecipe::resultItem,
      ByteBufCodecs.VAR_INT.cast(),
      BlackboxProcessingRecipe::count,
      ByteBufCodecs.VAR_INT.cast(),
      BlackboxProcessingRecipe::duration,
      ByteBufCodecs.VAR_INT.cast(),
      BlackboxProcessingRecipe::stabilityCost,
      BlackboxProcessingRecipe::new
   );

   public boolean matches(SingleRecipeInput input, Level level) {
      return this.ingredient.test(input.item());
   }

   public boolean matches(BlackboxMachineKind kind, ItemStack input, Level level) {
      return this.machine == kind && this.matches(new SingleRecipeInput(input), level);
   }

   public ItemStack result() {
      return new ItemStack(this.resultItem, this.count);
   }

   public ItemStack assemble(SingleRecipeInput input) {
      return this.result();
   }

   public boolean showNotification() {
      return true;
   }

   public String group() {
      return "echo7_blackbox_processing";
   }

   public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
      return (RecipeSerializer<? extends Recipe<SingleRecipeInput>>)ModRecipes.BLACKBOX_PROCESSING_SERIALIZER.get();
   }

   public RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
      return (RecipeType<? extends Recipe<SingleRecipeInput>>)ModRecipes.BLACKBOX_PROCESSING_TYPE.get();
   }

   public PlacementInfo placementInfo() {
      return PlacementInfo.create(this.ingredient);
   }

   public RecipeBookCategory recipeBookCategory() {
      return RecipeBookCategories.CRAFTING_MISC;
   }
}
