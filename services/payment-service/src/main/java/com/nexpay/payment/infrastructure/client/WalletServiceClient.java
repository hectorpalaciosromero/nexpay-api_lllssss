package com.nexpay.payment.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * REST client for communicating with wallet-service.
 *
 * <p>Uses RestTemplate with explicit connection and read timeouts to prevent
 * thread starvation if wallet-service is slow or unresponsive. The circuit
 * breaker in {@code PaymentServiceImpl} provides an additional safety net.
 */
@Slf4j
@Component
public class WalletServiceClient {

    private final RestTemplate restTemplate;
    private final String walletServiceUrl;

    public WalletServiceClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${wallet-service.url:http://localhost:8081}") String walletServiceUrl) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
        this.walletServiceUrl = walletServiceUrl;
    }

    public void debitWallet(UUID walletId, BigDecimal amount, String description, String referenceId) {
        log.info("Debiting wallet {} with amount {}", walletId, amount);
        String url = walletServiceUrl + "/api/v1/wallets/" + walletId + "/debit";
        executeWalletOperation(url, amount, description, referenceId);
    }

    public void creditWallet(UUID walletId, BigDecimal amount, String description, String referenceId) {
        log.info("Crediting wallet {} with amount {}", walletId, amount);
        String url = walletServiceUrl + "/api/v1/wallets/" + walletId + "/credit";
        executeWalletOperation(url, amount, description, referenceId);
    }

    private void executeWalletOperation(String url, BigDecimal amount, String description, String referenceId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "amount", amount,
                "description", description,
                "referenceId", referenceId
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, request, String.class);
    }
}
