package io.github.xiaoshicae.extension.core.test;

import org.junit.Test;
import io.github.xiaoshicae.extension.core.BaseDefaultAbility;
import io.github.xiaoshicae.extension.core.DefaultExtContext;
import io.github.xiaoshicae.extension.core.ability.AbstractAbility;
import io.github.xiaoshicae.extension.core.annotation.ExtensionPoint;
import io.github.xiaoshicae.extension.core.business.AbstractBusiness;
import io.github.xiaoshicae.extension.core.business.UsedAbility;
import io.github.xiaoshicae.extension.core.exception.ExtensionException;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.*;


public class DefaultExtContextTest {

    @Test
    public void testRegister() throws Exception {
        ExtensionException exception;
        DefaultExtContext<Object> context = new DefaultExtContext<>(true, true, true);
        context.registerBusiness(new BusinessX());
        exception = assertThrows(ExtensionException.class, () -> context.registerBusiness(new BusinessX()));
        assertEquals("business BusinessX already registered", exception.getMessage());
        context.registerBusiness(new BusinessY());
        context.registerBusiness(new BusinessZ());

        exception = assertThrows(ExtensionException.class, () -> context.registerAbility(new AbilityL()));
        assertEquals("ability AbilityL should implement at least one interface that annotated with @ExtensionPoint", exception.getMessage());
        context.registerAbility(new AbilityM());
        exception = assertThrows(ExtensionException.class, () -> context.registerAbility(new AbilityM()));
        assertEquals("ability AbilityM already registered", exception.getMessage());
        context.registerAbility(new AbilityN());
        context.registerAbility(new ExtDefaultAbility());
    }


    @Test
    public void testSession() throws Exception {
        DefaultExtContext<Object> context = new DefaultExtContext<>(true, false, false);
        context.registerBusiness(new BusinessX());
        context.registerBusiness(new BusinessY());
        context.registerBusiness(new BusinessZ());
        context.registerBusiness(new BusinessZZ());
        context.registerBusiness(new BusinessPriorityConflict());
        context.registerAbility(new AbilityM());
        context.registerAbility(new AbilityN());
        context.registerAbility(new ExtDefaultAbility());

        ExecutorService executor = Executors.newFixedThreadPool(3);
        Callable<String> task = () -> {
            System.out.println("thread " + Thread.currentThread().getName() + " running ... ");
            ExtensionException innerException;
            context.initSession("XXX");
            List<ExtA> extAList = context.getAllMatchedExtension(ExtA.class);
            assertEquals(1, extAList.size());

            context.initSession("BusinessX-BusinessY");
            extAList = context.getAllMatchedExtension(ExtA.class);
            assertEquals(2, extAList.size());

            context.initSession("BusinessPriorityConflict");
            extAList = context.getAllMatchedExtension(ExtA.class);
            assertEquals(3, extAList.size());
            assertTrue(extAList.get(0) instanceof BusinessPriorityConflict);
            assertTrue(extAList.get(1) instanceof AbilityM);
            assertTrue(extAList.get(2) instanceof ExtDefaultAbility);

            return "done";
        };

        Future<String> future1 = executor.submit(task);
        Future<String> future2 = executor.submit(task);
        Future<String> future3 = executor.submit(task);
        future1.get();
        future2.get();
        future3.get();
    }


    @Test
    public void testSessionWithStrict() throws Exception {
        DefaultExtContext<Object> context = new DefaultExtContext<>(true, true, true);
        context.registerBusiness(new BusinessX());
        context.registerBusiness(new BusinessY());
        context.registerBusiness(new BusinessZ());
        context.registerBusiness(new BusinessZZ());
        context.registerBusiness(new BusinessPriorityConflict());
        context.registerAbility(new AbilityM());
        context.registerAbility(new AbilityN());

        ExecutorService executor = Executors.newFixedThreadPool(3);
        Callable<String> task = () -> {
            System.out.println("thread " + Thread.currentThread().getName() + " running ... ");
            ExtensionException innerException;
            innerException = assertThrows(ExtensionException.class, () -> context.initSession("XXX"));
            assertEquals("business not found", innerException.getMessage());

            innerException = assertThrows(ExtensionException.class, () -> context.initSession("BusinessX-BusinessY"));
            assertEquals("multiple business found, matched business codes: [BusinessX, BusinessY]", innerException.getMessage());

            innerException = assertThrows(ExtensionException.class, () -> context.initSession("BusinessX"));
            assertEquals("ability ability.application.default not found", innerException.getMessage());

            return "done";
        };

        Future<String> future1 = executor.submit(task);
        Future<String> future2 = executor.submit(task);
        Future<String> future3 = executor.submit(task);
        future1.get();
        future2.get();
        future3.get();
    }

    @Test
    public void testGetMatchedExtension() throws Exception {
        DefaultExtContext<Object> context = new DefaultExtContext<>(true, false, false);
        context.registerBusiness(new BusinessX());
        context.registerBusiness(new BusinessY());
        context.registerBusiness(new BusinessZ());
        context.registerBusiness(new BusinessZZ());
        context.registerBusiness(new BusinessPriorityConflict());
        context.registerAbility(new AbilityM());
        context.registerAbility(new AbilityN());
        context.registerAbility(new ExtDefaultAbility());

        ExecutorService executor = Executors.newFixedThreadPool(3);
        Callable<String> task = () -> {
            System.out.println("thread " + Thread.currentThread().getName() + " running ... ");
            ExtensionException innerException;
            List<ExtA> extAList;
            ExtA extA;
            assertThrows(ExtensionException.class, ()-> context.getAllMatchedExtension(ExtA.class));
            assertThrows(ExtensionException.class, ()-> context.getFirstMatchedExtension(ExtA.class));

            context.initSession("XXX");
            extAList = context.getAllMatchedExtension(ExtA.class);
            assertEquals(1, extAList.size());
            extA = context.getFirstMatchedExtension(ExtA.class);
            assertEquals("ExtDefaultAbility do extA", extA.extA());

            context.initSession("BusinessX-BusinessY");
            extAList = context.getAllMatchedExtension(ExtA.class);
            assertEquals(2, extAList.size());
            assertEquals("BusinessX exec extA", extAList.get(0).extA());
            assertEquals("ExtDefaultAbility do extA", extAList.get(1).extA());
            extA = context.getFirstMatchedExtension(ExtA.class);
            assertEquals("BusinessX exec extA", extA.extA());

            context.initSession("BusinessPriorityConflict");
            extAList = context.getAllMatchedExtension(ExtA.class);
            assertEquals(3, extAList.size());
            assertEquals("BusinessPriorityConflict extA", extAList.get(0).extA());
            assertEquals("AbilityM exec extA", extAList.get(1).extA());
            assertEquals("ExtDefaultAbility do extA", extAList.get(2).extA());

            context.removeSession();
            assertThrows(ExtensionException.class, ()-> context.getAllMatchedExtension(ExtA.class));

            return "done";
        };

        Future<String> future1 = executor.submit(task);
        Future<String> future2 = executor.submit(task);
        Future<String> future3 = executor.submit(task);
        future1.get();
        future2.get();
        future3.get();
    }
}

class BusinessX extends AbstractBusiness<Object> implements ExtA {
    @Override
    public String code() {
        return "BusinessX";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("BusinessX");
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityM", 200));
    }


    @Override
    public String extA() {
        return "BusinessX exec extA";
    }
}

class BusinessY extends AbstractBusiness<Object> implements ExtB {
    @Override
    public String code() {
        return "BusinessY";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("BusinessY");
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityM", 100));
    }


    @Override
    public String extB() {
        return "BusinessY exec extB";
    }
}

class BusinessZ extends AbstractBusiness<Object> {
    @Override
    public String code() {
        return "BusinessZ";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("BusinessZ");
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("Ability2", 10));
    }
}


class BusinessZZ extends AbstractBusiness<Object> {
    @Override
    public String code() {
        return "BusinessZZ";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("BusinessZZ");
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return null;
    }
}


class BusinessPriorityConflict extends AbstractBusiness<Object> implements ExtA {
    @Override
    public String code() {
        return "BusinessPriorityConflict";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("BusinessPriorityConflict");
    }

    @Override
    public Integer priority() {
        return 100;
    }

    @Override
    public List<UsedAbility> usedAbilities() {
        return List.of(new UsedAbility("AbilityM", 100));
    }

    @Override
    public String extA() {
        return "BusinessPriorityConflict extA";
    }
}

class AbilityL extends AbstractAbility<Object> {
    @Override
    public String code() {
        return "AbilityL";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("AbilityL");
    }
}

class AbilityM extends AbstractAbility<Object> implements ExtA, ExtB {
    @Override
    public String code() {
        return "AbilityM";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("AbilityM") || param.toString().contains("BusinessPriorityConflict");
    }


    @Override
    public String extA() {
        return "AbilityM exec extA";
    }

    @Override
    public String extB() {
        return "AbilityM exec extB";
    }
}

class AbilityN extends AbstractAbility<Object> implements ExtB, ExtC {
    @Override
    public String code() {
        return "AbilityN";
    }

    @Override
    public Boolean match(Object param) {
        return param.toString().contains("AbilityN");
    }


    @Override
    public String extB() {
        return "AbilityN exec extB";
    }

    @Override
    public String extC() {
        return "AbilityN exec extC";
    }
}

@ExtensionPoint
interface ExtA {
    String extA();
}

@ExtensionPoint
interface ExtB {
    String extB();
}

@ExtensionPoint
interface ExtC {
    String extC();
}

class ExtDefaultAbility extends BaseDefaultAbility<Object> implements ExtA, ExtB, ExtC {
    @Override
    public String extA() {
        return "ExtDefaultAbility do extA";
    }

    @Override
    public String extB() {
        return "ExtDefaultAbility do extB";
    }

    @Override
    public String extC() {
        return "ExtDefaultAbility do extC";
    }
}