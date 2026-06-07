package com.knoxhack.echoscreencore.client.component.layout;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoRect;
import com.knoxhack.echoscreencore.client.component.EchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponentSurfaces;
import com.knoxhack.echoscreencore.client.input.EchoInputRouter;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.state.EchoPageStateStore;
import org.lwjgl.glfw.GLFW;

public final class ScrollPanelComponent extends ContainerComponent {
    private boolean draggingThumb;
    private int dragGrabOffset;

    public ScrollPanelComponent(EchoComponentFactory.Context context) {
        super(context);
    }

    @Override
    public boolean focusable() {
        return maxScroll() > 0;
    }

    @Override
    public void render(EchoRenderContext context) {
        if ("hidden".equalsIgnoreCase(style().value("visibility", "visible"))
                || bounds().width() <= 0
                || bounds().height() <= 0) {
            return;
        }
        renderSelf(context);
        context.render().enableScissor(context.graphics(), bounds().x(), bounds().y(), bounds().width(), bounds().height());
        try {
            int overscan = Math.max(48, bounds().height() / 2);
            int top = bounds().y() - overscan;
            int bottom = bounds().bottom() + overscan;
            for (EchoComponent child : children()) {
                if (withinRenderViewport(child.bounds(), top, bottom)) {
                    child.render(context);
                }
            }
        } finally {
            context.render().disableScissor(context.graphics());
        }
        if (maxScroll() > 0) {
            int railX = scrollbarRailX();
            int railH = scrollbarRailHeight();
            int thumbH = scrollbarThumbHeight();
            int thumbY = scrollbarThumbY(railH, thumbH);
            int rail = hovered() || focused() ? 0x772BEAFF : 0x552BEAFF;
            int thumb = focused()
                    ? context.theme().color("focus", context.theme().color("accent", 0xFF00E5FF))
                    : context.theme().color("accent", 0xFF00E5FF);
            if (EchoComponentSurfaces.isGlass(style())) {
                context.render().glassPanel(context.graphics(), context.font(), railX - 1, bounds().y() + 4,
                        4, railH, rail, 0x00000000, thumb, 3, 0, hovered() ? 16 : 8,
                        false, "bevel", "", 0, true);
                context.render().glassPanel(context.graphics(), context.font(), railX - 2, thumbY,
                        6, thumbH, thumb, 0x00000000, thumb, 3, 0, hovered() ? 26 : 14,
                        true, "bevel", "", 0, true);
            } else {
                context.render().fill(context.graphics(), railX, bounds().y() + 4, 2, railH, rail);
                context.render().fill(context.graphics(), railX - 1, thumbY, 4, thumbH, thumb);
            }
            if (focused()) {
                context.render().outline(context.graphics(), bounds().x(), bounds().y(), bounds().width(), bounds().height(),
                        context.theme().color("focus", context.theme().color("accent", 0xFF00E5FF)));
            }
        }
        renderInteractionTooltip(context);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, EchoInputRouter.ActionRunner actions) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !hasScrollbar() || !insideScrollbarRail(mouseX, mouseY)) {
            return false;
        }
        int thumbY = scrollbarThumbY(scrollbarRailHeight(), scrollbarThumbHeight());
        if (insideScrollbarThumb(mouseX, mouseY)) {
            dragGrabOffset = (int) Math.round(mouseY - thumbY);
        } else {
            dragGrabOffset = scrollbarThumbHeight() / 2;
            setScrollFromMouse(mouseY);
        }
        draggingThumb = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY,
            EchoInputRouter.ActionRunner actions) {
        if (!draggingThumb || button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !hasScrollbar()) {
            return false;
        }
        setScrollFromMouse(mouseY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button, EchoInputRouter.ActionRunner actions) {
        if (!draggingThumb || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        draggingThumb = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        if (maxScroll() <= 0 || !bounds().contains(mouseX, mouseY)) {
            return false;
        }
        int before = scrollOffset();
        setScrollOffset(scrollOffset() - (int) Math.round(deltaY * 24.0D));
        if (scrollOffset() == before) {
            return false;
        }
        persistScrollOffset();
        return true;
    }

    private boolean withinRenderViewport(EchoRect childBounds, int top, int bottom) {
        return childBounds.width() > 0
                && childBounds.height() > 0
                && childBounds.right() > bounds().x()
                && childBounds.x() < bounds().right()
                && childBounds.bottom() > top
                && childBounds.y() < bottom;
    }

    private boolean hasScrollbar() {
        return maxScroll() > 0 && bounds().width() > 0 && bounds().height() > 0;
    }

    private int scrollbarRailX() {
        return bounds().right() - 5;
    }

    private int scrollbarRailHeight() {
        return Math.max(12, bounds().height() - 8);
    }

    private int scrollbarThumbHeight() {
        int railHeight = scrollbarRailHeight();
        return Math.max(12, railHeight * bounds().height() / Math.max(1, bounds().height() + maxScroll()));
    }

    private int scrollbarThumbY(int railHeight, int thumbHeight) {
        return bounds().y() + 4 + (railHeight - thumbHeight) * scrollOffset() / Math.max(1, maxScroll());
    }

    private boolean insideScrollbarRail(double mouseX, double mouseY) {
        int railX = scrollbarRailX();
        return mouseX >= railX - 6
                && mouseX <= bounds().right() + 2
                && mouseY >= bounds().y() + 4
                && mouseY <= bounds().y() + 4 + scrollbarRailHeight();
    }

    private boolean insideScrollbarThumb(double mouseX, double mouseY) {
        int thumbY = scrollbarThumbY(scrollbarRailHeight(), scrollbarThumbHeight());
        return insideScrollbarRail(mouseX, mouseY)
                && mouseY >= thumbY
                && mouseY <= thumbY + scrollbarThumbHeight();
    }

    private void setScrollFromMouse(double mouseY) {
        int railHeight = scrollbarRailHeight();
        int thumbHeight = scrollbarThumbHeight();
        int travel = Math.max(1, railHeight - thumbHeight);
        int localY = (int) Math.round(mouseY - dragGrabOffset - (bounds().y() + 4));
        int clampedY = Math.max(0, Math.min(travel, localY));
        setScrollOffset(Math.round((float) clampedY * (float) maxScroll() / (float) travel));
        persistScrollOffset();
    }

    private void persistScrollOffset() {
        if ("true".equalsIgnoreCase(node().attribute("scroll-state", "false"))
                && dataContext() != null
                && node().hasAttribute("state-key")) {
            EchoPageStateStore.put(dataContext(), node().attribute("state-key", "") + ".scroll", scrollOffset());
        }
    }
}
