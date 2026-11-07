package at.fhtw.swen.paperless.ocr;

import at.fhtw.swen.paperless.share.storage.S3BucketInitializer;
import at.fhtw.swen.paperless.share.storage.S3Configuration;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@EnableRabbit
@SpringBootApplication
@Import({
        S3Configuration.class,
        S3BucketInitializer.class
})
public class OcrApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                OcrApplication.class,
                args
        );
    }
}