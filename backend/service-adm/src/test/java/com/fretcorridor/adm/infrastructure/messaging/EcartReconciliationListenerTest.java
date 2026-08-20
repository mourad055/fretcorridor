package com.fretcorridor.adm.infrastructure.messaging;

import com.fretcorridor.adm.domain.FileTravailService;
import com.fretcorridor.adm.domain.InMemoryDossierEventPort;
import com.fretcorridor.adm.domain.InMemoryDossierPort;
import com.fretcorridor.adm.domain.InMemoryJournalAuditPort;
import com.fretcorridor.adm.domain.PrioriteDossier;
import com.fretcorridor.adm.domain.TypeDossier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** EF-PAY-09, ENF-FIN-03 : consomme EcartReconciliation (service-pay) pour ouvrir un incident dans la file de travail. */
class EcartReconciliationListenerTest {

    private final InMemoryDossierPort dossierPort = new InMemoryDossierPort();
    private final FileTravailService fileTravailService =
            new FileTravailService(dossierPort, new InMemoryJournalAuditPort(), new InMemoryDossierEventPort());
    private final EcartReconciliationListener listener = new EcartReconciliationListener(fileTravailService, 48);

    @Test
    void ingerer_ouvre_un_incident_priorite_haute_pour_la_mission_en_ecart() {
        listener.ingerer(new EcartReconciliationEvent("event-1", "mission-a", "tenant-bgft-douala",
                new BigDecimal("15"), Instant.now()));

        var dossiers = fileTravailService.lister("tenant-bgft-douala");
        assertThat(dossiers).hasSize(1);
        assertThat(dossiers.get(0).type()).isEqualTo(TypeDossier.INCIDENT);
        assertThat(dossiers.get(0).priorite()).isEqualTo(PrioriteDossier.HAUTE);
        assertThat(dossiers.get(0).missionId()).isEqualTo("mission-a");
    }

    /** Le balayage quotidien de service-pay republie l'événement chaque jour tant que l'écart n'est pas résolu. */
    @Test
    void ingerer_le_meme_ecart_deux_fois_ne_duplique_pas_l_incident() {
        listener.ingerer(new EcartReconciliationEvent("event-1", "mission-a", "tenant-bgft-douala",
                new BigDecimal("15"), Instant.now()));
        listener.ingerer(new EcartReconciliationEvent("event-2", "mission-a", "tenant-bgft-douala",
                new BigDecimal("15"), Instant.now()));

        assertThat(fileTravailService.lister("tenant-bgft-douala")).hasSize(1);
    }
}
