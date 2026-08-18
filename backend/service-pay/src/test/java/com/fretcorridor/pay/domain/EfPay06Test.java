package com.fretcorridor.pay.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EF-PAY-06 (socle) : le moyen de paiement choisi par le chargeur à
 * l'encaissement (monnaie électronique, virement, terme contractuel) est
 * tracé sur l'écriture de grand livre correspondante.
 */
class EfPay06Test {

    private final FakeGrandLivrePort grandLivrePort = new FakeGrandLivrePort();
    private final FakeGarantiePort garantiePort = new FakeGarantiePort();
    private final FakeSequestrePort sequestrePort = new FakeSequestrePort();
    private final GrandLivreService service = new GrandLivreService(grandLivrePort, garantiePort, new FakeLitigeMissionPort(), sequestrePort);

    private void libererAvecPreuve(String missionId) {
        sequestrePort.sauvegarder(new Sequestre(missionId, SequestreEtat.LIBERE, Instant.now(), Instant.now(),
                "tenant-1", "actor-transporteur-1", "preuve-1"));
    }

    @Test
    void records_the_mode_de_paiement_chosen_for_an_encaissement() {
        EcritureMiroir encaissement = service.enregistrerEncaissement(
                "tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.MONNAIE_ELECTRONIQUE);

        assertThat(encaissement.modePaiement()).isEqualTo(ModePaiement.MONNAIE_ELECTRONIQUE);
    }

    @Test
    void a_reversement_never_carries_a_mode_de_paiement_it_only_concerns_the_encaissement_side() {
        service.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);
        libererAvecPreuve("mission-1");

        EcritureMiroir reversement = service.enregistrerReversement(
                "tenant-1", "mission-1", "actor-transporteur-1", new BigDecimal("90"), "ref-rev");

        assertThat(reversement.modePaiement()).isNull();
    }

    /**
     * EF-PAY-06 (terme contractuel), CDC §7.6 UC-PAY-01 A1 : une garantie
     * tierce active couvre le même rôle qu'un encaissement réel pour RG-075
     * — le transporteur est reversé sans attendre l'encaissement réel, le
     * risque de crédit restant porté par le garant.
     */
    @Test
    void a_reversement_is_allowed_against_an_active_garantie_with_no_real_encaissement_at_all() {
        garantiePort.enregistrer(new Garantie("g-1", "tenant-1", "mission-1", "garant-bnp", new BigDecimal("100"), "ref-garantie-1", java.time.Instant.now()));
        libererAvecPreuve("mission-1");

        EcritureMiroir reversement = service.enregistrerReversement(
                "tenant-1", "mission-1", "actor-transporteur-1", new BigDecimal("100"), "ref-rev");

        assertThat(reversement.nature()).isEqualTo(NatureEcriture.REVERSEMENT);
        assertThat(reversement.montant()).isEqualByComparingTo("100");
    }

    @Test
    void refuses_a_reversement_exceeding_the_garantie_when_no_encaissement_backs_the_rest() {
        garantiePort.enregistrer(new Garantie("g-1", "tenant-1", "mission-1", "garant-bnp", new BigDecimal("100"), "ref-garantie-1", java.time.Instant.now()));

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() ->
                        service.enregistrerReversement("tenant-1", "mission-1", "actor-transporteur-1", new BigDecimal("150"), "ref-rev")))
                .isInstanceOf(ReversementSansEncaissementException.class);
    }

    @Test
    void a_real_encaissement_and_a_garantie_pool_together_as_available_funds() {
        service.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("50"), "ref-enc", ModePaiement.VIREMENT);
        garantiePort.enregistrer(new Garantie("g-1", "tenant-1", "mission-1", "garant-bnp", new BigDecimal("50"), "ref-garantie-1", java.time.Instant.now()));
        libererAvecPreuve("mission-1");

        EcritureMiroir reversement = service.enregistrerReversement(
                "tenant-1", "mission-1", "actor-transporteur-1", new BigDecimal("100"), "ref-rev");

        assertThat(reversement.montant()).isEqualByComparingTo("100");
    }
}
