package com.knoxhack.echoscreencore.client.component;

import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.api.layout.EchoRect;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.client.parser.EchoNode;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import java.util.List;

public interface EchoComponent {
    EchoNode node();

    EchoStyle style();

    void setStyle(EchoStyle style);

    default EchoDataContext dataContext() {
        return null;
    }

    default void setDataContext(EchoDataContext dataContext) {
    }

    default EchoComponent parent() {
        return null;
    }

    default void setParent(EchoComponent parent) {
    }

    default boolean renderDirty() {
        return true;
    }

    default void setRenderDirty(boolean dirty) {
    }

    default boolean subtreeRenderDirty() {
        return true;
    }

    default void setSubtreeRenderDirty(boolean dirty) {
    }

    List<EchoComponent> children();

    EchoRect bounds();

    void setBounds(EchoRect bounds);

    EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight);

    void render(EchoRenderContext context);

    default boolean focusable() {
        return false;
    }

    default boolean participatesInFocus() {
        return bounds().width() > 0 && bounds().height() > 0 && !"hidden".equalsIgnoreCase(style().value("visibility", "visible"));
    }

    default boolean disabled() {
        if ("disabled".equalsIgnoreCase(node().attribute("variant", ""))) {
            return true;
        }
        if (!node().hasAttribute("disabled")) {
            return false;
        }
        String rawValue = node().attribute("disabled", "").strip();
        String value = rawValue.toLowerCase(java.util.Locale.ROOT);
        if (rawValue.startsWith("{") && rawValue.endsWith("}")) {
            String expression = rawValue.substring(1, rawValue.length() - 1);
            String[] parts = expression.split("\\|", 2);
            String resolved = dataContext() == null
                    ? (parts.length > 1 ? parts[1] : "")
                    : dataContext().resolveToString(parts[0].strip());
            if ((resolved == null || resolved.isBlank()) && parts.length > 1) {
                resolved = parts[1];
            }
            value = resolved == null ? "" : resolved.strip().toLowerCase(java.util.Locale.ROOT);
        }
        return value.isBlank()
                || (!"false".equals(value) && !"0".equals(value) && !"no".equals(value) && !"off".equals(value)
                        && !value.endsWith("|false}") && !value.endsWith("|0}"));
    }

    default String action() {
        return node().attribute("action", "");
    }

    default String actionValue() {
        return node().attribute("action-value", node().attribute("action-target", ""));
    }

    default String currentValue() {
        return "";
    }

    default boolean mouseClicked(double mouseX, double mouseY, int button, com.knoxhack.echoscreencore.client.input.EchoInputRouter.ActionRunner actions) {
        return false;
    }

    default boolean mouseReleased(double mouseX, double mouseY, int button, com.knoxhack.echoscreencore.client.input.EchoInputRouter.ActionRunner actions) {
        return false;
    }

    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY, com.knoxhack.echoscreencore.client.input.EchoInputRouter.ActionRunner actions) {
        return false;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double deltaY) {
        return false;
    }

    default boolean keyPressed(int key, com.knoxhack.echoscreencore.client.input.EchoInputRouter.ActionRunner actions) {
        return false;
    }

    default boolean charTyped(String typed, com.knoxhack.echoscreencore.client.input.EchoInputRouter.ActionRunner actions) {
        return false;
    }

    boolean hovered();

    void setHovered(boolean hovered);

    boolean focused();

    void setFocused(boolean focused);

    default int scrollOffset() {
        return 0;
    }

    default void setScrollOffset(int scrollOffset) {
    }

    default int maxScroll() {
        return 0;
    }

    default int contentTopInset(EchoRenderContext context) {
        return node().hasAttribute("title") ? 16 : 0;
    }
}
