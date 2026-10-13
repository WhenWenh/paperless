package at.fhtw.swen.paperless.ocr;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableRabbit
@SpringBootApplication
public class OcrApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                OcrApplication.class,
                args
        );
    }
}