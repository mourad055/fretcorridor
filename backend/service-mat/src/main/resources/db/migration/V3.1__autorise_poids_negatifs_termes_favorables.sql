-- Corrige chk_poids_positif (V1), devenue incompatible avec la convention de
-- signe introduite en V4 (CDC S8.5.3) : les termes FAVORABLES (gain
-- remplissage, fiabilite, valeur retour) sont deliberement encodes en poids
-- NEGATIF pour que CoutCompositeService (somme purement additive) les
-- soustraie correctement - convention documentee explicitement dans le
-- commentaire de V4 (RG-106 : "la convention de signe est portee par les
-- donnees, pas par le code").
--
-- V1 imposait poids >= 0, ecrite avant que cette convention n'existe -
-- jamais modifiee retroactivement (Flyway, principe deja applique partout
-- ailleurs dans ce projet), corrigee ici par une nouvelle contrainte.
--
-- Borne symetrique [-10, 10] plutot qu'aucune limite : RG-106 rend ces
-- poids configurables/versionnes en exploitation (axe par axe), une borne
-- large reste un garde-fou contre une valeur aberrante par erreur de
-- saisie, sans contraindre les poids actuels (max observe : 2.0000 sur
-- RISQUE_AXE, V4).

ALTER TABLE mat.ponderation_critere
    DROP CONSTRAINT chk_poids_positif;

ALTER TABLE mat.ponderation_critere
    ADD CONSTRAINT chk_poids_borne CHECK (poids BETWEEN -10 AND 10);
