package com.sadie.sadie_webhook_backend.repository;

import com.sadie.sadie_webhook_backend.entity.CallMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CallMessageRepository extends JpaRepository<CallMessage, UUID> {

    List<CallMessage> findBySessionIdOrderBySecondsFromStartAsc(UUID sessionId);
}
