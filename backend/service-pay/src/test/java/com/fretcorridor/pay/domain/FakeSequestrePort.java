package com.fretcorridor.pay.domain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class FakeSequestrePort implements SequestrePort {

    private final Map<String, Sequestre> sequestres = new HashMap<>();

    @Override
    public Optional<Sequestre> parMission(String missionId) {
        return Optional.ofNullable(sequestres.get(missionId));
    }

    @Override
    public void sauvegarder(Sequestre sequestre) {
        sequestres.put(sequestre.missionId(), sequestre);
    }

    @Override
    public List<Sequestre> listerTous() {
        return List.copyOf(sequestres.values());
    }
}
