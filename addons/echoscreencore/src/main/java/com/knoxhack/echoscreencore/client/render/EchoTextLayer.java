package com.knoxhack.echoscreencore.client.render;

import com.knoxhack.echoscreencore.api.layout.EchoRect;
import com.knoxhack.echoscreencore.api.style.EchoStyle;
import com.knoxhack.echoscreencore.client.component.basic.TextComponent;
import com.knoxhack.echoscreencore.client.parser.EchoNode;
import com.knoxhack.echoscreencore.client.style.EchoStyleValues;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;

public final class EchoTextLayer {
    private final ArrayList<DrawText> queue = new ArrayList<>();

    public void beginFrame() {
        queue.clear();
    }

    public void queue(
            EchoRenderContext context,
            EchoNode node,
            EchoStyle style,
            EchoRect bounds,
            String value,
            int color,
            boolean shadow) {
        if (context == null || node == null || style == null || bounds == null || value == null || value.isBlank()) {
            if (node != null && value != null) {
                TextComponent.recordDrawForTests(node.tagName(), node.id(), String.join(" ", node.classes()),
                        value, bounds == null ? EchoRect.ZERO : bounds, color, false, false, false,
                        value.isBlank(), false, false, false);
            }
            return;
        }
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            if (context.diagnostics() != null) {
                context.diagnostics().warnOnce("nonblank_text_without_bounds", node.tagName() + "#" + node.id());
            }
            TextComponent.recordDrawForTests(node.tagName(), node.id(), String.join(" ", node.classes()), value,
                    bounds, color, context.render().currentClip() != null, false, false,
                    false, true, false, false);
            return;
        }
        EchoRect clip = context.render().currentClip();
        if ("hidden".equalsIgnoreCase(style.value("overflow", ""))) {
            clip = intersect(clip, bounds);
        }
        if (clip != null && (clip.width() <= 0 || clip.height() <= 0)) {
            if (context.diagnostics() != null) {
                context.diagnostics().warnOnce("text_clipped_before_draw", node.tagName() + "#" + node.id());
            }
            TextComponent.recordDrawForTests(node.tagName(), node.id(), String.join(" ", node.classes()), value,
                    bounds, color, true, false, false, false, false, false, true);
            return;
        }
        int fontSize = EchoStyleValues.length(style, "font-size", 0, 10, context.theme(), context.diagnostics());
        int lineHeight = Math.max(8,
                EchoStyleValues.length(style, "line-height", 0, fontSize + 2, context.theme(), context.diagnostics()));
        int maxLines = Math.max(1, EchoStyleValues.intValue(style.value("max-lines", "999"), 999));
        TextComponent.recordDrawForTests(node.tagName(), node.id(), String.join(" ", node.classes()), value,
                bounds, color, clip != null, true, false, false, false, false, false);
        queue.add(new DrawText(node.tagName(), node.id(), String.join(" ", node.classes()), bounds, clip,
                value, color, shadow, lineHeight, maxLines, style.bool("wrap", false),
                style.value("text-align", "left")));
    }

    public int queuedCount() {
        return queue.size();
    }

    public void flush(EchoRenderContext context) {
        if (context == null || queue.isEmpty()) {
            return;
        }
        if (context.graphics() == null || context.font() == null) {
            for (DrawText text : queue) {
                TextComponent.recordDrawForTests(text.tag, text.id, text.classes, text.value, text.bounds,
                        text.color, text.clip != null, false, false, false, false, true, false);
            }
            queue.clear();
            return;
        }
        ArrayList<DrawText> pending = new ArrayList<>(queue);
        queue.clear();
        for (DrawText text : pending) {
            draw(context, text);
        }
    }

    private void draw(EchoRenderContext context, DrawText text) {
        if (text.clip != null) {
            context.render().enableScissor(context.graphics(), text.clip.x(), text.clip.y(), text.clip.width(), text.clip.height());
        }
        try {
            List<String> lines = text.wrap
                    ? wrap(context.font(), text.value, text.bounds.width())
                    : List.of(trim(context.font(), text.value, text.bounds.width(), false));
            int y = text.bounds.y();
            int drawn = 0;
            for (int i = 0; i < lines.size() && drawn < text.maxLines; i++) {
                String line = lines.get(i);
                if (drawn > 0 && y + Math.min(text.lineHeight, 8) > text.bounds.bottom()) {
                    if (context.diagnostics() != null) {
                        context.diagnostics().warnOnce("text_overflow_without_scroll", text.tag + "#" + text.id);
                    }
                    break;
                }
                if (i == text.maxLines - 1 && lines.size() > text.maxLines) {
                    line = trim(context.font(), line + "...", text.bounds.width(), true);
                }
                if (line.isBlank() && !text.value.isBlank()) {
                    line = text.value.substring(0, Math.min(text.value.length(), 1));
                }
                int drawX = switch (text.align) {
                    case "center" -> text.bounds.x() + Math.max(0, (text.bounds.width() - context.font().width(line)) / 2);
                    case "right", "end" -> text.bounds.right() - context.font().width(line);
                    default -> text.bounds.x();
                };
                context.graphics().text(context.font(), line, drawX, y, text.color, text.shadow);
                TextComponent.recordDrawForTests(text.tag, text.id, text.classes, line,
                        new EchoRect(drawX, y, text.bounds.width(), Math.max(8, Math.min(text.bounds.height(), text.lineHeight))),
                        text.color, text.clip != null, false, true, false, false, false, false);
                y += text.lineHeight;
                drawn++;
            }
        } finally {
            if (text.clip != null) {
                context.render().disableScissor(context.graphics());
            }
        }
    }

    private static EchoRect intersect(EchoRect first, EchoRect second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        int left = Math.max(first.x(), second.x());
        int top = Math.max(first.y(), second.y());
        int right = Math.min(first.right(), second.right());
        int bottom = Math.min(first.bottom(), second.bottom());
        return new EchoRect(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }

    private static String trim(Font font, String value, int width, boolean ellipsis) {
        if (font == null || width <= 0 || value == null) {
            return "";
        }
        String trimmed = font.plainSubstrByWidth(value, width);
        if (trimmed.isBlank() && !value.isBlank() && font.width(value.substring(0, 1)) <= Math.max(1, width)) {
            trimmed = value.substring(0, 1);
        }
        if (ellipsis && !trimmed.endsWith("...") && font.width(value) > width) {
            String base = font.plainSubstrByWidth(value, Math.max(0, width - font.width("...")));
            if (base.isBlank() && !value.isBlank()) {
                base = value.substring(0, 1);
            }
            return base + "...";
        }
        return trimmed;
    }

    private static List<String> wrap(Font font, String value, int width) {
        if (font == null || value == null || value.isBlank()) {
            return List.of("");
        }
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : value.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.width(candidate) <= width || line.isEmpty()) {
                line.setLength(0);
                line.append(trim(font, candidate, width, false));
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(trim(font, word, width, false));
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines.isEmpty() ? List.of("") : List.copyOf(lines);
    }

    private record DrawText(
            String tag,
            String id,
            String classes,
            EchoRect bounds,
            EchoRect clip,
            String value,
            int color,
            boolean shadow,
            int lineHeight,
            int maxLines,
            boolean wrap,
            String align) {
    }
}
