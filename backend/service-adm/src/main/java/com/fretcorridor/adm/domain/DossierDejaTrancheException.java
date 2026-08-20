package com.fretcorridor.adm.domain;

/** RG-097 (CDC) : une décision doit être motivée par un état cible valide, jamais rouvrir un dossier déjà tranché. */
public class DossierDejaTrancheException extends RuntimeException {

    public DossierDejaTrancheException(String dossierId) {
        super("Le dossier " + dossierId + " est déjà clos, sa décision ne peut pas être rouverte");
    }
}
