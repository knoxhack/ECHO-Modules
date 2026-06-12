package com.echoplatform.echocore.api.spine;

public interface EchoSpineBus {
    AutoCloseable subscribe(String channel, EchoSpineSubscriber subscriber);

    void publish(EchoSpineEvent event);
}
