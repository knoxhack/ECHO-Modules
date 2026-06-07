package com.knoxhack.echoterminal.api;

@FunctionalInterface
public interface TerminalActionValidator {
    TerminalActionValidator ALLOW = context -> true;

    boolean validate(TerminalActionContext context);
}
