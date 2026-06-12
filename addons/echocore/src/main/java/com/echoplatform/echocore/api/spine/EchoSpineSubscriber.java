package com.echoplatform.echocore.api.spine;

@FunctionalInterface
public interface EchoSpineSubscriber {
    void onEchoEvent(EchoSpineEvent event);
}
