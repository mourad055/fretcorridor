package com.fretcorridor.pay.domain;

/** FE-PAY-02 : le séquestre se déclenche à la prise en charge et se libère à la clôture — jamais l'inverse. */
public class SequestreInvalideException extends RuntimeException {

    public SequestreInvalideException(String message) {
        super(message);
    }
}
