-- EF-GEO-01 : "rattacher toute mission a un axe" - opt.affectation ne
-- portait jusqu'ici aucun axe_id, alors que l'axe est deja connu au moment
-- du calcul (demande.axeId(), utilise pour la tarification) et simplement
-- jamais persiste sur le resultat.
ALTER TABLE opt.affectation
    ADD COLUMN axe_id UUID;

CREATE INDEX idx_affectation_axe ON opt.affectation (axe_id);
