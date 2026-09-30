package com.example.banking.dto.request;

import com.example.banking.entity.ReversalReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReversalRequest {
    @NotNull private ReversalReason reason;

    @Size(max = 255)
    private String notes;
}