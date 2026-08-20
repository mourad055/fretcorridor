package com.flysoft.fretcorridor.flt.service;

public class ImmatriculationDejaUtiliseeException extends RuntimeException {
    public ImmatriculationDejaUtiliseeException(String immatriculation) {
        super("Immatriculation déjà utilisée : " + immatriculation);
    }
}
