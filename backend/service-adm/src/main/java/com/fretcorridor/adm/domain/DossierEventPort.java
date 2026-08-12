package com.fretcorridor.adm.domain;

/**
 * EF-PAY-08 : notifie toute transition d'un dossier — l'adaptateur infra
 * décide seul ce qui mérite d'être publié (aujourd'hui : uniquement les
 * dossiers {@link TypeDossier#LITIGE}, pour que service-pay suspende le
 * reversement automatique en cas de contestation ouverte). Le domaine ne
 * connaît ni Kafka ni le consommateur.
 */
public interface DossierEventPort {

    void publier(Dossier dossier);
}
