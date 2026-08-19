package com.fretcorridor.pay.domain;

/** EF-PAY-08, CDC UC-PAY-02 A1 : le reversement est suspendu tant qu'un litige est actif sur la mission. */
public class ReversementSuspenduPourLitigeException extends RuntimeException {

    public ReversementSuspenduPourLitigeException(String missionId) {
        super("Reversement suspendu, litige actif sur la mission " + missionId);
    }
}
