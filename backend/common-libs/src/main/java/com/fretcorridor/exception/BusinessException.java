package com.fretcorridor.exception;

/**
 * Exception métier - erreur fonctionnelle (règle de gestion violée).
 * Utilisée par tous les services pour les erreurs métier.
 */
public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
