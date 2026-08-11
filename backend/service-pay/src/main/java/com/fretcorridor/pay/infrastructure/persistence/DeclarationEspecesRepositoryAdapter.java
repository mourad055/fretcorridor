package com.fretcorridor.pay.infrastructure.persistence;

import com.fretcorridor.pay.domain.DeclarationEspeces;
import com.fretcorridor.pay.domain.DeclarationEspecesPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DeclarationEspecesRepositoryAdapter implements DeclarationEspecesPort {

    private final DeclarationEspecesJpaRepository jpaRepository;

    public DeclarationEspecesRepositoryAdapter(DeclarationEspecesJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<DeclarationEspeces> parMission(String missionId) {
        return jpaRepository.findByMissionId(missionId).map(DeclarationEspecesEntity::toDomain);
    }

    @Override
    public List<DeclarationEspeces> parTenant(String tenantId) {
        return jpaRepository.findByTenantId(tenantId).stream().map(DeclarationEspecesEntity::toDomain).toList();
    }

    @Override
    public void enregistrer(DeclarationEspeces declaration) {
        jpaRepository.save(DeclarationEspecesEntity.from(declaration));
    }
}
