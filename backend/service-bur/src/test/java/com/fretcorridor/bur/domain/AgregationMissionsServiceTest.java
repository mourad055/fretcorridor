package com.fretcorridor.bur.domain;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgregationMissionsServiceTest {

    private static class FakeRepository implements MissionRepositoryPort {
        private final AtomicLong compteur = new AtomicLong();

        @Override
        public void enregistrer(String tenantId, String axeId) {
            compteur.incrementAndGet();
        }

        @Override
        public long compter(String tenantId, String axeId) {
            return compteur.get();
        }
    }

    @Test
    void rejects_a_threshold_below_one() {
        assertThatThrownBy(() -> new AgregationMissionsService(new FakeRepository(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recording_missions_increases_the_aggregate_once_the_threshold_is_reached() {
        AgregationMissionsService service = new AgregationMissionsService(new FakeRepository(), 2);

        assertThat(service.agregatPourAxe("tenant-1", "axe-1").seuilAtteint()).isFalse();

        service.enregistrerMission("tenant-1", "axe-1");
        assertThat(service.agregatPourAxe("tenant-1", "axe-1").seuilAtteint()).isFalse();

        service.enregistrerMission("tenant-1", "axe-1");
        AgregatMissions agregat = service.agregatPourAxe("tenant-1", "axe-1");
        assertThat(agregat.seuilAtteint()).isTrue();
        assertThat(agregat.nombreMissions()).contains(2L);
    }
}
