package com.knoxhack.echo.equipmentcore.api;

import com.knoxhack.echo.equipmentcore.EchoEquipmentCore;
import net.minecraft.resources.Identifier;

/**
 * Canonical equipment slots exposed by ECHO EquipmentCore.
 */
public final class EquipmentSlot {
    public static final EquipmentSlot SUIT_FRAME = builtin("suit_frame");
    public static final EquipmentSlot REBREATHER = builtin("rebreather");
    public static final EquipmentSlot LIGHT_SENSOR = builtin("light_sensor");
    public static final EquipmentSlot TOOL_MOUNT = builtin("tool_mount");

    private final Identifier id;

    private EquipmentSlot(Identifier id) {
        this.id = id;
    }

    public static EquipmentSlot of(Identifier id) {
        return new EquipmentSlot(id);
    }

    private static EquipmentSlot builtin(String path) {
        return new EquipmentSlot(Identifier.fromNamespaceAndPath(EchoEquipmentCore.MODID, path));
    }

    public Identifier id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof EquipmentSlot other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
