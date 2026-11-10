package at.fhtw.swen.paperless.share.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@ConditionalOnProperty(
        name = "storage.type",
        havingValue = "s3"
)
public class S3BucketInitializer {

    private final S3Client s3Client;
    private final StorageProperties properties;

    public S3BucketInitializer(
            S3Client s3Client,
            StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @PostConstruct
    public void initializeBucket() {
        String bucket = properties.getBucket();

        try {
            s3Client.headBucket(
                    HeadBucketRequest.builder()
                            .bucket(bucket)
                            .build());

        } catch (S3Exception e) {
            if (e.statusCode() == 404
                    || (e.awsErrorDetails() != null
                    && "NoSuchBucket".equals(e.awsErrorDetails().errorCode()))) {
                s3Client.createBucket(
                        CreateBucketRequest.builder()
                                .bucket(bucket)
                                .build());
            } else {
                throw e;
            }
        }
    }
}
