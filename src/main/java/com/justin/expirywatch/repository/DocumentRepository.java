package com.justin.expirywatch.repository;

import com.justin.expirywatch.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    @Query("SELECT d FROM Document d JOIN FETCH d.documentType WHERE d.user.id = :userId ORDER BY d.extractedExpiryDate ASC")
    List<Document> findByUserIdOrderByExtractedExpiryDateAsc(@Param("userId") UUID userId);
}
