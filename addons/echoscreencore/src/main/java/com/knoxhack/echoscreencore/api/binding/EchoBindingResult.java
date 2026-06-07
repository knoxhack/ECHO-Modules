package com.knoxhack.echoscreencore.api.binding;

public record EchoBindingResult(String text, boolean missing, String missingPath) {
    public static EchoBindingResult resolved(String text) {
        return new EchoBindingResult(text == null ? "" : text, false, "");
    }

    public static EchoBindingResult missing(String placeholder, String path) {
        return new EchoBindingResult(placeholder == null ? "" : placeholder, true, path == null ? "" : path);
    }
}
