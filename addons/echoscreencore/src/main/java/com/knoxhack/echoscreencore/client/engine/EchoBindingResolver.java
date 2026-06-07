package com.knoxhack.echoscreencore.client.engine;

import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.client.debug.EchoScreenDiagnostics;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EchoBindingResolver {
    private static final Pattern BINDING = Pattern.compile("\\{([^{}]+)}");
    private static volatile boolean debugPlaceholders;

    public static void setDebugPlaceholders(boolean enabled) {
        debugPlaceholders = enabled;
    }

    public static boolean debugPlaceholders() {
        return debugPlaceholders;
    }

    public String resolve(String raw, EchoDataContext context, EchoScreenDiagnostics diagnostics) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        EchoDataContext safeContext = context == null ? EchoDataContext.empty() : context;
        Matcher matcher = BINDING.matcher(raw);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String expression = matcher.group(1);
            String replacement = resolveExpression(expression, safeContext, diagnostics).orElseGet(() -> {
                if (diagnostics != null) {
                    diagnostics.warnOnce("missing_binding_provider", bindingPath(expression));
                }
                return debugPlaceholders ? "{?" + bindingPath(expression) + "}" : safeContext.missingPlaceholder();
            });
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    public boolean containsBinding(String raw) {
        return raw != null && BINDING.matcher(raw).find();
    }

    public Optional<Object> resolveObject(String expression, EchoDataContext context, EchoScreenDiagnostics diagnostics) {
        EchoDataContext safeContext = context == null ? EchoDataContext.empty() : context;
        String clean = unwrap(expression);
        String path = bindingPath(clean);
        Optional<Object> resolved = safeContext.resolve(path);
        if (resolved.isEmpty() && diagnostics != null) {
            diagnostics.warnOnce("missing_binding_provider", path);
        }
        return resolved;
    }

    public String bindingPath(String expression) {
        return pathPart(expression);
    }

    private Optional<String> resolveExpression(String expression, EchoDataContext context, EchoScreenDiagnostics diagnostics) {
        String path = pathPart(expression);
        String fallback = fallback(expression);
        Optional<Object> value = context.resolve(path);
        if (value.isPresent()) {
            return Optional.of(String.valueOf(value.get()));
        }
        if (fallback != null) {
            return Optional.of(fallback);
        }
        return Optional.empty();
    }

    private static String pathPart(String expression) {
        String clean = unwrap(expression);
        int fallback = clean.indexOf('|');
        return (fallback < 0 ? clean : clean.substring(0, fallback)).trim();
    }

    private static String fallback(String expression) {
        String clean = unwrap(expression);
        int fallback = clean.indexOf('|');
        return fallback < 0 ? null : clean.substring(fallback + 1).trim();
    }

    private static String unwrap(String expression) {
        String clean = expression == null ? "" : expression.trim();
        if (clean.startsWith("{") && clean.endsWith("}")) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }
        return clean;
    }
}
