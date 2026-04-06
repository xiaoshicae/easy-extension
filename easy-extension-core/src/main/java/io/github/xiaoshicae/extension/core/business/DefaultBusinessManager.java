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
    // Synchronized LinkedHashMap for O(1) lookup and ordered iteration
    private final Map<String, IBusiness<T>> businesses = Collections.synchronizedMap(new LinkedHashMap<>());

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

        synchronized (businesses) {
            if (businesses.containsKey(business.code())) {
                throw new RegisterDuplicateException(String.format("business with code [%s] already register", business.code()));
            }
            businesses.put(business.code(), business);
        }
    }

    @Override
    public IBusiness<T> getBusiness(String businessCode) throws QueryException {
        if (businessCode == null) {
            throw new QueryParamException("businessCode should not be null");
        }
        IBusiness<T> business = businesses.get(businessCode);
        if (business == null) {
            throw new QueryNotFoundException(String.format("business not found by code [%s]", businessCode));
        }
        return business;
    }

    @Override
    public List<IBusiness<T>> listAllBusinesses() {
        synchronized (businesses) {
            return Collections.unmodifiableList(new ArrayList<>(businesses.values()));
        }
    }
}
