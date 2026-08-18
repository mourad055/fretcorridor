-- EF-CAP-07 / CDC S8.6.1 (capacite dynamique, Phase 2 sequencement L2) -
-- grandeur reellement disponible dans une capacite, distincte du plafond
-- du vehicule (profil_poids_max_t, deja present depuis V6).
ALTER TABLE opt.capacite_en_attente
    ADD COLUMN capacite_residuelle_kg NUMERIC(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN volume_residuel_m3     NUMERIC(12,3);

-- DEFAULT 0 uniquement pour permettre l'ALTER sur une table potentiellement
-- non vide (lignes deja traitees=true, historiques) - toute nouvelle ligne
-- via CapaciteDeclareeListener fournira la vraie valeur (champ requis dans
-- le contrat), jamais 0 par defaut en pratique.
ALTER TABLE opt.capacite_en_attente ALTER COLUMN capacite_residuelle_kg DROP DEFAULT;
