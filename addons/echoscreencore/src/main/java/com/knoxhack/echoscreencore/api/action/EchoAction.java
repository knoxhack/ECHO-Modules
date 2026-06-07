package com.knoxhack.echoscreencore.api.action;

@FunctionalInterface
public interface EchoAction {
    boolean run(EchoActionContext context);
}
