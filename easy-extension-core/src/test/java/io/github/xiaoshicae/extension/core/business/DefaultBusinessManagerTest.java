package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.exception.BusinessException;

import java.util.List;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;


public class DefaultBusinessManagerTest {
    DefaultBusinessManager<Object> manager = new DefaultBusinessManager<>();

    @Test
    public void testDefaultBusinessManager() throws BusinessException {
        BusinessException exception;

        exception = assertThrows(BusinessException.class, () -> manager.getBusiness(null));
        assertEquals("businessCode can not be null", exception.getMessage());

        exception = assertThrows(BusinessException.class, () -> manager.registerBusiness(null));
        assertEquals("business can not be null", exception.getMessage());

        manager.registerBusiness(new Business1());
        exception = assertThrows(BusinessException.class, () -> manager.registerBusiness(new Business1()));
        assertEquals("business Business1 already registered", exception.getMessage());

        manager.registerBusiness(new Business2());

        IBusiness<Object> b = manager.getBusiness("Business2");
        assertNotNull(b);
        assertEquals("Business2", b.code());
        assertEquals(1, b.implementsExtensions().size());
        assertEquals(Ext1.class, b.implementsExtensions().get(0));

        List<IBusiness<Object>> businessList = manager.listAllBusinesses();
        assertEquals(2, businessList.size());
        assertEquals(b, businessList.get(1));
    }
}
