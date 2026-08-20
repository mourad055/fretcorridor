-- G3 (CDC S4.5) / EF-GEO-04 (S9.9) : "Aucune operation n'est enrolee dans
-- les zones sous verrou securitaire sans renseignement a jour et decision
-- explicite tracee." Verification prevue par le CDC : "Journal d'audit ;
-- indicateur 'operations en zone gelee = 0'."
--
-- Append-only (JournalAudit est une entite officielle du modele CDC S13,
-- "Trace inviolable des actions sensibles") : aucun UPDATE/DELETE prevu par
-- l'application, seulement des INSERT - cf absence volontaire de colonne
-- date_modification.
CREATE TABLE geo.journal_audit_risque (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    axe_id                  UUID NOT NULL REFERENCES geo.axe(id),
    acteur_id               UUID NOT NULL,
    niveau_risque_avant     VARCHAR(30),
    niveau_risque_apres     VARCHAR(30) NOT NULL,
    motif                   TEXT NOT NULL,
    date_decision           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_journal_audit_risque_axe ON geo.journal_audit_risque (axe_id, date_decision DESC);
