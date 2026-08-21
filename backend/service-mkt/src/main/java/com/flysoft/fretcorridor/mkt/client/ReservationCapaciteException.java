package com.flysoft.fretcorridor.mkt.client;

/** EF-MKT-08 : la reservation reelle de capacite a echoue — l'acceptation ne doit pas etre validee. */
public class ReservationCapaciteException extends RuntimeException {
    public ReservationCapaciteException(String message, Throwable cause) {
        super(message, cause);
    }
}
