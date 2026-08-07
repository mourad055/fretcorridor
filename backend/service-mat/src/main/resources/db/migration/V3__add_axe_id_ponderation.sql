-- RG-106 : "les coefficients sont configures par axe, versionnes" - jusqu'ici
-- mat.modele_ponderation etait un modele global unique, aucune colonne axe_id.
-- Meme patron que opt.bareme_tarification (V2) : axe_id nullable = modele par
-- defaut, utilise en repli si aucun modele specifique a l'axe n'est actif
-- (cf EF-GEO-02, "chaque axe a ses propres parametres de matching").
ALTER TABLE mat.modele_ponderation
    ADD COLUMN axe_id UUID;

-- L'ancien index unique global (un seul modele actif, tout court) ne
-- correspond plus au besoin : on veut au plus un modele actif PAR axe, et
-- au plus un modele actif par defaut (axe_id NULL). Meme mecanique que
-- opt.bareme_tarification (COALESCE vers UUID sentinelle, Postgres ne
-- deduplique pas NULL nativement dans un index unique classique).
DROP INDEX IF EXISTS mat.idx_modele_ponderation_unique_actif;

CREATE UNIQUE INDEX idx_modele_ponderation_actif_par_axe
    ON mat.modele_ponderation (COALESCE(axe_id, '00000000-0000-0000-0000-000000000000'))
    WHERE actif = true;

-- Le modele V0 initial (insere en V1) reste le modele par defaut (axe_id
-- NULL) - aucune migration de donnees necessaire, axe_id est deja NULL
-- par defaut sur la ligne existante.
