package com.knoxhack.echosoundcore.api.context;

import java.util.ArrayDeque;
import java.util.Deque;

public final class SoundCoreContextStack {
    private static final Deque<SoundCoreContext> STACK = new ArrayDeque<>();
    private static SoundCoreContext base = new SoundCoreContext();

    private SoundCoreContextStack() {}

    public static void push(SoundCoreContext context) {
        STACK.push(context.copy());
    }

    public static void pop() {
        if (!STACK.isEmpty()) {
            STACK.pop();
        }
    }

    public static void clear() {
        STACK.clear();
    }

    public static void resetBase() {
        base = new SoundCoreContext();
        clear();
    }

    public static SoundCoreContext current() {
        return STACK.isEmpty() ? base : STACK.peek();
    }

    public static void setBase(SoundCoreContext context) {
        base = context.copy();
    }

    public static boolean isEmpty() {
        return STACK.isEmpty();
    }
}
