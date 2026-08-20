package com.fretcorridor.gateway.domain;

/** Levée quand le couple (téléphone, code) ne correspond à aucun acteur connu. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Numéro de téléphone ou code invalide");
    }
}
