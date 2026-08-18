package com.fretcorridor.pay.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * EF-PAY-08, CDC UC-PAY-02 : déclenche le reversement automatiquement à
 * l'expiration du délai de contestation, et le suspend en cas de litige.
 * Domaine pur, sans dépendance à Spring — testable sans mock de framework.
 *
 * ADR 0015 : le délai de contestation part de {@link Sequestre#libereLe()}
 * (clôture/encaissement), faute d'un signal de livraison distinct — pas de
 * l'endpoint qui remplacera ce point de départ le jour où EXE (Mobile)
 * publiera un événement de livraison.
 */
public class ReversementAutomatiqueService {

    private final SequestrePort sequestrePort;
    private final GrandLivreService grandLivreService;
    private final Duration delaiContestation;

    public ReversementAutomatiqueService(SequestrePort sequestrePort, GrandLivreService grandLivreService, Duration delaiContestation) {
        this.sequestrePort = sequestrePort;
        this.grandLivreService = grandLivreService;
        this.delaiContestation = delaiContestation;
    }

    public List<EcritureMiroir> detecterEtReverser(Instant maintenant) {
        List<EcritureMiroir> reversements = new ArrayList<>();
        for (Sequestre sequestre : eligibles(maintenant)) {
            BigDecimal solde = grandLivreService.soldeDisponiblePourReversement(sequestre.missionId());
            if (solde.signum() <= 0) {
                continue; // déjà intégralement reversé (ex. reversement anticipé A3, UC-PAY-02)
            }
            try {
                reversements.add(grandLivreService.enregistrerReversement(
                        sequestre.tenantId(), sequestre.missionId(), sequestre.transporteurId(), solde,
                        "auto-reversement-" + sequestre.missionId() + "-" + maintenant.toEpochMilli()));
            } catch (ReversementSuspenduPourLitigeException litigeActif) {
                // Suspendu, pas une erreur — retenté à la prochaine exécution (RG-079 : délai plafonné et suivi).
            }
        }
        return reversements;
    }

    private List<Sequestre> eligibles(Instant maintenant) {
        return sequestrePort.listerTous().stream()
                .filter(s -> s.etat() == SequestreEtat.LIBERE)
                .filter(s -> s.tenantId() != null && s.transporteurId() != null)
                .filter(s -> s.libereLe().plus(delaiContestation).isBefore(maintenant))
                .toList();
    }
}
