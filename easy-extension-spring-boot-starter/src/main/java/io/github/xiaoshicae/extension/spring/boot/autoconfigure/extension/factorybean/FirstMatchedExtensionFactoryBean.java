package io.github.xiaoshicae.extension.spring.boot.autoconfigure.extension.factorybean;

import io.github.xiaoshicae.extension.core.IExtensionFactory;
import io.github.xiaoshicae.extension.core.FirstMatchedExtPointProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

public class FirstMatchedExtensionFactoryBean<T> implements FactoryBean<T> {
    private final Class<T> extensionPointClass;
    private final FirstMatchedExtPointProxyFactory<T> firstMatchedExtPointProxyFactory;

    public FirstMatchedExtensionFactoryBean(Class<T> extensionPointClass, IExtensionFactory extensionFactory) {
        this.extensionPointClass = extensionPointClass;
        this.firstMatchedExtPointProxyFactory = new FirstMatchedExtPointProxyFactory<>(extensionPointClass, extensionFactory);
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
    public FirstMatchedExtensionFactoryBean(Class<T> extensionPointClass, ObjectProvider<IExtensionFactory> extensionFactory) {
        this(extensionPointClass, new LazyExtensionFactory(extensionFactory));
    }

    @Override
    public T getObject() throws Exception {
        return firstMatchedExtPointProxyFactory.getProxy();
    }

    @Override
    public Class<T> getObjectType() {
        return extensionPointClass;
    }
}
