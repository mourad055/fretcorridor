package com.fretcorridor.pay.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class FakeLitigeMissionPort implements LitigeMissionPort {

    private final Map<String, LitigeMission> parMission = new HashMap<>();

    @Override
    public Optional<LitigeMission> parMission(String missionId) {
        return Optional.ofNullable(parMission.get(missionId));
    }

    @Override
    public void enregistrerSiPlusRecent(LitigeMission litige) {
        LitigeMission existant = parMission.get(litige.missionId());
        if (existant == null || litige.horodatage().isAfter(existant.horodatage())) {
            parMission.put(litige.missionId(), litige);
        }
    }
}
