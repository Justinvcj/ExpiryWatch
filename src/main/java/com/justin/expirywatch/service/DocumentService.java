package com.justin.expirywatch.service;

import com.justin.expirywatch.model.Document;
import com.justin.expirywatch.model.DocumentType;
import com.justin.expirywatch.model.User;
import com.justin.expirywatch.repository.DocumentRepository;
import com.justin.expirywatch.repository.DocumentTypeRepository;
import com.justin.expirywatch.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final DocumentTypeRepository documentTypeRepository;
    private final UserRepository userRepository;

    public DocumentService(DocumentRepository documentRepository, DocumentTypeRepository documentTypeRepository, UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.userRepository = userRepository;
    }

    public List<Document> getDocumentsForUser(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return documentRepository.findByUserIdOrderByExtractedExpiryDateAsc(user.getId());
    }

    public List<DocumentType> getAllDocumentTypes() {
        return documentTypeRepository.findAll();
    }

    public Document createDocument(String email, Integer typeId, String title, LocalDate expiryDate, String severity, byte[] fileData, String fileContentType, String rawText, BigDecimal confidence) {
        User user = userRepository.findByEmail(email).orElseThrow();
        DocumentType type = documentTypeRepository.findById(typeId).orElseThrow();

        Document doc = new Document();
        doc.setUser(user);
        doc.setDocumentType(type);
        doc.setTitle(title);
        doc.setExtractedExpiryDate(expiryDate);
        doc.setSeverity(severity);
        doc.setStatus("active");
        doc.setFileData(fileData);
        doc.setFileContentType(fileContentType);
        doc.setRawOcrText(rawText);
        doc.setConfidenceScore(confidence);
        
        return documentRepository.save(doc);
    }

    public Document getDocument(UUID id, String email) {
        Document doc = documentRepository.findById(id).orElseThrow();
        if (!doc.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }
        return doc;
    }

    public void updateDocumentStatus(UUID id, String email, String status) {
        Document doc = getDocument(id, email);
        doc.setStatus(status);
        documentRepository.save(doc);
    }
}
