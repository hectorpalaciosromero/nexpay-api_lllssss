package com.nexpay.wallet.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new digital wallet")
public class CreateWalletRequest {

    @NotBlank(message = "Owner email is required")
    @Email(message = "Must be a valid email address")
    @Schema(description = "Owner's email address", example = "john.doe@nexpay.com")
    private String ownerEmail;

    @NotBlank(message = "Owner name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(description = "Owner's full name", example = "John Doe")
    private String ownerName;

    @Schema(description = "Wallet currency (ISO 4217)", example = "USD", defaultValue = "USD")
    private String currency;
}
