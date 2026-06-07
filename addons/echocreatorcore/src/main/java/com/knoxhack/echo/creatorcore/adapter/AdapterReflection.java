package com.knoxhack.echo.creatorcore.adapter;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class AdapterReflection {
    private AdapterReflection() {
    }

    static Class<?> load(String className) {
        try {
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException | LinkageError exception) {
            throw new IllegalStateException("Class unavailable: " + className, exception);
        }
    }

    static Object staticField(String className, String fieldName) {
        try {
            Field field = load(className).getField(fieldName);
            return field.get(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException("Field unavailable: " + className + "#" + fieldName, exception);
        }
    }

    static Object invoke(Object target, String methodName, Object... args) {
        if (target == null) {
            throw new IllegalStateException("Cannot invoke " + methodName + " on null target.");
        }
        Method method = findMethod(target instanceof Class<?> clazz ? clazz : target.getClass(), methodName, args);
        try {
            return method.invoke(target instanceof Class<?> ? null : target, args);
        } catch (IllegalAccessException | InvocationTargetException | LinkageError exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation && invocation.getCause() != null
                    ? invocation.getCause()
                    : exception;
            throw new IllegalStateException("Reflection call failed: " + methodName, cause);
        }
    }

    static List<Object> iterable(Object value) {
        if (value instanceof Iterable<?> iterable) {
            List<Object> values = new ArrayList<>();
            iterable.forEach(values::add);
            return values;
        }
        return List.of();
    }

    static <T> Optional<T> value(Object target, String methodName, Class<T> type) {
        return value(invoke(target, methodName), type);
    }

    static <T> Optional<T> value(Object value, Class<T> type) {
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }

    static Optional<String> optionalString(Object value) {
        if (value instanceof Optional<?> optional) {
            return optional.map(Object::toString);
        }
        return Optional.empty();
    }

    static Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, String> strings = new java.util.LinkedHashMap<>();
        map.forEach((key, entry) -> {
            if (key != null && entry != null) {
                strings.put(key.toString(), entry.toString());
            }
        });
        return Map.copyOf(strings);
    }

    private static Method findMethod(Class<?> type, String methodName, Object[] args) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean matches = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (args[i] != null && !wrap(parameterTypes[i]).isInstance(args[i])) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return method;
            }
        }
        throw new IllegalStateException("Method unavailable: " + type.getName() + "#" + methodName);
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return Void.class;
    }
}
