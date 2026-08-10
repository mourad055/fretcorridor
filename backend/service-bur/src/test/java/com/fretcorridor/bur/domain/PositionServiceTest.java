package com.fretcorridor.bur.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PositionServiceTest {

    private static class FakeRepository implements PositionRepositoryPort {
        private final List<PositionVehicule> positions = new ArrayList<>();

        @Override
        public void enregistrerSiPlusRecente(PositionVehicule position) {
            positions.removeIf(p -> p.missionId().equals(position.missionId())
                    && !position.capturedLe().isAfter(p.capturedLe()));
            if (positions.stream().noneMatch(p -> p.missionId().equals(position.missionId()))) {
                positions.add(position);
            }
        }

        @Override
        public List<PositionVehicule> listerParTenant(String tenantId) {
            return positions.stream().filter(p -> p.tenantId().equals(tenantId)).toList();
        }
    }

    private final FakeRepository repository = new FakeRepository();
    private final PositionService service = new PositionService(repository);

    @Test
    void lists_only_positions_of_the_requested_tenant() {
        service.ingerer(positionExemple("tenant-1", Instant.now()));
        service.ingerer(positionExemple("tenant-2", Instant.now()));

        assertThat(service.listerParTenant("tenant-1")).hasSize(1)
                .allMatch(p -> p.tenantId().equals("tenant-1"));
    }

    private PositionVehicule positionExemple(String tenantId, Instant capturedLe) {
        return new PositionVehicule(UUID.randomUUID(), tenantId, UUID.randomUUID(), 4.05, 9.76, capturedLe);
    }
}
