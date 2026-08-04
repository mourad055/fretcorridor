package com.fretcorridor.gateway.infrastructure.audit;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ENF-SEC-02 : journal d'audit en ajout seul. Implémentation provisoire, en mémoire —
 * remplacée par le JournalAudit append-only de service-adm au Sprint 10 (PRD §9 S10).
 * Aucune méthode de suppression n'est exposée par construction.
 */
@Component
public class AuditLog {

    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>();

    public void enregistrer(String operateurId, String action, String ressource) {
        entries.add(new AuditEntry(operateurId, action, ressource, Instant.now()));
    }

    public List<AuditEntry> consulter() {
        return Collections.unmodifiableList(entries);
    }
}
