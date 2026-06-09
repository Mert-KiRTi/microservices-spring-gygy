package com.turkcell.order_service.repository;

import com.turkcell.order_service.entity.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
    List<OutboxMessage> findAllByProcessedFalse();
}
