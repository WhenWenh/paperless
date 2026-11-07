package at.fhtw.swen.paperless.ocr.ocr.impl;

import at.fhtw.swen.paperless.ocr.ocr.CommandExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class ProcessCommandExecutorImpl implements CommandExecutor {

    @Override
    public String execute(String... command) throws IOException {
        Process process = new ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();

        try {
            String output;

            try (var input = process.getInputStream()) {
                output = new String(
                        input.readAllBytes(),
                        StandardCharsets.UTF_8
                );
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IllegalStateException(
                        "Command failed (" + exitCode + "): "
                                + String.join(" ", command)
                                + "\n"
                                + output
                );
            }

            return output;

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IOException(
                    "Command interrupted",
                    exception
            );

        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}