-- UC-MAT-02 du CDC ("Notification, acceptation ou refus d'une mission par
-- le chauffeur", page 43) : le rang 1 du L1 etait jusqu'ici auto-confirme
-- (Affectation creee, capacite reservee, Mission publiee IMMEDIATEMENT au
-- sortir du solveur Kuhn-Munkres) sans jamais demander au chauffeur/
-- transporteur d'accepter ou refuser -- ecart au CDC, pas une simplification
-- documentee. Cette table introduit l'etape manquante : le rang 1 devient
-- une proposition EN_ATTENTE, notifiee au transporteur, qui doit l'accepter
-- explicitement avant que opt.affectation (et tout ce qui en decoule --
-- reservation de capacite, evenement AffectationConfirmee) ne soit cree.
--
-- Champs d'affichage (RG-049 : remuneration en premier) captures ici en
-- instantane depuis DemandeAvecCandidats au moment du matching, plutot que
-- re-interroges a la lecture -- coherent avec le principe deja applique a
-- opt.affectation (source de verite locale au perimetre Moteur).
--
-- Tarification NON dupliquee ici (contrairement a opt.affectation) : a
-- l'acceptation, TarificationL4Service est rappele avec les memes entrees
-- deterministes (axe_id, type_vehicule, poids_taxable_kg, distance_metres)
-- pour reconstruire le detail complet -- evite de dupliquer 12 colonnes de
-- decomposition de prix pour une ligne qui peut ne jamais etre acceptee.
CREATE TABLE opt.proposition_mission (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    demande_id                      UUID NOT NULL,
    capacite_id                     UUID NOT NULL,
    transporteur_id                 UUID NOT NULL,
    vehicule_id                     UUID,
    type_vehicule                   VARCHAR(100),
    cycle_matching_id               UUID,
    axe_id                          UUID,
    rang                            INTEGER NOT NULL DEFAULT 1,

    poids_taxable_kg                NUMERIC(12,3),

    origine_nom                     VARCHAR(255),
    destination_nom                 VARCHAR(255),
    origine_latitude                DOUBLE PRECISION NOT NULL,
    origine_longitude               DOUBLE PRECISION NOT NULL,
    destination_latitude            DOUBLE PRECISION NOT NULL,
    destination_longitude           DOUBLE PRECISION NOT NULL,

    distance_metres                 DOUBLE PRECISION,
    duree_secondes                  DOUBLE PRECISION,
    intervalle_confiance_secondes   DOUBLE PRECISION,
    geometrie_encodee               TEXT,

    -- Remuneration a afficher en premier (RG-049) -- instantane pour un
    -- affichage stable, independant d'une eventuelle evolution du bareme
    -- entre la proposition et la reponse.
    prix_transport                  NUMERIC(12,4),

    -- RG-048 (tracabilite des decisions) : cout composite Kuhn-Munkres ayant
    -- determine ce rang, reporte tel quel sur opt.affectation a l'acceptation.
    cout_total                      NUMERIC(12,4) NOT NULL,

    type_emballage_nom              VARCHAR(255),
    quantite                        INTEGER,
    destinataire_nom                VARCHAR(255),
    destinataire_telephone          VARCHAR(50),
    mode_collecte                   VARCHAR(50),
    type_disponibilite              VARCHAR(50),
    poids_total_kg                  DOUBLE PRECISION,
    grande_valeur                   BOOLEAN,

    statut                          VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE'
                                     CHECK (statut IN ('EN_ATTENTE', 'ACCEPTEE', 'REFUSEE', 'EXPIREE')),
    motif_refus                     VARCHAR(255),

    expire_a                        TIMESTAMPTZ NOT NULL,
    date_creation                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    date_reponse                    TIMESTAMPTZ
);

-- "Mes propositions" cote chauffeur (filtre transporteur_id + statut) ;
-- verification d'expiration a la lecture/reponse.
CREATE INDEX idx_proposition_mission_transporteur ON opt.proposition_mission (transporteur_id, statut);
CREATE INDEX idx_proposition_mission_demande ON opt.proposition_mission (demande_id);
