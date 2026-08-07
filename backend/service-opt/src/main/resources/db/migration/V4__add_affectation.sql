-- Persistance de l'affectation confirmee par L1 (EF-MAT-01/02/03).
-- Cette table comble le trou d'architecture identifie : TRK n'a aucun moyen
-- de connaitre l'origine/destination d'une mission tant que rien n'est
-- persiste ici. L'id genere ci-dessous devient le "mission_id" que TRK
-- appellera en synchrone interne (meme porteur, cf regle de communication
-- du perimetre Moteur) pour recuperer ces coordonnees et calculer son ETA.
--
-- A ne jamais confondre avec l'evenement Kafka AffectationConfirmee
-- (OPT -> Mobile/service-exe, cross-porteur donc strictement asynchrone) :
-- cette table est la source de verite interne au perimetre Moteur, la
-- publication Kafka en est une consequence separee, pas remplacee par elle.
CREATE TABLE opt.affectation (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    demande_id                      UUID NOT NULL,
    capacite_id                     UUID NOT NULL,
    cycle_matching_id               UUID,

    -- Colonnes simples (pas de type geometry PostGIS) : OPT n'a besoin
    -- d'aucune requete spatiale sur cette table, juste stocker/relire pour
    -- TRK - ajouter hibernate-spatial ici serait une dependance inutile.
    origine_latitude                DOUBLE PRECISION NOT NULL,
    origine_longitude               DOUBLE PRECISION NOT NULL,
    destination_latitude            DOUBLE PRECISION NOT NULL,
    destination_longitude           DOUBLE PRECISION NOT NULL,

    -- Itineraire Valhalla : nullable, car un itineraire peut etre en mode
    -- degrade (Valhalla injoignable) meme si l'affectation elle-meme est
    -- valide - cf AffectationResultat, ne jamais confondre les deux cas.
    distance_metres                 DOUBLE PRECISION,
    duree_secondes                  DOUBLE PRECISION,
    intervalle_confiance_secondes   DOUBLE PRECISION,
    geometrie_encodee               TEXT,

    cout_total                      NUMERIC(12,4) NOT NULL,

    -- Tarification L4 (CDC S8.9) : meme structure a plat que TarificationResultat,
    -- toutes nullable individuellement puisque le mode degrade de la
    -- tarification peut laisser prix_transport a null sans que le reste
    -- (bareme_id, regime) le soit forcement.
    bareme_id                       UUID,
    bareme_version                  INTEGER,
    regime                          VARCHAR(50),
    cout_base                       NUMERIC(12,4),
    cout_variable_poids_taxable     NUMERIC(12,4),
    cout_services                   NUMERIC(12,4),
    facteur_tension_applique        NUMERIC(12,4),
    prix_transport_avant_plancher   NUMERIC(12,4),
    plancher_applique               BOOLEAN,
    prix_transport                  NUMERIC(12,4),
    commission_plateforme           NUMERIC(12,4),
    montant_verse_transporteur      NUMERIC(12,4),
    tarification_mode_degrade       BOOLEAN NOT NULL DEFAULT false,

    date_creation                   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Recherches frequentes attendues : "l'affectation pour telle demande",
-- "l'historique des affectations d'une capacite".
CREATE INDEX idx_affectation_demande_id ON opt.affectation (demande_id);
CREATE INDEX idx_affectation_capacite_id ON opt.affectation (capacite_id);
