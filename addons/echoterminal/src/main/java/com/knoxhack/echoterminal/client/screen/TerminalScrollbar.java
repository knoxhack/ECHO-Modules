package com.knoxhack.echoterminal.client.screen;

public final class TerminalScrollbar {
    private TerminalScrollbar() {
    }

    public static Metrics vertical(int trackX, int trackY, int trackW, int trackH, int scroll, int maxScroll) {
        if (maxScroll <= 0 || trackH <= 16) {
            return Metrics.disabled();
        }
        int safeTrackW = Math.max(1, trackW);
        int safeScroll = clamp(scroll, 0, maxScroll);
        int thumbH = Math.min(trackH, Math.max(18, trackH * trackH / Math.max(trackH, trackH + maxScroll)));
        int thumbRange = Math.max(1, trackH - thumbH);
        int thumbY = trackY + Math.round(thumbRange * (safeScroll / (float) maxScroll));
        return new Metrics(trackX, trackY, safeTrackW, trackH, trackX, thumbY, safeTrackW, thumbH, maxScroll, true);
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public record Metrics(
            int trackX,
            int trackY,
            int trackW,
            int trackH,
            int thumbX,
            int thumbY,
            int thumbW,
            int thumbH,
            int maxScroll,
            boolean enabled) {
        private static Metrics disabled() {
            return new Metrics(0, 0, 0, 0, 0, 0, 0, 0, 0, false);
        }

        public boolean insideTrack(double mouseX, double mouseY) {
            return enabled
                    && mouseX >= trackX
                    && mouseY >= trackY
                    && mouseX < trackX + trackW
                    && mouseY < trackY + trackH;
        }

        public boolean insideThumb(double mouseX, double mouseY) {
            return enabled
                    && mouseX >= thumbX
                    && mouseY >= thumbY
                    && mouseX < thumbX + thumbW
                    && mouseY < thumbY + thumbH;
        }

        public int dragOffset(double mouseY) {
            return insideThumb(trackX, mouseY)
                    ? (int) Math.round(mouseY) - thumbY
                    : thumbH / 2;
        }

        public int scrollForMouse(double mouseY, int thumbOffset) {
            if (!enabled) {
                return 0;
            }
            int range = Math.max(1, trackH - thumbH);
            int thumbStart = clamp((int) Math.round(mouseY) - thumbOffset, trackY, trackY + range);
            return (int) Math.round((thumbStart - trackY) * (double) maxScroll / range);
        }

        public int scrollForTrackClick(double mouseY) {
            return scrollForMouse(mouseY, thumbH / 2);
        }
    }
}
