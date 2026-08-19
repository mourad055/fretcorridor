package com.fretcorridor.pay.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * EF-PAY-06 (terme contractuel) : souscription de la garantie tierce qui
 * autorise une mission à être confirmée sans encaissement préalable
 * (CDC §7.6, UC-PAY-01 A1).
 */
public class GarantieService {

    private final GarantiePort garantiePort;

    public GarantieService(GarantiePort garantiePort) {
        this.garantiePort = garantiePort;
    }

    public Garantie souscrire(String tenantId, String missionId, String garantId, BigDecimal montant, String referenceGarantie) {
        if (garantiePort.parMission(missionId).isPresent()) {
            throw new GarantieInvalideException("Une garantie existe déjà pour la mission " + missionId);
        }
        Garantie garantie = new Garantie(UUID.randomUUID().toString(), tenantId, missionId, garantId, montant, referenceGarantie, Instant.now());
        garantiePort.enregistrer(garantie);
        return garantie;
    }
}
