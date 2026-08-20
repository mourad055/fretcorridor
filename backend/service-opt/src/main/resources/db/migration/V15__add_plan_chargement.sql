-- Oracle de chargement 3D (CDC S8.7, EF-MAT-07/13, Sprint 16).
-- 1 PlanChargement par etat intermediaire de la tournee (par EtapeTournee),
-- jamais un seul pour toute la tournee - EF-MAT-07 exige une verification
-- "a chaque etat intermediaire", pas seulement au chargement initial.
CREATE TABLE opt.plan_chargement (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournee_id             UUID NOT NULL,
    etape_tournee_id       UUID NOT NULL,

    -- Charges par essieu a cet etat - JSONB, jamais de colonne dediee par
    -- essieu (nombre variable selon le vehicule, cf ProfilCamionDto.nombreEssieux).
    charges_par_essieu     JSONB NOT NULL DEFAULT '{}'::jsonb,

    -- Positions/orientations des colis a cet etat - structure ouverte tant
    -- que le contrat Lot/Colis cote Mobile n'est pas valide (cf README_ORACLE_3D.md S6).
    positions_colis        JSONB,

    faisable               BOOLEAN NOT NULL,
    motif_rejet            TEXT,

    -- ENF-DIS-04 : jamais un echec silencieux. Coherent avec CycleMatching.modeDegrade
    -- et TarificationResultat.modeDegrade deja en place.
    mode_degrade           BOOLEAN NOT NULL DEFAULT false,

    date_creation          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_plan_chargement_tournee_id ON opt.plan_chargement (tournee_id);
CREATE INDEX idx_plan_chargement_etape_id ON opt.plan_chargement (etape_tournee_id);
