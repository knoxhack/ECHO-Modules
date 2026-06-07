package com.knoxhack.echoscreencore.client.debug;

import com.knoxhack.echoscreencore.client.component.EchoComponent;
import com.knoxhack.echoscreencore.client.input.EchoFocusManager;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import java.util.List;

public final class EchoDebugOverlay {
    public void render(EchoRenderContext context, EchoComponent root, EchoComponent hoverTarget, EchoFocusManager focusManager) {
        if (context.accessibility().hideDebugInfo()) {
            return;
        }
        drawBounds(context, root, hoverTarget, focusManager.focusOrder());
        List<EchoScreenDiagnostics.Issue> issues = context.diagnostics().issues();
        int x = 8;
        int y = 8;
        context.render().fill(context.graphics(), x - 4, y - 4, 320, Math.min(120, 16 + issues.size() * 10), 0xAA000000);
        String focus = focusManager.focused() == null ? "none" : label(focusManager.focused());
        String hover = hoverTarget == null ? "none" : label(hoverTarget);
        context.graphics().text(context.font(), "ScreenCore Debug: " + issues.size() + " diagnostic(s)", x, y, 0xFF45FFB0, false);
        y += 12;
        context.graphics().text(context.font(), "focus=" + focus + " hover=" + hover, x, y, 0xFFB7D7E3, false);
        y += 10;
        String overlays = context.overlays() == null ? "base" : context.overlays().describeStack();
        String breakpoint = context.responsive() == null ? "?" : context.responsive().activeBreakpoint().name().toLowerCase(java.util.Locale.ROOT);
        context.graphics().text(context.font(), "breakpoint=" + breakpoint + " overlays=" + overlays, x, y, 0xFFB7D7E3, false);
        y += 10;
        for (EchoScreenDiagnostics.Issue issue : issues.stream().limit(9).toList()) {
            String message = issue.code() + ": " + issue.message() + " Fix: " + EchoDiagnosticCatalog.fixHint(issue.code());
            context.graphics().text(context.font(), message, x, y, 0xFFFFD166, false);
            y += 10;
        }
    }

    private void drawBounds(EchoRenderContext context, EchoComponent component, EchoComponent hoverTarget, List<EchoComponent> focusOrder) {
        if (component == null) {
            return;
        }
        int color = component == hoverTarget ? 0xFFFFD166 : focusOrder.contains(component) ? 0xFF45FFB0 : 0x665BC0EB;
        context.render().outline(context.graphics(), component.bounds().x(), component.bounds().y(), component.bounds().width(), component.bounds().height(), color);
        if (component.maxScroll() > 0) {
            context.graphics().text(context.font(), "scroll " + component.scrollOffset() + "/" + component.maxScroll(),
                component.bounds().x() + 3, component.bounds().bottom() - 10, 0xFF5BC0EB, false);
        }
        if (component == hoverTarget) {
            context.graphics().text(context.font(), label(component), component.bounds().x() + 3, component.bounds().y() + 3, 0xFFFFD166, false);
        }
        for (EchoComponent child : component.children()) {
            drawBounds(context, child, hoverTarget, focusOrder);
        }
    }

    private static String label(EchoComponent component) {
        if (component == null) {
            return "";
        }
        String id = component.node().id().isBlank() ? "" : "#" + component.node().id();
        String classes = component.node().classes().isEmpty() ? "" : "." + String.join(".", component.node().classes());
        return component.node().tagName() + id + classes;
    }
}
