package com.fretcorridor.pay.infrastructure.persistence;

import com.fretcorridor.pay.domain.NotificationIdempotencePort;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class NotificationIdempotenceRepositoryAdapter implements NotificationIdempotencePort {

    private final NotificationTraiteeJpaRepository jpaRepository;

    public NotificationIdempotenceRepositoryAdapter(NotificationTraiteeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean dejaTraitee(String idempotenceKey) {
        return jpaRepository.existsById(idempotenceKey);
    }

    @Override
    public void marquerTraitee(String idempotenceKey) {
        jpaRepository.save(new NotificationTraiteeEntity(idempotenceKey, Instant.now()));
    }
}
