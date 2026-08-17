package com.fretcorridor.adm.infrastructure.persistence;

import com.fretcorridor.adm.domain.Dossier;
import com.fretcorridor.adm.domain.DossierPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DossierRepositoryAdapter implements DossierPort {

    private final DossierJpaRepository repository;

    public DossierRepositoryAdapter(DossierJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void sauvegarder(Dossier dossier) {
        repository.save(versEntite(dossier));
    }

    @Override
    public Optional<Dossier> parId(String id) {
        return repository.findById(id).map(this::versDomaine);
    }

    @Override
    public List<Dossier> lister(String tenantId) {
        return repository.findByTenantId(tenantId).stream().map(this::versDomaine).toList();
    }

    @Override
    public List<Dossier> listerTous() {
        return repository.findAll().stream().map(this::versDomaine).toList();
    }

    private DossierEntity versEntite(Dossier dossier) {
        return new DossierEntity(dossier.id(), dossier.tenantId(), dossier.type(), dossier.priorite(),
                dossier.statut(), dossier.missionId(), dossier.parties(), dossier.preuvesReferences(),
                dossier.ouvertLe(), dossier.delaiTraitement(), dossier.priseEnChargeParActeurId(),
                dossier.decision(), dossier.motifDecision(), dossier.decidePar(), dossier.decideLe(),
                dossier.grilleVersionAppliquee(), dossier.recoursDeDossierId());
    }

    private Dossier versDomaine(DossierEntity entity) {
        return new Dossier(entity.getId(), entity.getTenantId(), entity.getType(), entity.getPriorite(),
                entity.getStatut(), entity.getMissionId(), entity.getParties(), entity.getPreuvesReferences(),
                entity.getOuvertLe(), entity.getDelaiTraitement(), entity.getPriseEnChargeParActeurId(),
                entity.getDecision(), entity.getMotifDecision(), entity.getDecidePar(), entity.getDecideLe(),
                entity.getGrilleVersionAppliquee(), entity.getRecoursDeDossierId());
    }
}
