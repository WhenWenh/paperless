package at.fhtw.swen.paperless.api.persistence.repository;

import at.fhtw.swen.paperless.api.persistence.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("""
            SELECT COUNT(d)
            FROM Document d
            WHERE d.tag.id = :tagId
            """)
    long countDocumentsByTagId(UUID tagId);
}
