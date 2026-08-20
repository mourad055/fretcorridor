package com.fretcorridor.pay.domain;

/** RG-078 : aucun reversement sans preuve de livraison enregistrée (séquestre non libéré, ou libéré sans preuve). */
public class ReversementSansPreuveLivraisonException extends RuntimeException {

    public ReversementSansPreuveLivraisonException(String missionId) {
        super("Aucune preuve de livraison enregistrée pour la mission " + missionId + " : reversement refusé");
    }
}
