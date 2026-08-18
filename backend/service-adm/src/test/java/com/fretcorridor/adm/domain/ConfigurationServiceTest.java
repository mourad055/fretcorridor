package com.fretcorridor.adm.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationServiceTest {

    private final InMemoryConfigurationPort configurationPort = new InMemoryConfigurationPort();
    private final InMemoryJournalAuditPort journalAuditPort = new InMemoryJournalAuditPort();
    private final ConfigurationService service = new ConfigurationService(configurationPort, journalAuditPort);

    @Test
    void chaque_redefinition_cree_une_nouvelle_version_jamais_une_modification_en_place() {
        service.definir("seuil-agregation-bur", ConfigurationVersionnee.PERIMETRE_GLOBAL, "3", "actor-admin-1");
        service.definir("seuil-agregation-bur", ConfigurationVersionnee.PERIMETRE_GLOBAL, "5", "actor-admin-1");

        var historique = service.historique("seuil-agregation-bur", ConfigurationVersionnee.PERIMETRE_GLOBAL);

        assertThat(historique).hasSize(2);
        assertThat(historique.get(0).version()).isEqualTo(1);
        assertThat(historique.get(1).version()).isEqualTo(2);
        assertThat(service.valeurCourante("seuil-agregation-bur", ConfigurationVersionnee.PERIMETRE_GLOBAL))
                .hasValueSatisfying(c -> assertThat(c.valeur()).isEqualTo("5"));
    }

    @Test
    void definir_une_configuration_journalise_l_action() {
        service.definir("seuil-agregation-bur", ConfigurationVersionnee.PERIMETRE_GLOBAL, "3", "actor-admin-1");

        assertThat(journalAuditPort.lister(null)).anyMatch(e -> e.action().equals("CONFIGURATION_MODIFIEE"));
    }

    /** EF-ADM-06 : le catalogue expose une entrée par clé, avec sa valeur courante — pas tout l'historique. */
    @Test
    void le_catalogue_expose_une_seule_entree_par_cle_avec_la_valeur_courante() {
        service.definir("seuil-agregation-bur", ConfigurationVersionnee.PERIMETRE_GLOBAL, "3", "actor-admin-1");
        service.definir("seuil-agregation-bur", ConfigurationVersionnee.PERIMETRE_GLOBAL, "5", "actor-admin-1");
        service.definir("grille-decision", ConfigurationVersionnee.PERIMETRE_GLOBAL, "1", "actor-admin-2");

        var catalogue = service.catalogue();

        assertThat(catalogue).hasSize(2);
        assertThat(catalogue).filteredOn(c -> c.cle().equals("seuil-agregation-bur"))
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.valeur()).isEqualTo("5");
                    assertThat(c.version()).isEqualTo(2);
                });
    }

    @Test
    void le_catalogue_est_vide_quand_aucune_configuration_n_a_ete_definie() {
        assertThat(service.catalogue()).isEmpty();
    }
}
