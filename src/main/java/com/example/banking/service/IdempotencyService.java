package com.example.banking.service;

import com.example.banking.entity.IdempotencyKey;
import com.example.banking.exception.BusinessException;
import com.example.banking.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;

    @Value("${banking.idempotency.ttl-hours:24}")
    private long ttlHours;

    public record CachedResponse(String body, int status) {}

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<CachedResponse> lookup(String key, String requestHash) {
        if (key == null || key.isBlank()) return Optional.empty();
        return repository.findById(key).map(existing -> {
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new BusinessException("Idempotency key reused with different payload");
            }
            return new CachedResponse(existing.getResponseBody(), existing.getResponseStatus());
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void store(String key, String requestHash, String responseBody, int status) {
        if (key == null || key.isBlank()) return;
        repository.save(IdempotencyKey.builder()
            .key(key)
            .requestHash(requestHash)
            .responseBody(responseBody)
            .responseStatus(status)
            .expiresAt(LocalDateTime.now().plusHours(ttlHours))
            .build());
    }

    public String hash(Object payload) {
        try {
            String json = payload == null ? "" : payload.toString();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(json.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Hashing failed", e);
        }
    }

    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void cleanupExpired() {
        repository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}