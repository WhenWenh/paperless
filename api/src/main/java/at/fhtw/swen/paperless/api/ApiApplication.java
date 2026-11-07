package at.fhtw.swen.paperless.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import at.fhtw.swen.paperless.api.config.UploadProperties;

import at.fhtw.swen.paperless.share.storage.S3BucketInitializer;
import at.fhtw.swen.paperless.share.storage.S3Configuration;
import org.springframework.context.annotation.Import;

@EnableConfigurationProperties(UploadProperties.class)
@SpringBootApplication
@Import({
        S3Configuration.class,
        S3BucketInitializer.class
})
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}