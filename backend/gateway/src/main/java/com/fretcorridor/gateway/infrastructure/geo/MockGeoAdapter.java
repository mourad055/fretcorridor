package com.fretcorridor.gateway.infrastructure.geo;

import com.fretcorridor.gateway.domain.geo.Axe;
import com.fretcorridor.gateway.domain.geo.GeoPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Reste l'implementation ACTIVE PAR DEFAUT (voir RealGeoAdapter, profil
 * "real-geo" requis pour l'activer) : service-geo reel existe mais ne filtre
 * pas encore par tenant (Phase 1, un seul tenant reel - BGFT), et
 * RealGeoAdapter compense en collant le tenantId du JWT sur des donnees non
 * filtrees plutot que de filtrer reellement - casse ENF-MUL-01 des qu'un
 * deuxieme tenant existe (cf. docs/ANALYSE_backend-stevetelecom.md §2).
 * A retirer une fois la decision d'equipe prise et RealGeoAdapter fiabilise.
 */
@Component
@org.springframework.context.annotation.Profile("!real-geo")
public class MockGeoAdapter implements GeoPort {

    private final List<Axe> axes = List.of(
            new Axe("axe-1", "tenant-bgft-douala", "Douala", "Yaoundé", 300, true, true, true),
            new Axe("axe-2", "tenant-bgft-douala", "Douala", "Bafoussam", 350, true, true, false),
            new Axe("axe-3", "tenant-bnft-ndjamena", "N'Djamena", "Moundou", 470, true, false, false),
            new Axe("axe-4", "tenant-bnft-ndjamena", "N'Djamena", "Sarh", 550, true, true, false)
    );

    @Override
    public Flux<Axe> listerAxesParTenant(String tenantId) {
        return Flux.fromIterable(axes).filter(axe -> axe.tenantId().equals(tenantId));
    }
}
