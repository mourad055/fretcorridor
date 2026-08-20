package com.fretcorridor.pay.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

class FakeReconciliationEventPort implements ReconciliationEventPort {

    record Publication(String missionId, String tenantId, BigDecimal ecart, Instant declencheeLe) {
    }

    private final List<Publication> publications = new ArrayList<>();

    @Override
    public void publier(String missionId, String tenantId, BigDecimal ecart, Instant declencheeLe) {
        publications.add(new Publication(missionId, tenantId, ecart, declencheeLe));
    }

    List<Publication> publications() {
        return List.copyOf(publications);
    }
}
