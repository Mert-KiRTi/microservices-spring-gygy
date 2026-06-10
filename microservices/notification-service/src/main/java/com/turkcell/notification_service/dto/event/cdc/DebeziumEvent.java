package com.turkcell.notification_service.dto.event.cdc;

/**
 * Generic Debezium Event DTO
 * Debezium tarafından Kafka'ya fırlatılan ana event yapısını karşılar.
 * 
 * @param <T> Tablodaki veri türü
 */
public class DebeziumEvent<T> {
    
    private DebeziumPayload<T> payload;
    
    public DebeziumEvent() {
    }
    
    public DebeziumEvent(DebeziumPayload<T> payload) {
        this.payload = payload;
    }
    
    public DebeziumPayload<T> getPayload() {
        return payload;
    }
    
    public void setPayload(DebeziumPayload<T> payload) {
        this.payload = payload;
    }
    
    @Override
    public String toString() {
        return "DebeziumEvent{" +
                "payload=" + payload +
                '}';
    }
}
