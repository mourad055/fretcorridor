package com.fretcorridor.bur.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** EF-BUR-03, UC-BUR-02 : indicateurs de marché — volumes, prix (médiane/dispersion), déséquilibre directionnel. */
class ObservatoireServiceTest {

    private static class FakeRepository implements MissionAppparieeRepositoryPort {
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

    private static class FakeEstimationMarcheAxePort implements EstimationMarcheAxePort {
        private final Map<String, EstimationMarcheAxe> estimations = new HashMap<>();

        @Override
        public void definir(EstimationMarcheAxe estimation) {
            estimations.put(estimation.tenantId() + ":" + estimation.axeId(), estimation);
        }

        @Override
        public Optional<EstimationMarcheAxe> pour(String tenantId, UUID axeId) {
            return Optional.ofNullable(estimations.get(tenantId + ":" + axeId));
        }
    }

    private final FakeRepository repository = new FakeRepository();
    private final FakeEstimationMarcheAxePort estimationPort = new FakeEstimationMarcheAxePort();
    private static final String TENANT = "tenant-bgft-douala";
    private static final Instant MAINTENANT = Instant.parse("2026-06-15T10:00:00Z");

    @Test
    void rejects_a_threshold_below_one() {
        assertThatThrownBy(() -> new ObservatoireService(repository, estimationPort, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** RG-085 : aucun indicateur n'est exposé en dessous du seuil d'agrégation. */
    @Test
    void hides_every_indicator_below_the_aggregation_threshold() {
        ObservatoireService service = new ObservatoireService(repository, estimationPort, 3);
        UUID axeId = UUID.randomUUID();
        ajouterMission(axeId, "Douala", "Yaoundé", "10000", MAINTENANT);
        ajouterMission(axeId, "Douala", "Yaoundé", "20000", MAINTENANT);

        ObservatoireAxe observatoire = service.indicateursPourAxe(TENANT, axeId, MAINTENANT);

        assertThat(observatoire.seuilAtteint()).isFalse();
        assertThat(observatoire.nombreMissions()).isEmpty();
        assertThat(observatoire.prixMediane()).isEmpty();
        assertThat(observatoire.prixDispersion()).isEmpty();
        assertThat(observatoire.tauxDesequilibreDirectionnel()).isEmpty();
        assertThat(observatoire.couverturePourcentage()).isEmpty();
    }

    @Test
    void computes_median_and_interquartile_dispersion_once_the_threshold_is_reached() {
        ObservatoireService service = new ObservatoireService(repository, estimationPort, 3);
        UUID axeId = UUID.randomUUID();
        for (String prix : List.of("10000", "20000", "30000", "40000", "50000")) {
            ajouterMission(axeId, "Douala", "Yaoundé", prix, MAINTENANT);
        }

        ObservatoireAxe observatoire = service.indicateursPourAxe(TENANT, axeId, MAINTENANT);

        assertThat(observatoire.seuilAtteint()).isTrue();
        assertThat(observatoire.nombreMissions()).contains(5L);
        assertThat(observatoire.prixMediane()).hasValueSatisfying(m -> assertThat(m).isEqualByComparingTo("30000"));
        assertThat(observatoire.prixDispersion()).hasValueSatisfying(d -> assertThat(d).isEqualByComparingTo("20000"));
        assertThat(observatoire.devise()).contains("XAF");
    }

    @Test
    void a_perfectly_balanced_axe_has_a_directional_imbalance_of_one_half() {
        ObservatoireService service = new ObservatoireService(repository, estimationPort, 4);
        UUID axeId = UUID.randomUUID();
        ajouterMission(axeId, "Douala", "Yaoundé", "10000", MAINTENANT);
        ajouterMission(axeId, "Douala", "Yaoundé", "10000", MAINTENANT);
        ajouterMission(axeId, "Yaoundé", "Douala", "10000", MAINTENANT);
        ajouterMission(axeId, "Yaoundé", "Douala", "10000", MAINTENANT);

        ObservatoireAxe observatoire = service.indicateursPourAxe(TENANT, axeId, MAINTENANT);

        assertThat(observatoire.tauxDesequilibreDirectionnel()).hasValueSatisfying(t -> assertThat(t).isCloseTo(0.5, within(0.001)));
    }

    @Test
    void an_axe_with_traffic_mostly_in_one_direction_has_a_high_imbalance() {
        ObservatoireService service = new ObservatoireService(repository, estimationPort, 4);
        UUID axeId = UUID.randomUUID();
        ajouterMission(axeId, "Douala", "Yaoundé", "10000", MAINTENANT);
        ajouterMission(axeId, "Douala", "Yaoundé", "10000", MAINTENANT);
        ajouterMission(axeId, "Douala", "Yaoundé", "10000", MAINTENANT);
        ajouterMission(axeId, "Yaoundé", "Douala", "10000", MAINTENANT);

        ObservatoireAxe observatoire = service.indicateursPourAxe(TENANT, axeId, MAINTENANT);

        assertThat(observatoire.tauxDesequilibreDirectionnel()).hasValueSatisfying(t -> assertThat(t).isCloseTo(0.75, within(0.001)));
    }

    @Test
    void missions_of_another_axe_never_pollute_the_indicators() {
        ObservatoireService service = new ObservatoireService(repository, estimationPort, 2);
        UUID axeId = UUID.randomUUID();
        UUID autreAxeId = UUID.randomUUID();
        ajouterMission(axeId, "Douala", "Yaoundé", "10000", MAINTENANT);
        ajouterMission(axeId, "Douala", "Yaoundé", "20000", MAINTENANT);
        ajouterMission(autreAxeId, "Douala", "Bafoussam", "999999", MAINTENANT);

        ObservatoireAxe observatoire = service.indicateursPourAxe(TENANT, axeId, MAINTENANT);

        assertThat(observatoire.nombreMissions()).contains(2L);
        assertThat(observatoire.prixMediane()).hasValueSatisfying(m -> assertThat(m).isLessThan(new BigDecimal("999999")));
    }

    /** EF-BUR-05, RG-087 : sans estimation déclarée, aucune couverture n'est calculée — jamais déduite silencieusement. */
    @Test
    void coverage_is_absent_when_no_market_estimation_has_been_declared() {
        ObservatoireService service = new ObservatoireService(repository, estimationPort, 2);
        UUID axeId = UUID.randomUUID();
        ajouterMission(axeId, "Douala", "Yaoundé", "10000", MAINTENANT);
        ajouterMission(axeId, "Douala", "Yaoundé", "20000", MAINTENANT);

        ObservatoireAxe observatoire = service.indicateursPourAxe(TENANT, axeId, MAINTENANT);

        assertThat(observatoire.couverturePourcentage()).isEmpty();
        assertThat(observatoire.estimationDefinieLe()).isEmpty();
    }

    @Test
    void coverage_percentage_is_computed_against_the_declared_monthly_estimation() {
        ObservatoireService service = new ObservatoireService(repository, estimationPort, 2);
        UUID axeId = UUID.randomUUID();
        ajouterMission(axeId, "Douala", "Yaoundé", "10000", MAINTENANT);
        ajouterMission(axeId, "Douala", "Yaoundé", "20000", MAINTENANT);
        Instant definieLe = MAINTENANT.minus(5, ChronoUnit.DAYS);
        service.definirEstimationMarche(TENANT, axeId, new BigDecimal("16"), "enquête terrain", "actor-bureau-1", definieLe);

        ObservatoireAxe observatoire = service.indicateursPourAxe(TENANT, axeId, MAINTENANT);

        // 2 missions récentes / 16 estimées = 12.5%
        assertThat(observatoire.couverturePourcentage()).hasValueSatisfying(c -> assertThat(c).isEqualByComparingTo("12.50"));
        assertThat(observatoire.estimationDefinieLe()).contains(definieLe);
    }

    /** EF-BUR-05 : sans notion de période sélectionnée par l'analyste, seule une fenêtre glissante rend la couverture interprétable. */
    @Test
    void coverage_only_counts_missions_within_the_thirty_day_rolling_window() {
        ObservatoireService service = new ObservatoireService(repository, estimationPort, 2);
        UUID axeId = UUID.randomUUID();
        ajouterMission(axeId, "Douala", "Yaoundé", "10000", MAINTENANT.minus(29, ChronoUnit.DAYS));
        ajouterMission(axeId, "Douala", "Yaoundé", "20000", MAINTENANT.minus(45, ChronoUnit.DAYS));
        service.definirEstimationMarche(TENANT, axeId, new BigDecimal("10"), "enquête terrain", "actor-bureau-1", MAINTENANT);

        ObservatoireAxe observatoire = service.indicateursPourAxe(TENANT, axeId, MAINTENANT);

        // une seule des deux missions est dans la fenêtre de 30 jours : 1/10 = 10%
        assertThat(observatoire.couverturePourcentage()).hasValueSatisfying(c -> assertThat(c).isEqualByComparingTo("10.00"));
    }

    /** RG-085 : une estimation déclarée ne contourne jamais le seuil d'agrégation. */
    @Test
    void a_declared_estimation_never_bypasses_the_aggregation_threshold() {
        ObservatoireService service = new ObservatoireService(repository, estimationPort, 3);
        UUID axeId = UUID.randomUUID();
        ajouterMission(axeId, "Douala", "Yaoundé", "10000", MAINTENANT);
        service.definirEstimationMarche(TENANT, axeId, new BigDecimal("10"), "enquête terrain", "actor-bureau-1", MAINTENANT);

        ObservatoireAxe observatoire = service.indicateursPourAxe(TENANT, axeId, MAINTENANT);

        assertThat(observatoire.seuilAtteint()).isFalse();
        assertThat(observatoire.couverturePourcentage()).isEmpty();
    }

    @Test
    void rejects_a_non_positive_monthly_estimation() {
        ObservatoireService service = new ObservatoireService(repository, estimationPort, 2);
        UUID axeId = UUID.randomUUID();

        assertThatThrownBy(() -> service.definirEstimationMarche(TENANT, axeId, BigDecimal.ZERO, "enquête terrain",
                "actor-bureau-1", MAINTENANT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Missions ingérées sans prix (Kafka / mobile) ne doivent jamais faire planter la médiane. */
    @Test
    void missions_sans_prix_n_empechent_pas_le_calcul_des_indicateurs() {
        ObservatoireService service = new ObservatoireService(repository, estimationPort, 3);
        UUID axeId = UUID.randomUUID();
        ajouterMission(axeId, "Douala", "Yaoundé", "100000", MAINTENANT);
        ajouterMission(axeId, "Douala", "Yaoundé", "120000", MAINTENANT);
        ajouterMission(axeId, "Douala", "Yaoundé", "140000", MAINTENANT);
        repository.enregistrer(new MissionAppariee(UUID.randomUUID(), TENANT, axeId, UUID.randomUUID(),
                "Douala", "Yaoundé", null, null, MAINTENANT), UUID.randomUUID());

        ObservatoireAxe observatoire = service.indicateursPourAxe(TENANT, axeId, MAINTENANT);

        assertThat(observatoire.seuilAtteint()).isTrue();
        assertThat(observatoire.nombreMissions()).contains(4L);
        assertThat(observatoire.prixMediane()).hasValueSatisfying(m -> assertThat(m).isEqualByComparingTo("120000"));
    }

    private void ajouterMission(UUID axeId, String origine, String destination, String prix, Instant confirmeeLe) {
        repository.enregistrer(new MissionAppariee(UUID.randomUUID(), TENANT, axeId, UUID.randomUUID(),
                origine, destination, new BigDecimal(prix), "XAF", confirmeeLe), UUID.randomUUID());
    }
}
