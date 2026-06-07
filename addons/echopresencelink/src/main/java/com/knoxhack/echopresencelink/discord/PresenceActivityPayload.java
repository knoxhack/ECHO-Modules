package com.knoxhack.echopresencelink.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.knoxhack.echopresencelink.api.EchoPresenceButton;
import com.knoxhack.echopresencelink.api.EchoPresenceSnapshot;
import java.util.List;
import java.util.UUID;

public final class PresenceActivityPayload {
    private PresenceActivityPayload() {
    }

    public static JsonObject activity(EchoPresenceSnapshot snapshot, List<EchoPresenceButton> buttons) {
        JsonObject activity = baseActivity(snapshot);
        JsonObject assets = new JsonObject();
        assets.addProperty("large_image", snapshot.largeImageKey());
        if (!snapshot.largeImageText().isBlank()) {
            assets.addProperty("large_text", snapshot.largeImageText());
        }
        if (!snapshot.smallImageKey().isBlank()) {
            assets.addProperty("small_image", snapshot.smallImageKey());
        }
        if (!snapshot.smallImageText().isBlank()) {
            assets.addProperty("small_text", snapshot.smallImageText());
        }
        activity.add("assets", assets);

        JsonArray buttonArray = new JsonArray();
        for (EchoPresenceButton button : buttons == null ? snapshot.buttons() : buttons) {
            if (button == null || !button.valid() || buttonArray.size() >= 2) {
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("label", button.label());
            entry.addProperty("url", button.url());
            buttonArray.add(entry);
        }
        if (!buttonArray.isEmpty()) {
            activity.add("buttons", buttonArray);
        }
        return activity;
    }

    public static JsonObject minimalActivity(EchoPresenceSnapshot snapshot) {
        return baseActivity(snapshot);
    }

    private static JsonObject baseActivity(EchoPresenceSnapshot snapshot) {
        JsonObject activity = new JsonObject();
        activity.addProperty("type", 0);
        activity.addProperty("details", snapshot.details());
        if (!snapshot.state().isBlank()) {
            activity.addProperty("state", snapshot.state());
        }
        if (snapshot.startTimestamp() > 0L) {
            JsonObject timestamps = new JsonObject();
            timestamps.addProperty("start", snapshot.startTimestamp());
            activity.add("timestamps", timestamps);
        }
        return activity;
    }

    public static CommandPayload setActivity(JsonObject activity) {
        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", activity);
        String nonce = UUID.randomUUID().toString();
        JsonObject payload = new JsonObject();
        payload.addProperty("cmd", "SET_ACTIVITY");
        payload.add("args", args);
        payload.addProperty("nonce", nonce);
        return new CommandPayload(payload, nonce);
    }

    public static CommandPayload clearActivity() {
        JsonObject args = new JsonObject();
        args.addProperty("pid", ProcessHandle.current().pid());
        args.add("activity", JsonNull.INSTANCE);
        String nonce = UUID.randomUUID().toString();
        JsonObject payload = new JsonObject();
        payload.addProperty("cmd", "SET_ACTIVITY");
        payload.add("args", args);
        payload.addProperty("nonce", nonce);
        return new CommandPayload(payload, nonce);
    }

    public record CommandPayload(JsonObject json, String nonce) {
    }
}
