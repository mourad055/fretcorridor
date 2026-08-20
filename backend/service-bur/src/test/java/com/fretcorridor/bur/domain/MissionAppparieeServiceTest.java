package com.fretcorridor.bur.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MissionAppparieeServiceTest {

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
    private final MissionAppparieeService service = new MissionAppparieeService(repository);

    @Test
    void lists_only_missions_of_the_requested_tenant() {
        service.ingerer(missionExemple("tenant-1"), UUID.randomUUID());
        service.ingerer(missionExemple("tenant-2"), UUID.randomUUID());

        assertThat(service.listerParTenant("tenant-1")).hasSize(1)
                .allMatch(m -> m.tenantId().equals("tenant-1"));
    }

    private MissionAppariee missionExemple(String tenantId) {
        return new MissionAppariee(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), UUID.randomUUID(),
                "Douala", "Yaoundé", BigDecimal.valueOf(50000), "XAF", Instant.now());
    }
}
