package com.fretcorridor.adm.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryDossierPort implements DossierPort {

    private final Map<String, Dossier> dossiers = new LinkedHashMap<>();

    @Override
    public void sauvegarder(Dossier dossier) {
        dossiers.put(dossier.id(), dossier);
    }

    @Override
    public Optional<Dossier> parId(String id) {
        return Optional.ofNullable(dossiers.get(id));
    }

    @Override
    public List<Dossier> lister(String tenantId) {
        return dossiers.values().stream().filter(d -> d.tenantId().equals(tenantId)).toList();
    }

    @Override
    public List<Dossier> listerTous() {
        return List.copyOf(dossiers.values());
    }
}
