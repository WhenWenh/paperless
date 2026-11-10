package at.fhtw.swen.paperless.share.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    private String type;
    private String endpoint;
    private String region;
    private String accessKey;
    private String secretKey;
    private String bucket;
}
