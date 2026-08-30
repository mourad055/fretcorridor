-- Diffusion-course (plan de reorientation) + infos marchandise/destinataire
-- (audit de suivi Mobile) : ces champs etaient connus au moment L1 (via
-- DemandeAvecCandidats/CandidatCoutDto) mais jamais persistes sur Affectation,
-- donc irrecuperables au moment de la confirmation differee (chauffeur qui
-- accepte plus tard) - AffectationConfirmationService en a besoin pour
-- construire AffectationConfirmeeEvent au bon moment.
ALTER TABLE opt.affectation
    ADD COLUMN vehicule_id UUID,
    ADD COLUMN type_emballage_nom VARCHAR(150),
    ADD COLUMN quantite INTEGER,
    ADD COLUMN destinataire_nom VARCHAR(150),
    ADD COLUMN destinataire_telephone VARCHAR(30),
    ADD COLUMN mode_collecte VARCHAR(30),
    ADD COLUMN type_disponibilite VARCHAR(30),
    ADD COLUMN poids_total_kg NUMERIC(10,2),
    ADD COLUMN grande_valeur BOOLEAN;
