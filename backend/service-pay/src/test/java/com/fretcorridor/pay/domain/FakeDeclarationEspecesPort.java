package com.fretcorridor.pay.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class FakeDeclarationEspecesPort implements DeclarationEspecesPort {

    private final Map<String, DeclarationEspeces> parMission = new HashMap<>();

    @Override
    public Optional<DeclarationEspeces> parMission(String missionId) {
        return Optional.ofNullable(parMission.get(missionId));
    }

    @Override
    public List<DeclarationEspeces> parTenant(String tenantId) {
        return parMission.values().stream().filter(d -> d.tenantId().equals(tenantId)).toList();
    }

    @Override
    public void enregistrer(DeclarationEspeces declaration) {
        parMission.put(declaration.missionId(), declaration);
    }
}
