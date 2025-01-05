package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.QueryParamException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DefaultBusinessManager<T> implements IBusinessManager<T> {
    private final Map<String, IBusiness<T>> businesses = new LinkedHashMap<>();

    @Override
    public void registerBusiness(IBusiness<T> business) throws RegisterException {
        if (Objects.isNull(business)) {
            throw new RegisterException("business should not be null");
        }

        for (Class<?> clazz : business.implementExtensionPoints()) {
            if (!clazz.isInterface()) {
                throw new RegisterParamException(String.format("business [%s] implement extension point class [%s] invalid, class should be an interface type", business.code(), clazz.getName()));
            }
            if (!clazz.isInstance(business)) {
                throw new RegisterParamException(String.format("business [%s] not implement extension point class [%s]", business.code(), clazz.getName()));
            }
        }

        if (businesses.containsKey(business.code())) {
            throw new RegisterDuplicateException(String.format("business [%s] already registered", business.code()));
        }

        businesses.put(business.code(), business);
    }

    @Override
    public IBusiness<T> getBusiness(String businessCode) throws QueryException {
        if (Objects.isNull(businessCode)) {
            throw new QueryParamException("businessCode should not be null");
        }

        IBusiness<T> business = businesses.get(businessCode);
        if (Objects.isNull(business)) {
            throw new QueryNotFoundException(String.format("business not found by code [%s]", businessCode));
        }

        return business;
    }

    @Override
    public List<IBusiness<T>> listAllBusinesses() {
        return new ArrayList<>(businesses.values());
    }
}
