package com.agora.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@Service
public class UserImageService {


    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.presign-duration-minutes}")
    private long presignDurationMinutes;

    public UserImageService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Sube la imagen al bucket y devuelve la key del objeto (no una URL).
     */
    public String uploadUserImage(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
        String key = UUID.randomUUID() + "_" + originalName.replaceAll("[^A-Za-z0-9._-]", "_");

        var request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        // Se sube directo desde el stream, sin archivo temporal
        try (InputStream in = file.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(in, file.getSize()));
        }

        return key;
    }

    /**
     * Convierte lo guardado en users.image_url en una URL utilizable por la app:
     * las URLs externas (http/https) se devuelven tal cual y las keys del bucket
     * se transforman en una URL firmada temporal.
     */
    public String resolveImageUrl(String stored) {
        if (stored == null || stored.isBlank()
                || stored.startsWith("http://") || stored.startsWith("https://")) {
            return stored;
        }

        var presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignDurationMinutes))
                .getObjectRequest(GetObjectRequest.builder().bucket(bucketName).key(stored).build())
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
