package com.fretcorridor.pay.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ENF-FIN-03 (bloquant) : la réconciliation quotidienne lève une alerte
 * bloquante sur tout écart, et l'écart doit être isolé.
 */
class EnfFin03Test {

    private final FakeGrandLivrePort grandLivrePort = new FakeGrandLivrePort();
    private final GrandLivreService grandLivreService = new GrandLivreService(grandLivrePort, new FakeGarantiePort(), new FakeLitigeMissionPort(), new FakeSequestrePort());
    private final FakeReconciliationEventPort reconciliationEventPort = new FakeReconciliationEventPort();

    @Test
    void raises_no_alert_when_the_provider_statement_matches_the_local_ledger() {
        grandLivreService.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);
        var prestataire = fakePrestataire("mission-1", new BigDecimal("100"));
        var reconciliation = new ReconciliationService(grandLivrePort, prestataire, reconciliationEventPort);

        AlerteReconciliation alerte = reconciliation.reconcilier("mission-1");

        assertThat(alerte.bloquante()).isFalse();
        assertThat(grandLivrePort.parMission("mission-1")).allMatch(e -> e.statut() == StatutEcriture.VALIDE);
    }

    @Test
    void raises_a_blocking_alert_and_isolates_the_entries_when_an_ecart_is_injected() {
        grandLivreService.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);
        // Écart injecté : le prestataire annonce un montant différent du grand livre local.
        var prestataire = fakePrestataire("mission-1", new BigDecimal("85"));
        var reconciliation = new ReconciliationService(grandLivrePort, prestataire, reconciliationEventPort);

        AlerteReconciliation alerte = reconciliation.reconcilier("mission-1");

        assertThat(alerte.bloquante()).isTrue();
        assertThat(alerte.ecart()).isEqualByComparingTo("15");
        assertThat(grandLivrePort.parMission("mission-1"))
                .as("toute écriture de la mission en écart doit être isolée, jamais laissée VALIDE en silence")
                .allMatch(e -> e.statut() == StatutEcriture.SUSPENDUE);
    }

    /** EF-PAY-09 : l'écart bloquant est aussi levé comme alerte vers la file de travail Admin. */
    @Test
    void a_blocking_ecart_publishes_a_reconciliation_event() {
        grandLivreService.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);
        var prestataire = fakePrestataire("mission-1", new BigDecimal("85"));
        var reconciliation = new ReconciliationService(grandLivrePort, prestataire, reconciliationEventPort);

        reconciliation.reconcilier("mission-1");

        assertThat(reconciliationEventPort.publications()).hasSize(1);
        var publication = reconciliationEventPort.publications().get(0);
        assertThat(publication.missionId()).isEqualTo("mission-1");
        assertThat(publication.tenantId()).isEqualTo("tenant-1");
        assertThat(publication.ecart()).isEqualByComparingTo("15");
    }

    /** EF-PAY-02, RG-077 : le balayage quotidien ne remonte que les écarts bloquants, sur toutes les missions actives. */
    @Test
    void reconcilierMissionsActives_only_returns_blocking_alerts() {
        grandLivreService.enregistrerEncaissement("tenant-1", "mission-ok", new BigDecimal("100"), "ref-ok", ModePaiement.VIREMENT);
        grandLivreService.enregistrerEncaissement("tenant-1", "mission-ecart", new BigDecimal("100"), "ref-ecart", ModePaiement.VIREMENT);
        var prestataire = new PrestatairePaiementPort() {
            @Override
            public ReleveLigne obtenirReleve(String missionId) {
                BigDecimal montant = missionId.equals("mission-ok") ? new BigDecimal("100") : new BigDecimal("50");
                return new ReleveLigne(missionId, montant);
            }
        };
        var reconciliation = new ReconciliationService(grandLivrePort, prestataire, reconciliationEventPort);

        List<AlerteReconciliation> alertes = reconciliation.reconcilierMissionsActives();

        assertThat(alertes).hasSize(1);
        assertThat(alertes.get(0).missionId()).isEqualTo("mission-ecart");
    }

    private PrestatairePaiementPort fakePrestataire(String missionId, BigDecimal montant) {
        return id -> new ReleveLigne(missionId, montant);
    }
}
