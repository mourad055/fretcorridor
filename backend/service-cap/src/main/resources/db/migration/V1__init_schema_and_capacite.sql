-- Schema dedie a service-cap (isolation par service, meme principe que
-- le reste du monorepo).
CREATE SCHEMA IF NOT EXISTS cap;

-- Capacite declaree (CDC S13, entite Capacite) : espace offert par un
-- vehicule sur un axe, avec fenetre de depart et poids taxable calcule
-- via la regle du maximum (EF-CAP-01/02).
CREATE TABLE cap.capacite (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicule_id                 UUID NOT NULL,
    axe_id                      UUID NOT NULL,
    -- EF-CAP-03 : trois modes de declaration, chaine libre (pas d'enum
    -- fige en base) pour rester coherent avec le reste du systeme
    -- (anti-bareme-en-dur).
    mode_declaration            VARCHAR(30) NOT NULL
                                    CHECK (mode_declaration IN ('TOTALE', 'FRACTIONNAIRE_ASSISTEE', 'PRECISE')),
    -- EF-CAP-01 : poids/volume/longueur de plancher declares.
    poids_kg                    NUMERIC(10,2) NOT NULL CHECK (poids_kg > 0),
    volume_m3                   NUMERIC(10,3),
    longueur_plancher_m         NUMERIC(6,2),
    -- EF-CAP-02 : poids taxable = regle du maximum (jamais recalcule en
    -- place, fige au moment de la declaration - coherent avec le principe
    -- d'immuabilite deja applique a CycleMatching/BaremeTarification).
    poids_taxable_kg            NUMERIC(10,2) NOT NULL,
    -- EF-CAP-07 : capacite residuelle, decrementee de facon atomique/
    -- idempotente au fil des affectations (cf CapaciteService.decrementer).
    capacite_residuelle_kg      NUMERIC(10,2) NOT NULL,
    origine_latitude             DOUBLE PRECISION NOT NULL,
    origine_longitude            DOUBLE PRECISION NOT NULL,
    type_vehicule                VARCHAR(50),
    profil_hauteur_m             NUMERIC(5,2),
    profil_largeur_m             NUMERIC(5,2),
    profil_longueur_m            NUMERIC(5,2),
    profil_poids_max_t           NUMERIC(6,2),
    profil_charge_essieu_max_t   NUMERIC(6,2),
    profil_nb_essieux            INTEGER,
    profil_matieres_dangereuses  BOOLEAN NOT NULL DEFAULT false,
    -- EF-CAP-08 : fenetre de depart, base de l'expiration automatique.
    date_depart                  TIMESTAMPTZ NOT NULL,
    expiree                      BOOLEAN NOT NULL DEFAULT false,
    publiee                      BOOLEAN NOT NULL DEFAULT false,
    date_creation                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Verrou optimiste : condition du decrement atomique (EF-CAP-07),
    -- evite un ecrasement silencieux en cas d'acces concurrents.
    version                      BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_capacite_axe ON cap.capacite (axe_id);
CREATE INDEX idx_capacite_expiration ON cap.capacite (date_depart) WHERE expiree = false;

-- Journal d'idempotence pour le decrement (EF-CAP-07, "decrement atomique
-- ET idempotent") : une cle d'idempotence deja vue ne redecremente jamais
-- une seconde fois, meme en cas de retry reseau cote appelant.
CREATE TABLE cap.decrement_log (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    capacite_id         UUID NOT NULL REFERENCES cap.capacite(id),
    cle_idempotence     VARCHAR(100) NOT NULL,
    montant_kg          NUMERIC(10,2) NOT NULL,
    date_creation        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_decrement_idempotence UNIQUE (capacite_id, cle_idempotence)
);
