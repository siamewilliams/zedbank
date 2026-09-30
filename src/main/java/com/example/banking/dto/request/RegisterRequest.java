package com.example.banking.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank @Size(min = 3, max = 50)
    private String username;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 8, max = 100)
    private String password;

    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @NotBlank private String phone;

    @NotNull @Past
    private LocalDate dateOfBirth;

    @NotBlank private String nationalId;
    @NotBlank private String address;
}