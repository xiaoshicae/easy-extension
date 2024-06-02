package io.github.xiaoshicae.extension.core.test;

import org.junit.Test;
import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.session.DefaultSession;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class DefaultSessionTest {
    @Test
    public void testSetMatchedCode() throws Exception {
        DefaultSession session = new DefaultSession();

        ExecutorService executor = Executors.newFixedThreadPool(3);
        Callable<String> task = () -> {
            System.out.println("thread " + Thread.currentThread().getName() + " running ... ");
            SessionException innerException;

            session.setMatchedCode("a", 1);
            innerException = assertThrows(SessionException.class, () -> session.setMatchedCode("a", 1));
            assertEquals("code a already exist", innerException.getMessage());
            session.setMatchedCode("b", 1);
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
    public void testSetMatchedCodeWithStrict() throws Exception {
        DefaultSession session = new DefaultSession(true);

        ExecutorService executor = Executors.newFixedThreadPool(3);
        Callable<String> task = () -> {
            System.out.println("thread " + Thread.currentThread().getName() + " running ... ");
            SessionException innerException;

            session.setMatchedCode("a", 1);
            innerException = assertThrows(SessionException.class, () -> session.setMatchedCode("a", 1));
            assertEquals("code a already exist", innerException.getMessage());

            innerException = assertThrows(SessionException.class, () -> session.setMatchedCode("b", 1));
            assertEquals("priority equal conflict, (code b with priority 1) equal to (code a with priority 1)", innerException.getMessage());
            session.setMatchedCode("b", 2);
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
    public void testGetMatchedCode() throws Exception {
        DefaultSession session = new DefaultSession();

        ExecutorService executor = Executors.newFixedThreadPool(3);
        Callable<String> task = () -> {
            System.out.println("thread " + Thread.currentThread().getName() + " running ... ");
            SessionException innerException;

            innerException = assertThrows(SessionException.class, session::getMatchedCodes);
            assertEquals("matched codes is empty, may be no code register", innerException.getMessage());

            session.setMatchedCode("a", 1);
            session.setMatchedCode("b", 10);
            session.setMatchedCode("c", 5);
            session.setMatchedCode("d", 5);
            List<String> innerMatchedCodes = session.getMatchedCodes();
            assertEquals(4, innerMatchedCodes.size());
            assertEquals("a", innerMatchedCodes.get(0));
            assertEquals("c", innerMatchedCodes.get(1));
            assertEquals("d", innerMatchedCodes.get(2));
            assertEquals("b", innerMatchedCodes.get(3));

            session.remove();
            innerException = assertThrows(SessionException.class, session::getMatchedCodes);
            assertEquals("matched codes is empty, may be no code register", innerException.getMessage());
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
