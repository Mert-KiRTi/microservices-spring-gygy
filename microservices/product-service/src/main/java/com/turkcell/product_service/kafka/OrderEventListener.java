package com.turkcell.product_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.product_service.dto.OrderCreatedEvent;
import com.turkcell.product_service.entity.ProcessedEvent;
import com.turkcell.product_service.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class OrderEventListener {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderEventListener.class);
    
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;
    
    public OrderEventListener(ProcessedEventRepository processedEventRepository,
                             ObjectMapper objectMapper) {
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }
    
    @KafkaListener(topics = "order-events", groupId = "product-group")
    public void consumeOrderCreatedEvent(String message) {
        logger.info("Kafka mesajı alındı: {}", message);
        
        try {
            // JSON mesajı OrderCreatedEvent'e çevir
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
            String eventId = event.getEventId();
            
            logger.info("OrderCreatedEvent deserialize edildi. EventId: {}", eventId);
            
            // Idempotency Kontrolü: Aynı eventId daha önce işlendimi?
            if (processedEventRepository.existsById(eventId)) {
                logger.warn("Event daha önce işlenmiş: {}", eventId);
                return;
            }
            
            // Event'i işlenmiş olarak kaydet
            ProcessedEvent processedEvent = new ProcessedEvent(eventId, LocalDate.now());
            processedEventRepository.save(processedEvent);
            logger.info("ProcessedEvent kaydedildi. EventId: {}", eventId);
            
            // Stok kontrol işlemi başlat
            logger.info("Sipariş alındı, stok kontrol ediliyor... Product ID: {}", event.getProductId());
            logger.info("Order ID: {}, Quantity: {}", event.getOrderId(), event.getQuantity());
            
        } catch (Exception e) {
            logger.error("Mesaj işlenirken hata oluştu: {}", e.getMessage(), e);
        }
    }
}
