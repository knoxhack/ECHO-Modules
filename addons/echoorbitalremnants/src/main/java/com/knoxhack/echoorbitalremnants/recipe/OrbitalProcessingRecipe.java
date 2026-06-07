package com.knoxhack.echoorbitalremnants.recipe;

import com.knoxhack.echoorbitalremnants.block.OrbitalMachineBlock.MachineKind;
import com.knoxhack.echoorbitalremnants.registry.ModRecipes;
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

public record OrbitalProcessingRecipe(
        MachineKind machine,
        Ingredient ingredient,
        Item resultItem,
        int count,
        int duration,
        int chargeCost
) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<OrbitalProcessingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MachineKind.CODEC.fieldOf("machine").forGetter(OrbitalProcessingRecipe::machine),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(OrbitalProcessingRecipe::ingredient),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(OrbitalProcessingRecipe::resultItem),
            com.mojang.serialization.Codec.INT.optionalFieldOf("count", 1).forGetter(OrbitalProcessingRecipe::count),
            com.mojang.serialization.Codec.INT.optionalFieldOf("duration", 160).forGetter(OrbitalProcessingRecipe::duration),
            com.mojang.serialization.Codec.INT.optionalFieldOf("chargeCost", 20).forGetter(OrbitalProcessingRecipe::chargeCost)
    ).apply(instance, OrbitalProcessingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, OrbitalProcessingRecipe> STREAM_CODEC = StreamCodec.composite(
            MachineKind.STREAM_CODEC, OrbitalProcessingRecipe::machine,
            Ingredient.CONTENTS_STREAM_CODEC, OrbitalProcessingRecipe::ingredient,
            ByteBufCodecs.registry(Registries.ITEM), OrbitalProcessingRecipe::resultItem,
            ByteBufCodecs.VAR_INT.cast(), OrbitalProcessingRecipe::count,
            ByteBufCodecs.VAR_INT.cast(), OrbitalProcessingRecipe::duration,
            ByteBufCodecs.VAR_INT.cast(), OrbitalProcessingRecipe::chargeCost,
            OrbitalProcessingRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    public boolean matches(MachineKind kind, ItemStack input, Level level) {
        return machine == kind && matches(new SingleRecipeInput(input), level);
    }

    public ItemStack result() {
        return new ItemStack(resultItem, count);
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
        return "echo7_orbital_processing";
    }

    @Override
    public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
        return ModRecipes.ORBITAL_PROCESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
        return ModRecipes.ORBITAL_PROCESSING_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(ingredient);
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
