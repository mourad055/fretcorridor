package com.fretcorridor.pay.domain;

import java.util.Optional;

public interface GarantiePort {

    Optional<Garantie> parMission(String missionId);

    void enregistrer(Garantie garantie);
}
