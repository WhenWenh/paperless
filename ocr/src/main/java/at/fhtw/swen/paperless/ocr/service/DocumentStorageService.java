package at.fhtw.swen.paperless.ocr.service;

import org.springframework.core.io.Resource;

import java.util.UUID;

public interface DocumentStorageService {

    Resource load(UUID storageUuid);
}
