-- Ajout transporteur_id/vehicule_id sur capacite_en_attente : ferme le bug S7
-- remonte par Personne 1 (AffectationConfirmeeEvent.transporteurId toujours
-- null - "Mes missions" vide cote chauffeur). Nullable : CapaciteDeclareeEvent
-- ne garantit pas encore ces champs tant que service-cap ne les publie pas
-- (contrat en cours de discussion avec Personne 1).
ALTER TABLE opt.capacite_en_attente
    ADD COLUMN transporteur_id UUID,
    ADD COLUMN vehicule_id UUID;
