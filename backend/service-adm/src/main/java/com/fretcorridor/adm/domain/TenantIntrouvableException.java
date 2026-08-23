package com.fretcorridor.adm.domain;

public class TenantIntrouvableException extends RuntimeException {

    public TenantIntrouvableException(String tenantId) {
        super("Aucun tenant avec l'identifiant " + tenantId);
    }
}
