package com.flysoft.fretcorridor.ida.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Stockage objet MinIO pour les pièces justificatives KYC (EF-IDA-03).
 * Les objets restent privés ; l'accès se fait via URLs présignées à durée
 * limitée, jamais d'URL publique stockée en base.
 *
 * Écart connu : RG-014 exige un chiffrement au repos avec une clé distincte
 * de celle des données d'exploitation — non implémenté ici (MinIO stocke en
 * clair côté objet). À traiter séparément si ce niveau de garantie devient
 * requis avant la mise en production.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.presign-expiry-minutes:15}")
    private int presignExpiryMinutes;

    /** Dépose une pièce KYC et retourne la clé objet (stockée en BDD, jamais d'URL). */
    public String deposer(String tenantId, UUID acteurId, String typeDocument, MultipartFile fichier) {
        try {
            String objectKey = tenantId + "/" + acteurId + "/"
                    + typeDocument.toLowerCase() + "_" + System.currentTimeMillis() + extensionDe(fichier.getOriginalFilename());

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(fichier.getInputStream(), fichier.getSize(), -1)
                    .contentType(fichier.getContentType() != null ? fichier.getContentType() : "application/octet-stream")
                    .build());

            log.info("Pièce KYC déposée MinIO : {}", objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("Échec dépôt MinIO : {}", e.getMessage());
            throw new RuntimeException("STOCKAGE_INDISPONIBLE");
        }
    }

    /** URL présignée pour consultation temporaire (jamais stockée telle quelle). */
    public String urlAcces(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(presignExpiryMinutes, TimeUnit.MINUTES)
                            .build());
        } catch (Exception e) {
            log.warn("Impossible de générer une URL présignée pour {} : {}", objectKey, e.getMessage());
            return null;
        }
    }

    private static String extensionDe(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".bin";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
