package at.fhtw.swen.paperless.api.persistence.repository;

import at.fhtw.swen.paperless.api.controller.response.TagStatisticsResponse;
import at.fhtw.swen.paperless.api.persistence.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findByIdAndOwnerId(
            UUID id,
            UUID ownerId
    );

    List<Document> findAllByOwnerId(
            UUID ownerId
    );

    List<Document> findByOwnerIdAndTag_Name(
            UUID ownerId,
            String name
    );

    List<Document> findByOwnerIdAndTagIsNull(UUID ownerId);

    @Query("""
            SELECT new at.fhtw.swen.paperless.api.controller.response.TagStatisticsResponse(
                t.id,
                t.name,
                COUNT(d)
            )
            FROM Tag t
            LEFT JOIN Document d
                ON d.tag = t
               AND d.ownerId = :ownerId
            GROUP BY t.id, t.name
            ORDER BY t.name
            """)
    List<TagStatisticsResponse> getTagStatistics(
            UUID ownerId
    );

    @Query("""
            SELECT COUNT(d)
            FROM Document d
            WHERE d.ownerId = :ownerId
              AND d.tag IS NULL
            """)
    long countDocumentsWithoutTag(
            UUID ownerId
    );
}