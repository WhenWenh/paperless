package at.fhtw.swen.paperless.api.service.impl;


import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemDocumentStorageServiceTest {

    @org.junit.jupiter.api.io.TempDir
    Path storageDirectory;
    private FileSystemDocumentStorageService storageService;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        storageService = new FileSystemDocumentStorageService(storageDirectory);
    }
    @Test
    void store_shouldCreateFileAndReturnUuid() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "test.txt",
                        "text/plain",
                        "hello paperless".getBytes()
                );
        UUID storageUuid = storageService.store(file);
        assertThat(storageUuid).isNotNull();
        Path storedFile =
                storageDirectory.resolve(
                        storageUuid.toString()
                );
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(
                Files.readString(storedFile)
        )
                .isEqualTo("hello paperless");
    }

    @Test
    void load_shouldReturnStoredResource() throws Exception {
        UUID storageUuid =
                UUID.randomUUID();
        Path file =
                storageDirectory.resolve(
                        storageUuid.toString()
                );
        Files.createDirectories(
                storageDirectory
        );
        Files.writeString(
                file,
                "content"
        );
        Resource resource = storageService.load(storageUuid);
        assertThat(resource.exists()).isTrue();
        assertThat(
                resource.getContentAsString(StandardCharsets.UTF_8)
        )
                .isEqualTo("content");
    }

    @Test
    void delete_shouldRemoveStoredFile() throws Exception {
        UUID storageUuid =
                UUID.randomUUID();
        Files.createDirectories(
                storageDirectory
        );
        Path file =
                storageDirectory.resolve(
                        storageUuid.toString()
                );
        Files.writeString(
                file,
                "delete me"
        );
        assertThat(Files.exists(file)).isTrue();
        storageService.delete(storageUuid);
        assertThat(Files.exists(file)).isFalse();
    }

    @Test
    void delete_shouldNotFailWhenFileDoesNotExist() {
        UUID storageUuid = UUID.randomUUID();
        storageService.delete(storageUuid);
        assertThat(
                storageDirectory.resolve(
                        storageUuid.toString()
                )
        )
                .doesNotExist();
    }
    @Test
    void store_sameFilename_shouldKeepSeparateFiles() throws Exception {
        UUID first = storageService.store(new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf", new byte[]{1, 2}));
        UUID second = storageService.store(new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf", new byte[]{3, 4}));
        assertThat(first).isNotEqualTo(second);
        assertThat(storageService.load(first).getContentAsByteArray()).containsExactly((byte) 1, (byte) 2);
        assertThat(storageService.load(second).getContentAsByteArray()).containsExactly((byte) 3, (byte) 4);
    }
}