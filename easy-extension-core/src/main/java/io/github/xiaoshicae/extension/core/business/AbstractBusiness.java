package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.extension.AbstractExtGroupRealization;

import java.util.List;

public abstract class AbstractBusiness<T> extends AbstractExtGroupRealization<T> implements IBusiness<T> {
    @Override
    public abstract Integer priority();

    @Override
    public abstract List<UsedAbility> usedAbilities();
}
