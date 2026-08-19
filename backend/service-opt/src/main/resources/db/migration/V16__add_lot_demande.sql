-- Detail colis par colis (CDC S13, EF-MKT-10) - contrat demande-publiee-lots.yaml
-- valide avec Mobile. Persiste tel quel, la logique de verification vit
-- dans OracleChargementService.
CREATE TABLE opt.lot_demande (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    demande_id      UUID NOT NULL,
    lot_id          UUID NOT NULL,
    event_id        UUID NOT NULL,
    type_catalogue  VARCHAR(100) NOT NULL,
    quantite        INTEGER NOT NULL,
    poids_kg        NUMERIC(10,2) NOT NULL,
    longueur_m      DOUBLE PRECISION,
    largeur_m       DOUBLE PRECISION,
    hauteur_m       DOUBLE PRECISION,
    gerbable        BOOLEAN NOT NULL,
    fragile         BOOLEAN NOT NULL,
    classe_danger   VARCHAR(50),
    date_reception  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_lot_demande_event UNIQUE (event_id)
);

CREATE INDEX idx_lot_demande_demande_id ON opt.lot_demande (demande_id);
