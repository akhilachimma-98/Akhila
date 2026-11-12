package com.ledgerline.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ledgerline.dto.TransactionResponse;
import com.ledgerline.entity.IdempotencyRecord;
import com.ledgerline.entity.User;
import com.ledgerline.enums.IdempotencyStatus;
import com.ledgerline.exception.BadRequestException;
import com.ledgerline.repository.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service managing request idempotency to strictly eliminate duplicate transactions.
 */
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository idempotencyRecordRepository) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Checks if a transaction with this idempotency key has already completed.
     * Returns cached response if completed, or throws exception if currently in-progress.
     */
    @Transactional(readOnly = true)
    public Optional<TransactionResponse> findExistingResponse(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        Optional<IdempotencyRecord> recordOpt = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
        if (recordOpt.isEmpty()) {
            return Optional.empty();
        }

        IdempotencyRecord record = recordOpt.get();

        if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            throw new BadRequestException("A transaction with idempotency key '" + idempotencyKey + "' is already in progress. Please wait.");
        }

        if (record.getStatus() == IdempotencyStatus.COMPLETED && record.getResponseBody() != null) {
            try {
                TransactionResponse cached = objectMapper.readValue(record.getResponseBody(), TransactionResponse.class);
                return Optional.of(cached);
            } catch (JsonProcessingException e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * Starts tracking an in-flight idempotent request in a separate transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startTracking(String idempotencyKey, User user, String path, Object payload) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        String requestHash = computeHash(payload);
        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey,
                user,
                path,
                requestHash,
                LocalDateTime.now().plusHours(24)
        );
        record.setStatus(IdempotencyStatus.IN_PROGRESS);
        idempotencyRecordRepository.save(record);
    }

    /**
     * Finalizes tracking with the serialized response upon successful execution.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String idempotencyKey, TransactionResponse response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey).ifPresent(record -> {
            try {
                record.setStatus(IdempotencyStatus.COMPLETED);
                record.setResponseStatus(200);
                record.setResponseBody(objectMapper.writeValueAsString(response));
                idempotencyRecordRepository.save(record);
            } catch (JsonProcessingException ignored) {
            }
        });
    }

    /**
     * Marks record as failed if transaction encountered an unrecoverable business failure.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String idempotencyKey, String failureMessage) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey).ifPresent(record -> {
            record.setStatus(IdempotencyStatus.FAILED);
            record.setResponseBody(failureMessage);
            idempotencyRecordRepository.save(record);
        });
    }

    private String computeHash(Object payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(payload.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "HASH_FALLBACK_" + System.currentTimeMillis();
        }
    }
}
