-- Tarification par chauffeur (spec-tarification-par-chauffeur.md,
-- plan-fretcorridor-reorientation.md §1bis) : chaque chauffeur fixe son
-- propre tarif en F/kg, par axe (geo.axe, referentiel externe, pas de FK
-- cross-service) et par categorie de marchandise. Remplace le barreme
-- unique (service-opt.BaremeTarification) comme source du taux au kg --
-- BaremeTarification reste, mais uniquement pour la politique plateforme
-- (commission, plancher, tension).
CREATE TABLE cap.tarif_chauffeur (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transporteur_id         UUID NOT NULL,
    axe_id                  UUID NOT NULL,
    categorie_marchandise   VARCHAR(20) NOT NULL
                                CHECK (categorie_marchandise IN ('STANDARD', 'FRAGILE', 'ULTRA_FRAGILE')),
    tarif_fcfa_par_kg       NUMERIC(10,2) NOT NULL CHECK (tarif_fcfa_par_kg > 0),
    actif                   BOOLEAN NOT NULL DEFAULT true,
    tenant_id               VARCHAR(100),
    date_creation           TIMESTAMPTZ NOT NULL DEFAULT now(),
    date_mise_a_jour        TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Un seul tarif actif par (chauffeur, axe, categorie) -- "configurer"
    -- cote mobile met a jour la ligne existante plutot que d'en creer une
    -- autre si elle existe deja.
    CONSTRAINT uq_tarif_chauffeur UNIQUE (transporteur_id, axe_id, categorie_marchandise)
);

-- Lu par OPT a chaque candidat du L1 (detail) et par l'intervalle client
-- (agregat) -- les deux filtrent sur actif=true en premier.
CREATE INDEX idx_tarif_chauffeur_axe_categorie ON cap.tarif_chauffeur (axe_id, categorie_marchandise) WHERE actif = true;
CREATE INDEX idx_tarif_chauffeur_transporteur ON cap.tarif_chauffeur (transporteur_id);
