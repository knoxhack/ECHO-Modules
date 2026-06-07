package com.knoxhack.echoconvoyprotocol.recipe;

import com.knoxhack.echoconvoyprotocol.block.ConvoyBlock.ConvoyBlockKind;
import com.knoxhack.echoconvoyprotocol.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

public record ConvoyStationRecipe(
   ConvoyBlockKind station,
   List<StationIngredient> ingredients,
   Item resultItem,
   int count,
   int duration,
   int energyCost
) implements Recipe<SingleRecipeInput> {
   public static final MapCodec<ConvoyStationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
      ConvoyBlockKind.CODEC.fieldOf("station").forGetter(ConvoyStationRecipe::station),
      Ingredient.CODEC.optionalFieldOf("ingredient").forGetter(ConvoyStationRecipe::legacyIngredient),
      StationIngredient.CODEC.listOf().optionalFieldOf("ingredients", List.of()).forGetter(ConvoyStationRecipe::ingredients),
      BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(ConvoyStationRecipe::resultItem),
      Codec.INT.optionalFieldOf("count", 1).forGetter(ConvoyStationRecipe::count),
      Codec.INT.optionalFieldOf("duration", 120).forGetter(ConvoyStationRecipe::duration),
      Codec.INT.optionalFieldOf("energyCost", 15).forGetter(ConvoyStationRecipe::energyCost)
   ).apply(instance, (station, ingredient, ingredients, result, count, duration, energyCost) ->
      new ConvoyStationRecipe(station, mergeIngredients(ingredient, ingredients), result, count, duration, energyCost)));

   public static final StreamCodec<RegistryFriendlyByteBuf, ConvoyStationRecipe> STREAM_CODEC = StreamCodec.composite(
      ConvoyBlockKind.STREAM_CODEC, ConvoyStationRecipe::station,
      StationIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), ConvoyStationRecipe::ingredients,
      ByteBufCodecs.registry(Registries.ITEM), ConvoyStationRecipe::resultItem,
      ByteBufCodecs.VAR_INT.cast(), ConvoyStationRecipe::count,
      ByteBufCodecs.VAR_INT.cast(), ConvoyStationRecipe::duration,
      ByteBufCodecs.VAR_INT.cast(), ConvoyStationRecipe::energyCost,
      ConvoyStationRecipe::new);

   public ConvoyStationRecipe {
      ingredients = List.copyOf(ingredients == null ? List.of() : ingredients.stream()
         .filter(ingredient -> ingredient != null && !ingredient.ingredient().isEmpty())
         .toList());
   }

   @Override
   public boolean matches(SingleRecipeInput input, Level level) {
      return ingredients.size() == 1 && ingredients.getFirst().ingredient().test(input.item());
   }

   public boolean matches(ConvoyBlockKind kind, List<ItemStack> inputs, Level level) {
      return station == kind && hasIngredients(inputs);
   }

   public ItemStack result() {
      return new ItemStack(resultItem, Math.max(1, count));
   }

   public boolean consumeIngredients(List<ItemStack> inputs) {
      if (!hasIngredients(inputs)) {
         return false;
      }
      for (StationIngredient requirement : ingredients) {
         int remaining = requirement.count();
         for (ItemStack stack : inputs) {
            if (remaining <= 0) {
               break;
            }
            if (!stack.isEmpty() && requirement.ingredient().test(stack)) {
               int moved = Math.min(remaining, stack.getCount());
               stack.shrink(moved);
               remaining -= moved;
            }
         }
      }
      return true;
   }

   private boolean hasIngredients(List<ItemStack> inputs) {
      if (ingredients.isEmpty()) {
         return false;
      }
      List<ItemStack> simulated = inputs.stream().map(ItemStack::copy).toList();
      for (StationIngredient requirement : ingredients) {
         int remaining = requirement.count();
         for (ItemStack stack : simulated) {
            if (remaining <= 0) {
               break;
            }
            if (!stack.isEmpty() && requirement.ingredient().test(stack)) {
               int moved = Math.min(remaining, stack.getCount());
               stack.shrink(moved);
               remaining -= moved;
            }
         }
         if (remaining > 0) {
            return false;
         }
      }
      return true;
   }

   private Optional<Ingredient> legacyIngredient() {
      if (ingredients.size() == 1 && ingredients.getFirst().count() == 1) {
         return Optional.of(ingredients.getFirst().ingredient());
      }
      return Optional.empty();
   }

   @Override
   public ItemStack assemble(SingleRecipeInput input) {
      return result();
   }

   @Override
   public boolean showNotification() {
      return true;
   }

   @Override
   public String group() {
      return "echo_convoy_station_processing";
   }

   @Override
   public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
      return ModRecipes.CONVOY_STATION_PROCESSING_SERIALIZER.get();
   }

   @Override
   public RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
      return ModRecipes.CONVOY_STATION_PROCESSING_TYPE.get();
   }

   @Override
   public PlacementInfo placementInfo() {
      return PlacementInfo.create(ingredients.stream().map(StationIngredient::ingredient).toList());
   }

   @Override
   public RecipeBookCategory recipeBookCategory() {
      return RecipeBookCategories.CRAFTING_MISC;
   }

   private static List<StationIngredient> mergeIngredients(Optional<Ingredient> legacy, List<StationIngredient> ingredients) {
      List<StationIngredient> merged = new ArrayList<>();
      legacy.ifPresent(ingredient -> merged.add(new StationIngredient(ingredient, 1)));
      if (ingredients != null) {
         merged.addAll(ingredients);
      }
      return merged;
   }

   public record StationIngredient(Ingredient ingredient, int count) {
      public static final Codec<StationIngredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
         Ingredient.CODEC.fieldOf("ingredient").forGetter(StationIngredient::ingredient),
         Codec.INT.optionalFieldOf("count", 1).forGetter(StationIngredient::count)
      ).apply(instance, StationIngredient::new));

      public static final StreamCodec<RegistryFriendlyByteBuf, StationIngredient> STREAM_CODEC = StreamCodec.composite(
         Ingredient.CONTENTS_STREAM_CODEC, StationIngredient::ingredient,
         ByteBufCodecs.VAR_INT.cast(), StationIngredient::count,
         StationIngredient::new);

      public StationIngredient {
         count = Math.max(1, count);
      }
   }
}
