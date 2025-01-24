package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.exception.SessionException;

import java.util.List;
import java.util.concurrent.*;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultSessionTest {
    private final DefaultScopedSessionManager session = new DefaultScopedSessionManager();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    @AfterEach
    public void clear() {
        session.removeAllSession();
    }

    @Test
    public void testScopedSessionInSingleThread() throws Exception {
        testScopedSession();
    }

    @Test
    public void testScopedSessionInMultiThread() throws Exception {
        for (int i = 0; i < 3; i++) {
            executor.execute(() -> {
                try {
                    testScopedSession();
                } catch (SessionException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    private void testScopedSession() throws SessionException {
        SessionException e;
        e = assertThrows(SessionException.class, () -> session.getScopedMatchedCodes("xxx"));
        assertEquals("scope [xxx], matched codes is empty, may be session not init", e.getMessage());

        e = assertThrows(SessionException.class, () -> session.setScopedMatchedCode(null,null, null));
        assertEquals("scope should not be null", e.getMessage());

        e = assertThrows(SessionException.class, () -> session.setScopedMatchedCode("xxx",null, null));
        assertEquals("code should not be null", e.getMessage());


        e = assertThrows(SessionException.class, () -> session.setScopedMatchedCode("xxx","zzz", null));
        assertEquals("priority should not be null", e.getMessage());


        session.setScopedMatchedCode("xxx","a", 0);
        e = assertThrows(SessionException.class, () -> session.setScopedMatchedCode("xxx","a", 1));
        assertEquals("scope [xxx], code [a] already exist", e.getMessage());

        session.setScopedMatchedCode("xxx","b", 1);
        e = assertThrows(SessionException.class, () -> session.setScopedMatchedCode("xxx","c", 1));
        assertEquals("scope [xxx], priority [1] already exist", e.getMessage());

        session.setScopedMatchedCode("xxx","d", 100);
        session.setScopedMatchedCode("xxx","e", 10);
        List<String> codes = session.getScopedMatchedCodes("xxx");
        assertEquals(4, codes.size());
        assertEquals("a", codes.get(0));
        assertEquals("b", codes.get(1));
        assertEquals("e", codes.get(2));
        assertEquals("d", codes.get(3));

        session.setScopedMatchedCode("yyy","b", 1);
        session.getScopedMatchedCodes("yyy");

        session.removeScopedSession("xxx");
        e = assertThrows(SessionException.class, () -> session.getScopedMatchedCodes("xxx"));
        assertEquals("scope [xxx], matched codes is empty, may be session not init", e.getMessage());

        session.removeAllSession();

        e = assertThrows(SessionException.class, () -> session.getScopedMatchedCodes("yyy"));
        assertEquals("scope [yyy], matched codes is empty, may be session not init", e.getMessage());
    }
}
