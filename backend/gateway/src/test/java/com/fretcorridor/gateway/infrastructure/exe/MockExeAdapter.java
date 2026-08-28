package com.fretcorridor.gateway.infrastructure.exe;

import com.fretcorridor.gateway.domain.exe.EtapeEtat;
import com.fretcorridor.gateway.domain.exe.EtapeMission;
import com.fretcorridor.gateway.domain.exe.EtapeType;
import com.fretcorridor.gateway.domain.exe.ExePort;
import com.fretcorridor.gateway.domain.exe.Mission;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/** Fixture @SpringBootTest — remplace RealExeAdapter pendant les tests unitaires d'intégration gateway. */
@Component
@Primary
public class MockExeAdapter implements ExePort {

    private final List<Mission> missions = List.of(
            new Mission("mission-a", "tenant-bgft-douala", "a0000000-0000-0000-0000-000000000002", "Transport Étoile SARL",
                    "Douala", "Yaoundé", List.of(
                    new EtapeMission(1, EtapeType.ENLEVEMENT, "Douala", EtapeEtat.TERMINEE),
                    new EtapeMission(2, EtapeType.LIVRAISON, "Yaoundé", EtapeEtat.EN_COURS)
            )),
            new Mission("mission-b", "tenant-bgft-douala", "a0000000-0000-0000-0000-000000000005", "Fourgon 3T — LT 5678 CD",
                    "Douala", "Bafoussam", List.of(
                    new EtapeMission(1, EtapeType.ENLEVEMENT, "Douala", EtapeEtat.A_VENIR),
                    new EtapeMission(2, EtapeType.LIVRAISON, "Bafoussam", EtapeEtat.A_VENIR)
            )),
            new Mission("mission-c", "tenant-bnft-ndjamena", "actor-transporteur-tchad-1", "Transporteur Sahel",
                    "N'Djamena", "Moundou", List.of(
                    new EtapeMission(1, EtapeType.ENLEVEMENT, "N'Djamena", EtapeEtat.TERMINEE),
                    new EtapeMission(2, EtapeType.LIVRAISON, "Moundou", EtapeEtat.TERMINEE)
            )),
            new Mission("mission-d", "tenant-bnft-ndjamena", "actor-transporteur-tchad-2", "Logistique Sahel Tchad",
                    "N'Djamena", "Sarh", List.of(
                    new EtapeMission(1, EtapeType.ENLEVEMENT, "N'Djamena", EtapeEtat.EN_COURS),
                    new EtapeMission(2, EtapeType.LIVRAISON, "Sarh", EtapeEtat.A_VENIR)
            ))
    );

    @Override
    public Flux<Mission> listerMissionsParTenant(String tenantId, String delegationToken) {
        if (delegationToken == null) {
            return Flux.empty();
        }
        return Flux.fromIterable(missions).filter(m -> m.tenantId().equals(tenantId));
    }

    @Override
    public Flux<Mission> listerMissionsParTransporteur(String tenantId, String transporteurId, String delegationToken) {
        if (delegationToken == null) {
            return Flux.empty();
        }
        return Flux.fromIterable(missions).filter(m -> m.transporteurId().equals(transporteurId));
    }
}
