package at.fhtw.swen.paperless.genai;

import at.fhtw.swen.paperless.genai.config.OllamaProperties;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableRabbit
@SpringBootApplication
@EnableConfigurationProperties(OllamaProperties.class)
public class GenaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GenaiApplication.class, args);
    }
}