-- Bug bloquant (audit CDC v4, 20/08) : UNIQUE(event_id) seul faisait
-- silencieusement disparaitre tout lot au-dela du premier pour un evenement
-- DemandePublieeLots a plusieurs lots - le 2e lot (et suivants) declenchait
-- DataIntegrityViolationException, capturee et loguee a tort comme
-- "deja ingere (idempotence)" alors que c'etait un lot DIFFERENT jamais
-- persiste. La cle d'idempotence correcte est la PAIRE (event_id, lot_id) :
-- un meme evenement rejoue avec les memes lots reste detecte comme doublon,
-- mais deux lots distincts du meme evenement ne se bloquent plus l'un l'autre.
ALTER TABLE opt.lot_demande DROP CONSTRAINT uq_lot_demande_event;
ALTER TABLE opt.lot_demande ADD CONSTRAINT uq_lot_demande_event_lot UNIQUE (event_id, lot_id);
