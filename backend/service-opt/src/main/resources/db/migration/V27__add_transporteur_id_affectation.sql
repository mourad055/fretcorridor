-- Diffusion-course (reponse a la trouvaille Mobile) : le chauffeur doit
-- pouvoir connaitre les propositions qui lui ont ete diffusees. Le lien
-- transporteur <-> proposition etait indirect (Affectation.capaciteId ->
-- CapaciteEnAttente.transporteurId, nullable et eventuellement incomplet),
-- ce qui interdisait un filtre fiable "mes propositions en attente".
-- Denormalisation : transporteurId porte directement sur l'Affectation,
-- renseigne a la creation depuis CandidatCoutDto.transporteurId (source de
-- verite du "diffuse a qui"). Nullable = une capacite sans transporteur
-- identifie n'expose pas de propositions (tolérance identique a
-- CapaciteDeclareeEvent.transporteurId).
ALTER TABLE opt.affectation ADD COLUMN transporteur_id uuid;

-- Index pour le GET /api/opt/affectations/proposees?transporteurId=
CREATE INDEX idx_affectation_transporteur_statut
    ON opt.affectation (transporteur_id, statut);
