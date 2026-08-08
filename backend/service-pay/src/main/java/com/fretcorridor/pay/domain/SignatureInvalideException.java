package com.fretcorridor.pay.domain;

/** EF-PAY-05 : la signature de la notification entrante ne correspond pas au corps reçu. */
public class SignatureInvalideException extends RuntimeException {

    public SignatureInvalideException() {
        super("Signature de notification invalide — notification rejetée sans traitement");
    }
}
