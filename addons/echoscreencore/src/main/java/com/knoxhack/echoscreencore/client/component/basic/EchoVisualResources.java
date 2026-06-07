package com.knoxhack.echoscreencore.client.component.basic;

import com.knoxhack.echoscreencore.EchoScreenCoreMod;
import com.knoxhack.echoscreencore.api.layout.EchoRect;
import com.knoxhack.echoscreencore.client.render.EchoRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

final class EchoVisualResources {
    private EchoVisualResources() {
    }

    static Identifier texture(String raw, String defaultFolder, EchoRenderContext context, String diagnosticCode) {
        String value = raw == null ? "" : raw.strip();
        if (value.isBlank()) {
            return null;
        }
        try {
            Identifier id;
            if (value.contains(":")) {
                id = Identifier.parse(value);
            } else {
                String path = value;
                if (!path.endsWith(".png")) {
                    path = defaultFolder + "/" + path + ".png";
                }
                id = EchoScreenCoreMod.id(path);
            }
            if (Minecraft.getInstance().getResourceManager().getResource(id).isEmpty()) {
                if (context != null && context.diagnostics() != null) {
                    context.diagnostics().warnOnce(diagnosticCode, id.toString());
                }
                return null;
            }
            return id;
        } catch (RuntimeException exception) {
            if (context != null && context.diagnostics() != null) {
                context.diagnostics().warnOnce("bad_resource_path", value);
            }
            return null;
        }
    }

    static EchoRect fit(EchoRect bounds, int sourceWidth, int sourceHeight, String mode) {
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            return EchoRect.ZERO;
        }
        int sw = Math.max(1, sourceWidth);
        int sh = Math.max(1, sourceHeight);
        String fit = mode == null || mode.isBlank() ? "contain" : mode.strip().toLowerCase(java.util.Locale.ROOT);
        if ("stretch".equals(fit)) {
            return bounds;
        }
        if ("center".equals(fit)) {
            int w = Math.min(bounds.width(), sw);
            int h = Math.min(bounds.height(), sh);
            return new EchoRect(bounds.x() + (bounds.width() - w) / 2, bounds.y() + (bounds.height() - h) / 2, w, h);
        }
        float sx = bounds.width() / (float) sw;
        float sy = bounds.height() / (float) sh;
        float scale = "cover".equals(fit) ? Math.max(sx, sy) : Math.min(sx, sy);
        int w = Math.max(1, Math.round(sw * scale));
        int h = Math.max(1, Math.round(sh * scale));
        return new EchoRect(bounds.x() + (bounds.width() - w) / 2, bounds.y() + (bounds.height() - h) / 2, w, h);
    }

    static int intAttr(String raw, int fallback) {
        try {
            return raw == null || raw.isBlank() ? fallback : Math.max(0, Math.round(Float.parseFloat(raw.replace("px", "").strip())));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
