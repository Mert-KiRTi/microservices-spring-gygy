package com.turkcell.notification_service.kafka;

import com.turkcell.notification_service.dto.event.cdc.DebeziumEvent;
import com.turkcell.notification_service.dto.event.cdc.ProductCdcEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * CDC Consumer Konfigürasyonu
 * 
 * Spring Cloud Stream aracılığıyla Debezium tarafından üretilen
 * CDC olaylarını Kafka'dan tüketir ve işler.
 */
@Configuration
public class CdcConsumerConfig {
    
    /**
     * Product CDC Event Consumer
     * 
     * Debezium'dan gelen product-service.public.products konusundaki
     * CDC olaylarını işler.
     * 
     * Fonksiyon adı: processProductCdc
     * Spring Cloud Stream binding adıyla birebir eşleşmesi gerekir:
     * spring.cloud.stream.bindings.processProductCdc-in-0.destination
     * 
     * @return Consumer işleyen function
     */
    @Bean
    public Consumer<DebeziumEvent<ProductCdcEvent>> processProductCdc() {
        return event -> {
            try {
                if (event == null || event.getPayload() == null) {
                    System.err.println("[CDC FLOW] Geçersiz event alındı!");
                    return;
                }
                
                String operation = event.getPayload().getOp();
                ProductCdcEvent afterData = event.getPayload().getAfter();
                
                // Create operasyonu
                if ("c".equals(operation)) {
                    if (afterData != null) {
                        System.out.println("[CDC FLOW] Yeni Ürün Eklendi! Ürün Adı: " + afterData.getName());
                    }
                }
                // Update operasyonu
                else if ("u".equals(operation)) {
                    if (afterData != null) {
                        System.out.println("[CDC FLOW] Ürün Güncellendi! Yeni Stok: " + afterData.getStock());
                    }
                }
                // Delete operasyonu
                else if ("d".equals(operation)) {
                    ProductCdcEvent beforeData = event.getPayload().getBefore();
                    if (beforeData != null) {
                        System.out.println("[CDC FLOW] Ürün Silindi! Ürün ID: " + beforeData.getId());
                    }
                }
                // Bilinmeyen operasyon
                else {
                    System.out.println("[CDC FLOW] Bilinmeyen operasyon alındı: " + operation);
                }
                
            } catch (Exception exception) {
                System.err.println("[CDC FLOW] Error occurred while processing product CDC event: " + exception.getMessage());
                exception.printStackTrace();
            }
        };
    }
}
