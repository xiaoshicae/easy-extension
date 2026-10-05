package io.github.xiaoshicae.extension.core.business;

import io.github.xiaoshicae.extension.core.exception.QueryException;
import io.github.xiaoshicae.extension.core.exception.RegisterException;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class DefaultBusinessManagerTest {

    @Test
    public void testRegisterBusiness() throws RegisterException {
        RegisterException e;
        DefaultBusinessManager<Object> manager = new DefaultBusinessManager<>();

        e = assertThrows(RegisterException.class, () -> manager.registerBusiness(null));
        assertEquals("business should not be null", e.getMessage());

        manager.registerBusiness(new Business1());

        e = assertThrows(RegisterException.class, () -> manager.registerBusiness(new Business2()));
        assertEquals("business [Business2] implement extension point class [java.lang.String] invalid, class should be an interface type", e.getMessage());

        e = assertThrows(RegisterException.class, () -> manager.registerBusiness(new Business3()));
        assertEquals("business [Business3] not implement extension point class [" + Ext1.class.getName() + "]", e.getMessage());

        e = assertThrows(RegisterException.class, () -> manager.registerBusiness(new Business1()));
        assertEquals("business with code [Business1] already register", e.getMessage());
    }

    @Test
    public void testRegisterBusinessWithNullCodeAndListAll() throws RegisterException {
        DefaultBusinessManager<Object> manager = new DefaultBusinessManager<>();

        // null code must stay a RegisterException (not a NullPointerException from the concurrent map)
        RegisterException e = assertThrows(RegisterException.class, () -> manager.registerBusiness(new Business1() {
            @Override
            public String code() {
                return null;
            }
        }));
        assertEquals("instance code should not be null", e.getMessage());

        manager.registerBusiness(new Business1());
        List<IBusiness<Object>> all = manager.listAllBusinesses();
        assertEquals(1, all.size());
        // null-tolerant, read-only view
        assertFalse(all.contains(null));
        assertThrows(UnsupportedOperationException.class, () -> all.add(null));
    }

    @Test
    public void testGetBusiness() throws QueryException {
        QueryException e;
        DefaultBusinessManager<Object> manager = new DefaultBusinessManager<>();

        e = assertThrows(QueryException.class, () -> manager.getBusiness(null));
        assertEquals("businessCode should not be null", e.getMessage());

        e = assertThrows(QueryException.class, () -> manager.getBusiness("c"));
        assertEquals("business not found by code [c]", e.getMessage());

        try {
            manager.registerBusiness(new Business1());
        } catch (RegisterException e1) {
            throw new RuntimeException(e1);
        }
        IBusiness<Object> business = manager.getBusiness("Business1");
        assertEquals("Business1", business.code());
    }

    @Test
    public void testListAllAbilities() throws QueryException {
        QueryException e;
        DefaultBusinessManager<Object> manager = new DefaultBusinessManager<>();


        try {
            manager.registerBusiness(new Business1());
            manager.registerBusiness(new Business4());
        } catch (RegisterException e1) {
            throw new RuntimeException(e1);
        }

        List<IBusiness<Object>> businesses = manager.listAllBusinesses();
        assertEquals(2, businesses.size());
        assertEquals("Business1", businesses.get(0).code());
        assertEquals("Business4", businesses.get(1).code());

        manager.getBusiness("Business1");

        businesses = manager.listAllBusinesses();
        assertEquals(2, businesses.size());
        assertEquals("Business1", businesses.get(0).code());
        assertEquals("Business4", businesses.get(1).code());
    }
}
