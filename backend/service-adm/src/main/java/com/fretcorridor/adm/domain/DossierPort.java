package com.fretcorridor.adm.domain;

import java.util.List;
import java.util.Optional;

public interface DossierPort {
    void sauvegarder(Dossier dossier);

    Optional<Dossier> parId(String id);

    List<Dossier> lister(String tenantId);

    List<Dossier> listerTous();
}
