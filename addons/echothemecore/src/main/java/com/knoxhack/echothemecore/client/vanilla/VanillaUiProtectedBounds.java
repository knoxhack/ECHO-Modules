package com.knoxhack.echothemecore.client.vanilla;

public record VanillaUiProtectedBounds(int x, int y, int width, int height) {
    public boolean contains(int px, int py) {
        return px >= x && py >= y && px < x + width && py < y + height;
    }
}
