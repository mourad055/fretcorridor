package com.fretcorridor.pay.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * FE-PAY-01/02/04 : orchestration du grand livre miroir. Domaine pur, sans
 * dépendance à Spring — testable sans mock de framework (PRD §8.1).
 */
public class GrandLivreService {

    private final GrandLivrePort grandLivrePort;

    public GrandLivreService(GrandLivrePort grandLivrePort) {
        this.grandLivrePort = grandLivrePort;
    }

    public EcritureMiroir enregistrerEncaissement(String tenantId, String missionId, BigDecimal montant, String referencePrestataire) {
        EcritureMiroir ecriture = new EcritureMiroir(
                UUID.randomUUID().toString(), tenantId, missionId, TypeCompte.COMPTE_SEQUESTRE_PRESTATAIRE, null,
                SensEcriture.CREDIT, NatureEcriture.ENCAISSEMENT, montant, referencePrestataire,
                Instant.now(), StatutEcriture.VALIDE
        );
        grandLivrePort.enregistrer(ecriture);
        return ecriture;
    }

    /** ENF-FIN-02 : refuse tout reversement dont le montant cumulé dépasse l'encaissement validé de la mission. */
    public EcritureMiroir enregistrerReversement(String tenantId, String missionId, String transporteurId, BigDecimal montant, String referencePrestataire) {
        BigDecimal totalEncaisse = totalParNature(missionId, NatureEcriture.ENCAISSEMENT);
        BigDecimal totalDejaReverse = totalParNature(missionId, NatureEcriture.REVERSEMENT);

        if (totalEncaisse.subtract(totalDejaReverse).compareTo(montant) < 0) {
            throw new ReversementSansEncaissementException(missionId);
        }

        EcritureMiroir ecriture = new EcritureMiroir(
                UUID.randomUUID().toString(), tenantId, missionId, TypeCompte.COMPTE_TRANSPORTEUR, transporteurId,
                SensEcriture.DEBIT, NatureEcriture.REVERSEMENT, montant, referencePrestataire,
                Instant.now(), StatutEcriture.VALIDE
        );
        grandLivrePort.enregistrer(ecriture);
        return ecriture;
    }

    public List<EcritureMiroir> ecrituresDuBeneficiaire(String beneficiaireId) {
        return grandLivrePort.parBeneficiaire(beneficiaireId);
    }

    public List<EcritureMiroir> ecrituresDuTenant(String tenantId) {
        return grandLivrePort.parTenant(tenantId);
    }

    private BigDecimal totalParNature(String missionId, NatureEcriture nature) {
        List<EcritureMiroir> ecritures = grandLivrePort.parMission(missionId);
        return ecritures.stream()
                .filter(e -> e.nature() == nature && e.statut() == StatutEcriture.VALIDE)
                .map(EcritureMiroir::montant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
