package com.fretcorridor.pay.domain;

import java.util.Optional;

public interface SequestrePort {

    Optional<Sequestre> parMission(String missionId);

    void sauvegarder(Sequestre sequestre);
}
