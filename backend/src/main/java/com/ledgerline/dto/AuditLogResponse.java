package com.ledgerline.dto;

import com.ledgerline.enums.AuditAction;
import java.time.LocalDateTime;

/**
 * Single immutable audit record returned to Auditor or Admin.
 */
public class AuditLogResponse {

    private Long id;
    private Long actorUserId;
    private String actorUsername;
    private String actorRole;
    private AuditAction action;
    private String entityName;
    private String entityId;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;

    public AuditLogResponse() {
    }

    public AuditLogResponse(Long id, Long actorUserId, String actorUsername, String actorRole,
                            AuditAction action, String entityName, String entityId,
                            String details, String ipAddress, LocalDateTime createdAt) {
        this.id = id;
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.action = action;
        this.entityName = entityName;
        this.entityId = entityId;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getActorRole() {
        return actorRole;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getDetails() {
        return details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
