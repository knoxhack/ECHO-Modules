package com.echoplatform.echocore.api;

import java.util.List;

public record EchoDialogueTree(String greeting, List<String> topics, String farewell) {
    public static final EchoDialogueTree EMPTY = new EchoDialogueTree("", List.of(), "");

    public EchoDialogueTree {
        greeting = greeting == null ? "" : greeting;
        topics = topics == null ? List.of() : List.copyOf(topics);
        farewell = farewell == null ? "" : farewell;
    }
}
