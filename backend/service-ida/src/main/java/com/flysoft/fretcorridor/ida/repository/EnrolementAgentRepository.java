package com.flysoft.fretcorridor.ida.repository;

import com.flysoft.fretcorridor.ida.entity.EnrolementAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrolementAgentRepository extends JpaRepository<EnrolementAgent, UUID> {
    Optional<EnrolementAgent> findByIdempotencyKey(String idempotencyKey);
    List<EnrolementAgent> findByAgentId(UUID agentId);
}
