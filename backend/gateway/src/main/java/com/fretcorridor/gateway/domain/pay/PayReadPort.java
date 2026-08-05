package com.fretcorridor.gateway.domain.pay;

import reactor.core.publisher.Flux;

/**
 * Port hexagonal vers service-pay — contrairement aux autres périmètres
 * consommés cette Phase 1 (GEO, OPT, TRK, EXE), service-pay appartient à ce
 * même périmètre et est réellement implémenté : cet adaptateur appelle le
 * service réel (WebClient), ce n'est pas un mock.
 */
public interface PayReadPort {

    Flux<EcritureVue> ecrituresDuTransporteur(String transporteurId);

    Flux<EcritureVue> rapportDuTenant(String tenantId);
}
