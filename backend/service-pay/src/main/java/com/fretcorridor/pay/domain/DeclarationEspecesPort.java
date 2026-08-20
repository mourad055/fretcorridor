package com.fretcorridor.pay.domain;

import java.util.List;
import java.util.Optional;

public interface DeclarationEspecesPort {

    Optional<DeclarationEspeces> parMission(String missionId);

    List<DeclarationEspeces> parTenant(String tenantId);

    void enregistrer(DeclarationEspeces declaration);
}
