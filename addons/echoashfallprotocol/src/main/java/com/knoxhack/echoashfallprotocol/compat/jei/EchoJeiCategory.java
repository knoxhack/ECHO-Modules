package com.knoxhack.echoashfallprotocol.compat.jei;

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

public class EchoJeiCategory implements IRecipeCategory<EchoJeiRecipe> {
    private static final int TEXT_COLOR = 0xFF404850;
    private static final int DIM_COLOR = 0xFF687480;
    private static final int ACCENT_COLOR = 0xFF2D9FD6;
    private static final int WARNING_COLOR = 0xFFAA6030;

    private final IRecipeType<EchoJeiRecipe> type;
    private final Component title;
    private final IDrawable icon;

    public EchoJeiCategory(IRecipeType<EchoJeiRecipe> type, Component title, IDrawable icon) {
        this.type = type;
        this.title = title;
        this.icon = icon;
    }

    @Override
    public IRecipeType<EchoJeiRecipe> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return 150;
    }

    @Override
    public int getHeight() {
        return 78;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EchoJeiRecipe recipe, IFocusGroup focuses) {
        for (EchoJeiRecipe.EchoJeiSlot slot : recipe.inputs()) {
            builder.addSlot(RecipeIngredientRole.INPUT, slot.x(), slot.y())
                    .setStandardSlotBackground()
                    .addItemStacks(slot.stacks());
        }
        for (EchoJeiRecipe.EchoJeiSlot slot : recipe.outputs()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, slot.x(), slot.y())
                    .setOutputSlotBackground()
                    .addItemStacks(slot.stacks());
        }
    }

    @Override
    public void draw(EchoJeiRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.fill(0, 0, getWidth(), getHeight(), 0x10000000);
        graphics.fill(4, 4, getWidth() - 4, 5, ACCENT_COLOR);

        graphics.text(font, "INPUT", 12, 10, DIM_COLOR, false);
        graphics.text(font, "OUTPUT", 98, 10, DIM_COLOR, false);
        drawArrow(graphics, 66, 34);

        if (recipe.processTicks() > 0) {
            String seconds = String.format(java.util.Locale.ROOT, "%.1fs", recipe.processTicks() / 20.0f);
            graphics.text(font, seconds, 64, 49, DIM_COLOR, false);
        }

        int y = 58;
        for (Component note : recipe.notes()) {
            if (y > 71) {
                break;
            }
            graphics.text(font, note, 4, y, note.getString().startsWith("Requires") ? WARNING_COLOR : TEXT_COLOR, false);
            y += 9;
        }
    }

    @Override
    public net.minecraft.resources.Identifier getIdentifier(EchoJeiRecipe recipe) {
        return recipe.id();
    }

    private static void drawArrow(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 22, y + 4, 0xFF2D9FD6);
        graphics.fill(x + 18, y - 4, x + 22, y + 8, 0xFF2D9FD6);
        graphics.fill(x + 22, y - 2, x + 25, y + 6, 0xFF2D9FD6);
    }
}
