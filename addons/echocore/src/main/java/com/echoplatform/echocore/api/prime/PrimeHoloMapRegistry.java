package com.echoplatform.echocore.api.prime;

import java.util.List;
import net.minecraft.resources.Identifier;

public interface PrimeHoloMapRegistry {
    boolean registerLayer(PrimeMapLayer layer);

    boolean registerMarkerType(PrimeMarkerType markerType);

    List<PrimeMapLayer> layers();

    List<PrimeMarkerType> markerTypes();

    record PrimeMapLayer(
            Identifier id,
            String title,
            String summary,
            int color,
            boolean visibleByDefault,
            int order) {
        public PrimeMapLayer {
            title = title == null ? "" : title;
            summary = summary == null ? "" : summary;
        }
    }

    record PrimeMarkerType(
            Identifier id,
            Identifier layerId,
            String title,
            String icon,
            int order) {
        public PrimeMarkerType {
            title = title == null ? "" : title;
            icon = icon == null ? "" : icon;
        }
    }
}
