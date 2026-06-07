package com.knoxhack.echoscreencore.client.component.basic;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.client.component.AbstractEchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponentSupport;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import net.minecraft.resources.Identifier;

public final class IconComponent extends AbstractEchoComponent {
    public IconComponent(EchoComponentFactory.Context context) {
        super(EchoComponentSupport.node(context), EchoComponentSupport.children(context));
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        int width = EchoStyleValues.length(style(), "width", availableWidth,
            EchoVisualResources.intAttr(attr(context, "width", ""), 20), context.theme(), context.diagnostics());
        int height = EchoStyleValues.length(style(), "height", availableHeight,
            EchoVisualResources.intAttr(attr(context, "height", ""), width), context.theme(), context.diagnostics());
        return new EchoMeasureResult(Math.max(8, Math.min(availableWidth, width)), Math.max(8, height));
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        Identifier texture = EchoVisualResources.texture(textureName(context), "eui/icons", context, "missing_icon");
        int tint = EchoStyleValues.color(style(), "color", context.theme(), context.theme().color("accent", 0xFF00E5FF), context.diagnostics());
        if (texture != null) {
            context.render().enableScissor(context.graphics(), bounds().x(), bounds().y(), bounds().width(), bounds().height());
            try {
                context.graphics().blit(texture, bounds().x(), bounds().y(), bounds().right(), bounds().bottom(), 0.0F, 1.0F, 0.0F, 1.0F);
            } finally {
                context.render().disableScissor(context.graphics());
            }
            return;
        }
        int x = bounds().x();
        int y = bounds().y();
        int w = bounds().width();
        int h = bounds().height();
        context.render().fill(context.graphics(), x + w / 2 - 1, y + 3, 2, Math.max(2, h - 6), tint);
        context.render().fill(context.graphics(), x + 3, y + h / 2 - 1, Math.max(2, w - 6), 2, tint);
        context.render().outline(context.graphics(), x, y, w, h, EchoRenderAlpha.withAlpha(tint, 170));
    }

    private String textureName(EchoRenderContext context) {
        String texture = attr(context, "texture", "");
        if (texture.isBlank()) {
            texture = attr(context, "src", "");
        }
        if (texture.isBlank()) {
            texture = attr(context, "icon", "");
        }
        if (texture.isBlank()) {
            texture = attr(context, "name", "");
        }
        return texture;
    }

    private static final class EchoRenderAlpha {
        private static int withAlpha(int color, int alpha) {
            return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
        }
    }
}
