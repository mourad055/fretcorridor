-- Diffusion-course (ajouts Mobile, compte a rebours) : libelles d'origine et
-- de destination pour l'affichage cote app chauffeur (connus au moment L1 via
-- DemandeAvecCandidats, comme PropositionEmiseEvent origineNom/destinationNom,
-- mais jamais persistes) + horodatage d'expiration de la proposition.
--
-- expire_a : poser a la creation de l'Affectation PROPOSEE (dateCreation +
-- fretcorridor.opt.proposition-expiration-ms, defaut 15 min). Une tache
-- planifiee (ExpirationPropositionService) la passe EXPIREE une fois depassee
-- pour eviter les propositions "fantomes" et liberer la capacite pour le
-- prochain cycle de matching.
ALTER TABLE opt.affectation
    ADD COLUMN origine_nom VARCHAR(200),
    ADD COLUMN destination_nom VARCHAR(200),
    ADD COLUMN expire_a TIMESTAMPTZ;
