package io.github.xiaoshicae.extension.core.test;

import org.junit.Test;
import io.github.xiaoshicae.extension.core.business.AbstractBusiness;
import io.github.xiaoshicae.extension.core.business.DefaultBusinessManager;
import io.github.xiaoshicae.extension.core.business.IBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.exception.BusinessException;

import java.util.List;

import static org.junit.Assert.*;

public class DefaultBusinessManagerTest {
    @Test
    public void testRegisterBusiness() throws BusinessException {
        BusinessException exception;
        DefaultBusinessManager<Object> manager = new DefaultBusinessManager<>();

        exception = assertThrows(BusinessException.class, () -> manager.registerBusiness(null));
        assertEquals("business can not be null", exception.getMessage());

        manager.registerBusiness(new Business2());
        exception = assertThrows(BusinessException.class, () -> manager.registerBusiness(new Business2()));
        assertEquals("business Business2 already registered", exception.getMessage());
    }

    @Test
    public void testGetBusiness() throws BusinessException {
        BusinessException exception;
        DefaultBusinessManager<Object> manager = new DefaultBusinessManager<>();

        exception = assertThrows(BusinessException.class, () -> manager.getBusiness(null));
        assertEquals("businessCode can not be null", exception.getMessage());

        exception = assertThrows(BusinessException.class, () -> manager.getBusiness("biz"));
        assertEquals("business biz not found", exception.getMessage());

        manager.registerBusiness(new Business2());
        IBusiness<Object> b = manager.getBusiness("Business2");
        assertNotNull(b);
        assertEquals("Business2", b.code());
        assertEquals(1, b.implementsExtensions().size());
        assertEquals(ExtA.class, b.implementsExtensions().get(0));
        List<IBusiness<Object>> businessList = manager.listAllBusinesses();
        assertEquals(1, businessList.size());
        assertEquals(b, businessList.get(0));
    }
}


class Business1 extends AbstractBusiness<Object> {
    @Override
    public String code() {
        return "Business1";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("Business1");
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("Ability1", 200));
    }
}

class Business2 extends AbstractBusiness<Object> implements ExtA {
    @Override
    public String code() {
        return "Business2";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("Business1");
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("Ability1", 200));
    }

    @Override
    public String extA() {
        return "Business2 extA";
    }
}
