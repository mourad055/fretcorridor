-- Sprint 11 (Phase 2) : sequencement L2, PDPTW/ALNS (CDC S8.6). Une Tournee
-- regroupe plusieurs Affectation deja confirmees (L1) sur la MEME capacite,
-- dans un ordre donne - ne modifie JAMAIS les Affectation elles-memes
-- (immuables, cf javadoc Affectation.java : "toute evolution future ...
-- passera par une methode metier explicite").
CREATE TABLE opt.tournee (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    capacite_id     UUID NOT NULL,
    axe_id          UUID,
    -- EN_CONSTRUCTION : l'ALNS explore encore des solutions candidates.
    -- CONFIRMEE : sequence retenue, publiee (AffectationConfirmee enrichi
    -- ou nouvel evenement, a definir a l'increment suivant).
    -- EN_EXECUTION : au moins une etape marquee EXECUTEE (EF-MAT-09,
    -- figeage) - une tournee EN_EXECUTION ne peut plus etre entierement
    -- recalculee, seules les etapes non executees restent modifiables.
    statut          VARCHAR(30) NOT NULL DEFAULT 'EN_CONSTRUCTION'
                        CHECK (statut IN ('EN_CONSTRUCTION', 'CONFIRMEE', 'EN_EXECUTION', 'TERMINEE')),
    date_creation   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tournee_capacite ON opt.tournee (capacite_id);

-- Une etape = un enlevement ou une livraison d'une Affectation donnee, a un
-- rang precis dans la Tournee. Precedence enlevement-livraison (CDC S8.6.1
-- point 2) : verifiee en Java (rang enlevement < rang livraison pour la meme
-- affectation), pas en contrainte SQL - trop complexe a exprimer proprement
-- en CHECK, et la verification vit deja naturellement dans le solveur ALNS.
CREATE TABLE opt.etape_tournee (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tournee_id      UUID NOT NULL REFERENCES opt.tournee(id),
    affectation_id  UUID NOT NULL REFERENCES opt.affectation(id),
    rang            INTEGER NOT NULL,
    type_etape      VARCHAR(20) NOT NULL CHECK (type_etape IN ('ENLEVEMENT', 'LIVRAISON')),

    -- EF-MAT-09 (Phase 2) : "replanifier en figeant l'execute" - une etape
    -- EXECUTEE ne doit plus jamais etre deplacee ni recalculee par l'ALNS.
    etat            VARCHAR(20) NOT NULL DEFAULT 'PLANIFIEE'
                        CHECK (etat IN ('PLANIFIEE', 'EXECUTEE')),

    -- Capacite dynamique (CDC S8.6.1 point 3) : charge du vehicule APRES
    -- cette etape (poids ajoute a un enlevement, retire a une livraison).
    -- Stockee explicitement plutot que recalculee a la volee : permet de
    -- verifier rapidement qu'aucun etat intermediaire ne depasse
    -- capacite_residuelle_kg de la Capacite, sans reparcourir toute la
    -- tournee a chaque lecture.
    charge_apres_etape_kg   NUMERIC(12,2) NOT NULL,

    -- RG-056/RG-108/EF-MAT-10 : detour reellement subi par la demande de
    -- cette affectation dans cette tournee - calcule une fois, persiste
    -- pour tracabilite (meme logique que CycleMatching cote MAT).
    detour_distance_metres  DOUBLE PRECISION,
    detour_duree_secondes   DOUBLE PRECISION,

    date_creation   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_etape_tournee_rang UNIQUE (tournee_id, rang)
);

CREATE INDEX idx_etape_tournee_tournee ON opt.etape_tournee (tournee_id);
CREATE INDEX idx_etape_tournee_affectation ON opt.etape_tournee (affectation_id);
