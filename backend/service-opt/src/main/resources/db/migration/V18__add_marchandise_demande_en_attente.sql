-- Infos marchandise (audit de suivi Mobile) : jusqu'ici absentes de
-- demande_en_attente, jamais propagees vers Mission -- l'app Chauffeur ne
-- pouvait donc jamais savoir ce qu'elle transporte une fois la mission
-- creee.
ALTER TABLE opt.demande_en_attente ADD COLUMN type_emballage_nom VARCHAR(150);
ALTER TABLE opt.demande_en_attente ADD COLUMN quantite INTEGER;
