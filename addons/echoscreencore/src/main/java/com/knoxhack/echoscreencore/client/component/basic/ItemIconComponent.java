package com.knoxhack.echoscreencore.client.component.basic;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.client.component.AbstractEchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponentSupport;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ItemIconComponent extends AbstractEchoComponent {
    public ItemIconComponent(EchoComponentFactory.Context context) {
        super(EchoComponentSupport.node(context), EchoComponentSupport.children(context));
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        int size = EchoStyleValues.length(style(), "width", availableWidth,
            EchoVisualResources.intAttr(attr(context, "size", ""), 20), context.theme(), context.diagnostics());
        int height = EchoStyleValues.length(style(), "height", availableHeight, size, context.theme(), context.diagnostics());
        return new EchoMeasureResult(Math.max(18, Math.min(availableWidth, size)), Math.max(18, height));
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        ItemStack stack = stack(context);
        int slot = Math.min(bounds().width(), bounds().height());
        int x = bounds().x() + Math.max(0, (bounds().width() - slot) / 2);
        int y = bounds().y() + Math.max(0, (bounds().height() - slot) / 2);
        int border = hovered() ? context.theme().color("accent", 0xFF00E5FF) : context.theme().color("borderMuted", 0xFF1A6F8A);
        context.render().fill(context.graphics(), x, y, slot, slot, context.theme().color("overlay", 0x6610243A));
        context.render().outline(context.graphics(), x, y, slot, slot, border);
        if (!stack.isEmpty() && slot >= 18) {
            context.graphics().item(stack, x + Math.max(1, (slot - 16) / 2), y + Math.max(1, (slot - 16) / 2));
            if (drawDecorations()) {
                context.graphics().itemDecorations(context.font(), stack, x + Math.max(1, (slot - 16) / 2), y + Math.max(1, (slot - 16) / 2));
            }
            if (hovered() && tooltipEnabled(context)) {
                context.graphics().setTooltipForNextFrame(context.font(), stack, x + slot / 2, y + slot / 2);
            }
        } else {
            int muted = context.theme().color("textMuted", 0xFF8AAFC2);
            context.render().fill(context.graphics(), x + 5, y + slot / 2 - 1, Math.max(4, slot - 10), 2, muted);
        }
    }

    protected boolean drawDecorations() {
        return true;
    }

    protected ItemStack stack(EchoRenderContext context) {
        String raw = attr(context, "item", "");
        if (raw.isBlank()) {
            if (context.diagnostics() != null) {
                context.diagnostics().warnOnce("invalid_item_id", node().tagName() + " missing item");
            }
            return ItemStack.EMPTY;
        }
        try {
            Identifier id = Identifier.parse(raw);
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
            if (item == Items.AIR) {
                if (context.diagnostics() != null) {
                    context.diagnostics().warnOnce("invalid_item_id", raw);
                }
                return ItemStack.EMPTY;
            }
            return new ItemStack(item, count(context));
        } catch (RuntimeException exception) {
            if (context.diagnostics() != null) {
                context.diagnostics().warnOnce("invalid_item_id", raw);
            }
            return ItemStack.EMPTY;
        }
    }

    protected int count(EchoRenderContext context) {
        try {
            return Math.max(1, Integer.parseInt(attr(context, "count", "1")));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private boolean tooltipEnabled(EchoRenderContext context) {
        return Boolean.parseBoolean(attr(context, "tooltip", "true"));
    }
}
