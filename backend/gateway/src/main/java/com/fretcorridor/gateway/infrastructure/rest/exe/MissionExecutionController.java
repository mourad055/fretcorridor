package com.fretcorridor.gateway.infrastructure.rest.exe;

import com.fretcorridor.gateway.domain.exe.MissionExecutionPort;
import com.fretcorridor.gateway.infrastructure.rest.exe.dto.AjouterEtapeRequest;
import com.fretcorridor.gateway.infrastructure.rest.exe.dto.MissionExecutionDetailResponse;
import com.fretcorridor.gateway.infrastructure.rest.exe.dto.MissionExecutionResponse;
import com.fretcorridor.gateway.infrastructure.rest.exe.dto.TourneeDetailResponse;
import com.fretcorridor.gateway.infrastructure.security.AuthenticatedActor;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * S7 (EF-EXE-02/04) : exécution de mission côté chauffeur/transporteur.
 * Reste vide en pratique tant que transporteurId n'est pas peuplé en amont
 * (écart documenté, cf. MissionExecutionPort).
 */
@RestController
@RequestMapping("/api/v1/missions")
public class MissionExecutionController {

    private final MissionExecutionPort missionExecutionPort;

    public MissionExecutionController(MissionExecutionPort missionExecutionPort) {
        this.missionExecutionPort = missionExecutionPort;
    }

    @GetMapping("/mes")
    public Flux<MissionExecutionResponse> mesMissions(@AuthenticationPrincipal AuthenticatedActor actor) {
        return missionExecutionPort.mesMissions(actor.delegationToken()).map(MissionExecutionResponse::from);
    }

    @GetMapping("/{missionId}")
    public Mono<MissionExecutionDetailResponse> chronologie(@PathVariable String missionId,
                                                              @AuthenticationPrincipal AuthenticatedActor actor) {
        return missionExecutionPort.chronologie(actor.delegationToken(), missionId).map(MissionExecutionDetailResponse::from);
    }

    @PostMapping(value = "/{missionId}/etapes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<MissionExecutionDetailResponse> ajouterEtape(@PathVariable String missionId,
                                                               @Valid @RequestBody AjouterEtapeRequest request,
                                                               @AuthenticationPrincipal AuthenticatedActor actor) {
        return missionExecutionPort.ajouterEtape(actor.delegationToken(), missionId, request.type(), request.libelle(),
                        request.horodatageCapture())
                .map(MissionExecutionDetailResponse::from);
    }

    // RG-070/EF-EXE-03 : PRISE_EN_CHARGE/LIVRAISON avec preuve minimale
    // obligatoire (photo(s) + signature tactile du tiers).
    @PostMapping(value = "/{missionId}/etapes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<MissionExecutionDetailResponse> ajouterEtapeAvecPreuve(
            @PathVariable String missionId,
            @RequestPart("type") String type,
            @RequestPart("libelle") String libelle,
            @RequestPart(value = "horodatageCapture", required = false) String horodatageCapture,
            @RequestPart(value = "photos", required = false) List<FilePart> photos,
            @RequestPart(value = "signature", required = false) FilePart signature,
            @AuthenticationPrincipal AuthenticatedActor actor) {
        return missionExecutionPort.ajouterEtapeAvecPreuve(actor.delegationToken(), missionId, type, libelle,
                        horodatageCapture, photos == null ? List.of() : photos, signature)
                .map(MissionExecutionDetailResponse::from);
    }

    // S11 : ordre planifié de la tournée (multi-étapes, LTL consolidé).
    @GetMapping("/tournees/{tourneeId}")
    public Mono<TourneeDetailResponse> tournee(@PathVariable String tourneeId,
                                                @AuthenticationPrincipal AuthenticatedActor actor) {
        return missionExecutionPort.tournee(actor.delegationToken(), tourneeId).map(TourneeDetailResponse::from);
    }
}
