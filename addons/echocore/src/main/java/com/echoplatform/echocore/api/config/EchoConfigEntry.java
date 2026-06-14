package com.echoplatform.echocore.api.config;

import com.echoplatform.echocore.api.config.EchoNativeConfigSpec.BooleanValue;
import com.echoplatform.echocore.api.config.EchoNativeConfigSpec.DoubleValue;
import com.echoplatform.echocore.api.config.EchoNativeConfigSpec.EnumValue;
import com.echoplatform.echocore.api.config.EchoNativeConfigSpec.IntValue;
import com.echoplatform.echocore.api.config.EchoNativeConfigSpec.StringValue;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public record EchoConfigEntry(String key, EchoConfigValueKind kind, String defaultValue, String description,
        EchoConfigSide side, Function<String, EchoConfigApplyResult> applier) {
    public EchoConfigEntry {
        side = side == null ? EchoConfigSide.COMMON : side;
        applier = applier == null ? ignored -> EchoConfigApplyResult.acceptedResult() : applier;
    }

    public EchoConfigEntry(String key, EchoConfigValueKind kind, String defaultValue, String description,
            EchoConfigSide side) {
        this(key, kind, defaultValue, description, side, null);
    }

    public EchoConfigEntry(String key, EchoConfigValueKind kind, String defaultValue, String description) {
        this(key, kind, defaultValue, description, EchoConfigSide.COMMON, null);
    }

    public EchoConfigApplyResult apply(String value) {
        return applier.apply(value);
    }

    public static EchoConfigEntry booleanSpec(String key, String label, String description, EchoConfigSide side,
            BooleanValue value, boolean defaultValue, boolean requiresRestart, boolean serverSynced,
            boolean editable) {
        return new EchoConfigEntry(key, EchoConfigValueKind.BOOLEAN, Boolean.toString(defaultValue), description, side);
    }

    public static EchoConfigEntry booleanSpec(String key, String label, String description, EchoConfigSide side,
            BooleanValue value, boolean requiresRestart, boolean serverSynced, boolean editable) {
        boolean defaultValue = value != null && Boolean.TRUE.equals(value.get());
        return new EchoConfigEntry(key, EchoConfigValueKind.BOOLEAN, Boolean.toString(defaultValue), description, side);
    }

    public static EchoConfigEntry intSpec(String key, String label, String description, EchoConfigSide side,
            IntValue value, int min, int max, boolean requiresRestart, boolean serverSynced, boolean editable) {
        String defaultValue = value == null ? Integer.toString(min) : Integer.toString(value.get());
        return new EchoConfigEntry(key, EchoConfigValueKind.INTEGER, defaultValue, description, side);
    }

    public static EchoConfigEntry doubleSpec(String key, String label, String description, EchoConfigSide side,
            DoubleValue value, double min, double max, boolean requiresRestart, boolean serverSynced,
            boolean editable) {
        String defaultValue = value == null ? Double.toString(min) : Double.toString(value.get());
        return new EchoConfigEntry(key, EchoConfigValueKind.DECIMAL, defaultValue, description, side);
    }

    public static EchoConfigEntry stringSpec(String key, String label, String description, EchoConfigSide side,
            StringValue value, boolean requiresRestart, boolean serverSynced, boolean editable) {
        String defaultValue = value == null ? "" : value.get();
        return new EchoConfigEntry(key, EchoConfigValueKind.STRING, defaultValue, description, side);
    }

    public static <T extends Enum<T>> EchoConfigEntry enumSpec(String key, String label, String description,
            EchoConfigSide side, EnumValue<T> value, Class<T> type, boolean requiresRestart, boolean serverSynced,
            boolean editable) {
        T defaultValue = value == null ? null : value.get();
        return new EchoConfigEntry(key, EchoConfigValueKind.ENUM, defaultValue == null ? "" : defaultValue.name(),
                description, side);
    }

    public static <T extends Enum<T>> EchoConfigEntry enumEntry(String key, String label, String description,
            EchoConfigSide side, T defaultValue, Class<T> type, Supplier<T> getter, Consumer<T> setter,
            Object validator, boolean editable, boolean restartRequired, boolean newWorldOnly) {
        T value = getter == null ? null : getter.get();
        return new EchoConfigEntry(key, EchoConfigValueKind.ENUM,
                String.valueOf(value == null ? defaultValue : value), description, side);
    }

    public static EchoConfigEntry booleanEntry(String key, String label, String description, EchoConfigSide side,
            boolean defaultValue, BooleanSupplier getter, Consumer<Boolean> setter, Object validator,
            boolean editable, boolean restartRequired, boolean newWorldOnly) {
        boolean value = getter == null ? defaultValue : getter.getAsBoolean();
        return new EchoConfigEntry(key, EchoConfigValueKind.BOOLEAN, Boolean.toString(value), description, side, raw -> {
            if (setter == null) {
                return EchoConfigApplyResult.rejected("Config entry is read-only.");
            }
            if (!"true".equalsIgnoreCase(raw) && !"false".equalsIgnoreCase(raw)) {
                return EchoConfigApplyResult.rejected("Expected boolean value.");
            }
            setter.accept(Boolean.parseBoolean(raw));
            return EchoConfigApplyResult.acceptedResult();
        });
    }

    public static EchoConfigEntry intEntry(String key, String label, String description, EchoConfigSide side,
            int defaultValue, int min, int max, Supplier<Integer> getter, Consumer<Integer> setter, Object validator,
            boolean editable, boolean restartRequired, boolean newWorldOnly) {
        int value = getter == null ? defaultValue : getter.get();
        return new EchoConfigEntry(key, EchoConfigValueKind.INTEGER, Integer.toString(value), description, side, raw -> {
            if (setter == null) {
                return EchoConfigApplyResult.rejected("Config entry is read-only.");
            }
            try {
                int parsed = Integer.parseInt(raw);
                if (parsed < min || parsed > max) {
                    return EchoConfigApplyResult.rejected("Value is outside the allowed range.");
                }
                setter.accept(parsed);
                return EchoConfigApplyResult.acceptedResult();
            } catch (NumberFormatException exception) {
                return EchoConfigApplyResult.rejected("Expected integer value.");
            }
        });
    }
}
