package com.fretcorridor.pay.domain;

/** ENF-FIN-02 (bloquant) : aucun reversement ne peut être instruit sans encaissement correspondant enregistré. */
public class ReversementSansEncaissementException extends RuntimeException {

    public ReversementSansEncaissementException(String missionId) {
        super("Aucun encaissement suffisant enregistré pour la mission " + missionId + " : reversement refusé");
    }
}
