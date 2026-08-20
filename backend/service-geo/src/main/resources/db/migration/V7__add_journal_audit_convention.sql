-- G4 (CDC S4.5) / EF-GEO-05, RG-052 (S9.9, S3.3 C2) : "Aucun ... quota de
-- repartition ... n'est code en dur : tout est configuration versionnee et
-- auditee." Meme patron que journal_audit_risque (Sprint 15, G3) - append-only,
-- une ligne par changement de cle de convention sur un axe.
CREATE TABLE geo.journal_audit_convention (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    axe_id                  UUID NOT NULL REFERENCES geo.axe(id),
    acteur_id               UUID NOT NULL,
    convention_code_avant   VARCHAR(50),
    convention_code_apres   VARCHAR(50) NOT NULL,
    parts_pourcent_apres    JSONB NOT NULL,
    motif                   TEXT NOT NULL,
    date_decision           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_journal_audit_convention_axe ON geo.journal_audit_convention (axe_id, date_decision DESC);
