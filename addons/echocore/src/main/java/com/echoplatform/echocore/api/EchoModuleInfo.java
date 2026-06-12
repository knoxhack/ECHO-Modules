package com.echoplatform.echocore.api;

public record EchoModuleInfo(String moduleId, String version, String lane, boolean loaded) {
    public EchoModuleInfo {
        moduleId = moduleId == null ? "" : moduleId;
        version = version == null ? "" : version;
        lane = lane == null ? "" : lane;
    }

    public String modId() {
        return moduleId;
    }

    public String displayName() {
        if (moduleId.isBlank()) {
            return "ECHO Module";
        }
        String[] parts = moduleId.replace('_', ' ').replace('-', ' ').split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? moduleId : builder.toString();
    }

    public String ownership() {
        return lane;
    }

    public String statusLine() {
        String state = loaded ? "Loaded" : "Unavailable";
        return version.isBlank() ? state : state + " / " + version;
    }

    public String projectPath() {
        return ":" + moduleId;
    }
}
