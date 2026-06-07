package com.knoxhack.echoscreencore.client.component;

import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.api.layout.EchoRect;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.client.parser.EchoNode;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleResolver;
import com.knoxhack.echoscreencore.client.style.EchoStyleSheet;
import com.knoxhack.echoscreencore.client.style.EchoStyleState;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import java.util.List;
import java.util.Locale;

public abstract class AbstractEchoComponent implements EchoComponent {
    private static final EchoStyleResolver STYLE_RESOLVER = new EchoStyleResolver();

    private final EchoNode node;
    private final List<EchoComponent> children;
    private EchoStyle style = EchoStyle.EMPTY;
    private List<EchoStyleSheet> styleSheets = List.of();
    private List<EchoNode> styleAncestors = List.of();
    private EchoDataContext dataContext;
    private EchoRect bounds = EchoRect.ZERO;
    private boolean hovered;
    private boolean focused;
    private int maxScroll;
    private int scrollOffset;

    protected AbstractEchoComponent(EchoNode node, List<EchoComponent> children) {
        this.node = node;
        this.children = children == null ? List.of() : List.copyOf(children);
    }

    @Override
    public EchoNode node() {
        return node;
    }

    @Override
    public EchoStyle style() {
        return style;
    }

    @Override
    public void setStyle(EchoStyle style) {
        this.style = style == null ? EchoStyle.EMPTY : style;
    }

    public void setStyleContext(List<EchoStyleSheet> styleSheets, List<EchoNode> ancestors) {
        this.styleSheets = styleSheets == null ? List.of() : List.copyOf(styleSheets);
        this.styleAncestors = ancestors == null ? List.of() : List.copyOf(ancestors);
    }

    @Override
    public EchoDataContext dataContext() {
        return dataContext;
    }

    @Override
    public void setDataContext(EchoDataContext dataContext) {
        this.dataContext = dataContext;
    }

    @Override
    public List<EchoComponent> children() {
        return children;
    }

    @Override
    public EchoRect bounds() {
        return bounds;
    }

    @Override
    public void setBounds(EchoRect bounds) {
        this.bounds = bounds == null ? EchoRect.ZERO : bounds;
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        int width = EchoStyleValues.length(style, "width", availableWidth, Math.min(availableWidth, 120), context.theme(), context.diagnostics());
        int height = EchoStyleValues.length(style, "height", availableHeight, defaultHeight(context), context.theme(), context.diagnostics());
        return new EchoMeasureResult(Math.max(0, width), Math.max(0, height));
    }

    @Override
    public void render(EchoRenderContext context) {
        if ("hidden".equalsIgnoreCase(style.value("visibility", "visible"))
                || bounds.width() <= 0
                || bounds.height() <= 0) {
            return;
        }
        renderSelf(context);
        for (EchoComponent child : children) {
            child.render(context);
        }
        renderInteractionTooltip(context);
    }

    @Override
    public boolean hovered() {
        return hovered;
    }

    @Override
    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    @Override
    public boolean focused() {
        return focused;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public int scrollOffset() {
        return scrollOffset;
    }

    @Override
    public void setScrollOffset(int scrollOffset) {
        this.scrollOffset = Math.max(0, Math.min(Math.max(0, maxScroll), scrollOffset));
    }

    @Override
    public int maxScroll() {
        return maxScroll;
    }

    public void setMaxScroll(int maxScroll) {
        this.maxScroll = Math.max(0, maxScroll);
        setScrollOffset(scrollOffset);
    }

    public EchoRect contentRect(EchoRenderContext context) {
        EchoStyleValues.Insets padding = EchoStyleValues.insets(style, "padding", context.theme(), context.diagnostics());
        return bounds.inset(padding.left(), padding.top() + contentTopInset(context), padding.right(), padding.bottom());
    }

    protected int defaultHeight(EchoRenderContext context) {
        if (children.isEmpty()) {
            return 18;
        }
        return 48;
    }

    protected String text(EchoRenderContext context) {
        String value = node.attribute("value", "");
        if (value.isBlank()) {
            value = node.text();
        }
        return context.bindingResolver().resolve(value, bindingContext(context), context.diagnostics());
    }

    protected String attr(EchoRenderContext context, String name, String fallback) {
        return context.bindingResolver().resolve(node.attribute(name, fallback), bindingContext(context), context.diagnostics());
    }

    protected EchoDataContext bindingContext(EchoRenderContext context) {
        return dataContext == null ? context.dataContext() : dataContext;
    }

    protected void renderSelf(EchoRenderContext context) {
    }

    protected EchoStyle effectiveStyle(EchoRenderContext context) {
        if (styleSheets.isEmpty() || context == null) {
            return style;
        }
        return STYLE_RESOLVER.resolve(node, styleAncestors, styleSheets, context.accessibility(),
                context.diagnostics(), styleState(context));
    }

    protected EchoStyleState styleState(EchoRenderContext context) {
        boolean selected = truthy(attr(context, "selected", ""));
        boolean active = truthy(attr(context, "active", ""));
        return new EchoStyleState(hovered(), focused(), disabled(), selected, active);
    }

    protected static boolean truthy(String value) {
        String normalized = value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
        return !normalized.isBlank()
                && !"false".equals(normalized)
                && !"0".equals(normalized)
                && !"no".equals(normalized)
                && !"off".equals(normalized);
    }

    protected void renderInteractionTooltip(EchoRenderContext context) {
        if (context.overlays() == null || (!hovered() && !focused())) {
            return;
        }
        String tooltip = attr(context, "tooltip", "");
        if (tooltip.isBlank() && disabled()) {
            tooltip = attr(context, "disabled-reason", "");
        }
        if (tooltip.isBlank()) {
            tooltip = attr(context, "help", "");
        }
        if (!tooltip.isBlank()) {
            context.overlays().requestTooltip(this, tooltip);
        }
    }
}
