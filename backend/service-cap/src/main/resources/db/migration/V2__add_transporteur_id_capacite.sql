-- Ferme le bug S7 remonte par Personne 1 (Mobile) - AffectationConfirmeeEvent
-- .transporteurId toujours null cote OPT, faute de donnee source ici.
-- Resolu via service-flt (proprietaireActeurId de Vehicule) au moment de la
-- declaration (CapaciteService.declarer -> ServiceFltClient). Nullable :
-- service-flt injoignable ou vehicule non enregistre ne doit jamais bloquer
-- une declaration de capacite (ENF-DIS-04) - degrade seulement en aval
-- (AffectationConfirmeeEvent.transporteurId reste null dans ce cas).
ALTER TABLE cap.capacite
    ADD COLUMN transporteur_id UUID;
