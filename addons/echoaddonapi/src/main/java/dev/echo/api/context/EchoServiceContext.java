package dev.echo.api.context;

import java.util.Optional;

public interface EchoServiceContext extends EchoContext {
    <T> Optional<T> service(Class<T> serviceType);
}
