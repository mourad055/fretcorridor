package com.fretcorridor.adm.infrastructure.persistence;

import com.fretcorridor.adm.domain.EntreeJournalAudit;
import com.fretcorridor.adm.domain.JournalAuditPort;
import org.springframework.stereotype.Component;

import java.util.List;

/** ENF-SEC-02 : aucune méthode de suppression ou de mise à jour n'est exposée par construction. */
@Component
public class JournalAuditRepositoryAdapter implements JournalAuditPort {

    private final JournalAuditJpaRepository repository;

    public JournalAuditRepositoryAdapter(JournalAuditJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void enregistrer(EntreeJournalAudit entree) {
        repository.save(new JournalAuditEntity(entree.id(), entree.tenantId(), entree.acteurId(), entree.action(),
                entree.ressource(), entree.horodatage()));
    }

    @Override
    public List<EntreeJournalAudit> lister(String tenantId) {
        List<JournalAuditEntity> entities = tenantId == null
                ? repository.findAllByOrderByHorodatageDesc()
                : repository.findByTenantIdOrderByHorodatageDesc(tenantId);
        return entities.stream()
                .map(e -> new EntreeJournalAudit(e.getId(), e.getTenantId(), e.getActeurId(), e.getAction(),
                        e.getRessource(), e.getHorodatage()))
                .toList();
    }
}
