package com.sadie.sadie_webhook_backend.repository;

import com.sadie.sadie_webhook_backend.entity.CallSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CallSessionRepository extends JpaRepository<CallSession, UUID> {

    boolean existsByCallId(String callId);

    Optional<CallSession> findByCallId(String callId);
}
