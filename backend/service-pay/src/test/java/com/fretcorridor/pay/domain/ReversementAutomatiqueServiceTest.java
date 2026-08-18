package com.fretcorridor.pay.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EF-PAY-08, CDC UC-PAY-02 : premier ordonnanceur réel du périmètre. ADR
 * 0015 : le délai part de Sequestre.libereLe (clôture), pas d'une preuve de
 * livraison distincte.
 */
class ReversementAutomatiqueServiceTest {

    private static final Duration DELAI_48H = Duration.ofHours(48);

    private final FakeSequestrePort sequestrePort = new FakeSequestrePort();
    private final FakeGrandLivrePort grandLivrePort = new FakeGrandLivrePort();
    private final FakeGarantiePort garantiePort = new FakeGarantiePort();
    private final FakeLitigeMissionPort litigeMissionPort = new FakeLitigeMissionPort();
    private final GrandLivreService grandLivreService = new GrandLivreService(grandLivrePort, garantiePort, litigeMissionPort, sequestrePort);
    private final ReversementAutomatiqueService service =
            new ReversementAutomatiqueService(sequestrePort, grandLivreService, DELAI_48H);

    @Test
    void reverses_a_mission_whose_delai_de_contestation_has_expired() {
        Instant maintenant = Instant.now();
        grandLivreService.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);
        sequestrePort.sauvegarder(new Sequestre("mission-1", SequestreEtat.LIBERE,
                maintenant.minus(50, ChronoUnit.HOURS), maintenant.minus(49, ChronoUnit.HOURS), "tenant-1", "actor-transporteur-1", "preuve-1"));

        List<EcritureMiroir> reversements = service.detecterEtReverser(maintenant);

        assertThat(reversements).hasSize(1);
        assertThat(reversements.get(0).montant()).isEqualByComparingTo("100");
        assertThat(reversements.get(0).beneficiaireId()).isEqualTo("actor-transporteur-1");
    }

    @Test
    void does_not_reverse_a_mission_still_within_the_delai_de_contestation() {
        Instant maintenant = Instant.now();
        grandLivreService.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);
        sequestrePort.sauvegarder(new Sequestre("mission-1", SequestreEtat.LIBERE,
                maintenant.minus(10, ChronoUnit.HOURS), maintenant.minus(1, ChronoUnit.HOURS), "tenant-1", "actor-transporteur-1", "preuve-1"));

        assertThat(service.detecterEtReverser(maintenant)).isEmpty();
    }

    @Test
    void skips_a_mission_with_an_active_litige_without_aborting_the_rest_of_the_batch() {
        Instant maintenant = Instant.now();
        grandLivreService.enregistrerEncaissement("tenant-1", "mission-litige", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);
        sequestrePort.sauvegarder(new Sequestre("mission-litige", SequestreEtat.LIBERE,
                maintenant.minus(50, ChronoUnit.HOURS), maintenant.minus(49, ChronoUnit.HOURS), "tenant-1", "actor-transporteur-1", "preuve-1"));
        litigeMissionPort.enregistrerSiPlusRecent(new LitigeMission("mission-litige", "tenant-1", true, maintenant.minus(49, ChronoUnit.HOURS)));

        grandLivreService.enregistrerEncaissement("tenant-1", "mission-ok", new BigDecimal("200"), "ref-enc-2", ModePaiement.VIREMENT);
        sequestrePort.sauvegarder(new Sequestre("mission-ok", SequestreEtat.LIBERE,
                maintenant.minus(50, ChronoUnit.HOURS), maintenant.minus(49, ChronoUnit.HOURS), "tenant-1", "actor-transporteur-2", "preuve-1"));

        List<EcritureMiroir> reversements = service.detecterEtReverser(maintenant);

        assertThat(reversements).hasSize(1);
        assertThat(reversements.get(0).missionId()).isEqualTo("mission-ok");
    }

    @Test
    void does_not_re_reverse_a_mission_already_fully_reversed_manually() {
        Instant maintenant = Instant.now();
        grandLivreService.enregistrerEncaissement("tenant-1", "mission-1", new BigDecimal("100"), "ref-enc", ModePaiement.VIREMENT);
        sequestrePort.sauvegarder(new Sequestre("mission-1", SequestreEtat.LIBERE,
                maintenant.minus(50, ChronoUnit.HOURS), maintenant.minus(49, ChronoUnit.HOURS), "tenant-1", "actor-transporteur-1", "preuve-1"));
        grandLivreService.enregistrerReversement("tenant-1", "mission-1", "actor-transporteur-1", new BigDecimal("100"), "ref-rev-anticipe");

        assertThat(service.detecterEtReverser(maintenant)).isEmpty();
    }

    @Test
    void ignores_a_sequestre_still_declenche() {
        Instant maintenant = Instant.now();
        sequestrePort.sauvegarder(new Sequestre("mission-1", SequestreEtat.DECLENCHE, maintenant.minus(50, ChronoUnit.HOURS), null, null, null, null));

        assertThat(service.detecterEtReverser(maintenant)).isEmpty();
    }

    @Test
    void reverses_a_mission_backed_only_by_a_garantie_terme_contractuel() {
        Instant maintenant = Instant.now();
        garantiePort.enregistrer(new Garantie("g-1", "tenant-1", "mission-1", "garant-bnp", new BigDecimal("300"), "ref-garantie-1", maintenant.minus(60, ChronoUnit.HOURS)));
        sequestrePort.sauvegarder(new Sequestre("mission-1", SequestreEtat.LIBERE,
                maintenant.minus(50, ChronoUnit.HOURS), maintenant.minus(49, ChronoUnit.HOURS), "tenant-1", "actor-transporteur-1", "preuve-1"));

        List<EcritureMiroir> reversements = service.detecterEtReverser(maintenant);

        assertThat(reversements).hasSize(1);
        assertThat(reversements.get(0).montant()).isEqualByComparingTo("300");
    }
}
