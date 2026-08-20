package com.fretcorridor.adm.domain;

public class DossierIntrouvableException extends RuntimeException {

    public DossierIntrouvableException(String dossierId) {
        super("Aucun dossier avec l'identifiant " + dossierId);
    }
}
