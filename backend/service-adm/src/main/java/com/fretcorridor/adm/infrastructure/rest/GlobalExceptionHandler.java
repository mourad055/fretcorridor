package com.fretcorridor.adm.infrastructure.rest;

import com.fretcorridor.adm.domain.AccesRefuseException;
import com.fretcorridor.adm.domain.DossierDejaTrancheException;
import com.fretcorridor.adm.domain.DossierIntrouvableException;
import com.fretcorridor.adm.domain.DossierNonTrancheException;
import com.fretcorridor.adm.domain.GrilleDecisionAbsenteException;
import com.fretcorridor.adm.domain.RecoursMemeOperateurException;
import com.fretcorridor.adm.domain.TenantDejaExistantException;
import com.fretcorridor.adm.domain.TenantIntrouvableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DossierIntrouvableException.class)
    public ProblemDetail handleDossierIntrouvable(DossierIntrouvableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Dossier introuvable");
        return problem;
    }

    @ExceptionHandler(DossierDejaTrancheException.class)
    public ProblemDetail handleDossierDejaTranche(DossierDejaTrancheException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Dossier déjà clos");
        return problem;
    }

    @ExceptionHandler(GrilleDecisionAbsenteException.class)
    public ProblemDetail handleGrilleDecisionAbsente(GrilleDecisionAbsenteException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Grille de décision absente");
        return problem;
    }

    @ExceptionHandler(DossierNonTrancheException.class)
    public ProblemDetail handleDossierNonTranche(DossierNonTrancheException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Dossier pas encore tranché");
        return problem;
    }

    @ExceptionHandler(RecoursMemeOperateurException.class)
    public ProblemDetail handleRecoursMemeOperateur(RecoursMemeOperateurException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Opérateur identique au premier décideur");
        return problem;
    }

    @ExceptionHandler(TenantDejaExistantException.class)
    public ProblemDetail handleTenantDejaExistant(TenantDejaExistantException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Tenant déjà existant");
        return problem;
    }

    @ExceptionHandler(TenantIntrouvableException.class)
    public ProblemDetail handleTenantIntrouvable(TenantIntrouvableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Tenant introuvable");
        return problem;
    }

    @ExceptionHandler(AccesRefuseException.class)
    public ProblemDetail handleAccesRefuse(AccesRefuseException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Accès refusé");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " : " + error.getDefaultMessage())
                .orElse("Requête invalide");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Requête invalide");
        return problem;
    }
}
