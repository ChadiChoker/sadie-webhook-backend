package com.sadie.sadie_webhook_backend.repository;

import com.sadie.sadie_webhook_backend.entity.CallSession;
import com.sadie.sadie_webhook_backend.entity.ToolCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ToolCallLogRepository extends JpaRepository<ToolCallLog, UUID> {

    List<ToolCallLog> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    @Modifying
    @Query("UPDATE ToolCallLog t SET t.session = :session WHERE t.callId = :callId AND t.session IS NULL")
    int linkToSession(@Param("session") CallSession session, @Param("callId") String callId);
}
