package com.echoplatform.echocore.api.network;

import net.minecraft.resources.Identifier;

public record EchoDiscoveryToast(
        Identifier featureId,
        String category,
        String title,
        String subtitle,
        String iconArt,
        String heroArt,
        int accentColor) {
    public EchoDiscoveryToast {
        category = category == null ? "" : category;
        title = title == null ? "" : title;
        subtitle = subtitle == null ? "" : subtitle;
        iconArt = iconArt == null ? "" : iconArt;
        heroArt = heroArt == null ? "" : heroArt;
    }
}
