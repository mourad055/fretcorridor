package com.flysoft.fretcorridor.ida.service;

/**
 * Envoi de SMS — abstraction pour permettre un vrai fournisseur (Twilio,
 * Africa's Talking...) sans changer les appelants. Aucun fournisseur réel
 * n'est intégré dans ce dépôt (pas de compte/API key disponible) ; voir
 * {@link LogSmsAdapter} pour l'implémentation de développement.
 */
public interface SmsPort {
    void envoyer(String telephone, String message);
}
