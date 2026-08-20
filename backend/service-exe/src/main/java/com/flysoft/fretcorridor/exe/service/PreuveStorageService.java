package com.flysoft.fretcorridor.exe.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Stockage objet MinIO pour les preuves d'enlèvement/livraison (RG-070,
 * EF-EXE-03) — même pattern que service-ida/DocumentStorageService (pièces
 * KYC), avec en plus l'empreinte cryptographique exigée par RG-072/EF-EXE-05
 * ("preuves stockées en écriture seule, avec empreinte cryptographique").
 *
 * Les objets restent privés (accès via URL présignée, jamais stockée telle
 * quelle) — mêmes garanties que côté KYC.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PreuveStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public record ResultatDepot(String objectKey, String empreinteSha256) {
    }

    /**
     * Dépose une pièce de preuve (photo ou signature) et retourne sa clé
     * objet ainsi que l'empreinte SHA-256 de son contenu (RG-072).
     * L'intégralité du fichier est lue en mémoire pour calculer l'empreinte
     * avant l'envoi -- acceptable ici (photos/signatures, jamais des fichiers
     * volumineux), contrairement au flux en streaming utilisé côté KYC.
     */
    public ResultatDepot deposer(String tenantId, UUID missionId, String categorie, MultipartFile fichier) {
        try {
            byte[] contenu = fichier.getBytes();
            String empreinte = sha256Hex(contenu);
            String objectKey = tenantId + "/" + missionId + "/"
                    + categorie + "_" + System.currentTimeMillis() + extensionDe(fichier.getOriginalFilename());

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(new java.io.ByteArrayInputStream(contenu), contenu.length, -1)
                    .contentType(fichier.getContentType() != null ? fichier.getContentType() : "application/octet-stream")
                    .build());

            log.info("Preuve déposée MinIO : {} (empreinte={})", objectKey, empreinte);
            return new ResultatDepot(objectKey, empreinte);
        } catch (Exception e) {
            log.error("Échec dépôt MinIO (preuve) : {}", e.getMessage());
            throw new RuntimeException("STOCKAGE_PREUVE_INDISPONIBLE");
        }
    }

    private static String sha256Hex(byte[] contenu) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(contenu));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private static String extensionDe(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".bin";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
