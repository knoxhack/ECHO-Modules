package com.knoxhack.echoscreencore.client.input;

import com.knoxhack.echoscreencore.client.component.EchoComponent;
import com.knoxhack.echoscreencore.client.state.EchoPageStateStore;
import org.lwjgl.glfw.GLFW;

public final class EchoInputRouter {
    private final EchoFocusManager focusManager;
    private final EchoKeyboardRouter keyboardRouter = new EchoKeyboardRouter();
    private EchoComponent hoverTarget;
    private EchoComponent hoverRoot;
    private double hoverMouseX = Double.NaN;
    private double hoverMouseY = Double.NaN;
    private boolean hoverDirty = true;
    private int hoverHitTests;
    private EchoComponent activeMouseTarget;
    private int activeMouseButton = -1;

    public EchoInputRouter(EchoFocusManager focusManager) {
        this.focusManager = focusManager;
    }

    public EchoFocusManager focusManager() {
        return focusManager;
    }

    public EchoComponent hoverTarget() {
        return hoverTarget;
    }

    public void invalidateHover() {
        hoverDirty = true;
    }

    public int hoverHitTestsForTests() {
        return hoverHitTests;
    }

    public void updateHover(EchoComponent root, double mouseX, double mouseY) {
        if (!hoverDirty && root == hoverRoot
                && Double.compare(mouseX, hoverMouseX) == 0
                && Double.compare(mouseY, hoverMouseY) == 0) {
            return;
        }
        hoverRoot = root;
        hoverMouseX = mouseX;
        hoverMouseY = mouseY;
        hoverDirty = false;
        hoverHitTests++;
        EchoComponent next = hit(root, mouseX, mouseY);
        if (next == hoverTarget) {
            return;
        }
        if (hoverTarget != null) {
            hoverTarget.setHovered(false);
        }
        hoverTarget = next;
        if (hoverTarget != null) {
            hoverTarget.setHovered(true);
        }
    }

    public boolean mouseClicked(EchoComponent root, double mouseX, double mouseY, int button, ActionRunner actions) {
        EchoComponent target = hit(root, mouseX, mouseY);
        if (target == null) {
            return false;
        }
        if (disabledActionBarrier(root, target)) {
            return true;
        }
        if (target.focusable()) {
            focusManager.focus(target);
        }
        if (target.mouseClicked(mouseX, mouseY, button, actions)) {
            activeMouseTarget = target;
            activeMouseButton = button;
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && target.focusable() && !target.action().isBlank()) {
            return actions.run(target.action(), target, "mouse");
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            EchoComponent actionable = parentAction(root, target);
            if (actionable != null) {
                focusManager.focus(actionable);
                return actions.run(actionable.action(), actionable, "mouse");
            }
        }
        return target.focusable();
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button, ActionRunner actions) {
        EchoComponent target = activeMouseTarget;
        activeMouseTarget = null;
        activeMouseButton = -1;
        return target != null && target.mouseReleased(mouseX, mouseY, button, actions);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY,
            ActionRunner actions) {
        if (activeMouseTarget == null || button != activeMouseButton) {
            return false;
        }
        boolean handled = activeMouseTarget.mouseDragged(mouseX, mouseY, button, dragX, dragY, actions);
        if (handled) {
            invalidateHover();
        }
        return handled;
    }

    public boolean mouseScrolled(EchoComponent root, double mouseX, double mouseY, double deltaY) {
        EchoComponent target = hit(root, mouseX, mouseY);
        if (target != null && target.mouseScrolled(mouseX, mouseY, deltaY)) {
            return true;
        }
        while (target != null) {
            if (target.maxScroll() > 0) {
                int before = target.scrollOffset();
                target.setScrollOffset(target.scrollOffset() - (int) Math.round(deltaY * 24.0D));
                if (target.scrollOffset() == before) {
                    return false;
                }
                if ("true".equalsIgnoreCase(target.node().attribute("scroll-state", "false"))
                    && target.dataContext() != null && target.node().hasAttribute("state-key")) {
                    EchoPageStateStore.put(target.dataContext(), target.node().attribute("state-key", "") + ".scroll", target.scrollOffset());
                }
                invalidateHover();
                return true;
            }
            target = parent(root, target);
        }
        return false;
    }

    public boolean keyPressed(int key, ActionRunner actions) {
        if (key == GLFW.GLFW_KEY_TAB) {
            return keyboardRouter.shiftDown() ? focusManager.focusPrevious() : focusManager.focusNext();
        }
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE) && focusManager.focused() != null) {
            if (focusManager.focused().disabled()) {
                focusManager.clearFocus();
                return true;
            }
            String action = focusManager.focused().action();
            if (focusManager.focused().keyPressed(key, actions)) {
                return true;
            }
            return !action.isBlank() && actions.run(action, focusManager.focused(), "key");
        }
        if (focusManager.focused() != null && focusManager.focused().keyPressed(key, actions)) {
            return true;
        }
        if (focusManager.focused() != null && focusManager.focused().maxScroll() > 0
            && (key == GLFW.GLFW_KEY_PAGE_DOWN || key == GLFW.GLFW_KEY_PAGE_UP || key == GLFW.GLFW_KEY_HOME || key == GLFW.GLFW_KEY_END)) {
            EchoComponent focused = focusManager.focused();
            int delta = key == GLFW.GLFW_KEY_PAGE_DOWN ? 80 : key == GLFW.GLFW_KEY_PAGE_UP ? -80 : 0;
            if (key == GLFW.GLFW_KEY_HOME) {
                focused.setScrollOffset(0);
            } else if (key == GLFW.GLFW_KEY_END) {
                focused.setScrollOffset(focused.maxScroll());
            } else {
                focused.setScrollOffset(focused.scrollOffset() + delta);
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_DOWN) {
            return focusManager.focusNext();
        }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_UP) {
            return focusManager.focusPrevious();
        }
        return false;
    }

    public boolean charTyped(String typed, ActionRunner actions) {
        return focusManager.focused() != null && focusManager.focused().charTyped(typed, actions);
    }

    private EchoComponent hit(EchoComponent component, double mouseX, double mouseY) {
        if (component == null || !component.bounds().contains(mouseX, mouseY)) {
            return null;
        }
        for (int i = component.children().size() - 1; i >= 0; i--) {
            EchoComponent child = component.children().get(i);
            EchoComponent hit = hit(child, mouseX, mouseY);
            if (hit != null) {
                return hit;
            }
        }
        return component;
    }

    private EchoComponent parent(EchoComponent root, EchoComponent child) {
        if (root == null || child == null) {
            return null;
        }
        for (EchoComponent candidate : root.children()) {
            if (candidate == child) {
                return root;
            }
            EchoComponent parent = parent(candidate, child);
            if (parent != null) {
                return parent;
            }
        }
        return null;
    }

    private EchoComponent parentAction(EchoComponent root, EchoComponent child) {
        EchoComponent candidate = parent(root, child);
        while (candidate != null) {
            if (candidate.disabled() && !candidate.action().isBlank()) {
                return null;
            }
            if (candidate.focusable() && !candidate.disabled() && !candidate.action().isBlank()) {
                return candidate;
            }
            candidate = parent(root, candidate);
        }
        return null;
    }

    private boolean disabledActionBarrier(EchoComponent root, EchoComponent target) {
        EchoComponent candidate = target;
        while (candidate != null) {
            if (candidate.disabled() && (!candidate.action().isBlank() || candidate.focusable())) {
                return true;
            }
            candidate = parent(root, candidate);
        }
        return false;
    }

    @FunctionalInterface
    public interface ActionRunner {
        boolean run(String action, EchoComponent component, String inputEvent);
    }
}
