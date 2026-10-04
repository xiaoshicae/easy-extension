package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean;

import io.github.xiaoshicae.extension.core.IExtensionFactory;
import io.github.xiaoshicae.extension.core.AllMatchedExtPointProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class AllMatchedExtensionFactoryBean<T> implements FactoryBean<List<T>> {
    private final AllMatchedExtPointProxyFactory<T> allMatchedExtPointProxyFactory;

    public AllMatchedExtensionFactoryBean(Class<T> extensionPointClass, IExtensionFactory extensionFactory) {
        this.allMatchedExtPointProxyFactory = new AllMatchedExtPointProxyFactory<>(extensionPointClass, extensionFactory);
    }

    /**
     * The constructor the container uses: the extension factory is looked up when the extension point is first
     * called, so that creating the proxy does not create the extension context (see {@link LazyExtensionFactory}).
     *
     * @param extensionPointClass the extension point the proxy stands for
     * @param extensionFactory    where to get the extension factory from, when it is first needed
     * @since 3.4
     */
    @Autowired
    public AllMatchedExtensionFactoryBean(Class<T> extensionPointClass, ObjectProvider<IExtensionFactory> extensionFactory) {
        this(extensionPointClass, new LazyExtensionFactory(extensionFactory));
    }

    @Override
    public List<T> getObject() throws Exception {
        return allMatchedExtPointProxyFactory.getProxy();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<List<T>> getObjectType() {
        return (Class<List<T>>) (Class<?>) List.class;
    }
}
