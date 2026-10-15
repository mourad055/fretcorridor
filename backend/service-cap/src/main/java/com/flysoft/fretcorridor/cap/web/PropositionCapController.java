package com.flysoft.fretcorridor.cap.web;

import com.flysoft.fretcorridor.cap.client.ServiceOptClient;
import com.flysoft.fretcorridor.cap.messaging.CapEventPublisher;
import com.flysoft.fretcorridor.cap.messaging.DemandeAccepteeEvent;
import com.flysoft.fretcorridor.cap.messaging.DemandeRefuseeParChauffeurEvent;
import com.flysoft.fretcorridor.cap.security.JwtService;
import com.flysoft.fretcorridor.cap.web.dto.PropositionCapResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * UC-MAT-02/diffusion-course (30/08) : "mes propositions" cote app
 * Chauffeur/Transporteur. Lecture via {@link ServiceOptClient} (GET
 * synchrone OPT, entorse deliberee et bilaterale documentee dans
 * application.yml) ; ecriture (accepter/refuser) via evenements Kafka
 * publies par {@link CapEventPublisher}, contrats
 * shared-contracts/asyncapi/events/demande-{acceptee,refusee-par-chauffeur}.yaml.
 *
 * transporteurId toujours resolu depuis le JWT (jamais depuis le corps de
 * requete) -- meme principe que CapaciteController (IDOR, audit CDC 19 aout).
 */
@RestController
@RequestMapping("/api/cap/propositions")
public class PropositionCapController {

    private final ServiceOptClient serviceOptClient;
    private final CapEventPublisher capEventPublisher;
    private final JwtService jwtService;

    public PropositionCapController(ServiceOptClient serviceOptClient, CapEventPublisher capEventPublisher,
                                     JwtService jwtService) {
        this.serviceOptClient = serviceOptClient;
        this.capEventPublisher = capEventPublisher;
        this.jwtService = jwtService;
    }

    @GetMapping("/mes")
    public List<PropositionCapResponse> mesPropositions(@RequestHeader("Authorization") String authHeader) {
        UUID transporteurId = jwtService.extraireActeurId(authHeader.substring(7));
        return serviceOptClient.listerPropositions(transporteurId).stream()
                .map(PropositionCapResponse::from)
                .toList();
    }

    @PostMapping("/{affectationId}/accepter")
    public void accepter(@PathVariable UUID affectationId, @RequestBody(required = false) AffecterRequest requete,
                          @RequestHeader("Authorization") String authHeader) {
        UUID transporteurId = jwtService.extraireActeurId(authHeader.substring(7));
        if (requete == null || requete.demandeId() == null || requete.capaciteId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "demandeId et capaciteId requis");
        }
        capEventPublisher.publierDemandeAcceptee(new DemandeAccepteeEvent(
                UUID.randomUUID(), affectationId, requete.demandeId(), requete.capaciteId(), transporteurId));
    }

    @PostMapping("/{affectationId}/refuser")
    public void refuser(@PathVariable UUID affectationId, @RequestBody(required = false) AffecterRequest requete,
                         @RequestHeader("Authorization") String authHeader) {
        UUID transporteurId = jwtService.extraireActeurId(authHeader.substring(7));
        if (requete == null || requete.demandeId() == null || requete.capaciteId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "demandeId et capaciteId requis");
        }
        capEventPublisher.publierDemandeRefuseeParChauffeur(new DemandeRefuseeParChauffeurEvent(
                UUID.randomUUID(), affectationId, requete.demandeId(), requete.capaciteId(), transporteurId));
    }

    // demandeId/capaciteId transmis par l'app (deja connus du GET /mes) plutot
    // que reconstitues cote serveur -- evite un aller-retour supplementaire
    // vers OPT juste pour retrouver ces deux identifiants avant publication.
    // motif (RG-050, liste courte de motifs de refus) volontairement absent :
    // hors perimetre du contrat demande-refusee-par-chauffeur.yaml (pas de
    // champ prevu cote OPT) -- reste une question posee a l'utilisateur cote
    // app pour l'UX, sans persistance backend a ce jour.
    public record AffecterRequest(UUID demandeId, UUID capaciteId) {
    }
}
