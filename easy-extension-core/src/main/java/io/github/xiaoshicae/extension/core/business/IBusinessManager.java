package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.exception.BusinessException;

import java.util.List;

public interface IBusinessManager<T> {

    /**
     * register business with the manager
     *
     * @param business business need to be registered with the manager
     * @throws BusinessException business invalid
     */
    void registerBusiness(IBusiness<T> business) throws BusinessException;

    /**
     * get business form the manager
     *
     * @param businessCode code of business
     * @throws BusinessException businessCode is null or ability not found(BusinessNotFoundException)
     */
    IBusiness<T> getBusiness(String businessCode) throws BusinessException;

    /**
     * get all businesses from the manager
     *
     * @return businesses
     */
    List<IBusiness<T>> listAllBusinesses();
}
