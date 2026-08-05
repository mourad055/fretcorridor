package com.fretcorridor.pay.domain;

import java.util.List;

public interface GrandLivrePort {

    void enregistrer(EcritureMiroir ecriture);

    List<EcritureMiroir> parMission(String missionId);

    /** FE-TRP-03 : le grand livre d'un transporteur, strictement ses propres écritures (PRD §5.3). */
    List<EcritureMiroir> parBeneficiaire(String beneficiaireId);

    /** Rapport financier Bureau/Admin : strictement le territoire du tenant (ENF-MUL-01). */
    List<EcritureMiroir> parTenant(String tenantId);

    /** ENF-FIN-03 : isole une écriture après détection d'un écart de réconciliation. */
    void suspendre(String ecritureId);
}
