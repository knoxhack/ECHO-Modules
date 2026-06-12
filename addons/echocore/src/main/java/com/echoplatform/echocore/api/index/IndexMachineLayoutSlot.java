package com.echoplatform.echocore.api.index;

public record IndexMachineLayoutSlot(
        int recipeSlotIndex,
        IndexSlotRole role,
        String label,
        int x,
        int y,
        int size,
        boolean optional) {
    public IndexMachineLayoutSlot {
        role = role == null ? IndexSlotRole.INFO : role;
        label = label == null ? "" : label;
        size = Math.max(1, size);
    }

    public IndexMachineLayoutSlot(IndexSlotRole role, int x, int y, int w, int h, String label) {
        this(-1, role, label, x, y, Math.max(w, h), false);
    }

    public int w() {
        return size;
    }

    public int h() {
        return size;
    }
}
