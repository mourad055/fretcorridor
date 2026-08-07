package com.fretcorridor.gateway.infrastructure.exe;

import com.fretcorridor.gateway.domain.exe.EtapeEtat;
import com.fretcorridor.gateway.domain.exe.EtapeMission;
import com.fretcorridor.gateway.domain.exe.EtapeType;
import com.fretcorridor.gateway.domain.exe.ExePort;
import com.fretcorridor.gateway.domain.exe.Mission;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * TODO(mobile): remplacer par l'appel réel à service-exe une fois ce service
 * livré (Sprint 7 Mobile, @estie-glo, issue #16).
 */
@Component
public class MockExeAdapter implements ExePort {

    private final List<Mission> missions = List.of(
            new Mission("mission-a", "tenant-bgft-douala", "actor-transporteur-1", "Transport Étoile SARL",
                    "Douala", "Yaoundé", List.of(
                    new EtapeMission(1, EtapeType.ENLEVEMENT, "Douala", EtapeEtat.TERMINEE),
                    new EtapeMission(2, EtapeType.LIVRAISON, "Yaoundé", EtapeEtat.EN_COURS)
            )),
            new Mission("mission-b", "tenant-bgft-douala", "actor-transporteur-2", "Fourgon 3T — LT 5678 CD",
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
    public Flux<Mission> listerMissionsParTenant(String tenantId) {
        return Flux.fromIterable(missions).filter(m -> m.tenantId().equals(tenantId));
    }

    @Override
    public Flux<Mission> listerMissionsParTransporteur(String transporteurId) {
        return Flux.fromIterable(missions).filter(m -> m.transporteurId().equals(transporteurId));
    }
}
