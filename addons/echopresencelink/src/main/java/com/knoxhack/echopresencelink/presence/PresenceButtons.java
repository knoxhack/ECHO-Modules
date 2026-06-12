package com.knoxhack.echopresencelink.presence;

import com.knoxhack.echopresencelink.api.EchoPresenceButton;
import com.knoxhack.echopresencelink.api.EchoPresenceContext;
import com.knoxhack.echopresencelink.config.PresenceLinkConfig;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class PresenceButtons {
    private PresenceButtons() {
    }

    public static List<EchoPresenceButton> buttons(EchoPresenceContext context) {
        if (context == null || !context.showButtons()) {
            return List.of();
        }
        List<EchoPresenceButton> buttons = new ArrayList<>();
        EchoPresenceButton configured = new EchoPresenceButton(
                PresenceLinkConfig.primaryButtonLabel(),
                PresenceLinkConfig.primaryButtonUrl());
        if (configured.valid()) {
            buttons.add(configured);
        }
        communityBridgeInvite().stream()
                .filter(button -> buttons.stream().noneMatch(existing -> existing.url().equals(button.url())))
                .findFirst()
                .ifPresent(buttons::add);
        return List.copyOf(buttons.stream().limit(2).toList());
    }

    private static java.util.Optional<EchoPresenceButton> communityBridgeInvite() {
        if (!EchoRuntimeModules.isLoaded("echocommunitybridge")) {
            return java.util.Optional.empty();
        }
        try {
            Class<?> configClass = Class.forName("com.knoxhack.echocommunitybridge.config.CommunityBridgeConfig");
            Field field = configClass.getField("DISCORD_INVITE_URL");
            Object configValue = field.get(null);
            Method get = configValue.getClass().getMethod("get");
            Object raw = get.invoke(configValue);
            EchoPresenceButton button = new EchoPresenceButton("Join Discord", raw instanceof String value ? value : "");
            return button.valid() ? java.util.Optional.of(button) : java.util.Optional.empty();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return java.util.Optional.empty();
        }
    }
}
