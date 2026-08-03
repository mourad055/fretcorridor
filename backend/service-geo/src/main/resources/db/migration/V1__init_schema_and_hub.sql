-- Schema dedie a service-geo (isolation par service, cf Plan d'execution 4.1)
CREATE SCHEMA IF NOT EXISTS geo;

-- Extension PostGIS, necessaire au type geometry
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE geo.hub (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom             VARCHAR(150) NOT NULL,
    ville           VARCHAR(150) NOT NULL,
    type_hub        VARCHAR(30)  NOT NULL, -- VILLE | PLATEFORME | POINT_CONSOLIDATION
    position        GEOMETRY(POINT, 4326) NOT NULL,
    date_creation   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_hub_type CHECK (type_hub IN ('VILLE', 'PLATEFORME', 'POINT_CONSOLIDATION'))
);

-- Index spatial : indispensable des que le volume d'axes/hubs grandit
CREATE INDEX idx_hub_position ON geo.hub USING GIST (position);
