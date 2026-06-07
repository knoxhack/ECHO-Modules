package com.knoxhack.echoscreencore.client.layout;

import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.api.layout.EchoRect;
import com.knoxhack.echoscreencore.client.component.AbstractEchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponent;
import com.knoxhack.echoscreencore.client.component.layout.ScrollPanelComponent;
import com.knoxhack.echoscreencore.client.component.layout.TabsComponent;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class EchoLayoutEngine {
    private static final int SCROLL_CHILD_GUTTER = 8;

    private final EchoResponsiveResolver responsiveResolver = new EchoResponsiveResolver();
    private final Map<EchoComponent, ScrollMetrics> scrollMetrics = new WeakHashMap<>();

    public void layout(EchoComponent root, EchoRenderContext context, int width, int height) {
        if (root != null) {
            layoutComponent(root, new EchoRect(0, 0, Math.max(1, width), Math.max(1, height)), context);
        }
    }

    public void layoutWithin(EchoComponent component, EchoRect bounds, EchoRenderContext context) {
        if (component != null && bounds != null) {
            layoutComponent(component, bounds, context);
        }
    }

    private void layoutComponent(EchoComponent component, EchoRect bounds, EchoRenderContext context) {
        EchoResponsiveRules rules = responsiveResolver.resolve(component, context.responsive());
        if (rules.hidden()) {
            hideTree(component);
            return;
        }
        component.setBounds(bounds);
        if (rules.collapsed()) {
            for (EchoComponent child : component.children()) {
                hideTree(child);
            }
            return;
        }
        if (component.children().isEmpty()) {
            return;
        }
        String layout = component.style().value("layout", component.node().tagName());
        if (component instanceof TabsComponent tabs) {
            layoutTabs(tabs, context);
        } else if (component instanceof ScrollPanelComponent) {
            layoutScroll(component, contentRect(component, context), context);
        } else if (rules.stacked() && gridLike(component, layout)) {
            layoutStacked(component, contentRect(component, context), context);
        } else if (!rules.stacked() && gridLike(component, layout)) {
            layoutGrid(component, contentRect(component, context), context);
        } else if (rules.stacked() && rowLike(component, layout)) {
            layoutStacked(component, contentRect(component, context), context);
        } else if (!rules.stacked() && rowLike(component, layout)) {
            layoutRow(component, contentRect(component, context), context);
        } else {
            layoutColumn(component, contentRect(component, context), context);
        }
    }

    private void layoutStacked(EchoComponent component, EchoRect content, EchoRenderContext context) {
        List<EchoComponent> children = visibleChildren(component, context);
        int gap = gap(component, context);
        int y = content.y();
        for (EchoComponent child : children) {
            int height = responsivePreferredHeight(child, content.width(), content.height(), context, true);
            layoutComponent(child, new EchoRect(content.x(), y, content.width(), Math.max(0, height)), context);
            y += height + gap;
        }
    }

    private void layoutColumn(EchoComponent component, EchoRect content, EchoRenderContext context) {
        List<EchoComponent> children = visibleChildren(component, context);
        int gap = gap(component, context);
        int gaps = Math.max(0, children.size() - 1) * gap;
        int fillCount = 0;
        int fixedHeight = 0;
        List<Integer> preferred = new ArrayList<>();
        for (EchoComponent child : children) {
            int explicit = explicitHeight(child, content.height(), context);
            boolean fill = explicit < 0 && fillChild(child);
            if (fill) {
                fillCount++;
                preferred.add(-1);
            } else {
                int measured = explicit >= 0 ? responsiveHeight(child, explicit, content.height(), context, false)
                        : responsivePreferredHeight(child, content.width(), content.height(), context, false);
                preferred.add(measured);
                fixedHeight += measured;
            }
        }
        int remaining = Math.max(0, content.height() - fixedHeight - gaps);
        int fillHeight = fillCount == 0 ? 0 : remaining / fillCount;
        int y = content.y();
        for (int i = 0; i < children.size(); i++) {
            EchoComponent child = children.get(i);
            int height = preferred.get(i) < 0 ? fillHeight : preferred.get(i);
            layoutComponent(child, new EchoRect(content.x(), y, content.width(), Math.max(0, height)), context);
            y += height + gap;
        }
    }

    private void layoutRow(EchoComponent component, EchoRect content, EchoRenderContext context) {
        List<EchoComponent> children = visibleChildren(component, context);
        int gap = gap(component, context);
        int gaps = Math.max(0, children.size() - 1) * gap;
        int fixedWidth = 0;
        int fillCount = 0;
        List<Integer> widths = new ArrayList<>();
        for (EchoComponent child : children) {
            int explicit = EchoStyleValues.length(child.style(), "width", content.width(), -1, context.theme(), context.diagnostics());
            if (explicit >= 0) {
                int width = boundedWidth(child, explicit, content.width(), context);
                widths.add(width);
                fixedWidth += width;
            } else if (fixedInlineChild(child)) {
                int measured = child.measure(context, content.width(), content.height()).width();
                int width = boundedWidth(child, measured, content.width(), context);
                widths.add(width);
                fixedWidth += width;
            } else {
                widths.add(-1);
                fillCount++;
            }
        }
        int remaining = Math.max(0, content.width() - fixedWidth - gaps);
        int fillWidth = fillCount == 0 ? 0 : remaining / fillCount;
        int x = content.x();
        for (int i = 0; i < children.size(); i++) {
            EchoComponent child = children.get(i);
            int width = widths.get(i) < 0 ? fillWidth : widths.get(i);
            int childHeight = EchoStyleValues.length(child.style(), "height", content.height(), -1, context.theme(), context.diagnostics());
            if (childHeight < 0) {
                childHeight = fillChild(child) || rowLike(child, child.style().value("layout", child.node().tagName()))
                        || verticalContainer(child)
                        ? content.height()
                        : Math.min(content.height(), child.measure(context, width, content.height()).height());
            }
            int minHeight = EchoStyleValues.length(child.style(), "min-height", content.height(), -1, context.theme(), context.diagnostics());
            if (minHeight >= 0) {
                childHeight = Math.max(childHeight, Math.min(content.height(), minHeight));
            }
            childHeight = Math.min(content.height(), Math.max(0, childHeight));
            int childY = content.y() + Math.max(0, (content.height() - childHeight) / 2);
            layoutComponent(child, new EchoRect(x, childY, Math.max(0, width), childHeight), context);
            x += width + gap;
        }
    }

    private void layoutGrid(EchoComponent component, EchoRect content, EchoRenderContext context) {
        List<EchoComponent> children = visibleChildren(component, context);
        if (children.isEmpty()) {
            return;
        }
        int gap = gap(component, context);
        List<Track> columns = tracks(component.style().value("columns", component.node().attribute("columns", "1fr")), content.width(), context);
        if (columns.isEmpty()) {
            columns = List.of(Track.fr(1.0F));
        }
        int columnCount = columns.size();
        int[] widths = resolveTracks(component, columns, content.width(), Math.max(0, columnCount - 1) * gap, context);
        int usedWidth = Math.max(0, columnCount - 1) * gap;
        for (int width : widths) {
            usedWidth += width;
        }
        if (usedWidth > content.width() + 1 && context.diagnostics() != null) {
            context.diagnostics().warnOnce("grid_columns_overflow", component.node().tagName() + "#" + component.node().id());
        }
        int rowCount = Math.max(1, (int) Math.ceil(children.size() / (double) columnCount));
        int rowHeight = Math.max(0, (content.height() - Math.max(0, rowCount - 1) * gap) / rowCount);
        for (int i = 0; i < children.size(); i++) {
            int col = i % columnCount;
            int row = i / columnCount;
            int x = content.x();
            for (int c = 0; c < col; c++) {
                x += widths[c] + gap;
            }
            int y = content.y() + row * (rowHeight + gap);
            layoutComponent(children.get(i), new EchoRect(x, y, widths[col], rowHeight), context);
        }
    }

    private void layoutScroll(EchoComponent component, EchoRect content, EchoRenderContext context) {
        List<EchoComponent> children = visibleChildren(component, context);
        int gap = gap(component, context);
        ScrollMetrics metrics = scrollMetrics(component, children, content, context, gap);
        if (component instanceof AbstractEchoComponent abstractComponent) {
            abstractComponent.setMaxScroll(Math.max(0, metrics.measuredHeight() - content.height()));
            if ("true".equalsIgnoreCase(component.node().attribute("scroll-state", "false"))
                && component.node().hasAttribute("state-key") && component.scrollOffset() == 0 && component.dataContext() != null) {
                component.dataContext().resolve("state." + component.node().attribute("state-key", "") + ".scroll")
                    .ifPresent(value -> component.setScrollOffset(EchoStyleValues.intValue(String.valueOf(value), 0)));
            }
        }
        int y = content.y() - component.scrollOffset();
        int childWidth = Math.max(0, content.width() - SCROLL_CHILD_GUTTER);
        int overscan = scrollOverscan(content);
        int visibleTop = content.y() - overscan;
        int visibleBottom = content.bottom() + overscan;
        for (int i = 0; i < children.size(); i++) {
            EchoComponent child = children.get(i);
            int height = metrics.heights().get(i);
            EchoRect childBounds = new EchoRect(content.x(), y, childWidth, height);
            if (intersectsVertical(childBounds, visibleTop, visibleBottom)) {
                layoutComponent(child, childBounds, context);
            } else {
                child.setBounds(childBounds);
                hideChildren(child);
            }
            y += height + gap;
        }
    }

    private ScrollMetrics scrollMetrics(EchoComponent component, List<EchoComponent> children, EchoRect content,
            EchoRenderContext context, int gap) {
        int signature = scrollSignature(component, children, content, context, gap);
        ScrollMetrics cached = scrollMetrics.get(component);
        if (cached != null && cached.signature() == signature) {
            return cached;
        }
        List<Integer> heights = new ArrayList<>();
        int measuredHeight = 0;
        for (EchoComponent child : children) {
            int height = EchoStyleValues.length(child.style(), "height", content.height(), -1, context.theme(), context.diagnostics());
            EchoResponsiveRules childRules = responsiveResolver.resolve(child, context.responsive());
            if (height < 0 || childRules.stacked() && layoutContainer(child)) {
                height = responsivePreferredHeight(child, Math.max(0, content.width() - SCROLL_CHILD_GUTTER),
                        Math.max(content.height(), 2048), context, false);
            } else {
                height = responsiveHeight(child, height, Math.max(content.height(), 2048), context, false);
            }
            int resolved = Math.max(0, height);
            heights.add(resolved);
            measuredHeight += resolved;
        }
        if (!children.isEmpty()) {
            measuredHeight += Math.max(0, children.size() - 1) * gap;
        }
        ScrollMetrics next = new ScrollMetrics(signature, List.copyOf(heights), measuredHeight);
        scrollMetrics.put(component, next);
        return next;
    }

    private static int scrollSignature(EchoComponent component, List<EchoComponent> children, EchoRect content,
            EchoRenderContext context, int gap) {
        int signature = 17;
        signature = 31 * signature + content.width();
        signature = 31 * signature + content.height();
        signature = 31 * signature + gap;
        signature = 31 * signature + context.responsive().viewportWidth();
        signature = 31 * signature + context.responsive().viewportHeight();
        signature = 31 * signature + context.responsive().activeBreakpoint().hashCode();
        signature = 31 * signature + context.accessibility().hashCode();
        signature = 31 * signature + component.style().properties().hashCode();
        signature = 31 * signature + children.size();
        for (EchoComponent child : children) {
            signature = 31 * signature + System.identityHashCode(child);
            signature = 31 * signature + child.style().properties().hashCode();
            signature = 31 * signature + child.node().tagName().hashCode();
            signature = 31 * signature + child.node().attributes().hashCode();
            signature = 31 * signature + child.children().size();
        }
        return signature;
    }

    private static int scrollOverscan(EchoRect content) {
        return Math.max(48, content.height() / 2);
    }

    private static boolean intersectsVertical(EchoRect bounds, int top, int bottom) {
        return bounds.height() > 0 && bounds.bottom() > top && bounds.y() < bottom;
    }

    private void layoutTabs(TabsComponent component, EchoRenderContext context) {
        EchoRect content = component.contentArea();
        EchoComponent selected = component.selectedChild();
        for (EchoComponent child : component.children()) {
            layoutComponent(child, child == selected ? content : EchoRect.ZERO, context);
        }
    }

    private EchoRect contentRect(EchoComponent component, EchoRenderContext context) {
        if (component instanceof AbstractEchoComponent abstractComponent) {
            return abstractComponent.contentRect(context);
        }
        EchoStyleValues.Insets padding = EchoStyleValues.insets(component.style(), "padding", context.theme(), context.diagnostics());
        return component.bounds().inset(padding.left(), padding.top() + component.contentTopInset(context), padding.right(), padding.bottom());
    }

    private int gap(EchoComponent component, EchoRenderContext context) {
        int gap = EchoStyleValues.length(component.style(), "gap", 0, 0, context.theme(), context.diagnostics());
        EchoResponsiveRules rules = responsiveResolver.resolve(component, context.responsive());
        if (rules.dense()) {
            return Math.max(0, gap / 2);
        }
        if (rules.compact()) {
            return Math.max(0, Math.round(gap * 0.75F));
        }
        return gap;
    }

    private int responsivePreferredHeight(
            EchoComponent component,
            int availableWidth,
            int availableHeight,
            EchoRenderContext context,
            boolean clampToAvailable) {
        int explicit = explicitHeight(component, availableHeight, context);
        EchoResponsiveRules rules = responsiveResolver.resolve(component, context.responsive());
        if (rules.stacked() && layoutContainer(component)) {
            return responsiveHeight(component,
                    stackedContentHeight(component, availableWidth, availableHeight, context),
                    availableHeight, context, clampToAvailable);
        }
        if (explicit >= 0) {
            return responsiveHeight(component, explicit, availableHeight, context, clampToAvailable);
        }
        if (verticalContainer(component)) {
            return responsiveHeight(component,
                    stackedContentHeight(component, availableWidth, availableHeight, context),
                    availableHeight, context, clampToAvailable);
        }
        return responsiveHeight(component,
                component.measure(context, availableWidth, availableHeight).height(),
                availableHeight, context, clampToAvailable);
    }

    private int stackedContentHeight(EchoComponent component, int availableWidth, int availableHeight, EchoRenderContext context) {
        EchoStyleValues.Insets padding = EchoStyleValues.insets(component.style(), "padding", context.theme(), context.diagnostics());
        int contentWidth = Math.max(0, availableWidth - padding.left() - padding.right());
        List<EchoComponent> children = visibleChildren(component, context);
        int height = padding.top() + component.contentTopInset(context) + padding.bottom();
        if (!children.isEmpty()) {
            height += Math.max(0, children.size() - 1) * gap(component, context);
        }
        for (EchoComponent child : children) {
            height += responsivePreferredHeight(child, contentWidth, availableHeight, context, false);
        }
        int minHeight = EchoStyleValues.length(component.style(), "min-height", availableHeight, -1, context.theme(), context.diagnostics());
        if (minHeight >= 0) {
            height = Math.max(height, minHeight);
        }
        return height;
    }

    private int explicitHeight(EchoComponent component, int availableHeight, EchoRenderContext context) {
        return EchoStyleValues.length(component.style(), "height", availableHeight, -1, context.theme(), context.diagnostics());
    }

    private int responsiveHeight(
            EchoComponent component,
            int height,
            int availableHeight,
            EchoRenderContext context,
            boolean clampToAvailable) {
        int resolved = Math.max(0, height);
        int maxHeight = EchoStyleValues.length(component.style(), "max-height", availableHeight, -1, context.theme(), context.diagnostics());
        if (maxHeight >= 0) {
            resolved = Math.min(resolved, maxHeight);
        }
        EchoResponsiveRules rules = responsiveResolver.resolve(component, context.responsive());
        if (clampToAvailable && (rules.stacked() || rules.compact() || rules.dense())) {
            resolved = Math.min(resolved, Math.max(0, availableHeight));
        }
        return resolved;
    }

    private boolean layoutContainer(EchoComponent component) {
        String layout = component.style().value("layout", component.node().tagName());
        return gridLike(component, layout) || rowLike(component, layout) || component.node().tagName().equals("column")
                || component.node().tagName().equals("section") || component.node().tagName().equals("panel")
                || component.node().tagName().equals("screen-shell") || component.node().tagName().equals("scroll");
    }

    private boolean verticalContainer(EchoComponent component) {
        String layout = component.style().value("layout", component.node().tagName());
        return "column".equals(layout) || component.node().tagName().equals("column")
                || component.node().tagName().equals("section") || component.node().tagName().equals("panel")
                || component.node().tagName().equals("screen-shell") || component.node().tagName().equals("scroll");
    }

    private boolean gridLike(EchoComponent component, String layout) {
        return "grid".equals(layout) || component.node().tagName().equals("grid") || component.node().tagName().equals("split-view");
    }

    private boolean rowLike(EchoComponent component, String layout) {
        String tag = component.node().tagName();
        return "row".equals(layout)
                || "horizontal".equals(component.style().value("direction", ""))
                || tag.equals("row")
                || tag.equals("list-row")
                || tag.equals("nav-item")
                || tag.equals("dropdown-item")
                || tag.equals("option");
    }

    private boolean fillChild(EchoComponent child) {
        String tag = child.node().tagName();
        return !child.children().isEmpty() || tag.equals("grid") || tag.equals("scroll") || tag.equals("screen-shell") || tag.equals("panel");
    }

    private int boundedWidth(EchoComponent child, int width, int availableWidth, EchoRenderContext context) {
        int resolved = Math.max(0, width);
        int minWidth = EchoStyleValues.length(child.style(), "min-width", availableWidth, -1, context.theme(), context.diagnostics());
        if (minWidth >= 0) {
            resolved = Math.max(resolved, minWidth);
        }
        int maxWidth = EchoStyleValues.length(child.style(), "max-width", availableWidth, -1, context.theme(), context.diagnostics());
        if (maxWidth >= 0) {
            resolved = Math.min(resolved, maxWidth);
        }
        return Math.min(Math.max(0, availableWidth), resolved);
    }

    private boolean fixedInlineChild(EchoComponent child) {
        String tag = child.node().tagName();
        if (tag.equals("progress-bar")) {
            return child.style().value("width").isPresent();
        }
        return switch (tag) {
            case "button", "status-chip", "icon", "item-icon", "item-stack", "spacer", "input", "search-box",
                    "select", "dropdown", "toggle", "checkbox" -> true;
            default -> false;
        };
    }

    private static List<Track> tracks(String raw, int parentWidth, EchoRenderContext context) {
        ArrayList<Track> tracks = new ArrayList<>();
        for (String part : (raw == null ? "" : raw).split("\\s+")) {
            String value = part.strip();
            if (value.isBlank()) {
                continue;
            }
            if (value.equalsIgnoreCase("auto")) {
                tracks.add(Track.auto());
            } else if (value.endsWith("fr")) {
                tracks.add(Track.fr(number(value.substring(0, value.length() - 2), 1.0F)));
            } else if (value.endsWith("%")) {
                tracks.add(Track.pixels(Math.round(parentWidth * number(value.substring(0, value.length() - 1), 0.0F) / 100.0F)));
            } else {
                float parsed = number(value.replace("px", ""), Float.NaN);
                if (Float.isNaN(parsed)) {
                    if (context.diagnostics() != null) {
                        context.diagnostics().warnOnce("invalid_unit", value);
                    }
                    tracks.add(Track.fr(1.0F));
                } else {
                    tracks.add(Track.pixels(Math.max(0, Math.round(parsed))));
                }
            }
        }
        return List.copyOf(tracks);
    }

    private static int[] resolveTracks(EchoComponent component, List<Track> tracks, int width, int gaps, EchoRenderContext context) {
        int[] values = new int[tracks.size()];
        int fixed = 0;
        for (int i = 0; i < tracks.size(); i++) {
            Track track = tracks.get(i);
            values[i] = track.automatic() ? autoTrackWidth(component, i, tracks.size(), context) : track.pixels();
            fixed += values[i];
        }
        float fr = 0.0F;
        for (Track track : tracks) {
            fr += track.fr();
        }
        int remaining = Math.max(0, width - fixed - gaps);
        int used = 0;
        for (int i = 0; i < tracks.size(); i++) {
            Track track = tracks.get(i);
            if (track.fr() > 0.0F && fr > 0.0F) {
                values[i] = Math.round(remaining * (track.fr() / fr));
            }
            used += values[i];
        }
        if (values.length > 0 && used + gaps < width) {
            values[values.length - 1] += width - gaps - used;
        }
        return values;
    }

    private static int autoTrackWidth(EchoComponent component, int column, int columnCount, EchoRenderContext context) {
        int width = 80;
        for (int i = column; i < component.children().size(); i += columnCount) {
            width = Math.max(width, Math.min(240, component.children().get(i).measure(context, 240, 2048).width()));
        }
        return width;
    }

    private static float number(String raw, float fallback) {
        try {
            return Float.parseFloat(raw == null || raw.isBlank() ? "" + fallback : raw.strip());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private List<EchoComponent> visibleChildren(EchoComponent component, EchoRenderContext context) {
        ArrayList<EchoComponent> visible = new ArrayList<>();
        for (EchoComponent child : component.children()) {
            EchoResponsiveRules childRules = responsiveResolver.resolve(child, context.responsive());
            if (childRules.hidden()) {
                hideTree(child);
            } else {
                visible.add(child);
            }
        }
        return List.copyOf(visible);
    }

    private static void hideTree(EchoComponent component) {
        if (component == null) {
            return;
        }
        component.setBounds(EchoRect.ZERO);
        for (EchoComponent child : component.children()) {
            hideTree(child);
        }
    }

    private static void hideChildren(EchoComponent component) {
        for (EchoComponent child : component.children()) {
            hideTree(child);
        }
    }

    private record ScrollMetrics(int signature, List<Integer> heights, int measuredHeight) {
    }

    private record Track(int pixels, float fr, boolean automatic) {
        private static Track pixels(int pixels) {
            return new Track(pixels, 0.0F, false);
        }

        private static Track fr(float fr) {
            return new Track(0, fr, false);
        }

        private static Track auto() {
            return new Track(0, 0.0F, true);
        }
    }
}
