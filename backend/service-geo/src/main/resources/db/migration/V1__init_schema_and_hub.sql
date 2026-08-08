-- Schema dedie a service-geo (isolation par service, cf Plan d'execution S4.1) :
-- aucune autre table d'un autre microservice ne doit vivre dans ce schema.
CREATE SCHEMA IF NOT EXISTS geo;

-- Extension PostGIS, necessaire au type GEOMETRY utilise ci-dessous
CREATE EXTENSION IF NOT EXISTS postgis;

-- Table hub : noeud du reseau (cf CDC v4 S13 - Entite Hub)
CREATE TABLE geo.hub (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),  -- genere cote base, jamais cote appli
    nom             VARCHAR(150) NOT NULL,
    ville           VARCHAR(150) NOT NULL,
    type_hub        VARCHAR(30)  NOT NULL, -- VILLE | PLATEFORME | POINT_CONSOLIDATION
    position        GEOMETRY(POINT, 4326) NOT NULL,               -- SRID 4326 = WGS84, coherent avec le GPS
    date_creation   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_hub_type CHECK (type_hub IN ('VILLE', 'PLATEFORME', 'POINT_CONSOLIDATION'))
);

-- Index spatial GIST : indispensable des que le volume d'axes/hubs grandit,
-- sinon toute requete "hubs proches de X" degenere en scan complet de table
CREATE INDEX idx_hub_position ON geo.hub USING GIST (position);
