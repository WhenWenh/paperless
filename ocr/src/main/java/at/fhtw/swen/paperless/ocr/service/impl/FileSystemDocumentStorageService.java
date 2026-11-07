package at.fhtw.swen.paperless.ocr.service.impl;

import at.fhtw.swen.paperless.ocr.service.DocumentStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.UUID;

@Service
@ConditionalOnProperty(
        name = "storage.type",
        havingValue = "filesystem",
        matchIfMissing = true
)
public class FileSystemDocumentStorageService implements DocumentStorageService {

    private static final Path STORAGE_DIRECTORY =
            Path.of("/tmp/paperless");

    @Override
    public Resource load(UUID storageUuid) {
        Resource resource = new FileSystemResource(
                STORAGE_DIRECTORY.resolve(storageUuid.toString())
        );

        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Document not found: " + storageUuid
            );
        }

        return resource;
    }
}
