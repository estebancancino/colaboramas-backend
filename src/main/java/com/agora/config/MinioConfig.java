package com.agora.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class MinioConfig {

    private static final Logger logger = LoggerFactory.getLogger(MinioConfig.class);

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Bean
    public S3Client s3Client() {
        S3Client client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(s3Configuration())
                .region(Region.US_EAST_1)
                .build();

        // Crear el bucket si no existe. En R2 el bucket ya existe y la credencial no puede
        // crearlo, así que cualquier error solo se registra y la app sigue arrancando.
        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                try {
                    client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                    logger.info("Bucket '{}' creado", bucketName);
                } catch (SdkException createError) {
                    logger.warn("No se pudo crear el bucket '{}': {}", bucketName, createError.getMessage());
                }
            } else {
                logger.warn("No se pudo verificar el bucket '{}': {}", bucketName, e.getMessage());
            }
        } catch (SdkException e) {
            logger.warn("No se pudo verificar el bucket '{}': {}", bucketName, e.getMessage());
        }

        return client;
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(s3Configuration())
                .region(Region.US_EAST_1)
                .build();
    }

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    private S3Configuration s3Configuration() {
        return S3Configuration.builder().pathStyleAccessEnabled(true).build();
    }
}
