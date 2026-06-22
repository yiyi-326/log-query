package com.example.logquery.dto;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Nested
    class Success {

        @Test
        void shouldCreateSuccessWithData() {
            ApiResponse<String> resp = ApiResponse.success("hello");

            assertTrue(resp.isSuccess());
            assertEquals("ok", resp.getMessage());
            assertEquals("hello", resp.getData());
            assertNotNull(resp.getTimestamp());
        }

        @Test
        void shouldCreateSuccessWithMessageAndData() {
            ApiResponse<Integer> resp = ApiResponse.success("操作成功", 42);

            assertTrue(resp.isSuccess());
            assertEquals("操作成功", resp.getMessage());
            assertEquals(42, resp.getData());
        }

        @Test
        void shouldSupportNullData() {
            ApiResponse<Void> resp = ApiResponse.success(null);

            assertTrue(resp.isSuccess());
            assertNull(resp.getData());
        }
    }

    @Nested
    class Error {

        @Test
        void shouldCreateErrorResponse() {
            ApiResponse<Void> resp = ApiResponse.error("发生了错误");

            assertFalse(resp.isSuccess());
            assertEquals("发生了错误", resp.getMessage());
            assertNull(resp.getData());
        }
    }

    @Test
    void timestampShouldBeSetAtCreation() {
        long before = System.currentTimeMillis();
        ApiResponse<String> resp = ApiResponse.success("test");
        long after = System.currentTimeMillis();

        assertTrue(resp.getTimestamp() >= before);
        assertTrue(resp.getTimestamp() <= after + 10); // small tolerance
    }
}
