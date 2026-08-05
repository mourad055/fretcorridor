-- Bareme tarifaire versionne (CDC S8.9, Couche L4 - Tarification). Meme
-- principe d'immuabilite que mat.modele_ponderation : une correction de
-- tarif cree une nouvelle version, ne modifie jamais une ligne existante -
-- condition de l'explicabilite (RG-115) sur les prix deja affiches/factures.
--
-- RG-112 : aucun bareme en dur. Cette table EST le bareme - le code Java
-- (TarificationL4Service) ne fait qu'evaluer les valeurs qu'elle contient.
CREATE TABLE opt.bareme_tarification (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- NULL = bareme par defaut (pas specifique a un axe). Cf EF-GEO-02.
    axe_id                      UUID,
    version                     INTEGER NOT NULL,
    actif                       BOOLEAN NOT NULL DEFAULT false,
    -- RG-102 : coexistence des regimes poids taxable / forfaitaire vehicule.
    regime                      VARCHAR(30) NOT NULL
                                    CHECK (regime IN ('POIDS_TAXABLE', 'FORFAITAIRE_VEHICULE')),
    cout_base_par_km            NUMERIC(12,4),
    cout_socle_forfaitaire      NUMERIC(12,4),
    cout_unitaire_poids_taxable NUMERIC(12,4),
    -- RG-113 : plancher activable par axe, desactive par defaut.
    prix_plancher_actif         BOOLEAN NOT NULL DEFAULT false,
    prix_plancher                NUMERIC(12,4),
    -- RG-114 : bornes du facteur de tension marche (fraction, ex. -0.1000 a 0.3000).
    tension_min_fraction        NUMERIC(5,4) NOT NULL DEFAULT 0,
    tension_max_fraction        NUMERIC(5,4) NOT NULL DEFAULT 0,
    CHECK (tension_min_fraction <= tension_max_fraction),
    -- RG-116 : commission separee, jamais fondue dans le prix transport.
    commission_taux_fraction    NUMERIC(5,4) NOT NULL DEFAULT 0,
    description                 VARCHAR(255),
    date_creation                TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Au plus un bareme actif par axe, et au plus un bareme actif par defaut
-- (axe_id NULL) - COALESCE vers un UUID sentinelle car Postgres ne
-- deduplique pas NULL par defaut dans un index unique classique.
CREATE UNIQUE INDEX idx_bareme_tarification_actif_par_axe
    ON opt.bareme_tarification (COALESCE(axe_id, '00000000-0000-0000-0000-000000000000'))
    WHERE actif = true;

-- Lignes de cout de service (manutention, attente, garde - S8.9.1,
-- SOMME(COUT_SERVICES)). Meme logique anti-bareme-en-dur que
-- mat.ponderation_critere.
CREATE TABLE opt.composant_cout_service (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bareme_id   UUID NOT NULL REFERENCES opt.bareme_tarification(id),
    code_service VARCHAR(50) NOT NULL,
    montant     NUMERIC(12,4) NOT NULL
);

CREATE INDEX idx_composant_cout_service_bareme ON opt.composant_cout_service (bareme_id);
