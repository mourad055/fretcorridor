package com.fretcorridor.bur.domain;

import java.util.List;

public interface AlerteSeuilPort {

    void sauvegarder(AlerteSeuil alerte);

    List<AlerteSeuil> listerParTenant(String tenantId);

    /** Silencieux si l'alerte n'existe pas ou n'appartient pas à ce tenant (idempotent, pas de fuite d'existence). */
    void supprimer(String id, String tenantId);
}
