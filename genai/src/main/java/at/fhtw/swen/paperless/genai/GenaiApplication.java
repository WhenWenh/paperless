package at.fhtw.swen.paperless.genai;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableRabbit
@SpringBootApplication
public class GenaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GenaiApplication.class, args);
    }
}