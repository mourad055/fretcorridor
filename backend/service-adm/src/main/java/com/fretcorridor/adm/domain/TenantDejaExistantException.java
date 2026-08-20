package com.fretcorridor.adm.domain;

public class TenantDejaExistantException extends RuntimeException {

    public TenantDejaExistantException(String tenantId) {
        super("Un tenant avec l'identifiant " + tenantId + " existe déjà");
    }
}
