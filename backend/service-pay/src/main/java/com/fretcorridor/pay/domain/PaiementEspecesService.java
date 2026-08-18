package com.fretcorridor.pay.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * EF-PAY-07 (S) : déclaration du paiement en espèces d'une mission (CDC
 * §7.6, UC-PAY-01 A3). Mode dégradé sans séquestre ni garantie — l'absence
 * de protection de la plateforme doit être signalée explicitement à
 * l'affichage (cf. {@link com.fretcorridor.pay.infrastructure.rest.dto.DeclarationEspecesResponse}).
 */
public class PaiementEspecesService {

    private final DeclarationEspecesPort declarationEspecesPort;

    public PaiementEspecesService(DeclarationEspecesPort declarationEspecesPort) {
        this.declarationEspecesPort = declarationEspecesPort;
    }

    public DeclarationEspeces declarer(String tenantId, String missionId, BigDecimal montant) {
        if (declarationEspecesPort.parMission(missionId).isPresent()) {
            throw new DeclarationEspecesInvalideException("Une déclaration espèces existe déjà pour la mission " + missionId);
        }
        DeclarationEspeces declaration = new DeclarationEspeces(UUID.randomUUID().toString(), tenantId, missionId, montant, Instant.now());
        declarationEspecesPort.enregistrer(declaration);
        return declaration;
    }

    public List<DeclarationEspeces> paiementsDuTenant(String tenantId) {
        return declarationEspecesPort.parTenant(tenantId);
    }
}
