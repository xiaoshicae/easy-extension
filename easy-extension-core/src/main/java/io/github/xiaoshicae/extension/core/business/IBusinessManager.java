package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.QueryNotFoundException;
import io.github.xiaoshicae.extension.core.exception.QueryParamException;
import io.github.xiaoshicae.extension.core.exception.RegisterDuplicateException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;
import io.github.xiaoshicae.extension.core.exception.RegisterParamException;

import java.util.List;

public interface IBusinessManager<T> {

    /**
     * Register business by code.
     *
     * @param business business instance
     * @throws RegisterParamException     if {@code business} is null or implement extension point class not an interface type
     * @throws RegisterDuplicateException if business by code duplicate register
     */
    void registerBusiness(IBusiness<T> business) throws RegisterException;

    /**
     * Get business instance by code.
     *
     * @param businessCode code of business
     * @throws QueryParamException    if {@code businessCode} is null
     * @throws QueryNotFoundException if business not found
     */
    IBusiness<T> getBusiness(String businessCode) throws QueryException;

    /**
     * Get all businesses.
     *
     * @return all businesses
     */
    List<IBusiness<T>> listAllBusinesses();
}
