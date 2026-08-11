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
    private final GarantiePort garantiePort;

    public GrandLivreService(GrandLivrePort grandLivrePort, GarantiePort garantiePort) {
        this.grandLivrePort = grandLivrePort;
        this.garantiePort = garantiePort;
    }

    public EcritureMiroir enregistrerEncaissement(String tenantId, String missionId, BigDecimal montant, String referencePrestataire, ModePaiement modePaiement) {
        EcritureMiroir ecriture = new EcritureMiroir(
                UUID.randomUUID().toString(), tenantId, missionId, TypeCompte.COMPTE_SEQUESTRE_PRESTATAIRE, null,
                SensEcriture.CREDIT, NatureEcriture.ENCAISSEMENT, modePaiement, montant, referencePrestataire,
                Instant.now(), StatutEcriture.VALIDE
        );
        grandLivrePort.enregistrer(ecriture);
        return ecriture;
    }

    /**
     * ENF-FIN-02 : refuse tout reversement dont le montant cumulé dépasse
     * l'encaissement validé de la mission. EF-PAY-06 (terme contractuel) :
     * une garantie tierce active couvre le même rôle qu'un encaissement réel
     * pour cette vérification — RG-075 reste vérifié par construction, le
     * risque de crédit restant porté par le garant (jamais FretCorridor).
     */
    public EcritureMiroir enregistrerReversement(String tenantId, String missionId, String transporteurId, BigDecimal montant, String referencePrestataire) {
        BigDecimal totalEncaisse = totalParNature(missionId, NatureEcriture.ENCAISSEMENT);
        BigDecimal totalGaranti = garantiePort.parMission(missionId).map(Garantie::montant).orElse(BigDecimal.ZERO);
        BigDecimal totalDejaReverse = totalParNature(missionId, NatureEcriture.REVERSEMENT);

        if (totalEncaisse.add(totalGaranti).subtract(totalDejaReverse).compareTo(montant) < 0) {
            throw new ReversementSansEncaissementException(missionId);
        }

        EcritureMiroir ecriture = new EcritureMiroir(
                UUID.randomUUID().toString(), tenantId, missionId, TypeCompte.COMPTE_TRANSPORTEUR, transporteurId,
                SensEcriture.DEBIT, NatureEcriture.REVERSEMENT, null, montant, referencePrestataire,
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
