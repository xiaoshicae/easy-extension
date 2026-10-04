package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean;

import io.github.xiaoshicae.extension.core.IExtensionFactory;
import io.github.xiaoshicae.extension.core.exception.QueryException;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

/**
 * An {@link IExtensionFactory} that looks the real one (the extension context bean) up when it is first used, not
 * when it is created.
 * <p>
 * The proxy an extension point is injected as does nothing until it is called, so creating it must not need the
 * extension context. If it does, every bean that carries {@code @ExtensionInject} depends on the context, and the
 * context depends on every business and ability: a business that needs a service that calls an extension point is a
 * cycle, which Spring Boot refuses by default.
 * </p>
 *
 * @since 3.4
 */
public final class LazyExtensionFactory implements IExtensionFactory {
    private final ObjectProvider<IExtensionFactory> provider;
    private volatile IExtensionFactory resolved;

    /**
     * @param provider where to get the real extension factory from, when it is first needed
     */
    public LazyExtensionFactory(ObjectProvider<IExtensionFactory> provider) {
        this.provider = provider;
    }

    private IExtensionFactory delegate() {
        IExtensionFactory factory = resolved;
        if (factory == null) {
            factory = provider.getObject();
            resolved = factory;
        }
        return factory;
    }

    @Override
    public <T> T getFirstMatchedExtension(Class<T> extensionPointType) throws QueryException {
        return delegate().getFirstMatchedExtension(extensionPointType);
    }

    @Override
    public <T> List<T> getAllMatchedExtension(Class<T> extensionPointType) throws QueryException {
        return delegate().getAllMatchedExtension(extensionPointType);
    }

    @Override
    public <T> T getFirstMatchedExtension(String scope, Class<T> extensionPointType) throws QueryException {
        return delegate().getFirstMatchedExtension(scope, extensionPointType);
    }

    @Override
    public <T> List<T> getAllMatchedExtension(String scope, Class<T> extensionPointType) throws QueryException {
        return delegate().getAllMatchedExtension(scope, extensionPointType);
    }
}
