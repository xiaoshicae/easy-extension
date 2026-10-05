package io.github.xiaoshicae.extension.core.internal;

import io.github.xiaoshicae.extension.core.ExtensionContext;
import io.github.xiaoshicae.extension.core.exception.ResolutionException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * The proxies behind {@link ExtensionContext#proxy(Class)} and {@link ExtensionContext#proxyAll(Class)}.
 * They are bound to the context they are created from, so a wrapping context gets proxies bound to the wrapper.
 */
public final class ExtensionProxies {

    private ExtensionProxies() {
    }

    /**
     * A proxy of {@code point} that forwards every call to {@code context.first(point)}.
     */
    @SuppressWarnings("unchecked")
    public static <E> E proxy(ExtensionContext<?> context, Class<E> point) {
        return (E) Proxy.newProxyInstance(point.getClassLoader(), new Class<?>[]{point}, new FirstHandler(context, point));
    }

    /**
     * A read-only list view that always reflects {@code context.all(point)}.
     */
    public static <E> List<E> proxyAll(ExtensionContext<?> context, Class<E> point) {
        return new AllList<>(context, point);
    }

    /**
     * Forwards each call to the first implementation of the current binding and rethrows what that implementation threw.
     * {@code toString/hashCode/equals} never need a binding.
     */
    private static final class FirstHandler implements InvocationHandler {
        private final ExtensionContext<?> context;
        private final Class<?> point;

        FirstHandler(ExtensionContext<?> context, Class<?> point) {
            this.context = context;
            this.point = point;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "ExtensionProxy[" + point.getName() + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                };
            }
            Object target = context.first(point);
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    /**
     * Read-only list view of {@code context.all(point)}. Every operation resolves the current binding once and works on
     * that snapshot; successive calls (e.g. {@code size()} then {@code get(i)}) may see different snapshots.
     */
    private static final class AllList<E> extends AbstractList<E> {
        private final ExtensionContext<?> context;
        private final Class<E> point;

        AllList(ExtensionContext<?> context, Class<E> point) {
            this.context = context;
            this.point = point;
        }

        private List<E> snapshot() {
            return context.all(point);
        }

        @Override
        public E get(int index) {
            return snapshot().get(index);
        }

        @Override
        public int size() {
            return snapshot().size();
        }

        @Override
        public Iterator<E> iterator() {
            return snapshot().iterator();
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            return snapshot().listIterator(index);
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            return snapshot().subList(fromIndex, toIndex);
        }

        @Override
        public boolean contains(Object o) {
            return snapshot().contains(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return snapshot().containsAll(c);
        }

        @Override
        public int indexOf(Object o) {
            return snapshot().indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return snapshot().lastIndexOf(o);
        }

        @Override
        public Object[] toArray() {
            return snapshot().toArray();
        }

        @Override
        public <A> A[] toArray(A[] a) {
            return snapshot().toArray(a);
        }

        @Override
        public Spliterator<E> spliterator() {
            return snapshot().spliterator();
        }

        @Override
        public void forEach(Consumer<? super E> action) {
            snapshot().forEach(action);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || snapshot().equals(o);
        }

        @Override
        public int hashCode() {
            return snapshot().hashCode();
        }

        @Override
        public String toString() {
            try {
                return snapshot().toString();
            } catch (ResolutionException e) {
                if (e.reason() == ResolutionException.Reason.NO_BINDING) {
                    return "ExtensionProxyList[" + point.getName() + "]";
                }
                throw e;
            }
        }
    }
}
