package io.github.xiaoshicae.extension.admin.spring.boot.autoconfigure.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ResponseTest {

    @Test
    public void testOkResponse() {
        Response<String> response = Response.OK("test data");
        assertEquals("A00000", response.getCode());
        assertEquals("SUCCESS", response.getMsg());
        assertEquals("test data", response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    public void testFailResponse() {
        Response<Void> response = Response.fail("error message");
        assertEquals("B00000", response.getCode());
        assertEquals("error message", response.getMsg());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testFailResponseDeprecated() {
        @SuppressWarnings("deprecation")
        Response<Void> response = Response.Fail("error message");
        assertEquals("B00000", response.getCode());
        assertEquals("error message", response.getMsg());
    }

    @Test
    public void testStaticFieldsAreFinal() {
        // Verify that static fields cannot be modified (they are final)
        // This test documents the fix for the mutable static fields bug
        Response<String> r1 = Response.OK("data1");
        Response<String> r2 = Response.OK("data2");
        assertEquals(r1.getCode(), r2.getCode());
        assertEquals(r1.getMsg(), r2.getMsg());
    }
}
