-- Point 6 (plan de reorientation) : transition TRK "colis recupere = position
-- chauffeur". Table dediee a l'etat de recuperation du colis par mission, a
-- laquelle TRK bascule, une fois l'enlevement confirme (EtapeExecuteeEvent
-- typeEtape = ENLEVEMENT), le point a afficher sur le suivi : avant
-- recuperation on affiche la position estimee du colis (son point
-- d'enlevement), apres la position GPS temps reel du chauffeur.

CREATE TABLE trk.colis_recuperation (
    mission_id             UUID PRIMARY KEY,
    horodatage_enlevement  TIMESTAMPTZ NOT NULL,
    horodatage_ingestion   TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE trk.colis_recuperation IS
    'Marque a quel instant le colis d une mission a ete recupere (enlevement execute). '
    'Une seule ligne par mission (PK = mission_id) : la recuperation est un evenement unique, '
    'idempotent (un doublon Kafka ne doit jamais cree de seconde ligne).';
