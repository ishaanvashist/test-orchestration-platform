package dev.ishaan.test_orchestration_platform;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_KEY = "correlationId";        // the label used to store/find the id in MDC

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = UUID.randomUUID().toString();                 // generate a fresh, genuinely unique id for this one request
        MDC.put(CORRELATION_ID_KEY, correlationId);                          // store it, so every log line during this request picks it up automatically

        try {
            filterChain.doFilter(request, response);                         // continue processing the request as normal
        } finally {
            MDC.clear();                                                      // always clean up afterward, so this id can't leak into the NEXT request
        }
    }
}