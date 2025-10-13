package com.gourav.LedgerLens.Domain.Dtos;

import jakarta.validation.constraints.Email;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message="name is mandatory")
    private String fullname;
    @NotBlank(message="email is not blank")
    @Email(message="email should be valid")
    private String email;
    @Size(min = 8)
    private String password;

}
