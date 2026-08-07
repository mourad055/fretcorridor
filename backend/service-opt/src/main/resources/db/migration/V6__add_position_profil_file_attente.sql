-- Complete la file d'attente (V5) avec les donnees necessaires en aval du L1
-- (Valhalla, Tarification L4) - decouvert lors du cablage de MatchingCycleService :
-- CandidatCoutDto/DemandeAvecCandidats exigent position/profil/destination
-- depuis l'ajout de Valhalla, mais la file d'attente ne les portait pas encore.
ALTER TABLE opt.capacite_en_attente
    ADD COLUMN position_latitude    DOUBLE PRECISION,
    ADD COLUMN position_longitude   DOUBLE PRECISION,
    ADD COLUMN profil_hauteur_m     NUMERIC(6,2),
    ADD COLUMN profil_largeur_m     NUMERIC(6,2),
    ADD COLUMN profil_longueur_m    NUMERIC(6,2),
    ADD COLUMN profil_poids_max_t   NUMERIC(8,2),
    ADD COLUMN profil_charge_essieu_max_t NUMERIC(8,2),
    ADD COLUMN profil_nb_essieux    INTEGER,
    ADD COLUMN profil_matieres_dangereuses BOOLEAN,
    ADD COLUMN type_vehicule        VARCHAR(30);

ALTER TABLE opt.demande_en_attente
    ADD COLUMN origine_latitude       DOUBLE PRECISION,
    ADD COLUMN origine_longitude      DOUBLE PRECISION,
    ADD COLUMN destination_latitude   DOUBLE PRECISION,
    ADD COLUMN destination_longitude  DOUBLE PRECISION,
    ADD COLUMN poids_taxable_kg       NUMERIC(12,3);
