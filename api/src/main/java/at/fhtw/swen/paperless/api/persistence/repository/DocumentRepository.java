package at.fhtw.swen.paperless.api.persistence.repository;

import at.fhtw.swen.paperless.api.controller.response.TagStatisticsResponse;
import at.fhtw.swen.paperless.api.persistence.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByTag_Name(String name);

    @Query("""
            SELECT new at.fhtw.swen.paperless.api.controller.response.TagStatisticsResponse(
                t.id,
                t.name,
                COUNT(d)
            )
            FROM Tag t
            LEFT JOIN Document d
                ON d.tag = t
            GROUP BY t.id, t.name
            ORDER BY t.name
            """)
    List<TagStatisticsResponse> getTagStatistics();

    @Query("""
            SELECT COUNT(d)
            FROM Document d
            WHERE d.tag IS NULL
            """)
    long countDocumentsWithoutTag();
}
