package com.fretcorridor.pay.domain;

/** EF-PAY-07 : une seule déclaration espèces par mission, comme le séquestre et la garantie. */
public class DeclarationEspecesInvalideException extends RuntimeException {

    public DeclarationEspecesInvalideException(String message) {
        super(message);
    }
}
