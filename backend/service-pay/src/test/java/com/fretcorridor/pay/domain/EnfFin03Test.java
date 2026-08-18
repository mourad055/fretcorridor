package com.fretcorridor.pay.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ENF-FIN-03 (bloquant) : la réconciliation quotidienne lève une alerte
 * bloquante sur tout écart, et l'écart doit être isolé.
 */
class EnfFin03Test {

    private final FakeGrandLivrePort grandLivrePort = new FakeGrandLivrePort();
    private final GrandLivreService grandLivreService = new GrandLivreService(grandLivrePort, new FakeGarantiePort(), new FakeLitigeMissionPort(), new FakeSequestrePort());

    @Test
    void raises_no_alert_when_the_provider_statement_matches_the_local_ledger() {
        grandLivreService.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);
        var prestataire = fakePrestataire("mission-1", new BigDecimal("100"));
        var reconciliation = new ReconciliationService(grandLivrePort, prestataire);

        AlerteReconciliation alerte = reconciliation.reconcilier("mission-1");

        assertThat(alerte.bloquante()).isFalse();
        assertThat(grandLivrePort.parMission("mission-1")).allMatch(e -> e.statut() == StatutEcriture.VALIDE);
    }

    @Test
    void raises_a_blocking_alert_and_isolates_the_entries_when_an_ecart_is_injected() {
        grandLivreService.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);
        // Écart injecté : le prestataire annonce un montant différent du grand livre local.
        var prestataire = fakePrestataire("mission-1", new BigDecimal("85"));
        var reconciliation = new ReconciliationService(grandLivrePort, prestataire);

        AlerteReconciliation alerte = reconciliation.reconcilier("mission-1");

        assertThat(alerte.bloquante()).isTrue();
        assertThat(alerte.ecart()).isEqualByComparingTo("15");
        assertThat(grandLivrePort.parMission("mission-1"))
                .as("toute écriture de la mission en écart doit être isolée, jamais laissée VALIDE en silence")
                .allMatch(e -> e.statut() == StatutEcriture.SUSPENDUE);
    }

    private PrestatairePaiementPort fakePrestataire(String missionId, BigDecimal montant) {
        return id -> new ReleveLigne(missionId, montant);
    }
}
