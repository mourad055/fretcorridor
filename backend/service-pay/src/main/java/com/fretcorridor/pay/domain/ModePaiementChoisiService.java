package com.fretcorridor.pay.domain;

import java.time.Instant;
import java.util.Optional;

/**
 * EF-PAY-06, Item B de docs/DEPENDANCES_MOBILE_PHASE4.md : le chargeur
 * choisit son moyen de paiement (CDC §7.6, UC-PAY-01 étape 2), affiché côté
 * Chauffeur avant tout encaissement réel.
 */
public class ModePaiementChoisiService {

    private final ModePaiementChoisiPort modePaiementChoisiPort;

    public ModePaiementChoisiService(ModePaiementChoisiPort modePaiementChoisiPort) {
        this.modePaiementChoisiPort = modePaiementChoisiPort;
    }

    public ModePaiementChoisi choisir(String tenantId, String missionId, ModePaiement modePaiement, Instant maintenant) {
        if (modePaiementChoisiPort.parMission(missionId).isPresent()) {
            throw new ModePaiementDejaChoisiException("Un moyen de paiement a déjà été choisi pour la mission " + missionId);
        }
        ModePaiementChoisi choix = new ModePaiementChoisi(missionId, tenantId, modePaiement, maintenant);
        modePaiementChoisiPort.enregistrer(choix);
        return choix;
    }

    public Optional<ModePaiementChoisi> pour(String missionId) {
        return modePaiementChoisiPort.parMission(missionId);
    }
}
