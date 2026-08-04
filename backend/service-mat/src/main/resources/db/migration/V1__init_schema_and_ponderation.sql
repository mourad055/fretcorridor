-- Schema dedie a service-mat (isolation par service, cf Plan d'execution S4.1).
CREATE SCHEMA IF NOT EXISTS mat;

-- Modele de ponderation versionne (EF-MAT-04) : un ensemble complet de poids par
-- critere forme une "version". Un modele n'est JAMAIS modifie en place une fois
-- cree - changer les poids = creer une nouvelle version. C'est ce qui garantit
-- que chaque CycleMatching passe reste reconstructible a l'identique (EF-MAT-11)
-- meme longtemps apres que les poids ont evolue.
CREATE TABLE mat.modele_ponderation (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version         INTEGER NOT NULL,
    actif           BOOLEAN NOT NULL DEFAULT false,
    description     VARCHAR(255),
    date_creation   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_modele_ponderation_version UNIQUE (version)
);

-- Un seul modele actif a la fois : idiome Postgres (index unique partiel sur la
-- valeur true uniquement) plutot qu'un controle applicatif fragile qui pourrait
-- laisser deux modeles actifs simultanement en cas de bug.
CREATE UNIQUE INDEX idx_modele_ponderation_unique_actif
    ON mat.modele_ponderation (actif)
    WHERE actif = true;

-- Poids par critere pour un modele donne. code_critere reste une chaine libre
-- (pas un enum Java figé dans le code) : ajouter un nouveau critere de matching
-- (ex. "RETOUR_A_VIDE" en Phase 2) ne doit jamais necessiter de redeploiement,
-- juste une nouvelle ligne ici - anti-patron CDC S12.4 evite par construction.
CREATE TABLE mat.ponderation_critere (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    modele_id       UUID NOT NULL REFERENCES mat.modele_ponderation(id),
    code_critere    VARCHAR(50) NOT NULL,
    poids           NUMERIC(6,4) NOT NULL,
    CONSTRAINT uq_ponderation_modele_critere UNIQUE (modele_id, code_critere),
    CONSTRAINT chk_poids_positif CHECK (poids >= 0)
);

CREATE INDEX idx_ponderation_critere_modele ON mat.ponderation_critere (modele_id);

-- Modele V0 initial, actif des la creation du service : sans lui, le tout premier
-- appel tournerait en mode degrade des le depart. Poids indicatifs (a valider
-- avec l'equipe/le metier) - la somme fait 1.0 ici par choix de depart, mais ce
-- n'est pas une contrainte imposee en base (les poids n'ont pas obligation de
-- sommer a 1, seulement d'etre coherents entre eux).
INSERT INTO mat.modele_ponderation (id, version, actif, description)
VALUES ('00000000-0000-0000-0000-000000000001', 1, true,
        'Modele V0 initial - poids indicatifs a valider avec l''equipe');

INSERT INTO mat.ponderation_critere (modele_id, code_critere, poids) VALUES
    ('00000000-0000-0000-0000-000000000001', 'DISTANCE',   0.4000),
    ('00000000-0000-0000-0000-000000000001', 'DELAI',      0.3000),
    ('00000000-0000-0000-0000-000000000001', 'FIABILITE',  0.2000),
    ('00000000-0000-0000-0000-000000000001', 'PRIX',       0.1000);
