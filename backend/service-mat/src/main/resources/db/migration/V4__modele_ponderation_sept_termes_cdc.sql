-- Fonction de cout composite complete (CDC S8.5.3) - les 7 termes reels.
--
--   COUT = a1 x KM_APPROCHE      + a2 x KM_DETOUR       + a3 x ECART_TEMPOREL
--        - a4 x GAIN_REMPLISSAGE - a5 x FIABILITE       + a6 x RISQUE_AXE
--        - a7 x VALEUR_RETOUR
--
-- Le modele V0 de V1 (DISTANCE/DELAI/FIABILITE/PRIX) etait une somme ponderee
-- generique alimentee par des placeholders cap (tout a 0.5) : aucune paire ne
-- se distinguait. Ce V4 le remplace comme modele PAR DEFAUT actif (axe_id NULL)
-- avec les codes de criteres du CDC, calcules cote OPT par paire demande/
-- capacite depuis le branchement L0/criteres (CalculateurCoutSeptTermes).
--
-- CONVENTION DE SIGNE : le moteur MAT est une somme ponderee purement additive
-- (CoutCompositeService.calculerCoutCandidat). Les termes FAVORABLES du CDC
-- (signe "-" dans la formule) sont donc encodes par un POIDS NEGATIF ici -
-- la convention de signe est porteuse par les donnees (RG-106), pas par le code.
--
-- Poids initiaux (a1..a7) : valeurs de calage raisonnables, PAS des valeurs du
-- CDC. RG-106 les rend configurables par axe et versionnes - calibrer en
-- exploitation, ne jamais modifier ce modele en place mais creer une nouvelle
-- version active.
--
-- KM_DETOUR est present avec un poids non nul des maintenant : il vaut 0.0 au
-- stade L1 (pas de tournee existante avant sequencement - hypothese documentee
-- dans CalculateurCoutSeptTermes), le poids s'activera naturellement quand le
-- terme sera evalue reellement apres ALNS/L2.

UPDATE mat.modele_ponderation
SET actif = false
WHERE axe_id IS NULL AND actif = true;

INSERT INTO mat.modele_ponderation (id, version, axe_id, actif, description)
VALUES ('00000000-0000-0000-0000-000000000002', 2, NULL, true,
        'CDC S8.5.3 - 7 termes reels calcules par paire cote OPT (V4)');

INSERT INTO mat.ponderation_critere (modele_id, code_critere, poids) VALUES
    -- Termes defavorables (poids positifs)
    ('00000000-0000-0000-0000-000000000002', 'KM_APPROCHE',     1.0000),
    ('00000000-0000-0000-0000-000000000002', 'KM_DETOUR',       0.8000),
    ('00000000-0000-0000-0000-000000000002', 'ECART_TEMPOREL',  0.5000),  -- heures hors fenetre
    ('00000000-0000-0000-0000-000000000002', 'RISQUE_AXE',      2.0000),  -- score [0,1] securitaire
    -- Termes favorables (poids negatifs = soustraction CDC)
    ('00000000-0000-0000-0000-000000000002', 'GAIN_REMPLISSAGE', -3.0000), -- ratio [0,1] remplissage
    ('00000000-0000-0000-0000-000000000002', 'FIABILITE',        -1.0000), -- indice [0,1] transporteur
    ('00000000-0000-0000-0000-000000000002', 'VALEUR_RETOUR',    -0.5000); -- score [0,1] fret au retour
