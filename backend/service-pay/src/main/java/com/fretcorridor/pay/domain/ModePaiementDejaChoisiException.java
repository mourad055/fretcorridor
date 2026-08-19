package com.fretcorridor.pay.domain;

/** EF-PAY-06 : un seul choix de moyen de paiement par mission, comme la garantie et le séquestre. */
public class ModePaiementDejaChoisiException extends RuntimeException {

    public ModePaiementDejaChoisiException(String message) {
        super(message);
    }
}
