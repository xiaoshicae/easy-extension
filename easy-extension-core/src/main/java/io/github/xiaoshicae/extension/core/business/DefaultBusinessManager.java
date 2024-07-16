package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.exception.BusinessNotFoundException;
import io.github.xiaoshicae.extension.core.exception.BusinessException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultBusinessManager<T> implements IBusinessManager<T> {
    private final Map<String, IBusiness<T>> businesses = new LinkedHashMap<>();

    @Override
    public void registerBusiness(IBusiness<T> business) throws BusinessException {
        if (business == null) {
            throw new BusinessException("business can not be null");
        }

        if (businesses.containsKey(business.code())) {
            throw new BusinessException("business " + business.code() + " already registered");
        }

        businesses.put(business.code(), business);
    }

    @Override
    public IBusiness<T> getBusiness(String businessCode) throws BusinessException {
        if (businessCode == null) {
            throw new BusinessException("businessCode can not be null");
        }

        IBusiness<T> business = businesses.get(businessCode);
        if (business == null) {
            throw new BusinessNotFoundException("business " + businessCode + " not found");
        }

        return business;
    }

    @Override
    public List<IBusiness<T>> listAllBusinesses() {
        return new ArrayList<>(businesses.values());
    }
}
