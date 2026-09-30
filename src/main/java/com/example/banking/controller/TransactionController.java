package com.example.banking.controller;

import com.example.banking.dto.request.DepositRequest;
import com.example.banking.dto.request.ReversalRequest;
import com.example.banking.dto.request.TransferRequest;
import com.example.banking.dto.response.TransactionResponse;
import com.example.banking.service.IdempotencyService;
import com.example.banking.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER')")
    public ResponseEntity<?> deposit(@Valid @RequestBody DepositRequest req,
                                     @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
                                     @AuthenticationPrincipal UserDetails user,
                                     HttpServletRequest http) throws Exception {
        String hash = idempotencyService.hash(req);
        var cached = idempotencyService.lookup(idemKey, hash);
        if (cached.isPresent()) return ResponseEntity.status(cached.get().status()).body(cached.get().body());

        TransactionResponse res = transactionService.deposit(req, user.getUsername(), http.getRemoteAddr());
        String body = objectMapper.writeValueAsString(res);
        idempotencyService.store(idemKey, hash, body, 201);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER')")
    public ResponseEntity<?> transfer(@Valid @RequestBody TransferRequest req,
                                      @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
                                      @AuthenticationPrincipal UserDetails user,
                                      HttpServletRequest http) throws Exception {
        String hash = idempotencyService.hash(req);
        var cached = idempotencyService.lookup(idemKey, hash);
        if (cached.isPresent()) return ResponseEntity.status(cached.get().status()).body(cached.get().body());

        TransactionResponse res = transactionService.transfer(req, user.getUsername(), http.getRemoteAddr());
        String body = objectMapper.writeValueAsString(res);
        idempotencyService.store(idemKey, hash, body, 201);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PostMapping("/{reference}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionResponse> reverse(@PathVariable String reference,
                                                       @Valid @RequestBody ReversalRequest req,
                                                       @AuthenticationPrincipal UserDetails user,
                                                       HttpServletRequest http) {
        return ResponseEntity.ok(transactionService.reverse(reference, req,
            user.getUsername(), http.getRemoteAddr()));
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> history(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(transactionService.history(user.getUsername(),
            PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/{reference}")
    public ResponseEntity<TransactionResponse> getByReference(@PathVariable String reference) {
        return ResponseEntity.ok(transactionService.getByReference(reference));
    }
}