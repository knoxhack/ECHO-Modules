package com.knoxhack.echonexusprotocol.compat.jei;

import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import java.util.List;
import mezz.jei.api.recipe.types.IRecipeType;

public final class NexusJeiRecipeTypes {
   public static final IRecipeType<NexusJeiRecipe> RECYCLER = IRecipeType.create(EchoNexusProtocol.MODID, "nexus_recycler", NexusJeiRecipe.class);
   public static final IRecipeType<NexusJeiRecipe> INFUSER = IRecipeType.create(EchoNexusProtocol.MODID, "nexus_infuser", NexusJeiRecipe.class);
   public static final IRecipeType<NexusJeiRecipe> MEMORY_DECODER = IRecipeType.create(EchoNexusProtocol.MODID, "memory_decoder", NexusJeiRecipe.class);
   public static final IRecipeType<NexusJeiRecipe> REALITY_FORGE = IRecipeType.create(EchoNexusProtocol.MODID, "reality_forge", NexusJeiRecipe.class);
   public static final IRecipeType<NexusJeiRecipe> CORRUPTION_REACTOR = IRecipeType.create(EchoNexusProtocol.MODID, "corruption_reactor", NexusJeiRecipe.class);
   public static final List<IRecipeType<NexusJeiRecipe>> ALL = List.of(RECYCLER, INFUSER, MEMORY_DECODER, REALITY_FORGE, CORRUPTION_REACTOR);

   private NexusJeiRecipeTypes() {
   }
}
