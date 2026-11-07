package at.fhtw.swen.paperless.ocr.ocr;

import java.io.IOException;

public interface CommandExecutor {

    String execute(String... command) throws IOException;
}