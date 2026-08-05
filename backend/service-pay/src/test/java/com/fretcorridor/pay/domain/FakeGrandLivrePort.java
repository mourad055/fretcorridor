package com.fretcorridor.pay.domain;

import java.util.ArrayList;
import java.util.List;

class FakeGrandLivrePort implements GrandLivrePort {

    private final List<EcritureMiroir> ecritures = new ArrayList<>();

    @Override
    public void enregistrer(EcritureMiroir ecriture) {
        ecritures.add(ecriture);
    }

    @Override
    public List<EcritureMiroir> parMission(String missionId) {
        return ecritures.stream().filter(e -> e.missionId().equals(missionId)).toList();
    }

    @Override
    public List<EcritureMiroir> parBeneficiaire(String beneficiaireId) {
        return ecritures.stream().filter(e -> beneficiaireId.equals(e.beneficiaireId())).toList();
    }

    @Override
    public List<EcritureMiroir> parTenant(String tenantId) {
        return ecritures.stream().filter(e -> tenantId.equals(e.tenantId())).toList();
    }

    @Override
    public void suspendre(String ecritureId) {
        for (int i = 0; i < ecritures.size(); i++) {
            if (ecritures.get(i).id().equals(ecritureId)) {
                ecritures.set(i, ecritures.get(i).suspendue());
            }
        }
    }

    List<EcritureMiroir> toutes() {
        return List.copyOf(ecritures);
    }
}
