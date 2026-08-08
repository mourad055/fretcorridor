package com.fretcorridor.opt.tarification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComposantCoutServiceRepository extends JpaRepository<ComposantCoutService, UUID> {

    List<ComposantCoutService> findByBaremeId(UUID baremeId);
}
