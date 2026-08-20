package com.fretcorridor.pay.infrastructure.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fretcorridor.pay.domain.NotificationPrestataireService;
import com.fretcorridor.pay.infrastructure.rest.dto.NotificationEncaissementPayload;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * EF-PAY-05 : point d'entrée des notifications entrantes du prestataire de
 * paiement (confirmation d'encaissement). Signature HMAC obligatoire, clé
 * d'idempotence obligatoire — voir {@link NotificationPrestataireService}.
 * Verrou V2 non levé (aucun prestataire agréé sélectionné) : ce endpoint est
 * prêt à recevoir de vraies notifications, mais rien ne les émet encore en
 * environnement de développement.
 */
@RestController
@RequestMapping("/api/v1/pay/webhooks")
public class WebhookPrestataireController {

    private final NotificationPrestataireService notificationService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public WebhookPrestataireController(NotificationPrestataireService notificationService, ObjectMapper objectMapper, Validator validator) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @PostMapping("/prestataire")
    public ResponseEntity<Object> recevoir(
            @RequestHeader("X-Prestataire-Signature") String signature,
            @RequestHeader("X-Prestataire-Idempotency-Key") String idempotenceKey,
            @RequestBody String corpsBrut) {

        NotificationEncaissementPayload payload;
        try {
            payload = objectMapper.readValue(corpsBrut, NotificationEncaissementPayload.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(problemeCorpsInvalide());
        }

        Set<ConstraintViolation<NotificationEncaissementPayload>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            return ResponseEntity.badRequest().body(problemeCorpsInvalide());
        }

        // DEJA_TRAITEE renvoie 200 comme TRAITEE : du point de vue du prestataire, le
        // rejeu a bien été « reçu » — dupliquer l'écriture serait l'erreur, pas la réponse HTTP.
        notificationService.traiter(corpsBrut, signature, idempotenceKey, payload.versDomaine());
        return ResponseEntity.ok().build();
    }

    private ProblemDetail problemeCorpsInvalide() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Corps de notification invalide");
        problem.setTitle("Notification prestataire invalide");
        return problem;
    }
}
