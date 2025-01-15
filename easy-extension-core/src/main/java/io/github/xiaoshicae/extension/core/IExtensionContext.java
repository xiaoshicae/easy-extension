package io.github.xiaoshicae.extension.core;

public interface IExtensionContext<T> extends IExtensionFactory, IExtensionInvoker, IExtensionRegister<T>, IExtensionReader<T>, ISessionManager<T> {
}
