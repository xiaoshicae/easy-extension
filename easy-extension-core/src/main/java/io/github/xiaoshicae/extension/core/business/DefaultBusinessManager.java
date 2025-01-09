package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.QueryParamException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultBusinessManager<T> implements IBusinessManager<T> {
    private final List<IBusiness<T>> businesses = new CopyOnWriteArrayList<>();

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

        boolean codeExist = businesses.stream().anyMatch(b -> b.code().equals(business.code()));
        if (codeExist) {
            throw new RegisterDuplicateException(String.format("business with code [%s] already register", business.code()));
        }

        businesses.add(business);
    }

    @Override
    public IBusiness<T> getBusiness(String businessCode) throws QueryException {
        if (Objects.isNull(businessCode)) {
            throw new QueryParamException("businessCode should not be null");
        }
        return businesses.stream().
                filter(b -> b.code().equals(businessCode)).
                findFirst().
                orElseThrow(() -> new QueryNotFoundException(String.format("business not found by code [%s]", businessCode)));
    }

    @Override
    public List<IBusiness<T>> listAllBusinesses() {
        return businesses;
    }
}
