package com.fretcorridor.gateway.domain.kyc;

/** service-ida injoignable ou erreur non métier lors d'un appel KYC admin. */
public class KycServiceIndisponibleException extends RuntimeException {
    public KycServiceIndisponibleException() {
        super("Le service d'identité (KYC) est temporairement indisponible");
    }
}
