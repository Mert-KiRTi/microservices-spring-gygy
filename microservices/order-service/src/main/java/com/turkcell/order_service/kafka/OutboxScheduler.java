package com.turkcell.order_service.kafka;

import com.turkcell.order_service.entity.OutboxMessage;
import com.turkcell.order_service.repository.OutboxMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@EnableScheduling
public class OutboxScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(OutboxScheduler.class);
    private static final String TOPIC_NAME = "order-events";
    
    private final OutboxMessageRepository outboxMessageRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public OutboxScheduler(OutboxMessageRepository outboxMessageRepository,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxMessageRepository = outboxMessageRepository;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @Scheduled(fixedRate = 5000)
    public void publishOutboxEvents() {
        logger.info("OutboxScheduler başladı - processed=false olan mesajlar aranıyor...");
        
        List<OutboxMessage> unprocessedMessages = outboxMessageRepository.findAllByProcessedFalse();
        
        if (unprocessedMessages.isEmpty()) {
            logger.info("İşlenecek OutboxMessage bulunamadı");
            return;
        }
        
        logger.info("{} adet işlenmemiş mesaj bulundu", unprocessedMessages.size());
        
        for (OutboxMessage message : unprocessedMessages) {
            try {
                // Kafka'ya mesajı gönder
                kafkaTemplate.send(TOPIC_NAME, message.getEventId(), message.getPayload());
                logger.info("Mesaj Kafka'ya gönderildi. EventId: {}, EventType: {}", 
                           message.getEventId(), message.getEventType());
                
                // Veritabanında processed=true olarak güncelle
                message.setProcessed(true);
                outboxMessageRepository.save(message);
                logger.info("Mesaj başarıyla işlenmiş olarak işaretlendi. EventId: {}", message.getEventId());
                
            } catch (Exception e) {
                logger.error("Mesaj gönderilirken hata oluştu. EventId: {}, Error: {}", 
                            message.getEventId(), e.getMessage(), e);
            }
        }
        
        logger.info("OutboxScheduler tamamlandı");
    }
}
