package com.knoxhack.echotutorialcore.client;

import com.knoxhack.echo.adaptercore.EchoBackendClientBridge;
import com.knoxhack.echotutorialcore.config.TutorialConfig;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class TutorialToastOverlay {
    private static final ArrayDeque<Entry> QUEUE = new ArrayDeque<>();
    private static final int MAX_QUEUE = 5;
    private static Entry active;
    private static int frames;
    private static BooleanSupplier internalToastRendererAllowed = () -> true;

    private TutorialToastOverlay() {}

    public static void push(String title, String message, String details, boolean danger) {
        if (!clientEnabled(danger)) {
            return;
        }
        while (QUEUE.size() >= MAX_QUEUE) {
            QUEUE.removeFirst();
        }
        QUEUE.addLast(new Entry(clean(title, "ECHO-7"), clean(message, ""), clean(details, ""), danger));
    }

    public static void tick(Object event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            QUEUE.clear();
            active = null;
            frames = 0;
            return;
        }
        if (mc.options.hideGui || mc.screen != null) {
            return;
        }
        int duration = durationTicks();
        if (active == null) {
            active = QUEUE.pollFirst();
            frames = 0;
        } else if (++frames >= duration) {
            active = QUEUE.pollFirst();
            frames = 0;
        }
    }

    public static void render(Object event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null || active == null
                || !internalToastRendererAllowed()) {
            return;
        }
        GuiGraphicsExtractor graphics = EchoBackendClientBridge.guiGraphics(event);
        if (graphics != null) {
            renderToast(graphics, active, frames + EchoBackendClientBridge.guiPartialTick(event));
        }
    }

    public static void setInternalToastRendererAllowedSupplier(BooleanSupplier supplier) {
        internalToastRendererAllowed = supplier == null ? () -> true : supplier;
    }

    public static Optional<NoticeSnapshot> activeNoticeSnapshotForHud() {
        return active == null ? Optional.empty() : Optional.of(toNoticeSnapshot(active));
    }

    public static NoticeSnapshot noticeSnapshotForTests(String title, String message, String details, boolean danger) {
        return toNoticeSnapshot(new Entry(clean(title, "ECHO-7"), clean(message, ""), clean(details, ""), danger));
    }

    private static void renderToast(GuiGraphicsExtractor graphics, Entry entry, float age) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int w = Math.max(220, Math.min(330, screenW - 16));
        int h = entry.details().isBlank() ? 58 : 72;
        float enter = Math.min(1.0F, age / 10.0F);
        float exit = age > durationTicks() - 16 ? Math.min(1.0F, (age - (durationTicks() - 16)) / 16.0F) : 0.0F;
        int slide = Math.round((1.0F - enter + exit) * 34.0F);
        int x = screenW - w - 8 + slide;
        int y = 78;
        int accent = entry.danger() ? 0xFFFF665E : 0xFF92F7A6;

        graphics.fill(x, y, x + w, y + h, 0xDD091014);
        graphics.fill(x, y, x + 3, y + h, accent);
        graphics.fill(x + 6, y + 6, x + w - 6, y + 7, 0x6630473E);
        graphics.text(font, "ECHO-7", x + 12, y + 11, accent, false);
        graphics.text(font, font.plainSubstrByWidth(entry.title(), w - 92), x + 72, y + 11, 0xFFE9FBFF, false);
        graphics.text(font, font.plainSubstrByWidth(entry.message(), w - 24), x + 12, y + 30, 0xFFC7D7D0, false);
        if (!entry.details().isBlank()) {
            graphics.text(font, font.plainSubstrByWidth(entry.details(), w - 24), x + 12, y + 47, 0xFF8CA7B5, false);
        }
    }

    private static NoticeSnapshot toNoticeSnapshot(Entry entry) {
        String status = entry.danger() ? "DANGER" : "Guide Card".equalsIgnoreCase(entry.title()) ? "GUIDE" : "HINT";
        int accent = entry.danger() ? 0xFFFF665E : 0xFF92F7A6;
        return new NoticeSnapshot("ECHO-7", status, entry.title(), entry.message(), entry.details(), accent);
    }

    private static boolean clientEnabled(boolean danger) {
        try {
            return TutorialConfig.SHOW_TUTORIAL_POPUPS.get()
                    && TutorialConfig.SHOW_TOAST_HINTS.get()
                    && (!danger || TutorialConfig.SHOW_DANGER_WARNINGS.get());
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private static int durationTicks() {
        try {
            return Math.max(40, TutorialConfig.HINT_DURATION_TICKS.get());
        } catch (RuntimeException ignored) {
            return 160;
        }
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static boolean internalToastRendererAllowed() {
        try {
            return internalToastRendererAllowed.getAsBoolean();
        } catch (LinkageError | RuntimeException ignored) {
            return true;
        }
    }

    private record Entry(String title, String message, String details, boolean danger) {}

    public record NoticeSnapshot(
            String sourceLabel,
            String statusLabel,
            String title,
            String detail,
            String footer,
            int accentColor) {
    }
}
