package com.fretcorridor.pay.domain;

import java.util.Optional;

public interface ModePaiementChoisiPort {

    Optional<ModePaiementChoisi> parMission(String missionId);

    void enregistrer(ModePaiementChoisi choix);
}
