package com.fretcorridor.bur.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** EF-BUR-07 (S) : configuration d'alertes sur seuils, évaluées à la demande contre l'observatoire (EF-BUR-03). */
class AlerteSeuilServiceTest {

    private static class FakeAlerteSeuilPort implements AlerteSeuilPort {
        private final List<AlerteSeuil> alertes = new ArrayList<>();

        @Override
        public void sauvegarder(AlerteSeuil alerte) {
            alertes.add(alerte);
        }

        @Override
        public List<AlerteSeuil> listerParTenant(String tenantId) {
            return alertes.stream().filter(a -> a.tenantId().equals(tenantId)).toList();
        }

        @Override
        public void supprimer(String id, String tenantId) {
            alertes.removeIf(a -> a.id().equals(id) && a.tenantId().equals(tenantId));
        }
    }

    private static class FakeMissionRepository implements MissionAppparieeRepositoryPort {
        private final List<MissionAppariee> missions = new ArrayList<>();

        @Override
        public void enregistrer(MissionAppariee mission, UUID eventId) {
            missions.add(mission);
        }

        @Override
        public List<MissionAppariee> listerParTenant(String tenantId) {
            return missions.stream().filter(m -> m.tenantId().equals(tenantId)).toList();
        }
    }

    private static final String TENANT = "tenant-bgft-douala";
    private final FakeAlerteSeuilPort alerteSeuilPort = new FakeAlerteSeuilPort();
    private final FakeMissionRepository missionRepository = new FakeMissionRepository();
    private final ObservatoireService observatoireService = new ObservatoireService(missionRepository, 3);
    private final AlerteSeuilService service = new AlerteSeuilService(alerteSeuilPort, observatoireService);

    @Test
    void configurer_une_alerte_la_persiste_et_la_liste() {
        UUID axeId = UUID.randomUUID();

        AlerteSeuil alerte = service.configurer(TENANT, axeId, IndicateurObservatoire.PRIX_MEDIANE,
                Comparateur.SUPERIEUR, new BigDecimal("25000"), "actor-bureau-1");

        assertThat(service.lister(TENANT)).containsExactly(alerte);
        assertThat(alerte.creeParActeurId()).isEqualTo("actor-bureau-1");
    }

    @Test
    void supprimer_une_alerte_la_retire_de_la_liste() {
        UUID axeId = UUID.randomUUID();
        AlerteSeuil alerte = service.configurer(TENANT, axeId, IndicateurObservatoire.NOMBRE_MISSIONS,
                Comparateur.INFERIEUR, new BigDecimal("5"), "actor-bureau-1");

        service.supprimer(alerte.id(), TENANT);

        assertThat(service.lister(TENANT)).isEmpty();
    }

    /** RG-085 : une alerte ne peut jamais se déclencher sur une donnée masquée par le seuil d'agrégation. */
    @Test
    void une_alerte_sur_un_axe_sous_le_seuil_d_agregation_n_est_pas_evaluable() {
        UUID axeId = UUID.randomUUID();
        ajouterMission(axeId, "10000");
        service.configurer(TENANT, axeId, IndicateurObservatoire.PRIX_MEDIANE, Comparateur.SUPERIEUR,
                new BigDecimal("5000"), "actor-bureau-1");

        List<EtatAlerte> etats = service.evaluer(TENANT);

        assertThat(etats).hasSize(1);
        assertThat(etats.get(0).evaluable()).isFalse();
        assertThat(etats.get(0).declenchee()).isFalse();
        assertThat(etats.get(0).valeurActuelle()).isNull();
    }

    @Test
    void une_alerte_prix_median_superieur_au_seuil_se_declenche() {
        UUID axeId = UUID.randomUUID();
        ajouterMission(axeId, "10000");
        ajouterMission(axeId, "20000");
        ajouterMission(axeId, "30000");
        service.configurer(TENANT, axeId, IndicateurObservatoire.PRIX_MEDIANE, Comparateur.SUPERIEUR,
                new BigDecimal("15000"), "actor-bureau-1");

        EtatAlerte etat = service.evaluer(TENANT).get(0);

        assertThat(etat.evaluable()).isTrue();
        assertThat(etat.declenchee()).isTrue();
        assertThat(etat.valeurActuelle()).isEqualByComparingTo("20000");
    }

    @Test
    void une_alerte_prix_median_sous_le_seuil_ne_se_declenche_pas() {
        UUID axeId = UUID.randomUUID();
        ajouterMission(axeId, "10000");
        ajouterMission(axeId, "20000");
        ajouterMission(axeId, "30000");
        service.configurer(TENANT, axeId, IndicateurObservatoire.PRIX_MEDIANE, Comparateur.SUPERIEUR,
                new BigDecimal("50000"), "actor-bureau-1");

        EtatAlerte etat = service.evaluer(TENANT).get(0);

        assertThat(etat.evaluable()).isTrue();
        assertThat(etat.declenchee()).isFalse();
    }

    @Test
    void une_alerte_nombre_missions_inferieur_au_seuil_se_declenche() {
        UUID axeId = UUID.randomUUID();
        ajouterMission(axeId, "10000");
        ajouterMission(axeId, "20000");
        ajouterMission(axeId, "30000");
        service.configurer(TENANT, axeId, IndicateurObservatoire.NOMBRE_MISSIONS, Comparateur.INFERIEUR,
                new BigDecimal("10"), "actor-bureau-1");

        EtatAlerte etat = service.evaluer(TENANT).get(0);

        assertThat(etat.evaluable()).isTrue();
        assertThat(etat.declenchee()).isTrue();
        assertThat(etat.valeurActuelle()).isEqualByComparingTo("3");
    }

    private void ajouterMission(UUID axeId, String prix) {
        missionRepository.enregistrer(new MissionAppariee(UUID.randomUUID(), TENANT, axeId, UUID.randomUUID(),
                "Douala", "Yaoundé", new BigDecimal(prix), "XAF", Instant.now()), UUID.randomUUID());
    }
}
