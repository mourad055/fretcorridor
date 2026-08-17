package com.fretcorridor.bur.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

    private final FakeRepository repository = new FakeRepository();
    private static final String TENANT = "tenant-bgft-douala";

    @Test
    void rejects_a_threshold_below_one() {
        assertThatThrownBy(() -> new ObservatoireService(repository, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** RG-085 : aucun indicateur n'est exposé en dessous du seuil d'agrégation. */
    @Test
    void hides_every_indicator_below_the_aggregation_threshold() {
        ObservatoireService service = new ObservatoireService(repository, 3);
        UUID axeId = UUID.randomUUID();
        ajouterMission(axeId, "Douala", "Yaoundé", "10000");
        ajouterMission(axeId, "Douala", "Yaoundé", "20000");

        ObservatoireAxe observatoire = service.indicateursPourAxe(TENANT, axeId);

        assertThat(observatoire.seuilAtteint()).isFalse();
        assertThat(observatoire.nombreMissions()).isEmpty();
        assertThat(observatoire.prixMediane()).isEmpty();
        assertThat(observatoire.prixDispersion()).isEmpty();
        assertThat(observatoire.tauxDesequilibreDirectionnel()).isEmpty();
    }

    @Test
    void computes_median_and_interquartile_dispersion_once_the_threshold_is_reached() {
        ObservatoireService service = new ObservatoireService(repository, 3);
        UUID axeId = UUID.randomUUID();
        for (String prix : List.of("10000", "20000", "30000", "40000", "50000")) {
            ajouterMission(axeId, "Douala", "Yaoundé", prix);
        }

        ObservatoireAxe observatoire = service.indicateursPourAxe(TENANT, axeId);

        assertThat(observatoire.seuilAtteint()).isTrue();
        assertThat(observatoire.nombreMissions()).contains(5L);
        assertThat(observatoire.prixMediane()).hasValueSatisfying(m -> assertThat(m).isEqualByComparingTo("30000"));
        assertThat(observatoire.prixDispersion()).hasValueSatisfying(d -> assertThat(d).isEqualByComparingTo("20000"));
        assertThat(observatoire.devise()).contains("XAF");
    }

    @Test
    void a_perfectly_balanced_axe_has_a_directional_imbalance_of_one_half() {
        ObservatoireService service = new ObservatoireService(repository, 4);
        UUID axeId = UUID.randomUUID();
        ajouterMission(axeId, "Douala", "Yaoundé", "10000");
        ajouterMission(axeId, "Douala", "Yaoundé", "10000");
        ajouterMission(axeId, "Yaoundé", "Douala", "10000");
        ajouterMission(axeId, "Yaoundé", "Douala", "10000");

        ObservatoireAxe observatoire = service.indicateursPourAxe(TENANT, axeId);

        assertThat(observatoire.tauxDesequilibreDirectionnel()).hasValueSatisfying(t -> assertThat(t).isCloseTo(0.5, within(0.001)));
    }

    @Test
    void an_axe_with_traffic_mostly_in_one_direction_has_a_high_imbalance() {
        ObservatoireService service = new ObservatoireService(repository, 4);
        UUID axeId = UUID.randomUUID();
        ajouterMission(axeId, "Douala", "Yaoundé", "10000");
        ajouterMission(axeId, "Douala", "Yaoundé", "10000");
        ajouterMission(axeId, "Douala", "Yaoundé", "10000");
        ajouterMission(axeId, "Yaoundé", "Douala", "10000");

        ObservatoireAxe observatoire = service.indicateursPourAxe(TENANT, axeId);

        assertThat(observatoire.tauxDesequilibreDirectionnel()).hasValueSatisfying(t -> assertThat(t).isCloseTo(0.75, within(0.001)));
    }

    @Test
    void missions_of_another_axe_never_pollute_the_indicators() {
        ObservatoireService service = new ObservatoireService(repository, 2);
        UUID axeId = UUID.randomUUID();
        UUID autreAxeId = UUID.randomUUID();
        ajouterMission(axeId, "Douala", "Yaoundé", "10000");
        ajouterMission(axeId, "Douala", "Yaoundé", "20000");
        ajouterMission(autreAxeId, "Douala", "Bafoussam", "999999");

        ObservatoireAxe observatoire = service.indicateursPourAxe(TENANT, axeId);

        assertThat(observatoire.nombreMissions()).contains(2L);
        assertThat(observatoire.prixMediane()).hasValueSatisfying(m -> assertThat(m).isLessThan(new BigDecimal("999999")));
    }

    private void ajouterMission(UUID axeId, String origine, String destination, String prix) {
        repository.enregistrer(new MissionAppariee(UUID.randomUUID(), TENANT, axeId, UUID.randomUUID(),
                origine, destination, new BigDecimal(prix), "XAF", Instant.now()), UUID.randomUUID());
    }
}
