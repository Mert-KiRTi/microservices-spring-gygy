package com.turkcell.order_service.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(36)")
    private String eventId; // UUID
    
    @Column(nullable = false, length = 100)
    private String eventType;
    
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload; // JSON
    
    @Column(nullable = false)
    private Boolean processed = false;
    
    public OutboxMessage() {
    }
    
    public OutboxMessage(String eventId, String eventType, String payload, Boolean processed) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.payload = payload;
        this.processed = processed;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getPayload() {
        return payload;
    }
    
    public void setPayload(String payload) {
        this.payload = payload;
    }
    
    public Boolean getProcessed() {
        return processed;
    }
    
    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }
}
