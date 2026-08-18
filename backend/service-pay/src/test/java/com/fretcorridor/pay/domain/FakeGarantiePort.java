package com.fretcorridor.pay.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class FakeGarantiePort implements GarantiePort {

    private final Map<String, Garantie> garanties = new HashMap<>();

    @Override
    public Optional<Garantie> parMission(String missionId) {
        return Optional.ofNullable(garanties.get(missionId));
    }

    @Override
    public void enregistrer(Garantie garantie) {
        garanties.put(garantie.missionId(), garantie);
    }
}
