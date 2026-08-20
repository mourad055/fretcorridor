package com.fretcorridor.adm.domain;

/**
 * IDOR corrigé (audit CDC §Transverse, "tenantId lu du corps de requête") :
 * un acteur authentifié demandait la file de travail d'un autre tenant que
 * le sien sans aucune vérification. Distinct du pattern "même exception
 * pour introuvable et pas le sien" utilisé pour un Dossier précis
 * (DossierIntrouvableException, cf. consulter()) : ici la ressource
 * demandée (un tenant) existe légitimement et n'est pas confidentielle en
 * soi — seul l'accès à sa file de travail est refusé, d'où 403 plutôt que
 * 404. Même principe que AccesRefuseException côté service-pay.
 */
public class AccesRefuseException extends RuntimeException {
    public AccesRefuseException() {
        super("Accès refusé : ce tenant n'est pas le vôtre");
    }
}
