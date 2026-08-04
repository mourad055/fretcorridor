-- Trace d'execution de chaque decision de matching (EF-MAT-11/12). Aucune utilite
-- fonctionnelle immediate visible, mais condition de toute amelioration mesurable
-- du moteur et de tout litige instruisible (cf Plan d'execution S3, "Trois choix
-- de modelisation a ne pas simplifier") - ne pas supprimer cette table "pour
-- aller plus vite", c'est explicitement l'erreur documentee a eviter.
--
-- Pas de FK vers capacite_id/demande_id : ces entites vivent dans les bases de
-- service-cap et service-mkt (Mobile, un autre porteur) - une FK inter-service
-- coupterait le deploiement de mat a celui de cap/mkt et casserait l'isolation
-- par schema (cf Plan d'execution S4.1). On stocke uniquement les identifiants.
CREATE TABLE mat.cycle_matching (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    capacite_id             UUID NOT NULL,
    demande_id              UUID NOT NULL,
    -- Nullable uniquement en mode degrade (aucun modele actif trouve en base) :
    -- dans ce cas on trace explicitement qu'aucune version n'a pu etre utilisee.
    modele_ponderation_id   UUID REFERENCES mat.modele_ponderation(id),
    cout_total              NUMERIC(12,4) NOT NULL,
    -- Detail de la contribution de chaque critere, pas seulement le total :
    -- c'est ce qui rend la decision reellement reconstructible (EF-MAT-11).
    details_couts           JSONB NOT NULL,
    mode_degrade            BOOLEAN NOT NULL DEFAULT false,
    date_execution          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cycle_matching_demande ON mat.cycle_matching (demande_id);
CREATE INDEX idx_cycle_matching_capacite ON mat.cycle_matching (capacite_id);
CREATE INDEX idx_cycle_matching_date ON mat.cycle_matching (date_execution);
