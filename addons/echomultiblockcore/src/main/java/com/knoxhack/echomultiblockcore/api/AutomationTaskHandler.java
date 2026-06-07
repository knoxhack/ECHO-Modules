package com.knoxhack.echomultiblockcore.api;

/**
 * Legacy extension point for code-driven task behavior. New addon side effects should use
 * {@link AutomationEffectHandler} and data recipe {@code effects} entries.
 */
@Deprecated(forRemoval = false)
public interface AutomationTaskHandler {
    default boolean canHandle(MultiblockAutomationRecipe recipe) {
        return recipe != null;
    }

    default boolean canStart(AutomationTaskContext context, MultiblockAutomationRecipe recipe, AutomationExecutionPlan plan) {
        return true;
    }

    default void onStart(AutomationTaskContext context, MultiblockAutomationRecipe recipe, AutomationExecutionPlan plan) {
    }

    default void onTick(AutomationTaskContext context, MultiblockAutomationRecipe recipe, AutomationExecutionPlan plan) {
    }

    default void onComplete(AutomationTaskContext context, MultiblockAutomationRecipe recipe, AutomationExecutionPlan plan) {
    }

    default void onFail(AutomationTaskContext context, MultiblockAutomationRecipe recipe, AutomationExecutionPlan plan, String reason) {
    }
}
