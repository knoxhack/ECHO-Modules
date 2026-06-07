package com.knoxhack.echoscreencore.client.component.basic;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.api.layout.EchoRect;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.client.component.layout.ContainerComponent;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import java.util.ArrayList;
import java.util.List;

public final class CopyBlockComponent extends ContainerComponent {
    public CopyBlockComponent(EchoComponentFactory.Context context) {
        super(context);
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        EchoStyle current = effectiveStyle(context);
        int titleHeight = titleLineHeight(context, current);
        int detailHeight = detailLineHeight(context, current);
        int gap = textGap(context, current);
        int lines = title(context).isBlank() ? 0 : 1;
        if (!detail(context).isBlank()) {
            lines++;
        }
        int textHeight = lines <= 0 ? 0 : titleHeight + (lines > 1 ? gap + detailHeight : 0);
        int childHeight = children().isEmpty()
                ? 0
                : EchoStyleValues.length(current, "content-height", availableHeight, 6, context.theme(), context.diagnostics());
        int height = EchoStyleValues.length(current, "height", availableHeight,
                textHeight + (childHeight > 0 && textHeight > 0 ? gap : 0) + childHeight,
                context.theme(), context.diagnostics());
        int minHeight = EchoStyleValues.length(current, "min-height", availableHeight, -1,
                context.theme(), context.diagnostics());
        if (minHeight >= 0) {
            height = Math.max(height, minHeight);
        }
        return new EchoMeasureResult(Math.max(0, availableWidth), Math.max(0, height));
    }

    @Override
    public int contentTopInset(EchoRenderContext context) {
        if (children().isEmpty()) {
            return 0;
        }
        EchoStyle current = effectiveStyle(context);
        int inset = copyHeight(context, current);
        return inset <= 0 ? 0 : inset + textGap(context, current);
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        EchoStyle current = effectiveStyle(context);
        String title = title(context);
        String detail = detail(context);
        if (title.isBlank() && detail.isBlank()) {
            return;
        }
        if (bounds().width() <= 0 || bounds().height() <= 0) {
            record(context, title.isBlank() ? detail : title, bounds(), 0, false, false, true);
            return;
        }
        if (context.graphics() == null || context.font() == null) {
            record(context, title.isBlank() ? detail : title, bounds(), 0, false, true, false);
            return;
        }

        boolean explicitClip = "hidden".equalsIgnoreCase(current.value("overflow", ""));
        boolean clipped = explicitClip || context.render().currentClip() != null;
        if (explicitClip) {
            context.render().enableScissor(context.graphics(), bounds().x(), bounds().y(), bounds().width(), bounds().height());
        }
        try {
            int y = bounds().y();
            int textWidth = Math.max(0, bounds().width());
            if (!title.isBlank()) {
                int color = EchoStyleValues.color(current, "title-color", context.theme(),
                        EchoStyleValues.color(current, "color", context.theme(),
                                context.theme().color("textPrimary", 0xFFEAFBFF), context.diagnostics()),
                        context.diagnostics());
                drawLine(context, current, title, bounds().x(), y, textWidth, color, true, clipped);
                y += titleLineHeight(context, current) + textGap(context, current);
            }
            if (!detail.isBlank()) {
                int color = EchoStyleValues.color(current, "detail-color", context.theme(),
                        EchoStyleValues.color(current, "subtitle-color", context.theme(),
                                context.theme().color("textSecondary", 0xFFB7D7E3), context.diagnostics()),
                        context.diagnostics());
                drawLine(context, current, detail, bounds().x(), y, textWidth, color, false, clipped);
            }
        } finally {
            if (explicitClip) {
                context.render().disableScissor(context.graphics());
            }
        }
    }

    private void drawLine(EchoRenderContext context, EchoStyle current, String value, int x, int y, int width,
            int color, boolean titleLine, boolean clipped) {
        int maxLines = Math.max(1, EchoStyleValues.intValue(current.value(titleLine ? "title-max-lines" : "detail-max-lines",
                current.value("max-lines", "1")), 1));
        List<String> lines = current.bool(titleLine ? "title-wrap" : "detail-wrap", current.bool("wrap", false))
                ? wrap(context, value, width)
                : List.of(trim(context, value, width, false));
        int lineHeight = titleLine ? titleLineHeight(context, current) : detailLineHeight(context, current);
        int drawn = 0;
        for (int i = 0; i < lines.size() && drawn < maxLines; i++) {
            String line = lines.get(i);
            if (i == maxLines - 1 && lines.size() > maxLines) {
                line = trim(context, line + "...", width, true);
            }
            int drawX = switch (current.value("text-align", "left")) {
                case "center" -> x + Math.max(0, (width - context.font().width(line)) / 2);
                case "right", "end" -> x + Math.max(0, width - context.font().width(line));
                default -> x;
            };
            context.graphics().text(context.font(), line, drawX, y, color, titleLine && current.bool("title-shadow", false));
            TextComponent.recordDrawForTests(node().tagName(), node().id(), String.join(" ", node().classes()),
                    line, new EchoRect(drawX, y, width, lineHeight), color, clipped, false,
                    true, false, false, false, false);
            y += lineHeight;
            drawn++;
        }
    }

    private String title(EchoRenderContext context) {
        String value = attr(context, "title", "");
        if (value.isBlank()) {
            value = attr(context, "value", "");
        }
        return value == null ? "" : value;
    }

    private String detail(EchoRenderContext context) {
        for (String attribute : List.of("subtitle", "summary", "detail", "body")) {
            String value = attr(context, attribute, "");
            if (!value.isBlank()) {
                return value;
            }
        }
        return node().text();
    }

    private int copyHeight(EchoRenderContext context, EchoStyle current) {
        int height = 0;
        if (!title(context).isBlank()) {
            height += titleLineHeight(context, current);
        }
        if (!detail(context).isBlank()) {
            height += (height > 0 ? textGap(context, current) : 0) + detailLineHeight(context, current);
        }
        return height;
    }

    private int titleLineHeight(EchoRenderContext context, EchoStyle current) {
        return Math.max(8, EchoStyleValues.length(current, "title-line-height", 0,
                EchoStyleValues.length(current, "line-height", 0, 11, context.theme(), context.diagnostics()),
                context.theme(), context.diagnostics()));
    }

    private int detailLineHeight(EchoRenderContext context, EchoStyle current) {
        return Math.max(8, EchoStyleValues.length(current, "detail-line-height", 0,
                EchoStyleValues.length(current, "line-height", 0, 10, context.theme(), context.diagnostics()),
                context.theme(), context.diagnostics()));
    }

    private int textGap(EchoRenderContext context, EchoStyle current) {
        return Math.max(0, EchoStyleValues.length(current, "text-gap", 0, 1, context.theme(), context.diagnostics()));
    }

    private String trim(EchoRenderContext context, String value, int width, boolean ellipsis) {
        if (value == null || width <= 0 || context.font() == null) {
            return "";
        }
        String trimmed = context.font().plainSubstrByWidth(value, width);
        if (trimmed.isBlank() && !value.isBlank() && context.font().width(value.substring(0, 1)) <= Math.max(1, width)) {
            trimmed = value.substring(0, 1);
        }
        if (ellipsis && !trimmed.endsWith("...") && context.font().width(value) > width) {
            String base = context.font().plainSubstrByWidth(value, Math.max(0, width - context.font().width("...")));
            if (base.isBlank() && !value.isBlank()) {
                base = value.substring(0, 1);
            }
            return base + "...";
        }
        return trimmed;
    }

    private List<String> wrap(EchoRenderContext context, String value, int width) {
        if (value == null || value.isBlank()) {
            return List.of("");
        }
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (context.font().width(candidate) <= width || line.isEmpty()) {
                line.setLength(0);
                line.append(trim(context, candidate, width, false));
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(trim(context, word, width, false));
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines.isEmpty() ? List.of("") : List.copyOf(lines);
    }

    private void record(EchoRenderContext context, String value, EchoRect bounds, int color, boolean skippedBlank,
            boolean skippedNoGraphics, boolean skippedBounds) {
        TextComponent.recordDrawForTests(node().tagName(), node().id(), String.join(" ", node().classes()),
                value == null ? "" : value, bounds, color, context.render().currentClip() != null, false,
                false, skippedBlank, skippedBounds, skippedNoGraphics, false);
    }
}
