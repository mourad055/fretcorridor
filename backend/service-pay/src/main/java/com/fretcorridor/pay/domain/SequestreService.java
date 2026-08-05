package com.fretcorridor.pay.domain;

import java.time.Instant;

public class SequestreService {

    private final SequestrePort sequestrePort;

    public SequestreService(SequestrePort sequestrePort) {
        this.sequestrePort = sequestrePort;
    }

    public Sequestre declencher(String missionId) {
        if (sequestrePort.parMission(missionId).isPresent()) {
            throw new SequestreInvalideException("Un séquestre existe déjà pour la mission " + missionId);
        }
        Sequestre sequestre = new Sequestre(missionId, SequestreEtat.DECLENCHE, Instant.now(), null);
        sequestrePort.sauvegarder(sequestre);
        return sequestre;
    }

    public Sequestre liberer(String missionId) {
        Sequestre existant = sequestrePort.parMission(missionId)
                .orElseThrow(() -> new SequestreInvalideException("Aucun séquestre déclenché pour la mission " + missionId));

        if (existant.etat() != SequestreEtat.DECLENCHE) {
            throw new SequestreInvalideException("Le séquestre de la mission " + missionId + " n'est pas dans l'état DECLENCHE");
        }

        Sequestre libere = new Sequestre(missionId, SequestreEtat.LIBERE, existant.declencheLe(), Instant.now());
        sequestrePort.sauvegarder(libere);
        return libere;
    }
}
