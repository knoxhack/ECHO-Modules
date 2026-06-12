package com.echoplatform.echocore.api;

public record EchoNpcRole(String id, String title, String summary) {
    public EchoNpcRole {
        id = id == null ? "" : id;
        title = title == null ? "" : title;
        summary = summary == null ? "" : summary;
    }

    public String displayName() {
        return title;
    }
}
