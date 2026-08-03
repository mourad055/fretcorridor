package com.fretcorridor.gateway.infrastructure.rest;

import com.fretcorridor.gateway.domain.InvalidCredentialsException;
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
}
