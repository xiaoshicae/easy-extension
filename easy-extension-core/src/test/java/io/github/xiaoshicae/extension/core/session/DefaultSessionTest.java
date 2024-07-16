package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.exception.SessionException;

import java.util.List;
import java.util.concurrent.*;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultSessionTest {

    private final DefaultSession session = new DefaultSession();

    private final ExecutorService executor = Executors.newFixedThreadPool(3);

    @AfterEach
    public void init() {
        session.remove();
    }

    @Test
    public void testSessionInSingleThread() throws Exception {
        testSession();
    }

    @Test
    public void testSessionInMultiThread() throws Exception {
        for (int i = 0; i < 3; i++) {
            executor.execute(() -> {
                try {
                    testSession();
                } catch (SessionException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }


    private void testSession() throws SessionException {
        SessionException e;
        e = assertThrows(SessionException.class, session::getMatchedCodes);
        assertEquals("matched codes is empty, may be no code register", e.getMessage());

        session.setMatchedCode("a", 0);
        e = assertThrows(SessionException.class, () -> session.setMatchedCode("a", 1));
        assertEquals("code a already exist", e.getMessage());

        session.setMatchedCode("b", 1);
        e = assertThrows(SessionException.class, () -> session.setMatchedCode("c", 1));
        assertEquals("priority 1 already exist", e.getMessage());

        session.setMatchedCode("d", 100);
        session.setMatchedCode("e", 10);

        List<String> codes = session.getMatchedCodes();
        assertEquals(4, codes.size());
        assertEquals("a", codes.get(0));
        assertEquals("b", codes.get(1));
        assertEquals("e", codes.get(2));
        assertEquals("d", codes.get(3));
    }
}
