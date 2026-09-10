package com.justin.expirywatch.repository;

import com.justin.expirywatch.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Integer> {
    Optional<DocumentType> findByName(String name);
}
