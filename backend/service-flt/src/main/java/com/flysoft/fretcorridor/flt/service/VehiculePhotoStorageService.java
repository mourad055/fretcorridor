package com.flysoft.fretcorridor.flt.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Stockage objet MinIO des photos de carte grise (recto/verso) -- retour
 * utilisatrice du 24/08. Même pattern que service-exe/PreuveStorageService
 * (preuves d'enlèvement/livraison), sans l'empreinte SHA-256 (pas exigée ici,
 * contrairement à RG-072 côté preuves de mission).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VehiculePhotoStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    /** @param cote "recto" ou "verso" */
    public String deposer(String tenantId, UUID vehiculeId, String cote, MultipartFile fichier) {
        try {
            byte[] contenu = fichier.getBytes();
            String objectKey = tenantId + "/" + vehiculeId + "/"
                    + cote + "_" + System.currentTimeMillis() + extensionDe(fichier.getOriginalFilename());

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(new java.io.ByteArrayInputStream(contenu), contenu.length, -1)
                    .contentType(fichier.getContentType() != null ? fichier.getContentType() : "application/octet-stream")
                    .build());

            log.info("Photo véhicule déposée MinIO : {}", objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("Échec dépôt MinIO (photo véhicule) : {}", e.getMessage());
            throw new RuntimeException("STOCKAGE_PHOTO_INDISPONIBLE");
        }
    }

    private static String extensionDe(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
