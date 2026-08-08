package com.fretcorridor.pay.infrastructure.security;

import com.fretcorridor.pay.domain.SignatureVerifierPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.util.HexFormat;

/**
 * EF-PAY-05 : HMAC-SHA256 du corps brut de la requête, comparaison en temps
 * constant (évite une fuite de temporisation qui faciliterait la falsification).
 * Secret centralisé via variable d'environnement, jamais dans le code (ENF-SEC-05).
 */
@Component
public class HmacSha256SignatureVerifierAdapter implements SignatureVerifierPort {

    private static final String ALGORITHME = "HmacSHA256";

    private final byte[] secret;

    public HmacSha256SignatureVerifierAdapter(
            @Value("${fretcorridor.pay.webhook-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean estValide(String corpsBrut, String signatureRecue) {
        if (signatureRecue == null || signatureRecue.isBlank()) {
            return false;
        }
        String signatureAttendue = signer(corpsBrut);
        return MessageDigest.isEqual(
                signatureAttendue.getBytes(StandardCharsets.UTF_8),
                signatureRecue.getBytes(StandardCharsets.UTF_8));
    }

    private String signer(String corpsBrut) {
        try {
            Mac mac = Mac.getInstance(ALGORITHME);
            mac.init(new SecretKeySpec(secret, ALGORITHME));
            byte[] digest = mac.doFinal(corpsBrut.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Impossible de calculer la signature HMAC", e);
        }
    }
}
