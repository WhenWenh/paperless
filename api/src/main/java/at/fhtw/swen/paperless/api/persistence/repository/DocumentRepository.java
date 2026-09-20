package at.fhtw.swen.paperless.api.persistence.repository;

import at.fhtw.swen.paperless.api.persistence.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
}