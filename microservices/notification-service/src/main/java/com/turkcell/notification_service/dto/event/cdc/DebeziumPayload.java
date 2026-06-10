package com.turkcell.notification_service.dto.event.cdc;

/**
 * Generic Debezium CDC Payload DTO
 * Debezium tarafından üretilen ham CDC JSON yapısını karşılar.
 * 
 * @param <T> Tablodaki veri türü
 */
public class DebeziumPayload<T> {
    
    private T before;
    private T after;
    private String op;  // c: create, u: update, d: delete
    
    public DebeziumPayload() {
    }
    
    public DebeziumPayload(T before, T after, String op) {
        this.before = before;
        this.after = after;
        this.op = op;
    }
    
    public T getBefore() {
        return before;
    }
    
    public void setBefore(T before) {
        this.before = before;
    }
    
    public T getAfter() {
        return after;
    }
    
    public void setAfter(T after) {
        this.after = after;
    }
    
    public String getOp() {
        return op;
    }
    
    public void setOp(String op) {
        this.op = op;
    }
    
    @Override
    public String toString() {
        return "DebeziumPayload{" +
                "before=" + before +
                ", after=" + after +
                ", op='" + op + '\'' +
                '}';
    }
}
