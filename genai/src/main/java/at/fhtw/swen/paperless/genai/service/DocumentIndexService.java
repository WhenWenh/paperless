package at.fhtw.swen.paperless.genai.service;

import at.fhtw.swen.paperless.genai.index.DocumentIndex;
import at.fhtw.swen.paperless.genai.index.DocumentIndexRepository;
import at.fhtw.swen.paperless.share.messaging.OcrCompletedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentIndexService {

    private final DocumentIndexRepository repository;

    public void index(OcrCompletedMessage message) {

        DocumentIndex index = new DocumentIndex(
                message.documentId(),
                message.text(),
                message.ownerId(),
                message.title(),
                message.originalFilename(),
                message.tagId()
        );

        repository.save(index);
    }
}