package com.fretcorridor.pay.domain;

/**
 * Port hexagonal vers le prestataire de paiement agréé (CDC, verrou V2 non levé).
 * TODO(produit): remplacer l'adaptateur sandbox/mock par l'intégration réelle
 * une fois le prestataire sélectionné — le domaine ne change pas (PRD §1.3).
 */
public interface PrestatairePaiementPort {

    ReleveLigne obtenirReleve(String missionId);
}
