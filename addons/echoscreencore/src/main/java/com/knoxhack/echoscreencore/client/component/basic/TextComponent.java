package com.knoxhack.echoscreencore.client.component.basic;

import com.knoxhack.echoscreencore.api.component.EchoComponentFactory;
import com.knoxhack.echoscreencore.api.layout.EchoMeasureResult;
import com.knoxhack.echoscreencore.api.layout.EchoRect;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.client.component.AbstractEchoComponent;
import com.knoxhack.echoscreencore.client.component.EchoComponentSupport;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TextComponent extends AbstractEchoComponent {
    private static Consumer<TextDrawRecord> drawProbe;

    public TextComponent(EchoComponentFactory.Context context) {
        super(EchoComponentSupport.node(context), EchoComponentSupport.children(context));
    }

    public static void setDrawProbeForTests(Consumer<TextDrawRecord> probe) {
        drawProbe = probe;
    }

    @Override
    public EchoMeasureResult measure(EchoRenderContext context, int availableWidth, int availableHeight) {
        EchoStyle current = effectiveStyle(context);
        int lineHeight = lineHeight(context, current);
        int width = EchoStyleValues.length(current, "width", availableWidth, availableWidth, context.theme(), context.diagnostics());
        int lines = current.bool("wrap", false) && context.font() != null
                ? wrap(context, text(context), Math.max(1, width)).size()
                : 1;
        return new EchoMeasureResult(Math.max(0, width), Math.max(lineHeight, lines * lineHeight));
    }

    @Override
    protected void renderSelf(EchoRenderContext context) {
        EchoStyle current = effectiveStyle(context);
        int color = EchoStyleValues.color(current, "color", context.theme(), context.theme().color("textSecondary", 0xFFB7D7E3));
        renderText(context, current, color, false);
    }

    protected void renderText(EchoRenderContext context, int color, boolean shadow) {
        renderText(context, effectiveStyle(context), color, shadow);
    }

    protected void renderText(EchoRenderContext context, EchoStyle current, int color, boolean shadow) {
        String value = text(context);
        if (value == null) {
            value = "";
        }
        if (value.isBlank()) {
            recordDraw(value, bounds(), color, context.render().currentClip() != null, false,
                    false, true, false, false, false);
            return;
        }
        if (bounds().width() <= 0 || bounds().height() <= 0) {
            if (context.diagnostics() != null) {
                context.diagnostics().warnOnce("nonblank_text_without_bounds", node().tagName() + "#" + node().id());
            }
            recordDraw(value, bounds(), color, context.render().currentClip() != null, false,
                    false, false, true, false, false);
            return;
        }
        String textLayerMode = current.value("text-layer", "").trim();
        if (context.textLayer() != null
                && ("overlay".equalsIgnoreCase(textLayerMode) || "debug".equalsIgnoreCase(textLayerMode))) {
            context.textLayer().queue(context, node(), current, bounds(), value, color, shadow);
            return;
        }
        boolean explicitClip = "hidden".equalsIgnoreCase(current.value("overflow", ""));
        boolean clippedByParent = context.render().currentClip() != null;
        if (context.graphics() == null || context.font() == null) {
            recordDraw(value, bounds(), color, explicitClip || clippedByParent, false,
                    false, false, false, true, false);
            return;
        }
        int x = bounds().x();
        int y = bounds().y();
        int lineHeight = lineHeight(context, current);
        int maxLines = Math.max(1, EchoStyleValues.intValue(current.value("max-lines", "999"), 999));
        List<String> lines = current.bool("wrap", false) ? wrap(context, value, bounds().width()) : List.of(trim(context, value, bounds().width(), false));
        boolean clip = explicitClip;
        if (clip) {
            context.render().enableScissor(context.graphics(), bounds().x(), bounds().y(), bounds().width(), bounds().height());
        }
        try {
            int drawn = 0;
            for (int i = 0; i < lines.size() && drawn < maxLines; i++) {
                String line = lines.get(i);
                if (drawn > 0 && y + Math.min(lineHeight, 8) > bounds().bottom()) {
                    if (context.diagnostics() != null && !value.isBlank()) {
                        context.diagnostics().warnOnce("text_overflow_without_scroll", node().tagName() + "#" + node().id());
                    }
                    break;
                }
                if (i == maxLines - 1 && lines.size() > maxLines) {
                    line = trim(context, line + "...", bounds().width(), true);
                    if (context.diagnostics() != null) {
                        context.diagnostics().warnOnce("text_overflow_without_scroll", node().tagName() + "#" + node().id());
                    }
                }
                int drawX = switch (current.value("text-align", "left")) {
                    case "center" -> bounds().x() + Math.max(0, (bounds().width() - context.font().width(line)) / 2);
                    case "right", "end" -> bounds().right() - context.font().width(line);
                    default -> x;
                };
                context.graphics().text(context.font(), line, drawX, y, color, shadow);
                recordDraw(line, new EchoRect(drawX, y, bounds().width(), Math.max(8, Math.min(bounds().height(), lineHeight))),
                        color, explicitClip || clippedByParent, false,
                        true, false, false, false, false);
                y += lineHeight;
                drawn++;
            }
        } finally {
            if (clip) {
                context.render().disableScissor(context.graphics());
            }
        }
        if (lineHeight < 8 && context.diagnostics() != null) {
            context.diagnostics().warnOnce("text_below_readable_size", node().tagName() + "#" + node().id());
        }
    }

    public static void recordDrawForTests(
            String tag,
            String id,
            String classes,
            String value,
            EchoRect bounds,
            int color,
            boolean clipped,
            boolean queued,
            boolean drawCalled,
            boolean skippedBlank,
            boolean skippedBounds,
            boolean skippedNoGraphics,
            boolean clippedAway) {
        Consumer<TextDrawRecord> probe = drawProbe;
        if (probe != null && value != null) {
            probe.accept(new TextDrawRecord(tag, id, classes, value, bounds, color, clipped, queued, drawCalled,
                    skippedBlank, skippedBounds, skippedNoGraphics, clippedAway));
        }
    }

    private void recordDraw(String value, EchoRect bounds, int color, boolean clipped, boolean queued,
            boolean drawCalled, boolean skippedBlank, boolean skippedBounds, boolean skippedNoGraphics,
            boolean clippedAway) {
        recordDrawForTests(node().tagName(), node().id(), String.join(" ", node().classes()), value, bounds,
                color, clipped, queued, drawCalled, skippedBlank, skippedBounds, skippedNoGraphics, clippedAway);
    }

    protected int lineHeight(EchoRenderContext context) {
        return lineHeight(context, style());
    }

    protected int lineHeight(EchoRenderContext context, EchoStyle current) {
        int fontSize = EchoStyleValues.length(current, "font-size", 0, 10, context.theme(), context.diagnostics());
        return Math.max(8, EchoStyleValues.length(current, "line-height", 0, fontSize + 2, context.theme(), context.diagnostics()));
    }

    protected String trim(EchoRenderContext context, String value, int width) {
        return trim(context, value, width, false);
    }

    protected String trim(EchoRenderContext context, String value, int width, boolean ellipsis) {
        if (width <= 0 || value == null) {
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

    protected List<String> wrap(EchoRenderContext context, String value, int width) {
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

    public record TextDrawRecord(
            String tag,
            String id,
            String classes,
            String value,
            EchoRect bounds,
            int color,
            boolean clipped,
            boolean queued,
            boolean drawCalled,
            boolean skippedBlank,
            boolean skippedBounds,
            boolean skippedNoGraphics,
            boolean clippedAway) {
        public boolean drawn() {
            return drawCalled;
        }

        public String status() {
            if (drawCalled) {
                return "draw_called";
            }
            if (queued) {
                return "queued";
            }
            if (skippedBlank) {
                return "skipped_blank";
            }
            if (skippedBounds) {
                return "skipped_bounds";
            }
            if (skippedNoGraphics) {
                return "skipped_no_graphics";
            }
            if (clippedAway) {
                return "clipped";
            }
            return "resolved";
        }
    }
}
