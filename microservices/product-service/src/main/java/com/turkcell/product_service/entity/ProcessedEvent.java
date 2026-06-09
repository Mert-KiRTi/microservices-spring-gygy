package com.turkcell.product_service.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    
    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private String eventId;
    
    @Column(nullable = false)
    private LocalDate processedAt;
    
    public ProcessedEvent() {
    }
    
    public ProcessedEvent(String eventId, LocalDate processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public LocalDate getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDate processedAt) {
        this.processedAt = processedAt;
    }
}
