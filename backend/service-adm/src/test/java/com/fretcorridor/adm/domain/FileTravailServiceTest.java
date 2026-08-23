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
    private final InMemoryDossierEventPort dossierEventPort = new InMemoryDossierEventPort();
    private final FileTravailService service = new FileTravailService(dossierPort, journalAuditPort, dossierEventPort);

    @Test
    void ouvrir_un_dossier_le_place_en_statut_ouvert_et_journalise() {
        Dossier dossier = service.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.NORMALE,
                "mission-a", List.of("acteur-transporteur-1"), List.of(), null, null, Instant.now().plus(2, ChronoUnit.DAYS));

        assertThat(dossier.statut()).isEqualTo(StatutDossier.OUVERT);
        assertThat(journalAuditPort.lister("tenant-bgft-douala"))
                .anyMatch(e -> e.action().equals("DOSSIER_OUVERT"));
    }

    /** Motif/description (audit de suivi, 23 aout) : contenu libre saisi par l'auteur, doit survivre au round-trip. */
    @Test
    void ouvrir_un_dossier_conserve_le_motif_et_la_description() {
        Dossier dossier = service.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.NORMALE,
                "mission-a", List.of(), List.of(), "Marchandise endommagée",
                "Le carton était écrasé à la livraison.", Instant.now().plus(2, ChronoUnit.DAYS));

        assertThat(dossier.motif()).isEqualTo("Marchandise endommagée");
        assertThat(dossier.description()).isEqualTo("Le carton était écrasé à la livraison.");
    }

    @Test
    void ouvrir_un_dossier_litige_avec_mission_le_publie_pour_service_pay() {
        service.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.NORMALE,
                "mission-a", List.of(), List.of(), null, null, Instant.now().plus(2, ChronoUnit.DAYS));

        assertThat(dossierEventPort.publies()).hasSize(1);
        assertThat(dossierEventPort.publies().get(0).missionId()).isEqualTo("mission-a");
    }

    @Test
    void ouvrir_un_dossier_moderation_ou_sans_mission_n_est_jamais_publie() {
        service.ouvrir("tenant-bgft-douala", TypeDossier.MODERATION, PrioriteDossier.NORMALE,
                null, List.of(), List.of(), null, null, Instant.now().plus(2, ChronoUnit.DAYS));
        service.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.NORMALE,
                null, List.of(), List.of(), null, null, Instant.now().plus(2, ChronoUnit.DAYS));

        assertThat(dossierEventPort.publies()).isEmpty();
    }

    @Test
    void la_file_de_travail_priorise_les_dossiers_haute_priorite_puis_le_delai_le_plus_proche() {
        Instant maintenant = Instant.now();
        Dossier basse = service.ouvrir("tenant-bgft-douala", TypeDossier.MODERATION, PrioriteDossier.BASSE, null,
                List.of(), List.of(), null, null, maintenant.plus(1, ChronoUnit.DAYS));
        Dossier hauteLointaine = service.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.HAUTE, null,
                List.of(), List.of(), null, null, maintenant.plus(5, ChronoUnit.DAYS));
        Dossier hauteProche = service.ouvrir("tenant-bgft-douala", TypeDossier.INCIDENT, PrioriteDossier.HAUTE, null,
                List.of(), List.of(), null, null, maintenant.plus(1, ChronoUnit.HOURS));

        List<Dossier> file = service.lister("tenant-bgft-douala");

        assertThat(file).extracting(Dossier::id).containsExactly(hauteProche.id(), hauteLointaine.id(), basse.id());
    }

    @Test
    void prendre_en_charge_un_dossier_le_passe_en_cours_et_journalise_l_acteur() {
        Dossier dossier = service.ouvrir("tenant-bgft-douala", TypeDossier.INCIDENT, PrioriteDossier.NORMALE, null,
                List.of(), List.of(), null, null, Instant.now().plus(1, ChronoUnit.DAYS));

        Dossier misAJour = service.prendreEnCharge(dossier.id(), "actor-admin-1");

        assertThat(misAJour.statut()).isEqualTo(StatutDossier.EN_COURS);
        assertThat(misAJour.priseEnChargeParActeurId()).isEqualTo("actor-admin-1");
        assertThat(journalAuditPort.lister("tenant-bgft-douala"))
                .anyMatch(e -> e.action().equals("DOSSIER_PRIS_EN_CHARGE") && e.acteurId().equals("actor-admin-1"));
    }

    /** IDOR corrigé (audit CDC du 19 août, §7.2). */
    @Test
    void consulter_un_dossier_de_son_propre_tenant_reussit() {
        Dossier dossier = service.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.NORMALE,
                "mission-a", List.of(), List.of(), null, null, Instant.now().plus(1, ChronoUnit.DAYS));

        Dossier consulte = service.consulter(dossier.id(), "tenant-bgft-douala");

        assertThat(consulte.id()).isEqualTo(dossier.id());
    }

    /** IDOR corrigé (audit CDC du 19 août, §7.2) : même exception que "introuvable", jamais une confirmation d'existence. */
    @Test
    void consulter_un_dossier_d_un_autre_tenant_est_refuse_comme_introuvable() {
        Dossier dossier = service.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.NORMALE,
                "mission-a", List.of(), List.of(), null, null, Instant.now().plus(1, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service.consulter(dossier.id(), "tenant-bnft-ndjamena"))
                .isInstanceOf(DossierIntrouvableException.class);
    }

    @Test
    void prendre_en_charge_un_dossier_inconnu_leve_une_exception() {
        assertThatThrownBy(() -> service.prendreEnCharge("dossier-inconnu", "actor-admin-1"))
                .isInstanceOf(DossierIntrouvableException.class);
    }

    private Dossier ouvrirEtTrancher(String tenantId, String decideur) {
        InMemoryConfigurationPort configurationPort = new InMemoryConfigurationPort();
        configurationPort.sauvegarder(new ConfigurationVersionnee("g-1", DecisionService.CLE_GRILLE_DECISION,
                tenantId, "grille v1", "actor-admin-1", 1, Instant.now()));
        DecisionService decisionService = new DecisionService(dossierPort, journalAuditPort, dossierEventPort, configurationPort);
        Dossier dossier = service.ouvrir(tenantId, TypeDossier.LITIGE, PrioriteDossier.NORMALE, "mission-a",
                List.of("acteur-transporteur-1"), List.of("preuve-1"), null, null, Instant.now().plus(1, ChronoUnit.DAYS));
        return decisionService.trancher(dossier.id(), "RESOLU_EN_FAVEUR_TRANSPORTEUR", "motif", decideur);
    }

    /** UC-ADM-01 A2 : un recours ouvre un second Dossier lié à l'original, jamais une réouverture. */
    @Test
    void ouvrir_un_recours_cree_un_second_dossier_avec_le_meme_contexte() {
        Dossier original = ouvrirEtTrancher("tenant-bgft-douala", "actor-admin-1");

        Dossier recours = service.ouvrirRecours(original.id(), PrioriteDossier.HAUTE, Instant.now().plus(1, ChronoUnit.DAYS));

        assertThat(recours.id()).isNotEqualTo(original.id());
        assertThat(recours.statut()).isEqualTo(StatutDossier.OUVERT);
        assertThat(recours.recoursDeDossierId()).isEqualTo(original.id());
        assertThat(recours.missionId()).isEqualTo(original.missionId());
        assertThat(recours.parties()).isEqualTo(original.parties());
        assertThat(recours.preuvesReferences()).isEqualTo(original.preuvesReferences());
        assertThat(original.statut())
                .as("l'original reste clos, jamais rouvert")
                .isEqualTo(StatutDossier.CLOS);
    }

    @Test
    void ouvrir_un_recours_sur_un_dossier_pas_encore_tranche_est_refuse() {
        Dossier dossier = service.ouvrir("tenant-bgft-douala", TypeDossier.LITIGE, PrioriteDossier.NORMALE,
                null, List.of(), List.of(), null, null, Instant.now().plus(1, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service.ouvrirRecours(dossier.id(), PrioriteDossier.HAUTE, Instant.now().plus(1, ChronoUnit.DAYS)))
                .isInstanceOf(DossierNonTrancheException.class);
    }

    /** RG-098 : l'opérateur ayant décidé en premier ressort n'instruit pas le recours. */
    @Test
    void prendre_en_charge_un_recours_par_le_meme_operateur_que_le_premier_decideur_est_refuse() {
        Dossier original = ouvrirEtTrancher("tenant-bgft-douala", "actor-admin-1");
        Dossier recours = service.ouvrirRecours(original.id(), PrioriteDossier.HAUTE, Instant.now().plus(1, ChronoUnit.DAYS));

        assertThatThrownBy(() -> service.prendreEnCharge(recours.id(), "actor-admin-1"))
                .isInstanceOf(RecoursMemeOperateurException.class);
    }

    @Test
    void prendre_en_charge_un_recours_par_un_operateur_different_est_autorise() {
        Dossier original = ouvrirEtTrancher("tenant-bgft-douala", "actor-admin-1");
        Dossier recours = service.ouvrirRecours(original.id(), PrioriteDossier.HAUTE, Instant.now().plus(1, ChronoUnit.DAYS));

        Dossier misAJour = service.prendreEnCharge(recours.id(), "actor-admin-2");

        assertThat(misAJour.statut()).isEqualTo(StatutDossier.EN_COURS);
    }

    /** EF-PAY-09, ENF-FIN-03 : un écart de réconciliation ouvre un incident priorité haute. */
    @Test
    void ouvrir_un_incident_reconciliation_cree_un_dossier_incident_priorite_haute() {
        var incident = service.ouvrirIncidentReconciliation("tenant-bgft-douala", "mission-a",
                "Écart de réconciliation : 15", Instant.now().plus(2, ChronoUnit.DAYS));

        assertThat(incident).isPresent();
        assertThat(incident.get().type()).isEqualTo(TypeDossier.INCIDENT);
        assertThat(incident.get().priorite()).isEqualTo(PrioriteDossier.HAUTE);
        assertThat(incident.get().missionId()).isEqualTo("mission-a");
        assertThat(incident.get().preuvesReferences()).containsExactly("Écart de réconciliation : 15");
    }

    /** Le balayage quotidien republie l'événement chaque jour tant que l'écart n'est pas résolu — pas de doublon. */
    @Test
    void ouvrir_un_incident_reconciliation_ne_duplique_pas_un_incident_deja_ouvert_pour_la_meme_mission() {
        var premier = service.ouvrirIncidentReconciliation("tenant-bgft-douala", "mission-a",
                "Écart de réconciliation : 15", Instant.now().plus(2, ChronoUnit.DAYS));

        var second = service.ouvrirIncidentReconciliation("tenant-bgft-douala", "mission-a",
                "Écart de réconciliation : 15", Instant.now().plus(2, ChronoUnit.DAYS));

        assertThat(premier).isPresent();
        assertThat(second).isEmpty();
        assertThat(service.lister("tenant-bgft-douala")).hasSize(1);
    }

    @Test
    void ouvrir_un_incident_reconciliation_est_de_nouveau_possible_une_fois_le_premier_clos() {
        var premier = service.ouvrirIncidentReconciliation("tenant-bgft-douala", "mission-a",
                "Écart de réconciliation : 15", Instant.now().plus(2, ChronoUnit.DAYS));
        InMemoryConfigurationPort configurationPort = new InMemoryConfigurationPort();
        configurationPort.sauvegarder(new ConfigurationVersionnee("g-1", DecisionService.CLE_GRILLE_DECISION,
                "tenant-bgft-douala", "grille v1", "actor-admin-1", 1, Instant.now()));
        DecisionService decisionService = new DecisionService(dossierPort, journalAuditPort, dossierEventPort, configurationPort);
        decisionService.trancher(premier.get().id(), "ECART_RESOLU", "motif", "actor-admin-1");

        var second = service.ouvrirIncidentReconciliation("tenant-bgft-douala", "mission-a",
                "Écart de réconciliation : 15 (nouveau)", Instant.now().plus(2, ChronoUnit.DAYS));

        assertThat(second).isPresent();
    }
}
