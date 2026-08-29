-- Diffusion-course (plan de reorientation post-demo) : une Affectation
-- n'est plus committee immediatement a la creation - elle est PROPOSEE a
-- chaque candidat compatible, puis CONFIRMEE (premier accepte, atomique)
-- ou EXPIREE (perdant de la course, ou refus explicite du chauffeur).
ALTER TABLE opt.affectation
    ADD COLUMN statut VARCHAR(20) NOT NULL DEFAULT 'CONFIRMEE';
-- DEFAULT 'CONFIRMEE' uniquement pour les lignes deja existantes (avant ce
-- correctif, toute Affectation etait committee directement) - les nouvelles
-- lignes creees par AffectationL1Service passeront explicitement PROPOSEE.

CREATE INDEX idx_affectation_demande_statut ON opt.affectation (demande_id, statut);
-- Index cible : retrouver rapidement "les autres propositions de cette
-- demande" au moment ou l'une d'elles est acceptee (pour les marquer
-- EXPIREE) - requete frequente, sur le chemin critique de la course.
