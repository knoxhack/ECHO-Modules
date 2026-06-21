package com.knoxhack.echoscreencore.client.component.layout;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.api.layout.EchoRect;
import com.knoxhack.echoscreencore.client.component.EchoComponent;
import com.knoxhack.echoscreencore.client.input.EchoInputRouter;
import com.knoxhack.echoscreencore.client.render.EchoRenderBridge;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import org.lwjgl.glfw.GLFW;

public final class TabsComponent extends ContainerComponent {
    private String selectedId;

    public TabsComponent(EchoComponentFactory.Context context) {
        super(context);
        selectedId = node().attribute("selected", "");
    }

    @Override
    public boolean focusable() {
        return !disabled() && !children().isEmpty();
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        int minHeight = EchoStyleValues.length(style(), "min-height", availableHeight, 120, context.theme(), context.diagnostics());
        int height = EchoStyleValues.length(style(), "height", availableHeight, minHeight, context.theme(), context.diagnostics());
        return new EchoMeasureResult(Math.max(0, availableWidth), Math.max(minHeight, height));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, EchoInputRouter.ActionRunner actions) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !headerRect().contains(mouseX, mouseY)) {
            return false;
        }
        int index = headerIndex(mouseX);
        if (index < 0 || index >= children().size()) {
            return false;
        }
        EchoComponent child = children().get(index);
        if (child.disabled()) {
            return true;
        }
        selectedId = tabId(child);
        return true;
    }

    @Override
    public boolean keyPressed(int key, EchoInputRouter.ActionRunner actions) {
        if (key != GLFW.GLFW_KEY_LEFT && key != GLFW.GLFW_KEY_RIGHT && key != GLFW.GLFW_KEY_UP && key != GLFW.GLFW_KEY_DOWN) {
            return false;
        }
        int current = selectedIndex();
        int delta = key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_UP ? -1 : 1;
        for (int i = 1; i <= children().size(); i++) {
            int next = Math.floorMod(current + delta * i, children().size());
            EchoComponent child = children().get(next);
            if (!child.disabled()) {
                selectedId = tabId(child);
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(EchoRenderContext context) {
        if (!isSubtreeDirty()) {
            return;
        }
        if ("hidden".equalsIgnoreCase(style().value("visibility", "visible"))
                || bounds().width() <= 0
                || bounds().height() <= 0) {
            clearRenderDirty();
            return;
        }
        if (isDirty()) {
            renderSelf(context);
        }
        EchoComponent selected = selectedChild();
        if (selected != null) {
            selected.render(context);
        }
        clearRenderDirty();
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        super.renderSelf(context);
        int count = Math.max(1, children().size());
        int tabWidth = Math.max(44, headerRect().width() / count);
        int x = headerRect().x();
        for (int i = 0; i < children().size(); i++) {
            EchoComponent child = children().get(i);
            boolean selected = child == selectedChild();
            int bg = selected ? context.theme().color("cardSelected", 0xDD123E58) : context.theme().color("overlay", 0x6610243A);
            int border = selected || focused() ? context.theme().color("accent", 0xFF00E5FF) : context.theme().color("borderMuted", 0xFF1A6F8A);
            if (child.disabled()) {
                bg = EchoRenderBridge.withAlpha(context.theme().color("disabled", 0xFF3B4652), 75);
                border = context.theme().color("disabled", 0xFF3B4652);
            }
            context.render().fill(context.graphics(), x, headerRect().y(), tabWidth, headerRect().height(), bg);
            context.render().outline(context.graphics(), x, headerRect().y(), tabWidth, headerRect().height(), border);
            String title = context.bindingResolver().resolve(tabTitle(child), context.dataContext(), context.diagnostics());
            title = context.font().plainSubstrByWidth(title, Math.max(0, tabWidth - 8));
            context.graphics().text(context.font(), title, x + 4, headerRect().y() + 6,
                child.disabled() ? context.theme().color("textMuted", 0xFF8AAFC2) : context.theme().color("textPrimary", 0xFFEAFBFF), false);
            x += tabWidth;
        }
    }

    public EchoRect headerRect() {
        int headerHeight = Math.max(24, EchoStyleValues.intValue(style().value("tab-height", "26"), 26));
        return new EchoRect(bounds().x(), bounds().y(), bounds().width(), Math.min(bounds().height(), headerHeight));
    }

    public EchoRect contentArea() {
        EchoRect header = headerRect();
        return new EchoRect(bounds().x(), header.bottom() + 4, bounds().width(), Math.max(0, bounds().bottom() - header.bottom() - 4));
    }

    public EchoComponent selectedChild() {
        if (children().isEmpty()) {
            return null;
        }
        if (selectedId == null || selectedId.isBlank()) {
            selectedId = tabId(children().get(0));
        }
        for (EchoComponent child : children()) {
            if (tabId(child).equals(selectedId)) {
                return child.disabled() ? firstEnabled() : child;
            }
        }
        return firstEnabled();
    }

    private EchoComponent firstEnabled() {
        for (EchoComponent child : children()) {
            if (!child.disabled()) {
                selectedId = tabId(child);
                return child;
            }
        }
        return children().isEmpty() ? null : children().get(0);
    }

    private int selectedIndex() {
        EchoComponent selected = selectedChild();
        int index = children().indexOf(selected);
        return index < 0 ? 0 : index;
    }

    private int headerIndex(double mouseX) {
        int tabWidth = Math.max(44, headerRect().width() / Math.max(1, children().size()));
        return (int) Math.floor((mouseX - headerRect().x()) / Math.max(1, tabWidth));
    }

    private static String tabId(EchoComponent child) {
        if (child instanceof TabComponent tab) {
            return tab.tabId();
        }
        String id = child.node().attribute("id", "");
        return id.isBlank() ? child.node().attribute("title", child.node().tagName()) : id;
    }

    private static String tabTitle(EchoComponent child) {
        return child instanceof TabComponent tab ? tab.tabTitle() : child.node().attribute("title", tabId(child));
    }
}
