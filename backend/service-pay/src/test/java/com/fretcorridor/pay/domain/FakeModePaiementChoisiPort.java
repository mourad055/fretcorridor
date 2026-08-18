package com.fretcorridor.pay.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class FakeModePaiementChoisiPort implements ModePaiementChoisiPort {

    private final Map<String, ModePaiementChoisi> choix = new HashMap<>();

    @Override
    public Optional<ModePaiementChoisi> parMission(String missionId) {
        return Optional.ofNullable(choix.get(missionId));
    }

    @Override
    public void enregistrer(ModePaiementChoisi c) {
        choix.put(c.missionId(), c);
    }
}
