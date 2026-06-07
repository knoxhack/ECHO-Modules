package com.knoxhack.echonexusprotocol.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class NexusJeiCategory implements IRecipeCategory<NexusJeiRecipe> {
   private static final int TEXT = 0xFFD8F6FF;
   private static final int MUTED = 0xFF88AAB8;
   private static final int BLUE = 0xFF66E8FF;
   private static final int PURPLE = 0xFFB85CFF;
   private final IRecipeType<NexusJeiRecipe> type;
   private final Component title;
   private final IDrawable icon;

   public NexusJeiCategory(IRecipeType<NexusJeiRecipe> type, Component title, IDrawable icon) {
      this.type = type;
      this.title = title;
      this.icon = icon;
   }

   public IRecipeType<NexusJeiRecipe> getRecipeType() {
      return this.type;
   }

   public Component getTitle() {
      return this.title;
   }

   public int getWidth() {
      return 156;
   }

   public int getHeight() {
      return 88;
   }

   public IDrawable getIcon() {
      return this.icon;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, NexusJeiRecipe recipe, IFocusGroup focuses) {
      builder.addSlot(RecipeIngredientRole.INPUT, 18, 28).setStandardSlotBackground().addItemStacks(recipe.inputs());
      if (!recipe.outputs().isEmpty()) {
         builder.addSlot(RecipeIngredientRole.OUTPUT, 118, 28).setOutputSlotBackground().addItemStacks(recipe.outputs());
      }
   }

   public void draw(NexusJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
      var font = Minecraft.getInstance().font;
      graphics.fill(0, 0, this.getWidth(), this.getHeight(), 0xE006101A);
      graphics.fill(4, 4, this.getWidth() - 4, 5, BLUE);
      graphics.fill(4, this.getHeight() - 5, this.getWidth() - 4, this.getHeight() - 4, PURPLE);
      graphics.text(font, "INPUT", 14, 14, MUTED, false);
      graphics.text(font, "OUTPUT", 110, 14, MUTED, false);
      drawArrow(graphics, 64, 35);
      graphics.text(font, String.format(java.util.Locale.ROOT, "%.1fs", recipe.duration() / 20.0F), 61, 52, MUTED, false);
      int y = 64;
      for (Component note : recipe.notes()) {
         if (y > 80) {
            break;
         }
         graphics.text(font, note, 5, y, note.getString().contains("contamination") ? PURPLE : TEXT, false);
         y += 9;
      }
   }

   public Identifier getIdentifier(NexusJeiRecipe recipe) {
      return recipe.id();
   }

   private static void drawArrow(GuiGraphicsExtractor graphics, int x, int y) {
      graphics.fill(x, y, x + 26, y + 4, BLUE);
      graphics.fill(x + 21, y - 4, x + 26, y + 8, BLUE);
      graphics.fill(x + 26, y - 2, x + 30, y + 6, BLUE);
   }
}
