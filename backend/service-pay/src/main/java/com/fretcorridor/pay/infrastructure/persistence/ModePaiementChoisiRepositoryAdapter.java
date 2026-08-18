package com.fretcorridor.pay.infrastructure.persistence;

import com.fretcorridor.pay.domain.ModePaiementChoisi;
import com.fretcorridor.pay.domain.ModePaiementChoisiPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ModePaiementChoisiRepositoryAdapter implements ModePaiementChoisiPort {

    private final ModePaiementChoisiJpaRepository jpaRepository;

    public ModePaiementChoisiRepositoryAdapter(ModePaiementChoisiJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ModePaiementChoisi> parMission(String missionId) {
        return jpaRepository.findById(missionId).map(ModePaiementChoisiEntity::toDomain);
    }

    @Override
    public void enregistrer(ModePaiementChoisi choix) {
        jpaRepository.save(ModePaiementChoisiEntity.from(choix));
    }
}
