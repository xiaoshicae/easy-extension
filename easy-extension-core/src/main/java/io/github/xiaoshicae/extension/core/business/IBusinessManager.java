package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.exception.BusinessException;

import java.util.List;

public interface IBusinessManager<T> {

    /**
     * register business to container
     *
     * @param business @NonNull
     * @throws BusinessException business invalid
     */
    void registerBusiness(IBusiness<T> business) throws BusinessException;

    /**
     * get business form container
     *
     * @param businessCode @NonNull code of business
     * @throws BusinessException businessCode is null or ability not found(BusinessNotFoundException)
     */
    IBusiness<T> getBusiness(String businessCode) throws BusinessException;

    /**
     * get all businesses from container
     *
     * @return businesses
     */
    List<IBusiness<T>> listAllBusinesses();
}
