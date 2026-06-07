package com.knoxhack.echoscreencore.api.layout;

public record EchoRect(int x, int y, int width, int height) {
    public static final EchoRect ZERO = new EchoRect(0, 0, 0, 0);

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean contains(double px, double py) {
        return px >= x && px < right() && py >= y && py < bottom();
    }

    public EchoRect inset(int left, int top, int right, int bottom) {
        int nx = x + left;
        int ny = y + top;
        return new EchoRect(nx, ny, Math.max(0, width - left - right), Math.max(0, height - top - bottom));
    }
}
