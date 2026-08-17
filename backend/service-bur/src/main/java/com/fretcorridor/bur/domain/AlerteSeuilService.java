package com.fretcorridor.bur.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * EF-BUR-07 (S) : configuration d'alertes sur seuils par l'agent (UC-BUR-02
 * A1 « détection de variations anormales de prix ou de volume »). Domaine
 * pur, sans dépendance à Spring — testable sans mock de framework.
 *
 * Évaluation à la demande, pas de notification poussée : service-not
 * (canal de notification) exige un endpoint côté Mobile qui n'existe pas
 * encore (cf. docs/CONTEXTE_SESSION_UI.md §3, bloqué, hors périmètre Web).
 * {@link #evaluer(String)} expose l'état courant de chaque alerte
 * configurée ; la livraison effective d'une notification reste à construire
 * une fois ce blocage levé côté Mobile.
 */
public class AlerteSeuilService {

    private final AlerteSeuilPort alerteSeuilPort;
    private final ObservatoireService observatoireService;

    public AlerteSeuilService(AlerteSeuilPort alerteSeuilPort, ObservatoireService observatoireService) {
        this.alerteSeuilPort = alerteSeuilPort;
        this.observatoireService = observatoireService;
    }

    public AlerteSeuil configurer(String tenantId, UUID axeId, IndicateurObservatoire indicateur,
                                   Comparateur comparateur, BigDecimal seuil, String acteurId) {
        AlerteSeuil alerte = new AlerteSeuil(UUID.randomUUID().toString(), tenantId, axeId, indicateur,
                comparateur, seuil, acteurId, Instant.now());
        alerteSeuilPort.sauvegarder(alerte);
        return alerte;
    }

    public List<AlerteSeuil> lister(String tenantId) {
        return alerteSeuilPort.listerParTenant(tenantId);
    }

    public void supprimer(String id, String tenantId) {
        alerteSeuilPort.supprimer(id, tenantId);
    }

    public List<EtatAlerte> evaluer(String tenantId) {
        return lister(tenantId).stream().map(this::evaluerUne).toList();
    }

    private EtatAlerte evaluerUne(AlerteSeuil alerte) {
        ObservatoireAxe observatoire = observatoireService.indicateursPourAxe(alerte.tenantId(), alerte.axeId());
        if (!observatoire.seuilAtteint()) {
            return new EtatAlerte(alerte, false, false, null);
        }
        BigDecimal valeur = valeurIndicateur(alerte.indicateur(), observatoire);
        return new EtatAlerte(alerte, true, alerte.comparateur().declenchee(valeur, alerte.seuil()), valeur);
    }

    private static BigDecimal valeurIndicateur(IndicateurObservatoire indicateur, ObservatoireAxe observatoire) {
        return switch (indicateur) {
            case NOMBRE_MISSIONS -> BigDecimal.valueOf(observatoire.nombreMissions().orElseThrow());
            case PRIX_MEDIANE -> observatoire.prixMediane().orElseThrow();
            case TAUX_DESEQUILIBRE_DIRECTIONNEL -> BigDecimal.valueOf(observatoire.tauxDesequilibreDirectionnel().orElseThrow());
        };
    }
}
