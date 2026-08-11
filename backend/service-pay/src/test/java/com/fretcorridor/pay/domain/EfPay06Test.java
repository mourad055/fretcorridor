package com.fretcorridor.pay.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EF-PAY-06 (socle) : le moyen de paiement choisi par le chargeur à
 * l'encaissement (monnaie électronique, virement, terme contractuel) est
 * tracé sur l'écriture de grand livre correspondante.
 */
class EfPay06Test {

    private final FakeGrandLivrePort grandLivrePort = new FakeGrandLivrePort();
    private final GrandLivreService service = new GrandLivreService(grandLivrePort);

    @Test
    void records_the_mode_de_paiement_chosen_for_an_encaissement() {
        EcritureMiroir encaissement = service.enregistrerEncaissement(
                "tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.MONNAIE_ELECTRONIQUE);

        assertThat(encaissement.modePaiement()).isEqualTo(ModePaiement.MONNAIE_ELECTRONIQUE);
    }

    @Test
    void a_reversement_never_carries_a_mode_de_paiement_it_only_concerns_the_encaissement_side() {
        service.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);

        EcritureMiroir reversement = service.enregistrerReversement(
                "tenant-1", "mission-1", "actor-transporteur-1", new BigDecimal("90"), "ref-rev");

        assertThat(reversement.modePaiement()).isNull();
    }
}
