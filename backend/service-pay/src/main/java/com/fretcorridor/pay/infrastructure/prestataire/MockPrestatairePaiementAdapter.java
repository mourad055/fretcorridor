package com.fretcorridor.pay.infrastructure.prestataire;

import com.fretcorridor.pay.domain.PrestatairePaiementPort;
import com.fretcorridor.pay.domain.ReleveLigne;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO(produit): adaptateur sandbox — verrou V2 non levé (aucun prestataire de
 * paiement agréé sélectionné à ce stade, PRD §1.3, §21.1). À remplacer par
 * l'intégration réelle sans changer le domaine (port {@link PrestatairePaiementPort}
 * inchangé). Simule un relevé prestataire qui confirme chaque encaissement
 * enregistré via {@link #confirmer(String, BigDecimal)} — le mécanisme d'écart
 * (ENF-FIN-03) est prouvé au niveau domaine par {@code EnfFin03Test}, avec un
 * faux port dédié.
 */
@Component
public class MockPrestatairePaiementAdapter implements PrestatairePaiementPort {

    private final Map<String, BigDecimal> releves = new ConcurrentHashMap<>();

    public void confirmer(String missionId, BigDecimal montant) {
        releves.merge(missionId, montant, BigDecimal::add);
    }

    @Override
    public ReleveLigne obtenirReleve(String missionId) {
        return new ReleveLigne(missionId, releves.getOrDefault(missionId, BigDecimal.ZERO));
    }
}
