package com.fretcorridor.pay.infrastructure.persistence;

import com.fretcorridor.pay.domain.EcritureMiroir;
import com.fretcorridor.pay.domain.GrandLivrePort;
import com.fretcorridor.pay.domain.StatutEcriture;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GrandLivreRepositoryAdapter implements GrandLivrePort {

    private final EcritureMiroirJpaRepository jpaRepository;

    public GrandLivreRepositoryAdapter(EcritureMiroirJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void enregistrer(EcritureMiroir ecriture) {
        jpaRepository.save(EcritureMiroirEntity.from(ecriture));
    }

    @Override
    public List<EcritureMiroir> parMission(String missionId) {
        return jpaRepository.findByMissionId(missionId).stream().map(EcritureMiroirEntity::toDomain).toList();
    }

    @Override
    public List<EcritureMiroir> parBeneficiaire(String beneficiaireId) {
        return jpaRepository.findByBeneficiaireId(beneficiaireId).stream().map(EcritureMiroirEntity::toDomain).toList();
    }

    @Override
    public List<EcritureMiroir> parTenant(String tenantId) {
        return jpaRepository.findByTenantId(tenantId).stream().map(EcritureMiroirEntity::toDomain).toList();
    }

    @Override
    public void suspendre(String ecritureId) {
        jpaRepository.findById(ecritureId).ifPresent(entity -> {
            entity.setStatut(StatutEcriture.SUSPENDUE);
            jpaRepository.save(entity);
        });
    }

    @Override
    public List<String> listerMissionsAvecEcritures() {
        return jpaRepository.findDistinctMissionIds();
    }
}
