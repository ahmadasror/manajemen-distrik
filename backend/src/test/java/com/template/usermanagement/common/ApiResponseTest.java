package com.template.usermanagement.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    @DisplayName("success(data) sets success=true and data, message and errorCode are null")
    void successWithData_shouldSetSuccessAndData() {
        String data = "test data";

        ApiResponse<String> response = ApiResponse.success(data);

        assertTrue(response.isSuccess());
        assertEquals("test data", response.getData());
        assertNull(response.getMessage());
        assertNull(response.getErrorCode());
    }

    @Test
    @DisplayName("success(data) works with complex objects")
    void successWithData_complexObject_shouldWork() {
        var data = java.util.Map.of("key", "value");

        ApiResponse<java.util.Map<String, String>> response = ApiResponse.success(data);

        assertTrue(response.isSuccess());
        assertEquals(data, response.getData());
    }

    @Test
    @DisplayName("success(data) works with null data")
    void successWithNullData_shouldSetSuccessTrue() {
        ApiResponse<Object> response = ApiResponse.success(null);

        assertTrue(response.isSuccess());
        assertNull(response.getData());
    }

    @Test
    @DisplayName("success(message, data) sets success=true, message, and data")
    void successWithMessageAndData_shouldSetAllFields() {
        String message = "Operation completed";
        Integer data = 42;

        ApiResponse<Integer> response = ApiResponse.success(message, data);

        assertTrue(response.isSuccess());
        assertEquals("Operation completed", response.getMessage());
        assertEquals(42, response.getData());
        assertNull(response.getErrorCode());
    }

    @Test
    @DisplayName("success(message, data) with null message still sets success=true")
    void successWithNullMessage_shouldStillBeSuccess() {
        ApiResponse<String> response = ApiResponse.success(null, "data");

        assertTrue(response.isSuccess());
        assertNull(response.getMessage());
        assertEquals("data", response.getData());
    }

    @Test
    @DisplayName("error(message, errorCode) sets success=false, message, and errorCode")
    void error_shouldSetErrorFields() {
        ApiResponse<Object> response = ApiResponse.error("Something went wrong", "ERR_001");

        assertFalse(response.isSuccess());
        assertEquals("Something went wrong", response.getMessage());
        assertEquals("ERR_001", response.getErrorCode());
        assertNull(response.getData());
    }

    @Test
    @DisplayName("error sets data to null")
    void error_dataShouldBeNull() {
        ApiResponse<String> response = ApiResponse.error("error msg", "CODE");

        assertNull(response.getData());
    }

    @Test
    @DisplayName("error with different error codes")
    void error_differentErrorCodes_shouldWork() {
        ApiResponse<Object> response1 = ApiResponse.error("Not found", "NOT_FOUND");
        ApiResponse<Object> response2 = ApiResponse.error("Unauthorized", "UNAUTHORIZED");

        assertEquals("NOT_FOUND", response1.getErrorCode());
        assertEquals("UNAUTHORIZED", response2.getErrorCode());
        assertFalse(response1.isSuccess());
        assertFalse(response2.isSuccess());
    }
}
