package com.sigsu.backend.common.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.Set;

@Component
@Profile("observability")
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final Set<String> PUBLIC_PATHS = Set.of("/actuator/health", "/actuator/prometheus",
            "/api/auth/login", "/api/auth/logout", "/v3/api-docs");
    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");
    private final Tracer tracer;

    public RequestLoggingFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    public static String safePath(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        // NoResourceFound puede usar /**; nunca sustituirlo por la URL original.
        if (pattern != null) return pattern.toString();
        return PUBLIC_PATHS.contains(request.getRequestURI()) ? request.getRequestURI() : "/unmapped";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        Span span = tracer.currentSpan();
        if (span != null) response.setHeader("X-Trace-Id", span.context().traceId());
        try {
            chain.doFilter(request, response);
        } finally {
            // El MDC de traceId/spanId pertenece a Micrometer; no se agrega ni se limpia MDC propio.
            LOG.atInfo().addKeyValue("event", "http.request")
                    .addKeyValue("method", METHODS.contains(request.getMethod()) ? request.getMethod() : "OTHER")
                    .addKeyValue("path", safePath(request))
                    .addKeyValue("status", response.getStatus())
                    .addKeyValue("durationMs", (System.nanoTime() - start) / 1_000_000.0)
                    .log("HTTP request completed");
        }
    }
}
