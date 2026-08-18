package com.fretcorridor.adm.domain;

/** UC-ADM-01 A2 : un recours conteste une décision rendue — pas de recours sur un dossier pas encore tranché. */
public class DossierNonTrancheException extends RuntimeException {

    public DossierNonTrancheException(String dossierId) {
        super("Le dossier " + dossierId + " n'est pas encore tranché, aucun recours n'est possible");
    }
}
