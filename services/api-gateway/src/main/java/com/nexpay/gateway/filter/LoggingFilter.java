package com.nexpay.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global logging filter that adds correlation IDs to all requests.
 */
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Generate or extract correlation ID
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        log.info("[{}] {} {} -> routing to downstream service",
                correlationId,
                request.getMethod(),
                request.getURI().getPath());

        // Add correlation ID to downstream request
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        String finalCorrelationId = correlationId;
        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    log.info("[{}] Response status: {}",
                            finalCorrelationId,
                            exchange.getResponse().getStatusCode());
                }));
    }

    @Override
    public int getOrder() {
        return -1; // Execute first
    }
}
