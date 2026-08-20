package com.fretcorridor.pay.domain;

/**
 * IDOR corrigé (audit CDC §Transverse, "tenantId lu du corps de requête") :
 * un acteur authentifié demandait le rapport/l'historique d'un autre
 * tenant/transporteur que le sien sans aucune vérification. Distinct du
 * pattern "même exception pour introuvable et pas le sien" utilisé ailleurs
 * (capacité/dossier/notification) : ici la ressource demandée (un tenant, un
 * transporteur) existe légitimement et n'est pas confidentielle en soi —
 * seul l'accès à ses données financières est refusé, d'où 403 plutôt que 404.
 */
public class AccesRefuseException extends RuntimeException {
    public AccesRefuseException() {
        super("Accès refusé : ce tenant/transporteur n'est pas le vôtre");
    }
}
