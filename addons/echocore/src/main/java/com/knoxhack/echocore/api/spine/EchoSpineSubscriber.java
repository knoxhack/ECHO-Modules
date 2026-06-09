package com.knoxhack.echocore.api.spine;

@FunctionalInterface
public interface EchoSpineSubscriber {
    void onEchoEvent(EchoSpineEvent event);
}
