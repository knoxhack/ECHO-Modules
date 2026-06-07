package com.knoxhack.echoterminal.api;

public interface TerminalTab {
    TerminalTabDescriptor descriptor();

    default TerminalTabChrome chrome() {
        return TerminalTabChrome.fromDescriptor(descriptor());
    }
}
