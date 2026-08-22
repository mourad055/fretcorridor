ALTER TABLE opt.demande_en_attente ADD COLUMN mode_collecte VARCHAR(50);
ALTER TABLE opt.demande_en_attente ADD COLUMN type_disponibilite VARCHAR(50);
ALTER TABLE opt.demande_en_attente ADD COLUMN poids_total_kg DOUBLE PRECISION;
ALTER TABLE opt.demande_en_attente ADD COLUMN grande_valeur BOOLEAN;
