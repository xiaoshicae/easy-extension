package io.github.xiaoshicae.extension.core;

import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;
import io.github.xiaoshicae.extension.core.extension.IExtensionPointGroupDefaultImplementation;
import io.github.xiaoshicae.extension.core.proxy.IProxy;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace.EntryType;
import io.github.xiaoshicae.extension.core.trace.ResolveTrace.ResolutionEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The default implementations registered with a context.
 * <p>
 * There may be several, each answering for the extension points it implements (no extension point has two).
 * In a resolved chain they take one place per distinct code: default implementations extending
 * {@code AbstractExtensionPointDefaultImplementation} all share one code and priority, so the chain shows a single
 * "default" entry that answers for all of them.
 * </p>
 * Reads are lock-free (an immutable snapshot); registration is serialized.
 *
 * @param <T> matcher param class
 */
final class DefaultsRegistry<T> {

    private record Snapshot<T>(List<IExtensionPointGroupDefaultImplementation<T>> implementations,
                               Set<Class<?>> covered,
                               List<ResolutionEntry> entries) {
    }

    private final Object lock = new Object();
    private volatile Snapshot<T> snapshot = new Snapshot<>(List.of(), Set.of(), List.of());

    boolean isEmpty() {
        return snapshot.implementations().isEmpty();
    }

    /**
     * The default implementation registered first, or {@code null} if there is none.
     */
    IExtensionPointGroupDefaultImplementation<T> first() {
        List<IExtensionPointGroupDefaultImplementation<T>> all = snapshot.implementations();
        return all.isEmpty() ? null : all.get(0);
    }

    List<IExtensionPointGroupDefaultImplementation<T>> list() {
        return snapshot.implementations();
    }

    /**
     * One entry per distinct code, in registration order: the places the defaults take in a resolved chain.
     */
    List<ResolutionEntry> entries() {
        return snapshot.entries();
    }

    boolean covers(Class<?> extensionPoint) {
        return snapshot.covered().contains(extensionPoint);
    }

    boolean hasCode(String code) {
        for (ResolutionEntry entry : snapshot.entries()) {
            if (entry.code().equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Run {@code registration} (which registers the instance elsewhere) and, if it succeeds, record the instance.
     * Serialized, so that checking and recording cannot interleave with another registration.
     */
    void register(IExtensionPointGroupDefaultImplementation<T> instance, Registration registration) throws RegisterException {
        synchronized (lock) {
            registration.run();
            Snapshot<T> current = snapshot;

            List<IExtensionPointGroupDefaultImplementation<T>> implementations = new ArrayList<>(current.implementations());
            implementations.add(instance);
            Set<Class<?>> covered = new LinkedHashSet<>(current.covered());
            covered.addAll(instance.implementExtensionPoints());

            Map<String, ResolutionEntry> entries = new LinkedHashMap<>();
            for (ResolutionEntry entry : current.entries()) {
                entries.put(entry.code(), entry);
            }
            entries.putIfAbsent(instance.code(), new ResolutionEntry(instance.code(), instance.priority(), EntryType.DEFAULT));

            snapshot = new Snapshot<>(
                    Collections.unmodifiableList(implementations),
                    Collections.unmodifiableSet(covered),
                    List.copyOf(entries.values()));
        }
    }

    /**
     * Checks for a default implementation that is added next to the ones already registered, as opposed to the
     * first (and traditionally only) one: it must implement something, must not implement an extension point that
     * already has a default, must agree about priority with the defaults that share its code, and must not take
     * the priority of a default with another code (a chain cannot hold two entries of the same priority, so every
     * session would fail to resolve).
     */
    void checkAdditional(IExtensionPointGroupDefaultImplementation<T> instance) throws RegisterException {
        if (instance == null) {
            throw new RegisterParamException("extension point default implementation should not be null");
        }
        if (instance.implementExtensionPoints().isEmpty()) {
            throw new RegisterParamException(String.format(
                    "extension point default implementation [%s] should implement at least one extension point", describe(instance)));
        }
        Snapshot<T> current = snapshot;
        List<String> overlapping = instance.implementExtensionPoints().stream()
                .filter(current.covered()::contains)
                .map(Class::getName)
                .toList();
        if (!overlapping.isEmpty()) {
            throw new RegisterDuplicateException(String.format(
                    "extension point default implementation for [%s] already registered, [%s] cannot implement it again",
                    String.join(", ", overlapping), describe(instance)));
        }
        for (ResolutionEntry entry : current.entries()) {
            if (entry.code().equals(instance.code())) {
                if (!entry.priority().equals(instance.priority())) {
                    throw new RegisterParamException(String.format(
                            "extension point default implementations with code [%s] should have the same priority, found [%d] and [%d]",
                            entry.code(), entry.priority(), instance.priority()));
                }
            } else if (entry.priority().equals(instance.priority())) {
                throw new RegisterParamException(String.format(
                        "extension point default implementations with different codes should have different priorities, [%s] and [%s] both have priority [%d]",
                        entry.code(), instance.code(), entry.priority()));
            }
        }
    }

    /**
     * The extension points among {@code extensionPoints} that no default implementation answers for.
     */
    List<Class<?>> uncovered(Set<Class<?>> extensionPoints) {
        Set<Class<?>> covered = snapshot.covered();
        return extensionPoints.stream().filter(c -> !covered.contains(c)).collect(Collectors.toList());
    }

    /**
     * Whether the extension point is {@code @ExtensionPoint(mandatory = true)}: implemented by a business or an ability, with no default.
     */
    static boolean isMandatory(Class<?> extensionPoint) {
        ExtensionPoint annotation = extensionPoint.getAnnotation(ExtensionPoint.class);
        return annotation != null && annotation.mandatory();
    }

    static String describe(Object instance) {
        return instance instanceof IProxy<?> proxy ? proxy.getTargetClass().getName() : instance.getClass().getName();
    }

    /**
     * Registration step that may fail with a {@link RegisterException}.
     */
    @FunctionalInterface
    interface Registration {
        void run() throws RegisterException;
    }
}
