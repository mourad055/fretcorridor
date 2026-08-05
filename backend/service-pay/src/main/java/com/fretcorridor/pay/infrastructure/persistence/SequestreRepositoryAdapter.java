package com.fretcorridor.pay.infrastructure.persistence;

import com.fretcorridor.pay.domain.Sequestre;
import com.fretcorridor.pay.domain.SequestrePort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SequestreRepositoryAdapter implements SequestrePort {

    private final SequestreJpaRepository jpaRepository;

    public SequestreRepositoryAdapter(SequestreJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Sequestre> parMission(String missionId) {
        return jpaRepository.findById(missionId).map(SequestreEntity::toDomain);
    }

    @Override
    public void sauvegarder(Sequestre sequestre) {
        jpaRepository.save(SequestreEntity.from(sequestre));
    }
}
