package at.fhtw.swen.paperless.api.service.impl;

import at.fhtw.swen.paperless.api.service.DocumentStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileSystemDocumentStorageService
        implements DocumentStorageService {

    private final Path storageDirectory;

    public FileSystemDocumentStorageService() {
        this(Path.of("/tmp/paperless"));
    }

    FileSystemDocumentStorageService(Path storageDirectory) {
        this.storageDirectory = storageDirectory;
    }

    @Override
    public UUID store(MultipartFile file) {
        try {
            Files.createDirectories(storageDirectory);

            UUID storageUuid = UUID.randomUUID();

            Path target =
                    storageDirectory.resolve(storageUuid.toString());

            Files.copy(
                    file.getInputStream(),
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return storageUuid;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to store uploaded file",
                    exception
            );
        }
    }

    @Override
    public Resource load(UUID storageUuid) {
        return new FileSystemResource(
                storageDirectory.resolve(storageUuid.toString())
        );
    }

    @Override
    public void delete(UUID storageUuid) {
        try {
            Files.deleteIfExists(
                    storageDirectory.resolve(storageUuid.toString())
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to delete stored file",
                    exception
            );
        }
    }
}