package com.fretcorridor.pay.domain;

import java.util.Optional;

public interface LitigeMissionPort {

    Optional<LitigeMission> parMission(String missionId);

    void enregistrerSiPlusRecent(LitigeMission litige);
}
