-- Schema dedie a service-trk (isolation par service, cf Plan d'execution S4.1).
-- Vide pour l'instant, meme logique que service-opt V1 : le modele de donnees
-- reel (Position, EcartTrajectoire) arrive au point 2 du plan, une fois le
-- contrat PositionBrute fixe (point 1 termine d'abord).
CREATE SCHEMA IF NOT EXISTS trk;
