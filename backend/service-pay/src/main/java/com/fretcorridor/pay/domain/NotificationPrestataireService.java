package com.fretcorridor.pay.domain;

/**
 * EF-PAY-05 : vérification cryptographique et traitement idempotent des
 * notifications entrantes du prestataire de paiement. La signature protège
 * contre un tiers qui forgerait une confirmation d'encaissement ; la clé
 * d'idempotence protège contre un rejeu réseau (retry du prestataire) qui
 * dupliquerait l'écriture au grand livre — anti-patron proscrit (§12.4).
 */
public class NotificationPrestataireService {

    public enum Resultat {
        TRAITEE,
        DEJA_TRAITEE
    }

    private final GrandLivreService grandLivreService;
    private final NotificationIdempotencePort idempotencePort;
    private final SignatureVerifierPort signatureVerifierPort;

    public NotificationPrestataireService(
            GrandLivreService grandLivreService,
            NotificationIdempotencePort idempotencePort,
            SignatureVerifierPort signatureVerifierPort
    ) {
        this.grandLivreService = grandLivreService;
        this.idempotencePort = idempotencePort;
        this.signatureVerifierPort = signatureVerifierPort;
    }

    public Resultat traiter(String corpsBrut, String signatureRecue, String idempotenceKey, NotificationEncaissement notification) {
        if (!signatureVerifierPort.estValide(corpsBrut, signatureRecue)) {
            throw new SignatureInvalideException();
        }

        if (idempotencePort.dejaTraitee(idempotenceKey)) {
            return Resultat.DEJA_TRAITEE;
        }

        grandLivreService.enregistrerEncaissement(
                notification.tenantId(), notification.missionId(), notification.montant(), notification.referencePrestataire());
        idempotencePort.marquerTraitee(idempotenceKey);
        return Resultat.TRAITEE;
    }
}
