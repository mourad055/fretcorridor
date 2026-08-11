package com.fretcorridor.pay.infrastructure.rest;

import com.fretcorridor.pay.domain.GarantieInvalideException;
import com.fretcorridor.pay.domain.ReversementSansEncaissementException;
import com.fretcorridor.pay.domain.SequestreInvalideException;
import com.fretcorridor.pay.domain.SignatureInvalideException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReversementSansEncaissementException.class)
    public ProblemDetail handleReversementSansEncaissement(ReversementSansEncaissementException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Reversement refusé (ENF-FIN-02)");
        return problem;
    }

    @ExceptionHandler(SequestreInvalideException.class)
    public ProblemDetail handleSequestreInvalide(SequestreInvalideException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Transition de séquestre invalide");
        return problem;
    }

    @ExceptionHandler(GarantieInvalideException.class)
    public ProblemDetail handleGarantieInvalide(GarantieInvalideException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Souscription de garantie invalide");
        return problem;
    }

    @ExceptionHandler(SignatureInvalideException.class)
    public ProblemDetail handleSignatureInvalide(SignatureInvalideException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Notification prestataire rejetée (EF-PAY-05)");
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
