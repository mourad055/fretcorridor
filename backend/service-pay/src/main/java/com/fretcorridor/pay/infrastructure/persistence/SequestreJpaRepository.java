package com.fretcorridor.pay.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SequestreJpaRepository extends JpaRepository<SequestreEntity, String> {
}
