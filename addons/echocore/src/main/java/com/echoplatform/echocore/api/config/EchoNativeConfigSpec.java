package com.echoplatform.echocore.api.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class EchoNativeConfigSpec {
    private final List<ConfigValue<?>> values;

    private EchoNativeConfigSpec(List<ConfigValue<?>> values) {
        this.values = List.copyOf(values);
    }

    public List<ConfigValue<?>> values() {
        return values;
    }

    public List<EchoConfigEntry> entries() {
        return values.stream()
                .map(value -> new EchoConfigEntry(
                        value.key(),
                        value.kind(),
                        String.valueOf(value.defaultValue()),
                        value.description()))
                .toList();
    }

    public static final class Builder {
        private final List<ConfigValue<?>> values = new ArrayList<>();
        private final List<String> path = new ArrayList<>();
        private String pendingComment = "";

        public Builder comment(String comment) {
            pendingComment = comment == null ? "" : comment.strip();
            return this;
        }

        public Builder push(String section) {
            String normalized = section == null ? "" : section.strip();
            if (!normalized.isBlank()) {
                path.add(normalized);
            }
            return this;
        }

        public Builder pop() {
            if (!path.isEmpty()) {
                path.remove(path.size() - 1);
            }
            return this;
        }

        public BooleanValue define(String key, boolean defaultValue) {
            BooleanValue value = new BooleanValue(scopedKey(key), defaultValue, takeComment());
            values.add(value);
            return value;
        }

        public IntValue define(String key, int defaultValue) {
            IntValue value = new IntValue(scopedKey(key), defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE,
                    takeComment());
            values.add(value);
            return value;
        }

        public DoubleValue define(String key, double defaultValue) {
            DoubleValue value = new DoubleValue(scopedKey(key), defaultValue, -Double.MAX_VALUE, Double.MAX_VALUE,
                    takeComment());
            values.add(value);
            return value;
        }

        public StringValue define(String key, String defaultValue) {
            StringValue value = new StringValue(scopedKey(key), defaultValue, takeComment(), ignored -> true);
            values.add(value);
            return value;
        }

        public StringValue define(String key, String defaultValue, Predicate<Object> validator) {
            StringValue value = new StringValue(scopedKey(key), defaultValue, takeComment(), validator);
            values.add(value);
            return value;
        }

        public ListValue defineList(String key, List<String> defaultValue, Predicate<Object> elementValidator) {
            ListValue value = new ListValue(scopedKey(key), defaultValue, takeComment(), elementValidator);
            values.add(value);
            return value;
        }

        public <T> ConfigValue<T> define(String key, T defaultValue) {
            ConfigValue<T> value = new ConfigValue<>(scopedKey(key), defaultValue, kind(defaultValue), takeComment());
            values.add(value);
            return value;
        }

        public IntValue defineInRange(String key, int defaultValue, int min, int max) {
            IntValue value = new IntValue(scopedKey(key), defaultValue, min, max, takeComment());
            values.add(value);
            return value;
        }

        public DoubleValue defineInRange(String key, double defaultValue, double min, double max) {
            DoubleValue value = new DoubleValue(scopedKey(key), defaultValue, min, max, takeComment());
            values.add(value);
            return value;
        }

        public <E extends Enum<E>> EnumValue<E> defineEnum(String key, E defaultValue) {
            EnumValue<E> value = new EnumValue<>(scopedKey(key), defaultValue, takeComment());
            values.add(value);
            return value;
        }

        public EchoNativeConfigSpec build() {
            return new EchoNativeConfigSpec(values);
        }

        private String takeComment() {
            String comment = pendingComment;
            pendingComment = "";
            return comment;
        }

        private String scopedKey(String key) {
            String normalized = key == null ? "" : key.strip();
            if (path.isEmpty() || normalized.isBlank()) {
                return normalized;
            }
            return String.join(".", path) + "." + normalized;
        }

        private static EchoConfigValueKind kind(Object value) {
            if (value instanceof Boolean) {
                return EchoConfigValueKind.BOOLEAN;
            }
            if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
                return EchoConfigValueKind.INTEGER;
            }
            if (value instanceof Float || value instanceof Double) {
                return EchoConfigValueKind.DECIMAL;
            }
            if (value instanceof List<?>) {
                return EchoConfigValueKind.STRING_LIST;
            }
            if (value instanceof String || value instanceof Enum<?>) {
                return EchoConfigValueKind.STRING;
            }
            return EchoConfigValueKind.OBJECT;
        }
    }

    public static class ConfigValue<T> {
        private final String key;
        private final T defaultValue;
        private final EchoConfigValueKind kind;
        private final String description;
        private final Predicate<Object> validator;
        private T value;

        protected ConfigValue(String key, T defaultValue, EchoConfigValueKind kind, String description) {
            this(key, defaultValue, kind, description, ignored -> true);
        }

        protected ConfigValue(
                String key,
                T defaultValue,
                EchoConfigValueKind kind,
                String description,
                Predicate<Object> validator
        ) {
            this.key = requireKey(key);
            this.defaultValue = defaultValue;
            this.kind = Objects.requireNonNull(kind, "kind");
            this.description = description == null ? "" : description;
            this.validator = validator == null ? ignored -> true : validator;
            requireValid(defaultValue);
            this.value = defaultValue;
        }

        public String key() {
            return key;
        }

        public T defaultValue() {
            return defaultValue;
        }

        public T getDefault() {
            return defaultValue;
        }

        public EchoConfigValueKind kind() {
            return kind;
        }

        public String description() {
            return description;
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            requireValid(value);
            this.value = value;
        }

        public void save() {
        }

        protected final void requireValid(Object value) {
            if (!validator.test(value)) {
                throw new IllegalArgumentException("Invalid value for config key " + key + ": " + value);
            }
        }
    }

    public static final class BooleanValue extends ConfigValue<Boolean> {
        private BooleanValue(String key, boolean defaultValue, String description) {
            super(key, defaultValue, EchoConfigValueKind.BOOLEAN, description);
        }
    }

    public static final class IntValue extends ConfigValue<Integer> {
        private final int min;
        private final int max;

        private IntValue(String key, int defaultValue, int min, int max, String description) {
            super(key, clamp(defaultValue, min, max), EchoConfigValueKind.INTEGER, description);
            this.min = Math.min(min, max);
            this.max = Math.max(min, max);
        }

        public int min() {
            return min;
        }

        public int max() {
            return max;
        }

        @Override
        public void set(Integer value) {
            super.set(clamp(value == null ? defaultValue() : value, min, max));
        }
    }

    public static final class StringValue extends ConfigValue<String> {
        private StringValue(
                String key,
                String defaultValue,
                String description,
                Predicate<Object> validator
        ) {
            super(key, defaultValue == null ? "" : defaultValue, EchoConfigValueKind.STRING, description, validator);
        }

        @Override
        public void set(String value) {
            super.set(value == null ? "" : value);
        }
    }

    public static final class ListValue extends ConfigValue<List<String>> {
        private final Predicate<Object> elementValidator;

        private ListValue(
                String key,
                List<String> defaultValue,
                String description,
                Predicate<Object> elementValidator
        ) {
            super(
                    key,
                    sanitizeList(defaultValue, elementValidator),
                    EchoConfigValueKind.STRING_LIST,
                    description,
                    value -> validList(value, elementValidator)
            );
            this.elementValidator = elementValidator == null ? ignored -> true : elementValidator;
        }

        @Override
        public void set(List<String> value) {
            super.set(sanitizeList(value, elementValidator));
        }
    }

    public static final class DoubleValue extends ConfigValue<Double> {
        private final double min;
        private final double max;

        private DoubleValue(String key, double defaultValue, double min, double max, String description) {
            super(key, clamp(defaultValue, min, max), EchoConfigValueKind.DECIMAL, description);
            this.min = Math.min(min, max);
            this.max = Math.max(min, max);
        }

        public double min() {
            return min;
        }

        public double max() {
            return max;
        }

        @Override
        public void set(Double value) {
            super.set(clamp(value == null ? defaultValue() : value, min, max));
        }
    }

    public static final class EnumValue<E extends Enum<E>> extends ConfigValue<E> {
        private EnumValue(String key, E defaultValue, String description) {
            super(key, Objects.requireNonNull(defaultValue, "defaultValue"), EchoConfigValueKind.STRING, description);
        }
    }

    private static int clamp(int value, int min, int max) {
        int low = Math.min(min, max);
        int high = Math.max(min, max);
        return Math.max(low, Math.min(high, value));
    }

    private static double clamp(double value, double min, double max) {
        double low = Math.min(min, max);
        double high = Math.max(min, max);
        return Math.max(low, Math.min(high, value));
    }

    private static String requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("config key is required");
        }
        return key;
    }

    private static List<String> sanitizeList(List<String> value, Predicate<Object> elementValidator) {
        if (value == null) {
            return List.of();
        }
        Predicate<Object> validator = elementValidator == null ? ignored -> true : elementValidator;
        ArrayList<String> result = new ArrayList<>();
        for (String item : value) {
            if (!validator.test(item)) {
                throw new IllegalArgumentException("Invalid list config value: " + item);
            }
            result.add(item == null ? "" : item);
        }
        return List.copyOf(result);
    }

    private static boolean validList(Object value, Predicate<Object> elementValidator) {
        if (!(value instanceof List<?> list)) {
            return false;
        }
        Predicate<Object> validator = elementValidator == null ? ignored -> true : elementValidator;
        return list.stream().allMatch(validator);
    }
}
