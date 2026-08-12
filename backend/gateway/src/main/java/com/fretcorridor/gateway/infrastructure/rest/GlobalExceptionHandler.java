package com.fretcorridor.gateway.infrastructure.rest;

import com.fretcorridor.gateway.domain.AuthenticationServiceUnavailableException;
import com.fretcorridor.gateway.domain.InvalidCredentialsException;
import com.fretcorridor.gateway.domain.agent.AgentServiceIndisponibleException;
import com.fretcorridor.gateway.domain.agent.EnrolementIntrouvableException;
import com.fretcorridor.gateway.domain.agent.EnrolementRefuseException;
import com.fretcorridor.gateway.domain.cap.CapServiceIndisponibleException;
import com.fretcorridor.gateway.domain.cap.CapaciteRefuseeException;
import com.fretcorridor.gateway.domain.flt.FltServiceIndisponibleException;
import com.fretcorridor.gateway.domain.flt.PositionRefuseeException;
import com.fretcorridor.gateway.domain.flt.VehiculeRefuseException;
import com.fretcorridor.gateway.domain.ida.ProfilCompletionRefuseeException;
import com.fretcorridor.gateway.domain.ida.ProfilServiceIndisponibleException;
import com.fretcorridor.gateway.domain.kyc.DecisionInvalideException;
import com.fretcorridor.gateway.domain.kyc.KycDossierIntrouvableException;
import com.fretcorridor.gateway.infrastructure.rest.ida.ProfilController;
import com.fretcorridor.gateway.infrastructure.rest.kyc.KycController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

/**
 * Erreurs au format RFC 7807 (PRD §7.3). "L'erreur est un état normal du système" (EUX P6) :
 * le message nomme la cause, jamais une trace technique.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Authentification refusée");
        return problem;
    }

    @ExceptionHandler(AuthenticationServiceUnavailableException.class)
    public ProblemDetail handleAuthenticationServiceUnavailable(AuthenticationServiceUnavailableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("Service d'authentification indisponible");
        return problem;
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ProblemDetail handleValidation(WebExchangeBindException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("Requête invalide");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Requête invalide");
        return problem;
    }

    @ExceptionHandler(KycDossierIntrouvableException.class)
    public ProblemDetail handleKycDossierIntrouvable(KycDossierIntrouvableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Dossier introuvable");
        return problem;
    }

    @ExceptionHandler(DecisionInvalideException.class)
    public ProblemDetail handleDecisionInvalide(DecisionInvalideException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Décision invalide");
        return problem;
    }

    @ExceptionHandler(KycController.MissingIdempotencyKeyException.class)
    public ProblemDetail handleMissingIdempotencyKey(KycController.MissingIdempotencyKeyException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("En-tête manquant");
        return problem;
    }

    @ExceptionHandler(ProfilCompletionRefuseeException.class)
    public ProblemDetail handleProfilCompletionRefusee(ProfilCompletionRefuseeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Complétion de profil refusée");
        return problem;
    }

    @ExceptionHandler(ProfilServiceIndisponibleException.class)
    public ProblemDetail handleProfilServiceIndisponible(ProfilServiceIndisponibleException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("Service d'identité indisponible");
        return problem;
    }

    @ExceptionHandler(ProfilController.PieceManquanteException.class)
    public ProblemDetail handlePieceManquante(ProfilController.PieceManquanteException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Requête invalide");
        return problem;
    }

    @ExceptionHandler(EnrolementRefuseException.class)
    public ProblemDetail handleEnrolementRefuse(EnrolementRefuseException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Enrôlement refusé");
        return problem;
    }

    @ExceptionHandler(EnrolementIntrouvableException.class)
    public ProblemDetail handleEnrolementIntrouvable(EnrolementIntrouvableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Enrôlement introuvable");
        return problem;
    }

    @ExceptionHandler(AgentServiceIndisponibleException.class)
    public ProblemDetail handleAgentServiceIndisponible(AgentServiceIndisponibleException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("Service d'identité indisponible");
        return problem;
    }

    @ExceptionHandler(VehiculeRefuseException.class)
    public ProblemDetail handleVehiculeRefuse(VehiculeRefuseException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Véhicule refusé");
        return problem;
    }

    @ExceptionHandler(FltServiceIndisponibleException.class)
    public ProblemDetail handleFltServiceIndisponible(FltServiceIndisponibleException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("Service de suivi indisponible");
        return problem;
    }

    @ExceptionHandler(PositionRefuseeException.class)
    public ProblemDetail handlePositionRefusee(PositionRefuseeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Position refusée");
        return problem;
    }

    @ExceptionHandler(CapaciteRefuseeException.class)
    public ProblemDetail handleCapaciteRefusee(CapaciteRefuseeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Déclaration de capacité refusée");
        return problem;
    }

    @ExceptionHandler(CapServiceIndisponibleException.class)
    public ProblemDetail handleCapServiceIndisponible(CapServiceIndisponibleException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("Service de capacité indisponible");
        return problem;
    }
}
