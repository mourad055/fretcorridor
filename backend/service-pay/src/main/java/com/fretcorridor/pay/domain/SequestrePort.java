package com.fretcorridor.pay.domain;

import java.util.List;
import java.util.Optional;

public interface SequestrePort {

    Optional<Sequestre> parMission(String missionId);

    void sauvegarder(Sequestre sequestre);

    List<Sequestre> listerTous();
}
