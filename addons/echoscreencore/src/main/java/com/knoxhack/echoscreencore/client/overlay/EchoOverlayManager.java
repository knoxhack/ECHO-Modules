package com.knoxhack.echoscreencore.client.overlay;

import com.knoxhack.echoscreencore.api.layout.EchoRect;
import com.knoxhack.echoscreencore.client.component.EchoComponent;
import com.knoxhack.echoscreencore.client.component.data.SelectComponent;
import com.knoxhack.echoscreencore.client.component.layout.DialogComponent;
import com.knoxhack.echoscreencore.client.input.EchoFocusManager;
import com.knoxhack.echoscreencore.client.input.EchoInputRouter;
import com.knoxhack.echoscreencore.client.layout.EchoLayoutEngine;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.lwjgl.glfw.GLFW;

public final class EchoOverlayManager {
    private final EchoLayoutEngine overlayLayout = new EchoLayoutEngine();
    private EchoComponent root;
    private SelectComponent openSelect;
    private EchoComponent renderingModal;
    private EchoComponent modalOpener;
    private String activeModalId = "";
    private TooltipRequest tooltip;

    public void beginFrame(EchoComponent root) {
        this.root = root;
        tooltip = null;
        if (openSelect != null && !contains(root, openSelect)) {
            openSelect = null;
        }
        if (!activeModalId.isBlank() && findById(root, activeModalId) == null) {
            activeModalId = "";
        }
    }

    public void openSelect(SelectComponent select) {
        openSelect = select;
    }

    public boolean isSelectOpen(SelectComponent select) {
        return openSelect == select;
    }

    public void closeSelect() {
        if (openSelect != null) {
            openSelect.close();
        }
        openSelect = null;
    }

    public boolean openModal(String modalId) {
        return openModal(modalId, null, null);
    }

    public boolean openModal(String modalId, EchoFocusManager focusManager, EchoComponent opener) {
        if (modalId == null || modalId.isBlank()) {
            return false;
        }
        EchoComponent modal = findById(root, modalId.strip());
        if (modal == null) {
            return false;
        }
        activeModalId = modalId.strip();
        modalOpener = opener;
        modal.setRenderDirty(true);
        closeSelect();
        if (focusManager != null) {
            focusManager.clearFocus();
        }
        return true;
    }

    public void closeModal() {
        closeModal(null);
    }

    public void closeModal(EchoFocusManager focusManager) {
        activeModalId = "";
        if (focusManager != null && modalOpener != null) {
            focusManager.focus(modalOpener);
        }
        modalOpener = null;
    }

    public boolean hasActiveModal() {
        return !activeModalId.isBlank();
    }

    public String describeStack() {
        ArrayList<String> layers = new ArrayList<>();
        if (openSelect != null) {
            layers.add("select#" + openSelect.node().id());
        }
        if (tooltip != null) {
            layers.add("tooltip");
        }
        if (!activeModalId.isBlank()) {
            layers.add("modal#" + activeModalId);
        }
        return layers.isEmpty() ? "base" : String.join(" > ", layers);
    }

    public boolean isRenderingModal(EchoComponent component) {
        return component != null && component == renderingModal;
    }

    public boolean isModalActive(EchoComponent component) {
        return component != null && !activeModalId.isBlank() && activeModalId.equals(component.node().id());
    }

    public void requestTooltip(EchoComponent owner, String text) {
        if (owner != null && text != null && !text.isBlank()) {
            tooltip = new TooltipRequest(owner, text.strip());
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, EchoInputRouter router,
            EchoInputRouter.ActionRunner actions) {
        EchoComponent modal = findById(root, activeModalId);
        if (modal != null) {
            if (modal.bounds().contains(mouseX, mouseY)) {
                return router.mouseClicked(modal, mouseX, mouseY, button, actions);
            }
            DialogComponent dialog = modal instanceof DialogComponent resolved ? resolved : null;
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && (dialog == null || dialog.closeOnOutside())) {
                closeModal(router.focusManager());
            }
            return true;
        }
        if (openSelect != null) {
            if (openSelect.overlayMouseClicked(mouseX, mouseY, button, actions)) {
                return true;
            }
            closeSelect();
            return true;
        }
        return false;
    }

    public boolean keyPressed(int key, EchoInputRouter router, EchoInputRouter.ActionRunner actions) {
        if (openSelect != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                closeSelect();
                return true;
            }
            if (openSelect.overlayKeyPressed(key, actions)) {
                return true;
            }
        }
        EchoComponent modal = findById(root, activeModalId);
        if (modal != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                DialogComponent dialog = modal instanceof DialogComponent resolved ? resolved : null;
                if (dialog == null || dialog.closeOnEscape()) {
                    closeModal(router.focusManager());
                }
                return true;
            }
            return router.keyPressed(key, actions);
        }
        return false;
    }

    public void render(EchoRenderContext context) {
        if (openSelect != null) {
            openSelect.renderOverlay(context);
        }
        if (tooltip != null && !hasActiveModal()) {
            renderTooltip(context, tooltip);
        }
        EchoComponent modal = findById(root, activeModalId);
        if (modal != null) {
            context.render().fill(context.graphics(), 0, 0, context.screenWidth(), context.screenHeight(), 0x99000000);
            EchoRect bounds = modalBounds(context, modal);
            overlayLayout.layoutWithin(modal, bounds, context);
            context.focusManager().trap(modal);
            renderingModal = modal;
            modal.render(context);
            renderingModal = null;
        }
    }

    private EchoRect modalBounds(EchoRenderContext context, EchoComponent modal) {
        String size = modal.node().attribute("size", "md");
        int defaultWidth = switch (size) {
            case "sm" -> 320;
            case "lg" -> 560;
            case "xl" -> 720;
            case "fullscreen" -> context.screenWidth() - 16;
            default -> 440;
        };
        int defaultHeight = switch (size) {
            case "sm" -> 180;
            case "lg" -> 320;
            case "xl" -> 420;
            case "fullscreen" -> context.screenHeight() - 16;
            default -> 240;
        };
        int fallbackWidth = Math.min(context.screenWidth() - 32, defaultWidth);
        int fallbackHeight = Math.min(context.screenHeight() - 32, defaultHeight);
        int width = Math.max(160, Math.min(context.screenWidth() - 24,
            com.knoxhack.echoscreencore.client.style.EchoStyleValues.length(modal.style(), "width", context.screenWidth(), fallbackWidth, context.theme(), context.diagnostics())));
        int height = Math.max(120, Math.min(context.screenHeight() - 24,
            com.knoxhack.echoscreencore.client.style.EchoStyleValues.length(modal.style(), "height", context.screenHeight(), fallbackHeight, context.theme(), context.diagnostics())));
        return new EchoRect((context.screenWidth() - width) / 2, (context.screenHeight() - height) / 2, width, height);
    }

    private void renderTooltip(EchoRenderContext context, TooltipRequest request) {
        int maxWidth = Math.min(260, Math.max(96, context.screenWidth() - 20));
        List<String> lines = wrapTooltip(context, request.text(), maxWidth - 14);
        int textWidth = 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, context.font().width(line));
        }
        int width = Math.max(48, Math.min(maxWidth, textWidth + 14));
        int height = 10 + lines.size() * 10;
        int x = Math.min(context.screenWidth() - width - 6, Math.max(6, request.owner().bounds().x()));
        int y = request.owner().bounds().bottom() + 6;
        if (y + height > context.screenHeight() - 6) {
            y = request.owner().bounds().y() - height - 6;
        }
        int background = context.theme().color("panel", 0xF008111F);
        int border = context.theme().color("borderStrong", 0xFF5BC0EB);
        context.render().panel(context.graphics(), context.font(), x, y, width, height, background, border, true);
        for (int i = 0; i < lines.size(); i++) {
            context.graphics().text(context.font(), lines.get(i), x + 7, y + 6 + i * 10,
                    context.theme().color("textPrimary", 0xFFEAFBFF), false);
        }
    }

    private static List<String> wrapTooltip(EchoRenderContext context, String raw, int maxWidth) {
        ArrayList<String> lines = new ArrayList<>();
        String[] sourceLines = raw.replace("\\n", "\n").split("\\R");
        for (String source : sourceLines) {
            String line = source.strip();
            if (line.isEmpty()) {
                continue;
            }
            while (!line.isEmpty()) {
                String next = context.font().plainSubstrByWidth(line, maxWidth);
                if (next.isEmpty()) {
                    break;
                }
                lines.add(next.stripTrailing());
                line = line.substring(next.length()).stripLeading();
                if (lines.size() >= 4) {
                    return lines;
                }
            }
        }
        if (lines.isEmpty()) {
            lines.add(context.font().plainSubstrByWidth(raw.strip(), maxWidth));
        }
        return lines.size() > 4 ? List.copyOf(lines.subList(0, 4)) : List.copyOf(lines);
    }

    private static boolean contains(EchoComponent root, EchoComponent target) {
        if (root == null || target == null) {
            return false;
        }
        if (root == target) {
            return true;
        }
        for (EchoComponent child : root.children()) {
            if (contains(child, target)) {
                return true;
            }
        }
        return false;
    }

    private static EchoComponent findById(EchoComponent root, String id) {
        if (root == null || id == null || id.isBlank()) {
            return null;
        }
        Deque<EchoComponent> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            EchoComponent component = stack.pop();
            if (id.equals(component.node().id())) {
                return component;
            }
            for (EchoComponent child : component.children()) {
                stack.push(child);
            }
        }
        return null;
    }

    private record TooltipRequest(EchoComponent owner, String text) {
    }
}
