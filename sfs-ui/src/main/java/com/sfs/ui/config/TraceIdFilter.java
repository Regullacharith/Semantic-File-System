package com.sfs.ui.config;

import com.sfs.core.observe.ObservabilityRegistry;
import com.sfs.core.observe.StructuredEvent;
import com.sfs.core.observe.TraceId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

public final class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-SFS-Trace-Id";
    public static final String MDC_KEY = "traceId";

    private final ObservabilityRegistry registry;

    public TraceIdFilter(ObservabilityRegistry registry) {
        this.registry = registry;
    }

    public static String requestShape(HttpServletRequest request) {
        return request.getMethod() + " " + redactPath(request.getRequestURI());
    }

    public static String redactPath(String uri) {
        StringBuilder shape = new StringBuilder();
        for (String segment : uri.split("/")) {
            if (segment.isBlank()) {
                continue;
            }
            if (shape.length() > 0) {
                shape.append('/');
            }
            shape.append(isLiteralSegment(segment) ? segment : "{id}");
        }
        return "/" + shape;
    }

    private static boolean isLiteralSegment(String segment) {
        return SAFE_SEGMENTS.contains(segment)
                || segment.matches("[a-z][a-z-]+")
                || segment.matches("sfs-obj-[0-9]{4}-[0-9a-z]+")
                || segment.matches("job-[0-9]+")
                || segment.matches("sfs-ref-[0-9a-f]+");
    }

    private static final java.util.Set<String> SAFE_SEGMENTS = java.util.Set.of(
            "api", "v1", "files", "objects", "search", "jobs", "reconstructions",
            "evaluations", "version", "security", "settings", "error");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String incoming = request.getHeader(HEADER);
        TraceId traceId = TraceId.isValid(incoming)
                ? new TraceId(incoming)
                : TraceId.generate();
        response.setHeader(HEADER, traceId.value());
        MDC.put(MDC_KEY, traceId.value());
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.nanoTime() - start;
            int status = response.getStatus();
            registry.recordStage("http.request", duration);
            registry.increment(status < 400 ? "requests.served" : "requests.refused");
            registry.record(new StructuredEvent(
                    Instant.now(),
                    status >= 500 ? "ERROR" : status >= 400 ? "WARN" : "INFO",
                    "request.served",
                    traceId.value(),
                    null,
                    status < 400 ? "OK" : "FAILED",
                    duration,
                    requestShape(request)));
            MDC.remove(MDC_KEY);
        }
    }
}
