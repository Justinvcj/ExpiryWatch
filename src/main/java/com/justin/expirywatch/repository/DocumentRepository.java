package com.justin.expirywatch.repository;

import com.justin.expirywatch.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByUserIdOrderByExtractedExpiryDateAsc(UUID userId);
}
