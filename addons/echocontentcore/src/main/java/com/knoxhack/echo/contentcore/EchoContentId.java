package com.knoxhack.echo.contentcore;

import java.util.Locale;

public record EchoContentId(String value) {
    public EchoContentId {
        value = ContentContractGuards.requireText(value, "content id").toLowerCase(Locale.ROOT);
    }

    public static EchoContentId of(String value) {
        return new EchoContentId(value);
    }

    public static EchoContentId of(String namespace, String path) {
        return new EchoContentId(
                ContentContractGuards.requireText(namespace, "content id namespace")
                        + ":"
                        + ContentContractGuards.requireText(path, "content id path")
        );
    }

    public String namespace() {
        int split = value.indexOf(':');
        return split < 0 ? "" : value.substring(0, split);
    }

    public String path() {
        int split = value.indexOf(':');
        return split < 0 ? value : value.substring(split + 1);
    }

    public boolean namespaced() {
        return value.indexOf(':') > 0 && value.indexOf(':') < value.length() - 1;
    }

    @Override
    public String toString() {
        return value;
    }
}
