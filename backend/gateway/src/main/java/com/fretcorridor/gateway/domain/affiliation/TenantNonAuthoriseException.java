package com.fretcorridor.gateway.domain.affiliation;

/** Sélection d'un tenant auquel l'acteur n'est pas affilié (tenant d'origine ou affiliation accordée par un bureau). */
public class TenantNonAuthoriseException extends RuntimeException {
    public TenantNonAuthoriseException() {
        super("TENANT_NON_AFFILIE");
    }
}
