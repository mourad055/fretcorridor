package com.flysoft.fretcorridor.ida.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implémentation de développement : journalise le SMS au lieu de l'envoyer.
 * À remplacer par un vrai fournisseur avant la mise en production —
 * aucun compte SMS n'est disponible pour ce dépôt (cf. SmsPort).
 */
@Component
@Slf4j
public class LogSmsAdapter implements SmsPort {

    @Override
    public void envoyer(String telephone, String message) {
        log.info("[SMS SIMULÉ] à {} : {}", telephone, message);
    }
}
