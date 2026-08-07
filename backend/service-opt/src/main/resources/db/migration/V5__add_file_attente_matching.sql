-- File d'attente pour l'appariement "par cycles a fenetre" (EF-MAT-01, README
-- S2 Sprint 5) - PAS un matching immediat evenement par evenement. Les
-- CapaciteDeclaree/DemandePubliee (Mobile, async Kafka) sont d'abord mises en
-- attente ici ; un cycle planifie (MatchingCycleService, @Scheduled) les
-- regroupe par axe actif et lance un seul Kuhn-Munkres sur le lot entier.
--
-- traitee=false : en attente d'un prochain cycle. Passe a true des qu'un
-- cycle l'a incluse dans un lot - jamais supprimee immediatement (garde une
-- trace courte utile au debug), purgee separement si besoin (hors perimetre
-- de cet increment).
CREATE TABLE opt.capacite_en_attente (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    capacite_id     UUID NOT NULL,
    axe_id          UUID NOT NULL,
    event_id        UUID NOT NULL,
    valeurs_criteres JSONB NOT NULL,
    traitee         BOOLEAN NOT NULL DEFAULT false,
    date_reception  TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Idempotence (ENF-SEC-03) : un meme evenement Kafka rejoue (replay,
    -- redemarrage consumer) ne doit jamais dupliquer l'entree en attente.
    CONSTRAINT uq_capacite_en_attente_event UNIQUE (event_id)
);

CREATE INDEX idx_capacite_en_attente_axe_non_traitee
    ON opt.capacite_en_attente (axe_id) WHERE traitee = false;

CREATE TABLE opt.demande_en_attente (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    demande_id      UUID NOT NULL,
    axe_id          UUID NOT NULL,
    event_id        UUID NOT NULL,
    valeurs_criteres JSONB NOT NULL,
    traitee         BOOLEAN NOT NULL DEFAULT false,
    date_reception  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_demande_en_attente_event UNIQUE (event_id)
);

CREATE INDEX idx_demande_en_attente_axe_non_traitee
    ON opt.demande_en_attente (axe_id) WHERE traitee = false;
