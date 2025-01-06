package io.github.xiaoshicae.extension.core;

public interface IExtensionContext<T> extends IExtensionFactory, IExtensionRegister<T>, IExtensionReader<T>, ISessionManager<T> {
}
