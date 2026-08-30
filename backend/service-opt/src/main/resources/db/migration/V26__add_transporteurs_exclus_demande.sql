-- Diffusion-course (plan de reorientation, partie Chauffeur point 2) :
-- liste des transporteurs ayant refuse explicitement CETTE demande
-- (DemandeRefuseeParChauffeur). Persistee en JSONB, cumulee a chaque refus,
-- et consommee par MatchingCycleService pour ecarter les capacites de ces
-- chauffeurs du prochain cycle sur cette demande : on ne re-diffuse jamais
-- sur la meme demande a un chauffeur qui vient de refuser.
ALTER TABLE opt.demande_en_attente ADD COLUMN transporteurs_exclus jsonb;
