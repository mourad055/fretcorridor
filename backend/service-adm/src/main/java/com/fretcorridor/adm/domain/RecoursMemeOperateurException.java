package com.fretcorridor.adm.domain;

/** RG-098 (CDC) : l'opérateur ayant décidé en premier ressort n'instruit pas le recours. */
public class RecoursMemeOperateurException extends RuntimeException {

    public RecoursMemeOperateurException(String dossierId, String acteurId) {
        super("L'acteur " + acteurId + " a déjà décidé en premier ressort du dossier dont "
                + dossierId + " est le recours — il ne peut pas l'instruire");
    }
}
