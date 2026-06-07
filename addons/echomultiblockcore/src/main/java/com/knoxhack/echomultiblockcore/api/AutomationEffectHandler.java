package com.knoxhack.echomultiblockcore.api;

import net.minecraft.resources.Identifier;

public interface AutomationEffectHandler {
    Identifier providerId();

    default boolean handles(Identifier effectId) {
        return effectId != null;
    }

    default AutomationEffectResult beforeStart(AutomationEffectInvocation invocation) {
        return AutomationEffectResult.allow();
    }

    default AutomationEffectResult onStart(AutomationEffectInvocation invocation) {
        return AutomationEffectResult.allow();
    }

    default AutomationEffectResult onTick(AutomationEffectInvocation invocation) {
        return AutomationEffectResult.allow();
    }

    default AutomationEffectResult onComplete(AutomationEffectInvocation invocation) {
        return AutomationEffectResult.allow();
    }

    default AutomationEffectResult onFail(AutomationEffectInvocation invocation) {
        return AutomationEffectResult.allow();
    }
}
