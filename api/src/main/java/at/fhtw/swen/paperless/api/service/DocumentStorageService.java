package at.fhtw.swen.paperless.api.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface DocumentStorageService {

    UUID store(MultipartFile file);

    Resource load(UUID storageUuid);

    void delete(UUID storageUuid);
}