-- Table position : une ligne par PositionBrute ingeree (EF-TRK-01/02, Sprint 6).
--
-- event_id UNIQUE = mecanisme d'idempotence (ENF-SEC-03, plan S4.3 "chaque
-- evenement porte une cle d'idempotence") : un re-envoi identique (mode hors
-- ligne cote FLT, redelivery Kafka at-least-once) est silencieusement ignore
-- via une contrainte UNIQUE plutot qu'une logique applicative fragile.
--
-- horodatage_capture distinct de horodatage_transmission (EF-TRK-04, meme
-- principe que EF-EXE-04/05) : "l'age" d'une position affichee au client se
-- calcule comme now() - horodatage_capture, jamais - transmission, sinon une
-- position vieille de 20 minutes transmise en rafale au retour du reseau
-- paraitrait "fraiche".
CREATE TABLE trk.position (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id                 UUID NOT NULL,
    mission_id                UUID NOT NULL,
    vehicule_id               UUID NOT NULL,
    latitude                 DOUBLE PRECISION NOT NULL,
    longitude                DOUBLE PRECISION NOT NULL,
    source_capture            VARCHAR(30) NOT NULL,   -- GPS_NATIF | GPS_DEGRADE | MANUEL
    precision_metres          DOUBLE PRECISION,        -- nullable : pas toujours rapporte par l'appareil
    horodatage_capture        TIMESTAMPTZ NOT NULL,
    horodatage_transmission   TIMESTAMPTZ NOT NULL,
    horodatage_ingestion      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_position_event_id UNIQUE (event_id),
    CONSTRAINT chk_source_capture CHECK (source_capture IN ('GPS_NATIF', 'GPS_DEGRADE', 'MANUEL'))
);

-- Requete la plus frequente attendue : "derniere position connue d'une mission/vehicule" -
-- index compose avec tri descendant sur l'horodatage, evite un tri en memoire a chaque appel.
CREATE INDEX idx_position_mission ON trk.position (mission_id, horodatage_capture DESC);
CREATE INDEX idx_position_vehicule ON trk.position (vehicule_id, horodatage_capture DESC);
