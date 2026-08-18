-- EF-MAT-08/09, ENF-SEC-03 (idempotence) : trace la premiere execution de la
-- livraison pour une Affectation FTL simple (jamais sequencee en Tournee -
-- cf SequencementDeclencheur, capacite non consolidee). Sans cette colonne,
-- un EtapeExecuteeEvent(LIVRAISON) redelivre publierait PropositionRetourAVideEvent
-- en double.
ALTER TABLE opt.affectation
    ADD COLUMN livraison_executee_le TIMESTAMPTZ;
