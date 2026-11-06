package com.nexpay.wallet.integration;

import com.nexpay.wallet.WalletServiceApplication;
import com.nexpay.wallet.application.dto.CreateWalletRequest;
import com.nexpay.wallet.application.dto.CreditDebitRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Wallet Service Integration Tests.
 *
 * <p>Uses H2 in-memory database (PostgreSQL compatibility mode) so this suite
 * runs in any CI environment without Docker or a live PostgreSQL instance.
 * The schema is provisioned by {@code schema-test.sql} before the class runs
 * and the database is reset between test classes via {@link DirtiesContext}.
 */
@SpringBootTest(classes = WalletServiceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/schema-test.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Wallet Service Integration Tests")
class WalletIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String createdWalletId;

    @Test
    @Order(1)
    @DisplayName("POST /api/v1/wallets - Should create a new wallet")
    void shouldCreateWallet() throws Exception {
        CreateWalletRequest request = CreateWalletRequest.builder()
                .ownerName("John Doe")
                .ownerEmail("john.doe@nexpay.com")
                .currency("USD")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.ownerName").value("John Doe"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        createdWalletId = objectMapper.readTree(response).get("id").asText();
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/v1/wallets/{id} - Should retrieve wallet by ID")
    void shouldGetWalletById() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/" + createdWalletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdWalletId))
                .andExpect(jsonPath("$.ownerName").value("John Doe"));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/v1/wallets/{id}/credit - Should credit wallet")
    void shouldCreditWallet() throws Exception {
        CreditDebitRequest request = CreditDebitRequest.builder()
                .amount(new BigDecimal("500.00"))
                .description("Initial deposit")
                .build();

        mockMvc.perform(post("/api/v1/wallets/" + createdWalletId + "/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/v1/wallets/{id}/debit - Should debit wallet")
    void shouldDebitWallet() throws Exception {
        CreditDebitRequest request = CreditDebitRequest.builder()
                .amount(new BigDecimal("150.00"))
                .description("ATM withdrawal")
                .build();

        mockMvc.perform(post("/api/v1/wallets/" + createdWalletId + "/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(5)
    @DisplayName("POST /api/v1/wallets/{id}/debit - Should reject insufficient funds")
    void shouldRejectInsufficientFunds() throws Exception {
        CreditDebitRequest request = CreditDebitRequest.builder()
                .amount(new BigDecimal("99999.00"))
                .description("Overdraft attempt")
                .build();

        mockMvc.perform(post("/api/v1/wallets/" + createdWalletId + "/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/v1/wallets/{id}/movements - Should list movements with pagination")
    void shouldListMovements() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/" + createdWalletId + "/movements")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(7)
    @DisplayName("PATCH /api/v1/wallets/{id}/status - Should deactivate wallet")
    void shouldDeactivateWallet() throws Exception {
        mockMvc.perform(patch("/api/v1/wallets/" + createdWalletId + "/status")
                        .param("status", "INACTIVE"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/v1/wallets/nonexistent - Should return 404")
    void shouldReturn404ForNonExistent() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(9)
    @DisplayName("POST /api/v1/wallets - Should reject duplicate email")
    void shouldRejectDuplicateEmail() throws Exception {
        CreateWalletRequest request = CreateWalletRequest.builder()
                .ownerName("Jane Doe")
                .ownerEmail("john.doe@nexpay.com")
                .currency("USD")
                .build();

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
