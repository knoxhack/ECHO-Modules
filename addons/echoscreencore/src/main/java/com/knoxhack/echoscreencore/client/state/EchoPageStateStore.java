package com.knoxhack.echoscreencore.client.state;

import com.knoxhack.echoscreencore.api.EchoDataContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

public final class EchoPageStateStore {
    private static final Map<Identifier, EchoPageState> PAGE_STATE = new ConcurrentHashMap<>();
    private static final EchoPageState SESSION_STATE = new EchoPageState();

    private EchoPageStateStore() {
    }

    public static EchoPageState page(Identifier pageId) {
        return PAGE_STATE.computeIfAbsent(pageId, id -> new EchoPageState());
    }

    public static EchoPageState session() {
        return SESSION_STATE;
    }

    public static EchoDataContext attach(EchoDataContext context, Identifier pageId) {
        EchoPageState page = page(pageId);
        return (context == null ? EchoDataContext.empty() : context)
            .child("state", page.values())
            .put("screencore.pageId", pageId == null ? "" : pageId.toString());
    }

    public static void put(EchoDataContext context, String key, Object value) {
        if (context == null || key == null || key.isBlank()) {
            return;
        }
        context.put("state." + key.strip(), value);
        Object page = context.resolve("screencore.pageId").orElse(null);
        Identifier pageId = page == null ? null : Identifier.tryParse(String.valueOf(page));
        if (pageId != null) {
            page(pageId).put(key, value);
        } else {
            SESSION_STATE.put(key, value);
        }
    }

    public static void clear(Identifier pageId) {
        if (pageId == null) {
            PAGE_STATE.clear();
            SESSION_STATE.clear();
            return;
        }
        PAGE_STATE.remove(pageId);
    }
}
