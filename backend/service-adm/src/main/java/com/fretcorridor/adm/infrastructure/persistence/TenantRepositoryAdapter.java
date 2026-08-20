package com.fretcorridor.adm.infrastructure.persistence;

import com.fretcorridor.adm.domain.Tenant;
import com.fretcorridor.adm.domain.TenantPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TenantRepositoryAdapter implements TenantPort {

    private final TenantJpaRepository repository;

    public TenantRepositoryAdapter(TenantJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void sauvegarder(Tenant tenant) {
        repository.save(new TenantEntity(tenant.id(), tenant.nom(), tenant.pays()));
    }

    @Override
    public Optional<Tenant> parId(String id) {
        return repository.findById(id).map(e -> new Tenant(e.getId(), e.getNom(), e.getPays()));
    }

    @Override
    public List<Tenant> lister() {
        return repository.findAll().stream().map(e -> new Tenant(e.getId(), e.getNom(), e.getPays())).toList();
    }
}
