-- Axe : liaison entre deux hubs (EF-GEO-01).
-- Etats d'activation independants (EF-GEO-03) : un axe peut etre visible
-- sans que le matching ou le paiement y soient actifs, et inversement.
CREATE TABLE geo.axe (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom                 VARCHAR(150) NOT NULL,
    hub_origine_id      UUID NOT NULL REFERENCES geo.hub(id),
    hub_destination_id  UUID NOT NULL REFERENCES geo.hub(id),
    -- Etats d'activation independants (EF-GEO-03).
    visibilite_active   BOOLEAN NOT NULL DEFAULT false,
    matching_actif      BOOLEAN NOT NULL DEFAULT false,
    paiement_actif      BOOLEAN NOT NULL DEFAULT false,
    -- Parametres de matching/tarification propres a l'axe (EF-GEO-02),
    -- stockes en JSONB pour rester configurable sans migration a chaque ajout de cle.
    parametres          JSONB NOT NULL DEFAULT '{}'::jsonb,
    date_creation       TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Un axe relie deux hubs distincts.
    CONSTRAINT chk_axe_hubs_distincts CHECK (hub_origine_id <> hub_destination_id)
);

-- Index pour les recherches frequentes : filtrer les axes actifs pour le matching (OPT).
CREATE INDEX idx_axe_matching_actif ON geo.axe (matching_actif) WHERE matching_actif = true;
CREATE INDEX idx_axe_hub_origine ON geo.axe (hub_origine_id);
CREATE INDEX idx_axe_hub_destination ON geo.axe (hub_destination_id);
