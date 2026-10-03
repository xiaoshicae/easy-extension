package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.QueryParamException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class DefaultBusinessManager<T> implements IBusinessManager<T> {
    /**
     * Immutable view of the registered businesses: O(1) lookup by code plus the businesses in registration order.
     *
     * @param <T>    matcher param class
     * @param byCode the businesses by code
     * @param all    the businesses in registration order
     */
    private record Snapshot<T>(Map<String, IBusiness<T>> byCode, List<IBusiness<T>> all) {
    }

    private final Object registrationLock = new Object();

    // Copy-on-write: registration happens at startup and is rare, so it builds a new snapshot under the lock;
    // reads happen on every request (each session init walks all businesses) and just dereference the current one,
    // with no lock and no copying.
    private volatile Snapshot<T> snapshot = new Snapshot<>(Collections.emptyMap(), Collections.emptyList());

    @Override
    public void registerBusiness(IBusiness<T> business) throws RegisterException {
        if (business == null) {
            throw new RegisterParamException("business should not be null");
        }

        for (Class<?> clazz : business.implementExtensionPoints()) {
            if (!clazz.isInterface()) {
                throw new RegisterParamException(String.format("business [%s] implement extension point class [%s] invalid, class should be an interface type", business.code(), clazz.getName()));
            }
            if (!clazz.isInstance(business)) {
                throw new RegisterParamException(String.format("business [%s] not implement extension point class [%s]", business.code(), clazz.getName()));
            }
        }

        synchronized (registrationLock) {
            Snapshot<T> current = snapshot;
            if (current.byCode().containsKey(business.code())) {
                throw new RegisterDuplicateException(String.format("business with code [%s] already register", business.code()));
            }
            Map<String, IBusiness<T>> byCode = new LinkedHashMap<>(current.byCode());
            byCode.put(business.code(), business);
            snapshot = new Snapshot<>(
                    Collections.unmodifiableMap(byCode),
                    Collections.unmodifiableList(new ArrayList<>(byCode.values())));
        }
    }

    @Override
    public IBusiness<T> getBusiness(String businessCode) throws QueryException {
        IBusiness<T> business = findBusiness(businessCode);
        if (business == null) {
            throw new QueryNotFoundException(String.format("business not found by code [%s]", businessCode));
        }
        return business;
    }

    @Override
    public IBusiness<T> findBusiness(String businessCode) throws QueryException {
        if (businessCode == null) {
            throw new QueryParamException("businessCode should not be null");
        }
        return snapshot.byCode().get(businessCode);
    }

    @Override
    public List<IBusiness<T>> listAllBusinesses() {
        return snapshot.all();
    }
}
