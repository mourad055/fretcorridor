package com.fretcorridor.pay.domain;

/**
 * Port hexagonal : mémorisation des notifications déjà traitées (EF-PAY-05),
 * pour absorber un rejeu réseau du prestataire sans dupliquer l'écriture.
 */
public interface NotificationIdempotencePort {

    boolean dejaTraitee(String idempotenceKey);

    void marquerTraitee(String idempotenceKey);
}
