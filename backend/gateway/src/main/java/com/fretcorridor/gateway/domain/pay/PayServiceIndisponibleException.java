package com.fretcorridor.gateway.domain.pay;

public class PayServiceIndisponibleException extends RuntimeException {
    public PayServiceIndisponibleException() {
        super("service-pay indisponible");
    }
}
