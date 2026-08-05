package com.fretcorridor.adm.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileTravailServiceTest {

    private final InMemoryDossierPort dossierPort = new InMemoryDossierPort();
    private final InMemoryJournalAuditPort journalAuditPort = new InMemoryJournalAuditPort();
    private final FileTravailService service = new FileTravailService(dossierPort, journalAuditPort);

    @Test
    void ouvrir_un_dossier_le_place_en_statut_ouvert_et_journalise() {
        Dossier dossier = service.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.NORMALE,
                "mission-a", List.of("acteur-transporteur-1"), List.of(), Instant.now().plus(2, ChronoUnit.DAYS));

        assertThat(dossier.statut()).isEqualTo(StatutDossier.OUVERT);
        assertThat(journalAuditPort.lister("tenant-bgft-douala"))
                .anyMatch(e -> e.action().equals("DOSSIER_OUVERT"));
    }

    @Test
    void la_file_de_travail_priorise_les_dossiers_haute_priorite_puis_le_delai_le_plus_proche() {
        Instant maintenant = Instant.now();
        Dossier basse = service.ouvrir("tenant-bgft-douala", TypeDossier.MODERATION, PrioriteDossier.BASSE, null,
                List.of(), List.of(), maintenant.plus(1, ChronoUnit.DAYS));
        Dossier hauteLointaine = service.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.HAUTE, null,
                List.of(), List.of(), maintenant.plus(5, ChronoUnit.DAYS));
        Dossier hauteProche = service.ouvrir("tenant-bgft-douala", TypeDossier.INCIDENT, PrioriteDossier.HAUTE, null,
                List.of(), List.of(), maintenant.plus(1, ChronoUnit.HOURS));

        List<Dossier> file = service.lister("tenant-bgft-douala");

        assertThat(file).extracting(Dossier::id).containsExactly(hauteProche.id(), hauteLointaine.id(), basse.id());
    }

    @Test
    void prendre_en_charge_un_dossier_le_passe_en_cours_et_journalise_l_acteur() {
        Dossier dossier = service.ouvrir("tenant-bgft-douala", TypeDossier.INCIDENT, PrioriteDossier.NORMALE, null,
                List.of(), List.of(), Instant.now().plus(1, ChronoUnit.DAYS));

        Dossier misAJour = service.prendreEnCharge(dossier.id(), "actor-admin-1");

        assertThat(misAJour.statut()).isEqualTo(StatutDossier.EN_COURS);
        assertThat(misAJour.priseEnChargeParActeurId()).isEqualTo("actor-admin-1");
        assertThat(journalAuditPort.lister("tenant-bgft-douala"))
                .anyMatch(e -> e.action().equals("DOSSIER_PRIS_EN_CHARGE") && e.acteurId().equals("actor-admin-1"));
    }

    @Test
    void prendre_en_charge_un_dossier_inconnu_leve_une_exception() {
        assertThatThrownBy(() -> service.prendreEnCharge("dossier-inconnu", "actor-admin-1"))
                .isInstanceOf(DossierIntrouvableException.class);
    }
}
