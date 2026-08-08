package com.fretcorridor.pay.domain;

/**
 * Port hexagonal : vérification cryptographique d'une notification entrante
 * (EF-PAY-05). Le domaine ignore l'algorithme et le secret utilisés.
 */
public interface SignatureVerifierPort {

    boolean estValide(String corpsBrut, String signatureRecue);
}
