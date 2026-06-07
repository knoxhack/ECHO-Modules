package com.knoxhack.echoscreencore.api;

import java.util.ArrayList;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class EchoDataContext {
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Map<String, EchoDataProvider> providers = new LinkedHashMap<>();
    private String missingPlaceholder = "-";

    public static EchoDataContext empty() {
        return new EchoDataContext();
    }

    public EchoDataContext put(String path, Object value) {
        if (path != null && !path.isBlank()) {
            values.put(path.trim(), value);
        }
        return this;
    }

    public EchoDataContext child(String path, Object value) {
        EchoDataContext child = new EchoDataContext();
        child.values.putAll(values);
        child.providers.putAll(providers);
        child.missingPlaceholder = missingPlaceholder;
        child.put(path, value);
        return child;
    }

    public EchoDataContext provider(String key, EchoDataProvider provider) {
        if (key != null && !key.isBlank() && provider != null) {
            providers.put(key.trim(), provider);
        }
        return this;
    }

    public EchoDataContext provider(Identifier id, EchoDataProvider provider) {
        if (id != null && provider != null) {
            providers.put(id.toString(), provider);
            providers.put(id.getPath(), provider);
        }
        return this;
    }

    public EchoDataContext missingPlaceholder(String placeholder) {
        missingPlaceholder = placeholder == null ? "" : placeholder;
        return this;
    }

    public String missingPlaceholder() {
        return missingPlaceholder;
    }

    public Optional<Object> resolve(String path) {
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        String clean = path.trim();
        if (values.containsKey(clean)) {
            return Optional.ofNullable(values.get(clean));
        }
        List<String> parts = split(clean);
        if (parts.isEmpty()) {
            return Optional.empty();
        }
        String root = parts.get(0);
        List<String> tail = parts.subList(1, parts.size());
        EchoDataProvider provider = providers.get(root);
        if (provider == null) {
            provider = EchoScreenRegistry.dataProvider(root).orElse(null);
        }
        if (provider != null) {
            return Optional.ofNullable(provider.resolve(this, List.copyOf(tail)));
        }
        Object current = values.get(root);
        if (current == null) {
            return Optional.empty();
        }
        for (String part : tail) {
            current = valuePart(current, part);
            if (current == null) {
                return Optional.empty();
            }
        }
        return Optional.of(current);
    }

    public String resolveToString(String path) {
        return resolve(path).map(String::valueOf).orElse(missingPlaceholder);
    }

    public static List<String> splitPath(String path) {
        return split(path);
    }

    private static List<String> split(String path) {
        String[] raw = path.split("\\.");
        ArrayList<String> parts = new ArrayList<>();
        for (String part : raw) {
            String clean = part.trim();
            if (!clean.isEmpty()) {
                addPart(parts, clean);
            }
        }
        return parts;
    }

    private static void addPart(List<String> parts, String clean) {
        int cursor = 0;
        int bracket = clean.indexOf('[');
        if (bracket < 0) {
            parts.add(clean);
            return;
        }
        if (bracket > 0) {
            parts.add(clean.substring(0, bracket));
        }
        cursor = bracket;
        while (cursor >= 0 && cursor < clean.length()) {
            int open = clean.indexOf('[', cursor);
            int close = open < 0 ? -1 : clean.indexOf(']', open + 1);
            if (open < 0 || close < 0) {
                break;
            }
            String index = clean.substring(open + 1, close).trim();
            if (!index.isBlank()) {
                parts.add(index);
            }
            cursor = close + 1;
        }
    }

    private static Object valuePart(Object current, String part) {
        if (current instanceof Map<?, ?> map) {
            return map.get(part);
        }
        if (current instanceof List<?> list) {
            return listValue(list, part);
        }
        if (current != null && current.getClass().isArray()) {
            return arrayValue(current, part);
        }
        return objectValue(current, part);
    }

    private static Object listValue(List<?> list, String part) {
        try {
            int index = Integer.parseInt(part);
            return index >= 0 && index < list.size() ? list.get(index) : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Object arrayValue(Object array, String part) {
        try {
            int index = Integer.parseInt(part);
            int length = Array.getLength(array);
            return index >= 0 && index < length ? Array.get(array, index) : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Object objectValue(Object target, String part) {
        if (target == null || part == null || part.isBlank()) {
            return null;
        }
        String clean = part.trim();
        for (String methodName : List.of(clean, "get" + capitalize(clean), "is" + capitalize(clean))) {
            try {
                Method method = target.getClass().getMethod(methodName);
                if (method.getParameterCount() == 0) {
                    return method.invoke(target);
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        try {
            Field field = target.getClass().getField(clean);
            return field.get(target);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static String capitalize(String value) {
        return value == null || value.isBlank()
            ? ""
            : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
