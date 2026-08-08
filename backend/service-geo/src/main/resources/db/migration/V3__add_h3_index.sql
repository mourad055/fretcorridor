-- Zonage H3 (EF-GEO-01 complement) : chaque hub porte l'index de sa cellule
-- hexagonale, calcule automatiquement a la creation a partir de lat/lon.
-- Alimente en continu le filtre L0 d'OPT (rayon d'appariement borne).
ALTER TABLE geo.hub
    ADD COLUMN h3_index VARCHAR(20);

-- Index pour les requetes "quels hubs sont dans telle cellule / tel k-ring".
CREATE INDEX idx_hub_h3_index ON geo.hub (h3_index);

-- Resolution H3 configurable par defaut (jamais codee en dur - anti-patron
-- explicite du CDC). Resolution 7 ~= hexagones de 1.2 km2, un compromis
-- raisonnable ville/axe pour le MVP ; ajustable sans redeploiement via
-- cette table de configuration versionnee.
CREATE TABLE geo.configuration_h3 (
    cle             VARCHAR(50) PRIMARY KEY,
    valeur          VARCHAR(50) NOT NULL,
    description     VARCHAR(255),
    date_modification TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO geo.configuration_h3 (cle, valeur, description) VALUES
    ('resolution_defaut', '7', 'Resolution H3 par defaut pour le zonage des hubs (0=plus grossier, 15=plus fin)');
