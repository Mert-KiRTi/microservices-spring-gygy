package com.turkcell.order_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turkcell.order_service.dto.OrderCreatedEvent;
import com.turkcell.order_service.dto.OrderRequest;
import com.turkcell.order_service.entity.Order;
import com.turkcell.order_service.entity.OutboxMessage;
import com.turkcell.order_service.repository.OrderRepository;
import com.turkcell.order_service.repository.OutboxMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;
    
    public OrderService(OrderRepository orderRepository, 
                       OutboxMessageRepository outboxMessageRepository,
                       ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxMessageRepository = outboxMessageRepository;
        this.objectMapper = objectMapper;
    }
    
    @Transactional
    public Order createOrder(OrderRequest request) {
        // 1. Order Entity'sini oluştur ve kaydet
        Order order = new Order(
            request.getProductId(),
            request.getQuantity(),
            request.getTotalAmount(),
            "CREATED"
        );
        Order savedOrder = orderRepository.save(order);
        
        // 2. OrderCreatedEvent nesnesi oluştur
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent event = new OrderCreatedEvent(
            eventId,
            savedOrder.getId(),
            savedOrder.getProductId(),
            savedOrder.getQuantity()
        );
        
        // 3. Event'i JSON String'e çevir ve OutboxMessage'a kaydet
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxMessage outboxMessage = new OutboxMessage(
                eventId,
                "OrderCreatedEvent",
                payload,
                false
            );
            outboxMessageRepository.save(outboxMessage);
        } catch (Exception e) {
            throw new RuntimeException("OutboxMessage oluşturulurken hata: " + e.getMessage(), e);
        }
        
        return savedOrder;
    }
}
