package com.justin.expirywatch.dto;

import java.time.LocalDate;
import java.util.UUID;

public interface DocumentView {
    UUID getId();
    String getTitle();
    LocalDate getExtractedExpiryDate();
    String getSeverity();
    String getStatus();
    DocumentTypeView getDocumentType();

    interface DocumentTypeView {
        String getName();
    }
}
