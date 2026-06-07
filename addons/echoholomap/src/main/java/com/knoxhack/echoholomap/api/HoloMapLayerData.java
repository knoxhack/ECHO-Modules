package com.knoxhack.echoholomap.api;

import com.knoxhack.echocore.api.EchoMapLayer;
import com.knoxhack.echocore.api.IMapLayer;
import com.knoxhack.echoholomap.HoloMapIds;
import net.minecraft.resources.Identifier;

public record HoloMapLayerData(
        Identifier id,
        String title,
        int sortOrder,
        int color,
        boolean visibleByDefault) {
    public HoloMapLayerData {
        id = id == null ? HoloMapIds.layer("unknown") : id;
        title = title == null || title.isBlank() ? id.getPath() : title.strip();
        color = color == 0 ? 0xFF66E8FF : color;
    }

    public static HoloMapLayerData from(IMapLayer layer) {
        if (layer == null) {
            return new HoloMapLayerData(HoloMapIds.layer("unknown"), "Unknown", 500, 0xFF8CA7B5, true);
        }
        return new HoloMapLayerData(layer.id(), layer.title(), layer.sortOrder(), layer.color(), layer.visibleByDefault());
    }

    public IMapLayer toCore() {
        return new EchoMapLayer(id, title, sortOrder, color, visibleByDefault);
    }
}
