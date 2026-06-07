package com.knoxhack.echoscreencore.client.component.basic;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.api.layout.EchoRect;
import com.knoxhack.echoscreencore.client.component.AbstractEchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponentSupport;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import net.minecraft.resources.Identifier;

public final class ImageComponent extends AbstractEchoComponent {
    private String cachedTextureName = "";
    private Identifier cachedTexture;
    private boolean cachedTextureResolved;

    public ImageComponent(EchoComponentFactory.Context context) {
        super(EchoComponentSupport.node(context), EchoComponentSupport.children(context));
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        int width = EchoStyleValues.length(style(), "width", availableWidth,
            EchoVisualResources.intAttr(attr(context, "width", ""), Math.min(availableWidth, 96)), context.theme(), context.diagnostics());
        int height = EchoStyleValues.length(style(), "height", availableHeight,
            EchoVisualResources.intAttr(attr(context, "height", ""), Math.min(availableHeight, 64)), context.theme(), context.diagnostics());
        return new EchoMeasureResult(Math.max(8, Math.min(availableWidth, width)), Math.max(8, height));
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        Identifier texture = texture(context);
        if (texture == null) {
            int border = context.theme().color("borderMuted", 0xFF1A6F8A);
            context.render().fill(context.graphics(), bounds().x(), bounds().y(), bounds().width(), bounds().height(), context.theme().color("overlay", 0x6610243A));
            context.render().outline(context.graphics(), bounds().x(), bounds().y(), bounds().width(), bounds().height(), border);
            String label = context.font().plainSubstrByWidth("missing image", Math.max(0, bounds().width() - 8));
            context.graphics().text(context.font(), label, bounds().x() + 4, bounds().y() + Math.max(4, (bounds().height() - 8) / 2),
                context.theme().color("textMuted", 0xFF8AAFC2), false);
            return;
        }
        EchoRect draw = EchoVisualResources.fit(bounds(), EchoVisualResources.intAttr(attr(context, "source-width", ""), bounds().width()),
            EchoVisualResources.intAttr(attr(context, "source-height", ""), bounds().height()), attr(context, "fit", "contain"));
        context.render().enableScissor(context.graphics(), bounds().x(), bounds().y(), bounds().width(), bounds().height());
        try {
            context.graphics().blit(texture, draw.x(), draw.y(), draw.right(), draw.bottom(), 0.0F, 1.0F, 0.0F, 1.0F);
        } finally {
            context.render().disableScissor(context.graphics());
        }
    }

    private Identifier texture(EchoRenderContext context) {
        String textureName = textureName(context);
        if (!cachedTextureResolved || !textureName.equals(cachedTextureName)) {
            cachedTextureName = textureName;
            cachedTexture = EchoVisualResources.texture(textureName, "textures/gui", context, "missing_image");
            cachedTextureResolved = true;
        }
        return cachedTexture;
    }

    private String textureName(EchoRenderContext context) {
        String texture = attr(context, "texture", "");
        if (texture.isBlank()) {
            texture = attr(context, "src", "");
        }
        return texture;
    }
}
