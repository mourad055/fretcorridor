-- Corrige une incoherence type SQL / type Java introduite en V6 : les entites
-- CapaciteEnAttente declarent ces champs en Double (Java), ce qui exige
-- "double precision" cote Hibernate en ddl-auto=validate - pas NUMERIC.
-- Meme convention que opt.affectation (V4) pour ses colonnes latitude/longitude.
ALTER TABLE opt.capacite_en_attente
    ALTER COLUMN profil_hauteur_m           TYPE DOUBLE PRECISION,
    ALTER COLUMN profil_largeur_m           TYPE DOUBLE PRECISION,
    ALTER COLUMN profil_longueur_m          TYPE DOUBLE PRECISION,
    ALTER COLUMN profil_poids_max_t         TYPE DOUBLE PRECISION,
    ALTER COLUMN profil_charge_essieu_max_t TYPE DOUBLE PRECISION;

ALTER TABLE opt.demande_en_attente
    ALTER COLUMN poids_taxable_kg TYPE NUMERIC(12,3);
    -- poids_taxable_kg reste NUMERIC : DemandeEnAttente.poidsTaxableKg est un
    -- BigDecimal Java (pas Double), donc NUMERIC est le bon type ici - pas de
    -- changement necessaire sur cette colonne, ligne gardee explicite pour
    -- que la migration documente clairement ce qui a ete verifie, pas
    -- seulement ce qui a change.
