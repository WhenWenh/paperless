package at.fhtw.swen.paperless.ocr.service.impl;

import at.fhtw.swen.paperless.ocr.service.DocumentStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@ConditionalOnProperty(
        name = "storage.type",
        havingValue = "s3"
)
public class S3DocumentStorageService implements DocumentStorageService {

    private final S3Client s3;
    private final String bucket;

    public S3DocumentStorageService(
            S3Client s3,
            @Value("${storage.bucket}") String bucket) {

        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public Resource load(UUID storageUuid) {
        try {
            byte[] content = s3.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(storageUuid.toString())
                            .build(),
                    software.amazon.awssdk.core.sync.ResponseTransformer.toBytes()
            ).asByteArray();

            Path temp = Files.createTempFile(storageUuid.toString(), ".tmp");
            Files.write(temp, content);

            return new FileSystemResource(temp);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load document from S3: " + storageUuid,
                    e
            );
        }
    }
}