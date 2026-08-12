package com.fretcorridor.adm.domain;

import java.util.ArrayList;
import java.util.List;

public class InMemoryDossierEventPort implements DossierEventPort {

    private final List<Dossier> publies = new ArrayList<>();

    @Override
    public void publier(Dossier dossier) {
        publies.add(dossier);
    }

    public List<Dossier> publies() {
        return List.copyOf(publies);
    }
}
