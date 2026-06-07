package com.knoxhack.echo.npcore.visual;

import com.knoxhack.echo.npcore.EchoNpcCore;
import java.util.List;
import net.minecraft.resources.Identifier;

public record EchoNpcVisualProfile(
        Identifier id,
        Identifier model,
        Identifier texture,
        Identifier emissiveTexture,
        Identifier portrait,
        Identifier factionBadge,
        Identifier screenFrame,
        String nameplateStyle,
        Identifier theme,
        List<Layer> layers) {
    public static final Identifier FALLBACK_TEXTURE =
            Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "textures/entity/npc/missing_npc.png");

    public EchoNpcVisualProfile {
        id = id == null ? Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "missing") : id;
        model = model == null ? Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "humanoid_basic") : model;
        texture = texture == null ? FALLBACK_TEXTURE : texture;
        nameplateStyle = nameplateStyle == null ? "survivor" : nameplateStyle.trim();
        theme = theme == null ? Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, "ashfall_survivor") : theme;
        layers = List.copyOf(layers == null ? List.of() : layers);
    }

    public record Layer(String type, Identifier texture, boolean emissive, String tint, String visibleWhen) {
        public Layer {
            type = type == null ? "overlay" : type.trim();
            tint = tint == null ? "" : tint.trim();
            visibleWhen = visibleWhen == null ? "" : visibleWhen.trim();
        }
    }
}
