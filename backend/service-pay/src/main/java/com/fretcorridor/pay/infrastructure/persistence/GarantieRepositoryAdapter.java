package com.fretcorridor.pay.infrastructure.persistence;

import com.fretcorridor.pay.domain.Garantie;
import com.fretcorridor.pay.domain.GarantiePort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GarantieRepositoryAdapter implements GarantiePort {

    private final GarantieJpaRepository jpaRepository;

    public GarantieRepositoryAdapter(GarantieJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Garantie> parMission(String missionId) {
        return jpaRepository.findByMissionId(missionId).map(GarantieEntity::toDomain);
    }

    @Override
    public void enregistrer(Garantie garantie) {
        jpaRepository.save(GarantieEntity.from(garantie));
    }
}
