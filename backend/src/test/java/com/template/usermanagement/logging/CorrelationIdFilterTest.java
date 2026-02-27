package com.template.usermanagement.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @InjectMocks
    private CorrelationIdFilter filter;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("When no correlation ID header is present, a UUID is generated")
    void doFilter_noHeader_shouldGenerateUuid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcValue = new AtomicReference<>();
        doAnswer(invocation -> {
            mdcValue.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        // Verify a UUID was generated and set on the response
        String responseHeader = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(responseHeader);
        assertDoesNotThrow(() -> UUID.fromString(responseHeader));

        // Verify MDC was set during chain execution
        assertNotNull(mdcValue.get());
        assertEquals(responseHeader, mdcValue.get());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("When correlation ID header is present, it is reused")
    void doFilter_existingHeader_shouldReuseValue() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "existing-correlation-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcValue = new AtomicReference<>();
        doAnswer(invocation -> {
            mdcValue.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("existing-correlation-id", response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
        assertEquals("existing-correlation-id", mdcValue.get());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("When blank correlation ID header is present, a new UUID is generated")
    void doFilter_blankHeader_shouldGenerateNewUuid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        String responseHeader = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertNotNull(responseHeader);
        assertDoesNotThrow(() -> UUID.fromString(responseHeader));
    }

    @Test
    @DisplayName("MDC is cleaned up after filter chain completes normally")
    void doFilter_normalExecution_shouldCleanMdc() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
    }

    @Test
    @DisplayName("MDC is cleaned up even when filter chain throws exception")
    void doFilter_chainThrowsException_shouldStillCleanMdc() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        doThrow(new ServletException("test error")).when(filterChain).doFilter(any(), any());

        assertThrows(ServletException.class, () -> filter.doFilterInternal(request, response, filterChain));

        assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
    }

    @Test
    @DisplayName("Response header is set with the correlation ID")
    void doFilter_shouldSetResponseHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "my-trace-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("my-trace-id", response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }
}
