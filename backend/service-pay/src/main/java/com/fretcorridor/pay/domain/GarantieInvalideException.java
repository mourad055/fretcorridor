package com.fretcorridor.pay.domain;

/** EF-PAY-06 : une seule garantie active par mission, comme le séquestre. */
public class GarantieInvalideException extends RuntimeException {

    public GarantieInvalideException(String message) {
        super(message);
    }
}
