-- Schema dedie "opt", isole des autres services (cf Plan d'execution S4.1).
-- Vide pour l'instant : la premiere brique (filtrage L0) n'a pas encore besoin
-- de persistance - elle interroge service-geo en direct et retourne le resultat
-- au demandeur, sans rien stocker. L'entite CycleMatching (traçabilite des
-- decisions, EF-MAT-11/12) arrivera avec service-mat, pas ici.
CREATE SCHEMA IF NOT EXISTS opt;
